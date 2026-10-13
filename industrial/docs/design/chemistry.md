# Chemistry

How chemistry works, petroleum, coal, wood and plants, the heavy inorganic chemicals, polymers, the separations and the
specialties. How fluids move and what a leak does is in [Fluids and gases](fluids-and-gases.md); the materials
themselves in [Materials](materials.md) and [Minerals](minerals.md).

Before oil took over, almost all chemistry came from coal and wood, and the game keeps that path: a player with no oil
nearby gets everything from coal, as Germany did in the 1930s and South Africa's Sasol later.

## What exists today

J's Core moves chemicals that are neither items nor fluids (Mekanism's gases, for example) through a bridge, as an id
and an amount. J's Industrial has no chemistry yet.

## To build

### How chemistry works

- **Every chemical recipe is a real reaction, in its real proportions**, and the machine's screen shows the equation:
  N₂ + 3 H₂ → 2 NH₃. Gases and liquids count in mB, solids in dusts, and the coefficients give the amounts: 100 mB of
  nitrogen and 300 mB of hydrogen make 200 mB of ammonia. For gases, that is Avogadro's law.
- **Conditions count.** Each reaction has its temperature (the heat comes through the heat port or the machine's own
  heater), its pressure (Haber-Bosch runs at 200 bar) and its real catalyst: iron for ammonia, vanadium pentoxide for
  sulfuric acid, platinum-rhodium gauze for nitric acid, zeolites for cracking. The catalyst sits in a **catalyst slot**
  and wears out slowly. The **Catalyst Cartridge** of [Machines](machines.md) is a catalyst's promoted grade, which
  works 50% faster.
- **Pressure is the gases' own.** A real high-pressure reaction compresses the gases that react. The **Gas Compressor**
  (T2 reciprocating, T3 centrifugal) takes any gas from a gas pipe into a High-Pressure Pipe, and a reactor or a plant
  that asks for pressure only takes gases already compressed. Compressing costs energy in proportion to the pressure: in
  real Haber-Bosch plants it is a large part of the bill.
