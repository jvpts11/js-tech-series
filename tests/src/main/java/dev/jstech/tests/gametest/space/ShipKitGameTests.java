/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.space;

import dev.jstech.core.content.BlockEntry;
import dev.jstech.core.multiblock.plan.IPlanMember;
import dev.jstech.space.api.ShipClass;
import dev.jstech.space.api.ShipKit;
import dev.jstech.space.api.ShipKits;
import dev.jstech.space.api.ShipPart;
import dev.jstech.space.api.ShipParts;
import dev.jstech.space.api.ShipPlace;
import dev.jstech.space.ship.ShipKitBlocks;
import dev.jstech.space.ship.ShipLayout;
import dev.jstech.space.ship.SmallShipParts;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The small starship's block kit: its 61 blocks, each with its item, its name, its place in the mod's tab and its
 * drop; every part of the catalogue built of its kit, a spine section as wide as the hull and as long as it is, the
 * hung parts set from their places on the ship; the kits using the blocks of the kit, or the vanilla blocks that work
 * aboard, and every block of the kit; and the example ships laid whole, no two of their parts wanting one place.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ShipKitGameTests {

    private static final String ARENA = "empty";
    private static final String BATCH = "jsspace_ship_kits";

    private ShipKitGameTests() {
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void kit_sixtyOneBlocksNamedInTheTabAndDroppingThemselves(final GameTestHelper helper) {
        final List<BlockEntry<Block>> blocks = ShipKitBlocks.all();
        helper.assertTrue(blocks.size() == 61, "61 blocks: " + blocks.size());
        for (final BlockEntry<Block> block : blocks) {
            helper.assertTrue(block.get().asItem() != Items.AIR, block.getId() + " has its item");
            helper.assertTrue(Language.getInstance().has(block.get().getDescriptionId()), block.getId()
                    + " is named");
            helper.assertTrue(ShipKitBlocks.SHIP_PARTS.items().contains(block.get().asItem()), block.getId()
                    + " is in the tab's row");
            final LootTable drop = helper.getLevel().getServer().reloadableRegistries()
                    .getLootTable(block.get().getLootTable());
            helper.assertTrue(drop != LootTable.EMPTY, block.getId() + " drops");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void everyPart_isBuiltOfItsKitWhereItGoes(final GameTestHelper helper) {
        final Map<ResourceLocation, ShipKit> kits = ShipKits.all(helper.getLevel());
        final Set<ResourceLocation> blocks = new HashSet<>();
        for (final BlockEntry<Block> block : ShipKitBlocks.all()) {
            blocks.add(block.getId());
        }
        final Set<ResourceLocation> used = new HashSet<>();
        for (final Map.Entry<ResourceLocation, ShipPart> entry : small(helper).entrySet()) {
            final ShipPart part = entry.getValue();
            final ShipKit kit = kits.get(entry.getKey());
            helper.assertTrue(kit != null, "a kit for " + entry.getKey());
            for (final ResourceLocation block : kit.key().values()) {
                helper.assertTrue(blocks.contains(block) || IPlanMember.keepsItself(BuiltInRegistries.BLOCK
                        .get(block).defaultBlockState()), entry.getKey() + "'s " + block + " is the kit's, or a "
                        + "vanilla block working aboard");
            }
            used.addAll(kit.key().values());
            final boolean spine = part.place() == ShipPlace.NOSE || part.place() == ShipPlace.SPINE
                    || part.place() == ShipPlace.TAIL;
            for (final ShipKit.Piece piece : kit.pieces()) {
                helper.assertTrue(spine == (piece.anchor() == ShipKit.Anchor.SECTION), entry.getKey()
                        + ": a section is set from its own front, a hung part from its place on the ship");
                if (spine) {
                    helper.assertTrue(piece.x() == -2 && piece.layers().size() == 3, entry.getKey()
                            + " stands in the hull's three layers from its left side");
                    for (final List<String> layer : piece.layers()) {
                        helper.assertTrue(layer.size() == (int) part.length() && layer.stream()
                                .allMatch(row -> row.length() == 4), entry.getKey() + " is as long as it is, "
                                + "four blocks across");
                    }
                }
            }
        }
        final Set<ResourceLocation> unused = new HashSet<>(blocks);
        unused.removeAll(used);
        helper.assertTrue(unused.isEmpty(), "every block of the kit is in a part's kit: " + unused);
        helper.succeed();
    }

    @GameTest(template = ARENA, batch = BATCH)
    public static void exampleShips_areLaidWholeWithNoTwoPartsInOnePlace(final GameTestHelper helper) {
        final Map<ResourceLocation, ShipKit> kits = ShipKits.all(helper.getLevel());
        final Map<String, List<List<String>>> ships = Map.of(
                "ORBITER", List.of(List.of("orbiter_cockpit", "airlock", "bay", "reactor_flush"),
                        List.of("twin_bells", "side_intakes", "blended_delta", "twin_canted_fins", "blisters",
                                "wheels")),
                "HAULER", List.of(List.of("cab_cockpit", "airlock", "reactor_fins", "ramp"),
                        List.of("magnetic_nozzle", "nacelle_intake", "stub_wings", "quads", "legs")),
                "COURIER", List.of(List.of("greenhouse_cockpit", "docking", "side_door", "reactor_fins"),
                        List.of("cluster", "side_intakes", "swept_wings", "single_fin", "quads", "wheels")),
                "SURVEYOR", List.of(List.of("orbiter_cockpit", "corridor", "reactor_flush", "ramp"),
                        List.of("magnetic_nozzle", "nacelle_intake", "stub_wings", "twin_canted_fins", "blisters",
                                "legs")));
        final Map<ResourceLocation, ShipPart> parts = small(helper);
        for (final Map.Entry<String, List<List<String>>> ship : ships.entrySet()) {
            final List<ShipLayout.Section> spine = new ArrayList<>();
            for (final String path : ship.getValue().get(0)) {
                final ResourceLocation id = SmallShipParts.id("small_" + path);
                spine.add(new ShipLayout.Section((int) parts.get(id).length(), kits.get(id).pieces()));
            }
            final List<List<ShipKit.Piece>> hung = new ArrayList<>();
            for (final String path : ship.getValue().get(1)) {
                hung.add(kits.get(SmallShipParts.id("small_" + path)).pieces());
            }
            final ShipLayout.Laid laid = ShipLayout.lay(spine, hung);
            helper.assertTrue(laid.clashes().isEmpty(), ship.getKey() + " has no two parts in one place: "
                    + laid.clashes());
            helper.assertTrue(laid.length() == (ship.getKey().equals("SURVEYOR") ? 14 : 15), ship.getKey()
                    + "'s spine is as long as its sections: " + laid.length());
            final Set<Integer> laidParts = new HashSet<>();
            laid.cells().values().forEach(spot -> laidParts.add(spot.part()));
            helper.assertTrue(laidParts.size() == spine.size() + hung.size(), ship.getKey() + " lays every part");
        }
        helper.succeed();
    }

    /* The small ship's parts, by id. */
    private static Map<ResourceLocation, ShipPart> small(final GameTestHelper helper) {
        final Map<ResourceLocation, ShipPart> out = new HashMap<>();
        ShipParts.all(helper.getLevel()).forEach((id, part) -> {
            if (part.shipClass() == ShipClass.SMALL) {
                out.put(id, part);
            }
        });
        return out;
    }
}
