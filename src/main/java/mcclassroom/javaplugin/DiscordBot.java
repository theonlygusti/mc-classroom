package mcclassroom.javaplugin;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class DiscordBot extends ListenerAdapter {
  public static final String GROUP_CHANNEL_PREFIX = "Group ";

  public JDA jda;
  public String homeChannelId;
  public int party_count = 0;
  public HashSet<String> seenDiscordUsers;
  public Plugin plugin;
  public Guild guild;
  public VoiceChannel homeChannel;

  public DiscordBot(Plugin plugin, String token, String homeChannelId) {
    this.plugin = plugin;
    this.homeChannelId = homeChannelId;
    try {
      jda = JDABuilder.createDefault(token)
        .setChunkingFilter(ChunkingFilter.ALL)
        .enableIntents(GatewayIntent.GUILD_MEMBERS)
        .setMemberCachePolicy(MemberCachePolicy.ALL)
        .build();
      jda.addEventListener(this);
    } catch (LoginException e) {
      throw new RuntimeException(e);
    }
    guild = jda.getGuildById(plugin.getFullyQualified("discord-guild-id"));
    homeChannel = guild.getVoiceChannelById(homeChannelId);
    if (homeChannel == null) {
      homeChannel = guild.getVoiceChannels().get(0);
      homeChannelId = homeChannel.getId();
      plugin.setDiscordHome(homeChannelId);
    }
  }

  @Override
  public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
    if (event.getChannelJoined().getId().equals(homeChannelId)) {
      if (!seenDiscordUsers.contains(event.getEntity().getId())) {
        event.getEntity().getUser().openPrivateChannel()
          .flatMap(channel -> channel.sendMessage(plugin.getMessage("link-token").replaceAll("%token%", snowflakeToToken(event.getEntity().getId()))))
          .queue();
        seenDiscordUsers.add(event.getEntity().getId());
      }
    }
  }

  @Override
  public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
    String command = event.getMessage().getContentRaw().split(" ")[0];
    switch(command){
      case "?link":
      case "?token":
        event.getChannel().sendMessage(plugin.getMessage("link-token").replaceAll("%token%", snowflakeToToken(event.getAuthor().getId())));
        break;
      case "?help":
        event.getChannel().sendMessage(plugin.getMessage("discord-help")).queue();
        break;
      default:
    }
  }

  /**
   * Convert discord snowflake ID into human-readable 'token' for linking discord account to minecraft account.
   *
   * Planning to set up some kind of what3words assignment.
   */
  public String snowflakeToToken(String snowflake) {
    return snowflake;
  }

  public String tokenToSnowflake(String token) {
    return token;
  }

  public void makeGroups() {
    for (int groupNumber : plugin.groups.keySet()) {
      List<User> users = plugin.groups.get(groupNumber);
      makeGroup(groupNumber, users);
    }
  }

  public void deleteAllGroups() {
    for (Integer groupNumber : plugin.groups.keySet()) {
      endGroup(groupNumber);
    }
  }

  private void makeGroup(int groupNumber, List<User> users) {
    guild.createVoiceChannel(GROUP_CHANNEL_PREFIX + groupNumber).setParent(homeChannel.getParent()).queue(vc -> {
      for(User user : users) {
        moveUser(user, groupNumber);
      }
    });
  }

  public void moveUser(User user, int groupNumber) {
    VoiceChannel groupVoiceChannel = getGroupChannel(groupNumber);
    Member member = getMember(user);
    guild.moveVoiceMember(member, groupVoiceChannel).queue();
  }

  private VoiceChannel getGroupChannel(int groupNumber) {
    return guild.getVoiceChannelsByName(GROUP_CHANNEL_PREFIX + groupNumber, false).get(0);
  }

  private Member getMember(User user) {
    return guild.getMemberById(user.discordId);
  }

  private void endGroup(int groupNumber) {
    VoiceChannel groupChannel = getGroupChannel(groupNumber);
    for(Member member : groupChannel.getMembers()){
      guild.moveVoiceMember(member, homeChannel).queue();
    }
    groupChannel.delete().queueAfter(2, TimeUnit.SECONDS);
  }
}
