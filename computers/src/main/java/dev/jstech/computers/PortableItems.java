/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers;

import static dev.jstech.computers.registry.ComputingContent.CONTENT;
import static dev.jstech.computers.registry.ComputingContent.shelf;

import dev.jstech.computers.block.ChargingDockBlock;
import dev.jstech.computers.blockentity.ChargingDockBlockEntity;
import dev.jstech.computers.item.CabinetBlockItem;
import dev.jstech.computers.portable.PortableItem;
import dev.jstech.computers.portable.PortableModel;
import dev.jstech.computers.registry.ComputingContent.Shelf;
import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemBuilder;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * The portable devices and their docks (computers/docs/design/wireless.md): the twelve PDAs, smartphones and tablets,
 * each with its system from the factory, and a Mobile Device Charging Dock for each era that has them. They stand on
 * the network's shelf of their era, beside the access points they join through.
 */
public final class PortableItems {

    public static final ItemEntry<PortableItem> HIGHTIDE_POCKET =
            portable(PortableModel.HIGHTIDE_POCKET).named("Hightide Pocket").register();
    public static final ItemEntry<PortableItem> HIGHTIDE_POCKET_PRO =
            portable(PortableModel.HIGHTIDE_POCKET_PRO).named("Hightide Pocket Pro").register();
    public static final ItemEntry<PortableItem> QPHONE = portable(PortableModel.QPHONE).named("qPhone").register();
    public static final ItemEntry<PortableItem> MOTOROLLER_LANDMARK =
            portable(PortableModel.MOTOROLLER_LANDMARK).named("Motoroller Landmark").register();
    public static final ItemEntry<PortableItem> QPHONE_5 = portable(PortableModel.QPHONE_5).named("qPhone 5")
            .register();
    public static final ItemEntry<PortableItem> HIGHTIDE_WAVE =
            portable(PortableModel.HIGHTIDE_WAVE).named("Hightide Wave").register();
    public static final ItemEntry<PortableItem> QPAD_AIR = portable(PortableModel.QPAD_AIR).named("qPad Air")
            .register();
    public static final ItemEntry<PortableItem> SUNSUNG_NEBULA_TAB =
            portable(PortableModel.SUNSUNG_NEBULA_TAB).named("Sunsung Nebula Tab").register();
    public static final ItemEntry<PortableItem> QPHONE_X = portable(PortableModel.QPHONE_X).named("qPhone X")
            .register();
    public static final ItemEntry<PortableItem> SUNSUNG_NEBULA_S10 =
            portable(PortableModel.SUNSUNG_NEBULA_S10).named("Sunsung Nebula S10").register();
    public static final ItemEntry<PortableItem> QPAD_PRO = portable(PortableModel.QPAD_PRO).named("qPad Pro")
            .register();
    public static final ItemEntry<PortableItem> SUNSUNG_NEBULA_TAB_S7 =
            portable(PortableModel.SUNSUNG_NEBULA_TAB_S7).named("Sunsung Nebula Tab S7").register();

    public static final BlockEntry<ChargingDockBlock> LEGACY_CHARGING_DOCK =
            dock("legacy_mobile_device_charging_dock", HardwareEra.LEGACY, MapColor.COLOR_GRAY)
                    .named("Legacy Mobile Device Charging Dock").register();
    public static final BlockEntry<ChargingDockBlock> TRANSITION_CHARGING_DOCK =
            dock("transition_mobile_device_charging_dock", HardwareEra.TRANSITION, MapColor.SNOW)
                    .named("Transition Mobile Device Charging Dock").register();
    public static final BlockEntry<ChargingDockBlock> STANDARD_CHARGING_DOCK =
            dock("standard_mobile_device_charging_dock", HardwareEra.STANDARD, MapColor.METAL)
                    .named("Standard Mobile Device Charging Dock").register();
    public static final BlockEntry<ChargingDockBlock> ADVANCED_CHARGING_DOCK =
            dock("advanced_mobile_device_charging_dock", HardwareEra.ADVANCED, MapColor.COLOR_GRAY)
                    .named("Advanced Mobile Device Charging Dock").register();
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ChargingDockBlockEntity>>
            CHARGING_DOCK_BE = CONTENT.blockEntity("mobile_device_charging_dock", ChargingDockBlockEntity::new,
                    LEGACY_CHARGING_DOCK, TRANSITION_CHARGING_DOCK, STANDARD_CHARGING_DOCK, ADVANCED_CHARGING_DOCK);

    private PortableItems() {
    }

    public static void init() {
        // Referencing the class runs the declarations above.
    }

    /** The item of the device {@code model}. */
    public static PortableItem item(final PortableModel model) {
        return (switch (model) {
            case HIGHTIDE_POCKET -> HIGHTIDE_POCKET;
            case HIGHTIDE_POCKET_PRO -> HIGHTIDE_POCKET_PRO;
            case QPHONE -> QPHONE;
            case MOTOROLLER_LANDMARK -> MOTOROLLER_LANDMARK;
            case QPHONE_5 -> QPHONE_5;
            case HIGHTIDE_WAVE -> HIGHTIDE_WAVE;
            case QPAD_AIR -> QPAD_AIR;
            case SUNSUNG_NEBULA_TAB -> SUNSUNG_NEBULA_TAB;
            case QPHONE_X -> QPHONE_X;
            case SUNSUNG_NEBULA_S10 -> SUNSUNG_NEBULA_S10;
            case QPAD_PRO -> QPAD_PRO;
            case SUNSUNG_NEBULA_TAB_S7 -> SUNSUNG_NEBULA_TAB_S7;
        }).get();
    }

    /*
     * A portable: its icon in a slot, its model in a hand, as its item model (written beside the textures) says, one
     * to a stack.
     */
    private static ItemBuilder<PortableItem> portable(final PortableModel model) {
        return CONTENT.item(model.serializedName(), properties -> new PortableItem(properties, model))
                .look(IItemLook.HANDMADE).tab(shelf(Shelf.NETWORK, model.era()));
    }

    /* A dock of {@code era}: drawn by its block entity, its item the dock as it comes, its lamp dark, no hookup. */
    private static BlockBuilder<ChargingDockBlock> dock(final String id, final Era era, final MapColor colour) {
        final String model = ComputingLooks.chargingDock(era);
        return CONTENT.block(id, properties -> new ChargingDockBlock(properties, era))
                .properties(properties -> properties.mapColor(colour).strength(0.5F).sound(SoundType.METAL)
                        .noOcclusion())
                .look(IBlockLook.fixed(new IBlockModel.ParticleOnly(id + "_body",
                        "block/wireless/" + model + "_particle")))
                .geo(ComputingLooks.CHARGING_DOCK)
                .item((block, properties) -> new CabinetBlockItem(block, properties, ComputingLooks.CHARGING_DOCK,
                        model, ComputingModule.DEVICE_FIT, ComputingLooks.chargingDockAtRest()))
                .itemLook(IItemLook.DRAWN_BY_ENTITY).tab(shelf(Shelf.NETWORK, era));
    }
}
