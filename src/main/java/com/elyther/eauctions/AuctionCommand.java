package com.elyther.eauctions;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AuctionCommand
        implements CommandExecutor, TabCompleter {

    private final EAuctions plugin;
    private final AuctionGUI gui;

    public AuctionCommand(
            EAuctions plugin,
            AuctionGUI gui
    ) {

        this.plugin = plugin;
        this.gui = gui;
    }

    // =========================================================
    // COMMAND
    // =========================================================

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {

        if (!(sender instanceof Player)) {

            sender.sendMessage(
                    "Only players can use this command."
            );

            return true;
        }

        Player player =
                (Player) sender;

        /*
         * /ah
         */

        if (args.length == 0) {

            if (!player.hasPermission(
                    "eauctions.use"
            )) {

                send(
                        player,
                        "no-permission"
                );

                return true;
            }

            gui.open(player);

            return true;
        }

        String first =
                args[0].toLowerCase(
                        Locale.ROOT
                );

        /*
         * =====================================================
         * /ah toggle
         * =====================================================
         */

        if (first.equals("toggle")) {

            if (!player.hasPermission(
                    "eauctions.use"
            )) {

                send(
                        player,
                        "no-permission"
                );

                return true;
            }

            gui.togglePurchaseConfirmation(
                    player
            );

            return true;
        }

        /*
         * =====================================================
         * /ah reload
         * =====================================================
         */

        if (first.equals("reload")) {

            if (!player.hasPermission(
                    "eauctions.reload"
            )) {

                send(
                        player,
                        "no-permission"
                );

                return true;
            }

            plugin.reloadConfig();

            player.sendMessage(
                    color(
                            plugin.getConfig()
                                    .getString(
                                            "messages.prefix",
                                            "&d&lEAuctions &8» "
                                    )
                                    +
                                    plugin.getConfig()
                                            .getString(
                                                    "messages.reload",
                                                    "&aConfiguration reloaded."
                                            )
                    )
            );

            return true;
        }

        /*
         * =====================================================
         * /ah sell <price>
         * =====================================================
         */

        if (first.equals("sell")) {

            if (!player.hasPermission(
                    "eauctions.sell"
            )) {

                send(
                        player,
                        "no-permission"
                );

                return true;
            }

            if (args.length < 2) {

                player.sendMessage(
                        color(
                                "&d&lEAuctions &8» "
                                        +
                                        "&7Usage: &f/ah sell <price>"
                        )
                );

                player.sendMessage(
                        color(
                                "&7Examples: &f5k &7| &f2.5k &7| &f1m &7| &f1.5m &7| &f1b"
                        )
                );

                return true;
            }

            double price =
                    plugin.parsePrice(
                            args[1]
                    );

            if (price <= 0) {

                send(
                        player,
                        "invalid-price"
                );

                return true;
            }

            ItemStack item =
                    player.getInventory()
                            .getItemInMainHand();

            if (item.getType() ==
                    Material.AIR) {

                send(
                        player,
                        "invalid-item"
                );

                return true;
            }

            plugin.getAuctionManager()
                    .addAuction(
                            player.getUniqueId(),
                            item,
                            price
                    );

            player.getInventory()
                    .setItemInMainHand(
                            null
                    );

            String formattedPrice =
                    plugin.formatMoney(
                            price
                    );

            String message =
                    plugin.getConfig()
                            .getString(
                                    "messages.sold",
                                    "&aYour item has been listed for &f%price%&a."
                            );

            message =
                    message.replace(
                            "%price%",
                            plugin.getConfig()
                                    .getString(
                                            "auction.currency-symbol",
                                            "$"
                                    )
                                    +
                                    formattedPrice
                    );

            message =
                    message.replace(
                            "%amount%",
                            String.valueOf(
                                    item.getAmount()
                            )
                    );

            player.sendMessage(
                    color(
                            plugin.getConfig()
                                    .getString(
                                            "messages.prefix",
                                            "&d&lEAuctions &8» "
                                    )
                                    +
                                    message
                    )
            );

            return true;
        }

        /*
         * =====================================================
         * /ah <item>
         * =====================================================
         */

        if (!player.hasPermission(
                "eauctions.use"
        )) {

            send(
                    player,
                    "no-permission"
            );

            return true;
        }

        StringBuilder search =
                new StringBuilder();

        for (String arg : args) {

            if (search.length() > 0) {
                search.append(" ");
            }

            search.append(arg);
        }

        gui.openSearch(
                player,
                search.toString()
        );

        return true;
    }

    // =========================================================
    // TAB COMPLETE
    // =========================================================

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {

        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player =
                (Player) sender;

        /*
         * /ah <TAB>
         */

        if (args.length == 1) {

            List<String> suggestions =
                    new ArrayList<>();

            suggestions.add(
                    "toggle"
            );

            if (player.hasPermission(
                    "eauctions.sell"
            )) {

                suggestions.add(
                        "sell"
                );
            }

            if (player.hasPermission(
                    "eauctions.reload"
            )) {

                suggestions.add(
                        "reload"
                );
            }

            for (Material material :
                    Material.values()) {

                if (!material.isItem()) {
                    continue;
                }

                String name =
                        material.name()
                                .toLowerCase(
                                        Locale.ROOT
                                );

                if (!suggestions.contains(
                        name
                )) {

                    suggestions.add(name);
                }
            }

            String input =
                    args[0].toLowerCase(
                            Locale.ROOT
                    );

            List<String> result =
                    new ArrayList<>();

            for (String suggestion :
                    suggestions) {

                if (suggestion.startsWith(
                        input
                )) {

                    result.add(
                            suggestion
                    );
                }
            }

            Collections.sort(result);

            return result;
        }

        /*
         * /ah sell <TAB>
         */

        if (args.length == 2 &&
                args[0].equalsIgnoreCase(
                        "sell"
                )) {

            if (!player.hasPermission(
                    "eauctions.sell"
            )) {

                return Collections.emptyList();
            }

            return List.of(
                    "1k",
                    "5k",
                    "10k",
                    "50k",
                    "100k",
                    "500k",
                    "1m",
                    "5m",
                    "10m",
                    "100m",
                    "1b"
            );
        }

        return Collections.emptyList();
    }

    // =========================================================
    // SEND
    // =========================================================

    private void send(
            Player player,
            String path
    ) {

        String prefix =
                plugin.getConfig()
                        .getString(
                                "messages.prefix",
                                "&d&lEAuctions &8» "
                        );

        String message =
                plugin.getConfig()
                        .getString(
                                "messages." + path,
                                "&cSomething went wrong."
                        );

        player.sendMessage(
                color(
                        prefix + message
                )
        );
    }

    // =========================================================
    // COLOR
    // =========================================================

    private String color(
            String text
    ) {

        if (text == null) {
            return "";
        }

        return plugin.color(text);
    }
}
