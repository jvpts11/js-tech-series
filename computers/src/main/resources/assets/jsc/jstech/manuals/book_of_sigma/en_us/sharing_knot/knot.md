---
section: sharing_knot
order: 499
---
# Knot

Knot is a desktop program that keeps the source of a file you are still writing. Each time you push the file, Knot saves a copy as a revision in the network's repository. A revision holds one saved state of the file, with its author, its message and the time it was saved.

Knot then shows you who changed what, line by line. It works only where a KnotHub is running on the network ([](jsc:knothub_service)). Knot is not a package manager: a package is a finished program handed to others, and Knot keeps the source before that.

## The window

Table: Part of the window

| Part | What it does |
| --- | --- |
| Push, Pull and Refresh | Push saves a new revision, Pull writes the picked revision back to disk, and Refresh asks the KnotHub for the state again. |
| File | The full path of the file a Push reads. A Pull does not use it. |
| Message box | The note a push carries, up to 128 characters. |
| History | The newest revisions, each with its number, author and message. It shows 4 rows and scrolls. |
| Comparison | The changes of the picked revision against the one before it: a plus sign for an added line, a minus sign for a removed line, and plain lines that did not change. |
| Status bar | On the left, the last answer, or the KnotHub's host name. On the right, the number of revisions and the size of the repository. |

Click a row of the history to pick that revision. The File field then shows the revision's file, and the comparison shows its changes. The newest revision is picked when the window opens.

## Pushing a file

1. Type the file's full path in the File field.
2. Type a message in the message box, if you want one.
3. Press Push.
4. Read the answer on the status bar, such as Pushed plant.sgs.

Knot reads the file from the system disk of the computer you are at, not from the text in the window. A push with an empty message is saved as "no message". A push of the same text as the newest revision of that file changes nothing, so Knot refuses it.

## Pulling a revision

1. Click the revision in the history.
2. Press Pull.
3. Read the answer, such as Wrote r3 to plant.sgs.

A Pull writes the revision back to the file it belongs to, on the system disk. It does not write to the path in the File field.

Table: Answers on the status bar

| Answer | When you see it |
| --- | --- |
| `No Computer` | The window is not on a computer. |
| `No System Disk` | The computer has no system disk. |
| `No <file> on this machine` | Push cannot find the file on the system disk. |
| `Nothing changed since the last revision` | The file has the same text as its newest revision, or it is a new file and the repository already holds 64 files. |
| `Pushed <file>` | Push saved a revision. |
| `No such revision` | The picked revision is gone from the history, or the KnotHub was reset. |
| `Not enough free space` | Pull could not write: the disk is full. |
| `Could not write <file>` | Pull failed for another reason. |
| `Wrote r<number> to <file>` | Pull wrote the revision. |

## Revisions and the comparison

A revision's number counts up across the whole repository, not file by file, and a number is never reused while the repository lives. The first revision of a file is compared with empty text, so every line of it shows as added.

The comparison runs line by line, in reading order. A line that both versions share is shown as it is. A line only in the older version is removed, and a line only in the newer version is added. It does not search for the smallest set of changes, so it can show more lines than that.

Table: Limits

| Limit | Value |
| --- | --- |
| Revisions kept per file | 64 revisions |
| Files in one repository | 64 files |
| Lines of comparison shown | 128 lines |
| Path in the File field | 160 characters |
| Message | 128 characters |

> [!WARNING] When a file already has 64 revisions, its next push drops the oldest one, and nothing tells you so.

> [!DEFINE] Knot
> The desktop program that saves the revisions of a file and shows what changed between them.

> [!DEFINE] Repository
> The store of every file and every revision that one KnotHub keeps.

> [!DEFINE] Revision
> One saved state of one file in the repository, with its author, its message and the time it was saved.

> [!DEFINE] Diff
> The list of lines that changed between a revision and the revision before it.

## If something goes wrong

> [!PROBLEM] Push says No System Disk.
> Put a system disk in the computer. Knot reads files from it.

> [!PROBLEM] Push says No <file> on this machine.
> Check the path in the File field. Knot reads the file from the system disk, by its full path.

> [!PROBLEM] Push says Nothing changed since the last revision.
> The file has the same text as its newest revision, so nothing new was saved. Change the file and push again. A new file is also refused once the repository holds 64 files, and the window shows the same message for that case.

> [!PROBLEM] Pull says Not enough free space.
> The system disk is full. Free some space on it, then pull again.

> [!PROBLEM] Pull says No such revision.
> The revision was dropped, or the KnotHub was reset. Pick another revision from the history.

> [!PROBLEM] The window says No KnotHub on this network.
> No Server with KnotHub is running. See the KnotHub entry ([](jsc:knothub_service)).

> [!SEE] jsc:knothub_service, jsc:sigma_projects
