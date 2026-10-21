/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Example.
 */
package dev.jstech.example;

import dev.jstech.core.cable.CableEntry;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.Lane;
import dev.jstech.core.connect.Connection;
import dev.jstech.core.content.BlockBuilder;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.ContentTab;
import dev.jstech.core.content.Device;
import dev.jstech.core.content.DeviceBlock;
import dev.jstech.core.content.FluidEntry;
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.IBlockModel;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.content.PartEntry;
import dev.jstech.core.content.PartLook;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.machine.MachineBlockEntity;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.ProcessingKind;
import dev.jstech.example.machine.BatteryBlockEntity;
import dev.jstech.example.machine.CrusherBlockEntity;
import dev.jstech.example.machine.GeneratorBlockEntity;
import dev.jstech.example.machine.TankBlock;
import dev.jstech.example.machine.TankBlockEntity;
import dev.jstech.example.part.LevelSensorPart;
import dev.jstech.example.screen.LogbookItem;
import dev.jstech.example.screen.LogbookMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Everything the example mod adds, each declared once: what it is called, how it looks, which tab shows it, and what
 * it does. The Core registers them, writes their block states, models, names and loot from these declarations, gives
 * the machines their standard screen, and offers their energy, items and fluids to cables and pipes.
 *
 * <p>The blocks wear the game's own textures, so the example carries no pictures of its own.
 */
public final class ExampleContent {

    /** Everything the mod declares, under its id. */
    public static final ModContent CONTENT = new ModContent(JsExample.MODID);

    /** The mod's creative tab, with a shelf for the machines, one for the lines and one for the rest. */
    public static final ContentTab TAB = CONTENT.tab("example", "J's Example", () -> ExampleContent.GENERATOR);
    private static final ContentTab.Section MACHINES = TAB.section();
    private static final ContentTab.Section LINES = TAB.section();
    private static final ContentTab.Section THINGS = TAB.section();

    /**
     * The crusher's recipes: a recipe type of the mod's own, whose recipes are files of data under
     * {@code data/jsexample/recipe/crushing}, so a data pack adds, changes or takes away any of them.
     */
    public static final ProcessingKind CRUSHING = CONTENT.processing("crushing", "Crushing",
            () -> ExampleContent.CRUSHER);

    // The machines: each ticks its block entity the Core's way and opens the Core's standard machine screen.

    public static final BlockEntry<DeviceBlock> GENERATOR = machine("generator",
            Device.of(() -> ExampleContent.GENERATOR_ENTITY.get()).ticks(MachineBlockEntity::serverTick)
                    .opensMenu(MachineMenu::new))
            .named("Generator")
            .look(IBlockLook.facing(IBlockModel.orientable("generator", "minecraft:block/blast_furnace_side",
                    "minecraft:block/blast_furnace_front", "minecraft:block/blast_furnace_top")))
            .register();
    public static final BlockEntry<DeviceBlock> BATTERY = machine("battery",
            Device.of(() -> ExampleContent.BATTERY_ENTITY.get()).ticks(MachineBlockEntity::serverTick)
                    .opensMenu(MachineMenu::new))
            .named("Battery")
            .look(IBlockLook.fixed(new IBlockModel.Column("battery", "minecraft:block/lodestone_side",
                    "minecraft:block/lodestone_top")))
            .register();
    public static final BlockEntry<DeviceBlock> CRUSHER = machine("crusher",
            Device.of(() -> ExampleContent.CRUSHER_ENTITY.get()).ticks(MachineBlockEntity::serverTick)
                    .opensMenu(MachineMenu::new))
            .named("Crusher")
            .look(IBlockLook.facing(IBlockModel.orientable("crusher", "minecraft:block/smooth_stone_slab_side",
                    "minecraft:block/grindstone_side", "minecraft:block/smooth_stone")))
            .register();

    // The tank and what it holds.

