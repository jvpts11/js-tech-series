/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.abandoned;

import dev.jstech.computers.block.AbstractSmallComputerBlock;
import dev.jstech.computers.block.MainframeBlock;
import dev.jstech.computers.block.MiningComputerBlock;
import dev.jstech.computers.block.ServerRackBlock;
import dev.jstech.computers.block.part.ComputingParts;
import dev.jstech.computers.blockentity.AbstractCabinetComputerBlockEntity;
import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.blockentity.MiningComputerBlockEntity;
import dev.jstech.computers.blockentity.ServerRackBlockEntity;
import dev.jstech.computers.chain.machine.ComputerKit;
import dev.jstech.computers.hardware.RigGeometry;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.cable.CableBlockEntity;
import dev.jstech.core.cable.CableType;
import dev.jstech.core.cable.CoreCables;
import dev.jstech.core.jstechseries.energy.BatteryBankBlock;
import dev.jstech.core.jstechseries.energy.BatteryBankBlockEntity;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * What the machines of an abandoned building hold as it is placed (computers/docs/design/survival.md, "Abandoned
 * buildings"), each made as a player makes it, in a machine that stands nowhere yet and is then saved into the place:
 * a computer and a Mainframe their era's parts, a Mining Computer its parts, risers and cards, a rack its supply at its
 * foot, its switch at its top and servers between, a battery bank its charge, a cable its wires and the part on it.
 * In a working building the machines are whole, their systems installed, set to come up as soon as they have energy;
 * in a ruin every part is there only with an even chance, the board first and what sits on it only with it, a third of
 * a rack's rows empty, its drives gone, and a third of a rig's cards gone.
 */
final class AbandonedFills {

    /* Where the board comes among a machine's parts: first, as a player seats it. */
    private static final int BOARD = 0;
    /* A rack's rows a supercomputer node fills: every other one, a node standing two high. */
    private static final int NODE_ROWS = 2;
    /* The share of a ruined rack's rows that stand empty. */
    private static final float EMPTY_ROW = 1.0F / 3.0F;
    /* Where a rig's processor, memory and supply come among its parts, after its board. */
    private static final int RIG_CPU = 1;
    private static final int RIG_RAM = 2;
    private static final int RIG_PSU = 3;
    /* The share of a ruined rig's cards that are gone, as the mining farm draws them. */
    private static final float GONE_CARD = 1.0F / 3.0F;
    private static final String PHI_STANDARD = "phi_5100";
    private static final String PHI_ADVANCED = "phi_9000";

    private AbandonedFills() {
    }

    /*
     * A computer of the era: whole and set to come up in a working building, its parts only by chance in a ruin, built
     * as a player builds one, by the computer's own rules.
     */
    static @Nullable CompoundTag computer(final LevelReader level, final StructureTemplate.StructureBlockInfo placed,
                                          final CompoundTag mark, final Era era, final boolean working,
                                          final RandomSource random) {
        if (!(placed.state().getBlock() instanceof AbstractSmallComputerBlock<?> block)
                || !(block.newBlockEntity(placed.pos(), placed.state())
                instanceof AbstractSmallComputerBlockEntity computer)) {
            return null;
        }
        final Era own = eraOf(mark, era);
        final boolean broken = mark.getBoolean("broken") || !working;
        final ComputerKit.Built built = ComputerKit.assemble(new ItemStack(block),
                broken ? leftIn(AbandonedLoadouts.parts(own), random) : AbandonedLoadouts.parts(own));
        if (!built.computer().isEmpty()) {
            final ItemStack item = built.computer();
            item.remove(ComputingComponents.NEVER_STARTED.get());
            computer.applyComponentsFromItemStack(item);
        }
        final boolean on = working && mark.getBoolean("on") && !broken;
        if (on) {
            computer.installOs(AbandonedLoadouts.system(own));
        }
        final CompoundTag tag = computer.saveWithFullMetadata(level.registryAccess());
        if (on) {
            switchedOn(tag);
        }
        return tag;
    }

    /* A Mainframe of the era: whole, running the network's system in a working building; half there in a ruin. */
    static @Nullable CompoundTag mainframe(final LevelReader level, final StructureTemplate.StructureBlockInfo placed,
                                           final Era era, final boolean working, final RandomSource random) {
        if (!(placed.state().getBlock() instanceof MainframeBlock block)
                || !(block.newBlockEntity(placed.pos(), placed.state())
                instanceof AbstractCabinetComputerBlockEntity mainframe)) {
            return null;
        }
        final List<ItemStack> parts = working ? AbandonedLoadouts.mainframeParts(era)
                : leftIn(AbandonedLoadouts.mainframeParts(era), random);
        final ItemStackHandler inventory = mainframe.getInventory();
        for (final ItemStack part : parts) {
            seat(inventory, part);
        }
        if (working) {
            inventory.setStackInSlot(AbstractCabinetComputerBlockEntity.DISK_SLOTS_START, AbandonedLoadouts.disk(era));
            mainframe.installOs(AbandonedLoadouts.networkSystem());
        }
        final CompoundTag tag = mainframe.saveWithFullMetadata(level.registryAccess());
        if (working) {
            switchedOn(tag);
        }
        return tag;
    }

    /*
     * A rack of the era: its supply unit at its foot, its switch at its top, servers between, each with its era's disk;
     * a supercomputer's rack its nodes, each with its co-processor. A ruin's rows a third empty, its servers half
     * there and their disks gone.
     */
    static @Nullable CompoundTag rack(final LevelReader level, final StructureTemplate.StructureBlockInfo placed,
                                      final CompoundTag mark, final Era era, final boolean working,
                                      final RandomSource random) {
        if (!(placed.state().getBlock() instanceof ServerRackBlock block)
                || !(block.newBlockEntity(placed.pos(), placed.state()) instanceof ServerRackBlockEntity rack)) {
            return null;
        }
        final Era own = eraOf(mark, era);
        final boolean broken = mark.getBoolean("broken") || !working;
        final ItemStackHandler rows = rack.getServers();
        final boolean nodes = "supercomputer".equals(mark.getString("role"));
        final int last = ServerRackBlockEntity.CAPACITY_U - 1;
        for (int row = 0; row <= last; row += nodes ? NODE_ROWS : 1) {
            if (broken && random.nextFloat() < EMPTY_ROW) {
                continue;
            }
            final ItemStack unit = nodes ? node(own, broken, random)
                    : row == 0 ? AbandonedLoadouts.rackUps(own)
                    : row == last ? AbandonedLoadouts.kvmSwitch(own) : server(own, broken, random);
            if (rows.insertItem(row, unit, false).isEmpty() && !broken && (nodes || row != 0 && row != last)) {
                rack.insertDrive(row, AbandonedLoadouts.disk(own));
            }
        }
        return rack.saveWithFullMetadata(level.registryAccess());
    }

    /*
     * A Mining Computer of the era, as the mining farm draws it: its mining board, processor, memory, supply and disk,
     * a riser in every x1 slot and a graphics card standing on each; a ruin has lost a third of its cards. In a working
     * building it runs its era's system, set to come up as soon as it has energy.
     */
    static @Nullable CompoundTag rig(final LevelReader level, final StructureTemplate.StructureBlockInfo placed,
                                     final CompoundTag mark, final Era era, final boolean working,
                                     final RandomSource random) {
        if (!(placed.state().getBlock() instanceof MiningComputerBlock block)
                || !(block.newBlockEntity(placed.pos(), placed.state()) instanceof MiningComputerBlockEntity rig)) {
            return null;
        }
        final Era own = eraOf(mark, era);
        final boolean broken = mark.getBoolean("broken") || !working;
        final List<ItemStack> parts = AbandonedLoadouts.rigParts(own);
        final ItemStackHandler hardware = rig.getHardware();
        hardware.setStackInSlot(MiningComputerBlockEntity.MOTHERBOARD_SLOT, parts.get(BOARD));
        hardware.setStackInSlot(MiningComputerBlockEntity.CPU_SLOT, parts.get(RIG_CPU));
        for (int i = 0; i < MiningComputerBlockEntity.RAM_SLOTS; i++) {
            hardware.setStackInSlot(MiningComputerBlockEntity.RAM_SLOTS_START + i, parts.get(RIG_RAM).copy());
        }
        hardware.setStackInSlot(MiningComputerBlockEntity.PSU_SLOT, parts.get(RIG_PSU));
        hardware.setStackInSlot(MiningComputerBlockEntity.DISK_SLOT, AbandonedLoadouts.disk(own));
        for (int i = 0; i < RigGeometry.of(own).risers(); i++) {
            rig.risers().setStackInSlot(i, AbandonedLoadouts.riser(own));
            if (!broken || random.nextFloat() >= GONE_CARD) {
                hardware.setStackInSlot(MiningComputerBlockEntity.CARD_SLOTS_START + i, AbandonedLoadouts.rigCard(own));
            }
        }
        if (working) {
            rig.installOs(AbandonedLoadouts.system(own));
        }
        final CompoundTag tag = rig.saveWithFullMetadata(level.registryAccess());
        if (working) {
            switchedOn(tag);
        }
        return tag;
    }

    /* A battery bank charged as the building left it: full in a working one. */
    static @Nullable CompoundTag battery(final LevelReader level, final StructureTemplate.StructureBlockInfo placed) {
        if (!(placed.state().getBlock() instanceof BatteryBankBlock block)
                || !(block.newBlockEntity(placed.pos(), placed.state()) instanceof BatteryBankBlockEntity bank)) {
            return null;
        }
        final int charge = placed.state().getValue(BatteryBankBlock.CHARGE);
        bank.store().setStored(bank.store().capacity() * charge / BatteryBankBlock.STEPS);
        return bank.saveWithFullMetadata(level.registryAccess());
    }

    /*
     * A cable of the game holding the wires its mark names, and the part on one of its faces (a Crafting Interface),
     * that face turned with the building.
     */
    static @Nullable CompoundTag cable(final LevelReader level, final StructureTemplate.StructureBlockInfo placed,
                                       final CompoundTag mark, final Era era, final StructurePlaceSettings settings) {
        final List<CableType> types = new ArrayList<>();
        for (final Tag id : mark.getList("cable", Tag.TAG_STRING)) {
            final CableType type = CoreCables.REGISTRY.get(ResourceLocation.parse(id.getAsString()));
            if (type != null) {
                types.add(type);
            }
        }
        if (types.isEmpty()) {
            return null;
        }
        final CableBlockEntity cable = CableBlockEntity.unsettled(placed.pos(), types);
        final Direction face = Direction.byName(mark.getString("face"));
        if (mark.contains("part") && face != null) {
            final Direction turned = settings.getRotation().rotate(settings.getMirror().mirror(face));
            cable.parts().add(turned, ComputingParts.craftingInterface(era).create());
        }
        return cable.saveWithFullMetadata(level.registryAccess());
    }

    /* Its power writes its own keys into its save: switched on, and set to come up whenever it has energy. */
    private static void switchedOn(final CompoundTag tag) {
        tag.putBoolean("ManualOn", true);
        tag.putBoolean("AutoStart", true);
    }

    /* A rack server of the era: whole, or what is left of it. */
    private static ItemStack server(final Era era, final boolean broken, final RandomSource random) {
        final List<ItemStack> parts = AbandonedLoadouts.serverParts(era);
        final ComputerKit.Built built = ComputerKit.assemble(AbandonedLoadouts.server(era),
                broken ? leftIn(parts, random) : parts);
        return built.computer().isEmpty() ? AbandonedLoadouts.server(era) : built.computer();
    }

    /* A supercomputer node of the era with its co-processor: whole, or what is left of it. */
    private static ItemStack node(final Era era, final boolean broken, final RandomSource random) {
        final List<ItemStack> parts = new ArrayList<>(AbandonedLoadouts.serverParts(era));
        parts.add(AbandonedLoadouts.named(HardwareEra.withParts(era) == HardwareEra.ADVANCED ? PHI_ADVANCED
                : PHI_STANDARD));
        final ItemStack shell = AbandonedLoadouts.node(era);
        final ComputerKit.Built built = ComputerKit.assemble(shell, broken ? leftIn(parts, random) : parts);
        return built.computer().isEmpty() ? shell : built.computer();
    }

    /* What a ruined machine still holds: its board with an even chance, what sits on it only with it, each again. */
    private static List<ItemStack> leftIn(final List<ItemStack> parts, final RandomSource random) {
        final List<ItemStack> out = new ArrayList<>();
        final boolean board = random.nextBoolean();
        for (int i = 0; i < parts.size(); i++) {
            final ItemStack part = parts.get(i);
            final boolean onBoard = i > BOARD && AbandonedLoadouts.onBoard(part);
            if ((i == BOARD ? board : random.nextBoolean()) && (!onBoard || board)) {
                out.add(part);
            }
        }
        return out;
    }

    /* {@code part} in the first slot of the Mainframe that takes it. */
    private static void seat(final ItemStackHandler inventory, final ItemStack part) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (inventory.getStackInSlot(slot).isEmpty() && inventory.isItemValid(slot, part)) {
                inventory.setStackInSlot(slot, part.copy());
                return;
            }
        }
    }

    /* The era a machine's mark names (a station's computer is of the era before), else the building's. */
    private static Era eraOf(final CompoundTag mark, final Era building) {
        final Era named = mark.contains("era") ? HardwareEra.seriesNamed(mark.getString("era")) : null;
        return named != null ? named : building;
    }
}
