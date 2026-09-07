package com.elyther.eauctions;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;

public class EAuctions extends JavaPlugin {

    private Economy economy;
    private AuctionManager auctionManager;
    private AuctionGUI auctionGUI;
    private AuctionSearch auctionSearch;

    private File langFile;
    private FileConfiguration lang;

    @Override
    public void onEnable() {

        saveDefaultConfig();

        // =====================================================
        // LANGUAGE
        // =====================================================

        setupLang();

        // =====================================================
        // VAULT
        // =====================================================

        if (!setupEconomy()) {

            getLogger().severe(
                    "Vault economy not found!"
            );

            getLogger().severe(
                    "Please install Vault and an economy plugin."
            );

            getServer()
                    .getPluginManager()
                    .disablePlugin(this);

            return;
        }

        getLogger().info(
                "Vault economy hooked!"
        );

        // =====================================================
        // AUCTION MANAGER
        // =====================================================

        auctionManager =
                new AuctionManager(this);

        // =====================================================
        // AUCTION GUI
        // =====================================================

        auctionGUI =
                new AuctionGUI(this);

        getServer()
                .getPluginManager()
                .registerEvents(
                        auctionGUI,
                        this
                );

        // =====================================================
        // SEARCH
        // =====================================================

        auctionSearch =
                new AuctionSearch(this);

        getServer()
                .getPluginManager()
                .registerEvents(
                        auctionSearch,
                        this
                );

        // =====================================================
        // COMMAND
        // =====================================================

        AuctionCommand command =
                new AuctionCommand(
                        this,
                        auctionGUI
                );

        if (getCommand("ah") != null) {

            getCommand("ah")
                    .setExecutor(command);

            getCommand("ah")
                    .setTabCompleter(command);

        } else {

            getLogger().severe(
                    "Command /ah not found in plugin.yml!"
            );
        }

        // =====================================================
        // STARTUP
        // =====================================================

        getLogger().info(
                "================================"
        );

        getLogger().info(
                "EAuctions enabled!"
        );

        getLogger().info(
                "Vault: Enabled"
        );

        getLogger().info(
                "Auction GUI: Enabled"
        );

        getLogger().info(
                "Search System: Enabled"
        );

        getLogger().info(
                "Language: lang.yml"
        );

        getLogger().info(
                "Made by Elyther"
        );

        getLogger().info(
                "================================"
        );
    }

    // =========================================================
    // LANGUAGE FILE
    // =========================================================

    private void setupLang() {

        langFile =
                new File(
                        getDataFolder(),
                        "lang.yml"
                );

        if (!langFile.exists()) {

            saveResource(
                    "lang.yml",
                    false
            );
        }

        lang =
                YamlConfiguration.loadConfiguration(
                        langFile
                );
    }

    public String lang(
            String path
    ) {

        if (lang == null) {
            return "";
        }

        String value =
                lang.getString(
                        path,
                        path
                );

        return color(value);
    }

    public void reloadLang() {

        if (langFile == null) {
            setupLang();
            return;
        }

        lang =
                YamlConfiguration.loadConfiguration(
                        langFile
                );
    }

    // =========================================================
    // DISABLE
    // =========================================================

    @Override
    public void onDisable() {

        if (auctionManager != null) {
            auctionManager.save();
        }

        getLogger().info(
                "EAuctions disabled!"
        );
    }

    // =========================================================
    // VAULT
    // =========================================================

    private boolean setupEconomy() {

        if (getServer()
                .getPluginManager()
                .getPlugin("Vault") == null) {

            return false;
        }

        RegisteredServiceProvider<Economy> provider =
                getServer()
                        .getServicesManager()
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

    // =========================================================
    // GETTERS
    // =========================================================

    public Economy getEconomy() {
        return economy;
    }

    public AuctionManager getAuctionManager() {
        return auctionManager;
    }

    public AuctionGUI getAuctionGUI() {
        return auctionGUI;
    }

    public AuctionSearch getAuctionSearch() {
        return auctionSearch;
    }

    // =========================================================
    // COLOR
    // =========================================================

    public String color(
            String text
    ) {

        if (text == null) {
            return "";
        }

        text =
                ChatColor.translateAlternateColorCodes(
                        '&',
                        text
                );

        return translateHexColors(text);
    }

    // =========================================================
    // HEX COLORS
    // =========================================================

    private String translateHexColors(
            String text
    ) {

        StringBuilder result =
                new StringBuilder();

        for (int i = 0;
             i < text.length();
             i++) {

            if (text.charAt(i) == '&' &&
                    i + 7 < text.length() &&
                    text.charAt(i + 1) == '#') {

                String hex =
                        text.substring(
                                i + 2,
                                i + 8
                        );

                if (isHex(hex)) {

                    result.append('§');
                    result.append('x');

                    for (char c :
                            hex.toCharArray()) {

                        result.append('§');
                        result.append(
                                Character.toLowerCase(c)
                        );
                    }

                    i += 7;

                    continue;
                }
            }

            result.append(
                    text.charAt(i)
            );
        }

        return result.toString();
    }

    private boolean isHex(
            String text
    ) {

        if (text == null ||
                text.length() != 6) {

            return false;
        }

        for (char c :
                text.toCharArray()) {

            if (!(
                    (c >= '0' && c <= '9') ||
                    (c >= 'a' && c <= 'f') ||
                    (c >= 'A' && c <= 'F')
            )) {

                return false;
            }
        }

        return true;
    }

    // =========================================================
    // PRICE PARSER
    // =========================================================

    public double parsePrice(
            String input
    ) {

        if (input == null ||
                input.isEmpty()) {

            return -1;
        }

        String value =
                input
                        .toLowerCase()
                        .replace(",", "")
                        .replace("$", "")
                        .trim();

        try {

            double multiplier = 1;

            if (value.endsWith("k")) {

                multiplier = 1_000;

                value =
                        value.substring(
                                0,
                                value.length() - 1
                        );

            } else if (value.endsWith("m")) {

                multiplier = 1_000_000;

                value =
                        value.substring(
                                0,
                                value.length() - 1
                        );

            } else if (value.endsWith("b")) {

                multiplier = 1_000_000_000;

                value =
                        value.substring(
                                0,
                                value.length() - 1
                        );

            } else if (value.endsWith("t")) {

                multiplier =
                        1_000_000_000_000D;

                value =
                        value.substring(
                                0,
                                value.length() - 1
                        );
            }

            double price =
                    Double.parseDouble(value)
                            * multiplier;

            if (Double.isNaN(price) ||
                    Double.isInfinite(price)) {

                return -1;
            }

            return price;

        } catch (NumberFormatException e) {

            return -1;
        }
    }

    // =========================================================
    // MONEY FORMAT
    // =========================================================

    public String formatMoney(
            double amount
    ) {

        if (amount >=
                1_000_000_000_000D) {

            return String.format(
                    "%.2ft",
                    amount /
                            1_000_000_000_000D
            );
        }

        if (amount >=
                1_000_000_000) {

            return String.format(
                    "%.2fb",
                    amount /
                            1_000_000_000D
            );
        }

        if (amount >=
                1_000_000) {

            return String.format(
                    "%.2fm",
                    amount /
                            1_000_000D
            );
        }

        if (amount >=
                1_000) {

            return String.format(
                    "%.2fk",
                    amount /
                            1_000D
            );
        }

        if (amount ==
                Math.floor(amount)) {

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
