---
section: fab
order: 45
icon: jsc:prebuilder_robot_arm
covers: [jsc:prebuilder_case_lift, jsc:prebuilder_robot_arm, jsc:prebuilder_parts_tray, jsc:preinstaller_cradle, jsc:preinstaller_console, jsc:preinstaller_disk_dock, jsc:preinstaller_board_fixture]
---
# The Prebuilder and the Preinstaller

These two machines make whole computers, ready to place and switch on, for you or for the network's autocrafting: the Prebuilder puts a computer together from its parts, and the Preinstaller writes what it runs.

Give the Prebuilder one empty case and the parts that go in it, in any order: the case of a one-block computer, or a rack server. It puts every part in its seat by the same rules as the assembly screen ([](jsc:assembly_screen)), the board first, and gives you the computer with every part inside. When a part does not fit, it builds nothing and says why, on its terminal's screen and in its window.

A motherboard can come with no firmware on it, and a computer built on one only turns its fans. The Preinstaller writes the firmware of the board's era, then a system and its programs onto the system disk, as an installer would ([](jsc:installing)). You lay the install media among its inputs: it reads them and leaves them there, never used up.

It takes one thing to write onto at a time: a computer with its parts, a loose motherboard, a loose disk, or a rack server. A loose disk is how you give a rack server's bay drives their system. A job takes the machine's step and a second more for every 100 MB it writes.

```table
{"caption": "Each machine's size, its step and its draw", "heads": ["Machine", "Size", "Step", "Draw"], "rows": [[{"words": "Prebuilder"}, {"words": "3 × 3 × 2"}, {"amount": 10, "unit": "s"}, {"amount": 1000, "unit": "W"}], [{"words": "Preinstaller"}, {"words": "2 × 2 × 2"}, {"amount": 10, "unit": "s"}, {"amount": 1000, "unit": "W"}]]}
```

## If something goes wrong

> [!PROBLEM] The Prebuilder builds nothing and names a part.
> That part has no seat left in the case, or the case's rules refuse it. Take it out, or give a case and a board that take it.

> [!PROBLEM] The Preinstaller says there is nothing to write.
> The board's firmware is written already, and no medium brings a system or a program the computer lacks.

> [!PROBLEM] The Preinstaller refuses a system.
> The system needs a computer of a later era, another processor, more room on the disk, or every one of its install disks among the inputs.

> [!SEE] jsc:building_chain_machines, jsc:firmware, jsc:crafting_interfaces
