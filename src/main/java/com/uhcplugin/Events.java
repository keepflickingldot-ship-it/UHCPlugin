package com.uhcplugin;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Events implements Listener {
    private final Main plugin;
    private final Random rng = new Random();

    public Events(Main plugin) {
        this.plugin = plugin;
        registerGoldenHeadRecipe();
    }

    private void registerGoldenHeadRecipe() {
        ItemStack head = goldenHead();
        NamespacedKey key = new NamespacedKey(plugin, "golden_head");
        ShapedRecipe r = new ShapedRecipe(key, head);
        r.shape("GGG", "GSG", "GGG");
        r.setIngredient('G', Material.GOLD_INGOT);
        r.setIngredient('S', Material.PLAYER_HEAD);
        try { Bukkit.addRecipe(r); } catch (Exception ignored) {}
    }

    public static ItemStack goldenHead() {
        ItemStack it = new ItemStack(Material.GOLDEN_APPLE);
        var meta = it.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Golden Head");
        meta.addEnchant(Enchantment.LUCK, 1, true);
        it.setItemMeta(meta);
        return it;
    }

    public static boolean isGoldenHead(ItemStack it) {
        if (it == null || it.getType() != Material.GOLDEN_APPLE) return false;
        return it.hasItemMeta() && it.getItemMeta().hasDisplayName()
            && ChatColor.stripColor(it.getItemMeta().getDisplayName()).equalsIgnoreCase("Golden Head");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        if (plugin.getGameManager().isRunning()
                && !plugin.getGameManager().getAlive().contains(e.getPlayer().getUniqueId())) {
            e.getPlayer().setGameMode(GameMode.SPECTATOR);
            plugin.getGameManager().getSpectators().add(e.getPlayer().getUniqueId());
        }
    }

    @EventHandler
    public void onRegen(EntityRegainHealthEvent e) {
        if (e.getRegainReason() == EntityRegainHealthEvent.RegainReason.SATIATED
            || e.getRegainReason() == EntityRegainHealthEvent.RegainReason.REGEN) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent e) {
        if (!(e.getEntity() instanceof Player victim)) return;
        if (!(e.getDamager() instanceof Player att)) return;
        GameManager gm = plugin.getGameManager();
        if (gm.isRunning() && !gm.isPvpEnabled()) { e.setCancelled(true); return; }
        if (!plugin.getConfig().getBoolean("friendly-fire", false)
            && plugin.getTeamManager().areAllied(att, victim)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onFall(EntityDamageEvent e) {
        if (e.getCause() == EntityDamageEvent.DamageCause.FALL
            && plugin.getScenarioManager().isEnabled("nofall")) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        // Golden head drop
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        sm.setOwningPlayer(p);
        sm.setDisplayName(ChatColor.GOLD + p.getName() + "'s Head");
        head.setItemMeta(sm);
        e.getDrops().add(head);

        if (p.getKiller() != null) {
            plugin.getStatsManager().addKill(p.getKiller().getUniqueId());
        }
        if (plugin.getGameManager().isRunning()) {
            Bukkit.getScheduler().runTask(plugin, () -> plugin.getGameManager().eliminate(p));
        }

        // TimeBomb: keep items 45s
        if (plugin.getScenarioManager().isEnabled("timebomb")) {
            // items naturally despawn after 5 min; we don't extend (sufficient)
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent e) {
        if (plugin.getGameManager().getSpectators().contains(e.getPlayer().getUniqueId())) {
            Bukkit.getScheduler().runTask(plugin, () -> e.getPlayer().setGameMode(GameMode.SPECTATOR));
        }
    }

    @EventHandler
    public void onConsume(PlayerItemConsumeEvent e) {
        if (isGoldenHead(e.getItem())) {
            int hearts = plugin.getConfig().getInt("golden-head-heal", 4);
            Player p = e.getPlayer();
            double newHp = Math.min(p.getMaxHealth(), p.getHealth() + hearts * 2.0);
            Bukkit.getScheduler().runTask(plugin, () -> p.setHealth(newHp));
            p.addPotionEffect(new PotionEffect(PotionEffectType.ABSORPTION, 60 * 20, 0));
            p.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, 5 * 20, 1));
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (e.isCancelled()) return;
        Block b = e.getBlock();
        Material m = b.getType();
        ScenarioManager sm = plugin.getScenarioManager();

        if (sm.isEnabled("cutclean")) {
            Map<Material, ItemStack> map = cutCleanDrops();
            if (map.containsKey(m)) {
                e.setDropItems(false);
                b.getWorld().dropItemNaturally(b.getLocation(), map.get(m));
                if (m == Material.IRON_ORE || m == Material.GOLD_ORE
                    || m == Material.DEEPSLATE_IRON_ORE || m == Material.DEEPSLATE_GOLD_ORE) {
                    e.getPlayer().giveExp(2);
                }
            }
        }
        if (sm.isEnabled("blooddiamond")
            && (m == Material.DIAMOND_ORE || m == Material.DEEPSLATE_DIAMOND_ORE)) {
            Player p = e.getPlayer();
            double nh = Math.max(0.5, p.getHealth() - 2.0);
            p.setHealth(nh);
        }
    }

    private Map<Material, ItemStack> cutCleanDrops() {
        Map<Material, ItemStack> m = new HashMap<>();
        m.put(Material.IRON_ORE, new ItemStack(Material.IRON_INGOT));
        m.put(Material.DEEPSLATE_IRON_ORE, new ItemStack(Material.IRON_INGOT));
        m.put(Material.GOLD_ORE, new ItemStack(Material.GOLD_INGOT));
        m.put(Material.DEEPSLATE_GOLD_ORE, new ItemStack(Material.GOLD_INGOT));
        m.put(Material.SAND, new ItemStack(Material.GLASS));
        m.put(Material.RED_SAND, new ItemStack(Material.GLASS));
        m.put(Material.GRAVEL, new ItemStack(Material.FLINT));
        return m;
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        String msg = e.getMessage();
        if (msg.startsWith("!") && plugin.getTeamManager().getTeamOf(e.getPlayer().getUniqueId()) != null) {
            e.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () ->
                plugin.getTeamManager().sendTeamChat(e.getPlayer(), msg.substring(1).trim()));
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent e) {
        // SpeedUHC: nothing here; movement speed handled on join/start
    }

    @EventHandler
    public void onJoinSpeed(PlayerJoinEvent e) {
        if (plugin.getScenarioManager().isEnabled("speeduhc")) {
            e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 0, false, false));
            e.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, Integer.MAX_VALUE, 0, false, false));
        }
    }
}
