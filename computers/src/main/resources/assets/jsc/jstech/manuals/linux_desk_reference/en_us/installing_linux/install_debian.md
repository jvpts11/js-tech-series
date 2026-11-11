---
section: installing_linux
order: 287
---
# Installing Debian

Debian has its own installer, titled "Debian installer". You start it from the install medium, as you do for Ubuntu. It is a grey window with a tab on its top edge that carries the page's heading.

The line at the bottom shows the keys to use. Read that line with care: some of the keys it names do nothing here.

The installer goes through its pages in order, and ENTER goes on from each page. The software page appears only when a Mirror answers.

Table: The pages of the Debian installer

| Page | Heading on the page | What you do |
| --- | --- | --- |
| Disk | Partition disks | Choose the disk, then press ENTER. |
| Name | Configure the network | Type the computer's name, then press ENTER. |
| Desktop | Software selection | Choose a desktop or none, then press ENTER. |
| Copy | Installing the base system | Wait for the steps: unpacking, the boot entry and the host name. |
| Done | Finish the installation | Take the install medium out, then press ENTER to reboot. |

The name page has the heading "Configure the network", which is the installer's own text. The field on that page is the computer's name, and the name becomes the host name.

The line at the bottom reads "<Tab> moves; <Space> selects; <Enter> activates buttons". On Debian, Tab and Space do nothing. Use UP and DOWN to move, and ENTER to choose.

1. Boot the computer from the install medium.
2. Choose the disk, then press ENTER.
3. Type the computer's name, then press ENTER.
4. Choose a desktop or none, then press ENTER.
5. Wait until the copy is finished.
6. Take the install medium out, then press ENTER to reboot.

## If something goes wrong

> [!PROBLEM] The software page is missing.
> No Mirror answers. Connect the computer to a network whose Mainframe runs the Mirror, then add a desktop after the first boot with apt install gnome ([](jsc:apt)).

> [!PROBLEM] The line at the bottom says "<Tab> moves", but Tab does nothing.
> Use UP and DOWN to move between the choices, and ENTER to choose.

> [!SEE] jsc:installing_linux_keys, jsc:first_boot
