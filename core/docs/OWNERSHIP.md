# Ownership

Who a machine belongs to, who else may use it, and which team a player is on. J's Core keeps one answer to each
for every mod, so a machine of one mod and a robot of another agree on who may touch them.

## Teams

*Added 2026-10-01.*

```java
String team = CoreTeams.teamOf(server, playerUuid);
```

The answer is a team's id, stable for as long as the team exists. Where it comes from depends on what is
installed:

- With **FTB Teams** installed, its teams (and its parties) answer.
- Without it, the game's own teams (the `/team` command) answer.
- A player on no team is on a team of their own, so "same team" never lumps the teamless together.

A mod that brings its own kind of teams replaces the source once, while the game loads:
`CoreTeams.use(new MyTeamSource())`, where `ITeamSource` answers `teamOf(server, uuid)`. The log says when a
source is replaced, since only one can answer.

## Owners and access

*Added 2026-10-05.*

```java
Ownership ownership = Ownership.of(placer.getUUID())     // private: the owner only
        .withAccess(Access.TEAM);                        // or PUBLIC: everyone

if (!ownership.mayUse(player)) {
    player.sendSystemMessage(GameText.component(ownership.denial(server)));   // "This belongs to ..."
    return InteractionResult.FAIL;
}
```

- `Access`: `PRIVATE` (the owner), `TEAM` (the owner's team), `PUBLIC` (everyone).
- A block entity or entity that has an owner implements `IOwned` and answers `ownership()`; null means it is
  nobody's, and anyone may use it. Jade shows "Owner: ..." for it ([Overlays](OVERLAYS.md)).
- `Ownership` has a codec and a stream codec, so it is saved and sent like any value.
- **Operators pass every lock.** The permission `jscore.ownership.bypass` (operators of level 2 and up, unless a
  permission mod says otherwise) lets a player use anything; `CorePermissions.mayPassOwners(player)` asks it.

## What can go wrong

- **Two players on one team are told no.** The team source in use is not the one their team is in: with FTB Teams
  installed, `/team` teams are not asked.
- **Nobody can use a machine.** Its owner is a player who left; an operator can still use it and change it.
