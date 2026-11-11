---
section: dos_basics
order: 205
---
# The system's own files

When MC-DOS is installed, the system disk, C:, shows you some files that were never copied onto it. The system builds them from what is installed each time the disk is read, so they are not stored on the disk. They sit at the top of C: and in its DOS folder.

Table: The files of the system disk

| File or folder | What it is |
| --- | --- |
| `DOS\` | The folder of the system's own files. |
| `AUTOEXEC.BAT` | The startup file, with the PATH line. |
| `CONFIG.SYS` | The configuration file, whose lines have no effect on this system. |
| `COMMAND.COM` | The command interpreter: the program that reads what you type and runs it. |
| `MCDOS.SYS` | The system itself. |
| `DOS\HIMEM.SYS` | The memory driver of the system. |
| `DOS\README.TXT` | The notes of the system, which tell you to type HELP for the commands. |
| `PROGRAM\` | The folder of an installed program, named with up to eight capital letters. |
| `PROGRAM\PROGRAM.EXE` | The program itself. |
| `PROGRAM\README.TXT` | The notes of that program. |

AUTOEXEC.BAT holds three lines: @ECHO OFF, PROMPT $P$G and a PATH line. The PATH line lists the DOS folder first, then the folder of each installed program in the order they were installed. When you type only a program's name, MC-DOS searches the folders on that line. If AUTOEXEC.BAT or its PATH line is missing, it searches the DOS folder only.

The PROMPT line does not set your prompt. The prompt always shows the folder you are in and a greater-than sign ([](jsc:dos_prompt)).

CONFIG.SYS holds the lines DEVICE=C:\\DOS\\HIMEM.SYS, DOS=HIGH, FILES=30 and BUFFERS=20. The file is there for you to read, but its lines have no effect on this system.

## If something goes wrong

> [!PROBLEM] You type the name of a program you installed, and the prompt says command not found.
> Type the full path of the program's .EXE file: C:\\ followed by its folder and its file name. Programs typed by name are found in the folder you are in and in the folders of the PATH line.

> [!PROBLEM] You expect the PROMPT line to change the prompt, and it does not.
> The prompt always shows the folder and a greater-than sign. No line of AUTOEXEC.BAT changes it ([](jsc:dos_prompt)).

> [!SEE] jsc:dos_prompt, jsc:dos_shell
