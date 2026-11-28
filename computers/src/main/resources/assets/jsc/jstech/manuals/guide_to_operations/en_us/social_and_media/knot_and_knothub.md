---
section: social_and_media
order: 91
icon: jsc:cd_rom
---
# Knot and KnotHub

Knot keeps the history of a file that the players of a network are still working on. You send the file to the network's repository, and Knot keeps it as a revision: one saved state of the file, numbered r1, r2 and so on.

> [!DEFINE] Revision
> One saved state of a file in Knot, numbered r1, r2 and so on.

> [!DEFINE] Push
> To send the file's current state to the repository as a new revision.

> [!DEFINE] Pull
> To fetch the network's copy of a file.

Knot holds the source that is still being worked on, before it becomes a finished package ([](jsc:programs)). Push reads the file from this computer's own disk, not from text typed in the window, so the network keeps what the machine really holds. The history runs along the top of the window, and the changes sit underneath.

A file keeps at most 64 revisions, and the oldest go first. A repository keeps at most 64 files. Each revision is kept whole, not as a difference from the last one, so the computer that holds the repository needs the disk space for all of them.

KnotHub keeps the repository for the network, and it runs on a server in a rack of the network ([](jsc:server_racks)). The [Book of Sigma](jsc:knot) explains Knot for programs.

A network that has no KnotHub of its own uses one on a network it is federated with. The port of the KnotHub's network decides, as it does for items: to read the repository, the port must let your network see; to push to it, the port must let your network send.

## At the prompt

Knot also works at the command prompt, as git does. A file is kept by its name, without its folders, so another machine pulls it by the same name.

Table: Knot at the prompt

| Command | What it does |
| --- | --- |
| knot push oplog.sgs -m "first version" | Sends the file as a new revision, with a message. |
| knot pull oplog.sgs | Writes the newest revision of the file onto this computer. Give a revision number after it to get an older one. |
| knot log oplog.sgs | Lists the file's revisions, newest first. Without a file, it lists the newest of all. |
| knot status | Names the KnotHub you reached and lists each file with its count of revisions. |

## If something goes wrong

> [!PROBLEM] The window says No KnotHub on this network.
> Install KnotHub on a server in a rack of this network, and let it run.

> [!PROBLEM] Push answers No change in r, followed by a number.
> The file has not changed since its last revision. Change the file on your disk, then push again.

> [!SEE] jsc:knot, jsc:server_racks, jsc:programs
