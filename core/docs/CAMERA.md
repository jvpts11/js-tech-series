# The camera

*Added 2026-10-21.*

What the player sees through, from and around: lenses that magnify by real powers, the view following what the
player rides, looking about freely in a cockpit, scenes along camera paths, the view of an entity or from a point
elsewhere, and cameras in the world whose view a screen shows.

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
