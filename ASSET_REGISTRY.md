# Asset registry

Every asset of the J's Tech Series is listed here: where it is in the repository, what it is, where and how the
mods use it, where it came from and who made it. A pull request that adds, replaces or removes an asset updates this
file in the same pull request; one that brings an asset this file does not account for, or an entry that does not say
its origin and author, is rejected. See [AI_POLICY.md](AI_POLICY.md), rules 5 and 6.

The build holds the line: a test reads this file and fails on any asset in the repository that no entry covers, on
any entry that covers nothing, and on any entry whose origin or author is missing.

## How to read an entry

- **Asset** is a path from the root of the repository. `*` stands for any part of one file or folder name, `**` for
  any number of folders, and `{a,b}` for either of the names in the braces. Every asset file falls under exactly one
  entry.
- **Origin** is one of four:
  - **AI-generated**: made with an AI. It is a temporary asset: it helps the mods be built and played while they are
    built, and it is meant to be replaced by one made by a person. A hand-made replacement is always welcome.
  - **Hand-made**: drawn, modelled or recorded by a person, who is credited under **Made by**.
  - **Datagen**: written by a mod's own data generation (`runData`) from the declarations in its code, as every
    NeoForge mod's block states and models are. It changes when the code does.
  - **Third-party**: made outside the project and shipped under its own licence, named under **Made by** with its
    source, and reproduced in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
- **Made by** is the author for a hand-made asset, the licence and source for a third-party one, and how it was made
  for the rest.

Language files (`lang/`) are text, not assets, and are not listed; the language guide in
[docs/CODE_STYLE.md](docs/CODE_STYLE.md) covers them.

## The series

| Asset | What it is | Where and how it is used | Origin | Made by |
| --- | --- | --- | --- | --- |
| `docs/brand/*.png` | The logos and square icons of the series and of each mod | The READMEs and the repository's pages | AI-generated | AI assistant, with a generator script |

## J's Core

| Asset | What it is | Where and how it is used | Origin | Made by |
| --- | --- | --- | --- | --- |
| `core/src/main/resources/jscore_{logo,icon}.png` | J's Core's logo and square icon | The mods list: the logo drawn over the series logo, the icon in lists that show one | AI-generated | AI assistant, with a generator script |
| `core/src/main/resources/assets/jscore/textures/item/*.png` | The material items: iron dust, iron and copper plates | Flat item textures of the material items | AI-generated | AI assistant, with a generator script |
| `core/src/generated/resources/assets/**` | Item models, the English file and the era skins' palettes | Written from the core's declarations; the palettes are the colours a resource pack can replace | Datagen | The core's data generation |

## J's Computers

