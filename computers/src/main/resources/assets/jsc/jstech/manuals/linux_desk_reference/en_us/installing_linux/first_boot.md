---
section: installing_linux
order: 290
---
# The first boot

When the installer has finished and the computer has rebooted, the installed system starts from its own disk. You see a list of lines as the kernel and the services start, then a login line on the text console, the first screen of the machine.

```text
{"text": "The boot begins with \"Loading Linux 6.8-jsc ...\" and \"Loading initial ramdisk ...\". The lines that begin with \"[  OK  ]\" report that a service started. The system is run by systemd."}
```

> [!DEFINE] Systemd
> The service manager of these systems. It starts the services at boot, in order.

```table
{"caption": "Lines at the start", "heads": ["Line", "When it shows"], "rows": [[{"literal": "Loading Linux 6.8-jsc ..."}, {"words": "Always, first."}], [{"literal": "Linux version 6.8-jsc (x86_64)"}, {"words": "Always. On a 32-bit machine it reads i686."}], [{"literal": "CPU: <processor model>"}, {"words": "Always: the central processing unit (CPU) of the machine."}], [{"literal": "Memory: <MB> MB available"}, {"words": "Always: the memory of the machine."}], [{"literal": "sda: <disk name>"}, {"words": "One line for each disk, with the letters sda, sdb and on."}], [{"literal": "[  OK  ] Started Journal Service."}, {"words": "Always."}], [{"literal": "[  OK  ] Reached target Local File Systems."}, {"words": "Always."}], [{"literal": "[  OK  ] Reached target Network is Online."}, {"words": "Only when a data cable joins the computer to a Mainframe."}], [{"literal": "[  OK  ] Found package mirror on <Mainframe name>."}, {"words": "Only when that Mainframe runs the Mirror."}], [{"literal": "Starting kde plasma Display Manager..."}, {"words": "Only when a desktop is installed, named in lower case: kde plasma, gnome or cinnamon."}]]}
```

Without a desktop there is no display manager line. The boot ends at the text console: a banner, a login and a shell, and nothing else.

> [!DEFINE] Display manager
> The program that shows the graphical login and starts the desktop. Here it starts only when a desktop is installed.

The text console prints a greeting, then waits at its login line. Here is the greeting on Ubuntu. Debian and Fedora print the same lines with their own name.

```session
{"lines": [{"text": "Ubuntu <hostname> tty1"}, {"text": ""}, {"text": "<hostname> login: player"}, {"text": "Password:"}, {"text": "Welcome to Ubuntu (Linux 6.8-jsc x86_64)"}, {"text": ""}], "look": "jsc:guide/terminal/bash", "title": "Ubuntu"}
```

The login is a line, and nothing checks the password you are asked for, because nothing in this world logs in with a password yet. You are always the user player.

> [!DEFINE] Login
> The step where the system asks who is using the computer, before it starts a session for that user.

## If something goes wrong

> [!PROBLEM] The system starts at a shell, with no desktop.
> Install a desktop with apt install gnome on Ubuntu or Debian, or dnf install gnome on Fedora, then reboot. Cinnamon needs Standard hardware.

> [!PROBLEM] The boot shows no "Found package mirror" line.
> The computer is not on a network whose Mainframe runs the Mirror. Connect its data cable to that Mainframe ([](jsc:data_cables)).

> [!SEE] jsc:apt, jsc:dnf, jsc:mirror_and_packages
