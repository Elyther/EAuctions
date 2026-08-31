package com.elyther.eauctions;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AuctionManager {

    private final EAuctions plugin;
    private final List<Auction> auctions = new ArrayList<>();

    public AuctionManager(EAuctions plugin) {
        this.plugin = plugin;
    }

    public void addAuction(
            UUID seller,
            ItemStack item,
            double price
    ) {

        Auction auction =
                new Auction(
                        UUID.randomUUID(),
                        seller,
                        item,
                        price
                );

        auctions.add(auction);
    }

    public List<Auction> getAuctions() {
        return new ArrayList<>(auctions);
    }

    public List<Auction> search(
            String search
    ) {

        List<Auction> result =
                new ArrayList<>();

        String query =
                search.toLowerCase();

        for (Auction auction : auctions) {

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

    public void removeAuction(
            Auction auction
    ) {

        auctions.remove(auction);
    }

    public Auction find(
            UUID id
    ) {

        for (Auction auction : auctions) {

            if (auction.getId().equals(id)) {

                return auction;
            }
        }

        return null;
    }

    // =========================================================
    // AUCTION CLASS
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
