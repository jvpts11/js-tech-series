# Notifications

How a mod on J's Core tells a player something: a notification on the Core's HUD, not one of the game's toasts, whose
way of being drawn changes with every version of the game. It has a level, alike ones become one with a count, a key
opens the history, a click opens the right place, and the player chooses where they appear and what to silence.

## A kind, then a notification

*Added 2026-10-15.*

A mod declares each kind of notification once, with its level and what the player calls it:

```java
public static final TextKey OVERVOLTAGE_NAME = TextKey.of("mymod.notification.overvoltage", "Overvoltage");
public static final NotificationKind OVERVOLTAGE = Notifications.kind(rl("overvoltage"), NotificationLevel.ALERT,
        OVERVOLTAGE_NAME).declare();
```

and sends notifications of it from the server:

```java
Notifications.sendToTeamOf(server, owner, OVERVOLTAGE.of(OVERVOLTAGE_NAME.text(), BURNED.with(where))
        .withAction(new NotificationAction.ShowPlace(level.dimension(), pos)));
```

- `send(player, notification)` sends to one player; `sendTo(server, uuid, ...)` to a player if they are on;
  `sendToTeamOf(server, uuid, ...)` to every player on that player's team, the way a machine tells its owner.
- Something the player's own game finds out is shown there with `NotificationsClient.show(notification)`.
- A player whose game has no Core, or a fake player, is sent nothing.

## Levels

| Level | Icon | Title | Sound | On the screen |
| --- | --- | --- | --- | --- |
| Information | a blue disc | white | the game's toast coming in | 5 seconds |
| Warning | a yellow triangle | yellow | a note block's pling | 8 seconds |
| Alert | a red diamond | red | a note block's bell | 12 seconds |

A kind may stay on the screen longer or shorter: `.lasts(Duration.ofSeconds(20))`. The colours come from the player's
style, and the sounds play in the Interface channel of the Sound Mixer.

## On the HUD

A notification is drawn in the shape of the game's toast, in the frame of the player's style: its icon, its title in
its level's colour, how many alike came (`x3`), what it says, the mod it came from, what a click does, and a line along
its foot that shortens as its time runs out. Up to five are on the screen at once, the newest nearest the corner.

- **Alike notifications become one**: of one kind and one title, while they keep coming within a minute of each other.
  The count goes up and it stays on the screen anew; it sounds again only if it had left the screen.
- **A click** while the chat is open does what it offers: `OpenManual(manual, entry)` opens a manual at an entry,
  `ShowPlace(dimension, pos)` shows the place on a map with how far it is.

## The history

*Added 2026-10-15.*

**N** (a key the player can change, "Notification history") opens the recent notifications, newest first, each with
when it came and a cross that takes it off the list. Buttons over the list show only alerts, warnings or information,
only one mod, and move the notifications to another corner. A click on a line does what it offers; the right button
on one silences its kind, or everything of its mod, and hears it again. The footer turns **do not disturb** on and
off: nothing comes on the screen or sounds, and the game's own toasts are held too, while everything is still kept in
the history.

## The player's settings

`jstech-notifications.toml`, in the player's own game, which the settings screen shows too:

| Setting | Default | What it does |
| --- | --- | --- |
| `notifications.place` | top_right | The corner they appear in: top_right, top_left, bottom_right or bottom_left. |
| `notifications.do_not_disturb` | false | Nothing comes on the screen or sounds; the history keeps it all. |
| `notifications.hold_game_toasts` | true | Do not disturb holds the game's own toasts (advancements, recipes) too. |
| `notifications.silenced_kinds` | none | The kinds that never come on the screen, by id. |
| `notifications.silenced_mods` | none | The mods whose notifications never come on the screen, by id. |
| `notifications.history` | 100 | How many the history keeps (10 to 1,000). |

## What can go wrong

- **A kind is declared twice.** The game stops loading, naming the kind: declare it once, as a constant.
- **A notification never shows.** Its kind or its mod is silenced, or the player is not to be disturbed; it is in the
  history all the same.
- **A notification shows its key instead of its words.** The player's game lacks the mod that sent it, or its language
  file; the title travels as its key.
