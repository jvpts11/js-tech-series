# Wireless

Wireless is an essential part of the mod, like the cables: it can't be turned off ([Overview](overview.md)). It starts
in the Legacy, when Wi-Fi arrives; the Vintage has no wireless. Nothing on this page exists yet.

## To build

### Access points

The **Wireless Access Point** (WAP) is a network device, linked by a data cable, that draws energy and gives the
wireless network to the portable devices and to the computers with a Wi-Fi card around it. There is one per era: Legacy
(802.11b/g), Transition (802.11n), Standard (802.11ac) and Advanced (Wi-Fi 6), each with its own reach and speed, like
the cables (estimates).

Several access points in the same area don't conflict: each device joins the nearest one, and an access point has no
limit of devices.

### Wi-Fi cards

An expansion card per era (PCI in the Legacy, PCI Express later) that joins a computer to the network through an access
point instead of a cable, at the access point's speed.

### Portable devices

The PDA, the Smartphone and the Tablet follow real history, each per era: the PDA from the Legacy on (the Pocket PC with
Wi-Fi), the Smartphone from the Transition on (2007) and the Tablet from the Standard on (2010). Each has its own
hardware (the system on a chip, the memory, the storage), shown in the item's window, and joins the network through an
access point.

- **The mobile system** is a real system, from the factory, with no installer: parodies of the real ones
  ([Operating systems](operating-systems.md)). **Apps** are programs of the registry with a mobile platform, installed
  through the Mirror like an app store ([Programs](programs.md)). The basic ones: the network (a simpler version of the
  Network Interactor, to request, deposit, see the stock and craft anywhere an access point reaches), notifications
  (below), the Crypto Wallet ([Teracoin](teracoin.md)) and the Messenger.
- Portable devices have no "tabs": they run their mobile system and its apps.
- **Hardware:** the camera and GPS come from the factory on the smartphones and tablets of their era, as on the real
  ones. Prospecting and extra reach are decided when wireless is built, as apps or internal parts, never as side card
  slots.
- **A device's state** lives on the server, kept by the id the item carries. **Whoever loses the device loses what was
  on it:** when the item is destroyed (lava, fire, the void, despawning), the server erases its state and frees the
  room; and the state of a device that hasn't been in any inventory for a configurable time is erased too.

With the portables comes a third family of systems and hardware, next to Frames and Linux
([Operating systems](operating-systems.md)).

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
