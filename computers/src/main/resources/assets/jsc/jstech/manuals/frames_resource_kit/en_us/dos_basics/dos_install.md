---
section: dos_basics
order: 204
---
# Installing MC-DOS

MC-DOS is the Vintage system that works at the prompt ([](jsc:the_systems)). You install it from a medium only: a floppy disk, put in a Floppy Drive. The installer copies the system to a disk you choose, and then the computer starts from that disk.

```table
{"caption": "What you need", "rows": [[{"words": "Install medium"}, {"words": "The MC-DOS floppy disk, from the creative tabs."}], [{"words": "Drive"}, {"words": "A Floppy Drive joined to the computer ([](jsc:drives))."}], [{"words": "Room on the disk"}, {"words": "4 megabytes (MB) free on the disk you install on."}], [{"words": "Random-access memory (RAM)"}, {"words": "1 megabyte (MB) while MC-DOS runs."}]]}
```

The firmware is the program on the motherboard that starts the computer ([](jsc:firmware)).

1. Put the MC-DOS floppy disk in the Floppy Drive.
2. Start the computer with the floppy in its drive, as the installing page explains ([](jsc:installing)).
3. Read the page "Welcome to Setup." and press [[Enter]].
4. Move the cursor to a disk with the arrow keys. Type a name in "Computer name:" if you want one, then press [[Enter]].
5. Wait while the bar fills. The page says "Setup is copying MC-DOS to the disk."
6. Read "MC-DOS Setup is complete." and press [[Enter]] to restart.

```table
{"caption": "The four pages of Setup", "heads": ["Page", "Heading on the screen", "Keys at the foot"], "rows": [[{"words": "Welcome"}, {"words": "Welcome to Setup."}, {"literal": "ENTER=Continue  F3=Exit"}], [{"words": "Settings"}, {"words": "Setup will use the following settings:"}, {"literal": "ENTER=Continue  F3=Exit"}], [{"words": "Copy"}, {"words": "Setup is copying MC-DOS to the disk."}, {"words": "none"}], [{"words": "Done"}, {"words": "MC-DOS Setup is complete."}, {"literal": "ENTER=Restart"}]]}
```

The settings page lists the disks of the computer. Continue stays off until you pick a disk that has at least 4 megabytes (MB) free. Setup copies the system onto the disk you picked. When the copy is done, the computer restarts from that disk.

As the computer starts, the screen prints a line for each drive, such as Drive C:, and then the version line, MC-DOS Version 1.0. You are then at the prompt ([](jsc:dos_prompt)).

> [!DEFINE] Firmware
> The program on the motherboard that starts the computer. It runs before any system does.

## If something goes wrong

> [!PROBLEM] Nothing starts from the floppy.
> The Floppy Drive is not joined to the computer, or the firmware does not find it. Join the drive, then check the firmware ([](jsc:firmware)).

> [!PROBLEM] The screen says "No room here. Erase this disk, or choose another."
> The disk you picked has less than 4 megabytes (MB) free. Pick another disk that has room ([](jsc:disks)).

> [!PROBLEM] Continue does nothing on the settings page.
> No disk is picked yet, or the picked disk has no room. Pick a disk with room first.

> [!PROBLEM] Pressing [[F3]] does nothing.
> The copy has begun, and [[F3]] works only before it. Wait for the last page.

> [!SEE] jsc:dos_system_files, jsc:frames_which_machine
