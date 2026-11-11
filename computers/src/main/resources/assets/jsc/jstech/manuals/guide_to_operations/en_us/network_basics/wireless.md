---
section: network_basics
order: 100
icon: jsc:standard_access_point
covers_family: jsc:wireless
---
# Wi-Fi: access points and Wi-Fi cards

From the Legacy era on, a small computer can join the network with no cable of its own. You put a Wi-Fi card in it, and set a Wireless Access Point on the network within its reach. The access point gives the network it is cabled to to every computer with a card round it.

> [!DEFINE] Wireless Access Point
> A network device that gives its network to the computers with a Wi-Fi card within its reach.

> [!DEFINE] Wi-Fi card
> An expansion card that joins a small computer to the network through an access point instead of a cable.

Table: The access points

| Era | Standard | Reach | Speed | Draw |
| --- | --- | --- | --- | --- |
| Legacy | 802.11b/g | 24 blocks | 4 items a tick | 10 W |
| Transition | 802.11n | 32 blocks | 16 items a tick | 12 W |
| Standard | 802.11ac | 48 blocks | 64 items a tick | 15 W |
| Advanced | Wi-Fi 6 | 64 blocks | 128 items a tick | 20 W |

## Setting up an access point

An access point takes two lines: the access cable of its era or an older one, and the energy cable. Both come in through one face. The Legacy's and the Transition's stand on a desk, facing you, and take their cables from behind. The Standard's and the Advanced's are fixed to a ceiling or a wall on their mounting plate, and take their cables through the face they are fixed to; take that block away and the access point comes off with it.

Its lamps tell how it stands: the power lamp while it has its energy, and the two traffic lamps blinking while it is on a network. Several access points in one place do not conflict: each computer joins the nearest one that reaches it, and an access point takes any number of computers.

## The cards

Each era has its Wi-Fi card: PCI on the Legacy, PCI Express later. It goes in a Personal Computer, a Crafting Computer or a Cluster Management Computer; the Mainframe and the racks stay on the backbone, by cable. The card draws nothing. A computer that has a cable keeps the cable for its link, card or no card.

A link goes at the speed of the older of its two ends, as real Wi-Fi does: a Standard card through a Legacy access point goes at 4 items a tick. Copies of files and songs over the network go no faster than that. The Network Manager lists each access point as an ACCESS POINT node on its cable, and shows the computer's link as Wi-Fi; pick the computer in the Devices list to read which end sets its speed ([](jsc:network_manager)).

## If something goes wrong

> [!PROBLEM] A computer with a card does not join the network.
> Move it within the access point's reach, or check that the access point has its energy and its cable: its power lamp is lit and its traffic lamps blink.

> [!PROBLEM] A fixed access point will not go where you click.
> It goes under a ceiling or on a wall, never on a floor, and the face it is fixed to must be solid.

> [!SEE] jsc:routers, jsc:data_cables, jsc:network_manager
