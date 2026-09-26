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
| `computers/src/main/resources/assets/jsc/textures/item/sound_card_*.png` | The sound cards, one per bus of the Vintage and Legacy boards | Flat item textures | AI-generated | AI assistant, with a generator script, from photographs of the real cards |
| `computers/src/main/resources/assets/jsc/textures/item/{cd_rom,cd_rw,dvd_rom,dvd_rw,floppy_disk,usb_flash_drive}.png` | The media: discs, the floppy disk and the USB flash drive | Flat item textures; the USB flash drive's is the atlas of its 3D model | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/item/{server,server_case,legacy_server,legacy_server_case,vintage_server,vintage_server_case,compute_server,compute_server_case,storage_server,storage_server_case,supercomputer_node}.png` | The servers and their empty cases | Flat item textures | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/item/{cooling_unit,kvm_switch,rack_ups}.png` | The rack units: cooling, KVM switch and UPS | Flat item textures | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/item/{mainframe,legacy_mainframe,vintage_mainframe,server_rack,legacy_server_rack,vintage_server_rack,supercomputer_rack}.png` | The cabinets as items | Their inventory icons | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/gui/program/**/*.png` | The program icons, one per program and per desktop look, and those of the windows no program owns: a setup, the welcome and a window a player's Sigma program opens | Drawn on the desktops: icons, launchers, taskbars, title bars and file views | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/gui/wallpaper/*.png` | The desktop wallpapers, scenes built from blocks the way the game's own art is | Hung behind every desktop but CDE's, and shown small as the thumbnails a player picks from | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/gui/file/**/*.png` | The file icons, one per kind of file and per desktop look | Drawn beside every file and folder: the explorer, the file dialog, the studios' trees and the desktop | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/gui/splash/*.png` | The maker's badge and logo, the marks of the Frames editions, FreeBSD's lockup, and the marks KDE Plasma and Cinnamon come up behind | Drawn on the boot and splash screens | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/gui/splash/ground/*.png` | The sky the oldest Frames edition starts on and the blue bands of the later one | Laid over the whole glass on those editions' boot, welcome and shutdown screens | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/gui/emblem/*.png` | The small four-pane mark of each Frames edition | Drawn on each edition's Start button and at the head of the newest one's installer | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/gui/installer/*.png` | The picture down the side of the oldest Frames wizard: a computer, a boxed program and a disk | Drawn in that wizard's side rail | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/gui/tray/*.png` | The network and speaker icons of a panel's notification area, and the badge of no network | Drawn in the panel's text colour at the corner of every panel but CDE's | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/gui/cde/backdrop_*.png` | The patterns of CDE's backdrops | Tiled across a CDE workspace and tinted in its colour scheme | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/textures/gui/cde/panel/*.png` | The pictures on the controls of CDE's front panel: the clock face, the calendar page, the folder, the page, the paints, the tiles and the book | Drawn on the front panel, the clock's hands and the day written over them | AI-generated | AI assistant, with a generator script |
| `computers/src/main/resources/assets/jsc/sounds/computer/power_button.ogg` | A computer's power button pressed and let go | Played at a computer, or at a rack for one of its bays, when the player presses its power button | Third-party | Pixel_Stick, CC0, https://freesound.org/people/Pixel_Stick/sounds/863988/ |
| `computers/src/main/resources/assets/jsc/sounds/computer/post_beep.ogg` | The one short beep of a PC speaker after a self-test | Played at a Vintage or Legacy computer when its self-test passes | Third-party | Author unknown, from an old YouTube video with no licence stated, https://www.youtube.com/watch?v=eTvft2-afpo |
| `computers/src/main/resources/assets/jsc/sounds/computer/vintage_startup.ogg` | A late-1990s desktop coming on: fan, drives and heads | Played at a Vintage computer when it comes on | Third-party | 607freesound, CC0, https://freesound.org/people/607freesound/sounds/550456/ |
| `computers/src/main/resources/assets/jsc/sounds/computer/hard_drive_{spin_up,idle,spin_down}.ogg` | A 7200 RPM hard drive spinning up, idling and spinning down, cut from one recording | Played at a computer with a hard drive when it comes on and goes off; the idle loops while it runs | Third-party | conath, CC0, https://freesound.org/people/conath/sounds/465592/ |
| `computers/src/main/resources/assets/jsc/sounds/media/disc_tray.ogg` | An optical drive's tray | Played at a CD or DVD drive, and at the Pattern Encoder, when a disc goes in or comes out | Third-party | griffinjennings, CC0, https://freesound.org/people/griffinjennings/sounds/595859/ |
| `computers/src/main/resources/assets/jsc/sounds/media/usb_insert.ogg` | A USB drive plugged in | Played at a Dock Station, and at the Pattern Encoder, when a USB drive goes in | Third-party | Pixabay Content License, https://pixabay.com/sound-effects/search/usb/ |
| `computers/src/main/resources/assets/jsc/sounds/media/usb_remove.ogg` | A USB drive pulled out | Played at a Dock Station, and at the Pattern Encoder, when a USB drive comes out | Third-party | BigKahuna360, Pixabay Content License, https://pixabay.com/sound-effects/film-special-effects-usb-slide-back-106529/ |
| `computers/src/main/resources/assets/jsc/sounds/media/floppy_insert.ogg` | A 3.5-inch floppy disk sliding into its drive | Played at a Floppy Drive when a disk goes in | Third-party | micropolis, CC0, https://freesound.org/people/micropolis/sounds/637057/ |
| `computers/src/main/resources/assets/jsc/sounds/media/floppy_read.ogg` | A diskette drive reading, its head stepping | Loops at a Floppy Drive while a system or a program installs from its disk | Third-party | 607freesound, CC0, https://freesound.org/people/607freesound/sounds/550461/ |
| `computers/src/main/resources/assets/jsc/sounds/media/floppy_eject.ogg` | A floppy disk ejected and drawn from its drive, two recordings joined | Played at a Floppy Drive when a disk comes out | Third-party | asiekierka, CC0, https://freesound.org/people/asiekierka/sounds/628245/, and micropolis, CC0, https://freesound.org/people/micropolis/sounds/637058/ |
| `computers/src/main/resources/assets/jsc/sounds/monitor/power_on.ogg` | A cathode ray tube monitor switching | Played at a monitor when it lights with its computer | Third-party | EdR, Pixabay Content License, https://pixabay.com/sound-effects/technology-old-pc-monitor-switch-off-8575/ |
| `computers/src/main/resources/assets/jsc/sounds/server/fan.ogg` | A quiet computer fan | Loops at each running server in a rack while fewer than five run close together | Third-party | EagleStealthTeam, Pixabay Content License, https://pixabay.com/sound-effects/technology-quiet-computer-fan-25659/ |
| `computers/src/main/resources/assets/jsc/sounds/server/room.ogg` | The hum of a server room | Loops in the middle of five or more running servers close together, in place of their fans | Third-party | soundandmelodies, CC0, https://freesound.org/people/soundandmelodies/sounds/776267/ |
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
| `computers/src/main/java/dev/jstech/computers/client/BootSplashArt.java` | The moving parts of the boot and shutdown screens: the running bar, the blocks in their trough and the ring of dots | Drawn over the boot and shutdown pictures | AI-generated | AI assistant, in code |
| `computers/src/main/java/dev/jstech/computers/client/DesktopSplashArt.java` | The loading screens of the desktops: the bordered box of the older ones with its row of squares, and the bars and spinners of the modern ones | Drawn between a system's last boot line and its desktop | AI-generated | AI assistant, in code |
