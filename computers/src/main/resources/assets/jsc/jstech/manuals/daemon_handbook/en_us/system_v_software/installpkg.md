---
section: system_v_software
order: 351
---
# Installing a package from the medium

Programs on UNIX System V come on their own medium, and you install them with installpkg. There is no package manager and no Mirror. The program is on the medium, and the medium must be in a drive linked to the computer ([](jsc:drives)).

With nothing said, installpkg takes the program on the medium in the drive, which is how the real system worked: the medium is the question and the answer. With more than one medium in, it lists the programs and asks you to say which one, by name.

1. Put the program's medium in a drive linked to the computer.
2. Type installpkg and press Enter.
3. If the system lists several packages, type installpkg and the name of the one you want.
4. Wait while the program is set up, then type its command name to run it.

Table: What the screen says

| What you see | What it means |
| --- | --- |
| `installpkg: no package medium in a drive` | No medium is in a linked drive. Put the program's medium in a drive. |
| `installpkg: <name>: no such package on the media in the drives` | The name is on none of the media in the drives. |
| `The following packages are on the media in the drives:` | More than one medium is in. Type installpkg and the name you want. |
| `Installing the <name> package.` | The install has started. The setup line names the program and the medium. |
| `This computer is still setting up <name>.` | An earlier setup is still running. Wait for it to finish. |
| `<name> is already installed.` | The program is on this computer already. |
| `<name> needs <era> hardware or newer. This computer is <era>.` | The computer is older than the program. Use a newer computer. |
| `<name> runs on <list>, not on <system>.` | The program is written for another system. |
| `<name> needs <n> MB free on the disk. There are <m> MB.` | The disk has too little free space for the program. |
| `<name> cannot install on this computer's system or hardware.` | The program does not fit this system or this hardware. |

The setup line names the medium: the floppy, the compact disc (CD), the digital versatile disc (DVD), the USB drive (Universal Serial Bus) or the Blu-ray disc.

A program written for a newer era than your computer is refused, with the line about its hardware. A program written for the Vintage era, on a floppy, has no era rule.

Installed programs go to /usr/bin. While a program's medium is in a drive, its files appear under /mnt with the letter of the drive.

```command
id: jsc:command/unix/installpkg
```

## If something goes wrong

> [!PROBLEM] The screen says "installpkg: no package medium in a drive".
> Insert the medium the package came on, then try again.

> [!PROBLEM] The screen says "installpkg: <name>: no such package on the media in the drives".
> Type installpkg alone to see the list of packages, then type the name exactly as the list shows it.

> [!PROBLEM] The screen says "This computer is still setting up <name>."
> Wait until the first setup finishes, then type the command again.

> [!PROBLEM] The screen says "<name> needs <era> hardware or newer."
> Use a computer of that era or a newer one. The line names the era the program needs.

> [!SEE] jsc:programs, jsc:drives, jsc:installing
