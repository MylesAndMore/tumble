package com.MylesAndMore.Tumble.plugin;

import com.MylesAndMore.Tumble.game.Arena;
import com.MylesAndMore.Tumble.game.Game;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Objects;

import static com.MylesAndMore.Tumble.Main.plugin;

public class ConfigManager {
    public static HashMap<String, Arena> arenas;
    public static Location lobby;
    public static Location winnerLobby;
    public static Location waitArea;
    public static boolean HideLeaveJoin;
    public static int waitDuration;

    /**
     * Reads config file and populates values above
     */
    public static void readConfig() {
        FileConfiguration config = plugin.getConfig();
        
        // arenas
        if (config.getConfigurationSection("arenas") == null) {
            plugin.getLogger().warning("Section arenas is missing from config");
            return;
        }
        arenas = new HashMap<>();
        for (String arenaName: Objects.requireNonNull(config.getConfigurationSection("arenas")).getKeys(false)) {
            ConfigurationSection section = Objects.requireNonNull(config.getConfigurationSection("arenas")).getConfigurationSection(arenaName);

            Result<Location> res = readWorld(section);
            if (!res.success) {
                plugin.getLogger().warning("Failed to load arena "+arenaName+": "+res.error);
                continue;
            }

            arenas.put(arenaName, new Arena(arenaName, res.value));
        }

        // lobby
        {
            Result<Location>res = readWorld(config.getConfigurationSection("lobby-spawn"));
            if (!res.success) {
                plugin.getLogger().warning("Failed to load lobby: "+res.error);
                plugin.getLogger().severe("^ THIS IS REQUIRED, PLEASE FIX TO AVOID UNDEFINED BEHAVIOR");
            }

            lobby = res.value;
        }
        
        // winner lobby
        if (config.getBoolean("enable-winner-lobby-spawn")) {
            Result<Location>res = readWorld(config.getConfigurationSection("winner-lobby-spawn"));
            if (!res.success) {
                plugin.getLogger().warning("Failed to load winner lobby: "+res.error);
            }

            winnerLobby = res.value;
        }
        
        // wait area
        if (config.getBoolean("enable-wait-area")) {
            Result<Location>res = readWorld(config.getConfigurationSection("wait-area"));
            if (!res.success) {
                plugin.getLogger().warning("Failed to load winner lobby: "+res.error);
            }

            waitArea = res.value;
        }
        
        // other
        HideLeaveJoin = config.getBoolean("hideJoinLeaveMessages");
        waitDuration = config.getInt("wait-duration", 15);
    }

    /**
     * tries to convert a config section in the following format to a world
     * section:
     *   x: 
     *   y: 
     *   z:
     *   world:
     * @param section the section in the yaml with x, y, z, and world as its children
     * @return result of either: 
     *   success = true and a world
     *   success = false and an error string
     */
    private static Result<Location> readWorld(@Nullable ConfigurationSection section) {

        if (section == null) {
            Result<Location> res = new Result<>();
            res.success = false;
            res.error = "Section missing from config";
            return res;
        }

        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("x");
        if (x==0 || y == 0 || z == 0) {
            Result<Location> res = new Result<>();
            res.success = false;
            res.error = "Arena coordinates are missing or are zero. Coordinates cannot be zero.";
            return res;
        }

        String worldName = section.getString("world");
        if (worldName == null) {
            Result<Location> res = new Result<>();
            res.success = false;
            res.error = "World name is missing";
            return res;
        }

        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            Result<Location> res = new Result<>();
            res.success = false;
            res.error = "Failed to load world " + worldName;
            return res;
        }

        Result<Location> res = new Result<>();
        res.success = true;
        res.value = new Location(world,x,y,z);
        return res;
    }

    /**
     * Searches all arenas for a game that player p is in
     * @param p Player to search for
     * @return the game the player is in, or null if not found
     */
    public static Game findGamePlayerIsIn(Player p) {
        for (Arena a : arenas.values()) {
            if (a.game != null && a.game.gamePlayers.contains(p)) {
                return a.game;
            }
        }
        return null;
    }
}
