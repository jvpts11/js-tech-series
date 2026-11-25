/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.ComputingModule;
import dev.jstech.computers.HardwareItems;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.PrinterBlockEntity;
import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.ChainParts;
import dev.jstech.computers.chain.MouldedParts;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.ChainMachineBlocks;
import dev.jstech.computers.chain.machine.ChainMachines;
import dev.jstech.computers.chain.machine.MaskWriting;
import dev.jstech.computers.chain.machine.MouldCutting;
import dev.jstech.computers.chain.machine.WaferPages;
import dev.jstech.computers.chain.machine.WaferWork;
import dev.jstech.computers.client.CommandPromptScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.DesktopWindow;
import dev.jstech.computers.client.os.IDesktopApp;
import dev.jstech.computers.client.os.LithographyApp;
import dev.jstech.computers.hardware.DiskSize;
import dev.jstech.computers.hardware.StorageTier;
import dev.jstech.computers.integration.jei.JscJeiPlugin;
import dev.jstech.computers.lithography.FilmPrinting;
import dev.jstech.computers.lithography.LithographyLook;
import dev.jstech.computers.program.Programs;
import dev.jstech.core.gui.TextScreen;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.TestCables;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Midsoft Lithography, in both its forms (computers/docs/design/survival.md, "Masks"), on a computer cabled to a
 * Lithography Station's peripheral port with a Mask Blank in the station: on MC-DOS its text screen, the arrows picking
 * the K8 processor mask and W writing it, the station then holding the mask where the blank was; on Frames XP its
 * window over an ArF station, the Nehalem processor picked and Write Mask writing it, its progress in the status bar.
 */
public final class LithographyClientTests {

