---
section: starting_a_computer
order: 6
icon: jsc:floppy_disk
---
# The Boot Menu and the boot manager

The Boot Menu starts a disk or medium for one start only. Press [[F12]] while the computer starts ([](jsc:turning_on)). You choose with the keys below.

Table: The keys in the Boot Menu

| Key | What it does |
| --- | --- |
| `Up and Down` | Select an entry in the list. |
| `Enter` | Boot the selected entry. |

```text
{"text": "The screen is titled Boot Menu. On a blue BIOS machine it reads \"Boot Menu  (this boot only)\", and its list is headed \"Boot Menu:\"."}
```

The computer does not save the choice, so the next start follows the boot order ([](jsc:firmware)) again.

## Boot managers

The boot manager is a different menu from the Boot Menu. Some systems show their own start menu before they start: GRUB does this on Linux, and the Midsoft Boot Manager on Frames, and FreeBSD has its own loader. The boot manager lets you choose between the systems of a disk ([](jsc:two_systems)).

If you do nothing, the boot manager counts down, and then it starts the highlighted entry by itself, after 5 seconds. Press Enter to start the highlighted entry at once.

Its help reads: "Use the Up and Down keys to select which entry is highlighted. Press Enter to boot the selected entry."

On a server, the setting boot.show_boot_menu decides whether a machine stops at its boot manager every time it starts. It is on by default. Turn it off, and the chosen system boots at once. The setting is in the jscomputers-server config file.

> [!DEFINE] Boot manager
> The system's own start menu, shown before the system starts. It lets you choose one of the systems on a disk.

## If something goes wrong

> [!PROBLEM] F12 does nothing.
> The Boot Menu opens only when a disk or medium can start the computer ([](jsc:no_bootable_device)).

> [!PROBLEM] The boot manager starts the wrong system.
> Press Up or Down to highlight the system you want, then press Enter before the countdown ends.

> [!PROBLEM] The computer stops at the boot manager every time it starts.
> Turn off boot.show_boot_menu in the jscomputers-server config file.

> [!SEE] jsc:two_systems, jsc:help_on_a_computer, jsc:firmware
