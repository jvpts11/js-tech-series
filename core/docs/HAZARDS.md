# Hazards

What harms a body from where it is and what it carries: radiation, a pollution, dust in the lungs, the air of
another world, a hot ingot in the hand. J's Core gives every mod one kit for them: kinds of harm and what resists
each, hazards that build up in a body and harm it in bands, the hazards of carried things, the lungs, the devices that
measure them and the screen of the body, maps of the chunks that were measured, the land withering where it is
contaminated, the radiation family, the series' ionizing radiation and dissonance. It is in the spirit of the radiation of HBM's Nuclear Tech and Mekanism and the hazards of GregTech.

The design behind this page is [Hazards](design/hazards.md).

## The words

- A **kind of harm** is a family of damage things protect from: heat, cryogenic, acid, radiation. Every damage of
  the game, and every mod's, can be of one.
- A **resistance** is the share of a kind of harm a worn thing, or a creature by itself, stops: from 0 to 1.
- A **hazard** is something that builds up in a body from where it is: a dose, a contamination, a harm to the lungs.
  Its **level** is how much has built up, in its own unit; its **rate** how fast it builds up this second, after what
  is worn took its share; its **exposure** how fast it came at the body, before.
- A **band** is a stretch of a hazard's level that does something: Rad I, a slight cough.
- A **contamination** is how much a place's land is fouled for what grows on it.

## Kinds of harm and resistances

*Added 2026-10-17.*

The Core declares the kinds every mod shares (`DamageKinds`): radiation, electrical, cryogenic, acid, heat, blast,
pressure, dust breathed, toxic and glare. A mod declares more:

```java
public static final DamageKind SPORES = DamageKinds.declare(new DamageKind(
        ResourceLocation.fromNamespaceAndPath(MODID, "spores"), MyTexts.SPORES));
```

A damage type is of a kind when it is in the kind's damage type tag, `<namespace>:damage_kind/<path>`: the game's
fire and the Core's hot things are in `jscore:damage_kind/heat`, explosions in `jscore:damage_kind/blast`, freezing in
`jscore:damage_kind/cryogenic`, lightning and the Core's electric shocks in `jscore:damage_kind/electrical`, radiation
sickness in `jscore:damage_kind/radiation`. A data pack adds any mod's damage to a kind the same way.

Every kind of damage the Core declares has a damage of the Core's own in it, with its death messages, so a mod that
hurts by a kind needs no damage type of its own to do it: `DamageKinds.source(level, DamageKinds.COLD_BURN)` is the
burn of something far too cold ("was burned by something far too cold"), `DamageKinds.PRESSURE_DAMAGE` what a
difference of pressure does to a body ("was crushed by the pressure"); the others are `jscore:electric`,
`jscore:corrosive_item`, `jscore:heat`, `jscore:shrapnel` and `jscore:radiation_sickness`. A worn thing's share of a
kind is taken off them as off any damage of the kind.

What a worn thing stops is data, the data map `jscore:resistances` on items, and what a kind of creature stops by
itself the same map on entity types:

```json
{
  "values": {
    "mymod:hazmat_helmet": { "jscore:radiation": 0.2, "jscore:inhaled": 0.95 },
    "mymod:hazmat_suit": { "jscore:radiation": 0.4, "jscore:acid": 0.6 }
  }
}
```

What is worn adds up by kind, never past all of it; with what the body stops itself, what gets through is
`(1 - worn) x (1 - own)`. As damage of a kind lands on a living thing, it is cut by that share, whoever dealt it. A
worn thing's tooltip says what it stops of each kind.

```java
double share = Resistances.total(entity, DamageKinds.HEAT);   // what stops heat for it in all
Map<DamageKind, Double> worn = Resistances.worn(player);      // every kind its worn things stop
```

## Declaring a hazard

*Added 2026-10-17.*

A hazard is declared once, from the mod's constructor, with its texts in a class of texts beside it:

