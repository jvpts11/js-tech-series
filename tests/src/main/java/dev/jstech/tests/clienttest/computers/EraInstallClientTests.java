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
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.ThisPcApp;
import dev.jstech.computers.os.HostScope;
import dev.jstech.computers.os.OsDef;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.install.SetupTiming;
import dev.jstech.computers.os.media.InstallMedia;
import dev.jstech.computers.os.media.InstallerStacks;
import dev.jstech.computers.os.media.MediaFormat;
import dev.jstech.computers.program.Programs;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.lwjgl.glfw.GLFW;

/**
 * Each era's programs installed from their own discs by a player: at the era's Personal Computer, every program its
 * system can take that had been written by then comes on the medium of its age, a floppy, a CD or a DVD. The player
 * swaps the disc in the drive beside the computer, sits back down and installs it from This PC, or with install at the
 * MC-DOS prompt, and the machine has it once the setup has run its time.
 */
public final class EraInstallClientTests {

    /** The floppy drive, its back on the computer and its front to the south. */
    private static final BlockPos FLOPPY_DRIVE = new BlockPos(9, 2, 3);
    /** Where a player stands to reach the floppy drive's front. */
    private static final BlockPos AT_THE_FLOPPY = new BlockPos(9, 2, 5);
    /** The era's optical drive, its back on the computer and its front to the north. */
    private static final BlockPos OPTICAL_DRIVE = new BlockPos(9, 2, 1);
    /** Where a player stands to reach the optical drive's front. */
    private static final BlockPos AT_THE_OPTICAL = new BlockPos(9, 2, 0);
    /** How long a step may take to show on the screen. */
    private static final int WAIT = 80;
    /** What a setup may run over the time it is given, the window's own opening and closing. */
    private static final int SETUP_SLACK = 100;

    private EraInstallClientTests() {
    }

    @ClientTest(timeoutTicks = 12_000)
    public static void vintage_everyProgramOfTheEraInstallsFromItsDisc(final ClientTestContext ctx) {
        installs(ctx, EraKit.VINTAGE);
    }

