/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.input.KeyAction;
import dev.jstech.core.item.ItemActions;
import dev.jstech.core.item.ItemMode;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.module.CoreModuleEffects;
import dev.jstech.core.module.ModuleDef;
import dev.jstech.core.module.ModuleHostDef;
import dev.jstech.core.module.Modules;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tool.BuildMode;
import dev.jstech.core.tool.ChargingItem;
import dev.jstech.core.tool.MiningShape;
import dev.jstech.core.tool.ToolHeat;
import dev.jstech.core.tool.ToolPower;
import dev.jstech.core.tool.ToolSpec;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;

/**
 * The tools of the test mod, declared the way a mod declares its own, to prove the Core's kit of tools: a drill on
 * energy with a mode for a three by three area and one for a vein; a pick that heats; a chainsaw on the test fuel; a
 * shovel on the test gas; a railgun held to charge; a building wand with a line, a plane, a copy and a paste; a
 * multitool whose modules give it a vein, silk touch and reach; a hammer that lasts by tin's atomic number; and a
 * magnet a key turns on wherever it is carried. They wear the game's own pictures, so the test mod draws nothing; the
 * numbers are the tests', small enough that a few blocks show what a tool spends.
 */
@TextHolder
public final class TestTools {

    public static final TextKey SINGLE_NAME = TextKey.of("jstests.mode.single", "Single");
    public static final TextKey AREA_NAME = TextKey.of("jstests.mode.area", "Area 3x3");
    public static final TextKey VEIN_NAME = TextKey.of("jstests.mode.vein", "Vein");
    public static final TextKey LINE_NAME = TextKey.of("jstests.mode.line", "Line");
    public static final TextKey PLANE_NAME = TextKey.of("jstests.mode.plane", "Plane");
    public static final TextKey COPY_NAME = TextKey.of("jstests.mode.copy", "Copy");
    public static final TextKey PASTE_NAME = TextKey.of("jstests.mode.paste", "Paste");
    public static final TextKey MAGNET_KEY_NAME = TextKey.of("key.jstests.test_magnet", "Test Magnet");

    public static final ItemMode SINGLE = ItemMode.of("single", SINGLE_NAME);
    public static final ItemMode AREA = ItemMode.of("area", AREA_NAME);
    public static final ItemMode VEIN = ItemMode.of("vein", VEIN_NAME);
    public static final ItemMode LINE = ItemMode.of("line", LINE_NAME);
    public static final ItemMode PLANE = ItemMode.of("plane", PLANE_NAME);
    public static final ItemMode COPY = ItemMode.of("copy", COPY_NAME);
    public static final ItemMode PASTE = ItemMode.of("paste", PASTE_NAME);

    /** What the drill holds, and spends a block, in FE; the most blocks of a vein it takes. */
    public static final int DRILL_ENERGY = 100_000;
    public static final int DRILL_PER_BLOCK = 100;
    public static final int DRILL_VEIN = 16;
    /** The pick's heat: it overheats at 30, a block adds 10, and it cools 2 a second. */
    public static final ToolHeat PICK_HEAT = new ToolHeat(30, 10, 2);
    /** What the chainsaw holds of the test fuel, in millibuckets, and burns a block, in joules (2 mB of it). */
    public static final int CHAINSAW_TANK = 4_000;
    public static final double CHAINSAW_JOULES = 200_000;
    /** The shovel's tank, in millibuckets at ten times the open air's pressure, and what it spends a block. */
    public static final long SHOVEL_TANK = 1_000;
    public static final long SHOVEL_PRESSURE = 10;
    public static final long SHOVEL_PER_BLOCK = 50;
    /** What the railgun holds and spends a shot, in FE; how long it charges, and waits after a shot, in ticks. */
    public static final int RAILGUN_ENERGY = 10_000;
    public static final int RAILGUN_PER_SHOT = 1_000;
    public static final int RAILGUN_CHARGE = 20;
    public static final int RAILGUN_WAIT = 40;
    /** The most blocks the wand's line and plane place. */
    public static final int WAND_LINE = 8;
    public static final int WAND_PLANE = 16;
    /** What the multitool holds, in FE; what its vein module takes and what each module spends a use. */
    public static final int MULTITOOL_ENERGY = 100_000;
    public static final int MODULE_VEIN = 8;
    public static final int MODULE_ENERGY = 10;
    public static final double REACH_BOOST = 2.0;
    /** How many uses the tin hammer lasts for each of tin's protons. */
    public static final int USES_PER_PROTON = 5;

