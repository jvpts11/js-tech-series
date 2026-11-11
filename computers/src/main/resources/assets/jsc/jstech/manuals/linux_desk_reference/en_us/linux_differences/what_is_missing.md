---
section: linux_differences
order: 294
---
# What a Linux here does not have, and why

Many commands you may know from other Linux systems are not in the shell here. The table lists the common ones and what to do instead. The shell still has reboot, which restarts the computer.

Nothing in this world logs in with a password yet, so a step that set one would be setting nothing. Every command runs as the user player, and there is no other user to switch to.

Table: Commands you may expect

| Command | What to do here |
| --- | --- |
| `sudo` | Not needed: install with apt install or dnf install as player. |
| `su, passwd, useradd, usermod, adduser` | There are no other users. Every command runs as player. |
| `chmod, chown` | The shell keeps no file permissions to change. |
| `systemctl` | No service command. A service starts once its program is installed. |
| `journalctl` | Not in the shell. |
| `ip, ifconfig` | Not in the shell. The shell chapter ([](jsc:shell)) lists the commands that do exist. |
| `shutdown` | Not in the shell. Use reboot to restart the computer. |

The package commands need no sudo. apt install and dnf install run directly for player ([](jsc:apt), [](jsc:dnf)).

When a command is missing, the shell says "command not found" with the word you typed, and adds a dim line: "type 'help' to list commands".

## If something goes wrong

> [!PROBLEM] command not found: sudo
> Not needed here. Install with apt install or dnf install instead.

> [!PROBLEM] command not found: systemctl
> There is no service command. A service is installed, and the Mirror is put on the Mainframe with mirror install.

> [!PROBLEM] command not found: chmod
> There are no file permissions to change in this world.

> [!SEE] jsc:apt, jsc:dnf
