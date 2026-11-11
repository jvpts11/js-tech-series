---
section: network_basics
order: 96
icon: jsc:mainframe
shows: [jsc:mainframe, jsc:server_rack, jsc:server, jsc:standard_router, jsc:fibre_cable, jsc:gigabit_cable, jsc:personal_computer]
---
# Your first network

Here is the smallest network that does something useful.

```scene
{"balloons": [{"detail": "It runs the network. Join the backbone cable to any side of it.", "number": 1, "part": {"x": 1, "y": 0, "z": 1}, "text": "The Mainframe"}, {"detail": "Run it from the router to the Mainframe.", "number": 2, "part": {"x": 3, "y": 0, "z": 1}, "text": "The backbone cable"}, {"detail": "Join the access cable and the backbone cable to it.", "number": 3, "part": {"x": 5, "y": 0, "z": 1}, "text": "The router"}, {"detail": "Run it from the computer to the router.", "number": 4, "part": {"x": 6, "y": 0, "z": 1}, "text": "The access cable"}, {"detail": "Plug the access cable into its back.", "number": 5, "part": {"x": 6, "y": 0, "z": 2}, "text": "The Personal Computer"}], "blocks": [{"data": "{VisualFlags:7}", "state": "jsc:mainframe[facing=south,optical=true]", "x": 1, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,4144,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 3, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:fibre_cable\"}],Links:[I;0,8240,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 4, "y": 0, "z": 1}, {"state": "jsc:standard_router", "x": 5, "y": 0, "z": 1}, {"data": "{Wires:[{cable:\"jsc:gigabit_cable\"}],Links:[I;6168,0,0,0,0,0,0,0,0]}", "state": "jscore:cable", "x": 6, "y": 0, "z": 1}, {"state": "jsc:personal_computer[facing=south]", "x": 6, "y": 0, "z": 2}], "caption": "A Mainframe, a router, data cables and a Personal Computer: the smallest network. Drag it to see the back.", "height": 120}
```

1. Build a Mainframe and give it its parts: an MTX motherboard, a power supply, processors, memory, a disk and a graphics card ([](jsc:mainframes)).
2. Install a system on the Mainframe, such as MC-NET, as on any computer: a monitor on its graphics card, and the install medium in a drive joined to it ([](jsc:installing)). A Mainframe without a system runs nothing.
3. Build a Server Rack and put servers with disks in its bays ([](jsc:server_racks)).
4. Join the Mainframe and the rack with the backbone cable of their era: HBW in the Legacy, Fibre Optic in the Standard. Fibre goes into the Mainframe only while it holds an Optical Network Card ([](jsc:expansion_cards)), and into the rack only through a Fibre Optic Interface mounted in it ([](jsc:rack_equipment)).
5. Place a router, and join your Personal Computer to it with the access cable, Ethernet or Gigabit ([](jsc:routers)). Run the backbone from the router to the Mainframe.
6. At the computer, open the Network program ([](jsc:network_interactor)). Put items in, and they are stored on the servers' disks; ask for them back, and they come out.

The Mainframe joins every data cable that touches any side of its footprint, so a backbone can meet it on whichever side is nearest. Fibre is the one cable it takes only with the card.

That is it. From here everything else is more of the same: more racks for more storage, more computers on the access line, a Crafting Computer for autocrafting.

## If something goes wrong

> [!PROBLEM] A computer is not on the network.
> Its cable is longer than its range, or of another era than the router. Add a repeater, or a router of the newer era.

> [!PROBLEM] The Network program says "○ Mainframe offline".
> No Mainframe is on the computer's network. Follow the cables from the computer to the Mainframe ([](jsc:data_cables)).

> [!PROBLEM] Nothing goes into storage, and every Operation stays queued.
> The Mainframe has no system, so it holds every Operation until one is installed ([](jsc:installing)).

> [!PROBLEM] The fibre does not join the Mainframe or the rack.
> Put an Optical Network Card in the Mainframe's card slots ([](jsc:expansion_cards)), and mount a Fibre Optic Interface in the rack ([](jsc:rack_equipment)).
