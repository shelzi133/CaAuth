package z.caAuth.skins;

import net.skinsrestorer.api.SkinsRestorerProvider;
import net.skinsrestorer.api.connections.model.MineSkinResponse;
import net.skinsrestorer.api.exception.DataRequestException;
import net.skinsrestorer.api.exception.MineSkinException;
import net.skinsrestorer.api.property.InputDataResult;
import net.skinsrestorer.api.property.SkinIdentifier;
import net.skinsrestorer.api.property.SkinProperty;
import net.skinsrestorer.api.property.SkinType;
import net.skinsrestorer.api.property.SkinVariant;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.account.AccountManager;
import z.caAuth.account.AccountRepository;
import z.caAuth.account.SkinProfile;
import z.caAuth.account.SkinSource;
import z.caAuth.config.MainConfig;
import z.caAuth.config.SkinsConfig;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public class SkinService {

    public enum Status { OK, INVALID_INPUT, NOT_FOUND, INVALID_PNG, TOO_LARGE, UNAVAILABLE, ERROR }

    public record Result(Status status, SkinProfile profile) {
    }

    public record Account(UUID uuid, String name) {
    }

    private static final Pattern NICK_PATTERN = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final int MAX_URL_LENGTH = 512;
    private static final int KILOBYTE = 1024;
    private static final String PNG_LABEL = "PNG";

    private final JavaPlugin plugin;
    private final AccountManager accountManager;
    private final SkinsConfig config;
    private final MainConfig mainConfig;

    public SkinService(JavaPlugin plugin, AccountManager accountManager, SkinsConfig config, MainConfig mainConfig) {
        this.plugin = plugin;
        this.accountManager = accountManager;
        this.config = config;
        this.mainConfig = mainConfig;
    }

    public boolean isAvailable() {
        return mainConfig.module.skins.enabled && plugin.getServer().getPluginManager().isPluginEnabled("SkinsRestorer");
    }

    public int maxPngKilobytes() {
        return Math.max(1, config.maxPngBytes / KILOBYTE);
    }

    public List<SkinProfile> profiles(UUID owner) {
        return accountManager.getSkinProfiles(owner);
    }

    public SkinProfile profile(UUID owner, int slot) {
        return accountManager.getSkinProfile(owner, slot);
    }

    public void remove(UUID owner, int slot) {
        accountManager.deleteSkinProfile(owner, slot);
    }

    public List<Account> accounts(UUID owner) {
        List<Account> accounts = new ArrayList<>();
        String ownerName = accountManager.getPlayerName(owner);
        accounts.add(new Account(owner, ownerName != null && !ownerName.isBlank() ? ownerName : owner.toString()));
        for (AccountRepository.TwinkRow twink : accountManager.getTwinks(owner)) {
            accounts.add(new Account(twink.uuid(), twink.name()));
        }
        return accounts;
    }

    public Account account(UUID owner, String rawUuid) {
        UUID uuid;
        try {
            uuid = UUID.fromString(rawUuid);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return accounts(owner).stream()
                .filter(account -> account.uuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    public Result setFromNick(UUID owner, int slot, String rawNick) {
        if (!isAvailable()) {
            return new Result(Status.UNAVAILABLE, null);
        }
        String nick = rawNick.trim();
        if (!NICK_PATTERN.matcher(nick).matches()) {
            return new Result(Status.INVALID_INPUT, null);
        }
        return fromInput(owner, slot, SkinSource.NICK, nick);
    }

    public Result setFromUrl(UUID owner, int slot, String rawUrl) {
        if (!isAvailable()) {
            return new Result(Status.UNAVAILABLE, null);
        }
        String url = rawUrl.trim();
        if (!isHttpUrl(url)) {
            return new Result(Status.INVALID_INPUT, null);
        }
        return fromInput(owner, slot, SkinSource.URL, url);
    }

    public Result setFromPng(UUID owner, int slot, byte[] png) {
        if (!isAvailable()) {
            return new Result(Status.UNAVAILABLE, null);
        }
        if (png.length > config.maxPngBytes) {
            return new Result(Status.TOO_LARGE, null);
        }
        if (!isSkinPng(png)) {
            return new Result(Status.INVALID_PNG, null);
        }
        try {
            MineSkinResponse response = SkinsRestorerProvider.get().getMineSkinAPI().genSkin(png, SkinVariant.CLASSIC);
            SkinProperty property = response.getProperty();
            String customId = UUID.randomUUID().toString();
            SkinsRestorerProvider.get().getSkinStorage().setCustomSkinData(customId, property);
            return store(owner, slot, SkinSource.PNG, PNG_LABEL, SkinIdentifier.ofCustom(customId), property);
        } catch (DataRequestException | MineSkinException | RuntimeException e) {
            plugin.getLogger().warning("Не удалось загрузить PNG-скин: " + e.getMessage());
            return new Result(Status.ERROR, null);
        }
    }

    public boolean wear(Player player, SkinProfile profile) {
        return wear(player.getUniqueId(), player, profile);
    }

    public boolean wear(UUID target, Player online, SkinProfile profile) {
        if (!isAvailable()) {
            return false;
        }
        try {
            SkinIdentifier identifier = identifierOf(profile);
            SkinsRestorerProvider.get().getPlayerStorage().setSkinIdOfPlayer(target, identifier);
            if (online != null) {
                SkinsRestorerProvider.get().getSkinApplier(Player.class).applySkin(online, identifier);
            }
            return true;
        } catch (RuntimeException e) {
            plugin.getLogger().warning("Не удалось надеть скин игроку " + target + ": " + e.getMessage());
            return false;
        }
    }

    private Result fromInput(UUID owner, int slot, SkinSource source, String input) {
        try {
            Optional<InputDataResult> found = SkinsRestorerProvider.get().getSkinStorage().findOrCreateSkinData(input);
            if (found.isEmpty()) {
                return new Result(Status.NOT_FOUND, null);
            }
            InputDataResult data = found.get();
            return store(owner, slot, source, input, data.getIdentifier(), data.getProperty());
        } catch (DataRequestException | MineSkinException | RuntimeException e) {
            plugin.getLogger().warning("Не удалось получить скин «" + input + "»: " + e.getMessage());
            return new Result(Status.ERROR, null);
        }
    }

    private Result store(UUID owner, int slot, SkinSource source, String input, SkinIdentifier identifier,
                         SkinProperty property) {
        SkinProfile profile = new SkinProfile(slot, source, input, identifier.getIdentifier(),
                identifier.getSkinType().name(),
                identifier.getSkinVariant() != null ? identifier.getSkinVariant().name() : SkinVariant.CLASSIC.name(),
                property.getValue(), property.getSignature());
        accountManager.saveSkinProfile(owner, profile);
        return new Result(Status.OK, profile);
    }

    private static SkinIdentifier identifierOf(SkinProfile profile) {
        return SkinIdentifier.of(profile.skinId(), SkinVariant.valueOf(profile.variant()),
                SkinType.valueOf(profile.skinType()));
    }

    private static boolean isHttpUrl(String url) {
        String lower = url.toLowerCase(Locale.ROOT);
        return url.length() <= MAX_URL_LENGTH
                && (lower.startsWith("https://") || lower.startsWith("http://"))
                && !url.contains(" ");
    }

    private static boolean isSkinPng(byte[] png) {
        if (png.length < PNG_SIGNATURE.length) {
            return false;
        }
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (png[i] != PNG_SIGNATURE[i]) {
                return false;
            }
        }
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(png));
            return image != null && image.getWidth() == 64 && (image.getHeight() == 32 || image.getHeight() == 64);
        } catch (IOException e) {
            return false;
        }
    }
}
