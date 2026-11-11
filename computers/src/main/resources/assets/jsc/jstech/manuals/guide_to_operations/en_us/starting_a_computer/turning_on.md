---
section: starting_a_computer
order: 3
icon: jsc:personal_computer
---
# Turning the computer on

Turning a computer on starts a sequence that you can watch on its monitor. It begins with a check of the hardware, and then it looks for a system to start.

The check is the power-on self-test (POST). The firmware ([](jsc:firmware)) checks each part it finds, and lists it on the screen, one line after another.

## Turning it on

1. Press the power button on the front of the case ([](jsc:case_front)).
2. Watch the monitor while the self-test runs.
3. Press [[DEL]] during the self-test to enter the Setup, or [[F12]] to open the Boot Menu.

If you switch the computer off during its self-test, its screen stays up until you press [[Esc]].

Table: The keys while the computer starts

| Key | What it does |
| --- | --- |
| `DEL` | Enters the Setup, while the self-test runs. |
| `F12` | Opens the Boot Menu, when a disk or medium can start the computer. |
| Any key | After a self-test that finds nothing to start, goes to the Setup. |

## What the start-up screen shows

A new line appears about every 0.15 seconds. These are the lines, in order.

```table
{"caption": "The lines of the start-up", "heads": ["Line", "What it tells you"], "rows": [[{"literal": "Processor :"}, {"words": "The processor's name and its cores and clock speed, or \"not detected\". Legacy machines say Main Processor."}], [{"literal": "Board     :"}, {"words": "The motherboard's name. Legacy machines do not show this line."}], [{"literal": "Memory    :"}, {"words": "The memory counted so far, which climbs while it is tested, and then OK."}], [{"literal": "Video     :"}, {"words": "The graphics card's name, or none when there is no card. Legacy machines say Video Adapter."}], [{"literal": "Detecting drives"}, {"words": "The firmware looks for disks and drives, and lists each one it finds."}], [{"literal": "Booting from"}, {"words": "The disk the computer starts from, once it has found a system to start."}]]}
```

The self-test takes between 2 and 12 seconds. The more memory modules and devices it finds, the longer it takes, and a newer era finishes sooner.

After the self-test the system loads, which takes between 2 and 20 seconds. A big system and short memory make it longer. Shutting down takes a third of the boot time, but never less than 1 second or more than 5 seconds.

> [!DEFINE] Power-on self-test (POST)
> The check the firmware runs each time the computer starts. It lists each part it finds, one line at a time.

> [!DEFINE] Setup
> The firmware's own settings screens. Its pages show the disks and the hardware the firmware found.

> [!DEFINE] Boot Menu
> The list the firmware opens with F12 to start a disk or medium, for this start only.

## If something goes wrong

> [!PROBLEM] Nothing starts when you press the power button.
> The computer has no motherboard or no power supply, so it has no build at all. Add them ([](jsc:first_computer)).

> [!PROBLEM] The switch goes on, but the computer does not run.
> The parts do not make a valid build. See what each refusal means in ([](jsc:build_refusals)).

> [!PROBLEM] DEL does nothing.
> DEL works only while the self-test runs. Turn the computer off and on again, and press DEL in time.

> [!PROBLEM] F12 does nothing.
> The Boot Menu opens only when a disk or medium can start the computer ([](jsc:no_bootable_device)).

> [!SEE] jsc:firmware, jsc:setup_utility, jsc:boot_menu
