---
section: arch_system
order: 311
icon: jsc:cd_rom
---
# Stepping into the new system (arch-chroot)

Until now you have typed on the live medium, and your commands reached the disk from the outside. To finish the new system, you step inside it. From then on, the folder /mnt is the top of the system, and every path you type means a path inside the new system.

The Arch tool for this is arch-chroot. Running a program with a different folder as the top of the disk is called a chroot, and arch-chroot makes one for the new system.

> [!DEFINE] chroot
> Running a program with a different folder as the top of the disk, so that the program sees that folder as its whole system.

Outside the new system, the prompt reads root@archiso ~ #. Inside, it reads [root@archiso /]#, with the folder you are in between the brackets.

1. Type arch-chroot /mnt and press [[Enter]].
2. Check the prompt. It must read [root@archiso /]#.
3. Do the steps of this chapter inside. Type exit when you are done.

If you name a folder other than /mnt, the tool refuses, and you stay outside:

```session
{"lines": [{"prompt": "root@archiso ~ # ", "text": "arch-chroot /mnt/x", "typed": true}, {"text": "chroot: cannot change root directory to '/mnt/x': No such file or directory"}], "look": "jsc:guide/terminal/zsh", "title": "Arch Linux"}
```

Leaving is done with exit. Nothing is lost when you leave: the work you did stays, and arch-chroot /mnt takes you back in.

Table: Commands that work only inside the new system

| Command | On the live medium |
| --- | --- |
| `pacman` | Refused: you are not root in the new system yet. |
| `mkinitcpio` | Not found: step in first. |
| `grub-install` | Not found: step in first. |
| `grub-mkconfig` | Not found: step in first. |
| `hostname` | Refused: you cannot change the name of the live medium. |

## If something goes wrong

> [!PROBLEM] The screen says "chroot: cannot change root directory to '/mnt/x': No such file or directory".
> The folder is not /mnt. Type arch-chroot /mnt.

> [!PROBLEM] The screen says "chroot: failed to run command '/bin/bash': No such file or directory".
> The base system is not on the disk yet, so there is no shell to start. Install it first ([](jsc:arch_base_system)).

> [!PROBLEM] The screen says "error: you cannot perform this operation unless you are root in the new" and then "system (arch-chroot /mnt first)".
> You typed pacman on the live medium. Step in first with arch-chroot /mnt.

> [!PROBLEM] The screen says "mkinitcpio: command not found (it is the new system's: arch-chroot /mnt first)".
> You are on the live medium. Step in first with arch-chroot /mnt.

> [!SEE] jsc:arch_clock_and_name
