# Building on J's Computers

For a programmer making an addon that adds to the computers: an instruction set, an operating system, a program,
a desktop, a network engine, a kind of window component, a program window written in Java. It explains the mod's
own ideas from zero, each with the code that adds one; it does not teach Java or NeoForge.

What is promised and for how long (versions, the experimental mark, deprecation) is on the series' page,
[docs/API.md](../../docs/API.md). Everything here is part of that promise: the packages `dev.jstech.computers.api`,
`.api.client` and `.api.planner`, and the types this page names from elsewhere. `JsComputersApi.VERSION` is the
number of its shape (5 today); much of it is still marked `@ApiStatus.Experimental`.

## The way in

*Added 2026-09-17.*

Everything is added once, while the game loads, by listening for `ComputersRegisterEvent` on your mod's bus:

```java
@EventBusSubscriber(modid = MyAddon.MODID, bus = EventBusSubscriber.Bus.MOD)
public final class MyComputersContent {

    @SubscribeEvent
    public static void register(final ComputersRegisterEvent event) {
        event.kernel(MY_KERNEL);
        event.operatingSystem(MY_OS);
        event.program(MY_PROGRAM);
    }
}
```

The event is fired once, during common setup. Afterwards every registry is closed, so what a world knows does not
change under it while it is played. An id taken twice is refused with an exception, which stops the load while
somebody is there to read why; ids are `namespace:path`, in your mod's namespace, and never change once released
because worlds save them.

J's Computers adds its own systems, desktops and programs through the very same event.

## The ideas

A computer of the mod is built like a real one, and the API follows the same layers:

