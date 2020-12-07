package mcclassroom.javaplugin;

import org.bukkit.plugin.java.JavaPlugin;

public class Plugin extends JavaPlugin {
  @Override
  public void onEnable() {
    getLogger().info("Enabled");
  }
}

