package com.uhcplugin;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public class StatsManager {
    private final Main plugin;
    private final File file;
    private FileConfiguration data;

    public StatsManager(Main plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "stats.yml");
        if (!file.exists()) { try { plugin.getDataFolder().mkdirs(); file.createNewFile(); } catch (IOException ignored) {} }
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    public int getKills(UUID id) { return data.getInt(id + ".kills", 0); }
    public int getDeaths(UUID id) { return data.getInt(id + ".deaths", 0); }
    public int getWins(UUID id) { return data.getInt(id + ".wins", 0); }

    public void addKill(UUID id) { data.set(id + ".kills", getKills(id) + 1); save(); }
    public void addDeath(UUID id) { data.set(id + ".deaths", getDeaths(id) + 1); save(); }
    public void addWin(UUID id) { data.set(id + ".wins", getWins(id) + 1); save(); }

    private void save() { try { data.save(file); } catch (IOException e) { plugin.getLogger().warning(e.getMessage()); } }
}
