---
section: freebsd_system
order: 332
---
# FreeBSD on this computer

FreeBSD is a UNIX-like system, and the Daemon Handbook is about it and about UNIX System V. On your computer it works the way the other systems do: you install it from a medium, you start it, and you work at its prompt. It starts at a text prompt, and a desktop is an extra package you may add later.

> [!DEFINE] Terminal system
> A system that starts at a text prompt. A desktop is an optional package.

FreeBSD needs Legacy hardware or newer, the first hardware era it runs on. A computer of the Vintage era cannot install it. The installed system takes 2,048 MB of disk space, and 16 MB of memory while it runs.

> [!DEFINE] Hardware era
> The generation of a computer's hardware, from Vintage to Singularity. Legacy is the first one FreeBSD runs on.

```table
{"caption": "FreeBSD at a glance", "rows": [[{"words": "Minimum hardware era"}, {"words": "Legacy"}], [{"words": "Disk space of the installed system"}, {"amount": 2048, "unit": "MB"}], [{"words": "Memory while it runs"}, {"amount": 16, "unit": "MB"}], [{"words": "Kernel"}, {"words": "FreeBSD 14.1-RELEASE GENERIC"}], [{"words": "Shell"}, {"words": "sh"}], [{"words": "Package manager"}, {"words": "pkg"}], [{"words": "Console"}, {"words": "ttyv0"}]]}
```

## What you see when it starts

When a prompt opens, FreeBSD greets you with the lines below. The first line names the architecture: IA-32, the 32-bit Intel architecture, on a 32-bit computer, and vel64 on a 64-bit one.

```session
{"lines": [{"text": "FreeBSD/vel64 (<hostname>) (ttyv0)"}, {"text": ""}, {"text": "login: <your name>"}, {"text": "FreeBSD 14.1-RELEASE (GENERIC)"}, {"text": ""}, {"text": "Welcome to FreeBSD, <your name>."}, {"text": "  Find a command by what it does:"}, {"text": "    apropos <word>"}, {"text": "  Learn the system:"}, {"text": "    man intro"}, {"text": "  See every command here:"}, {"text": "    Tab, twice"}, {"text": ""}, {"text": "Tip: pkg info lists every package installed here."}], "look": "jsc:guide/terminal/sh", "title": "FreeBSD"}
```

The greeting names three ways to find your way. The last section of this chapter explains each one ([](jsc:freebsd_help)).

## Desktops on FreeBSD

You add a desktop later, or choose one during the installation. A desktop is offered only when the network's Mirror answers and your computer's hardware is new enough for it.

> [!DEFINE] Mirror
> The package server of the network, run by a Mainframe that runs the Mirror service.

Table: Desktops on FreeBSD

| Desktop | Command | Needs at least |
| --- | --- | --- |
| KDE Plasma | `kde-plasma` | Legacy era, 256 MB of disk, 224 MB of memory |
| GNOME | `gnome` | Legacy era, 192 MB of disk, 256 MB of memory |
| CDE | `cde` | Legacy era, 32 MB of disk, 16 MB of memory |
| Cinnamon | `cinnamon` | Standard era, 160 MB of disk, 160 MB of memory |

The desktops of every system are described in [Desktops](jsc:desktops).

## If something goes wrong

> [!PROBLEM] The installer says "FreeBSD needs Legacy era hardware or newer; this machine is Vintage era."
> Install FreeBSD on a Legacy computer, or on a newer one.

> [!PROBLEM] The installer says "No disk is installed to put FreeBSD on."
> Put a disk in one of the computer's slots.

> [!PROBLEM] The installer says "No disk in this machine has room for it."
> Use a bigger disk, or erase a disk on the Partitioning page ([](jsc:bsdinstall)).

> [!SEE] jsc:the_systems, jsc:installing, jsc:bsdinstall
