---
section: peripherals
order: 67
icon: jsc:monitor
covers_family: jsc:monitors
---
# Monitors

The monitor is the screen of a computer. Everything you do with its programs, you do at the monitor: the computer is the case, and the monitor is where you sit. A monitor is a peripheral, and you join it to its computer with a peripheral cable, or you place it right against the computer.

> [!DEFINE] Peripheral
> A device, such as a monitor, a drive or a printer, that a computer reaches through its own cable.

> [!DEFINE] Peripheral Cable
> The cable that joins a computer to its devices. It carries no network: the network has its own data cables.

Ten monitors come in five eras. The Vintage has four: the Mono I with paper-white, the Mono II with green, the Amber, and the CGA with sixteen colours. The Legacy is the first full colour tube. The Transition, Standard and Advanced monitors are flat colour panels, and the Standard and the Advanced also come as touch monitors.

Table: The monitors

| Monitor | Era | Picture | Joins others |
| --- | --- | --- | --- |
| Mono I Monitor | Vintage | Paper-white, every system in white | No |
| Mono II Monitor | Vintage | Green | No |
| Amber Monitor | Vintage | Amber | No |
| CGA Monitor | Vintage | Sixteen colours | No |
| Legacy Monitor | Legacy | Colour picture tube | No |
| Transition Monitor | Transition | Colour flat panel | Yes |
| Monitor | Standard | Colour flat panel | Yes |
| Color Monitor | Advanced | Colour flat panel | Yes |
| Touch Monitor | Standard | Colour flat panel, touch screen | Yes, touch monitors |
| Advanced Touch Monitor | Advanced | Colour flat panel, touch screen | Yes, touch monitors |

> [!DEFINE] Flat panel
> A monitor that can join others into one big screen: the Transition, Standard and Advanced monitors.

A flat panel joins its neighbours into one big screen. Put monitors of the same kind side by side, touching, in one wall and facing the same way, and they become one screen. A gap, or a step in the wall, leaves them as separate monitors. The game's tooltip reads "Joins the same monitors beside it into one screen, up to 8 wide and 6 tall", so one screen holds 48 monitors at most. Vintage and Legacy monitors do not join.

```scene
{"balloons": [{"detail": "Monitors of one kind, touching and facing the same way, make one screen.", "number": 1, "part": {"x": 15, "y": 0, "z": 14}, "text": "A flat Monitor"}, {"detail": "Click it to switch the whole screen on or off.", "number": 2, "part": {"box": [13, 1, 15, 15, 3, 16], "x": 16, "y": 0, "z": 14}, "text": "Power button"}], "blocks": [{"state": "jsc:monitor[facing=north,button=false,lit=true]", "x": 14, "y": 0, "z": 14}, {"state": "jsc:monitor[facing=north,button=false,lit=true]", "x": 15, "y": 0, "z": 14}, {"state": "jsc:monitor[facing=north,button=true,lit=true]", "x": 16, "y": 0, "z": 14}], "caption": "Three flat Monitors side by side in one wall, switched on. Only the corner monitor shows its power button. Drag the scene to see the back.", "height": 110}
```

Each monitor takes one video output of its computer. A video output is a connector on a graphics card, or on a board, and you can use it for one monitor only ([](jsc:graphics_cards)).

> [!DEFINE] Video output
> A connector on a graphics card, or on a board, that drives one monitor.

A lit monitor also takes video memory, one block for each monitor of its screen. The bigger the era, the more memory a block takes:

```table
{"caption": "Video memory per block", "heads": ["Monitor", "Memory per block"], "rows": [[{"words": "Vintage, monochrome"}, {"amount": 64, "unit": "KB"}], [{"words": "Vintage, sixteen colours"}, {"amount": 256, "unit": "KB"}], [{"words": "Legacy"}, {"amount": 4, "unit": "MB"}], [{"words": "Transition"}, {"amount": 16, "unit": "MB"}], [{"words": "Standard"}, {"amount": 64, "unit": "MB"}], [{"words": "Advanced"}, {"amount": 256, "unit": "MB"}]]}
```

