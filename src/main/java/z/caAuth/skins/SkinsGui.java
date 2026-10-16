package z.caAuth.skins;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;
import z.caAuth.account.SkinProfile;
import z.caAuth.account.SkinSource;
import z.caAuth.config.AuthMessagesConfig;
import z.caAuth.config.SkinsConfig;
import z.caAuth.util.Colors;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkinsGui implements Listener {

    private static final int PROFILE_INVENTORY_SIZE = 45;
    private static final int TYPE_INVENTORY_SIZE = 27;
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final String BASE_HEAD_PREFIX = "base-head=";

    private enum Kind { PROFILES, TYPES }

    private enum Choice { NICK, URL, BOT }

    private record PendingInput(int slot, Choice choice, UUID target, long expiresAt) {
    }

    private static final class Holder implements InventoryHolder {
        private final Kind kind;
        private final int slot;
        private final UUID target;
        private Inventory inventory;

        private Holder(Kind kind, int slot, UUID target) {
            this.kind = kind;
            this.slot = slot;
            this.target = target;
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }

    private final JavaPlugin plugin;
    private final SkinService skins;
    private final SkinsConfig config;
    private final SkinsBotBridge bots;
    private final AuthMessagesConfig authMessages;
    private final Map<UUID, PendingInput> pending = new ConcurrentHashMap<>();

    public SkinsGui(JavaPlugin plugin, SkinService skins, SkinsConfig config, SkinsBotBridge bots,
                    AuthMessagesConfig authMessages) {
        this.plugin = plugin;
        this.skins = skins;
        this.config = config;
        this.bots = bots;
        this.authMessages = authMessages;
    }

    public void openProfiles(Player player) {
        openProfiles(player, player.getUniqueId());
    }

    public void openProfiles(Player player, UUID target) {
        Holder holder = new Holder(Kind.PROFILES, 0, target);
        Inventory inventory = Bukkit.createInventory(holder, PROFILE_INVENTORY_SIZE, component(config.gui.title));
        holder.inventory = inventory;
        fill(inventory, PROFILE_INVENTORY_SIZE);

        Map<Integer, SkinProfile> profiles = new ConcurrentHashMap<>();
        for (SkinProfile profile : skins.profiles(target)) {
            profiles.put(profile.slot(), profile);
        }
        for (int index = 0; index < config.gui.profileSlots.size(); index++) {
            int slot = index + 1;
            inventory.setItem(config.gui.profileSlots.get(index), profileItem(slot, profiles.get(slot)));
        }
        player.openInventory(inventory);
    }

    private void openTypes(Player player, int slot, UUID target) {
        Holder holder = new Holder(Kind.TYPES, slot, target);
        Inventory inventory = Bukkit.createInventory(holder, TYPE_INVENTORY_SIZE, component(config.gui.typeTitle));
        holder.inventory = inventory;
        fill(inventory, TYPE_INVENTORY_SIZE);

        inventory.setItem(config.gui.typeNickSlot, item(material(config.gui.typeNickMaterial, Material.NAME_TAG),
                config.gui.typeNickName, config.gui.typeNickLore));
        inventory.setItem(config.gui.typeUrlSlot, item(material(config.gui.typeUrlMaterial, Material.COMPASS),
                config.gui.typeUrlName, config.gui.typeUrlLore));
        inventory.setItem(config.gui.typeBotSlot, item(material(config.gui.typeBotMaterial, Material.BOOK),
                config.gui.typeBotName, config.gui.typeBotLore));
        inventory.setItem(config.gui.typeBackSlot, item(material(config.gui.backMaterial, Material.ARROW),
                config.gui.backName, List.of()));
        player.openInventory(inventory);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || event.getClickedInventory() != event.getInventory()) {
            return;
        }
        if (holder.kind == Kind.PROFILES) {
            handleProfileClick(player, event, holder.target);
        } else {
            handleTypeClick(player, holder.slot, event.getRawSlot(), holder.target);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        pending.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        PendingInput input = pending.remove(player.getUniqueId());
        if (input == null) {
            return;
        }
        event.setCancelled(true);
        String text = PlainTextComponentSerializer.plainText().serialize(event.message()).trim();

        if (System.currentTimeMillis() > input.expiresAt()) {
            runSync(() -> error(player, config.messages.timeout));
            return;
        }
        if (text.equalsIgnoreCase(config.cancelWord)) {
            runSync(() -> warning(player, config.messages.cancelled));
            return;
        }
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            UUID owner = input.target();
            SkinService.Result result = input.choice() == Choice.NICK
                    ? skins.setFromNick(owner, input.slot(), text)
                    : skins.setFromUrl(owner, input.slot(), text);
            runSync(() -> {
                if (player.isOnline()) {
                    String message = resultMessage(result, input, text);
                    if (result.status() == SkinService.Status.OK) {
                        success(player, message);
                    } else {
                        error(player, message);
                    }
                }
            });
        });
    }

    private void handleProfileClick(Player player, InventoryClickEvent event, UUID target) {
        int index = config.gui.profileSlots.indexOf(event.getRawSlot());
        if (index < 0) {
            return;
        }
        int slot = index + 1;
        SkinProfile profile = skins.profile(target, slot);
        ClickType click = event.getClick();

        if (click == ClickType.SHIFT_RIGHT) {
            if (profile == null) {
                error(player, config.messages.deleteEmpty.replace("{slot}", String.valueOf(slot)));
                return;
            }
            skins.remove(target, slot);
            success(player, config.messages.deleted.replace("{slot}", String.valueOf(slot)));
            openProfiles(player, target);
        } else if (click == ClickType.LEFT) {
            if (profile == null) {
                openTypes(player, slot, target);
                return;
            }
            if (skins.wear(target, Bukkit.getPlayer(target), profile)) {
                success(player, config.messages.wearSuccess.replace("{slot}", String.valueOf(slot)));
                player.closeInventory();
            } else {
                error(player, config.messages.wearError);
            }
        } else if (click == ClickType.RIGHT) {
            openTypes(player, slot, target);
        }
    }

    private void handleTypeClick(Player player, int profileSlot, int rawSlot, UUID target) {
        if (rawSlot == config.gui.typeNickSlot) {
            startInput(player, profileSlot, Choice.NICK, target);
        } else if (rawSlot == config.gui.typeUrlSlot) {
            startInput(player, profileSlot, Choice.URL, target);
        } else if (rawSlot == config.gui.typeBotSlot) {
            player.closeInventory();
            boolean requested = target.equals(player.getUniqueId()) && bots.requestPng(target, profileSlot);
            if (requested) {
                success(player, config.messages.promptBot.replace("{slot}", String.valueOf(profileSlot)));
            } else {
                error(player, config.messages.botUnavailable);
            }
        } else if (rawSlot == config.gui.typeBackSlot) {
            openProfiles(player, target);
        }
    }

    private void startInput(Player player, int slot, Choice choice, UUID target) {
        long expiresAt = System.currentTimeMillis() + config.inputTimeoutSeconds * MILLIS_PER_SECOND;
        pending.put(player.getUniqueId(), new PendingInput(slot, choice, target, expiresAt));
        player.closeInventory();
        String template = choice == Choice.NICK ? config.messages.promptNick : config.messages.promptUrl;
        warning(player, template
                .replace("{cancel}", config.cancelWord)
                .replace("{seconds}", String.valueOf(config.inputTimeoutSeconds)));
    }

    private String resultMessage(SkinService.Result result, PendingInput input, String text) {
        return switch (result.status()) {
            case OK -> config.messages.setSuccess.replace("{slot}", String.valueOf(input.slot()));
            case INVALID_INPUT -> input.choice() == Choice.NICK ? config.messages.invalidNick : config.messages.invalidUrl;
            case NOT_FOUND -> config.messages.notFound.replace("{input}", text);
            case UNAVAILABLE -> config.messages.unavailable;
            case INVALID_PNG, TOO_LARGE, ERROR -> config.messages.saveError;
        };
    }

    private ItemStack profileItem(int slot, SkinProfile profile) {
        String emptyMaterial = config.gui.emptyProfile.material;
        boolean customHead = emptyMaterial.startsWith(BASE_HEAD_PREFIX);
        boolean texturedHead = profile != null || customHead;
        ItemStack item = new ItemStack(profile == null && !customHead
                ? material(emptyMaterial, Material.PLAYER_HEAD)
                : Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (texturedHead && meta instanceof SkullMeta skull) {
            String value = profile != null ? profile.textureValue() : emptyMaterial.substring(BASE_HEAD_PREFIX.length());
            String signature = profile != null ? profile.textureSignature() : null;
            PlayerProfile playerProfile = Bukkit.createProfile(UUID.randomUUID(), null);
            playerProfile.setProperty(new ProfileProperty("textures", value, signature));
            skull.setPlayerProfile(playerProfile);
        }
        meta.displayName(component(config.gui.profileName.replace("{slot}", String.valueOf(slot))));
        List<String> lore = profile == null ? config.gui.profileEmptyLore : config.gui.profileFilledLore;
        meta.lore(lore.stream().map(line -> component(profile == null ? line
                : line.replace("{input}", profile.input()).replace("{source}", sourceLabel(profile.source()))))
                .toList());
        item.setItemMeta(meta);
        return item;
    }

    private String sourceLabel(SkinSource source) {
        return switch (source) {
            case NICK -> config.labels.sourceNick;
            case URL -> config.labels.sourceUrl;
            case PNG -> config.labels.sourcePng;
        };
    }

    private void fill(Inventory inventory, int size) {
        for (SkinsConfig.Filter filter : config.gui.filters.values()) {
            ItemStack filterItem = item(material(filter.material, Material.GRAY_STAINED_GLASS_PANE), " ", List.of());
            for (int slot : filter.slots) {
                if (slot >= 0 && slot < size) {
                    inventory.setItem(slot, filterItem);
                }
            }
        }
    }

    private ItemStack item(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(component(name));
        meta.lore(lore.stream().map(this::component).toList());
        item.setItemMeta(meta);
        return item;
    }

    private Component component(String text) {
        return LegacyComponentSerializer.legacySection()
                .deserialize(Colors.colorize(text))
                .decoration(TextDecoration.ITALIC, false);
    }

    private static Material material(String name, Material fallback) {
        Material material = Material.matchMaterial(name);
        return material != null ? material : fallback;
    }

    private void success(Player player, String text) {
        player.sendMessage(authMessages.getSuccessPrefix() + Colors.colorize(text));
    }

    private void error(Player player, String text) {
        player.sendMessage(authMessages.getErrorPrefix() + Colors.colorize(text));
    }

    private void warning(Player player, String text) {
        player.sendMessage(authMessages.getPrefix() + Colors.colorize(text));
    }

    private void runSync(Runnable task) {
        Bukkit.getScheduler().runTask(plugin, task);
    }
}
