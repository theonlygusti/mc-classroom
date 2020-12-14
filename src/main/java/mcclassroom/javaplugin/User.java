package mcclassroom.javaplugin;

import java.util.UUID;

public class User {
  public Plugin plugin;
  public String discordId;
  public UUID minecraftUniqueId;
  public Integer groupNumber;
  public Boolean teacher;

  public User(Plugin plugin) {
    this.plugin = plugin;
  }

  public void setTeacher(boolean teacher) {
    this.teacher = teacher;
    if (minecraftUniqueId != null) {
      if (teacher) {
        plugin.setPermission(minecraftUniqueId, "mcclassroom.teacher", true);
      } else {
        plugin.unsetPermission(minecraftUniqueId, "mcclassroom.teacher");
      }
    }
  }

  public boolean isTeacher() {
    return teacher;
  }
}
