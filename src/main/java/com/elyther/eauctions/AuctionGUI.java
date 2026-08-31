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
import java.util.List;

public class AuctionGUI implements Listener {

    private final EAuctions plugin;

    private final String title =
            ChatColor.DARK_PURPLE +
                    "EAuctions";

    public AuctionGUI(EAuctions plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        openSearch(player, "");
    }

    public void openSearch(
            Player player,
            String search
    ) {

        List<Auction> auctions =
                plugin.getAuctionManager()
                        .search(search);

        Inventory inventory =
                Bukkit.createInventory(
                        null,
                        54,
                        title
                );

        // Auction items
        int slot = 0;

        for (Auction auction : auctions) {

            if (slot >= 45) {
                break;
            }

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

        // Bottom bar
        ItemStack filler =
                createItem(
                        Material.GRAY_STAINED_GLASS_PANE,
                        " "
                );

        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, filler);
        }

        // Search info
        ItemStack searchItem =
                createItem(
                        Material.COMPASS,
                        "&bSearch",
                        "&7Current search:",
                        "&f" +
                                (
                                        search == null ||
                                                search.isEmpty()
                                                ? "All items"
                                                : search
                                ),
                        "",
                        "&7Use:",
                        "&f/ah <item>"
                );

        inventory.setItem(
                49,
                searchItem
        );

        player.openInventory(inventory);
    }

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

    @EventHandler
    public void onClick(
            InventoryClickEvent event
    ) {

        if (!(event.getWhoClicked()
                instanceof Player player)) {
            return;
        }

        if (!event.getView()
                .getTitle()
                .equals(title)) {
            return;
        }

        event.setCancelled(true);

        int slot =
                event.getRawSlot();

        if (slot < 0 ||
                slot >= 45) {
            return;
        }

        List<Auction> auctions =
                plugin.getAuctionManager()
                        .getAuctions();

        if (slot >= auctions.size()) {
            return;
        }

        Auction auction =
                auctions.get(slot);

        buy(player, auction);
    }

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
                                    "&cThis auction no longer exists."
                            )
            );

            return;
        }

        if (current.getSeller()
                .equals(buyer.getUniqueId())) {

            buyer.sendMessage(
                    prefix() +
                            color(
                                    "&cYou cannot buy your own item."
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
                                    "&cYou don't have enough money."
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
                                    "&cYour inventory is full."
                            )
            );

            return;
        }

        OfflinePlayer seller =
                Bukkit.getOfflinePlayer(
                        current.getSeller()
                );

        // Withdraw buyer money
        if (!plugin.getEconomy()
                .withdrawPlayer(
                        buyer,
                        price
                )
                .transactionSuccess()) {

            buyer.sendMessage(
                    prefix() +
                            color(
                                    "&cPayment failed."
                            )
            );

            return;
        }

        // Give seller money
        plugin.getEconomy()
                .depositPlayer(
                        seller,
                        price
                );

        // Give item
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
                                "&aYou bought &f" +
                                        current.getItem()
                                                .getType()
                                                .name() +
                                        " &afor &f$" +
                                        plugin.formatMoney(
                                                price
                                        ) +
                                        "&a."
                        )
        );

        if (seller.isOnline()) {

            Player sellerPlayer =
                    seller.getPlayer();

            if (sellerPlayer != null) {

                sellerPlayer.sendMessage(
                        prefix() +
                                color(
                                        "&aYour item was sold for &f$" +
                                                plugin.formatMoney(
                                                        price
                                                ) +
                                                "&a."
                                )
                );
            }
        }

        buyer.closeInventory();

        Bukkit.getScheduler()
                .runTask(
                        plugin,
                        () -> open(buyer)
                );
    }

    private boolean hasInventorySpace(
            Player player,
            ItemStack item
    ) {

        int remaining =
                item.getAmount();

        for (ItemStack content :
                player.getInventory()
                        .getStorageContents()) {

            if (content == null ||
                    content.getType() == Material.AIR) {

                remaining -= item.getMaxStackSize();

                if (remaining <= 0) {
                    return true;
                }
            }
        }

        return false;
    }

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
                list.add(color(line));
            }

            meta.setLore(list);

            item.setItemMeta(meta);
        }

        return item;
    }

    private String prefix() {

        return color(
                plugin.getConfig()
                        .getString(
                                "messages.prefix",
                                "&d&lEAuctions &8» "
                        )
        );
    }

    private String color(String text) {
        return plugin.color(text);
    }
}
