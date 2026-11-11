/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest.computers;

import dev.jstech.computers.PeripheralLinks;
import dev.jstech.computers.abandoned.AbandonedBlocks;
import dev.jstech.computers.abandoned.AbandonedBuilding;
import dev.jstech.computers.abandoned.AbandonedBuildings;
import dev.jstech.computers.abandoned.AbandonedLoadouts;
import dev.jstech.computers.abandoned.AbandonedLoot;
import dev.jstech.computers.abandoned.AbandonedMachines;
import dev.jstech.computers.abandoned.AbandonedPropBlock;
import dev.jstech.computers.abandoned.AbandonedProps;
import dev.jstech.computers.abandoned.AbandonedSettings;
import dev.jstech.computers.abandoned.AbandonedSoftware;
import dev.jstech.computers.abandoned.HardwareBoxBlock;
import dev.jstech.computers.abandoned.HardwareBoxBlockEntity;
import dev.jstech.computers.abandoned.HardwareBoxKind;
import dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity;
import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.blockentity.MiningComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.config.ComputersServerConfig;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.bigblock.BigBlockPartBlockEntity;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.jstechseries.energy.BatteryBankBlockEntity;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.worldgen.structure.ChosenElement;
import dev.jstech.core.worldgen.structure.ITemplateChooser;
import dev.jstech.core.worldgen.structure.TemplateChoosers;
import dev.jstech.tests.JsTests;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * The abandoned buildings (computers/docs/design/survival.md, "Abandoned buildings"): every building is a structure
 * with a template for each era it may be of, working and in ruin; the chooser picks the era by the distance from the
 * world's spawn and working as often as the server says; placed, a working building's machines are whole and set to
 * come up (computers, racks, Mainframes, the mining farm's rigs with their risers and cards, the factory's machines
 * formed and the Lithography Station linked to its computer), its battery bank charged; its chests and hardware boxes draw from their tables, the main chest from the
 * one that holds a graphical system; its fittings stand as the decor block and its frames hold boxed programs.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class AbandonedBuildingGameTests {

    private static final String ARENA = "abandoned";
    private static final String SMALL = "empty";
    private static final BlockPos FOOT = new BlockPos(1, 1, 1);
    private static final int FAR = 6000;
    private static final int NEAR_LEGACY = 800;
    /* The ground layer the template stands on: every cell of the mock sits a block higher in it. */
    private static final int GROUND = 1;
    /* The factory's Lithography Station: its terminal, its peripheral port, its computer behind it. */
    private static final BlockPos LITHO_TERMINAL = new BlockPos(18, 1 + GROUND, 8);
    private static final BlockPos LITHO_PORT = new BlockPos(18, 1 + GROUND, 9);
    private static final BlockPos LITHO_COMPUTER = new BlockPos(20, GROUND, 10);
    /* The factory's Electronics Assembly Line's Crafting Interface, on the cable against its inlet. */
    private static final BlockPos INTERFACE = new BlockPos(5, GROUND, 8);
    private static final int SETTLE_TICKS = 400;

    private AbandonedBuildingGameTests() {
    }

    @GameTest(template = SMALL)
    public static void buildings_haveAStructureAndATemplateOfEachEraAndState(final GameTestHelper helper) {
        final List<String> missing = new ArrayList<>();
        for (final AbandonedBuilding building : AbandonedBuilding.values()) {
            if (helper.getLevel().registryAccess().registryOrThrow(Registries.STRUCTURE).get(ResourceKey.create(
                    Registries.STRUCTURE, AbandonedBuildings.structure(building))) == null) {
                missing.add(AbandonedBuildings.structure(building).toString());
            }
            for (final Era era : building.eras()) {
                for (final boolean working : new boolean[] {true, false}) {
                    final ResourceLocation template = AbandonedBuildings.template(building, era, working);
                    if (helper.getLevel().getStructureManager().get(template).isEmpty()) {
                        missing.add(template.toString());
                    }
                }
                for (final ResourceLocation table : List.of(AbandonedLoot.table(building, era),
                        AbandonedLoot.main(building, era))) {
                    if (lootTable(helper, table) == LootTable.EMPTY) {
                        missing.add(table.toString());
                    }
                }
            }
        }
        for (final Era era : HardwareEra.WITH_PARTS) {
            for (final HardwareBoxKind kind : HardwareBoxKind.values()) {
                if (lootTable(helper, AbandonedLoot.box(era, kind)) == LootTable.EMPTY) {
                    missing.add(AbandonedLoot.box(era, kind).toString());
                }
            }
        }
        helper.assertTrue(missing.isEmpty(), "nothing missing: " + missing);
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void chooser_picksTheEraByDistanceAndWorkingAsTheServerSays(final GameTestHelper helper) {
        final ITemplateChooser chooser = TemplateChoosers.get(AbandonedBuildings.CHOOSER).orElseThrow();
        final List<ResourceLocation> templates = List.of(AbandonedBuildings.template(
                AbandonedBuilding.ABANDONED_HOUSE, HardwareEra.VINTAGE, true));
        final BlockPos spawn = helper.getLevel().getSharedSpawnPos();
        try {
            ComputersServerConfig.FILE.set(AbandonedSettings.WORKING_CHANCE, 1.0);
            helper.assertValueEqual(chooser.choose(helper.getLevel(), spawn, RandomSource.create(1),
                    templates).template(), AbandonedBuildings.template(AbandonedBuilding.ABANDONED_HOUSE,
                    HardwareEra.VINTAGE, true), "near the spawn, the Vintage, and every one working");
            helper.assertValueEqual(chooser.choose(helper.getLevel(), spawn.offset(NEAR_LEGACY, 0, 0),
                    RandomSource.create(1), templates).template(), AbandonedBuildings.template(
                    AbandonedBuilding.ABANDONED_HOUSE, HardwareEra.LEGACY, true),
                    "past the first distance, the Legacy");
            ComputersServerConfig.FILE.set(AbandonedSettings.WORKING_CHANCE, 0.0);
            helper.assertValueEqual(chooser.choose(helper.getLevel(), spawn.offset(FAR, 0, FAR),
                    RandomSource.create(1), templates).template(), AbandonedBuildings.template(
                    AbandonedBuilding.ABANDONED_HOUSE, HardwareEra.LEGACY, false),
                    "far out, the nearest era the house may be of, and none working");
        } finally {
            ComputersServerConfig.FILE.set(AbandonedSettings.WORKING_CHANCE, 0.2);
        }
        helper.succeed();
    }

    @GameTest(template = SMALL)
    public static void software_everyBuildingOfEveryEraHoldsAWayToAGraphicalSystem(final GameTestHelper helper) {
        final List<String> without = new ArrayList<>();
        for (final Era era : HardwareEra.WITH_PARTS) {
            final List<ItemStack> installers = AbandonedSoftware.graphicalSystem(era);
            if (installers.isEmpty() || installers.stream().anyMatch(ItemStack::isEmpty)) {
                without.add(era.path());
            }
        }
        helper.assertTrue(without.isEmpty(), "every era has its installers: " + without);
        helper.assertValueEqual(AbandonedSoftware.graphicalSystem(HardwareEra.VINTAGE).size(), 2,
                "the Vintage's way is UNIX System V and CDE for it");
        helper.assertTrue(AbandonedSoftware.programs(AbandonedBuilding.MIDSOFT_OFFICE, HardwareEra.STANDARD).stream()
                .allMatch(program -> program.house().name().equals("Midsoft")), "a Midsoft office only Midsoft's");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void working_computersAreWholeTheirSystemInstalledAndSetToStart(final GameTestHelper helper) {
        place(helper, AbandonedBuilding.ABANDONED_HOUSE, HardwareEra.VINTAGE, true);
        final List<AbstractSmallComputerBlockEntity> computers = found(helper, AbstractSmallComputerBlockEntity.class);
        helper.assertTrue(!computers.isEmpty(), "the house has its computer");
        for (final AbstractSmallComputerBlockEntity computer : computers) {
            helper.assertTrue(computer.buildValid(), "whole: its parts make a computer");
            helper.assertValueEqual(computer.installedOsId(), AbandonedLoadouts.system(HardwareEra.VINTAGE),
                    "its era's system installed");
            helper.assertTrue(computer.isAutoStart(), "set to come up as soon as it has energy");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void ruin_chestsDrawFromTheBuildingsTablesOfItsEra(final GameTestHelper helper) {
        place(helper, AbandonedBuilding.ABANDONED_HOUSE, HardwareEra.LEGACY, false);
        final List<ChestBlockEntity> chests = found(helper, ChestBlockEntity.class);
        helper.assertTrue(!chests.isEmpty(), "the house has its chests");
        int main = 0;
        for (final ChestBlockEntity chest : chests) {
            final ResourceLocation table = chest.getLootTable() == null ? null : chest.getLootTable().location();
            if (AbandonedLoot.main(AbandonedBuilding.ABANDONED_HOUSE, HardwareEra.LEGACY).equals(table)) {
                main++;
            } else {
                helper.assertValueEqual(table, AbandonedLoot.table(AbandonedBuilding.ABANDONED_HOUSE,
                        HardwareEra.LEGACY), "each other chest draws from the house's table of the Legacy");
            }
        }
        helper.assertValueEqual(main, 1, "one main chest, with the way to a graphical system");
        for (final AbstractSmallComputerBlockEntity computer : found(helper, AbstractSmallComputerBlockEntity.class)) {
            helper.assertTrue(!computer.isAutoStart(), "a ruin's computer is set to nothing");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = SETTLE_TICKS)
    public static void factory_machinesFormedTheStationLinkedToItsComputer(final GameTestHelper helper) {
        place(helper, AbandonedBuilding.ELECTRONICS_FACTORY, HardwareEra.STANDARD, true);
        helper.assertTrue(helper.getBlockEntity(FOOT.offset(INTERFACE)) instanceof CableBlockEntity cable
                && cable.parts().has(Direction.WEST), "the Crafting Interface on its cable, against the inlet");
        helper.assertTrue(!found(helper, BatteryBankBlockEntity.class).isEmpty()
                && found(helper, BatteryBankBlockEntity.class).getFirst().store().stored() > 0,
                "the working factory's battery bank is charged");
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getBlockEntity(FOOT.offset(LITHO_TERMINAL)) instanceof ChainMachineBlockEntity
                    station && station.formed(), "the Lithography Station stands formed");
            final OptionalLong owner = PeripheralLinks.discoverOwner(helper.getLevel(),
                    helper.absolutePos(FOOT.offset(LITHO_PORT)).asLong());
            helper.assertTrue(owner.isPresent() && owner.getAsLong()
                            == helper.absolutePos(FOOT.offset(LITHO_COMPUTER)).asLong(),
                    "its peripheral port reaches its computer of the era before along the cable");
        });
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void datacenter_racksFilledMainframeWhole(final GameTestHelper helper) {
        place(helper, AbandonedBuilding.DATACENTER, HardwareEra.STANDARD, true);
        final List<ServerRackBlockEntity> racks = found(helper, ServerRackBlockEntity.class);
        helper.assertTrue(!racks.isEmpty(), "the datacenter has its racks");
        helper.assertTrue(!found(helper, BigBlockPartBlockEntity.class).isEmpty(), "their parts raised");
        for (final ServerRackBlockEntity rack : racks) {
            final int last = ServerRackBlockEntity.CAPACITY_U - 1;
            helper.assertValueEqual(rack.getServers().getStackInSlot(0).getItem(),
                    AbandonedLoadouts.rackUps(HardwareEra.STANDARD).getItem(), "its supply unit at its foot");
            helper.assertValueEqual(rack.getServers().getStackInSlot(last).getItem(),
                    AbandonedLoadouts.kvmSwitch(HardwareEra.STANDARD).getItem(), "its switch at its top");
            final ItemStack server = rack.getServers().getStackInSlot(1);
            helper.assertTrue(server.getItem() instanceof ServerItem
                    && ServerItem.hardware(server).nonEmptyItems().iterator().hasNext(), "a whole server between");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void miningFarm_rigsStandWholeOnTheirShelves(final GameTestHelper helper) {
        place(helper, AbandonedBuilding.MINING_FARM, HardwareEra.STANDARD, true);
        final List<MiningComputerBlockEntity> rigs = found(helper, MiningComputerBlockEntity.class);
        helper.assertTrue(!rigs.isEmpty(), "the mining farm has its Mining Computers");
        helper.assertTrue(!found(helper, BigBlockPartBlockEntity.class).isEmpty(), "their second blocks raised");
        for (final MiningComputerBlockEntity rig : rigs) {
            helper.assertValueEqual(rig.getHardware().getStackInSlot(MiningComputerBlockEntity.MOTHERBOARD_SLOT)
                    .getItem(), AbandonedLoadouts.rigParts(HardwareEra.STANDARD).getFirst().getItem(),
                    "its era's mining board");
            int risers = 0;
            for (int i = 0; i < rig.riserCount(); i++) {
                risers += rig.hasRiser(i) ? 1 : 0;
            }
            helper.assertValueEqual(risers, 6, "a riser in each of its six slots");
            helper.assertValueEqual(rig.seatedCards(), 6, "and a card standing on each");
            helper.assertValueEqual(rig.installedOsId(), AbandonedLoadouts.system(HardwareEra.STANDARD),
                    "its era's system installed");
            helper.assertTrue(rig.isAutoStart(), "set to come up as soon as it has energy");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void bank_mainframeRunsTheNetworksSystem(final GameTestHelper helper) {
        place(helper, AbandonedBuilding.BANK, HardwareEra.STANDARD, true);
        final List<AbstractCabinetComputerBlockEntity> mainframes = found(helper,
                AbstractCabinetComputerBlockEntity.class);
        helper.assertTrue(!mainframes.isEmpty(), "the bank has its Mainframe in the vault");
        for (final AbstractCabinetComputerBlockEntity mainframe : mainframes) {
            helper.assertValueEqual(mainframe.installedOsId(), AbandonedLoadouts.networkSystem(),
                    "the network's system installed");
            helper.assertTrue(mainframe.isAutoStart(), "set to come up as soon as it has energy");
        }
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void shop_hardwareBoxesDrawFromTheirTablesAndStayOpenOnceOpened(final GameTestHelper helper) {
        place(helper, AbandonedBuilding.COMPUTER_SHOP, HardwareEra.LEGACY, true);
        final List<HardwareBoxBlockEntity> boxes = found(helper, HardwareBoxBlockEntity.class);
        helper.assertTrue(!boxes.isEmpty(), "the shop has its hardware boxes");
        for (final HardwareBoxBlockEntity box : boxes) {
            final BlockState state = box.getBlockState();
            helper.assertValueEqual(state.getValue(HardwareBoxBlock.ERA), 1, "printed with the Legacy's machine");
            if (!state.getValue(HardwareBoxBlock.OPEN)) {
                helper.assertTrue(box.getLootTable() != null, "a closed box draws from its table");
            }
        }
        final HardwareBoxBlockEntity box = boxes.getFirst();
        box.startOpen(helper.makeMockPlayer(GameType.SURVIVAL));
        helper.assertTrue(box.getBlockState().getValue(HardwareBoxBlock.OPEN), "opened once, it stays open");
        helper.succeed();
    }

    @GameTest(template = ARENA, timeoutTicks = 200)
    public static void props_standAsTheDecorBlockAndTheLightsLight(final GameTestHelper helper) {
        place(helper, AbandonedBuilding.SCHOOL, HardwareEra.VINTAGE, true);
        final List<BlockState> props = new ArrayList<>();
        helper.forEveryBlockInStructure(pos -> {
            final BlockState state = helper.getBlockState(pos);
            if (state.is(AbandonedBlocks.PROP.get())) {
                props.add(state);
            }
        });
        helper.assertTrue(!props.isEmpty(), "the school's fittings stand as the decor block");
        helper.assertTrue(props.stream().anyMatch(state -> state.getLightEmission() > 0),
                "its ceiling lights light the room");
        helper.assertTrue(props.stream().allMatch(state -> !AbandonedProps.shape(state.getValue(
                AbandonedPropBlock.PART), state.getValue(AbandonedPropBlock.FACING)).isEmpty()), "each has its shape");
        final List<ItemFrame> frames = helper.getLevel().getEntitiesOfClass(ItemFrame.class,
                new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(48));
        helper.assertTrue(!frames.isEmpty() && frames.stream().allMatch(frame -> frame.getItem().has(
                ComputingComponents.MEDIA_PAYLOAD.get())), "its frames hold boxed programs");
        helper.succeed();
    }

    /* {@code building}'s template of {@code era} placed at the arena's foot, as the structure places it. */
    private static void place(final GameTestHelper helper, final AbandonedBuilding building, final Era era,
                              final boolean working) {
        final ResourceLocation id = AbandonedBuildings.template(building, era, working);
        final StructureTemplate template = helper.getLevel().getStructureManager().get(id).orElseThrow();
        final BlockPos at = helper.absolutePos(FOOT);
        ChosenElement.placeTemplate(id, template, helper.getLevel(), at, at, new StructurePlaceSettings()
                .addProcessor(new AbandonedMachines(building, era, working)), RandomSource.create(1));
    }

    /* Every block entity of {@code type} in the arena. */
    private static <T> List<T> found(final GameTestHelper helper, final Class<T> type) {
        final List<T> out = new ArrayList<>();
        helper.forEveryBlockInStructure(pos -> {
            final BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(pos));
            if (type.isInstance(entity)) {
                out.add(type.cast(entity));
            }
        });
        return out;
    }

    private static LootTable lootTable(final GameTestHelper helper, final ResourceLocation id) {
        return helper.getLevel().getServer().reloadableRegistries().getLootTable(ResourceKey.create(
                Registries.LOOT_TABLE, id));
    }
}
