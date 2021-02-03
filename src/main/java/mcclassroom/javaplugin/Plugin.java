package mcclassroom.javaplugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.java.JavaPlugin;

public class Plugin extends JavaPlugin implements Listener {
  public static final String GROUP_WORLD_PREFIX = "group";

  public SandboxWorlds sandboxWorlds;
  public PluginConfig config;
  public String discordGuild;
  public String discordHome;
  public DiscordBot discordBot;
  public HashMap<UUID, PermissionAttachment> permissionAttachments = new HashMap<>();
  public HashMap<UUID, User> users = new HashMap<>();
  public HashMap<Integer, ArrayList<User>> groups = new HashMap<>();
  public ArrayList<User> teachers = new ArrayList<>();
  public ArrayList<User> students = new ArrayList<>();

  @Override
  public void onEnable() {
    config = new PluginConfig(this);

    if (!this.getDataFolder().exists()) {
      config.saveDefaultConfig();
      config.reload();
    }

    getCommand("autogroups").setExecutor(this);
    getCommand("groupsof").setExecutor(this);
    getCommand("makegroup").setExecutor(this);
    getCommand("rotateworlds").setExecutor(this);
    getCommand("shufflegroups").setExecutor(this);
    getCommand("bring").setExecutor(this);
    getCommand("breakout").setExecutor(this);
    getCommand("movegroup").setExecutor(this);
    getCommand("visit").setExecutor(this);
    getCommand("link").setExecutor(this);

    sandboxWorlds = new SandboxWorlds(this);
    loadWorlds();

    String discordToken = config.getYamlConfiguration().getString("discord-bot-token");
    discordGuild = config.getYamlConfiguration().getString("discord-guild-id");
    discordHome = config.getYamlConfiguration().getString("default-vc");
    discordBot = new DiscordBot(this, discordToken, discordGuild, discordHome);
  }

  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
    if (!(sender instanceof Player)) {
      sender.sendMessage(getMessage("youre-not-a-player"));
      return true;
    }
    if (sender.isOp()) {
      Player player = (Player) sender;
      User user = users.get(player.getUniqueId());
      if (!user.isTeacher()) {
        makeTeacher(user);
      }
    }
    if (cmd.getName().equalsIgnoreCase("link")) {
      Player player = (Player) sender;
      String token = args[0];
      String discordId = discordBot.tokenToSnowflake(token);
      User user = users.get(player.getUniqueId());
      user.discordId = discordId;
      if (user.groupNumber != null) {
        discordBot.moveUser(user, user.groupNumber);
      }
      discordBot.jda.retrieveUserById(user.discordId).map(net.dv8tion.jda.api.entities.User::getName).queue(name -> {
        sender.sendMessage(getMessage("link-success").replaceAll("%discord_name%", name));
      });
    } else if (cmd.getName().equalsIgnoreCase("autogroups")) {
      groups = new HashMap<>();
      int totalGroups = Integer.parseInt(args[0]);
      Collections.shuffle(students);
      for (int i = 0; i < students.size(); i++) {
        int groupNumber = i / totalGroups + 1;
        User student = students.get(i);
        student.groupNumber = groupNumber;
        ArrayList<User> group = groups.get(groupNumber);
        if (group == null) {
          group = new ArrayList<>();
          groups.put(groupNumber, group);
        }
        group.add(student);
      }
      sender.sendMessage(getMessage("groups-success").replaceAll("%N%", String.valueOf(totalGroups)).replaceAll("%n%", String.valueOf(groups.get(1).size())));
    } else if (cmd.getName().equalsIgnoreCase("groupsof")) {
      groups = new HashMap<>();
      int groupSize = Integer.parseInt(args[0]);
      Collections.shuffle(students);
      for (int i = 0; i < students.size(); i++) {
        int groupNumber = i % groupSize + 1;
        ArrayList<User> group = groups.get(groupNumber);
        if (group == null) {
          group = new ArrayList<>();
          groups.put(groupNumber, group);
        }
        students.get(i).groupNumber = groupNumber;
        group.add(students.get(i));
      }
      sender.sendMessage(getMessage("groups-success").replaceAll("%N%", String.valueOf(groups.size())).replaceAll("%n%", String.valueOf(groupSize)));
    } else if (cmd.getName().equalsIgnoreCase("rotateworlds")) {
      int amount;
      if (args.length == 1) {
        amount = Integer.parseInt(args[0]);
      } else {
        amount = 1;
      }
      int totalGroups = groups.size();
      HashMap<Integer, ArrayList<User>> newGroups = new HashMap<>();
      for (int groupNumber : groups.keySet()) {
        int newGroupNumber = (groupNumber + amount - 1) % totalGroups + 1; // one-indexed group numbers
        newGroups.put(newGroupNumber, new ArrayList<>());
        for (User student : groups.get(groupNumber)) {
          student.groupNumber = newGroupNumber;
        }
      }
      groups = newGroups;
      for (User student : students) {
        discordBot.moveUser(student, student.groupNumber);
        sandboxWorlds.sendp(GROUP_WORLD_PREFIX + student.groupNumber, Bukkit.getPlayer(student.minecraftUniqueId));
      }
      sender.sendMessage(getMessage("rotateworlds-success"));
    } else if (cmd.getName().equalsIgnoreCase("shufflegroups")) {
      int totalGroups = groups.size();
      groups = new HashMap<>();
      Collections.shuffle(students);
      for (int i = 0; i < students.size(); i++) {
        int groupNumber = i / totalGroups + 1;
        User student = students.get(i);
        student.groupNumber = groupNumber;
        ArrayList<User> group = groups.get(groupNumber);
        if (group == null) {
          group = new ArrayList<>();
          groups.put(groupNumber, group);
        }
        group.add(student);
      }
      for (User student : students) {
        discordBot.moveUser(student, student.groupNumber);
        sandboxWorlds.sendp(GROUP_WORLD_PREFIX + student.groupNumber, Bukkit.getPlayer(student.minecraftUniqueId));
      }
      sender.sendMessage(getMessage("shufflegroups-success").replaceAll("%N%", String.valueOf(totalGroups)).replaceAll("%n%", String.valueOf(groups.get(1).size())));
    } else if (cmd.getName().equalsIgnoreCase("movegroup")) {
      String playerName = args[0];
      int newGroupNumber = Integer.parseInt(args[1]);
      Player player = Bukkit.getPlayer(playerName);
      User user = users.get(player.getUniqueId());
      int previousGroupNumber = user.groupNumber;
      user.groupNumber = newGroupNumber;
      groups.get(previousGroupNumber).remove(user);
      groups.get(newGroupNumber).add(user);
      discordBot.moveUser(user, user.groupNumber);
      sandboxWorlds.sendp(GROUP_WORLD_PREFIX + newGroupNumber, player);
      sender.sendMessage(getMessage("movegroup-success").replaceAll("%group%", String.valueOf(newGroupNumber)));
    } else if (cmd.getName().equalsIgnoreCase("makegroup")) {
      ArrayList<User> newGroup = new ArrayList<User>();
      for (String playerName : args) {
        User student = users.get(Bukkit.getPlayer(playerName).getUniqueId());
        if (student.groupNumber != null) {
          groups.get(student.groupNumber).remove(student);
        }
        newGroup.add(student);
      }
      int newGroupNumber = 1;
      while (groups.containsKey(newGroupNumber) && groups.get(newGroupNumber).size() > 0) {
        newGroupNumber += 1;
      }
      for (User student : newGroup) {
        student.groupNumber = newGroupNumber;
      }
      groups.put(newGroupNumber, newGroup);
      sender.sendMessage(getMessage("makegroup-success").replaceAll("%n%", String.valueOf(newGroup.size())));
    } else if (cmd.getName().equalsIgnoreCase("breakout")) {
      discordBot.makeGroups();
      for (int groupNumber : groups.keySet()) {
        sandboxWorlds.createWorld(GROUP_WORLD_PREFIX + groupNumber, 1);
        for (User user : groups.get(groupNumber)) {
          sandboxWorlds.sendp(GROUP_WORLD_PREFIX + groupNumber, Bukkit.getPlayer(user.minecraftUniqueId));
        }
      }
      sender.sendMessage(getMessage("breakout-success"));
    } else if (cmd.getName().equalsIgnoreCase("visit")) {
      Player player = (Player) sender;
      User user = users.get(player.getUniqueId());
      int groupNumber = Integer.parseInt(args[0]);
      if (groupNumber == 0) { // send to default world
        sandboxWorlds.sendp(sandboxWorlds.defaultWorld.getName(), player);
        return true;
      }
      discordBot.moveUser(user, groupNumber);
      String worldName = GROUP_WORLD_PREFIX + groupNumber;
      sandboxWorlds.sendp(worldName, player);
      sender.sendMessage(getMessage("visit-success").replaceAll("%group%", String.valueOf(groupNumber)));
    } else if (cmd.getName().equalsIgnoreCase("bring")) {
      Player player = (Player) sender;
      ArrayList<Player> players = new ArrayList<>();
      if (args.length > 0) {
        for (String playerName : args) {
          players.add(Bukkit.getPlayer(playerName));
        }
      } else {
        players = new ArrayList<>(Bukkit.getOnlinePlayers());
      }
      sandboxWorlds.bring(player, players);
      sender.sendMessage(getMessage("bring-success"));
    }
    return true;
  }

  public void makeTeacher(User user) {
    user.setTeacher(true);
    teachers.add(user);
    students.remove(user);
    if (user.groupNumber != null) { // teachers don't belong to student grouops
      groups.get(user.groupNumber).remove(user);
      user.groupNumber = null;
    }
  }

  public void makeStudent(User user) {
    user.setTeacher(false);
    teachers.remove(user);
    students.add(user);
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    UUID javaId = player.getUniqueId();
    if (!users.containsKey(javaId)) {
      User user = new User(this);
      user.minecraftUniqueId = javaId;
      user.setTeacher(player.isOp());
      users.put(javaId, user);
      if (user.isTeacher()) {
        makeTeacher(user);
      } else {
        makeStudent(user);
      }
    }
  }

  @EventHandler
  public void onPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    UUID javaId = player.getUniqueId();
    User user = users.remove(javaId);
    students.remove(user);
    teachers.remove(user);
    if (user.groupNumber != null) {
      groups.get(user.groupNumber).remove(user);
    }
  }

  public void setPermission(UUID minecraftUniqueId, String permissionName, boolean value) {
    permissionAttachments.get(minecraftUniqueId).setPermission(permissionName, value);
  }

  public void unsetPermission(UUID minecraftUniqueId, String permissionName) {
    permissionAttachments.get(minecraftUniqueId).unsetPermission(permissionName);
  }

  public void setDiscordHome(String newDiscordHome) {
    discordHome = newDiscordHome;
    config.set("default-vc", discordHome);
  }

  public void loadWorlds() {
    for(String worldName : config.getYamlConfiguration().getConfigurationSection("loadedworlds").getKeys(false)) {
      Bukkit.getServer().createWorld(new WorldCreator(worldName));
      World world = Bukkit.getWorld(worldName);
      SandboxWorlds.setBorder(world, config.getYamlConfiguration().getInt("loadedworlds."+worldName+".bordersize"));
    }
    Bukkit.getServer().createWorld(new WorldCreator(sandboxWorlds.defaultWorld.getName()));
  }

  public String getMessage(String messageName) {
    return config.getYamlConfiguration().getString("messages." + messageName);
  }

  public String getFullyQualified(String path) {
    return config.getYamlConfiguration().getString(path);
  }

  public String getPermission(String permissionName) {
    return config.getYamlConfiguration().getString("permissions." + permissionName);
  }

  public String getHelp(String messageName) {
    return config.getYamlConfiguration().getString("help." + messageName);
  }

  public void deleteDir(String dir) {
    try {
      Path path = Paths.get(dir);
      Files.walk(path)
        .sorted(Comparator.reverseOrder())
        .map(Path::toFile)
        .forEach(File::delete);
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }
}
