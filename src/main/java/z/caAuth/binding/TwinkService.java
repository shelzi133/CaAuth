package z.caAuth.binding;

import z.caAuth.account.AccountManager;
import z.caAuth.account.AccountRepository.Invite;
import z.caAuth.account.AccountRepository.TwinkRow;
import z.caAuth.account.BindingType;
import z.caAuth.config.AuthConfig;
import z.caAuth.config.MainConfig;
import z.caAuth.integration.velocity.VelocityClient;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

public class TwinkService {

    public enum InviteStatus {
        CREATED, NOT_BOUND, UNAVAILABLE, INVALID_NAME, LIMIT_REACHED, ALREADY_BOUND, ALREADY_PENDING, NOT_SEEN
    }

    public record InviteResult(InviteStatus status, String twinkName, String code, int lifetimeMinutes) {
    }

    public enum RedeemStatus { BOUND, INVALID, LOCKED, ERROR }

    public record RedeemResult(RedeemStatus status, long lockedMinutes) {
    }

    public record TwinkEntry(String name, boolean pending) {
    }

    public enum RemoveStatus { REMOVED, NOT_FOUND, NOT_BOUND }

    public record Slots(int max, List<TwinkEntry> entries) {
        public int free() {
            return Math.max(0, max - entries.size());
        }
    }

    private static final Pattern NAME_PATTERN = Pattern.compile("^[.*+!\\-]{0,3}[A-Za-z0-9_]{3,16}$");

    private static final int MAX_NAME_LENGTH = 16;

    private final AccountManager accountManager;
    private final BindingService bindingService;
    private final VelocityClient velocityClient;
    private final MainConfig mainConfig;
    private final AuthConfig authConfig;
    private final CodeAttemptLimiter attemptLimiter;
    private final SecureRandom random = new SecureRandom();

    public TwinkService(AccountManager accountManager, BindingService bindingService, VelocityClient velocityClient,
                        MainConfig mainConfig, AuthConfig authConfig, CodeAttemptLimiter attemptLimiter) {
        this.accountManager = accountManager;
        this.bindingService = bindingService;
        this.velocityClient = velocityClient;
        this.mainConfig = mainConfig;
        this.authConfig = authConfig;
        this.attemptLimiter = attemptLimiter;
    }

    public boolean isAvailable() {
        return mainConfig.module.tiauth.enabled && mainConfig.isVelocityMode();
    }

    public int lifetimeMinutes() {
        return Math.max(1, authConfig.bots.twink.inviteLifetimeMinutes);
    }

    public Slots slots(BindingType type, String bindingId) {
        UUID owner = bindingService.getPlayerByBinding(type, bindingId);
        List<TwinkEntry> entries = new ArrayList<>();
        if (owner != null) {
            for (TwinkRow row : accountManager.getTwinks(owner)) {
                entries.add(new TwinkEntry(row.name(), false));
            }
            for (Invite invite : accountManager.getInvites(owner)) {
                entries.add(new TwinkEntry(invite.realName(), true));
            }
        }
        return new Slots(Math.max(0, authConfig.bots.twink.maxCount), entries);
    }

    public RemoveStatus remove(BindingType type, String bindingId, String twinkName) {
        UUID owner = bindingService.getPlayerByBinding(type, bindingId);
        if (owner == null) {
            return RemoveStatus.NOT_BOUND;
        }
        for (TwinkRow row : accountManager.getTwinks(owner)) {
            if (row.name() != null && row.name().equalsIgnoreCase(twinkName)) {
                bindingService.unbindAccount(row.uuid());
                return RemoveStatus.REMOVED;
            }
        }
        Invite invite = accountManager.findInvite(twinkName);
        if (invite != null && invite.owner().equals(owner)) {
            accountManager.deleteInvite(twinkName);
            return RemoveStatus.REMOVED;
        }
        return RemoveStatus.NOT_FOUND;
    }

    public boolean hasActiveInvite(String twinkName) {
        return accountManager.findInvite(twinkName) != null;
    }

    public CompletableFuture<InviteResult> invite(BindingType type, String bindingId, String rawName) {
        UUID owner = bindingService.getPlayerByBinding(type, bindingId);
        if (owner == null) {
            return done(InviteStatus.NOT_BOUND, null);
        }
        if (!isAvailable()) {
            return done(InviteStatus.UNAVAILABLE, null);
        }

        String name = rawName == null ? "" : rawName.trim();
        if (!NAME_PATTERN.matcher(name).matches() || name.length() > MAX_NAME_LENGTH) {
            return done(InviteStatus.INVALID_NAME, name);
        }
        if (accountManager.isNameBound(name)) {
            return done(InviteStatus.ALREADY_BOUND, name);
        }

        Invite existing = accountManager.findInvite(name);
        if (existing != null && !existing.owner().equals(owner)) {
            return done(InviteStatus.ALREADY_PENDING, name);
        }
        Slots slots = slots(type, bindingId);
        if (existing == null && slots.free() == 0) {
            return done(InviteStatus.LIMIT_REACHED, name);
        }

        return velocityClient.findPlayer(name).thenApply(lookup -> {
            if (!lookup.available()) {
                return new InviteResult(InviteStatus.UNAVAILABLE, name, null, 0);
            }
            if (!lookup.exists()) {
                return new InviteResult(InviteStatus.NOT_SEEN, name, null, 0);
            }
            String realName = lookup.realName();
            if (accountManager.isNameBound(realName) || accountManager.isAccountBound(offlineUuid(realName))) {
                return new InviteResult(InviteStatus.ALREADY_BOUND, realName, null, 0);
            }
            String code = generateCode();
            accountManager.createInvite(realName, owner, code);
            return new InviteResult(InviteStatus.CREATED, realName, code, lifetimeMinutes());
        });
    }

    public RedeemResult redeem(UUID twinkUUID, String twinkName, String code) {
        String limiterKey = "twink:" + twinkUUID;
        long lockedMinutes = attemptLimiter.lockedMinutesLeft(limiterKey);
        if (lockedMinutes > 0) {
            return new RedeemResult(RedeemStatus.LOCKED, lockedMinutes);
        }

        Invite invite = accountManager.findInvite(twinkName);
        if (invite == null || !invite.code().equals(code)) {
            attemptLimiter.registerFailure(limiterKey);
            return new RedeemResult(RedeemStatus.INVALID, 0);
        }

        BindingType type = accountManager.getBindingType(invite.owner());
        String bindingId = accountManager.getBindingId(invite.owner());
        if (type == null || bindingId == null || accountManager.isTwink(invite.owner())) {
            accountManager.deleteInvite(twinkName);
            return new RedeemResult(RedeemStatus.ERROR, 0);
        }

        accountManager.bindTwink(twinkUUID, invite.owner(), type, bindingId, twinkName);
        accountManager.deleteInvite(twinkName);
        attemptLimiter.reset(limiterKey);
        return new RedeemResult(RedeemStatus.BOUND, 0);
    }

    private UUID offlineUuid(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }

    private String generateCode() {
        String code;
        do {
            code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        } while (accountManager.isInviteCodeTaken(code));
        return code;
    }

    private CompletableFuture<InviteResult> done(InviteStatus status, String name) {
        return CompletableFuture.completedFuture(new InviteResult(status, name, null, 0));
    }
}
