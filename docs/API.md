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

## How to add something

Each mod opens its registries once, while the game loads, by firing one event on the mod bus:

- `CoreRegisterEvent`, for languages and kinds of Operation.
- `ComputersRegisterEvent`, for architectures, kernels, operating systems, programs and desktops.

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
assembly the machines already run, or an **architecture**, which is a new kind of machine. Both are open,
and both keep the promise above.

**Sockets** need nothing. A `CpuSocketId` is an open id in the `namespace:path` shape, so a mod that brings
processors of its own brings the socket they sit in by writing its id, with nothing to register.

## The version, and how stable this is

`JsCoreApi.VERSION` is the number of the shape of the API. It goes up by one whenever something is added.
A mod that needs something added later can refuse to load below the number that added it.

`JsCoreApi.SETTLED` says whether the shape has settled. **It has not.** The computing side of the series
is still being built, and until J's Computers and J's Industrial are both finished in what they do and in
how they are written, anything here may change shape between releases. What is here works and is worth
building on; what it is called and what it takes may not survive. It settles when those two do, which is
also when the rest of the series starts building its own on top of them.

That is not an excuse to break things for no reason. It is a warning that a release may, and a promise
that it will say so in the changelog when it does.

## Taking something away

Anything about to go is marked `@Deprecated` first, with a note saying what to use instead. It stays for
one whole cycle of the series and goes in the next one, so there is always a release where both the old
way and the new one work and a mod can move between them without a version of its own that does neither.

## Numbers that travel

Some things are written into saved worlds and sent over the wire as numbers rather than names: the kinds
of Operation, the status an Operation is in, the sizes and tiers of hardware. Those numbers are part of
what a saved world means, so they never change once given out. A test in the Core reads every one of them
and fails if a number moves, which is what a mod that stores one of them is relying on.
