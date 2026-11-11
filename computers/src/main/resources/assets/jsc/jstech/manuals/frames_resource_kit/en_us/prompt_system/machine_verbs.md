---
section: prompt_system
order: 241
---
# The machine: status, config, devices, reboot, date and mem

These verbs ask the machine about itself. status shows its power, its rate and its link. config shows and changes its settings. devices lists the linked devices, reboot restarts the computer, date tells the day and the time, and mem shows the memory.

## status

status shows whether the computer is online, its processing rate, its memory buffer and its network link. The first line reads ONLINE in green, or OFFLINE in red. Then come three rows.

> [!DEFINE] Tick
> The game's beat. A second is 20 of them.

Table: The rows of status

| Row | What it shows |
| --- | --- |
| `cpu` | The processing rate, in it/t (items a tick). |
| `ram` | The memory buffer, in it, the unit the mod uses for items. |
| `network` | linked, with the network's id in brackets, or -- when there is no link. |

```command
id: jsc:command/mc_dos/status
```

## config

config with no words lists this computer's settings under the heading settings, and tells you how to change one. With a key and a value after it, config key value changes one setting.

Table: Settings with their own rules

| Key | What it does |
| --- | --- |
| `name` | The computer's name, which whoami shows. |
| `theme` | Applies a preset of accent colour and wallpaper. |
| `netshare` | How much of the disk the network may read, in permille (parts per thousand), from 0 to 1000. |
| `share FOLDER [read or write]` | Opens a folder to the network. A shared folder shows as \\\\<host>\\<name>. |
| `unshare NAME` | Closes a shared folder. |

Other keys go to the machine's settings store. A machine without one says that it has no settings store. For example, config netshare 500 sets how much of the disk the network may read to half of it.

```session
{"lines": [{"prompt": "C:\\> ", "text": "config netshare 500", "typed": true}, {"text": "netshare set"}], "look": "jsc:guide/terminal/dos", "title": "MC-DOS"}
```

```command
id: jsc:command/mc_dos/config
```

## devices and reboot

> [!DEFINE] Peripheral
> A device that is linked to a port of a computer, such as a monitor, a drive or a printer.

devices lists the peripherals linked to this computer. Only a computer with ports for peripherals has this verb.

```command
id: jsc:command/mc_dos/devices
```

reboot restarts the computer at once. reboot --firmware restarts it into the firmware setup instead, the program that runs before the system ([](jsc:firmware)). The prompt says "The system is going down for reboot NOW!" and then restarts.

> [!WARNING] reboot starts at once, with no question first. Save your work before you type it.

```command
id: jsc:command/mc_dos/reboot
```

## date and mem

date prints the day and the time by the world's clock. The first day is day 1. A day starts at 06:00 by that clock, so the hours of a day run from six in the morning.

> [!DEFINE] World clock
> The one clock that the whole world uses. Jobs, and the times of files, are read from it.

```command
id: jsc:command/mc_dos/date
```

mem shows how much memory the computer has, how much is held right now, and how much is promised to what runs. Free is what is left once the promised memory is taken away, so it tells you whether one more program fits.

> [!DEFINE] Committed memory
> Memory promised to running programs. It can be more than the memory in use at this moment.

Table: The rows of mem

| Row | What it shows |
| --- | --- |
| Total memory | All the memory the computer has, in MB. |
| In use | The memory held at this moment. |
| Committed | The memory promised to running programs, in MB. |
| Free | Total memory minus committed memory, never below zero, in MB. |

```command
id: jsc:command/mc_dos/mem
```

## If something goes wrong

> [!PROBLEM] config says unknown setting: NAME
> The machine has no setting with that key. Type config alone to list the settings.

> [!PROBLEM] config says netshare needs a number from 0 to 1000
> Give a whole number from 0 to 1000.

> [!PROBLEM] config share refuses: this computer already has all the folders it can share.
> Unshare one folder with config unshare NAME, then share the new one.

> [!PROBLEM] config share says another folder is already shared as NAME
> Choose another name for the share, or unshare the old folder first.

> [!PROBLEM] config unshare says nothing is shared as NAME
> Check the name. config unshare takes the name the share was given.

> [!PROBLEM] devices is not in help.
> This computer has no ports for peripherals, so it has no devices verb. Use a computer with a port, or read [](jsc:peripheral_cables).

> [!SEE] jsc:firmware, jsc:peripheral_cables
