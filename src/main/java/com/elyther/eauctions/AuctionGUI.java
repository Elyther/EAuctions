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

    private static final int ITEMS_PER_PAGE = 45;

    /*
     * Current page for each player.
     */
    private final Map<UUID, Integer> pages =
            new HashMap<>();

    /*
     * Whether the player is viewing only their auctions.
     */
    private final Map<UUID, Boolean> myAuctions =
            new HashMap<>();

    /*
     * Current sort type.
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

        pages.put(
                player.getUniqueId(),
                0
        );

        myAuctions.put(
                player.getUniqueId(),
                false
        );

        sorts.put(
                player.getUniqueId(),
                SortType.NEWEST
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

        if (!pages.containsKey(uuid)) {
            pages.put(uuid, 0);
        }

        if (!myAuctions.containsKey(uuid)) {
            myAuctions.put(uuid, false);
        }

        if (!sorts.containsKey(uuid)) {
            sorts.put(
                    uuid,
                    SortType.NEWEST
            );
        }

        String currentSearch =
                search == null
                        ? ""
                        : search.trim();

        /*
         * Get correctly filtered and sorted auctions.
         */
        List<Auction> auctions =
                plugin.getAuctionManager()
                        .getFiltered(
                                currentSearch,
                                uuid,
                                myAuctions.get(uuid),
                                sorts.get(uuid)
                        );

        /*
         * Calculate pages.
         */
        int maxPages =
                Math.max(
                        1,
                        (int) Math.ceil(
                                auctions.size()
                                        / (double) ITEMS_PER_PAGE
                        )
                );

        int page =
                pages.getOrDefault(
                        uuid,
                        0
                );

        if (page >= maxPages) {
            page = maxPages - 1;
        }

        if (page < 0) {
            page = 0;
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
                page * ITEMS_PER_PAGE;

        int end =
                Math.min(
                        start + ITEMS_PER_PAGE,
                        auctions.size()
                );

        int guiSlot = 0;

        for (int i = start; i < end; i++) {

            Auction auction =
                    auctions.get(i);

            ItemStack display =
                    auction.getItem().clone();

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
                                "&7Seller: &f" +
                                        getSellerName(auction)
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
                        color(
                                "&eClick to buy!"
                        )
                );

                meta.setLore(lore);

                display.setItemMeta(meta);
            }

            inventory.setItem(
                    guiSlot,
                    display
            );

            guiSlot++;
        }

        // =====================================================
        // BOTTOM BAR
        // =====================================================

        /*
         * Slot 45 - Previous Page
         */

        ItemStack previous =
                createItem(
                        Material.ARROW,
                        "&e&lPrevious Page",
                        "",
                        "&7Page: &f" +
                                (page + 1) +
                                "&7/&f" +
                                maxPages,
                        "",
                        "&eClick to go back"
                );

        inventory.setItem(
                45,
                previous
        );

        /*
         * Slots 46, 51, 52 are intentionally empty.
         */

        // =====================================================
        // COMMANDS - SLOT 47
        // =====================================================

        ItemStack commands =
                createItem(
                        Material.BOOK,
                        "&b&lCommands",
                        "",
                        "&7Auction House commands:",
                        "",
                        "&f/ah",
                        "&f/ah sell <price>",
                        "&f/ah search <item>",
                        "",
                        "&eClick for more information"
                );

        inventory.setItem(
                47,
                commands
        );

        // =====================================================
        // MY AUCTIONS - SLOT 48
        // =====================================================

        boolean viewingMine =
                myAuctions.getOrDefault(
                        uuid,
                        false
                );

        ItemStack mine =
                createItem(
                        Material.ENDER_CHEST,
                        viewingMine
                                ? "&a&lMy Auctions"
                                : "&d&lMy Auctions",
                        "",
                        viewingMine
                                ? "&aCurrently showing your auctions."
                                : "&7Show only your auctions.",
                        "",
                        "&eClick to toggle"
                );

        inventory.setItem(
                48,
                mine
        );

        // =====================================================
        // SORT - SLOT 49
        // =====================================================

        SortType sort =
                sorts.getOrDefault(
                        uuid,
                        SortType.NEWEST
                );

        ItemStack sortItem =
                createItem(
                        Material.HOPPER,
                        "&6&lSort",
                        "",
                        "&7Current:",
                        "&f" + getSortName(sort),
                        "",
                        "&eClick to change"
                );

        inventory.setItem(
                49,
                sortItem
        );

        // =====================================================
        // SEARCH - SLOT 50
        // =====================================================

        String searchDisplay =
                currentSearch.isEmpty()
                        ? "All items"
                        : currentSearch;

        ItemStack searchItem =
                createItem(
                        Material.OAK_SIGN,
                        "&b&lSearch",
                        "",
                        "&7Current search:",
                        "&f" + searchDisplay,
                        "",
                        "&eClick to search",
                        "",
                        "&7Example: &fdia",
                        "&7Finds items beginning with",
                        "&7the word you enter."
                );

        inventory.setItem(
                50,
                searchItem
        );

        // =====================================================
        // NEXT PAGE - SLOT 53
        // =====================================================

        ItemStack next =
                createItem(
                        Material.ARROW,
                        "&e&lNext Page",
                        "",
                        "&7Page: &f" +
                                (page + 1) +
                                "&7/&f" +
                                maxPages,
                        "",
                        "&eClick to go forward"
                );

        inventory.setItem(
                53,
                next
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

        /*
         * Ignore clicks outside the GUI.
         */
        if (slot < 0 ||
                slot >= event.getView()
                        .getTopInventory()
                        .getSize()) {

            return;
        }

        UUID uuid =
                player.getUniqueId();

        // =====================================================
        // PREVIOUS PAGE - SLOT 45
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

                reopen(player);

            } else {

                player.sendMessage(
                        prefix() +
                                color(
                                        "&cYou are already on the first page."
                                )
                );
            }

            return;
        }

        // =====================================================
        // COMMANDS - SLOT 47
        // =====================================================

        if (slot == 47) {

            player.closeInventory();

            player.sendMessage("");
            player.sendMessage(
                    color(
                            "&d&lEAuctions Commands"
                    )
            );
            player.sendMessage(
                    color(
                            "&8&m--------------------------"
                    )
            );
            player.sendMessage(
                    color(
                            "&f/ah &7- Open Auction House"
                    )
            );
            player.sendMessage(
                    color(
                            "&f/ah sell <price> &7- Sell item"
                    )
            );
            player.sendMessage(
                    color(
                            "&f/ah search <item> &7- Search"
                    )
            );
            player.sendMessage(
                    color(
                            "&8&m--------------------------"
                    )
            );
            player.sendMessage("");

            return;
        }

        // =====================================================
        // MY AUCTIONS - SLOT 48
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

            reopen(player);

            return;
        }

        // =====================================================
        // SORT - SLOT 49
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

            player.sendMessage(
                    prefix() +
                            color(
                                    "&aSort changed to: &f" +
                                            getSortName(next)
                            )
            );

            reopen(player);

            return;
        }

        // =====================================================
        // SEARCH - SLOT 50
        // =====================================================

        if (slot == 50) {

            player.closeInventory();

            AuctionSearch search =
                    plugin.getAuctionSearch();

            if (search != null) {

                search.open(player);

            } else {

                player.sendMessage(
                        prefix() +
                                color(
                                        "&cSearch system is unavailable."
                                )
                );
            }

            return;
        }

        // =====================================================
        // NEXT PAGE - SLOT 53
        // =====================================================

        if (slot == 53) {

            String searchText =
                    plugin.getAuctionSearch()
                            .getCurrentSearch(player);

            List<Auction> auctions =
                    plugin.getAuctionManager()
                            .getFiltered(
                                    searchText,
                                    uuid,
                                    myAuctions.getOrDefault(
                                            uuid,
                                            false
                                    ),
                                    sorts.getOrDefault(
                                            uuid,
                                            SortType.NEWEST
                                    )
                            );

            int maxPages =
                    Math.max(
                            1,
                            (int) Math.ceil(
                                    auctions.size()
                                            / (double) ITEMS_PER_PAGE
                            )
                    );

            int page =
                    pages.getOrDefault(
                            uuid,
                            0
                    );

            if (page + 1 < maxPages) {

                pages.put(
                        uuid,
                        page + 1
                );

                reopen(player);

            } else {

                player.sendMessage(
                        prefix() +
                                color(
                                        "&cYou are already on the last page."
                                )
                );
            }

            return;
        }

        // =====================================================
        // EMPTY BOTTOM SLOTS
        // =====================================================

        if (slot == 46 ||
                slot == 51 ||
                slot == 52) {

            return;
        }

        // =====================================================
        // AUCTION ITEM
        // =====================================================

        if (slot >= 0 &&
                slot < 45) {

            String searchText =
                    plugin.getAuctionSearch()
                            .getCurrentSearch(player);

            List<Auction> auctions =
                    plugin.getAuctionManager()
                            .getFiltered(
                                    searchText,
                                    uuid,
                                    myAuctions.getOrDefault(
                                            uuid,
                                            false
                                    ),
                                    sorts.getOrDefault(
                                            uuid,
                                            SortType.NEWEST
                                    )
                            );

            int page =
                    pages.getOrDefault(
                            uuid,
                            0
                    );

            int index =
                    page * ITEMS_PER_PAGE +
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
    }

    // =========================================================
    // REOPEN
    // =========================================================

    private void reopen(
            Player player
    ) {

        String search =
                plugin.getAuctionSearch()
                        .getCurrentSearch(player);

        openSearch(
                player,
                search
        );
    }

    // =========================================================
    // SORT
    // =========================================================

    private SortType getNextSort(
            SortType current
    ) {

        switch (current) {

            case NEWEST:
                return SortType.OLDEST;

            case OLDEST:
                return SortType.LOWEST_PRICE;

            case LOWEST_PRICE:
                return SortType.HIGHEST_PRICE;

            case HIGHEST_PRICE:
            default:
                return SortType.NEWEST;
        }
    }

    private String getSortName(
            SortType sort
    ) {

        switch (sort) {

            case OLDEST:
                return "Oldest → Newest";

            case LOWEST_PRICE:
                return "Lowest Price → Highest Price";

            case HIGHEST_PRICE:
                return "Highest Price → Lowest Price";

            case NEWEST:
            default:
                return "Newest → Oldest";
        }
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

            reopen(buyer);

            return;
        }

        if (current.getSeller()
                .equals(buyer.getUniqueId())) {

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

        // =====================================================
        // WITHDRAW
        // =====================================================

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

        // =====================================================
        // DEPOSIT
        // =====================================================

        plugin.getEconomy()
                .depositPlayer(
                        seller,
                        price
                );

        // =====================================================
        // GIVE ITEM
        // =====================================================

        buyer.getInventory()
                .addItem(
                        current.getItem()
                );

        // =====================================================
        // REMOVE AUCTION
        // =====================================================

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

        // =====================================================
        // SELLER MESSAGE
        // =====================================================

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

        // =====================================================
        // REOPEN
        // =====================================================

        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> reopen(buyer)
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
                    content.getType() == Material.AIR) {

                remaining -= item.getMaxStackSize();

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
