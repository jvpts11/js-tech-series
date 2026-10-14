/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.IMainframeService;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.ComputerTerminalScreen;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.install.SetupTiming;
import dev.jstech.computers.os.media.InstallMedia;
import dev.jstech.computers.os.media.InstallerStacks;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.computers.program.ComputerConsoleState;
import dev.jstech.computers.program.Programs;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;

/**
 * Each era's services installed from their own media by a player, at the machines that run them. At the Mainframe,
 * through a monitor beside it, every program the network's system takes that had been written by then comes off its
 * disc or stick with install at the prompt. From the Legacy on, at the rack's server, which runs Debian, through the
 * monitor beside the rack, every service a server keeps does the same: the Load Balancer, and from the Standard the
 * Messenger Service, KnotHub, the Soundfoundry Server, the Predictive Cache and the Integrity Monitor.
 */
public final class EraServiceInstallClientTests {

    /** How long a step may take to show on the screen. */
    private static final int WAIT = 80;
    /** What a setup may run over the time it is given. */
    private static final int SETUP_SLACK = 100;
    private static final ResourceLocation SERVER_SYSTEM = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "debian");

    /* The Mainframe's monitor in front of it, its three drives beside it, and where the player stands for each. */
    private static final BlockPos MAINFRAME_MONITOR = new BlockPos(3, 2, 1);
    private static final BlockPos AT_THE_MAINFRAME = new BlockPos(5, 2, 1);
    private static final BlockPos MAINFRAME_FLOPPY = new BlockPos(3, 3, 1);
    private static final BlockPos MAINFRAME_OPTICAL = new BlockPos(3, 2, 3);
    private static final BlockPos MAINFRAME_DOCK = new BlockPos(3, 3, 3);
    private static final BlockPos AT_THE_MAINFRAME_DRIVES = new BlockPos(4, 2, 3);

    /* The rack's monitor beside it, its three drives beside it, and where the player stands for each. */
    private static final BlockPos RACK_MONITOR = new BlockPos(6, 2, 6);
    private static final BlockPos AT_THE_RACK_MONITOR = new BlockPos(6, 2, 8);
    private static final BlockPos RACK_FLOPPY = new BlockPos(6, 3, 6);
    private static final BlockPos RACK_OPTICAL = new BlockPos(6, 2, 5);
    private static final BlockPos RACK_DOCK = new BlockPos(6, 3, 5);
    private static final BlockPos AT_THE_RACK_DRIVES = new BlockPos(8, 2, 5);

    private EraServiceInstallClientTests() {
    }

    @ClientTest(timeoutTicks = 6000)
    public static void vintage_everyServiceOfTheEraInstallsFromItsMedium(final ClientTestContext ctx) {
        services(ctx, EraKit.VINTAGE);
    }

    @ClientTest(timeoutTicks = 12_000)
    public static void legacy_everyServiceOfTheEraInstallsFromItsMedium(final ClientTestContext ctx) {
        services(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = 12_000)
    public static void transition_everyServiceOfTheEraInstallsFromItsMedium(final ClientTestContext ctx) {
        services(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = 15_000)
    public static void standard_everyServiceOfTheEraInstallsFromItsMedium(final ClientTestContext ctx) {
        services(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = 15_000)
    public static void advanced_everyServiceOfTheEraInstallsFromItsMedium(final ClientTestContext ctx) {
        services(ctx, EraKit.ADVANCED);
    }

    private static void services(final ClientTestContext ctx, final EraKit kit) {
        final boolean served = kit != EraKit.VINTAGE;
        final List<ProgramSpec> forTheMainframe = installable(kit, EraKit.NETWORK_OS,
                Set.of(HostScope.MAINFRAME, HostScope.ANY));
        final List<ProgramSpec> forTheServer = served
                ? installable(kit, SERVER_SYSTEM, Set.of(HostScope.SERVER)) : List.of();
        EraJourney.lay(ctx, kit).thenBuild(0, world -> {
            world.placeMonitor(MAINFRAME_MONITOR, Direction.EAST, kit.monitor());
            drives(world, kit, MAINFRAME_FLOPPY, MAINFRAME_OPTICAL, MAINFRAME_DOCK);
            if (served) {
                world.placeMonitor(RACK_MONITOR, Direction.SOUTH, kit.monitor());
                drives(world, kit, RACK_FLOPPY, RACK_OPTICAL, RACK_DOCK);
            }
        });
        if (served) {
            // The server's own system, which the systems' journey puts on it from its medium.
            ctx.thenServer(EraJourney.SETTLE, level -> rack(ctx, level).unitHost(0).installOs(SERVER_SYSTEM));
        }
        for (final ProgramSpec program : forTheMainframe) {
            install(ctx, kit, program, new Machine(MAINFRAME_MONITOR, AT_THE_MAINFRAME, Direction.WEST,
                    MAINFRAME_FLOPPY, MAINFRAME_OPTICAL, MAINFRAME_DOCK, AT_THE_MAINFRAME_DRIVES, true));
        }
        for (final ProgramSpec program : forTheServer) {
            install(ctx, kit, program, new Machine(RACK_MONITOR, AT_THE_RACK_MONITOR, Direction.NORTH,
                    RACK_FLOPPY, RACK_OPTICAL, RACK_DOCK, AT_THE_RACK_DRIVES, false));
        }
        ctx.assertTrue(!forTheMainframe.isEmpty(), "the " + kit + " Mainframe has programs to install");
    }

    /*
     * Every program a player installs on a machine running {@code system}: written by the era, allowed on its
     * hardware, made for the system's platform, and living on that kind of machine.
     */
    private static List<ProgramSpec> installable(final EraKit kit, final ResourceLocation system,
                                                 final Set<HostScope> scopes) {
        final OsDef os = OsRegistry.getOs(system);
        return Programs.all().stream()
                .filter(program -> JsComputers.MODID.equals(program.id().getNamespace()))
                .filter(ProgramSpec::installable)
                .filter(program -> kit.era().isAtLeast(program.era()) && kit.era().isAtLeast(program.minEra()))
                .filter(program -> program.platforms().contains(os.platform()))
                .filter(program -> scopes.contains(program.hostScope()))
                .sorted(Comparator.comparing((ProgramSpec program) -> program.era().level())
                        .thenComparing(ProgramSpec::commandName))
                .toList();
    }

    /*
     * The program's medium swapped into the machine's drive of that format, the player at the machine's monitor,
     * install typed at its prompt, and the machine has it once the setup has run its time. A program a new machine
     * already carries is found installed at once.
     */
    private static void install(final ClientTestContext ctx, final EraKit kit, final ProgramSpec program,
                                final Machine machine) {
        final MediaFormat format = InstallMedia.forProgram(program.era(), program.kind());
        final BlockPos drive = switch (format) {
            case FLOPPY -> machine.floppy();
            case USB -> machine.dock();
            default -> machine.optical();
        };
        leaveTheDesk(ctx).thenTeleport(EraJourney.SETTLE, machine.atTheDrives(), Direction.WEST);
        DiscSteps.swap(ctx, drive, InstallerStacks.forProgram(program));
        ctx.thenTeleport(EraJourney.SETTLE, machine.atTheMonitor(), machine.facing())
                .thenRightClick(EraJourney.SETTLE, machine.monitor());
        if (machine.mainframe()) {
            // The Mainframe's monitor opens its panel; its prompt is the Console heading.
            ctx.thenAwaitScreen(ComputerTerminalScreen.class, EraJourney.BOOT_WAIT)
                    .then(EraJourney.SETTLE, () -> {
                        final int[] console = ctx.screen(ComputerTerminalScreen.class).headingPoint("Console");
                        ctx.clickGui(console[0], console[1]);
                    });
        } else {
            ctx.thenAwaitScreen(CommandPromptScreen.class, EraJourney.BOOT_WAIT);
        }
        ctx.then(EraJourney.SETTLE, () -> {
                    ctx.type("install " + program.id().getPath());
                    ctx.key(GLFW.GLFW_KEY_ENTER);
                })
                .thenWaitUntilServer(level -> has(ctx, level, machine, program),
                        SetupTiming.ticks(program.minDiskMb(), format, false) + SETUP_SLACK,
                        program.displayName() + " to be installed from its " + format.name().toLowerCase(Locale.ROOT)
                                + " on the " + kit + (machine.mainframe() ? " Mainframe" : " server"),
                        level -> "installed=" + console(ctx, level, machine).installed() + " screen="
                                + lastLine(ctx));
    }

    /* The end of what the prompt shows, for a failure to name what the machine answered. */
    private static String lastLine(final ClientTestContext ctx) {
        final ComputerTerminalScreen panel = ctx.openScreen(ComputerTerminalScreen.class);
        if (panel != null) {
            final String text = panel.consoleText();
            return text.substring(Math.max(0, text.length() - 160));
        }
        final CommandPromptScreen<?> prompt = ctx.openScreen(CommandPromptScreen.class);
        if (prompt == null || prompt.scrollbackText().isEmpty()) {
            return "";
        }
        final List<String> lines = prompt.scrollbackText();
        return lines.get(lines.size() - 1);
    }

    /* Closes whatever screen the player has up, so the hands are free for a drive. */
    private static ClientTestContext leaveTheDesk(final ClientTestContext ctx) {
        return ctx.then(0, () -> {
            if (ctx.mc().screen != null) {
                ctx.key(GLFW.GLFW_KEY_ESCAPE);
            }
        }).thenAwaitNoScreen(WAIT);
    }

    /* A floppy drive, the era's optical drive and a Dock Station, each facing east to the player. */
    private static void drives(final TestWorldBuilder world, final EraKit kit, final BlockPos floppy,
                               final BlockPos optical, final BlockPos dock) {
        world.setBlock(floppy, facingEast(ComputingModule.FLOPPY_DRIVE.get()));
        world.setBlock(optical, facingEast(opticalDrive(kit)));
        world.setBlock(dock, facingEast(ComputingModule.DOCK_STATION.get()));
    }

    /* The drive that reads the era's discs: a CD drive, then DVD, then Blu-ray; the Vintage reads floppies alone. */
    private static Block opticalDrive(final EraKit kit) {
        return switch (kit) {
            case VINTAGE, LEGACY -> ComputingModule.CD_DRIVE.get();
            case TRANSITION, STANDARD -> ComputingModule.DVD_DRIVE.get();
            case ADVANCED -> ComputingModule.BLU_RAY_DRIVE.get();
        };
    }

    private static BlockState facingEast(final Block block) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, Direction.EAST);
    }

    /*
     * Whether the machine has the program, as the machine itself counts it: on its console's list, or, on the
     * Mainframe, a service it keeps, as the engine of its age a new one ships with.
     */
    private static boolean has(final ClientTestContext ctx, final ServerLevel level, final Machine machine,
                               final ProgramSpec program) {
        if (console(ctx, level, machine).isInstalled(program.id().toString())) {
            return true;
        }
        if (!machine.mainframe()) {
            return false;
        }
        final IMainframeService service = TestWorldBuilder.at(level, ctx.origin())
                .blockEntity(EraNetwork.MAINFRAME, MainframeBlockEntity.class).service(program.id());
        return service != null && service.installed();
    }

    private static ComputerConsoleState console(final ClientTestContext ctx, final ServerLevel level,
                                                final Machine machine) {
        return machine.mainframe() ? TestWorldBuilder.at(level, ctx.origin())
                .blockEntity(EraNetwork.MAINFRAME, MainframeBlockEntity.class).console()
                : rack(ctx, level).consoleOf(0);
    }

    private static ServerRackBlockEntity rack(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.RACK, ServerRackBlockEntity.class);
    }

    /** A machine a service goes on, by where its monitor and drives are and where the player stands to use them. */
    private record Machine(BlockPos monitor, BlockPos atTheMonitor, Direction facing, BlockPos floppy,
                           BlockPos optical, BlockPos dock, BlockPos atTheDrives, boolean mainframe) {
    }
}
