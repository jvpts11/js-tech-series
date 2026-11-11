---
section: library_basics
order: 440
---
# Files: whole files and files you open

A file is a named piece of data on a disk. Its path is its full name: the drive letter that names the disk, then the folders, then the file's own name.

There are two ways to work on a file. File works on a whole file at once: it reads all of it, or writes all of it. File.Open gives you a FILE instead, which you read and write a piece at a time until you close it.

## Whole files

Table: File members

| Member | What it does |
| --- | --- |
| `File.Exists(path)` | Yes when the file can be read. |
| `File.Read(path)` | The whole text of the file. Stops the program when the file is missing. |
| `File.TryRead(path, out text)` | True and the text when the file is read. False and empty text otherwise. |
| `File.Write(path, text)` | Replaces the file with the text. False when the write did not happen. |
| `File.Append(path, text)` | Adds the text to the end of the file. False when the add did not happen. |
| `File.Delete(path)` | Removes the file. False when refused. |
| `File.MkDir(path)` | Makes a folder. False when refused. |
| `File.List(path)` | The names in a folder, as a list. |
| `File.Open(path, mode)` | Opens a file to work on it piece by piece: gives a FILE, or null. Version 2. |
| `File.Move(from, to)` | Renames the file. False when refused. Version 2. |

Exists, Read and TryRead only look at a file. Write, Append, Delete, MkDir and Move change the disk, and each of them answers true when the change happened.

## A missing file

File.Read stops the program when the file is missing or cannot be read. The console then shows the file's message, such as "<path>: file not found". Use File.TryRead when you want to ask first and carry on either way.

## Files you open: FILE

A FILE is a file the program has opened. It came in version 2, and all its members are version 2 too. The program works on its own copy of the text, and nothing reaches the disk until you call Close, or until the program ends with the file still open.

Table: FILE members

| Member | What it does |
| --- | --- |
| `FILE.Close()` | Writes the text to the disk, if it changed, and closes the file. |
| `FILE.ReadLine(out line)` | Gives the next line. False at the end, with an empty line. |
| `FILE.Read()` | The next character as a number, or -1 at the end. |
| `FILE.Scan(out value)` | Reads one value of the variable's type. |
| `FILE.Write(text)` | Writes the text into the file. |
| `FILE.Write(char)` | Writes one character. |
| `FILE.Seek(place)` | Moves to a place, counted in characters from the start and kept between 0 and the length of the file. |
| `FILE.Position` | The place now, in characters from the start. |
| `FILE.AtEnd` | Yes when the place is at the end of the file. |

## Modes

The mode says how a file is opened. The letters b and t are ignored, so rb means r.

Table: Modes of File.Open

| Mode | What it does |
| --- | --- |
| `r` | Reads a file that exists. Gives null when it is missing. |
| `w` | Writes a file, and starts it empty. |
| `a` | Adds text at the end of the file. |
| `r+` | Reads and writes a file that exists. |
| `w+` | Writes a file that starts empty, and reads it too. |
| `a+` | Adds at the end, and reads too. |

> [!WARNING] Opening a file with w starts it empty, so the text it held is replaced by what the program writes.

> [!DEFINE] Mode
> The way a file is opened: reading, writing, adding at the end, or both with a plus sign.

## If something goes wrong

> [!PROBLEM] The program stops and says that the file was not found.
> Check the path, or use File.TryRead to ask first without stopping.

> [!PROBLEM] Write or Append gives false, and the file's message says there is not enough free space on the disk.
> Free some space on the disk, or write less text.

> [!PROBLEM] Write gives false, and the message says the file is read-only.
> Choose another path. A file ending in .dat is read-only, and the message names the Network Interactor for items.

> [!PROBLEM] Write gives false, and the message says the file name is not valid for this filesystem.
> Use a name that this drive accepts.

> [!PROBLEM] Delete gives false for a .dat file.
> Use the Network Interactor, as the message says. .dat files cannot be deleted from here.

> [!PROBLEM] Move gives false, and the message says a file of that name is already there.
> Choose a new name, or delete the file that is there first.

> [!PROBLEM] Move gives false: a file on another machine is copied, not moved.
> Read the file with File.Read, write it with File.Write, then delete the original with File.Delete.

> [!PROBLEM] MkDir gives false: the drive has no folders.
> Make the folder on a drive that supports folders.

> [!PROBLEM] Open gives null.
> Check that the mode is one listed above, that the path is not blank, and that a file read in mode r exists. A program may also hold only so many files at once: 8 on a Vintage machine, 20 on a Legacy machine and 64 on every other era. Close a file you no longer need.

> [!PROBLEM] The program stops and says that the file was opened with "r", which does not write.
> Open the file in a mode that writes, such as w, a or r+.

> [!PROBLEM] The program stops and says that the file was opened with "w", which does not read.
> Open the file in a mode that reads, such as r, r+ or a+.

> [!PROBLEM] The program stops and says that the file is closed.
> Open the file again with File.Open before you use it.

> [!SEE] jsc:disks, jsc:drives
