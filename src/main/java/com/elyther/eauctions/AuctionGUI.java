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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AuctionGUI implements Listener {

    private final EAuctions plugin;

    private final String title =
            ChatColor.DARK_PURPLE + "EAuctions";

    private final Map<UUID, Integer> pages =
            new HashMap<>();

    private final Map<UUID, SortMode> sortModes =
            new HashMap<>();

    private final Map<UUID, Boolean> ownOnly =
            new HashMap<>();

    public AuctionGUI(EAuctions plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        openSearch(player, "");
    }

    public void openSearch(Player player, String search) {

        if (search == null) {
            search = "";
        }

        pages.put(player.getUniqueId(), 0);
        ownOnly.put(player.getUniqueId(), false);

        if (!sortModes.containsKey(player.getUniqueId())) {
            sortModes.put(
                    player.getUniqueId(),
                    SortMode.NEWEST
            );
        }

        openPage(player, search);
    }

    private void openPage(
            Player player,
            String search
    ) {

        UUID uuid = player.getUniqueId();

        int page =
                pages.getOrDefault(uuid, 0);

        SortMode sortMode =
                sortModes.getOrDefault(
                        uuid,
                        SortMode.NEWEST
                );

        boolean showOwn =
                ownOnly.getOrDefault(
                        uuid,
                        false
                );

        List<Auction> auctions =
                new ArrayList<>(
                        plugin.getAuctionManager()
                                .search(search)
                );

        // =====================================================
        // ONLY MY AUCTIONS
        // =====================================================

        if (showOwn) {

            UUID playerUUID =
                    player.getUniqueId();

            auctions.removeIf(
                    auction ->
                            !auction.getSeller()
                                    .equals(playerUUID)
            );
        }

        // =====================================================
        // SORT
        // =====================================================

        switch (sortMode) {

            case NEWEST:
                auctions.sort(
                        Comparator.comparingInt(
                                Auction::getId
                        ).reversed()
                );
                break;

            case OLDEST:
                auctions.sort(
                        Comparator.comparingInt(
                                Auction::getId
                        )
                );
                break;

            case CHEAPEST:
                auctions.sort(
                        Comparator.comparingDouble(
                                Auction::getPrice
                        )
                );
                break;

            case EXPENSIVE:
                auctions.sort(
                        Comparator.comparingDouble(
                                Auction::getPrice
                        ).reversed()
                );
                break;
        }

        // =====================================================
        // PAGE
        // =====================================================

        int maxPages =
                Math.max(
                        1,
                        (int) Math.ceil(
                                auctions.size() / 45.0
                        )
                );

        if (page >= maxPages) {
            page = maxPages - 1;
            pages.put(uuid, page);
        }

        if (page < 0) {
            page = 0;
            pages.put(uuid, page);
        }

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
                page * 45;

        int end =
                Math.min(
                        start + 45,
                        auctions.size()
                );

        int slot = 0;

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
                        color(
                                "&eClick to buy!"
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

        ItemStack filler =
                createItem(
                        Material.GRAY_STAINED_GLASS_PANE,
                        " "
                );

        for (int i = 45; i < 54; i++) {
            inventory.setItem(
                    i,
                    filler
            );
        }

        // =====================================================
        // SLOT 45 - PREVIOUS
        // =====================================================

        ItemStack previous =
                createItem(
                        Material.ARROW,
                        "&e&l← Geri",
                        "",
                        "&7Səhifə: &f" +
                                (page + 1) +
                                "&7/&f" +
                                maxPages
                );

        inventory.setItem(
                45,
                previous
        );

        // =====================================================
        // SLOT 48 - COMMANDS
        // =====================================================

        ItemStack commands =
                createItem(
                        Material.BOOK,
                        "&d&lEAuctions Komutları",
                        "",
                        "&e/ah",
                        "&7Auction House açır.",
                        "",
                        "&e/ah sell <qiymət>",
                        "&7Əlindəki əşyanı satışa qoyur.",
                        "",
                        "&e/ah <əşy adı>",
                        "&7Əşya axtarır."
                );

        inventory.setItem(
                48,
                commands
        );

        // =====================================================
        // SLOT 49 - SORT
        // =====================================================

        ItemStack sort =
                createItem(
                        Material.HOPPER,
                        "&6&lSıralama",
                        "",
                        sortMode == SortMode.NEWEST
                                ? "&e● &fYenidən köhnəyə"
                                : "&7○ &fYenidən köhnəyə",
                        sortMode == SortMode.OLDEST
                                ? "&e● &fKöhnədən təzəyə"
                                : "&7○ &fKöhnədən təzəyə",
                        sortMode == SortMode.CHEAPEST
                                ? "&e● &fAzdan çoxa"
                                : "&7○ &fAzdan çoxa",
                        sortMode == SortMode.EXPENSIVE
                                ? "&e● &fÇoxdan aza"
                                : "&7○ &fÇoxdan aza",
                        "",
                        "&eKliklə dəyiş!"
                );

        inventory.setItem(
                49,
                sort
        );

        // =====================================================
        // SLOT 50 - SEARCH
        // =====================================================

        String currentSearch =
                search == null ||
                        search.isBlank()
                        ? "Hamısı"
                        : search;

        ItemStack searchItem =
                createItem(
                        Material.OAK_SIGN,
                        "&b&lAxtarış",
                        "",
                        "&7Axtarış: &f" +
                                currentSearch,
                        "",
                        "&eKliklə axtar!"
                );

        inventory.setItem(
                50,
                searchItem
        );

        // =====================================================
        // SLOT 51 - MY AUCTIONS
        // =====================================================

        ItemStack myAuctions;

        if (showOwn) {

            myAuctions =
                    createItem(
                            Material.ENDER_CHEST,
                            "&a&lMənim Satışlarım",
                            "",
                            "&aHazırda öz satışlarını görürsən.",
                            "",
                            "&eKliklə bütün satışlara qayıt!"
                    );

        } else {

            myAuctions =
                    createItem(
                            Material.ENDER_CHEST,
                            "&a&lMənim Satışlarım",
                            "",
                            "&7Sənin satışa qoyduğun",
                            "&7əşyaları göstərir.",
                            "",
                            "&eKliklə aç!"
                    );
        }

        inventory.setItem(
                51,
                myAuctions
        );

        // =====================================================
        // SLOT 53 - NEXT
        // =====================================================

        ItemStack next =
                createItem(
                        Material.ARROW,
                        "&e&lİrəli →",
                        "",
                        "&7Səhifə: &f" +
                                (page + 1) +
                                "&7/&f" +
                                maxPages
                );

        inventory.setItem(
                53,
                next
        );

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
                instanceof Player)) {
            return;
        }

        Player player =
                (Player) event.getWhoClicked();

        if (!event.getView()
                .getTitle()
                .equals(title)) {
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
        // MY AUCTIONS
        // =====================================================

        if (slot == 51) {

            boolean current =
                    ownOnly.getOrDefault(
                            uuid,
                            false
                    );

            ownOnly.put(
                    uuid,
                    !current
            );

            pages.put(
                    uuid,
                    0
            );

            String searchText =
                    "";

            AuctionSearch search =
                    plugin.getAuctionSearch();

            if (search != null) {
                searchText =
                        search.getCurrentSearch(
                                player
                        );
            }

            openPage(
                    player,
                    searchText
            );

            return;
        }

        // =====================================================
        // SORT
        // =====================================================

        if (slot == 49) {

            SortMode current =
                    sortModes.getOrDefault(
                            uuid,
                            SortMode.NEWEST
                    );

            SortMode next;

            switch (current) {

                case NEWEST:
                    next = SortMode.OLDEST;
                    break;

                case OLDEST:
                    next = SortMode.CHEAPEST;
                    break;

                case CHEAPEST:
                    next = SortMode.EXPENSIVE;
                    break;

                default:
                    next = SortMode.NEWEST;
                    break;
            }

            sortModes.put(
                    uuid,
                    next
            );

            pages.put(
                    uuid,
                    0
            );

            String searchText =
                    getSearch(player);

            openPage(
                    player,
                    searchText
            );

            return;
        }

        // =====================================================
        // COMMANDS
        // =====================================================

        if (slot == 48) {

            player.sendMessage(
                    color("&8&m--------------------------")
            );

            player.sendMessage(
                    color("&d&lEAuctions &7Komutlar")
            );

            player.sendMessage(
                    color("&e/ah &7- Auction House")
            );

            player.sendMessage(
                    color("&e/ah sell <qiymət> &7- Satış")
            );

            player.sendMessage(
                    color("&e/ah <əşy adı> &7- Axtarış")
            );

            player.sendMessage(
                    color("&8&m--------------------------")
            );

            return;
        }

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

                openPage(
                        player,
                        getSearch(player)
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
                            uuid,
                            0
                    );

            List<Auction> auctions =
                    getDisplayedAuctions(
                            player
                    );

            int maxPages =
                    Math.max(
                            1,
                            (int) Math.ceil(
                                    auctions.size() / 45.0
                            )
                    );

            if (page + 1 < maxPages) {

                pages.put(
                        uuid,
                        page + 1
                );

                openPage(
                        player,
                        getSearch(player)
                );
            }

            return;
        }

        // =====================================================
        // AUCTION ITEM
        // =====================================================

        if (slot >= 45) {
            return;
        }

        List<Auction> auctions =
                getDisplayedAuctions(
                        player
                );

        int page =
                pages.getOrDefault(
                        uuid,
                        0
                );

        int index =
                page * 45 + slot;

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
    // GET DISPLAYED AUCTIONS
    // =========================================================

    private List<Auction> getDisplayedAuctions(
            Player player
    ) {

        String search =
                getSearch(player);

        UUID uuid =
                player.getUniqueId();

        List<Auction> auctions =
                new ArrayList<>(
                        plugin.getAuctionManager()
                                .search(search)
                );

        if (ownOnly.getOrDefault(uuid, false)) {

            auctions.removeIf(
                    auction ->
                            !auction.getSeller()
                                    .equals(uuid)
            );
        }

        SortMode mode =
                sortModes.getOrDefault(
                        uuid,
                        SortMode.NEWEST
                );

        switch (mode) {

            case NEWEST:
                auctions.sort(
                        Comparator.comparingInt(
                                Auction::getId
                        ).reversed()
                );
                break;

            case OLDEST:
                auctions.sort(
                        Comparator.comparingInt(
                                Auction::getId
                        )
                );
                break;

            case CHEAPEST:
                auctions.sort(
                        Comparator.comparingDouble(
                                Auction::getPrice
                        )
                );
                break;

            case EXPENSIVE:
                auctions.sort(
                        Comparator.comparingDouble(
                                Auction::getPrice
                        ).reversed()
                );
                break;
        }

        return auctions;
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

        return search.getCurrentSearch(
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
                                    "&cBu satış artıq mövcud deyil."
                            )
            );

            openPage(
                    buyer,
                    getSearch(buyer)
            );

            return;
        }

        if (current.getSeller()
                .equals(buyer.getUniqueId())) {

            buyer.sendMessage(
                    prefix() +
                            color(
                                    "&cÖz əşyanı ala bilməzsən."
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
                                    "&cKifayət qədər pulun yoxdur."
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
                                    "&cİnventarın doludur."
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
                                    "&cÖdəniş uğursuz oldu."
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
                                "&aƏşyanı &f$" +
                                        plugin.formatMoney(
                                                price
                                        ) +
                                        " &aqiymətinə aldın."
                        )
        );

        if (seller.isOnline()) {

            Player sellerPlayer =
                    seller.getPlayer();

            if (sellerPlayer != null) {

                sellerPlayer.sendMessage(
                        prefix() +
                                color(
                                        "&aƏşyan satıldı! Məbləğ: &f$" +
                                                plugin.formatMoney(
                                                        price
                                                )
                                )
                );
            }
        }

        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> openPage(
                                buyer,
                                getSearch(buyer)
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

        int amount =
                item.getAmount();

        for (ItemStack content :
                player.getInventory()
                        .getStorageContents()) {

            if (content == null ||
                    content.getType() == Material.AIR) {

                return true;
            }

            if (content.isSimilar(item)) {

                int free =
                        content.getMaxStackSize()
                                - content.getAmount();

                amount -= free;

                if (amount <= 0) {
                    return true;
                }
            }
        }

        return false;
    }

    // =========================================================
    // SELLER
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

    // =========================================================
    // SORT MODE
    // =========================================================

    private enum SortMode {

        NEWEST,
        OLDEST,
        CHEAPEST,
        EXPENSIVE
    }
}
