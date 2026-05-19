package com.uhcplugin;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

public class ScoreboardManager {
    public static void updateAll(Main plugin) {
        GameManager gm = plugin.getGameManager();
        for (Player p : Bukkit.getOnlinePlayers()) update(plugin, p, gm);
    }

    public static void update(Main plugin, Player p, GameManager gm) {
        Scoreboard sb = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective obj = sb.registerNewObjective("uhc", "dummy",
            ChatColor.RED + "" + ChatColor.BOLD + "UHC");
        obj.setDisplaySlot(DisplaySlot.SIDEBAR);

        int line = 10;
        set(obj, ChatColor.GRAY + "---------------", line--);
        set(obj, ChatColor.YELLOW + "Alive: " + ChatColor.WHITE + gm.getAlive().size(), line--);
        int border = (int) p.getWorld().getWorldBorder().getSize();
        set(obj, ChatColor.YELLOW + "Border: " + ChatColor.WHITE + border, line--);
        if (gm.isRunning() && !gm.isPvpEnabled()) {
            set(obj, ChatColor.YELLOW + "Grace: " + ChatColor.WHITE + gm.getGraceSecondsLeft() + "s", line--);
        } else {
            set(obj, ChatColor.YELLOW + "PvP: " + (gm.isPvpEnabled() ? ChatColor.GREEN + "ON" : ChatColor.RED + "OFF"), line--);
        }
        int kills = plugin.getStatsManager().getKills(p.getUniqueId());
        set(obj, ChatColor.YELLOW + "Kills: " + ChatColor.WHITE + kills, line--);
        String team = plugin.getTeamManager().getTeamOf(p.getUniqueId());
        set(obj, ChatColor.YELLOW + "Team: " + ChatColor.WHITE + (team == null ? "none" : team), line--);
        set(obj, ChatColor.GRAY + "" + ChatColor.ITALIC + "uhc.lovable", line--);

        p.setScoreboard(sb);
    }

    private static void set(Objective o, String text, int score) {
        // ensure unique entries
        String entry = text;
        while (o.getScoreboard().getEntries().contains(entry)) entry += ChatColor.RESET;
        o.getScore(entry).setScore(score);
    }
}
