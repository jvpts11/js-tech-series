# Progression

How far a player, or a team, has come, kept by J's Core so that any mod can read what another mod's player has
reached without depending on it.

## What exists today

Progress is counted along **axes**: named ladders of steps, each kept per player and saved with the world
([Progression](../PROGRESSION.md)).

- **The two axes of the series**: the **hardware eras** (`jscore:hardware_era`: Vintage, Legacy, Transition, Standard,
  Advanced, Exa, Singularity), the generation of the computers a player builds, and the **industrial tiers**
  (`jscore:industrial_tier`: T0 to T9), how far a player's industry has come. They are two ladders on purpose: a player
  can run an Advanced computer on a T2 industry, and the other way round; nothing derives one from the other.
- Any mod registers **axes of its own**. A player only moves forward on an axis, and the Core says once when they arrive
  at a step; an operator can put a player on any step (`/jstech progress`).
- **Gates in data**: a data file can ask for a step of an axis before something is allowed.
- **Advancements**: two triggers (something happened, a step was reached), and tabs of advancements written from code,
  with advancements that only exist when another mod is installed.

Today no mod moves the two axes by playing: only an operator does.

## To build

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

### Naming what is discovered

The **fictional elements** (from 119 on) can be named by the player who first discovers one on a server: the element
and its items take that name, kept with the world, sent to every player, and the same in every language. With the
setting off, the names the series gives them are used ([Materials](materials.md)).
