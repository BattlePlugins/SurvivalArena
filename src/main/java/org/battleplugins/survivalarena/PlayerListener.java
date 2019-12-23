package org.battleplugins.survivalarena;

import mc.alk.arena.util.Log;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class PlayerListener implements Listener {

    private SurvivalArenaPlugin plugin;

    public PlayerListener(SurvivalArenaPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        int addType = -1;

        if (player.getMetadata("addType").isEmpty()) {
            return;
        }

        addType = player.getMetadata("addType").get(0).asInt();

        if (addType == -1) {
            Log.info("BORK");
            return;
        }

        if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            Block block = event.getClickedBlock();
            if (block.getState() instanceof Chest) {
                Chest chest = (Chest) block.getState();

                if (addType == 1) {
                    String arena = player.getMetadata("itemSetArena").get(0).asString();
                    String setName = player.getMetadata("itemSetName").get(0).asString();
                    ItemStack[] itemsinchest = chest.getInventory().getContents();

                    String itemsetPath = arena + ".ITEM_SETS." + setName;
                    int numItems = 0;

                    for (ItemStack item : itemsinchest) {
                        if (item != null) {
                            this.plugin.getConfig().set(itemsetPath + "." + item.getType().name() + ".amount", item.getAmount());
                            Map<Enchantment, Integer> enchants = item.getEnchantments();
                            for (Enchantment e : enchants.keySet()) {
                                String enchantment = e.getName();
                                int level = enchants.get(e);

                                Log.info("YO: " + item.getType().name() + enchantment + " " + level);
                                this.plugin.getConfig().set(itemsetPath + "." + item.getType().name() + ".enchantments." + enchantment, level);
                            }

                            this.plugin.getConfig().set(itemsetPath + "." + item.getType().name() + ".chance", 100);
                            numItems++;
                        }
                    }
                    player.sendMessage(ChatColor.GREEN + "Saved item set " + ChatColor.AQUA + setName + ChatColor.GREEN + " with " + numItems + " items for arena " + ChatColor.YELLOW + arena);

                    this.plugin.saveConfig();

                    player.setMetadata("addType", new FixedMetadataValue(this.plugin, 0));

                    PlayerInteractEvent.getHandlerList().unregister(this.plugin);
                }
                else if (addType == 2) {
                    String arena = player.getMetadata("itemSetArena").get(0).asString();
                    String setName = player.getMetadata("itemSetName").get(0).asString();
                    int chestCount = player.getMetadata("chestCount").get(0).asInt();
                    boolean chestExists = false;
                    int indexToUse = -1;
                    String origSetName = null;

                    Location chestLocation = chest.getLocation();
                    int x = chestLocation.getBlockX();
                    int y = chestLocation.getBlockY();
                    int z = chestLocation.getBlockZ();

                    String world = chestLocation.getWorld().getName();

                    ConfigurationSection chestLocations = this.plugin.getConfig().getConfigurationSection(arena + ".CHESTS");
                    if (chestLocations != null) {
                        Set<String> chestIds = chestLocations.getKeys(false);
                        int i = 0;
                        for (String chestId : chestIds) {
                            String chestInfo = chestLocations.getString(chestId);

                            Scanner parseChest = (new Scanner(chestInfo)).useDelimiter(",");
                            String cworld = parseChest.next();
                            int cx = parseChest.nextInt();
                            int cy = parseChest.nextInt();
                            int cz = parseChest.nextInt();
                            String citemSet = parseChest.next();
                            if (x == cx && y == cy && z == cz) {
                                if (citemSet.compareTo(setName) == 0) {
                                    player.sendMessage(ChatColor.RED + "Chest location already set to " + ChatColor.GREEN + setName);

                                    return;
                                }
                                chestExists = true;
                                origSetName = citemSet;
                                indexToUse = i;
                                break;
                            }
                            i++;
                        }
                    }
                    String chestInfo = world + "," + x + "," + y + "," + z + "," + setName;
                    String chestsPath = arena + ".CHESTS.";
                    if (chestExists) {
                        chestsPath = chestsPath + indexToUse;

                        player.sendMessage(ChatColor.GREEN + "Chest item set changed from " + origSetName + " to " + setName);
                    } else {
                        chestsPath = chestsPath + chestCount;

                        player.setMetadata("chestCount", new FixedMetadataValue(this.plugin, ++chestCount));
                        player.sendMessage(ChatColor.GREEN + "Saved chest location for item_set " + setName);
                    }

                    this.plugin.getConfig().set(chestsPath, chestInfo);

                    this.plugin.saveConfig();
                }
            }
        }
    }
}
