/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.clienttest.computers;

import dev.jstech.computers.blockentity.CraftingComputerBlockEntity;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PersonalComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.PersonalComputerScreen;
import dev.jstech.computers.client.os.DesktopScreen;
import dev.jstech.computers.client.os.ShellApp;
import dev.jstech.computers.gui.layout.AssemblyGeometry.SeatKind;
import dev.jstech.computers.gui.layout.AssemblyGeometry;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.hardware.SoundCardSpec;
import dev.jstech.computers.operation.NetworkStorage;
import dev.jstech.computers.operation.payload.OperationRecord;
import dev.jstech.computers.operation.payload.WireLine;
import dev.jstech.computers.os.FilesystemKind;
import dev.jstech.computers.os.fs.DiskFilesystem;
import dev.jstech.computers.os.fs.FileType;
import dev.jstech.computers.rack.RaidMode;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.uuid.NetworkUuid;
import dev.jstech.testkit.client.ClientTestFailure;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.CraftFiles;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.EraParts;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import org.lwjgl.glfw.GLFW;

/**
 * The end of each era's journey. With a window open on the computer (from the Legacy), a program of the player's
 * running on it, a craft under way, the rack's storage server holding its array as a mirror and the network up, the
 * world is saved and loaded again, and each is as it was: the window is open again when the player sits back down,
 * the program still runs, the craft carries on, the mirror is still a mirror over both its disks, and the network is
 * the same network. Then the computer is broken by a player who is not in creative: its case and every part fall
 * from it, the player picks them up, puts the case back down and builds it again through its screen, part by part,
 * and the system on its disk comes back up.
 */
public final class EraEndClientTests {

    private static final int SETTLE = EraJourney.SETTLE;
    private static final int WAIT = 200;
    private static final int TIMEOUT = 12_000;
    private static final int STORAGE_ROW = 2;
    /* Within what a mirror of the era's smallest disks holds, logs and planks together. */
    private static final int LOGS = 400;
    private static final int PLANKS = 400;
    /** What a Vintage Crafting Computer, under a run a tick, makes in a journey's time, from what its disks hold. */
    private static final int VINTAGE_PLANKS = 16;
    private static final int VINTAGE_LOGS = 8;
    /** Where the player stands to break the computer and pick up what falls from it. */
    private static final BlockPos BY_THE_COMPUTER = new BlockPos(9, 2, 4);
    /** East of the computer, past its monitor, to put the case down facing east again. */
    private static final BlockPos EAST_OF_THE_COMPUTER = new BlockPos(11, 2, 2);
    private static final String CALCULATOR = "Calculator";
    private static final String SIGMA_BEAT = "using Standard.*; namespace Beat; class Beat : Script {"
            + " public override void OnTick() { } }";
    private static final String SHARP_BEAT = "using System.*; namespace Beat; class Beat : IScript {"
            + " public void OnInit() { } public void OnTick() { } public void OnDestroy() { } }";