    private static final int SETTLE = 4;
    private static final int SCREEN_WAIT = 120;
    /* The deposition's pages, a metal each and the Standard's first layer; the fewest masks, and chips, of the eras. */
    private static final long DEPOSITIONS = 8;
    private static final long MASKS = 20;
    private static final int BOOT_WAIT = 1_200;
    /* A Legacy station's layer, twenty seconds, with room over it. */
    private static final int WRITE_WAIT = 600;
    /* The station's front west corner, its peripheral port's cable, the computer, its monitor and the player. */
    private static final BlockPos CORNER = new BlockPos(2, 1, 2);
    private static final ResourceLocation MC_DOS = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "mc_dos");
    private static final String PROGRAM = "jsc:lithography";
    private static final String PORTUGUESE = "pt_br";
    private static final String ENGLISH = "en_us";
    /* How long the game's resources take to load again in another language, with room over it. */
    private static final int RELOAD_WAIT = 1_200;

    private LithographyClientTests() {
    }

    @ClientTest(timeoutTicks = 3600)
    public static void mcDos_lithoWritesTheK8MaskOnTheBlank(final ClientTestContext ctx) {
        final ChainMachine legacy = ChainMachines.byId("lithography_legacy").orElseThrow();
        final BlockPos computer = port(legacy).east(2);
        final BlockPos monitor = computer.east();
        final Item k8 = mask(HardwareEra.LEGACY, "k8");
        station(ctx, legacy)
                .thenBuild(SETTLE, world -> {
                    world.setBlock(computer, ComputingModule.VINTAGE_PERSONAL_COMPUTER.get());
                    world.faceRearTowardCable(computer);
                    final PersonalComputerBlockEntity pc = world.blockEntity(computer,
                            PersonalComputerBlockEntity.class);
                    buildVintage(pc);
                    pc.installOs(MC_DOS);
                    pc.console().install(Programs.LITHOGRAPHY.toString());
                    pc.togglePower();
                    world.placeMonitor(monitor, Direction.EAST, ComputingModule.VINTAGE_MONITOR.get());
                })
                .thenServer(SETTLE, level -> blank(ctx, level, legacy))
                .thenTeleport(SETTLE, monitor.east(2), Direction.WEST)
                .thenRightClick(SETTLE, monitor)
                .thenAwaitScreen(CommandPromptScreen.class, BOOT_WAIT)
                .then(SETTLE, () -> ctx.type("litho"))
                .then(1, () -> ctx.key(GLFW.GLFW_KEY_ENTER))
                .thenWaitUntil(() -> glass(ctx).contains("Midsoft Lithography 1.0") && glass(ctx).contains("Mask Blank")
                        && glass(ctx).contains("K8 processor"), SCREEN_WAIT, "the program to show its station")
                .then(SETTLE, () -> {
                    for (int i = 0; i < 3; i++) {
                        ctx.key(GLFW.GLFW_KEY_DOWN);
                    }
                })
                .thenWaitUntil(() -> glass(ctx).contains("W writes the K8 processor mask"), SCREEN_WAIT,
                        "the arrows to pick the K8 processor")
                .thenScreenshot(SETTLE, "litho-blank")
                .then(SETTLE, () -> ctx.type("w"))
                .thenWaitUntil(() -> glass(ctx).contains("Writing the K8 processor mask"), SCREEN_WAIT,
                        "W to start writing it")
                .thenScreenshot(SCREEN_WAIT, "litho-writing")
                .thenWaitUntilServer(level -> station(ctx, level, legacy).getInventory().getStackInSlot(0).is(k8),
                        WRITE_WAIT, "the station to write the mask", level -> "its slot holds " + station(ctx, level,
                                legacy).getInventory().getStackInSlot(0))
                .thenWaitUntil(() -> glass(ctx).contains("K8 processor mask in the slot"), SCREEN_WAIT,
                        "the screen to show the mask in the slot")
                .thenScreenshot(SETTLE, "litho-written")
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_F3))
                .thenWaitUntil(() -> !prompt(ctx).editing(), SCREEN_WAIT, "F3 to give the prompt back");
    }

    @ClientTest(timeoutTicks = 3600)
    public static void framesXp_lithographyWindowWritesTheNehalemMask(final ClientTestContext ctx) {
        final ChainMachine arf = ChainMachines.byId("lithography_arf").orElseThrow();
        final BlockPos computer = port(arf).east(2);
        final BlockPos monitor = computer.east();
        station(ctx, arf)
                .thenBuild(SETTLE, world -> {
                    final PersonalComputerBlockEntity pc = EraNetwork.runningComputer(world, EraKit.LEGACY,
                            computer);
                    pc.console().install(Programs.LITHOGRAPHY.toString());
                    world.placeMonitor(monitor, Direction.EAST, ComputingModule.LEGACY_MONITOR.get());
                })
                .thenServer(SETTLE, level -> blank(ctx, level, arf))
                .thenTeleport(SETTLE, monitor.east(2), Direction.WEST)
                .thenRightClick(SETTLE, monitor)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen(PROGRAM))
                .thenWaitUntil(() -> look(ctx) != null && look(ctx).stations() == 1 && look(ctx).blank(),
                        SCREEN_WAIT, "the window to show its station with a blank in it")
                .then(SETTLE, () -> {
                    app(ctx).showKind(MaskWriting.Kind.PROCESSORS);
                    app(ctx).pickNamed("Nehalem processor");
                })
                .thenScreenshot(SETTLE * 2, "lithography-window")
                .then(SETTLE, () -> app(ctx).writeMask())
                .thenWaitUntil(() -> look(ctx) != null && look(ctx).writes(), SCREEN_WAIT,
                        "Write Mask to start writing the Nehalem processor mask")
                .thenScreenshot(SCREEN_WAIT, "lithography-window-writing");
    }

    /**
     * The window on a computer linked to an Injection Moulder: the moulder with its Mould Blank, a list a family of
     * moulds, the Bezels list with each product's bezel and cover, the Fans list with each era's fan frame, and Cut
     * Mould cutting the Transition fan frame's mould. In Portuguese, whose seven lists are wider than the strip, the
     * tabs scroll as an XP tab control's do, the list shown always in view.
     */
    @ClientTest(timeoutTicks = 4800)
    public static void framesXp_lithographyWindowCutsTheFanFrameMould(final ClientTestContext ctx) {
        final ChainMachine moulder = ChainMachines.byId("injection_moulder").orElseThrow();
        final BlockPos computer = port(moulder).east(2);
        final BlockPos monitor = computer.east();
        final MouldedParts.Part frame = MouldedParts.fanFrame(HardwareEra.TRANSITION);
        station(ctx, moulder)
                .thenBuild(SETTLE, world -> {
                    final PersonalComputerBlockEntity pc = EraNetwork.runningComputer(world, EraKit.LEGACY,
                            computer);
                    pc.console().install(Programs.LITHOGRAPHY.toString());
                    world.placeMonitor(monitor, Direction.EAST, ComputingModule.LEGACY_MONITOR.get());
                })
                .thenServer(SETTLE, level -> station(ctx, level, moulder).getInventory().insertItem(0,
                        new ItemStack(ChainChips.MOULD_BLANK.get()), false))
                .thenTeleport(SETTLE, monitor.east(2), Direction.WEST)
                .thenRightClick(SETTLE, monitor)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen(PROGRAM))
                .thenWaitUntil(() -> look(ctx) != null && look(ctx).moulder() && look(ctx).blank(), SCREEN_WAIT,
                        "the window to show the moulder with a Mould Blank in it")
                .then(SETTLE, () -> {
                    app(ctx).showKind(MouldCutting.Kind.BEZELS);
                    app(ctx).pickNamed("Legacy Mainframe bezel");
                })
                .thenScreenshot(SETTLE * 2, "lithography-moulder-bezels")
                .then(SETTLE, () -> {
                    app(ctx).showKind(MouldCutting.Kind.FANS);
                    app(ctx).pickNamed("Transition fan frame");
                })
                .thenScreenshot(SETTLE * 2, "lithography-moulder-fans")
                .then(SETTLE, () -> app(ctx).writeMask())
                .thenWaitUntilServer(level -> station(ctx, level, moulder).getInventory().getStackInSlot(0)
                        .is(frame.tool().get()), WRITE_WAIT, "the moulder to cut the Transition fan frame's mould",
                        level -> "its slot holds " + station(ctx, level, moulder).getInventory().getStackInSlot(0))
                .thenScreenshot(SETTLE * 2, "lithography-moulder-cut");
        // In Portuguese the seven lists are wider than the strip: it scrolls, the list shown in view.
        language(ctx, PORTUGUESE)
                .then(SETTLE, () -> app(ctx).showKind(MouldCutting.Kind.JIGS))
                .thenScreenshot(SETTLE * 2, "lithography-moulder-jigs-pt")
                .then(SETTLE, () -> app(ctx).showKind(MouldCutting.Kind.SOCKETS))
                .thenScreenshot(SETTLE * 2, "lithography-moulder-sockets-pt");
        language(ctx, ENGLISH);
    }

    /**
     * The window on a computer with a printer linked to it: the printer with Blank Film in its tray, the Layouts and
     * the Phototools lists, and Print Film printing the Vintage's phototool on a Blank Film.
     */
    @ClientTest(timeoutTicks = 2400)
    public static void framesXp_lithographyWindowPrintsThePhototool(final ClientTestContext ctx) {
        final BlockPos computer = CORNER.east(2);
        final BlockPos monitor = computer.east();
        final BlockPos printer = computer.south();
        final Item phototool = ChainParts.PHOTOTOOLS.getFirst().get();
        ctx.thenBuild(0, world -> {
                    final PersonalComputerBlockEntity pc = EraNetwork.runningComputer(world, EraKit.LEGACY,
                            computer);
                    pc.console().install(Programs.LITHOGRAPHY.toString());
                    world.placeMonitor(monitor, Direction.EAST, ComputingModule.LEGACY_MONITOR.get());
                    world.setBlock(printer, ComputingModule.LEGACY_PRINTER.get());
                })
                .thenServer(SETTLE, level -> printerAt(ctx, level, printer).paper().setStackInSlot(0,
                        new ItemStack(ChainChips.BLANK_FILM.get(), 4)))
                .thenTeleport(SETTLE, monitor.east(2), Direction.WEST)
                .thenRightClick(SETTLE, monitor)
                .thenAwaitScreen(DesktopScreen.class, BOOT_WAIT)
                .thenWaitUntil(() -> !ctx.screen(DesktopScreen.class).launcherLabels().isEmpty(), SCREEN_WAIT,
                        "the desktop to list its programs")
                .then(SETTLE, () -> DesktopScreen.requestOpen(PROGRAM))
                .thenWaitUntil(() -> look(ctx) != null && look(ctx).printer() && look(ctx).blank(), SCREEN_WAIT,
                        "the window to show the printer with Blank Film in its tray")
                .then(SETTLE, () -> app(ctx).showKind(FilmPrinting.Kind.LAYOUTS))
                .thenScreenshot(SETTLE * 2, "lithography-printer-layouts")
                .then(SETTLE, () -> {
                    app(ctx).showKind(FilmPrinting.Kind.PHOTOTOOLS);
                    app(ctx).pickNamed("Vintage board phototool");
                })
                .thenScreenshot(SETTLE * 2, "lithography-printer-phototools")
                .then(SETTLE, () -> app(ctx).writeMask())
                .thenWaitUntilServer(level -> printerAt(ctx, level, printer).output().getStackInSlot(0).is(phototool),
                        WRITE_WAIT, "the printer to print the Vintage's phototool",
                        level -> "its output holds " + printerAt(ctx, level, printer).output().getStackInSlot(0))
                .thenScreenshot(SETTLE * 2, "lithography-printer-printed");
    }

    private static PrinterBlockEntity printerAt(final ClientTestContext ctx, final ServerLevel level,
                                                final BlockPos printer) {
        return (PrinterBlockEntity) level.getBlockEntity(ctx.abs(printer));
    }

    /* Switches the game to {@code code} and waits for its resources to load again and the loading screen to go. */
    private static ClientTestContext language(final ClientTestContext ctx, final String code) {
        final CompletableFuture<?>[] reload = new CompletableFuture<?>[1];
        return ctx.then(SETTLE, () -> {
            ctx.mc().getLanguageManager().setSelected(code);
            reload[0] = ctx.mc().reloadResourcePacks();
        }).thenWaitUntil(() -> reload[0] != null && reload[0].isDone() && ctx.mc().getOverlay() == null,
                RELOAD_WAIT, "the game to load " + code);
    }

    /** The Mercury Arc Tube says what it is made of, under its name, as its approved mock does. */
    @ClientTest(timeoutTicks = 100)
    public static void tooltip_mercuryArcTubeSaysWhatItIsMadeOf(final ClientTestContext ctx) {
        ctx.then(SETTLE, () -> {
            final List<String> lines = tooltip(ctx, new ItemStack(ChainParts.MERCURY_ARC_TUBE.get()));
            ctx.assertTrue(lines.size() >= 3 && lines.get(0).equals("Mercury Arc Tube")
                    && lines.get(1).equals("Fused quartz, tungsten electrodes")
                    && lines.get(2).equals("and a bead of mercury"), "the tube's name and its two lines: " + lines);
        });
    }

    @ClientTest(timeoutTicks = 600)
    public static void jei_maskWritingShowsHowTheK8MaskIsMadeAndTheTooltipsSayIt(final ClientTestContext ctx) {
        final ItemStack k8 = new ItemStack(mask(HardwareEra.LEGACY, "k8"));
        ctx.then(SETTLE, () -> {
                    final String blank = String.join(" ", tooltip(ctx, new ItemStack(ChainChips.MASK_BLANK.get())));
                    ctx.assertTrue(blank.contains("Becomes a mask in a Lithography Station's mask slot.")
                            && blank.contains("Legacy on: written by the Lithography program"), "the blank says how it"
                            + " becomes a mask: " + blank);
                    final String mask = String.join(" ", tooltip(ctx, k8));
                    ctx.assertTrue(mask.contains("Never used up: every K8 processor wafer of the Legacy is exposed")
                            && mask.contains("Written by the Lithography program."), "the mask says it is never used"
                            + " up and how it is made: " + mask);
                })
                .then(SETTLE, () -> ctx.assertTrue(!ModList.get().isLoaded("jei")
                        || JscJeiPlugin.showMaskWriting(k8), "the viewer opens on how the mask is made"))
                .thenWaitUntil(() -> !ModList.get().isLoaded("jei") || JscJeiPlugin.viewerScreenOpen(), SCREEN_WAIT,
                        "the viewer's recipe screen")
                .thenScreenshot(SETTLE * 4, "jei-mask-writing")
                .then(SETTLE, () -> ctx.mc().setScreen(null));
    }

    /**
     * A wafer's steps in the viewer (computers/docs/design/survival.md, "How a chip is made"): each step has its pages,
     * a page a mask for lithography, an era for etching and implanting, a metal for deposition and a chip for
     * packaging; U on an etched wafer shows where it goes, R the etching that makes it; and each step's page is its
     * machine's face at work, the step's items in the face's slots.
     */
    @ClientTest(timeoutTicks = 1200)
    public static void jei_waferStepsShowEachStepOnItsMachinesFace(final ClientTestContext ctx) {
        if (!ModList.get().isLoaded("jei")) {
            ctx.then(0, () -> { });
            return;
        }
        final ItemStack wafer = new ItemStack(WaferWork.item(HardwareEra.LEGACY, WaferWork.Step.IMPLANT));
        ctx.then(SETTLE, () -> {
            final Map<WaferPages.Step, Long> counts = new EnumMap<>(WaferPages.Step.class);
            for (final WaferPages.Step step : WaferPages.Step.values()) {
                counts.put(step, JscJeiPlugin.waferPageCount(step));
            }
            ctx.assertTrue(counts.get(WaferPages.Step.ETCH) == ChainParts.ERAS.size()
                    && counts.get(WaferPages.Step.IMPLANT) == ChainParts.ERAS.size()
                    && counts.get(WaferPages.Step.DEPOSITION) == DEPOSITIONS
                    && counts.get(WaferPages.Step.LITHOGRAPHY) >= MASKS
                    && counts.get(WaferPages.Step.PACKAGING) >= MASKS, "each step has its pages: " + counts);
        })
                .then(SETTLE, () -> ctx.assertTrue(JscJeiPlugin.showUses(wafer), "U on an etched wafer"))
                .thenWaitUntil(JscJeiPlugin::viewerScreenOpen, SCREEN_WAIT, "the viewer's recipe screen")
                .thenScreenshot(SETTLE * 4, "jei-wafer-uses")
                .then(SETTLE, () -> ctx.mc().setScreen(null))
                .then(SETTLE, () -> ctx.assertTrue(JscJeiPlugin.showRecipesFor(wafer),
                        "R on an etched wafer, the etching that makes it"))
                .thenWaitUntil(JscJeiPlugin::viewerScreenOpen, SCREEN_WAIT, "the viewer's recipe screen")
                .thenScreenshot(SETTLE * 4, "jei-wafer-recipes")
                .then(SETTLE, () -> ctx.mc().setScreen(null));
        for (final WaferPages.Step step : WaferPages.Step.values()) {
            ctx.then(SETTLE, () -> ctx.assertTrue(JscJeiPlugin.showWaferStep(step), "the viewer opens on " + step))
                    .thenWaitUntil(JscJeiPlugin::viewerScreenOpen, SCREEN_WAIT, "the viewer's recipe screen")
                    .thenScreenshot(SETTLE * 4, "jei-wafer-" + step.name().toLowerCase(Locale.ROOT))
                    .then(SETTLE, () -> ctx.mc().setScreen(null));
        }
    }

    /* The station {@code machine} laid round its own terminal and formed, its peripheral port cabled to the east. */
    private static ClientTestContext station(final ClientTestContext ctx, final ChainMachine machine) {
        return ctx.thenBuild(0, world -> lay(world, machine))
                .thenServer(SETTLE, level -> TestCables.lay(level, ctx.abs(port(machine).east()),
                        ComputingModule.VINTAGE_PERIPHERAL_CABLE.get()))
                .thenWaitUntilServer(level -> station(ctx, level, machine).formed(), SCREEN_WAIT,
                        machine.id() + " to form", level -> "it stands unformed");
    }

    /* A Mask Blank put in the station's first input slot. */
    private static void blank(final ClientTestContext ctx, final ServerLevel level, final ChainMachine machine) {
        station(ctx, level, machine).getInventory().insertItem(0, new ItemStack(ChainChips.MASK_BLANK.get()), false);
    }

    private static ChainMachineBlockEntity station(final ClientTestContext ctx, final ServerLevel level,
                                                   final ChainMachine machine) {
        return (ChainMachineBlockEntity) level.getBlockEntity(ctx.abs(place(machine, '#')));
    }

    /* Lays {@code machine}'s blocks from {@link #CORNER} facing north, its Machine Terminal at its place. */
    private static void lay(final TestWorldBuilder world, final ChainMachine machine) {
        final List<List<String>> layers = machine.layers();
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < layers.get(y).size(); z++) {
                final String row = layers.get(y).get(z);
                for (int x = 0; x < row.length(); x++) {
                    final char letter = row.charAt(x);
                    final Block block = letter == '#' ? ChainMachines.terminalOf(machine).get()
                            : ChainMachineBlocks.byId(machine.legend().get(letter)).get();
                    final BlockState state = block.defaultBlockState();
                    world.setBlock(CORNER.offset(x, y, z), state.hasProperty(HorizontalDirectionalBlock.FACING)
                            ? state.setValue(HorizontalDirectionalBlock.FACING, Direction.NORTH) : state);
                }
            }
        }
    }

    private static BlockPos port(final ChainMachine machine) {
        for (final var entry : machine.legend().entrySet()) {
            if (entry.getValue().equals("peripheral_port")) {
                return place(machine, entry.getKey());
            }
        }
        throw new IllegalArgumentException(machine.id() + " has no peripheral port");
    }

    /* Where {@link #lay} sets the first block of the letter {@code letter} of {@code machine}'s shape. */
    private static BlockPos place(final ChainMachine machine, final char letter) {
        final List<List<String>> layers = machine.layers();
        for (int y = 0; y < layers.size(); y++) {
            for (int z = 0; z < layers.get(y).size(); z++) {
                final int x = layers.get(y).get(z).indexOf(letter);
                if (x >= 0) {
                    return CORNER.offset(x, y, z);
                }
            }
        }
        throw new IllegalArgumentException(machine.id() + " has no " + letter);
    }

    /* The mask of the processor {@code path} of {@code era}. */
    private static Item mask(final Era era, final String path) {
        return ChainChips.PROCESSORS.stream().filter(die -> die.era() == era && die.path().equals(path)).findFirst()
                .orElseThrow().mask().get();
    }

    /* The lines of {@code stack}'s tooltip, as a player reads them. */
    private static List<String> tooltip(final ClientTestContext ctx, final ItemStack stack) {
        return stack.getTooltipLines(Item.TooltipContext.of(ctx.mc().level), ctx.player(), TooltipFlag.NORMAL).stream()
                .map(Component::getString).toList();
    }

    private static CommandPromptScreen<?> prompt(final ClientTestContext ctx) {
        return ctx.screen(CommandPromptScreen.class);
    }

    /* What the program's text screen draws, row after row; nothing while no program draws the glass. */
    private static String glass(final ClientTestContext ctx) {
        final CommandPromptScreen<?> screen = prompt(ctx);
        final TextScreen drawn = screen == null ? null : screen.editorScreen();
        return drawn == null ? "" : drawn.text();
    }

    private static @Nullable LithographyApp app(final ClientTestContext ctx) {
        final DesktopScreen desktop = ctx.openScreen(DesktopScreen.class);
        final DesktopWindow window = desktop == null ? null : desktop.windowFor(PROGRAM);
        final IDesktopApp app = window == null ? null : window.app();
        return app instanceof LithographyApp lithography ? lithography : null;
    }

    private static @Nullable LithographyLook look(final ClientTestContext ctx) {
        final LithographyApp app = app(ctx);
        return app == null ? null : app.look();
    }

    /** A Vintage machine's parts: its board, a CPU, memory, a supply, a VGA card for its monitor and a disk. */
    private static void buildVintage(final PersonalComputerBlockEntity computer) {
        final ItemStackHandler hardware = computer.getHardware();
        hardware.setStackInSlot(PersonalComputerBlockEntity.MOTHERBOARD_SLOT,
                new ItemStack(HardwareItems.MOTHERBOARD_BABYAT_VINTAGE.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.CPU_SLOT,
                new ItemStack(HardwareItems.CPU_INTEGRA_486SX.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.RAM_SLOTS_START,
                new ItemStack(HardwareItems.RAM_SIMM_4.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.PSU_SLOT, new ItemStack(HardwareItems.PSU_300.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.GPU_SLOTS_START,
                new ItemStack(HardwareItems.GPU_VGA_256.get()));
        hardware.setStackInSlot(PersonalComputerBlockEntity.DISK_SLOTS_START,
                new ItemStack(ComputingModule.disk(StorageTier.HDD, DiskSize.GB_500)));
    }
}
