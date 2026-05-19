package com.uhcplugin;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;

public class Commands implements CommandExecutor, TabCompleter {
    private final Main plugin;

    public Commands(Main plugin) { this.plugin = plugin; }

    private boolean admin(CommandSender s) {
        if (s.hasPermission("uhc.admin")) return true;
        s.sendMessage(plugin.msg("no-permission"));
        return false;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (a.length == 0) { help(s); return true; }
        String sub = a[0].toLowerCase();
        switch (sub) {
            case "help" -> help(s);
            case "menu" -> { if (s instanceof Player p) plugin.getGUIManager().openMain(p); }
            case "start" -> { if (admin(s)) plugin.getGameManager().startGame(); }
            case "forcestart" -> { if (admin(s)) plugin.getGameManager().startGame(); }
            case "stop" -> { if (admin(s)) plugin.getGameManager().stopGame(true); }
            case "setcenter" -> {
                if (!admin(s)) return true;
                if (!(s instanceof Player p)) { s.sendMessage("Players only."); return true; }
                plugin.getBorderManager().setCenter(p.getLocation());
                s.sendMessage(ChatColor.GREEN + "Center set.");
            }
            case "setborder" -> {
                if (!admin(s)) return true;
                if (a.length < 4) { s.sendMessage(ChatColor.RED + "/uhc setborder <start> <end> <time>"); return true; }
                try {
                    plugin.getBorderManager().setBorder(Double.parseDouble(a[1]), Double.parseDouble(a[2]), Integer.parseInt(a[3]));
                } catch (NumberFormatException e) { s.sendMessage(ChatColor.RED + "Invalid numbers."); }
            }
            case "grace" -> {
                if (!admin(s)) return true;
                if (a.length < 2) { s.sendMessage(ChatColor.RED + "/uhc grace <seconds>"); return true; }
                try { plugin.getGameManager().setGrace(Integer.parseInt(a[1])); s.sendMessage(ChatColor.GREEN + "Grace set."); }
                catch (NumberFormatException e) { s.sendMessage(ChatColor.RED + "Invalid number."); }
            }
            case "pvp" -> {
                if (!admin(s)) return true;
                if (a.length < 2) { s.sendMessage(ChatColor.RED + "/uhc pvp on|off"); return true; }
                plugin.getGameManager().setPvp(a[1].equalsIgnoreCase("on") || a[1].equalsIgnoreCase("enable"));
            }
            case "team" -> teamCmd(s, a);
            case "scenarios", "scenario" -> scenarioCmd(s, a);
            case "kits", "kit" -> kitCmd(s, a);
            case "revive" -> {
                if (!admin(s)) return true;
                if (a.length < 2) { s.sendMessage(ChatColor.RED + "/uhc revive <player>"); return true; }
                Player t = plugin.getServer().getPlayer(a[1]);
                if (t == null) { s.sendMessage(ChatColor.RED + "Player not found."); return true; }
                plugin.getGameManager().revive(t);
            }
            case "setlives" -> s.sendMessage(ChatColor.YELLOW + "Lives system uses 1 life by default in this build.");
            case "stats" -> {
                if (!(s instanceof Player p)) { s.sendMessage("Players only."); return true; }
                var sm = plugin.getStatsManager();
                UUID id = p.getUniqueId();
                s.sendMessage(ChatColor.GOLD + "--- Your UHC Stats ---");
                s.sendMessage(ChatColor.YELLOW + "Kills: " + ChatColor.WHITE + sm.getKills(id));
                s.sendMessage(ChatColor.YELLOW + "Deaths: " + ChatColor.WHITE + sm.getDeaths(id));
                s.sendMessage(ChatColor.YELLOW + "Wins: " + ChatColor.WHITE + sm.getWins(id));
            }
            case "leave" -> { if (s instanceof Player p) { plugin.getTeamManager().leave(p); s.sendMessage(plugin.msg("team-left")); } }
            case "reload" -> { if (!admin(s)) return true; plugin.reloadConfig(); plugin.reloadMessages(); s.sendMessage(plugin.msg("reload-done")); }
            default -> help(s);
        }
        return true;
    }

