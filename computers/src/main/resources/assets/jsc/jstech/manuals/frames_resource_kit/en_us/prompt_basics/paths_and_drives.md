---
section: prompt_basics
order: 234
---
# Paths, drives and the option switches

A path is the address of a file or a folder: the folder names, with a backslash between them, starting from a drive letter. C:\\DOS is the folder that MC-DOS searches for its programs by default. The rules for paths are in [the prompt, the drives and the paths](jsc:dos_prompt); this page gives the forms the prompt reads.

> [!DEFINE] Folder
> A named place on a disk that holds files, and other places of the same kind.

> [!DEFINE] Drive
> A disk that the computer can read, named by a letter. C: is the system disk.

Read a path from left to right. The forms below are the ones the prompt understands.

Table: Forms of a path

| Form | What it means |
| --- | --- |
| `..` | Goes up one folder. |
| `\DOS` | Starts from the root of the current drive, the top folder, not from the folder you are in. |
| `D:\WORK` | Starts from the root of drive D:, whichever folder you were in. |
| `D:` | Goes to the root of drive D:, not to the folder you used there before. |
| `.` | Is skipped, so it adds nothing to the path. |

The prompt shows where you are: the current folder, then a greater-than sign. At the top of drive C: it reads C:\\>. After you type cd C:\\DOS, it reads C:\\DOS>.

## Changing drive

Type a drive letter and a colon, and nothing else, and the prompt moves to that drive. Type D: to move to drive D:. The drives you can use are the ones linked to the computer.

```session
{"lines": [{"prompt": "C:\\DOS> ", "text": "cd", "typed": true}, {"text": "C:\\DOS"}], "look": "jsc:guide/terminal/dos", "title": "MC-DOS"}
```

With no words after it, cd prints the folder you are in, such as C:\\DOS.

## Switches

A switch changes what the command does. You write it the way that command takes it, such as /y. Each command reads only the switches it declares; the shell itself knows none of them.

> [!DEFINE] Switch
> An option word after a command's name, written the way that command takes it.

Table: Switches of the DOS family

| Switch | Command and what it does |
| --- | --- |
| `/y` | format: confirms that you want the drive erased. |
| `/PID` | taskkill: names the task to stop, by its number. |
| `/EVERY:` | at: runs the job only on the days named after it. |
| `/DELETE` | at: takes one job off the list. |
| `/I` | find: ignores capital letters. |
| `/V` | find: prints the lines that do not hold the text. |
| `/R` | sort: reverses the order of the lines. |
| `/LOCAL` | interac: leaves what it takes in this computer, not in your hands. |
| `/S:COUNT` | interac: orders a listing by how much there is. |

## Asking for help

Table: Two ways to ask for help

| Form | Where it works |
| --- | --- |
| `--help` | After the command's name, on every family: MC-DOS, MC-NET, Frames and the Unix systems. |
| `/?` | After the command's name, on the DOS family only: MC-DOS and the Frames editions. |

Both forms print the command's page, the same text that help <command> prints. You can read the same page here, in the command pages of this book.

## Quotes and names

Words in double quotes stay together as one word. The find command takes its text that way, so find "oak log" looks for the whole phrase.

On the DOS family, a name between percent signs, such as %USERNAME%, is replaced by its value before the command runs. The built-in names are HOSTNAME, COMPUTERNAME, USER, USERNAME, OS, CD (the prompt) and PATH. You give a name a value with set ([](jsc:shell_verbs)).

A star (\*) in a name is passed to the command as it is. The command decides what the star matches.

If the word you type is not a command, the shell searches for a program of that name in the PATH. The PATH is the list of folders it searches. On MC-DOS it comes from the PATH line of C:\\AUTOEXEC.BAT, and C:\\DOS is the default.

## If something goes wrong

> [!PROBLEM] The system cannot find the path specified.
> Check the folder name with dir. A drive letter alone, such as C:, goes to the root of that drive, not to the folder you were in.

> [!PROBLEM] The system cannot find the file specified.
> The file is not in the folder you are in. Type its full path, such as C:\\DOS\\NAME, or go to its folder first with cd.

> [!PROBLEM] MYFILE.TXT: file not found
> The file name is wrong, or the file is in another folder. Check the name with dir.

> [!PROBLEM] D:\\ The system cannot find the drive specified.
> No drive D: is linked to this computer. Pick a drive the computer has, or link a drive to it.

> [!PROBLEM] D:\\ The device is not ready.
> The drive has no disk in it. Put a disk in the drive, then try again.

> [!PROBLEM] A command typed with /? on MC-NET or a Unix system does not show its help.
> Type --help after the command's name on those systems.

> [!SEE] jsc:dos_prompt, jsc:finding_a_command
