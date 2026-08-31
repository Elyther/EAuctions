package com.elyther.eauctions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class AuctionManager {

    private final EAuctions plugin;

    private final List<Auction> auctions =
            new ArrayList<>();

    private int nextId = 1;

    private final File file;

    public AuctionManager( EAuctions plugin) {

        this.plugin = plugin;

        file = new File(
                plugin.getDataFolder(),
                "auctions.yml"
        );

        load();
    }

    // =========================================================
    // ADD AUCTION
    // =========================================================

    public synchronized Auction addAuction(
            UUID seller,
            ItemStack item,
            double price
    ) {

        Auction auction =
                new Auction(
                        nextId++,
                        seller,
                        item.clone(),
                        price
                );

        auctions.add(auction);

        save();

        return auction;
    }

    // =========================================================
    // REMOVE AUCTION
    // =========================================================

    public synchronized boolean removeAuction(
            int id
    ) {

        boolean removed =
                auctions.removeIf(
                        auction ->
                                auction.getId() == id
                );

        if (removed) {
            save();
        }

        return removed;
    }

    // =========================================================
    // GET AUCTION
    // =========================================================

    public synchronized Auction getAuction(
            int id
    ) {

        for (Auction auction : auctions) {

            if (auction.getId() == id) {
                return auction;
            }
        }

        return null;
    }

    // =========================================================
    // GET ALL AUCTIONS
    // =========================================================

    public synchronized List<Auction> getAuctions() {

        return new ArrayList<>(
                auctions
        );
    }

    // =========================================================
    // SEARCH
    // =========================================================

    public synchronized List<Auction> search(
            String search
    ) {

        if (search == null ||
                search.trim().isEmpty()) {

            return getAuctions();
        }

        String query =
                search
                        .trim()
                        .toLowerCase();

        List<Auction> result =
                new ArrayList<>();

        for (Auction auction :
                auctions) {

            ItemStack item =
                    auction.getItem();

            if (item == null ||
                    item.getType() == null) {

                continue;
            }

            // =================================================
            // MATERIAL
            // =================================================

            String material =
                    item.getType()
                            .name()
                            .toLowerCase();

            // =================================================
            // DISPLAY NAME
            // =================================================

            String displayName = "";

            if (item.hasItemMeta() &&
                    item.getItemMeta() != null &&
                    item.getItemMeta()
                            .hasDisplayName()) {

                displayName =
                        item.getItemMeta()
                                .getDisplayName()
                                .toLowerCase();
            }

            // =================================================
            // PLAIN DISPLAY NAME
            // Remove Minecraft color codes
            // =================================================

            String plainDisplayName =
                    displayName
                            .replaceAll(
                                    "§[0-9a-fk-orx]",
                                    ""
                            )
                            .toLowerCase();

            // =================================================
            // MATCH
            //
            // Example:
            // dia
            //
            // DIAMOND
            // DIAMOND_BLOCK
            // DIAMOND_ORE
            // etc.
            // =================================================

            if (material.contains(query) ||
                    displayName.contains(query) ||
                    plainDisplayName.contains(query)) {

                result.add(auction);
            }
        }

        return result;
    }

    // =========================================================
    // SEARCH + SORT
    // =========================================================

    public synchronized List<Auction> searchSorted(
            String search,
            SortType sortType
    ) {

        List<Auction> result =
                search(search);

        sortList(
                result,
                sortType
        );

        return result;
    }

    // =========================================================
    // SORT ALL
    // =========================================================

    public synchronized List<Auction> getSortedAuctions(
            SortType sortType
    ) {

        List<Auction> result =
                new ArrayList<>(
                        auctions
                );

        sortList(
                result,
                sortType
        );

        return result;
    }

    // =========================================================
    // SORT
    // =========================================================

    private void sortList(
            List<Auction> list,
            SortType sortType
    ) {

        if (sortType == null) {

            sortType =
                    SortType.NEWEST;
        }

        switch (sortType) {

            // =============================================
            // NEWEST
            // =============================================

            case NEWEST:

                list.sort(
                        Comparator.comparingInt(
                                Auction::getId
                        ).reversed()
                );

                break;

            // =============================================
            // OLDEST
            // =============================================

            case OLDEST:

                list.sort(
                        Comparator.comparingInt(
                                Auction::getId
                        )
                );

                break;

            // =============================================
            // CHEAPEST
            // =============================================

            case CHEAPEST:

                list.sort(
                        Comparator.comparingDouble(
                                Auction::getPrice
                        )
                );

                break;

            // =============================================
            // MOST EXPENSIVE
            // =============================================

            case MOST_EXPENSIVE:

                list.sort(
                        Comparator.comparingDouble(
                                Auction::getPrice
                        ).reversed()
                );

                break;
        }
    }

    // =========================================================
    // SAVE
    // =========================================================

    public synchronized void save() {

        if (!plugin.getDataFolder()
                .exists()) {

            if (!plugin.getDataFolder()
                    .mkdirs()) {

                plugin.getLogger().warning(
                        "Could not create plugin data folder!"
                );
            }
        }

        YamlConfiguration config =
                new YamlConfiguration();

        // =====================================================
        // NEXT ID
        // =====================================================

        config.set(
                "next-id",
                nextId
        );

        // =====================================================
        // AUCTIONS
        // =====================================================

        int index = 0;

        for (Auction auction :
                auctions) {

            String path =
                    "auctions." + index;

            config.set(
                    path + ".id",
                    auction.getId()
            );

            config.set(
                    path + ".seller",
                    auction.getSeller()
                            .toString()
            );

            config.set(
                    path + ".price",
                    auction.getPrice()
            );

            config.set(
                    path + ".item",
                    auction.getItem()
            );

            index++;
        }

        // =====================================================
        // SAVE FILE
        // =====================================================

        try {

            config.save(file);

        } catch (IOException e) {

            plugin.getLogger().severe(
                    "Could not save auctions.yml!"
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // LOAD
    // =========================================================

    private synchronized void load() {

        auctions.clear();

        if (!file.exists()) {

            plugin.getLogger().info(
                    "No auctions.yml found. Creating a new one."
            );

            nextId = 1;

            save();

            return;
        }

        YamlConfiguration config =
                YamlConfiguration
                        .loadConfiguration(file);

        // =====================================================
        // NEXT ID
        // =====================================================

        nextId =
                config.getInt(
                        "next-id",
                        1
                );

        // =====================================================
        // AUCTIONS SECTION
        // =====================================================

        ConfigurationSection section =
                config.getConfigurationSection(
                        "auctions"
                );

        if (section == null) {

            plugin.getLogger().info(
                    "No auctions found in auctions.yml."
            );

            return;
        }

        // =====================================================
        // LOAD AUCTIONS
        // =====================================================

        for (String key :
                section.getKeys(false)) {

            String path =
                    "auctions." + key;

            try {

                int id =
                        config.getInt(
                                path + ".id"
                        );

                String sellerString =
                        config.getString(
                                path + ".seller"
                        );

                double price =
                        config.getDouble(
                                path + ".price"
                        );

                ItemStack item =
                        config.getItemStack(
                                path + ".item"
                        );

                // =============================================
                // VALIDATION
                // =============================================

                if (sellerString == null ||
                        sellerString.isEmpty()) {

                    plugin.getLogger().warning(
                            "Auction " +
                                    key +
                                    " has no seller. Skipping."
                    );

                    continue;
                }

                if (item == null ||
                        item.getType() == null) {

                    plugin.getLogger().warning(
                            "Auction " +
                                    key +
                                    " has no item. Skipping."
                    );

                    continue;
                }

                UUID seller =
                        UUID.fromString(
                                sellerString
                        );

                // =============================================
                // CREATE AUCTION
                // =============================================

                Auction auction =
                        new Auction(
                                id,
                                seller,
                                item,
                                price
                        );

                auctions.add(
                        auction
                );

                // =============================================
                // MAKE SURE NEXT ID IS SAFE
                // =============================================

                if (id >= nextId) {

                    nextId =
                            id + 1;
                }

            } catch (Exception e) {

                plugin.getLogger().warning(
                        "Could not load auction " +
                                key +
                                "!"
                );

                e.printStackTrace();
            }
        }

        plugin.getLogger().info(
                "Loaded " +
                        auctions.size() +
                        " auction(s)."
        );
    }
}
