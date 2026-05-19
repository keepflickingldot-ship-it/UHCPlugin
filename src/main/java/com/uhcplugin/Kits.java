package com.uhcplugin;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public class Kits {
    public static boolean give(Main plugin, Player p, String name) {
        List<String> items = plugin.getConfig().getStringList("kits." + name);
        if (items.isEmpty()) {
            p.sendMessage(plugin.msg("kit-not-found"));
            return false;
        }
        for (String entry : items) {
            String[] parts = entry.split(" ");
            try {
                Material m = Material.valueOf(parts[0].toUpperCase());
                int amt = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
                p.getInventory().addItem(new ItemStack(m, amt));
            } catch (Exception ignored) {}
        }
        p.sendMessage(plugin.msg("kit-given", Map.of("name", name)));
        return true;
    }
}
