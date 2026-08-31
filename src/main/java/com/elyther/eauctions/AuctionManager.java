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

            getLogger().severe(
                    "================================"
            );

            getLogger().severe(
                    "Vault economy was not found!"
            );

            getLogger().severe(
                    "Install Vault + an economy plugin."
            );

            getLogger().severe(
                    "EAuctions has been disabled."
            );

            getLogger().severe(
                    "================================"
            );

            Bukkit.getPluginManager()
                    .disablePlugin(this);

            return;
        }

        // ==========================================
        // AUCTION MANAGER
        // ==========================================

        auctionManager =
                new AuctionManager(this);

        // ==========================================
        // AUCTION GUI
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
                    "Command /ah is missing from plugin.yml!"
            );

            Bukkit.getPluginManager()
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

        getLogger().info(
                "================================"
        );

        getLogger().info(
                "        EAuctions Enabled"
        );

        getLogger().info(
                "================================"
        );

        getLogger().info(
                "Vault: Connected"
        );

        getLogger().info(
                "Economy: " +
                        economy.getName()
        );

        getLogger().info(
                "Command: /ah"
        );

        getLogger().info(
                "Database: SQLite"
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

        if (auctionManager != null) {

            auctionManager.close();
        }

        getLogger().info(
                "EAuctions disabled."
        );
    }

    // ==========================================
    // VAULT ECONOMY
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
    // PARSE PRICE
    // ==========================================

    public double parsePrice(
            String input
    ) {

        if (input == null ||
                input.isBlank()) {

            return -1;
        }

        input =
                input
                        .toLowerCase()
                        .replace(",", "")
                        .trim();

        try {

            double multiplier = 1.0;

            // ==============================
            // THOUSAND
            // 5k
            // ==============================

            if (input.endsWith("k")) {

                multiplier = 1_000.0;

                input =
                        input.substring(
                                0,
                                input.length() - 1
                        );
            }

            // ==============================
            // MILLION
            // 5m
            // ==============================

            else if (input.endsWith("m")) {

                multiplier = 1_000_000.0;

                input =
                        input.substring(
                                0,
                                input.length() - 1
                        );
            }

            // ==============================
            // BILLION
            // 5b
            // ==============================

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

            double result =
                    number * multiplier;

            if (result <= 0 ||
                    Double.isNaN(result) ||
                    Double.isInfinite(result)) {

                return -1;
            }

            return result;

        } catch (NumberFormatException e) {

            return -1;
        }
    }

    // ==========================================
    // FORMAT MONEY
    // ==========================================

    public String formatMoney(
            double amount
    ) {

        if (amount >= 1_000_000_000) {

            return formatNumber(
                    amount / 1_000_000_000
            ) + "b";
        }

        if (amount >= 1_000_000) {

            return formatNumber(
                    amount / 1_000_000
            ) + "m";
        }

        if (amount >= 1_000) {

            return formatNumber(
                    amount / 1_000
            ) + "k";
        }

        return formatNumber(amount);
    }

    // ==========================================
    // FORMAT NUMBER
    // ==========================================

    private String formatNumber(
            double number
    ) {

        if (number == Math.floor(number)) {

            return String.format(
                    "%.0f",
                    number
            );
        }

        if (number * 10 ==
                Math.floor(number * 10)) {

            return String.format(
                    "%.1f",
                    number
            );
        }

        return String.format(
                "%.2f",
                number
        );
    }

    // ==========================================
    // COLOR
    // ==========================================

    public String color(
            String text
    ) {

        if (text == null) {

            return "";
        }

        return org.bukkit.ChatColor
                .translateAlternateColorCodes(
                        '&',
                        text
                );
    }
}
