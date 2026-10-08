# The world

Where the minerals lie, how veins are shaped, the reservoirs of oil, gas and brine, and the rubber trees. The minerals
themselves are in [Minerals](minerals.md); what a mineral and an ore are is in [Materials](materials.md).

Nothing on this page exists yet. Today the world has vanilla's ores only; J's Core can already declare an ore that
generates in vanilla's way (a vein size, veins per chunk, a height range, biomes), which the vein system below replaces.

## To build

### Veins

Ores don't lie scattered: they form **veins**, zones about 3×3 chunks wide, dense at the centre and thinning towards the
edges. A player finds nothing, or finds hundreds. The vein system is a **world generation kit of J's Core**, so any
mod's ores can use it.

**A vein is a real deposit type.** In nature a deposit brings several minerals together, so each vein holds the minerals
of its type, **plus the "X ore" of their elements** ([Materials](materials.md)): a porphyry copper vein holds
chalcopyrite, bornite, molybdenite and pyrite, and also vanilla copper ore and molybdenum ore.

**A vein crosses rocks.** It can lie in stone, deepslate, granite, sandstone or any other host rock, and extend across
more than one; every ore block takes the variant of the rock it replaced.

Every deposit type has its height range, its frequency and its biome preference (the numbers are in
[Implementation](implementation.md)). With the vein system on, vanilla iron, copper and gold only generate in veins:
vanilla's scattered placement is removed by NeoForge's `remove_features` biome modifiers, as data.

### The deposit types

| Deposit type | Setting | Minerals |
| --- | --- | --- |
| Porphyry copper | granite, andesite; middle to deep, with an enriched zone above | chalcopyrite, bornite, molybdenite, pyrite, magnetite; chalcocite, covellite, digenite in the enriched zone |
| High-sulfidation epithermal | andesite, tuff; shallow | enargite, tennantite, tetrahedrite, alunite |
| Low-sulfidation epithermal gold and silver | andesite, tuff; shallow to middle | calaverite, krennerite, sylvanite, nagyagite, electrum, acanthite, pyrargyrite, proustite, stephanite, polybasite, naumannite, argyrodite |
| Orogenic gold, arsenic and antimony veins | stone, deepslate; middle to deep | arsenopyrite, pyrrhotite, maldonite, stibnite, kermesite, native antimony, boulangerite, jamesonite, bournonite, dyscrasite, native arsenic, tellurobismuthite, altaite, clausthalite |
| Carbonate-hosted lead and zinc | calcite, stone; middle | galena, sphalerite, wurtzite, marcasite, greenockite, barite, witherite, fluorite, celestine, strontianite, dolomite |
| Volcanogenic massive sulfide | stone, deepslate; middle to deep | pyrite, chalcopyrite, sphalerite, galena, gallite, roquesite, germanite |
| Skarn | where granite meets calcite; middle | scheelite, powellite, magnetite, chalcopyrite, sphalerite, uvarovite, almandine, rhodonite, franklinite, willemite, zincite, spinel |
| Oxidised zone | above sulfide deposits; near the surface, more in dry biomes | cuprite, malachite, azurite, chrysocolla, atacamite, cerussite, anglesite, pyromorphite, mimetite, vanadinite, crocoite, wulfenite, smithsonite, hydrozincite, hemimorphite, goethite, erythrite, annabergite, spherocobaltite, bismite, bismutite, valentinite, senarmontite, arsenolite, stolzite, tungstite, ferrimolybdite, descloizite, mottramite, autunite, torbernite, uranophane, chlorargyrite, bromargyrite, calomel, turquoise |
| Magmatic nickel, copper and platinum | deepslate; deep | pentlandite, pyrrhotite, millerite, chalcopyrite, sperrylite, cooperite, braggite, isoferroplatinum, osmiridium, irarsite, laurite, stibiopalladinite, potarite |
| Ultramafic | deepslate, basalt; deep | chromite, magnesiochromite, eskolaite, olivine, brucite, magnesite, heazlewoodite, talc, benitoite |
| Cobalt and nickel arsenide veins | deepslate; deep | cobaltite, skutterudite, safflorite, glaucodot, nickeline, gersdorffite, linnaeite, native bismuth |
| Sediment-hosted copper and cobalt | stone, sandstone; middle | chalcocite, bornite, carrollite, malachite; heterogenite near the surface |
| Granite pegmatite | granite; middle | spodumene, lepidolite, petalite, amblygonite, zinnwaldite, eucryptite, beryl, chrysoberyl, phenakite, euclase, columbite, tantalite, microlite, wodginite, tapiolite, pollucite, muscovite, orthoclase, topaz, gahnite, gadolinite, samarskite, fergusonite, euxenite, allanite, thortveitite, hafnon, uraninite, thorite, bismuthinite; cryolite in cold biomes |
| Tin and tungsten greisen | granite; middle | cassiterite, stannite, teallite, franckeite, wolframite, ferberite, hübnerite, molybdenite, topaz, fluorite, arsenopyrite, anatase, brookite |
| Laterite | near the surface; hot and humid biomes | gibbsite, boehmite, diaspore (deeper), garnierite, limonite, goethite, ion-adsorption clay, kaolinite, pyrolusite |
| Banded iron formation | stone, deepslate; huge and poor | taconite, hematite, magnetite |
| Sedimentary iron and manganese | stone, sandstone, calcite; shallow to middle | siderite, chamosite, pyrolusite, psilomelane, manganite, rhodochrosite, hausmannite, braunite |
| Sandstone uranium and vanadium | sandstone, more in deserts; shallow to middle | carnotite, coffinite, uraninite, roscoelite, patronite |
| Evaporite | deserts and salt flats; shallow | halite, sylvite, carnallite, polyhalite, langbeinite, gypsum, anhydrite, borax, kernite, ulexite, colemanite, tincalconite, trona, thenardite, nitratine, lautarite, sellaite |
| Carbonatite | calcite, deepslate; deep | pyrochlore, bastnäsite, parisite, synchysite, apatite, perovskite, loparite, eudialyte, baddeleyite, monazite, magnetite, fluorite |
| Heavy mineral sands | stone, deepslate, granite; and sandstone by the water near beaches | ilmenite, rutile, leucoxene, titanite, zircon, monazite, xenotime, thorianite |
| Metamorphic | deepslate, diorite; deep | graphite, corundum (with its ruby and sapphire), spinel, almandine, rhodonite, braunite, hausmannite, talc, uvarovite |
| Volcanic and hot springs | tuff, beside lava; shallow | native sulfur, realgar, orpiment, cinnabar, metacinnabar, livingstonite, native mercury, sassolite, opal, tridymite, cristobalite, bertrandite, bixbyite, alunite, aragonite |
| Silica veins | stone, granite | quartz ore, chalcedony, agate, flint |
| Phosphate beds | stone, sandstone; shallow | phosphorite, wavellite, apatite |
| Clay beds | near the surface; rivers and swamps | kaolinite, bentonite, vivianite |

