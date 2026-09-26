package com.elyther.eauctions;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FavoriteManager {

    private final EAuctions plugin;
    private final File file;
    private final YamlConfiguration config;

    // 9 yox, 45 favorit slotu
    private static final int MAX_FAVORITES = 45;

    public FavoriteManager(EAuctions plugin) {
        this.plugin = plugin;

        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        file = new File(
                plugin.getDataFolder(),
                "favorites.yml"
        );

        config = YamlConfiguration.loadConfiguration(file);
    }

    public int getMaxFavorites() {
        return MAX_FAVORITES;
    }

    public ItemStack getFavorite(
            UUID player,
            int slot
    ) {

        if (player == null ||
                slot < 0 ||
                slot >= MAX_FAVORITES) {

            return null;
        }

        String path =
                "favorites."
                        + player
                        + "."
                        + slot;

        return config.getItemStack(path);
    }

    public void setFavorite(
            UUID player,
            int slot,
            ItemStack item
    ) {

        if (player == null ||
                slot < 0 ||
                slot >= MAX_FAVORITES) {

            return;
        }

        String path =
                "favorites."
                        + player
                        + "."
                        + slot;

        if (item == null ||
                item.getType().isAir()) {

            config.set(path, null);

        } else {

            config.set(
                    path,
                    item.clone()
            );
        }

        save();
    }

    public void removeFavorite(
            UUID player,
            int slot
    ) {

        if (player == null ||
                slot < 0 ||
                slot >= MAX_FAVORITES) {

            return;
        }

        config.set(
                "favorites."
                        + player
                        + "."
                        + slot,
                null
        );

        save();
    }

    public List<ItemStack> getFavorites(
            UUID player
    ) {

        List<ItemStack> result =
                new ArrayList<>();

        if (player == null) {
            return result;
        }

        for (int i = 0; i < MAX_FAVORITES; i++) {

            result.add(
                    getFavorite(
                            player,
                            i
                    )
            );
        }

        return result;
    }

    public void save() {

        try {

            config.save(file);

        } catch (IOException e) {

            plugin.getLogger().severe(
                    "favorites.yml kaydedilemedi!"
            );

            e.printStackTrace();
        }
    }

    public void reload() {
        // favorites.yml restart zamanı avtomatik oxunur.
    }
}
