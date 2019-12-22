package org.battleplugins.survivalarena;

import mc.alk.arena.alib.arenaregenutil.ArenaRegenController;
import mc.alk.arena.alib.arenaregenutil.region.ArenaSelection;
import mc.alk.arena.executors.CustomCommandExecutor;
import mc.alk.arena.executors.MCCommand;
import mc.alk.arena.objects.ArenaPlayer;
import mc.alk.arena.objects.arenas.Arena;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Scanner;
import java.util.Set;

public class SurvivalArenaExecutor extends CustomCommandExecutor {

    private SurvivalArenaPlugin plugin;

    public SurvivalArenaExecutor(SurvivalArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @MCCommand(cmds = {"additemset"}, admin = true)
    public boolean addItemSet(ArenaPlayer sender, Arena arena, String setname) {
        if (!(arena instanceof SurvivalArena)) {
            return sendMessage(sender, "&eArena " + arena.getName() + " is not a Survival Arena!");
        }

        if (setname.equalsIgnoreCase("done")) {
            return sendMessage(sender, "&cYou cannot use done as an item set name");
        }

        sender.getPlayer().setMetadata("addType", new FixedMetadataValue(plugin, 1));
        sender.getPlayer().setMetadata("itemSetArena", new FixedMetadataValue(plugin, arena.getName()));
        sender.getPlayer().setMetadata("itemSetName", new FixedMetadataValue(plugin, setname));

        plugin.getServer().getPluginManager().registerEvents(new PlayerListener(plugin), plugin);
        return sendMessage(sender, "&eFill a chest with the items for &b" + setname + " &eand left click the chest to save it");
    }

    @MCCommand(cmds = {"addchests"}, admin = true)
    public boolean addChests(ArenaPlayer sender, Arena arena, String setname) {
        int chestCount;
        if (!(arena instanceof SurvivalArena)) {
            return sendMessage(sender, "&eArena " + arena.getName() + " is not a Survival Arena!");
        }

        if (setname.equalsIgnoreCase("done")) {
            Player player = sender.getPlayer();
            if (player.getMetadata("addType").isEmpty() || player.getMetadata("addType").get(0).asInt() != 2) {
                return sendMessage(sender, "&cYou were not adding any chests");
            }

            PlayerInteractEvent.getHandlerList().unregister(plugin);
            player.setMetadata("addType", new FixedMetadataValue(plugin, 0));

            return sendMessage(sender, "&eYou are now done adding chests");
        }

        String itemsetPath = arena.getName() + ".ITEM_SETS." + setname;
        String itemset = plugin.getConfig().getString(itemsetPath);
        if (itemset == null) {
            return sendMessage(sender, "&cAdd an item set for " + setname + " first");
        }

        String path = arena.getName() + ".CHESTS";
        ConfigurationSection chests = plugin.getConfig().getConfigurationSection(path);
        if (chests == null) {
            chestCount = 0;
        } else {
            Set<String> chestIds = chests.getKeys(false);
            chestCount = chestIds.size();
        }

        sender.getPlayer().setMetadata("addType", new FixedMetadataValue(plugin, 2));
        sender.getPlayer().setMetadata("itemSetArena", new FixedMetadataValue(plugin, arena.getName()));
        sender.getPlayer().setMetadata("itemSetName", new FixedMetadataValue(plugin, setname));
        sender.getPlayer().setMetadata("chestCount", new FixedMetadataValue(plugin, chestCount));

        PlayerInteractEvent.getHandlerList().unregister(plugin);
        plugin.getServer().getPluginManager().registerEvents(new PlayerListener(plugin), plugin);

        return sendMessage(sender, "&eLeft click a chest to add &b" + setname + " &eto it");
    }

    @MCCommand(cmds = {"set"}, admin = true)
    public boolean setConfig(ArenaPlayer sender, Arena arena, String node, int value) {
        String path;
        if (!(arena instanceof SurvivalArena)) {
            return sendMessage(sender, "&eArena " + arena.getName() + " is not a Survival Arena!");
        }

        if (node.equalsIgnoreCase("graceperiod")) {
            path = arena.getName() + ".gracePeriod";
        }
        else if (node.equalsIgnoreCase("suddendeathtime")) {
            path = arena.getName() + ".suddenDeathTime";
        }
        else if (node.equalsIgnoreCase("suddendeathplayers")) {
            path = arena.getName() + ".suddenDeathPlayers";
        } else {
            return sendMessage(sender, "&cNot a valid config option [graceperiod, suddendeathtime, suddendeathplayers]");
        }

        plugin.getConfig().set(path, value);
        plugin.saveConfig();

        return sendMessage(sender, "&eConfig node " + node + " set to " + value);
    }


    @MCCommand(cmds = {"showitemset"}, admin = true)
    public boolean showItemSet(ArenaPlayer sender, Arena arena, String setname) {
        if (!(arena instanceof SurvivalArena)) {
            return sendMessage(sender, "&eArena " + arena.getName() + " is not a Survival Arena!");
        }

        String path = arena.getName() + ".ITEM_SETS." + setname;
        ConfigurationSection itemSets = plugin.getConfig().getConfigurationSection(path);

        if (itemSets == null) {
            return sendMessage(sender, "&cThere is no item_set: " + setname);
        }

        int i = 0;
        Set<String> itemNames = itemSets.getKeys(false);
        for (String item : itemNames) {
            int amount = plugin.getConfig().getInt(path + "." + item + ".amount");
            int chance = plugin.getConfig().getInt(path + "." + item + ".chance");
            sendMessage(sender, "&b" + i++ + ": &e" + item + " - &6 Amount: " + amount + " &a Chance: " + chance + "%");
        }
        return true;
    }


    @MCCommand(cmds = {"setchance"}, admin = true)
    public boolean setChance(ArenaPlayer sender, Arena arena, String setname, int index, int chance) {
        if (!(arena instanceof SurvivalArena)) {
            return sendMessage(sender, "&eArena " + arena.getName() + " is not a Survival Arena!");
        }

        String path = arena.getName() + ".ITEM_SETS." + setname;
        ConfigurationSection itemSets = plugin.getConfig().getConfigurationSection(path);

        if (itemSets == null) {
            return sendMessage(sender, "&cThere is no item_set: " + setname);
        }

        int i = 0;
        Set<String> itemNames = itemSets.getKeys(false);
        for (String item : itemNames) {
            if (i == index) {
                plugin.getConfig().set(path + "." + item + ".chance", chance);
                sendMessage(sender, "&eSet item " + item + " to &a Chance: " + chance + "%");
                plugin.saveConfig();
                break;
            }
            i++;
        }

        return true;
    }

    @MCCommand(cmds = {"autochests"}, admin = true)
    public boolean autoChests(ArenaPlayer sender, Arena arena, String item_set, int clear) {
        Player player = sender.getPlayer();

        if (!(arena instanceof SurvivalArena)) {
            return sendMessage(sender, "&eArena " + arena.getName() + " is not a Survival Arena!");
        }

        String itemPath = arena.getName() + ".ITEM_SETS." + item_set;
        ConfigurationSection itemSets = plugin.getConfig().getConfigurationSection(itemPath);

        if (itemSets == null) {
            return sendMessage(sender, "&cThere is no item_set: " + item_set);
        }

        ArenaSelection sel = ArenaRegenController.getSelection(player);
        if (sel == null) {
            sendMessage(sender, "&cYou need to select a region to use this command.");
            return false;
        }

        Location minLoc = sel.getMinimumPoint();
        Location maxLoc = sel.getMaximumPoint();

        Chunk minChunk = minLoc.getChunk();
        Chunk maxChunk = maxLoc.getChunk();

        World world = sel.getWorld();
        String path = arena.getName() + ".CHESTS.";
        int chestCount = getChestCount(path);

        for (int cx = minChunk.getX(); cx <= maxChunk.getX(); cx++) {
            for (int cz = minChunk.getZ(); cz <= maxChunk.getZ(); cz++) {
                Chunk chunk = world.getChunkAt(cx, cz);
                for (BlockState blockEntity : chunk.getTileEntities()) {
                    if (blockEntity instanceof Chest && containsLoc(sel, blockEntity.getBlock().getLocation())) {
                        Block chestLoc = world.getBlockAt(blockEntity.getBlock().getLocation());

                        ConfigurationSection chestLocations = plugin.getConfig().getConfigurationSection(arena.getName() + ".CHESTS");
                        boolean chestExists = false;
                        String origSetName = null;

                        if (chestLocations != null) {
                            Set<String> chestIds = chestLocations.getKeys(false);

                            for (String chestId : chestIds) {
                                String chestInfo = chestLocations.getString(chestId);

                                Scanner parseChest = (new Scanner(chestInfo)).useDelimiter(",");
                                String cworld = parseChest.next();
                                int ex = parseChest.nextInt();
                                int ey = parseChest.nextInt();
                                int ez = parseChest.nextInt();
                                String citemSet = parseChest.next();

                                if (chestLoc.getX() == ex && chestLoc.getY() == ey && chestLoc.getZ() == ez) {
                                    chestExists = true;
                                    origSetName = citemSet;
                                }
                            }

                            if (chestExists) {
                                sendMessage(sender, "&cSkipping chest at &b" + chestLoc.getX() + " " + chestLoc.getY() + " " + chestLoc.getZ() + ", &calready set to &a" + origSetName);
                            }
                            else {

                                String chestInfo = world.getName() + "," + chestLoc.getX() + "," + chestLoc.getY() + "," + chestLoc.getZ() + "," + item_set;
                                String chestsPath = path + chestCount++;

                                plugin.getConfig().set(chestsPath, chestInfo);
                                plugin.saveConfig();
                                if (clear == 1) {
                                    Chest chest = (Chest)chestLoc.getState();
                                    chest.getInventory().clear();
                                }

                                sendMessage(sender, "&eAdding chest at &b" + chestLoc.getX() + " " + chestLoc.getY() + " " + chestLoc.getZ() + " &ewith item_set &a" + item_set);
                            }

                        } else {
                            String chestInfo = world.getName() + "," + chestLoc.getX() + "," + chestLoc.getY() + "," + chestLoc.getZ() + "," + item_set;
                            String chestsPath = path + ++chestCount;

                            plugin.getConfig().set(chestsPath, chestInfo);
                            plugin.saveConfig();

                            sendMessage(sender, "&eAdding chest at &b" + chestLoc.getX() + " " + chestLoc.getY() + " " + chestLoc.getZ() + " &ewith item_set &a" + item_set);
                        }
                    }
                }
            }
        }

        return sendMessage(sender, "&eDone");
    }

    private int getChestCount(String path) {
        ConfigurationSection chests = plugin.getConfig().getConfigurationSection(path);
        if (chests == null) {
            return 0;
        } else {
            return chests.getKeys(false).size();
        }
    }

    private boolean containsLoc(ArenaSelection sel, Location loc) {
        return loc.getWorld().equals(sel.getWorld())
                && loc.getX() >= sel.getMinimumPoint().getX()
                && loc.getX() <= sel.getMaximumPoint().getX()
                && loc.getY() >= sel.getMinimumPoint().getY()
                && loc.getY() <= sel.getMaximumPoint().getY()
                && loc.getZ() >= sel.getMinimumPoint().getZ()
                && loc.getZ() <= sel.getMaximumPoint().getZ();
    }
}
