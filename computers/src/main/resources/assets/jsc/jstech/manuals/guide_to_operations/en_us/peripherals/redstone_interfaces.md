---
section: peripherals
order: 76
icon: jsc:standard_redstone_interface
covers_family: jsc:redstone_interfaces
---
# Redstone Interfaces

A Redstone Interface is a small block that lets a program read the redstone signal at one of its faces, or emit a signal from that face. Redstone is the wiring of Minecraft. A computer reaches the interface through a peripheral cable, as it reaches its other devices ([](jsc:peripheral_cables)).

```scene
{"balloons": [{"detail": "It reads or emits the redstone signal at the face it looks at.", "number": 1, "part": {"box": [5, 1, 15, 11, 7, 16], "x": 12, "y": 0, "z": 11}, "text": "The lens"}, {"number": 2, "part": {"x": 12, "y": 0, "z": 11}, "text": "The Redstone Interface"}, {"detail": "It lights while the interface emits a signal.", "number": 3, "part": {"x": 12, "y": 0, "z": 12}, "text": "A redstone lamp"}, {"detail": "It plugs into the back of the interface.", "number": 4, "part": {"x": 12, "y": 0, "z": 10}, "text": "The Peripheral Cable"}, {"number": 5, "part": {"x": 14, "y": 0, "z": 10}, "text": "The computer"}], "blocks": [{"state": "jsc:personal_computer[facing=south]", "x": 14, "y": 0, "z": 10}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,8240,0,0,0,0,0]}", "state": "jscore:cable", "x": 13, "y": 0, "z": 10}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,2088,0,0,0,0,0]}", "state": "jscore:cable", "x": 12, "y": 0, "z": 10}, {"data": "{Emits:1b,Strength:15,LinkedOwner:3848290738176L}", "state": "jsc:standard_redstone_interface[facing=south]", "x": 12, "y": 0, "z": 11}, {"state": "minecraft:redstone_lamp[lit=true]", "x": 12, "y": 0, "z": 12}], "caption": "A Redstone Interface, its lens toward a redstone lamp that it lights, cabled to its computer. Drag the scene to see the back.", "height": 110}
```

> [!DEFINE] Lens
> The face of the block that reads or emits a signal. It is the face you look at when you place the block.

Place it the way you place an observer. Its lens points toward what you look at. Its back, where the cable plugs in, can face any way. Only the lens reads or emits.

The interface has two modes. Its signal strength runs from 0 to 15 in either mode.

> [!DEFINE] Signal strength
> How strong a redstone signal is, from 0 to 15.

Table: The two modes

| Mode | What it does | What the window shows |
| --- | --- | --- |
| IN | Reads the signal that comes into its lens. | Reads, with the strength |
| OUT | Emits a signal from its lens. | Emits, with the strength |

A program sets the mode and the strength. The window says which program set them last.

Name the interface in its window, with up to 32 characters. Programs find an interface by that name, and no two interfaces of one computer can share a name.

The interface has no power of its own. With no computer at the other end of its cable, its window says "Off, no power", and it reads and emits nothing. It keeps its mode and its strength until a computer is linked to it again.

## The window

Table: Part of the window

| Part | What it shows |
| --- | --- |
| NAME | The name of the interface. Programs find it by this name. |
| SIGNAL | Whether the interface reads or emits, with the strength. |
| STRENGTH | The signal strength, from 0 to 15. |
| MODE | IN or OUT. |
| SOFTWARE | The program that last set the mode and the strength, after "Set by". |

## If something goes wrong

> [!PROBLEM] It reads and emits nothing.
> Its window says "Off, no power". Join the interface to a computer with a peripheral cable ([](jsc:peripheral_cables)).

> [!PROBLEM] The name is refused: "Another interface already has this name".
> Pick another name for this interface, in its window.

> [!PROBLEM] A program cannot find the interface.
> Check the name in its window. Programs find an interface only by its name.

> [!SEE] jsc:redstone_programs, jsc:redstone_library
