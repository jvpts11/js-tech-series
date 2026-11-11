---
section: openbsd_system
order: 343
---
# Installing OpenBSD, question by question

OpenBSD's installer asks its questions at the terminal, one under the other, and you answer each one by typing and pressing Enter. The answer in square brackets is the one Enter takes when you type nothing.

Put the OpenBSD medium in a drive joined to the computer and start the computer from it. The first question is (I)nstall, (U)pgrade, (A)utoinstall or (S)hell?: type i.

Table: The questions it asks

| Question | What your answer does |
| --- | --- |
| `System hostname?` | Names the computer. |
| `Start sshd(8) by default?` | Lets other computers reach this one, or not. |
| `Do you expect to run the X Window System?` | Says whether a desktop will run here at all. |
| `...started by xenodm(1)?` | Starts the desktop with the computer. No, the default, keeps it at the terminal. |
| `Which disk is the root disk?` | Chooses the disk the system goes on: wd0 for an IDE disk, sd0 for a SATA one. |
| `Use (W)hole disk or (E)dit the MBR?` | w takes the whole disk; e puts OpenBSD in the free room beside what the disk holds. |
| `Location of sets?` | The medium, or http to fetch them from the network's Mirror. |
| `Set name(s)?` | Which sets to install; done starts the copy. |

The network, the password and the time zone are answered for you: the computer has no account to protect yet and the world keeps one clock.

## The sets

The system comes in five sets. Type -man\* to leave the manual out, and the name of a set to put it back; all puts every set back.

Table: The sets

| Set | What it holds | Left out |
| --- | --- | --- |
| `bsd` | The kernel. | Always installed. |
| `bsd.rd` | The kernel that repairs a system. | Saves its room. |
| `base75.tgz` | The base system. | Always installed. |
| `comp75.tgz` | The compiler and its tools. | Saves its room, and make goes with it. |
| `man75.tgz` | The manual's pages. | Saves its room, and man and apropos go with it. |

When the copy ends, answer Exit to (S)hell, (H)alt or (R)eboot? with r, and take the medium out.

## If something goes wrong

> [!PROBLEM] The installer says the disk is too small.
> OpenBSD needs 1,024 MB. Choose a bigger disk, or answer w to take the whole disk.

> [!PROBLEM] The installer says it is not a valid choice.
> Type the disk's name as the question lists it, such as wd0.

> [!SEE] jsc:installing, jsc:openbsd_overview
