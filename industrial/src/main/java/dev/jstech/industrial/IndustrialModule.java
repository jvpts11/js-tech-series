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
import dev.jstech.core.content.IBlockLook;
import dev.jstech.core.content.ModContent;
import dev.jstech.core.material.MaterialForm;
import dev.jstech.core.material.MaterialItems;
import dev.jstech.core.material.ModMaterial;
import dev.jstech.industrial.block.CoalGeneratorBlock;
import dev.jstech.industrial.block.CompressorBlock;
import dev.jstech.industrial.block.ElectricFurnaceBlock;
import dev.jstech.industrial.block.MaceratorBlock;
import dev.jstech.industrial.blockentity.CoalGeneratorBlockEntity;
import dev.jstech.industrial.blockentity.CompressorBlockEntity;
import dev.jstech.industrial.blockentity.ElectricFurnaceBlockEntity;
import dev.jstech.industrial.blockentity.MaceratorBlockEntity;
import dev.jstech.industrial.menu.CoalGeneratorMenu;
import dev.jstech.industrial.menu.CompressorMenu;
import dev.jstech.industrial.menu.ElectricFurnaceMenu;
import dev.jstech.industrial.menu.MaceratorMenu;
import dev.jstech.industrial.recipe.CompressingRecipe;
import dev.jstech.industrial.recipe.MaceratingRecipe;
import java.util.function.Function;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
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

    // Machines, in the order the tab shows them

    public static final BlockEntry<MaceratorBlock> MACERATOR = machine("macerator", MaceratorBlock::new)
            .named("Macerator").machineFor(MACERATING_TYPE.getId()).register();

    public static final BlockEntry<ElectricFurnaceBlock> ELECTRIC_FURNACE =
            machine("electric_furnace", ElectricFurnaceBlock::new)
                    .named("Electric Furnace").machineFor(ResourceLocation.withDefaultNamespace("smelting")).register();

    public static final BlockEntry<CompressorBlock> COMPRESSOR = machine("compressor", CompressorBlock::new)
            .named("Compressor").machineFor(COMPRESSING_TYPE.getId()).register();

    public static final BlockEntry<CoalGeneratorBlock> COAL_GENERATOR =
            machine("coal_generator", CoalGeneratorBlock::new).named("Coal Generator").register();

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

    public static final DeferredHolder<MenuType<?>, MenuType<MaceratorMenu>> MACERATOR_MENU =
            MENUS.register("macerator", () -> IMenuTypeExtension.create(MaceratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CoalGeneratorMenu>> COAL_GENERATOR_MENU =
            MENUS.register("coal_generator", () -> IMenuTypeExtension.create(CoalGeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE_MENU =
            MENUS.register("electric_furnace", () -> IMenuTypeExtension.create(ElectricFurnaceMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<CompressorMenu>> COMPRESSOR_MENU =
            MENUS.register("compressor", () -> IMenuTypeExtension.create(CompressorMenu::new));

    private IndustrialModule() {
    }

    public static void register(final IEventBus modEventBus) {
        CONTENT.register(modEventBus);
        RECIPE_TYPES.register(modEventBus);
        RECIPE_SERIALIZERS.register(modEventBus);
        MENUS.register(modEventBus);
    }

    /**
     * A machine: a metal box that faces the way it was placed, needs a pickaxe to come away with its contents, and
     * is shown with the other machines.
     */
    private static <B extends Block> BlockBuilder<B> machine(final String id,
                                                            final Function<BlockBehaviour.Properties, B> factory) {
        return CONTENT.block(id, factory)
                .properties(properties -> properties.mapColor(MapColor.METAL).strength(3.5F)
                        .requiresCorrectToolForDrops())
                .look(IBlockLook::orientable)
                .item()
                .tab(MACHINES)
                .tag(BlockTags.MINEABLE_WITH_PICKAXE);
    }
}
