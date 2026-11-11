---
section: library_readings
order: 455
---
# Power: the draw, the supply and the UPS

A program can read what its machine takes from the line, the power supply that feeds it, and the UPS that carries it. You read them through Power, a built-in type of System.Machine, the same way you read Computer. Power came with Sigma# 3.

Table: Power members

| Member | What it gives |
| --- | --- |
| `Power.DrawWatts` | What the machine takes from the line now, in watts (W): what its parts draw, plus what its supply loses. It gives 0 while the machine is off. |
| `Power.Supply` | The power supply, as a SupplyInfo record, or null when the machine has none. |
| `Power.Ups` | The UPS that carries the machine, as a UpsInfo record, or null when no UPS does. |

Table: Records of Power

| Record | Fields | What each field holds |
| --- | --- | --- |
| `SupplyInfo` | `SupplyInfo.Name, SupplyInfo.RatingWatts, SupplyInfo.Units, SupplyInfo.UnitsFailed, SupplyInfo.Degraded, SupplyInfo.Failed` | Name: the supply's name. RatingWatts: the most it gives, in watts. Units: how many units it has, more than one for a redundant supply. UnitsFailed: how many of them have failed. Degraded: yes when a unit is worn or has failed and the rest carry on. Failed: yes when every unit has failed. |
| `UpsInfo` | `UpsInfo.Name, UpsInfo.OnBattery, UpsInfo.Bypass, UpsInfo.ChargePercent, UpsInfo.MinutesLeft` | Name: the UPS's name. OnBattery: yes when the line is gone and the battery carries the machine. Bypass: yes when the machine draws more than the UPS carries, so it runs straight from the line, unprotected. ChargePercent: how full the battery is, from 0 to 100. MinutesLeft: how long the battery lasts at this draw. |

A server in a rack reads its own supply, and the rack's UPS, which carries every server in the rack.

```sgs Power.sgs
UpsInfo ups = Power.Ups;
if (ups != null && ups.OnBattery && ups.MinutesLeft < 5) {
    Console.PrintLine("Saving: the battery is almost empty");
}
```

> [!DEFINE] UPS
> Uninterruptible power supply: a battery that carries a machine when the line goes down.

> [!DEFINE] Redundant supply
> A power supply made of several units, where the others carry on when one fails.

## If something goes wrong

> [!PROBLEM] Power.Ups gives null.
> No UPS carries the machine. A UPS block stands against the computer; a rack takes a Rack UPS.

> [!PROBLEM] Power.DrawWatts gives 0.
> The machine is off, or it has no build yet.

> [!SEE] jsc:power_supplies, jsc:upses, jsc:computer_info