- **Compressed air** ([Machines](machines.md)) goes where it goes in real life: as a **reactant** in reactions that use
  air (nitric acid, sulfuric acid, roasting, the blast furnace's blowers), and as **instrument air**: big plants move
  their control valves with it, and without air a plant stops.
- **Generic machines and real plants.** The generic ones run any reaction within their limits of temperature and
  pressure, in small batches: the **Chemical Reactor** (T2), the **High-Pressure Reactor** (T3), the **High-Temperature
  Reactor** (T4, above 1,500 °C) and the **Electrolytic Cell** (T2). The famous processes have their **plant**, a
  multiblock of variable size that runs the same reaction at scale. It is the same choice as with upgrades: many small
  machines, or one big plant.
- **Byproducts are the real ones, and each has somewhere to go**: chlor-alkali gives hydrogen too, Solvay gives calcium
  chloride, roasting gives sulfur dioxide, which becomes acid. Everything J's Industrial makes has a use in a recipe of
  the series.
- **Waste is local**: a toxic gas vented to the air makes a cloud, an acid spills
  ([Fluids and gases](fluids-and-gases.md)). **Pollution** is a J's Core system any mod can use, as radiation is: it is
  kept per chunk, spreads to neighbouring chunks and fades as plants take it up; its kinds are the real ones (soot and
  particles, sulfur and nitrogen oxides as acid rain, heavy metals in soil and water, carbon dioxide); its effects
  belong to J's Core's design, with J's Agriculture and J's Geology. J's Industrial says what each machine emits and has
  the equipment that cleans it, which, as in real life, gives byproducts:

| Equipment | Tier | Takes out | Gives | In real life |
| --- | --- | --- | --- | --- |
| Electrostatic Precipitator | T2 | soot and particles | dusts with thallium, selenium and tellurium, and **fly ash** (for J's Civil Works' cement) | Cottrell, 1907 |
| Flue Gas Desulfurizer | T3 | sulfur dioxide | **gypsum**, from limestone | the scrubbers of the 1970s |
| SCR Unit | T4 | nitrogen oxides | nitrogen and water, with ammonia on a catalyst | selective catalytic reduction |
| Carbon Capture Unit | T5 | carbon dioxide | pure carbon dioxide, to store in a cavern or use in chemistry | today's carbon capture |

### Petroleum

The world holds oil in **reservoir rock** ([The world](world.md)). Real reservoir rock is about 20% pores, so each block
holds **200 mB** of oil, and a reservoir of 10,000 blocks holds 2,000 m³. The **Well Pump**
([Fluids and gases](fluids-and-gases.md)) takes 10 mB/t, a very good well, and spends **drilling mud** as it drills
(below). How often reservoirs appear and how big they are is in [Implementation](implementation.md).

**Distillation**, with the shares of a real light crude, per 1,000 mB:

| Unit | Tier | Gives |
| --- | --- | --- |
| Desalter | T2 | takes the salt out of the crude first; without it, the tower **corrodes** slowly and ends up damaged |
| Atmospheric Distillation Tower (multiblock, variable size) | T2 | 20 refinery gas and LPG, 250 naphtha, 150 kerosene, 250 diesel, 330 residue |
| Vacuum Distillation Unit | T2 | the residue becomes 200 vacuum gas oil, 30 lubricating oils, 100 bitumen |

**Conversion**, which gets more fuel out of the same barrel:

| Unit | Tier | What it does | In real life |
| --- | --- | --- | --- |
| Visbreaker | T2 | mild thermal cracking of the residue | |
| Delayed Coker | T2 | residue into gas oil and **petroleum coke**; calcined, the coke makes the **anodes of Hall-Héroult cells** and, with coal tar pitch, the **graphite electrodes** of the arc furnace | 1929 |
| Fluid Catalytic Cracker | T2 | heavy gas oil into gasoline and LPG, on silica-alumina, then on zeolites from the T3 | 1942 |
| Catalytic Reformer | T3 | naphtha into high-octane gasoline, **hydrogen** and **aromatics** (benzene, toluene, xylene), on platinum and rhenium | 1949 |
| Isomerization Unit | T3 | light naphtha into higher-octane gasoline | the 1950s |
| Hydrotreater and Claus Unit | T3 | take the sulfur out of fuels; the hydrogen sulfide becomes pure **sulfur**, where most of the world's sulfur comes from today | the 1950s |
| Merox Unit | T3 | takes the foul, corrosive mercaptans out of LPG and kerosene | 1958 |
| Alkylation Unit | T3 | LPG into aviation gasoline | 1940 |
| Steam Cracker | T3 | naphtha into **ethylene, propylene, butadiene** and pyrolysis gasoline | the 1950s |
| Hydrocracker | T4 | heavy gas oil into clean diesel and kerosene, with hydrogen | the 1960s |

**Finishing:** the **Solvent Dewaxing** unit (T2) takes paraffin wax out of the lubricating oils, and **Bitumen
Blowing** (T2) blows air through bitumen to make the bitumen of roofs and waterproofing.

**Oil at the surface:** the **Oil Sands Separator** (T3) washes oil sands with hot water into bitumen, as in Alberta in
1967, and the Coker upgrades the bitumen into synthetic crude; the **Shale Retort** (T2) heats oil shale into shale oil,
as in 19th-century Scotland.

**Safety:** the **Flare Stack** (T2) burns the gases nobody wants. Nothing explodes, but it pollutes.

**Products:** the fuels of J's Core's shared catalogue (gasoline, kerosene, diesel, fuel oil, LPG), lubricating oil,
bitumen (the asphalt of J's Civil Works' roads), paraffin wax, petroleum coke and sulfur; ethylene, propylene, butadiene
and aromatics go on to [Polymers](#polymers). Natural gas and its processing come with J's Geology's gas reservoirs;
moving crude far, in long pipelines or tankers, is J's Transport's.

### Coal, wood and plants

**Coal**

| Machine | Tier | What it does | In real life |
| --- | --- | --- | --- |
| Coke Oven | T1 | also gives **coke oven gas** (hydrogen, methane, carbon monoxide), a fuel, and **ammonia liquor**, which gives ammonium sulfate fertiliser | |
| Tar Still | T1 | also gives **benzene, phenol, cresols and anthracene**; the phenol makes Bakelite | coal tar started the dye industry (mauveine, 1856) |
| Coal Gasifier | T2 | coal and steam into **synthesis gas** (carbon monoxide and hydrogen) | water gas of the 1870s; Lurgi, the 1930s |
| Methanol Synthesis (plant) | T2 | synthesis gas into methanol, at high pressure | BASF, 1923 |
| Fischer-Tropsch Reactor (plant) | T2 | synthesis gas into **synthetic diesel, gasoline and waxes**, with no oil | 1925; Sasol, 1955 |
| Calcium Carbide Furnace | T2 | lime and coke into carbide; carbide and water give **acetylene**: miners' lamps, the oxy-acetylene torch, and acetylene chemistry (the PVC of the T2, neoprene) | Willson, 1892 |
| Acheson Furnace | T2 | coke and pitch into **graphite electrodes** (the arc furnace's); sand and coke into **silicon carbide**, an abrasive and, in the T5, a semiconductor | Acheson, 1891 to 1896 |
| Activation Furnace | T2 | charcoal into **activated carbon**: the filters of gas masks, water purification, gold recovery | the gas masks of the First World War |
| Carbon Black Reactor | T2 | oil or gas half burned into **carbon black**, the reinforcement of tyre rubber and inks | the 1920s |

**Wood**

| Machine | Tier | What it does | In real life |
| --- | --- | --- | --- |
| Wood Distillation Retort | T1 | wood into **methanol** ("wood alcohol"), acetic acid, acetone, tar and charcoal | until 1923, the world's methanol came from wood |
| Kraft Pulp Mill (plant) | T2 | chips and caustic liquor into **pulp** (the Paper Machine's) and black liquor, which a **recovery boiler** burns for steam while giving the chemicals back; also **tall oil** and **turpentine** | the kraft process, 1879 to 1890 |

**Plants**, from vanilla's crops (J's Agriculture adds more through tags):

| Machine | Tier | What it does |
| --- | --- | --- |
| Extractor | T1 | also **vegetable oil** from seeds |
| Fermenter | T1 | sugar cane, potatoes or wheat into **ethanol** and carbon dioxide; ethanol is a solvent and a fuel, and from the T2 becomes ethylene, as in Brazil |
| Anaerobic Digester | T2 | rotten flesh, manure and plant waste into **biogas** (the Gas Engine Generator's) and **digestate**, a fertiliser |
| Chemical Reactor | T2 | vegetable oil and methanol into **biodiesel** (the Diesel Generator's) and **glycerine** |

### Heavy inorganic chemicals

**Acids**

| Machine | Tier | What it does | In real life |
| --- | --- | --- | --- |
| Lead Chamber | T1 | sulfur dioxide, air and water into sulfuric acid, slowly and in small amounts | 1746 |
| Contact Process Plant | T2 | sulfur dioxide into sulfuric acid at scale, on vanadium pentoxide; the sulfur dioxide comes from native sulfur, the Claus unit, roasting or pyrite | 1870s to 1920s |
| Birkeland-Eyde Arc | T1 | air through a huge electric arc into nitric acid; it eats energy like little else | Norway, 1903, on hydroelectric power |
| Ostwald Plant | T2 | ammonia and air into nitric acid, on platinum-rhodium gauze | 1902 |
| Hydrogen Chloride Burner | T2 | hydrogen and chlorine into hydrochloric acid | |
| Hydrofluoric Acid Plant | T2 | fluorite and sulfuric acid into **hydrogen fluoride**, in stainless steel gas pipe | |
| Fluorine Cell | T3 | hydrogen fluoride into **fluorine**: the uranium hexafluoride of [Nuclear](nuclear.md), the nitrogen trifluoride of the [Clean Room](clean-room.md), PTFE, synthetic cryolite | Moissan, 1886; industrial in the Manhattan Project |

There is one sulfuric acid: the Lead Chamber and the Contact Process Plant make the same acid, at very different rates.

**Ammonia and hydrogen**

| Machine | Tier | What it does | In real life |
| --- | --- | --- | --- |
| Haber-Bosch Plant | T2 | N₂ + 3 H₂ → 2 NH₃, at 200 bar, on iron, with a Gas Compressor at its inlet | 1913 |
| Steam Reformer | T2 | methane and steam into hydrogen and carbon monoxide, on nickel | the 1930s |

Hydrogen also comes from electrolysis, from coal gasification and from the Catalytic Reformer. Ammonia goes to the
fertilisers (ammonium sulfate, ammonium nitrate, urea), nitric acid, the SCR Unit and refrigeration: Linde's ice machine
ran on ammonia.

**Salt, chlorine and soda**

| Machine | Tier | What it does | In real life |
| --- | --- | --- | --- |
| Salt Evaporation Pond | T1 | sea water or brine into salt, in the sun | salt pans |
| Solvay Plant | T1 | brine, limestone and ammonia (from the coke ovens) into **soda ash**, with calcium chloride; soda ash makes glass, soap, and the caustic soda of the Bayer process | 1861 to 1870 |
| Chlor-Alkali Cell, in three generations | T2, T2, T4 | brine into **chlorine, caustic soda and hydrogen**: the **Castner-Kellner** cell (the 1890s), with mercury, which **pollutes with heavy metals**; the **Diaphragm** cell; the **Membrane** cell (the 1970s), clean and efficient | |

**Salt flats and evaporites** ([Minerals](minerals.md))

| Machine | Tier | What it does |
| --- | --- | --- |
| Brine Evaporation Pond | T2 | desert brine into **lithium carbonate**, as in the Atacama |
| Spodumene Converter | T3 | spodumene into lithium, by roasting and acid |
| Bromine Tower | T2 | brine and chlorine into **bromine**, blown out with air (Dow, 1891) |
| Iodine Works | T1 | kelp ash or caliche (lautarite) into iodine |
| Potash Works | T2 | sylvite and carnallite into **potash** fertiliser |
| Boric Acid Works | T1 | borax into boric acid, and **borosilicate glass** (Pyrex, 1915; the vitrification of nuclear waste) |

**Phosphorus, peroxide, titanium and cyanide**

| Machine | Tier | What it does | In real life |
| --- | --- | --- | --- |
| Superphosphate Works | T1 | apatite or phosphorite and sulfuric acid into **superphosphate** fertiliser | Lawes, 1842 |
| Phosphorus Furnace | T2 | phosphate, silica and coke, in an arc, into **elemental phosphorus** | the 1890s |
| Peroxide Plant | T3 | **hydrogen peroxide**: it bleaches pulp, is a rocket propellant (J's Space) and cleans wafers ([Clean Room](clean-room.md)) | the anthraquinone process, the 1940s |
| Chlorinator | T3 | rutile, chlorine and coke into **titanium tetrachloride**, which the Kroll Reactor reduces to titanium, and the **titanium white** of paints | the 1950s |
| Cyanidation Tank | T1 | **gold** from poor ore, with cyanide and then activated carbon; cyanide is toxic ([Fluids and gases](fluids-and-gases.md)) | MacArthur-Forrest, 1887 |

Silicon for semiconductors (metallurgical silicon, trichlorosilane, the Siemens reactor, polysilicon) is in
[Clean Room](clean-room.md).

### Polymers

Polymers are **materials** of J's Core's catalogue, with their forms: pellets, sheet, film, fibre, foam. Each comes from
a chain of this page, in the order of real history:

| Polymer | Tier | Made from | Used for | In real life |
| --- | --- | --- | --- | --- |
| Vulcanised rubber | T1 | resin and sulfur | cable insulation, seals | 1839 |
| Celluloid | T1 | nitrocellulose and camphor | film, the first plastic parts | 1870 |
| Bakelite (phenolic) | T1 | phenol from the Tar Still and formaldehyde from wood methanol | cases, insulators, the **phenolic laminate** of T2 boards | 1907 |
| PVC | T2 | the carbide route: acetylene and hydrochloric acid | pipes, cables, electrical tape | the 1930s |
| Neoprene | T2 | acetylene | oil-resistant seals | 1930 |
| SBR rubber | T2 | styrene and butadiene (butadiene from ethanol, by Lebedev's process) | tyres, with carbon black | Buna-S, the 1930s |
| Polystyrene | T2 | benzene and ethylene | parts, insulating foam | the 1930s |
| LDPE | T2 | ethylene at **1,500 to 3,000 bar** | film, insulation | ICI, 1939 |
| Nylon | T2 | from benzene | fibres, gears, parachute cloth | Carothers, 1935 |
| PMMA (acrylic) | T2 | from methanol and acetone | acrylic glass | 1933 |
| PTFE | T2 | hydrogen fluoride and chloroform | seals and linings that nothing attacks | Plunkett, 1938 |
| Epoxy | T2 | | glues and board laminates | the 1940s |
| Silicones | T2 | silicon and methyl chloride | sealants, oils, silicone rubber | Rochow, the 1940s |
| Polyurethane | T2 | | foams, sealants | Bayer, 1937 |
| Ion exchange resin | T2 | sulfonated polystyrene | the Ion Exchange Column | the 1940s |
| HDPE and polypropylene | T3 | ethylene and propylene on the **Ziegler-Natta** catalyst, of titanium (the Chlorinator's) | pipes, tanks, parts | 1953 to 1954 |
| XLPE | T3 | cross-linked polyethylene | medium and high voltage cables | the 1960s |
| Polycarbonate | T3 | | hard clear parts | 1953 |
| PET | T3 | | bottles, fibres | |
| ABS | T3 | | appliance cases | 1948 |
| Polyacrylamide | T3 | | gels | the 1950s |
| Polyimide (Kapton) | T3 | | flexible circuits, spacecraft insulation | the 1960s |
| FR-4 (epoxy and glass fibre) | T3 | | circuit boards from the T3 on | |
| Kevlar (aramid) | T4 | | high-strength fibre, vests | Kwolek, 1965 |
| PVDF | T4 | | ultra-pure water pipe | |
| PEEK | T4 | | engineering parts for high heat | 1978 |
| UHMWPE (Dyneema) | T4 | | ropes, light armour | 1979 |
| PLA | T5 | **fermented** lactic acid | biodegradable plastic | the 2000s |

**Machines.** Polymerisation runs in the Chemical Reactor and the High-Pressure Reactor, and at scale in the **LDPE
Autoclave Reactor** (T2) and the **Loop Reactor** (T3). To give polymers their shape:

| Machine | Tier | What it does |
| --- | --- | --- |
| Calender | T1 | rubber and PVC sheet |
| Laminating Press | T2 | board laminates: the phenolic of the T2, FR-4 from the T3 |
| Compounding Extruder | T2 | polymer and additives (carbon black, plasticisers, stabilisers) into **pellets** |
| Fibre Spinning Machine | T2 | nylon, rayon and later aramid into fibre |
| Film Blower | T2 | plastic film |
| Blow Molder | T3 | bottles and tanks |

The Injection Molder is in [Machines](machines.md).

**Recycling, as in real life:** **thermoplastics** (PVC, polyethylene, nylon) are recycled, their ground scraps becoming
pellets again, as the metal scraps of [Metallurgy](metallurgy.md); **thermosets** (Bakelite, epoxy, polyurethane) are
not, and go to waste or to the Waste Incinerator for energy.

### Separations

**Rare earths.** There are 17: the 15 lanthanides, yttrium and scandium.

| Machine | Tier | What it does | In real life |
| --- | --- | --- | --- |
| Rare Earth Digester | T2 | monazite, bastnäsite, xenotime or ion-adsorption clay into a mix of rare earths; thorium and uranium go to [Nuclear](nuclear.md) | |
| Fractional Crystallizer | T2 | separates **slowly** only cerium, lanthanum and **didymium** (praseodymium and neodymium together) | before 1950, rare earths were separated this way, through thousands of crystallisations |
| Solvent Extraction Cascade (multiblock, **variable size**) | T3 | separates them all into oxides; the **more stages**, the closer the neighbours it separates | Ames, the 1950s; in real plants the number of stages decides the separation |
| Molten Salt Electrolysis | T3 | oxides into metals (neodymium, praseodymium, lanthanum, cerium); the heavy ones are reduced with calcium in the Vacuum Furnace | |

The **light** rare earths come from **monazite and bastnäsite**; the **heavy** ones and yttrium from **xenotime and the
ion-adsorption clays** of hot, humid biomes. Getting all 17 takes both kinds of deposit, as China does with its southern
clays. A cascade of 4 stages separates the light ones, 8 the middle ones, 16 the heavy ones (estimates).

| Element | Group | From | Separated in | Used for |
| --- | --- | --- | --- | --- |
| Lanthanum | light | monazite, bastnäsite | Fractional Crystallizer | optical glass for lenses; it stabilises the cracker's zeolites |
| Cerium | light | monazite, bastnäsite | Fractional Crystallizer (it oxidises and comes out first) | polishing powder (chemical-mechanical planarisation), Welsbach gas mantles, catalysts |
| Praseodymium | light | monazite, bastnäsite | with neodymium as didymium; split in the cascade | NdFeB magnets; **didymium glass**, the lenses of glassblowers' and welders' goggles |
| Neodymium | light | monazite, bastnäsite | the same | **NdFeB magnets**, the Nd:YAG laser |
| Promethium | | **not found in nature** (it has no stable isotope): from the reactor ([Nuclear](nuclear.md)) | | betavoltaic batteries and luminous paint (promethium-147) |
| Samarium | middle | monazite, bastnäsite | cascade, middle stages | **SmCo magnets**, which stand heat; neutron absorber |
| Europium | middle | monazite, bastnäsite, xenotime | cascade, middle stages | red and blue **phosphors** of screens and LEDs |
| Gadolinium | middle | monazite, xenotime | cascade, middle stages | control rods, neutron shielding |
| Terbium | heavy | xenotime, ion-adsorption clay, gadolinite | long cascade | green phosphor, magnets |
| Dysprosium | heavy | the same | long cascade | **high-temperature magnets** (motors, wind turbines) |
| Holmium | heavy | the same | long cascade | the Ho:YAG laser, the pole pieces of the strongest magnets |
| Erbium | heavy | the same | long cascade | **fibre amplifiers** |
| Thulium | heavy | the same | long cascade | portable X-ray sources (thulium-170), lasers |
| Ytterbium | heavy | the same | long cascade | **fibre lasers** (the Fibre Laser Cutter), atomic clocks |
| Lutetium | heavy, the rarest | the same | the longest cascade | scintillators |
| Yttrium | (heavy) | xenotime, ion-adsorption clay, samarskite | cascade | **YBCO** (the REBCO tape), the YAG laser, red phosphor, the stabilised zirconia that coats turbine blades |
| Scandium | apart | **thortveitite**, and as a byproduct of titanium and uranium | not in the cascade | aluminium-scandium alloys ([Metallurgy](metallurgy.md)), the zirconia of fuel cells |

**Byproducts**, from their real sources:

| Element | Where it really comes from | Machine |
| --- | --- | --- |
| Gallium | the liquor of the Bayer process, where 90% of the world's gallium comes from | Gallium Recovery (T3) |
| Germanium, indium, cadmium, thallium | the residues of zinc refining and the precipitator's dust | Zinc Residue Leach (T2) |
| Selenium, tellurium, silver, gold, platinum metals | the **anode slime** of electrolytic copper refining | Anode Slime Refinery (T3) |
| Rhenium | the dust of roasting molybdenite | the precipitator, then refining (T3) |
| Hafnium | separated from zirconium; it leaves nuclear-grade zirconium ([Nuclear](nuclear.md)) and the hafnium of transistor gates ([Clean Room](clean-room.md)) | Zr-Hf Separator (T3) |
| Niobium and tantalum | separated with hydrogen fluoride: the niobium of NbTi, the tantalum of capacitors | Nb-Ta Separator (T3) |
| Platinum, palladium, rhodium, ruthenium, iridium, osmium | | PGM Refinery (T3) |
| Caesium and rubidium | pollucite and lepidolite | (T3) |
| Strontium and barium | celestine and barite | (T2) |
| Beryllium | beryl; its dust is **very toxic**, as the real one, which causes berylliosis | (T3) |

**Catalysts.** The **Catalyst Works** (T2) makes the real catalyst of each reaction: promoted iron, nickel, vanadium
pentoxide, platinum-rhodium gauze, platinum-rhenium (the reformer's), palladium, rhodium and Ziegler-Natta. **Synthetic
zeolites** arrive in the T3 and replace the cracker's silica-alumina, as in 1964.

### Specialties

| Product | Tier | What it is | What it is for |
| --- | --- | --- | --- |
| Aerogel | T4 | silica gel dried with supercritical carbon dioxide in the **Supercritical Dryer** (Kistler, 1931) | extreme insulation: cryogenic tanks, suits ([Tools and armour](tools-and-armour.md)), J's Civil Works' panels |
| YBCO | T4 | yttrium, barium and copper oxides in the High-Temperature Reactor | the REBCO tape |
| Drilling mud | T2, T3, T4 | **bentonite, barite and water** (T2); heavy **bromide** brines (T3); **caesium formate** (T4) | the Well Pump spends mud as it drills, and deeper wells need heavier mud, as in real life |
| Brominated flame retardants | T3 | | the "FR" of FR-4 |
| Synthetic dyes | T1 | the **16 vanilla dyes in quantity**, from coal tar (mauveine, 1856; alizarin; indigo, 1897) | dyes with no plants or mobs |
| Soap | T1 | fat and caustic soda, with glycerine | |

**Industrial explosives**, for mines and quarries (weapons are J's Warfare's):

- **dynamite** (T1, Nobel, 1867): nitroglycerine (the glycerine of biodiesel, with nitric and sulfuric acids) in an
  absorbent. **Pure nitroglycerine explodes on a knock**: dropping it sets it off, as in real life;
- **ANFO** (T3): ammonium nitrate and fuel oil.

They go into **blasting charges**, set off from a distance by a blasting machine
([Tools and armour](tools-and-armour.md)).

### Potions in quantity

Vanilla potions are made as an industry, as a brewery makes beer:

- the **Fermenter** brews potions as **liquids**, in tanks (1,000 mB fill 3 bottles); the base, the awkward potion, is
  water with nether wart, which is farmed;
- the **Bottling Machine** (T2) fills the bottles, which the Glass Furnace makes in quantity;
- **splash** potions take **black powder**, which J's Industrial makes as the real one, saltpetre, sulfur and charcoal
  in the Mixer; **lingering** potions take an **aerosol propellant** instead of dragon's breath, since a lingering
  potion is an aerosol.

The ingredients that come from mobs have synthetic counterparts, each with a real logic tied to its effect:

| Vanilla ingredient | Synthetic | Made from |
| --- | --- | --- |
| Blaze powder | Synthetic Blaze Powder | red phosphorus and sulfur, the paste of match heads |
| Magma cream | Synthetic Magma Cream | synthetic blaze powder in a polyacrylamide gel |
| Ghast tear | Synthetic Ghast Tear | crystallised saline: a tear is water and salt |
| Spider eye | Synthetic Toxin | arsenic trioxide, the real poison (the fermented spider eye takes it instead of the eye) |
| Rabbit's foot | Elastomer Compound | synthetic rubber, a spring |
| Phantom membrane | Nylon Canopy | parachute cloth |
| Pufferfish | Oxygen Capsule | compressed oxygen, from air separation |
| Turtle scute | Polycarbonate Scute | polycarbonate, the plastic of shields |
| Dragon's breath | Aerosol Propellant | LPG, from the refinery |

Sugar, golden carrots, glistering melons, redstone and glowstone don't come from mobs and stay as in vanilla.
