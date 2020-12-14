package mcclassroom.javaplugin;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;

public class PluginConfig {
  private File file;
  private Plugin plugin;
  private String name;
  private YamlConfiguration config;

  public PluginConfig(Plugin plugin) {
    this.plugin = plugin;
    this.name = "config.yml";
  }

  public PluginConfig save() {
    if ((this.config == null) || (this.file == null)) {
      return this;
    }
    try {
      config.save(this.file);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
    return this;
  }

  public YamlConfiguration getYamlConfiguration() {
    if (this.config == null) {
      reload();
    }
    return this.config;
  }

  public PluginConfig saveDefaultConfig() {
    file = new File(plugin.getDataFolder(), this.name);
    plugin.saveResource(this.name, false);
    return this;
  }

  public PluginConfig reload() {
    if (file == null) {
      this.file = new File(plugin.getDataFolder(), this.name);
    }
    this.config = YamlConfiguration.loadConfiguration(file);
    Reader defaultConfigStream;
    try {
      defaultConfigStream = new InputStreamReader(plugin.getResource(this.name), "UTF8");
      if (defaultConfigStream != null) {
        YamlConfiguration defaultConfig = YamlConfiguration.loadConfiguration(defaultConfigStream);
        this.config.setDefaults(defaultConfig);
      }
    } catch (UnsupportedEncodingException | NullPointerException e) {
      throw new RuntimeException(e);
    }
    return this;
  }

  public PluginConfig copyDefaults(boolean force) {
    getYamlConfiguration().options().copyDefaults(force);
    return this;
  }

  public PluginConfig set(String key, Object value) {
    getYamlConfiguration().set(key, value);
    return this;
  }

  public Object get(String key) {
    return getYamlConfiguration().get(key);
  }
}