The computer gives its video memory first to its graphics windows, then to the monitors in the order you linked them. A monitor that does not fit stays dark.

Every monitor has a power button on its bezel. Click the button to switch that monitor on or off. It switches the monitor alone: the computer behind it keeps running, and you switch the computer at its case. On a big screen, the button in the corner switches the whole screen. Every monitor starts switched on.

From the Legacy on, a monitor that is switched on with no picture sleeps, and its lamp glows amber. A Vintage monitor keeps its lamp lit. A switched-off monitor opens nothing: a click on its face says "The monitor is off".

The monitor shows what its computer is doing at each stage: the self-test when the computer powers up, the Boot Menu, the Setup, the installer and the progress of an install. On a rack with a [KVM Switch](jsc:kvm_switch), it also shows the picker of the channels.

One player at a time sits at a monitor. If someone else is at it, you are told that another player is using the monitor.

> [!DEFINE] KVM
> Keyboard, video and mouse: one monitor and keyboard that reach several machines.

A picture tube, in the Vintage or the Legacy, makes a sound when it switches on or off. A flat panel goes dark without a sound.

A touch monitor is used where it stands, without sitting down at it. Its glass is flush with its bezel, and its chin carries the touch mark. Aim at the glass and right click to tap; hold the button and move to drag; hold still for a second for the right button. The first touch wakes it: the computer's screen comes up on the glass, and every touch after that reaches it. Sneak to use the block as usual, and click the bezel to sit down at it.

A touch on something that takes typing, a text field, a document or a prompt, brings up the typing line where the chat's line is. What you type goes to the screen as you type it; Enter sends Enter, and Escape puts the line away. While the line is up, the mouse still aims and the right button still touches. Frames 10 and Frames 11 also put up their touch keyboard on the glass: Frames 10 along the foot of the desktop, Frames 11 floating in the middle. Hide puts it away until the next field you touch. The other systems take the text through the line alone.

Touch monitors join only touch monitors of their own kind, into one big touch screen, one surface.

> [!DEFINE] Touch monitor
> A flat monitor you use by touching its glass in the world: the Touch Monitor of the Standard and the Advanced Touch Monitor.

## If something goes wrong

> [!PROBLEM] No computer found in range over a Peripheral Cable
> Run the peripheral cable all the way to the computer, within the reach of its era ([](jsc:peripheral_cables)), or stand the monitor right against the computer.

> [!PROBLEM] The computer has no video output: install a graphics card
> Put a graphics card in the computer, or use a processor with built-in graphics ([](jsc:graphics_cards)).

> [!PROBLEM] The computer's video outputs are all in use
> Each graphics card has its own number of outputs. Add a card, or take a monitor away.

> [!PROBLEM] The monitor says "Not enough video memory" and names how much it needs.
> Use a graphics card with more video memory, or fewer monitors, or smaller ones in a joined screen.

> [!PROBLEM] No signal - the computer is powered off
> Switch the computer on at its case.

> [!PROBLEM] No signal - the computer is on, but its parts do not make a computer: its assembly screen lists why
> Open the computer and fix what its CHECKS list names ([](jsc:build_refusals)).

> [!PROBLEM] No signal - the computer has disabled this monitor
> Enable the monitor again ([](jsc:disable_device)).

> [!PROBLEM] The monitor is off
> Click the power button on its bezel.

> [!PROBLEM] This rack holds several computers - mount a KVM Switch to pick one
> Mount a KVM Switch in the rack ([](jsc:kvm_switch)).

> [!PROBLEM] The rack holds no computer to show
> Put a computer in the rack.

> [!PROBLEM] No computer linked
> Join the monitor to a computer with a peripheral cable ([](jsc:peripheral_cables)).

> [!PROBLEM] The monitor says that another player is using it.
> The other player keeps the screen until they leave it.

> [!SEE] jsc:peripheral_cables, jsc:graphics_cards, jsc:kvm_switch
