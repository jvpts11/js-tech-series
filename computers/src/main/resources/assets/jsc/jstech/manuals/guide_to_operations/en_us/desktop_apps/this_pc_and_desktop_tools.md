---
section: desktop_apps
order: 80
icon: jsc:monitor
---
# This PC, Files, the Editor and Settings

Some programs come with every desktop, under the names its system gives them. This page covers four of them: This PC, which shows the machine and its drives; Files, which holds your files; the Editor, which writes text; and Settings, which changes the way the desktop and the machine behave. The Network window has its own page ([](jsc:network_interactor)).

## This PC

This PC is the page of the machine itself. A card shows the computer's name, its kind, its era, its system and its network. Below it are the devices and drives: each disk with a bar that shows how full it is, and each linked drive with what is in it. For an installer in a linked drive, This PC names the program it would install.

Table: Parts of This PC

| Part | What it does |
| --- | --- |
| Card | The computer's name, kind, era, system and network. |
| Devices and drives | Each disk, and each linked drive with its Install, Open and Eject buttons. |
| Hardware | The parts fitted to the computer. |
| Programs | The programs installed, as a grid. |

This PC is on Frames, Linux and FreeBSD. UNIX has [Workstation Info](jsc:cde_workstation_info) in its place.

## Files

Files is the program for your files. Its toolbar has back, forward and up buttons. An address trail shows the folder you are in, and each part of it is clickable. A search box sits beside it.

The drive tree on the left holds the quick-access shortcuts and every volume, each with its letter. A removable disk has an eject control. The list in the middle shows the files of the folder, with an icon for each type, and you sort it by name, type or size.

Table: Parts of Files

| Part | What it does |
| --- | --- |
| Toolbar | Back, forward and up. |
| Address trail | The folder you are in, each part clickable. |
| Drive tree | Shortcuts and every volume, with its letter. |
| List | The files of the folder, sortable by name, type or size. |
| Status bar | Along the foot of the window. |

Double-click a folder to open it. Double-click an installer to run its setup, or a text file to open it in the Editor. The right-click menu offers cut, copy, paste, rename, delete, new file, new folder and properties, and it greys out what the volume forbids. An archive opens with 67ark ([](jsc:archiver)).

## The Editor

The Editor edits plain text. It is not an integrated development environment (IDE). It has no syntax highlighting, no completion and no compiler, so it only edits the text.

Table: Parts of the Editor

| Part | What it does |
| --- | --- |
| Line numbers | Beside each line of the text. |
| Find and replace | Finds text, and changes it. |
| Go to line | Jumps to the line you name. |
| Wrap | Breaks long lines to the width of the window. You can turn it off. |
| Bar at the foot | The line and the column you are on. |

You can have several files open at once. Open and Save As use the system's own file window. The Editor is the default for text files, so a .txt file opens there.

## Settings

Settings is the control panel. A list on the left holds its pages, and the page you pick fills the pane on the right. A change takes effect as soon as you make it.

The eight pages are Personalize, System, Network, Storage, Display, Programs, Sound and Users. The Programs page holds the line Uninstall a program.

1. Open Files from the desktop.
2. Double-click the folder that holds the text file.
3. Double-click the text file. It opens in the Editor.

## If something goes wrong

> [!PROBLEM] The Editor says a file is too large to open here.
> Split the text into smaller files, and open each one.

> [!PROBLEM] An item in the right-click menu is greyed out.
> The volume does not allow that change. Choose another action, or another volume.

> [!SEE] jsc:programs, jsc:archiver, jsc:help_on_a_computer
