---
section: installing_linux
order: 289
---
# The installers' keys, disks and desktop choice

The three installers share one set of keys. The entries above say which key each step needs; this entry lists them all, with what each one does.

Table: Keys in the Linux installers

| Key | What it does |
| --- | --- |
| `UP and DOWN` | Move the cursor through the disk list, the desktop list and the Fedora hub. |
| `ENTER` | Goes on from the page. On the done page it reboots. On the Fedora hub it opens the question under the cursor. |
| `1 to 9` | On the Fedora hub, opens the question with that number. |
| `Letters` | On the name page, type the computer's name, up to 256 letters. |
| `BACKSPACE` | Deletes the last letter of the name. |
| `E` | On the disk page, asks to erase the chosen disk. |
| `Y` | In the erase prompt, erases the disk. ENTER erases too; any other key cancels. |
| `B` | On the Fedora hub, begins the installation. |
| `F3` | Quits the installation, but only before anything is written to a disk. |
| `ESC` | Closes the monitor the installer is shown on. The installation keeps running. |

The disk page lists the disks of the machine in four columns: "Disk", "Size", "Free" and "Holds". A disk can take the system only when its free room covers what the system takes. A disk marked for erasing counts as empty, so its whole size counts.

To erase a disk, press E on the disk page. The prompt reads "Erase Disk N?", where N is the disk's number. It says every file on the disk will be deleted and that "This cannot be undone." Below it, "Y = erase" and "N = cancel" show the keys.

> [!WARNING] Erasing deletes every file on the disk, and that cannot be undone.

> [!DEFINE] Erase
> Delete every file on a disk, so that the disk is empty before a system goes on it. An erase cannot be undone.

The name page says "This name identifies the computer at the prompt and on the network." The name becomes the host name and the name shown in the prompt.

The desktop page appears only when a Mirror answers. Its list holds the desktops this machine can run: a Legacy computer sees KDE Plasma and GNOME, and a Standard computer also sees Cinnamon. Choosing "None, the terminal only" leaves the system at its terminal.

The copy page shows the progress as "<n>% complete" and "About <n> seconds left". When the copy ends, the installer says "INSTALLATION COMPLETE" and asks you to "Take the installation medium out before rebooting, or the machine boots the installer again."

## If something goes wrong

> [!PROBLEM] The disk page says "No disk in this machine has room for it."
> Add a bigger disk to the machine, or erase a disk you no longer need with E on the disk page.

> [!PROBLEM] The disk page says "No room here. Erase this disk, or choose another."
> Choose a disk with enough free room, or press E to erase the one you have chosen.

> [!SEE] jsc:install_ubuntu, jsc:install_debian, jsc:install_fedora
