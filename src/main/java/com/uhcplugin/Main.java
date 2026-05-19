package com.uhcplugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class Main extends JavaPlugin {
    private static Main instance;
    private FileConfiguration messages;
    private GameManager gameManager;
    private TeamManager teamManager;
    private ScenarioManager scenarioManager;
    private BorderManager borderManager;
    private GUIManager guiManager;
    private StatsManager statsManager;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        saveResource("messages.yml", false);
        reloadMessages();

        this.teamManager = new TeamManager(this);
        this.scenarioManager = new ScenarioManager(this);
        this.borderManager = new BorderManager(this);
        this.statsManager = new StatsManager(this);
        this.gameManager = new GameManager(this);
        this.guiManager = new GUIManager(this);

        Commands cmd = new Commands(this);
        getCommand("uhc").setExecutor(cmd);
        getCommand("uhc").setTabCompleter(cmd);

        getServer().getPluginManager().registerEvents(new Events(this), this);
        getServer().getPluginManager().registerEvents(guiManager, this);

        getLogger().info("UHCPlugin enabled.");
    }

    @Override
    public void onDisable() {
        if (gameManager != null) gameManager.stopGame(false);
        getLogger().info("UHCPlugin disabled.");
    }

    public void reloadMessages() {
        File f = new File(getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(f);
    }

    public String msg(String key) {
        String s = messages.getString(key, "&cmissing:" + key);
        String prefix = messages.getString("prefix", "");
        return color(prefix + s);
    }

    public String msg(String key, java.util.Map<String, String> placeholders) {
        String s = msg(key);
        for (var e : placeholders.entrySet()) s = s.replace("{" + e.getKey() + "}", e.getValue());
        return s;
    }

    public static String color(String s) {
        return org.bukkit.ChatColor.translateAlternateColorCodes('&', s);
    }

    public static Main get() { return instance; }
    public GameManager getGameManager() { return gameManager; }
    public TeamManager getTeamManager() { return teamManager; }
    public ScenarioManager getScenarioManager() { return scenarioManager; }
    public BorderManager getBorderManager() { return borderManager; }
    public GUIManager getGUIManager() { return guiManager; }
    public StatsManager getStatsManager() { return statsManager; }
}
