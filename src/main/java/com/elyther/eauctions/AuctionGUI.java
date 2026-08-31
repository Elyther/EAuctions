package com.elyther.eauctions;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionGUI implements Listener {

    private final EAuctions plugin;

    private static final String TITLE =
            "§d§lEAuctions";

    private static final String CONFIRM_TITLE =
            "§d§lConfirm Purchase";

    private final java.util.Map<UUID, String> searches =
            new java.util.HashMap<>();

    private final java.util.Map<UUID, Integer> pages =
            new java.util.HashMap<>();

    public AuctionGUI(EAuctions plugin) {
        this.plugin = plugin;
    }

    // =========================================================
    // OPEN AUCTION HOUSE
    // =========================================================

    public void open(Player player) {
        openSearch(player, "");
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
                search
        );

        pages.put(
                player.getUniqueId(),
                0
        );

        openPage(
                player,
                search,
                0
        );
    }

    // =========================================================
    // OPEN PAGE
    // =========================================================

    private void openPage(
            Player player,
            String search,
            int page
    ) {

        List<AuctionManager.Auction> auctions;

        if (search == null ||
                search.isBlank()) {

            auctions =
                    plugin.getAuctionManager()
                            .getAuctions();

        } else {

            auctions =
                    plugin.getAuctionManager()
                            .search(search);
        }

        int size = 54;

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        size,
                        TITLE
                );

        /*
         * Item slots:
         *
         * 0 - 44
         *
         * Bottom row:
         *
         * 45 Previous
         * 46-48 decoration
         * 49 Sell
         * 50-51 decoration
         * 52 Next
         * 53 Close
         */

        int itemsPerPage = 45;

        int start =
                page * itemsPerPage;

        int end =
                Math.min(
                        start + itemsPerPage,
                        auctions.size()
                );

        int slot = 0;

        for (int i = start;
             i < end;
             i++) {

            AuctionManager.Auction auction =
                    auctions.get(i);

            ItemStack display =
                    auction.getItem();

            ItemMeta meta =
                    display.getItemMeta();

            if (meta != null) {

                List<String> lore =
                        new ArrayList<>();

                if (meta.hasLore() &&
                        meta.getLore() != null) {

                    lore.addAll(
                            meta.getLore()
                    );
                }

                lore.add("");

                OfflinePlayer seller =
                        Bukkit.getOfflinePlayer(
                                auction.getSeller()
                        );

                String sellerName =
                        seller.getName();

                if (sellerName == null) {
                    sellerName = "Unknown";
                }

                String currency =
                        plugin.getConfig()
                                .getString(
                                        "auction.currency-symbol",
                                        "$"
                                );

                String price =
                        currency +
                                plugin.formatMoney(
                                        auction.getPrice()
                                );

                lore.add(
                        color(
                                "&7Seller: &f" +
                                        sellerName
                        )
                );

                lore.add(
                        color(
                                "&7Price: &a" +
                                        price
                        )
                );

                lore.add("");

                lore.add(
                        color(
                                "&eClick to buy"
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
        // PREVIOUS
        // =====================================================

        if (page > 0) {

            inventory.setItem(
                    45,
                    createItem(
                            Material.ARROW,
                            "&ePrevious Page",
                            "&7Click to go back."
                    )
            );
        }

        // =====================================================
        // SELL
        // =====================================================

        inventory.setItem(
                49,
                createItem(
                        Material.EMERALD,
                        "&aSell Item",
                        "&7Hold an item and use:",
                        "&f/ah sell <price>",
                        "",
                        "&7Examples:",
                        "&f/ah sell 5k",
                        "&f/ah sell 2.5m"
                )
        );

        // =====================================================
        // NEXT
        // =====================================================

        if (end < auctions.size()) {

            inventory.setItem(
                    52,
                    createItem(
                            Material.ARROW,
                            "&eNext Page",
                            "&7Click to see more auctions."
                    )
            );
        }

        // =====================================================
        // CLOSE
        // =====================================================

        inventory.setItem(
                53,
                createItem(
                        Material.BARRIER,
                        "&cClose",
                        "&7Close Auction House."
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

        if (!event.getView()
                .getTitle()
                .equals(TITLE)) {

            if (event.getView()
                    .getTitle()
                    .equals(CONFIRM_TITLE)) {

                handleConfirmClick(event);
            }

            return;
        }

        event.setCancelled(true);

        if (!(event.getWhoClicked()
                instanceof Player player)) {

            return;
        }

        int slot =
                event.getRawSlot();

        if (slot < 0 ||
                slot >= event.getView()
                        .getTopInventory()
                        .getSize()) {

            return;
        }

        // =====================================================
        // PREVIOUS
        // =====================================================

        if (slot == 45) {

            int current =
                    pages.getOrDefault(
                            player.getUniqueId(),
                            0
                    );

            if (current > 0) {

                current--;

                pages.put(
                        player.getUniqueId(),
                        current
                );

                openPage(
                        player,
                        searches.getOrDefault(
                                player.getUniqueId(),
                                ""
                        ),
                        current
                );
            }

            return;
        }

        // =====================================================
        // SELL
        // =====================================================

        if (slot == 49) {

            player.closeInventory();

            player.sendMessage(
                    color(
                            "&d&lEAuctions &8» &7Hold an item and use:"
                    )
            );

            player.sendMessage(
                    color(
                            "&f/ah sell <price>"
                    )
            );

            player.sendMessage(
                    color(
                            "&7Examples: &f5k &7| &f2.5k &7| &f1m"
                    )
            );

            return;
        }

        // =====================================================
        // NEXT
        // =====================================================

        if (slot == 52) {

            int current =
                    pages.getOrDefault(
                            player.getUniqueId(),
                            0
                    );

            String search =
                    searches.getOrDefault(
                            player.getUniqueId(),
                            ""
                    );

            List<AuctionManager.Auction> auctions;

            if (search.isBlank()) {

                auctions =
                        plugin.getAuctionManager()
                                .getAuctions();

            } else {

                auctions =
                        plugin.getAuctionManager()
                                .search(search);
            }

            int maxPage =
                    Math.max(
                            0,
                            (auctions.size() - 1)
                                    / 45
                    );

            if (current < maxPage) {

                current++;

                pages.put(
                        player.getUniqueId(),
                        current
                );

                openPage(
                        player,
                        search,
                        current
                );
            }

            return;
        }

        // =====================================================
        // CLOSE
        // =====================================================

        if (slot == 53) {

            player.closeInventory();
            return;
        }

        // =====================================================
        // AUCTION ITEM
        // =====================================================

        if (slot >= 0 &&
                slot < 45) {

            String search =
                    searches.getOrDefault(
                            player.getUniqueId(),
                            ""
                    );

            List<AuctionManager.Auction> auctions;

            if (search.isBlank()) {

                auctions =
                        plugin.getAuctionManager()
                                .getAuctions();

            } else {

                auctions =
                        plugin.getAuctionManager()
                                .search(search);
            }

            int page =
                    pages.getOrDefault(
                            player.getUniqueId(),
                            0
                    );

            int index =
                    page * 45 + slot;

            if (index >= auctions.size()) {
                return;
            }

            AuctionManager.Auction auction =
                    auctions.get(index);

            openConfirm(
                    player,
                    auction
            );
        }
    }

    // =========================================================
    // CONFIRM GUI
    // =========================================================

    private void openConfirm(
            Player player,
            AuctionManager.Auction auction
    ) {

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        27,
                        CONFIRM_TITLE
                );

        ItemStack item =
                auction.getItem();

        inventory.setItem(
                13,
                item
        );

        inventory.setItem(
                11,
                createItem(
                        Material.LIME_CONCRETE,
                        "&aBUY",
                        "&7Price: &a" +
                                plugin.getConfig()
                                        .getString(
                                                "auction.currency-symbol",
                                                "$"
                                        ) +
                                plugin.formatMoney(
                                        auction.getPrice()
                                ),
                        "",
                        "&eClick to purchase"
                )
        );

        inventory.setItem(
                15,
                createItem(
                        Material.RED_CONCRETE,
                        "&cCANCEL",
                        "&7Cancel purchase."
                )
        );

        /*
         * Store auction ID in player's
         * temporary metadata.
         */

        player.setMetadata(
                "eauction_confirm",
                new org.bukkit.metadata.FixedMetadataValue(
                        plugin,
                        auction.getId().toString()
                )
        );

        player.openInventory(inventory);
    }

    // =========================================================
    // CONFIRM CLICK
    // =========================================================

    private void handleConfirmClick(
            InventoryClickEvent event
    ) {

        event.setCancelled(true);

        if (!(event.getWhoClicked()
                instanceof Player player)) {

            return;
        }

        int slot =
                event.getRawSlot();

        // Cancel
        if (slot == 15) {

            removeConfirmMetadata(player);

            open(player);

            return;
        }

        // Buy
        if (slot != 11) {
            return;
        }

        if (!player.hasMetadata(
                "eauction_confirm"
        )) {

            player.closeInventory();
            return;
        }

        String idString =
                player.getMetadata(
                        "eauction_confirm"
                )
                .get(0)
                .asString();

        UUID id;

        try {

            id =
                    UUID.fromString(
                            idString
                    );

        } catch (IllegalArgumentException e) {

            player.closeInventory();
            return;
        }

        AuctionManager.Auction auction =
                plugin.getAuctionManager()
                        .find(id);

        if (auction == null) {

            removeConfirmMetadata(player);

            player.sendMessage(
                    color(
                            "&d&lEAuctions &8» &cThis auction no longer exists."
                    )
            );

            open(player);

            return;
        }

        // =====================================================
        // CANNOT BUY OWN ITEM
        // =====================================================

        if (auction.getSeller()
                .equals(
                        player.getUniqueId()
                )) {

            player.sendMessage(
                    color(
                            "&d&lEAuctions &8» &cYou cannot buy your own item."
                    )
            );

            return;
        }

        double price =
                auction.getPrice();

        // =====================================================
        // CHECK MONEY
        // =====================================================

        if (!plugin.getEconomy()
                .has(
                        player,
                        price
                )) {

            player.sendMessage(
                    color(
                            "&d&lEAuctions &8» &cYou don't have enough money."
                    )
            );

            return;
        }

        // =====================================================
        // REMOVE MONEY
        // =====================================================

        var withdraw =
                plugin.getEconomy()
                        .withdrawPlayer(
                                player,
                                price
                        );

        if (!withdraw.transactionSuccess()) {

            player.sendMessage(
                    color(
                            "&d&lEAuctions &8» &cPayment failed."
                    )
            );

            return;
        }

        // =====================================================
        // GIVE MONEY TO SELLER
        // =====================================================

        OfflinePlayer seller =
                Bukkit.getOfflinePlayer(
                        auction.getSeller()
                );

        plugin.getEconomy()
                .depositPlayer(
                        seller,
                        price
                );

        // =====================================================
        // GIVE ITEM TO BUYER
        // =====================================================

        ItemStack item =
                auction.getItem();

        java.util.HashMap<Integer, ItemStack> leftover =
                player.getInventory()
                        .addItem(item);

        /*
         * If inventory is full, return the item
         * and refund the player.
         */

        if (!leftover.isEmpty()) {

            plugin.getEconomy()
                    .depositPlayer(
                            player,
                            price
                    );

            plugin.getEconomy()
                    .withdrawPlayer(
                            seller,
                            price
                    );

            player.sendMessage(
                    color(
                            "&d&lEAuctions &8» &cYour inventory is full."
                    )
            );

            return;
        }

        // =====================================================
        // REMOVE AUCTION
        // =====================================================

        plugin.getAuctionManager()
                .removeAuction(
                        auction
                );

        removeConfirmMetadata(player);

        // =====================================================
        // BUYER MESSAGE
        // =====================================================

        String currency =
                plugin.getConfig()
                        .getString(
                                "auction.currency-symbol",
                                "$"
                        );

        String formatted =
                currency +
                        plugin.formatMoney(
                                price
                        );

        player.sendMessage(
                color(
                        "&d&lEAuctions &8» &aYou bought the item for &f"
                                + formatted
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
                        color(
                                "&d&lEAuctions &8» &aYour item was sold for &f"
                                        + formatted
                        )
                );
            }
        }

        player.closeInventory();

        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> open(player)
                );
    }

    // =========================================================
    // REMOVE METADATA
    // =========================================================

    private void removeConfirmMetadata(
            Player player
    ) {

        if (player.hasMetadata(
                "eauction_confirm"
        )) {

            player.removeMetadata(
                    "eauction_confirm",
                    plugin
            );
        }
    }

    // =========================================================
    // CLOSE
    // =========================================================

    @EventHandler
    public void onClose(
            InventoryCloseEvent event
    ) {

        if (!(event.getPlayer()
                instanceof Player player)) {

            return;
        }

        if (event.getView()
                .getTitle()
                .equals(CONFIRM_TITLE)) {

            removeConfirmMetadata(player);
        }
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

            List<String> loreList =
                    new ArrayList<>();

            for (String line : lore) {

                loreList.add(
                        color(line)
                );
            }

            meta.setLore(
                    loreList
            );

            item.setItemMeta(meta);
        }

        return item;
    }

    // =========================================================
    // COLOR
    // =========================================================

    private String color(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return plugin.color(text);
    }
}
