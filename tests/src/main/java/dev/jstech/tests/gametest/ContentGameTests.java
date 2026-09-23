/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.os.OsBootstrap;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.content.Drops;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.content.ModContent;
import dev.jstech.tests.JsTests;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Everything a block or an item needs before it can be called done, checked for every one the mods of the series
 * register: it was declared rather than registered by hand, it has its block state and models, a name, a place in
 * a creative tab, the drop it declares, the tool it needs, and a block entity that accepts it. A missing piece used
 * to be found in game; here it fails the build, whichever block it is and whoever added it.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ContentGameTests {

    private static final String ARENA = "empty";

    private ContentGameTests() {
    }

    /** Nothing is registered around the declarations, so nothing escapes what they generate and what is checked. */
    @GameTest(template = ARENA)
    public static void declaredContent_coversEveryRegisteredBlockAndItem(final GameTestHelper helper) {
        final List<String> wrong = new ArrayList<>();
        final Set<String> mods = new HashSet<>();
        ModContent.all().forEach(content -> mods.add(content.modid()));
        if (!mods.containsAll(Set.of("jscore", "jsc", "jsindustrial"))) {
            wrong.add("the series declared content only for " + mods);
        }
        for (final ModContent content : ModContent.all()) {
            final Set<ResourceLocation> blocks = new HashSet<>();
            final Set<ResourceLocation> items = new HashSet<>();
            for (final BlockEntry<?> block : content.declaredBlocks()) {
                blocks.add(block.getId());
                if (block.hasItem()) {
                    items.add(block.getId());
                }
            }
            content.declaredItems().forEach(item -> items.add(item.getId()));
            BuiltInRegistries.BLOCK.keySet().stream()
                    .filter(id -> id.getNamespace().equals(content.modid()) && !blocks.contains(id))
                    .forEach(id -> wrong.add("block " + id));
            BuiltInRegistries.ITEM.keySet().stream()
                    .filter(id -> id.getNamespace().equals(content.modid()) && !items.contains(id))
                    .forEach(id -> wrong.add("item " + id));
        }
        report(helper, wrong, "registered without being declared");
    }

    /** Every block has its block state and every item its model; no model is left over for an item that is gone. */
    @GameTest(template = ARENA)
    public static void declaredContent_hasItsFilesAndNoneLeftOver(final GameTestHelper helper) {
        final List<String> wrong = new ArrayList<>();
        for (final ModContent content : ModContent.all()) {
            final String mod = content.modid();
            final Set<String> items = new HashSet<>();
            for (final BlockEntry<?> block : content.declaredBlocks()) {
                if (!Files.exists(resource(mod, "blockstates", block.getId().getPath() + ".json"))) {
                    wrong.add("no block state for " + block.getId());
                }
                if (block.hasItem()) {
                    items.add(block.getId().getPath());
                }
            }
            content.declaredItems().forEach(item -> items.add(item.getId().getPath()));
            for (final String item : items) {
                if (!Files.exists(resource(mod, "models", "item", item + ".json"))) {
                    wrong.add("no item model for " + mod + ":" + item);
                }
            }
            final List<String> models = fileNames(resource(mod, "models", "item"));
            if (models.isEmpty() && !items.isEmpty()) {
                wrong.add("the item models of " + mod + " were not found where they are read from");
            }
            for (final String model : models) {
                if (!items.contains(model)) {
                    wrong.add("an item model for no item: " + mod + ":" + model);
                }
            }
        }
        report(helper, wrong, "files missing or left over");
    }

    /** Every block and item reads as a name, in English here and in whatever language a player has. */
    @GameTest(template = ARENA)
    public static void declaredContent_isNamed(final GameTestHelper helper) {
        final Language english = Language.getInstance();
        final List<String> wrong = new ArrayList<>();
        for (final ModContent content : ModContent.all()) {
            for (final BlockEntry<?> block : content.declaredBlocks()) {
                if (!english.has(block.get().getDescriptionId())) {
                    wrong.add(block.get().getDescriptionId());
                }
            }
            for (final ItemEntry<?> item : content.declaredItems()) {
                if (!english.has(item.get().getDescriptionId())) {
                    wrong.add(item.get().getDescriptionId());
                }
            }
        }
        report(helper, wrong, "without a name in the language file");
    }

    /** Every item is in a creative tab of the series, which is where a creative player finds it. */
    @GameTest(template = ARENA)
    public static void declaredItems_areInACreativeTab(final GameTestHelper helper) {
        final ServerLevel level = helper.getLevel();
        final Set<String> mods = new HashSet<>();
        ModContent.all().forEach(content -> mods.add(content.modid()));
        final Set<Item> shown = new HashSet<>();
        final CreativeModeTab.ItemDisplayParameters parameters = new CreativeModeTab.ItemDisplayParameters(
                level.enabledFeatures(), true, level.registryAccess());
        for (final var tab : BuiltInRegistries.CREATIVE_MODE_TAB.entrySet()) {
            if (mods.contains(tab.getKey().location().getNamespace())) {
                tab.getValue().buildContents(parameters);
                tab.getValue().getDisplayItems().forEach(stack -> shown.add(stack.getItem()));
            }
        }
        final List<String> wrong = new ArrayList<>();
        for (final ModContent content : ModContent.all()) {
            for (final BlockEntry<?> block : content.declaredBlocks()) {
                if (block.hasItem() && !shown.contains(block.item())) {
                    wrong.add(block.getId().toString());
                }
            }
            for (final ItemEntry<?> item : content.declaredItems()) {
                if (!shown.contains(item.get())) {
                    wrong.add(item.getId().toString());
                }
            }
        }
        report(helper, wrong, "in no creative tab");
    }

    /** A block that drops itself drops just its own item, mined with the best tool; one that drops nothing, nothing. */
    @GameTest(template = ARENA)
    public static void declaredBlocks_dropWhatTheyDeclare(final GameTestHelper helper) {
        final List<String> wrong = new ArrayList<>();
        for (final ModContent content : ModContent.all()) {
            for (final BlockEntry<?> block : content.declaredBlocks()) {
                final List<ItemStack> drops = block.get().defaultBlockState().getDrops(
                        new LootParams.Builder(helper.getLevel())
                                .withParameter(LootContextParams.ORIGIN, Vec3.ZERO)
                                .withParameter(LootContextParams.TOOL, new ItemStack(Items.NETHERITE_PICKAXE)));
                final boolean right = block.drops() == Drops.SELF
                        ? drops.size() == 1 && drops.getFirst().is(block.item()) && drops.getFirst().getCount() == 1
                        : drops.isEmpty();
                if (!right) {
                    wrong.add(block.getId() + " declared " + block.drops() + " and dropped " + drops);
                }
            }
        }
        report(helper, wrong, "not dropping what they declare");
    }

    /** A block that needs the right tool to come away has a tool that is right for it. */
    @GameTest(template = ARENA)
    public static void declaredBlocks_thatNeedATool_haveOne(final GameTestHelper helper) {
        final List<ItemStack> tools = List.of(new ItemStack(Items.NETHERITE_PICKAXE),
                new ItemStack(Items.NETHERITE_AXE), new ItemStack(Items.NETHERITE_SHOVEL),
                new ItemStack(Items.NETHERITE_HOE));
        final List<String> wrong = new ArrayList<>();
        for (final ModContent content : ModContent.all()) {
            for (final BlockEntry<?> block : content.declaredBlocks()) {
                final BlockState state = block.get().defaultBlockState();
                if (state.requiresCorrectToolForDrops()
                        && tools.stream().noneMatch(tool -> tool.isCorrectToolForDrops(state))) {
                    wrong.add(block.getId().toString());
                }
            }
        }
        report(helper, wrong, "needing a tool that no tool is right for");
    }

    /** A block that makes a block entity is one its entity accepts, so no variant is left out of its type. */
    @GameTest(template = ARENA)
    public static void declaredBlocks_areValidForTheEntityTheyMake(final GameTestHelper helper) {
        final List<String> wrong = new ArrayList<>();
        for (final ModContent content : ModContent.all()) {
            for (final BlockEntry<?> block : content.declaredBlocks()) {
                if (block.get() instanceof EntityBlock maker) {
                    final BlockState state = block.get().defaultBlockState();
                    final BlockEntity made = maker.newBlockEntity(BlockPos.ZERO, state);
                    if (made != null && !made.getType().isValid(state)) {
                        wrong.add(block.getId() + " makes "
                                + BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(made.getType()));
                    }
                }
            }
        }
        report(helper, wrong, "not accepted by the block entity they make");
    }

    /** Every program says what it does, in the language file, where its install disc and the lists show it. */
    @GameTest(template = ARENA)
    public static void programs_sayWhatTheyDo(final GameTestHelper helper) {
        final List<String> wrong = new ArrayList<>();
        for (final ProgramSpec program : OsBootstrap.builtinPrograms()) {
            if (program.description().isBlank() || !Language.getInstance().has(program.descriptionKey())) {
                wrong.add(program.id().toString());
            }
        }
        report(helper, wrong, "programs saying nothing about what they do");
    }

    private static void report(final GameTestHelper helper, final List<String> wrong, final String what) {
        if (!wrong.isEmpty()) {
            helper.fail(wrong.size() + " " + what + ": " + wrong);
        }
        helper.succeed();
    }

    /** A file in the mod's own resources, whether written by hand or generated. */
    private static Path resource(final String mod, final String... path) {
        final String[] full = new String[path.length + 2];
        full[0] = "assets";
        full[1] = mod;
        System.arraycopy(path, 0, full, 2, path.length);
        return ModList.get().getModFileById(mod).getFile().findResource(full);
    }

    /** The names of the JSON files in a folder of the mod's resources, without the extension. */
    private static List<String> fileNames(final Path folder) {
        if (!Files.isDirectory(folder)) {
            return List.of();
        }
        try (Stream<Path> files = Files.list(folder)) {
            return files.map(file -> file.getFileName().toString())
                    .filter(name -> name.endsWith(".json"))
                    .map(name -> name.substring(0, name.length() - ".json".length()))
                    .toList();
        } catch (final IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
