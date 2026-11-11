---
section: library_readings
order: 457
---
# Wireless: the machine's Wi-Fi

A computer with a Wi-Fi card joins its network through an access point. A program can read how: whether it joined, through which access point, at which standard, and how fast the link carries. You read it through Wireless, a built-in type of System.Machine. Wireless came with Sigma# 3.

Table: Wireless members

| Member | What it gives |
| --- | --- |
| `Wireless.Joined` | Yes when the machine is on its network through an access point. |
| `Wireless.AccessPoint` | The access point's name, as its network lists it, or empty text when the machine has not joined one. |
| `Wireless.Standard` | The standard of the machine's Wi-Fi card, such as 802.11n, or empty text when it has no card. |
| `Wireless.Speed` | How many items a tick the link carries: the speed of the older of the card and the access point. It gives 0 when the machine has not joined. |
| `Wireless.Signal` | How strong the access point's signal is where the machine stands, from 100 beside it to 0 at the edge of its reach. It gives 0 when the machine has not joined. |

The signal only tells you where the machine stands: the link carries at full speed anywhere in the access point's reach.

A machine on a data cable has not joined by Wi-Fi, even with a card in it: the cable carries it.

> [!DEFINE] Access point
> A network device that gives the wireless network to the computers and the portable devices around it.

## If something goes wrong

> [!PROBLEM] Wireless.Joined gives no, though the machine has a card.
> No access point reaches it, or a data cable carries it instead.

> [!PROBLEM] Wireless.Standard gives empty text.
> The machine has no Wi-Fi card.

> [!SEE] jsc:wireless, jsc:computer_info
