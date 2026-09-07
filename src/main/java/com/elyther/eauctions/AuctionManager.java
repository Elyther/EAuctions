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
    private final List<Auction> auctions = new ArrayList<>();

    private int nextId = 1;
    private final File file;

    public AuctionManager(EAuctions plugin) {
        this.plugin = plugin;

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        this.file = new File(
                plugin.getDataFolder(),
                "auctions.yml"
        );

        load();
    }

    public synchronized Auction addAuction(
            UUID seller,
            ItemStack item,
            double price
    ) {
        if (seller == null) {
            return null;
        }

        if (item == null || item.getType().isAir()) {
            return null;
        }

        if (price < 0) {
            return null;
        }

        Auction auction = new Auction(
                nextId,
                seller,
                item.clone(),
                price
        );

        nextId++;
        auctions.add(auction);
        save();

        return auction;
    }

    public synchronized boolean removeAuction(int id) {
        Auction found = null;

        for (Auction auction : auctions) {
            if (auction.getId() == id) {
                found = auction;
                break;
            }
        }

        if (found == null) {
            return false;
        }

        auctions.remove(found);
        save();

        return true;
    }

    public synchronized Auction getAuction(int id) {
        for (Auction auction : auctions) {
            if (auction.getId() == id) {
                return auction;
            }
        }

        return null;
    }

    public synchronized List<Auction> getAuctions() {
        return new ArrayList<>(auctions);
    }

    // =========================================================
    // SEARCH
    // =========================================================

    public synchronized List<Auction> search(String search) {

        List<Auction> result = new ArrayList<>();

        if (search == null) {
            return getAuctions();
        }

        String query = normalize(search);

        if (query.isEmpty()) {
            return getAuctions();
        }

        for (Auction auction : auctions) {

            if (auction == null || auction.getItem() == null) {
                continue;
            }

            ItemStack item = auction.getItem();

            if (item.getType().isAir()) {
                continue;
            }

            // Minecraft material name
            String material = normalize(
                    item.getType().name()
            );

            if (material.contains(query)) {
                result.add(auction);
                continue;
            }

            // Custom/display name
            if (item.hasItemMeta()
                    && item.getItemMeta() != null
                    && item.getItemMeta().hasDisplayName()) {

                String displayName =
                        item.getItemMeta().getDisplayName();

                if (displayName != null) {

                    displayName = normalize(
                            removeColorCodes(displayName)
                    );

                    if (displayName.contains(query)) {
                        result.add(auction);
                    }
                }
            }
        }

        return result;
    }

    // =========================================================
    // SEARCH + SORT
    // =========================================================

    public synchronized List<Auction> search(
            String search,
            SortType sortType
    ) {
        List<Auction> result = search(search);

        sortList(result, sortType);

        return result;
    }

    // =========================================================
    // SORT ALL
    // =========================================================

    public synchronized List<Auction> sort(
            SortType sortType
    ) {
        List<Auction> result = getAuctions();

        sortList(result, sortType);

        return result;
    }

    private void sortList(
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
            UUID seller
    ) {

        List<Auction> result = new ArrayList<>();

        if (seller == null) {
            return result;
        }

        for (Auction auction : auctions) {

            if (auction == null) {
                continue;
            }

            if (auction.getSeller() == null) {
                continue;
            }

            if (auction.getSeller().equals(seller)) {
                result.add(auction);
            }
        }

        return result;
    }

    // =========================================================
    // SELLER AUCTIONS
    // =========================================================

    public synchronized List<Auction> getSellerAuctions(
            UUID seller
    ) {
        return getPlayerAuctions(seller);
    }

    // =========================================================
    // PLAYER AUCTIONS + SORT
    // =========================================================

    public synchronized List<Auction> getPlayerAuctions(
            UUID seller,
            SortType sortType
    ) {

        List<Auction> result =
                getPlayerAuctions(seller);

        sortList(result, sortType);

        return result;
    }

    // =========================================================
    // SORTED AUCTIONS
    // =========================================================

    public synchronized List<Auction> getSortedAuctions(
            SortType sortType
    ) {
        return sort(sortType);
    }

    // =========================================================
    // SEARCHED + SORTED
    // =========================================================

    public synchronized List<Auction> getSortedAuctions(
            String search,
            SortType sortType
    ) {
        return search(search, sortType);
    }

    // =========================================================
    // SAVE
    // =========================================================

    public synchronized void save() {

        if (!plugin.getDataFolder().exists()) {

            if (!plugin.getDataFolder().mkdirs()) {
                plugin.getLogger().warning(
                        "Could not create plugin data folder."
                );
            }
        }

        YamlConfiguration config =
                new YamlConfiguration();

        config.set("next-id", nextId);

        int index = 0;

        for (Auction auction : auctions) {

            if (auction == null) {
                continue;
            }

            if (auction.getSeller() == null) {
                continue;
            }

            if (auction.getItem() == null) {
                continue;
            }

            String path = "auctions." + index;

            config.set(
                    path + ".id",
                    auction.getId()
            );

            config.set(
                    path + ".seller",
                    auction.getSeller().toString()
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
            nextId = 1;
            return;
        }

        YamlConfiguration config =
                YamlConfiguration.loadConfiguration(file);

        nextId = config.getInt(
                "next-id",
                1
        );

        if (nextId < 1) {
            nextId = 1;
        }

        ConfigurationSection section =
                config.getConfigurationSection(
                        "auctions"
                );

        if (section == null) {

            plugin.getLogger().info(
                    "No auctions found."
            );

            return;
        }

        for (String key :
                section.getKeys(false)) {

            String path =
                    "auctions." + key;

            try {

                int id =
                        config.getInt(
                                path + ".id",
                                -1
                        );

                String sellerString =
                        config.getString(
                                path + ".seller"
                        );

                double price =
                        config.getDouble(
                                path + ".price",
                                -1
                        );

                ItemStack item =
                        config.getItemStack(
                                path + ".item"
                        );

                if (id < 1) {

                    plugin.getLogger().warning(
                            "Skipping auction " +
                                    key +
                                    ": invalid ID."
                    );

                    continue;
                }

                if (sellerString == null ||
                        sellerString.trim().isEmpty()) {

                    plugin.getLogger().warning(
                            "Skipping auction " +
                                    key +
                                    ": seller missing."
                    );

                    continue;
                }

                if (price < 0 ||
                        Double.isNaN(price) ||
                        Double.isInfinite(price)) {

                    plugin.getLogger().warning(
                            "Skipping auction " +
                                    key +
                                    ": invalid price."
                    );

                    continue;
                }

                if (item == null ||
                        item.getType().isAir()) {

                    plugin.getLogger().warning(
                            "Skipping auction " +
                                    key +
                                    ": item missing."
                    );

                    continue;
                }

                UUID seller =
                        UUID.fromString(
                                sellerString
                        );

                Auction auction =
                        new Auction(
                                id,
                                seller,
                                item,
                                price
                        );

                auctions.add(auction);

                if (id >= nextId) {
                    nextId = id + 1;
                }

            } catch (Exception e) {

                plugin.getLogger().warning(
                        "Could not load auction " +
                                key +
                                ": " +
                                e.getMessage()
                );
            }
        }

        plugin.getLogger().info(
                "Loaded " +
                        auctions.size() +
                        " auctions from auctions.yml."
        );
    }

    // =========================================================
    // SEARCH NORMALIZATION
    // =========================================================

    private String normalize(String text) {

        if (text == null) {
            return "";
        }

        return removeColorCodes(text)
                .trim()
                .toLowerCase()
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
    }

    // =========================================================
    // REMOVE MINECRAFT COLOR CODES
    // =========================================================

    private String removeColorCodes(String text) {

        if (text == null) {
            return "";
        }

        text = text.replaceAll(
                "§[0-9a-fk-or]",
                ""
        );

        text = text.replaceAll(
                "§x(§[0-9a-fA-F]){6}",
                ""
        );

        return text;
    }
}
