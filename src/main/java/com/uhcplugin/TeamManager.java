package com.uhcplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.*;

public class TeamManager {
    public static class UhcTeam {
        public final String name;
        public ChatColor color = ChatColor.WHITE;
        public final Set<UUID> members = new HashSet<>();
        public UhcTeam(String name) { this.name = name; }
    }

    private final Main plugin;
    private final Map<String, UhcTeam> teams = new LinkedHashMap<>();
    private final Map<UUID, String> playerTeam = new HashMap<>();
    private static final ChatColor[] PALETTE = {
        ChatColor.RED, ChatColor.BLUE, ChatColor.GREEN, ChatColor.YELLOW,
        ChatColor.AQUA, ChatColor.LIGHT_PURPLE, ChatColor.GOLD, ChatColor.DARK_GREEN
    };

    public TeamManager(Main plugin) { this.plugin = plugin; }

    public Collection<UhcTeam> all() { return teams.values(); }
    public UhcTeam get(String name) { return teams.get(name.toLowerCase()); }
    public String getTeamOf(UUID id) { return playerTeam.get(id); }
    public Set<UUID> getMembers(String name) {
        UhcTeam t = get(name); return t == null ? Set.of() : t.members;
    }

    public boolean create(String name) {
        String k = name.toLowerCase();
        if (teams.containsKey(k)) return false;
        UhcTeam t = new UhcTeam(name);
        t.color = PALETTE[teams.size() % PALETTE.length];
        teams.put(k, t);
        return true;
    }

    public boolean join(Player p, String name) {
        UhcTeam t = get(name);
        if (t == null) return false;
        leave(p);
        t.members.add(p.getUniqueId());
        playerTeam.put(p.getUniqueId(), t.name);
        updateDisplay(p, t);
        return true;
    }

    public void leave(Player p) {
        String cur = playerTeam.remove(p.getUniqueId());
        if (cur != null) {
            UhcTeam t = get(cur);
            if (t != null) t.members.remove(p.getUniqueId());
        }
        p.setDisplayName(p.getName());
        p.setPlayerListName(p.getName());
    }

    private void updateDisplay(Player p, UhcTeam t) {
        String n = t.color + p.getName() + ChatColor.RESET;
        p.setDisplayName(n);
        p.setPlayerListName(n);
    }

    public boolean areAllied(Player a, Player b) {
        String ta = playerTeam.get(a.getUniqueId());
        String tb = playerTeam.get(b.getUniqueId());
        return ta != null && ta.equals(tb);
    }

    public void sendTeamChat(Player from, String message) {
        String team = playerTeam.get(from.getUniqueId());
        if (team == null) { from.sendMessage(ChatColor.RED + "You are not in a team."); return; }
        UhcTeam t = get(team);
        String out = ChatColor.GRAY + "[TEAM] " + t.color + from.getName() + ChatColor.WHITE + ": " + message;
        for (UUID id : t.members) {
            Player p = Bukkit.getPlayer(id);
            if (p != null) p.sendMessage(out);
        }
    }
}
