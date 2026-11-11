---
section: shell_commands
order: 253
---
# Machine and system information

These commands tell you about the computer you use: its system, its name, its power and its settings. Some work on every machine, and the others only on the Unix systems, as each block says.

The config command shows and changes the settings of the computer. With no argument it lists them. With a key and a value, as in config name <name>, it changes one setting.

Table: Two settings of config

| Setting | What it takes |
| --- | --- |
| `name` | The computer's name, which the prompt shows after the at sign. |
| `netshare` | A number from 0 to 1000, in thousandths (permille). |

The config command has more settings, such as wallpaper and theme.

```command
id: jsc:command/linux/uname
```

```command
id: jsc:command/linux/hostname
```

```command
id: jsc:command/linux/date
```

```command
id: jsc:command/linux/status
```

```command
id: jsc:command/linux/config
```

```command
id: jsc:command/linux/reboot
```

The reboot command can open the firmware instead of the system: its --firmware option enters the setup of the [firmware](jsc:firmware).

```command
id: jsc:command/linux/devices
```

The fsck command repairs a corrupted disk, as the Defrag does on the other systems ([](jsc:corruption_and_the_defrag)).

```command
id: jsc:command/linux/fsck
```

```command
id: jsc:command/linux/which
```

```command
id: jsc:command/linux/whoami
```

```command
id: jsc:command/linux/echo
```

```command
id: jsc:command/linux/clear
```

```command
id: jsc:command/linux/set
```

```command
id: jsc:command/linux/export
```

```command
id: jsc:command/linux/unset
```

You will not find a version command on these systems. It belongs to MC-DOS and the Frames systems, where it shows the shell's version.

## If something goes wrong

> [!PROBLEM] The config command says unknown setting, followed by the name you typed.
> Run config with no argument to see the setting names.

> [!PROBLEM] The config command says netshare needs a number from 0 to 1000.
> Give a whole number from 0 to 1000.

> [!PROBLEM] whoami shows no login name.
> Type echo "$USER", which prints player.

> [!SEE] jsc:devices_commands, jsc:shell_prompt
