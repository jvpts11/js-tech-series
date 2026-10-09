# Documentation

The documentation of the J's Tech Series. Each mod keeps its own, in a `docs/` folder beside its code; this folder
holds what concerns the whole series.

## Each mod

| Mod | For players | For programmers |
| --- | --- | --- |
| [J's Core](../core/README.md) | (a library: nothing to play on its own) | [J's Core documentation](../core/docs/README.md): every part of the library, from zero, with code. |
| [J's Computers](../computers/README.md) | [J's Computers documentation](../computers/docs/README.md): computers, systems, the network, autocrafting, cables, Σ#. | [Building on J's Computers](../computers/docs/API.md). |
| [J's Industrial](../industrial/README.md) | [The machines](../industrial/docs/MACHINES.md). | [For mod and pack authors](../industrial/docs/FOR_MOD_AUTHORS.md). |

## The whole series

- [The API](API.md): what a mod built on the series may use, what it may not, and what it can expect to keep
  working from one release to the next.
- [Code style](CODE_STYLE.md): headers, naming, modelling, imports, comments and commit messages.
- [Versions, phases and releases](RELEASING.md): what a version number means, the development phases and their
  gates, snapshot builds, and how a release is cut.

## The design

Each mod's design lives in a `design/` folder inside its `docs/`, one page per subject, as each one is written:
[J's Computers' design](../computers/docs/design/README.md),
[J's Industrial's design](../industrial/docs/design/README.md) and
[J's Space's design](../space/docs/design/README.md).
