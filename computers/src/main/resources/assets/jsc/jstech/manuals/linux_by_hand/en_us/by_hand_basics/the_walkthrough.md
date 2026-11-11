---
section: by_hand_basics
order: 302
---
# The walkthrough on the medium

The medium carries its own walkthrough: the steps of the distribution's guide, written out in order, as a plain file. You do not need a guide from outside the game to follow it.

> [!DEFINE] Tool
> A command that takes time and holds the terminal until it ends, such as pacstrap, or emerge on Gentoo.

The walkthrough is kept in /root/install.txt, on the medium and not on your disk, so it is there whatever the disk holds. To find it and read it:

1. Type help and press [[Enter]].
2. Type less /root/install.txt and press [[Enter]].
3. Read the step you are on, then type it at the prompt.
4. Check what the shell answered, and only then type the next step.

The walkthrough lists the commands in order. Some steps carry a mark, and the table shows what each mark means.

Table: Marks in the walkthrough

| Mark | What it means |
| --- | --- |
| `*` | A step the system cannot boot without. |
| `modern firmware only` | A note under a step: the step is for a Standard or later computer. On older firmware it is left out. |

The medium greets you when it starts: "Type 'help' for the installation walkthrough." It logs you in as root on its own, so you type no name and no password.

## What help prints

```session
{"lines": [{"prompt": "root@archiso ~ # ", "text": "help", "typed": true}, {"text": "This is the Arch Linux installation medium."}, {"text": "Nothing here installs itself: the steps are typed by hand, in"}, {"text": "order, and every tool is the real one."}, {"text": ""}, {"text": "The whole walkthrough is in /root/install.txt"}, {"text": "    less /root/install.txt"}, {"text": "A tool that is running can be stopped with CTRL+C."}], "look": "jsc:guide/terminal/zsh", "title": "Arch Linux"}
```

On Gentoo, the same help names Gentoo, and the prompt reads livecd instead of root@archiso.

## The every-step settings

The server can ask for more before a reboot. Each distribution has one setting, off by default, kept in jscomputers-server.toml beside the world. The table lists what each one adds.

Table: The every-step settings

| Setting | Default | When it is on, reboot also asks for |
| --- | --- | --- |
| Every step of Arch | Off | The hardware clock set, and a name for the machine. |
| Every step of Gentoo | Off | The bind mounts, a profile, the @world update, the kernel link and a name for the machine. |

These settings change only what reboot refuses. Every command still answers the way the real tool does, so you type the same commands either way. The basic list that reboot always checks is in [When the reboot refuses](jsc:when_reboot_refuses).

## If something goes wrong

> [!PROBLEM] reboot answers: "The new system will not boot:", with a list under it.
> Each line names what is missing and the step that provides it, in brackets. Do that step, then type reboot again.

> [!PROBLEM] reboot answers: "reboot: you are inside the new system. exit first."
> Type exit to leave the new system, then type reboot.

> [!SEE] jsc:installing, jsc:the_systems
