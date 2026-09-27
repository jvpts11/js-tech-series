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
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.material.MaterialForm;
import dev.jstech.core.material.MaterialItems;
import dev.jstech.core.material.ModMaterial;
import dev.jstech.industrial.blockentity.CoalGeneratorBlockEntity;
import dev.jstech.industrial.blockentity.CompressorBlockEntity;
import dev.jstech.industrial.blockentity.ElectricFurnaceBlockEntity;
import dev.jstech.industrial.blockentity.MaceratorBlockEntity;
import dev.jstech.industrial.blockentity.ProcessingMachineBlockEntity;
import dev.jstech.industrial.menu.CoalGeneratorMenu;
import dev.jstech.industrial.menu.ProcessingMachineMenu;
import dev.jstech.industrial.recipe.CompressingRecipe;
import dev.jstech.industrial.recipe.MaceratingRecipe;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Everything the industrial mod registers: its machines, their block entities, recipes, menus and its
 * creative tab.
 */
public final class IndustrialModule {

    public static final ModContent CONTENT = new ModContent(JsIndustrial.MODID);

    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, JsIndustrial.MODID);

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, JsIndustrial.MODID);

    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, JsIndustrial.MODID);

    // Creative tab: the machines, then the core's material items, which have no tab of their own.

    public static final ContentTab INDUSTRIAL_TAB =
            CONTENT.tab("industrial", "J's Industrial", () -> IndustrialModule.MACERATOR);

    private static final ContentTab.Section MACHINES = INDUSTRIAL_TAB.section().alsoShowing(output -> {
        for (final ModMaterial material : ModMaterial.values()) {
            for (final MaterialForm form : material.activeModForms()) {
                output.accept(MaterialItems.get(material, form).get());
            }
        }
    });

    // Recipes

    public static final DeferredHolder<RecipeType<?>, RecipeType<MaceratingRecipe>> MACERATING_TYPE =
            RECIPE_TYPES.register("macerating", () -> new RecipeType<MaceratingRecipe>() {
                @Override
                public String toString() {
                    return "macerating";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, MaceratingRecipe.Serializer> MACERATING_SERIALIZER =
            RECIPE_SERIALIZERS.register("macerating", MaceratingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CompressingRecipe>> COMPRESSING_TYPE =
            RECIPE_TYPES.register("compressing", () -> new RecipeType<CompressingRecipe>() {
                @Override
                public String toString() {
                    return "compressing";
                }
            });

    public static final DeferredHolder<RecipeSerializer<?>, CompressingRecipe.Serializer> COMPRESSING_SERIALIZER =
            RECIPE_SERIALIZERS.register("compressing", CompressingRecipe.Serializer::new);

    // Machines, in the order the tab shows them: each ticks its block entity and opens its menu when used

    public static final BlockEntry<DeviceBlock> MACERATOR = machine("macerator",
            Device.of(() -> IndustrialModule.MACERATOR_BE.get()).ticks(ProcessingMachineBlockEntity::serverTick)
                    .opensMenu((id, inventory, machine) ->
                            new ProcessingMachineMenu(IndustrialModule.MACERATOR_MENU.get(), id, inventory, machine)))
            .named("Macerator").machineFor(MACERATING_TYPE.getId()).register();

    public static final BlockEntry<DeviceBlock> ELECTRIC_FURNACE = machine("electric_furnace",
            Device.of(() -> IndustrialModule.ELECTRIC_FURNACE_BE.get())
                    .ticks(ProcessingMachineBlockEntity::serverTick)
                    .opensMenu((id, inventory, machine) -> new ProcessingMachineMenu(
                            IndustrialModule.ELECTRIC_FURNACE_MENU.get(), id, inventory, machine)))
            .named("Electric Furnace").machineFor(ResourceLocation.withDefaultNamespace("smelting")).register();

    public static final BlockEntry<DeviceBlock> COMPRESSOR = machine("compressor",
            Device.of(() -> IndustrialModule.COMPRESSOR_BE.get()).ticks(ProcessingMachineBlockEntity::serverTick)
                    .opensMenu((id, inventory, machine) ->
                            new ProcessingMachineMenu(IndustrialModule.COMPRESSOR_MENU.get(), id, inventory, machine)))
            .named("Compressor").machineFor(COMPRESSING_TYPE.getId()).register();

    public static final BlockEntry<DeviceBlock> COAL_GENERATOR = machine("coal_generator",
            Device.of(() -> IndustrialModule.COAL_GENERATOR_BE.get()).ticks(CoalGeneratorBlockEntity::serverTick)
                    .opensMenu(CoalGeneratorMenu::new))
            .named("Coal Generator").register();

    // Block entities

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MaceratorBlockEntity>> MACERATOR_BE =
            CONTENT.blockEntity("macerator", MaceratorBlockEntity::new, MACERATOR);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CoalGeneratorBlockEntity>> COAL_GENERATOR_BE =
            CONTENT.blockEntity("coal_generator", CoalGeneratorBlockEntity::new, COAL_GENERATOR);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE_BE =
            CONTENT.blockEntity("electric_furnace", ElectricFurnaceBlockEntity::new, ELECTRIC_FURNACE);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CompressorBlockEntity>> COMPRESSOR_BE =
            CONTENT.blockEntity("compressor", CompressorBlockEntity::new, COMPRESSOR);

    // Menus

    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMachineMenu>> MACERATOR_MENU =
            MENUS.register("macerator", () -> IMenuTypeExtension.create((id, inventory, buf) ->
                    ProcessingMachineMenu.fromNetwork(IndustrialModule.MACERATOR_MENU.get(), id, inventory, buf)));

    public static final DeferredHolder<MenuType<?>, MenuType<CoalGeneratorMenu>> COAL_GENERATOR_MENU =
            MENUS.register("coal_generator", () -> IMenuTypeExtension.create(CoalGeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMachineMenu>> ELECTRIC_FURNACE_MENU =
            MENUS.register("electric_furnace", () -> IMenuTypeExtension.create((id, inventory, buf) ->
                    ProcessingMachineMenu.fromNetwork(IndustrialModule.ELECTRIC_FURNACE_MENU.get(), id, inventory,
                            buf)));

    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMachineMenu>> COMPRESSOR_MENU =
            MENUS.register("compressor", () -> IMenuTypeExtension.create((id, inventory, buf) ->
                    ProcessingMachineMenu.fromNetwork(IndustrialModule.COMPRESSOR_MENU.get(), id, inventory, buf)));

    private IndustrialModule() {
    }

    public static void register(final IEventBus modEventBus) {
        CONTENT.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        MENUS.register(modEventBus);
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