    private EraEndClientTests() {
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void vintage_everythingComesBackAndTheComputerIsRebuilt(final ClientTestContext ctx) {
        end(ctx, EraKit.VINTAGE);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_everythingComesBackAndTheComputerIsRebuilt(final ClientTestContext ctx) {
        end(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void transition_everythingComesBackAndTheComputerIsRebuilt(final ClientTestContext ctx) {
        end(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_everythingComesBackAndTheComputerIsRebuilt(final ClientTestContext ctx) {
        end(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void advanced_everythingComesBackAndTheComputerIsRebuilt(final ClientTestContext ctx) {
        end(ctx, EraKit.ADVANCED);
    }

    private static void end(final ClientTestContext ctx, final EraKit kit) {
        final boolean desktop = kit != EraKit.VINTAGE;
        final AtomicReference<NetworkUuid> network = new AtomicReference<>();
        EraJourney.lay(ctx, kit).thenBuild(0, world -> {
            final CraftingComputerBlockEntity crafter = EraNetwork.craftingComputer(world, kit);
            crafter.loadPattern(CraftFiles.oakPlanks());
            final ServerRackBlockEntity rack = world.blockEntity(EraNetwork.RACK, ServerRackBlockEntity.class);
            rack.getServers().setStackInSlot(STORAGE_ROW, EraNetwork.storageServer(kit));
            for (int disk = 0; disk < 2; disk++) {
                rack.insertDrive(STORAGE_ROW, new ItemStack(EraParts.smallestDiskFor(kit.era())));
            }
        });
        ctx.thenServer(SETTLE, level -> {
            final MainframeBlockEntity mainframe = mainframe(ctx, level);
            network.set(mainframe.networkUuid());
            NetworkStorage.of(level, mainframe.networkUuid()).insert(StorageKey.of(Items.OAK_LOG),
                    desktop ? LOGS : VINTAGE_LOGS);
            ctx.assertTrue(rack(ctx, level).setRaidMode(STORAGE_ROW, RaidMode.RAID1), "the storage server mirrors");
            final PersonalComputerBlockEntity computer = computer(ctx, level);
            computer.console().install("jsc:" + (desktop ? "sgsc" : "scc"));
            computer.console().install("jsc:sigma");
            DiskFilesystem.write(computer.systemDisk(), desktop ? "progs/beat.sgs" : "progs/beat.sg",
                    desktop ? FileType.SGS : FileType.SG, desktop ? SHARP_BEAT : SIGMA_BEAT, Long.MAX_VALUE,
                    FilesystemKind.HIERARCHICAL);
        });
        EraJourney.sit(ctx, kit);
        if (desktop) {
            EraJourney.launch(ctx, kit, CALCULATOR)
                    .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(CALCULATOR) != null, WAIT,
                            "the Calculator to open from Start");
            final String terminal = EraJourney.terminal(kit);
            EraJourney.launch(ctx, kit, terminal)
                    .thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(terminal) != null, WAIT,
                            "the " + terminal + " to open from Start");
        }
        line(ctx, (desktop ? "sgsc" : "scc") + " progs\\beat." + (desktop ? "sgs" : "sg"));
        // MC-DOS leaves a line running as a task with start; Frames' start opens installed programs by their name.
        line(ctx, (desktop ? "" : "start ") + "sigma run progs\\beat.asm");
        final AtomicReference<String> shown = new AtomicReference<>("");
        ctx.then(WAIT / 4, () -> shown.set(terminalText(ctx, kit)))
                .thenWaitUntilServer(level -> running(ctx, level), WAIT, "the player's program to run",
                        level -> "programs=" + programs(ctx, level) + "; the prompt shows " + shown.get()
                                + glassText(computer(ctx, level)));
        // The planks reach the network when the craft ends, which on the faster eras is before the save.
        final long asked = desktop ? PLANKS : VINTAGE_PLANKS;
        line(ctx, "iql CRAFT " + asked + " oak_planks");
        ctx.thenWaitUntilServer(level -> !crafts(ctx, level).isEmpty()
                                || stored(ctx, level, Items.OAK_PLANKS) >= asked, WAIT * 2,
                        "the network to take the craft", level -> "planks=" + stored(ctx, level, Items.OAK_PLANKS))
                .thenWaitUntil(EraJourney.away(ctx), WAIT, "the desk to be left")
                .thenSaveAndReload(SETTLE)
                .thenWaitUntilServer(level -> network.get().equals(mainframe(ctx, level).networkUuid()), WAIT,
                        "the network to come back as the same network",
                        level -> "was " + network.get() + ", is " + mainframe(ctx, level).networkUuid())
                .thenWaitUntilServer(level -> rack(ctx, level).raidModeOf(STORAGE_ROW) == RaidMode.RAID1
                                && rack(ctx, level).claimedDriveStacks(STORAGE_ROW).size() == 2, WAIT,
                        "the mirror to come back over both its disks",
                        level -> "mode=" + rack(ctx, level).raidModeOf(STORAGE_ROW))
                .thenWaitUntilServer(level -> running(ctx, level), WAIT, "the player's program to still run",
                        level -> "programs=" + programs(ctx, level))
                .thenWaitUntilServer(level -> stored(ctx, level, Items.OAK_PLANKS) >= asked, WAIT * 3,
                        "the craft asked for before the save to be whole after it",
                        level -> "planks=" + stored(ctx, level, Items.OAK_PLANKS) + " of " + asked + "; crafts "
                                + described(crafts(ctx, level)) + "; the crafter can craft "
                                + crafter(ctx, level).canCraft() + ", busy " + crafter(ctx, level).craftBusy());
        EraJourney.sit(ctx, kit);
        if (desktop) {
            ctx.thenWaitUntil(() -> ctx.screen(DesktopScreen.class).windowFor(CALCULATOR) != null, WAIT,
                    "the Calculator to be open again when the player sits back down");
        }
        ctx.thenScreenshot(SETTLE, shot(kit, "after-reload"));
        rebuilt(ctx, kit);
    }

    /*
     * The computer broken by a player out of creative, its case and parts picked up, the case put back down and
     * built again through its screen, and its system up again on the monitor.
     */
    private static void rebuilt(final ClientTestContext ctx, final EraKit kit) {
        final List<Item> parts = parts(kit);
        final List<SeatKind> seats = seats(kit);
        final AtomicReference<List<ItemStack>> picked = new AtomicReference<>(List.of());
        ctx.thenWaitUntil(EraJourney.away(ctx), WAIT, "the desk to be left")
                .thenServer(SETTLE, level -> {
                    ctx.serverPlayer().getInventory().clearContent();
                    ctx.serverPlayer().setGameMode(GameType.SURVIVAL);
                })
                .thenTeleport(SETTLE, BY_THE_COMPUTER, Direction.NORTH)
                .then(SETTLE, () -> ctx.selectHotbar(8))
                .thenWaitUntil(new Breaking(ctx, EraNetwork.COMPUTER), WAIT * 3,
                        "the computer to come apart under the player's hand")
                .thenTeleport(SETTLE, EraNetwork.COMPUTER, Direction.NORTH)
                .thenWaitUntilServer(level -> holds(ctx.serverPlayer().getInventory(), kit.computer().asItem())
                                && parts.stream().allMatch(part -> holds(ctx.serverPlayer().getInventory(), part)),
                        WAIT, "the case and every part to fall and be picked up",
                        level -> "inventory=" + ctx.serverPlayer().getInventory().items)
                .thenScreenshot(SETTLE, shot(kit, "broken"))
                /*
                 * What was picked up laid out along the hotbar, the case first and then the parts in the order they go
                 * in, as the player would sort them: the very stacks that fell, so the disk keeps its system.
                 */
                .thenServer(SETTLE, level -> {
                    final Inventory inventory = ctx.serverPlayer().getInventory();
                    final ItemStack caseStack = taken(inventory, kit.computer().asItem());
                    final List<ItemStack> sorted = new ArrayList<>();
                    for (final Item part : parts) {
                        sorted.add(taken(inventory, part));
                    }
                    inventory.clearContent();
                    picked.set(sorted);
                    ctx.serverPlayer().setGameMode(GameType.CREATIVE);
                    ctx.give(0, caseStack);
                })
                .thenTeleport(SETTLE, EAST_OF_THE_COMPUTER, Direction.WEST)
                .then(SETTLE, () -> ctx.selectHotbar(0))
                .thenPlace(1, EraNetwork.COMPUTER)
                .thenWaitUntilServer(level -> level.getBlockEntity(ctx.abs(EraNetwork.COMPUTER))
                                instanceof PersonalComputerBlockEntity, WAIT, "the case to go back down",
                        level -> "found " + level.getBlockState(ctx.abs(EraNetwork.COMPUTER)))
                .thenServer(0, level -> {
                    for (int slot = 0; slot < 9; slot++) {
                        ctx.give(slot, slot < picked.get().size() ? picked.get().get(slot) : ItemStack.EMPTY);
                    }
                })
                .thenTeleport(SETTLE, BY_THE_COMPUTER, Direction.NORTH)
                .then(SETTLE, () -> ctx.selectHotbar(8))
                .then(1, () -> ctx.rightClick(EraNetwork.COMPUTER, Direction.SOUTH))
                .thenAwaitScreen(PersonalComputerScreen.class, WAIT);
        for (int i = 0; i < seats.size(); i++) {
            final int slot = i;
            final SeatKind kind = seats.get(i);
            final int index = (int) seats.subList(0, i).stream().filter(kind::equals).count();
            ctx.then(2, () -> EraJourney.seatFromHotbar(ctx, PersonalComputerScreen.class, slot, kind, index));
        }
        ctx.thenWaitUntilServer(level -> parts.stream().allMatch(part -> seated(computer(ctx, level), part)), WAIT,
                        "every part to sit in the case again", level -> "hardware=" + hardware(computer(ctx, level)))
                .then(SETTLE, () -> {
                    final AssemblyGeometry.Box power = ctx.screen(PersonalComputerScreen.class).geometry().power();
                    ctx.clickGui(power.x() + power.w() / 2, power.y() + power.h() / 2);
                })
                .thenWaitUntilServer(level -> computer(ctx, level).isRunning(), WAIT,
                        "the power button to start the rebuilt computer", level -> "off")
                .thenWaitUntil(EraJourney.away(ctx), WAIT, "the screen to be left")
                .thenTeleport(SETTLE, EraNetwork.AT_THE_DESK, Direction.WEST)
                .thenRightClick(SETTLE, EraNetwork.MONITOR)
                .thenAwaitScreen(EraJourney.systemScreen(kit), EraJourney.BOOT_WAIT)
                .thenScreenshot(SETTLE * 3, shot(kit, "rebuilt"));
    }

    /* The parts the era's computer is built from, in the order its screen takes them back. */
    private static List<Item> parts(final EraKit kit) {
        final Item board = kit.computerBoard();
        final List<Item> parts = new ArrayList<>(List.of(board, EraParts.cpuFor(board), EraParts.ramFor(board),
                EraParts.psuFor(kit.era()), EraParts.diskFor(kit.era()), EraParts.cardFor(board, GpuSpec.class)));
        if (!EraParts.board(board).hasOnBoardAudio()) {
            parts.add(EraParts.cardFor(board, SoundCardSpec.class));
        }
        return parts;
    }

    private static List<SeatKind> seats(final EraKit kit) {
        final List<SeatKind> seats = new ArrayList<>(List.of(SeatKind.BOARD, SeatKind.CPU, SeatKind.RAM, SeatKind.PSU,
                SeatKind.DISK, SeatKind.CARD));
        if (!EraParts.board(kit.computerBoard()).hasOnBoardAudio()) {
            seats.add(SeatKind.CARD);
        }
        return seats;
    }

    /* Types a line at the prompt, the system's own or its terminal's, and sends it. */
    private static void line(final ClientTestContext ctx, final String line) {
        ctx.then(SETTLE, () -> {
            ctx.type(line);
            ctx.key(GLFW.GLFW_KEY_ENTER);
        });
    }

    /* The crafts the network has in hand or has done. */
    private static List<OperationRecord> crafts(final ClientTestContext ctx, final ServerLevel level) {
        final List<OperationRecord> out = new ArrayList<>();
        final MainframeBlockEntity mainframe = mainframe(ctx, level);
        for (final List<OperationRecord> records : List.of(mainframe.activeOperationRecords(),
                mainframe.recentOperations())) {
            records.stream().filter(one -> one.type() == OperationRecord.TYPE_CRAFT).forEach(out::add);
        }
        return out;
    }

    private static String described(final List<OperationRecord> crafts) {
        final StringBuilder out = new StringBuilder();
        for (final OperationRecord one : crafts) {
            out.append("[status ").append(one.status()).append(' ').append(one.moved()).append('/')
                    .append(one.requested()).append(" cause ").append(one.cause()).append(']');
        }
        return out.toString();
    }

    private static CraftingComputerBlockEntity crafter(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.SECOND_COMPUTER,
                CraftingComputerBlockEntity.class);
    }

    /* Whether the player's program runs, by the name it gave itself or the file it was started from. */
    private static boolean running(final ClientTestContext ctx, final ServerLevel level) {
        return computer(ctx, level).programs().view().stream()
                .anyMatch(one -> (one.name() + " " + one.file()).toLowerCase(Locale.ROOT).contains("beat"));
    }

    private static String programs(final ClientTestContext ctx, final ServerLevel level) {
        final StringBuilder out = new StringBuilder();
        computer(ctx, level).programs().view().forEach(one -> out.append('[').append(one.name()).append(' ')
                .append(one.file()).append(']'));
        return out.toString();
    }

    /* The last of what the terminal window shows on a desktop; MC-DOS's own prompt is read from its glass. */
    private static String terminalText(final ClientTestContext ctx, final EraKit kit) {
        if (kit == EraKit.VINTAGE) {
            return "";
        }
        final ShellApp shell = DesktopSteps.app(ctx, EraJourney.terminal(kit), ShellApp.class);
        final String text = shell == null ? "no terminal" : shell.scrollbackText();
        return text.substring(Math.max(0, text.length() - 300));
    }

    /* The last lines on the machine's own glass. */
    private static String glassText(final PersonalComputerBlockEntity computer) {
        final StringBuilder out = new StringBuilder();
        for (final WireLine line : computer.console().glass()) {
            line.spans().forEach(span -> out.append(span.text().english()));
            out.append('\n');
        }
        return out.substring(Math.max(0, out.length() - 300));
    }

    /* One of {@code item} taken out of the inventory, the stack that was picked up with its data. */
    private static ItemStack taken(final Inventory inventory, final Item item) {
        for (final ItemStack stack : inventory.items) {
            if (stack.is(item)) {
                return stack.split(1);
            }
        }
        throw new ClientTestFailure("nothing of " + item + " was picked up");
    }

    private static boolean holds(final Inventory inventory, final Item item) {
        for (final ItemStack stack : inventory.items) {
            if (stack.is(item)) {
                return true;
            }
        }
        return false;
    }

    private static boolean seated(final PersonalComputerBlockEntity computer, final Item part) {
        for (int slot = 0; slot < computer.getHardware().getSlots(); slot++) {
            if (computer.getHardware().getStackInSlot(slot).is(part)) {
                return true;
            }
        }
        return false;
    }

    private static String hardware(final PersonalComputerBlockEntity computer) {
        final List<String> out = new ArrayList<>();
        for (int slot = 0; slot < computer.getHardware().getSlots(); slot++) {
            out.add(String.valueOf(computer.getHardware().getStackInSlot(slot)));
        }
        return String.join(", ", out);
    }

    private static long stored(final ClientTestContext ctx, final ServerLevel level, final Item item) {
        return NetworkStorage.of(level, mainframe(ctx, level).networkUuid()).count(StorageKey.of(item));
    }

    private static PersonalComputerBlockEntity computer(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.COMPUTER,
                PersonalComputerBlockEntity.class);
    }

    private static MainframeBlockEntity mainframe(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.MAINFRAME, MainframeBlockEntity.class);
    }

    private static ServerRackBlockEntity rack(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(EraNetwork.RACK, ServerRackBlockEntity.class);
    }

    private static String shot(final EraKit kit, final String what) {
        return kit.name().toLowerCase(Locale.ROOT) + "-" + what;
    }

    /* Done once the block is gone; until then the player keeps the button down on it, as one breaking it does. */
    private static final class Breaking implements BooleanSupplier {

        private final ClientTestContext ctx;
        private final BlockPos target;
        private boolean started;

        Breaking(final ClientTestContext ctx, final BlockPos target) {
            this.ctx = ctx;
            this.target = target;
        }

        @Override
        public boolean getAsBoolean() {
            final BlockPos at = ctx.abs(target);
            if (Objects.requireNonNull(ctx.mc().level, "no level").getBlockState(at).isAir()) {
                return true;
            }
            final var mode = Objects.requireNonNull(ctx.mc().gameMode, "no game mode");
            if (!started) {
                mode.startDestroyBlock(at, Direction.SOUTH);
                started = true;
            } else {
                mode.continueDestroyBlock(at, Direction.SOUTH);
            }
            return false;
        }
    }
}
