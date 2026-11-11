---
section: assembly
order: 16
icon: jsc:internal_cd_drive
covers_family: jsc:case_front
---
# The front of the case

The front of a Personal Computer, a Crafting Computer or a Cluster Management Computer has parts you can click. It has a power button, bays for drives and, from the Transition on, USB ports. Click one of those parts and it does its job. Click anywhere else on the case and the assembly screen opens, or, with a sneak-click, the side panel comes off or goes back on.

```scene
{"balloons": [{"detail": "Click it to switch the computer on or off.", "number": 1, "part": {"box": [12, 9, 15, 14, 12, 16], "x": 15, "y": 0, "z": 15}, "text": "Power button"}, {"detail": "The first is lit while the computer runs. The second blinks while it works its disk.", "number": 2, "part": {"box": [9, 10, 15, 12, 12, 16], "x": 15, "y": 0, "z": 15}, "text": "Power and disk lamps"}, {"detail": "Its eject button opens and closes the tray.", "number": 3, "part": {"box": [3, 13, 14, 13, 15, 16], "x": 15, "y": 0, "z": 15}, "text": "The Internal DVD Drive"}, {"detail": "Click with a disc in your hand to lay it on the open tray. Sneak-click to take it out.", "number": 4, "part": {"box": [4, 12, 14, 12, 13, 16], "x": 15, "y": 0, "z": 15}, "text": "The tray and its disc"}, {"detail": "Sneak-click the port to pull the stick out.", "number": 5, "part": {"box": [1, 11, 15, 5, 13, 16], "x": 15, "y": 0, "z": 15}, "text": "A USB flash drive"}, {"detail": "Click it with a USB flash drive in your hand to plug the stick in.", "number": 6, "part": {"box": [1, 9, 15, 5, 10, 16], "x": 15, "y": 0, "z": 15}, "text": "A free USB port"}], "blocks": [{"data": "{Hardware:{Size:15,Items:[{Slot:0,id:\"jsc:motherboard_atx_standard_lga1150\",count:1},{Slot:1,id:\"jsc:cpu_integra_centro_c5_4690k\",count:1},{Slot:2,id:\"jsc:ram_ddr3_8192\",count:1},{Slot:3,id:\"jsc:ram_ddr3_8192\",count:1},{Slot:6,id:\"jsc:gpu_vertex_gtx_970\",count:1},{Slot:10,id:\"jsc:psu_650g\",count:1},{Slot:11,id:\"jsc:disk_hdd_4t\",count:1},{Slot:12,id:\"jsc:disk_nvme_500g\",count:1},{Slot:13,id:\"jsc:internal_dvd_drive\",count:1}]},Front:{Size:4,Items:[{Slot:0,id:\"jsc:dvd_rom\",count:1},{Slot:2,id:\"jsc:usb_flash_drive\",count:1}]},Bay0TrayOpen:1b,Lamps:3}", "state": "jsc:personal_computer[facing=south]", "x": 15, "y": 0, "z": 15}], "caption": "The front of a Standard Personal Computer: the power button and its two lamps, a DVD drive with its tray out, and a USB stick in a port. Drag it to look at the case from the side.", "height": 130, "view": {"pitch": 15.0, "yaw": -20.0, "zoom": 1.4}}
```

Look at a standing part, such as an open tray or a stick in a port, and your click goes to that part. The power button, at the top of the front, switches the computer on and off, whatever you hold. It does the same as POWER on the assembly screen.

> [!DEFINE] Standing part
> A part that sticks out of the front and takes clicks of its own.

Table: The front of each era's case

| Era | 5.25-inch bays | USB ports on the front |
| --- | --- | --- |
| Vintage | 1 bay | 0 USB ports |
| Legacy | 2 bays | 0 USB ports |
| Transition | 2 bays | 2 USB ports |
| Standard | 1 bay | 2 USB ports |
| Advanced, Exa and Singularity | 0 bays | 2 USB ports |

> [!DEFINE] Bay
> A slot on the front of the case for a 5.25-inch drive.

## What goes in a bay

Four parts go in a bay, one to a bay, and any of them goes in any bay the case has. You put one in on the assembly screen.

Table: The parts of a bay

| Part | Era | What it takes |
| --- | --- | --- |
| Hot-Swap Drive Bay | Vintage | Disks, from the front |
| Internal CD Drive | Legacy | CDs |
| Internal DVD Drive | Transition | DVDs and CDs |
| Internal Blu-ray Drive | Advanced | Blu-ray discs, DVDs and CDs |

An internal drive works like the drive of its kind outside the case ([the drives](jsc:drives)). The eject button on its front opens or closes the tray. With the tray open, click with a disc in your hand to lay it on the tray. Sneak-click the open tray to take the disc out again.

A drive reads only the discs of its kind. A CD drive reads CDs, a DVD drive reads DVDs and CDs, and a Blu-ray drive reads all three.

> [!DEFINE] Tray
> The drawer of an optical drive, which carries the disc.

## The Hot-Swap Drive Bay

The Hot-Swap Drive Bay takes a disk from the front. Click with a disk in your hand to slide it in, and click with an empty hand to take it out. It works with the computer on or off. It takes a disk of the computer's own era, or of an older one.

> [!DEFINE] Hot-swap
> A part you can pull out and push back in while the computer is on.

The disk in the bay is one of the computer's own. It gets a drive letter, a system can go on it, and the computer can boot from it. It adds to the computer's capacity and to the power it needs. It takes none of the board's disk slots, so a full board still takes one more disk here.

## USB ports

Click a port while you hold a USB flash drive to plug it in. Sneak-click a port to pull its stick out. A stick works as a drive of its own, and a system can be installed from it ([Installing a system](jsc:installing)).

## If something goes wrong

> [!PROBLEM] The disc will not go in, and the game says "The tray is closed - press the eject button to open it."
> Press the eject button on the drive's front to open the tray.

> [!PROBLEM] The drive cannot read that disc.
> The game names the drive, as in "This CD drive cannot read that disc." A CD drive reads CDs, a DVD drive DVDs and CDs, and a Blu-ray drive all three. Use a drive of the right kind.

> [!PROBLEM] The tray will not come out, and the game says "The drive is reading - wait for it to finish."
> The drive is reading a disc. Wait for it to finish.

> [!PROBLEM] The drive already holds a disc.
> Press the eject button to open the tray, then sneak-click the open tray to take the disc out.

> [!PROBLEM] The port already holds a stick.
> Sneak-click the port to pull its stick out.

> [!PROBLEM] The bay already holds a disk.
> Click the bay with an empty hand to take its disk out.

> [!PROBLEM] The hot-swap bay refuses a disk, and the message says this computer does not take that disk.
> The disk is newer than the computer's era. Use a disk of the computer's era or of an older one.

> [!SEE] jsc:assembly_screen, jsc:drives, jsc:disks
