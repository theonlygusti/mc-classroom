# Minecraft Classroom

This is an open-source implementation of a virtual classroom using Minecraft and Discord.

The reason behind its creation, the initiative and mission of this project is to:

 - Provide remote learners in today's challenging online climate the familiarity of a classroom setting, to be amongst fellow pupils and feel the presence of a teacher in a more physical way than voice-only classrooms allow
 - Repair some of the alienation and isolation felt by students in the wake of learning-from-home

This project is welcoming any and all contributions to its development and capabilities.

# The wiki

This repository is hosted on GitHub, and uses GitHub's Wiki feature to allow public contribution and collaboration towards
lesson plans and other educational ideas that can be delivered with or enhanced by using the environment that this project provides.

# Wish list

- Use MS Teams instead of Discord
- Use ProtocolLib instead of many-worlds:
  - groups sent to their own chunks of the main world
  - teacher can see entire world at once
  - students can only see their own group, and is surrounded by a world border
    - or alternatively (option/toggleable?) students can also see the other groups, but they cannot physically go there (trapped in world border)
    - or alternatively, students can also move freely between groups' regions to join different voicechats (might lead to goofing around?)
  - when teacher enters their region, teacher is visible to them (or invisible, toggleable?)
  - teacher can use commands to show all groups' areas, to show only one area, to bring all students to different areas etc.
- Write a client mod and server plugin to integrate voice chat into Minecraft
  - write a protocol first, then implement it as a fabric/forge mod and on-mod-only client (optifine style)
  - the plugin that facilitates this voice chat should run on a standard spigot server, so that it's easy to mix and match other plugins too
  - unmodded clients should be able to connect

