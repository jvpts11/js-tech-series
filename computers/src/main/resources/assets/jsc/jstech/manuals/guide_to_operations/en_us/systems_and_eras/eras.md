---
section: systems_and_eras
order: 10
icon: jsc:vintage_personal_computer
shows: [jsc:vintage_personal_computer, jsc:legacy_personal_computer, jsc:transition_personal_computer, jsc:personal_computer, jsc:advanced_personal_computer]
---
# The eras

Computers and parts of J's Computers belong to an era, a generation of hardware. The eras run from Vintage to Advanced, and each has its own word size, instruction set, firmware and media.

The word size is how much data a processor handles at once. Older computers had smaller words, so the room an item takes on a disk depends on the era ([](jsc:items_as_data)).

A disk's room is measured in megabytes (MB). The room per item is how many megabytes one item takes on a disk of that era.

```table
{"caption": "The eras", "heads": ["Era", "Word size", "Instruction set", "Room per item"], "rows": [[{"words": "Vintage"}, {"words": "16-bit"}, {"literal": "IA-16"}, {"amount": 1, "unit": "MB"}], [{"words": "Legacy"}, {"words": "32-bit"}, {"literal": "x86"}, {"amount": 16, "unit": "MB"}], [{"words": "Transition"}, {"words": "64-bit"}, {"literal": "x86-64"}, {"amount": 256, "unit": "MB"}], [{"words": "Standard"}, {"words": "64-bit"}, {"literal": "x86-64"}, {"amount": 256, "unit": "MB"}], [{"words": "Advanced"}, {"words": "64-bit"}, {"literal": "x86-64"}, {"amount": 256, "unit": "MB"}]]}
```

Table: The firmware and the media of each era

| Era | Firmware | System medium | Application medium |
| --- | --- | --- | --- |
| Vintage | Text BIOS | Floppy Disk, in a Floppy Drive | Floppy Disk, in a Floppy Drive |
| Legacy | Blue BIOS | CD-ROM, in a CD Drive | CD-ROM, in a CD Drive |
| Transition | Blue BIOS | DVD-ROM, in a DVD Drive | DVD-ROM, in a DVD Drive |
| Standard | UEFI | USB Flash Drive, in a Dock Station or a case's USB port | DVD-ROM for programs, USB Flash Drive for background services |
| Advanced | UEFI | USB Flash Drive, in a Dock Station or a case's USB port | Blu-ray for programs, USB Flash Drive for background services |

Parts do not mix freely across eras, so check the era of each part before you put it in. A Personal Computer takes only a motherboard of its own era. A processor, a memory module and a sound card must also match the era of the board. A graphics card is checked only for the bus it plugs into.

A newer era is faster: its parts have more cores, higher clocks, more memory and faster disks. Its start-up also finishes sooner ([](jsc:turning_on)).

Every era has its own creative tab, from J's Computers - Vintage to J's Computers - Advanced.

> [!DEFINE] Instruction set (ISA)
> The language a processor understands. A program built for one runs on it and on every newer one, never on an older one: x86-64 runs x86 and IA-16 programs.

## If something goes wrong

> [!PROBLEM] A part does not go into the slot of a board from another era.
> The processor, the memory module or the sound card must have the era of its board ([](jsc:motherboards)).

> [!SEE] jsc:items_as_data, jsc:motherboards
