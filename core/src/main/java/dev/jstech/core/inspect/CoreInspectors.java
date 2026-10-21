/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.inspect;

import dev.jstech.core.energy.EnergyPorts;
import dev.jstech.core.energy.IEnergyPort;
import dev.jstech.core.grid.CoreGrids;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.grid.GridPlace;
import dev.jstech.core.grid.GridPlaces;
import dev.jstech.core.heat.HeatPorts;
import dev.jstech.core.heat.IHeatPort;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.mechanical.IMechanicalPart;
import dev.jstech.core.mechanical.MechanicalPorts;
import dev.jstech.core.multiblock.LevelStructures;
import dev.jstech.core.network.NetworkMembers;
import dev.jstech.core.network.NetworkMembership;
import dev.jstech.core.room.Room;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.core.team.CoreTeams;
import dev.jstech.core.team.IOwned;
import dev.jstech.core.team.Ownership;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.TreeSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

/**
 * The Core's own parts of an inspection: the block, its state and its block entity's fields as they are saved now;
 * what it offers on each face (items, energy, fluids, heat, a shaft), every face alike shown once; the data network it
 * is a member of; the grids laid there; the sealed room it stands in; and the multiblock it is a part of.
 */
public final class CoreInspectors {

    /** How deep a block entity's fields are followed, and how many of its lines are shown at most. */
    private static final int DEEPEST = 3;
    private static final int MOST_FIELDS = 64;
    /** How many stacks of a face's items are named. */
    private static final int STACKS_NAMED = 4;
    private static final String TIMES = " x";
    private static final String OUT_OF = " / ";
    private static final String KELVIN = " K";
    private static final String RPM = " rpm";
    private static final String FE = " FE";
    private static final String MB = " mB";
    private static final String KPA = " kPa";
    private static final String ENTRIES = " entries";
    private static final String SPACE = " ";

    private CoreInspectors() {
    }

    /** Adds the Core's parts, once, from the Core's constructor. */
    @ApiStatus.Internal
    public static void declare() {
        Inspectors.provide(CoreLogs.CONTENT, CoreInspectors::block);
        Inspectors.provide(CoreLogs.CONTENT, CoreInspectors::offers);
        Inspectors.provide(CoreLogs.NETWORK, CoreInspectors::network);
        Inspectors.provide(CoreLogs.CABLES, CoreInspectors::grids);
        Inspectors.provide(CoreLogs.ROOMS, CoreInspectors::room);
        Inspectors.provide(CoreLogs.WORLD, CoreInspectors::multiblock);
    }

