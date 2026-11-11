---
section: library_readings
order: 461
---
# Signals: radios and the frequencies heard

J's Core sends signals without wire by frequency: a transmitter sends a value from 0 to 15, and every receiver on its frequency gives it out. A program reaches a radio joined to its machine the way it reaches a sensor: through a Redstone Interface linked to the machine, with its lens toward the radio. You use them through Signals, a built-in type of System.Machine. Signals came with Sigma# 3.

Table: Signals members

| Member | What it does |
| --- | --- |
| `Signals.Radio(name)` | The radio in front of the interface with that name, as a RadioInfo record, or null when no interface has that name or it faces no radio. |
| `Signals.Send(name, value)` | Sends the value, from 0 to 15, through the transmitter in front of the interface with that name, by setting the interface to emit it. It gives no when the interface faces no transmitter, or is not live. |
| `Signals.Heard()` | Every frequency heard where the machine stands, as a list of HeardSignal records, as a signal probe there would show them. |

Table: Records of Signals

| Record | Fields | What each field holds |
| --- | --- | --- |
| `RadioInfo` | `RadioInfo.Role, RadioInfo.Band, RadioInfo.Frequency, RadioInfo.Value, RadioInfo.Clash` | Role: transmitter, receiver or relay. Band: whose frequencies it is on. Frequency: its number. Value: what a transmitter sends, or what a receiver hears. Clash: yes when two transmitters of one owner send on its frequency. |
| `HeardSignal` | `HeardSignal.Band, HeardSignal.Frequency, HeardSignal.Bars, HeardSignal.Value, HeardSignal.Clash, HeardSignal.Relayed` | Band and Frequency: which frequency it is. Bars: how well it arrives. Value: what it carries. Clash: yes when it carries nothing for a clash. Relayed: yes when it arrives through a relay. |

Send is the same as Redstone.Out on that interface: the transmitter sends what the lens gives it. The last setting wins, whichever call made it.

> [!DEFINE] Frequency
> A numbered line a transmitter sends one value on, from 1 to 9999.

> [!DEFINE] Clash
> Two transmitters of one owner on the same frequency. Its receivers give out nothing until one of them is taken off.

## If something goes wrong

> [!PROBLEM] The program stops and says no Redstone Interface with that name is linked to this machine.
> Link the interface to the machine with cable, and check its name in its window.

> [!PROBLEM] Signals.Send gives no.
> The interface's lens does not face a transmitter, or the interface is not powered.

> [!SEE] jsc:redstone_interfaces, jsc:redstone_library
