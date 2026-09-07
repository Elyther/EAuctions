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

    /*
     * 7-row GUI
     *
     * Rows 1-2 = auction items
     * Row 3    = categories
     * Row 4    = sorting
     * Row 5    = refresh
     * Row 6    = search
     * Row 7    = my auctions
     */
    private static final int AUCTION_SLOTS = 18;

    private final Map<UUID, Integer> pages = new HashMap<>();
    private final Map<UUID, SortType> sorts = new HashMap<>();
    private final Map<UUID, Boolean> myAuctions = new HashMap<>();

    /*
     * true  = confirmation enabled
     * false = confirmation disabled
     */
    private final Map<UUID, Boolean> purchaseConfirmation = new HashMap<>();

    private final Map<UUID, Integer> pendingPurchases = new HashMap<>();

    /*
     * Selected category for each player.
     */
    private final Map<UUID, AuctionCategory> categories = new HashMap<>();

    /*
     * =========================================================
     * CATEGORIES
     * =========================================================
     */

    public enum AuctionCategory {

        ALL,
        BLOCKS,
        ORES,
        TOOLS,
        WEAPONS,
        ARMOR,
        FOOD,
        REDSTONE,
        FARMING,
        DECORATION,
        OTHER
    }

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
        categories.put(uuid, AuctionCategory.ALL);

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

        AuctionCategory category =
                categories.getOrDefault(
                        uuid,
                        AuctionCategory.ALL
                );

        List<Auction> auctions =
                getCurrentAuctions(
                        player,
                        query
                );

        /*
         * 63 = 7 rows.
         */
        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        63,
                        plugin.color(
                                plugin.lang("gui.title")
                        )
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

        /*
         * =====================================================
         * AUCTION ITEMS
         * =====================================================
         */

        for (int i = start; i < end; i++) {

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
                        color(
                                "&8&m----------------"
                        )
                );

                lore.add(
                        color(
                                lang(
                                        "gui.auction.seller"
                                ).replace(
                                        "%seller%",
                                        getSellerName(
                                                auction
                                        )
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

        /*
         * =====================================================
         * EMPTY SLOTS / SEPARATOR
         * =====================================================
         */

        ItemStack glass =
                createItem(
                        Material.GRAY_STAINED_GLASS_PANE,
                        " "
                );

        /*
         * Row 3 = slots 18-26
         * Row 4 = slots 27-35
         * Row 5 = slots 36-44
         * Row 6 = slots 45-53
         * Row 7 = slots 54-62
         */

        for (int i = 18; i <= 62; i++) {

            inventory.setItem(
                    i,
                    glass
            );
        }

        /*
         * =====================================================
         * ROW 3
         * CATEGORIES
         * =====================================================
         */

        inventory.setItem(
                18 + 4,
                createItem(
                        Material.HOPPER,
                        lang(
                                "gui.categories.name"
                        ),
                        "",
                        lang(
                                "gui.categories.current"
                        ).replace(
                                "%category%",
                                getCategoryName(
                                        category
                                )
                        ),
                        "",
                        lang(
                                "gui.categories.click"
                        )
                )
        );

        /*
         * Previous page.
         */
        if (page > 0) {

            inventory.setItem(
                    18,
                    createItem(
                            Material.ARROW,
                            lang(
                                    "gui.previous.name"
                            ),
                            "",
                            lang(
                                    "gui.previous.lore"
                            )
                    )
            );
        }

        /*
         * Next page.
         */
        if (end < auctions.size()) {

            inventory.setItem(
                    26,
                    createItem(
                            Material.ARROW,
                            lang(
                                    "gui.next.name"
                            ),
                            "",
                            lang(
                                    "gui.next.lore"
                            )
                    )
            );
        }

        /*
         * =====================================================
         * ROW 4
         * SORTING
         * =====================================================
         */

        inventory.setItem(
                27 + 4,
                createItem(
                        Material.ENDER_CHEST,
                        lang(
                                "gui.sort.name"
                        ),
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
                        lang(
                                "gui.sort.click"
                        )
                )
        );

        /*
         * =====================================================
         * ROW 5
         * REFRESH
         * =====================================================
         */

        inventory.setItem(
                36 + 4,
                createItem(
                        Material.ANVIL,
                        lang(
                                "gui.refresh.name"
                        ),
                        "",
                        lang(
                                "gui.refresh.description"
                        ),
                        "",
                        lang(
                                "gui.refresh.click"
                        )
                )
        );

        /*
         * =====================================================
         * ROW 6
         * SEARCH
         * =====================================================
         */

        String searchDisplay =
                query.isEmpty()
                        ? lang(
                                "gui.search.all-items"
                        )
                        : query;

        inventory.setItem(
                45 + 4,
                createItem(
                        Material.OAK_SIGN,
                        lang(
                                "gui.search.name"
                        ),
                        "",
                        lang(
                                "gui.search.current"
                        ),
                        "&f" + searchDisplay,
                        "",
                        lang(
                                "gui.search.click"
                        )
                )
        );

        /*
         * =====================================================
         * ROW 7
         * MY AUCTIONS
         * =====================================================
         */

        if (onlyMine) {

            inventory.setItem(
                    54 + 4,
                    createItem(
                            Material.CHEST,
                            lang(
                                    "gui.my-auctions.viewing-name"
                            ),
                            "",
                            lang(
                                    "gui.my-auctions.viewing"
                            ),
                            lang(
                                    "gui.my-auctions.your-auctions"
                            ),
                            "",
                            lang(
                                    "gui.my-auctions.all"
                            )
                    )
            );

        } else {

            inventory.setItem(
                    54 + 4,
                    createItem(
                            Material.CHEST,
                            lang(
                                    "gui.my-auctions.name"
                            ),
                            "",
                            lang(
                                    "gui.my-auctions.description"
                            ),
                            lang(
                                    "gui.my-auctions.description-2"
                            ),
                            "",
                            lang(
                                    "gui.my-auctions.open"
                            )
                    )
            );
        }

        player.openInventory(inventory);
    }

    // =========================================================
    // CATEGORY GUI
    // =========================================================

    private void openCategories(Player player) {

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        27,
                        color(
                                lang(
                                        "gui.categories.title"
                                )
                        )
                );

        ItemStack glass =
                createItem(
                        Material.GRAY_STAINED_GLASS_PANE,
                        " "
                );

        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, glass);
        }

        inventory.setItem(
                10,
                createItem(
                        Material.GRASS_BLOCK,
                        lang(
                                "gui.categories.blocks"
                        ),
                        "",
                        lang(
                                "gui.categories.select"
                        )
                )
        );

        inventory.setItem(
                11,
                createItem(
                        Material.DIAMOND_ORE,
                        lang(
                                "gui.categories.ores"
                        ),
                        "",
                        lang(
                                "gui.categories.select"
                        )
                );

        inventory.setItem(
                12,
                createItem(
                        Material.DIAMOND_PICKAXE,
                        lang(
                                "gui.categories.tools"
                        ),
                        "",
                        lang(
                                "gui.categories.select"
                        )
                );

        inventory.setItem(
                13,
                createItem(
                        Material.DIAMOND_SWORD,
                        lang(
                                "gui.categories.weapons"
                        ),
                        "",
                        lang(
                                "gui.categories.select"
                        )
                );

        inventory.setItem(
                14,
                createItem(
                        Material.DIAMOND_CHESTPLATE,
                        lang(
                                "gui.categories.armor"
                        ),
                        "",
                        lang(
                                "gui.categories.select"
                        )
                );

        inventory.setItem(
                15,
                createItem(
                        Material.GOLDEN_APPLE,
                        lang(
                                "gui.categories.food"
                        ),
                        "",
                        lang(
                                "gui.categories.select"
                        )
                );

        inventory.setItem(
                16,
                createItem(
                        Material.REDSTONE,
                        lang(
                                "gui.categories.redstone"
                        ),
                        "",
                        lang(
                                "gui.categories.select"
                        )
                );

        inventory.setItem(
                19,
                createItem(
                        Material.WHEAT,
                        lang(
                                "gui.categories.farming"
                        ),
                        "",
                        lang(
                                "gui.categories.select"
                        )
                );

        inventory.setItem(
                20,
                createItem(
                        Material.BRICKS,
                        lang(
                                "gui.categories.decoration"
                        ),
                        "",
                        lang(
                                "gui.categories.select"
                        )
                );

        inventory.setItem(
                21,
                createItem(
                        Material.CHEST,
                        lang(
                                "gui.categories.other"
                        ),
                        "",
                        lang(
                                "gui.categories.select"
                        )
                );

        inventory.setItem(
                22,
                createItem(
                        Material.COMPASS,
                        lang(
                                "gui.categories.all"
                        ),
                        "",
                        lang(
                                "gui.categories.select"
                        )
                );

        player.openInventory(inventory);
    }

    // =========================================================
    // CATEGORY CLICK
    // =========================================================

    private void selectCategory(
            Player player,
            AuctionCategory category
    ) {

        categories.put(
                player.getUniqueId(),
                category
        );

        pages.put(
                player.getUniqueId(),
                0
        );

        openGUI(
                player,
                getSearch(player)
        );
    }

    // =========================================================
    // CATEGORY NAME
    // =========================================================

    private String getCategoryName(
            AuctionCategory category
    ) {

        switch (category) {

            case BLOCKS:
                return lang(
                        "gui.categories.blocks"
                );

            case ORES:
                return lang(
                        "gui.categories.ores"
                );

            case TOOLS:
                return lang(
                        "gui.categories.tools"
                );

            case WEAPONS:
                return lang(
                        "gui.categories.weapons"
                );

            case ARMOR:
                return lang(
                        "gui.categories.armor"
                );

            case FOOD:
                return lang(
                        "gui.categories.food"
                );

            case REDSTONE:
                return lang(
                        "gui.categories.redstone"
                );

            case FARMING:
                return lang(
                        "gui.categories.farming"
                );

            case DECORATION:
                return lang(
                        "gui.categories.decoration"
                );

            case OTHER:
                return lang(
                        "gui.categories.other"
                );

            case ALL:
            default:
                return lang(
                        "gui.categories.all"
                );
        }
    }

    // =========================================================
    // IS CATEGORY
    // =========================================================

    private boolean isCategory(
            Material material,
            AuctionCategory category
    ) {

        if (category == AuctionCategory.ALL) {
            return true;
        }

        String name =
                material.name();

        /*
         * ORES
         */
        if (category == AuctionCategory.ORES) {

            return name.contains("ORE")
                    || name.contains("RAW_")
                    || name.equals("COAL")
                    || name.equals("DIAMOND")
                    || name.equals("EMERALD")
                    || name.equals("REDSTONE")
                    || name.equals("LAPIS_LAZULI")
                    || name.equals("QUARTZ")
                    || name.equals("AMETHYST_SHARD")
                    || name.equals("NETHERITE_SCRAP")
                    || name.equals("NETHERITE_INGOT");
        }

        /*
         * TOOLS
         */
        if (category == AuctionCategory.TOOLS) {

            return name.endsWith("_PICKAXE")
                    || name.endsWith("_AXE")
                    || name.endsWith("_SHOVEL")
                    || name.endsWith("_HOE")
                    || name.equals("SHEARS")
                    || name.equals("FLINT_AND_STEEL")
                    || name.equals("FISHING_ROD")
                    || name.equals("CARROT_ON_A_STICK")
                    || name.equals("WARPED_FUNGUS_ON_A_STICK");
        }

        /*
         * WEAPONS
         */
        if (category == AuctionCategory.WEAPONS) {

            return name.endsWith("_SWORD")
                    || name.equals("BOW")
                    || name.equals("CROSSBOW")
                    || name.equals("TRIDENT")
                    || name.equals("MACE");
        }

        /*
         * ARMOR
         */
        if (category == AuctionCategory.ARMOR) {

            return name.endsWith("_HELMET")
                    || name.endsWith("_CHESTPLATE")
                    || name.endsWith("_LEGGINGS")
                    || name.endsWith("_BOOTS")
                    || name.equals("ELYTRA")
                    || name.equals("SHIELD")
                    || name.equals("TURTLE_HELMET");
        }

        /*
         * FOOD
         */
        if (category == AuctionCategory.FOOD) {

            return name.contains("APPLE")
                    || name.contains("BREAD")
                    || name.contains("BEEF")
                    || name.contains("PORKCHOP")
                    || name.contains("CHICKEN")
                    || name.contains("MUTTON")
                    || name.contains("RABBIT")
                    || name.contains("COD")
                    || name.contains("SALMON")
                    || name.contains("POTATO")
                    || name.contains("CARROT")
                    || name.contains("BEETROOT")
                    || name.contains("COOKIE")
                    || name.contains("CAKE")
                    || name.contains("STEW")
                    || name.contains("SOUP")
                    || name.contains("PIE")
                    || name.equals("MUSHROOM");
        }

        /*
         * REDSTONE
         */
        if (category == AuctionCategory.REDSTONE) {

            return name.contains("REDSTONE")
                    || name.contains("PISTON")
                    || name.contains("COMPARATOR")
                    || name.contains("REPEATER")
                    || name.contains("OBSERVER")
                    || name.contains("HOPPER")
                    || name.contains("DROPPER")
                    || name.contains("DISPENSER")
                    || name.contains("LEVER")
                    || name.contains("BUTTON")
                    || name.contains("PRESSURE_PLATE")
                    || name.equals("TARGET")
                    || name.equals("DAYLIGHT_DETECTOR");
        }

        /*
         * FARMING
         */
        if (category == AuctionCategory.FARMING) {

            return name.equals("WHEAT")
                    || name.equals("WHEAT_SEEDS")
                    || name.equals("CARROT")
                    || name.equals("POTATO")
                    || name.equals("BEETROOT")
                    || name.equals("BEETROOT_SEEDS")
                    || name.equals("MELON")
                    || name.equals("MELON_SEEDS")
                    || name.equals("PUMPKIN")
                    || name.equals("PUMPKIN_SEEDS")
                    || name.equals("SUGAR_CANE")
                    || name.equals("COCOA_BEANS")
                    || name.equals("CACTUS")
                    || name.equals("BONE_MEAL")
                    || name.equals("BONE");
        }

        /*
         * DECORATION
         */
        if (category == AuctionCategory.DECORATION) {

            return name.contains("GLASS")
                    || name.contains("CARPET")
                    || name.contains("WOOL")
                    || name.contains("BANNER")
                    || name.contains("CANDLE")
                    || name.contains("FLOWER")
                    || name.contains("POT")
                    || name.contains("BRICK")
                    || name.contains("TERRACOTTA")
                    || name.contains("CONCRETE")
                    || name.contains("SIGN")
                    || name.contains("LANTERN")
                    || name.contains("PAINTING")
                    || name.contains("BED");
        }

        /*
         * BLOCKS
         */
        if (category == AuctionCategory.BLOCKS) {

            return material.isBlock()
                    && categoryForSpecial(material)
                            == AuctionCategory.BLOCKS;
        }

        /*
         * OTHER
         */
        if (category == AuctionCategory.OTHER) {

            return !isCategory(
                    material,
                    AuctionCategory.BLOCKS
            )
                    && !isCategory(
                    material,
                    AuctionCategory.ORES
            )
                    && !isCategory(
                    material,
                    AuctionCategory.TOOLS
            )
                    && !isCategory(
                    material,
                    AuctionCategory.WEAPONS
            )
                    && !isCategory(
                    material,
                    AuctionCategory.ARMOR
            )
                    && !isCategory(
                    material,
                    AuctionCategory.FOOD
            )
                    && !isCategory(
                    material,
                    AuctionCategory.REDSTONE
            )
                    && !isCategory(
                    material,
                    AuctionCategory.FARMING
            )
                    && !isCategory(
                    material,
                    AuctionCategory.DECORATION
            );
        }

        return false;
    }

    // =========================================================
    // BLOCK SPECIAL CATEGORY
    // =========================================================

    private AuctionCategory categoryForSpecial(
            Material material
    ) {

        String name =
                material.name();

        if (name.contains("ORE")
                || name.contains("RAW_")) {

            return AuctionCategory.ORES;
        }

        if (name.endsWith("_PICKAXE")
                || name.endsWith("_AXE")
                || name.endsWith("_SHOVEL")
                || name.endsWith("_HOE")) {

            return AuctionCategory.TOOLS;
        }

        if (name.endsWith("_SWORD")
                || name.equals("BOW")
                || name.equals("CROSSBOW")
                || name.equals("TRIDENT")
                || name.equals("MACE")) {

            return AuctionCategory.WEAPONS;
        }

        if (name.endsWith("_HELMET")
                || name.endsWith("_CHESTPLATE")
                || name.endsWith("_LEGGINGS")
                || name.endsWith("_BOOTS")
                || name.equals("ELYTRA")) {

            return AuctionCategory.ARMOR;
        }

        if (name.contains("GLASS")
                || name.contains("WOOL")
                || name.contains("CARPET")
                || name.contains("TERRACOTTA")
                || name.contains("CONCRETE")
                || name.contains("BRICK")) {

            return AuctionCategory.DECORATION;
        }

        return AuctionCategory.BLOCKS;
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

        if (!(event.getWhoClicked()
                instanceof Player)) {

            return;
        }

        Player player =
                (Player) event.getWhoClicked();

        String clickedTitle =
                event.getView().getTitle();

        /*
         * =====================================================
         * CATEGORY GUI
         * =====================================================
         */

        String categoryTitle =
                color(
                        lang(
                                "gui.categories.title"
                        )
                );

        if (clickedTitle.equals(categoryTitle)) {

            event.setCancelled(true);

            if (event.getRawSlot() >=
                    event.getView()
                            .getTopInventory()
                            .getSize()) {

                return;
            }

            switch (event.getRawSlot()) {

                case 10:
                    selectCategory(
                            player,
                            AuctionCategory.BLOCKS
                    );
                    return;

                case 11:
                    selectCategory(
                            player,
                            AuctionCategory.ORES
                    );
                    return;

                case 12:
                    selectCategory(
                            player,
                            AuctionCategory.TOOLS
                    );
                    return;

                case 13:
                    selectCategory(
                            player,
                            AuctionCategory.WEAPONS
                    );
                    return;

                case 14:
                    selectCategory(
                            player,
                            AuctionCategory.ARMOR
                    );
                    return;

                case 15:
                    selectCategory(
                            player,
                            AuctionCategory.FOOD
                    );
                    return;

                case 16:
                    selectCategory(
                            player,
                            AuctionCategory.REDSTONE
                    );
                    return;

                case 19:
                    selectCategory(
                            player,
                            AuctionCategory.FARMING
                    );
                    return;

                case 20:
                    selectCategory(
                            player,
                            AuctionCategory.DECORATION
                    );
                    return;

                case 21:
                    selectCategory(
                            player,
                            AuctionCategory.OTHER
                    );
                    return;

                case 22:
                    selectCategory(
                            player,
                            AuctionCategory.ALL
                    );
                    return;

                default:
                    return;
            }
        }

        /*
         * =====================================================
         * CONFIRMATION GUI
         * =====================================================
         */

        String confirmationTitle =
                color(
                        lang(
                                "gui.confirmation.title"
                        )
                );

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

            /*
             * CONFIRM
             */
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

            /*
             * CANCEL
             */
            if (slot == 15) {

                pendingPurchases.remove(uuid);

                player.closeInventory();

                open(player);

                return;
            }

            return;
        }

        /*
         * =====================================================
         * NORMAL GUI
         * =====================================================
         */

        String guiTitle =
                color(
                        lang(
                                "gui.title"
                        )
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

        /*
         * =====================================================
         * CATEGORY
         * =====================================================
         */

        if (slot == 22) {

            openCategories(player);
            return;
        }

        /*
         * =====================================================
         * PREVIOUS
         * =====================================================
         */

        if (slot == 18) {

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

        /*
         * =====================================================
         * NEXT
         * =====================================================
         */

        if (slot == 26) {

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

        /*
         * =====================================================
         * SORT
         * =====================================================
         */

        if (slot == 31) {

            SortType current =
                    sorts.getOrDefault(
                            uuid,
                            SortType.NEWEST
                    );

            sorts.put(
                    uuid,
                    getNextSort(current)
            );

            pages.put(
                    uuid,
                    0
            );

            refresh(player);

            return;
        }

        /*
         * =====================================================
         * REFRESH
         * =====================================================
         */

        if (slot == 40) {

            pages.put(
                    uuid,
                    0
            );

            refresh(player);

            return;
        }

        /*
         * =====================================================
         * SEARCH
         * =====================================================
         */

        if (slot == 49) {

            player.closeInventory();

            AuctionSearch search =
                    plugin.getAuctionSearch();

            if (search != null) {
                search.open(player);
            }

            return;
        }

        /*
         * =====================================================
         * MY AUCTIONS
         * =====================================================
         */

        if (slot == 58) {

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

        /*
         * =====================================================
         * AUCTION ITEM
         * =====================================================
         */

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

            /*
             * OWN AUCTION
             */

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
                .equals(
                        player.getUniqueId()
                )) {

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
                                current.getItem()
                                        .clone()
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
                        auction.getItem()
                                .getType(),
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
                .equals(
                        buyer.getUniqueId()
                )) {

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
                .has(
                        buyer,
                        price
                )) {

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
                                current.getItem()
                                        .clone()
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
                                plugin.formatMoney(
                                        price
                                )
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
                                        plugin.formatMoney(
                                                price
                                        )
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

        AuctionCategory category =
                categories.getOrDefault(
                        uuid,
                        AuctionCategory.ALL
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

        /*
         * MY AUCTIONS
         */

        if (onlyMine) {

            List<Auction> mine =
                    new ArrayList<>();

            for (Auction auction :
                    auctions) {

                if (auction.getSeller()
                        .equals(uuid)) {

                    mine.add(auction);
                }
            }

            auctions = mine;
        }

        /*
         * CATEGORY
         */

        if (category != AuctionCategory.ALL) {

            List<Auction> filtered =
                    new ArrayList<>();

            for (Auction auction :
                    auctions) {

                if (auction == null ||
                        auction.getItem() == null) {

                    continue;
                }

                if (isCategory(
                        auction.getItem()
                                .getType(),
                        category
                )) {

                    filtered.add(auction);
                }
            }

            auctions = filtered;
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
    // TOGGLE CONFIRMATION
    // =========================================================

    public void togglePurchaseConfirmation(
            Player player
    ) {

        UUID uuid =
                player.getUniqueId();

        boolean current =
                purchaseConfirmation.getOrDefault(
                        uuid,
                        getDefaultConfirmation()
                );

        boolean newState =
                !current;

        purchaseConfirmation.put(
                uuid,
                newState
        );

        if (newState) {

            player.sendMessage(
                    prefix() +
                            "&aPurchase confirmation enabled."
            );

        } else {

            player.sendMessage(
                    prefix() +
                            "&cPurchase confirmation disabled."
            );
        }
    }

    // =========================================================
    // GET CONFIRMATION
    // =========================================================

    public boolean getPurchaseConfirmation(
            Player player
    ) {

        return purchaseConfirmation.getOrDefault(
                player.getUniqueId(),
                getDefaultConfirmation()
        );
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
                    content.getType() ==
                            Material.AIR) {

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

    private String lang(
            String path
    ) {

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

    private String color(
            String text
    ) {

        return plugin.color(text);
    }
}
