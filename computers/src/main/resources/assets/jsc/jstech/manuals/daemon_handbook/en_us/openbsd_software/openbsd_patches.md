---
section: openbsd_software
order: 346
---
# syspatch, the system's own patches

OpenBSD stays at version 7.5: what changes is its patches, which syspatch fetches for you from the network's Mirror and installs. The release has two, 001_ksh and 002_sshd.

Table: What syspatch does

| Command | What it does |
| --- | --- |
| `syspatch -c` | Lists the patches not installed yet. |
| `syspatch` | Installs them, and links the kernel anew. |
| `syspatch -l` | Lists the patches installed. |
| `syspatch -r` | Takes the last patch installed back off. |

```command
id: jsc:command/openbsd/syspatch
```

## If something goes wrong

> [!PROBLEM] syspatch says there is no route to the Mirror.
> The computer is not on a network whose Mainframe runs the Mirror ([](jsc:openbsd_packages)).

> [!SEE] jsc:openbsd_packages
