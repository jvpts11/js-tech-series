/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial;

import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.item.FluidBlockItem;
import dev.jstech.core.jstechseries.SeriesMaterials;
import dev.jstech.core.jstechseries.SharedUses;
import dev.jstech.core.jstechseries.energy.SharedEnergy;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.industrial.blockentity.CoalGeneratorBlockEntity;
import dev.jstech.industrial.blockentity.CompressorBlockEntity;
import dev.jstech.industrial.blockentity.ElectricFurnaceBlockEntity;
import dev.jstech.industrial.blockentity.MaceratorBlockEntity;
import dev.jstech.industrial.blockentity.TankBlockEntity;
import java.util.function.Supplier;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Everything the industrial mod registers: its machines, their block entities, recipes and its creative tab, and what
 * it uses of the series' shared content. Its machines open the screen every machine of the series has, the Core's.
 */
public final class IndustrialModule {

    public static final ModContent CONTENT = new ModContent(JsIndustrial.MODID);

    /*
     * Creative tab: the machines, then the storage. What the mod uses of the series' shared content (its cable, its
     * materials) is on the J's Tech tab, with everything else the series' mods share.
     */

    public static final ContentTab INDUSTRIAL_TAB =
            CONTENT.tab("industrial", "J's Industrial", () -> IndustrialModule.MACERATOR);

    private static final ContentTab.Section MACHINES = INDUSTRIAL_TAB.section();
    private static final ContentTab.Section STORAGE = INDUSTRIAL_TAB.section();
    /** The tab's last shelf: the mod's manual. */
    public static final ContentTab.Section MANUALS = INDUSTRIAL_TAB.section();

    // Recipes: the kinds of the processing machines, each a recipe type of its own, read the Core's way.

    /** Grinding: an ore or a block into its dusts. */
    public static final ProcessingKind MACERATING =
            CONTENT.processing("macerating", "Macerating", () -> IndustrialModule.MACERATOR);
    /** Pressing: an ingot into a plate. */
    public static final ProcessingKind COMPRESSING =
            CONTENT.processing("compressing", "Compressing", () -> IndustrialModule.COMPRESSOR);

    /*
     * Machines, in the order the tab shows them: each is ticked by the Core's machine kit, which runs its work, its
     * sides and its state, and opens the screen every machine has when used.
     */

    public static final BlockEntry<DeviceBlock> MACERATOR = machine("macerator",
            Device.of(() -> IndustrialModule.MACERATOR_BE.get()).ticks(MachineBlockEntity::serverTick)
                    .opensMenu(MachineMenu::new))
            .named("Macerator").register();

    public static final BlockEntry<DeviceBlock> ELECTRIC_FURNACE = machine("electric_furnace",
            Device.of(() -> IndustrialModule.ELECTRIC_FURNACE_BE.get()).ticks(MachineBlockEntity::serverTick)
                    .opensMenu(MachineMenu::new))
            .named("Electric Furnace").register();

    public static final BlockEntry<DeviceBlock> COMPRESSOR = machine("compressor",
            Device.of(() -> IndustrialModule.COMPRESSOR_BE.get()).ticks(MachineBlockEntity::serverTick)
                    .opensMenu(MachineMenu::new))
            .named("Compressor").register();

    public static final BlockEntry<DeviceBlock> COAL_GENERATOR = machine("coal_generator",
            Device.of(() -> IndustrialModule.COAL_GENERATOR_BE.get()).ticks(MachineBlockEntity::serverTick)
                    .opensMenu(MachineMenu::new))
            .named("Coal Generator").register();

    // Materials: the series' shared ones the machines make, which the Core registers because the mod uses them.

    /** Iron Dust, which the Macerator grinds from ore and from ingots. */
    public static final Supplier<Item> IRON_DUST =
            MaterialUses.use(JsIndustrial.MODID, SeriesMaterials.IRON, Forms.DUST);
    /** Iron Plate, which the Compressor presses from an ingot. */
    public static final Supplier<Item> IRON_PLATE =
            MaterialUses.use(JsIndustrial.MODID, SeriesMaterials.IRON, Forms.PLATE);
    /** Copper Plate, which the Compressor presses from an ingot. */
    public static final Supplier<Item> COPPER_PLATE =
            MaterialUses.use(JsIndustrial.MODID, SeriesMaterials.COPPER, Forms.PLATE);

    static {
        // Its machines' energy runs along the series' basic cable, J's Core's: it has no energy cable of its own.
        SharedUses.declare(JsIndustrial.MODID, () -> SharedEnergy.INSULATED_COPPER_CABLE);
    }

    // Storage

    /*
     * The Tank: glass walls in a metal casing, so it reads as a vessel rather than a solid block. A bucket fills and
     * empties it, and broken, it keeps its liquid in its item.
     */
    public static final BlockEntry<DeviceBlock> TANK = CONTENT.block("tank",
                    Device.of(() -> IndustrialModule.TANK_BE.get()).fillsFromContainers()::block)
            .properties(properties -> properties.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.6F)
                    .sound(SoundType.GLASS).noOcclusion())
            .named("Tank")
            .look(IBlockLook.fixed(new IBlockModel.BottomTop("tank", "minecraft:block/glass",
                    "block/tank_casing", "block/tank_casing", "cutout")))
            .item((block, properties) -> new FluidBlockItem(block, properties, TankBlockEntity.CAPACITY_MB))
            .tab(STORAGE).drops(Drops.SELF_WITH_CONTENTS).register();

    // Block entities

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MaceratorBlockEntity>> MACERATOR_BE =
            CONTENT.blockEntity("macerator", MaceratorBlockEntity::new, MACERATOR);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoalGeneratorBlockEntity>> COAL_GENERATOR_BE =
            CONTENT.blockEntity("coal_generator", CoalGeneratorBlockEntity::new, COAL_GENERATOR);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE_BE =
            CONTENT.blockEntity("electric_furnace", ElectricFurnaceBlockEntity::new, ELECTRIC_FURNACE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CompressorBlockEntity>> COMPRESSOR_BE =
            CONTENT.blockEntity("compressor", CompressorBlockEntity::new, COMPRESSOR);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>> TANK_BE =
            CONTENT.blockEntity("tank", TankBlockEntity::new, TANK);

    private IndustrialModule() {
    }

    public static void register(final IEventBus modEventBus) {
        CONTENT.register(modEventBus);
    }

    /**
     * A machine: a metal box that faces the way it was placed, does what its device says, needs a pickaxe to come
     * away with its contents, and is shown with the other machines.
     */
    private static BlockBuilder<DeviceBlock> machine(final String id, final Device<?> device) {
        return CONTENT.block(id, device::block)
                .properties(properties -> properties.mapColor(MapColor.METAL).strength(3.5F)
                        .requiresCorrectToolForDrops())
                .look(IBlockLook::orientable)
                .item()
                .tab(MACHINES)
                .tag(BlockTags.MINEABLE_WITH_PICKAXE);
    }
}
