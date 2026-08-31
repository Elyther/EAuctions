package com.elyther.eauctions;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class EAuctions extends JavaPlugin {

    private Economy economy;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        // Hook into Vault
        if (!setupEconomy()) {
            getLogger().severe("Vault economy could not be found!");
            getLogger().severe("Please install Vault and an economy plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register command
        if (getCommand("ah") != null) {

            AuctionCommand command =
                    new AuctionCommand(this);

            getCommand("ah").setExecutor(command);
            getCommand("ah").setTabCompleter(command);

        } else {

            getLogger().severe(
                    "Command 'ah' was not found in plugin.yml!"
            );

            getServer().getPluginManager()
                    .disablePlugin(this);

            return;
        }

        // Register GUI
        AuctionGUI gui =
                new AuctionGUI(this);

        Bukkit.getPluginManager()
                .registerEvents(gui, this);

        getLogger().info("==============================");
        getLogger().info("       EAuctions Enabled");
        getLogger().info("==============================");
        getLogger().info("Vault: Connected");
        getLogger().info("Economy: " + economy.getName());
        getLogger().info("Made by Elyther");
    }

    @Override
    public void onDisable() {

        getLogger().info("EAuctions disabled.");
    }

    // =========================================================
    // VAULT ECONOMY
    // =========================================================

    private boolean setupEconomy() {

        if (Bukkit.getPluginManager()
                .getPlugin("Vault") == null) {

            getLogger().severe(
                    "Vault is not installed!"
            );

            return false;
        }

        RegisteredServiceProvider<Economy> provider =
                Bukkit.getServicesManager()
                        .getRegistration(Economy.class);

        if (provider == null) {

            getLogger().severe(
                    "No Vault economy provider found!"
            );

            return false;
        }

        economy =
                provider.getProvider();

        return economy != null;
    }

    // =========================================================
    // GET ECONOMY
    // =========================================================

    public Economy getEconomy() {
        return economy;
    }

    // =========================================================
    // FORMAT MONEY
    // =========================================================

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

    // =========================================================
    // PARSE PRICE
    // =========================================================

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

            if (input.endsWith("k")) {

                multiplier = 1_000.0;

                input =
                        input.substring(
                                0,
                                input.length() - 1
                        );

            } else if (input.endsWith("m")) {

                multiplier = 1_000_000.0;

                input =
                        input.substring(
                                0,
                                input.length() - 1
                        );

            } else if (input.endsWith("b")) {

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
                    Double.isInfinite(price) ||
                    Double.isNaN(price)) {

                return -1;
            }

            return price;

        } catch (NumberFormatException e) {

            return -1;
        }
    }
}
