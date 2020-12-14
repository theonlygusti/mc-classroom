package mcclassroom.javaplugin;

import org.bukkit.*;
import org.bukkit.entity.Player;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class SandboxWorlds {
  public Plugin plugin;
  public World defaultWorld;

  public SandboxWorlds(Plugin plugin) {
    this.plugin = plugin;
    this.defaultWorld = Bukkit.getWorld(plugin.getFullyQualified("default-world"));
  }

	public static void setBorder(World world, int borderSizeInChunks) {
		int borderSize = borderSizeInChunks * 16;
		int borderCenter = borderSize / 2;
		WorldBorder border = world.getWorldBorder();
		border.setCenter(borderCenter, borderCenter);
		border.setSize(borderSize);
		Location spawnPoint = border.getCenter();
		world.setSpawnLocation(new Location(world, spawnPoint.getX(), 57, spawnPoint.getZ()));
	}

  public void createWorld(String name, int chunkSize) {
    WorldCreator worldCreator = new WorldCreator(name);
    worldCreator.environment(World.Environment.NORMAL);
    worldCreator.type(WorldType.FLAT);

    worldCreator.generateStructures(false);
    worldCreator.createWorld();

    World world = Bukkit.getWorld(name);
    setBorder(world, chunkSize);

    List<String> loadedWorlds = new ArrayList<>();
    HashMap<String, Integer> worldSizes = new HashMap<>();

    for (String worldName : plugin.config.getYamlConfiguration().getConfigurationSection("loadedworlds").getKeys(false)) {
      loadedWorlds.add(worldName);
      worldSizes.put(worldName, plugin.config.getYamlConfiguration().getInt("loadedworlds." + worldName + ".bordersize"));
    }

    loadedWorlds.add(name);
    worldSizes.put(name, chunkSize);

    plugin.config.getYamlConfiguration().set("loadedworlds", null);
    for(String s : loadedWorlds) {
      plugin.config.getYamlConfiguration().set("loadedworlds." + s + ".bordersize", worldSizes.get(s));
    }
    plugin.config.save();
  }

  public void setWorldSize(String name, int chunkSize) {
    World world = Bukkit.getWorld(name);
    setBorder(world, chunkSize);

    plugin.config.getYamlConfiguration().set("loadedworlds." + name + ".bordersize", chunkSize);
    plugin.config.save();
  }

  public void sendp(String name, List<Player> players) {
    for(Player p : players) {
      sendp(name, p);
    }
  }

  public void sendp(String worldName, Player player) {
    World world = Bukkit.getWorld(worldName);
    player.teleport(world.getSpawnLocation());
  }

  public void bring(Player sender, List<Player> players) {
    World world = sender.getWorld();

    for(Player p : players) {
      p.teleport(world.getSpawnLocation());
    }
  }

  public void deleteWorld(String name) {
    World world = Bukkit.getWorld(name);

    for(Player player : world.getPlayers()) {
      player.teleport(plugin.sandboxWorlds.defaultWorld.getSpawnLocation());
    }

    Bukkit.getServer().unloadWorld(world, false);
    plugin.deleteDir(name);

    List<String> loadedWorlds = new ArrayList<>();
    HashMap<String, Integer> worldSizes = new HashMap<>();

    for (String worldName : plugin.config.getYamlConfiguration().getConfigurationSection("loadedworlds").getKeys(false)) {
      loadedWorlds.add(worldName);
      worldSizes.put(worldName, plugin.config.getYamlConfiguration().getInt("loadedworlds." + worldName + ".bordersize"));
    }

    loadedWorlds.remove(name);
    worldSizes.remove(name);

    plugin.config.getYamlConfiguration().set("loadedworlds", null);
    for(String s : loadedWorlds) {
      plugin.config.getYamlConfiguration().set("loadedworlds." + s + ".bordersize", worldSizes.get(s));
    }
    plugin.config.save();
  }

  public void copyWorld(Player sender, String name) {
    World from = Bukkit.getWorld(name);
    World to = sender.getWorld();

    File fromFolder = from.getWorldFolder();
    File toFolder = to.getWorldFolder();

    for(Player player : to.getPlayers()) {
      player.teleport(plugin.sandboxWorlds.defaultWorld.getSpawnLocation());
    }

    Bukkit.unloadWorld(to, false);
    copyWorld(fromFolder, toFolder);
    Bukkit.getServer().createWorld(new WorldCreator(to.getName()));
  }

  public void copyWorld(File source, File target){
    try {
      ArrayList<String> ignore = new ArrayList<String>(Arrays.asList("uid.dat", "session.dat"));
      if(!ignore.contains(source.getName())) {
        if(source.isDirectory()) {
          if(!target.exists())
            target.mkdirs();
          String files[] = source.list();
          for (String file : files) {
            File srcFile = new File(source, file);
            File destFile = new File(target, file);
            copyWorld(srcFile, destFile);
          }
        } else {
          InputStream in = new FileInputStream(source);
          OutputStream out = new FileOutputStream(target);
          byte[] buffer = new byte[1024];
          int length;
          while ((length = in.read(buffer)) > 0)
            out.write(buffer, 0, length);
          in.close();
          out.close();
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}

