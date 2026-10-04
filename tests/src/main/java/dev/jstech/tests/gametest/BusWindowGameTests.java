/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.block.part.AbstractBusPart;
import dev.jstech.computers.block.part.BusEdits;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.computers.block.part.ExportBusPart;
import dev.jstech.computers.block.part.ImportBusPart;
import dev.jstech.computers.bus.BusCondition;
import dev.jstech.computers.bus.BusSettings;
import dev.jstech.computers.operation.payload.BusEditPayload;
import dev.jstech.computers.operation.payload.BusStatePayload;
import dev.jstech.computers.operation.payload.CraftingView;
import dev.jstech.computers.storage.ExternalDataPort;
import dev.jstech.computers.storage.FilteredDataPort;
import dev.jstech.computers.storage.IDataPort;
import dev.jstech.computers.storage.StorageKey;
import dev.jstech.core.text.Text;
import dev.jstech.core.tier.HardwareEra;
import dev.jstech.tests.JsTests;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.List;

/**
 * A bus's window: what is changed in it reaches the bus, only where the bus's era can be set to it, by hand, which
 * takes back what a program set; what the window is sent arrives as it was sent; and a crafting bus's face carries
 * every item its filter lists.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class BusWindowGameTests {

    private static final String ARENA = "empty";

    private BusWindowGameTests() {
    }

    /** The Legacy window's toggles and steppers set the bus. */
    @GameTest(template = ARENA)
    public static void edits_setWhatTheLegacyWindowShows(final GameTestHelper helper) {
        final ImportBusPart bus = ComputingParts.LEGACY_IMPORT.get().create();
        apply(bus, BusEditPayload.EXCLUDE, 0, 1L);
        apply(bus, BusEditPayload.KEEP, 0, 16L);
        apply(bus, BusEditPayload.MAX, 0, 64L);
        apply(bus, BusEditPayload.MODE, 0, 1L);
        apply(bus, BusEditPayload.POWER, 0, 0L);
        helper.assertTrue(bus.exclude(), "all but these");
        helper.assertTrue(bus.keep() == 16 && bus.max() == 64, "keep 16, max 64; got " + bus.keep() + "/" + bus.max());
        helper.assertTrue(bus.mode() == AbstractBusPart.MODE_REDSTONE, "on demand");
        helper.assertFalse(bus.powered(), "and off");
        apply(bus, BusEditPayload.KEEP, 0, -100L);
        helper.assertTrue(bus.keep() == 0, "a keep never goes under nothing; got " + bus.keep());
        helper.succeed();
    }

    /** A Vintage bus has nothing to keep and no filter past its first slot, as its window shows. */
    @GameTest(template = ARENA)
    public static void edits_refuseWhatTheEraCannotBeSetTo(final GameTestHelper helper) {
        final ExportBusPart bus = ComputingParts.VINTAGE_EXPORT.get().create();
        helper.assertFalse(BusEdits.apply(bus, BusEditPayload.of(BusEditPayload.KEEP, 0, 16L), ItemStack.EMPTY),
                "no keep on the Vintage bus");
        helper.assertFalse(BusEdits.apply(bus, BusEditPayload.of(BusEditPayload.FILTER_SLOT, 1, 0L),
                new ItemStack(Items.DIRT)), "nor a second filter slot");
        helper.assertTrue(BusEdits.apply(bus, BusEditPayload.of(BusEditPayload.FILTER_SLOT, 0, 0L),
                new ItemStack(Items.DIRT)), "but the one kind it sends");
        helper.assertTrue(bus.filterItem() == Items.DIRT, "which is the dirt");
        helper.assertFalse(BusEdits.apply(ComputingParts.IMPORT.get().create(), BusEditPayload.of(
                BusEditPayload.ITEM_KEEP, 0, 4L), ItemStack.EMPTY), "and no keep per item on the Standard");
        helper.succeed();
    }

    /** A hand in the window takes back the setting a program set: its mark goes. */
    @GameTest(template = ARENA)
    public static void edits_takeBackWhatAProgramSet(final GameTestHelper helper) {
        final ImportBusPart bus = ComputingParts.IMPORT.get().create();
        bus.setPriority(5, "Night shift");
        helper.assertValueEqual(bus.setBy(BusSettings.PRIORITY), "Night shift", "the program's mark");
        apply(bus, BusEditPayload.PRIORITY, 0, 1L);
        helper.assertTrue(bus.priority() == 6, "the priority went up by one; got " + bus.priority());
        helper.assertValueEqual(bus.setBy(BusSettings.PRIORITY), "", "and it is the hand's now");
        helper.succeed();
    }

    /** The conditions and the tags are written in the window, and taken off there. */
    @GameTest(template = ARENA)
    public static void edits_addAndTakeOffConditionsAndTags(final GameTestHelper helper) {
        final ImportBusPart bus = ComputingParts.ADVANCED_IMPORT.get().create();
        apply(bus, new BusEditPayload(BusEditPayload.ADD_TAG, 0, 0L, "#c:ores"));
        apply(bus, new BusEditPayload(BusEditPayload.ADD_CONDITION, BusCondition.Kind.STOCK.id(), 10L,
                "cobblestone"));
        apply(bus, BusEditPayload.ADD_CONDITION, BusCondition.Kind.HOURS.id(), 18L * 24L + 6L);
        apply(bus, new BusEditPayload(BusEditPayload.ADD_CONDITION, BusCondition.Kind.AFTER.id(), 0L, "Coal in"));
        helper.assertTrue(bus.tags().equals(List.of(ResourceLocation.parse("c:ores"))), "the tag; got " + bus.tags());
        helper.assertTrue(bus.conditions().equals(List.of(BusCondition.stock("minecraft:cobblestone", 10),
                BusCondition.hours(18, 6), BusCondition.after("Coal in"))), "the three; got " + bus.conditions());
        helper.assertFalse(BusEdits.apply(bus, new BusEditPayload(BusEditPayload.ADD_CONDITION,
                BusCondition.Kind.STOCK.id(), 10L, "no_such_item_at_all"), ItemStack.EMPTY),
                "an item that is not there is no condition");
        apply(bus, BusEditPayload.REMOVE_CONDITION, 1, 0L);
        apply(bus, BusEditPayload.REMOVE_TAG, 0, 0L);
        helper.assertTrue(bus.conditions().size() == 2 && bus.tags().isEmpty(), "taken off");
        helper.succeed();
    }

    /** The carried item goes in the cell clicked, and "+ item" puts it in the first empty one. */
    @GameTest(template = ARENA)
    public static void edits_listTheCarriedItem(final GameTestHelper helper) {
        final ImportBusPart bus = ComputingParts.TRANSITION_IMPORT.get().create();
        BusEdits.apply(bus, BusEditPayload.of(BusEditPayload.FILTER_SLOT, 2, 0L), new ItemStack(Items.COAL, 5));
        BusEdits.apply(bus, BusEditPayload.of(BusEditPayload.ADD_ITEM, 0, 0L), new ItemStack(Items.IRON_ORE));
        apply(bus, BusEditPayload.ITEM_KEEP, 2, 16L);
        final List<String> listed = bus.settings().filter();
        helper.assertValueEqual(listed.get(0), "item|minecraft:iron_ore", "the first empty cell took the ore");
        helper.assertValueEqual(listed.get(2), "item|minecraft:coal", "the clicked cell the coal");
        helper.assertTrue(bus.getFilterHandler().getStackInSlot(2).getCount() == 1, "one, not the five carried");
        helper.assertTrue(bus.itemKeep(2) == 16, "and the coal keeps 16");
        BusEdits.apply(bus, BusEditPayload.of(BusEditPayload.FILTER_SLOT, 2, 0L), ItemStack.EMPTY);
        helper.assertTrue(bus.settings().filter().get(2).isEmpty(), "an empty hand clears the cell");
        helper.succeed();
    }

    /** What the window is sent comes off the wire as it went on. */
    @GameTest(template = ARENA)
    public static void state_comesBackFromTheWireAsItWent(final GameTestHelper helper) {
        final ImportBusPart bus = ComputingParts.ADVANCED_IMPORT.get().create();
        bus.setName("Ore in");
        bus.setFilterSlot(0, new ItemStack(Items.IRON_ORE), "");
        bus.addTag(ResourceLocation.parse("c:ores"), "Stock keeper");
        bus.addCondition(BusCondition.hours(18, 6), "Night shift");
        bus.activity().moved(100L, "item|minecraft:iron_ore", 23L, false);
        final CraftingView crafting = new CraftingView(List.of(new CraftingView.Line(CraftingView.GOOD,
                Text.literal("TIED TO"), Text.literal("Kiln A"), Text.EMPTY)), List.of(), List.of());
        final BusStatePayload sent = new BusStatePayload(7, bus.settings(), bus.filterStacks(), true, 64L, 256L,
                HardwareEra.ADVANCED, bus.activity().entries(), 27, 14, crafting);
        final RegistryFriendlyByteBuf wire = new RegistryFriendlyByteBuf(Unpooled.buffer(),
                helper.getLevel().registryAccess());
        try {
            BusStatePayload.STREAM_CODEC.encode(wire, sent);
            final BusStatePayload got = BusStatePayload.STREAM_CODEC.decode(wire);
            helper.assertTrue(got.settings().equals(sent.settings()), "the settings; got " + got.settings());
            helper.assertTrue(got.activity().equals(sent.activity()), "the log");
            helper.assertTrue(got.filter().get(0).is(Items.IRON_ORE), "the filter's ore, to draw");
            helper.assertTrue(got.speed() == 64L && got.carries() == 256L && got.linked()
                    && got.skin() == HardwareEra.ADVANCED && got.containerId() == 7 && got.places() == 27
                    && got.placesUsed() == 14, "and the rest");
            helper.assertTrue(got.crafting().equals(sent.crafting()), "the crafting lines; got " + got.crafting());
            helper.assertTrue(wire.readableBytes() == 0, "with nothing left over on the wire");
        } finally {
            wire.release();
        }
        helper.succeed();
    }

    /** A face filtered by several items carries each of them and nothing else. */
    @GameTest(template = ARENA)
    public static void filteredPort_carriesEveryItemItLists(final GameTestHelper helper) {
        final ItemStackHandler face = new ItemStackHandler(3);
        final IDataPort port = new FilteredDataPort(new ExternalDataPort(face, null),
                List.of(StorageKey.of(Items.COAL), StorageKey.of(Items.IRON_ORE)));
        helper.assertTrue(port.insert(StorageKey.of(Items.COAL), 4, false) == 4, "the coal goes in");
        helper.assertTrue(port.insert(StorageKey.of(Items.IRON_ORE), 4, false) == 4, "the ore too");
        helper.assertTrue(port.insert(StorageKey.of(Items.DIRT), 4, false) == 0, "the dirt does not");
        helper.succeed();
    }

    private static void apply(final AbstractBusPart bus, final int op, final int slot, final long value) {
        apply(bus, BusEditPayload.of(op, slot, value));
    }

    private static void apply(final AbstractBusPart bus, final BusEditPayload edit) {
        BusEdits.apply(bus, edit, ItemStack.EMPTY);
    }
}
