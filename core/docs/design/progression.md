# Progression

How far a player, or a team, has come, kept by J's Core so that any mod can read what another mod's player has
reached without depending on it.

## What exists today

Everything this page designs is built, as of October 2026, except what *Still to build* at the end lists: the axes and
the eras as a registry, axes that move by playing, progression kept per team, milestones, knowledge and the knowledge
gate, the knowledge seal of structures, research with its discoveries and its kinds of data, advancements, and naming
what is discovered. How a mod uses it, with code:
[Progression](../PROGRESSION.md).

## The design

### Eras as a registry

The eras stop being a closed list. Each era has an **id**, a name, a colour and a **position in the order**, and is
saved by its id; an add-on's era goes after Singularity or between two eras, and every rule that asks for "this era or
later" follows the order. It touches what blocks have saved, so it is done in one go, together with J's Computers'
registries of hardware and computers, when the first mod that needs them starts. Breaking alpha worlds is acceptable.

### The axes move by playing

- The **hardware era** moves forward the first time a player builds a computer of that era (J's Computers).
- The **industrial tier** moves forward the first time a player makes that tier's circuit (J's Industrial).

### Progression kept per team

Besides what a player reaches, the Core keeps what a **team** reaches, by one rule for every mod: a player's team is
the one [teams](platform.md#teams-and-owners) say (FTB Teams when installed, the game's teams otherwise, the player
alone if on none). **What a team reaches is the team's alone**: a player who leaves takes nothing with them, a player
who joins has all the team has, and two teams that merge keep everything either of them had.

- **Milestones**: a mod declares an ordered set of milestones, and a team reaches each one once. J's Space declares its
  firsts (a suborbital flight, an orbit, a landing on the Moon, a planet, a star, a galaxy), and any mod reads them: J's
  Industrial can ask for "orbit reached" before a discovery about space. Milestones sit beside the tier axis, never
  mixed with it.
- **Knowledge**: a team knows things, and any mod can say what a piece of knowledge is and give it: J's Industrial's
  research, J's Space's discovery of each world and its Enigmatic comprehension (a share from 0 to 100%), the
  milestones.

### The knowledge gate

Written once for every mod: a **recipe may ask for a piece of knowledge**.

- **JEI and EMI always show the recipe**, with a lock and the knowledge it needs; a manual shows it too. A recipe is
  locked for running, never for seeing: a player can always plan.
- A **machine refuses to run** a locked recipe until its **owner's team** has the knowledge.
- A **structure** can keep a door or a chamber shut behind a piece of knowledge, such as J's Space's sealed chambers
  ([The world](world.md)).

### Research

Research is written once in the Core, so J's Industrial's Computational Research Centre and J's Space's own research
computer stand on the same thing and neither mod needs the other.

- **A discovery** is declared in code by its mod, with a builder: its name and its date, the research it costs, the
  data it asks for and its prerequisites. It is a piece of knowledge a team holds **in part** while its research goes,
  its share the progress made, and whole once it is done; only then does it open what it unlocks.
- **Research is counted in points a second**, which research computers make; what each discovery costs is content,
  set in data, so a modpack changes it.
- **Data come in kinds**, an open list any mod adds to: sample, crystal, spectral and particle data now, astronomical
  data with J's Space, singularity data with the Aleph. Data are items, spent as the research goes, each one covering
  an equal share of it.
- **Prerequisites**: other discoveries, knowledge the team has, milestones. A discovery starts only once they are all
  known, and one that asks for what a mod not installed gives does not exist.
- **The whole tree shows**, every discovery with what it needs, so a player can plan.
- **A mod may open the gate**: with its setting off, none of its discoveries locks anything and none is granted;
  research can still be done, for its advancements.

### Naming what is discovered

The **fictional elements** (from 119 on) can be named by the player who first discovers one on a server: the element
and its items take that name, kept with the world, sent to every player, and the same in every language. With the
setting off, the names the series gives them are used ([Materials](materials.md)).

## Still to build

- **The hardware eras' axis read from the registry of eras**: the mechanism is built, and the axis J's Computers'
  hardware reads moves onto it when J's Computers does.
