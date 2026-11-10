/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.datagen;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.abandoned.AbandonedBuilding;
import dev.jstech.computers.abandoned.AbandonedLoadouts;
import dev.jstech.computers.abandoned.AbandonedLoot;
import dev.jstech.computers.abandoned.AbandonedSoftware;
import dev.jstech.computers.abandoned.HardwareBoxKind;
import dev.jstech.computers.hardware.GpuSpec;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.media.InstallMedia;
import dev.jstech.computers.os.media.InstallerStacks;
import dev.jstech.computers.registry.HardwareRegistry;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

/**
 * The loot of the abandoned buildings (computers/docs/design/survival.md, "Abandoned buildings"), every chest drawing
 * 4 to 8 things from its building's table of its era: the era's parts, its blank media and peripheral cables, what the
 * building is known for (a library's books and manuals, an exchange's routers and cables, a studio's top graphics card,
 * a factory's wafers and chips, a supercomputing centre's co-processors), and its boxed software, a house's office only
 * its house's. A building's main chest also always holds its way to a graphical system. A hardware box holds what it
 * is printed with: a few of its era's computer parts, or one of its era's graphics cards.
 */
public record AbandonedLootTables(HolderLookup.Provider registries) implements LootTableSubProvider {

    private static final int THINGS_MIN = 4;
    private static final int THINGS_MAX = 8;
    private static final int SOFTWARE_MAX = 3;
    private static final int CABLES_MIN = 2;
    private static final int CABLES_MAX = 8;
    private static final int PART_WEIGHT = 2;
    private static final int BOX_PARTS_MIN = 2;
    private static final int BOX_PARTS_MAX = 4;
    private static final Map<Era, String> PERIPHERAL_CABLE = Map.of(HardwareEra.VINTAGE, "vintage_peripheral_cable",
            HardwareEra.LEGACY, "legacy_peripheral_cable", HardwareEra.TRANSITION, "transition_peripheral_cable",
            HardwareEra.STANDARD, "peripheral_cable", HardwareEra.ADVANCED, "advanced_peripheral_cable");
    private static final Map<Era, String> NETWORK_CABLE = Map.of(HardwareEra.VINTAGE, "thin_coax_cable",
            HardwareEra.LEGACY, "ethernet_cable", HardwareEra.TRANSITION, "cat5e_cable", HardwareEra.STANDARD,
            "gigabit_cable", HardwareEra.ADVANCED, "cat6a_cable");
    private static final Map<Era, String> ROUTER = Map.of(HardwareEra.VINTAGE, "vintage_router",
            HardwareEra.LEGACY, "personal_router", HardwareEra.TRANSITION, "transition_router",
            HardwareEra.STANDARD, "standard_router", HardwareEra.ADVANCED, "advanced_router");
    private static final List<String> PHI = List.of("phi_5100", "phi_7120", "phi_7290", "phi_9000");

    @Override
    public void generate(final BiConsumer<ResourceKey<LootTable>, LootTable.Builder> out) {
        for (final AbandonedBuilding building : AbandonedBuilding.values()) {
            for (final Era era : building.eras()) {
                out.accept(key(AbandonedLoot.table(building, era)), chest(building, era, false));
                out.accept(key(AbandonedLoot.main(building, era)), chest(building, era, true));
            }
        }
        for (final Era era : HardwareEra.WITH_PARTS) {
            out.accept(key(AbandonedLoot.box(era, HardwareBoxKind.COMPUTER)), computerBox(era));
            out.accept(key(AbandonedLoot.box(era, HardwareBoxKind.GRAPHICS_CARD)), cardBox(era));
        }
    }

