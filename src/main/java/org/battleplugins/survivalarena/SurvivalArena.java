package org.battleplugins.survivalarena;

import mc.alk.arena.alib.bukkitadapter.MaterialAdapter;
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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class SurvivalArena extends Arena {

    private SurvivalArenaPlugin plugin;

    private boolean inGracePeriod = false;
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
            Scanner parseChest = new Scanner(chestInfo).useDelimiter(",");
            String world = parseChest.next();
            int x = parseChest.nextInt();
            int y = parseChest.nextInt();
            int z = parseChest.nextInt();
            String itemSet = parseChest.next();

            path = this.getMatch().getArena().getName() + ".ITEM_SETS." + itemSet;

            ConfigurationSection itemSets = this.plugin.getConfig().getConfigurationSection(path);
            Set<String> itemNames = itemSets.getKeys(false);
            Block chestLocation = Bukkit.getWorld(world).getBlockAt(x, y, z);
            Chest chest = (Chest) chestLocation.getState();
            if (!(chestLocation.getState() instanceof Chest)) {
                Log.err("Can't populate, not a valid chest at " + x + " " + y + " " + z);
                continue;
            }
            boolean[] occupiedSlots = new boolean[chest.getInventory().getSize()];
            for (String item : itemNames) {
                int chance = this.plugin.getConfig().getInt(path + "." + item + ".chance");
                int result = ThreadLocalRandom.current().nextInt(0, 100);

                if (chance >= result) {
                    Material mat = MaterialAdapter.getMaterial(item);
                    if (mat == null) {
                        Log.warn("Material " + item + " does not exist, make sure you typed it in correctly!");
                        continue;
                    }

                    ItemStack itemstack = new ItemStack(mat, 1);
                    path = path + "." + item + ".enchantments";
                    ConfigurationSection enchantments = this.plugin.getConfig().getConfigurationSection(path);
                    if (enchantments != null) {
                        Set<String> enchants = enchantments.getKeys(false);
                        for (String enchant : enchants) {
                            int level = this.plugin.getConfig().getInt(path + "." + enchant);
                            itemstack.addEnchantment(Enchantment.getByName(enchant), level);
                        }
                    }

                    int slot;
                    do {
                        slot = ThreadLocalRandom.current().nextInt(chest.getInventory().getSize());
                    } while (occupiedSlots[slot]);

                    occupiedSlots[slot] = true;
                    chest.getInventory().setItem(slot, itemstack);
                }
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
            this.inGracePeriod = true;

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

            Scanner parseChest = new Scanner(chestInfo).useDelimiter(",");
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
        this.inGracePeriod = false;
        getMatch().sendMessage("&eGrace period now over!!");
    }

    @ArenaEventHandler(priority = EventPriority.LOW)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (Objects.equals(this.getState(), MatchState.ONPRESTART) || Objects.equals(this.getState(), MatchState.INPRESTART)) {
            if (event.getTo().getX() != event.getFrom().getX() ||
                    event.getTo().getY() != event.getFrom().getY() ||
                    event.getTo().getZ() != event.getFrom().getZ()) {

                event.setCancelled(true);
            }
        }
    }

    @ArenaEventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (event.isCancelled()) {
            return;
        }

        if (this.inGracePeriod) {
            event.setDamage(0);
            event.setCancelled(true);
        }
    }
}