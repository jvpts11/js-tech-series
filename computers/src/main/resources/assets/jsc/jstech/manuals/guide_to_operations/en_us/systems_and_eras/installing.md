---
section: systems_and_eras
order: 12
icon: jsc:usb_flash_drive
covers_family: jsc:installing
---
# Installing a system

A computer without a system is a box that shows its firmware. The system is what you actually use: its files and its programs. To install one, you start the computer from the system's install medium, and follow its installer.

Every era has its own install medium. It goes in a drive that reads it. The drive has to be joined to the computer, like any other device ([](jsc:drives)).

```table
{"caption": "The install media of each era", "rows": [[{"words": "Vintage"}, {"words": "Floppy Disk, in a Floppy Drive"}], [{"words": "Legacy"}, {"words": "CD-ROM, in a CD Drive"}], [{"words": "Transition"}, {"words": "DVD-ROM, in a DVD Drive"}], [{"words": "Standard and Advanced"}, {"words": "USB Flash Drive, in a Dock Station or a case's USB port"}]]}
```

Programs come on media of their own. The Standard uses DVD-ROM for programs, the Advanced uses Blu-ray for them, and both use the USB Flash Drive for background services.

1. Put the install medium in a drive that is joined to the computer.
2. Turn the computer on, and watch the monitor ([](jsc:turning_on)).
3. Choose the medium on the Boot page of the Setup ([](jsc:setup_utility)), or in the Boot Menu ([](jsc:boot_menu)), and start it.
4. Follow the installer's pages to the end.

Not every system holds your hand. Arch Linux and Gentoo boot like a live medium, and you put their system on the disk by hand. Gentoo also builds its programs from source.

A server setting can make both ask for every step. In the jscomputers-server config file, install_by_hand.arch_every_step and install_by_hand.gentoo_every_step are off by default. With the Arch setting on, Arch asks for the whole guide, plus the hardware clock and a name for the machine. With the Gentoo setting on, Gentoo asks for the whole handbook.

A system needs hardware of its era or newer, so Frames 10 will not go on a Legacy computer, and the installer says so ([](jsc:the_systems)).

## If something goes wrong

> [!PROBLEM] The disk would not take the system.
> The disk has no room for the system. Use a bigger disk, or a disk of a newer era, which holds more ([](jsc:items_as_data)).

> [!PROBLEM] The installation could not be written to the disk (no space or no disk).
> The disk is full, or it is not joined to the computer. Check the disk and its room.

> [!PROBLEM] The medium was taken out before the system was written.
> Keep the medium in its drive until the installer has finished.

> [!PROBLEM] There is no installer on that medium.
> The medium is not an install medium for a system. Use the install medium of the system you want.

> [!PROBLEM] No installable system in a linked drive.
> Put the install medium in a drive that is joined to the computer ([](jsc:drives)).

> [!PROBLEM] The installer asks for its install disc in a linked drive.
> Put that system's install medium in a drive joined to the computer ([](jsc:drives)).

> [!PROBLEM] The installer says that the system needs other hardware.
> Install it on a computer that has the hardware the installer names ([](jsc:eras)).

> [!SEE] jsc:the_systems, jsc:drives, jsc:firmware
