---
section: dos_programs
order: 206
---
# DOSSHELL and its keys

The MC-DOS Shell is a menu program for the files of the system. You see the folders and the files, copy, move or delete them, and start programs with the keys, without typing each command yourself. Every action of its File menu runs one of MC-DOS's own commands.

Type dosshell at the prompt to open it. It runs on MC-DOS only, takes the whole screen and opens on the top folder of C:.

1. Type dosshell at the prompt and press [[Enter]].
2. Press [[Tab]] to move from one area to the next, and the arrow keys to move within one.
3. Press [[Enter]] on a program to start it as a task.
4. Hold [[Alt]] and press [[Tab]] to pick the next task, and let go of [[Alt]] to switch to it.
5. Press [[F3]] to leave the shell, once no task is listed.

Table: The areas of the screen

| Area | What it shows |
| --- | --- |
| Directory Tree | The folders of the disk you are on. |
| Drives | The drives the machine has, shown only when it has some. |
| Main | The files of the folder you picked. |
| Active Task List | The tasks you started, shown while the Task Swapper is on. |

Table: The views

| View | What it shows |
| --- | --- |
| Program/File Lists | The tree and the files, over the programs and the tasks, the way the shell opens. |
| Single File List | The tree and the list of files, down to the bottom of the screen. |
| Program List | The programs and the tasks only. |

Dual File Lists and All Files answer "Not available on this machine." Select Across Directories does the same.

The program list has a group called Main. It holds Command Prompt, which starts a command prompt as a task, and Sigma when Sigma is installed. It holds the groups [Programs] and [Disk Utilities]. [Programs] lists the programs you installed, except the Command Prompt.

Table: Keys in the shell

| Key | What it does |
| --- | --- |
| `Tab` | Moves to the next area, and Shift with Tab goes back. |
| `Arrows` | Move within an area. |
| `Page Up` | Moves a page up the list. |
| `Page Down` | Moves a page down the list. |
| `Home` | Goes to the first item. |
| `End` | Goes to the last item. |
| `Enter` | Opens the item picked, or starts a program as a task. |
| `F10` | Opens the File menu. |
| `Alt+F` | Opens the File menu. |
| `Alt+O` | Opens the Options menu. |
| `Alt+V` | Opens the View menu. |
| `Alt+T` | Opens the Tree menu. |
| `Alt+H` | Opens the Help menu. |
| `F3` | Leaves the shell, which is refused while a task is listed. |
| `Alt+F4` | Leaves the shell, as F3 does. |
| `F5` | Reads the disk again. |
| `Shift+F5` | Draws the screen again. |
| `F7` | Moves the selected file. |
| `F8` | Copies the selected file. |
| `F9` | Shows the contents of the selected file, which Escape closes. |
| `Shift+F9` | Opens a Command Prompt as a task. |
| `Del` | Deletes the selected file, or the selected empty folder in the tree. |
| `Alt+Tab` | Switches to the next task, and Shift+Alt+Tab goes back. |
| `Escape` | Leaves a group of the program list, or closes a menu. |
| `+` | In the tree: shows the folders in the selected folder. |
| `*` | In the tree: shows every folder under the selected folder. |
| `-` | In the tree: puts away the folders under the selected folder. |

Table: The File menu

| Item | Key | What it does |
| --- | --- | --- |
| Open | `Enter` | Starts a program, or opens the folder picked. |
| View File Contents | `F9` | Shows the file, as the TYPE command does. |
| Move..., from and to names | `F7` | Moves a file, as MOVE does. |
| Copy..., from and to names | `F8` | Copies a file, as COPY does. |
| Delete..., with a question when Confirmation is on | `Del` | Deletes the file, as DEL does, or the folder, as RMDIR does. |
| Rename..., with a new name | none | Gives the file another name, as REN does. |
| Create Directory..., with the parent and a name | none | Makes a folder, as MKDIR does. |
| Exit | `F3` | Leaves the shell for the prompt. |

The Options menu holds Confirmation, which asks before deleting, and Enable Task Swapper. The Task Swapper keeps each program you start as a task, so that you can switch between them. Show Information shows the selected file, its folder and its disk.

Disk Utilities holds Format. Its dialog has a field that starts with the text "a:". Replace the text with the drive you mean and add /y, as in D: /y. Without /y, Format prints its warning and erases nothing.

Print sends the selected file to a printer ([](jsc:printers)). It is greyed out when no printer is linked.

The bottom line shows the keys: F10=Actions, Shift+F9=Command Prompt and, while the Task Swapper is on, Alt+Tab=Next Task.

> [!DEFINE] MC-DOS Shell
> The menu program of MC-DOS for its files and programs, opened with the command dosshell.

> [!DEFINE] task
> A program or a Command Prompt that runs from the shell and is kept in the Active Task List.

```command
id: jsc:command/mc_dos/dosshell
```

## If something goes wrong

> [!PROBLEM] Enter on a file does nothing.
> Only the files that end in .EXE, .COM and .BAT start from the shell. Press [[F9]] to read a text file.

> [!PROBLEM] The screen says You cannot quit MC-DOS Shell with programs in the Active Task List; quit those programs first.
> Finish each task, then press [[F3]]. The tasks are the programs listed in the Active Task List.

> [!PROBLEM] The screen says No files match file specifier.
> The pattern in File Display Options shows no file. Change it, for example to \*.\* to see every file.

> [!PROBLEM] The screen says Drive not ready.
> The drive has no disk. Put a disk in it, or pick another drive.

> [!PROBLEM] The screen says Not available on this machine.
> The shell does not offer that item. Use the commands at the prompt for the same job.

> [!SEE] jsc:dos_msd, jsc:dos_help
