---
section: peripherals
order: 72
icon: jsc:dock_station
---
# The Dock Station

The Dock Station reads storage disks and Universal Serial Bus (USB) flash drives for a computer. You join it to a computer like any device ([](jsc:peripheral_cables)), with a cable of any era. It has three bays for disks, and a USB port for one flash drive.

```scene
{"balloons": [{"detail": "It takes a hard disk.", "number": 1, "part": {"box": [5, 10, 15, 14, 14, 16], "x": 15, "y": 0, "z": 15}, "text": "The 3.5\" bay"}, {"detail": "It takes a solid-state drive.", "number": 2, "part": {"box": [5, 6, 15, 14, 9, 16], "x": 15, "y": 0, "z": 15}, "text": "The 2.5\" bay"}, {"detail": "It takes a Non-Volatile Memory Express (NVMe) disk.", "number": 3, "part": {"box": [5, 2, 15, 14, 5, 16], "x": 15, "y": 0, "z": 15}, "text": "The M.2 tray"}, {"detail": "It takes one USB flash drive.", "number": 4, "part": {"box": [2, 7, 15, 4, 9, 16], "x": 15, "y": 0, "z": 15}, "text": "The USB port"}, {"number": 5, "part": {"x": 15, "y": 0, "z": 14}, "text": "The Peripheral Cable"}, {"number": 6, "part": {"x": 12, "y": 0, "z": 14}, "text": "The computer"}], "blocks": [{"state": "jsc:personal_computer[facing=south]", "x": 12, "y": 0, "z": 14}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,4144,0,0,0,0,0]}", "state": "jscore:cable", "x": 13, "y": 0, "z": 14}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,48,0,0,0,0,0]}", "state": "jscore:cable", "x": 14, "y": 0, "z": 14}, {"data": "{Wires:[{cable:\"jsc:peripheral_cable\"}],Links:[I;0,0,0,2072,0,0,0,0,0]}", "state": "jscore:cable", "x": 15, "y": 0, "z": 14}, {"data": "{LinkedOwner:3298534940672L,MediaSlot:{Size:1,Items:[{Slot:0,id:\"jsc:usb_flash_drive\",count:1}]},Bays:{Size:3,Items:[{Slot:0,id:\"jsc:disk_hdd_500g\",count:1},{Slot:1,id:\"jsc:disk_ssd_500g\",count:1},{Slot:2,id:\"jsc:disk_nvme_500g\",count:1}]}}", "state": "jsc:dock_station[facing=south,loaded=true]", "x": 15, "y": 0, "z": 15}], "caption": "A Dock Station joined to a Personal Computer, with a disk in each of its three bays and a USB flash drive in its port. Drag the scene to see the back.", "height": 120, "view": {"zoom": 1.35}}
```

> [!DEFINE] Bay
> A tray of the Dock Station that takes one disk of one size.

Table: The bays and the port

| Place | Takes | Shown on the Dock |
| --- | --- | --- |
| Bay | A hard disk | 3.5" BAY |
| Bay | A solid-state drive (SSD) | 2.5" BAY |
| Bay | A Non-Volatile Memory Express (NVMe) disk | M.2 TRAY |
| Port | A USB flash drive | USB |

A bay takes one disk, and only a disk of its own size and of the Dock's era, the Standard, or an older one. The Dock refuses anything that is not a disk or a flash drive.

## Putting things in and out

1. Click the Dock with a disk in your hand to put it in its bay.
2. Click the Dock with a USB flash drive in your hand to plug it into the port.
3. Sneak-click the Dock to take out the last thing you put in: the flash drive first, then the bays from the bottom.
4. Click the Dock with an empty hand to open its window.

A system's installer can be on a USB flash drive in the Dock, so that you install from the Dock ([](jsc:installing)).

## The window

The window shows each bay and the port, with what they hold. You can put disks in and take them out there too.

Table: Part of the window

| Part | What it shows |
| --- | --- |
| Link line | "Linked to a computer" when a computer reaches the Dock, and "No computer linked" when none does. |
| Bay rows | Each bay, empty, or with its disk and how much of it is used. |
| Port row | The USB port, with the flash drive in it, or empty. |
| Letters | The drive letter the computer gives each disk and the flash drive. |

> [!DEFINE] Drive letter
> The short name a computer gives a drive or a docked disk, so that programs can find it.

The computer sees each disk in the Dock as an external drive, and it can open and copy to it. The Dock works only while a computer is linked to it.

## If something goes wrong

> [!PROBLEM] That tray already holds a disk - sneak-click or use the window to take it out.
> Take the disk out in the window first.

> [!PROBLEM] The Dock says "Not docked to a computer" or "No computer linked".
> Join the Dock to a computer with a peripheral cable ([](jsc:peripheral_cables)).

> [!PROBLEM] Nothing happens when you click the Dock.
> Only disks and USB flash drives go in the Dock. Anything else is refused, and the Dock says nothing.

> [!PROBLEM] Too new for this Dock Station: a Standard dock takes disks of its era or older.
> The disk is of the Advanced. Read it in an Advanced computer instead.

> [!SEE] jsc:drives, jsc:disks, jsc:installing