```java
public static final HazardKind SPORES = Hazards.declare(HazardKind.builder(id("spores"), Texts.SPORES, Texts.SPORE)
        .band(10, Texts.ITCHY, HazardEffects.effect(MobEffects.WEAKNESS, 0))
        .band(50, Texts.CHOKING, HazardEffects.damage(1))
        .recovers(0.5)                          // what a body loses a second away from its sources
        .protectedBy(DamageKinds.INHALED)       // what is worn against dust takes its share off
        .treatedBy(Texts.SPORES_TREATMENT)      // what treats it, in words
        .deadlyAs(MyDamageTypes.SPORES)         // its own damage type, whose death message is its own
        .enabledWhen(MySettings::spores)        // its setting, so a server can turn it off
        .affectsMobs()                          // creatures suffer it too
        .upTo(100)                              // the most a body can build up
        .shortName(Texts.SPR)                   // its name on a device's button
        .rateShownIn(Texts.PER_HOUR, 3600)      // its rate shown in its unit an hour
        .background(0.001)                      // what comes no faster builds up nothing
        .mapped(1, 10, 100)                     // a map of hazards shows it, stepping up at each
        .build());
SPORES.addSource((body, level) -> sporesNear(level, body.blockPosition()));   // its unit a second
SPORES.addRecovery((body, level) -> body.hasEffect(MyEffects.ANTIFUNGAL) ? 2 : 0);  // three times as fast
```

Once a second each body takes from every source of each hazard, what it wears cutting it by the kind the hazard is
protected by, or recovers away from them, `recovers` a second and `recoversShare` of what it has, faster by what each
of its recoveries (`IHazardRecovery`) adds, a medicine taken or a shower; then each band it stands in does what it
does. An effect of a band (`IHazardEffect`) acts each second, each tick, or both:
`HazardEffects.effect` keeps a mob effect on, `HazardEffects.damage` hurts as the hazard's damage type. Players always;
creatures only for the kinds they suffer, when the server lets them, near a player. Each body has its own second, so a
crowd's are spread over the ticks.

What a body has of each hazard is kept with it and gone with its death:

```java
double level = Hazards.level(body, SPORES);
double rate = Hazards.rate(body, SPORES);          // this second, after what is worn
double exposure = Hazards.exposure(body, SPORES);  // this second, before what is worn
Hazards.add(body, SPORES, 5);      // a sudden dose
Hazards.treat(body, SPORES, 5);    // a medicine, a shower
```

A player's game is told their own hazards each second something changed (`HazardsClient` holds them), for the
instruments in their hands and the body's screen.

A kind can be measured rather than built up: each second a body stands in it as far as its gauge reads, with no
sources and no recovery, as hypothermia stands as far as the core is below normal. What the gauge reads is worked out
first, each second, by what a mod hands the kit to do before the hazards are weighed:

```java
.gauged((body, level) -> MyBreath.short(body))      // in place of sources and recovery
Hazards.everySecond((body, level) -> MyBreath.second(body, level));
```

## A body's warmth

*Added 2026-10-22.*

A player's core warms and cools in kelvin (`BodyTemperature`), each second, as a real body's does (`BodyHeat`, pure):
seventy kilograms holding about 243 kJ in each kelvin, its heat going out through the tissue under its skin (closed in
the cold, open in the heat), what it wears and the film of air on it, which the wind thins and the water it is in takes
the place of; the sun on it warms it through the same layers, blocks hot and cold near warm and cool what it sees by
radiation, and the body makes heat by working (100 W standing, 200 walking, 600 sprinting, 400 swimming, 100 more
swinging an arm), shivers past 36.8 °C and sweats past 37.2 °C. The place is the air of a sealed room or of a biome's
own climate where they have one, in a dimension of the game's own rules the biome's climate from the game's values,
elsewhere the dimension's air; the wind under the open sky; the sun by its height and the rain; the rain on the body.

Past the edges of the normal warmth come two measured kinds of the kit: **hypothermia** below 35 °C (mild, slowed),
32 °C (moderate, slowed further, weaker, slower to dig) and 28 °C (severe, harmed), and **overheating** above 37.5 °C
(heat exhaustion, weaker and hungrier) and 40 °C (heatstroke, confused and harmed), each with its death message. A body
that stays where the air is below 5 °C or above 30 °C gets used to it, its bands moving up to three quarters of a
degree toward it over an hour, and loses that away from it. A player is born with a normal core and keeps one in
creative.

What a worn thing does against the weather is the data map `jscore:clothing`: how much heat it lets out, in W/(m²K),
over the part of the body it covers (the head a tenth, the trunk and arms about a half, the legs a third, the feet the
rest but the hands), and whether it keeps the wind and the water out. The game's leather lets six out, about one clo,
and keeps the wind out; its plates hold a little in; mail almost nothing.

```json
{ "values": { "mymod:fur_coat": { "lets_out": 1.2, "keeps_wind_out": true } } }
```

