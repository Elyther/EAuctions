package com.elyther.eauctions;

import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionManager {

    private final EAuctions plugin;
    private Connection connection;

    public AuctionManager(EAuctions plugin) {
        this.plugin = plugin;

        setupDatabase();
    }

    // =========================================================
    // DATABASE
    // =========================================================

    private void setupDatabase() {

        try {

            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            File database =
                    new File(
                            plugin.getDataFolder(),
                            "auctions.db"
                    );

            connection =
                    DriverManager.getConnection(
                            "jdbc:sqlite:" +
                                    database.getAbsolutePath()
                    );

            try (Statement statement =
                         connection.createStatement()) {

                statement.executeUpdate(
                        """
                        CREATE TABLE IF NOT EXISTS auctions (
                            id TEXT PRIMARY KEY,
                            seller TEXT NOT NULL,
                            item BLOB NOT NULL,
                            price REAL NOT NULL
                        )
                        """
                );
            }

            plugin.getLogger().info(
                    "Auction database connected."
            );

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Could not connect to auction database!"
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // ADD AUCTION
    // =========================================================

    public void addAuction(
            UUID seller,
            ItemStack item,
            double price
    ) {

        UUID id =
                UUID.randomUUID();

        byte[] itemData;

        try {

            itemData =
                    item.serializeAsBytes();

        } catch (Exception e) {

            plugin.getLogger().severe(
                    "Could not serialize item."
            );

            e.printStackTrace();

            return;
        }

        String sql =
                """
                INSERT INTO auctions
                (id, seller, item, price)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    id.toString()
            );

            statement.setString(
                    2,
                    seller.toString()
            );

            statement.setBytes(
                    3,
                    itemData
            );

            statement.setDouble(
                    4,
                    price
            );

            statement.executeUpdate();

        } catch (SQLException e) {

            plugin.getLogger().severe(
                    "Could not add auction."
            );

            e.printStackTrace();
        }
    }

    // =========================================================
    // GET ALL AUCTIONS
    // =========================================================

    public List<Auction> getAuctions() {

        List<Auction> auctions =
                new ArrayList<>();

        String sql =
                "SELECT * FROM auctions";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql);

             ResultSet result =
                     statement.executeQuery()) {

            while (result.next()) {

                Auction auction =
                        fromResultSet(result);

                if (auction != null) {

                    auctions.add(auction);
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return auctions;
    }

    // =========================================================
    // SEARCH
    // =========================================================

    public List<Auction> search(
            String search
    ) {

        List<Auction> result =
                new ArrayList<>();

        if (search == null ||
                search.isBlank()) {

            return getAuctions();
        }

        String query =
                search
                        .toLowerCase()
                        .trim();

        for (Auction auction :
                getAuctions()) {

            String material =
                    auction.getItem()
                            .getType()
                            .name()
                            .toLowerCase();

            if (material.contains(query)) {

                result.add(auction);
            }
        }

        return result;
    }

    // =========================================================
    // FIND AUCTION
    // =========================================================

    public Auction find(
            UUID id
    ) {

        String sql =
                "SELECT * FROM auctions WHERE id = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    id.toString()
            );

            try (ResultSet result =
                         statement.executeQuery()) {

                if (result.next()) {

                    return fromResultSet(result);
                }
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }

        return null;
    }

    // =========================================================
    // REMOVE AUCTION
    // =========================================================

    public boolean removeAuction(
            Auction auction
    ) {

        if (auction == null) {
            return false;
        }

        String sql =
                "DELETE FROM auctions WHERE id = ?";

        try (PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    auction.getId().toString()
            );

            int affected =
                    statement.executeUpdate();

            return affected > 0;

        } catch (SQLException e) {

            e.printStackTrace();

            return false;
        }
    }

    // =========================================================
    // RESULT SET → AUCTION
    // =========================================================

    private Auction fromResultSet(
            ResultSet result
    ) throws SQLException {

        UUID id =
                UUID.fromString(
                        result.getString("id")
                );

        UUID seller =
                UUID.fromString(
                        result.getString("seller")
                );

        byte[] itemData =
                result.getBytes("item");

        double price =
                result.getDouble("price");

        ItemStack item;

        try {

            item =
                    ItemStack.deserializeBytes(
                            itemData
                    );

        } catch (Exception e) {

            plugin.getLogger().warning(
                    "Could not deserialize auction item: "
                            + id
            );

            return null;
        }

        return new Auction(
                id,
                seller,
                item,
                price
        );
    }

    // =========================================================
    // CLOSE DATABASE
    // =========================================================

    public void close() {

        if (connection == null) {
            return;
        }

        try {

            if (!connection.isClosed()) {

                connection.close();
            }

        } catch (SQLException e) {

            e.printStackTrace();
        }
    }

    // =========================================================
    // AUCTION
    // =========================================================

    public static class Auction {

        private final UUID id;
        private final UUID seller;
        private final ItemStack item;
        private final double price;

        public Auction(
                UUID id,
                UUID seller,
                ItemStack item,
                double price
        ) {

            this.id = id;
            this.seller = seller;
            this.item = item.clone();
            this.price = price;
        }

        public UUID getId() {
            return id;
        }

        public UUID getSeller() {
            return seller;
        }

        public ItemStack getItem() {
            return item.clone();
        }

        public double getPrice() {
            return price;
        }
    }
}
