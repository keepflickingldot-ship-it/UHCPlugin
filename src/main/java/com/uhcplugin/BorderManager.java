package com.uhcplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;

import java.util.Map;

public class BorderManager {
    private final Main plugin;
    private Location center;

    public BorderManager(Main plugin) {
        this.plugin = plugin;
        World w = Bukkit.getWorlds().get(0);
        this.center = new Location(w, 0.5, w.getHighestBlockYAt(0, 0), 0.5);
    }

    public void setCenter(Location loc) { this.center = loc; }
    public Location getCenter() { return center; }

    public WorldBorder border() {
        return center.getWorld().getWorldBorder();
    }

    public void applyStartBorder() {
        double start = plugin.getConfig().getInt("border-start", 4000);
        double end = plugin.getConfig().getInt("border-end", 100);
        int time = plugin.getConfig().getInt("border-shrink-time", 3600);
        WorldBorder wb = border();
        wb.setCenter(center.getX(), center.getZ());
        wb.setSize(start);
        wb.setSize(end, time);
        wb.setWarningDistance(20);
        wb.setDamageAmount(0.5);
        Bukkit.broadcastMessage(plugin.msg("border-shrinking", Map.of(
            "start", String.valueOf((int) start),
            "end", String.valueOf((int) end),
            "time", String.valueOf(time)
        )));
    }

    public void setBorder(double start, double end, int time) {
        plugin.getConfig().set("border-start", (int) start);
        plugin.getConfig().set("border-end", (int) end);
        plugin.getConfig().set("border-shrink-time", time);
        plugin.saveConfig();
        applyStartBorder();
    }
}
