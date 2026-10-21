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
import dev.jstech.computers.block.AbstractSmallComputerBlock;
import dev.jstech.computers.block.part.CablePartItem;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.blockentity.CaseFront;
import dev.jstech.computers.blockentity.MainframeBlockEntity;
import dev.jstech.computers.blockentity.PartRefusals;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.client.AbstractMachineAssemblyScreen;
import dev.jstech.computers.client.KvmChannelScreen;
import dev.jstech.computers.client.MainframeScreen;
import dev.jstech.computers.client.PersonalComputerScreen;
import dev.jstech.computers.client.ServerAssemblyScreen;
import dev.jstech.computers.client.ServerRackScreen;
import dev.jstech.computers.gui.layout.AssemblyGeometry.Seat;
import dev.jstech.computers.gui.layout.AssemblyGeometry.SeatKind;
import dev.jstech.computers.gui.layout.ServerRackLayout;
import dev.jstech.computers.hardware.BayDriveSpec;
import dev.jstech.computers.hardware.CaseFrontLayout;
import dev.jstech.computers.hardware.FormFactor;
import dev.jstech.computers.hardware.FrontArea;
import dev.jstech.computers.item.GpuItem;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.item.SoundCardItem;
import dev.jstech.computers.os.media.EjectButton;
import dev.jstech.computers.registry.ComputingContent;
import dev.jstech.computers.workshop.UpdateDoor;
import dev.jstech.computers.workshop.WorkshopCard;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.cable.CableItem;
import dev.jstech.testkit.client.ClientTestFailure;
import dev.jstech.tests.clienttest.ClientTest;
import dev.jstech.tests.clienttest.ClientTestContext;
import dev.jstech.tests.testkit.computers.EraKit;
import dev.jstech.tests.testkit.computers.EraNetwork;
import dev.jstech.tests.testkit.computers.EraParts;
import dev.jstech.tests.testkit.computers.TestWorldBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * Each era built by hand, with nothing laid by the test. Every block of the era, and every block the eras share, is
 * put down from the hand, and every cable part is snapped onto a cable the hand laid first. The era's Mainframe and
 * Personal Computer are put together part by part through their screens: the computer first has a processor of
 * another socket held over its processor slot, which says the rule that keeps it out; it is then started from its
 * front's power button, a disk goes into its hot-swap bay and comes out again, a stick goes into its first port, the
 * tray of its optical drive opens and closes, and its side comes off and goes back on. And the era's rack is filled
 * through its own screen: the 1U server, the Storage Server with the Vaultis Array the player seats in it through the
 * server's screen, the Compute Server, the KVM Switch, the UPS, the Cooling Unit and, from the Standard on, the Fibre
 * Optic Interface, with disks in the servers' bays. The monitor beside it then shows the KVM's channel bar, and a
 * channel key puts each machine on the screen.
 */
public final class EraBuildClientTests {

    private static final int SETTLE = EraJourney.SETTLE;
    private static final int WAIT = EraJourney.SCREEN_WAIT;
    private static final int TIMEOUT = 6_000;
    /* The block grid: a cell every six blocks east and seven south, eight to a row, each with room for a Mainframe. */
    private static final int CELL_X = 6;
    private static final int CELL_Z = 7;
    private static final int ROW = 8;
    /** How far in front of a cell the player stands to put its block down. */
    private static final int REACH = 3;
    /** The Mainframe faces east from here, its other parts west and south of it. */
    private static final BlockPos MAINFRAME = new BlockPos(2, 2, 2);
    private static final BlockPos BEFORE_THE_MAINFRAME = new BlockPos(5, 2, 2);
    private static final BlockPos MAINFRAME_PART = new BlockPos(1, 2, 3);
    private static final BlockPos BY_THE_MAINFRAME_PART = new BlockPos(1, 2, 5);
    /** The computer faces south; its left side, the one that comes off, faces west. */
    private static final BlockPos COMPUTER = new BlockPos(9, 2, 8);
    private static final BlockPos BEFORE_THE_COMPUTER = new BlockPos(9, 2, 10);
    private static final BlockPos WEST_OF_THE_COMPUTER = new BlockPos(7, 2, 8);
    /** The rack faces south, as the era's network has it, its monitor east of it. */
    private static final BlockPos RACK = EraNetwork.RACK;
    private static final BlockPos AT_THE_RACK = new BlockPos(5, 2, 9);
    private static final BlockPos RACK_MONITOR = new BlockPos(6, 2, 6);
    private static final BlockPos AT_THE_RACK_MONITOR = new BlockPos(6, 2, 8);
    /* The rack's rows: the 1U server, the cooling or the fibre, the 2U storage and compute servers, KVM and UPS. */
    private static final int SERVER_ROW = 0;
    private static final int SECOND_ROW = 1;
    private static final int STORAGE_ROW = 2;
    private static final int COMPUTE_ROW = 4;
    private static final int KVM_ROW = 6;
    private static final int UPS_ROW = 7;
    /** The hotbar slot kept empty, for a hand that opens screens and takes things back. */
    private static final int EMPTY_HAND = 8;
    /** The shelves of things put in the world rather than in a machine: every one but the parts and the media. */
    private static final List<TagKey<Item>> SHELVES = shelves();