**Saltpetre** is not a vein: it grows on cave walls.

### Settings

The vein kit has settings, in J's Core's files:

- turn the **vein system off**: ores then generate as in vanilla Minecraft, scattered, with rarity by height;
- each deposit type's **rarity**, how far its veins **spread from their centre**, their **density**;
- per material, **do not generate** its ore (for packs where another mod already brings it);
- **try brute-force oregen**: try to move other mods' ores into veins, removing their own generation and placing them by
  their common tags. It works with mods that generate ores as data, and can fail with mods that generate them their own
  way, hence the "try".

The minerals' radioactivity also has a setting ([Materials](materials.md)).

### Prospecting

- The **prospecting pick** gives the direction and strength of nearby veins ("traces of copper to the south-east", "a
  rich vein of iron right below"), never coordinates: the player triangulates. It also senses oil and gas reservoirs,
  and roughly how deep they are.
- The **portable scanner** gives a vein's composition and how much it holds, and a reservoir's size.

Both are in [Tools and armour](tools-and-armour.md).

### Reservoirs of oil, gas and brine

Oil is a **physical** thing in the world, as it is in the ground: it fills **reservoir rock**, porous blocks soaked in
it (oil sandstone, oil shale), under a **cap rock** that holds it in.

- Each block of reservoir rock holds an amount of oil. A **pump** on the surface, with its well going down to the
  reservoir, drains the blocks one by one, and an empty block turns back into plain rock.
- It is finite and visible: digging into a reservoir shows it, and nothing floods the mine.
- At the surface there are **oil sands** and **oil shale**, which are mined and processed (as in Alberta).
- Reservoirs are kept in J's Core's data over areas, so other mods read them.
- **Natural gas** (J's Geology's) and **brines** (lithium and bromine, in desert salt flats) lie the same way, in their
  own reservoir rock. With J's Geology installed, its seismic prospecting also finds J's Industrial's oil.

Reservoir sizes, how often they appear and the pump's rate are decided with [Chemistry](chemistry.md) and
[Implementation](implementation.md).

### Rubber trees

Rubber trees grow in jungles and tropical biomes, placed by a biome modifier over the jungle and tropical tags, so they
work with vanilla's biomes and with J's Overworld's. Resin is tapped from their logs with the tool, and a log gives
resin again after about 5 minutes. The Industrial Greenhouse grows them ([Machines](machines.md)).
