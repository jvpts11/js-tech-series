---
section: prompt_files
order: 238
---
# Partitions: FDISK and diskpart

A disk can hold several partitions, each a drive of its own ([](jsc:partitions)). MC-DOS divides a disk with FDISK, and Frames with diskpart; on Frames XP and later, Disk Management does the same in a window.

> [!DEFINE] Disk Management
> The window of Frames XP to 11 that makes, formats, resizes and deletes the volumes of each disk.

FDISK opens a menu. Its option 1 makes a partition of the size you type, option 2 sets the active one, the partition the machine starts, option 3 deletes one and option 4 shows them. An empty choice leaves it.

```command
id: jsc:command/mc_dos/fdisk
```

diskpart takes its commands at its own prompt, DISKPART>: "list disk", then "select disk 1", then "create partition primary", "format fs=ntfs quick", and "exit" to leave.

```session
{"lines": [{"prompt": "DISKPART> ", "text": "select disk 1", "typed": true}, {"text": "Disk 1 is now the selected disk."}, {"prompt": "DISKPART> ", "text": "create partition primary", "typed": true}, {"text": "DiskPart succeeded in creating the specified partition."}], "look": "jsc:guide/terminal/dos", "title": "Frames"}
```

```command
id: jsc:command/frames/diskpart
```

## If something goes wrong

> [!PROBLEM] diskpart says there is no disk selected.
> Type select disk and the number list disk shows, first.

> [!PROBLEM] The partition the system runs from is refused.
> Neither tool deletes or formats it. Change another disk.

> [!SEE] jsc:format_a_drive, jsc:disk_tools_of_each_system
