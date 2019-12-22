package org.battleplugins.survivalarena;

import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.MatchState;
import mc.alk.arena.objects.arenas.Arena;
import mc.alk.arena.objects.events.ArenaEventHandler;
import mc.alk.arena.objects.events.EventPriority;
import mc.alk.arena.util.Log;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class SurvivalArena extends Arena {

    private SurvivalArenaPlugin plugin;

    private int inGracePeriod = 0;
    private int suddenDeathPlayers = 0;

    private Map<String, Integer> runningTasks;

    public SurvivalArena(SurvivalArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPrestart() {
        String arenaName = getMatch().getArena().getName();
        String path = arenaName + ".CHESTS";
        ConfigurationSection chestLocations = this.plugin.getConfig().getConfigurationSection(path);

        if (chestLocations == null) {
            Log.warn(arenaName + " has no chest locations defined");
            return;
        }

        Set<String> chestIds = chestLocations.getKeys(false);
        for (String chestId : chestIds) {
            String chestInfo = chestLocations.getString(chestId);

            Scanner parseChest = (new Scanner(chestInfo)).useDelimiter(",");
            String world = parseChest.next();
            int x = parseChest.nextInt();
            int y = parseChest.nextInt();
            int z = parseChest.nextInt();
            String itemSet = parseChest.next();

            path = this.getMatch().getArena().getName() + ".ITEM_SETS." + itemSet;

            ConfigurationSection itemSets = this.plugin.getConfig().getConfigurationSection(path);
            Set<String> itemNames = itemSets.getKeys(false);
            for (String item : itemNames) {
                Block chestLocation = Bukkit.getWorld(world).getBlockAt(x, y, z);
                Chest chest = (Chest) chestLocation.getState();

                if (chestLocation.getState() instanceof Chest) {
                    int chance = this.plugin.getConfig().getInt(String.valueOf(path) + "." + item + ".chance");
                    int result = ThreadLocalRandom.current().nextInt(100);

                    if (result <= chance) {
                        ItemStack itemstack = new ItemStack(Material.matchMaterial(item), 1);
                        path = path + "." + item + ".enchantments";

                        ConfigurationSection enchantments = this.plugin.getConfig().getConfigurationSection(path);
                        if (enchantments != null) {
                            Set<String> enchants = enchantments.getKeys(false);
                            for (String enchant : enchants) {
                                int level = this.plugin.getConfig().getInt(String.valueOf(path) + "." + enchant);
                                itemstack.addEnchantment(Enchantment.getByName(enchant), level);
                            }
                        }

                        chest.getInventory().addItem(itemstack);
                    }
                    continue;
                }
                Log.err("Can't populate, not a valid chest at " + x + " " + y + " " + z);
            }
        }
    }

    @Override
    public void onStart() {
        String arenaName = getMatch().getArena().getName();
        int gracePeriod = this.plugin.getConfig().getInt(arenaName + ".gracePeriod");
        int suddenDeathTime = this.plugin.getConfig().getInt(arenaName + ".suddenDeathTime");
        this.suddenDeathPlayers = this.plugin.getConfig().getInt(arenaName + ".suddenDeathPlayers");

        if (this.runningTasks == null) {
            this.runningTasks = new ConcurrentHashMap<>();
        }

        if (suddenDeathTime > 0 || this.suddenDeathPlayers > 0) {
            int taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, SurvivalArena.this::suddenDeath,
                    600L,
                    600L);

            this.runningTasks.put("suddenDeath", taskId);
        }

        if (gracePeriod > 0) {
            this.inGracePeriod = 1;

            this.getMatch().sendMessage("&eGrace period starting now for " + gracePeriod + " seconds");

            int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, SurvivalArena.this::endGracePeriod,  gracePeriod * 20L);
            this.runningTasks.put("gracePeriod", taskId);
        }
    }

    @Override
    public void onFinish() {
        String arenaName = getMatch().getArena().getName();
        String path = arenaName + ".CHESTS";
        ConfigurationSection chestLocations = this.plugin.getConfig().getConfigurationSection(path);

        if (chestLocations == null) {
            Log.warn(arenaName + " has no chest locations defined");
            return;
        }

        Set<String> chestIds = chestLocations.getKeys(false);
        for (String chestId : chestIds) {
            String chestInfo = chestLocations.getString(chestId);

            Scanner parseChest = (new Scanner(chestInfo)).useDelimiter(",");
            String world = parseChest.next();
            int x = parseChest.nextInt();
            int y = parseChest.nextInt();
            int z = parseChest.nextInt();

            Block chestLocation = Bukkit.getWorld(world).getBlockAt(x, y, z);
            if (chestLocation.getState() instanceof Chest) {
                Chest chest = (Chest) chestLocation.getState();
                chest.getInventory().clear();
                continue;
            }

            Log.err("Can't clear items, not a valid chest at " + x + " " + y + " " + z);
        }


        if (this.runningTasks != null) {
            this.runningTasks.values().forEach(Bukkit.getScheduler()::cancelTask);
            this.runningTasks = null;
        }
    }

    private void suddenDeath() {
        Collection<ArenaPlayer> players = getMatch().getAlivePlayers();
        if (this.suddenDeathPlayers == 0) {
            return;
        }

        if (players.size() <= this.suddenDeathPlayers) {
            for (ArenaPlayer ap : players) {
                Player player = Bukkit.getPlayerExact(ap.getName());
                player.getWorld().strikeLightningEffect(player.getLocation());
            }
        }
    }

    private void endGracePeriod() {
        this.inGracePeriod = 0;
        getMatch().sendMessage("&eGrace period now over!!");
    }

    @ArenaEventHandler(priority = EventPriority.LOW)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (this.getState() == MatchState.ONPRESTART) {
            event.getPlayer().teleport(event.getPlayer().getLocation());
        }
    }

    @ArenaEventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (this.inGracePeriod == 1) {
            event.setDamage(0);
            event.setCancelled(true);
        }
    }
}