package com.elyther.eauctions;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Auction {

    private final int id;
    private final UUID seller;
    private final ItemStack item;
    private final double price;

    public Auction(
            int id,
            UUID seller,
            ItemStack item,
            double price
    ) {
        this.id = id;
        this.seller = seller;
        this.item = item;
        this.price = price;
    }

    public int getId() {
        return id;
    }

    public UUID getSeller() {
        return seller;
    }

    public ItemStack getItem() {
        return item;
    }

    public double getPrice() {
        return price;
    }
}
