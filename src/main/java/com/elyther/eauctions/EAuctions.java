package com.elyther.eauctions;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EAuctions extends JavaPlugin {

    private Economy economy;
    private AuctionManager auctionManager;
    private AuctionGUI auctionGUI;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        // Vault
        if (!setupEconomy()) {
            getLogger().severe("Vault economy not found!");
            getLogger().severe("Please install Vault and an economy plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        auctionManager = new AuctionManager(this);
        auctionGUI = new AuctionGUI(this);

        AuctionCommand command =
                new AuctionCommand(this, auctionGUI);

        if (getCommand("ah") != null) {
            getCommand("ah").setExecutor(command);
            getCommand("ah").setTabCompleter(command);
        }

        getServer().getPluginManager().registerEvents(
                auctionGUI,
                this
        );

        getLogger().info("EAuctions enabled!");
    }

    @Override
    public void onDisable() {

        if (auctionManager != null) {
            auctionManager.save();
        }

        getLogger().info("EAuctions disabled!");
    }

    private boolean setupEconomy() {

        if (getServer().getPluginManager()
                .getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> provider =
                getServer().getServicesManager()
                        .getRegistration(Economy.class);

        if (provider == null) {
            return false;
        }

        economy = provider.getProvider();
        return economy != null;
    }

    public Economy getEconomy() {
        return economy;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public AuctionGUI getAuctionGUI() {
        return auctionGUI;
    }

    public String color(String text) {

        if (text == null) {
            return "";
        }

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }

    // 1k = 1000
    // 2.5k = 2500
    // 1m = 1000000
    // 1b = 1000000000
    // 1t = 1000000000000

    public double parsePrice(String input) {

        if (input == null || input.isEmpty()) {
            return -1;
        }

        String value = input
                .toLowerCase()
                .replace(",", "")
                .replace("$", "")
                .trim();

        try {

            double multiplier = 1;

            if (value.endsWith("k")) {
                multiplier = 1_000;
                value = value.substring(
                        0,
                        value.length() - 1
                );
            }

            else if (value.endsWith("m")) {
                multiplier = 1_000_000;
                value = value.substring(
                        0,
                        value.length() - 1
                );
            }

            else if (value.endsWith("b")) {
                multiplier = 1_000_000_000;
                value = value.substring(
                        0,
                        value.length() - 1
                );
            }

            else if (value.endsWith("t")) {
                multiplier = 1_000_000_000_000D;
                value = value.substring(
                        0,
                        value.length() - 1
                );
            }

            double price =
                    Double.parseDouble(value) * multiplier;

            if (Double.isNaN(price) ||
                    Double.isInfinite(price)) {
                return -1;
            }

            return price;

        } catch (NumberFormatException e) {
            return -1;
        }
    }

    public String formatMoney(double amount) {

        if (amount >= 1_000_000_000_000D) {
            return String.format(
                    "%.2ft",
                    amount / 1_000_000_000_000D
            );
        }

        if (amount >= 1_000_000_000) {
            return String.format(
                    "%.2fb",
                    amount / 1_000_000_000D
            );
        }

        if (amount >= 1_000_000) {
            return String.format(
                    "%.2fm",
                    amount / 1_000_000D
            );
        }

        if (amount >= 1_000) {
            return String.format(
                    "%.2fk",
                    amount / 1_000D
            );
        }

        if (amount == Math.floor(amount)) {
            return String.format(
                    "%.0f",
                    amount
            );
        }

        return String.format(
                "%.2f",
                amount
        );
    }
}
