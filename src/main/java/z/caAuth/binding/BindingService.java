package z.caAuth.binding;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.account.AccountManager;
import z.caAuth.account.AccountRepository;
import z.caAuth.account.BindingType;
import z.caAuth.config.AuthMessagesConfig;
import z.caAuth.util.Colors;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class BindingService {

    public enum RedeemStatus { BOUND, INVALID_CODE, LOCKED, ALREADY_BOUND, BINDING_FAILED }

    public record RedeemResult(RedeemStatus status, UUID playerUUID, String playerName, long lockedMinutes) {
    }

    private final AccountManager accountManager;
    private final JavaPlugin plugin;
    private final AuthMessagesConfig authMessagesConfig;
    private final CodeAttemptLimiter attemptLimiter;
    private final PlayerKicker playerKicker;
    private volatile Consumer<String> discordUnbindHandler;

    public void setDiscordUnbindHandler(Consumer<String> handler) {
        this.discordUnbindHandler = handler;
    }

    public BindingService(AccountManager accountManager, JavaPlugin plugin, AuthMessagesConfig authMessagesConfig,
                          CodeAttemptLimiter attemptLimiter, PlayerKicker playerKicker) {
        this.accountManager = accountManager;
        this.plugin = plugin;
        this.authMessagesConfig = authMessagesConfig;
        this.attemptLimiter = attemptLimiter;
        this.playerKicker = playerKicker;
    }

    public RedeemResult redeemCode(BindingType type, String bindingId, String code) {
        String limiterKey = type.getId() + ":" + bindingId;
        long lockedMinutes = attemptLimiter.lockedMinutesLeft(limiterKey);
        if (lockedMinutes > 0) {
            return new RedeemResult(RedeemStatus.LOCKED, null, null, lockedMinutes);
        }

        UUID playerUUID = accountManager.consumePendingCode(code);
        if (playerUUID == null) {
            attemptLimiter.registerFailure(limiterKey);
            return new RedeemResult(RedeemStatus.INVALID_CODE, null, null, 0);
        }
        attemptLimiter.reset(limiterKey);

        String playerName = getPlayerName(playerUUID);
        if (accountManager.isAccountBound(playerUUID)) {
            return new RedeemResult(RedeemStatus.ALREADY_BOUND, playerUUID, playerName, 0);
        }
        if (!bind(playerUUID, type, bindingId)) {
            return new RedeemResult(RedeemStatus.BINDING_FAILED, playerUUID, playerName, 0);
        }
        return new RedeemResult(RedeemStatus.BOUND, playerUUID, playerName, 0);
    }

    private boolean bind(UUID playerUUID, BindingType type, String bindingId) {
        UUID existingPlayer = accountManager.getPlayerByBinding(type, bindingId);
        if (existingPlayer != null && !existingPlayer.equals(playerUUID)) {
            return false;
        }

        accountManager.bind(playerUUID, type, bindingId);

        String message = type == BindingType.DISCORD
                ? authMessagesConfig.getAccountBoundSuccessDiscord()
                : authMessagesConfig.getAccountBoundSuccessTelegram();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player player = Bukkit.getPlayer(playerUUID);
            if (player != null) {
                Colors.sendMessage(player, message);
            }
        });
        return true;
    }

    public boolean unbindAccount(UUID playerUUID) {
        if (!accountManager.isAccountBound(playerUUID)) {
            return false;
        }

        List<AccountRepository.TwinkRow> twinks = accountManager.isTwink(playerUUID)
                ? List.of()
                : accountManager.getTwinks(playerUUID);
        String kickMessage = authMessagesConfig.getUnbindingKick();

        String playerName = getPlayerName(playerUUID);
        String discordId = getDiscordId(playerUUID);
        accountManager.unbindAccount(playerUUID);
        accountManager.deleteTwinks(playerUUID);
        Consumer<String> handler = discordUnbindHandler;
        if (handler != null && discordId != null && !discordId.isBlank()) {
            handler.accept(discordId);
        }
        playerKicker.kick(playerUUID, playerName, kickMessage);
        for (AccountRepository.TwinkRow twink : twinks) {
            playerKicker.kick(twink.uuid(), twink.name(), kickMessage);
        }
        return true;
    }

    public UUID getPlayerByBinding(BindingType type, String bindingId) {
        return accountManager.getPlayerByBinding(type, bindingId);
    }

    public String getDiscordId(UUID playerUUID) {
        if (accountManager.isTwink(playerUUID)) {
            return null;
        }
        return accountManager.getBindingId(playerUUID, BindingType.DISCORD);
    }

    public String getPlayerName(UUID playerUUID) {
        String stored = accountManager.getPlayerName(playerUUID);
        if (stored != null && !stored.isBlank()) {
            return stored;
        }
        String fromServer = Bukkit.getOfflinePlayer(playerUUID).getName();
        if (fromServer != null && !fromServer.isBlank()) {
            accountManager.setPlayerName(playerUUID, fromServer);
        }
        return fromServer;
    }
}
