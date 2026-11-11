---
section: shell_commands
order: 251
---
# Working with files and folders

Most of your work on a Unix system is with files and folders: you list them, move between them, copy, rename and delete them. These commands need a disk, the system disk or one in a drive.

Here is a short session. It shows how the shell answers when a command gets no file name.

```session
{"lines": [{"prompt": "player@ubuntu:~$ ", "text": "cat", "typed": true}, {"text": "cat: usage: cat <file>"}], "look": "jsc:guide/terminal/bash", "title": "Ubuntu"}
```

Folder names end with a slash in the ls listing, so you can tell files and folders apart at a glance. The commands below are the commonest first.

1. Type write notes.txt followed by the text you want in the file.
2. Type cat notes.txt to read the file back.
3. Type ls to see the file in the folder.

```command
id: jsc:command/linux/ls
```

```command
id: jsc:command/linux/cd
```

```command
id: jsc:command/linux/pwd
```

```command
id: jsc:command/linux/cat
```

```command
id: jsc:command/linux/mkdir
```

```command
id: jsc:command/linux/touch
```

```command
id: jsc:command/linux/write
```

```command
id: jsc:command/linux/cp
```

```command
id: jsc:command/linux/mv
```

```command
id: jsc:command/linux/rm
```

```command
id: jsc:command/linux/rmdir
```

```command
id: jsc:command/linux/run
```

```command
id: jsc:command/linux/du
```

```command
id: jsc:command/linux/df
```

The df command lists every disk the computer has, with its device name. The mkfs.ext4 command makes a new, empty file system on one device.

```command
id: jsc:command/linux/mkfs.ext4
```

> [!WARNING] mkfs.ext4 erases the whole device. Everything on it is lost, and the erase cannot be undone.

## If something goes wrong

> [!PROBLEM] The shell says usage: cat <file>, because you gave no file name.
> Give the name of one file after cat.

> [!PROBLEM] The mkfs.ext4 command says it cannot open a device, and that there is no such device.
> Run df and choose one of the devices it lists.

> [!PROBLEM] The df command says there are no filesystems mounted.
> No disk is in the system or in a drive. Put one in, then run df again.

> [!SEE] jsc:file_tree, jsc:text_tools
