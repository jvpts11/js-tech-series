---
section: system_v_menus
order: 352
---
# PACE, the menus in frames

PACE is the menu system of UNIX System V, and only that system has it. It opens a frame of menus over the screen, and each choice you make opens a frame of its own over the last one. The name stands for the Panelled Access Command Environment of Bellwether Labs' UNIX System V.

> [!DEFINE] Frame
> One window of menus in PACE. A new frame opens over the last one, and the keys move between them.

Type pace to start it. It takes the whole terminal, and its first frame is the main one, titled PACE. Choose Exit PACE, or press F5 on the last frame, and the terminal comes back.

Table: The main frame

| Choice | What it opens |
| --- | --- |
| `Office of player` | The Office: the Filecabinet, your home, and the other folders. |
| Programs | The programs that are installed. ENTER runs one. |
| System Administration | Machine Information, File Systems and Software Installation. |
| UNIX System | A shell of its own. Type exit at it to come back to PACE. |
| Exit PACE | Leaves PACE and gives the terminal back. |

Inside a frame, these keys work. The labels on the bottom line of the screen name the function keys.

Table: The keys in PACE

| Key | What it does |
| --- | --- |
| `Up, Down, Home, End, Page Up, Page Down` | Move in the frame. |
| `Enter or F2` | Opens the item. |
| `F1` | HELP: opens a help frame. |
| `F3` | PREV-FRM: goes to the frame before this one. |
| `F4` | NEXT-FRM: goes to the frame after this one. |
| `F5 or Escape` | CANCEL: closes the frame on top. On the last frame it leaves PACE. |
| `F6` | CMD-MENU: opens the command menu. |
| `A letter` | Moves to the next item that starts with that letter. |

A mouse click brings a frame to the front and picks its item. A second click on the same item opens it.

## The Office and its folders

The Filecabinet opens your home folder, /usr/player. Other Users lists the folders under /usr, except bin, lib and player, and says "(no other users)" when there are none. Preferences is greyed out, and it says "Nothing to set on this machine."

## Opening a file from PACE

1. Type pace and press Enter.
2. Choose Office of player and press Enter.
3. Choose Filecabinet and press Enter.
4. Move to a file with the arrow keys, and press Enter. A standard file opens in vi.

## Reading a folder

A folder frame is titled with its name and its path, and it lists what is in the folder. The vi editor is explained in [](jsc:vi). A Sigma listing is a program in the Sigma language, explained in [](jsc:sigma_two_languages).

Table: What ENTER does on each kind of entry

| Detail | What ENTER does |
| --- | --- |
| Directory | Opens the folder. |
| Sigma listing, a file ending in .sg | Opens the file in vi. F6 runs it. |
| Sigma# listing, a file ending in .sgs | Opens the file in vi. It does not run. |
| Executable, a file in /usr/bin or /bin | Runs the program. |
| Standard file | Opens the file in vi. |

Press F6 to open the command menu, except on a Sigma listing, where F6 runs the file. The menu's items are cancel, exit, help, next-frm, prev-frm, refresh, run and unix-system. Refresh reads the folder again.

Run opens a command line. Type a UNIX command, press Enter, and read its output. Escape or F5 cancels the line.

A command run from PACE shows its output, then says "Press ENTER to return to PACE." The UNIX System item has no such line: type exit in its shell to come back.

## System Administration

Table: What the items run

| Item | Command it runs |
| --- | --- |
| Machine Information | `uname -a` |
| File Systems | `df` |
| Software Installation | `installpkg` |

```command
id: jsc:command/unix/pace
```

## If something goes wrong

> [!PROBLEM] Preferences is greyed out.
> Nothing can be set on this system. The frame says "Nothing to set on this machine."

> [!PROBLEM] The Other Users frame says "(no other users)".
> Your home folder is /usr/player, and PACE does not list bin, lib or player under /usr, so no other user shows up.

> [!PROBLEM] ENTER on a Sigma file opens it in vi, and it does not run.
> Press F6 on a Sigma listing, a file ending in .sg, to run it.

> [!SEE] jsc:installpkg, jsc:vi, jsc:cde_on_system_v
