---
section: network_remote
order: 110
icon: jsc:server
---
# Shared folders

A folder on your computer can be shared with the other computers of the network. A shared folder is seen by its path, which starts with two backslashes, then the computer's name, then the share's name: \\\\computer\\name.

A share takes its name from the last part of its folder's path, and a drive root such as C: is named c. Two folders cannot share one name, and a computer shares at most 16 folders.

Table: The share commands

| Command | What it does |
| --- | --- |
| `config share <folder> [read\|write]` | Shares the folder. Read only, unless you say write. |
| `config unshare <name>` | Closes that share. |
| `config` | Lists the shares, or none when there are none. |
| `config netshare <0-1000>` | Sets the netshare of the system disk, from 0 to 1000 permille. |

Another computer's share is reached through its path. The path below a share is followed on the owner's computer, exactly as it would be at that computer's own prompt.

## If something goes wrong

> [!PROBLEM] The share is refused, with a message that it has no such folder.
> The folder does not exist. Check its name, and share a folder that exists.

> [!PROBLEM] Another folder is already shared under that name.
> Two folders cannot share one name. Unshare the other one with config unshare first.

> [!PROBLEM] This computer already shares 16 folders.
> Unshare one of them with config unshare first.

> [!PROBLEM] A write is refused.
> The share was opened for reading only. Ask its owner to share it with write.
