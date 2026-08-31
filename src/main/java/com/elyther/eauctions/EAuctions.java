package com.elyther.eauctions;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EAuctions extends JavaPlugin {

    private Economy economy;
    private AuctionManager auctionManager;
    private AuctionGUI auctionGUI;

    @Override
    public void onEnable() {

        // ==========================================
        // CONFIG
        // ==========================================

        saveDefaultConfig();

        // ==========================================
        // VAULT
        // ==========================================

        if (!setupEconomy()) {

            getLogger().severe("==============================");
            getLogger().severe("Vault economy not found!");
            getLogger().severe(
                    "Install Vault and an economy plugin."
            );
            getLogger().severe("==============================");

            getServer().getPluginManager()
                    .disablePlugin(this);

            return;
        }

        // ==========================================
        // AUCTION MANAGER
        // ==========================================

        auctionManager =
                new AuctionManager(this);

        // ==========================================
        // GUI
        // ==========================================

        auctionGUI =
                new AuctionGUI(this);

        Bukkit.getPluginManager()
                .registerEvents(
                        auctionGUI,
                        this
                );

        // ==========================================
        // COMMAND
        // ==========================================

        if (getCommand("ah") == null) {

            getLogger().severe(
                    "Command 'ah' is missing from plugin.yml!"
            );

            getServer().getPluginManager()
                    .disablePlugin(this);

            return;
        }

        AuctionCommand command =
                new AuctionCommand(
                        this,
                        auctionGUI
                );

        getCommand("ah")
                .setExecutor(command);

        getCommand("ah")
                .setTabCompleter(command);

        // ==========================================
        // ENABLE MESSAGE
        // ==========================================

        getLogger().info("==============================");
        getLogger().info("       EAuctions Enabled");
        getLogger().info("==============================");

        getLogger().info(
                "Vault: Connected"
        );

        getLogger().info(
                "Economy: " +
                        economy.getName()
        );

        getLogger().info(
                "Commands: /ah"
        );

        getLogger().info(
                "Made by Elyther"
        );
    }

    // ==========================================
    // DISABLE
    // ==========================================

    @Override
    public void onDisable() {

        getLogger().info(
                "EAuctions disabled."
        );
    }

    // ==========================================
    // VAULT SETUP
    // ==========================================

    private boolean setupEconomy() {

        if (Bukkit.getPluginManager()
                .getPlugin("Vault") == null) {

            return false;
        }

        RegisteredServiceProvider<Economy> provider =
                Bukkit.getServicesManager()
                        .getRegistration(
                                Economy.class
                        );

        if (provider == null) {
            return false;
        }

        economy =
                provider.getProvider();

        return economy != null;
    }

    // ==========================================
    // GET ECONOMY
    // ==========================================

    public Economy getEconomy() {
        return economy;
    }

    // ==========================================
    // GET AUCTION MANAGER
    // ==========================================

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    // ==========================================
    // GET AUCTION GUI
    // ==========================================

    public AuctionGUI getAuctionGUI() {
        return auctionGUI;
    }

    // ==========================================
    // FORMAT MONEY
    // ==========================================

    public String formatMoney(double amount) {

        if (amount >= 1_000_000_000) {

            return String.format(
                    "%.1fb",
                    amount / 1_000_000_000
            );
        }

        if (amount >= 1_000_000) {

            return String.format(
                    "%.1fm",
                    amount / 1_000_000
            );
        }

        if (amount >= 1_000) {

            return String.format(
                    "%.1fk",
                    amount / 1_000
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

    // ==========================================
    // PARSE PRICE
    // ==========================================

    public double parsePrice(String input) {

        if (input == null ||
                input.isEmpty()) {

            return -1;
        }

        input =
                input
                        .toLowerCase()
                        .replace(",", "")
                        .trim();

        try {

            double multiplier = 1.0;

            // 5k
            if (input.endsWith("k")) {

                multiplier = 1_000.0;

                input =
                        input.substring(
                                0,
                                input.length() - 1
                        );
            }

            // 5m
            else if (input.endsWith("m")) {

                multiplier = 1_000_000.0;

                input =
                        input.substring(
                                0,
                                input.length() - 1
                        );
            }

            // 5b
            else if (input.endsWith("b")) {

                multiplier = 1_000_000_000.0;

                input =
                        input.substring(
                                0,
                                input.length() - 1
                        );
            }

            double number =
                    Double.parseDouble(input);

            double price =
                    number * multiplier;

            if (price <= 0 ||
                    Double.isNaN(price) ||
                    Double.isInfinite(price)) {

                return -1;
            }

            return price;

        } catch (NumberFormatException e) {

            return -1;
        }
    }
}
