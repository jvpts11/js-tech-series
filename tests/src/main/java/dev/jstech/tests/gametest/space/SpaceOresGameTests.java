/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.jstechseries.SeriesMinerals;
import dev.jstech.core.material.Forms;
import dev.jstech.core.material.HostRock;
import dev.jstech.core.material.HostRocks;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialFormBlock;
import dev.jstech.core.material.MaterialUses;
import dev.jstech.space.geology.SpaceOres;
import dev.jstech.tests.JsTests;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The ores of the Moon and of Mars: the rocks of those worlds are host rocks of J's Core's registry, and each of the
 * twelve ores lies in its rock, named after it and its mineral, a little harder than the rock, filed with the
 * mineral's other ores, and broken with a pickaxe to drop the raw mineral.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class SpaceOresGameTests {

    private static final String ARENA = "empty";
    private static final BlockPos AT = new BlockPos(1, 2, 1);
    /* An ore is a little harder than its rock, as the game's are. */
    private static final float ORE_HARDER = 1.5F;

    private static final List<Ore> ORES = List.of(
            new Ore(SeriesMinerals.ILMENITE, SpaceOres.MARE_BASALT, "Mare Basalt Ilmenite Ore"),
            new Ore(SeriesMinerals.ILMENITE, SpaceOres.MARE_REGOLITH, "Mare Regolith Ilmenite Ore"),
            new Ore(SeriesMinerals.ARMALCOLITE, SpaceOres.MARE_BASALT, "Mare Basalt Armalcolite Ore"),
            new Ore(SeriesMinerals.TRANQUILLITYITE, SpaceOres.MARE_BASALT, "Mare Basalt Tranquillityite Ore"),
            new Ore(SeriesMinerals.MERRILLITE, SpaceOres.MARE_BASALT, "Mare Basalt Merrillite Ore"),
            new Ore(SeriesMinerals.NATIVE_IRON, SpaceOres.HIGHLAND_REGOLITH, "Highland Regolith Native Iron Ore"),
            new Ore(SeriesMinerals.HEMATITE, SpaceOres.MARTIAN_REGOLITH, "Martian Regolith Hematite Ore"),
            new Ore(SeriesMinerals.HEMATITE, SpaceOres.MUDSTONE, "Mudstone Hematite Ore"),
            new Ore(SeriesMinerals.MAGNETITE, SpaceOres.MARTIAN_BASALT, "Martian Basalt Magnetite Ore"),
            new Ore(SeriesMinerals.OLIVINE, SpaceOres.MARTIAN_BASALT, "Martian Basalt Olivine Ore"),
            new Ore(SeriesMinerals.JAROSITE, SpaceOres.MUDSTONE, "Mudstone Jarosite Ore"),
            new Ore(SeriesMinerals.GYPSUM, SpaceOres.MUDSTONE, "Mudstone Gypsum Ore"));

    private SpaceOresGameTests() {
    }

    @GameTest(template = ARENA)
    public static void rocks_ofTheMoonAndMarsAreHostRocksOfTheCore(final GameTestHelper helper) {
        for (final HostRock rock : List.of(SpaceOres.MARE_BASALT, SpaceOres.MARE_REGOLITH,
                SpaceOres.HIGHLAND_REGOLITH, SpaceOres.MARTIAN_REGOLITH, SpaceOres.MARTIAN_BASALT,
                SpaceOres.MUDSTONE)) {
            helper.assertTrue(HostRocks.byId(rock.id()).filter(found -> found == rock).isPresent(),
                    rock + " is in the Core's registry of host rocks, for any mod to lay ores in");
            helper.assertFalse(rock.found(), rock + " is declared by J's Space, not found by its tags");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void ores_lieInTheirRocksNamedAndFiled(final GameTestHelper helper) {
        for (final Ore ore : ORES) {
            final Block block = MaterialUses.oreBlock(ore.mineral(), ore.rock()).get();
            final String id = ore.mineral().id().getPath() + " in " + ore.rock();
            helper.assertTrue(block instanceof MaterialFormBlock, id + " has an ore; got " + block);
            helper.assertTrue(BuiltInRegistries.BLOCK.getKey(block).equals(ore.rock().oreId(ore.mineral())),
                    id + " is named as the Core names ores: " + BuiltInRegistries.BLOCK.getKey(block));
            final String name = ((MaterialFormBlock) block).displayName().english();
            helper.assertTrue(name.equals(ore.name()), id + " is called " + ore.name() + "; got " + name);
            helper.assertTrue(block.defaultDestroyTime() == ore.rock().rock().defaultDestroyTime() + ORE_HARDER,
                    id + " is a little harder than its rock; got " + block.defaultDestroyTime());
            helper.assertTrue(block.defaultBlockState().is(TagKey.create(Registries.BLOCK,
                    ResourceLocation.fromNamespaceAndPath("c", "ores/" + ore.mineral().id().getPath()))),
                    id + " is filed with the mineral's other ores");
            helper.assertTrue(block.asItem() != Items.AIR, id + " has its item");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA)
    public static void ores_breakUnderAPickaxeIntoTheRawMineral(final GameTestHelper helper) {
        final ItemStack pickaxe = new ItemStack(Items.IRON_PICKAXE);
        for (final Ore ore : ORES) {
            final String id = ore.mineral().id().getPath() + " in " + ore.rock();
            final BlockState state = MaterialUses.oreBlock(ore.mineral(), ore.rock()).get().defaultBlockState();
            helper.assertTrue(state.requiresCorrectToolForDrops() && pickaxe.isCorrectToolForDrops(state),
                    id + " comes away under a pickaxe");
            helper.assertFalse(ItemStack.EMPTY.isCorrectToolForDrops(state), id + " and not by hand");
            helper.setBlock(AT, state);
            final List<ItemStack> drops = Block.getDrops(state, helper.getLevel(), helper.absolutePos(AT), null,
                    null, pickaxe);
            helper.assertTrue(drops.size() == 1 && drops.getFirst().is(MaterialUses.item(ore.mineral(),
                    Forms.RAW).get()), id + " drops the raw mineral; got " + drops);
        }
        helper.succeed();
    }

    /* An ore of the Moon or of Mars: its mineral, its rock, and its English name. */
    private record Ore(Material mineral, HostRock rock, String name) {
    }
}
