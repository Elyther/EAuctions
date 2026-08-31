package com.elyther.eauctions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AuctionSearch implements Listener {

    private final EAuctions plugin;

    private final Map<UUID, String> searches =
            new HashMap<>();

    private final Map<UUID, Location> signLocations =
            new HashMap<>();

    public AuctionSearch(EAuctions plugin) {
        this.plugin = plugin;
    }

    // =========================================================
    // OPEN SEARCH
    // =========================================================

    public void open(Player player) {

        searches.put(
                player.getUniqueId(),
                ""
        );

        /*
         * Temporary sign location.
         *
         * We use a location around the player and restore
         * the original block after the sign is completed.
         */

        Location location =
                player.getLocation()
                        .getBlock()
                        .getLocation()
                        .add(0, -1, 0);

        Block block =
                location.getBlock();

        // Save location
        signLocations.put(
                player.getUniqueId(),
                location
        );

        /*
         * Only use this if the block can safely be replaced.
         */
        if (!block.getType().isAir()) {

            location =
                    player.getLocation()
                            .getBlock()
                            .getLocation()
                            .add(0, 1, 0);

            block =
                    location.getBlock();

            signLocations.put(
                    player.getUniqueId(),
                    location
            );
        }

        BlockData oldData =
                block.getBlockData();

        // Put temporary sign
        block.setType(
                Material.OAK_SIGN,
                false
        );

        if (!(block.getState()
                instanceof Sign sign)) {

            block.setBlockData(
                    oldData,
                    false
            );

            searches.remove(
                    player.getUniqueId()
            );

            signLocations.remove(
                    player.getUniqueId()
            );

            player.sendMessage(
                    plugin.color(
                            "&d&lEAuctions &8» &cCould not open search."
                    )
            );

            return;
        }

        sign.setLine(
                0,
                "§d§lEAuctions"
        );

        sign.setLine(
                1,
                "§7Search:"
        );

        sign.setLine(
                2,
                "§fType item"
        );

        sign.setLine(
                3,
                "§7then Done"
        );

        sign.update(
                true,
                false
        );

        /*
         * Open sign editor.
         */
        player.openSign(
                sign
        );

        /*
         * Store original block data in memory.
         */
        OldBlockStore.store(
                player.getUniqueId(),
                location,
                oldData
        );
    }

    // =========================================================
    // SIGN COMPLETE
    // =========================================================

    @EventHandler
    public void onSignChange(
            SignChangeEvent event
    ) {

        Player player =
                event.getPlayer();

        UUID uuid =
                player.getUniqueId();

        if (!searches.containsKey(uuid)) {
            return;
        }

        StringBuilder search =
                new StringBuilder();

        for (String line :
                event.getLines()) {

            if (line == null) {
                continue;
            }

            String text =
                    line.trim();

            if (text.isEmpty()) {
                continue;
            }

            /*
             * Ignore our instructions if the player
             * didn't change them.
             */
            if (text.equalsIgnoreCase("Search:") ||
                    text.equalsIgnoreCase("Type item") ||
                    text.equalsIgnoreCase("then Done")) {

                continue;
            }

            if (search.length() > 0) {
                search.append(" ");
            }

            search.append(text);
        }

        String query =
                search.toString().trim();

        searches.put(
                uuid,
                query
        );

        restoreBlock(
                uuid
        );

        if (query.isEmpty()) {

            player.sendMessage(
                    plugin.color(
                            "&d&lEAuctions &8» &cSearch cannot be empty."
                    )
            );

            plugin.getAuctionGUI()
                    .open(player);

            return;
        }

        player.sendMessage(
                plugin.color(
                        "&d&lEAuctions &8» &aSearching for: &f" +
                                query
                )
        );

        plugin.getAuctionGUI()
                .openSearch(
                        player,
                        query
                );
    }

    // =========================================================
    // CURRENT SEARCH
    // =========================================================

    public String getCurrentSearch(
            Player player
    ) {

        return searches.getOrDefault(
                player.getUniqueId(),
                ""
        );
    }

    // =========================================================
    // CLEAR
    // =========================================================

    public void clear(Player player) {

        UUID uuid =
                player.getUniqueId();

        searches.remove(uuid);

        restoreBlock(uuid);
    }

    // =========================================================
    // RESTORE BLOCK
    // =========================================================

    private void restoreBlock(
            UUID uuid
    ) {

        OldBlockStore.StoredBlock stored =
                OldBlockStore.remove(uuid);

        signLocations.remove(uuid);

        if (stored == null) {
            return;
        }

        Block block =
                stored.location()
                        .getBlock();

        block.setBlockData(
                stored.data(),
                false
        );
    }

    // =========================================================
    // SMALL MEMORY STORE
    // =========================================================

    private static class OldBlockStore {

        private static final Map<
                UUID,
                StoredBlock
                > BLOCKS =
                new HashMap<>();

        static void store(
                UUID uuid,
                Location location,
                BlockData data
        ) {

            BLOCKS.put(
                    uuid,
                    new StoredBlock(
                            location,
                            data
                    )
            );
        }

        static StoredBlock remove(
                UUID uuid
        ) {

            return BLOCKS.remove(uuid);
        }

        record StoredBlock(
                Location location,
                BlockData data
        ) {
        }
    }
}
