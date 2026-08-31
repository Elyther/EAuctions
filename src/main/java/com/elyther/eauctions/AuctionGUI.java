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
            ChatColor.DARK_PURPLE +
                    "EAuctions";

    /*
     * 45 auction slots.
     *
     * 0 - 44 = auction items
     * 45 - 53 = bottom menu
     */

    private static final int AUCTION_SLOTS = 45;

    /*
     * Player page
     */

    private final Map<UUID, Integer> pages =
            new HashMap<>();

    /*
     * Player search
     */

    private final Map<UUID, String> searches =
            new HashMap<>();

    /*
     * Player sorting
     */

    private final Map<UUID, SortType> sorts =
            new HashMap<>();

    public AuctionGUI(EAuctions plugin) {

        this.plugin = plugin;
    }

    // =========================================================
    // OPEN
    // =========================================================

    public void open(Player player) {

        searches.put(
                player.getUniqueId(),
                ""
        );

        pages.put(
                player.getUniqueId(),
                0
        );

        sorts.put(
                player.getUniqueId(),
                SortType.NEWEST
        );

        openPage(
                player,
                0
        );
    }

    // =========================================================
    // OPEN SEARCH
    // =========================================================

    public void openSearch(
            Player player,
            String search
    ) {

        searches.put(
                player.getUniqueId(),
                search == null
                        ? ""
                        : search.trim()
        );

        pages.put(
                player.getUniqueId(),
                0
        );

        if (!sorts.containsKey(
                player.getUniqueId()
        )) {

            sorts.put(
                    player.getUniqueId(),
                    SortType.NEWEST
            );
        }

        openPage(
                player,
                0
        );
    }

    // =========================================================
    // OPEN PAGE
    // =========================================================

    private void openPage(
            Player player,
            int page
    ) {

        UUID uuid =
                player.getUniqueId();

        String search =
                searches.getOrDefault(
                        uuid,
                        ""
                );

        SortType sort =
                sorts.getOrDefault(
                        uuid,
                        SortType.NEWEST
                );

        List<Auction> auctions =
                plugin.getAuctionManager()
                        .searchAndSort(
                                search,
                                sort
                        );

        int maxPage =
                Math.max(
                        0,
                        (auctions.size() - 1)
                                / AUCTION_SLOTS
                );

        if (page < 0) {
            page = 0;
        }

        if (page > maxPage) {
            page = maxPage;
        }

        pages.put(
                uuid,
                page
        );

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        54,
                        title
                );

        // =====================================================
        // AUCTION ITEMS
        // =====================================================

        int start =
                page *
                        AUCTION_SLOTS;

        int end =
                Math.min(
                        start +
                                AUCTION_SLOTS,
                        auctions.size()
                );

        int slot = 0;

        for (int i = start;
             i < end;
             i++) {

            Auction auction =
                    auctions.get(i);

            ItemStack display =
                    auction.getItem()
                            .clone();

            ItemMeta meta =
                    display.getItemMeta();

            if (meta != null) {

                List<String> lore =
                        meta.hasLore()
                                ? new ArrayList<>(
                                meta.getLore()
                        )
                                : new ArrayList<>();

                lore.add("");

                lore.add(
                        color(
                                "&8&m----------------"
                        )
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
                        );

                lore.add("");

                lore.add(
                        color(
                                "&e&lCLICK TO BUY"
                        )
                );

                lore.add(
                        color(
                                "&8&m----------------"
                        )
                );

                meta.setLore(
                        lore
                );

                display.setItemMeta(
                        meta
                );
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

        /*
         * 46 and 52 = empty-looking glass
         */

        inventory.setItem(
                46,
                glass
        );

        inventory.setItem(
                52,
                glass
        );

        // =====================================================
        // SLOT 45 - PREVIOUS
        // =====================================================

        ItemStack previous;

        if (page > 0) {

            previous =
                    createItem(
                            Material.ARROW,
                            "&a&lPrevious Page",
                            "",
                            "&7Page: &f" +
                                    page +
                                    "&7 / &f" +
                                    (maxPage + 1),
                            "",
                            "&eClick to go back"
                    );

        } else {

            previous =
                    createItem(
                            Material.GRAY_STAINED_GLASS_PANE,
                            "&7No previous page"
                    );
        }

        inventory.setItem(
                45,
                previous
        );

        // =====================================================
        // SLOT 47 - COMMANDS
        // =====================================================

        ItemStack commands =
                createItem(
                        Material.BOOK,
                        "&d&lEAuctions Commands",
                        "",
                        "&f/ah",
                        "&7Open Auction House",
                        "",
                        "&f/ah sell <price>",
                        "&7Sell item from your hand",
                        "",
                        "&f/ah <item>",
                        "&7Search an item",
                        "",
                        "&eExamples:",
                        "&7/ah dia",
                        "&7/ah diamond"
                );

        inventory.setItem(
                47,
                commands
        );

        // =====================================================
        // SLOT 48 - MY AUCTIONS
        // =====================================================

        ItemStack myAuctions =
                createItem(
                        Material.ENDER_CHEST,
                        "&5&lMy Auctions",
                        "",
                        "&7See your own listings.",
                        "",
                        "&eClick to open"
                );

        inventory.setItem(
                48,
                myAuctions
        );

        // =====================================================
        // SLOT 49 - SORT
        // =====================================================

        ItemStack sortItem;

        switch (sort) {

            case NEWEST:

                sortItem =
                        createItem(
                                Material.HOPPER,
                                "&6&lSort",
                                "",
                                "&eCurrent:",
                                "&fNewest → Oldest",
                                "",
                                "&7Click to change"
                        );

                break;

            case OLDEST:

                sortItem =
                        createItem(
                                Material.HOPPER,
                                "&6&lSort",
                                "",
                                "&eCurrent:",
                                "&fOldest → Newest",
                                "",
                                "&7Click to change"
                        );

                break;

            case CHEAPEST:

                sortItem =
                        createItem(
                                Material.HOPPER,
                                "&6&lSort",
                                "",
                                "&eCurrent:",
                                "&fCheapest → Most Expensive",
                                "",
                                "&7Click to change"
                        );

                break;

            case MOST_EXPENSIVE:

                sortItem =
                        createItem(
                                Material.HOPPER,
                                "&6&lSort",
                                "",
                                "&eCurrent:",
                                "&fMost Expensive → Cheapest",
                                "",
                                "&7Click to change"
                        );

                break;

            default:

                sortItem =
                        createItem(
                                Material.HOPPER,
                                "&6&lSort"
                        );
        }

        inventory.setItem(
                49,
                sortItem
        );

        // =====================================================
        // SLOT 50 - SEARCH
        // =====================================================

        String searchText =
                search.isBlank()
                        ? "All Items"
                        : search;

        ItemStack searchItem =
                createItem(
                        Material.OAK_SIGN,
                        "&b&lSearch",
                        "",
                        "&7Current search:",
                        "&f" + searchText,
                        "",
                        "&eClick to search"
                );

        inventory.setItem(
                50,
                searchItem
        );

        // =====================================================
        // SLOT 51 - PAGE INFO
        // =====================================================

        ItemStack pageItem =
                createItem(
                        Material.PAPER,
                        "&f&lPage",
                        "",
                        "&7Page: &f" +
                                (page + 1) +
                                "&7 / &f" +
                                (maxPage + 1),
                        "",
                        "&7Auctions: &f" +
                                auctions.size()
                );

        inventory.setItem(
                51,
                pageItem
        );

        // =====================================================
        // SLOT 53 - NEXT
        // =====================================================

        ItemStack next;

        if (page < maxPage) {

            next =
                    createItem(
                            Material.ARROW,
                            "&a&lNext Page",
                            "",
                            "&7Page: &f" +
                                    (page + 2) +
                                    "&7 / &f" +
                                    (maxPage + 1),
                            "",
                            "&eClick to continue"
                    );

        } else {

            next =
                    createItem(
                            Material.GRAY_STAINED_GLASS_PANE,
                            "&7No next page"
                    );
        }

        inventory.setItem(
                53,
                next
        );

        // =====================================================
        // OPEN
        // =====================================================

        player.openInventory(
                inventory
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
                instanceof Player player)) {

            return;
        }

        if (!event.getView()
                .getTitle()
                .equals(title)) {

            return;
        }

        event.setCancelled(true);

        int slot =
                event.getRawSlot();

        if (slot < 0 ||
                slot >= 54) {

            return;
        }

        // =====================================================
        // PREVIOUS
        // =====================================================

        if (slot == 45) {

            int page =
                    pages.getOrDefault(
                            player.getUniqueId(),
                            0
                    );

            if (page > 0) {

                openPage(
                        player,
                        page - 1
                );
            }

            return;
        }

        // =====================================================
        // COMMANDS
        // =====================================================

        if (slot == 47) {

            player.sendMessage(
                    color(
                            "&d&lEAuctions"
                    )
            );

            player.sendMessage(
                    color(
                            "&7/ah &f- Open Auction House"
                    )
            );

            player.sendMessage(
                    color(
                            "&7/ah sell <price> &f- Sell item"
                    )
            );

            player.sendMessage(
                    color(
                            "&7/ah <item> &f- Search item"
                    )
            );

            return;
        }

        // =====================================================
        // MY AUCTIONS
        // =====================================================

        if (slot == 48) {

            openMyAuctions(
                    player
            );

            return;
        }

        // =====================================================
        // SORT
        // =====================================================

        if (slot == 49) {

            changeSort(
                    player
            );

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

                search.open(
                        player
                );
            }

            return;
        }

        // =====================================================
        // NEXT
        // =====================================================

        if (slot == 53) {

            int page =
                    pages.getOrDefault(
                            player.getUniqueId(),
                            0
                    );

            String search =
                    searches.getOrDefault(
                            player.getUniqueId(),
                            ""
                    );

            SortType sort =
                    sorts.getOrDefault(
                            player.getUniqueId(),
                            SortType.NEWEST
                    );

            List<Auction> auctions =
                    plugin.getAuctionManager()
                            .searchAndSort(
                                    search,
                                    sort
                            );

            int maxPage =
                    Math.max(
                            0,
                            (auctions.size() - 1)
                                    / AUCTION_SLOTS
                    );

            if (page < maxPage) {

                openPage(
                        player,
                        page + 1
                );
            }

            return;
        }

        // =====================================================
        // EMPTY BOTTOM SLOTS
        // =====================================================

        if (slot >= 45) {
            return;
        }

        // =====================================================
        // AUCTION CLICK
        // =====================================================

        String search =
                searches.getOrDefault(
                        player.getUniqueId(),
                        ""
                );

        SortType sort =
                sorts.getOrDefault(
                        player.getUniqueId(),
                        SortType.NEWEST
                );

        List<Auction> auctions =
                plugin.getAuctionManager()
                        .searchAndSort(
                                search,
                                sort
                        );

        int page =
                pages.getOrDefault(
                        player.getUniqueId(),
                        0
                );

        int index =
                page *
                        AUCTION_SLOTS +
                        slot;

        if (index < 0 ||
                index >= auctions.size()) {

            return;
        }

        Auction auction =
                auctions.get(index);

        buy(
                player,
                auction
        );
    }

    // =========================================================
    // CHANGE SORT
    // =========================================================

    private void changeSort(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        SortType current =
                sorts.getOrDefault(
                        uuid,
                        SortType.NEWEST
                );

        SortType next;

        switch (current) {

            case NEWEST:

                next =
                        SortType.OLDEST;

                break;

            case OLDEST:

                next =
                        SortType.CHEAPEST;

                break;

            case CHEAPEST:

                next =
                        SortType.MOST_EXPENSIVE;

                break;

            case MOST_EXPENSIVE:

                next =
                        SortType.NEWEST;

                break;

            default:

                next =
                        SortType.NEWEST;
        }

        sorts.put(
                uuid,
                next
        );

        pages.put(
                uuid,
                0
        );

        openPage(
                player,
                0
        );
    }

    // =========================================================
    // MY AUCTIONS
    // =========================================================

    private void openMyAuctions(
            Player player
    ) {

        List<Auction> auctions =
                plugin.getAuctionManager()
                        .getPlayerAuctions(
                                player.getUniqueId()
                        );

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        54,
                        ChatColor.DARK_PURPLE +
                                "My Auctions"
                );

        int slot = 0;

        for (Auction auction :
                auctions) {

            if (slot >= 45) {
                break;
            }

            ItemStack item =
                    auction.getItem()
                            .clone();

            ItemMeta meta =
                    item.getItemMeta();

            if (meta != null) {

                List<String> lore =
                        meta.hasLore()
                                ? new ArrayList<>(
                                meta.getLore()
                        )
                                : new ArrayList<>();

                lore.add("");

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
                        color(
                                "&cRight-click to remove"
                        )
                );

                meta.setLore(
                        lore
                );

                item.setItemMeta(
                        meta
                );
            }

            inventory.setItem(
                    slot,
                    item
            );

            slot++;
        }

        // =====================================================
        // BOTTOM
        // =====================================================

        ItemStack filler =
                createItem(
                        Material.GRAY_STAINED_GLASS_PANE,
                        " "
                );

        for (int i = 45;
             i < 54;
             i++) {

            inventory.setItem(
                    i,
                    filler
            );
        }

        inventory.setItem(
                49,
                createItem(
                        Material.ARROW,
                        "&c&lBack",
                        "",
                        "&7Return to Auction House"
                )
        );

        player.openInventory(
                inventory
        );
    }

    // =========================================================
    // BUY
    // =========================================================

    private void buy(
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

            openPage(
                    buyer,
                    pages.getOrDefault(
                            buyer.getUniqueId(),
                            0
                    )
            );

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

        plugin.getEconomy()
                .depositPlayer(
                        seller,
                        price
                );

        buyer.getInventory()
                .addItem(
                        current.getItem()
                );

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

        String search =
                searches.getOrDefault(
                        buyer.getUniqueId(),
                        ""
                );

        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () ->
                                openSearch(
                                        buyer,
                                        search
                                )
                );
    }

    // =========================================================
    // INVENTORY SPACE
    // =========================================================

    private boolean hasInventorySpace(
            Player player,
            ItemStack item
    ) {

        int remaining =
                item.getAmount();

        for (ItemStack content :
                player.getInventory()
                        .getStorageContents()) {

            if (content == null ||
                    content.getType() ==
                            Material.AIR) {

                remaining -=
                        item.getMaxStackSize();

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
                new ItemStack(
                        material
                );

        ItemMeta meta =
                item.getItemMeta();

        if (meta != null) {

            meta.setDisplayName(
                    color(name)
            );

            List<String> list =
                    new ArrayList<>();

            for (String line :
                    lore) {

                list.add(
                        color(line)
                );
            }

            meta.setLore(
                    list
            );

            item.setItemMeta(
                    meta
            );
        }

        return item;
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