    private static void block(final ServerLevel level, final BlockPos pos, final Inspectors.Inspection report) {
        final BlockState state = level.getBlockState(pos);
        report.line(InspectTexts.BLOCK.text(), BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        if (!state.getProperties().isEmpty()) {
            report.under(InspectTexts.STATE.text(), properties -> {
                for (final Property<?> property : state.getProperties()) {
                    properties.line(Text.literal(property.getName()), valueName(state, property));
                }
            });
        }
        final BlockEntity entity = level.getBlockEntity(pos);
        if (entity == null) {
            return;
        }
        report.line(InspectTexts.BLOCK_ENTITY.text(), BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(entity.getType()));
        if (entity instanceof IOwned owned) {
            final Ownership ownership = owned.ownership();
            if (ownership != null) {
                report.line(InspectTexts.OWNER.text(), CoreTeams.nameOf(level.getServer(), ownership.owner())
                        .orElse(ownership.owner().toString()));
            }
        }
        final CompoundTag fields = entity.saveWithoutMetadata(level.registryAccess());
        if (!fields.isEmpty()) {
            final int[] left = {MOST_FIELDS};
            report.under(InspectTexts.FIELDS.text(), lines -> fields(lines, fields, 0, left));
        }
    }

    /* A tag's entries, each a line, a compound followed some way down; at most so many lines in all. */
    private static void fields(final Inspectors.Inspection lines, final CompoundTag tag, final int depth,
                               final int[] left) {
        for (final String key : new TreeSet<>(tag.getAllKeys())) {
            if (left[0]-- <= 0) {
                return;
            }
            final Tag value = Objects.requireNonNull(tag.get(key));
            if (value instanceof CompoundTag inner && depth < DEEPEST && !inner.isEmpty()) {
                lines.under(Text.literal(key), deeper -> fields(deeper, inner, depth + 1, left));
            } else if (value instanceof ListTag list) {
                lines.line(Text.literal(key), list.size() + ENTRIES);
            } else {
                lines.line(Text.literal(key), value.getAsString());
            }
        }
    }

    /*
     * What the block offers, every face alike once, and each face that differs on its own. Faces are told alike by
     * what they say, as a capability is often a new wrapper each time it is asked for.
     */
    private static void offers(final ServerLevel level, final BlockPos pos, final Inspectors.Inspection report) {
        final List<Object> everywhere = offered(level, pos, null);
        final List<Inspectors.Line> said = Inspectors.linesOf(lines -> describe(lines, everywhere));
        if (!said.isEmpty()) {
            report.under(InspectTexts.OFFERS.with(InspectTexts.ALL_FACES.text()), lines -> describe(lines,
                    everywhere));
        }
        for (final Direction face : Direction.values()) {
            final List<Object> own = offered(level, pos, face);
            final List<Inspectors.Line> saysOwn = Inspectors.linesOf(lines -> describe(lines, own));
            if (!saysOwn.isEmpty() && !saysOwn.equals(said)) {
                report.under(InspectTexts.OFFERS.with(face.getSerializedName()), lines -> describe(lines, own));
            }
        }
    }

    /* The items, energy, fluids, heat, shaft and energy port {@code face} offers, in that order, none where absent. */
    private static List<Object> offered(final ServerLevel level, final BlockPos pos, @Nullable final Direction face) {
        final List<Object> out = new ArrayList<>();
        out.add(capability(level, pos, Capabilities.ItemHandler.BLOCK, face));
        out.add(capability(level, pos, Capabilities.EnergyStorage.BLOCK, face));
        out.add(capability(level, pos, Capabilities.FluidHandler.BLOCK, face));
        out.add(capability(level, pos, HeatPorts.HEAT.block(), face));
        out.add(capability(level, pos, MechanicalPorts.MECHANICAL.block(), face));
        out.add(capability(level, pos, EnergyPorts.PORT, face));
        return out;
    }

    private static <T> @Nullable T capability(final ServerLevel level, final BlockPos pos,
                                              final BlockCapability<T, @Nullable Direction> kind,
                                              @Nullable final Direction face) {
        return level.getCapability(kind, pos, face);
    }

    private static void describe(final Inspectors.Inspection lines, final List<Object> offered) {
        for (final Object each : offered) {
            switch (each) {
                case IItemHandler items -> lines.under(InspectTexts.ITEMS.text(), held -> items(held, items));
                case IEnergyStorage energy -> lines.line(InspectTexts.ENERGY.text(), energy.getEnergyStored()
                        + OUT_OF + energy.getMaxEnergyStored() + FE);
                case IFluidHandler fluids -> lines.under(InspectTexts.FLUIDS.text(), held -> fluids(held, fluids));
                case IHeatPort heat -> lines.line(InspectTexts.HEAT.text(), Inspectors.written(heat.temperature())
                        + KELVIN);
                case IMechanicalPart shaft -> lines.line(InspectTexts.SHAFT.text(), Inspectors.written(shaft.rpm())
                        + RPM);
                case IEnergyPort port -> lines.line(port.energy().name(), port.stored() + OUT_OF + port.capacity()
                        + SPACE + port.portClass());
                case null, default -> {
                }
            }
        }
    }

    private static void items(final Inspectors.Inspection lines, final IItemHandler items) {
        int used = 0;
        int named = 0;
        for (int slot = 0; slot < items.getSlots(); slot++) {
            final ItemStack stack = items.getStackInSlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            used++;
            if (named++ < STACKS_NAMED) {
                lines.line(Text.literal(String.valueOf(slot)), BuiltInRegistries.ITEM.getKey(stack.getItem())
                        + TIMES + stack.getCount());
            }
        }
        lines.line(InspectTexts.ITEMS.text(), used + OUT_OF + items.getSlots());
    }

    private static void fluids(final Inspectors.Inspection lines, final IFluidHandler fluids) {
        for (int tank = 0; tank < fluids.getTanks(); tank++) {
            final FluidStack held = fluids.getFluidInTank(tank);
            lines.line(Text.literal(String.valueOf(tank)), (held.isEmpty() ? "" : BuiltInRegistries.FLUID.getKey(
                    held.getFluid()) + SPACE) + held.getAmount() + OUT_OF + fluids.getTankCapacity(tank) + MB);
        }
    }

    private static void network(final ServerLevel level, final BlockPos pos, final Inspectors.Inspection report) {
        final Optional<NetworkMembership> member = NetworkMembers.at(level, pos);
        if (member.isEmpty()) {
            return;
        }
        report.under(InspectTexts.NETWORK.text(), lines -> {
            lines.line(InspectTexts.NODE.text(), member.get().node().value());
            member.get().network().ifPresentOrElse(network -> {
                lines.line(InspectTexts.NETWORK.text(), network.value());
                lines.line(InspectTexts.MEMBERS.text(), NetworkMembers.on(level, network));
            }, () -> lines.line(InspectTexts.OFF_NETWORK.text()));
        });
    }

    private static void grids(final ServerLevel level, final BlockPos pos, final Inspectors.Inspection report) {
        final GridPlaces places = CoreGrids.places(level);
        for (final GridPlace place : places.at(pos.asLong())) {
            final OptionalLong number = places.find(place);
            for (final GridKind kind : GridKind.all()) {
                if (number.isEmpty() || !CoreGrids.contains(level, kind, place)) {
                    continue;
                }
                report.under(InspectTexts.GRID.with(kind.id().toString()), lines -> {
                    if (!place.isWhole()) {
                        lines.line(InspectTexts.LANE.text(), place.lane());
                    }
                    lines.line(InspectTexts.CONNECTED.text(), CoreGrids.of(level, kind)
                            .componentPositions(number.getAsLong()).size());
                });
            }
        }
    }

    private static void room(final ServerLevel level, final BlockPos pos, final Inspectors.Inspection report) {
        final Optional<Room> room = SealedRooms.roomAt(level, pos);
        room.ifPresent(found -> report.under(InspectTexts.ROOM.text(), lines -> {
            lines.line(InspectTexts.VOLUME.text(), found.volume());
            lines.line(InspectTexts.PRESSURE.text(), Inspectors.written(found.pressure()) + KPA);
            lines.line(InspectTexts.LEAKS.text(), found.leaks().size());
        }));
    }

    private static void multiblock(final ServerLevel level, final BlockPos pos, final Inspectors.Inspection report) {
        final BlockPos controller = LevelStructures.of(level).controllerHolding(pos);
        if (controller != null) {
            report.under(InspectTexts.MULTIBLOCK.text(), lines -> lines.line(InspectTexts.CONTROLLER.text(),
                    controller.toShortString() + SPACE + BuiltInRegistries.BLOCK.getKey(level.getBlockState(
                            controller).getBlock())));
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String valueName(final BlockState state, final Property property) {
        return property.getName(state.getValue(property));
    }
}