    @ClientTest(timeoutTicks = 30_000)
    public static void legacy_everyProgramOfTheEraInstallsFromItsDisc(final ClientTestContext ctx) {
        installs(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = 30_000)
    public static void transition_everyProgramOfTheEraInstallsFromItsDisc(final ClientTestContext ctx) {
        installs(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = 40_000)
    public static void standard_everyProgramOfTheEraInstallsFromItsDisc(final ClientTestContext ctx) {
        installs(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = 40_000)
    public static void advanced_everyProgramOfTheEraInstallsFromItsDisc(final ClientTestContext ctx) {
        installs(ctx, EraKit.ADVANCED);
    }

    private static void installs(final ClientTestContext ctx, final EraKit kit) {
        final List<ProgramSpec> programs = installable(kit);
        ctx.assertTrue(!programs.isEmpty(), "the " + kit + " computer has programs to install");
        EraJourney.lay(ctx, kit).thenBuild(0, world -> {
            world.setBlock(FLOPPY_DRIVE, facing(ComputingModule.FLOPPY_DRIVE.get(), Direction.SOUTH));
            final Block optical = opticalDrive(kit);
            if (optical != null) {
                world.setBlock(OPTICAL_DRIVE, facing(optical, Direction.NORTH));
            }
        });
        ctx.thenWaitUntilServer(level -> linked(ctx, level, FLOPPY_DRIVE), WAIT,
                "the floppy drive to link to the computer behind it",
                level -> "linked to " + DiscSteps.reader(ctx, level, FLOPPY_DRIVE).ownerPos());
        if (opticalDrive(kit) != null) {
            ctx.thenWaitUntilServer(level -> linked(ctx, level, OPTICAL_DRIVE), WAIT,
                    "the optical drive to link to the computer behind it",
                    level -> "linked to " + DiscSteps.reader(ctx, level, OPTICAL_DRIVE).ownerPos());
        }
        for (final ProgramSpec program : programs) {
            install(ctx, kit, program);
        }
    }

    /*
     * Every program a player installs on the era's computer: written by the era, allowed on its hardware, made for
     * its system's platform and edition, and living on a Personal Computer rather than on a server or the Mainframe.
     */
    private static List<ProgramSpec> installable(final EraKit kit) {
        final OsDef system = OsRegistry.getOs(kit.desktopOs());
        final int rank = OsRegistry.osVersionRank(system.id());
        return Programs.all().stream()
                // The mod's own programs, not the ones the test mod registers for its own checks.
                .filter(program -> JsComputers.MODID.equals(program.id().getNamespace()))
                .filter(ProgramSpec::installable)
                .filter(program -> kit.era().isAtLeast(program.era()) && kit.era().isAtLeast(program.minEra()))
                .filter(program -> program.platforms().contains(system.platform()))
                .filter(program -> rank == 0 || rank >= program.minOsRank())
                .filter(program -> program.hostScope() == HostScope.ANY
                        || program.hostScope() == HostScope.PERSONAL_COMPUTER)
                .sorted(Comparator.comparing((ProgramSpec program) -> program.era().level())
                        .thenComparing(ProgramSpec::commandName))
                .toList();
    }

    /* The disc swapped in its drive, the player back at the desk, and the program installed as that system does. */
    private static void install(final ClientTestContext ctx, final EraKit kit, final ProgramSpec program) {
        final MediaFormat format = InstallMedia.forProgram(program.era(), program.kind());
        final boolean floppy = format == MediaFormat.FLOPPY;
        final BlockPos drive = floppy ? FLOPPY_DRIVE : OPTICAL_DRIVE;
        leaveTheDesk(ctx)
                .thenTeleport(EraJourney.SETTLE, floppy ? AT_THE_FLOPPY : AT_THE_OPTICAL,
                        floppy ? Direction.NORTH : Direction.SOUTH);
        DiscSteps.swap(ctx, drive, InstallerStacks.forProgram(program));
        // The first sitting waits out the machine's start; every later one finds the system already up.
        ctx.thenTeleport(EraJourney.SETTLE, EraNetwork.AT_THE_DESK, Direction.WEST)
                .thenRightClick(EraJourney.SETTLE, EraNetwork.MONITOR)
                .thenAwaitScreen(EraJourney.systemScreen(kit), EraJourney.BOOT_WAIT);
        if (kit == EraKit.VINTAGE) {
            ctx.then(EraJourney.SETTLE, () -> {
                ctx.type("install " + program.id().getPath());
                ctx.key(GLFW.GLFW_KEY_ENTER);
            });
        } else {
            fromThisPc(ctx, kit, program);
        }
        ctx.thenWaitUntilServer(level -> computer(ctx, level).console().isInstalled(program.id().toString()),
                SetupTiming.ticks(program.minDiskMb(), format, false) + SETUP_SLACK,
                program.displayName() + " to be installed from its " + format.name().toLowerCase(Locale.ROOT)
                        + " on the " + kit + " computer",
                level -> "installed=" + computer(ctx, level).console().installed());
        if (kit != EraKit.VINTAGE) {
            // This PC closed with its close box, so the next disc is offered by a window opened fresh.
            final String thisPc = thisPc(kit);
            ctx.then(EraJourney.SETTLE, () -> {
                final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(thisPc);
                if (window != null) {
                    ctx.clickDesktop(window.buttonCentre(DesktopWindow.BUTTON_CLOSE));
                }
            }).thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(thisPc) == null, WAIT,
                    thisPc + " to close from its close box");
        }
    }

    /* This PC opened from Start, and the drive's Install button pressed once its row offers one. */
    private static void fromThisPc(final ClientTestContext ctx, final EraKit kit, final ProgramSpec program) {
        final String thisPc = thisPc(kit);
        EraJourney.launch(ctx, kit, thisPc)
                .thenWaitUntil(() -> installableRow(ctx, thisPc) >= 0, WAIT,
                        thisPc + " to offer " + program.displayName() + "'s disc to install")
                // A frame must have drawn the Install button before its centre can be read.
                .then(EraJourney.SETTLE, () -> {
                    final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(thisPc);
                    final ThisPcApp app = (ThisPcApp) window.app();
                    ctx.clickDesktop(DesktopSteps.contentPoint(window,
                            app.installButtonCenter(installableRow(ctx, thisPc))));
                });
    }

    /* Closes the screen at the desk, so the player's hands are free for the drive. */
    private static ClientTestContext leaveTheDesk(final ClientTestContext ctx) {
        return ctx.then(0, () -> {
            if (ctx.mc().screen != null) {
                ctx.key(GLFW.GLFW_KEY_ESCAPE);
            }
        }).thenAwaitNoScreen(WAIT);
    }

    /* The first row of This PC whose disc can be installed, or -1 while none is listed yet. */
    private static int installableRow(final ClientTestContext ctx, final String thisPc) {
        final DesktopWindow window = ctx.screen(DesktopScreen.class).windowFor(thisPc);
        if (window == null || !(window.app() instanceof ThisPcApp app)) {
            return -1;
        }
        for (int row = 0; row < 16; row++) {
            if (app.isInstallable(row)) {
                return row;
            }
        }
        return -1;
    }

    /** What the era's desktop calls the machine's own page: Frames 7 calls it Computer. */
    private static String thisPc(final EraKit kit) {
        return kit == EraKit.TRANSITION ? "Computer" : "This PC";
    }

    /* The drive that reads the era's program discs: a CD drive, then DVD, then Blu-ray; the Vintage has none. */
    private static Block opticalDrive(final EraKit kit) {
        return switch (kit) {
            case VINTAGE -> null;
            case LEGACY -> ComputingModule.CD_DRIVE.get();
            case TRANSITION, STANDARD -> ComputingModule.DVD_DRIVE.get();
            case ADVANCED -> ComputingModule.BLU_RAY_DRIVE.get();
        };
    }

    private static BlockState facing(final Block block, final Direction front) {
        return block.defaultBlockState().setValue(HorizontalDirectionalBlock.FACING, front);
    }

    private static boolean linked(final ClientTestContext ctx, final ServerLevel level, final BlockPos drive) {
        return ctx.abs(EraNetwork.COMPUTER).equals(DiscSteps.reader(ctx, level, drive).ownerPos());
    }

    private static PersonalComputerBlockEntity computer(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.COMPUTER,
                PersonalComputerBlockEntity.class);
    }
}
