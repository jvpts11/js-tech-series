# Materials

What the world of the series is made of: elements, minerals, alloys, isotopes, the superheavy elements beyond the known
table and the exotic forms of matter, and how they all live in one catalogue that every mod shares. The ores themselves,
one by one, are in [Minerals](minerals.md); where they lie in the world is in [The world](world.md).

## One catalogue for the whole series

J's Industrial does not own its materials. They live in **J's Core's shared catalogue**, so every mod of the series, and
any other mod, agrees on what copper dust or a lithium ingot is. J's Industrial gives them their industrial recipes; a
mod used without J's Industrial makes the ones it needs by a simpler path of its own. The same goes for the liquids and
gases more than one mod uses (diesel, kerosene, hydrogen, helium, xenon...) and for generic components (servomotors,
electric motors, LEDs, antennas, magnetic heads...).

Today the catalogue holds five metals (iron, copper, gold, tin and bronze), in seven forms (ingot, nugget, dust, plate,
bolt, rod and gear), and the Core registers three items of them: iron dust, iron plate and copper plate. Everything else
on this page is still to build.

## To build

### Materials are a registry

The catalogue becomes a **registry**: every material has an id (`namespace:path`). J's Core declares the real-world
catalogue (elements, alloys, minerals, compounds), and any mod can add its own.

A material is of a **kind**, decided by its **state at room temperature**, and its kind decides which forms make sense:

| Kind | Examples | Forms |
| --- | --- | --- |
| Metal (element or alloy) | copper, titanium, steel, NbTi | ingot, nugget, block, dust, plate, sheet, wire, rod, bolt, gear, coil, spring, foil |
| Crystal | quartz, monocrystalline silicon, fluorite | crystal, dust, block, lens, boule, wafer |
| Gem | ruby, sapphire, garnet, topaz | gem, dust, block |
| Non-metal solid | sulfur, graphite, phosphorus, salt | dust, block |
| Mineral (what comes out of the ground) | chalcopyrite, monazite, cassiterite | ore (one variant per host rock) and the raw item, like vanilla raw iron |
| Liquid | mercury, bromine, oil | the liquid |
| Gas | hydrogen, nitrogen, argon | the gas |
| Exotic matter | metallic hydrogen, neutronium | decided with each one |

- **Any material can also take another state**: molten metal and liquid gallium (the form used to grow gallium arsenide)
  are liquids, steam is a gas, a fusion reactor's fuel becomes a plasma.
- **Gallium** is a metal with an ingot: it is solid at 20 °C and melts at 29.8 °C. Melting in the hand in a hot biome is
  an optional touch.
- **Mercury** only exists as a liquid; its mineral, cinnabar, is its ore. **Native mercury** is droplets of liquid
  mercury inside its ore, which come out as liquid when the block is broken.
- **Forms are a registry too**, open to any mod. Forms added when a real use appears; the frame (the casing of
  multiblocks), lenses, boules and wafers are already in the base set. Whether metals get scraps, and whether ores go
  through crushed and purified stages, is decided with [Metallurgy](metallurgy.md) and the machines.
- **A form only exists if an installed mod declares it uses it.** Mods declare their needs while the game starts, and
  the Core registers the union. Every form carries the common tag `c:<forms>/<material>` (`c:dusts/copper`,
  `c:plates/titanium`), so a recipe that asks for the tag takes the item from any mod. Forms the game already has (the
  iron ingot, raw iron) stay vanilla's.

### States of matter

J's Core keeps the **states of matter apart**, instead of the usual "items and fluids", where "fluid" ends up meaning
everything that is not an item:

| State | How it is kept and moved | How it behaves |
| --- | --- | --- |
| Solid (item) | inventories, conveyors | as always |
| Liquid | tanks, pipes | it stays NeoForge's fluid, so every mod's tanks and pipes work with it |
| Gas | pressurised tanks, pressure pipes | it rises and is lost if released; a capability of J's Core |
| Plasma | magnetic containment | it needs containment and energy, and dissipates without them; a capability of J's Core |
| Supercritical fluid | pressure vessels | neither liquid nor gas, like the supercritical carbon dioxide of aerogel making |
| Slurry | tanks, pipes | solids suspended in a liquid: drilling mud, dissolved ores |

