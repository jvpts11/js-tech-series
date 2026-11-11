---
section: dos_basics
order: 203
---
# The prompt, the drives and the paths

MC-DOS waits for you at the prompt, the line where you type a command and press [[Enter]]. The prompt shows where you are: the folder you are in, then a greater-than sign. At the top of the system disk it reads C:\\>, and in a folder called SYSTEM it reads C:\\SYSTEM>. A command works on the folder the prompt shows, unless you name another place.

Every disk and drive has a drive letter: a letter and a colon, such as C:. Type the letter and the colon to change drive, for example D:. You then stand at the top folder of that drive. MC-DOS keeps no current folder for each drive, so a drive does not remember where you were.

Table: The drive letters

| Letter | Which drive it is |
| --- | --- |
| `C:` | The system disk, or the first data disk when there is no system disk. |
| D: and up | The data disks in the disk slots, in slot order; the first one is D: when there is a system disk. |
| After the disks | The readers, the Dock Station bays and the case-front drives, each keeping its letter. |
| `A: and B:` | Never. The machine has no drive A: and no drive B:. |

Removable drives are the ones you join to the computer ([](jsc:drives)). Data disks are the disks in its slots ([](jsc:disks)).

Table: Typed at the prompt

| Typed | What it means |
| --- | --- |
| `D:` | Changes to drive D: and its top folder. |
| `D:DIR` | The folder DIR at the top of drive D:. |
| `\` | The top folder of the current drive. |
| `\DIR` | The folder DIR at the top of the current drive. |
| `..` | The folder above; at the top folder it stays put, with no error. |

A path is the full address of a file or a folder: its drive, its folders and its name. A path can join the folders with a backslash or a slash, so C:\\DOS\\README.TXT and C:/DOS/README.TXT name the same file.

When you type only a word, MC-DOS takes it for a built-in command first, and then for a program. It looks for the program in the folder you are in, then in the folders of the search path, the list kept in the PATH setting.

To run a program that is in another folder, type its full path, such as C:\\DOS\\HELLO.

The commands for folders are dir, cd, mkdir and rmdir. dir lists a folder, cd shows the current folder or changes it, mkdir makes a folder and rmdir removes an empty one. [Working with files and folders](jsc:working_with_files) explains them all.

Type a command's name, a space and /? to read its page. Typing help dir opens the same page ([](jsc:dos_help)).

> [!DEFINE] Prompt
> The line where MC-DOS waits for a command. It shows the current folder and a greater-than sign.

> [!DEFINE] Drive letter
> The name a disk or a drive has on MC-DOS: a letter and a colon, such as C:.

> [!DEFINE] Path
> The full address of a file or a folder: its drive, its folders and its name.

> [!DEFINE] Search path
> The list of folders MC-DOS searches for a program when you type only the program's name.

## If something goes wrong

> [!PROBLEM] The prompt says "command not found: HELLO".
> No command or program of that name is in the folder you are in or on the search path. Check the spelling, or type the full path, such as C:\\DOS\\HELLO. [Finding a command](jsc:finding_a_command) explains the rest.

> [!PROBLEM] The prompt says E:\\ The system cannot find the drive specified.
> The machine has no drive with that letter. Type one of the letters in the table above.

> [!PROBLEM] The prompt says "D:\\ The device is not ready."
> The drive has no disk in it. Put a disk or a medium in the drive ([](jsc:drives)).

> [!PROBLEM] The prompt says no system disk or OS installed.
> The computer has no system disk to start from. Put a disk in a disk slot, or install MC-DOS on one ([](jsc:dos_install)).

> [!PROBLEM] The prompt says The system cannot find the path specified.
> The folder does not exist. List the folder with dir, then type the name again.

> [!PROBLEM] The prompt says The directory is not empty.
> Delete the files in the folder first, then remove the folder with rmdir.

> [!SEE] jsc:working_with_files, jsc:paths_and_drives
