package com.uhcplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class GUIManager implements Listener {
    private final Main plugin;

    public GUIManager(Main plugin) { this.plugin = plugin; }

    public void openMain(Player p) {
        Inventory inv = Bukkit.createInventory(p, 27, ChatColor.DARK_RED + "UHC Menu");
        inv.setItem(10, item(Material.WHITE_BANNER, ChatColor.AQUA + "Teams", "Click to manage teams"));
        inv.setItem(12, item(Material.BOOK, ChatColor.GREEN + "Scenarios", "Toggle scenarios"));
        inv.setItem(14, item(Material.IRON_SWORD, ChatColor.GOLD + "Kits", "Pick a starter kit"));
        inv.setItem(16, item(Material.EMERALD, ChatColor.GREEN + "Start Game", "Admin only"));
        p.openInventory(inv);
    }

    public void openTeams(Player p) {
        Inventory inv = Bukkit.createInventory(p, 54, ChatColor.AQUA + "Teams");
        int i = 0;
        for (TeamManager.UhcTeam t : plugin.getTeamManager().all()) {
            if (i >= 45) break;
            inv.setItem(i++, item(Material.WHITE_WOOL, t.color + t.name,
                "Members: " + t.members.size(), "Click to join"));
        }
        inv.setItem(49, item(Material.BARRIER, ChatColor.RED + "Leave Team", "Click to leave"));
        p.openInventory(inv);
    }

    public void openScenarios(Player p) {
        Inventory inv = Bukkit.createInventory(p, 27, ChatColor.GREEN + "Scenarios");
        int i = 10;
        for (String s : ScenarioManager.ALL) {
            boolean on = plugin.getScenarioManager().isEnabled(s);
            inv.setItem(i++, item(on ? Material.LIME_DYE : Material.GRAY_DYE,
                (on ? ChatColor.GREEN : ChatColor.RED) + s.toUpperCase(),
                on ? "Enabled" : "Disabled", "Click to toggle"));
        }
        p.openInventory(inv);
    }

    public void openKits(Player p) {
        Inventory inv = Bukkit.createInventory(p, 27, ChatColor.GOLD + "Kits");
        var sec = plugin.getConfig().getConfigurationSection("kits");
        int i = 10;
        if (sec != null) for (String k : sec.getKeys(false)) {
            inv.setItem(i++, item(Material.IRON_SWORD, ChatColor.YELLOW + k, "Click to get this kit"));
        }
        p.openInventory(inv);
    }

    private ItemStack item(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            if (lore.length > 0) {
                List<String> l = new ArrayList<>();
                for (String s : lore) l.add(ChatColor.GRAY + s);
                meta.setLore(l);
            }
            it.setItemMeta(meta);
        }
        return it;
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.startsWith(ChatColor.DARK_RED + "UHC Menu")
                && !title.startsWith(ChatColor.AQUA + "Teams")
                && !title.startsWith(ChatColor.GREEN + "Scenarios")
                && !title.startsWith(ChatColor.GOLD + "Kits")) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        ItemStack it = e.getCurrentItem();
        if (it == null || !it.hasItemMeta()) return;
        String name = ChatColor.stripColor(it.getItemMeta().getDisplayName());

        if (title.contains("UHC Menu")) {
            switch (name) {
                case "Teams" -> openTeams(p);
                case "Scenarios" -> openScenarios(p);
                case "Kits" -> openKits(p);
                case "Start Game" -> {
                    if (p.hasPermission("uhc.start") || p.hasPermission("uhc.admin")) {
                        plugin.getGameManager().startGame();
                        p.closeInventory();
                    } else p.sendMessage(plugin.msg("no-permission"));
                }
            }
        } else if (title.contains("Teams")) {
            if (name.equalsIgnoreCase("Leave Team")) {
                plugin.getTeamManager().leave(p);
                p.sendMessage(plugin.msg("team-left"));
                openTeams(p);
            } else {
                if (plugin.getTeamManager().join(p, name)) {
                    p.sendMessage(plugin.msg("team-joined", Map.of("name", name)));
                    openTeams(p);
                }
            }
        } else if (title.contains("Scenarios")) {
            String s = name.toLowerCase();
            if (ScenarioManager.ALL.contains(s)) {
                plugin.getScenarioManager().set(s, !plugin.getScenarioManager().isEnabled(s));
                openScenarios(p);
            }
        } else if (title.contains("Kits")) {
            Kits.give(plugin, p, name);
            p.closeInventory();
        }
    }
}
