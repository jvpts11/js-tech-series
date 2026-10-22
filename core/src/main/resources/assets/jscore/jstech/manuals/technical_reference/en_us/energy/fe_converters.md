---
section: energy
order: 7
icon: jscore:configurable_energy_converter
covers: [jscore:fe_converter_lv, jscore:fe_converter_mv, jscore:fe_converter_hv, jscore:fe_converter_ehv, jscore:fe_converter_uhv, jscore:configurable_energy_converter]
---
# FE Converters

## What it is

Cabinets that turn the game's FE into J's Energy, or J's Energy into FE: one of each voltage class, and the Configurable Energy Converter, whose class you set.

## What it is for

Joining the series' lines to the machines and cables of any other mod, either way.

Table: The most each carries

| Property | Value |
| --- | --- |
| FE Converter LV | 100 kW |
| FE Converter MV | 10 MW |
| FE Converter HV | 200 MW |
| FE Converter EHV | 1.5 GW |
| FE Converter UHV | 12 GW |
| Configurable Energy Converter | Its class's |

## How to get it

Craft the low voltage one of iron, copper, redstone and gold, and each class above of the one below and dearer things. The configurable one is made from the ultra-high voltage one.

## How to use it

1. Facing its front, FE goes in or out on its left side and J's Energy on its right.
2. Turn the knob to choose the way: FE to JE, or JE to FE.
3. Set the most it moves on the thumbwheels: click a drum's upper half to turn it up, its lower half to turn it down, and the last drum for kilowatts, megawatts or gigawatts. It never goes above the class's ceiling.
4. On the configurable one, press a class key to set its class.
5. A joule is worth one FE unless the server sets otherwise; a converter rounds down, so it never makes energy.

## What can go wrong

> [!PROBLEM] The FAULT lamp is lit and nothing crosses.
> A line of another class meets its J's Energy side: set the class to the line's, or lay a line of the converter's class.

> [!PROBLEM] The LIMIT lamp blinks.
> More is offered than its limit lets through; turn the thumbwheels up, or use a converter of a higher class.
