# Multipart

A kit for block spaces that hold several things at once: cables, covers, panels, buses, a torch, a microblock of any
material, in one kit any mod uses. It is in the spirit of CB Multipart and the mods built on it, AE2's facades, Framed
Blocks and Chisels and Bits.

## What exists today

Inside the cable block only: thin **parts** on the block's faces (a bus, a cover, a sensor), each a kind registered by
any mod, saving, ticking and answering a player on its own, beside the bundle of wires running through the block's
middle; a part on a face closes that face to wires ([Cables](../CABLES.md#parts-on-the-cable)).

## To build

### A block space with many occupants

- **Any block space can hold several parts**, not only the cable block. A space has **places**: the **centre** (where
  the cable bundle runs), the **six faces** (covers, panels, buses), the **twelve edges** (thin posts) and the **eight
  corners**.
- **Vanilla's small blocks share the space**: a torch, a lever, a button or a pressure plate sits beside a cable, with
  no block of its own.

### Microblocks

- **Microblocks of the material of any full block, from any mod**: covers (an eighth thick), panels (a quarter), slabs
  (a half) and the thicknesses between; corners and posts; and **hollow covers**, with a hole for a cable to pass.
- **Facades**: a cover that **hides the cable behind it** and looks like a whole block, while the cable keeps working,
  as AE2's do, so cables run inside the walls of a handsome base.
- **The saw** cuts microblocks: a tool of the Core's shared content, with a recipe of vanilla materials.

### What a part can do

- **Its state, its save, what it sends to players, its model, its collision and selection boxes.** A click hits exactly
  the part under the crosshair, and breaking takes only that part, with its drop.
- **Per part and per face**: redstone in and out, light, capabilities (items, fluids, energy and the rest), a comparator
  signal, a menu, and a tick only when it needs one.
- **Occlusion**: a part can't go where it overlaps another; a cover cuts the cable's link on its face, and a hollow
  cover lets it through.

### Drawing and cost

- **One model per space**, joining its parts, rebuilt only when a part changes, hiding the faces parts cover in each
  other and in their neighbours, with ambient occlusion; parts that move are drawn on their own, only when there are
  any.
- **One block entity per space**, with compact storage.

### Integration

- **Parts are declared once**, like blocks: the data generation writes their models, items and recipes, the microblocks'
  for every material included.
- **Jade** shows the part looked at; **JEI and EMI** show the microblocks' recipes **compacted** into one entry, not
  thousands.
- **The wrench** turns or takes off a part.
- **Multipart spaces travel in moving structures** like any block ([Moving structures](moving-structures.md)).
- **A testing kit** for GameTests of multipart spaces.