    public static final ItemEntry<PickaxeItem> DRILL = TestSounds.CONTENT.item("test_drill",
                    properties -> new PickaxeItem(Tiers.DIAMOND, properties))
            .named("Test Drill").look(IItemLook.HANDMADE).tab(TestItems.ITEMS)
            .modes(SINGLE, AREA, VEIN).holdsEnergy(DRILL_ENERGY, 10_000, 0)
            .tool(ToolSpec.builder().mines(AREA, MiningShape.area(3, 3, 1)).mines(VEIN, MiningShape.vein(DRILL_VEIN))
                    .runsOn(ToolPower.energy(DRILL_PER_BLOCK)).build())
            .register();
    public static final ItemEntry<PickaxeItem> HOT_PICK = TestSounds.CONTENT.item("test_hot_pick",
                    properties -> new PickaxeItem(Tiers.IRON, properties))
            .named("Test Hot Pick").look(IItemLook.HANDMADE).tab(TestItems.ITEMS)
            .tool(ToolSpec.builder().heats(PICK_HEAT).build()).register();
    public static final ItemEntry<AxeItem> CHAINSAW = TestSounds.CONTENT.item("test_chainsaw",
                    properties -> new AxeItem(Tiers.IRON, properties))
            .named("Test Chainsaw").look(IItemLook.HANDMADE).tab(TestItems.ITEMS).holdsFluid(CHAINSAW_TANK)
            .tool(ToolSpec.builder().runsOn(ToolPower.fuel(List.of(TestVehicles.FUEL), CHAINSAW_JOULES)).build())
            .register();
    public static final ItemEntry<ShovelItem> AIR_SHOVEL = TestSounds.CONTENT.item("test_air_shovel",
                    properties -> new ShovelItem(Tiers.IRON, properties))
            .named("Test Air Shovel").look(IItemLook.HANDMADE).tab(TestItems.ITEMS)
            .holdsGas(SHOVEL_TANK, SHOVEL_PRESSURE)
            .tool(ToolSpec.builder().runsOn(ToolPower.gas(List.of(TestMaterials.TESTOGEN.id()), SHOVEL_PER_BLOCK))
                    .build())
            .register();
    public static final ItemEntry<Railgun> RAILGUN = TestSounds.CONTENT.item("test_railgun", Railgun::new)
            .named("Test Railgun").look(IItemLook.HANDMADE).tab(TestItems.ITEMS)
            .holdsEnergy(RAILGUN_ENERGY, 1_000, 0)
            .tool(ToolSpec.builder().charges(RAILGUN_CHARGE).cooldown(RAILGUN_WAIT)
                    .runsOn(ToolPower.energy(RAILGUN_PER_SHOT)).build())
            .register();
    public static final ItemEntry<Item> WAND = TestSounds.CONTENT.item("test_wand", Item::new)
            .named("Test Wand").look(IItemLook.HANDMADE).tab(TestItems.ITEMS).modes(LINE, PLANE, COPY, PASTE)
            .tool(ToolSpec.builder().builds(LINE, BuildMode.line(WAND_LINE)).builds(PLANE, BuildMode.plane(WAND_PLANE))
                    .builds(COPY, BuildMode.copy()).builds(PASTE, BuildMode.paste()).build())
            .register();
    public static final ItemEntry<PickaxeItem> MULTITOOL = TestSounds.CONTENT.item("test_multitool",
                    properties -> new PickaxeItem(Tiers.DIAMOND, properties))
            .named("Test Multitool").look(IItemLook.HANDMADE).tab(TestItems.ITEMS)
            .holdsEnergy(MULTITOOL_ENERGY, 10_000, 0).register();
    public static final ItemEntry<Item> VEIN_MODULE = TestSounds.CONTENT.item("test_vein_module", Item::new)
            .named("Test Vein Module").look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();
    public static final ItemEntry<Item> SILK_MODULE = TestSounds.CONTENT.item("test_silk_module", Item::new)
            .named("Test Silk Touch Module").look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();
    public static final ItemEntry<Item> REACH_MODULE = TestSounds.CONTENT.item("test_reach_module", Item::new)
            .named("Test Reach Module").look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();
    public static final ItemEntry<Item> TIN_HAMMER = TestSounds.CONTENT.item("test_tin_hammer", Item::new)
            .named("Test Tin Hammer").look(IItemLook.HANDMADE).tab(TestItems.ITEMS)
            .durabilityFrom(SeriesMaterials.TIN, MaterialProperties.ATOMIC_NUMBER,
                    protons -> (int) protons * USES_PER_PROTON)
            .register();
    public static final ItemEntry<Item> MAGNET = TestSounds.CONTENT.item("test_magnet", Item::new)
            .named("Test Magnet").look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();

