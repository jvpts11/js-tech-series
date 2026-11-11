---
section: peripherals
order: 68
icon: jsc:peripheral_cable
covers_family: jsc:peripheral_cables
---
# Peripheral cables and hubs

A peripheral cable joins a computer to its devices: monitors, speakers, drives, printers and Redstone Interfaces. It is not a data cable, and it carries no network. A device placed right against its computer needs no cable at all.

```scene
{"balloons": [{"number": 1, "part": {"x": 9, "y": 0, "z": 11}, "text": "The computer"}, {"detail": "It joins the computer to its devices and carries no network.", "number": 2, "part": {"x": 10, "y": 0, "z": 11}, "text": "The Peripheral Cable"}, {"detail": "Join speakers to the computer's own cable, before the Hub: they do not pass through a Hub.", "number": 3, "part": {"x": 11, "y": 0, "z": 12}, "text": "A speaker"}, {"detail": "It takes one port of the computer and offers more ports of its own.", "number": 4, "part": {"x": 13, "y": 0, "z": 11}, "text": "The Hub"}, {"number": 5, "part": {"x": 13, "y": 0, "z": 12}, "text": "A Floppy Drive"}, {"detail": "Press the eject button beside a drive to bring its tray out.", "number": 6, "part": {"x": 14, "y": 0, "z": 12}, "text": "A CD Drive"}, {"number": 7, "part": {"x": 15, "y": 0, "z": 12}, "text": "The Printer"}], "blocks": [{"state": "jsc:personal_computer[facing=south]", "x": 9, "y": 0, "z": 11}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,4144,0,0,0,0,0]}", "state": "jscore:cable", "x": 10, "y": 0, "z": 11}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,2104,0,0,0,0,0]}", "state": "jscore:cable", "x": 11, "y": 0, "z": 11}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,10296,0,0,0,0,0]}", "state": "jscore:cable", "x": 12, "y": 0, "z": 11}, {"data": "{LinkedOwner:2473901207552L}", "state": "jsc:standard_hub", "x": 13, "y": 0, "z": 11}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,6200,0,0,0,0,0]}", "state": "jscore:cable", "x": 14, "y": 0, "z": 11}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,2072,0,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 0, "z": 11}, {"data": "{LinkedOwner:2473901207552L,Powered:1b}", "state": "jsc:speaker[facing=south,lit=true]", "x": 11, "y": 0, "z": 12}, {"data": "{LinkedOwner:2473901207552L,Powered:1b}", "state": "jsc:speaker[facing=south,lit=true]", "x": 12, "y": 0, "z": 12}, {"data": "{LinkedOwner:2473901207552L,MediaSlot:{Size:1,Items:[{Slot:0,id:\"jsc:floppy_disk\",count:1}]}}", "state": "jsc:floppy_drive[facing=south,loaded=true]", "x": 13, "y": 0, "z": 12}, {"data": "{LinkedOwner:2473901207552L,TrayOpen:1b,MediaSlot:{Size:1,Items:[{Slot:0,id:\"jsc:cd_rom\",count:1}]}}", "state": "jsc:cd_drive[facing=south,loaded=true]", "x": 14, "y": 0, "z": 12}, {"data": "{LinkedOwner:2473901207552L}", "state": "jsc:printer[facing=south]", "x": 15, "y": 0, "z": 12}], "caption": "A Personal Computer with its devices on a Peripheral Cable: two speakers, a Hub, a Floppy Drive, a CD Drive and a Printer. Drag the scene to see the back.", "height": 130, "view": {"zoom": 1.35}}
```

Each era has its own peripheral cable, and each one reaches a fixed number of cables from its computer. A device further away than that is not seen. A port takes the cable of its own era and of every earlier era, never a newer one.

> [!DEFINE] Port
> A socket on a computer, a hub or a device. Each socket has a kind: device, video or audio.

```table
source: jsc:peripheral_reach
caption: How far each cable reaches, in cables
heads: Era | Cable | Reach
```

Each kind of port takes its own kind of device. A drive, a printer, a Redstone Interface or a hub takes a device port. A monitor takes a video output of a graphics card. A speaker takes an audio output, and each audio output drives two speakers. The number of ports depends on the era of the board and of the card.

Table: Device ports by board era

| Board era | Device ports |
| --- | --- |
| Vintage | 2 ports |
| Legacy | 4 ports |
| Transition | 6 ports |
| Standard | 8 ports |
| Advanced | 10 ports |

Table: Video outputs by graphics card era

| Card era | Video outputs |
| --- | --- |
| Vintage | 1 output |
| Legacy | 2 outputs |
| Transition | 2 outputs |
| Standard | 4 outputs |
| Advanced | 4 outputs |

A server board adds one console output of its own. A processor with built-in graphics adds one output to the board. The audio outputs come from the sound card, and from the board itself from the Transition on ([](jsc:sound_cards)).

When the board runs out of device ports, a hub takes one port and offers ports of its own. A hub is a device, and each device cabled to it takes one of the hub's ports.

> [!DEFINE] Hub
> A device that takes one port and offers more ports of its kind, so that more devices can join one computer.

Table: Hubs and the ports they offer

| Hub | Ports it offers |
| --- | --- |
| Vintage Hub | 2 ports |
| Legacy Hub | 4 ports |
| Transition Hub | 4 ports |
| Standard Hub | 7 ports |
| Advanced Hub | 7 ports |

A cable after a hub reaches as far again as the cable before it, and a hub can hang from another hub. Only device links pass through a hub. Screens and speakers do not, so join them to the computer. If you break a hub, the devices behind it lose their link.

The Device Manager, the Frames tool that lists the hardware, shows what is on every port ([](jsc:device_manager)).

## If something goes wrong

> [!PROBLEM] The device is not seen.
> Check that the cable is within the reach of its era, and that it is not newer than the port.

> [!PROBLEM] A monitor joined through a hub stays dark.
> Screens and speakers do not pass through a hub. Join them to the computer.

> [!SEE] jsc:monitors, jsc:drives, jsc:device_manager
