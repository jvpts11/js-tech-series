---
section: openbsd_system
order: 342
---
# OpenBSD on this computer

OpenBSD is a UNIX-like system made by the Blowfish Project, and it is secure by default: it starts at its terminal with only what it needs switched on, and everything else waits for you to switch it on. Its mark is Puffy, the blowfish.

> [!DEFINE] Secure by default
> A system that comes up with as little running as it can, so nothing runs that you did not ask for.

OpenBSD needs Legacy hardware or newer. It takes 1,024 MB of disk space, less when you leave sets out at the installer, and 12 MB of memory while it runs.

```table
{"caption": "OpenBSD at a glance", "rows": [[{"words": "Minimum hardware era"}, {"words": "Legacy"}], [{"words": "Disk space of the installed system"}, {"amount": 1024, "unit": "MB"}], [{"words": "Memory while it runs"}, {"amount": 12, "unit": "MB"}], [{"words": "Kernel"}, {"words": "OpenBSD 7.5 GENERIC#79"}], [{"words": "Shell"}, {"words": "ksh"}], [{"words": "Package manager"}, {"words": "pkg_add"}], [{"words": "Console"}, {"words": "ttyC0"}]]}
```

## What you see when it starts

Its prompt is the computer's name and a hash, because you are root. The greeting names the ways to find your way: apropos and the afterboot page are there only when the man set is installed.

```session
{"lines": [{"text": "OpenBSD/IA-32 (<hostname>.my.domain) (ttyC0)"}, {"text": ""}, {"text": "login: player"}, {"text": "OpenBSD 7.5 (GENERIC) #79"}, {"text": ""}, {"text": "Welcome to OpenBSD: The proactively secure Unix-like operating system."}, {"text": "  Find a command by what it does:"}, {"text": "    apropos <word>"}, {"text": "  Learn the system:"}, {"text": "    man afterboot"}, {"text": ""}, {"text": "Tip: rcctl ls on lists every service switched on here."}], "look": "jsc:guide/terminal/sh", "title": "OpenBSD"}
```

## A desktop on OpenBSD

OpenBSD takes the same desktops FreeBSD takes. A desktop installed still comes up at the terminal: its display manager, xenodm, starts it only once you type rcctl enable xenodm ([](jsc:openbsd_daemons)). Every desktop hangs the same picture of the sea.

## If something goes wrong

> [!PROBLEM] A desktop is installed and the computer still starts at the terminal.
> xenodm is off, which is how OpenBSD ships. Type rcctl enable xenodm, and rcctl start xenodm to bring it up now.

> [!PROBLEM] The prompt says ksh: man: not found.
> The man set was left out at the installer. Install the system again with it.

> [!SEE] jsc:the_systems, jsc:openbsd_installer, jsc:openbsd_daemons
