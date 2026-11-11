---
section: by_hand_basics
order: 300
---
# What you need

Four things must be ready before you start. Each one has a refusal of its own, so the machine tells you which one is missing, and the table shows where each one comes from.

> [!DEFINE] Firmware
> The program on the motherboard that starts the computer, before any system runs.

> [!DEFINE] Era
> A generation of hardware, such as Vintage, Legacy or Standard. A computer has the era of its motherboard.

> [!DEFINE] Mainframe
> The computer that runs a network, which is every computer and device joined to it by data cable. It hosts the network's services, such as the Mirror.

First, the computer must be of Legacy era or newer. The era is the era of the motherboard in the computer, not of the parts you put on it. A Vintage computer cannot install either system.

Table: What you need

| Thing | Where it comes from | If it is missing |
| --- | --- | --- |
| A Legacy-era or newer motherboard | The motherboard in the computer. | The firmware refuses: "Arch Linux needs Legacy era hardware or newer; this machine is Vintage era." |
| A disk in a disk slot | The computer's disk slots, named sda, sdb and so on in slot order. | `fdisk: cannot open /dev/sdb: No such file or directory` |
| The Arch Linux or Gentoo CD in a CD Drive joined to the computer | The CD is in the Legacy creative tab, J's Computers - Legacy. | The firmware says: "No installation medium is in a drive linked to this machine." |
| The Mirror, running on the network's Mainframe | Installed on the network with the command mirror install. | The package steps fail, because the Mirror does not answer. |

> [!DEFINE] Mirror
> The network's package repository, a service on the Mainframe. Every package manager of the network installs from it.

The package steps fetch from the Mirror, so they need it. It answers only while the Mainframe is running ([](jsc:mainframes)). Type mirror on its own to see how it stands:

Table: How the Mirror stands

| What it says | What it means |
| --- | --- |
| `Mirror: not installed` | Install it on the network with the command mirror install. |
| `Mirror: installed (Mainframe off)` | It is installed, but the Mainframe is not running, so nothing answers. |
| `Mirror: serving` | The Mirror answers, and the package steps can fetch. |

Last, the machine must start from the medium. In the firmware's list, choose the CD. The machine restarts, runs its self-test again, and then the medium's shell comes up ([](jsc:firmware_for_install)).

## If something goes wrong

> [!PROBLEM] The firmware's list shows "Legacy era or newer" beside the medium.
> This machine is older than the medium needs. Put a Legacy-era or newer motherboard in the computer.

> [!PROBLEM] The firmware says there is no installation medium in a linked drive.
> Put the CD in a CD Drive and join that drive to the computer ([](jsc:drives)).

> [!PROBLEM] The shell says the disk does not exist.
> Put a disk in a disk slot. Then type lsblk to list the disks the shell can see ([](jsc:disks)).

> [!PROBLEM] The package steps fail, and the Mirror does not answer.
> Type mirror to see how it stands. Install it with mirror install if it is not installed, and keep the Mainframe running.

> [!SEE] jsc:disks, jsc:drives, jsc:mainframes
