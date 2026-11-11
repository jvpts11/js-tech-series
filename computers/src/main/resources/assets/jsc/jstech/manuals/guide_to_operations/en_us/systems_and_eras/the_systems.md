---
section: systems_and_eras
order: 11
icon: jsc:floppy_disk
---
# Operating systems

The operating system is what a computer starts into, and what every program runs on. You choose a system when you install it ([](jsc:installing)).

Each system needs hardware of its era or newer, takes room on the disk it is installed on, and takes memory while it runs.

```table
{"caption": "The systems", "heads": ["System", "Earliest era", "Room on the disk", "Memory while it runs"], "rows": [[{"words": "MC-DOS"}, {"words": "Vintage"}, {"amount": 4, "unit": "MB"}, {"amount": 1, "unit": "MB"}], [{"words": "MC-NET"}, {"words": "Vintage"}, {"amount": 8, "unit": "MB"}, {"amount": 2, "unit": "MB"}], [{"words": "UNIX System V"}, {"words": "Vintage"}, {"amount": 10, "unit": "MB"}, {"amount": 2, "unit": "MB"}], [{"words": "Frames 95"}, {"words": "Legacy"}, {"amount": 48, "unit": "MB"}, {"amount": 32, "unit": "MB"}], [{"words": "Frames XP"}, {"words": "Legacy"}, {"amount": 1536, "unit": "MB"}, {"amount": 128, "unit": "MB"}], [{"words": "Frames 7"}, {"words": "Transition"}, {"amount": 16384, "unit": "MB"}, {"amount": 1024, "unit": "MB"}], [{"words": "Frames 10"}, {"words": "Standard"}, {"amount": 16384, "unit": "MB"}, {"amount": 3072, "unit": "MB"}], [{"words": "Frames 11"}, {"words": "Advanced"}, {"amount": 20480, "unit": "MB"}, {"amount": 8192, "unit": "MB"}], [{"words": "Ubuntu"}, {"words": "Legacy"}, {"amount": 8192, "unit": "MB"}, {"amount": 48, "unit": "MB"}], [{"words": "Debian"}, {"words": "Legacy"}, {"amount": 4096, "unit": "MB"}, {"amount": 24, "unit": "MB"}], [{"words": "Fedora"}, {"words": "Legacy"}, {"amount": 8192, "unit": "MB"}, {"amount": 48, "unit": "MB"}], [{"words": "Arch Linux"}, {"words": "Legacy"}, {"amount": 2048, "unit": "MB"}, {"amount": 12, "unit": "MB"}], [{"words": "Gentoo"}, {"words": "Legacy"}, {"amount": 4096, "unit": "MB"}, {"amount": 12, "unit": "MB"}], [{"words": "FreeBSD"}, {"words": "Legacy"}, {"amount": 2048, "unit": "MB"}, {"amount": 16, "unit": "MB"}], [{"words": "OpenBSD"}, {"words": "Legacy"}, {"amount": 1024, "unit": "MB"}, {"amount": 12, "unit": "MB"}], [{"words": "Linux Mint"}, {"words": "Standard"}, {"amount": 12288, "unit": "MB"}, {"amount": 64, "unit": "MB"}]]}
```

MC-DOS is a command prompt. UNIX System V is a shell, with no desktop of its own.

Each Frames edition runs on its own era and every newer one. Frames 10 needs Standard hardware, and Frames 11 needs Advanced.

The programs you open take memory on top of the system's. What a program takes grows with the era of the computer: 8 times as much on a Transition computer as on a Legacy one, 32 times on a Standard one and 128 times on an Advanced one ([](jsc:frames_memory)).

Arch Linux and Gentoo are installed by hand, step by step. The other systems have a guided installer ([](jsc:installing)).

Table: The help book of each system

| System | Help book |
| --- | --- |
| MC-DOS and the Frames editions | The Frames Resource Kit |
| Ubuntu, Debian, Fedora and Linux Mint | The Linux Desk Reference |
| Arch Linux and Gentoo | Linux by Hand |
| FreeBSD, OpenBSD and UNIX System V | The Daemon Handbook |
| MC-NET | This Guide to Operations |

Each book is in the library ([](jsc:library)). Help on a computer opens on the book of its system first ([](jsc:help_on_a_computer)).

## If something goes wrong

> [!PROBLEM] The installer refuses the system.
> The hardware is older than the system's first era, or the disk is too small for it ([](jsc:installing)).

> [!SEE] jsc:installing, jsc:eras