Exotic matter has whatever state each form calls for. Other mods' gases (fluids tagged as gases, Mekanism's gases) come
in through bridges on the gas side, so none is left out. Every state is a kind of storage of its own, wherever things
are stored, the J's Computers network included.

### What a material knows

A material keeps **real data, never game rules**. The data are a registry of **properties**: J's Core declares a few
(symbol, atomic number, formula, colour, state at room temperature), and any mod registers its own (molar mass,
oxidation states, electron configuration...) and gives values to the materials, in code or from a data pack. Each
property says whether it shows in the tooltip, only with Shift, or never, and that can be changed material by material.
J's Industrial registers nothing beyond the basics; a mod of ultra-realistic chemistry can add everything it wants
without touching J's Core.

A material has **no tier**: when something becomes available is decided by each mod's recipes.

### Minerals

A **mineral** is what comes out of the ground: chalcopyrite, not copper. It knows what it contains (its main element and
the byproducts it gives, in fixed proportions) and the **family** that decides how it is processed:

| Family | How it is processed | Examples |
| --- | --- | --- |
| Sulfide | roasted, which gives sulfur dioxide and from it sulfuric acid | chalcopyrite, galena, sphalerite |
| Oxide | reduced | hematite, cassiterite, chromite |
| Carbonate | calcined, which gives carbon dioxide | malachite, cerussite, smithsonite |
| Silicate | harder to break down | hemimorphite, zircon, beryl |
| Arsenide | gives arsenic, which is toxic | cobaltite, nickeline, arsenopyrite |
| Native metal or natural alloy | needs no breaking down | electrum, native bismuth |
| Halide, sulfate, phosphate, arsenate, vanadate | each by its own chemistry | halite, barite, apatite, vanadinite |

How each family is processed is decided with the machines and [Chemistry](chemistry.md).

### Ores: vanilla's and the series'

Mods that ask for vanilla's iron ore by its id, not by its tag, must keep working, so **vanilla ores stay vanilla**,
with their vanilla ids. GregTech replaces the iron ore with one of its own, and that is exactly the incompatibility the
series avoids.

- Every element has its **"X ore"**: vanilla's for iron, copper and gold (the vanilla blocks themselves), and a block of
  J's Core (`c:ores/tin`) for the others.
- **And** every element has its **real minerals** as ore blocks of their own: hematite, magnetite, siderite... for iron.
- A vein holds its minerals **and** the "X ore" of their elements ([The world](world.md)). A vein of hematite has both
  hematite and iron ore.
- Coal, redstone, lapis, diamond, emerald, Nether quartz and ancient debris stay exactly as vanilla has them.
- The Overworld also gets a **quartz ore**. "Pure quartz" is what refining makes, not an ore.

Some elements have **no "X ore"**, because in nature nobody mines them as such:

- the elements that only come as **byproducts** (gallium, germanium, indium, cadmium, selenium, tellurium, hafnium,
  rubidium and the platinum metals other than platinum): their rare real minerals exist, but their main source is the
  byproduct;
- the **rare earths**: their minerals carry a **mixture**, light (lanthanum, cerium, praseodymium, neodymium, samarium,
  europium, gadolinium) or heavy (yttrium, terbium, dysprosium, holmium, erbium, thulium, ytterbium, lutetium),
  separated by chemistry, with research ([Science](science.md));
