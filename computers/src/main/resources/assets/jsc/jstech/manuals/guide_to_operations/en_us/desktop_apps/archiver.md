---
section: desktop_apps
order: 83
icon: jsc:cd_rom
---
# 67ark

67ark packs many files into one archive that weighs less, and it takes them back out again. That is how a small disk is made to stretch: the archive takes less room than the files it holds.

> [!DEFINE] Archive
> One file that holds other files, packed to weigh less. It ends in .ark.

> [!DEFINE] Pack
> To put files into an archive, which stores them in less room.

> [!DEFINE] Extract
> To copy files back out of an archive.

The window does one of two jobs, and it says which one you are doing. Either an archive is open, and its contents are listed, or you are putting an archive together from files you chose on the disk.

Table: Columns of the archive list

| Column | What it shows |
| --- | --- |
| Name | The name of the file inside the archive. |
| Size | The size of the file, in bytes, kilobytes or megabytes. |
| Saved | The percentage by which the archive is smaller than the files. |

1. Open an archive, or start a new one with New.
2. Press Add, and choose the files you want to pack.
3. Press Pack. The server does the packing, so the saving on disk is real.
4. Press Extract for the files you chose, Extract all for every file, or Take out into a folder.

After packing, the window asks whether to delete the original files or keep them.

## At the prompt

67ark also works at the command prompt, with the letters of 7-Zip. A program can pack its own files this way.

Table: 67ark at the prompt

| Command | What it does |
| --- | --- |
| 67ark a logs.ark ops.txt | Packs ops.txt into logs.ark. If logs.ark is already there, the file is added to it. |
| 67ark x logs.ark | Takes every file out where the prompt stands. Give a folder after it to take them out there. A file that is already there is left as it is. |
| 67ark l logs.ark | Lists the files in the archive with their sizes. |

The archive packs its files together, so only the whole archive has a packed size. The list shows it beside the file when the archive holds one file, and on the total line otherwise.

```command
id: jsc:command/mc_net/67ark
```

## If something goes wrong

> [!PROBLEM] 67ark says that file is not an archive.
> Open only a file that ends in .ark.

> [!PROBLEM] 67ark says an archive holds at most 512 files.
> Pack the files into two archives.

> [!SEE] jsc:programs, jsc:this_pc_and_desktop_tools