    public static final ModuleHostDef MULTITOOL_DEF = Modules.declareHost(MULTITOOL.getId(),
            ModuleHostDef.tool(List.of(new ModuleHostDef.Socket("tool", 3))));
    public static final ModuleDef VEIN_DEF = Modules.declare(VEIN_MODULE.getId(), new ModuleDef("tool",
            MODULE_ENERGY, List.of(new CoreModuleEffects.Mining(MiningShape.vein(MODULE_VEIN)))));
    public static final ModuleDef SILK_DEF = Modules.declare(SILK_MODULE.getId(), new ModuleDef("tool",
            MODULE_ENERGY, List.of(new CoreModuleEffects.Enchanted(Enchantments.SILK_TOUCH, 1))));
    public static final ModuleDef REACH_DEF = Modules.declare(REACH_MODULE.getId(), new ModuleDef("tool", 0,
            List.of(new CoreModuleEffects.AttributeChange(Attributes.BLOCK_INTERACTION_RANGE, REACH_BOOST,
                    AttributeModifier.Operation.ADD_VALUE))));

    /** Turns the magnet on or off wherever its player carries it; no key until given one. */
    public static final KeyAction MAGNET_KEY = ItemActions.declare(KeyAction.builder(
                    ResourceLocation.fromNamespaceAndPath(JsTests.MODID, "test_magnet"), MAGNET_KEY_NAME),
            stack -> stack.is(MAGNET.get()), (player, stack, shift) -> ItemActions.toggle(player, stack));

    private TestTools() {
    }

    /** Declares the tools, before the test mod's content is registered. */
    public static void declare() {
        // Loading the class declares them.
    }

    /**
     * The test railgun: held to charge, it fires let go at half its charge or more, and remembers the charge of the
     * last shot for the tests; it fires nothing.
     */
    public static final class Railgun extends ChargingItem {

        private static volatile float lastShot = -1.0F;

        public Railgun(final Properties properties) {
            super(properties);
        }

        /** The charge of the last shot fired, or below nothing before any. */
        public static float lastShot() {
            return lastShot;
        }

        /** Forgets the last shot, as a test begins. */
        public static void forget() {
            lastShot = -1.0F;
        }

        @Override
        protected boolean release(final Level level, final LivingEntity user, final ItemStack stack,
                                  final float charge) {
            if (charge < 0.5F) {
                return false;
            }
            lastShot = charge;
            return true;
        }
    }
}