| Asset | What it is | Where and how it is used | Origin | Made by |
| --- | --- | --- | --- | --- |
| `computers/src/main/resources/jsc_{logo,icon}.png` | J's Computers' logo and square icon | The mods list: the logo drawn over the series logo, the icon in lists that show one | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/block/*personal_computer_*.png` | The Personal Computer in its three eras: front, side and top | Faces of the block model, one set per era | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/block/*crafting_computer_*.png` | The Crafting Computer in its three eras: front, side and top | Faces of the block model, one set per era | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/block/*cluster_management_computer_*.png` | The Cluster Management Computer in its three eras: front, side and top | Faces of the block model, one set per era | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/block/*monitor_*.png` | The Monitor in its three eras: front dark and lit, and side | Faces of the block model; the lit front shows while its computer is on | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/block/{cd,dvd,floppy}_drive_*.png` | The CD, DVD and Floppy Drives: casing, front, and front with a medium in | Faces of the drive blocks; the active front shows while a medium is in | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/block/dock_hub_*.png` | The Dock Station: front empty and docked, side and top | Faces of the hand-made Dock Station models | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/block/network_gateway_*.png` | The Network Gateway: front, lit front, back, side and top | Faces of the gateway block; the lit front replaces the plain one while the gateway is lit | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/block/{server,personal}_router_*.png` | The Server Router and the Personal Router | Faces of the router blocks | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/block/{crafting,ethernet,hbw,hpc,peripheral}_cable.png` | The five cables | The cable core and arms, one texture per cable | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/block/{export,import}_bus.png` | The Export and Import Buses | The bus parts on a data cable | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/block/crafting_switch.png` | The Crafting Switch | Every face of the switch block | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/block/hbw_interface_*.png` | The HBW Interface: side and top | Faces of the interface block | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/block/{mainframe,pattern_encoder,rack}_particle.png` | The particles of the Mainframe, the Pattern Encoder and the racks | The particles those blocks break into; their bodies are drawn by their block entities | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/block/mainframe_side.png` | A grey metal plate | Top and bottom of the Tank | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/block/{mainframe,pattern_encoder,rack}/*.png` | The texture atlases of the cabinets: the Mainframes, the Pattern Encoders and the racks, per era | Painted on their GeckoLib models | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/geo/*.geo.json` | The GeckoLib models of the Mainframes, the Pattern Encoders and the racks, per era | Drawn by those blocks' renderers, with parts shown or hidden by what is installed | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/animations/*.animation.json` | The animations of those models: their fans turning | Played by the same renderers while the machine runs | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/models/block/*.json` | The cable core and arm, the Dock Station empty and docked, and the bus parts | Block models written by hand rather than by the data generation | AI-generated | AI assistant, modelled in Blockbench |
| `computers/src/main/resources/assets/jsc/models/item/usb_flash_drive.json` | The USB flash drive as a small 3D item | The item model of the USB flash drive | AI-generated | AI assistant, modelled in Blockbench |
| `computers/src/main/resources/assets/jsc/textures/item/cpu_*.png` | The processors, one per model across the eras | Flat item textures | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/item/gpu_*.png` | The graphics cards, one per model across the eras | Flat item textures | AI-generated | AI assistant, with a generator script, from photographs of the real cards |
| `computers/src/main/resources/assets/jsc/textures/item/phi_*.png` | The coprocessor cards | Flat item textures | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/item/ram_*.png` | The memory modules, one per generation | Flat item textures | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/item/motherboard_*.png` | The motherboards, one per form factor and era | Flat item textures | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/item/psu_*.png` | The power supplies | Flat item textures | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/item/disk_*.png` | The disks: hard disks, SSDs and NVMe drives by size, and the period drives | Flat item textures | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/item/{cache_card,crafting_card_t2,crafting_card_t3,fabric_host_adapter,management_nic,raid_controller,serial_console_card}.png` | The expansion cards | Flat item textures | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/item/{cd_rom,cd_rw,dvd_rom,dvd_rw,floppy_disk,usb_flash_drive}.png` | The media: discs, the floppy disk and the USB flash drive | Flat item textures; the USB flash drive's is the atlas of its 3D model | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/item/{server,server_case,legacy_server,legacy_server_case,vintage_server,vintage_server_case,compute_server,compute_server_case,storage_server,storage_server_case,supercomputer_node}.png` | The servers and their empty cases | Flat item textures | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/item/{cooling_unit,kvm_switch,rack_ups}.png` | The rack units: cooling, KVM switch and UPS | Flat item textures | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/item/{mainframe,legacy_mainframe,vintage_mainframe,server_rack,legacy_server_rack,vintage_server_rack,supercomputer_rack}.png` | The cabinets as items | Their inventory icons | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/gui/program/**/*.png` | The program icons, one per program and per desktop look | Drawn on the desktops: icons, launchers, taskbars and file views | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/gui/splash/*.png` | The maker's badge and logo, the marks of the Frames editions, and FreeBSD's lockup | Drawn on the boot and splash screens | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/gui/cde/backdrop_*.png` | The patterns of CDE's backdrops | Tiled across a CDE workspace and tinted in its colour scheme | AI-generated | AI assistant, with a generator script |
| `computers/src/generated/resources/assets/**` | Block states, block and item models, the English file and the palettes | Written from J's Computers' declarations; the palettes are the colours a resource pack can replace | Datagen | J's Computers' data generation |

## J's Industrial

| Asset | What it is | Where and how it is used | Origin | Made by |
| --- | --- | --- | --- | --- |
| `industrial/src/main/resources/jsindustrial_{logo,icon}.png` | J's Industrial's logo and square icon | The mods list: the logo drawn over the series logo, the icon in lists that show one | AI-generated | AI assistant, with a generator script |
| `industrial/src/main/resources/assets/jsindustrial/textures/block/*.png` | The Coal Generator, Compressor, Electric Furnace and Macerator: front, side and top | Faces of the machine blocks | AI-generated | AI assistant, with a generator script |
| `industrial/src/generated/resources/assets/**` | Block states, block and item models and the English file | Written from J's Industrial's declarations | Datagen | J's Industrial's data generation |

## Art that lives in the code

Some pictures are drawn by the code itself, out of rectangles and gradients, rather than loaded from a file.

| Asset | What it is | Where and how it is used | Origin | Made by |
| --- | --- | --- | --- | --- |
| `computers/src/main/java/dev/jstech/computers/program/cli/ScreenfetchLogos.java` | The text-art logos of Ubuntu, Debian, Fedora, Arch Linux, Gentoo and FreeBSD, and the two marks of Frames | Printed by the `screenfetch` command | Third-party | neofetch 7.1.0 by Dylan Araps, MIT licence, https://github.com/dylanaraps/neofetch |
| `computers/src/main/java/dev/jstech/computers/client/os/WallpaperPainter.java` | The desktop wallpapers | Painted behind every desktop | AI-generated | AI assistant, in code |
| `computers/src/main/java/dev/jstech/computers/client/os/FileIcons.java` | The small icons beside files and folders | Drawn by every program that lists files | AI-generated | AI assistant, in code |
| `computers/src/main/java/dev/jstech/computers/client/FramesEmblem.java` | The four-pane emblem of the Frames editions | Drawn on the firmware setup, the installer, the screen an edition starts behind and its Start button | AI-generated | AI assistant, in code |
| `computers/src/main/java/dev/jstech/computers/client/BootSplashArt.java` | The pictures the systems start and stop behind | Drawn on the boot and shutdown screens | AI-generated | AI assistant, in code |
| `computers/src/main/java/dev/jstech/computers/client/DesktopSplashArt.java` | The pictures a desktop shows while it starts | Drawn between a system's last boot line and its desktop | AI-generated | AI assistant, in code |
