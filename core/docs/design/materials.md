# Materials

The shared catalogue of what the world is made of: elements, isotopes, alloys, minerals, compounds and the exotic forms
of matter, and the liquids, gases and generic components more than one mod uses. It lives in J's Core so that every mod
of the series, and any other mod, agrees on what copper dust or a lithium ingot is, and none has to depend on another to
trade it. Which minerals exist, where each element comes from and what each is for are J's Industrial's to say, in its
own design; this page is the system they all live in.

## What exists today

A closed catalogue of five metals (iron, copper, gold, tin and bronze) in seven forms (ingot, nugget, dust, plate, bolt,
rod and gear). The Core registers the forms the game lacks as they are needed (today iron dust, iron plate and copper
plate, under `jscore`), and every form is in the common tag other mods use, `c:<forms>/<material>`, so a recipe that
asks for `c:dusts/iron` takes iron dust from any mod ([Content](../CONTENT.md#materials)). Forms the game already has,
such as the iron ingot, stay vanilla's.

## To build

### Materials are registries

The closed lists give way to **registries**, open to any mod, each entry with an id (`namespace:path`):

- **Materials.** J's Core declares the real catalogue (elements, alloys, minerals, compounds) and any mod adds its own.
  A material has a **kind**, set by its state at room temperature (metal, crystal, gem, non-metal solid, mineral,
  liquid, gas, exotic matter), and the kind says which forms make sense. Any material may also take another state:
  molten metals are liquids, steam is a gas, a fusion fuel becomes a plasma ([States of matter](states-of-matter.md)).
- **Forms**: the base set is ingot, nugget, block, dust, plate, sheet, wire, rod, bolt, gear, coil, spring, foil, frame,
  lens, boule, wafer, crystal, gem, ore and raw; a mod adds others when it has a real use for them.
- **Properties**: the data a material carries, **real data, never game rules**. The Core declares a few (symbol, atomic
  number, formula, colour, state at room temperature); a mod registers its own (molar mass, oxidation states...) and
  gives values in code or from a data pack. Each property says whether its tooltip line shows always, only with Shift,
  or never, and that can be changed material by material.
- **Mineral families** (sulfide, oxide, carbonate, silicate, arsenide, native metal, halide, sulfate, phosphate...): the
  chemistry that decides how a mineral is processed.
- **Host rocks**: the rocks ores sit in. The Core knows vanilla's (stone, deepslate, granite, diorite, andesite, tuff,
  calcite, sandstone, netherrack, basalt, blackstone, end stone); a mod that brings limestone or marble adds them.

**A form exists only if an installed mod declares it uses it.** Mods declare their needs while the game starts, and the
Core registers the union, each in its common tag. A material has **no tier**: when it becomes available is decided by
each mod's recipes.

### Minerals and ores

- A **mineral** is what comes out of the ground (chalcopyrite, not copper). It knows its contents (the main element and
  fixed byproducts, in fixed proportions), its family, and the host rocks its veins can cross.
- **Ore blocks are a form of a material.** A mineral has an ore variant in **every rock its veins can cross**, each
  textured from the rock with the mineral's overlay, generated, never drawn by hand; every variant drops the same raw
  item. Block count is not a concern.
- **Vanilla ores stay vanilla**, with their ids, so mods that ask for vanilla's iron ore keep working. Every element
  mined as such has its generic **"X ore"** (vanilla's for iron, copper and gold, a Core block in `c:ores/<element>` for
  the others) besides its real minerals; elements that only come as byproducts, the rare earths and the elements whose
  ores are salts have none.
- Every mineral with uranium or thorium in it is **naturally radioactive**, at the lowest level of the Core's radiation
  ([Hazards](hazards.md)), and a setting turns that off.

Where ores lie, and how veins are made, is in [The world](world.md).

### Elements and isotopes

- **All 118 elements** are in the catalogue as data; their forms exist only when a mod uses them.
- **Isotopes** are materials of their own, tied to their element with a mass number (uranium-235 and -238, plutonium-238
  and -239, deuterium, tritium, helium-3, calcium-48...); any mod adds those it needs.
- **The superheavy elements beyond 118** (119 to 250), the series' bet on the island of stability, are elements of the
  catalogue like any other, with names after scientists, places and ideas (Genevium, Teslium... Aeternium). Before
  anyone discovers one it shows under its systematic name (ununennium for 119), as the periodic table does today.

### Naming what is discovered

On a server, the player who first discovers an element beyond 118 **gives it its name and its symbol**, and the element
and all its items take them, for everyone and in every language: a name is a name.

- The name is kept with the world and sent to every player.
- It has a maximum length, letters only, and repeats no other element's name; the symbol has one or two letters and
  repeats none. A setting can require the "-ium" ending; it is off by default.
- Once given, a name is not changed; an operator can undo one with a command, for an offensive name.
- With the setting off, the series' names are used. The elements up to 118 keep their real names forever.

What counts as discovering an element is the mod's that makes it: J's Industrial's first synthesis
([Progression](progression.md)).

### Exotic forms of matter

A kind of material of their own, from real physics, observed or predicted: metallic hydrogen, neutronium, quark-gluon
plasma, strange matter, dark matter, Bose-Einstein condensate, negative-mass exotic matter, degenerate matter, time
crystals, photonic matter and computronium. Each has the state it calls for; most can only be kept in **containment**
([States of matter](states-of-matter.md)). J's Industrial makes them, J's Space finds some in nature, and J's Computers'
cosmological simulator makes them from a simulated universe; all three trade the same materials.

### The shared catalogue of fluids and components

The catalogue also holds what is not a material but is shared all the same:

- **common liquids and gases**: fuels (gasoline, kerosene, diesel), hydrogen, oxygen, nitrogen, helium, xenon,
  propellants, and the like;
- **generic components**: servomotors, electric motors, LEDs, sensors, oscillators, antennas, magnetic heads, and the
  like.

They are **items and fluids with no game of their own**. Each mod that makes one gives its recipe: J's Industrial the
full industrial chain, and a mod used without J's Industrial a simpler path of its own; with J's Industrial installed,
that mod's server rule, off by default, can make the industrial chain the only path. An entry exists only when an
installed mod declares it uses it.

### The J's Tech creative tab

The Core has one creative tab, **"J's Tech"**, with only what the installed mods use, in three sections: the
**materials**, the **fluids and components** of the shared catalogue, and the **shared blocks and tools** (the basic
lines, battery and wrench, the saw, the Configuration Card, the FE converters, the Space Persistor, the Atmosphere
Detector, the containment of exotic matter, the Structure Projector). Nothing of it is repeated in the mods' own tabs,
which keep their own content: J's Industrial's shows its machines, not the materials.
