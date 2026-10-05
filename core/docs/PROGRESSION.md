# Progression

How far a player has come, and the advancements that mark the way. J's Core counts progress along **axes**: named
ladders of steps a player climbs, each kept per player and saved with the world.

## The two axes of the series

*Added 2026-04-28.*

- **Hardware eras** (`HardwareEra`): Vintage, Legacy, Transition, Standard, Advanced, Exa, Singularity. The
  generation of the computers a player builds.
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

## What can go wrong

- **An advancement never pays.** Its event id is not the one the code awards, or the award is given to a fake
  player (a machine).
- **A step does not move.** `reach` refuses to go back; an operator uses `set` for that.
