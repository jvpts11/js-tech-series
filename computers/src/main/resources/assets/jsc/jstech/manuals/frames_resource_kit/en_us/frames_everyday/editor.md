---
section: frames_everyday
order: 221
---
# The Editor

The Editor writes and changes text files. Each file you open gets a tab of its own, and you can have up to 8 files open at once. It shows line numbers, so you can find your place, and the line and column of the cursor in the status bar.

Long lines can wrap onto the next row of the screen. You can turn wrap off, so that each line stays on one row.

## Keys

Table: Keys in the Editor

| Key | What it does |
| --- | --- |
| `Ctrl+S` | Saves the file. A file with no name asks for one first. |
| `Ctrl+O` | Opens a file through the system's file window. |
| `Ctrl+P` | Prints the file. |
| `Ctrl+F` | Opens the find bar. |
| `Ctrl+G` | Opens the find bar in Go to line mode. |
| `Ctrl+N` | Opens a new tab with a new file. |
| `Ctrl+W` | Closes the current tab. |
| `Ctrl+Tab` | Switches to the next tab. |
| `Enter` | While the find bar has focus, runs its action. |
| `Esc` | While the find bar is open, closes it and returns to the text. |

## The find bar

Press [[Ctrl]]+[[F]] to open the find bar, which offers Replace, Next and Replace all. Press [[Ctrl]]+[[G]] to use the same bar to go to a line: the bar then offers Go to and Go.

## Saving and closing

1. Press [[Ctrl]]+[[S]] to save the file under its name.
2. If the file has no name, the Editor says "Save As first". Use Save As to give it a name, then save.
3. To close a tab with changes you have not saved, press [[Ctrl]]+[[W]] and read the message.
4. Save with [[Ctrl]]+[[S]], or press [[Ctrl]]+[[W]] again to discard the changes.

> [!DEFINE] Tab
> One open file in the Editor. The Editor keeps up to 8 tabs at once.

> [!DEFINE] Wrap
> The way a long line continues on the next row of the screen. You can turn it off.

> [!DEFINE] Find bar
> The strip where you type what to find. It also does the Go to line action.

## If something goes wrong

> [!PROBLEM] The Editor says "Save As first".
> The file has no name yet, so [[Ctrl]]+[[S]] cannot save it. Give it a name first.

> [!PROBLEM] The Editor says "Cannot save a .dat file".
> A .dat file is read-only: you can read it, but the Editor cannot save it.

> [!PROBLEM] The Editor reports that the file is too long to save, with its length and the limit in characters.
> Shorten the text. A file holds up to 32,767 characters.

> [!PROBLEM] The Editor reports that the path is too long to save to, with its length and the limit in characters.
> Use a shorter name or fewer folders. A path holds up to 160 characters.

> [!PROBLEM] The Editor says "Only 8 files at once".
> Close a tab with [[Ctrl]]+[[W]] first.

> [!PROBLEM] The Editor warns about unsaved changes when you close a tab, and asks you to close again to discard them.
> Save with [[Ctrl]]+[[S]], or close the tab again to discard the changes.

> [!PROBLEM] The Editor says a file "is too large to open here".
> The Editor leaves the file as it is, so nothing is lost.

> [!SEE] jsc:file_explorer
