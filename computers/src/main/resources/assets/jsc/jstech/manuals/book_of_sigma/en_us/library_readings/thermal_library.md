---
section: library_readings
order: 456
---
# Thermal: how hot the machine runs

A program can read its machine's heat: how hot it is, the heat its parts make, the heat its cooling takes away, and how much of its clock the heat leaves it. You read it through Thermal, a built-in type of System.Machine. Thermal came with Sigma# 3.

Table: Thermal members

| Member | What it gives |
| --- | --- |
| `Thermal.Known` | Yes when the machine has a temperature of its own. When it gives no, every reading below gives its resting value. |
| `Thermal.Celsius` | How hot the machine is now, in degrees Celsius (°C). |
| `Thermal.AmbientCelsius` | How hot its surroundings are, in °C. |
| `Thermal.HeatWatts` | The heat its parts make, in watts. |
| `Thermal.BudgetWatts` | Its thermal budget: the heat its cooling takes away, in watts. |
| `Thermal.ClockPercent` | The share of its clock the heat leaves it, from 25 to 100. Programs run at this share. |
| `Thermal.Throttled` | Yes while the machine slows its clock to hold 90 °C. |
| `Thermal.LastTripCelsius` | How hot it was when heat last shut it down, in °C, or 0 when heat never did. |

A machine whose parts make more heat than its budget gets hotter until it throttles. A server in a rack reads the rack's heat, which every server in it shares.

```sgs Heat.sgs
if (Thermal.Throttled) {
    Console.PrintLine("Too hot: running at " + Thermal.ClockPercent + "%");
}
```

> [!DEFINE] Thermal budget
> The heat a machine's cooling takes away, in watts.

> [!DEFINE] Throttling
> Slowing the clock to make less heat, so the machine stays at a safe temperature.

## If something goes wrong

> [!PROBLEM] Thermal.Known gives no.
> The machine has no temperature of its own; something outside it cools it.

> [!PROBLEM] Thermal.Throttled stays yes.
> The parts make more heat than the cooling takes away. Fit a better cooler or more fans.

> [!SEE] jsc:cooling, jsc:rack_cooling_and_heat, jsc:computer_info
