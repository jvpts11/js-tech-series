---
section: gentoo_install
order: 318
icon: jsc:usb_flash_drive
---
# How Gentoo is installed

Gentoo has no installer that does the work for you. Its installation medium gives you a shell and a walkthrough, and you type every step yourself. Each tool you run is the real one. The general steps for every system are in [Installing a system](jsc:installing).

The system you end up with is built on your machine from source code. The kernel and every program you add are compiled from their source first, so the install takes a long time. You decide how many compiles run at once.

> [!DEFINE] compiling
> Turning source code, the text a programmer writes, into a program the computer can run.

> [!DEFINE] kernel
> The core program of the operating system. It drives the hardware and runs the other programs.

Gentoo is a Linux system of the Legacy era, and it takes 4,096 MB of disk once installed. Its package manager is Portage, which installs programs by compiling them.

> [!DEFINE] Portage
> Gentoo's package manager. It reads build recipes and compiles each program from its source.

The walkthrough is a file on the medium, /root/install.txt. Read it at any time: type less /root/install.txt and press [[Enter]]. The file is always in English.

Long steps keep the terminal busy until they finish. Press [[Ctrl]]+[[C]] to stop a step that runs too long ([](jsc:medium_shell)).

The walkthrough marks the steps a system cannot boot without with an asterisk. The setting install_by_hand.gentoo_every_step decides which steps you are asked, and it is off by default.

Table: Which steps you are asked

| Setting | What you are asked |
| --- | --- |
| `false` | Only the steps marked with an asterisk (the default). |
| `true` | Every step of the handbook, the optional ones too. |

Every download and every package comes from the [Mirror](jsc:mirror). A [Mainframe](jsc:mainframes) on the same network must run the Mirror, or the fetch steps fail.

The new system is built in /mnt/gentoo, on your disk. The live medium runs in memory, and it forgets every file written outside /mnt/gentoo when it restarts.

## If something goes wrong

> [!PROBLEM] A file you wrote is gone after a restart.
> Only /mnt/gentoo lives on your disk. Write the new system there, not in /root.

> [!PROBLEM] The steps stop, and you cannot tell what to do next.
> Each refusal names the step it is missing. Do that step, then try again.

> [!SEE] jsc:installing, jsc:the_systems
