package com.elyther.eauctions;

import net.milkbowl.vault.economy.Economy;
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

import java.util.*;

public class FavoriteGUI implements Listener {

    private final EAuctions plugin;
    private final FavoriteManager favoriteManager;

    private final Map<UUID, Integer> itemPages = new HashMap<>();
    private final Map<UUID, Integer> selectedFavoriteSlots = new HashMap<>();
    private final Map<UUID, ItemStack> pendingItems = new HashMap<>();
    private final Map<UUID, Enchantment> pendingEnchantments = new HashMap<>();

    private static final int[] FAVORITE_SLOTS = {
            10, 11, 12, 13, 14, 15, 16, 17, 18
    };

    private static final int[] ITEM_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };

    public FavoriteGUI(
            EAuctions plugin,
            FavoriteManager favoriteManager
    ) {
        this.plugin = plugin;
        this.favoriteManager = favoriteManager;
    }

    // =========================================================
    // FAVORİ ANA MENÜ
    // =========================================================

    public void openFavorites(Player player) {

        Inventory inventory = Bukkit.createInventory(
                null,
                54,
                plugin.color("&8⭐ Favoriler")
        );

        fillBackground(inventory);

        for (int i = 0; i < FAVORITE_SLOTS.length; i++) {

            int favoriteSlot = i;

            ItemStack favorite =
                    favoriteManager.getFavorite(
                            player.getUniqueId(),
                            favoriteSlot
                    );

            if (favorite == null ||
                    favorite.getType().isAir()) {

                inventory.setItem(
                        FAVORITE_SLOTS[i],
                        createItem(
                                Material.GRAY_STAINED_GLASS_PANE,
                                "&7Boş Favori",
                                "",
                                "&7Favori eklemek için tıklayın."
                        )
                );

                continue;
            }

            ItemStack display = favorite.clone();
            display.setAmount(1);

            ItemMeta meta = display.getItemMeta();

            List<String> lore = new ArrayList<>();

            if (meta != null && meta.hasLore() && meta.getLore() != null) {
                lore.addAll(meta.getLore());
            }

            lore.add("");
            lore.add(plugin.color("&8──────────────"));

            Auction cheapest =
                    getCheapestAuction(
                            favorite
                    );

            if (cheapest == null) {

                lore.add(
                        plugin.color(
                                "&cŞu anda satışta yok."
                        )
                );

            } else {

                lore.add(
                        plugin.color(
                                "&aEn ucuz: &f$" +
                                        plugin.formatMoney(
                                                cheapest.getPrice()
                                        )
                        )
                );

                lore.add("");
                lore.add(
                        plugin.color(
                                "&aSol Tık &7→ Satın Al"
                        )
                );
            }

            lore.add(
                    plugin.color(
                            "&cSağ Tık &7→ Favoriden Sil"
                    )
            );

            if (meta != null) {
                meta.setLore(lore);
                display.setItemMeta(meta);
            }

            inventory.setItem(
                    FAVORITE_SLOTS[i],
                    display
            );
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

        // 1 saniyede bir sessiz refresh
        startPriceUpdater(player);
    }

    // =========================================================
    // FİYAT REFRESH
    // =========================================================

    private void startPriceUpdater(Player player) {

        UUID uuid = player.getUniqueId();

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> {

                    if (!player.isOnline()) {
                        return;
                    }

                    if (!player.getOpenInventory()
                            .getTitle()
                            .equals(
                                    plugin.color("&8⭐ Favoriler")
                            )) {

                        return;
                    }

                    updateFavoritePrices(player);

                    startPriceUpdater(player);

                },
                20L
        );
    }

    private void updateFavoritePrices(Player player) {

        Inventory inventory =
                player.getOpenInventory()
                        .getTopInventory();

        for (int i = 0; i < FAVORITE_SLOTS.length; i++) {

            ItemStack favorite =
                    favoriteManager.getFavorite(
                            player.getUniqueId(),
                            i
                    );

            if (favorite == null ||
                    favorite.getType().isAir()) {
                continue;
            }

            ItemStack display = favorite.clone();
            display.setAmount(1);

            Auction cheapest =
                    getCheapestAuction(
                            favorite
                    );

            ItemMeta meta =
                    display.getItemMeta();

            if (meta == null) {
                continue;
            }

            List<String> lore = new ArrayList<>();

            if (meta.hasLore() &&
                    meta.getLore() != null) {

                for (String line :
                        meta.getLore()) {

                    if (line.contains("En ucuz:") ||
                            line.contains("Şu anda satışta") ||
                            line.contains("Sol Tık") ||
                            line.contains("Sağ Tık") ||
                            line.contains("────────")) {

                        continue;
                    }

                    lore.add(line);
                }
            }

            lore.add("");
            lore.add(
                    plugin.color(
                            "&8──────────────"
                    )
            );

            if (cheapest == null) {

                lore.add(
                        plugin.color(
                                "&cŞu anda satışta yok."
                        )
                );

            } else {

                lore.add(
                        plugin.color(
                                "&aEn ucuz: &f$" +
                                        plugin.formatMoney(
                                                cheapest.getPrice()
                                        )
                        )
                );

                lore.add("");
                lore.add(
                        plugin.color(
                                "&aSol Tık &7→ Satın Al"
                        )
                );
            }

            lore.add(
                    plugin.color(
                            "&cSağ Tık &7→ Favoriden Sil"
                    )
            );

            meta.setLore(lore);
            display.setItemMeta(meta);

            inventory.setItem(
                    FAVORITE_SLOTS[i],
                    display
            );
        }
    }

    // =========================================================
    // EN UCUZ AUKSİYON
    // =========================================================

    private Auction getCheapestAuction(
            ItemStack favorite
    ) {

        Auction cheapest = null;

        for (Auction auction :
                plugin.getAuctionManager().getAuctions()) {

            if (!matchesFavorite(
                    auction.getItem(),
                    favorite
            )) {
                continue;
            }

            if (cheapest == null ||
                    auction.getPrice()
                            < cheapest.getPrice()) {

                cheapest = auction;
            }
        }

        return cheapest;
    }

    // =========================================================
    // FAVORİ EŞLEŞTİRME
    // =========================================================

    private boolean matchesFavorite(
            ItemStack auctionItem,
            ItemStack favorite
    ) {

        if (auctionItem == null ||
                favorite == null) {
            return false;
        }

        if (auctionItem.getType()
                != favorite.getType()) {

            return false;
        }

        if (!favorite.hasItemMeta()) {
            return true;
        }

        if (!favorite.getEnchantments().isEmpty()) {

            for (Map.Entry<Enchantment, Integer> entry :
                    favorite.getEnchantments().entrySet()) {

                int auctionLevel =
                        auctionItem.getEnchantmentLevel(
                                entry.getKey()
                        );

                if (auctionLevel <
                        entry.getValue()) {

                    return false;
                }
            }
        }

        return true;
    }

    // =========================================================
    // ITEM SEÇİMİ
    // =========================================================

    private void openItemSelection(
            Player player,
            int favoriteSlot
    ) {

        selectedFavoriteSlots.put(
                player.getUniqueId(),
                favoriteSlot
        );

        itemPages.putIfAbsent(
                player.getUniqueId(),
                0
        );

        openItemPage(player);
    }

    private void openItemPage(Player player) {

        int page =
                itemPages.getOrDefault(
                        player.getUniqueId(),
                        0
                );

        List<Material> materials =
                getAllItems();

        int maxPages =
                (int) Math.ceil(
                        materials.size() / 45.0
                );

        if (page < 0) {
            page = 0;
        }

        if (page >= maxPages) {
            page = maxPages - 1;
        }

        itemPages.put(
                player.getUniqueId(),
                page
        );

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        54,
                        plugin.color(
                                "&8⭐ Item Seç &7(" +
                                        (page + 1) +
                                        "/" +
                                        maxPages +
                                        ")"
                        )
                );

        fillBottom(inventory);

        int start =
                page * 45;

        for (int i = 0; i < 45; i++) {

            int index = start + i;

            if (index >= materials.size()) {
                break;
            }

            Material material =
                    materials.get(index);

            ItemStack item =
                    new ItemStack(material);

            ItemMeta meta =
                    item.getItemMeta();

            if (meta != null) {

                meta.setDisplayName(
                        plugin.color(
                                "&f" +
                                        formatMaterial(material)
                        )
                );

                List<String> lore =
                        new ArrayList<>();

                lore.add("");

                if (hasEnchantments(material)) {

                    lore.add(
                            plugin.color(
                                    "&d✨ Büyü seçilebilir"
                            )
                    );

                }

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
                    ITEM_SLOTS[i],
                    item
            );
        }

        // Geri
        inventory.setItem(
                45,
                createItem(
                        Material.ARROW,
                        "&c← Geri",
                        "",
                        "&7Favorilere dön."
                )
        );

        // Önceki
        if (page > 0) {

            inventory.setItem(
                    48,
                    createItem(
                            Material.ARROW,
                            "&e← Önceki",
                            "",
                            "&7Önceki sayfa."
                    )
            );
        }

        inventory.setItem(
                49,
                createItem(
                        Material.BARRIER,
                        "&cKapat"
                )
        );

        // Sonraki
        if (page + 1 < maxPages) {

            inventory.setItem(
                    50,
                    createItem(
                            Material.ARROW,
                            "&eNövbəti →",
                            "",
                            "&7Sonraki sayfa."
                    )
            );
        }

        player.openInventory(inventory);
    }

    // =========================================================
    // BÜYÜ MENÜSÜ
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
                                "&8⭐ Büyüler"
                        )
                );

        fillBottom(inventory);

        int slot = 0;

        for (Enchantment enchantment :
                Enchantment.values()) {

            if (!enchantment.canEnchantItem(item)) {
                continue;
            }

            if (slot >= 45) {
                break;
            }

            int current =
                    item.getEnchantmentLevel(
                            enchantment
                    );

            String level =
                    current > 0
                            ? "&aSeviye " + current
                            : "&7Seçilmedi";

            inventory.setItem(
                    slot,
                    createItem(
                            Material.ENCHANTED_BOOK,
                            "&d" +
                                    formatEnchantment(
                                            enchantment
                                    ),
                            "",
                            level,
                            "",
                            "&eSol Tık &7→ Seviye seç",
                            "&cSağ Tık &7→ Büyüyü kaldır"
                    )
            );

            slot++;
        }

        inventory.setItem(
                45,
                createItem(
                        Material.ARROW,
                        "&c← Geri"
                )
        );

        inventory.setItem(
                49,
                createItem(
                        Material.EMERALD,
                        "&a✔ Kaydet",
                        "",
                        "&7Favoriyi kaydet."
                )
        );

        player.openInventory(inventory);
    }

    // =========================================================
    // BÜYÜ SEVİYESİ
    // =========================================================

    private void openEnchantLevels(
            Player player,
            Enchantment enchantment
    ) {

        pendingEnchantments.put(
                player.getUniqueId(),
                enchantment
        );

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

        fillBackground(inventory);

        int max =
                Math.min(
                        enchantment.getMaxLevel(),
                        10
                );

        for (int level = 1;
             level <= max;
             level++) {

            inventory.setItem(
                    9 + level - 1,
                    createItem(
                            Material.ENCHANTED_BOOK,
                            "&d" +
                                    formatEnchantment(
                                            enchantment
                                    ) +
                                    " " +
                                    roman(level),
                            "",
                            "&aSeviye " +
                                    level +
                                    " seç."
                    )
            );
        }

        inventory.setItem(
                22,
                createItem(
                        Material.ARROW,
                        "&c← Geri"
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

        if (!(event.getWhoClicked()
                instanceof Player)) {
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
                plugin.color("&8⭐ Favoriler")
        )) {

            event.setCancelled(true);

            if (slot == 49) {

                plugin.getAuctionGUI()
                        .open(player);

                return;
            }

            int favoriteIndex = -1;

            for (int i = 0;
                 i < FAVORITE_SLOTS.length;
                 i++) {

                if (FAVORITE_SLOTS[i] == slot) {

                    favoriteIndex = i;
                    break;
                }
            }

            if (favoriteIndex == -1) {
                return;
            }

            ItemStack favorite =
                    favoriteManager.getFavorite(
                            player.getUniqueId(),
                            favoriteIndex
                    );

            // BOŞ FAVORİ
            if (favorite == null ||
                    favorite.getType().isAir()) {

                openItemSelection(
                        player,
                        favoriteIndex
                );

                return;
            }

            // SAĞ KLİK = SİL
            if (event.isRightClick()) {

                favoriteManager.removeFavorite(
                        player.getUniqueId(),
                        favoriteIndex
                );

                player.playSound(
                        player.getLocation(),
                        Sound.BLOCK_NOTE_BLOCK_BASS,
                        1f,
                        0.7f
                );

                player.sendMessage(
                        plugin.color(
                                "&d&lEAuctions &8» " +
                                        "&cFavori silindi."
                        )
                );

                openFavorites(player);

                return;
            }

            // SOL KLİK = EN UCUZU AL
            if (event.isLeftClick()) {

                buyCheapest(
                        player,
                        favorite
                );

                return;
            }

            return;
        }

        // =====================================================
        // ITEM SEÇİMİ
        // =====================================================

        if (title.startsWith(
                plugin.color("&8⭐ Item Seç")
        )) {

            event.setCancelled(true);

            if (slot == 45) {

                openFavorites(player);
                return;
            }

            if (slot == 48) {

                int page =
                        itemPages.getOrDefault(
                                player.getUniqueId(),
                                0
                        );

                if (page > 0) {

                    itemPages.put(
                            player.getUniqueId(),
                            page - 1
                    );

                    openItemPage(player);
                }

                return;
            }

            if (slot == 49) {

                player.closeInventory();
                return;
            }

            if (slot == 50) {

                int page =
                        itemPages.getOrDefault(
                                player.getUniqueId(),
                                0
                        );

                int maxPages =
                        (int) Math.ceil(
                                getAllItems().size()
                                        / 45.0
                        );

                if (page + 1 < maxPages) {

                    itemPages.put(
                            player.getUniqueId(),
                            page + 1
                    );

                    openItemPage(player);
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

            int favoriteSlot =
                    selectedFavoriteSlots.getOrDefault(
                            player.getUniqueId(),
                            -1
                    );

            if (favoriteSlot < 0) {
                return;
            }

            ItemStack selected =
                    new ItemStack(
                            clicked.getType()
                    );

            if (hasEnchantments(
                    selected.getType()
            )) {

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
                plugin.color("&8⭐ Büyüler")
        )) {

            event.setCancelled(true);

            if (slot == 45) {

                int favoriteSlot =
                        selectedFavoriteSlots.getOrDefault(
                                player.getUniqueId(),
                                -1
                        );

                if (favoriteSlot >= 0) {
                    openItemSelection(
                            player,
                            favoriteSlot
                    );
                }

                return;
            }

            if (slot == 49) {

                ItemStack item =
                        pendingItems.get(
                                player.getUniqueId()
                        );

                int favoriteSlot =
                        selectedFavoriteSlots.getOrDefault(
                                player.getUniqueId(),
                                -1
                        );

                if (item != null &&
                        favoriteSlot >= 0) {

                    saveFavorite(
                            player,
                            favoriteSlot,
                            item
                    );
                }

                return;
            }

            if (slot < 45) {

                ItemStack clicked =
                        event.getCurrentItem();

                if (clicked == null ||
                        clicked.getItemMeta() == null) {
                    return;
                }

                String name =
                        clicked.getItemMeta()
                                .getDisplayName();

                for (Enchantment enchantment :
                        Enchantment.values()) {

                    String enchantName =
                            plugin.color(
                                    "&d" +
                                            formatEnchantment(
                                                    enchantment
                                            )
                            );

                    if (name.startsWith(
                            enchantName
                    )) {

                        if (event.isRightClick()) {

                            ItemStack item =
                                    pendingItems.get(
                                            player.getUniqueId()
                                    );

                            if (item != null) {

                                item.removeEnchantment(
                                        enchantment
                                );

                                openEnchantSelection(
                                        player,
                                        item
                                );
                            }

                        } else {

                            openEnchantLevels(
                                    player,
                                    enchantment
                            );
                        }

                        return;
                    }
                }
            }

            return;
        }

        // =====================================================
        // BÜYÜ SEVİYESİ
        // =====================================================

        if (title.equals(
                plugin.color("&8⭐ Büyü Seviyesi")
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
                }

                return;
            }

            if (slot < 9 ||
                    slot > 18) {
                return;
            }

            Enchantment enchantment =
                    pendingEnchantments.get(
                            player.getUniqueId()
                    );

            ItemStack item =
                    pendingItems.get(
                            player.getUniqueId()
                    );

            if (enchantment == null ||
                    item == null) {
                return;
            }

            int level =
                    slot - 8;

            if (level < 1 ||
                    level > enchantment.getMaxLevel()) {
                return;
            }

            item.removeEnchantment(
                    enchantment
            );

            item.addUnsafeEnchantment(
                    enchantment,
                    level
            );

            openEnchantSelection(
                    player,
                    item
            );

            return;
        }
    }

    // =========================================================
    // EN UCUZUNU SATIN AL
    // =========================================================

    private void buyCheapest(
            Player player,
            ItemStack favorite
    ) {

        Auction auction =
                getCheapestAuction(
                        favorite
                );

        if (auction == null) {

            player.sendMessage(
                    plugin.color(
                            "&d&lEAuctions &8» " +
                                    "&cBu item artıq satışda deyil."
                    )
            );

            return;
        }

        Economy economy =
                plugin.getEconomy();

        if (economy == null) {
            return;
        }

        double price =
                auction.getPrice();

        if (!economy.has(
                player,
                price
        )) {

            player.sendMessage(
                    plugin.color(
                            "&d&lEAuctions &8» " +
                                    "&cKifayət qədər pulunuz yoxdur."
                    )
            );

            return;
        }

        if (!hasInventorySpace(
                player,
                auction.getItem()
        )) {

            player.sendMessage(
                    plugin.color(
                            "&d&lEAuctions &8» " +
                                    "&cEnvanterinizdə yer yoxdur."
                    )
            );

            return;
        }

        boolean withdrawn =
                economy.withdrawPlayer(
                        player,
                        price
                ).transactionSuccess();

        if (!withdrawn) {
            return;
        }

        economy.depositPlayer(
                Bukkit.getOfflinePlayer(
                        auction.getSeller()
                ),
                price
        );

        player.getInventory().addItem(
                auction.getItem().clone()
        );

        plugin.getAuctionManager()
                .removeAuction(
                        auction.getId()
                );

        player.playSound(
                player.getLocation(),
                Sound.ENTITY_PLAYER_LEVELUP,
                1f,
                1.2f
        );

        player.sendMessage(
                plugin.color(
                        "&d&lEAuctions &8» " +
                                "&aItem satın alındı! " +
                                "&7($"
                                + plugin.formatMoney(price)
                                + ")"
                )
        );

        openFavorites(player);
    }

    // =========================================================
    // INVENTORY SPACE
    // =========================================================

    private boolean hasInventorySpace(
            Player player,
            ItemStack item
    ) {

        int amount =
                item.getAmount();

        for (ItemStack content :
                player.getInventory()
                        .getStorageContents()) {

            if (content == null ||
                    content.getType().isAir()) {

                return true;
            }

            if (content.isSimilar(item)) {

                int space =
                        content.getMaxStackSize()
                                - content.getAmount();

                if (space >= amount) {
                    return true;
                }

                amount -= space;

                if (amount <= 0) {
                    return true;
                }
            }
        }

        return false;
    }

    // =========================================================
    // BÜTÜN ITEMLƏR
    // =========================================================

    private List<Material> getAllItems() {

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
                Comparator.comparing(
                        this::formatMaterial
                )
        );

        return materials;
    }

    // =========================================================
    // ENCHANT OLUNA BİLƏR?
    // =========================================================

    private boolean hasEnchantments(
            Material material
    ) {

        ItemStack item =
                new ItemStack(material);

        for (Enchantment enchantment :
                Enchantment.values()) {

            if (enchantment.canEnchantItem(item)) {
                return true;
            }
        }

        return false;
    }

    // =========================================================
    // SAVE FAVORITE
    // =========================================================

    private void saveFavorite(
            Player player,
            int slot,
            ItemStack item
    ) {

        if (slot < 0) {
            return;
        }

        favoriteManager.setFavorite(
                player.getUniqueId(),
                slot,
                item
        );

        pendingItems.remove(
                player.getUniqueId()
        );

        pendingEnchantments.remove(
                player.getUniqueId()
        );

        player.playSound(
                player.getLocation(),
                Sound.UI_BUTTON_CLICK,
                1f,
                1.2f
        );

        player.sendMessage(
                plugin.color(
                        "&d&lEAuctions &8» " +
                                "&aFavori kaydedildi!"
                )
        );

        openFavorites(player);
    }

    // =========================================================
    // BACKGROUND
    // =========================================================

    private void fillBackground(
            Inventory inventory
    ) {

        ItemStack glass =
                createItem(
                        Material.GRAY_STAINED_GLASS_PANE,
                        "&7"
                );

        for (int i = 0;
             i < inventory.getSize();
             i++) {

            inventory.setItem(
                    i,
                    glass
            );
        }
    }

    private void fillBottom(
            Inventory inventory
    ) {

        ItemStack glass =
                createItem(
                        Material.GRAY_STAINED_GLASS_PANE,
                        "&7"
                );

        for (int i = 45; i < 54; i++) {

            inventory.setItem(
                    i,
                    glass
            );
        }
    }

    // =========================================================
    // ITEM CREATE
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

    // =========================================================
    // MATERIAL NAME
    // =========================================================

    private String formatMaterial(
            Material material
    ) {

        String[] parts =
                material.name()
                        .toLowerCase(Locale.ROOT)
                        .split("_");

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
    // ENCHANT NAME
    // =========================================================

    private String formatEnchantment(
            Enchantment enchantment
    ) {

        String[] parts =
                enchantment.getKey()
                        .getKey()
                        .split("_");

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
}
