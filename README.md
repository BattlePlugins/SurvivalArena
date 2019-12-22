# SurvivalArena
A Hunger Games plugin built with BattleArena.

SurvivalArena is a Hunger Games plugin built on the BattleArena API, therefore, [BattleArena](https://github.com/BattlePlugins/BattleArena) is required to be installed in order to use this plugin.

This is by far, the easiest, Hunger/Survival Games plugin to setup, with no config file editing at all. Everything can be done in game.

(This is a continuation of [SurvivalArena](https://dev.bukkit.org/projects/survivalarena) by Stryker76.)

Reporting Issues:
---------
If you need an answer in a timely manner, please contact us on [Discord](https://discord.gg/tMVPVJf): 

We really appreciate when you report bugs, and we would like you to report them to us whenever you find them.
However please be mindful of the information that you give us and/or how you contact us. If you really have no 
idea where to start, maybe a 1 on 1 conversation on Discord may be the way to go. If you know exactly how to replicate your problem, or 
you have a good amount of information about it, creating a new issue might be a better solution:


* https://github.com/BattlePlugins/SurvivalArena/issues

Setting Up:
------------

### Creating the Arena
- Stand on the first Spawn Point you want and type /sa create <arena name>
- Stand on each of your remaining Spawn Points and type /sa alter <arena name> # (Starting with 2)
- Select a WorldGaurd region for your arena and type /sa alter <arena name> addregion

### Creating an Item Set
- Place items into a chest that you want to be in the item set
- Type /sa additemset <arena name> <item_set name>
- Left click the chest to create and save the item set (You can create as many item sets as you wish)
- Note that items default to a 100% chance of spawning

### Setting items to spawn randomly
- First type /sa showitemset <arena name> <item_set name> this will show you a list of items prefixed with an index number
- Type /sa setchance <arena name> <item_set name> <index number> <chance> where <chance> is a percentage, such as 10, 50, 77, etc

### Adding Chests to the Arena

#### Manually

- Place chests where ever you want in your Arena
- Type /sa addchests <arena name> <item_set name>
- Left click each chest that you want to be populated with <item_set name>
- To change to another item_set, type /sa addchests <arena name> <item_set name>
- When you are done adding chests, type /sa addchests <arena name> done

#### Automatically

- Create a WorldEdit region covering the area of chests you want to auto assign an item_set to
- Type /sa autochests <arena name> <item_set> <clear>
  - Where <clear> is either 0, or 1, depending on if you want to clear the contents of the chests being added to the <item_set>. This is useful if you already have an existing Survival/Hunger Games plugin that does not clear the contents of the chests at the end of the game.

### Joining an Arena
- `/sa join`

Links
------------
SurvivalArena Download: 
* https://ci.battleplugins.org/job/BattlePlugins/job/SurvivalArena/ 

Live Chat on Discord:
* [BattlePlugins Dev](https://discord.gg/tMVPVJf): Join our Discord server to get support, talk about dev stuff, or just say hi!

Battle Arena Links:
* [Bukkit Page](http://dev.bukkit.org/projects/battlearena2/) (BattleArena on Bukkit!)
* [Spigot Page](http://spigotmc.org/resources/battle-arena.2164/) (BattleArena on Spigot!)

Battle Tracker Links:
* [Bukkit Page](http://dev.bukkit.org/projects/battletracker2/) (BattleTracker on Bukkit!)
* [Spigot Page](http://spigotmc.org/resources/battletracker.2165/) (BattleTracker on Spigot!)

Other Links:
* [BattleDocs](http://docs.battleplugins.org) (Support Wiki)
* [Battle Plugin Donations](https://patreon.com/battleplugins) (Donation link)