    private void teamCmd(CommandSender s, String[] a) {
        if (a.length < 2) {
            if (s instanceof Player p) plugin.getGUIManager().openTeams(p);
            return;
        }
        String op = a[1].toLowerCase();
        switch (op) {
            case "create" -> {
                if (a.length < 3) { s.sendMessage(ChatColor.RED + "/uhc team create <name>"); return; }
                if (plugin.getTeamManager().create(a[2])) s.sendMessage(plugin.msg("team-created", Map.of("name", a[2])));
                else s.sendMessage(ChatColor.RED + "Team exists.");
            }
            case "join" -> {
                if (a.length < 3 || !(s instanceof Player p)) return;
                if (plugin.getTeamManager().join(p, a[2])) s.sendMessage(plugin.msg("team-joined", Map.of("name", a[2])));
                else s.sendMessage(plugin.msg("team-not-found"));
            }
            case "leave" -> { if (s instanceof Player p) { plugin.getTeamManager().leave(p); s.sendMessage(plugin.msg("team-left")); } }
            case "list" -> {
                s.sendMessage(ChatColor.GOLD + "Teams:");
                for (var t : plugin.getTeamManager().all())
                    s.sendMessage(t.color + t.name + ChatColor.GRAY + " (" + t.members.size() + ")");
            }
        }
    }

    private void scenarioCmd(CommandSender s, String[] a) {
        if (a.length < 3) {
            if (s instanceof Player p) plugin.getGUIManager().openScenarios(p);
            return;
        }
        if (!admin(s)) return;
        plugin.getScenarioManager().set(a[1], a[2].equalsIgnoreCase("on"));
    }

    private void kitCmd(CommandSender s, String[] a) {
        if (a.length < 2) {
            if (s instanceof Player p) plugin.getGUIManager().openKits(p);
            return;
        }
        if (!(s instanceof Player p)) return;
        Kits.give(plugin, p, a[1]);
    }

    private void help(CommandSender s) {
        s.sendMessage(ChatColor.GOLD + "--- UHC Commands ---");
        for (String l : new String[]{
            "/uhc menu - open main menu",
            "/uhc start - start game",
            "/uhc stop - stop game",
            "/uhc setcenter - set border center",
            "/uhc setborder <start> <end> <time>",
            "/uhc grace <seconds>",
            "/uhc pvp on|off",
            "/uhc team create|join|leave|list",
            "/uhc scenario <name> on|off",
            "/uhc kit <name>",
            "/uhc revive <player>",
            "/uhc stats",
            "/uhc reload"
        }) s.sendMessage(ChatColor.YELLOW + l);
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command c, String alias, String[] a) {
        if (a.length == 1) return filter(List.of("help","menu","start","stop","setcenter","setborder",
            "grace","pvp","team","scenario","kit","revive","stats","leave","reload"), a[0]);
        if (a.length == 2 && a[0].equalsIgnoreCase("scenario")) return filter(ScenarioManager.ALL, a[1]);
        if (a.length == 3 && a[0].equalsIgnoreCase("scenario")) return filter(List.of("on","off"), a[2]);
        if (a.length == 2 && a[0].equalsIgnoreCase("team")) return filter(List.of("create","join","leave","list"), a[1]);
        if (a.length == 2 && a[0].equalsIgnoreCase("pvp")) return filter(List.of("on","off"), a[1]);
        return Collections.emptyList();
    }

    private List<String> filter(List<String> opts, String prefix) {
        String p = prefix.toLowerCase();
        List<String> out = new ArrayList<>();
        for (String o : opts) if (o.toLowerCase().startsWith(p)) out.add(o);
        return out;
    }
}
