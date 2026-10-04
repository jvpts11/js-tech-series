/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.crafting.CraftingLog;
import dev.jstech.computers.crafting.PatternWorkbench;
import dev.jstech.computers.operation.payload.CraftManagerStatePayload;
import dev.jstech.computers.operation.payload.CraftingInterfaceEditPayload;
import dev.jstech.computers.operation.payload.CraftingInterfaceStatePayload;
import dev.jstech.computers.operation.payload.CreateAutomationJobPayload;
import dev.jstech.computers.operation.payload.InterfaceView;
import dev.jstech.computers.operation.payload.PatternStudioEditPayload;
import dev.jstech.computers.operation.payload.PatternStudioStatePayload;
import dev.jstech.computers.operation.payload.RequestHelpPayload;
import dev.jstech.computers.operation.payload.TerminalSelectPayload;
import dev.jstech.computers.operation.payload.UninstallProgramPayload;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.Text;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Every autocraft message survives a StreamCodec encode and decode with the buffer fully consumed: the Crafting
 * Manager's state with its places and interfaces, a Crafting Interface's window state and edits, and the Pattern
 * Studio's state and edits.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class MachinePayloadGameTests {

    private static final String ARENA = "empty";

    private MachinePayloadGameTests() {
    }

    @GameTest(template = ARENA)
    public static void craftManagerState_streamCodecRoundTrip(final GameTestHelper helper) {
        final List<CraftManagerStatePayload.WireRomEntry> rom = List.of(
                new CraftManagerStatePayload.WireRomEntry(0, Text.literal("Iron Block"), true,
                        CraftManagerStatePayload.BENCH),
                new CraftManagerStatePayload.WireRomEntry(1, Text.literal("Gold Block"), false,
                        CraftManagerStatePayload.BENCH));
        final List<CraftManagerStatePayload.WireRomEntry> held = List.of(
                new CraftManagerStatePayload.WireRomEntry(256, Text.literal("Stone"), false,
                        CraftManagerStatePayload.PROCESSING),
                new CraftManagerStatePayload.WireRomEntry(257, Text.literal("Iron line"), true,
                        CraftManagerStatePayload.PIPELINE));
        final List<CraftManagerStatePayload.WirePlace> places = List.of(
                new CraftManagerStatePayload.WirePlace(true, Text.literal("Crafting Card, slot 1"), 2, 5, 5, rom),
                new CraftManagerStatePayload.WirePlace(false, Text.literal("Kiln A"), 2, 9, 0, held));
        final List<CraftManagerStatePayload.WireInterface> interfaces = List.of(
                new CraftManagerStatePayload.WireInterface(1, Text.literal("Kiln A"), Text.literal("Test kiln"), 2,
                        9, false, CraftManagerStatePayload.RUNNING, Text.literal("Stone x4"), 0),
                new CraftManagerStatePayload.WireInterface(2, Text.literal("Press"), Text.EMPTY, 0, 3, true,
                        CraftManagerStatePayload.NO_MACHINE, Text.EMPTY, 2));
        final CraftManagerStatePayload payload = new CraftManagerStatePayload("media:42",
                Text.literal("Floppy (A:)"), List.of("alpha.craft", "beta.craft"), places, true,
                Text.literal("Loaded 2"), true, interfaces, 5, 1);
        assertRoundTrip(helper, CraftManagerStatePayload.STREAM_CODEC, payload);
        helper.succeed();
    }

    /**
     * Text a client sends is held to what each message carries: made with far more, a message is cut to its caps,
     * still writes, and reads back as what it was cut to. These carried whatever length a client chose.
     */
    @GameTest(template = ARENA)
    public static void clientText_isHeldToWhatEachMessageCarries(final GameTestHelper helper) {
        final String flood = "x".repeat(20_000);
        final BlockPos at = new BlockPos(1, 2, 3);

        final CreateAutomationJobPayload job = new CreateAutomationJobPayload(at, at,
                CreateAutomationJobPayload.TYPE_PERIODIC_MOVE, flood, flood, 64, flood, flood, flood);
        helper.assertTrue(job.name().length() == 64 && job.item().length() == 128 && job.interval().length() == 32,
                "a job's fields are cut to what the form holds");
        assertRoundTrip(helper, CreateAutomationJobPayload.STREAM_CODEC, job);

        final TerminalSelectPayload select = new TerminalSelectPayload(at, at, StorageKey.of(Items.DIRT), 1L,
                List.of(flood, flood), TerminalSelectPayload.DEST_SERVER, flood);
        helper.assertTrue(select.serverKeys().getFirst().length() == 64 && select.destServer().length() == 64,
                "a terminal's server names are cut to the desktop's own limit");
        assertRoundTrip(helper, TerminalSelectPayload.STREAM_CODEC, select);

        assertRoundTrip(helper, UninstallProgramPayload.STREAM_CODEC, new UninstallProgramPayload(at, flood));
        assertRoundTrip(helper, RequestHelpPayload.STREAM_CODEC, new RequestHelpPayload(at, flood));
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void craftManagerState_emptyListsRoundTrip(final GameTestHelper helper) {
        final CraftManagerStatePayload payload = new CraftManagerStatePayload("", Text.EMPTY, List.of(), List.of(),
                false, Text.EMPTY, false, List.of(), 0, 0);
        assertRoundTrip(helper, CraftManagerStatePayload.STREAM_CODEC, payload);
        helper.succeed();
    }

    /** The most places, each as full as a place is sent, and the most interfaces all go through at once. */
    @GameTest(template = ARENA)
    public static void craftManagerState_mostPlacesAndInterfacesRoundTrip(final GameTestHelper helper) {
        final List<CraftManagerStatePayload.WirePlace> places = new ArrayList<>();
        for (int p = 0; p < CraftManagerStatePayload.MAX_PLACES; p++) {
            final List<CraftManagerStatePayload.WireRomEntry> entries = new ArrayList<>();
            for (int e = 0; e < CraftManagerStatePayload.MAX_ENTRIES; e++) {
                entries.add(new CraftManagerStatePayload.WireRomEntry(p * 256 + e, Text.literal("r" + e), e % 2 == 0,
                        (byte) (e % 3)));
            }
            places.add(new CraftManagerStatePayload.WirePlace(p % 2 == 0, Text.literal("place " + p),
                    CraftManagerStatePayload.MAX_ENTRIES, 16, p % 5, entries));
        }
        final List<CraftManagerStatePayload.WireInterface> interfaces = new ArrayList<>();
        for (int i = 0; i < CraftManagerStatePayload.MAX_INTERFACES; i++) {
            interfaces.add(new CraftManagerStatePayload.WireInterface(i, Text.literal("i" + i), Text.literal("m" + i),
                    i % 9, 9, i % 2 == 0, (byte) (i % 5), Text.literal("d" + i), i % 4));
        }
        final CraftManagerStatePayload payload = new CraftManagerStatePayload("media:1", Text.literal("Disc"),
                List.of(), places, true, Text.EMPTY, false, interfaces, 36, 3);
        assertRoundTrip(helper, CraftManagerStatePayload.STREAM_CODEC, payload);
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void craftingInterfaceEdit_streamCodecRoundTrip(final GameTestHelper helper) {
        assertRoundTrip(helper, CraftingInterfaceEditPayload.STREAM_CODEC,
                new CraftingInterfaceEditPayload(CraftingInterfaceEditPayload.NAME, 0, 0, 0, "Kiln A"));
        assertRoundTrip(helper, CraftingInterfaceEditPayload.STREAM_CODEC,
                new CraftingInterfaceEditPayload(CraftingInterfaceEditPayload.ROUTE, 3, 1, -1, ""));
        assertRoundTrip(helper, CraftingInterfaceEditPayload.STREAM_CODEC,
                new CraftingInterfaceEditPayload(CraftingInterfaceEditPayload.JOBS, 0, 0, 12, ""));
        helper.succeed();
    }

    /** Everything an interface's window shows goes through, its patterns' routes, warnings, log and marks too. */
    @GameTest(template = ARENA)
    public static void craftingInterfaceState_streamCodecRoundTrip(final GameTestHelper helper) {
        final InterfaceView.PatternView pattern = new InterfaceView.PatternView(ItemStack.EMPTY,
                Text.literal("Coarse dirt"), List.of(
                        new InterfaceView.InputView(StorageKey.of(Items.DIRT), 1L, 0, InterfaceView.FILTER),
                        new InterfaceView.InputView(StorageKey.of(Items.GRAVEL), 1L, -1, InterfaceView.NO_ROUTER)),
                false);
        final InterfaceView view = new InterfaceView("Mixer", "a1b2c3", HardwareEra.TRANSITION, true, 8,
                List.of(pattern), List.of(Text.literal("West router"), Text.literal("North router")),
                InterfaceView.CABLE, true, Text.literal("Test mixer through 2 routers"), Text.literal("Bus A"), false,
                3, List.of(Text.literal("no router takes the gravel")), Text.literal("idle"), InterfaceView.DIM,
                List.of(new CraftingLog.Entry(120L, StorageKey.of(Items.COARSE_DIRT).id(), 4L, 4L,
                        CraftingLog.COMPLETED, "")),
                Map.of("MODE", "Mixer line"));
        assertRoundTrip(helper, CraftingInterfaceStatePayload.STREAM_CODEC, new CraftingInterfaceStatePayload(7, view));
        final InterfaceView.PatternView withIcon = new InterfaceView.PatternView(new ItemStack(Items.STONE),
                Text.literal("Stone"), List.of(), true);
        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        final InterfaceView iconView = new InterfaceView("", "", HardwareEra.STANDARD, false, 9, List.of(withIcon),
                List.of(), InterfaceView.DIRECT, false, Text.EMPTY, Text.EMPTY, false, 0, List.of(), Text.EMPTY,
                InterfaceView.GOOD, List.of(), Map.of());
        InterfaceView.write(buf, iconView);
        final InterfaceView decoded = InterfaceView.read(buf);
        helper.assertTrue(ItemStack.matches(decoded.patterns().get(0).icon(), withIcon.icon()),
                "a pattern's icon survives the wire");
        helper.assertTrue(buf.readableBytes() == 0, "the buffer was fully consumed");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void patternStudioEdit_streamCodecRoundTrip(final GameTestHelper helper) {
        assertRoundTrip(helper, PatternStudioEditPayload.STREAM_CODEC,
                PatternStudioEditPayload.number(new BlockPos(1, 2, 3), new BlockPos(2, 2, 3),
                        PatternStudioEditPayload.PROC_SET_CHANCE, 4, 75));
        assertRoundTrip(helper, PatternStudioEditPayload.STREAM_CODEC,
                PatternStudioEditPayload.text(new BlockPos(-5, 60, -9), new BlockPos(-4, 60, -9),
                        PatternStudioEditPayload.PROC_SET_NAME, 0, "Kiln stone", "made in the test kiln"));
        // An item rides the wire by value; compare the fields around it (ItemStack has no value equality).
        final PatternStudioEditPayload withItem = PatternStudioEditPayload.item(new BlockPos(0, 1, 0),
                new BlockPos(1, 1, 0), PatternStudioEditPayload.BENCH_SET_CELL, 8, new ItemStack(Items.OAK_LOG, 3));
        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        PatternStudioEditPayload.STREAM_CODEC.encode(buf, withItem);
        final PatternStudioEditPayload decoded = PatternStudioEditPayload.STREAM_CODEC.decode(buf);
        helper.assertTrue(decoded.index() == 8 && decoded.action() == PatternStudioEditPayload.BENCH_SET_CELL
                && ItemStack.matches(decoded.item(), withItem.item()), "the item edit survives the wire");
        helper.assertTrue(buf.readableBytes() == 0, "the buffer was fully consumed");
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void patternStudioState_streamCodecRoundTrip(final GameTestHelper helper) {
        /*
         * The state is hand-written on the wire (far more than six fields): a full, busy state must go
         * through and come back field for field.
         */
        final List<PatternStudioStatePayload.BenchCell> bench = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            bench.add(new PatternStudioStatePayload.BenchCell(
                    i % 2 == 0 ? new ItemStack(Items.OAK_PLANKS) : ItemStack.EMPTY,
                    i == 0 ? "minecraft:planks" : "",
                    i == 0 ? new ItemStack(Items.BIRCH_PLANKS) : ItemStack.EMPTY, 12L * i));
        }
        final PatternWorkbench.DataCell cell = new PatternWorkbench.DataCell(StorageKey.of(Items.RAW_IRON), 2, true);
        final PatternStudioStatePayload state = new PatternStudioStatePayload(bench,
                new ItemStack(Items.CHEST), "Chest of any planks", "note", "chest.craft",
                List.of(new PatternStudioStatePayload.ProcCell(3, cell, 100, 40L)),
                List.of(new PatternStudioStatePayload.ProcCell(0, cell, 50, 0L)),
                600, "", "", "",
                List.of(new PatternStudioStatePayload.Stage(Text.literal("Iron Ingot"), false,
                        new ItemStack(Items.IRON_INGOT))),
                "pipe", "", "pipe.craft",
                List.of(new PatternStudioStatePayload.Drive("media:12345", Text.literal("DVD-RW"), true,
                                List.of("a.craft", "b.craft")),
                        new PatternStudioStatePayload.Drive("disk", Text.literal("System disk (crafts)"), true,
                                List.of())),
                new PatternStudioStatePayload.Encoder(true, Text.literal("Standard"), Text.literal("DVD-RW"),
                        Text.literal("Writing a.craft"), 42, 2, true, false),
                true, true, false, true, false, Text.literal("Sent"), 1);
        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        PatternStudioStatePayload.STREAM_CODEC.encode(buf, state);
        final PatternStudioStatePayload decoded = PatternStudioStatePayload.STREAM_CODEC.decode(buf);
        helper.assertTrue(buf.readableBytes() == 0, "the buffer was fully consumed");
        helper.assertTrue(decoded.bench().size() == 9 && "minecraft:planks".equals(decoded.bench().get(0).tag())
                && decoded.bench().get(0).resolved().is(Items.BIRCH_PLANKS)
                && decoded.bench().get(8).stock() == 96L, "the bench cells survive");
        helper.assertTrue(decoded.preview().is(Items.CHEST) && "Chest of any planks".equals(decoded.benchName())
                && "chest.craft".equals(decoded.benchOpened()), "the bench header survives");
        helper.assertTrue(decoded.inputs().size() == 1 && decoded.inputs().get(0).index() == 3
                && decoded.inputs().get(0).cell().estimated() && decoded.outputs().get(0).chance() == 50,
                "the machine cells survive");
        helper.assertTrue(decoded.timeout() == 600, "the timeout survives");
        helper.assertTrue(decoded.stages().size() == 1 && !decoded.stages().get(0).bench()
                && "Iron Ingot".equals(decoded.stages().get(0).label().english())
                && "pipe.craft".equals(decoded.pipeOpened()), "the pipeline survives");
        helper.assertTrue(decoded.drives().size() == 2 && decoded.drives().get(0).files().size() == 2
                && "DVD-RW".equals(decoded.drives().get(0).label().english()), "the drives survive");
        helper.assertTrue(decoded.encoder().linked() && decoded.encoder().progress() == 42
                && decoded.encoder().queued() == 2 && decoded.encoder().busy()
                && "Standard".equals(decoded.encoder().era().english())
                && "Writing a.craft".equals(decoded.encoder().status().english()), "the encoder survives");
        helper.assertTrue(decoded.craftingComputer() && decoded.hasCard() && !decoded.romHasBench()
                && decoded.romHasProc() && !decoded.romHasPipe() && "Sent".equals(decoded.status().english())
                && decoded.tabHint() == 1, "the flags survive");
        helper.succeed();
    }

    private static <T> void assertRoundTrip(final GameTestHelper helper,
                                            final StreamCodec<RegistryFriendlyByteBuf, T> codec, final T value) {
        final RegistryAccess registries = helper.getLevel().registryAccess();
        final RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries);
        codec.encode(buf, value);
        final T decoded = codec.decode(buf);
        helper.assertTrue(decoded.equals(value), "round-trip changed the value: " + value + " -> " + decoded);
        helper.assertTrue(buf.readableBytes() == 0, "the buffer was not fully consumed (" + buf.readableBytes()
                + " bytes left)");
    }
}
