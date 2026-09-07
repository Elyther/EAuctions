package com.elyther.eauctions;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuctionGUI implements Listener {

    private final EAuctions plugin;

    private final String title =
            ChatColor.DARK_PURPLE + "EAuctions";

    private final String confirmationTitle =
            ChatColor.DARK_PURPLE + "Confirm Purchase";

    private static final int AUCTION_SLOTS = 45;

    private final Map<UUID, Integer> pages =
            new HashMap<>();

    private final Map<UUID, SortType> sorts =
            new HashMap<>();

    private final Map<UUID, Boolean> myAuctions =
            new HashMap<>();

    private final Map<UUID, Boolean> purchaseConfirmation =
            new HashMap<>();

    private final Map<UUID, Integer> pendingPurchases =
            new HashMap<>();

    public AuctionGUI(EAuctions plugin) {
        this.plugin = plugin;
    }

    // =========================================================
    // OPEN
    // =========================================================

    public void open(Player player) {

        UUID uuid = player.getUniqueId();

        pages.put(uuid, 0);

        sorts.put(
                uuid,
                SortType.NEWEST
        );

        myAuctions.put(
                uuid,
                false
        );

        openSearch(
                player,
                ""
        );
    }

    // =========================================================
    // OPEN SEARCH
    // =========================================================

    public void openSearch(
            Player player,
            String search
    ) {

        UUID uuid =
                player.getUniqueId();

        pages.put(
                uuid,
                0
        );

        openGUI(
                player,
                search == null ? "" : search
        );
    }

    // =========================================================
    // OPEN GUI
    // =========================================================

    private void openGUI(
            Player player,
            String search
    ) {

        UUID uuid =
                player.getUniqueId();

        String query =
                search == null
                        ? ""
                        : search.trim();

        SortType sort =
                sorts.getOrDefault(
                        uuid,
                        SortType.NEWEST
                );

        boolean onlyMine =
                myAuctions.getOrDefault(
                        uuid,
                        false
                );

        List<Auction> auctions;

        if (query.isEmpty()) {

            auctions =
                    plugin.getAuctionManager()
                            .getSortedAuctions(
                                    sort
                            );

        } else {

            auctions =
                    plugin.getAuctionManager()
                            .getSortedAuctions(
                                    query,
                                    sort
                            );
        }

        if (onlyMine) {

            List<Auction> mine =
                    new ArrayList<>();

            for (Auction auction : auctions) {

                if (auction.getSeller()
                        .equals(uuid)) {

                    mine.add(auction);
                }
            }

            auctions = mine;
        }

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        54,
                        title
                );

        int page =
                pages.getOrDefault(
                        uuid,
                        0
                );

        int start =
                page * AUCTION_SLOTS;

        int end =
                Math.min(
                        start + AUCTION_SLOTS,
                        auctions.size()
                );

        int slot = 0;

        for (int i = start;
             i < end;
             i++) {

            Auction auction =
                    auctions.get(i);

            if (auction == null ||
                    auction.getItem() == null ||
                    auction.getItem()
                            .getType()
                            .isAir()) {

                continue;
            }

            ItemStack display =
                    auction.getItem().clone();

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
                        color("&8&m----------------")
                );

                lore.add(
                        color(
                                "&7Seller: &f" +
                                        getSellerName(
                                                auction
                                        )
                        )
                );

                lore.add(
                        color(
                                "&7Price: &a$" +
                                        plugin.formatMoney(
                                                auction.getPrice()
                                        )
                        )
                );

                lore.add("");

                lore.add(
                        color("&eClick to buy!")
                );

                lore.add(
                        color(
                                "&8Auction #" +
                                        auction.getId()
                        )
                );

                meta.setLore(lore);

                display.setItemMeta(meta);
            }

            inventory.setItem(
                    slot,
                    display
            );

            slot++;
        }

        // =====================================================
        // BOTTOM BAR
        // =====================================================

        ItemStack glass =
                createItem(
                        Material.GRAY_STAINED_GLASS_PANE,
                        " "
                );

        inventory.setItem(
                46,
                glass
        );

        inventory.setItem(
                52,
                glass
        );

        // =====================================================
        // PREVIOUS
        // =====================================================

        if (page > 0) {

            inventory.setItem(
                    45,
                    createItem(
                            Material.ARROW,
                            "&e&lPrevious Page",
                            "",
                            "&7Click to go back."
                    )
            );

        } else {

            inventory.setItem(
                    45,
                    glass
            );
        }

        // =====================================================
        // COMMANDS
        // =====================================================

        inventory.setItem(
                47,
                createItem(
                        Material.BOOK,
                        "&d&lEAuctions Commands",
                        "",
                        "&7/ah",
                        "&7Open Auction House",
                        "",
                        "&7/ah sell <price>",
                        "&7Sell an item",
                        "",
                        "&7/ah reload",
                        "&7Reload plugin"
                )
        );

        // =====================================================
        // MY AUCTIONS
        // =====================================================

        if (onlyMine) {

            inventory.setItem(
                    48,
                    createItem(
                            Material.ENDER_CHEST,
                            "&a&lMy Auctions",
                            "",
                            "&aCurrently viewing",
                            "&aYour auctions.",
                            "",
                            "&eClick to show all auctions."
                    )
            );

        } else {

            inventory.setItem(
                    48,
                    createItem(
                            Material.ENDER_CHEST,
                            "&d&lMy Auctions",
                            "",
                            "&7View items you are",
                            "&7currently selling.",
                            "",
                            "&eClick to open."
                    )
            );
        }

        // =====================================================
        // SORT
        // =====================================================

        inventory.setItem(
                49,
                createItem(
                        Material.HOPPER,
                        "&b&lSort Auctions",
                        "",
                        getSortLine(
                                SortType.NEWEST,
                                sort
                        ),
                        getSortLine(
                                SortType.OLDEST,
                                sort
                        ),
                        getSortLine(
                                SortType.CHEAPEST,
                                sort
                        ),
                        getSortLine(
                                SortType.MOST_EXPENSIVE,
                                sort
                        ),
                        "",
                        "&eClick to change."
                )
        );

        // =====================================================
        // SEARCH
        // =====================================================

        String searchDisplay =
                query.isEmpty()
                        ? "All items"
                        : query;

        inventory.setItem(
                50,
                createItem(
                        Material.OAK_SIGN,
                        "&b&lSearch",
                        "",
                        "&7Current search:",
                        "&f" + searchDisplay,
                        "",
                        "&eClick to search."
                )
        );

        // =====================================================
        // PURCHASE CONFIRMATION
        // =====================================================

        boolean confirmation =
                purchaseConfirmation.getOrDefault(
                        uuid,
                        getDefaultConfirmation()
                );

        String language =
                getLanguage();

        if (language.equals("tr")) {

            inventory.setItem(
                    51,
                    createItem(
                            confirmation
                                    ? Material.LIME_DYE
                                    : Material.GRAY_DYE,
                            "&b&lSatın Alma Onayı",
                            "",
                            "&7Durum: " +
                                    (confirmation
                                            ? "&aAktif"
                                            : "&cDevre Dışı"),
                            "",
                            confirmation
                                    ? "&eSatın alırken onay ekranı açılır."
                                    : "&eDirekt satın alınır.",
                            "",
                            "&eTıklayarak değiştir."
                    )
            );

        } else {

            inventory.setItem(
                    51,
                    createItem(
                            confirmation
                                    ? Material.LIME_DYE
                                    : Material.GRAY_DYE,
                            "&b&lPurchase Confirmation",
                            "",
                            "&7Status: " +
                                    (confirmation
                                            ? "&aEnabled"
                                            : "&cDisabled"),
                            "",
                            confirmation
                                    ? "&eShows a confirmation before buying."
                                    : "&eItems are purchased instantly.",
                            "",
                            "&eClick to toggle."
                    )
            );
        }

        // =====================================================
        // NEXT PAGE
        // =====================================================

        if (end < auctions.size()) {

            inventory.setItem(
                    53,
                    createItem(
                            Material.ARROW,
                            "&e&lNext Page",
                            "",
                            "&7Click to go forward."
                    )
            );

        } else {

            inventory.setItem(
                    53,
                    glass
            );
        }

        player.openInventory(
                inventory
        );
    }

    // =========================================================
    // SORT LINE
    // =========================================================

    private String getSortLine(
            SortType type,
            SortType current
    ) {

        String name;

        switch (type) {

            case NEWEST:
                name = "Newest -> Oldest";
                break;

            case OLDEST:
                name = "Oldest -> Newest";
                break;

            case CHEAPEST:
                name = "Cheapest -> Most Expensive";
                break;

            case MOST_EXPENSIVE:
                name = "Most Expensive -> Cheapest";
                break;

            default:
                name = type.name();
        }

        if (type == current) {

            return color(
                    "&a✔ &f" + name
            );
        }

        return color(
                "&7○ &f" + name
        );
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

        String clickedTitle =
                event.getView().getTitle();

        // =====================================================
        // CONFIRMATION GUI
        // =====================================================

        if (clickedTitle.equals(
                confirmationTitle
        )) {

            event.setCancelled(true);

            if (event.getRawSlot() >=
                    event.getView()
                            .getTopInventory()
                            .getSize()) {

                return;
            }

            UUID uuid =
                    player.getUniqueId();

            Integer auctionId =
                    pendingPurchases.get(uuid);

            if (auctionId == null) {

                player.closeInventory();

                return;
            }

            int slot =
                    event.getRawSlot();

            // CONFIRM
            if (slot == 11) {

                pendingPurchases.remove(uuid);

                Auction auction =
                        plugin.getAuctionManager()
                                .getAuction(
                                        auctionId
                                );

                player.closeInventory();

                if (auction != null) {

                    buyDirect(
                            player,
                            auction
                    );
                }

                return;
            }

            // CANCEL
            if (slot == 15) {

                pendingPurchases.remove(uuid);

                player.closeInventory();

                open(player);

                return;
            }

            return;
        }

        // =====================================================
        // NORMAL GUI
        // =====================================================

        if (!clickedTitle.equals(title)) {

            return;
        }

        event.setCancelled(true);

        int slot =
                event.getRawSlot();

        if (slot < 0 ||
                slot >= event.getView()
                        .getTopInventory()
                        .getSize()) {

            return;
        }

        UUID uuid =
                player.getUniqueId();

        // =====================================================
        // PREVIOUS
        // =====================================================

        if (slot == 45) {

            int page =
                    pages.getOrDefault(
                            uuid,
                            0
                    );

            if (page > 0) {

                pages.put(
                        uuid,
                        page - 1
                );

                refresh(player);
            }

            return;
        }

        // =====================================================
        // COMMANDS
        // =====================================================

        if (slot == 47) {
            return;
        }

        // =====================================================
        // MY AUCTIONS
        // =====================================================

        if (slot == 48) {

            boolean current =
                    myAuctions.getOrDefault(
                            uuid,
                            false
                    );

            myAuctions.put(
                    uuid,
                    !current
            );

            pages.put(
                    uuid,
                    0
            );

            refresh(player);

            return;
        }

        // =====================================================
        // SORT
        // =====================================================

        if (slot == 49) {

            SortType current =
                    sorts.getOrDefault(
                            uuid,
                            SortType.NEWEST
                    );

            SortType next =
                    getNextSort(current);

            sorts.put(
                    uuid,
                    next
            );

            pages.put(
                    uuid,
                    0
            );

            refresh(player);

            return;
        }

        // =====================================================
        // SEARCH
        // =====================================================

        if (slot == 50) {

            player.closeInventory();

            AuctionSearch search =
                    plugin.getAuctionSearch();

            if (search != null) {

                search.open(player);
            }

            return;
        }

        // =====================================================
        // PURCHASE CONFIRMATION TOGGLE
        // =====================================================

        if (slot == 51) {

            boolean current =
                    purchaseConfirmation.getOrDefault(
                            uuid,
                            getDefaultConfirmation()
                    );

            purchaseConfirmation.put(
                    uuid,
                    !current
            );

            refresh(player);

            return;
        }

        // =====================================================
        // NEXT PAGE
        // =====================================================

        if (slot == 53) {

            String query =
                    getSearch(player);

            List<Auction> auctions =
                    getCurrentAuctions(
                            player,
                            query
                    );

            int page =
                    pages.getOrDefault(
                            uuid,
                            0
                    );

            int maxPage =
                    auctions.isEmpty()
                            ? 0
                            : (auctions.size() - 1)
                            / AUCTION_SLOTS;

            if (page < maxPage) {

                pages.put(
                        uuid,
                        page + 1
                );

                refresh(player);
            }

            return;
        }

        // =====================================================
        // AUCTION ITEM
        // =====================================================

        if (slot >= 0 &&
                slot < AUCTION_SLOTS) {

            String query =
                    getSearch(player);

            List<Auction> auctions =
                    getCurrentAuctions(
                            player,
                            query
                    );

            int page =
                    pages.getOrDefault(
                            uuid,
                            0
                    );

            int index =
                    page * AUCTION_SLOTS
                            + slot;

            if (index < 0 ||
                    index >= auctions.size()) {

                return;
            }

            Auction auction =
                    auctions.get(index);

            if (auction != null) {

                buy(
                        player,
                        auction
                );
            }
        }
    }

    // =========================================================
    // BUY
    // =========================================================

    private void buy(
            Player buyer,
            Auction auction
    ) {

        boolean confirmation =
                purchaseConfirmation.getOrDefault(
                        buyer.getUniqueId(),
                        getDefaultConfirmation()
                );

        if (confirmation) {

            pendingPurchases.put(
                    buyer.getUniqueId(),
                    auction.getId()
            );

            openConfirmation(
                    buyer,
                    auction
            );

            return;
        }

        buyDirect(
                buyer,
                auction
        );
    }

    // =========================================================
    // CONFIRMATION GUI
    // =========================================================

    private void openConfirmation(
            Player player,
            Auction auction
    ) {

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        27,
                        confirmationTitle
                );

        String language =
                getLanguage();

        if (language.equals("tr")) {

            inventory.setItem(
                    11,
                    createItem(
                            Material.LIME_WOOL,
                            "&a&lSatın Al",
                            "",
                            "&7Bu ürünü satın al."
                    )
            );

            inventory.setItem(
                    15,
                    createItem(
                            Material.RED_WOOL,
                            "&c&lİptal",
                            "",
                            "&7Satın alma işlemini iptal et."
                    )
            );

            inventory.setItem(
                    13,
                    createItem(
                            auction.getItem().getType(),
                            "&e&lSatın Alma Onayı",
                            "",
                            "&7Ürün: &f" +
                                    auction.getItem()
                                            .getType()
                                            .name(),
                            "&7Fiyat: &a$" +
                                    plugin.formatMoney(
                                            auction.getPrice()
                                    ),
                            "",
                            "&eBu ürünü satın almak",
                            "&eistediğinize emin misiniz?"
                    )
            );

        } else {

            inventory.setItem(
                    11,
                    createItem(
                            Material.LIME_WOOL,
                            "&a&lConfirm Purchase",
                            "",
                            "&7Buy this item."
                    )
            );

            inventory.setItem(
                    15,
                    createItem(
                            Material.RED_WOOL,
                            "&c&lCancel",
                            "",
                            "&7Cancel the purchase."
                    )
            );

            inventory.setItem(
                    13,
                    createItem(
                            auction.getItem().getType(),
                            "&e&lPurchase Confirmation",
                            "",
                            "&7Item: &f" +
                                    auction.getItem()
                                            .getType()
                                            .name(),
                            "&7Price: &a$" +
                                    plugin.formatMoney(
                                            auction.getPrice()
                                    ),
                            "",
                            "&eAre you sure you want",
                            "&eto purchase this item?"
                    )
            );
        }

        player.openInventory(
                inventory
        );
    }

    // =========================================================
    // BUY DIRECT
    // =========================================================

    private void buyDirect(
            Player buyer,
            Auction auction
    ) {

        Auction current =
                plugin.getAuctionManager()
                        .getAuction(
                                auction.getId()
                        );

        if (current == null) {

            buyer.sendMessage(
                    prefix() +
                            color(
                                    "&cThis auction no longer exists."
                            )
            );

            refresh(buyer);

            return;
        }

        if (current.getSeller()
                .equals(
                        buyer.getUniqueId()
                )) {

            buyer.sendMessage(
                    prefix() +
                            color(
                                    "&cYou cannot buy your own item."
                            )
            );

            return;
        }

        double price =
                current.getPrice();

        if (!plugin.getEconomy()
                .has(
                        buyer,
                        price
                )) {

            buyer.sendMessage(
                    prefix() +
                            color(
                                    "&cYou don't have enough money."
                            )
            );

            return;
        }

        if (!hasInventorySpace(
                buyer,
                current.getItem()
        )) {

            buyer.sendMessage(
                    prefix() +
                            color(
                                    "&cYour inventory is full."
                            )
            );

            return;
        }

        OfflinePlayer seller =
                Bukkit.getOfflinePlayer(
                        current.getSeller()
                );

        if (!plugin.getEconomy()
                .withdrawPlayer(
                        buyer,
                        price
                )
                .transactionSuccess()) {

            buyer.sendMessage(
                    prefix() +
                            color(
                                    "&cPayment failed."
                            )
            );

            return;
        }

        if (!plugin.getEconomy()
                .depositPlayer(
                        seller,
                        price
                )
                .transactionSuccess()) {

            plugin.getEconomy()
                    .depositPlayer(
                            buyer,
                            price
                    );

            buyer.sendMessage(
                    prefix() +
                            color(
                                    "&cCould not pay the seller."
                            )
            );

            return;
        }

        HashMap<Integer, ItemStack> leftover =
                buyer.getInventory()
                        .addItem(
                                current.getItem().clone()
                        );

        if (!leftover.isEmpty()) {

            plugin.getEconomy()
                    .withdrawPlayer(
                            seller,
                            price
                    );

            plugin.getEconomy()
                    .depositPlayer(
                            buyer,
                            price
                    );

            buyer.sendMessage(
                    prefix() +
                            color(
                                    "&cCould not add the item to your inventory."
                            )
            );

            return;
        }

        plugin.getAuctionManager()
                .removeAuction(
                        current.getId()
                );

        buyer.sendMessage(
                prefix() +
                        color(
                                "&aYou bought &f" +
                                        current.getItem()
                                                .getType()
                                                .name() +
                                        " &afor &f$" +
                                        plugin.formatMoney(
                                                price
                                        ) +
                                        "&a."
                        )
        );

        if (seller.isOnline()) {

            Player sellerPlayer =
                    seller.getPlayer();

            if (sellerPlayer != null) {

                sellerPlayer.sendMessage(
                        prefix() +
                                color(
                                        "&aYour item was sold for &f$" +
                                                plugin.formatMoney(
                                                        price
                                                ) +
                                                "&a."
                                )
                );
            }
        }

        refresh(buyer);
    }

    // =========================================================
    // SEARCH
    // =========================================================

    private String getSearch(
            Player player
    ) {

        AuctionSearch search =
                plugin.getAuctionSearch();

        if (search == null) {
            return "";
        }

        String current =
                search.getCurrentSearch(
                        player
                );

        return current == null
                ? ""
                : current;
    }

    // =========================================================
    // CURRENT AUCTIONS
    // =========================================================

    private List<Auction> getCurrentAuctions(
            Player player,
            String query
    ) {

        UUID uuid =
                player.getUniqueId();

        SortType sort =
                sorts.getOrDefault(
                        uuid,
                        SortType.NEWEST
                );

        boolean onlyMine =
                myAuctions.getOrDefault(
                        uuid,
                        false
                );

        List<Auction> auctions;

        if (query == null ||
                query.trim().isEmpty()) {

            auctions =
                    plugin.getAuctionManager()
                            .getSortedAuctions(
                                    sort
                            );

        } else {

            auctions =
                    plugin.getAuctionManager()
                            .getSortedAuctions(
                                    query,
                                    sort
                            );
        }

        if (onlyMine) {

            List<Auction> mine =
                    new ArrayList<>();

            for (Auction auction : auctions) {

                if (auction.getSeller()
                        .equals(uuid)) {

                    mine.add(auction);
                }
            }

            auctions = mine;
        }

        return auctions;
    }

    // =========================================================
    // REFRESH
    // =========================================================

    private void refresh(
            Player player
    ) {

        String query =
                getSearch(player);

        openGUI(
                player,
                query
        );
    }

    // =========================================================
    // NEXT SORT
    // =========================================================

    private SortType getNextSort(
            SortType current
    ) {

        if (current == null) {
            return SortType.NEWEST;
        }

        switch (current) {

            case NEWEST:
                return SortType.OLDEST;

            case OLDEST:
                return SortType.CHEAPEST;

            case CHEAPEST:
                return SortType.MOST_EXPENSIVE;

            case MOST_EXPENSIVE:
            default:
                return SortType.NEWEST;
        }
    }

    // =========================================================
    // INVENTORY SPACE
    // =========================================================

    private boolean hasInventorySpace(
            Player player,
            ItemStack item
    ) {

        if (item == null ||
                item.getType().isAir()) {

            return false;
        }

        int remaining =
                item.getAmount();

        for (ItemStack content :
                player.getInventory()
                        .getStorageContents()) {

            if (content == null ||
                    content.getType() == Material.AIR) {

                remaining -=
                        item.getMaxStackSize();

                if (remaining <= 0) {
                    return true;
                }

            } else if (content.isSimilar(item)) {

                int space =
                        content.getMaxStackSize()
                                - content.getAmount();

                remaining -= space;

                if (remaining <= 0) {
                    return true;
                }
            }
        }

        return false;
    }

    // =========================================================
    // SELLER NAME
    // =========================================================

    private String getSellerName(
            Auction auction
    ) {

        if (auction == null ||
                auction.getSeller() == null) {

            return "Unknown";
        }

        OfflinePlayer seller =
                Bukkit.getOfflinePlayer(
                        auction.getSeller()
                );

        if (seller.getName() == null) {
            return "Unknown";
        }

        return seller.getName();
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
                    color(name)
            );

            List<String> list =
                    new ArrayList<>();

            for (String line : lore) {

                list.add(
                        color(line)
                );
            }

            meta.setLore(list);

            item.setItemMeta(meta);
        }

        return item;
    }

    // =========================================================
    // LANGUAGE
    // =========================================================

    private String getLanguage() {

        return plugin.getConfig()
                .getString(
                        "language",
                        "en"
                )
                .toLowerCase();
    }

    // =========================================================
    // DEFAULT CONFIRMATION
    // =========================================================

    private boolean getDefaultConfirmation() {

        return plugin.getConfig()
                .getBoolean(
                        "purchase-confirmation",
                        true
                );
    }

    // =========================================================
    // PREFIX
    // =========================================================

    private String prefix() {

        return color(
                plugin.getConfig()
                        .getString(
                                "messages.prefix",
                                "&d&lEAuctions &8» "
                        )
        );
    }

    // =========================================================
    // COLOR
    // =========================================================

    private String color(
            String text
    ) {

        return plugin.color(text);
    }
}
