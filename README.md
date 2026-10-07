<p align="center"><img src="docs/brand/series.png" alt="J's Tech Series" width="720"></p>

# J's Tech Series

A series of technology mods for Minecraft 1.21.1 on NeoForge, one mod per area, on top of a shared core library.
The objective of this project is to provide a full series of tech mods that are fully compatible and completely
integrated with each other, without all the known problems when making modpacks for personal use or multiple use.
Each mod can also be used as an independent one in a normal modpack that does not use any of them, with the only
real dependency being J's Core for all of them. They are also fully modifiable and can have addons that fully add
new things to them. This project is a personal passion project to, in the end, have a full series of tech mods
fully playable, and with a fun experience.

Currently, the series only has three mods: J's Core, J's Industrial and J's Computers. And the most developed one
currently is J's Computers, which has the backbone for every other mod that comes in the future, which is the mod's
own network and the operations system. After J's Computers reaches a good level of readiness, J's Industrial will
be the next one in the list.

The series is in alpha. See [what a version means](docs/RELEASING.md), [what changed](CHANGELOG.md), you can also find
the mod's builds and releases here: [releases page](https://github.com/jvpts11/js-tech-series/releases). Also, be warned, that
as long as both J's Industrial and J's Computers don't get fully stable in their foundations, worlds will
be broken while updating from older versions to focus time and effort in fixing problems and adding new features.

## The mods

| Mod | Id | What it is |
|---|---|---|
| [J's Core](core/README.md) | `jscore` | The shared library. Every mod of the series requires it. No gameplay of its own beyond the material items the others share. |
| [J's Computers](computers/README.md) | `jsc` | Computers, the data network, the systems and programs, storage and autocrafting. |
| [J's Industrial](industrial/README.md) | `jsindustrial` | Energy, machines and the processing chains that feed the network and the other ones in the future. |

Planned, in the order the series grows: Space, Warfare, Transport, Agriculture, Civil Works, Robotics,
Geology and Oceanics. The phases the series goes through, and what each phase needs before it starts, are
in [docs/RELEASING.md](docs/RELEASING.md).

**Every mod of the series depends only on J's Core, never on another mod of the series.** J's Computers needs only
J's Core, and so does J's Industrial; J's Space will need only J's Core, and so will every mod that comes after it.
When several are installed together, they work with each other through what J's Core gives them all and through
integrations that only wake when the other mod is present, so leaving any of them out never breaks the others. The
computing mod drives any machine that exposes the usual item and energy capabilities, and the industrial mod's
machines work with any FE generator.

Every mod carries the same version and they are released together. Put J's Core and the mods you want in
`mods/`, all at the same version; each mod's page lists its own requirements.

All the current obligatory dependencies are set to be replaced in the future for a native implementation of what
the dependency adds, the objective is to ease the process of porting by controlling the entire source code on the
main version, this way, it allows the J's Tech Series to not worry about any specific detail of a dependency in a
certain version of the game or of the dependency, it also allows us to better generalize anything that can be
generalized in an older or newer version and improve world conversion.

## Repository layout

Until the entire series hit v1.0.0r, all mods of the J's Tech Series live here in this monorepo, with the ones:

- `core/`: J's Core (`jscore`), the shared library.
- `computers/`: J's Computers (`jsc`), the computing mod.
- `industrial/`: J's Industrial (`jsindustrial`), the industrial mod.
- `tests/`: a development-only test mod. **It is not a mod to install.** It holds the tests of every mod
  and hosts the development runs; This mot is not intended to be used in-game, if you ever find this in your mods folder,
  uninstall, since this mod does nothing than just run tests for development, it also adds nothing to the game by itself. See
  [tests/README.md](tests/README.md).
- `docs/`: what concerns the whole series (the API promise, code style, versions and releases) and the
  [index of every mod's documentation](docs/README.md). Each mod keeps its own documentation in its `docs/`
  folder.

In the future, each mod will get their own dedicated repository, for now, all mods will stay here to ease development.

## Building from source

```
git clone https://github.com/jvpts11/js-tech-series.git
cd js-tech-series
./gradlew build
```

The J's Tech series use Java 21. Each mod's jar lands in its own `build/libs`, for example `computers/build/libs/jsc-1.21.1-<version>.jar`;
a build that is not the tagged release carries a `-SNAPSHOT.<commit>` suffix. Useful tasks: `runClient`,
`runServer`, `test` (pure logic, JUnit), `runGameTestServer` (the mods in a headless server),
`runClientTests0` (a real client that drives the screens and takes screenshots), and `:core:runData`,
`:computers:runData` and `:industrial:runData` to regenerate a mod's data.

## Contributing

If you want to make a contribution, read [docs/CODE_STYLE.md](docs/CODE_STYLE.md) before opening a pull request, and
[AI_POLICY.md](AI_POLICY.md) if you work with an AI assistant. Every asset of the mods is listed in
[ASSET_REGISTRY.md](ASSET_REGISTRY.md), with where it came from and who made it, and a pull request that brings an
asset records it there. Most of the art the mods ship today is AI-generated and temporary, waiting for hand-made art
to replace it; if you draw, model or record, that is a good place to start. Bug reports and ideas go in the issues.

## License

[LGPL-3.0-only](COPYING.LESSER), © jvpts11. The LGPL adds its permissions to the GPL, whose text is in
[COPYING](COPYING).
