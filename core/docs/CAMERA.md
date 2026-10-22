# The camera

*Added 2026-10-21.*

What the player sees through, from and around: lenses that magnify by real powers, the view following what the
player rides, shaken and leant by what any mod asks, looking about freely in a cockpit, a mod's camera in the third
person, scenes along camera paths, the view of an entity or from a point elsewhere, and cameras in the world whose
view a screen shows.

## Lenses

```java
public static final ItemEntry<ZoomItem> BINOCULARS = CONTENT.item("binoculars",
                properties -> new ZoomItem(properties, List.of(4.0, 8.0),
                        Optional.of(rl("textures/misc/binoculars.png"))))
        .named("Binoculars")
        .register();
```

A `ZoomItem` is looked through while it is held in use, as a spyglass is. Its powers are real magnifications: a lens
of 8 narrows the field of view so the tangent of half of it is an eighth of the eye's, and the mouse turns the view
eight times slower. The mouse wheel steps between its powers while the player looks, and the power last chosen is
where it starts next time. Its picture, if it has one, covers the screen as large as its height, the rest dark, as
the spyglass's does.

A block looked into (J's Space's telescopes) magnifies the player's view on their game with
`CameraZoomClient.set(power)`, and `CameraZoomClient.clear()` takes it back. The planets of the sky are drawn at their
angular size, so a telescope shows them as large as it magnifies.

## Riding

Riding a vehicle, the player turns with it, as a boat turns its rider: the camera behind follows its turn, and the
view ahead keeps facing where it faced on it. The view rolls with the vehicle's lean, or with the moving structure
whose seat the player sits in, in the first person and behind alike. A moving structure turns its riders already.

Others see the riders of a leaning vehicle lean with it: each sits where its seat is on the leaning body, on the
server and on every player's game, and is drawn tilted with the vehicle's pitch and roll, an aircraft's pilot banking
with its wings.

## Shakes and leans

```java
CameraMotion.shake(CameraShake.of(0.6, 1.5).from(blast, 24));   // felt up to 24 blocks off, weaker the farther
CameraMotion.lean(this, 0, 0, bank, 0.4);                         // the view banks with the aircraft
CameraMotion.release(this);                                       // and comes back level
```

Any mod moves the view through one service on the player's game, so shakes from two mods add up instead of fighting.
A shake has a strength from 0 to 1 (a few degrees each way at 1, the square of its strength, so a light one is a
tremor), a time it dies away over, how rough it is (`rough(timesASecond)`), and, when it comes from a place, how far
it is felt. It wanders smoothly, never repeating as a sine would. A lean holds the view turned by its owner, eased
there and back as fast as it can without swinging past, each owner's turn its own. The player's shake setting scales
every shake, and asking for less movement stills them; leans are not stilled, as they follow what the player rides.
A blast's shake in the effects (`ScreenEffects`) goes through the same service.

## In the third person

```java
ThirdPersonCameras.register(rl("tank"), 10, (viewer, front, partialTick) ->
        viewer.getVehicle() instanceof Tank tank
                ? ThirdPersonView.behind(9).shoulder(1.2, 0.6).around(tank.turretAt(partialTick)) : null);
```

A mod's camera in the third person says, every frame, where it stands, or gives none to leave the view to the next
camera, the highest priority first, and in the end to the game. A view stands back as far as it says (the game's
stands 4 blocks back), over a shoulder, to the right of the view and up it, and round a place of its own instead of
the eye: the middle of a long vehicle. A wall between it and where it would stand brings it nearer, as the game's
own is brought nearer, short of the wall. A change from one view to another, or back to the game's, is eased over
the view's time (0.3 seconds unless `easedOver` says), at once when the player asks for less movement or has just
come into the third person. With no camera giving a view, the game's own camera is left as the game and other mods
set it.

## Free look

While the Free Look key is held (it has no key until the player gives it one), the mouse turns the view and never the
player, so a vehicle steered by where the player faces keeps its course; let go, the view comes back ahead over a
moment.

## Views from elsewhere

