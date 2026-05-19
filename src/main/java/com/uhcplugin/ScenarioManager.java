package com.uhcplugin;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;

public class ScenarioManager {
    public static final List<String> ALL = List.of(
        "cutclean", "speeduhc", "nofall", "timebomb", "blooddiamond", "vanilla"
    );
    private final Main plugin;
    private final Map<String, Boolean> enabled = new HashMap<>();

    public ScenarioManager(Main plugin) {
        this.plugin = plugin;
        ConfigurationSection sec = plugin.getConfig().getConfigurationSection("scenarios");
        for (String s : ALL) {
            boolean v = sec != null && sec.getBoolean(s, false);
            enabled.put(s, v);
        }
    }

    public boolean isEnabled(String name) { return enabled.getOrDefault(name.toLowerCase(), false); }

    public void set(String name, boolean on) {
        String k = name.toLowerCase();
        if (!ALL.contains(k)) return;
        enabled.put(k, on);
        plugin.getConfig().set("scenarios." + k, on);
        plugin.saveConfig();
        Bukkit.broadcastMessage(plugin.msg(on ? "scenario-enabled" : "scenario-disabled",
            Map.of("name", k)));
    }

    public Map<String, Boolean> getAll() { return enabled; }
}
