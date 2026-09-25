package com.elyther.eauctions;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class FavoriteGUI implements Listener {

    private final EAuctions plugin;
    private final FavoriteManager manager;

    private final java.util.Map<UUID, Integer> selectedSlots =
            new java.util.HashMap<>();

    private final java.util.Map<UUID, ItemStack> pendingItems =
            new java.util.HashMap<>();

    public FavoriteGUI(
            EAuctions plugin,
            FavoriteManager manager
    ) {

        this.plugin = plugin;
        this.manager = manager;
    }

    // =========================================================
    // FAVORİLER
    // =========================================================

    public void openFavorites(Player player) {

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        54,
                        plugin.color(
                                "&8⭐ Favori Itemler"
                        )
                );

        ItemStack glass =
                createItem(
                        Material.GRAY_STAINED_GLASS_PANE,
                        "&7"
                );

        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, glass);
        }

        for (int i = 0; i < manager.getMaxFavorites(); i++) {

            ItemStack favorite =
                    manager.getFavorite(
                            player.getUniqueId(),
                            i
                    );

            if (favorite == null ||
                    favorite.getType().isAir()) {

                inventory.setItem(
                        i + 10,
                        createItem(
                                Material.GRAY_STAINED_GLASS_PANE,
                                "&7Boş Favori",
                                "",
                                "&7Bu slota bir item eklemek için",
                                "&7tıklayın."
                        )
                );

            } else {

                ItemStack display =
                        favorite.clone();

                ItemMeta meta =
                        display.getItemMeta();

                if (meta != null) {

                    List<String> lore =
                            meta.hasLore() &&
                                    meta.getLore() != null
                                    ? new ArrayList<>(
                                            meta.getLore()
                                    )
                                    : new ArrayList<>();

                    lore.add("");
                    lore.add(
                            plugin.color(
                                    "&a✔ Favori"
                            )
                    );

                    lore.add(
                            plugin.color(
                                    "&7AH'de bu itemi ara."
                            )
                    );

                    lore.add("");
                    lore.add(
                            plugin.color(
                                    "&cSağ Tık: Favoriyi Sil"
                            )
                    );

                    meta.setLore(lore);
                    display.setItemMeta(meta);
                }

                inventory.setItem(
                        i + 10,
                        display
                );
            }
        }

        inventory.setItem(
                49,
                createItem(
                        Material.ARROW,
                        "&cGeri",
                        "",
                        "&7Auction House'a dön."
                )
        );

        player.openInventory(inventory);
    }

    // =========================================================
    // ITEM SEÇİMİ
    // =========================================================

    private void openItemSelection(
            Player player,
            int favoriteSlot
    ) {

        selectedSlots.put(
                player.getUniqueId(),
                favoriteSlot
        );

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        54,
                        plugin.color(
                                "&8⭐ Favori Item Seç"
                        )
                );

        ItemStack glass =
                createItem(
                        Material.GRAY_STAINED_GLASS_PANE,
                        "&7"
                );

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, glass);
        }

        List<Material> materials =
                new ArrayList<>();

        for (Material material :
                Material.values()) {

            if (!material.isItem()) {
                continue;
            }

            if (material == Material.AIR) {
                continue;
            }

            materials.add(material);
        }

        materials.sort(
                (a, b) ->
                        a.name().compareToIgnoreCase(
                                b.name()
                        )
        );

        int slot = 0;

        for (Material material :
                materials) {

            if (slot >= 45) {
                break;
            }

            ItemStack item =
                    new ItemStack(
                            material
                    );

            ItemMeta meta =
                    item.getItemMeta();

            if (meta != null) {

                meta.setDisplayName(
                        plugin.color(
                                "&f" +
                                        formatMaterial(
                                                material
                                        )
                        )
                );

                List<String> lore =
                        new ArrayList<>();

                lore.add("");
                lore.add(
                        plugin.color(
                                "&aTıklayarak seç."
                        )
                );

                meta.setLore(lore);

                item.setItemMeta(meta);
            }

            inventory.setItem(
                    slot,
                    item
            );

            slot++;
        }

        inventory.setItem(
                49,
                createItem(
                        Material.ARROW,
                        "&cGeri",
                        "",
                        "&7Favorilere dön."
                )
        );

        player.openInventory(inventory);
    }

    // =========================================================
    // ENCHANT SEÇİMİ
    // =========================================================

    private void openEnchantSelection(
            Player player,
            ItemStack item
    ) {

        pendingItems.put(
                player.getUniqueId(),
                item.clone()
        );

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        54,
                        plugin.color(
                                "&8⭐ Büyü Seç"
                        )
                );

        ItemStack glass =
                createItem(
                        Material.GRAY_STAINED_GLASS_PANE,
                        "&7"
                );

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, glass);
        }

        int slot = 0;

        for (Enchantment enchantment :
                Enchantment.values()) {

            if (!enchantment.canEnchantItem(item)) {
                continue;
            }

            if (slot >= 45) {
                break;
            }

            int maxLevel =
                    enchantment.getMaxLevel();

            ItemStack display =
                    createItem(
                            Material.ENCHANTED_BOOK,
                            "&d" +
                                    formatEnchantment(
                                            enchantment
                                    ),
                            "",
                            "&7Maksimum seviye: &f" +
                                    maxLevel,
                            "",
                            "&aTıklayarak büyüyü seç."
                    );

            inventory.setItem(
                    slot,
                    display
            );

            slot++;
        }

        // Büyüsüz kaydet
        inventory.setItem(
                48,
                createItem(
                        Material.PAPER,
                        "&aBüyüsüz Kaydet",
                        "",
                        "&7Itemi büyü olmadan favorilere ekle."
                )
        );

        inventory.setItem(
                49,
                createItem(
                        Material.ARROW,
                        "&cGeri",
                        "",
                        "&7Item seçimine dön."
                )
        );

        player.openInventory(inventory);
    }

    // =========================================================
    // ENCHANT SEVİYE MENÜSÜ
    // =========================================================

    private void openEnchantLevelSelection(
            Player player,
            Enchantment enchantment
    ) {

        ItemStack item =
                pendingItems.get(
                        player.getUniqueId()
                );

        if (item == null) {
            openFavorites(player);
            return;
        }

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        27,
                        plugin.color(
                                "&8⭐ Büyü Seviyesi"
                        )
                );

        ItemStack glass =
                createItem(
                        Material.GRAY_STAINED_GLASS_PANE,
                        "&7"
                );

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, glass);
        }

        int maxLevel =
                enchantment.getMaxLevel();

        if (maxLevel > 7) {
            maxLevel = 7;
        }

        for (int level = 1;
             level <= maxLevel;
             level++) {

            int slot =
                    9 + (level - 1);

            inventory.setItem(
                    slot,
                    createItem(
                            Material.ENCHANTED_BOOK,
                            "&d" +
                                    formatEnchantment(
                                            enchantment
                                    ) +
                                    " " +
                                    roman(level),
                            "",
                            "&7Seviye: &f" +
                                    level,
                            "",
                            "&aBu seviyeyi seç."
                    )
            );
        }

        inventory.setItem(
                22,
                createItem(
                        Material.ARROW,
                        "&cGeri",
                        "",
                        "&7Büyülere dön."
                )
        );

        player.openInventory(inventory);
    }

    // =========================================================
    // CLICK
    // =========================================================

    @EventHandler
    public void onClick(
            InventoryClickEvent event
    ) {

        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }

        Player player =
                (Player) event.getWhoClicked();

        String title =
                event.getView().getTitle();

        int slot =
                event.getRawSlot();

        if (slot < 0 ||
                slot >= event.getView()
                        .getTopInventory()
                        .getSize()) {
            return;
        }

        // =====================================================
        // FAVORİLER
        // =====================================================

        if (title.equals(
                plugin.color(
                        "&8⭐ Favori Itemler"
                )
        )) {

            event.setCancelled(true);

            if (slot == 49) {

                plugin.getAuctionGUI()
                        .open(player);

                return;
            }

            if (slot >= 10 &&
                    slot <= 18) {

                int favoriteSlot =
                        slot - 10;

                ItemStack favorite =
                        manager.getFavorite(
                                player.getUniqueId(),
                                favoriteSlot
                        );

                if (event.isRightClick()) {

                    if (favorite != null) {

                        manager.removeFavorite(
                                player.getUniqueId(),
                                favoriteSlot
                        );

                        player.sendMessage(
                                plugin.color(
                                        "&d&lEAuctions &8» " +
                                                "&cFavori silindi."
                                )
                        );

                        player.playSound(
                                player.getLocation(),
                                Sound.BLOCK_NOTE_BLOCK_BASS,
                                1f,
                                0.7f
                        );

                        openFavorites(player);
                    }

                    return;
                }

                if (favorite == null ||
                        favorite.getType().isAir()) {

                    openItemSelection(
                            player,
                            favoriteSlot
                    );

                } else {

                    player.closeInventory();

                    String search =
                            favorite.getType()
                                    .name();

                    plugin.getAuctionGUI()
                            .openSearch(
                                    player,
                                    search
                            );

                    player.playSound(
                            player.getLocation(),
                            Sound.UI_BUTTON_CLICK,
                            1f,
                            1f
                    );
                }

                return;
            }

            return;
        }

        // =====================================================
        // ITEM SEÇİMİ
        // =====================================================

        if (title.equals(
                plugin.color(
                        "&8⭐ Favori Item Seç"
                )
        )) {

            event.setCancelled(true);

            if (slot == 49) {

                openFavorites(player);
                return;
            }

            if (slot < 0 ||
                    slot >= 45) {
                return;
            }

            ItemStack clicked =
                    event.getCurrentItem();

            if (clicked == null ||
                    clicked.getType().isAir()) {
                return;
            }

            int favoriteSlot =
                    selectedSlots.getOrDefault(
                            player.getUniqueId(),
                            -1
                    );

            if (favoriteSlot < 0) {
                openFavorites(player);
                return;
            }

            ItemStack selected =
                    clicked.clone();

            selected.setAmount(1);

            boolean enchantable = false;

            for (Enchantment enchantment :
                    Enchantment.values()) {

                if (enchantment.canEnchantItem(
                        selected
                )) {

                    enchantable = true;
                    break;
                }
            }

            if (enchantable) {

                openEnchantSelection(
                        player,
                        selected
                );

            } else {

                saveFavorite(
                        player,
                        favoriteSlot,
                        selected
                );
            }

            return;
        }

        // =====================================================
        // BÜYÜLER
        // =====================================================

        if (title.equals(
                plugin.color(
                        "&8⭐ Büyü Seç"
                )
        )) {

            event.setCancelled(true);

            if (slot == 48) {

                ItemStack item =
                        pendingItems.get(
                                player.getUniqueId()
                        );

                if (item != null) {

                    int favoriteSlot =
                            selectedSlots.getOrDefault(
                                    player.getUniqueId(),
                                    -1
                            );

                    saveFavorite(
                            player,
                            favoriteSlot,
                            item
                    );
                }

                return;
            }

            if (slot == 49) {

                int favoriteSlot =
                        selectedSlots.getOrDefault(
                                player.getUniqueId(),
                                -1
                        );

                if (favoriteSlot >= 0) {

                    openItemSelection(
                            player,
                            favoriteSlot
                    );

                } else {

                    openFavorites(player);
                }

                return;
            }

            if (slot < 0 ||
                    slot >= 45) {
                return;
            }

            ItemStack clicked =
                    event.getCurrentItem();

            if (clicked == null ||
                    clicked.getType().isAir()) {
                return;
            }

            String display =
                    clicked.getItemMeta()
                            .getDisplayName();

            Enchantment found = null;

            for (Enchantment enchantment :
                    Enchantment.values()) {

                String name =
                        plugin.color(
                                "&d" +
                                        formatEnchantment(
                                                enchantment
                                        )
                        );

                if (display.startsWith(name)) {

                    found = enchantment;
                    break;
                }
            }

            if (found != null) {

                openEnchantLevelSelection(
                        player,
                        found
                );
            }

            return;
        }

        // =====================================================
        // ENCHANT SEVİYESİ
        // =====================================================

        if (title.equals(
                plugin.color(
                        "&8⭐ Büyü Seviyesi"
                )
        )) {

            event.setCancelled(true);

            if (slot == 22) {

                ItemStack item =
                        pendingItems.get(
                                player.getUniqueId()
                        );

                if (item != null) {

                    openEnchantSelection(
                            player,
                            item
                    );

                } else {

                    openFavorites(player);
                }

                return;
            }

            if (slot < 9 ||
                    slot > 15) {
                return;
            }

            ItemStack item =
                    pendingItems.get(
                            player.getUniqueId()
                    );

            if (item == null) {
                openFavorites(player);
                return;
            }

            String name =
                    event.getCurrentItem() != null &&
                            event.getCurrentItem()
                                    .getItemMeta() != null
                            ? event.getCurrentItem()
                                    .getItemMeta()
                                    .getDisplayName()
                            : "";

            Enchantment enchantment = null;

            for (Enchantment e :
                    Enchantment.values()) {

                String enchantName =
                        plugin.color(
                                "&d" +
                                        formatEnchantment(
                                                e
                                        )
                        );

                if (name.startsWith(enchantName)) {

                    enchantment = e;
                    break;
                }
            }

            if (enchantment == null) {
                return;
            }

            int level =
                    slot - 8;

            item.addUnsafeEnchantment(
                    enchantment,
                    level
            );

            int favoriteSlot =
                    selectedSlots.getOrDefault(
                            player.getUniqueId(),
                            -1
                    );

            saveFavorite(
                    player,
                    favoriteSlot,
                    item
            );

            return;
        }
    }

    // =========================================================
    // SAVE
    // =========================================================

    private void saveFavorite(
            Player player,
            int slot,
            ItemStack item
    ) {

        if (slot < 0) {
            openFavorites(player);
            return;
        }

        manager.setFavorite(
                player.getUniqueId(),
                slot,
                item
        );

        pendingItems.remove(
                player.getUniqueId()
        );

        player.sendMessage(
                plugin.color(
                        "&d&lEAuctions &8» " +
                                "&aFavori kaydedildi!"
                )
        );

        player.playSound(
                player.getLocation(),
                Sound.ENTITY_PLAYER_LEVELUP,
                1f,
                1.2f
        );

        openFavorites(player);
    }

    // =========================================================
    // ITEM FORMAT
    // =========================================================

    private String formatMaterial(
            Material material
    ) {

        String[] parts =
                material.name()
                        .toLowerCase(
                                Locale.ROOT
                        )
                        .split("_");

        StringBuilder result =
                new StringBuilder();

        for (String part : parts) {

            if (part.isEmpty()) {
                continue;
            }

            if (result.length() > 0) {
                result.append(" ");
            }

            result.append(
                    Character.toUpperCase(
                            part.charAt(0)
                    )
            );

            if (part.length() > 1) {
                result.append(
                        part.substring(1)
                );
            }
        }

        return result.toString();
    }

    // =========================================================
    // ENCHANT FORMAT
    // =========================================================

    private String formatEnchantment(
            Enchantment enchantment
    ) {

        String key =
                enchantment.getKey()
                        .getKey();

        String[] parts =
                key.split("_");

        StringBuilder result =
                new StringBuilder();

        for (String part : parts) {

            if (result.length() > 0) {
                result.append(" ");
            }

            result.append(
                    Character.toUpperCase(
                            part.charAt(0)
                    )
            );

            if (part.length() > 1) {
                result.append(
                        part.substring(1)
                );
            }
        }

        return result.toString();
    }

    // =========================================================
    // ROMAN
    // =========================================================

    private String roman(int number) {

        String[] values = {
                "I",
                "II",
                "III",
                "IV",
                "V",
                "VI",
                "VII",
                "VIII",
                "IX",
                "X"
        };

        if (number >= 1 &&
                number <= values.length) {

            return values[number - 1];
        }

        return String.valueOf(number);
    }

    // =========================================================
    // CREATE ITEM
    // =========================================================

    private ItemStack createItem(
            Material material,
            String name,
            String... lore
    ) {

        ItemStack item =
                new ItemStack(material);

        ItemMeta meta =
                item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(
                    plugin.color(name)
            );

            List<String> lines =
                    new ArrayList<>();

            for (String line : lore) {

                lines.add(
                        plugin.color(line)
                );
            }

            meta.setLore(lines);

            item.setItemMeta(meta);
        }

        return item;
    }
}
