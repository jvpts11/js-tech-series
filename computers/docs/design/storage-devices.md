# Storage devices

Disks, removable media, and where what they hold is kept. How the network uses disks (local storage, the network's
storage, corruption and recovery) is in [Storage](storage.md).

## Disks

A disk is one of three kinds: a hard disk (HDD), a solid-state disk (SSD) or an NVMe disk. **Disks of the same size hold
the same amount**, whatever their kind. The kinds differ only in speed, in how long they wait before a transfer, and in
what they draw:

| Kind | Line | Speed | Wait before a transfer | Draw |
| --- | --- | --- | --- | --- |
| HDD | Vaultis Keep | 1x | 10 ticks | 6 W |
| SSD | Vaultis Swift | 4x | 3 ticks | 3 W |
| NVMe | Vaultis Bolt | 16x | 1 tick | 5 W |

Each kind's wait is a key in `jstech-balance.toml` ([Implementation](implementation.md)). Every disk waits on a virtual
thread of its own, so several disks in one server work in parallel without holding up the server's tick.

### Capacity follows the disk's era

What an item takes on a disk follows from the machine word of the disk's era: 16 bits spend 1 MB per item, 32 bits spend
16 MB, and 64 bits (from the Transition on) spend 256 MB. Every disk has an era, a capacity in items and a capacity in
megabytes that follows from it, and **a disk's name is true**: a Vintage Trench 20M holds 20 items, a Legacy IDE 4G
holds 256, a 500 GB disk holds 2,000 and every terabyte holds 4,096. A bucket of fluid (1,000 mB) weighs the same as an
item.

### The disks there are

From the Transition on, disks come in a grid of kind and size: 500 GB, 1, 2, 4 and 8 TB in every kind, and 12, 16, 20
and 24 TB in hard disks only (the helium disks of their years, when flash of that size was not sold to put in a
computer). Each combination is an item of its own ("Vaultis Keep HDD 4T"), and each belongs to the years it was sold in:

- hard disks up to 2 TB: Transition;
- the helium hard disks, the 8 TB SSD and the 4 and 8 TB NVMe disks: Advanced;
- the rest of the grid: Standard.

The eras before the grid have disks of their own: Vaultis Trench (Vintage hard disks) and Vaultis Link (Legacy IDE
disks, Transition SATA SSDs). All 26 are in [the catalogue](catalogue.md).

## What a disk holds

A disk holds items, fluids, chemicals, folders, files, programs and operating systems together, in a single tree: the
room a system and its programs take is room the items don't have. Several systems on one disk share the same tree.
Formatting a disk erases everything on it. Losing the disk a system runs from shuts the machine down
([Operations](operations.md), [Operating systems](operating-systems.md)).

Operating systems are measured in megabytes, and turn into items on the disk they are installed on:

| System | Size |
| --- | ---: |
| MC-DOS | 4 MB |
| MC-NET | 8 MB |
| UNIX System V | 10 MB |
| Frames 95 | 48 MB |
| Frames XP | 1,536 MB |
| Frames 7, Frames 10 | 16,384 MB |
| Frames 11 | 20,480 MB |
| Ubuntu, Fedora | 8,192 MB |
| Debian, Gentoo | 4,096 MB |
| Arch, FreeBSD | 2,048 MB |

Frames XP on a Legacy disk takes 96 items; Frames 11 doesn't fit on a Vintage disk; MC-NET fits anywhere.

Inside a disk's budget an item weighs 1,000 units (the same as a bucket of fluid), and a file weighs its size against
the item size of the disk's era, rounded up: a file as big as an item weighs as much as an item. The network counts a
server's room in items, not in megabytes.

A disk's tooltip says its word and what an item costs on it ("16-bit architecture, 1 MB per item") and, with a system
installed, which system it is, its desktop and how many programs it has.

### Volumes

What a disk holds is not kept on the item. It lives in a **volume**, saved with the world (in the overworld's saved
data, by id); the item only carries the volume's id and a summary of its use (weight used, items, fluid, chemical,
types). So a disk taken out of one machine and put in another takes everything with it, and a full disk is not a stack
tens of kilobytes heavy, copied every time a slot is synchronised. A volume is only encoded again when it was touched,
so saving the world costs what changed.

Removable media keep what they hold on the item, because they are small and travel.

A disk copied in creative mode shares its volume with the original.

## Removable media

| Medium | Era | Read in |
| --- | --- | --- |
| Floppy disk | Vintage | a Floppy Drive |
| CD | Legacy | a CD Drive, DVD Drive or Blu-ray Drive |
| DVD | Transition | a DVD Drive or Blu-ray Drive |
| USB stick | Standard | a Dock Station |
| Blu-ray | Advanced | a Blu-ray Drive |

A medium holds a system's installer, a program's installer, or data (files, crafting patterns). Optical discs come
**pressed** (CD-ROM, DVD-ROM, BD-ROM), which nothing can write, or **rewritable** (CD-RW, DVD-RW, BD-RE); floppy disks
and USB sticks can always be written. The drives are peripherals ([Peripherals](peripherals.md)).

Today a medium's room is counted in items of 4 MB: a floppy disk holds 1,024, a CD 8,192, a DVD 65,536, a USB stick
262,144 and a Blu-ray 1,048,576.

The media of the AI are designed with it ([Artificial intelligence](ai.md)). Disk corruption is in
[Storage](storage.md).

## To build

- **Real capacities for removable media**, in bytes: a floppy disk 1.44 MB, a CD 700 MB, a DVD 4.7 GB, a USB stick 8 to
  64 GB by model, a Blu-ray 25 GB. Files weigh what they weigh. Installation media are pressed images that project their
  files: what a system takes once installed counts on the disk it goes to, not on the medium, so an installer always
  fits on its medium. Capacity only limits writable media, with files and patterns.
- **Disks only in machines of their era or newer**: see [Eras](eras.md).
