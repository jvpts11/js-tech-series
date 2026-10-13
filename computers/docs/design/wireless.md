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

### Wi-Fi cards

An expansion card per era (PCI in the Legacy, PCI Express later) that joins a computer to the network through an access
point instead of a cable, at the speed of the older of the two, the card or the access point, as real Wi-Fi does. Only
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

- **The mobile system** is a real system, from the factory, with no installer: parodies of the real ones
  ([Operating systems](operating-systems.md)). **Apps** are programs of the registry with a mobile platform, installed
  through the Mirror like an app store ([Programs](programs.md)). The basic ones: the network (a simpler version of the
  Network Interactor, to request, deposit, see the stock and craft anywhere an access point reaches), notifications
  (below), the Crypto Wallet ([Teracoin](teracoin.md)) and the Messenger.
- Portable devices have no "tabs": they run their mobile system and its apps.
- **Hardware:** the camera and GPS come from the factory on the smartphones and tablets of their era, as on the real
  ones. The **camera** takes a picture of what the player sees, at the resolution of the device's era, kept as an image
  in its gallery and sent to a computer like any file. The **GPS** gives the Maps app, drawn with J's Core's map
  markers. **Prospecting** is an app on the smartphones and tablets from the Standard on: it asks J's Core's world kit
  for the direction and the strength of the veins nearby, never their coordinates ([Survival](survival.md)). There is
  no extra reach: another access point, on its cable, is how wireless goes further.
- **The battery:** a portable runs on its battery, charged in the **Charging Dock** of its era on any energy; a charge
  lasts about two game days of use, less on a PDA (estimates).
- **A device's state** lives on the server, kept by the id the item carries. **Whoever loses the device loses what was
  on it:** when the item is destroyed (lava, fire, the void, despawning), the server erases its state and frees the
  room; and the state of a device that hasn't been in any inventory for 72 hours of the server running is erased too
  (`[wireless] lost_device_hours`).

With the portables comes the third family of systems and hardware, next to Frames and Linux: **Quince**, with its
computers, servers and Mainframe as well ([Quince](quince.md)).

### Notifications

The network sends **alerts** to the players' portable devices: an item below a threshold, an Operation that failed, a
degraded power supply ([Power](power.md)), a UPS running on battery, a network conflict. Each player chooses in the
notifications app which ones they get. By default they only reach the device; a setting of each player
(`jscomputers-client.toml`, `notifications_on_hud`) shows them on the HUD too. Who can see which alerts is decided with
security ([Security](security.md)).

Inside the desktops, the system's own notifications (the Action Center, the balloons) and the advancement toasts carry
on as they are.

### Cost

One index of access points per network, checked on a timer, never per player and per tick. Reach through space (the
Communication Satellites) belongs to J's Space ([The series](series.md)).