    private EraBuildClientTests() {
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void vintage_everyBlockGoesDownFromTheHand(final ClientTestContext ctx) {
        everyBlock(ctx, EraKit.VINTAGE);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_everyBlockGoesDownFromTheHand(final ClientTestContext ctx) {
        everyBlock(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void transition_everyBlockGoesDownFromTheHand(final ClientTestContext ctx) {
        everyBlock(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_everyBlockGoesDownFromTheHand(final ClientTestContext ctx) {
        everyBlock(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void advanced_everyBlockGoesDownFromTheHand(final ClientTestContext ctx) {
        everyBlock(ctx, EraKit.ADVANCED);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void shared_everyBlockGoesDownFromTheHand(final ClientTestContext ctx) {
        everyBlock(ctx, null);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void vintage_machinesAreBuiltThroughTheirScreens(final ClientTestContext ctx) {
        machines(ctx, EraKit.VINTAGE);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_machinesAreBuiltThroughTheirScreens(final ClientTestContext ctx) {
        machines(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void transition_machinesAreBuiltThroughTheirScreens(final ClientTestContext ctx) {
        machines(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_machinesAreBuiltThroughTheirScreens(final ClientTestContext ctx) {
        machines(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void advanced_machinesAreBuiltThroughTheirScreens(final ClientTestContext ctx) {
        machines(ctx, EraKit.ADVANCED);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void vintage_rackIsFilledThroughItsScreen(final ClientTestContext ctx) {
        rack(ctx, EraKit.VINTAGE);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void legacy_rackIsFilledThroughItsScreen(final ClientTestContext ctx) {
        rack(ctx, EraKit.LEGACY);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void transition_rackIsFilledThroughItsScreen(final ClientTestContext ctx) {
        rack(ctx, EraKit.TRANSITION);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void standard_rackIsFilledThroughItsScreen(final ClientTestContext ctx) {
        rack(ctx, EraKit.STANDARD);
    }

    @ClientTest(timeoutTicks = TIMEOUT)
    public static void advanced_rackIsFilledThroughItsScreen(final ClientTestContext ctx) {
        rack(ctx, EraKit.ADVANCED);
    }

    /*
     * Every block, cable and cable part on the era's creative tab, or on the shared tab for null, each put down in a
     * cell of its own from the hand, a cable part on a cable the hand laid there first.
     */
    private static void everyBlock(final ClientTestContext ctx, @Nullable final EraKit kit) {
        final List<Item> things = placeable(kit);
        ctx.assertTrue(!things.isEmpty(), "the tab has blocks to put down");
        ctx.thenGive(0);
        for (int i = 0; i < things.size(); i++) {
            final Item item = things.get(i);
            final BlockPos at = new BlockPos(2 + (i % ROW) * CELL_X, 2, 2 + (i / ROW) * CELL_Z);
            final String name = BuiltInRegistries.ITEM.getKey(item).toString();
            ctx.thenTeleport(SETTLE, at.south(REACH), Direction.NORTH);
            if (item instanceof CablePartItem part) {
                final CableEntry cable = cableFor(part, kit);
                ctx.thenGive(0, new ItemStack(cable.item().get()), new ItemStack(item))
                        .then(SETTLE, () -> ctx.selectHotbar(0))
                        .thenPlace(1, at)
                        .thenWaitUntilServer(level -> cableAt(ctx, level, at) != null
                                        && cableAt(ctx, level, at).holds(cable.get()), WAIT,
                                "the cable for " + name + " to go down from the hand",
                                level -> "found " + level.getBlockState(ctx.abs(at)))
                        .then(SETTLE, () -> ctx.selectHotbar(1))
                        .thenSneakClick(1, at, Direction.UP)
                        .thenWaitUntilServer(level -> cableAt(ctx, level, at) != null
                                        && cableAt(ctx, level, at).partType(Direction.UP) == part.partType(), WAIT,
                                name + " to snap onto the top of the cable",
                                level -> "parts=" + (cableAt(ctx, level, at) == null ? "no cable"
                                        : cableAt(ctx, level, at).partType(Direction.UP)));
                continue;
            }
            ctx.thenGive(0, new ItemStack(item))
                    .then(SETTLE, () -> ctx.selectHotbar(0))
                    .thenPlace(1, at)
                    .thenWaitUntilServer(level -> placed(ctx, level, at, item), WAIT,
                            name + " to go down from the hand", level -> "found " + level.getBlockState(ctx.abs(at)));
        }
        ctx.thenScreenshot(SETTLE, (kit == null ? "shared" : shot(kit, "")) + "every-block");
    }

    /*
     * The Mainframe and the Personal Computer put together through their screens, the computer started from its front
     * and its front and side worked by hand.
     */
    private static void machines(final ClientTestContext ctx, final EraKit kit) {
        mainframe(ctx, kit);
        computer(ctx, kit);
        front(ctx, kit);
    }

    /* The Mainframe put down and built in its screen: board, processor, memory, supply, disk, graphics, fibre card. */
    private static void mainframe(final ClientTestContext ctx, final EraKit kit) {
        final Item board = kit.mainframeBoard();
        final List<Item> parts = new ArrayList<>(List.of(board, EraParts.cpuFor(board), EraParts.ramFor(board),
                EraParts.psuFor(kit.era()), EraParts.diskFor(kit.era()), EraParts.cardFor(board, GpuItem.class)));
        final List<SeatKind> seats = new ArrayList<>(List.of(SeatKind.BOARD, SeatKind.CPU, SeatKind.RAM, SeatKind.PSU,
                SeatKind.DISK, SeatKind.CARD));
        if (kit.fibre()) {
            parts.add(ComputingModule.OPTICAL_NETWORK_CARD.get());
            seats.add(SeatKind.CARD);
        }
        ctx.thenGive(0, new ItemStack(kit.mainframe()))
                .thenTeleport(SETTLE, BEFORE_THE_MAINFRAME, Direction.WEST)
                .then(SETTLE, () -> ctx.selectHotbar(0))
                .thenPlace(1, MAINFRAME)
                .thenWaitUntilServer(level -> level.getBlockEntity(ctx.abs(MAINFRAME)) instanceof MainframeBlockEntity,
                        WAIT, "the Mainframe to go down from the hand",
                        level -> "found " + level.getBlockState(ctx.abs(MAINFRAME)))
                .thenGive(0, stacks(parts))
                .thenTeleport(SETTLE, BY_THE_MAINFRAME_PART, Direction.NORTH)
                .then(SETTLE, () -> ctx.selectHotbar(EMPTY_HAND))
                .thenRightClick(1, MAINFRAME_PART)
                .thenAwaitScreen(MainframeScreen.class, WAIT);
        seatAll(ctx, MainframeScreen.class, seats);
        ctx.thenWaitUntilServer(level -> holdsAll(mainframeAt(ctx, level).getInventory(), parts), WAIT,
                        "every part to sit in the Mainframe", level -> "missing " + missing(
                                mainframeAt(ctx, level).getInventory(), parts))
                .thenScreenshot(SETTLE, shot(kit, "mainframe-built"))
                .then(0, () -> ctx.clickGui(MainframeScreen.powerButtonX(), MainframeScreen.powerButtonY()))
                .thenWaitUntilServer(level -> mainframeAt(ctx, level).isRunning(), WAIT,
                        "the Mainframe's power button to start it", level -> "running=false")
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(WAIT);
    }

    /*
     * The computer put down and built in its screen, with a part of another socket held over its processor slot first,
     * then its bays filled: the hot-swap bay, and from the Legacy on an optical drive.
     */
    private static void computer(final ClientTestContext ctx, final EraKit kit) {
        final Item board = kit.computerBoard();
        final List<Item> parts = new ArrayList<>(List.of(board, EraParts.cpuFor(board), EraParts.ramFor(board),
                EraParts.psuFor(kit.era()), EraParts.diskFor(kit.era()), EraParts.cardFor(board, GpuItem.class)));
        final List<SeatKind> seats = new ArrayList<>(List.of(SeatKind.BOARD, SeatKind.CPU, SeatKind.RAM, SeatKind.PSU,
                SeatKind.DISK, SeatKind.CARD));
        if (!EraParts.board(board).hasOnBoardAudio()) {
            parts.add(EraParts.cardFor(board, SoundCardItem.class));
            seats.add(SeatKind.CARD);
        }
        // The Furnace Card wants a slot of the PCI family, which no Vintage board has.
        if (kit != EraKit.VINTAGE) {
            parts.add(UpdateDoor.cardItem(WorkshopCard.FURNACE).getItem());
            seats.add(SeatKind.CARD);
        }
        final Item otherSocket = EraParts.cpuFor((kit == EraKit.VINTAGE ? EraKit.ADVANCED : EraKit.VINTAGE)
                .computerBoard());
        // A case with no bays at its front, as the Advanced's, takes no bay parts at all.
        final List<Item> bays = new ArrayList<>();
        if (!frontOf(kit).bays().isEmpty()) {
            bays.add(HardwareItems.HOT_SWAP_DRIVE_BAY.get());
        }
        if (kit.opticalBayDrive() != null && frontOf(kit).bays().size() > 1) {
            bays.add(kit.opticalBayDrive());
        }
        ctx.thenGive(0, new ItemStack(kit.computer()))
                .thenTeleport(SETTLE, BEFORE_THE_COMPUTER, Direction.NORTH)
                .then(SETTLE, () -> ctx.selectHotbar(0))
                .thenPlace(1, COMPUTER)
                .thenWaitUntilServer(level -> level.getBlockEntity(ctx.abs(COMPUTER))
                                instanceof AbstractSmallComputerBlockEntity, WAIT,
                        "the computer to go down from the hand", level -> "found "
                                + level.getBlockState(ctx.abs(COMPUTER)))
                .thenGive(0)
                .thenTeleport(SETTLE, WEST_OF_THE_COMPUTER, Direction.EAST)
                .then(SETTLE, () -> ctx.selectHotbar(EMPTY_HAND))
                .then(1, () -> ctx.rightClick(COMPUTER, Direction.WEST))
                .thenAwaitScreen(PersonalComputerScreen.class, WAIT)
                // The parts in the hotbar, and in its last place the processor of another socket.
                .thenGive(0, stacks(parts))
                .thenServer(0, level -> ctx.give(EMPTY_HAND, new ItemStack(otherSocket)))
                // The board first, so the processor slot has a socket to name.
                .then(SETTLE, () -> seatFromHotbar(ctx, PersonalComputerScreen.class, 0, SeatKind.BOARD, 0))
                .then(2, () -> ctx.clickGui(hotbarX(EMPTY_HAND), hotbarY(ctx, PersonalComputerScreen.class)))
                .then(2, () -> {
                    final Seat cpu = seat(ctx, PersonalComputerScreen.class, SeatKind.CPU, 0);
                    final PersonalComputerScreen screen = ctx.screen(PersonalComputerScreen.class);
                    ctx.pointAt(screen.getGuiLeft() + cpu.x() + 8, screen.getGuiTop() + cpu.y() + 8);
                })
                .thenScreenshot(SETTLE, shot(kit, "refusal"))
                .thenAssert(0, () -> {
                    final PersonalComputerScreen screen = ctx.screen(PersonalComputerScreen.class);
                    final Component why = screen.getMenu().refusal(slotAt(screen, seat(ctx,
                            PersonalComputerScreen.class, SeatKind.CPU, 0)), ctx.player().containerMenu.getCarried());
                    return why != null && why.getString().equals(PartRefusals.CPU.english());
                }, "a processor of another socket over the processor slot says the rule that keeps it out")
                // Put back where it came from, and the hand that held it emptied for what comes after.
                .then(2, () -> ctx.clickGui(hotbarX(EMPTY_HAND), hotbarY(ctx, PersonalComputerScreen.class)))
                .thenServer(SETTLE, level -> ctx.give(EMPTY_HAND, ItemStack.EMPTY));
        for (int i = 1; i < seats.size(); i++) {
            final int slot = i;
            final SeatKind kind = seats.get(i);
            final int index = (int) seats.subList(0, i).stream().filter(kind::equals).count();
            ctx.then(2, () -> seatFromHotbar(ctx, PersonalComputerScreen.class, slot, kind, index));
        }
        ctx.thenGive(SETTLE, stacks(bays));
        for (int bay = 0; bay < bays.size(); bay++) {
            final int which = bay;
            ctx.then(2, () -> seatFromHotbar(ctx, PersonalComputerScreen.class, which, SeatKind.BAY, which));
        }
        final List<Item> all = new ArrayList<>(parts);
        all.addAll(bays);
        ctx.thenWaitUntilServer(level -> holdsAll(computerAt(ctx, level).getHardware(), all), WAIT,
                        "every part to sit in the computer", level -> "missing "
                                + missing(computerAt(ctx, level).getHardware(), all))
                .thenScreenshot(SETTLE, shot(kit, "computer-built"))
                .then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(WAIT);
    }

    /*
     * The computer worked from the outside: started with its front's power button, a disk into its hot-swap bay and
     * back out, a stick into its first port, its optical drive's tray out and in, and its side off and on again.
     */
    private static void front(final ClientTestContext ctx, final EraKit kit) {
        final CaseFrontLayout layout = frontOf(kit);
        final FrontArea power = layout.power();
        ctx.thenGive(0, new ItemStack(EraParts.smallestDiskFor(kit.era())),
                        new ItemStack(ComputingModule.USB_FLASH_DRIVE.get()))
                .thenTeleport(SETTLE, BEFORE_THE_COMPUTER, Direction.NORTH)
                .then(SETTLE, () -> ctx.selectHotbar(EMPTY_HAND))
                .then(1, () -> ctx.rightClickFront(COMPUTER, power.middleX(), power.middleY(), 0))
                .thenWaitUntilServer(level -> computerAt(ctx, level).isRunning(), WAIT,
                        "the front's power button to start the computer", level -> "running=false");
        if (!layout.bays().isEmpty()) {
            final FrontArea hotSwap = layout.bays().get(0);
            ctx.then(SETTLE, () -> ctx.selectHotbar(0))
                    .then(1, () -> ctx.rightClickFront(COMPUTER, hotSwap.middleX(), hotSwap.middleY(), 0))
                    .thenWaitUntilServer(level -> !computerAt(ctx, level).front().held(0).isEmpty(), WAIT,
                            "a disk clicked into the hot-swap bay to go in",
                            level -> "held=" + computerAt(ctx, level).front().held(0))
                    .then(SETTLE, () -> ctx.selectHotbar(EMPTY_HAND))
                    .then(1, () -> ctx.rightClickFront(COMPUTER, hotSwap.middleX(), hotSwap.middleY(), 0))
                    .thenWaitUntilServer(level -> computerAt(ctx, level).front().held(0).isEmpty(), WAIT,
                            "an empty hand on the bay to take the disk back out",
                            level -> "held=" + computerAt(ctx, level).front().held(0));
        }
        if (!layout.ports().isEmpty()) {
            final FrontArea port = layout.ports().get(0);
            ctx.then(SETTLE, () -> ctx.selectHotbar(1))
                    .then(1, () -> ctx.rightClickFront(COMPUTER, port.middleX(), port.middleY(), 0))
                    .thenWaitUntilServer(level -> !computerAt(ctx, level).front().held(CaseFront.portSlot(0))
                                    .isEmpty(), WAIT, "the stick clicked into the first port to go in",
                            level -> "port=" + computerAt(ctx, level).front().held(CaseFront.portSlot(0)));
        }
        if (kit.opticalBayDrive() != null && layout.bays().size() > 1) {
            final EjectButton eject = layout.ejectButton(1, opticalKind(kit));
            final double x = (eject.left() + eject.right()) / 2.0;
            final double y = (eject.top() + eject.bottom()) / 2.0;
            ctx.then(SETTLE, () -> ctx.selectHotbar(EMPTY_HAND))
                    .then(1, () -> ctx.rightClickFront(COMPUTER, x, y, eject.depth()))
                    .thenWaitUntilServer(level -> computerAt(ctx, level).front().tray(1).isOpen(), WAIT,
                            "the optical drive's eject button to bring its tray out", level -> "closed")
                    .thenScreenshot(SETTLE * 3, shot(kit, "tray-out"))
                    .then(SETTLE, () -> ctx.rightClickFront(COMPUTER, x, y, eject.depth()))
                    .thenWaitUntilServer(level -> !computerAt(ctx, level).front().tray(1).isOpen(), WAIT,
                            "a second press to take the tray back in", level -> "open");
        }
        // The case's left side, west of it as it faces south, comes off and goes back on with a crouched empty hand.
        ctx.thenScreenshot(SETTLE, shot(kit, "front-worked"))
                .thenTeleport(SETTLE, WEST_OF_THE_COMPUTER, Direction.EAST)
                .then(SETTLE, () -> ctx.selectHotbar(EMPTY_HAND))
                .thenSneakClick(1, COMPUTER, Direction.WEST)
                .thenWaitUntilServer(level -> computerAt(ctx, level).sidePanelOff(), WAIT,
                        "a crouched click to take the side off", level -> "side on")
                .thenScreenshot(SETTLE, shot(kit, "side-off"))
                .thenSneakClick(SETTLE, COMPUTER, Direction.WEST)
                .thenWaitUntilServer(level -> !computerAt(ctx, level).sidePanelOff(), WAIT,
                        "a second crouched click to put it back", level -> "side off");
    }

    /*
     * The rack put down with its monitor, the Storage Server's array seated through the server's own screen, and the
     * rack filled through its screen; the monitor then shows the KVM's bar, and the channel keys pick the machines.
     */
    private static void rack(final ClientTestContext ctx, final EraKit kit) {
        final Item board = kit.serverBoard();
        final boolean ownVideo = EraParts.board(board).formFactor() == FormFactor.EEB;
        final ItemStack storage = ownVideo ? EraNetwork.built(kit.storageServer(), board)
                : EraNetwork.built(kit.storageServer(), board, EraParts.cardFor(board, GpuItem.class));
        final ItemStack compute = ownVideo ? EraNetwork.built(kit.computeServer(), board)
                : EraNetwork.built(kit.computeServer(), board, EraParts.cardFor(board, GpuItem.class));
        final Item second = kit.fibre() ? kit.fibreInterface() : kit.coolingUnit();
        final Item disk = EraParts.smallestDiskFor(kit.era());
        ctx.thenGive(0, new ItemStack(kit.rack()), new ItemStack(kit.monitor()))
                .thenTeleport(SETTLE, AT_THE_RACK, Direction.NORTH)
                .then(SETTLE, () -> ctx.selectHotbar(0))
                .thenPlace(1, RACK)
                .thenWaitUntilServer(level -> level.getBlockEntity(ctx.abs(RACK)) instanceof ServerRackBlockEntity,
                        WAIT, "the rack to go down from the hand", level -> "found "
                                + level.getBlockState(ctx.abs(RACK)))
                .thenTeleport(SETTLE, AT_THE_RACK_MONITOR, Direction.NORTH)
                .then(SETTLE, () -> ctx.selectHotbar(1))
                .thenPlace(1, RACK_MONITOR)
                // The array into the storage server through the server's own screen, opened from the hand.
                .thenGive(0, storage, new ItemStack(kit.array()))
                .then(SETTLE, () -> ctx.selectHotbar(0))
                .then(2, () -> Objects.requireNonNull(ctx.mc().gameMode, "no game mode")
                        .useItem(ctx.player(), InteractionHand.MAIN_HAND))
                .thenAwaitScreen(ServerAssemblyScreen.class, WAIT)
                .then(SETTLE, () -> seatFromHotbar(ctx, ServerAssemblyScreen.class, 1, SeatKind.CARD,
                        ownVideo ? 0 : 1))
                .then(SETTLE, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(WAIT)
                .thenWaitUntilServer(level -> holds(ctx.serverPlayer().getInventory().getItem(0), kit.array()), WAIT,
                        "the array to sit in the storage server", level -> "server="
                                + ctx.serverPlayer().getInventory().getItem(0))
                .thenServer(0, level -> {
                    final ItemStack built = ctx.serverPlayer().getInventory().getItem(0).copy();
                    ctx.give(0, EraNetwork.server(kit));
                    ctx.give(1, new ItemStack(second));
                    ctx.give(2, built);
                    ctx.give(3, compute);
                    ctx.give(4, new ItemStack(kit.kvmSwitch()));
                    ctx.give(5, new ItemStack(kit.rackUps()));
                })
                .thenTeleport(SETTLE, AT_THE_RACK, Direction.NORTH)
                .then(SETTLE, () -> ctx.selectHotbar(EMPTY_HAND))
                .thenRightClick(SETTLE, RACK)
                .thenAwaitScreen(ServerRackScreen.class, WAIT);
        mount(ctx, 0, SERVER_ROW);
        mount(ctx, 1, SECOND_ROW);
        mount(ctx, 2, STORAGE_ROW);
        mount(ctx, 3, COMPUTE_ROW);
        mount(ctx, 4, KVM_ROW);
        mount(ctx, 5, UPS_ROW);
        // The disks: three into the storage server's bays, one into each other server's; and the cooling unit to hand.
        ctx.thenGive(SETTLE, new ItemStack(disk), new ItemStack(disk), new ItemStack(disk), new ItemStack(disk),
                new ItemStack(disk), new ItemStack(kit.coolingUnit()));
        final int[][] bays = {{SERVER_ROW, 0}, {STORAGE_ROW, 0}, {STORAGE_ROW, 1}, {STORAGE_ROW, 2}, {COMPUTE_ROW, 0}};
        for (int i = 0; i < bays.length; i++) {
            final int slot = i;
            final int[] bay = bays[i];
            ctx.then(2, () -> ctx.clickGui(rackHotbarX(slot), rackHotbarY()))
                    .then(2, () -> ctx.clickGui(frontX(bay[1]), slotY(bay[0])));
        }
        ctx.thenWaitUntilServer(level -> {
                    final ServerRackBlockEntity rack = rackAt(ctx, level);
                    return rack.getServers().getStackInSlot(SERVER_ROW).getItem() instanceof ServerItem
                            && rack.getServers().getStackInSlot(SECOND_ROW).is(second)
                            && rack.getServers().getStackInSlot(STORAGE_ROW).is(kit.storageServer())
                            && rack.getServers().getStackInSlot(COMPUTE_ROW).is(kit.computeServer())
                            && rack.getServers().getStackInSlot(KVM_ROW).is(kit.kvmSwitch())
                            && rack.getServers().getStackInSlot(UPS_ROW).is(kit.rackUps())
                            && rack.claimedDriveStacks(SERVER_ROW).size() == 1
                            && rack.claimedDriveStacks(STORAGE_ROW).size() == 3
                            && rack.claimedDriveStacks(COMPUTE_ROW).size() == 1;
                }, WAIT, "every server, unit and disk to sit in the rack", level -> rows(rackAt(ctx, level)))
                .thenScreenshot(SETTLE, shot(kit, "rack-filled"));
        // From the Standard the fibre takes the second row, so the cooling goes where the UPS was.
        if (kit.fibre()) {
            ctx.then(2, () -> ctx.clickGui(serverSlotX(), slotY(UPS_ROW)))
                    .then(2, () -> ctx.clickGui(rackHotbarX(6), rackHotbarY()))
                    .then(2, () -> ctx.clickGui(rackHotbarX(5), rackHotbarY()))
                    .then(2, () -> ctx.clickGui(serverSlotX(), slotY(UPS_ROW)))
                    .thenWaitUntilServer(level -> rackAt(ctx, level).getServers().getStackInSlot(UPS_ROW)
                                    .is(kit.coolingUnit()), WAIT, "the cooling unit to take the UPS's row",
                            level -> rows(rackAt(ctx, level)));
        }
        ctx.then(0, () -> ctx.key(GLFW.GLFW_KEY_ESCAPE))
                .thenAwaitNoScreen(WAIT);
        channel(ctx, kit, GLFW.GLFW_KEY_2, STORAGE_ROW);
        channel(ctx, kit, GLFW.GLFW_KEY_3, COMPUTE_ROW);
        channel(ctx, kit, GLFW.GLFW_KEY_1, SERVER_ROW);
    }

    /* The monitor opened on the KVM's bar, and the channel key puts the machine in {@code row} on the screen. */
    private static void channel(final ClientTestContext ctx, final EraKit kit, final int key, final int row) {
        ctx.thenTeleport(SETTLE, AT_THE_RACK_MONITOR, Direction.NORTH)
                .then(SETTLE, () -> ctx.selectHotbar(EMPTY_HAND))
                .thenRightClick(SETTLE, RACK_MONITOR)
                .thenAwaitScreen(KvmChannelScreen.class, WAIT)
                .thenScreenshot(SETTLE, shot(kit, "kvm-bar"))
                .then(SETTLE, () -> ctx.key(key))
                .thenWaitUntilServer(level -> rackAt(ctx, level).activeChannel() == row, WAIT,
                        "the channel key to put row " + row + " on the monitor",
                        level -> "channel=" + rackAt(ctx, level).activeChannel())
                .thenWaitUntil(() -> ctx.openScreen(KvmChannelScreen.class) == null, WAIT,
                        "the bar to give way to the machine's own screen")
                .thenScreenshot(SETTLE * 3, shot(kit, "kvm-row-" + row))
                .thenWaitUntil(EraJourney.away(ctx), WAIT, "the monitor to be left");
    }

    /* Mounts what hotbar slot {@code slot} holds in rack row {@code row}, through the rack's screen. */
    private static void mount(final ClientTestContext ctx, final int slot, final int row) {
        ctx.then(2, () -> ctx.clickGui(rackHotbarX(slot), rackHotbarY()))
                .then(2, () -> ctx.clickGui(serverSlotX(), slotY(row)));
    }

    /* Seats every part in the hotbar, in order, in the seats listed for them. */
    private static void seatAll(final ClientTestContext ctx,
                                final Class<? extends AbstractMachineAssemblyScreen<?>> screen,
                                final List<SeatKind> seats) {
        for (int i = 0; i < seats.size(); i++) {
            final int slot = i;
            final SeatKind kind = seats.get(i);
            final int index = (int) seats.subList(0, i).stream().filter(kind::equals).count();
            ctx.then(2, () -> seatFromHotbar(ctx, screen, slot, kind, index));
        }
    }

    private static void seatFromHotbar(final ClientTestContext ctx,
                                       final Class<? extends AbstractMachineAssemblyScreen<?>> screen, final int slot,
                                       final SeatKind kind, final int index) {
        EraJourney.seatFromHotbar(ctx, screen, slot, kind, index);
    }

    private static Seat seat(final ClientTestContext ctx,
                             final Class<? extends AbstractMachineAssemblyScreen<?>> screen, final SeatKind kind,
                             final int index) {
        return EraJourney.seat(ctx, screen, kind, index);
    }

    /* The menu slot drawn at a seat. */
    private static Slot slotAt(final PersonalComputerScreen screen, final Seat seat) {
        for (final Slot slot : screen.getMenu().slots) {
            if (slot.x == seat.x() && slot.y == seat.y()) {
                return slot;
            }
        }
        throw new ClientTestFailure("no slot at the " + seat.kind() + " seat");
    }

    private static int hotbarX(final int slot) {
        return EraJourney.hotbarX(slot);
    }

    private static int hotbarY(final ClientTestContext ctx,
                               final Class<? extends AbstractMachineAssemblyScreen<?>> screen) {
        return EraJourney.hotbarY(ctx, screen);
    }

    private static int serverSlotX() {
        return ServerRackLayout.serverItemX() + 8;
    }

    private static int frontX(final int column) {
        return ServerRackLayout.frontItemX(column) + 8;
    }

    private static int slotY(final int row) {
        return ServerRackLayout.itemY(row) + 8;
    }

    private static int rackHotbarX(final int slot) {
        return ServerRackLayout.INV_X + slot * 18 + 8;
    }

    private static int rackHotbarY() {
        return ServerRackLayout.HOTBAR_Y + 8;
    }

    /*
     * The blocks, cables and cable parts of the era's tab, or of no era's tab for null, in registry order: what the
     * player can put down in the world from the hand.
     */
    private static List<Item> placeable(@Nullable final EraKit kit) {
        final List<TagKey<Item>> eras = new ArrayList<>();
        for (final EraKit each : EraKit.values()) {
            eras.add(eraTag(each));
        }
        final List<Item> out = new ArrayList<>();
        for (final Item item : BuiltInRegistries.ITEM) {
            if (!JsComputers.MODID.equals(BuiltInRegistries.ITEM.getKey(item).getNamespace())
                    || !(item instanceof BlockItem || item instanceof CableItem || item instanceof CablePartItem)) {
                continue;
            }
            final ItemStack stack = new ItemStack(item);
            final boolean inShelf = SHELVES.stream().anyMatch(stack::is);
            if (kit != null ? stack.is(eraTag(kit)) && inShelf
                    : inShelf && eras.stream().noneMatch(stack::is)) {
                out.add(item);
            }
        }
        return out;
    }

    private static List<TagKey<Item>> shelves() {
        final List<TagKey<Item>> out = new ArrayList<>();
        for (final ComputingContent.Shelf shelf : ComputingContent.Shelf.values()) {
            if (shelf != ComputingContent.Shelf.COMPONENTS && shelf != ComputingContent.Shelf.PROGRAMS) {
                out.add(TagKey.create(Registries.ITEM,
                        ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, shelf.tag())));
            }
        }
        return List.copyOf(out);
    }

    private static TagKey<Item> eraTag(final EraKit kit) {
        return TagKey.create(Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, ComputingContent.eraTag(kit.era())));
    }

    /* The cable a part goes on: a storage bus on the era's access line, a crafting part on the crafting cable. */
    private static CableEntry cableFor(final CablePartItem part, @Nullable final EraKit kit) {
        final boolean storage = ComputingParts.isImport(part.partType()) || ComputingParts.isExport(part.partType())
                || ComputingParts.isExternal(part.partType());
        return storage && kit != null ? kit.access() : ComputingModule.CRAFTING_CABLE;
    }

    private static boolean placed(final ClientTestContext ctx, final ServerLevel level, final BlockPos at,
                                  final Item item) {
        if (item instanceof CableItem cable) {
            return cableAt(ctx, level, at) != null && cableAt(ctx, level, at).holds(cable.type());
        }
        final BlockState state = level.getBlockState(ctx.abs(at));
        return item instanceof BlockItem block && state.is(block.getBlock());
    }

    @Nullable
    private static CableBlockEntity cableAt(final ClientTestContext ctx, final ServerLevel level, final BlockPos at) {
        return level.getBlockEntity(ctx.abs(at)) instanceof CableBlockEntity cable ? cable : null;
    }

    private static CaseFrontLayout frontOf(final EraKit kit) {
        return ((AbstractSmallComputerBlock<?>) kit.computer()).frontLayout();
    }

    private static BayDriveSpec.Kind opticalKind(final EraKit kit) {
        return switch (kit) {
            case VINTAGE, LEGACY -> BayDriveSpec.Kind.CD;
            case TRANSITION, STANDARD -> BayDriveSpec.Kind.DVD;
            case ADVANCED -> BayDriveSpec.Kind.BLU_RAY;
        };
    }

    private static ItemStack[] stacks(final List<Item> items) {
        return items.stream().map(ItemStack::new).toArray(ItemStack[]::new);
    }

    private static boolean holds(final ItemStack server, final Item part) {
        for (final ItemStack held : ServerItem.hardware(server).nonEmptyItems()) {
            if (held.is(part)) {
                return true;
            }
        }
        return false;
    }

    private static boolean holdsAll(final IItemHandler slots, final List<Item> parts) {
        return missing(slots, parts).isEmpty();
    }

    /* The parts no slot holds, each counted as often as it is listed. */
    private static List<Item> missing(final IItemHandler slots, final List<Item> parts) {
        final List<Item> left = new ArrayList<>(parts);
        for (int slot = 0; slot < slots.getSlots(); slot++) {
            left.remove(slots.getStackInSlot(slot).getItem());
        }
        return left;
    }

    private static String rows(final ServerRackBlockEntity rack) {
        final StringBuilder out = new StringBuilder();
        for (int row = 0; row < ServerRackBlockEntity.CAPACITY_U; row++) {
            out.append(row).append('=').append(rack.getServers().getStackInSlot(row)).append(" disks=")
                    .append(rack.claimedDriveStacks(row).size()).append("; ");
        }
        return out.toString();
    }

    private static MainframeBlockEntity mainframeAt(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(MAINFRAME, MainframeBlockEntity.class);
    }

    private static AbstractSmallComputerBlockEntity computerAt(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(COMPUTER, AbstractSmallComputerBlockEntity.class);
    }

    private static ServerRackBlockEntity rackAt(final ClientTestContext ctx, final ServerLevel level) {
        return TestWorldBuilder.at(level, ctx.origin()).blockEntity(RACK, ServerRackBlockEntity.class);
    }

    private static String shot(final EraKit kit, final String what) {
        return kit.name().toLowerCase(Locale.ROOT) + "-" + what;
    }
}
