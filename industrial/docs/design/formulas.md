# Formulas

Every formula of J's Industrial in one place, with the page where each one lives. All the numbers are estimates, to be
tuned in playtesting; the ones that can change without touching the code are settings
([Implementation](implementation.md)). The units are real: joules, watts, volts, kilograms, cubic metres. A block is one
cubic metre, and 1,000 mB are one cubic metre.

## Energy

| What | Formula | Page |
| --- | --- | --- |
| The rate between FE and J's Energy | 1 FE = 1 J, so 1 FE per tick = 20 W (a server setting) | [Energy](energy.md) |
| Loss along a way | each block of cable or wire loses its share of what it carries; the shares add up along the way, never past all of it | [Energy](energy.md) |
| A machine's speed with too little energy | its speed × the energy it gets ÷ the energy it asks for | [Energy](energy.md) |
| Real units | 1 kWh = 3.6 MJ; 1 MWh = 3.6 GJ | |
| Pumped storage | energy = 1,000 kg/m³ × water volume (m³) × 9.81 m/s² × height between the reservoirs (m): a thousand blocks of water 50 blocks up hold 490 MJ | [Energy](energy.md) |
| Evaporative cooling | each kilogram of water evaporated carries away 2.26 MJ of heat | [Machines](machines.md) |

## Machines

| What | Formula | Page |
| --- | --- | --- |
| Upgrade slots | 2 on a T1 machine, 3 on T2 and T3, 4 from T4 | [Machines](machines.md) |
| Speed with upgrades | 1 + 0.5 per Motor Upgrade + 1 per Overdrive (speeds add up) | [Machines](machines.md) |
| Energy with upgrades | (1 + 0.75 per Motor Upgrade) × 0.8 per Variable Frequency Drive × 2 per Overdrive (savings multiply) | [Machines](machines.md) |
| Recipes at once | 1 + 1 per Parallel Line + 2 per Advanced Parallel Line; the energy grows in the same proportion | [Machines](machines.md) |

## Fluids

| What | Formula | Page |
| --- | --- | --- |
| A liquid tank | its inner volume in blocks × 1,000 mB | [Fluids and gases](fluids-and-gases.md) |
| A gas tank | its inner volume × its pressure in bar × 1,000 mB (the gas law) | [Fluids and gases](fluids-and-gases.md) |
| A run of pipes | carries up to its worst pipe's top flow; no loss and no limit by length | [Fluids and gases](fluids-and-gases.md) |
| Oil in reservoir rock | 200 mB per block (real reservoir rock is about 20% pores) | [Chemistry](chemistry.md) |
| Air's main shares | 78.08% nitrogen, 20.95% oxygen, 0.93% argon, 0.04% carbon dioxide; the rare gases at a thousand times their real share | [Fluids and gases](fluids-and-gases.md) |

## Chemistry

| What | Formula | Page |
| --- | --- | --- |
| A reaction's amounts | the coefficients of its real equation: gases and liquids in mB, solids in dusts (N₂ + 3 H₂ → 2 NH₃: 100 mB and 300 mB give 200 mB) | [Chemistry](chemistry.md) |
| Rare earth cascade | 4 stages separate the light rare earths, 8 the middle ones, 16 the heavy ones | [Chemistry](chemistry.md) |

## Chips

| What | Formula | Page |
| --- | --- | --- |
| Chips per wafer | by the wafer's area over the die's: a 300 mm wafer gives about 40 times the chips of a 50 mm one | [Clean Room](clean-room.md) |
| Yield | the share of good chips falls as the room's particle count rises | [Clean Room](clean-room.md) |

## Science and nuclear

| What | Formula | Page |
| --- | --- | --- |
| A research group | the first CRC's power + 90% of each other CRC's | [Science](science.md) |
| A synchrotron's energy | grows with its magnets' field × its ring's radius | [Science](science.md) |
| Decay heat | about 7% of a reactor's power just after shutdown, falling over hours | [Nuclear](nuclear.md) |
| Enrichment | power reactors 3 to 5% uranium-235, research reactors up to 20%, nothing above | [Nuclear](nuclear.md) |
| Shielding | radiation crossing a block is weakened by its material: lead most, then concrete and stone, then water; air hardly at all | [Nuclear](nuclear.md) |

## Transcendence

| What | Formula | Page |
| --- | --- | --- |
| Annihilation | 1 mg of antimatter with 1 mg of matter releases 180 GJ (E = mc²) | [Exotic materials](exotic-materials.md) |
| Energy per unit of an anti-element | grows with its mass: antihelium 4 times antihydrogen, anticarbon 12, anti-iron 56, antilead 207 | [Exotic materials](exotic-materials.md) |
| The Singularity Reactor | up to 40% of the mass it is fed, as energy | [Exotic materials](exotic-materials.md) |
| The Dark Energy Collector | its power grows with the volume its frame encloses | [Exotic materials](exotic-materials.md) |
| Dissonance | builds up faster the further the local laws are from ours, and falls in normal laws | [The Aleph](aleph.md) |