    /** The tank: glass in an iron frame, which a bucket fills and empties and a pipe reaches on every side. */
    public static final BlockEntry<TankBlock> TANK = CONTENT.block("tank",
                    properties -> new TankBlock(properties, Device.of(() -> ExampleContent.TANK_ENTITY.get())))
            .properties(properties -> properties.mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.6F)
                    .sound(SoundType.GLASS).noOcclusion())
            .named("Tank")
            .look(IBlockLook.fixed(new IBlockModel.BottomTop("tank", "minecraft:block/glass",
                    "minecraft:block/iron_block", "minecraft:block/iron_block", "cutout")))
            .item().tab(MACHINES).register();
    /** Oil: dark, heavy and slow, with its block in the world and its bucket. */
    public static final FluidEntry OIL = CONTENT.fluid("oil").named("Oil")
            .look(ResourceLocation.withDefaultNamespace("block/water_still"),
                    ResourceLocation.withDefaultNamespace("block/water_flow"), 0xFF2B2118)
            .density(900).viscosity(4_000).tab(THINGS).register();

    // The lines, laid in the Core's shared cable block.

    /** What the energy cable joins: two blocks are on one line only when they carry it. */
    public static final Connection ENERGY_LINE = Connection.of(JsExample.id("energy"));
    /** The energy cable: a wire in one lane of a cable block, carrying any amount any distance. */
    public static final CableEntry ENERGY_CABLE = CONTENT.cable("energy_cable", CableType.builder(ENERGY_LINE)
                    .describe(ExampleTexts.ENERGY_CABLE_JOB, null)
                    .grid(GridKind.POWER).lane(Lane.BOTTOM_RIGHT).carries(Long.MAX_VALUE, 0)
                    .jacket(ResourceLocation.withDefaultNamespace("block/copper_block"))
                    .plug(JsExample.id("block/cable/plug/energy")))
            .named("Energy Cable").tab(LINES).register();
    /** The fluid pipe: alone in its block, as pipes are, carrying a bucket a second. */
    public static final CableEntry FLUID_PIPE = CONTENT.cable("fluid_pipe",
                    CableType.builder(Connection.of(JsExample.id("fluid")))
                            .describe(ExampleTexts.FLUID_PIPE_JOB, null)
                            .grid(GridKind.FLUID).alone().thickness(8).carries(50L, 0)
                            .jacket(ResourceLocation.withDefaultNamespace("block/iron_block"))
                            .plug(JsExample.id("block/cable/plug/fluid")))
            .named("Fluid Pipe").tab(LINES).register();
    /** The level sensor: a plate mounted on a cable's face, giving out how full the block beside it is. */
    public static final PartEntry<LevelSensorPart> SENSOR = CONTENT.part("level_sensor", LevelSensorPart::new)
            .named("Level Sensor")
            .look(PartLook.plate("minecraft:block/observer_front", "minecraft:block/smooth_stone"))
            .redstone().tab(LINES).register();

    // The logbook, and the screen it opens.

    public static final ItemEntry<LogbookItem> LOGBOOK = CONTENT.item("logbook", LogbookItem::new)
            .named("Logbook").look(IItemLook.parent("minecraft:item/writable_book")).tab(THINGS).register();
    public static final DeferredHolder<MenuType<?>, MenuType<LogbookMenu>> LOGBOOK_MENU = CONTENT.menu("logbook",
            (id, inventory, buf) -> new LogbookMenu(id, inventory));

    // The block entities, each naming the blocks that make it.

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> GENERATOR_ENTITY =
            CONTENT.blockEntity("generator", GeneratorBlockEntity::new, GENERATOR);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBlockEntity>> BATTERY_ENTITY =
            CONTENT.blockEntity("battery", BatteryBlockEntity::new, BATTERY);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrusherBlockEntity>> CRUSHER_ENTITY =
            CONTENT.blockEntity("crusher", CrusherBlockEntity::new, CRUSHER);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TankBlockEntity>> TANK_ENTITY =
            CONTENT.blockEntity("tank", TankBlockEntity::new, TANK);

    private ExampleContent() {
    }

    /* A machine: a metal block that needs a pickaxe to come away, shown with the other machines. */
    private static BlockBuilder<DeviceBlock> machine(final String id, final Device<?> device) {
        return CONTENT.block(id, device::block)
                .properties(properties -> properties.mapColor(MapColor.METAL).strength(3.5F)
                        .requiresCorrectToolForDrops())
                .item()
                .tab(MACHINES)
                .tag(BlockTags.MINEABLE_WITH_PICKAXE);
    }
}
