# Wireless

Wireless is an essential part of the mod, like the cables: it can't be turned off ([Overview](overview.md)). It starts
in the Legacy, when Wi-Fi arrives; the Vintage has no wireless. Nothing on this page exists yet.

## To build

### Access points

The **Wireless Access Point** (WAP) is a network device, linked by a data cable, that draws energy and gives the
wireless network to the portable devices and to the computers with a Wi-Fi card around it. There is one per era, each
slower than the access cable of its era (estimates):

| Era | Standard | Reach (blocks) | Speed (items per tick) | Draw |
| --- | --- | ---: | ---: | ---: |
| Legacy | 802.11b/g | 24 | 4 | 10 W |
| Transition | 802.11n | 32 | 16 | 12 W |
| Standard | 802.11ac | 48 | 64 | 15 W |
| Advanced | Wi-Fi 6 | 64 | 128 | 20 W |

Several access points in the same area don't conflict: each device joins the nearest one, and an access point has no
limit of devices.

The Legacy's (a blue and black box with two antennas) and the Transition's (glossy black, three antennas) stand on a
desk and take their cables from behind, each line showing its socket where it meets the face ([Quince](quince.md)); the
Standard's (a white disc with a blue ring) and the Advanced's (a rounded square with a light bar) go on a ceiling or a
wall on their mounting plate and take their cables through that face. Data and energy come on two lines, with no Power
over Ethernet. The Network Manager shows each as an ACCESS POINT node, and its tooltip gives its reach, its speed and
its draw.

### Wi-Fi cards

An expansion card per era (PCI in the Legacy, PCI Express x1 later) that joins a computer to the network through an
access point instead of a cable, at the speed of the older of the two, the card or the access point, as real Wi-Fi
does. The Legacy's has one antenna, the Transition's and the Standard's three, and the Advanced's two, with
Bluetooth on the same card; the antennas stand out of the computer's back. A card draws nothing of its own. Only
the small computers take one (the Personal Computer, the Crafting Computer, the Cluster Management Computer): the
Mainframe and the racks stay on the backbone, by cable ([The network](network.md)).

### Portable devices

The PDA, the Smartphone and the Tablet follow real history, each per era: the PDA in the Legacy and the Transition (the
Pocket PC with Wi-Fi), the Smartphone from the Transition on (2007) and the Tablet from the Standard on (2010). Each has
its own hardware (the system on a chip, the memory, the storage) of its era, shown in the item's window (a Legacy PDA:
400 MHz, 64 MB of memory, 64 MB of storage), and joins the network through an access point.

| Device | Eras | Systems |
| --- | --- | --- |
| PDA | Legacy, Transition | **Frames Pocket**, Midsoft's, after the Pocket PC |
| Smartphone | Transition on | **qOS** on Quince's **qPhone** ([Quince](quince.md)); **Droidix**, open, on the Linux kernel, on the other makers' phones, after Android |
| Tablet | Standard on | qOS on Quince's **qPad**; Droidix on the other makers' tablets |

The twelve devices, each with its system as it was in its years:

| Device | Kind | Era | System |
| --- | --- | --- | --- |
| Hightide Pocket | PDA | Legacy | Frames Pocket of 2002 |
| Hightide Pocket Pro | PDA, with a 2 GB memory card | Transition | Frames Pocket of 2007 |
| qPhone | smartphone | Transition | qOS of 2007 |
| Motoroller Landmark | smartphone | Transition | Droidix of 2009 |
| qPhone 5 | smartphone | Standard | qOS of 2013 |
| Hightide Wave | smartphone | Standard | Droidix of 2014 |
| qPad Air | tablet | Standard | qOS of 2013 |
| Sunsung Nebula Tab | tablet | Standard | Droidix of 2014 |
| qPhone X | smartphone | Advanced | qOS of 2017 |
| Sunsung Nebula S10 | smartphone | Advanced | Droidix of 2019 |
| qPad Pro, with the Quince Q1 | tablet | Advanced | qOS of 2017 |
| Sunsung Nebula Tab S7 | tablet | Advanced | Droidix of 2019 |

The tablets wear the look of their family's phone of the same era.

- **The mobile system** is a real system, from the factory, with no installer: parodies of the real ones
  ([Operating systems](operating-systems.md)). **Apps** are programs of the registry with a mobile platform, installed
  through the Mirror like an app store ([Programs](programs.md)). The basic ones: the network (a simpler version of the
  Network Interactor, to request, deposit, see the stock and craft anywhere an access point reaches), notifications
  (below), the Crypto Wallet ([Teracoin](teracoin.md)) and the Messenger.