    /* A chest's table: its things, its software, and in the main chest the way to a graphical system. */
    private LootTable.Builder chest(final AbandonedBuilding building, final Era era, final boolean main) {
        final List<ProgramSpec> programs = AbandonedSoftware.programs(building, era);
        final LootPool.Builder things = LootPool.lootPool().setRolls(UniformGenerator.between(
                programs.isEmpty() ? THINGS_MIN : THINGS_MIN - 1, programs.isEmpty() ? THINGS_MAX
                        : THINGS_MAX - SOFTWARE_MAX));
        for (final ItemStack part : AbandonedLoadouts.parts(era)) {
            things.add(stack(part).setWeight(PART_WEIGHT));
        }
        things.add(LootItem.lootTableItem(InstallerStacks.blank(InstallMedia.forProgram(era, ProgramKind.APP)))
                .setWeight(PART_WEIGHT));
        things.add(counted(item(PERIPHERAL_CABLE.get(HardwareEra.withParts(era))), CABLES_MIN, CABLES_MAX));
        knownFor(things, building, era);
        final LootTable.Builder table = LootTable.lootTable().withPool(things);
        if (!programs.isEmpty()) {
            final LootPool.Builder software = LootPool.lootPool().setRolls(UniformGenerator.between(1,
                    SOFTWARE_MAX));
            for (final ProgramSpec program : programs) {
                software.add(stack(AbandonedSoftware.boxed(program)));
            }
            table.withPool(software);
        }
        if (main) {
            for (final ItemStack installer : AbandonedSoftware.graphicalSystem(era)) {
                if (!installer.isEmpty()) {
                    table.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1)).add(stack(installer)));
                }
            }
        }
        return table;
    }

    /* What the building is known for, from the spec's table of the buildings. */
    private void knownFor(final LootPool.Builder things, final AbandonedBuilding building, final Era era) {
        final Era parts = HardwareEra.withParts(era);
        switch (building) {
            case LIBRARY -> {
                things.add(counted(Items.BOOK, 1, 3).setWeight(PART_WEIGHT * 2));
                things.add(LootItem.lootTableItem(item("guide_to_operations")));
            }
            case UNIVERSITY_LAB -> things.add(counted(Items.BOOK, 1, 2).setWeight(PART_WEIGHT));
            case TELEPHONE_EXCHANGE, DATACENTER -> {
                things.add(LootItem.lootTableItem(item(ROUTER.get(parts))).setWeight(PART_WEIGHT));
                things.add(counted(item(NETWORK_CABLE.get(parts)), CABLES_MIN, CABLES_MAX).setWeight(PART_WEIGHT));
                things.add(LootItem.lootTableItem(AbandonedLoadouts.server(parts).getItem()));
            }
            case GAME_STUDIO, MINING_FARM -> things.add(LootItem.lootTableItem(topCard(parts))
                    .setWeight(PART_WEIGHT));
            case ELECTRONICS_FACTORY -> {
                things.add(counted(item("finished_wafer_" + parts.path()), 1, 3).setWeight(PART_WEIGHT));
                things.add(counted(item("memory_chip_" + parts.path()), 1, 4).setWeight(PART_WEIGHT));
                things.add(counted(item("controller_chip_" + parts.path()), 1, 4).setWeight(PART_WEIGHT));
            }
            case SUPERCOMPUTING_CENTRE -> {
                for (final String phi : PHI) {
                    things.add(LootItem.lootTableItem(item(phi)));
                }
            }
            default -> {
                // The era's parts, media and cables are what it holds.
            }
        }
    }

    /* A computer's box: a few of its era's computer's parts. */
    private LootTable.Builder computerBox(final Era era) {
        final LootPool.Builder parts = LootPool.lootPool().setRolls(UniformGenerator.between(BOX_PARTS_MIN,
                BOX_PARTS_MAX));
        for (final ItemStack part : AbandonedLoadouts.parts(era)) {
            parts.add(stack(part));
        }
        return LootTable.lootTable().withPool(parts);
    }

    /* A graphics card's box: one of its era's graphics cards. */
    private LootTable.Builder cardBox(final Era era) {
        final LootPool.Builder card = LootPool.lootPool().setRolls(ConstantValue.exactly(1));
        for (final Item gpu : cards(era)) {
            card.add(LootItem.lootTableItem(gpu));
        }
        return LootTable.lootTable().withPool(card);
    }

    /* The graphics cards of {@code era}, by id. */
    private static List<Item> cards(final Era era) {
        final List<Item> out = new ArrayList<>();
        for (final Item item : HardwareRegistry.parts()) {
            if (HardwareRegistry.gpu(new ItemStack(item)) instanceof GpuSpec gpu
                    && HardwareEra.withParts(gpu.era()) == HardwareEra.withParts(era)) {
                out.add(item);
            }
        }
        out.sort(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).toString()));
        return out;
    }

    /* The era's top graphics card: the most cores at the highest clock. */
    private static Item topCard(final Era era) {
        return cards(era).stream().max(Comparator.comparingLong(item -> {
            final GpuSpec gpu = HardwareRegistry.gpu(new ItemStack(item));
            return (long) gpu.cores() * gpu.clockMhz();
        })).orElseThrow(() -> new IllegalStateException("no graphics card of the " + era.path()));
    }

    /* A stack as the loot gives it: its item, with every component it carries beyond its item's own. */
    private static LootPoolSingletonContainer.Builder<?> stack(final ItemStack stack) {
        final LootPoolSingletonContainer.Builder<?> entry = LootItem.lootTableItem(stack.getItem());
        for (final TypedDataComponent<?> component : stack.getComponents()) {
            if (!component.value().equals(stack.getItem().components().get(component.type()))) {
                with(entry, component);
            }
        }
        return entry;
    }

    private static <T> void with(final LootPoolSingletonContainer.Builder<?> entry,
                                 final TypedDataComponent<T> component) {
        entry.apply(SetComponentsFunction.setComponent(component.type(), component.value()));
    }

    private static LootPoolSingletonContainer.Builder<?> counted(final Item item, final int min, final int max) {
        return LootItem.lootTableItem(item).apply(SetItemCountFunction.setCount(UniformGenerator.between(min,
                max)));
    }

    private static Item item(final String path) {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, path));
    }

    private static ResourceKey<LootTable> key(final ResourceLocation id) {
        return ResourceKey.create(Registries.LOOT_TABLE, id);
    }
}
