---
section: freebsd_system
order: 333
---
# Installing FreeBSD, page by page

bsdinstall is FreeBSD's own installer. It asks its questions on pages, always in the same order, and then it copies the system onto the disk you choose. This page goes through each page, so you know what it asks before you answer.

> [!DEFINE] bsdinstall
> FreeBSD's own installer, which asks its questions on pages in one fixed order.

You need a computer with a disk and an install medium: the FreeBSD CD (compact disc), put in a CD Drive. A Mirror on the network is optional. With one, the installer also offers desktops and fetches packages from it.

> [!DEFINE] Ports Collection
> The ports tree, the folder of recipes that builds programs from source.

## The pages in order

Table: The pages of bsdinstall

| Page | What you do |
| --- | --- |
| Welcome | Press Install to begin. |
| Set Hostname | Keep the name the field shows, or type another one. |
| Distribution Select | Tick ports to install the Ports Collection. It is ticked by default. |
| Partitioning | Choose the disk and how it is used. |
| Mirror Selection | Choose the Mirror to fetch from, or none to install from the CD only. |
| Archive Extraction | Wait while the system is copied. |
| System Configuration | Tick the services to start at boot. cron is ticked by default, and sshd is not. |
| Desktop | Only when a Mirror answers: choose a desktop, or None. |
| Complete | Remove the CD and reboot. |

The Welcome page has the button Install, and every other page has OK. bsdinstall has no Back button: each page offers only Cancel and its own button, so to change an earlier answer, press Cancel and start the installer again.

## Choosing the disk

The Partitioning page lists one row per disk. The disks are named ada0, ada1 and so on, in the order of the drives. Each row offers a way to use that disk. Auto (UFS) puts the UNIX File System (UFS) on the whole disk.

Table: The choices for each disk

| Choice | When you see it |
| --- | --- |
| Auto (UFS): use the whole of the disk | The disk is empty. |
| Auto (UFS): use the whole of the disk, erasing what is on it | The disk holds a system or files. Its files are erased. |
| Beside: share the disk with its system | The disk holds a system and has room left beside it. |

> [!DEFINE] UFS
> The disk filesystem that bsdinstall lays down on the whole disk.

Before the installer erases a disk, it asks "Erase ada0?" and names what it erases. The dialog says "This cannot be undone." Press Y to erase, or N to cancel.

> [!WARNING] Erasing a disk deletes every file on it, and this cannot be undone.

## The services and the Mirror

The Mirror Selection page asks where pkg and ports fetch from. The row mirror://mainframe appears only when a Mirror answers ([](jsc:mirror)). Otherwise you install from the CD.

When the copy is done, your answers become settings. Ticking cron turns cron on, and ticking sshd turns remote on. Both are the settings you can change later ([](jsc:rc_services)). With the ports box ticked and a Mirror in use, the whole ports tree is laid onto the disk now. Without a Mirror, the installer leaves an empty /usr/ports folder for portsnap to fill.

At the end, the computer restarts into the new system once you take the CD out.

> [!DEFINE] Distribution set
> A compressed bundle of system files, such as base.txz or kernel.txz.

## If something goes wrong

> [!PROBLEM] The Desktop page has no desktop in its list.
> No Mirror answered, or this computer's era is older than the desktop's minimum. Connect to a network with a Mirror, or use a newer computer.

> [!PROBLEM] The Mirror Selection page has no mirror:// row.
> No Mainframe on this network runs the Mirror service. A Mainframe has to run it ([](jsc:mirror)).

> [!PROBLEM] You want to change an answer, and there is no Back button.
> Press Cancel, then start the installer again.

> [!SEE] jsc:disks, jsc:installing, jsc:rc_services, jsc:mirror