- Portable devices have no "tabs": they run their mobile system and its apps.
- **In play:** in the hand a device is held as the game holds an item, its screen lit; in use it rises to the middle
  of the view over the blurred world, its own face framing its screen. Its battery is the item's bar, always in
  view, with the percentage in its tooltip. It joins at the speed of the older of the two, the device or the access
  point, as a Wi-Fi card does.
- **The systems:** each shows the world's time and the date as Day N. A device starts with its maker's mark, then its
  system's start, then the lock screen, which only slides or takes a tap on Unlock (a password and a pattern come when
  security reaches the portables). Alerts arrive in each system's own place (Frames Pocket's balloon and Today
  page, qOS's lock screen and Notification Center, Droidix's lock screen and shade) and as a badge on the app's
  icon. Settings show the hardware, the Wi-Fi and the battery.
- **The apps by era:** the Crypto Wallet from the Transition, the Messenger from the Standard, the camera, the gallery
  and Maps only on the smartphones and tablets, prospecting from the Standard. The stores (the Frames Marketplace,
  the Quince Store and the Droidix Store) are served by the Mirror, and everything in them is free.
- **The network app** has three tabs, Stock, Device and Craft, in the Network Interactor's words. A request puts the
  items into the device's own storage, never straight into the inventory; the Device tab takes them to the inventory
  (Take) or back to the network (Return), and its Deposit lists the player's inventory row by row. A device holds what
  its storage holds over the item size of its era's disk, less its system ([Storage devices](storage-devices.md)): 4
  items on the Hightide Pocket, 8 on the Pocket Pro with its card, 112 on the qPhone 5, 480 on the Nebula S10, about a
  thousand on the qPad Pro.
- **Hardware:** the camera and GPS come from the factory on the smartphones and tablets of their era, as on the real
  ones. The **camera** takes a picture of what the player sees, without the HUD, at the resolution of the device's era
  (640 by 480 in the Transition, 1280 by 960 in the Standard, 4032 by 3024 in the Advanced), kept as an image in its
  gallery, which sends it to a computer of the network as a PNG file. The **GPS** gives the Maps app, drawn on J's
  Core's map with its markers and the reach of the access points. **Prospecting** is an app on the smartphones and
  tablets from the Standard on: it asks J's Core's world kit for the direction and the strength of the veins nearby,
  never their coordinates, and shows them as a compass and bars ([Survival](survival.md)). There is
  no extra reach: another access point, on its cable, is how wireless goes further.
- **The battery:** a portable runs on its battery, charged in the **Mobile Device Charging Dock** of its era on any
  energy; a charge lasts about two game days of use, less on a PDA (estimates). The dock holds a device of its era by
  the device's own model (a cradle in the Legacy, a white base in the Transition, an aluminium stand in the Standard,
  a wireless stand in the Advanced), the screen showing the battery filling, the dock's lamp lit. It has no window:
  a right click puts a device in and takes it out. Its energy comes from behind.
- **A device's state** lives on the server, kept by the id the item carries. **Whoever loses the device loses what was
  on it:** when the item is destroyed (lava, fire, the void, despawning), the server erases its state and frees the
  room; and the state of a device that hasn't been in any inventory for 72 hours of the server running is erased too
  (`[wireless] lost_device_hours`).

With the portables comes the third family of systems and hardware, next to Frames and Linux: **Quince**, with its
computers, servers and Mainframe as well ([Quince](quince.md)).

### Notifications

The network sends **alerts** to the players' portable devices: an item below a threshold, an Operation that failed, a
degraded power supply ([Power](power.md)), a UPS running on battery, a network conflict. Each player turns the five
kinds on or off in the Alerts app, and watches the items they choose, with a level for each, on their own device; a
rule for the whole network belongs to the automation. By default alerts only reach the device; a setting of each
player (`jscomputers-client.toml`, `notifications_on_hud`) shows them on the HUD too, as J's Core notifications at the
attention level, naming the device they came from. Who can see which alerts is decided with
security ([Security](security.md)).

Inside the desktops, the system's own notifications (the Action Center, the balloons) and the advancement toasts carry
on as they are.

### Cost

One index of access points per network, checked on a timer, never per player and per tick. Reach through space (the
Communication Satellites) belongs to J's Space ([The series](series.md)).
