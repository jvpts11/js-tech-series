/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests;

import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.module.CoreModuleEffects;
import dev.jstech.core.module.ModuleDef;
import dev.jstech.core.module.ModuleHostDef;
import dev.jstech.core.module.Modules;
import dev.jstech.core.robot.RobotCharger;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * What the test mod wears and charges, declared the way a mod declares its own, to prove the Core's worn modules and
 * robot charging: a helmet that holds energy, with a socket for a visor and two for a core; a module that makes its
 * wearer faster and one that lets them see in the dark; and a charging station for robots. They wear the game's own
 * pictures, so the test mod draws nothing.
 */
public final class TestModules {

    /** What the helmet holds, in FE. */
    public static final int HELMET_ENERGY = 100_000;
    /** What the speed module spends, in FE a second, and what it adds to its wearer's speed, as a share. */
    public static final int SPEED_ENERGY = 100;
    public static final double SPEED_BOOST = 0.2;
    /** What the night vision module spends, in FE a second. */
    public static final int SIGHT_ENERGY = 50;
    /** What the charging station holds, and gives a robot each half second, in FE. */
    public static final int CHARGER_ENERGY = 100_000;
    public static final int CHARGER_FLOW = 2_000;

    public static final ItemEntry<ArmorItem> HELMET = TestSounds.CONTENT.item("test_module_helmet",
            properties -> new ArmorItem(ArmorMaterials.IRON, ArmorItem.Type.HELMET, properties))
            .named("Test Module Helmet").look(IItemLook.HANDMADE).tab(TestItems.ITEMS)
            .holdsEnergy(HELMET_ENERGY, 2_000, 2_000).register();
    public static final ItemEntry<Item> SPEED_MODULE = TestSounds.CONTENT.item("test_speed_module", Item::new)
            .named("Test Speed Module").look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();
    public static final ItemEntry<Item> SIGHT_MODULE = TestSounds.CONTENT.item("test_sight_module", Item::new)
            .named("Test Night Vision Module").look(IItemLook.HANDMADE).tab(TestItems.ITEMS).register();

    public static final ModuleHostDef HELMET_DEF = Modules.declareHost(HELMET.getId(), new ModuleHostDef(List.of(
            new ModuleHostDef.Socket("visor", 1), new ModuleHostDef.Socket("core", 2))));
    public static final ModuleDef SPEED_DEF = Modules.declare(SPEED_MODULE.getId(), new ModuleDef("core",
            SPEED_ENERGY, List.of(new CoreModuleEffects.AttributeChange(Attributes.MOVEMENT_SPEED, SPEED_BOOST,
                    AttributeModifier.Operation.ADD_MULTIPLIED_BASE))));
    public static final ModuleDef SIGHT_DEF = Modules.declare(SIGHT_MODULE.getId(), new ModuleDef("visor",
            SIGHT_ENERGY, List.of(new CoreModuleEffects.GivenEffect(MobEffects.NIGHT_VISION, 0))));

    /* Its block state and model are written by hand, as the test mod's are. */
    public static final BlockEntry<DeviceBlock> CHARGER = TestSounds.CONTENT.block("test_robot_charger",
            Device.of(() -> TestModules.CHARGER_ENTITY.get()).ticks(ChargerBlockEntity::tick)::block)
            .named("Test robot charger").look(IBlockLook.cubeAll("test_robot_charger")).register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargerBlockEntity>> CHARGER_ENTITY =
            TestSounds.CONTENT.blockEntity("test_robot_charger", ChargerBlockEntity::new, CHARGER);

    private TestModules() {
    }

    /** Declares the helmet, the modules and the station, before the test mod's content is registered. */
    public static void declare(final IEventBus modBus) {
        modBus.addListener(RegisterCapabilitiesEvent.class, event ->
                RobotCharger.offer(event, CHARGER_ENTITY.get(), ChargerBlockEntity::charger));
    }

    /** The test charging station: the Core's as it comes. */
    public static final class ChargerBlockEntity extends SyncedBlockEntity {

        private final RobotCharger charger = new RobotCharger(CHARGER_ENERGY, CHARGER_FLOW, this::setChanged);

        public ChargerBlockEntity(final BlockPos pos, final BlockState state) {
            super(CHARGER_ENTITY.get(), pos, state);
            fields().part("Charger", this.charger).save();
        }

        /** Charges the robots in reach, each server tick. */
        public static void tick(final Level level, final BlockPos pos, final BlockState state,
                                final ChargerBlockEntity station) {
            if (level instanceof ServerLevel server) {
                station.charger.tick(server, pos);
            }
        }

        /** Its station. */
        public RobotCharger charger() {
            return this.charger;
        }
    }
}