| Layer | What it is | Type |
| --- | --- | --- |
| Instruction set (ISA) | What a processor runs. A program is built for one. | `IsaSpec` |
| Kernel | The core of an operating system: how it shares time, keeps files and what its shell is like. | `KernelDef` |
| Operating system | What a computer starts into and installs from a medium: kernel, era, size, installer. | `OsDef` |
| Program | Anything installed on a system: an application, a service, a desktop, a network engine. | `ProgramSpec` |
| Desktop | The windows, panel and menu a system shows, and the programs it ships. | `DesktopEnvironmentDef` |
| Operating space | A full-screen program a system runs instead of a prompt (MC-NET's Interactor). | `OperatingSpaceDef` |
| Network engine | The program on a Mainframe that reads the network's language and plans its work. | `EngineDef` |

Every kind of thing has an **era**, the generation of computing it belongs to (`HardwareEra`: Vintage, Legacy,
Transition, Standard, Advanced). A thing's **first era** is a gate: hardware older than it refuses it.

In the examples, `rl("scanner")` stands for `ResourceLocation.fromNamespaceAndPath(MyAddon.MODID, "scanner")`.

## An instruction set

*Added 2026-09-17.*

```java
public static final IsaSpec ARM64 = new IsaSpec("myaddon:arm64", "ARM64", 64, Set.of("myaddon:arm64"));
// event.isa(ARM64);
```

`IsaSpec(id, name, bits, runs)`: `runs` is every instruction set this one can run, its own included (an x86-64
processor runs x86 and IA-16 programs). A program built for an instruction set runs on every processor whose
`runs` holds it. Bringing processors of your own needs no socket registration: a socket is an id in the
`namespace:path` form that a processor and a board both name.

## A kernel and an operating system

*Added 2026-08-26.*

```java
public static final KernelDef MY_KERNEL = new KernelDef(rl("micro"),
        SchedulerKind.PREEMPTIVE,          // NONE, COOPERATIVE (programs yield) or PREEMPTIVE (the kernel shares time)
        FilesystemKind.HIERARCHICAL,       // NONE, FLAT (one folder) or HIERARCHICAL (folders in folders)
        ShellFamily.POSIX);                // DOS, POSIX or NET: how its command line behaves

public static final OsDef MY_OS = OsDef.terminalSystem(rl("micro_os"), rl("micro"), Platform.LINUX,
        HardwareEra.LEGACY,                // its first era
        512,                               // what it takes on disk, in MB
        "Micro OS",                        // its name
        ShellKind.SH, PackageManagerKind.NONE, InstallMode.GUIDED,
        new SoftwareHouse("Micro Labs", "Micro Labs Inc."))
        .withRam(16);                      // MB of memory it holds while it runs
```

- `OsDef.linuxDistro(...)` makes a Linux distribution (the `jsc:linux` kernel, first era Legacy) with its shell,
  package manager and install mode; `OsDef.mediaInstalled(...)` makes a system with its own desktop
  (`OsCapability.FULL_DESKTOP` and the desktop it bundles), a network terminal (`NETWORK_GUI`) or a prompt
  (`TERMINAL_ONLY`).
- `InstallMode`: `GUIDED` (pick a disk, confirm), `LIVE_MANUAL` (the player types the real steps), `SOURCE`
  (compiled first).
- `withInstaller(InstallerStyle...)` picks which existing installer it looks like; `withRank` orders systems of one
  family from oldest to newest, for programs that need a newer one.
- A `SoftwareHouse` is who made it, as banners and About boxes print it. Bring your own.
- The system's install medium is made for it, in the format of its era, and shows in the era's creative tab.

## A program

*Added 2026-08-26.*

```java
public static final ProgramSpec SCANNER = ProgramSpec.of(rl("scanner"),
                "scanner",                 // its command at a prompt
                false,                     // preinstalled with every system?
                Set.of(Platform.FRAMES, Platform.LINUX),
                24,                        // MB on disk
                ProgramKind.APP,
                0,                         // the lowest system rank it runs on
                HostScope.ANY)             // which computers may run it
        .named("Scanner")
        .described("Finds what the network holds near you.")
        .withMinEra(HardwareEra.STANDARD)  // a gate: older hardware refuses it
        .withEra(HardwareEra.STANDARD)     // what it looks like and ships on; never a gate
        .withHouse(new SoftwareHouse("Micro Labs", "Micro Labs Inc."));
```

- `ProgramKind`: `APP`, `SERVICE` (runs with no window), `HYBRID`, `DESKTOP_ENVIRONMENT`, `OPERATING_SPACE`,
  `NETWORK_ENGINE`.
- `HostScope` limits which computers run it: `ANY`, `PERSONAL_COMPUTER`, `CRAFTING_COMPUTER`, `MAINFRAME`,
  `SERVER`, `CLUSTER_MANAGEMENT_COMPUTER`.
- `requiring(ProgramRequirement.engine(rl, "version"))` says the program needs a particular network engine at
  that version or newer; it is checked when the program is opened, not when it is installed.
- `event.fileOpener("myaddon:scanner")` lets "Open with" offer it for files of any kind.
- A program gets an install medium of its era, like a system, and can be published on a network's Mirror.

A program registered this way exists, installs and runs. What its window looks like is the next section, or it is
a Σ# program (the mod's language, [Σ#](SIGMA.md)) that draws its window from components.

## A program window written in Java

*Added 2026-10-05.*

```java
// client setup
DesktopApps.register(rl("scanner"), false, (host, monitor, desktop) -> new ScannerWindow());

public final class ScannerWindow extends DesktopProgram {

    @Override
    public String title() {
        return "Scanner";
    }

    @Override
    public void draw(GuiGraphics graphics, Font font, ISkin skin, int x, int y, int w, int h,
                     int mouseX, int mouseY) {
        skin.panel(graphics, x + 4, y + 4, w - 8, h - 8);
        skin.button(graphics, font, x + 8, y + 8, 60, 14, "Scan", false, false, true);
    }
}
```

- The window runs on the client of the player who opened it. Other players at the same machine, and the monitor's
  face in the world, see a placeholder unless `worldSafe()` says what it draws can be shown to anyone.
- `ISkin` (from J's Core, `dev.jstech.core.api.client`) is the system's look: draw through it and your window looks
  like a Frames 95 window on Frames 95 and like Frames 11 on Frames 11.
- A window that draws a picture of its own (a game, a viewer) returns a `SurfaceRenderer` from `renderer()` and is
  registered with `surface` true: it then holds video memory on the machine, like a monitor.
  `SurfaceRenderer` asks for a `PixelSurface` (an array of colours) or a `GpuSurface` (drawn into with the game's
  rendering), is asked for frames no faster than its frame cap and only while it is seen, and is never asked again
  after it throws.

## A kind of component for Σ# windows

*Added 2026-10-05.*

Σ# programs build their windows from components (buttons, lists, tables). An addon adds a kind:

```java
// common setup: ComputersRegisterEvent, on both sides
event.componentKind(new ComponentKind(rl("dial")));

// client setup
ComponentRenderers.register(rl("dial"), new DialRenderer());
```

A program then makes one with `new GenericComponent("myaddon:dial")`, gives it a value to show, and hears through
`OnAction` what the player did. Your `IComponentRenderer` draws the value (handed as plain Java values: numbers,
text, lists, maps) through the system's skin, and reports what the player did with `IComponentActions.send(name,
value)`. An `IComponentValidator` can refuse values a program hands it and actions a screen reports. A game without
your mod shows a placeholder naming the kind. A kind marked `reachesOutside` (it fetches or shows something from
outside the game) is off unless the server allows it and each player allows it too.

## An operating space

*Added 2026-09-20.*

An operating space is what MC-NET runs instead of a prompt: the whole screen is one program. Register its name
with `event.operatingSpace(new OperatingSpaceDef(rl("console"), "Console", house))`, its program with kind
`OPERATING_SPACE`, and its screen on the client with `OperatingSpaceScreens.register(rl("console"), (space,
inventory, title) -> new ConsoleScreen(space.menu(), inventory, title))`. The screen is handed an
`IOperatingSpace`: the machine's menu (holding the player's inventory as real slots), and which machine, monitor,
space and era it is.

## A desktop

*Added 2026-08-26.*

```java
new DesktopEnvironmentDef(rl("lumen"), "Lumen", PanelStyle.GNOME, List.of(rl("scanner")), Map.of(), house)
```

The panel style is one of the existing ones (each Frames edition, KDE, GNOME, Cinnamon, CDE); the list is the
programs it ships; the map gives programs the names this desktop calls them (`this_pc` is "Computer" on some).
Linux and FreeBSD install a desktop as a package.

## A network engine

*Added 2026-10-04.*

```java
public static final EngineDef QUIRK = new EngineDef(rl("quirkiql"), "QuirkIQL",
        Map.of(HardwareEra.LEGACY, "1.0", HardwareEra.STANDARD, "2.0"),
        Set.of(EngineCapability.PROCEDURES_AND_VIEWS));
// event.engine(QUIRK); and register the program rl("quirkiql") with ProgramKind.NETWORK_ENGINE
```

An engine is the program on a Mainframe that reads the network's language (its dialect) and plans the work. It
names the version it ships in for each era of Mainframe, and the extras it offers (`EngineCapability`:
`PROCEDURES_AND_VIEWS`, `DECLARATIVE_STATE`, `SUBSCRIPTIONS`, `EXPLAIN`, `PLANNER_HINTS`, `EXTENSIONS`). Until it
brings a planner of its own, it plans the way the Midsoft IQL Server does.

### Extending the planner

*Added 2026-10-04.*

Engines that offer `EXTENSIONS` (NextgreIQL) take additions to how they plan a craft. A plan is a `PlanCandidate`
made of `PlanStep`s (an item, how many runs, how many made, on a machine or a crafting table, how long), with a
cost in ticks; the cheapest plan not set aside runs.

- `IPlannerRule` weighs every plan the planner considers: it adds to its cost or sets it aside, saying why. A player
  switches each rule on or off for their own Mainframe.
- `IPlannerOperator` is a hint a statement may end with, such as `PREFER COMPUTER 'Bench A'`, and changes every
  plan of that statement.
- `IPlannerStatistic` shows among the planner's statistics, and may say how long a step takes.
- `IExplainNode` adds notes under a plan's steps where the plan is explained.

Register each with `event.plannerRule`, `plannerOperator`, `plannerStatistic`, `explainNode`.

## What is not open

The calls a Σ# program can make are fixed, on purpose: a program built on one machine must mean the same thing on
every machine and every world, which it could not if a mod could add calls. What a mod adds instead is a language
(J's Core's `IProgrammingLanguage`), an instruction set, or a kind of component. [docs/API.md](../../docs/API.md)
says why at length.

## What can go wrong

- **"already registered".** Two registrations of one id, yours or another mod's. Ids are in your namespace.
- **Nothing shows up.** Your listener was not on the mod bus, or ran after loading (it is then refused with a line
  in the log).
- **A program installs but has no window.** Nothing registered one for its id: `DesktopApps.register` on the client,
  or it is a Σ# program.
- **Another player sees a placeholder.** Your window runs on its opener's client only; return true from
  `worldSafe()` only if what it draws follows from what every client knows.
- **A component shows a placeholder.** The player's game has no renderer for the kind (your mod is missing there),
  or it reaches outside and is not allowed.
