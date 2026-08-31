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

    public AuctionManager(EAuctions plugin) {

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
    // REMOVE
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
    // ALL AUCTIONS
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

        List<Auction> result =
                new ArrayList<>();

        if (search == null ||
                search.isBlank()) {

            result.addAll(auctions);

            return result;
        }

        String query =
                search
                        .trim()
                        .toLowerCase();

        for (Auction auction :
                auctions) {

            String material =
                    auction.getItem()
                            .getType()
                            .name()
                            .toLowerCase();

            String displayName = "";

            if (auction.getItem()
                    .hasItemMeta()) {

                if (auction.getItem()
                        .getItemMeta()
                        .hasDisplayName()) {

                    displayName =
                            auction.getItem()
                                    .getItemMeta()
                                    .getDisplayName()
                                    .toLowerCase();
                }
            }

            /*
             * Starts with:
             *
             * dia
             *
             * matches:
             *
             * diamond
             * diamond_sword
             * diamond_block
             * diamond_pickaxe
             *
             * Also accepts display names.
             */

            if (material.startsWith(query) ||
                    displayName.startsWith(query)) {

                result.add(auction);
            }
        }

        return result;
    }

    // =========================================================
    // SEARCH + SORT
    // =========================================================

    public synchronized List<Auction> searchAndSort(
            String search,
            SortType sortType
    ) {

        List<Auction> result =
                search(search);

        sort(result, sortType);

        return result;
    }

    // =========================================================
    // SORT
    // =========================================================

    public synchronized void sort(
            List<Auction> list,
            SortType sortType
    ) {

        if (sortType == null) {
            sortType = SortType.NEWEST;
        }

        switch (sortType) {

            case NEWEST:

                list.sort(
                        Comparator.comparingInt(
                                Auction::getId
                        ).reversed()
                );

                break;

            case OLDEST:

                list.sort(
                        Comparator.comparingInt(
                                Auction::getId
                        )
                );

                break;

            case CHEAPEST:

                list.sort(
                        Comparator.comparingDouble(
                                Auction::getPrice
                        )
                );

                break;

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
    // PLAYER AUCTIONS
    // =========================================================

    public synchronized List<Auction> getPlayerAuctions(
            UUID player
    ) {

        List<Auction> result =
                new ArrayList<>();

        for (Auction auction :
                auctions) {

            if (auction.getSeller()
                    .equals(player)) {

                result.add(auction);
            }
        }

        return result;
    }

    // =========================================================
    // SAVE
    // =========================================================

    public synchronized void save() {

        if (!plugin.getDataFolder()
                .exists()) {

            plugin.getDataFolder()
                    .mkdirs();
        }

        YamlConfiguration config =
                new YamlConfiguration();

        config.set(
                "next-id",
                nextId
        );

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

        try {

            config.save(file);

        } catch (IOException e) {

            plugin.getLogger().severe(
                    "Could not save auctions.yml"
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
            return;
        }

        YamlConfiguration config =
                YamlConfiguration
                        .loadConfiguration(file);

        nextId =
                config.getInt(
                        "next-id",
                        1
                );

        ConfigurationSection section =
                config.getConfigurationSection(
                        "auctions"
                );

        if (section == null) {
            return;
        }

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

                if (sellerString == null ||
                        item == null) {

                    continue;
                }

                UUID seller =
                        UUID.fromString(
                                sellerString
                        );

                auctions.add(
                        new Auction(
                                id,
                                seller,
                                item,
                                price
                        )
                );

                /*
                 * Protect against duplicate IDs
                 * after restart.
                 */

                if (id >= nextId) {
                    nextId = id + 1;
                }

            } catch (Exception e) {

                plugin.getLogger().warning(
                        "Could not load auction " +
                                key
                );
            }
        }

        plugin.getLogger().info(
                "Loaded " +
                        auctions.size() +
                        " auctions."
        );
    }
}
