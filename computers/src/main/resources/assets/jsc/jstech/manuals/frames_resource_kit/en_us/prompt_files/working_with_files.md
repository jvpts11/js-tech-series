---
section: prompt_files
order: 236
---
# Working with files and folders

Most of the prompt's work with files is this: you list them, move between folders, copy, rename and delete them. Each verb takes its words in the order the table shows, and each one has a page of its own below.

These verbs need a machine with a file system. MC-NET has none, so they are not on its prompt.

> [!DEFINE] Extension
> The letters after the last dot in a file name. They tell the system what kind of file it is.

> [!DEFINE] Read-only
> A file that you can read, but cannot change or delete.

Table: The file verbs

| Verb | Words after it, and what it does |
| --- | --- |
| `dir` | Takes an optional folder. Lists the files and folders in it. |
| `cd (chdir)` | Takes an optional folder. Shows the folder you are in, or changes to the folder you name. |
| `type` | A file name. Prints the lines of the file. |
| `write (save)` | A file name, then the text. Creates the file, or overwrites it, with the text. |
| `del (erase)` | A file name. Deletes one file from the system disk. |
| `copy` | A source, then a destination. Copies a file, or a folder tree within one drive. |
| `move` | A source, then a folder. Moves a file or a folder tree into that folder. |
| `ren (rename)` | A file name, then the new name. Renames the file in place; it does not move it. |
| `mkdir (md)` | A folder name. Creates the folder. |
| `rmdir (rd)` | A folder name. Removes the folder, if it is empty. |
| `tree` | No words. Draws the folders under the one you are in. |
| `run` | An .iql file name. Runs the file as IQL statements, the network's query language. |

dir lists the folder you name, or the folder you are in. Each row shows when the item was last written, its size and its name. A folder shows <DIR> in place of a size, and a read-only file has [RO] after its name. The last line counts the files and the folders and gives the total size in mB, the unit the listing prints. An empty folder says File Not Found.

```command
id: jsc:command/mc_dos/dir
```

cd with no words prints the folder you are in. cd with a folder changes to it. A drive letter alone goes to the root of that drive ([](jsc:paths_and_drives)).

```command
id: jsc:command/mc_dos/cd
```

type prints each line of a file. An empty file prints the dim line "(empty file)". A .dat file is refused, because it is the view of a disk's items.

```command
id: jsc:command/mc_dos/type
```

write takes the file name, then everything after it as the text. The extension decides the kind of file. You can write files with the extensions iql, txt, cfg, csv and cmd. You cannot write log and craft files by hand. On success write prints "wrote" and the file name.

```command
id: jsc:command/mc_dos/write
```

del deletes one file and prints "deleted" and its name. A .dat file cannot be deleted.

```command
id: jsc:command/mc_dos/del
```

copy copies one file, or a whole folder tree within one drive. Between drives it copies files only, and on success it prints "1 file(s) copied." A file on another machine can be copied, but not moved.

```command
id: jsc:command/mc_dos/copy
```

move takes the same words as copy. It moves a file or a folder tree into another folder, and between drives it moves files only. On success it prints "1 file(s) moved."

```command
id: jsc:command/mc_dos/move
```

ren changes a name in place. The new name is everything after the old name. ren does not move the file, so use move for that.

```command
id: jsc:command/mc_dos/ren
```

mkdir creates a folder. Some drives are flat and hold no folders at all, and mkdir refuses them.

```command
id: jsc:command/mc_dos/mkdir
```

rmdir removes a folder, but only an empty one. A folder with files in it is refused.

```command
id: jsc:command/mc_dos/rmdir
```

tree draws the folders under the one you are in. With none, it says "No subfolders exist".

```command
id: jsc:command/mc_dos/tree
```

run runs an .iql file, as if you had typed its statements with iql ([](jsc:network_verbs)). Only .iql files run. A result prints in green on success and in red on failure.

```command
id: jsc:command/mc_dos/run
```

## If something goes wrong

> [!PROBLEM] NAME: .dat files are read-only (use the Network Interactor to access items)
> A .dat file is the view of the disk's items. To move items, use interac ([](jsc:network_verbs)).

> [!PROBLEM] NAME: .log is read-only
> You cannot change log files by hand. For the network's log, see [](jsc:cancelling_and_the_log).

> [!PROBLEM] NAME: .craft files cannot be edited
> Crafts are asked for on the network, not written by hand ([](jsc:asking_for_a_craft)).

> [!PROBLEM] NAME: not enough free space on the disk
> Delete a file you no longer need, or use a larger disk.

> [!PROBLEM] Directories are not supported on this drive.
> This drive is flat and holds no folders. Put the folder on another drive.

> [!PROBLEM] NAME: a file of that name is already there
> Choose another name, or delete the old file first.

> [!PROBLEM] NAME: the destination is read-only
> Copy or move the file into a folder that you can write to.

> [!SEE] jsc:paths_and_drives, jsc:storage
