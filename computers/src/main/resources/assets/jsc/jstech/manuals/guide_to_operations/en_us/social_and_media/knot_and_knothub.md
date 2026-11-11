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

## If something goes wrong

> [!PROBLEM] The window says No KnotHub on this network.
> Install KnotHub on a server in a rack of this network, and let it run.

> [!PROBLEM] Push answers No change in r, followed by a number.
> The file has not changed since its last revision. Change the file on your disk, then push again.

> [!SEE] jsc:knot, jsc:server_racks, jsc:programs
