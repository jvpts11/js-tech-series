# Progression

How far a player has come, and the advancements that mark the way. J's Core counts progress along **axes**: named
ladders of steps a player climbs, each kept per player and saved with the world.

## The two axes of the series

*Added 2026-04-28.*

- **Hardware eras** (`HardwareEra`): Vintage, Legacy, Transition, Standard, Advanced, Exa, Singularity, and any era
  an add-on declares between them ([Eras as a registry](#eras-as-a-registry)). The generation of the computers a
  player builds.
- **Industrial tiers** (`IndustrialTier`): T0 to T9. How far a player's industry has come.

They are two different ladders on purpose: a player can run an Advanced computer on a T2 industry, and the other
way round. Nothing in the series derives one from the other.

## Axes

*Added 2026-10-05.*

An axis is registered once, with its steps:

```java
public enum Rank implements IAxisStep {
    CADET(0), PILOT(1), CAPTAIN(2);
    // level(), serializedName(), text() as IAxisStep asks
}

public static final ProgressionAxis<Rank> RANK =
        ProgressionAxis.of(rl("rank"), MyTexts.RANK, List.of(Rank.CADET, Rank.PILOT, Rank.CAPTAIN));

// in your mod's constructor:
ProgressionAxes.register(RANK);
```

Steps are numbered from 0 with no gaps, and each has a name used in files and commands. The Core registers the
two axes above as `jscore:hardware_era` and `jscore:industrial_tier`.

```java
PlayerProgress.reach(server, playerUuid, RANK, Rank.PILOT);   // moves forward only; false if already past it
Rank now = PlayerProgress.reached(server, playerUuid, RANK);
```

- `reach` only ever moves a player forward, and posts an `AxisStepReachedEvent` on the game's event bus once, when
  they arrive.
- `set` puts a player on any step, back included; it is for operators (`/jstech progress`, see
  [Commands](COMMANDS.md)).
- The player's side knows its own progress: `PlayerProgress.reachedHere(axis)`, as the server last said.
- A data file can gate something on a step with a `ProgressionGate` (`{"axis": "jscore:industrial_tier", "step":
  "t3"}`), which `passes(server, player)` once the player reached that step.

## Eras as a registry

*Added 2026-10-16.*

The eras of hardware are a registry (`Eras`), not a closed list, and `Era` is the one type of era in every mod. Each
era has an id, saved in worlds, a name, a colour and the year its machines are made in, which the firmware and the
installers print; the Core declares the series' seven as the constants of `HardwareEra` (`HardwareEra.LEGACY` is
`jscore:legacy`), and an add-on declares its own after the last or right after any other, while the game loads:

```java
public static final Era QUANTUM = Eras.declareAfter(
        new Era("myaddon:quantum", MyTexts.QUANTUM.text(), 0xFF00FF, 2034), HardwareEra.ADVANCED);
```

- `Eras.ordered()` is the order, earliest first, and `era.isAtLeast(other)` asks "this era or later", as every rule of
  the kind should; a rule of one era alone compares with `==`. Once the registry closes, both are a read by the era's
  place.
- `EraCodecs.CODEC` saves an era by its id and refuses one no mod declared; a data file may name one of the series'
  seven bare (`"legacy"`). A packet sends an era by its place in the order, which both sides agree on.
- **Values era by era** go in an `EraTable`: given for some eras, and an era without its own takes the nearest earlier
  era's. The series' Exa and Singularity take the Advanced's that way, and so does every era an add-on brings, so a
  rule written era by era answers for any era a world has.

```java
private static final EraTable<Integer> HUB_PORTS = EraTable.of(2)
        .from(HardwareEra.LEGACY, 4)
        .from(HardwareEra.STANDARD, 7);
int ports = HUB_PORTS.get(era);
```

*Changed 2026-10-25.* Progress along every axis is saved by the step's name, so an era an add-on inserts moves no step
a world has saved. A world saved before then starts its progress over. The registry closes once the game has loaded.

## The axes move by playing

*Added 2026-10-16.*

A mod says what moves a player along an axis the first time they do it:

```java
ProgressionTriggers.whenMade(MyItems.T2_CIRCUIT.get(), SeriesAxes.INDUSTRIAL_TIER, IndustrialTier.T2);
ProgressionTriggers.whenMade(MyTags.LEGACY_BOARDS, SeriesAxes.HARDWARE_ERA, HardwareEra.LEGACY);
ProgressionTriggers.whenPlaced(MyBlocks.LEGACY_PC.get(), SeriesAxes.HARDWARE_ERA, HardwareEra.LEGACY);
```

Making counts at a crafting table, in a furnace, and in any of the Core's processing machines, for the machine's
owner; a mod's own way of making things calls `ProgressionTriggers.made(server, player, stack)`. Each move forward is
told once, as `reach` tells it. The hardware era moves as J's Computers' computers are built, and the industrial tier
as J's Industrial's circuits are made, each declared by its mod.

## What a team reaches

*Added 2026-10-16.*

Besides what each player reaches, the Core keeps what each **team** reaches (`TeamProgress`): its furthest step along
each axis, the milestones it has passed, and how much it knows of each piece of knowledge. A player's team is the one
[teams](OWNERSHIP.md#teams) say: FTB Teams' when installed, the game's own otherwise, or the player alone.

- **What a team reaches is the team's alone.** A player who leaves takes nothing with them; a player who joins has all
  the team has.
- **Two teams that merge keep everything either had**: `TeamProgress.merge(server, from, into)` gives `into` the
  further step of each axis, every milestone and the more of each piece of knowledge.
- A player reaching a step (`PlayerProgress.reach`) moves their team there too.
- What a team has is sent to its players: `TeamProgress.here()` on their side.

Each move forward is posted on the game's bus once: `TeamProgressEvent.StepReached`, `MilestoneReached` and
`KnowledgeGained`.

### Milestones

A mod declares its firsts as an ordered set, while the game loads, and a team reaches each once:

```java
public static final List<Milestone> FIRSTS = Milestones.declare(rl("firsts"), List.of(
        new Milestone(rl("suborbital"), MyTexts.SUBORBITAL),
        new Milestone(rl("orbit"), MyTexts.ORBIT)));

TeamProgress.reach(server, team, FIRSTS.get(1));          // once; false after
boolean orbited = TeamProgress.reached(server, team, FIRSTS.get(1));
```

Any mod reads them. Each milestone is also a piece of knowledge of the same id, known once it is reached, so a recipe
can ask for one. Milestones sit beside the axes, never mixed with them.

### Knowledge

A team knows things, and any mod says what a piece of knowledge is and gives it:

```java
public static final Knowledge FUSION = Knowledges.declare(Knowledge.of(rl("fusion"), MyTexts.FUSION));
public static final Knowledge COMPREHENSION = Knowledges.declare(new Knowledge(rl("enigma"), MyTexts.ENIGMA, true));

TeamProgress.grant(server, team, FUSION, 1.0);            // known whole
TeamProgress.grant(server, team, COMPREHENSION, 0.4);     // 40% of it
boolean known = TeamProgress.knows(server, team, FUSION.id());
```

Knowledge comes whole, or, for one declared partial, by shares from 0 to 1, and is known once all of it is. A grant
of less than the team knows changes nothing.

## The knowledge gate

*Added 2026-10-16.*

A recipe of the Core's processing machines may ask for a piece of knowledge:

```json
{"type": "myaddon:pressing", "inputs": [...], "outputs": [...], "knowledge": "myaddon:fusion"}
```

or `ProcessingRecipeBuilder.needs(id)` in the data generation. A machine whose owner's team lacks it will not run it:
it passes the recipe over, and with nothing else to make it says `Needs knowledge` (`MachineStatus.LOCKED`); once the
team learns it the machine takes it up within a few seconds. A machine with no owner runs nothing locked.

A recipe is locked for running, never for seeing: JEI, EMI and the manuals always show it, with a padlock and the
knowledge it needs in the locked colour while the player's team lacks it, so a player can always plan.
`KnowledgeGate.allows(server, owner, knowledge)` is the same rule for a mod's own machines.

*Added 2026-10-24.* A mod may open knowledge to every team whatever they know, with
`KnowledgeGate.openWhen(opener)`: research turned off does that ([Research](#research)). And
`KnowledgeGate.opens(level, knowledge)` names what the recipes that ask for a piece of knowledge make, as a research
screen tells a player what a discovery opens; it reads every recipe, so a screen asks it once and keeps the answer.

## Research

*Added 2026-10-24.*

Research, for any mod, in `dev.jstech.core.research`: discoveries a team makes with research computers. A discovery
is a piece of knowledge the team holds in part while its research goes, its share the progress made, and whole once
it is made, when the recipes that ask for it open. So the progress is the team's, whichever computer worked it, and
nothing is lost when a computer stops.

**Kinds of data**, an open list any mod adds to, each with its item:

```java
public static final DataKind CRYSTAL = DataKinds.declare(new DataKind(rl("crystal"), MyTexts.CRYSTAL));
public static final ItemEntry<ResearchDataItem> CRYSTAL_DATA = CONTENT.item("crystal_data",
        properties -> new ResearchDataItem(properties, CRYSTAL, MyTexts.CRYSTAL_FROM)).named("Crystal Data")
        .register();
```

The item's tooltip says what gives it (`CRYSTAL_FROM`: "From the X-ray Diffractometer"); `DataKinds.of(stack)` says
what kind an item is.

**Discoveries**, declared with a builder, their names and dates as `TextKey`s of a `@TextHolder` class:

```java
public static final Discovery HIGH_TC = Discovery.builder(rl("high_tc"), MyTexts.HIGH_TC)
        .when(MyTexts.HIGH_TC_WHEN).year(1986).field(MyTexts.SOLID_STATE)
        .costs(36_000).needs(CRYSTAL, 10)
        .after(SUPERCONDUCTIVITY)
        .declare();
```

- `costs` is the research it takes, in points; `needs` how many data of a kind, each covering an equal share of the
  way: the first of ten is spent as the research starts, the next as it passes a tenth, and so on.
- `after` takes another discovery, any knowledge, or a milestone: the research starts only once the team knows them
  all. `year` and `field` lay the discovery out in a tree by the year it was made and its field of science; a
  discovery not made yet in the real world has no year.
- What a discovery costs is content: the data generation writes each declared cost to
  `data/<namespace>/jstech/research/<path>.json` (`DeclaredFilesProvider.researchCosts`), and a data pack's file of
  the same id says otherwise:

```json
{"points": 36000, "data": {"myaddon:crystal": 10}}
```

- A discovery whose cost names a kind of data no installed mod declares, or that comes after knowledge no installed
  mod gives, is not there to research: `Discoveries.exists(level, discovery)`, and `Discoveries.existing(level)` lists
  the ones that are.

**Working it on.** A research computer works once a second, with the points it made in that second, through
`Research.work(server, team, discovery, points, store)`; the store (`Research.IDataStore`) says how many data of each
kind it holds and gives up what is spent. The research goes as far as the points go, but never past the edge of the
last share its data pays for, and not at all before the team knows what the discovery comes after. It answers
`WORKED`, `NEEDS_DATA`, `LOCKED`, `MADE` or `ABSENT`. `Research.check` answers the same without spending, for a
computer to ask before it draws power. `Research.share`, `made`, `ready` and `missing` say where a team is. The
arithmetic alone, with nothing of the game, is `ResearchMath`, with the power of several computers working together:
`ResearchMath.groupShares(powers, eachOther)` gives the strongest its whole and each other `eachOther` of its own.
J's Industrial's Computational Research Centre is a research computer built on all this.

**Turning research off.** A mod's own setting may turn its research off: `Discoveries.gate(MODID, MySettings::require)`.
Off, nothing that asks for one of its discoveries is locked and none is granted; research can still be done, for its
advancements. The server sends every player which mods turned theirs off, once a second when it changes, so the recipe
viewers and the manuals show no padlock either.

## Commands

*Added 2026-10-16.*

- `/jstech knowledge <players>`: what their teams know.
- `/jstech knowledge <players> grant <id> [<share>]` and `forget <id>`: giving and taking a piece of knowledge.
- `/jstech milestone <players> <id>`: a milestone reached for their teams.

## Advancements

*Added 2026-10-05.*

The Core gives two triggers an advancement can wait for, and writes the advancement files from code.

- **`jscore:event`** is met when code says something happened: `Advancements.award(player,
  rl("first_network"))`, optionally with a detail (`"jsc:frames_95"`). Nothing happens for no player or for a
  machine acting as one.
- **`jscore:axis_step`** is met when a player reaches a step of an axis, or any later one. It is checked again when
  a player logs in, so a step reached while they were away still counts.

A tab of advancements is a class, declared root first and each advancement after its parent:

```java
public final class MyAdvancements extends AdvancementTab {

    public MyAdvancements() {
        super(MyMod.MODID, "main", ResourceLocation.withDefaultNamespace("textures/block/stone.png"));
        root(MyContent.KILN, "My Mod", "Things built with My Mod", on("first_kiln"));
        task("hot", "root", Items.BLAZE_POWDER, "Hot Hot Hot", "Fire a kiln", on("fired"));
        goal("pilot", "hot", Items.ELYTRA, "Wings", "Become a pilot", reached(RANK, Rank.PILOT));
    }
}
```

- `on("fired")` waits for the event `mymod:fired`, the id your code awards with `Advancements.award`; `on("fired",
  "detail")` for that event with exactly that detail.
- `task`, `goal`, `challenge` and `secret` (a task nobody sees until they earn it) each take a name, the name of
  their parent, an icon, an English title and description, and what earns them. `challengeOfAll` asks for several
  things at once; `goalWith(otherMod, ...)` only exists when that mod is installed.

Hand the tabs to the data generation:

```java
final ContentData data = ContentData.gather(event, MyContent.CONTENT)
        .alsoNaming(names -> new MyAdvancements().translations(names));
data.server(new ConditionalAdvancementProvider(data.output(), data.lookup(),
        () -> List.<AdvancementTab>of(new MyAdvancements())));
```

The provider writes `data/<namespace>/advancement/<tab>/<name>.json`; `alsoNaming` puts the titles and
descriptions in your English file.

### A wide background

*Added 2026-10-28.*

The game draws a tab's background as 16-pixel tiles, so any picture that small repeats every 16 pixels. A mod can
give a tab's tile a wide picture, at least as big as the tab's window (234 by 113), drawn once behind the tree and
sliding at half its speed as the player drags it, so it never repeats and the window never reaches its edge. Declare
it on the player's game, from the mod's client entry point:

```java
WideBackgrounds.declare("mymod:textures/gui/advancements/sky.png",
        "mymod:textures/gui/advancements/sky_wide.png", 400, 200);
```

The tab still names the tile as its background, and the tile is what is drawn where the Core is not installed, so it
should be a fair piece of the wide picture.

## What can go wrong

- **An advancement never pays.** Its event id is not the one the code awards, or the award is given to a fake
  player (a machine).
- **A step does not move.** `reach` refuses to go back; an operator uses `set` for that.
- **A machine says `Needs knowledge`.** Its recipe asks for knowledge its owner's team lacks, or it has no owner.
- **A research stops short.** `NEEDS_DATA`: the data held covers no further; put more of the kind it asks for in the
  computer. `LOCKED`: the team does not know yet what the discovery comes after (`Research.missing` says what).
  `ABSENT`: its cost names a kind of data, or it comes after knowledge, that no installed mod declares.
- **The game stops while loading, naming an era, a piece of knowledge or a milestone.** It is declared twice, after the
  game has loaded, or after an era no mod declared before it.
