---
section: openbsd_system
order: 344
---
# The boot, and the daemons rcctl switches

OpenBSD starts through its boot prompt, boot>. It waits five seconds without counting, and then starts the system; press any key to stop the wait, and Enter or boot to start. reboot starts the computer over.

> [!DEFINE] Daemon
> A program that runs in the background, without a window of its own.

rcctl is how you switch the daemons. enable and disable say whether a daemon starts with the system; start and stop act on it now. A service you install comes switched off, so you enable it before it runs.

Table: The daemons

| Daemon | What it does |
| --- | --- |
| `cron` | Runs scheduled jobs. On by default. |
| `sshd` | Lets other computers reach this one. |
| `pf, pflogd, syslogd` | The firewall, its log and the system log. They always run. |
| `xenodm` | Starts the desktop, once one is installed. Off by default. |

1. Type rcctl ls on to see what is switched on.
2. Type rcctl enable xenodm to start the desktop with the computer from now on.
3. Type rcctl start xenodm to bring it up now.

cron and sshd are the computer's own switches, so stopping one switches it off for good and starting one switches it on. A daemon switched off starts only with rcctl -f start.

```command
id: jsc:command/openbsd/rcctl
```

## If something goes wrong

> [!PROBLEM] rcctl says the service does not exist.
> Type rcctl ls all to see the daemons the computer has. xenodm is there only with a desktop.

> [!PROBLEM] rcctl says to use -f to force start.
> The daemon is switched off. Type rcctl enable and its name first.

> [!SEE] jsc:openbsd_overview, jsc:processes_and_jobs