How hot or cold a block is to a body near it is the data map `jscore:block_temperature`, in kelvin: the game's fire,
campfires while lit, magma and lava in a cauldron, ice, packed and blue ice and snow; a liquid is as hot as its fluid
says where it is far from room temperature, lava at 1,300. Within three blocks, each fills a few hundredths of what the
body sees, by its distance, and nothing through a wall.

```java
BodyWarmth warmth = BodyTemperature.warmth(player);              // its core and how far it is used to a climate
BodyTemperature.setCore(player, 305.15);                          // a fall into ice water
BodyHeat.Place here = BodyTemperature.placeOf(level, player);     // the place as heat meets the body
```

The HUD shows the core while it is out of its normal band ([HUD elements](OVERLAYS.md#hud-elements)): a
thermometer, the degrees, the band and which way the core goes, and five marks lit two for each band reached; the
screen's edge frosts in the cold and hazes in the heat. The setting `body_temperature` turns all of it off.

## Hazards of carried things

*Added 2026-10-17.*

What an item does to whoever carries it is data, the data map `jscore:item_hazards`, each hazard with its strength:

```json
{ "values": { "mymod:sodium_ingot": { "jscore:water_reactive": 4, "jscore:corrosive": 1 } } }
```

`radioactive` doses by its strength through the radiation; `hot` burns the hand, its damage of the heat kind;
`water_reactive` goes off in water or rain; `pyrophoric` catches fire in the air; `unstable` goes off when its carrier
burns, is caught in a blast or falls hard; `toxic` poisons; `corrosive` eats away, its damage of the acid kind;
`blinding` blinds while held. Each is weighed once a second for every stack carried, never for a player in creative,
and the tooltip says the hazard and what protects from it. A mod declares more with `ItemHazards.declare`.

What a container holds, as a shulker box or a bundle holds it, is carried too, and what reaches the carrier through
walls, as radiation, reaches them through its walls: `ItemHazards.carriedWithin` counts it by the share its walls let
through. A container that shields is the data map `jscore:shielding`, the density of its walls and their thickness in
metres; its walls weaken the radiation as a block of the same does (`RadiationKind.through`), so a box with lead walls
five centimetres thick lets about a thirtieth through, and a container the map says nothing of shields nothing. What
touches the hand (heat, acid, poison) does not act through a container.

```json
{ "values": { "mymod:lead_container": { "density": 11340, "thickness": 0.05 } } }
```

## The hazards of materials

*Added 2026-10-23.*

A material carries its hazard classes as real data, as a safety data sheet gives them: the property
`MaterialHazards.HAZARDS`, a list of `HazardClass` (`explosive`, `flammable`, `oxidising`, `corrosive`, `acutely_toxic`,
`harmful`, `health_hazard`, `environmental`, and the two outside the GHS, `asphyxiant` and `radioactive`). The series'
elements and compounds carry theirs; a mod gives its own materials theirs, and a data pack changes any material's as it
changes its other values.

```java
Materials.material(rl("phosgene"), MaterialKind.GAS, PHOSGENE_NAME)
        .with(MaterialHazards.HAZARDS, List.of(HazardClass.ACUTELY_TOXIC, HazardClass.CORROSIVE)).declare();
```

- **The GHS pictograms** follow from the classes (`MaterialHazards.pictograms(material)`, `GhsPictograms.of`), GHS01 to
  GHS09 in that order, the exclamation mark giving way to the skull. The gas cylinder is the container's:
  `MaterialHazards.pictograms(material, kilopascals)` adds it for a gas held above the open air's pressure.
- **The tooltip** of every thing of the material lists its pictograms by their meaning, then the hazards beyond them:
  chlorine reads "Oxidising, Toxic, Hazardous to the environment".
- **What a cloud of a gas does**, `MaterialHazards.gasHazards(material)`: `FLAMMABLE`, `TOXIC`, `ASPHYXIANT`,
  `OXIDISING`, `CORROSIVE`, `RADIOACTIVE`, read off the classes, for any mod that lets a gas out to act on.

## The lungs

*Added 2026-10-17.*

Dusts build up a lasting harm to the lungs, in percent, after the real diseases of miners and builders: coal dust
black lung, silica silicosis, asbestos fibres asbestosis, which hardly heals. A cough first, then less breath, then a
struggle for it, then the lungs failing, which kills with a death message of its own. Masks and respirators protect,
by what they stop of the kind dust breathed. Dust is breathed breaking the blocks of `jscore:dust/coal`,
`jscore:dust/silica` and `jscore:dust/asbestos`, and wherever a machine puts it in the air:

```java
Lungs.breathe(player, Lungs.SILICOSIS, 0.2);   // percent of the lungs, before what is worn
```

## The dimensions' hazards

*Added 2026-10-17.*

The hazards of a dimension's air and climate ([World](WORLD.md)) are kinds of the kit: air that cannot be breathed,
scorching heat, freezing cold, stale air and too much oxygen, each in seconds and gone at once out of it.

## Radiation

*Added 2026-10-17.*

Radiation is a family of hazards with more in common: it is given off by sources, travels out from them, and is
weakened by every block it crosses. A mod declares a kind of radiation for the hazard it doses:

```java
public static final RadiationKind GLOW = Radiation.declare(new RadiationKind(id("glow"), Texts.GLOW,
        MyHazards.GLOW_SICKNESS, 0.0005, 24));   // weakening a kilogram a cubic metre, reach in blocks
```

A block gives it off by saying how much reaches a block from it, in the unit the dosed hazard's rate is shown in, as it
loads and as its contents change, and nothing as it empties or goes; the sources are forgotten with their chunk:

```java
Radiation.emit(level, pos, MyRadiation.GLOW, 40);
Radiation.emit(level, pos, MyRadiation.GLOW, 0);
```

What reaches a place is what is everywhere there (`addAmbient`: the sky, what lies on a chunk), and what each source
within reach gives off, divided by the square of the distance and multiplied by the share every block on the straight
way lets through: `e` to the minus the kind's weakening times the block's density, the source's and the place's own
blocks left out. What a body carries is added (`addCarried`). What reaches a body each second builds up the hazard,
cut by what it wears.

```java
double here = Radiation.rateAt(level, at, MyRadiation.GLOW);
double through = Radiation.transmission(level, from, to, MyRadiation.GLOW);
```

How dense each block is lives in the data map `jscore:block_density`, in kilograms a cubic metre; the Core fills it for
the game's blocks (wool 100, wood 600, earth 1,500, stone 2,700, iron 7,874, gold 19,300). A block it says nothing of
is as dense as its item's material, as water or lava for a liquid, as rock for any other full block, as air otherwise.

## Ionizing radiation

*Added 2026-10-17.*

The series' kind, in real units (`IonizingRadiation`). Each place has a dose rate in millisieverts an hour, and a body
builds up a dose in sieverts through two channels:

- the **external dose**, from every source round it and what it carries, which only distance, time and shielding
  reduce: about five centimetres of a block's material for each block, so a block of lead stops nearly all of it,
  stone and concrete about half, water a quarter, air hardly any; worn shielding stops its share (the kind of harm
  `radiation`);
- **contamination**, radioactive dust on the body in kilobecquerels, each giving a hundredth of a millisievert an hour,
  which keeps dosing until it is washed off in water or the rain (four hundred times faster than the body sheds it by
  itself); a suit and a respirator keep it from settling (the kind of harm `contamination`), and carrying a
  radioactive thing leaves some on its carrier.

The dose brings the bands of the acute radiation syndrome: Rad I at 1 Sv (nausea), II at 2 (slowness), III at 4
(weakness, slow harm), IV at 6 (heavy harm, blurred sight), V at 8 (close to deadly); it kills as radiation sickness.
Away from the sources the body recovers half a millisievert a second; under the background of a tenth of a
millisievert an hour, the sky's and the rock's, it takes no dose and goes on recovering.

Its sources: the sky (`CosmicRadiation`: three ten-thousandths of a millisievert an hour under the overworld's air,
rising as the air thins to two hundredths where there is none, each solid block above taking seven tenths off; a mod
sets a dimension's own with `CosmicRadiation.declare`), every mineral with uranium or thorium in it (`NaturalRadiation`:
two thousandths at a block from each block of it), radioactive carried things by their strength, and any mod's blocks
through `Radiation.emit`. Where the dose rate passes a hundred millisieverts an hour the land withers.

## Dissonance

*Added 2026-10-17.*

Where the laws of physics round a body depart from the normal ones ([World](WORLD.md)), the difference builds up in it
(`Dissonance`), a percent a second for each unit the laws are from ours: how far the pull is from straight down at its
usual strength, twice how far time runs from its usual pace, how much light bends, shimmers, darkens or glows, one for
space that wraps or leads elsewhere, one for liquids that rise. A planet's own gravity is not a law of its own and
counts for nothing. I at 10 warps the sight and shifts its colours, swinging slowly towards red and towards blue as
the view shimmers; II at 25 lets the body's gravity drift, and every sound of the world plays a little higher or lower
than it should, wavering; III at 45 makes time stutter, the body caught back where it was a second ago, and hungry; IV
at 70 throws it in small jumps through space and harms it; V at 100 tears it apart. In normal laws it falls back half
a percent a second and two hundredths of what there is. The colours and the pitch go further the further it has built
up (`DissonanceSenses`), on the player's game.

Anything else may change how the player sees or hears the same way, on the player's game:
`LightLawsClient.see(sight)` puts what an `ISight` gives together with the light of the place, and
`SoundShaper.shiftPitch(factor)` multiplies the pitch of every sound of the world as it starts.

A bubble of normal laws round a body keeps it from building up, and doubles how fast it falls back: an armour module,
a block that makes a zone, each spending its own energy:

```java
Dissonance.shield(body -> MyModules.poweredStabiliser(body));
```

## Pollution

*Added 2026-10-17.*

Pollution is an amount over each chunk, built on the Core's fields over chunks ([World](WORLD.md)): it spreads to
the neighbouring chunks, fades, is kept with each dimension and is sent to the players near enough to see it. A mod
declares a kind, with what it does to the bodies on a chunk (a hazard built up from the chunk's amount each second)
and to the land:

```java
public static final PollutionKind SPORES = Pollution.declare(PollutionKind.builder(id("spores"), Texts.SPORES,
        Texts.PER_CUBIC_METRE)
        .spread(0.1).fades(0.01).every(100)   // a share to the neighbours and a share gone each step
        .plantsTakeUp(0.05)                   // more gone where plants grow
        .felt(MyHazards.SPORE_LUNG)           // what a body on the chunk builds up from its amount
        .withersAt(500)                       // the land withers under this much
        .enabledWhen(MySettings::spores).build());
SPORES.addEffect((level, top, amount) -> rotLeaves(level, top, amount));
Pollution.emit(level, chimneyPos, MyPollution.SPORES, 20);
```

Every five seconds a few columns of each polluted chunk the game has loaded are looked at: the plants on them (leaves,
grass, flowers, saplings, crops, moss) take their share up, and each of the kind's effects is done at the top of each.
The hazard map device measures the hazard of every kind that harms bodies, so carrying it maps them.

Cleaning is each mod's equipment, built on `PollutionCleaner`: a scrubber, a filter or a device carried about takes
up to its rate a second of one kind from the chunk it stands in, never more than lies there, and keeps what it took
until its mod empties it into a full filter or a byproduct; `Pollution.takeUp` does it once.

```java
private final PollutionCleaner scrubber = new PollutionCleaner(SeriesPollution.ACID, 5);   // 5 a second

public void serverTick() {
    scrubber.tick((ServerLevel) level, worldPosition);
    if (scrubber.taken() >= FILTER_HOLDS) { scrubber.empty(); fillFilter(); }
}
```

The series' kinds (`SeriesPollution`), each with what it really does:

- **Soot and particulates**, in micrograms a cubic metre: a haze over the chunk, the end of sight closing in as it
  thickens; breathed as **smog**, a cough over 150 and a struggle over 300, which a mask stops; rain and plants take
  it down; the land withers under 2,000.
- **Sulfur and nitrogen oxides**: where it rains over 40, acid rain kills leaves slowly and weathers copper and
  calcite; crops grow up to half as often; the land withers under 600.
- **Heavy metals**, in milligrams a kilogram of soil, which never fade: a crop harvested or a fish caught there
  carries a hundredth of the ground's amount for each, said in its tooltip, and eating it builds up **heavy metal
  poisoning**, from tiredness to aching bones to failing nerves, as in Minamata and Itai-itai; each block of soil dug
  out takes a sixty-fourth of the chunk's metals away, and plants draw them up slowly.
- **Radioactive fallout**, in kilobecquerels a square metre, which spreads slowly and decays: it raises the dose rate a
  thousandth of a millisievert an hour for each, and settles on whoever stands in it as contamination.
- **Carbon dioxide** (`CarbonDioxide`), one number for the whole world in parts per million: it starts at the air's
  before industry, 280, rises by what the mods' machines burn (`CarbonDioxide.emit(kilograms)`), is shown on the map of
  hazards, and does nothing yet.

## The hazard devices

*Added 2026-10-17.*

The series' shared content has three handheld devices that look like the real equipment, a yellow case with a black
face. They read the series' ionizing radiation (`jscore:ionizing_radiation`):

- the **Geiger counter**: the rate here, as it comes before what is worn, on a needle over a scale of tens and in
  figures, its range (as printed, ten times, a hundred times), and a lamp for every click; it clicks faster as the
  rate climbs, each click a sound cue (`jscore:hazard/geiger_click`) and named in the subtitles;
- the **pocket dosimeter**: the dose taken, its band, a lamp for each band reached, and a light;
- the **hazard map**: the land round the player as the game's maps draw it, a mapped hazard on each chunk the map
  knows, live, and what is here.

Carried, each shows on the HUD as the device itself: the Geiger counter in the hotbar or the off hand, the dosimeter
anywhere on the player, the map in a hand. Where and how big is the player's, in `jstech-hazard-devices.toml`: a corner
and a size for each. Used, the Geiger counter and the dosimeter are held up full size, their buttons pressed with the
mouse; the map opens whole, a button for each mapped hazard, the legend of its steps and a chunk's reading and its age
when clicked. Each device keeps how it is set (`HazardDevices.STATE`).

The **body's screen**, on its own key (B until the player changes it), holds what is worn, how much heat it lets out,
whether it keeps the wind and the water out and how much of each kind of harm it stops; the core's warmth on a scale of
its bands, moved by what the body is used to, with what the place does to it; the dose built up on a scale of the
radiation's bands, the rate here (from outside and from what is on the body) and a word asking for a shower; the
lungs; dissonance; and the other hazards on the body. *Changed 2026-10-22:* it is a screen declared in a file,
`assets/jscore/jstech/screens/body.json` ([Screens](SCREENS.md#screens-declared-in-files)), so a resource pack lays it
out again and the player's style draws it; the colours of its scales are the palette `jscore:screen/body`.
`BodyScreen.open()` opens it from code.

## Maps of hazards

*Added 2026-10-17.*

Each player has a map of the land's hazards, chunk by chunk, kept with them through death. An item that measures a
hazard implements `IHazardInstrument`: carried in the hotbar or the off hand, it reads the rate of its hazard at the
player's chunk each second, for each mapped hazard that is on. A mod's sensor puts in its own readings:

```java
HazardMaps.record(player, level.dimension(), chunkPos, MyHazards.SPORES, shownRate);
```

A chunk is read again when the rate moved by a tenth or the reading is half a minute old; each dimension keeps the
newest 16,384 readings of each hazard. The player's game is told every reading of the dimension they arrive in, the
nearest first, and each new one as it is read (`HazardMapsClient`); what was never read the map leaves grey.

## The land withering

*Added 2026-10-17.*

Where the land is contaminated past what living things bear, grass dies, flowers, saplings and grasses wither to dead
bushes, and crops stop growing; where it falls below half of that, what died comes back, slowly. A mod says how
contaminated a place is, as a share of what plants bear, and what withers into what:

```java
Withering.declare((level, pos) -> fallout(level, pos) / 10.0);
Withering.into(() -> Blocks.MYCELIUM, () -> MyBlocks.DEAD_MYCELIUM.get().defaultBlockState());
Withering.recovers(MyBlocks.DEAD_MYCELIUM, Blocks.MYCELIUM::defaultBlockState);
```

Each second a few columns round each player are looked at, only in chunks the game has whole. The series' grass
withers into **withered grass**, the game's grass gone the dry brown of the badlands, which drops dirt, or itself with
silk touch, and grows back into grass.

## Settings

*Added 2026-10-17.*

In the world's balance file, under `hazards`:

| Setting | Default | What it does |
| --- | --- | --- |
| `radiation` | `true` | Whether there is radiation at all: off, no dose, no contamination, no source, and the devices read nothing. |
| `natural_radioactivity` | `true` | Whether minerals with uranium or thorium in them are radioactive. |
| `mobs_suffer` | `true` | Whether creatures suffer hazards as players do. |
| `lungs` | `true` | Whether dust harms the lungs. |
| `body_temperature` | `true` | Whether a player's core warms and cools by the place, bringing hypothermia and heatstroke. |
| `item_hazards` | `true` | Whether what is carried harms its carrier. |
| `dissonance` | `true` | Whether laws of physics that depart from ours build up dissonance. |
| `pollution` | `true` | Whether there is pollution: soot, acid, heavy metals and carbon dioxide; fallout goes with radiation. |
| `withering` | `true` | Whether contamination withers the land. |
