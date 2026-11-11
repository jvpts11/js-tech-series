---
section: installing_linux
order: 288
---
# Installing Fedora

Fedora has its own installer, titled "Fedora installer". It does not go page by page. It shows a hub: a list of every question, each marked as given or as still wanted. You answer the questions one by one, and return to the hub after each.

Open a question with its digit, from 1 to 9, or move the cursor with UP and DOWN and press ENTER. When the hub lists nothing as wanted, press B to begin the installation. The hub's foot says so: "A letter begins the installation once nothing is still wanted."

Table: The questions of the Fedora installer

| Page | Heading on the page | What you do |
| --- | --- | --- |
| Hub | Installation | Lists every question. Open one, answer it, and come back. |
| Disk | Installation Destination | Choose the disk, then press ENTER. |
| Desktop | Software selection | Choose a desktop or none. Shown only when a Mirror answers. |
| Name | Host name | Type the computer's name, then press ENTER. |
| Copy | Progress | Wait for the steps, from the environment to the host name. |
| Done | Complete! | Take the install medium out, then press ENTER to reboot. |

The hint at the foot of the hub reads "Please make a selection from the above ['b' to begin installation, 'q' to quit, 'r' to refresh]:". The 'q' and 'r' do nothing here. To quit, press F3 before anything is written to a disk.

1. Boot the computer from the Fedora install medium.
2. Press 1 to open the first question.
3. Answer it, then press ENTER.
4. Open every question the hub still lists as wanted, and answer it.
5. Press B to begin the installation.

## If something goes wrong

> [!PROBLEM] The hub still lists a question as wanted.
> Open that question with its digit, answer it, and then press B.

> [!PROBLEM] The hint says 'q' to quit, but pressing q does nothing.
> Press F3 to quit. It works only before anything is written to a disk.

> [!SEE] jsc:installing_linux_keys, jsc:dnf