- **sodium, potassium and calcium**, whose ore *is* their salts and carbonates (vanilla's calcite is calcium carbonate);
- **phosphorus, fluorine, bromine, iodine and nitrogen**, whose ores are phosphates, fluorides and salts, and, for
  nitrogen, the air.

**Radioactive minerals:** every mineral with uranium or thorium in it gives off the natural Rad I of J's Core's
radiation: uraninite, carnotite, monazite, samarskite and the rest. A setting of J's Core turns the minerals'
radioactivity off.

### Host rocks

The rocks ore sits in are a **registry** too. J's Core knows vanilla's: stone, deepslate, granite, diorite, andesite,
tuff, calcite, sandstone, netherrack, basalt, blackstone and end stone. Limestone, marble and the rest come in when a
mod brings them (J's Overworld, J's Geology), and minerals then occur in them as well.

A mineral has ore variants in **every rock its veins can cross**, and **a vein can cross several rocks**: each ore block
takes the variant of the rock it replaced ([The world](world.md)). Every variant of a mineral drops the same raw item.
The textures are generated: the rock's own texture with the mineral's overlay on top, so no combination is drawn by
hand. Block count is not a concern.

### All the elements, and their isotopes

The catalogue holds **all 118 elements** as data, and their forms exist only when a mod uses them. **Isotopes** are
materials of their own, tied to their element, with their mass number: uranium-235 and -238, plutonium-238 and -239,
calcium-48, zinc-70, cobalt-60, strontium-90, iridium-192, thulium-170, deuterium, tritium, helium-3, and the stable
isotopes of the superheavy elements (below). Any mod adds the ones it needs.

**Every element J's Industrial produces has at least one real use** in some recipe of the series. The sources and uses
that had no home yet:

| Where it is decided | Elements and their real use |
| --- | --- |
| [Metallurgy](metallurgy.md) | scandium (aluminium-scandium alloy), beryllium (beryllium copper: springs, tools that make no sparks), magnesium (the reducer of the Kroll process for titanium), vanadium and rhenium (steels, superalloys), neodymium, praseodymium, dysprosium, terbium (NdFeB magnets), samarium (SmCo magnets), strontium and barium (ferrite magnets), limestone as the blast furnace's flux |
| [Energy](energy.md) | vanadium (the vanadium flow battery, an energy bank), cadmium (NiCd batteries), lanthanum (NiMH batteries), antimony (the lead of lead-acid batteries) |
| [Chemistry](chemistry.md) | rhenium and platinum (the reformer's catalysts), palladium and rhodium (catalysts), bromine (flame retardants, drilling fluids), caesium (caesium formate drilling fluid), mercury (rectifiers, cells), thallium (a byproduct of roasting sphalerite and pyrite) |
| [Clean Room](clean-room.md) | germanium (optical fibre, infrared lenses), gallium (gallium arsenide, the gallium nitride of blue LEDs), indium (ITO), arsenic, hafnium (transistor gates), cerium (the polishing powder of chemical-mechanical planarisation), iridium (the crucible of the Czochralski furnace), europium, terbium and yttrium (LED and screen phosphors), erbium (fibre amplifiers), selenium, tellurium |
| [Science](science.md) | lutetium (scintillators), ytterbium, holmium, thulium and neodymium (laser media), rubidium and caesium (atomic clocks) |
| [Nuclear](nuclear.md) | gadolinium, boron, hafnium (control rod absorbers), beryllium (neutron reflector), thorium (a thorium reactor as an alternative), strontium-90 (the RTG nuclear battery), iodine (potassium iodide); the uranium chain's radium, radon, polonium, actinium and protactinium, and the reactor's neptunium, technetium and promethium |
| [Fluids and gases](fluids-and-gases.md) | neon, argon, krypton, xenon, from the air |

Francium and astatine have no source and stay as data only.

### Transuranium and superheavy elements

They are made the way they are made in reality, in two places:

- **In the reactor**, by neutron capture in the fuel, from neptunium (93) to fermium (100), each with its real use:
  neptunium-237 (the target that makes plutonium-238), plutonium-239 (fuel, MOX), plutonium-238 (the RTG battery, the
  one of space probes), americium-241 (an americium-beryllium neutron source to start reactors; smoke detectors),
  curium-244 (RTGs, alpha sources), berkelium-249 (a target), californium-252 (a neutron source; well logging, which
  ties into oil prospecting), einsteinium and fermium (targets for the next ones). See [Nuclear](nuclear.md).
- **In the accelerators**, by heavy-ion fusion, from mendelevium (101) to oganesson (118), with the real target and beam
  pairs: oganesson from californium and calcium-48, tennessine from berkelium and calcium-48, flerovium from plutonium
  and calcium-48, nihonium from bismuth and zinc-70, the lighter ones with iron, chromium and nickel beams on lead and
  bismuth. Calcium-48 has to be enriched, and the targets come from the reactor: reactor and accelerator are one chain.
  See [Science](science.md).

**The superheavies are first discovered, then made stable.** The first synthesis of each superheavy element (104 to 118)
gives the isotope science has made so far, which lives seconds: it is a **discovery** (data for the research centres, an
advancement, the next step of research), not a material. With that research done, the player can make the **neutron-rich
isotope** of the same element. That is what real physics calls the **island of stability**, near 184 neutrons, the goal
of real superheavy research. It is **stable and usable**: an ingot, a dust, whatever a recipe asks for. It is much
harder (neutron-rich targets from the reactor, enriched beams, a higher-tier accelerator), and harder the higher the
atomic number. Their uses are decided with [Science](science.md) and [Exotic materials](exotic-materials.md).

### Beyond the known table

The limits on atomic number are predictions of calculations, not observations: nobody has made an element above 118. The
series bets that the island goes on: some calculations even predict a second island near 164. The elements beyond 118
are **stable superheavy elements**, named as IUPAC names elements today, after scientists, places and ideas tied to what
each one is for:

| Z | Name | Named after | What it is for |
| ---: | --- | --- | --- |
| 119 | Genevium | Geneva, home of CERN and its great collider | shielding, structures |
| 120 | Teslium | Nikola Tesla | electromagnets, levitation |
| 121 | Kelvinium | Lord Kelvin | heat exchangers |
| 122 | Diracium | Paul Dirac, who predicted antimatter | the target of antimatter |
| 123 | Lehmannium | Inge Lehmann, who found the Earth's inner core | high-tension springs; it comes from the Earth's core |
| 135 | Bosium | Satyendra Nath Bose | the perfect coolant |
| 145 | Planckium | Max Planck | quantum stabilisers |
| 148 | Bardeenium | John Bardeen, the theory of superconductivity | superconducting wires |
| 152 | Hawkingium | Stephen Hawking | coatings; the heat death of the universe |
| 156 | Boltzmannium | Ludwig Boltzmann, entropy | solid-state generators |
| 160 | Zwickium | Fritz Zwicky, who named dark matter | it comes from condensed dark matter |
| 165 | Maimanium | Theodore Maiman, who built the first laser | lasers, light |
| 170 | Illaenium | a name of the series' own, an echo of illinium, the name element 61 almost had in 1926 | dense fuel, made the way stars make elements |
| 180 | Cantorium | Georg Cantor, the mathematics of infinity | parts that never wear out |
| 200 | Newtonium | Isaac Newton; "newtonium" was a hypothetical element Mendeleev proposed | artificial gravity |
| 210 | Turingium | Alan Turing | neural interfaces |
| 250 | Aeternium | Latin *aeternus*, eternal | the Heart of the Universe, the last element of the game |

Their symbols are set without repeating any that exist. Before a player discovers one, it shows under its real
systematic name (ununennium for 119, unbinilium for 120...), as in the periodic table today.

**The discoverer names it.** On a server, the player who first discovers one of these elements (its first synthesis)
**gives it its name and its symbol**, and the element and all its items take them, for everyone and in every language: a
name is a name. The names above are the defaults, used when the naming system is turned off.

- The name is kept with the world.
- It has a maximum length, only letters, and repeats no other element's name. The symbol has one or two letters and
  repeats none.
- A server setting can require the IUPAC "-ium" ending; it is off by default.
- Once given, a name is not changed; an operator can undo it with a command, for an offensive name.
- Until it is named, the element keeps its systematic name.
- The real elements, up to 118, already have their names and are never renamed. Where each comes from is decided with
  [Science](science.md) and [Exotic materials](exotic-materials.md): every one has a source inside J's Industrial,
  slower or more expensive; with J's Computers installed, its cosmological simulator is an abundant alternative.

### Exotic forms of matter

Next to the elements, the catalogue has **exotic forms of matter**, a kind of their own, from real physics, predicted or
observed: **metallic hydrogen** (also planned by J's Space, in the cores of gas giants), **neutronium**, **quark-gluon
plasma**, **strange matter**, **dark matter**, **Bose-Einstein condensate**, **negative-mass exotic matter**,
**degenerate matter**, **time crystals**, **photonic matter** and **computronium**. Their sources and uses are decided
with [Exotic materials](exotic-materials.md), and with J's Space for the ones nature makes (gas giants, neutron stars,
white dwarfs).
