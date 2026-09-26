package com.elyther.eauctions;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class FavoriteGUI implements Listener {

    private static final String FAVORITES_TITLE =
            color("&8⭐ Favoriler");

    private static final String ITEM_SELECT_TITLE =
            color("&8⭐ Favori Eşya Seç");

    private static final String ENCHANT_SELECT_TITLE =
            color("&8⭐ Büyü Seç");

    private static final String ENCHANT_LEVEL_TITLE =
            color("&8⭐ Büyü Seviyesi");

    /*
     * =========================================================
     * 45 FAVORİ SLOTU
     *
     * 0 - 44 = Favoriler
     * 45 - 53 = Alt kontrol bölümü
     * =========================================================
     */

    private static final int[] FAVORITE_SLOTS = {
            0, 1, 2, 3, 4, 5, 6, 7, 8,
            9, 10, 11, 12, 13, 14, 15, 16, 17,
            18, 19, 20, 21, 22, 23, 24, 25, 26,
            27, 28, 29, 30, 31, 32, 33, 34, 35,
            36, 37, 38, 39, 40, 41, 42, 43, 44
    };

    private static final int ITEMS_PER_PAGE = 45;

    private final EAuctions plugin;
    private final FavoriteManager favoriteManager;

    private final Map<UUID, Integer> itemPages =
            new HashMap<>();

    /*
     * Satın alma kilidi.
     *
     * Aynı oyuncu çok hızlı şekilde birden fazla
     * tıklarsa ikinci satın alma işlemi engellenir.
     */
    private final Set<UUID> purchaseLocks =
            new HashSet<>();

    private final Map<UUID, Integer> enchantPages =
            new HashMap<>();

    private final Map<UUID, Integer> selectedFavoriteSlots =
            new HashMap<>();

    private final Map<UUID, ItemStack> pendingItems =
            new HashMap<>();

    private final Map<UUID, Map<Enchantment, Integer>>
            pendingEnchantments =
            new HashMap<>();

    private final Map<UUID, BukkitTask> refreshTasks =
            new HashMap<>();

    private final Map<UUID, Enchantment>
            pendingSelectedEnchantment =
            new HashMap<>();

    private final DecimalFormat moneyFormat =
            new DecimalFormat(
                    "0.##",
                    DecimalFormatSymbols.getInstance(
                            Locale.US
                    )
            );

    /*
     * =========================================================
     * CONSTRUCTOR
     *
     * ÖNƏMLİ:
     * Burada registerEvents YOXDUR.
     *
     * EAuctions.java artıq FavoriteGUI-ni register edir.
     * =========================================================
     */

    public FavoriteGUI(
            EAuctions plugin,
            FavoriteManager favoriteManager
    ) {
        this.plugin = plugin;
        this.favoriteManager = favoriteManager;
    }

    /* =========================================================
       ANA FAVORİ MENÜSÜ
       ========================================================= */

    public void open(Player player) {

        if (player == null || !player.isOnline()) {
            return;
        }

        stopRefresh(player);

        openFavorites(player);

        BukkitTask task =
                Bukkit.getScheduler().runTaskTimer(
                        plugin,
                        () -> {

                            if (!player.isOnline()) {
                                stopRefresh(player);
                                return;
                            }

                            if (!isFavoritesInventory(
                                    player.getOpenInventory()
                            )) {
                                stopRefresh(player);
                                return;
                            }

                            updateFavoritePrices(player);
                        },
                        20L,
                        20L
                );

        refreshTasks.put(
                player.getUniqueId(),
                task
        );
    }

    public void openFavorites(Player player) {

        if (player == null || !player.isOnline()) {
            return;
        }

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        54,
                        FAVORITES_TITLE
                );

        fillBackground(inventory);

        List<ItemStack> favorites =
                favoriteManager.getFavorites(
                        player.getUniqueId()
                );

        /*
         * 0 - 44
         *
         * Hər slot öz favoritinə aiddir.
         */
        for (int i = 0;
             i < FAVORITE_SLOTS.length;
             i++) {

            int slot =
                    FAVORITE_SLOTS[i];

            ItemStack favorite = null;

            if (i < favorites.size()) {
                favorite = favorites.get(i);
            }

            if (favorite == null
                    || favorite.getType().isAir()) {

                inventory.setItem(
                        slot,
                        createEmptyFavorite(
                                i + 1
                        )
                );

            } else {

                inventory.setItem(
                        slot,
                        createFavoriteDisplay(
                                favorite,
                                i
                        )
                );
            }
        }

        /*
         * ALT KONTROL BÖLMƏSİ
         */

        inventory.setItem(
                45,
                item(
                        Material.ARROW,
                        "&e← Geri",
                        "&7Müzayede menüsüne dön."
                )
        );

        inventory.setItem(
                49,
                item(
                        Material.BARRIER,
                        "&cKapat",
                        "&7Menüyü kapat."
                )
        );

        inventory.setItem(
                53,
                item(
                        Material.NETHER_STAR,
                        "&bFavoriler",
                        "&7Favori eşyalarınız burada."
                )
        );

        player.openInventory(inventory);
    }

    /* =========================================================
       FAVORİ GÖRÜNÜMÜ
       ========================================================= */

    private ItemStack createFavoriteDisplay(
            ItemStack favorite,
            int favoriteIndex
    ) {

        ItemStack display =
                favorite.clone();

        ItemMeta meta =
                display.getItemMeta();

        if (meta == null) {
            return display;
        }

        List<String> lore =
                new ArrayList<>();

        lore.add("");

        lore.add(
                color(
                        "&7Favori: &f#"
                                + (favoriteIndex + 1)
                )
        );

        lore.add("");

        Auction cheapest =
                getCheapestAuction(favorite);

        if (cheapest == null) {

            lore.add(
                    color(
                            "&cŞu anda satışta yok."
                    )
            );

        } else {

            lore.add(
                    color(
                            "&aEn ucuz fiyat: &f$"
                                    + formatMoney(
                                    cheapest.getPrice()
                            )
                    )
            );

            lore.add("");

            lore.add(
                    color(
                            "&eSol tık &7→ En ucuzu satın al"
                    )
            );
        }

        lore.add(
                color(
                        "&cSağ tık &7→ Favoriden kaldır"
                )
        );

        meta.setLore(lore);

        display.setItemMeta(meta);

        return display;
    }

    private ItemStack createEmptyFavorite(
            int number
    ) {

        ItemStack item =
                new ItemStack(
                        Material.GRAY_STAINED_GLASS_PANE
                );

        ItemMeta meta =
                item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(
                    color(
                            "&7Favori #" + number
                    )
            );

            List<String> lore =
                    new ArrayList<>();

            lore.add("");

            lore.add(
                    color(
                            "&eTıkla &7→ Favori eşya seç"
                    )
            );

            lore.add("");

            lore.add(
                    color(
                            "&8Buraya istediğin eşyayı"
                    )
            );

            lore.add(
                    color(
                            "&8favori olarak ekleyebilirsin."
                    )
            );

            meta.setLore(lore);

            item.setItemMeta(meta);
        }

        return item;
    }

    /* =========================================================
       FİYAT GÜNCELLEME
       ========================================================= */

    private void updateFavoritePrices(
            Player player
    ) {

        if (!isFavoritesInventory(
                player.getOpenInventory()
        )) {
            return;
        }

        Inventory inventory =
                player.getOpenInventory()
                        .getTopInventory();

        List<ItemStack> favorites =
                favoriteManager.getFavorites(
                        player.getUniqueId()
                );

        for (int i = 0;
             i < FAVORITE_SLOTS.length;
             i++) {

            int slot =
                    FAVORITE_SLOTS[i];

            ItemStack favorite =
                    i < favorites.size()
                            ? favorites.get(i)
                            : null;

            if (favorite == null
                    || favorite.getType().isAir()) {

                inventory.setItem(
                        slot,
                        createEmptyFavorite(
                                i + 1
                        )
                );

                continue;
            }

            inventory.setItem(
                    slot,
                    createFavoriteDisplay(
                            favorite,
                            i
                    )
            );
        }
    }

    /* =========================================================
       EN UCUZ İLAN
       ========================================================= */

    private Auction getCheapestAuction(
            ItemStack favorite
    ) {

        if (favorite == null
                || favorite.getType().isAir()) {

            return null;
        }

        Auction cheapest = null;

        for (Auction auction :
                plugin.getAuctionManager()
                        .getAuctions()) {

            if (!matchesFavorite(
                    favorite,
                    auction.getItem()
            )) {
                continue;
            }

            if (cheapest == null
                    || auction.getPrice()
                    < cheapest.getPrice()) {

                cheapest = auction;
            }
        }

        return cheapest;
    }

    /* =========================================================
       FAVORİ EŞLEŞTİRME
       ========================================================= */

    private boolean matchesFavorite(
            ItemStack favorite,
            ItemStack auctionItem
    ) {

        if (favorite == null
                || auctionItem == null) {

            return false;
        }

        if (favorite.getType()
                != auctionItem.getType()) {

            return false;
        }

        if (favorite.getEnchantments().isEmpty()) {
            return true;
        }

        for (Map.Entry<Enchantment, Integer> entry :
                favorite.getEnchantments()
                        .entrySet()) {

            Enchantment required =
                    entry.getKey();

            int requiredLevel =
                    entry.getValue();

            int auctionLevel =
                    auctionItem.getEnchantmentLevel(
                            required
                    );

            if (auctionLevel < requiredLevel) {
                return false;
            }
        }

        return true;
    }

    /* =========================================================
       FAVORİ SATIN ALMA
       ========================================================= */

    private void buyCheapest(
            Player player,
            int favoriteSlot
    ) {

        UUID uuid =
                player.getUniqueId();

        if (purchaseLocks.contains(uuid)) {
            return;
        }

        purchaseLocks.add(uuid);

        try {

            ItemStack favorite =
                    favoriteManager.getFavorite(
                            uuid,
                            favoriteSlot
                    );

            if (favorite == null
                    || favorite.getType().isAir()) {

                return;
            }

            Auction cheapest =
                    getCheapestAuction(
                            favorite
                    );

            if (cheapest == null) {

                player.sendMessage(
                        color(
                                "&d&lEAuctions &8» &cŞu anda satışta yok."
                        )
                );

                player.playSound(
                        player.getLocation(),
                        Sound.ENTITY_VILLAGER_NO,
                        1.0f,
                        1.0f
                );

                return;
            }

            Economy economy =
                    plugin.getEconomy();

            if (economy == null) {

                player.sendMessage(
                        color(
                                "&d&lEAuctions &8» &cEkonomi sistemi bulunamadı."
                        )
                );

                return;
            }

            Auction current =
                    plugin.getAuctionManager()
                            .getAuction(
                                    cheapest.getId()
                            );

            if (current == null) {

                player.sendMessage(
                        color(
                                "&d&lEAuctions &8» &cBu ilan az önce satıldı."
                        )
                );

                return;
            }

            if (!matchesFavorite(
                    favorite,
                    current.getItem()
            )) {

                player.sendMessage(
                        color(
                                "&d&lEAuctions &8» &cBu ilan favorinize artık uygun değil."
                        )
                );

                return;
            }

            double price =
                    current.getPrice();

            if (economy.getBalance(player)
                    < price) {

                player.sendMessage(
                        color(
                                "&d&lEAuctions &8» &cYeterli paran yok."
                        )
                );

                player.playSound(
                        player.getLocation(),
                        Sound.ENTITY_VILLAGER_NO,
                        1.0f,
                        1.0f
                );

                return;
            }

            if (!hasInventorySpace(
                    player,
                    current.getItem()
            )) {

                player.sendMessage(
                        color(
                                "&d&lEAuctions &8» &cEnvanterinde yeterli alan yok."
                        )
                );

                player.playSound(
                        player.getLocation(),
                        Sound.ENTITY_VILLAGER_NO,
                        1.0f,
                        1.0f
                );

                return;
            }

            /*
             * =================================================
             * İLANI SİL
             * =================================================
             */

            boolean removed =
                    plugin.getAuctionManager()
                            .removeAuction(
                                    current.getId()
                            );

            if (!removed) {

                player.sendMessage(
                        color(
                                "&d&lEAuctions &8» &cBu ilan az önce satıldı."
                        )
                );

                return;
            }

            /*
             * =================================================
             * BUYER-DAN PULU ÇƏK
             * =================================================
             */

            var withdraw =
                    economy.withdrawPlayer(
                            player,
                            price
                    );

            if (!withdraw.transactionSuccess()) {

                /*
                 * İlanı geri qaytar.
                 */

                plugin.getAuctionManager()
                        .addAuction(
                                current.getSeller(),
                                current.getItem(),
                                current.getPrice()
                        );

                player.sendMessage(
                        color(
                                "&d&lEAuctions &8» &cÖdeme başarısız oldu."
                        )
                );

                return;
            }

            /*
             * =================================================
             * SATICIYA PUL
             * =================================================
             */

            var seller =
                    Bukkit.getOfflinePlayer(
                            current.getSeller()
                    );

            var deposit =
                    economy.depositPlayer(
                            seller,
                            price
                    );

            if (!deposit.transactionSuccess()) {

                /*
                 * Buyer refund
                 */

                economy.depositPlayer(
                        player,
                        price
                );

                /*
                 * Auction geri
                 */

                plugin.getAuctionManager()
                        .addAuction(
                                current.getSeller(),
                                current.getItem(),
                                current.getPrice()
                        );

                player.sendMessage(
                        color(
                                "&d&lEAuctions &8» &cSatıcıya ödeme yapılamadı."
                        )
                );

                return;
            }

            /*
             * =================================================
             * ITEM VER
             * =================================================
             */

            HashMap<Integer, ItemStack> leftover =
                    player.getInventory()
                            .addItem(
                                    current.getItem()
                                            .clone()
                            );

            if (!leftover.isEmpty()) {

                /*
                 * Buyer refund
                 */

                economy.depositPlayer(
                        player,
                        price
                );

                /*
                 * Seller refund
                 */

                economy.withdrawPlayer(
                        seller,
                        price
                );

                /*
                 * Auction geri
                 */

                plugin.getAuctionManager()
                        .addAuction(
                                current.getSeller(),
                                current.getItem(),
                                current.getPrice()
                        );

                player.sendMessage(
                        color(
                                "&d&lEAuctions &8» &cEşya envantera eklenemedi."
                        )
                );

                return;
            }

            /*
             * =================================================
             * BAŞARILI
             * =================================================
             */

            player.playSound(
                    player.getLocation(),
                    Sound.ENTITY_PLAYER_LEVELUP,
                    1.0f,
                    1.2f
            );

            player.sendMessage(
                    color(
                            "&d&lEAuctions &8» &a✔ Satın alındı!"
                    )
            );

            player.sendMessage(
                    color(
                            "&7Qiymət: &a$"
                                    + formatMoney(price)
                    )
            );

            Bukkit.getScheduler().runTask(
                    plugin,
                    () -> {

                        if (!player.isOnline()) {
                            return;
                        }

                        if (isFavoritesInventory(
                                player.getOpenInventory()
                        )) {

                            updateFavoritePrices(
                                    player
                            );
                        }
                    }
            );

        } finally {

            Bukkit.getScheduler().runTaskLater(
                    plugin,
                    () -> purchaseLocks.remove(uuid),
                    10L
            );
        }
    }

    /* =========================================================
       ENVANTER KONTROLÜ
       ========================================================= */

    private boolean hasInventorySpace(
            Player player,
            ItemStack item
    ) {

        if (item == null
                || item.getType().isAir()) {

            return false;
        }

        int amount =
                item.getAmount();

        for (ItemStack content :
                player.getInventory()
                        .getStorageContents()) {

            if (content == null
                    || content.getType().isAir()) {

                continue;
            }

            if (!content.isSimilar(item)) {
                continue;
            }

            int max =
                    Math.min(
                            content.getMaxStackSize(),
                            player.getInventory()
                                    .getMaxStackSize()
                    );

            int free =
                    max - content.getAmount();

            if (free > 0) {

                amount -= free;

                if (amount <= 0) {
                    return true;
                }
            }
        }

        for (ItemStack content :
                player.getInventory()
                        .getStorageContents()) {

            if (content == null
                    || content.getType().isAir()) {

                int max =
                        Math.min(
                                item.getMaxStackSize(),
                                player.getInventory()
                                        .getMaxStackSize()
                        );

                amount -= max;

                if (amount <= 0) {
                    return true;
                }
            }
        }

        return false;
    }

    /* =========================================================
       EŞYA SEÇİMİ
       ========================================================= */

    private void openItemSelection(
            Player player,
            int favoriteSlot
    ) {

        selectedFavoriteSlots.put(
                player.getUniqueId(),
                favoriteSlot
        );

        itemPages.put(
                player.getUniqueId(),
                0
        );

        openItemPage(
                player,
                0
        );
    }

    private void openItemPage(
            Player player,
            int page
    ) {

        List<Material> materials =
                getItemMaterials();

        int maxPage =
                Math.max(
                        0,
                        (materials.size() - 1)
                                / ITEMS_PER_PAGE
                );

        if (page < 0) {
            page = 0;
        }

        if (page > maxPage) {
            page = maxPage;
        }

        itemPages.put(
                player.getUniqueId(),
                page
        );

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        54,
                        ITEM_SELECT_TITLE
                );

        fillBackground(inventory);

        int start =
                page * ITEMS_PER_PAGE;

        int end =
                Math.min(
                        start + ITEMS_PER_PAGE,
                        materials.size()
                );

        for (int i = start;
             i < end;
             i++) {

            Material material =
                    materials.get(i);

            int slot =
                    i - start;

            ItemStack display =
                    new ItemStack(material);

            ItemMeta meta =
                    display.getItemMeta();

            if (meta != null) {

                List<String> lore =
                        new ArrayList<>();

                lore.add("");

                lore.add(
                        color(
                                "&eTıkla &7→ Bu eşyayı seç"
                        )
                );

                if (isEnchantable(display)) {

                    lore.add(
                            color(
                                    "&bBüyü eklenebilir."
                            )
                    );
                }

                meta.setLore(lore);

                display.setItemMeta(meta);
            }

            inventory.setItem(
                    slot,
                    display
            );
        }

        inventory.setItem(
                45,
                item(
                        Material.ARROW,
                        "&e← Geri",
                        "&7Favorilere dön."
                )
        );

        if (page > 0) {

            inventory.setItem(
                    48,
                    item(
                            Material.PAPER,
                            "&e← Önceki Sayfa",
                            "&7Önceki sayfaya geç."
                    )
            );
        }

        inventory.setItem(
                49,
                item(
                        Material.BARRIER,
                        "&cKapat",
                        "&7Menüyü kapat."
                )
        );

        inventory.setItem(
                50,
                item(
                        Material.BOOK,
                        "&fSayfa &e"
                                + (page + 1)
                                + " &7/ &e"
                                + (maxPage + 1),
                        "&7Toplam eşya: &f"
                                + materials.size()
                )
        );

        if (page < maxPage) {

            inventory.setItem(
                    53,
                    item(
                            Material.PAPER,
                            "&eSonraki Sayfa →",
                            "&7Sonraki sayfaya geç."
                    )
            );
        }

        player.openInventory(inventory);
    }

    private List<Material> getItemMaterials() {

        List<Material> materials =
                new ArrayList<>();

        for (Material material :
                Material.values()) {

            if (material.isItem()
                    && !material.isAir()
                    && !material.name()
                    .contains("LEGACY")) {

                materials.add(material);
            }
        }

        materials.sort(
                Comparator.comparing(
                        Material::name
                )
        );

        return materials;
    }

    /* =========================================================
       BÜYÜ SEÇİMİ
       ========================================================= */

    private void openEnchantSelection(
            Player player,
            ItemStack item
    ) {

        pendingItems.put(
                player.getUniqueId(),
                item.clone()
        );

        pendingEnchantments.put(
                player.getUniqueId(),
                new HashMap<>()
        );

        enchantPages.put(
                player.getUniqueId(),
                0
        );

        openEnchantPage(
                player,
                0
        );
    }

    private void openEnchantPage(
            Player player,
            int page
    ) {

        ItemStack item =
                pendingItems.get(
                        player.getUniqueId()
                );

        if (item == null) {

            openFavorites(player);

            return;
        }

        List<Enchantment> enchantments =
                getCompatibleEnchantments(item);

        int maxPage =
                Math.max(
                        0,
                        (enchantments.size() - 1)
                                / 45
                );

        if (page < 0) {
            page = 0;
        }

        if (page > maxPage) {
            page = maxPage;
        }

        enchantPages.put(
                player.getUniqueId(),
                page
        );

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        54,
                        ENCHANT_SELECT_TITLE
                );

        fillBackground(inventory);

        int start =
                page * 45;

        int end =
                Math.min(
                        start + 45,
                        enchantments.size()
                );

        Map<Enchantment, Integer> selected =
                pendingEnchantments.computeIfAbsent(
                        player.getUniqueId(),
                        ignored -> new HashMap<>()
                );

        for (int i = start;
             i < end;
             i++) {

            Enchantment enchantment =
                    enchantments.get(i);

            int slot =
                    i - start;

            ItemStack display =
                    new ItemStack(
                            Material.ENCHANTED_BOOK
                    );

            ItemMeta meta =
                    display.getItemMeta();

            if (meta != null) {

                String name =
                        getEnchantmentName(
                                enchantment
                        );

                meta.setDisplayName(
                        color(
                                "&d" + name
                        )
                );

                List<String> lore =
                        new ArrayList<>();

                lore.add("");

                Integer selectedLevel =
                        selected.get(enchantment);

                if (selectedLevel != null) {

                    lore.add(
                            color(
                                    "&aSeçili seviye: &f"
                                            + selectedLevel
                            )
                    );

                    lore.add(
                            color(
                                    "&eSol tık &7→ Seviyeyi değiştir"
                            )
                    );

                    lore.add(
                            color(
                                    "&cSağ tık &7→ Büyüyü kaldır"
                            )
                    );

                } else {

                    lore.add(
                            color(
                                    "&eTıkla &7→ Seviye seç"
                            )
                    );
                }

                meta.setLore(lore);

                display.setItemMeta(meta);
            }

            inventory.setItem(
                    slot,
                    display
            );
        }

        inventory.setItem(
                45,
                item(
                        Material.ARROW,
                        "&e← Geri",
                        "&7Eşya seçimine dön."
                )
        );

        if (page > 0) {

            inventory.setItem(
                    47,
                    item(
                            Material.PAPER,
                            "&e← Önceki",
                            "&7Önceki sayfa."
                    )
            );
        }

        inventory.setItem(
                49,
                item(
                        Material.LIME_DYE,
                        "&aFavoriyi Kaydet",
                        "&7Seçtiğin eşyayı favoriye ekle."
                )
        );

        if (page < maxPage) {

            inventory.setItem(
                    53,
                    item(
                            Material.PAPER,
                            "&eSonraki →",
                            "&7Sonraki sayfa."
                    )
            );
        }

        player.openInventory(inventory);
    }

    private List<Enchantment> getCompatibleEnchantments(
            ItemStack item
    ) {

        List<Enchantment> result =
                new ArrayList<>();

        if (item == null) {
            return result;
        }

        for (Enchantment enchantment :
                Enchantment.values()) {

            try {

                if (enchantment.canEnchantItem(item)) {
                    result.add(enchantment);
                }

            } catch (Exception ignored) {
            }
        }

        result.sort(
                Comparator.comparing(
                        this::getEnchantmentName
                )
        );

        return result;
    }

    private String getEnchantmentName(
            Enchantment enchantment
    ) {

        String key =
                enchantment.getKey().getKey();

        String[] parts =
                key.split("_");

        StringBuilder builder =
                new StringBuilder();

        for (String part : parts) {

            if (part.isEmpty()) {
                continue;
            }

            builder.append(
                    Character.toUpperCase(
                            part.charAt(0)
                    )
            );

            if (part.length() > 1) {

                builder.append(
                        part.substring(1)
                                .toLowerCase(
                                        Locale.ROOT
                                )
                );
            }

            builder.append(" ");
        }

        return builder.toString().trim();
    }

    /* =========================================================
       BÜYÜ SEVİYESİ
       ========================================================= */

    private void openEnchantLevels(
            Player player,
            Enchantment enchantment
    ) {

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        27,
                        ENCHANT_LEVEL_TITLE
                );

        fillBackground(inventory);

        int maxLevel =
                enchantment.getMaxLevel();

        maxLevel =
                Math.max(
                        1,
                        Math.min(
                                maxLevel,
                                10
                        )
                );

        for (int level = 1;
             level <= maxLevel;
             level++) {

            int slot =
                    8 + level;

            if (slot >= 27) {
                break;
            }

            ItemStack display =
                    new ItemStack(
                            Material.ENCHANTED_BOOK
                    );

            ItemMeta meta =
                    display.getItemMeta();

            if (meta != null) {

                meta.setDisplayName(
                        color(
                                "&d"
                                        + getEnchantmentName(
                                        enchantment
                                )
                                        + " &f"
                                        + roman(level)
                        )
                );

                List<String> lore =
                        new ArrayList<>();

                lore.add("");

                lore.add(
                        color(
                                "&eTıkla &7→ Seviye "
                                        + level
                                        + " seç"
                        )
                );

                meta.setLore(lore);

                display.setItemMeta(meta);
            }

            inventory.setItem(
                    slot,
                    display
            );
        }

        /*
         * Level 10 slot 18 olduğu üçün
         * Geri düyməsi artıq 26-cı slottadır.
         */

        inventory.setItem(
                26,
                item(
                        Material.ARROW,
                        "&e← Geri",
                        "&7Büyü listesine dön."
                )
        );

        pendingSelectedEnchantment.put(
                player.getUniqueId(),
                enchantment
        );

        player.openInventory(inventory);
    }

    /* =========================================================
       CLICK
       ========================================================= */

    @EventHandler
    public void onInventoryClick(
            InventoryClickEvent event
    ) {

        if (!(event.getWhoClicked()
                instanceof Player player)) {

            return;
        }

        String title =
                event.getView().getTitle();

        int slot =
                event.getRawSlot();

        if (slot < 0) {
            return;
        }

        /* =====================================================
           FAVORİ ANA MENÜ
           ===================================================== */

        if (title.equals(FAVORITES_TITLE)) {

            event.setCancelled(true);

            if (slot == 45) {

                player.playSound(
                        player.getLocation(),
                        Sound.UI_BUTTON_CLICK,
                        1.0f,
                        1.0f
                );

                stopRefresh(player);

                plugin.getAuctionGUI()
                        .openGUI(
                                player,
                                null
                        );

                return;
            }

            if (slot == 49) {

                player.playSound(
                        player.getLocation(),
                        Sound.UI_BUTTON_CLICK,
                        1.0f,
                        1.0f
                );

                player.closeInventory();

                return;
            }

            for (int i = 0;
                 i < FAVORITE_SLOTS.length;
                 i++) {

                if (slot != FAVORITE_SLOTS[i]) {
                    continue;
                }

                ItemStack favorite =
                        favoriteManager.getFavorite(
                                player.getUniqueId(),
                                i
                        );

                if (favorite == null
                        || favorite.getType().isAir()) {

                    player.playSound(
                            player.getLocation(),
                            Sound.UI_BUTTON_CLICK,
                            1.0f,
                            1.0f
                    );

                    openItemSelection(
                            player,
                            i
                    );

                    return;
                }

                if (event.isRightClick()) {

                    player.playSound(
                            player.getLocation(),
                            Sound.UI_BUTTON_CLICK,
                            1.0f,
                            1.0f
                    );

                    favoriteManager.removeFavorite(
                            player.getUniqueId(),
                            i
                    );

                    openFavorites(player);

                    return;
                }

                if (event.isLeftClick()) {

                    player.playSound(
                            player.getLocation(),
                            Sound.UI_BUTTON_CLICK,
                            1.0f,
                            1.0f
                    );

                    buyCheapest(
                            player,
                            i
                    );

                    return;
                }
            }

            return;
        }

        /* =====================================================
           EŞYA SEÇİMİ
           ===================================================== */

        if (title.equals(ITEM_SELECT_TITLE)) {

            event.setCancelled(true);

            int favoriteSlot =
                    selectedFavoriteSlots.getOrDefault(
                            player.getUniqueId(),
                            -1
                    );

            if (favoriteSlot < 0) {

                openFavorites(player);

                return;
            }

            if (slot == 45) {

                player.playSound(
                        player.getLocation(),
                        Sound.UI_BUTTON_CLICK,
                        1.0f,
                        1.0f
                );

                openFavorites(player);

                return;
            }

            if (slot == 48) {

                player.playSound(
                        player.getLocation(),
                        Sound.UI_BUTTON_CLICK,
                        1.0f,
                        1.0f
                );

                int page =
                        itemPages.getOrDefault(
                                player.getUniqueId(),
                                0
                        );

                openItemPage(
                        player,
                        page - 1
                );

                return;
            }

            if (slot == 49) {

                player.playSound(
                        player.getLocation(),
                        Sound.UI_BUTTON_CLICK,
                        1.0f,
                        1.0f
                );

                player.closeInventory();

                return;
            }

            if (slot == 50) {
                return;
            }

            if (slot == 53) {

                player.playSound(
                        player.getLocation(),
                        Sound.UI_BUTTON_CLICK,
                        1.0f,
                        1.0f
                );

                int page =
                        itemPages.getOrDefault(
                                player.getUniqueId(),
                                0
                        );

                openItemPage(
                        player,
                        page + 1
                );

                return;
            }

            if (slot >= 0 && slot < 45) {

                ItemStack clicked =
                        event.getCurrentItem();

                if (clicked == null
                        || clicked.getType().isAir()) {

                    return;
                }

                player.playSound(
                        player.getLocation(),
                        Sound.UI_BUTTON_CLICK,
                        1.0f,
                        1.0f
                );

                ItemStack selected =
                        new ItemStack(
                                clicked.getType()
                        );

                if (isEnchantable(selected)) {

                    openEnchantSelection(
                            player,
                            selected
                    );

                } else {

                    favoriteManager.setFavorite(
                            player.getUniqueId(),
                            favoriteSlot,
                            selected
                    );

                    player.playSound(
                            player.getLocation(),
                            Sound.UI_TOAST_CHALLENGE_COMPLETE,
                            1.0f,
                            1.0f
                    );

                    player.sendMessage(
                            color(
                                    "&d&lEAuctions &8» &aFavori eklendi."
                            )
                    );

                    openFavorites(player);
                }
            }

            return;
        }

        /* =====================================================
           BÜYÜ SEÇİMİ
           ===================================================== */

        if (title.equals(ENCHANT_SELECT_TITLE)) {

            event.setCancelled(true);

            if (slot == 45) {

                player.playSound(
                        player.getLocation(),
                        Sound.UI_BUTTON_CLICK,
                        1.0f,
                        1.0f
                );

                ItemStack item =
                        pendingItems.get(
                                player.getUniqueId()
                        );

                if (item != null) {

                    openItemPage(
                            player,
                            itemPages.getOrDefault(
                                    player.getUniqueId(),
                                    0
                            )
                    );

                } else {

                    openFavorites(player);
                }

                return;
            }

            if (slot == 47) {

                player.playSound(
                        player.getLocation(),
                        Sound.UI_BUTTON_CLICK,
                        1.0f,
                        1.0f
                );

                int page =
                        enchantPages.getOrDefault(
                                player.getUniqueId(),
                                0
                        );

                openEnchantPage(
                        player,
                        page - 1
                );

                return;
            }

            if (slot == 49) {

                player.playSound(
                        player.getLocation(),
                        Sound.UI_BUTTON_CLICK,
                        1.0f,
                        1.0f
                );

                savePendingFavorite(player);

                return;
            }

            if (slot == 53) {

                player.playSound(
                        player.getLocation(),
                        Sound.UI_BUTTON_CLICK,
                        1.0f,
                        1.0f
                );

                int page =
                        enchantPages.getOrDefault(
                                player.getUniqueId(),
                                0
                        );

                openEnchantPage(
                        player,
                        page + 1
                );

                return;
            }

            if (slot >= 0 && slot < 45) {

                ItemStack clicked =
                        event.getCurrentItem();

                if (clicked == null
                        || clicked.getType()
                        != Material.ENCHANTED_BOOK) {

                    return;
                }

                ItemStack pendingItem =
                        pendingItems.get(
                                player.getUniqueId()
                        );

                if (pendingItem == null) {
                    return;
                }

                List<Enchantment> enchantments =
                        getCompatibleEnchantments(
                                pendingItem
                        );

                int page =
                        enchantPages.getOrDefault(
                                player.getUniqueId(),
                                0
                        );

                int index =
                        page * 45 + slot;

                if (index < 0
                        || index >= enchantments.size()) {

                    return;
                }

                Enchantment enchantment =
                        enchantments.get(index);

                if (event.isRightClick()) {

                    pendingEnchantments
                            .computeIfAbsent(
                                    player.getUniqueId(),
                                    ignored ->
                                            new HashMap<>()
                            )
                            .remove(enchantment);

                    player.playSound(
                            player.getLocation(),
                            Sound.UI_BUTTON_CLICK,
                            1.0f,
                            1.0f
                    );

                    openEnchantPage(
                            player,
                            page
                    );

                    return;
                }

                player.playSound(
                        player.getLocation(),
                        Sound.UI_BUTTON_CLICK,
                        1.0f,
                        1.0f
                );

                openEnchantLevels(
                        player,
                        enchantment
                );
            }

            return;
        }

        /* =====================================================
           BÜYÜ SEVİYESİ
           ===================================================== */

        if (title.equals(ENCHANT_LEVEL_TITLE)) {

            event.setCancelled(true);

            /*
             * Geri artıq 26-cı slotdadır.
             */
            if (slot == 26) {

                player.playSound(
                        player.getLocation(),
                        Sound.UI_BUTTON_CLICK,
                        1.0f,
                        1.0f
                );

                openEnchantPage(
                        player,
                        enchantPages.getOrDefault(
                                player.getUniqueId(),
                                0
                        )
                );

                return;
            }

            /*
             * Level 1-10:
             * 9-18
             */
            if (slot < 9 || slot > 18) {
                return;
            }

            Enchantment enchantment =
                    pendingSelectedEnchantment.get(
                            player.getUniqueId()
                    );

            if (enchantment == null) {
                return;
            }

            int level =
                    slot - 8;

            int max =
                    Math.min(
                            enchantment.getMaxLevel(),
                            10
                    );

            if (level < 1
                    || level > max) {

                return;
            }

            pendingEnchantments
                    .computeIfAbsent(
                            player.getUniqueId(),
                            ignored ->
                                    new HashMap<>()
                    )
                    .put(
                            enchantment,
                            level
                    );

            pendingSelectedEnchantment.remove(
                    player.getUniqueId()
            );

            player.playSound(
                    player.getLocation(),
                    Sound.UI_BUTTON_CLICK,
                    1.0f,
                    1.0f
            );

            openEnchantPage(
                    player,
                    enchantPages.getOrDefault(
                            player.getUniqueId(),
                            0
                    )
            );

            return;
        }
    }

    /* =========================================================
       FAVORİ KAYDET
       ========================================================= */

    private void savePendingFavorite(
            Player player
    ) {

        int favoriteSlot =
                selectedFavoriteSlots.getOrDefault(
                        player.getUniqueId(),
                        -1
                );

        ItemStack item =
                pendingItems.get(
                        player.getUniqueId()
                );

        if (favoriteSlot < 0
                || item == null) {

            openFavorites(player);

            return;
        }

        Map<Enchantment, Integer> enchantments =
                pendingEnchantments.getOrDefault(
                        player.getUniqueId(),
                        new HashMap<>()
                );

        ItemStack result =
                item.clone();

        for (Map.Entry<Enchantment, Integer> entry :
                enchantments.entrySet()) {

            result.addUnsafeEnchantment(
                    entry.getKey(),
                    entry.getValue()
            );
        }

        favoriteManager.setFavorite(
                player.getUniqueId(),
                favoriteSlot,
                result
        );

        player.playSound(
                player.getLocation(),
                Sound.UI_TOAST_CHALLENGE_COMPLETE,
                1.0f,
                1.0f
        );

        player.sendMessage(
                color(
                        "&d&lEAuctions &8» &aFavori kaydedildi."
                )
        );

        clearPending(player);

        openFavorites(player);
    }

    /* =========================================================
       ENCHANTABLE
       ========================================================= */

    private boolean isEnchantable(
            ItemStack item
    ) {

        if (item == null
                || item.getType().isAir()) {

            return false;
        }

        for (Enchantment enchantment :
                Enchantment.values()) {

            try {

                if (enchantment.canEnchantItem(item)) {
                    return true;
                }

            } catch (Exception ignored) {
            }
        }

        return false;
    }

    /* =========================================================
       GUI KAPATMA
       ========================================================= */

    @EventHandler
    public void onInventoryClose(
            InventoryCloseEvent event
    ) {

        if (!(event.getPlayer()
                instanceof Player player)) {

            return;
        }

        String title =
                event.getView().getTitle();

        if (title.equals(FAVORITES_TITLE)) {

            stopRefresh(player);
        }

        if (title.equals(ITEM_SELECT_TITLE)
                || title.equals(ENCHANT_SELECT_TITLE)
                || title.equals(ENCHANT_LEVEL_TITLE)) {

            Bukkit.getScheduler().runTask(
                    plugin,
                    () -> {

                        if (!player.isOnline()) {

                            clearPending(player);

                            return;
                        }

                        String newTitle =
                                player.getOpenInventory()
                                        .getTitle();

                        if (!newTitle.equals(
                                ITEM_SELECT_TITLE
                        )
                                && !newTitle.equals(
                                ENCHANT_SELECT_TITLE
                        )
                                && !newTitle.equals(
                                ENCHANT_LEVEL_TITLE
                        )) {

                            clearPending(player);
                        }
                    }
            );
        }
    }

    /* =========================================================
       OYUNCU ÇIKIŞI
       ========================================================= */

    @EventHandler
    public void onPlayerQuit(
            PlayerQuitEvent event
    ) {

        Player player =
                event.getPlayer();

        stopRefresh(player);

        clearPending(player);

        purchaseLocks.remove(
                player.getUniqueId()
        );
    }

    private void stopRefresh(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        BukkitTask task =
                refreshTasks.remove(uuid);

        if (task != null) {
            task.cancel();
        }
    }

    private void clearPending(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        selectedFavoriteSlots.remove(uuid);

        pendingItems.remove(uuid);

        pendingEnchantments.remove(uuid);

        pendingSelectedEnchantment.remove(uuid);

        itemPages.remove(uuid);

        enchantPages.remove(uuid);
    }

    /* =========================================================
       GUI YARDIMCILARI
       ========================================================= */

    private void fillBackground(
            Inventory inventory
    ) {

        ItemStack glass =
                item(
                        Material.BLACK_STAINED_GLASS_PANE,
                        " ",
                        (String[]) null
                );

        for (int i = 0;
             i < inventory.getSize();
             i++) {

            if (inventory.getItem(i) == null) {

                inventory.setItem(
                        i,
                        glass
                );
            }
        }
    }

    private ItemStack item(
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
                    color(name)
            );

            if (lore != null) {

                List<String> list =
                        new ArrayList<>();

                for (String line : lore) {

                    if (line == null) {
                        continue;
                    }

                    list.add(
                            color(line)
                    );
                }

                meta.setLore(list);
            }

            meta.addItemFlags(
                    ItemFlag.HIDE_ATTRIBUTES
            );

            item.setItemMeta(meta);
        }

        return item;
    }

    private String formatMoney(
            double amount
    ) {

        if (amount >=
                1_000_000_000_000D) {

            return moneyFormat.format(
                    amount /
                            1_000_000_000_000D
            ) + "T";
        }

        if (amount >=
                1_000_000_000D) {

            return moneyFormat.format(
                    amount /
                            1_000_000_000D
            ) + "B";
        }

        if (amount >=
                1_000_000D) {

            return moneyFormat.format(
                    amount /
                            1_000_000D
            ) + "M";
        }

        if (amount >=
                1_000D) {

            return moneyFormat.format(
                    amount /
                            1_000D
            ) + "K";
        }

        return moneyFormat.format(amount);
    }

    private boolean isFavoritesInventory(
            org.bukkit.inventory.InventoryView view
    ) {

        return view != null
                && view.getTopInventory() != null
                && view.getTitle().equals(
                FAVORITES_TITLE
        );
    }

    private static String color(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }

    private String roman(
            int number
    ) {

        return switch (number) {

            case 1 -> "I";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";

            default ->
                    String.valueOf(number);
        };
    }
}
