---
section: frames_everyday
order: 220
---
# Files and the File Explorer

The Files window is where you look at the files on your disks, and open, copy, move, rename and delete them. On Frames 10 the same window is called File Explorer. On the other editions it is called Files.

The tree on the left groups the places: Quick access, Storage, Network and Shares. Quick access holds the shortcuts. Shares are the network folders you can reach from this window. The Recycle Bin is a place too: a file dropped on it is deleted.

## The bar above the list

Frames 7 shows a command bar above the list. Frames 10 shows a ribbon with the tabs File, Home and View. The Layout group on the ribbon chooses between Details and Large icons. The other editions show no bar. Press [[Esc]] to close an open ribbon.

## Keys

Table: Keys in the Files window

| Key | What it does |
| --- | --- |
| `Backspace` | Goes up one folder. |
| `Enter` | Opens the selected row. |
| `Up, Down` | Moves the selection. |
| `Delete` | Deletes the selected row, or every row you swept. |
| `F2` | Renames the selected row. |
| `Ctrl+C` | Copies the selected row. |
| `Ctrl+X` | Cuts the selected row, unless it is on a read-only medium. |
| `Ctrl+V` | Pastes what you copied or cut. |
| `Ctrl+A` | Selects every file and folder in the listing. |
| `Esc` | Closes the ribbon, if one is open. |

## Moving a file

1. Click the file to select it.
2. Press [[Ctrl]]+[[X]] to cut it, or [[Ctrl]]+[[C]] to copy it.
3. Open the folder you want, and press [[Ctrl]]+[[V]].

Right-click a row to see its menu: Open, Run, Open with, Choose another program, Cut, Copy, Paste, Rename, Delete, New, Eject, Refresh, Extract here and Compress to, with the name of the archive.

## Deleting and read-only places

On the system disk, Delete moves the file into the Recycle Bin. On a medium or a network share, Delete removes the file for good, once you say yes. A read-only medium refuses Cut and Delete, and the bar says "read-only medium".

A .dat file is different: it is a read-only list of the items stored on a disk. You can read it, but no one can change it by hand. The window says that to change those items you use the network interactor ([](jsc:interactor_screen)).

> [!DEFINE] Sweep
> Dragging a box over several rows to select all of them at once.

> [!DEFINE] .dat file
> A read-only file that lists the items stored on a disk. You can read it, but you cannot change it by hand.

## If something goes wrong

> [!PROBLEM] A file on a medium is gone, and there is no Recycle Bin to bring it back.
> Delete on a medium is permanent. Copy the file to the system disk first if you may need it.

> [!PROBLEM] The window says "This file is part of the installer and cannot be changed, copied or deleted."
> Run the setup program to install it, as the message says.

> [!PROBLEM] The window says "This file is impossible to modify, create, delete or change manually, use the network interactor for it."
> Read the file. To change its items, use the network interactor ([](jsc:interactor_screen)).

> [!PROBLEM] Cut and Delete are refused on a read-only medium.
> Copy the file with [[Ctrl]]+[[C]] and paste it onto a disk you can change.

> [!PROBLEM] A dialog titled "Could not do that" opens.
> Read the message in the dialog: it says what was refused.

> [!SEE] jsc:drives, jsc:interactor_screen
