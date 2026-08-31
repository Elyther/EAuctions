package com.elyther.eauctions;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

    public synchronized boolean removeAuction(
            int id
    ) {

        boolean removed =
                auctions.removeIf(
                        auction -> auction.getId() == id
                );

        if (removed) {
            save();
        }

        return removed;
    }

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

    public synchronized List<Auction> getAuctions() {
        return new ArrayList<>(auctions);
    }

    public synchronized List<Auction> search(
            String search
    ) {

        if (search == null ||
                search.trim().isEmpty()) {

            return getAuctions();
        }

        String query =
                search.toLowerCase();

        List<Auction> result =
                new ArrayList<>();

        for (Auction auction : auctions) {

            String material =
                    auction.getItem()
                            .getType()
                            .name()
                            .toLowerCase();

            String displayName = "";

            if (auction.getItem().hasItemMeta() &&
                    auction.getItem()
                            .getItemMeta()
                            .hasDisplayName()) {

                displayName =
                        auction.getItem()
                                .getItemMeta()
                                .getDisplayName()
                                .toLowerCase();
            }

            if (material.contains(query) ||
                    displayName.contains(query)) {

                result.add(auction);
            }
        }

        return result;
    }

    public synchronized void save() {

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        YamlConfiguration config =
                new YamlConfiguration();

        config.set(
                "next-id",
                nextId
        );

        int index = 0;

        for (Auction auction : auctions) {

            String path =
                    "auctions." + index;

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
                    "Could not save auctions.yml"
            );

            e.printStackTrace();
        }
    }

    private synchronized void load() {

        auctions.clear();

        if (!file.exists()) {
            return;
        }

        YamlConfiguration config =
                YamlConfiguration.loadConfiguration(file);

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

            } catch (Exception e) {

                plugin.getLogger().warning(
                        "Could not load auction " + key
                );
            }
        }
    }
}