```java
CameraViews.play(player, rl("launch"), pad.getBottomCenter(), 0.0F);  // a scene along a camera path
CameraViews.watch(player, robot);                                      // what an entity sees
CameraViews.lookFrom(player, periscopeTop, yaw, pitch, true);          // the view from a point, turned by the player
CameraViews.end(player);
```

The server starts a view for one player; their body stays where it is and still, and sneaking skips a scene or leaves
a view. A scene ends at its last keyframe, a watch when its entity is gone, and the Core's events say how each ended
(`CameraViewEvent`, `camera.view_ended`: finished, skipped, lost or stopped), so the mod that started a scene carries
on after it. A view shows only what the player's game has: an entity watched or a point looked from must be in a
chunk the server sends the player. While a view shows, the player's hands and their name over their head are not
drawn, and a scene hides the HUD unless its path says otherwise.

### Camera paths

```json
{
  "keyframes": [
    {"time": 0, "position": [0, 2, -12], "pitch": 5},
    {"time": 60, "position": [8, 6, -6], "yaw": 50, "pitch": 15, "easing": "easeInOutSine"},
    {"time": 120, "position": [0, 30, -20], "pitch": 40, "fov": 50, "roll": 10, "easing": "easeOutQuad"}
  ],
  "hide_hud": true
}
```

A camera path is a list of keyframes: a time in ticks, a place in blocks from where the scene is played (x to the
left, y up, z ahead, turned as the scene's origin faces), a yaw from the origin's facing, a pitch down, a roll, and a
field of view (0 keeps the player's own). Between keyframes the place follows a smooth curve through every one, and
the rest goes over by each keyframe's easing, named as Blockbench names them. A path is declared in code with
`CameraPaths.declare(id, path)` or written by a resource pack under `assets/<namespace>/jstech/camera_paths/`, the
file winning over the declaration; the server names it only by its id.

The studio's Cameras page writes these files by flying: open a path or start a new one, press Fly and record, fly
where the camera goes and press the Keep a camera keyframe key (K) at each place; Play runs it as a scene on your game
alone before it is saved, and Save to file writes it to your mod (see [While you build](DEVELOPING.md#the-tools)).

## Cameras on screens

```java
// In a screen's painter, as the world's screens paint (Screens in the world):
CameraFeeds.show(graphics, new CameraFeed(lens, yaw, pitch, 70.0), 0, 0, width, height);
```

A camera in the world is where its lens is, which way it looks and how wide it sees. A screen that shows it asks for
its view each time it draws and gets the last picture drawn of it, its middle cut to the screen's shape. The pictures
are drawn before the frame from the land the game keeps built, so a camera shows only what the player's game has,
within their view distance; two screens showing one camera share one view. The player is drawn in it, as a camera
elsewhere sees them.

## Settings of this player

In `jstech-camera.toml`, under "This player" in the settings:

| Setting | Default | What it tunes |
| --- | --- | --- |
| `camera.chase` | true | Whether the view follows the turn and roll of what is ridden. |
| `camera.feeds` | 2 | How many cameras' views are drawn at once, the nearest first; 0 draws none. |
| `camera.feed_rate` | 10 | How many times a second each view is drawn again. |
| `camera.feed_resolution` | 256 | How many pixels wide each view is drawn; its height follows its screen. |

## What can go wrong

- **A view does not start.** The entity watched or the point looked from is in no chunk the server sends the player.
- **A scene ends at once, lost.** No resource pack's file and no declaration has its path's id on the player's game.
- **A screen shows nothing.** The camera is beyond what the player's game has built, the player's setting draws no
  views, or more screens are asked for than it draws at once and this one is not among the nearest.
- **Free look does nothing.** The Free Look key has no key bound.
- **A shake is not felt.** The player asked for less movement, or set the shake to 0.
- **A third-person camera does not show.** A camera of a higher priority gives a view first; or the player is not in
  the third person.
- **K opens nothing while a path records.** It keeps keyframes then; the diagram of forces opens on it again once the
  recording stops.
