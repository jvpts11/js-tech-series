# Motion

J's Core lets any mod built on it make things on a screen move: a window growing in as it opens, a menu sliding
out of its panel, a window going down to its button. This page explains it from the start: what a motion is, how a
mod declares how each of its looks moves, how a resource pack changes that, and how a screen draws it.

## What a motion is

A motion is a change drawn over a short time instead of all at once. The Core describes one with four things:

- a **style**, the way it moves: `scale` (grows or shrinks about a point), `slide` (comes in from an edge),
  `zoom` (goes down to the place that stands for it, a window to its button, or comes back from there),
  `outline` (only the outline of the thing travels there, the way the oldest window managers drew it) and `fade`
  (a flat colour over the thing thins away);
- a **duration** and a **delay**, in milliseconds;
- a **curve**, which says how far along the motion is at each moment of its time;
- the **numbers** its style reads: how small a window starts, how far a menu slides, where the point it grows from
  is.

The curves are written as CSS writes them, which is also how the desktops' own toolkits write them: a cubic Bezier
curve, `cubic-bezier(0, 0, 0, 1)`, a straight line, `linear`, or a number of equal jumps, `steps(16)`, for the
oldest systems that moved a shape in a few steps. The usual curves have names: `ease`, `ease-in`, `ease-out`,
`ease-in-out`, `ease-in-quad`, `ease-out-quad`, `ease-in-cubic`, `ease-out-cubic`, `ease-in-sine`, `ease-out-expo`,
`fluent-entrance`, `fluent-point` and `fluent-exit`.

In code a motion is a `MotionSpec`, and one under way is a `Motion`: the spec and the moment it started. A `Motion`
holds no timer and is never ticked. Whoever draws it asks how far along it is at the time of the frame, which keeps
it smooth between the game's ticks and costs nothing once it is over.

## The clock

Every motion is read against one clock, `MotionClock`, on the player's game: the game's ticks counted since it
started, plus the part of the next tick already gone at the frame being drawn, in milliseconds. A motion is started
with `MotionClock.start(spec)` and read with `motion.progress(MotionClock.now())`.

A player can reduce motion. Then `MotionClock.start` gives a motion that is already over, so everything is drawn
where it ends, and `MotionClock.loopTicks()`, which drives what turns round and round (a running bar, a ring of
dots), stands still at its first position. A mod gives its players the switch in its own settings; J's Computers
has Reduce motion in its client settings.

## Profiles

How one look moves is a **profile**: for each **kind** of thing it moves, a motion. The kinds are names, so a mod
can add its own; the Core names the ones its desktops need in `MotionKinds`: `window_open`, `dialog_open`,
`window_close`, `window_minimize`, `window_restore`, `menu_show` and `scene_fade`. A kind a profile does not name
does not move.

A mod declares each profile once, in code, with the timings of the thing it imitates:

```java
public static final DeclaredMotion PLASMA = MotionProfiles.declare("jsc", "plasma", MotionProfile.builder()
        .kind(MotionKinds.WINDOW_OPEN, MotionSpec.of(MotionStyles.SCALE, 200, IEasing.named("ease-out-cubic"),
                "scale").with("from", 0.8))
        .kind(MotionKinds.MENU_SHOW, MotionSpec.of(MotionStyles.SLIDE, 150, IEasing.named("ease-out-cubic"),
                "sliding_popups").with("distance", 1.0))
        .build());
```

The data generation writes every declared profile to `assets/<mod>/motions/<name>.json`. The fourth argument of
`MotionSpec.of` is the **group**: the names of the switches that turn the motion off, separated by spaces. A screen
that lets a player switch effects off checks them; any one switched off keeps the motion still.

## Changing a profile with a resource pack

The file the data generation writes is the one the game reads back each time the resource packs load, so a pack
that holds its own `assets/<mod>/motions/<name>.json` replaces the profile:

```json
{
  "window_open": { "style": "scale", "duration": 400, "easing": "ease-out-expo", "from": 0.5 },
  "window_close": { "style": "none" }
}
```

Every field but `style` may be left out: the duration and the delay are then 0, the curve `linear`, and no switch
turns it off. A kind left out of the file does not move, so a pack that wants a look still writes an empty object,
`{}`. A file that cannot be read is reported in the log and the declared profile is kept.

The numbers each style reads:

| Style | Numbers |
|---|---|
| `scale` | `from` and `to`, the size it starts and ends at against its own (1 where not given); `from_y` and `to_y`, the same downwards when it grows unevenly; `pivot_x` and `pivot_y`, where it grows from, 0 to 1 across and down it (the middle by default) |
| `slide` | `distance`, how far away it starts, in its own heights; `out` set to 1 slides it away instead |
| `zoom` | `out` set to 1 goes to the place that stands for it; otherwise it comes back from there |
| `outline` | as `zoom`, and `trail`, how many outlines follow the leading one |
| `fade` | `out` set to 1 thickens the colour over the thing instead of thinning it away |

## Drawing a motion

A screen reads a `Motion` each frame and moves its pose by it: `scale(now)` and `scaleY(now)` for the size,
`offset(now)` for how far a slide still has to come, `toward(now)` for how far a zoom has got to its place, and
`progress(now)` for anything else. Scaling and sliding transform what is drawn without asking it to be drawn
anywhere else, so a window grows with every letter and picture of it in place; that is why they came first. Fading
a whole window needs it drawn off the screen first, and the Core does not do it yet; a flat colour can fade, which
is what `fade` does.

J's Computers' desktops are the worked example: `DesktopMotion` reads the system's profile less what the machine's
owner switched off, `DesktopPainter` draws each window in its motion, and a closed window leaves the desktop at once
and is drawn going away for as long as that takes. A monitor's face in the world shows everything where it ends.
