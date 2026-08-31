package com.elyther.eauctions;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
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

    private static final int AUCTION_SLOTS = 45;
    private static final int PAGE_SIZE = 45;

    /*
     * Player GUI state
     *
     * SEARCH = normal search
     * ALL = all auctions
     * CHEAPEST = cheapest first
     * EXPENSIVE = most expensive first
     * NEWEST = newest first
     * MY = player's auctions
     */

    private final Map<UUID, GuiMode> modes =
            new HashMap<>();

    private final Map<UUID, Integer> pages =
            new HashMap<>();

    private final Map<UUID, String> searches =
            new HashMap<>();

    public AuctionGUI(EAuctions plugin) {
        this.plugin = plugin;
    }

    // =========================================================
    // MODES
    // =========================================================

    private enum GuiMode {

        ALL,
        SEARCH,
        CHEAPEST,
        EXPENSIVE,
        NEWEST,
        MY
    }

    // =========================================================
    // OPEN
    // =========================================================

    public void open(Player player) {

        UUID uuid =
                player.getUniqueId();

        modes.put(
                uuid,
                GuiMode.ALL
        );

        pages.put(
                uuid,
                0
        );

        searches.put(
                uuid,
                ""
        );

        openCurrent(player);
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

        String query =
                search == null
                        ? ""
                        : search.trim();

        searches.put(
                uuid,
                query
        );

        modes.put(
                uuid,
                query.isEmpty()
                        ? GuiMode.ALL
                        : GuiMode.SEARCH
        );

        pages.put(
                uuid,
                0
        );

        openCurrent(player);
    }

    // =========================================================
    // OPEN CURRENT
    // =========================================================

    private void openCurrent(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        GuiMode mode =
                modes.getOrDefault(
                        uuid,
                        GuiMode.ALL
                );

        int page =
                pages.getOrDefault(
                        uuid,
                        0
                );

        List<Auction> auctions =
                getAuctionsForPlayer(
                        player,
                        mode
                );

        int totalPages =
                Math.max(
                        1,
                        (int) Math.ceil(
                                auctions.size()
                                        / (double) PAGE_SIZE
                        )
                );

        /*
         * Make sure page is valid.
         */

        if (page >= totalPages) {
            page = totalPages - 1;
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
                page * PAGE_SIZE;

        int end =
                Math.min(
                        start + PAGE_SIZE,
                        auctions.size()
                );

        int slot = 0;

        for (int i = start;
             i < end;
             i++) {

            Auction auction =
                    auctions.get(i);

            ItemStack display =
                    createAuctionItem(
                            auction
                    );

            inventory.setItem(
                    slot,
                    display
            );

            slot++;
        }

        // =====================================================
        // BOTTOM BAR
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

        // =====================================================
        // MY AUCTIONS
        // =====================================================

        inventory.setItem(
                45,
                createItem(
                        Material.CHEST,
                        "&d&lMy Auctions",
                        "",
                        "&7View your own auctions.",
                        "",
                        mode == GuiMode.MY
                                ? "&aCurrently selected"
                                : "&eClick to open"
                )
        );

        // =====================================================
        // CHEAPEST
        // =====================================================

        inventory.setItem(
                46,
                createItem(
                        Material.GOLD_INGOT,
                        "&a&lCheapest",
                        "",
                        "&7Sort auctions from",
                        "&7cheapest to most expensive.",
                        "",
                        mode == GuiMode.CHEAPEST
                                ? "&aCurrently selected"
                                : "&eClick to sort"
                )
        );

        // =====================================================
        // MOST EXPENSIVE
        // =====================================================

        inventory.setItem(
                47,
                createItem(
                        Material.DIAMOND,
                        "&b&lMost Expensive",
                        "",
                        "&7Sort auctions from",
                        "&7most expensive to cheapest.",
                        "",
                        mode == GuiMode.EXPENSIVE
                                ? "&aCurrently selected"
                                : "&eClick to sort"
                )
        );

        // =====================================================
        // NEWEST
        // =====================================================

        inventory.setItem(
                48,
                createItem(
                        Material.CLOCK,
                        "&e&lNewest",
                        "",
                        "&7Show newest auctions first.",
                        "",
                        mode == GuiMode.NEWEST
                                ? "&aCurrently selected"
                                : "&eClick to sort"
                )
        );

        // =====================================================
        // SEARCH
        // =====================================================

        String currentSearch =
                searches.getOrDefault(
                        uuid,
                        ""
                );

        inventory.setItem(
                49,
                createItem(
                        Material.COMPASS,
                        "&b&lSearch",
                        "",
                        "&7Current search:",
                        "&f" +
                                (
                                        currentSearch.isEmpty()
                                                ? "All items"
                                                : currentSearch
                                ),
                        "",
                        "&eClick to search"
                )
        );

        // =====================================================
        // ALL AUCTIONS
        // =====================================================

        inventory.setItem(
                50,
                createItem(
                        Material.BOOK,
                        "&f&lAll Auctions",
                        "",
                        "&7Show all auctions.",
                        "",
                        mode == GuiMode.ALL
                                ? "&aCurrently selected"
                                : "&eClick to open"
                )
        );

        // =====================================================
        // REFRESH
        // =====================================================

        inventory.setItem(
                51,
                createItem(
                        Material.SUNFLOWER,
                        "&6&lRefresh",
                        "",
                        "&7Refresh the auction house.",
                        "",
                        "&eClick to refresh"
                )
        );

        // =====================================================
        // PREVIOUS
        // =====================================================

        if (page > 0) {

            inventory.setItem(
                    52,
                    createItem(
                            Material.ARROW,
                            "&e&lPrevious Page",
                            "",
                            "&7Page: &f" +
                                    (page),
                            "",
                            "&eClick"
                    )
            );

        } else {

            inventory.setItem(
                    52,
                    createItem(
                            Material.BARRIER,
                            "&c&lPrevious Page",
                            "",
                            "&7You are already on",
                            "&7the first page."
                    )
            );
        }

        // =====================================================
        // NEXT
        // =====================================================

        if (page + 1 < totalPages) {

            inventory.setItem(
                    53,
                    createItem(
                            Material.ARROW,
                            "&a&lNext Page",
                            "",
                            "&7Page: &f" +
                                    (page + 2),
                            "",
                            "&eClick"
                    )
            );

        } else {

            inventory.setItem(
                    53,
                    createItem(
                            Material.BARRIER,
                            "&c&lNext Page",
                            "",
                            "&7You are already on",
                            "&7the last page."
                    )
            );
        }

        // =====================================================
        // OPEN
        // =====================================================

        player.openInventory(
                inventory
        );
    }

    // =========================================================
    // GET AUCTIONS
    // =========================================================

    private List<Auction> getAuctionsForPlayer(
            Player player,
            GuiMode mode
    ) {

        AuctionManager manager =
                plugin.getAuctionManager();

        switch (mode) {

            case SEARCH:

                return manager.search(
                        searches.getOrDefault(
                                player.getUniqueId(),
                                ""
                        )
                );

            case CHEAPEST:

                return manager.getCheapest();

            case EXPENSIVE:

                return manager.getMostExpensive();

            case NEWEST:

                return manager.getNewest();

            case MY:

                return manager.getPlayerAuctions(
                        player.getUniqueId()
                );

            case ALL:
            default:

                return manager.getAuctions();
        }
    }

    // =========================================================
    // AUCTION ITEM
    // =========================================================

    private ItemStack createAuctionItem(
            Auction auction
    ) {

        ItemStack display =
                auction.getItem().clone();

        ItemMeta meta =
                display.getItemMeta();

        if (meta == null) {
            return display;
        }

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
                )
        );

        lore.add(
                color(
                        "&7Amount: &f" +
                                auction.getItem()
                                        .getAmount()
                )
        );

        lore.add("");

        lore.add(
                color(
                        "&e&lClick to buy"
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

        return display;
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
        // MY AUCTIONS
        // =====================================================

        if (slot == 45) {

            setMode(
                    player,
                    GuiMode.MY
            );

            return;
        }

        // =====================================================
        // CHEAPEST
        // =====================================================

        if (slot == 46) {

            setMode(
                    player,
                    GuiMode.CHEAPEST
            );

            return;
        }

        // =====================================================
        // MOST EXPENSIVE
        // =====================================================

        if (slot == 47) {

            setMode(
                    player,
                    GuiMode.EXPENSIVE
            );

            return;
        }

        // =====================================================
        // NEWEST
        // =====================================================

        if (slot == 48) {

            setMode(
                    player,
                    GuiMode.NEWEST
            );

            return;
        }

        // =====================================================
        // SEARCH
        // =====================================================

        if (slot == 49) {

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
        // ALL
        // =====================================================

        if (slot == 50) {

            setMode(
                    player,
                    GuiMode.ALL
            );

            return;
        }

        // =====================================================
        // REFRESH
        // =====================================================

        if (slot == 51) {

            openCurrent(
                    player
            );

            return;
        }

        // =====================================================
        // PREVIOUS
        // =====================================================

        if (slot == 52) {

            int page =
                    pages.getOrDefault(
                            player.getUniqueId(),
                            0
                    );

            if (page > 0) {

                pages.put(
                        player.getUniqueId(),
                        page - 1
                );

                openCurrent(
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

            List<Auction> auctions =
                    getAuctionsForPlayer(
                            player,
                            modes.getOrDefault(
                                    player.getUniqueId(),
                                    GuiMode.ALL
                            )
                    );

            int totalPages =
                    Math.max(
                            1,
                            (int) Math.ceil(
                                    auctions.size()
                                            / (double) PAGE_SIZE
                            )
                    );

            if (page + 1 < totalPages) {

                pages.put(
                        player.getUniqueId(),
                        page + 1
                );

                openCurrent(
                        player
                );
            }

            return;
        }

        // =====================================================
        // AUCTION ITEM
        // =====================================================

        if (slot < AUCTION_SLOTS) {

            UUID uuid =
                    player.getUniqueId();

            GuiMode mode =
                    modes.getOrDefault(
                            uuid,
                            GuiMode.ALL
                    );

            List<Auction> auctions =
                    getAuctionsForPlayer(
                            player,
                            mode
                    );

            int page =
                    pages.getOrDefault(
                            uuid,
                            0
                    );

            int index =
                    page * PAGE_SIZE + slot;

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
    // DRAG
    // =========================================================

    @EventHandler
    public void onDrag(
            InventoryDragEvent event
    ) {

        if (event.getView()
                .getTitle()
                .equals(title)) {

            event.setCancelled(true);
        }
    }

    // =========================================================
    // CHANGE MODE
    // =========================================================

    private void setMode(
            Player player,
            GuiMode mode
    ) {

        UUID uuid =
                player.getUniqueId();

        modes.put(
                uuid,
                mode
        );

        pages.put(
                uuid,
                0
        );

        /*
         * Keep search text stored.
         * It will be used when SEARCH
         * mode is selected again.
         */

        openCurrent(
                player
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

            openCurrent(
                    buyer
            );

            return;
        }

        // =====================================================
        // OWN AUCTION
        // =====================================================

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

        // =====================================================
        // MONEY
        // =====================================================

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

        // =====================================================
        // INVENTORY
        // =====================================================

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
        // SELLER MONEY
        // =====================================================

        plugin.getEconomy()
                .depositPlayer(
                        seller,
                        price
                );

        // =====================================================
        // ITEM
        // =====================================================

        HashMap<Integer, ItemStack> leftover =
                buyer.getInventory()
                        .addItem(
                                current.getItem()
                        );

        /*
         * This should normally be empty because
         * hasInventorySpace() was checked.
         */

        if (!leftover.isEmpty()) {

            /*
             * Safety:
             * refund the buyer if item couldn't
             * be delivered.
             */

            plugin.getEconomy()
                    .depositPlayer(
                            buyer,
                            price
                    );

            plugin.getEconomy()
                    .withdrawPlayer(
                            seller,
                            price
                    );

            buyer.sendMessage(
                    prefix() +
                            color(
                                    "&cCould not give you the item. Payment refunded."
                            )
            );

            return;
        }

        // =====================================================
        // REMOVE AUCTION
        // =====================================================

        plugin.getAuctionManager()
                .removeAuction(
                        current.getId()
                );

        // =====================================================
        // BUYER MESSAGE
        // =====================================================

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
        // REOPEN SAME PAGE/FILTER
        // =====================================================

        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> {

                            /*
                             * If the current page disappeared
                             * after buying the last item,
                             * openCurrent() automatically
                             * moves to the previous valid page.
                             */

                            openCurrent(
                                    buyer
                            );
                        }
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

        ItemStack[] contents =
                player.getInventory()
                        .getStorageContents();

        for (ItemStack content :
                contents) {

            if (content == null ||
                    content.getType() == Material.AIR) {

                remaining -=
                        item.getMaxStackSize();

            } else if (content.isSimilar(item)) {

                int free =
                        content.getMaxStackSize()
                                - content.getAmount();

                remaining -= free;
            }

            if (remaining <= 0) {
                return true;
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

        return plugin.color(
                text
        );
    }
}
