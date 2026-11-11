---
section: installing_linux
order: 286
---
# Installing Ubuntu

Ubuntu has its own installer. You start it from its install medium, the disc or stick a system is installed from, after you boot the computer from that drive ([](jsc:firmware)). The installer opens in a window titled "Ubuntu installer", with a band across the top that holds the page's heading and the words "[ Help ]".

> [!DEFINE] Install medium
> The disc or stick a system is installed from. The installer is the program on it.

The installer goes through its pages in order, and ENTER goes on from each page. The desktop page appears only when a Mirror answers.

Table: The pages of the Ubuntu installer

| Page | Heading on the page | What you do |
| --- | --- | --- |
| Disk | Guided storage configuration | Choose the disk with UP and DOWN, then press ENTER. |
| Name | Profile setup | Type the computer's name, then press ENTER. |
| Desktop | Desktop environment | Choose a desktop or none, then press ENTER. |
| Copy | Installing system | Wait while the system is copied. The page shows each step. |
| Done | Installation complete | Take the install medium out, then press ENTER to reboot. |

```text
{"text": "The foot of the screen shows the buttons \"[ Done ]  [ Back ]\" on the pages before the copy, and \"[ Reboot Now ]\" on the copy and done pages. The installers have no key that goes back one page, so check each page before you press ENTER."}
```

1. Boot the computer from the install medium.
2. Choose the disk on the Disk page, then press ENTER.
3. Type the computer's name on the Name page, then press ENTER.
4. On the Desktop page, choose a desktop or none, then press ENTER.
5. Wait until the copy is finished.
6. Take the install medium out, then press ENTER to reboot.

## If something goes wrong

> [!PROBLEM] The screen says "No Mirror answers, so it comes up at its terminal."
> The desktop page is skipped, and the system starts without a desktop. Connect the computer to a network whose Mainframe runs the Mirror, then add a desktop after the first boot ([](jsc:apt)).

> [!SEE] jsc:installing_linux_keys, jsc:first_boot
