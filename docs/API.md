# The API of the J's Tech Series

This page says what a mod built on the series may use, what it may not, and what it can expect to keep
working from one release to the next.

## What the API is

Two packages, and nothing else:

- `dev.jstech.core.api`, for what every mod of the series shares.
- `dev.jstech.computers.api`, for the computers.

Everything reachable from those two packages is the API. Everything else in either mod is that mod's own
business and may change in any release with no warning and no note. A mod that reaches into it is a mod
that will break, and no release will be held back to avoid breaking it.

A few types live where they belong rather than in those packages, and are part of the API all the same,
because a mod cannot add anything without holding them:

- The registries a mod adds to: `LanguageRegistry` and `OperationTypeRegistry`.
- What it adds to them: `IProgrammingLanguage`, `OperationType` and `IOperationArgs`.
- What a machine is and what can be installed on one: `IsaSpec`, `KernelDef`, `OsDef`, `ProgramSpec`,
  `DesktopEnvironmentDef` and `OperatingSpaceDef`, with `ArchitectureSpec`, the former name of `IsaSpec`,
  for one more cycle.
- What a Mainframe can run to plan its network's work: `EngineDef`, a Network Operations Engine, and the
  `EngineCapability` values it may offer.
- What a screen of a mod's own is handed: `ComputerTerminalMenu`, the machine's menu, which an operating
  space is given because the items in it are the server's.

Everything public in one of these is part of the promise, as it is in the two packages.

## How to add something

Each mod opens its registries once, while the game loads, by firing one event on the mod bus:

- `CoreRegisterEvent`, for languages and kinds of Operation.
- `ComputersRegisterEvent`, for instruction set architectures (ISAs), kernels, operating systems, programs,
  desktops and Network Operations Engines, and for what the engines that take extensions (NextgreIQL among
  them) add to their planner: rules (`IPlannerRule`), hints (`IPlannerOperator`), statistics
  (`IPlannerStatistic`) and notes under a plan's steps (`IExplainNode`), all in `dev.jstech.computers.api.planner`.
  A rule weighs every plan the planner considers for a craft, a `PlanCandidate` made of `PlanStep`s, and may add
  to its cost or set it aside, saying why; a player switches each rule on or off for their own Mainframe. A hint is
  words a statement may end with (`CRAFT 64 piston PREFER COMPUTER 'Bench A'`), and changes every plan of that
  statement the same way.

Listen for the one you need and add what you have. After the loading is done every registry is closed and
refuses to change, so that what a world knows how to do does not change under it while somebody plays it.

The mods of the series add their own through the same events. Sigma Sharp is registered as a language by
J's Computers listening for `CoreRegisterEvent`, exactly as an addon would, so the way in is the one that
is tried every time the game starts rather than a path only addons take.

Two of the same thing are refused rather than one quietly replacing the other, because which of the two
won would otherwise depend on the order the mods happened to load in.

## What is not open, and why

**The calls a program can make.** The functions of the language, the ones a program reaches with
`Console.PrintLine` or `Math.Floor`, are fixed. They are not a registry an addon adds to, and that is
deliberate: a call is part of what the language means. A listing built on one machine runs on every
machine of its line and on every world, which can only be true if the same listing means the same thing
everywhere. Let a mod add calls and it stops being true.

What a mod adds instead is a **language** of its own, which brings whatever it likes and compiles to the
assembly the machines already run, or an **instruction set architecture** (ISA), which is a new kind of
machine. Both are open, and both keep the promise above.

**Sockets** need nothing. A `CpuSocketId` is an open id in the `namespace:path` shape, so a mod that brings
processors of its own brings the socket they sit in by writing its id, with nothing to register.

## The version, and how stable this is

`JsCoreApi.VERSION` and `JsComputersApi.VERSION` are the numbers of the shape of each mod's API. Each goes
up by one whenever something is added to that mod's API or changes shape in it. A mod that needs something
added later can refuse to load below the number that added it.

`JsCoreApi.SETTLED` says whether the shape has settled. **It has not.** The computing side of the series
is still being built, and until J's Computers and J's Industrial are both finished in what they do and in
how they are written, anything here may change shape between releases. What is here works and is worth
building on; what it is called and what it takes may not survive. It settles when those two do, which is
also when the rest of the series starts building its own on top of them.

That is not an excuse to break things for no reason. It is a warning that a release may, and a promise
that it will say so in the changelog when it does.

## What is new is marked

Anything added or changed in the cycle being built, or in the last release, carries
`@ApiStatus.Experimental`, from the JetBrains annotations every Minecraft mod already has on its classpath.
It is the part of the API most likely to move, because no release has yet gone out with mods using it. The
mark comes off when the next cycle begins; what has not moved by then is as settled as the rest.

## How the API is kept

Each mod's API is written down line by line in a file of its own, `core/src/test/resources/api/core.txt`
and `computers.txt`: one line for every type and every member a mod can reach, each with the version of
the API that brought it in the shape written there. A line reads like this:

```
2 dev.jstech.core.language.IProgrammingLanguage  default CompileResult compile(List<SourceText>, CompileOptions)
```

The file also says the newest version that is no longer marked as new (`stable 1`). A test compares the
code with the file on every build, and fails when:

- the API has a type or member the file does not, or the file has one the API lost;
- the mod's `VERSION` is not the newest number in the file, so something came in without raising it;
- something newer than the stable version has no `@ApiStatus.Experimental`, or something as old as it
  still has one.

When it fails, it writes what the file would have to say to `core/build/api-photo`, with anything new at
the next number. Nothing reaches the API, or leaves it, without somebody reading that and deciding it.

## What changed, release by release

Each release's section of `CHANGELOG.md` opens with an API part, for the people who build on the series:
the version each mod's API reached, and what was added, changed, deprecated or removed.

## Taking something away

Anything about to go is marked `@Deprecated` first, with a note saying what to use instead. It stays for
one whole cycle of the series and goes in the next one, so there is always a release where both the old
way and the new one work and a mod can move between them without a version of its own that does neither.

## Numbers that travel

Some things are written into saved worlds and sent over the wire as numbers rather than names: the kinds
of Operation, the status an Operation is in, the sizes and tiers of hardware. Those numbers are part of
what a saved world means, so they never change once given out. A test in the Core reads every one of them
and fails if a number moves, which is what a mod that stores one of them is relying on.
