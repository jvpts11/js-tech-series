---
section: sharing_knot
order: 500
---
# KnotHub

KnotHub is the service that keeps the repository: every file you pushed with Knot and every revision of each one. It is installed on a Server, one of the computers in a Server Rack ([](jsc:server_racks)). It does not run on the Mainframe or on a desktop.

Each Server keeps its own repository, and the source is saved with that Server item. If you take the Server out of the rack, its history goes with it. Knot reads the repository of the first running KnotHub it finds on the network, in the order the network lists its Servers.

A KnotHub on a Server that is switched off does not answer. The window then shows no revisions, and its status bar reads no service.

> [!WARNING] Uninstalling KnotHub deletes all its source: every file and every revision. Its numbering starts again at revision 1.

## If something goes wrong

> [!PROBLEM] The window says No KnotHub on this network, and it shows no revisions.
> Put a Server with KnotHub installed in a Server Rack, and switch the Server on ([](jsc:server_racks)).

> [!PROBLEM] The status bar reads no service.
> The same fix applies: a running KnotHub must answer on the network.

> [!PROBLEM] Push or Pull says No System Disk.
> Put a system disk in the computer ([](jsc:knot)).

> [!PROBLEM] Pull says No such revision.
> The revision was dropped, or the KnotHub was reset. Pick another revision from the history.

> [!SEE] jsc:knot, jsc:server_racks
