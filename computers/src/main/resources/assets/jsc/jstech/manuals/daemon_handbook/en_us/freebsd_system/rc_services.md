---
section: freebsd_system
order: 335
---
# Services at boot: cron and sshd

A service is a program that runs in the background, without a window of its own. FreeBSD asks about two of them, at the installer and at each boot: cron, which runs scheduled jobs, and sshd, which lets other computers reach this one.

> [!DEFINE] Service
> A program that runs in the background, such as cron for scheduled jobs or sshd for remote login.

> [!DEFINE] rc
> The startup scripts of FreeBSD. They read the settings to decide which services start.

Table: The two services

| Service | Setting | What it does |
| --- | --- | --- |
| `cron` | `cron` | Runs scheduled jobs. On by default. |
| `sshd` | `remote` | Lets other computers reach this one. |

The sshd service and the remote setting are one setting. Turning remote on starts sshd at boot, and the sshd box of the installer sets the same setting. The box is not ticked by default, so a computer installed by bsdinstall starts with remote off unless you tick it.

A computer that was not installed by bsdinstall starts with remote on.

## Changing the services with config

You change both settings with the config command. The values are on and off, and true and false also work.

1. Type config to list the settings.
2. Type config cron on to turn cron on.
3. Type config cron off to turn cron off.
4. Type config remote on to let other computers reach this one.
5. Type config remote off to refuse the remote calls of other computers.

A successful change answers with the name of the setting and the word set, such as cron set. Typing config alone lists both settings, one line for each.

```table
{"caption": "The lines of config", "heads": ["Line", "What it means"], "rows": [[{"literal": "remote  on (other computers may run programs here)"}, {"words": "The remote setting is on."}], [{"literal": "cron  on"}, {"words": "The cron setting, shown as on or as off."}]]}
```

```text
{"text": "Without a value, config cron answers \"usage: config <key> <value>  (or 'config' to list)\". A value that is not on or off is refused with unknown setting: cron. The key is known, so the value is the part that is wrong."}
```

## The rc.conf file

The rc scripts read /etc/rc.conf at boot. On FreeBSD that file is generated from your settings, not kept on the disk, so change the settings with config. It holds these lines:

```conf rc.conf
hostname="<hostname>"
ifconfig_em0="DHCP"
cron_enable="YES"
sshd_enable="NO"
dumpdev="AUTO"
```

The ifconfig_em0 line appears only when the computer is networked. The cron and sshd lines read YES or NO from the two settings.

## What a service change does

With cron off, scheduled jobs wait instead of running. A job you put in the background with & keeps running. With remote off, the remote calls of other computers to this one are refused.

## If something goes wrong

> [!PROBLEM] sshd did not start after the installation.
> The sshd box was left unticked. Type config remote on.

> [!PROBLEM] Scheduled jobs do not run.
> The cron setting is off. Type config cron on.

> [!PROBLEM] Other computers cannot run programs here.
> The remote setting is off. Type config remote on.

> [!PROBLEM] The computer answers unknown setting: cron.
> The value was not on or off. Use config cron on or config cron off.

> [!SEE] jsc:processes_and_jobs, jsc:system_commands, jsc:bsdinstall
