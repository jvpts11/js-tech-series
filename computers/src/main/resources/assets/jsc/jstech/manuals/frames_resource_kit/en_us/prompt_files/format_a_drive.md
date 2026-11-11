---
section: prompt_files
order: 237
---
# Formatting a drive

Formatting erases everything on a drive and prepares the drive again to hold files. Use it on a new disk, or on a disk whose files you no longer need.

> [!DEFINE] Format
> To erase every file on a drive and prepare the drive again.

> [!DEFINE] Volume
> The disk as the machine sees it, with the files on it.

> [!WARNING] Formatting erases every file on the drive, and there is no undo. Check the drive letter before you run the line.

1. Type format, a space, the drive letter and a colon, such as D:.
2. Read the warning. It names the drive that will be erased.
3. Type the same line with /y at the end, such as format D: /y, to start the format.

format asks for /y before it does anything. Without it, format prints the warning in red, then the dim line that shows the line to type. The erasing itself needs the /y.

```session
{"lines": [{"prompt": "C:\\> ", "text": "format D:", "typed": true}, {"text": "WARNING: ALL DATA ON DRIVE D: WILL BE LOST!"}, {"text": "Run 'format D: /y' to proceed."}], "look": "jsc:guide/terminal/dos", "title": "MC-DOS"}
```

Table: The messages of format

| Message | What it means |
| --- | --- |
| usage: format <drive>: [/y] | You typed format with no drive. |
| format: invalid drive: NAME | The first word is not a drive letter. |
| format: drive D: drive not ready | No disk is in that drive. |
| format: drive D: not found | No drive of that letter is linked to the computer. |
| format: cannot format drive C: - the running system lives on it | The system disk cannot be formatted from its own prompt. |
| Formatting drive D: ... done | The drive is formatted. The next line says: All data on the volume was erased. |

```command
id: jsc:command/mc_dos/format
```

## If something goes wrong

> [!PROBLEM] format says it cannot format drive C:.
> C: holds the running system. Format a second drive instead ([](jsc:drives)).

> [!PROBLEM] format says the drive is not ready.
> Link a disk to that drive ([](jsc:drives)), then try again.

> [!PROBLEM] The wrong drive was erased.
> Check the drive letter and the warning before you add /y.

> [!SEE] jsc:disks, jsc:drives
