/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.abandoned.AbandonedBuilding;
import dev.jstech.computers.abandoned.AbandonedLoadouts;
import dev.jstech.computers.abandoned.AbandonedMachines;
import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.SoftwareHouse;
import dev.jstech.computers.villager.ComputerTrades;
import dev.jstech.computers.villager.ComputerVillagers;
import dev.jstech.computers.villager.SoftwareSources;
import dev.jstech.computers.villager.VillageShops;
import dev.jstech.computers.villager.VillagerSettings;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.text.Text;
import dev.jstech.core.worldgen.structure.ChosenElement;
import dev.jstech.tests.JsTests;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The two professions of the computers (computers/docs/design/survival.md, "Villagers"): each job site is the point of
 * interest of its profession, which villagers take while the server lets them; each level sells the things of its era
 * at the era's base times their factor, at most 64 emeralds, a system three times before it restocks; and every kind
 * of village has the computer shop among its houses, its demo computer running MC-DOS.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class ComputerVillagerGameTests {

    private static final String SMALL = "empty";
    private static final String ARENA = "abandoned";
    private static final int LEVELS = 5;
    private static final int MOST = 64;
    private static final int SYSTEM_USES = 3;
    private static final int TRIES = 20;

    private ComputerVillagerGameTests() {
    }

    @GameTest(template = SMALL)
    public static void jobSites_areThePointsOfInterestTheirProfessionsTake(final GameTestHelper helper) {
        final Optional<Holder<PoiType>> desk = PoiTypes.forState(
                ComputerVillagers.PROGRAMMERS_DESK.get().defaultBlockState());
        final Optional<Holder<PoiType>> bench = PoiTypes.forState(
                ComputerVillagers.REPAIR_BENCH.get().defaultBlockState());
        helper.assertTrue(desk.isPresent() && desk.get().is(ComputerVillagers.PROGRAMMER_POI.getKey()),
                "the Programmer's Desk is the Programmer's job site");
        helper.assertTrue(bench.isPresent() && bench.get().is(ComputerVillagers.IT_TECHNICIAN_POI.getKey()),
                "the Repair Bench is the IT Technician's");
        final VillagerProfession programmer = ComputerVillagers.PROGRAMMER.get();
        helper.assertTrue(programmer.acquirableJobSite().test(desk.get()), "a villager takes the desk");
        helper.assertTrue(!programmer.acquirableJobSite().test(bench.get()), "and not the bench");
        try {
            ComputersServerConfig.FILE.set(VillagerSettings.PROGRAMMER, false);
            helper.assertTrue(!programmer.acquirableJobSite().test(desk.get()),
                    "with the server's setting off, no villager takes it");
        } finally {
            ComputersServerConfig.FILE.set(VillagerSettings.PROGRAMMER, true);
        }
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void trades_eachLevelSellsItsErasThingsAtTheSpecsPrices(final GameTestHelper helper) {
        final Villager villager = helper.spawn(EntityType.VILLAGER, new BlockPos(1, 2, 1));
        final List<String> wrong = new ArrayList<>();
        int systems = 0;
        for (final VillagerProfession profession : List.of(ComputerVillagers.PROGRAMMER.get(),
                ComputerVillagers.IT_TECHNICIAN.get())) {
            villager.setVillagerData(new VillagerData(villager.getVillagerData().getType(), profession, 1));
            final Int2ObjectMap<VillagerTrades.ItemListing[]> byLevel = VillagerTrades.TRADES.get(profession);
            if (byLevel == null) {
                wrong.add("no trades for " + profession);
                continue;
            }
            for (int level = 1; level <= LEVELS; level++) {
                final VillagerTrades.ItemListing[] listings = byLevel.get(level);
                if (listings == null || listings.length < 2) {
                    wrong.add(profession + " level " + level + " has fewer than two trades");
                    continue;
                }
                for (final VillagerTrades.ItemListing listing : listings) {
                    for (int i = 0; i < TRIES; i++) {
                        final MerchantOffer offer = listing.getOffer(villager, RandomSource.create(level * 31L + i));
                        if (offer == null) {
                            continue;
                        }
                        if (offer.getResult().isEmpty() || offer.getCostA().isEmpty()) {
                            wrong.add(profession + " level " + level + " offers nothing");
                        }
                        if (offer.getCostA().is(Items.EMERALD) && offer.getCostA().getCount() > MOST) {
                            wrong.add(profession + " level " + level + " asks more than 64 emeralds");
                        }
                        if (offer.getMaxUses() == SYSTEM_USES) {
                            systems++;
                        }
                    }
                }
            }
        }
        helper.assertTrue(wrong.isEmpty(), "every trade as the spec sets it: " + wrong);
        helper.assertTrue(systems > 0, "a system sells three times before it restocks");
        helper.assertValueEqual(ComputerTrades.price(1, 1.0), 4, "a Vintage program costs the Vintage's base");
        helper.assertValueEqual(ComputerTrades.price(5, 2.0), 64, "an Advanced system costs twice 32, at most 64");
        helper.assertValueEqual(ComputerTrades.price(3, 0.25), 3, "eight Transition cables a quarter of its base");
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void sources_sayWhoSellsAProgramAndWhereItIsFound(final GameTestHelper helper) {
        final ProgramSpec midsoft = OsRegistry.programs().stream().filter(program -> program.installable()
                && program.era() == HardwareEra.STANDARD && program.house().equals(SoftwareHouse.MIDSOFT)
                && program.kind() != ProgramKind.DESKTOP_ENVIRONMENT).findFirst().orElseThrow();
        final List<Text> lines = SoftwareSources.of(midsoft);
        helper.assertTrue(lines.contains(SoftwareSources.SOLD_BY.with(4)), "sold by the Programmer of level 4");
        helper.assertTrue(lines.contains(SoftwareSources.MIDSOFT_OFFICE.text()), "found in a Midsoft office");
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void villages_haveTheComputerShopAmongTheirHouses(final GameTestHelper helper) {
        for (final String kind : VillageShops.KINDS) {
            final StructureTemplatePool houses = helper.getLevel().registryAccess()
                    .registryOrThrow(Registries.TEMPLATE_POOL).getOrThrow(VillageShops.houses(kind));
            helper.assertTrue(houses.getShuffledTemplates(RandomSource.create(0)).stream().anyMatch(element ->
                    element.toString().contains(VillageShops.template(kind).toString())),
                    "the " + kind + " village may build its computer shop");
            helper.assertTrue(helper.getLevel().getStructureManager().get(VillageShops.template(kind)).isPresent(),
                    "the " + kind + " shop's template");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void shop_demoComputerRunsMcDosAndHasBothJobSites(final GameTestHelper helper) {
        final ResourceLocation id = VillageShops.template("plains");
        final StructureTemplate template = helper.getLevel().getStructureManager().get(id).orElseThrow();
        final BlockPos at = helper.absolutePos(new BlockPos(1, 1, 1));
        ChosenElement.placeTemplate(id, template, helper.getLevel(), at, at, new StructurePlaceSettings()
                .addProcessor(new AbandonedMachines(AbandonedBuilding.COMPUTER_SHOP, HardwareEra.VINTAGE, true)),
                RandomSource.create(1));
        final List<AbstractSmallComputerBlockEntity> computers = new ArrayList<>();
        final int[] sites = {0, 0};
        helper.forEveryBlockInStructure(pos -> {
            final BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            if (entity instanceof AbstractSmallComputerBlockEntity computer) {
                computers.add(computer);
            }
            if (helper.getBlockState(pos).is(ComputerVillagers.PROGRAMMERS_DESK.get())) {
                sites[0]++;
            }
            if (helper.getBlockState(pos).is(ComputerVillagers.REPAIR_BENCH.get())) {
                sites[1]++;
            }
        });
        helper.assertTrue(!computers.isEmpty(), "the shop has its demo computer");
        for (final AbstractSmallComputerBlockEntity computer : computers) {
            helper.assertValueEqual(computer.installedOsId(), AbandonedLoadouts.system(HardwareEra.VINTAGE),
                    "running MC-DOS");
            helper.assertTrue(computer.isAutoStart(), "set to come up");
        }
        helper.assertTrue(sites[0] == 1 && sites[1] == 1, "both job sites in the shop");
        helper.succeed();
    }
}
