package com.elyther.eauctions;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.text.DecimalFormat;
import java.util.Locale;

public class EAuctions extends JavaPlugin {

    private Economy economy;
    private AuctionManager auctionManager;
    private AuctionGUI auctionGUI;

    @Override
    public void onEnable() {

        // =====================================================
        // CONFIG
        // =====================================================

        saveDefaultConfig();

        // =====================================================
        // VAULT
        // =====================================================

        if (!setupEconomy()) {

            getLogger().severe("======================================");
            getLogger().severe("Vault economy provider not found!");
            getLogger().severe("EAuctions will be disabled.");
            getLogger().severe("Install Vault + an economy plugin.");
            getLogger().severe("======================================");

            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        getLogger().info("Vault economy hooked successfully!");

        // =====================================================
        // AUCTION MANAGER
        // =====================================================

        auctionManager = new AuctionManager(this);

        // =====================================================
        // GUI
        // =====================================================

        auctionGUI = new AuctionGUI(this);

        // =====================================================
        // COMMAND
        // =====================================================

        AuctionCommand command =
                new AuctionCommand(this, auctionGUI);

        if (getCommand("ah") != null) {

            getCommand("ah").setExecutor(command);
            getCommand("ah").setTabCompleter(command);

        } else {

            getLogger().severe("Command /ah was not found in plugin.yml!");
        }

        // =====================================================
        // START
        // =====================================================

        getLogger().info("--------------------------------------");
        getLogger().info("EAuctions enabled!");
        getLogger().info("Vault: Connected");
        getLogger().info("Auction House: Ready");
        getLogger().info("Made by Elyther");
        getLogger().info("--------------------------------------");
    }

    // =========================================================
    // DISABLE
    // =========================================================

    @Override
    public void onDisable() {

        getLogger().info("EAuctions disabled.");
    }

    // =========================================================
    // VAULT ECONOMY
    // =========================================================

    private boolean setupEconomy() {

        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> provider =
                Bukkit.getServicesManager()
                        .getRegistration(Economy.class);

        if (provider == null) {
            return false;
        }

        economy = provider.getProvider();

        return economy != null;
    }

    // =========================================================
    // GET ECONOMY
    // =========================================================

    public Economy getEconomy() {
        return economy;
    }

    // =========================================================
    // GET AUCTION MANAGER
    // =========================================================

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    // =========================================================
    // GET GUI
    // =========================================================

    public AuctionGUI getAuctionGUI() {
        return auctionGUI;
    }

    // =========================================================
    // PARSE PRICE
    // =========================================================
    /*
     * Supports:
     *
     * 100
     * 1k
     * 5k
     * 2.5k
     * 1m
     * 2.5m
     * 1b
     * 1.5b
     */

    public double parsePrice(String input) {

        if (input == null || input.isBlank()) {
            return -1;
        }

        String value =
                input
                        .toLowerCase(Locale.ROOT)
                        .replace(",", "")
                        .replace("$", "")
                        .trim();

        try {

            double multiplier = 1.0;

            if (value.endsWith("k")) {

                multiplier = 1_000.0;
                value = value.substring(0, value.length() - 1);

            } else if (value.endsWith("m")) {

                multiplier = 1_000_000.0;
                value = value.substring(0, value.length() - 1);

            } else if (value.endsWith("b")) {

                multiplier = 1_000_000_000.0;
                value = value.substring(0, value.length() - 1);

            } else if (value.endsWith("t")) {

                multiplier = 1_000_000_000_000.0;
                value = value.substring(0, value.length() - 1);
            }

            double number =
                    Double.parseDouble(value);

            if (number < 0) {
                return -1;
            }

            return number * multiplier;

        } catch (NumberFormatException exception) {

            return -1;
        }
    }

    // =========================================================
    // FORMAT MONEY
    // =========================================================

    public String formatMoney(double amount) {

        if (amount >= 1_000_000_000_000L) {

            return formatShort(
                    amount / 1_000_000_000_000L
            ) + "t";

        }

        if (amount >= 1_000_000_000L) {

            return formatShort(
                    amount / 1_000_000_000L
            ) + "b";

        }

        if (amount >= 1_000_000L) {

            return formatShort(
                    amount / 1_000_000L
            ) + "m";

        }

        if (amount >= 1_000L) {

            return formatShort(
                    amount / 1_000L
            ) + "k";
        }

        if (amount == Math.floor(amount)) {

            return String.format(
                    Locale.US,
                    "%.0f",
                    amount
            );
        }

        return String.format(
                Locale.US,
                "%.2f",
                amount
        );
    }

    // =========================================================
    // SHORT NUMBER
    // =========================================================

    private String formatShort(double number) {

        if (number == Math.floor(number)) {

            return String.format(
                    Locale.US,
                    "%.0f",
                    number
            );
        }

        DecimalFormat format =
                new DecimalFormat("0.##");

        return format.format(number);
    }

    // =========================================================
    // COLOR
    // =========================================================

    public String color(String text) {

        if (text == null) {
            return "";
        }

        // =====================================================
        // HEX COLORS
        // Example: &#FF00FF
        // =====================================================

        text = translateHexColors(text);

        // =====================================================
        // NORMAL COLORS
        // &a &b &c etc.
        // =====================================================

        return ChatColor.translateAlternateColorCodes(
                '&',
                text
        );
    }

    // =========================================================
    // HEX COLOR
    // =========================================================

    private String translateHexColors(String text) {

        StringBuilder result =
                new StringBuilder();

        for (int i = 0; i < text.length(); i++) {

            char current =
                    text.charAt(i);

            if (
                    current == '&'
                            &&
                    i + 7 < text.length()
                            &&
                    text.charAt(i + 1) == '#'
            ) {

                String hex =
                        text.substring(
                                i + 2,
                                i + 8
                        );

                if (isHex(hex)) {

                    result.append("§x");

                    for (char character :
                            hex.toCharArray()) {

                        result.append('§');
                        result.append(
                                Character.toLowerCase(
                                        character
                                )
                        );
                    }

                    i += 7;
                    continue;
                }
            }

            result.append(current);
        }

        return result.toString();
    }

    // =========================================================
    // CHECK HEX
    // =========================================================

    private boolean isHex(String text) {

        if (text.length() != 6) {
            return false;
        }

        for (char character :
                text.toCharArray()) {

            if (
                    !(
                            character >= '0'
                                    && character <= '9'
                    )
                            &&
                    !(
                            character >= 'a'
                                    && character <= 'f'
                    )
                            &&
                    !(
                            character >= 'A'
                                    && character <= 'F'
                    )
            ) {

                return false;
            }
        }

        return true;
    }
}
