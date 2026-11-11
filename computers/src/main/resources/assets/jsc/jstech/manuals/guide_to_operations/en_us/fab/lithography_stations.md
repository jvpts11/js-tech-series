---
section: fab
order: 42
icon: jsc:lithography_wafer_stage
covers: [jsc:lithography_wafer_stage, jsc:lithography_glass_optics, jsc:lithography_mercury_lamp, jsc:lithography_krf_laser, jsc:lithography_arf_laser, jsc:lithography_fluoride_optics, jsc:lithography_euv_source, jsc:lithography_mirror_optics]
---
# The Lithography Stations

A Lithography Station exposes a wafer's layer through its mask. Each era has its own, the same body lit its era's way: the Vintage's mercury lamp, the Legacy's krypton fluoride laser, the argon fluoride laser with immersion of the ArF Lithography Station, which works both the Transition's recipes and the Standard's, and the Advanced's extreme ultraviolet source with its mirrors. You give a laser its gas through the Gas Inlet, and every station its liquid through the Liquid Inlet.

From the Legacy on, a station works only linked to a computer: lay a [peripheral cable](jsc:peripheral_cables) from its Peripheral Port to a computer of the era before the recipe's, or of a later one, switched on and running. The computer runs the exposure as its litho process and holds memory for it while it does, which you see among its processes. A computer two eras newer than the one asked for exposes in half the time.

```table
{"caption": "What the recipes of each era ask for", "heads": ["Recipes of", "Computer", "Memory", "Draw"], "rows": [[{"words": "Vintage"}, {"words": "none"}, {"words": "none"}, {"amount": 2000, "unit": "W"}], [{"words": "Legacy"}, {"words": "Vintage or later"}, {"amount": 2, "unit": "MB"}, {"amount": 3000, "unit": "W"}], [{"words": "Transition"}, {"words": "Legacy or later"}, {"amount": 32, "unit": "MB"}, {"amount": 4000, "unit": "W"}], [{"words": "Standard"}, {"words": "Transition or later"}, {"amount": 256, "unit": "MB"}, {"amount": 6000, "unit": "W"}], [{"words": "Advanced"}, {"words": "Standard or later"}, {"amount": 2048, "unit": "MB"}, {"amount": 10000, "unit": "W"}]]}
```

```table
{"caption": "Each station's size and its step", "heads": ["Station", "Size", "Step"], "rows": [[{"words": "Vintage Lithography Station"}, {"words": "3 × 3 × 2"}, {"amount": 20, "unit": "s"}], [{"words": "Legacy Lithography Station"}, {"words": "3 × 3 × 2"}, {"amount": 20, "unit": "s"}], [{"words": "ArF Lithography Station"}, {"words": "3 × 3 × 2"}, {"amount": 20, "unit": "s"}], [{"words": "Advanced Lithography Station"}, {"words": "3 × 3 × 2"}, {"amount": 20, "unit": "s"}]]}
```

## If something goes wrong

> [!PROBLEM] The station stands formed but exposes nothing.
> Its computer is off, of an older era than the recipe asks for, not linked to its Peripheral Port, or short of the memory the exposure holds.

> [!SEE] jsc:building_chain_machines, jsc:chip_factory_machines
