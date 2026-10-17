# Hazards

What harms a body from where it is and what it carries: radiation, a pollution, dust in the lungs, the air of
another world, a hot ingot in the hand. J's Core gives every mod one kit for them: kinds of harm and what resists
each, hazards that build up in a body and harm it in bands, the hazards of carried things, the lungs, the devices that
measure them and the screen of the body, maps of the chunks that were measured, and the land withering where it is
contaminated. It is in the spirit of the radiation of HBM's Nuclear Tech and Mekanism and the hazards of GregTech.

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
`jscore:damage_kind/cryogenic`, lightning and the Core's electric shocks in `jscore:damage_kind/electrical`. A data
pack adds any mod's damage to a kind the same way.

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

The **body's screen**, on its own key (B until the player changes it), is a worn panel of old green enamel: what is
worn and how much of each kind it stops, the dose on a big meter with the radiation's bands painted on its scale, the
rate here on a meter of two needles (from outside and from what is on the body) with a lamp asking for a shower, the
lungs on an edgewise meter, dissonance in five lamps, and the other hazards on the body.

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
| `item_hazards` | `true` | Whether what is carried harms its carrier. |
| `withering` | `true` | Whether contamination withers the land. |
