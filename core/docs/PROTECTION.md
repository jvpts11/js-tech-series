# Protection

How a mod on J's Core keeps what it does to the world inside what a server allows: one question asked before a robot,
a machine or a structure changes a place, permissions a permission mod can grant or deny, players' limits, an audit
log of what is done to others' things, and damage between players held to the server's rule and the teams.

## The one question

*Added 2026-10-15.*

```java
if (!Protection.may(Actor.forOwner(owner), ProtectedAction.BREAK, level, target)) {
    return Status.FAILED;   // a claim or a protection keeps it out
}
```

- **The actor** is who answers for the work: `Actor.of(player)`, a player in person; `Actor.forOwner(uuid)`, a
  machine, a robot or a moving structure acting for its owner, weighed as that owner whether they are online or not;
  `Actor.world(origin)`, nobody, the world itself (a fire spreading, a fluid flowing, an explosion with no one behind
  it), weighed by where it comes from.
- **The action** is what it would do: `BREAK`, `PLACE`, `USE`, `HURT`, `EXPLODE`, `BURN`, `FLOW`, `MOVE` (a moving
  structure entering a place), `SHOOT`.
- **Every protection installed answers**, and one that says no stops the work: the game's spawn protection (on a
  dedicated server, within its radius, for everyone but operators), **FTB Chunks** and **Open Parties and Claims**
  when they are there. `Protection.stoppedBy(...)` says which one stopped it.
- The world's own work stays inside the claim it comes from: a fluid may flow, and a fire spread, within one claim or
  out of one into unclaimed land, never into a claim from outside. FTB Chunks lets an explosion in only where the
  claiming team allows explosions.
- The question does not post the game's own events; work that a player could do also posts them as the player would
  (a robot breaking a block posts the block break event under its owner's stand-in), so a protection mod that only
  listens to the events still sees it.

A mod with protections of its own registers one, from its constructor:

```java
Protection.register("mymod:zones", (actor, action, level, pos) ->
        Zones.isSafe(level, pos) && action.changesBlocks() ? Verdict.DENY : Verdict.ABSTAIN);
```

`IProtectionAdapter.claimOwner(level, pos)` says who holds the claim a place is in, which is how the world's own work
is kept inside one: `Protection.sameClaim(adapter, level, origin, pos)`.

## Machines act for their owner

*Added 2026-10-15.*

A machine never acts as one generic fake player that every protection lets through or stops alike. The question
weighs its owner directly; where the game itself asks who is acting, `MachineActors.standIn(level, owner)` is a stand-in
under the owner's id and the name the server knew them by. Robots do both.

## Permissions

*Added 2026-10-05; for everything 2026-10-15.*

Everything the Core lets or stops goes through NeoForge's permission API, so LuckPerms and FTB Ranks decide; without
one, the operator level each permission was declared with applies.

| Node | Held by default | What it lets a player do |
| --- | --- | --- |
| `jscore.command.<name>` | the command's level | Run `/jstech <name>`; a source at the command's level always may. |
| `jscore.owned.use_others` | operators (2) | Use what other players own, whatever its access says. |
| `jscore.owned.open_others` | operators (2) | Open the machines other players own. |
| `jscore.owned.set_up_others` | operators (2) | Change how other players' machines are set up. |
| `jscore.owned.break_others` | operators (2) | Break what other players own. |
| `jscore.config.edit` | operators (2) | Change a server's settings from the settings screen. |
| `jscore.multiblock.build` | every player | Build multiblock structures. |
| `jscore.limits.bypass` | operators (2) | Go past the limits each player has. |

A mod declares its own the same way, from its constructor, and they are written out with the Core's:

```java
public static final PermissionNode<Boolean> LAUNCH = CorePermissions.declare(MODID, "rocket.launch",
        CorePermissions.OPERATORS, LAUNCH_TEXT);
// later, on the server:
if (CorePermissions.has(player, LAUNCH)) { ... }
```

`/jstech dump permissions` writes every node of the series into `jstech-permissions/` beside the server: `nodes.md`,
the table above for every mod installed, and `luckperms.txt`, the console commands that make two LuckPerms groups,
`jstech-players` with what every player holds by default and `jstech-admins`, built on it, with the rest.

## Limits per player

*Added 2026-10-15.*

How many of a kind of thing one player may have at once, so nobody can bring a server down on purpose. A kit declares
its limit with what sets it, takes one as a player gets one and gives it back as it goes; the counts are kept with
the world.

```java
public static final Limit CRANES = PlayerLimits.declare(id("cranes"), CRANES_NAME, Balance::cranesPerPlayer);

if (!PlayerLimits.take(server, owner, CRANES)) {
    player.sendSystemMessage(GameText.component(PlayerLimits.reached(server, owner, CRANES)));
    return;
}
// ... and when the crane goes:
PlayerLimits.giveBack(server, owner, CRANES);
```

The Core's own: **robots** (`protection.robots_per_player`, 16 by default). `RobotEntity.giveTo(owner)` takes one and
says no at the limit; a robot killed or discarded gives it back, one only unloaded with its chunk does not.

## The audit log

*Added 2026-10-15.*

Off by default (`protection.audit`). Kept on, it writes down who used, changed or broke what another player owns, with
when, what and where, the oldest entries making way past `protection.audit_entries` (10,000). What owners do to their
own things is not written. The Core writes it for owned blocks and entities used or broken; a mod writes its own with
`AuditLog.record(level, pos, actor, AuditLog.Kind.CHANGE, thing, owner)`.

`/jstech audit [page]`, `/jstech audit player <name> [page]` and `/jstech audit at <x y z> [page]` read it, newest
first, ten to a page.

## Damage between players

*Added 2026-10-15.*

When a player is hurt by another player, or by a machine or robot another player owns, the damage is held to:

- the server's PvP rule;
- the claim the victim stands in (FTB Chunks' "allow PvP");
- the teams: players on one team do not hurt each other unless `protection.team_damage` is on. While the teams are
  the game's scoreboard teams, a team's own friendly fire decides when it is off.

`Protection.mayHurt(attacker, victim)` asks the same, for a kit that deals damage of its own.

## What can go wrong

- **A robot will not break a block its owner could.** A claim keeps its owner out there, or the spawn protection does
  and the owner is no operator: `Protection.stoppedBy` names the protection.
- **A player granted a permission still cannot run the command.** The node is `jscore.command.<name>` for every command
  of the series, whichever mod declared it.
- **The audit command says the log is off.** Turn on "Keep the audit log" in the world's settings.
- **LuckPerms does not list the series' nodes.** It does not find NeoForge mods' nodes by itself: run
  `/jstech dump permissions` and paste `luckperms.txt` into the console.
