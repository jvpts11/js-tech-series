---
section: fab
order: 41
icon: jsc:ion_implanter_analyzer_magnet
covers: [jsc:plasma_etcher_etch_chamber, jsc:ion_implanter_end_station, jsc:ion_implanter_analyzer_magnet, jsc:deposition_chamber_sputter_chamber, jsc:packaging_line_mould_press, jsc:packaging_line_wire_bonder]
---
# The chip factory

A chip is made layer by layer on its wafer: a Lithography Station exposes each layer ([](jsc:lithography_stations)), and you take the wafer through these machines for the rest.

The Plasma Etcher etches the exposed layer, with a liquid and two gases. The Ion Implanter dopes the layer. The Deposition Chamber lays metal on it, and the magnetic layer of platters and floppy film, the chromium of a mask blank and the gold electrodes of oscillator crystals. The Packaging Line cuts a finished wafer into its dies and packages each: in ceramic in the Vintage, in epoxy with pins later, on solder balls from the Transition.

```table
{"caption": "Each machine's size, its step and its draw", "heads": ["Machine", "Size", "Step", "Draw"], "rows": [[{"words": "Plasma Etcher"}, {"words": "2 × 2 × 2"}, {"amount": 10, "unit": "s"}, {"amount": 3000, "unit": "W"}], [{"words": "Ion Implanter"}, {"words": "3 × 2 × 2"}, {"amount": 10, "unit": "s"}, {"amount": 4000, "unit": "W"}], [{"words": "Deposition Chamber"}, {"words": "2 × 2 × 2"}, {"amount": 10, "unit": "s"}, {"amount": 3000, "unit": "W"}], [{"words": "Packaging Line"}, {"words": "3 × 2 × 2"}, {"amount": 20, "unit": "s"}, {"amount": 2000, "unit": "W"}]]}
```

> [!SEE] jsc:building_chain_machines, jsc:lithography_stations
