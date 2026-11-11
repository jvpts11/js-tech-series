---
section: library_toolkit
order: 464
---
# Path, Directory and watching a file

Sigma# 3 adds what your program needs to find its way around the disks: taking paths apart, the folders, a file's size and when it last changed, and being told when it changes. Path and Directory live in System.IO, beside File.

Table: Path members

| Member | What it gives |
| --- | --- |
| `Path.Combine(folder, name)` | The name inside the folder, with the separator the folder is written with. |
| `Path.FileName(path)` | The name at the end of the path. |
| `Path.Extension(path)` | The name's ending with its dot, such as .log, or empty text. |
| `Path.Folder(path)` | The folder the path is in. |

Table: Directory and File members

| Member | What it does |
| --- | --- |
| `Directory.Exists(path)` | Yes when the folder is there. |
| `Directory.Make(path)` | Makes the folder. |
| `Directory.Delete(path)` | Deletes the folder. |
| `Directory.Files(path)` | The names of the files in it. |
| `Directory.Folders(path)` | The names of the folders in it. |
| `File.Size(path)` | The file's size in bytes, or -1 when it is not there. |
| `File.Changed(path)` | The world tick the file last changed in, which you compare with Time.Tick, or -1 when it is not there. |
| `File.Watch(path, handler)` | Calls the handler with a FileEvent each time the file changes, appears or goes. It gives a Subscription: let go of it to stop. |
| `FileEvent.Path, FileEvent.Changed, FileEvent.Previous` | The path watched, and the tick it changed in now and before, -1 for a file that is not there. |

Size, Changed and Watch read the files on this machine's own drives. A watch costs nothing to set: the machine looks once a tick, as it does for a watch on the network.

> [!DEFINE] Separator
> The character between the folders of a path: a backslash on Frames and MC-DOS, a slash on the Unix systems.

## If something goes wrong

> [!PROBLEM] File.Size gives -1 for a file on another computer's share.
> Size, Changed and Watch read this machine's own drives only. Copy the file here first.

> [!PROBLEM] Directory.Delete gives no.
> Delete the files inside the folder first.

> [!SEE] jsc:files_and_file_handles, jsc:file_tree, jsc:network_watches
