---
section: gateway
order: 163
icon: jsc:network_gateway
covers_family: jsc:network_gateway
---
# The Network Gateway

Do you play with ComputerCraft too? The Network Gateway is a bridge between its computers and yours. You place it like any block, and it joins two sides: a ComputerCraft wired network on its front, and one of your computers on its back.

The Gateway is placed facing you. Its front joins the ComputerCraft side, and its back has the socket for a [Peripheral Cable](jsc:peripheral_cables). Every computer takes that cable, and a cable of a later era does not fit this socket.

```scene
{"balloons": [{"detail": "Its front joins ComputerCraft and its back takes the cable to your computer.", "number": 1, "part": {"x": 15, "y": 0, "z": 15}, "text": "The Network Gateway"}, {"number": 2, "part": {"x": 15, "y": 0, "z": 16}, "text": "A ComputerCraft computer"}, {"number": 3, "part": {"x": 15, "y": 0, "z": 14}, "text": "The Peripheral Cable"}, {"number": 4, "part": {"x": 13, "y": 0, "z": 13}, "text": "The host"}], "blocks": [{"state": "jsc:personal_computer[facing=south]", "x": 13, "y": 0, "z": 13}, {"data": "{Wires:[{cable:\"jsc:vintage_peripheral_cable\"}],Links:[I;0,0,0,4144,0,0,0,0,0]}", "state": "jscore:cable", "x": 14, "y": 0, "z": 13}, {"data": "{Wires:[{cable:\"jsc:vintage_peripheral_cable\"}],Links:[I;0,0,0,24,0,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 0, "z": 13}, {"data": "{Wires:[{cable:\"jsc:vintage_peripheral_cable\"}],Links:[I;0,0,0,2060,0,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 0, "z": 14}, {"data": "{LinkedOwner:3573412843520L,LinkLength:3}", "state": "jsc:network_gateway[facing=south,lit=true]", "x": 15, "y": 0, "z": 15}, {"state": "computercraft:computer_normal[facing=north]", "x": 15, "y": 0, "z": 16}], "caption": "A Network Gateway cabled to a Personal Computer, with a ComputerCraft computer against its front. The ComputerCraft computer shows only when ComputerCraft is installed. Drag the scene to see the back.", "height": 120, "view": {"yaw": -75.0, "zoom": 1.3}}
```

> [!DEFINE] Wired network
> The cables and wired modems that ComputerCraft computers use to reach each other and the devices around them.

## Linking to a host

The computer a Gateway is linked to is its host. The two link in one of two ways, and the Gateway's screen says which one it found.

Table: How a Gateway links to its host

| Link | What it means |
| --- | --- |
| Peripheral Cable | A cable runs from the Gateway's back to the host, up to 16 blocks long. |
| adjacent | The Gateway stands directly against the host, with no cable. |

When the Gateway is linked, its screen shows its name, the host and the link, such as "cable, 5 blocks" or "adjacent". When it has no host, the screen says it is not linked and asks you to plug the cable in the back.

> [!DEFINE] Host
> The computer that a Network Gateway is linked to through its back socket.

## The buffer

A right-click opens the Gateway's buffer. It has nine item slots under the label "ITEM BUFFER", and the hint "pull lands here, push leaves" tells you what it is for.

Items that a ComputerCraft program pulls from the network arrive in the buffer, and items it pushes leave from there. The buffer's items drop on the ground when you break the block, so empty it first.

Everything else about the Gateway, such as its permissions and its other controls, is set on its host computer.

## Name and light

The first time it links, the Gateway takes the name gateway-1, then gateway-2, and so on. You rename it in the Gateway Manager or with the gateway command ([](jsc:gateway_manager)).

A name is cleaned up as you type it: lower case, letters, digits, underscores and dashes, and any other run of characters becomes one dash. A name holds 24 characters at most, and an empty name goes back to the default.

The status light on the block is lit while the Gateway is linked. Identify makes it blink for 3 seconds, so you can find the block among others.

## If something goes wrong

> [!PROBLEM] The Gateway does nothing.
> It is not linked to a host, or the host's chunk is not loaded, or the host has disabled it. Check the link, keep the host's chunk loaded, and enable the Gateway on the host.

> [!PROBLEM] The ComputerCraft side does nothing.
> CC: Tweaked is not installed. Without it the block still links to its host and holds items; only the ComputerCraft side never comes up.

> [!PROBLEM] The screen says it is not linked and to plug the cable in the back.
> Put a Peripheral Cable on the back socket and run it to the host, or stand the Gateway directly against the host.

> [!PROBLEM] Renaming is refused with "another gateway on the wire is already called <name>".
> Choose a different name. Two gateways on one wire cannot share a name.

> [!SEE] jsc:peripheral_cables, jsc:mainframes
