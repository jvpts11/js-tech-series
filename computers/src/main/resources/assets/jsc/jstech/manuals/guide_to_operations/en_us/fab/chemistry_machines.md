---
section: fab
order: 39
icon: jsc:chemical_reactor_vessel
covers: [jsc:coke_oven_oven_chamber, jsc:coke_oven_oven_door, jsc:coke_oven_standpipe, jsc:chemical_reactor_vessel, jsc:chemical_reactor_agitator, jsc:air_separation_unit_compressor, jsc:air_separation_unit_vaporizer, jsc:air_separation_unit_adsorber, jsc:air_separation_unit_column]
---
# Chemistry and gases

The chemistry the chain asks for happens in these machines, and you feed them the liquids and gases they work with through their inlets.

The Coke Oven bakes coal into coke and coal tar, and needs no energy. The Chemical Reactor is the chain's chemistry: the acids, the resins and the plastics, alumina from aluminium ore, photoresist, cultured quartz and ultrapure water among them, through its two liquid inlets and its two gas inlets. The Air Separation Unit separates air into nitrogen, oxygen, argon and krypton, out of its gas outlets.

```table
{"caption": "Each machine's size, its step and its draw", "heads": ["Machine", "Size", "Step", "Draw"], "rows": [[{"words": "Coke Oven"}, {"words": "3 × 3 × 3"}, {"amount": 30, "unit": "s"}, {"amount": 0, "unit": "W"}], [{"words": "Chemical Reactor"}, {"words": "3 × 3 × 2"}, {"amount": 10, "unit": "s"}, {"amount": 2000, "unit": "W"}], [{"words": "Air Separation Unit"}, {"words": "3 × 3 × 4"}, {"amount": 10, "unit": "s"}, {"amount": 3000, "unit": "W"}]]}
```

> [!SEE] jsc:building_chain_machines, jsc:silicon_machines
