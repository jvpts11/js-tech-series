---
section: system_v_desktop
order: 353
---
# CDE on System V

CDE, the Common Desktop Environment, is the only desktop UNIX System V has. It is a desktop with windows and a panel, and it needs a Legacy computer or a newer one. A Vintage computer runs the system at a prompt only.

CDE is a program, so you install it from its own medium, a CD. Once it is installed, the computer starts into it after a restart. The rest of CDE, its panel, its menus and its tools, is explained in [](jsc:cde).

```table
{"caption": "CDE on System V at a glance", "rows": [[{"words": "Hardware era needed"}, {"words": "Legacy or newer"}], [{"words": "Space on the disk"}, {"amount": 32, "unit": "MB"}], [{"words": "Memory while it runs"}, {"amount": 16, "unit": "MB"}], [{"words": "Install medium"}, {"words": "CD, read by a CD Drive"}], [{"words": "Installed with"}, {"words": "installpkg cde"}], [{"words": "Systems that run it"}, {"words": "UNIX System V, FreeBSD and Linux"}]]}
```

1. Put the CDE CD in a CD Drive linked to the computer.
2. Type installpkg cde and press Enter.
3. Wait for the setup to finish. Its line names the program and the CD.
4. Restart the computer. The desktop starts at the next boot.

The desktop has a raised panel at the bottom centre of the screen, with four workspaces named One, Two, Three and Four. Its programs include a file manager, which the panel calls Files, a text editor, a terminal, a calculator, a performance meter, the Style Manager and the Help Viewer.

## If something goes wrong

> [!PROBLEM] The installation says "CDE needs Legacy hardware or newer. This computer is Vintage."
> Use a Legacy computer or a newer one. A Vintage computer runs System V at a prompt only.

> [!PROBLEM] The installation says "CDE needs 32 MB free on the disk. There are <n> MB."
> Free up disk space, or install on a bigger disk.

> [!PROBLEM] The installpkg line says "no package medium in a drive".
> Put the CDE CD in a CD Drive linked to the computer, and try again.

> [!PROBLEM] The computer starts at a prompt after CDE is installed.
> The desktop shows only after a restart. Restart the computer.

> [!SEE] jsc:cde, jsc:desktops, jsc:installpkg
