package com.uhcplugin;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class GameManager {
    public enum State { LOBBY, RUNNING, ENDED }

    private final Main plugin;
    private State state = State.LOBBY;
    private final Set<UUID> alive = new HashSet<>();
    private final Set<UUID> spectators = new HashSet<>();
    private boolean pvpEnabled = false;
    private long startTimeMs;
    private int graceSeconds;
    private BukkitRunnable tickTask;

    public GameManager(Main plugin) { this.plugin = plugin; }

    public State getState() { return state; }
    public boolean isRunning() { return state == State.RUNNING; }
    public boolean isPvpEnabled() { return pvpEnabled; }
    public Set<UUID> getAlive() { return alive; }
    public Set<UUID> getSpectators() { return spectators; }

    public void startGame() {
        if (state == State.RUNNING) return;
        state = State.RUNNING;
        pvpEnabled = false;
        startTimeMs = System.currentTimeMillis();
        graceSeconds = plugin.getConfig().getInt("grace-time", 1200);

        alive.clear();
        spectators.clear();
        for (Player p : Bukkit.getOnlinePlayers()) {
            alive.add(p.getUniqueId());
            p.setGameMode(GameMode.SURVIVAL);
            p.setHealth(20.0);
            p.setFoodLevel(20);
            p.getInventory().clear();
            p.setExp(0); p.setLevel(0);
        }

        plugin.getBorderManager().applyStartBorder();
        Bukkit.broadcastMessage(plugin.msg("game-started"));

        // disable natural regen via gamerule
        for (World w : Bukkit.getWorlds()) {
            w.setGameRule(GameRule.NATURAL_REGENERATION, false);
            w.setDifficulty(Difficulty.HARD);
            w.setTime(0);
            w.setStorm(false);
        }

        startTickLoop();
    }

    private void startTickLoop() {
        if (tickTask != null) tickTask.cancel();
        tickTask = new BukkitRunnable() {
            int secs = 0;
            @Override public void run() {
                if (state != State.RUNNING) { cancel(); return; }
                secs++;
                if (!pvpEnabled) {
                    int left = graceSeconds - secs;
                    if (left == 60 || left == 30 || left == 10 || (left <= 5 && left > 0)) {
                        Bukkit.broadcastMessage(plugin.msg("grace-ending", Map.of("seconds", String.valueOf(left))));
                    }
                    if (left <= 0) {
                        pvpEnabled = true;
                        Bukkit.broadcastMessage(plugin.msg("pvp-enabled"));
                    }
                }
                ScoreboardManager.updateAll(plugin);
            }
        };
        tickTask.runTaskTimer(plugin, 20L, 20L);
    }

    public void stopGame(boolean announce) {
        state = State.ENDED;
        pvpEnabled = false;
        if (tickTask != null) { tickTask.cancel(); tickTask = null; }
        if (announce) Bukkit.broadcastMessage(plugin.msg("game-stopped"));
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setGameMode(GameMode.SURVIVAL);
        }
        alive.clear();
        spectators.clear();
        state = State.LOBBY;
    }

    public void setPvp(boolean on) {
        pvpEnabled = on;
        Bukkit.broadcastMessage(plugin.msg(on ? "pvp-enabled" : "pvp-disabled"));
    }

    public void setGrace(int seconds) {
        this.graceSeconds = seconds;
        plugin.getConfig().set("grace-time", seconds);
        plugin.saveConfig();
    }

    public void eliminate(Player p) {
        UUID id = p.getUniqueId();
        if (!alive.remove(id)) return;
        spectators.add(id);
        p.setGameMode(GameMode.SPECTATOR);
        Bukkit.broadcastMessage(plugin.msg("player-died", Map.of("player", p.getName())));
        plugin.getStatsManager().addDeath(id);
        checkWin();
    }

    public void revive(Player p) {
        UUID id = p.getUniqueId();
        spectators.remove(id);
        alive.add(id);
        p.setGameMode(GameMode.SURVIVAL);
        p.setHealth(20.0);
        p.setFoodLevel(20);
        Bukkit.broadcastMessage(plugin.msg("revived", Map.of("player", p.getName())));
    }

    private void checkWin() {
        if (state != State.RUNNING) return;
        // team win check
        TeamManager tm = plugin.getTeamManager();
        Set<String> aliveTeams = new HashSet<>();
        List<UUID> soloAlive = new ArrayList<>();
        for (UUID id : alive) {
            String t = tm.getTeamOf(id);
            if (t != null) aliveTeams.add(t);
            else soloAlive.add(id);
        }
        if (aliveTeams.size() + soloAlive.size() <= 1) {
            if (!aliveTeams.isEmpty()) {
                String winner = aliveTeams.iterator().next();
                Bukkit.broadcastMessage(plugin.msg("team-won", Map.of("team", winner)));
                for (UUID id : tm.getMembers(winner)) plugin.getStatsManager().addWin(id);
            } else if (!soloAlive.isEmpty()) {
                Player p = Bukkit.getPlayer(soloAlive.get(0));
                String name = p != null ? p.getName() : soloAlive.get(0).toString();
                Bukkit.broadcastMessage(plugin.msg("player-won", Map.of("player", name)));
                plugin.getStatsManager().addWin(soloAlive.get(0));
            }
            stopGame(false);
        }
    }

    public int getGraceSecondsLeft() {
        if (!isRunning() || pvpEnabled) return 0;
        long elapsed = (System.currentTimeMillis() - startTimeMs) / 1000L;
        return Math.max(0, graceSeconds - (int) elapsed);
    }
}
