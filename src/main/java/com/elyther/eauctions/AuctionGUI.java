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

    private static final int AUCTION_SLOTS = 45;

    private final Map<UUID, Integer> pages = new HashMap<>();
    private final Map<UUID, SortType> sorts = new HashMap<>();
    private final Map<UUID, Boolean> myAuctions = new HashMap<>();
    private final Map<UUID, Boolean> purchaseConfirmation = new HashMap<>();
    private final Map<UUID, Integer> pendingPurchases = new HashMap<>();

    public AuctionGUI(EAuctions plugin) {
        this.plugin = plugin;
    }

    // =========================================================
    // OPEN
    // =========================================================

    public void open(Player player) {

        UUID uuid = player.getUniqueId();

        pages.put(uuid, 0);
        sorts.put(uuid, SortType.NEWEST);
        myAuctions.put(uuid, false);

        openSearch(player, "");
    }

    // =========================================================
    // OPEN SEARCH
    // =========================================================

    public void openSearch(Player player, String search) {

        UUID uuid = player.getUniqueId();

        pages.put(uuid, 0);

        openGUI(
                player,
                search == null ? "" : search
        );
    }

    // =========================================================
    // OPEN GUI
    // =========================================================

    private void openGUI(Player player, String search) {

        UUID uuid = player.getUniqueId();

        String query =
                search == null ? "" : search.trim();

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

        List<Auction> auctions =
                getCurrentAuctions(
                        player,
                        query
                );

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        54,
                        plugin.color(
                                plugin.lang("gui.title")
                        )
                );

        int page =
                pages.getOrDefault(uuid, 0);

        int start =
                page * AUCTION_SLOTS;

        int end =
                Math.min(
                        start + AUCTION_SLOTS,
                        auctions.size()
                );

        int slot = 0;

        for (int i = start; i < end; i++) {

            Auction auction = auctions.get(i);

            if (auction == null ||
                    auction.getItem() == null ||
                    auction.getItem().getType().isAir()) {
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
                                ? new ArrayList<>(meta.getLore())
                                : new ArrayList<>();

                lore.add("");

                lore.add(
                        color("&8&m----------------")
                );

                lore.add(
                        color(
                                lang(
                                        "gui.auction.seller"
                                ).replace(
                                        "%seller%",
                                        getSellerName(auction)
                                )
                        )
                );

                lore.add(
                        color(
                                lang(
                                        "gui.auction.price"
                                ).replace(
                                        "%price%",
                                        plugin.formatMoney(
                                                auction.getPrice()
                                        )
                                )
                        )
                );

                lore.add("");

                if (onlyMine) {

                    lore.add(
                            color(
                                    lang(
                                            "gui.auction.remove"
                                    )
                            )
                    );

                } else {

                    lore.add(
                            color(
                                    lang(
                                            "gui.auction.buy"
                                    )
                            )
                    );
                }

                lore.add(
                        color(
                                lang(
                                        "gui.auction.id"
                                ).replace(
                                        "%id%",
                                        String.valueOf(
                                                auction.getId()
                                        )
                                )
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

        inventory.setItem(46, glass);
        inventory.setItem(52, glass);

        // =====================================================
        // PREVIOUS
        // =====================================================

        if (page > 0) {

            inventory.setItem(
                    45,
                    createItem(
                            Material.ARROW,
                            lang("gui.previous.name"),
                            "",
                            lang("gui.previous.lore")
                    )
            );

        } else {

            inventory.setItem(45, glass);
        }

        // =====================================================
        // COMMANDS
        // =====================================================

        inventory.setItem(
                47,
                createItem(
                        Material.BOOK,
                        lang("gui.commands.title"),
                        "",
                        lang("gui.commands.open"),
                        lang("gui.commands.open-description"),
                        "",
                        lang("gui.commands.sell"),
                        lang("gui.commands.sell-description"),
                        "",
                        lang("gui.commands.reload"),
                        lang("gui.commands.reload-description")
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
                            lang("gui.my-auctions.viewing-name"),
                            "",
                            lang("gui.my-auctions.viewing"),
                            lang("gui.my-auctions.your-auctions"),
                            "",
                            lang("gui.my-auctions.all")
                    )
            );

        } else {

            inventory.setItem(
                    48,
                    createItem(
                            Material.ENDER_CHEST,
                            lang("gui.my-auctions.name"),
                            "",
                            lang("gui.my-auctions.description"),
                            lang("gui.my-auctions.description-2"),
                            "",
                            lang("gui.my-auctions.open")
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
                        lang("gui.sort.name"),
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
                        lang("gui.sort.click")
                )
        );

        // =====================================================
        // SEARCH
        // =====================================================

        String searchDisplay =
                query.isEmpty()
                        ? lang("gui.search.all-items")
                        : query;

        inventory.setItem(
                50,
                createItem(
                        Material.OAK_SIGN,
                        lang("gui.search.name"),
                        "",
                        lang("gui.search.current"),
                        "&f" + searchDisplay,
                        "",
                        lang("gui.search.click")
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

        inventory.setItem(
                51,
                createItem(
                        confirmation
                                ? Material.LIME_DYE
                                : Material.GRAY_DYE,

                        lang("gui.confirmation-toggle.name"),

                        "",

                        lang(
                                confirmation
                                        ? "gui.confirmation-toggle.enabled"
                                        : "gui.confirmation-toggle.disabled"
                        ),

                        "",

                        lang(
                                confirmation
                                        ? "gui.confirmation-toggle.enabled-description"
                                        : "gui.confirmation-toggle.disabled-description"
                        ),

                        "",

                        lang("gui.confirmation-toggle.click")
                )
        );

        // =====================================================
        // NEXT PAGE
        // =====================================================

        if (end < auctions.size()) {

            inventory.setItem(
                    53,
                    createItem(
                            Material.ARROW,
                            lang("gui.next.name"),
                            "",
                            lang("gui.next.lore")
                    )
            );

        } else {

            inventory.setItem(53, glass);
        }

        player.openInventory(inventory);
    }

    // =========================================================
    // SORT LINE
    // =========================================================

    private String getSortLine(
            SortType type,
            SortType current
    ) {

        String path;

        switch (type) {

            case NEWEST:
                path = "gui.sort.newest";
                break;

            case OLDEST:
                path = "gui.sort.oldest";
                break;

            case CHEAPEST:
                path = "gui.sort.cheapest";
                break;

            case MOST_EXPENSIVE:
                path = "gui.sort.expensive";
                break;

            default:
                path = "gui.sort.newest";
        }

        String prefix =
                type == current
                        ? lang("gui.sort.selected")
                        : lang("gui.sort.not-selected");

        return prefix + lang(path);
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

        String clickedTitle =
                event.getView().getTitle();

        // =====================================================
        // CONFIRMATION GUI
        // =====================================================

        String confirmationTitle =
                color(
                        lang("gui.confirmation.title")
                );

        if (clickedTitle.equals(confirmationTitle)) {

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
                                .getAuction(auctionId);

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

        String guiTitle =
                color(
                        lang("gui.title")
                );

        if (!clickedTitle.equals(guiTitle)) {
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
                    pages.getOrDefault(uuid, 0);

            if (page > 0) {

                pages.put(uuid, page - 1);

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

            pages.put(uuid, 0);

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

            sorts.put(
                    uuid,
                    getNextSort(current)
            );

            pages.put(uuid, 0);

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
                    pages.getOrDefault(uuid, 0);

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
                    pages.getOrDefault(uuid, 0);

            int index =
                    page * AUCTION_SLOTS + slot;

            if (index < 0 ||
                    index >= auctions.size()) {
                return;
            }

            Auction auction =
                    auctions.get(index);

            if (auction == null) {
                return;
            }

            // =================================================
            // OWN AUCTION
            // =================================================

            if (auction.getSeller()
                    .equals(uuid)) {

                removeAuction(
                        player,
                        auction
                );

                return;
            }

            buy(
                    player,
                    auction
            );
        }
    }

    // =========================================================
    // REMOVE OWN AUCTION
    // =========================================================

    private void removeAuction(
            Player player,
            Auction auction
    ) {

        Auction current =
                plugin.getAuctionManager()
                        .getAuction(
                                auction.getId()
                        );

        if (current == null) {

            player.sendMessage(
                    prefix() +
                            lang(
                                    "messages.no-longer-exists"
                            )
            );

            refresh(player);
            return;
        }

        if (!current.getSeller()
                .equals(player.getUniqueId())) {

            player.sendMessage(
                    prefix() +
                            lang(
                                    "messages.remove-failed"
                            )
            );

            return;
        }

        if (!hasInventorySpace(
                player,
                current.getItem()
        )) {

            player.sendMessage(
                    prefix() +
                            lang(
                                    "messages.inventory-full"
                            )
            );

            return;
        }

        if (!plugin.getAuctionManager()
                .removeAuction(
                        current.getId()
                )) {

            player.sendMessage(
                    prefix() +
                            lang(
                                    "messages.remove-failed"
                            )
            );

            return;
        }

        HashMap<Integer, ItemStack> leftover =
                player.getInventory()
                        .addItem(
                                current.getItem().clone()
                        );

        if (!leftover.isEmpty()) {

            player.sendMessage(
                    prefix() +
                            lang(
                                    "messages.item-add-failed"
                            )
            );

            return;
        }

        player.sendMessage(
                prefix() +
                        lang(
                                "messages.removed"
                        )
        );

        player.sendMessage(
                prefix() +
                        lang(
                                "messages.returned"
                        )
        );

        refresh(player);
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
                        color(
                                lang(
                                        "gui.confirmation.title"
                                )
                        )
                );

        inventory.setItem(
                11,
                createItem(
                        Material.LIME_WOOL,
                        lang(
                                "gui.confirmation.confirm"
                        ),
                        "",
                        lang(
                                "gui.confirmation.confirm-lore"
                        )
                )
        );

        inventory.setItem(
                15,
                createItem(
                        Material.RED_WOOL,
                        lang(
                                "gui.confirmation.cancel"
                        ),
                        "",
                        lang(
                                "gui.confirmation.cancel-lore"
                        )
                )
        );

        inventory.setItem(
                13,
                createItem(
                        auction.getItem().getType(),
                        lang(
                                "gui.confirmation.item-title"
                        ),
                        "",
                        lang(
                                "gui.confirmation.item"
                        ).replace(
                                "%item%",
                                auction.getItem()
                                        .getType()
                                        .name()
                        ),
                        lang(
                                "gui.confirmation.price"
                        ).replace(
                                "%price%",
                                plugin.formatMoney(
                                        auction.getPrice()
                                )
                        ),
                        "",
                        lang(
                                "gui.confirmation.question-1"
                        ),
                        lang(
                                "gui.confirmation.question-2"
                        )
                )
        );

        player.openInventory(inventory);
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
                            lang(
                                    "messages.no-longer-exists"
                            )
            );

            refresh(buyer);
            return;
        }

        if (current.getSeller()
                .equals(buyer.getUniqueId())) {

            buyer.sendMessage(
                    prefix() +
                            lang(
                                    "messages.own-item"
                            )
            );

            return;
        }

        double price =
                current.getPrice();

        if (!plugin.getEconomy()
                .has(buyer, price)) {

            buyer.sendMessage(
                    prefix() +
                            lang(
                                    "messages.not-enough-money"
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
                            lang(
                                    "messages.inventory-full"
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
                            lang(
                                    "messages.payment-failed"
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
                            lang(
                                    "messages.seller-payment-failed"
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
                            lang(
                                    "messages.item-add-failed"
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
                        lang(
                                "messages.bought"
                        )
                        .replace(
                                "%item%",
                                current.getItem()
                                        .getType()
                                        .name()
                        )
                        .replace(
                                "%price%",
                                plugin.formatMoney(price)
                        )
        );

        if (seller.isOnline()) {

            Player sellerPlayer =
                    seller.getPlayer();

            if (sellerPlayer != null) {

                sellerPlayer.sendMessage(
                        prefix() +
                                lang(
                                        "messages.sold"
                                )
                                .replace(
                                        "%price%",
                                        plugin.formatMoney(price)
                                )
                );
            }
        }

        refresh(buyer);
    }

    // =========================================================
    // SEARCH
    // =========================================================

    private String getSearch(Player player) {

        AuctionSearch search =
                plugin.getAuctionSearch();

        if (search == null) {
            return "";
        }

        String current =
                search.getCurrentSearch(player);

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
                            .getSortedAuctions(sort);

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

    private void refresh(Player player) {

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

    private String lang(String path) {
        return plugin.lang(path);
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

        return plugin.color(
                plugin.lang("prefix")
        );
    }

    // =========================================================
    // COLOR
    // =========================================================

    private String color(String text) {

        return plugin.color(text);
    }
}
