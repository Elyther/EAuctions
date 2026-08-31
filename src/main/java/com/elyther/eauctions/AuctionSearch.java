package com.elyther.eauctions;

import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuctionSearch implements Listener {

    private final EAuctions plugin;

    private final Map<UUID, String> searchPlayers =
            new HashMap<>();

    public AuctionSearch(EAuctions plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {

        searchPlayers.put(
                player.getUniqueId(),
                ""
        );

        // Yaxınlıqda virtual/real sign yaratmaq əvəzinə
        // oyunçunun qarşısına müvəqqəti sign editor açılır.
        player.sendSignChange(
                player.getLocation().add(0, 0, 0).getBlock(),
                new String[]{
                        "^^^^^^^^^^^^",
                        "Search Item",
                        "____________",
                        "Type here"
                }
        );

        // Sign editor yalnız həqiqi sign block üçün işlədiyi
        // üçün aşağıdakı üsul istifadə olunmalıdır.
        player.openSign(
                new SearchSign(
                        plugin,
                        player
                ).getSign()
        );
    }

    @EventHandler
    public void onSignChange(SignChangeEvent event) {

        Player player = event.getPlayer();

        if (!searchPlayers.containsKey(
                player.getUniqueId()
        )) {
            return;
        }

        StringBuilder search =
                new StringBuilder();

        for (String line : event.getLines()) {

            if (line == null) {
                continue;
            }

            String text =
                    line.trim();

            if (text.isEmpty()) {
                continue;
            }

            if (search.length() > 0) {
                search.append(" ");
            }

            search.append(text);
        }

        searchPlayers.remove(
                player.getUniqueId()
        );

        String query =
                search.toString().trim();

        if (query.isEmpty()) {

            player.sendMessage(
                    plugin.color(
                            "&d&lEAuctions &8» &cSearch cannot be empty."
                    )
            );

            return;
        }

        plugin.getAuctionGUI()
                .openSearch(
                        player,
                        query
                );
    }

    public void remove(Player player) {

        searchPlayers.remove(
                player.getUniqueId()
        );
    }
}
