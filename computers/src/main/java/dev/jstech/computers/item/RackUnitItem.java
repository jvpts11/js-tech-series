/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.block.FibreReach;
import dev.jstech.computers.block.UpsBlock;
import dev.jstech.computers.power.UpsRating;
import dev.jstech.computers.rack.IMountableRackUnit;
import dev.jstech.computers.rack.RackLayout;
import dev.jstech.computers.rack.RackThermals;
import dev.jstech.core.jstechseries.energy.JsEnergy;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.IntFunction;

/**
 * Equipment that serves the rack itself rather than being a computer: the KVM switch that lets one
 * monitor address several machines, the UPS that carries the whole cabinet through an outage and the
 * battery expansions that make it last, the PDU that measures every bay and lets the network switch
 * them, the cooling that buys a dense rack its thermal headroom, the fibre interface that brings the
 * fibre backbone into its back, and the drive shelf that extends the bays of the server above it. Rack
 * units spend the same rack-unit budget the servers do, which is what makes filling a cabinet a set of
 * real trade-offs.
 */
@TextHolder
public class RackUnitItem extends Item implements IMountableRackUnit {

    /** What a rack unit does for the cabinet. */
    public enum Kind {
        /** Multiplexes the rack's machines onto one linked monitor, as a channel bar. */
        KVM_SWITCH("kvm_switch", 1),
        /** Carries every machine in the rack through a power outage. */
        RACK_UPS("rack_ups", 1),
        /** Active cooling: raises the cabinet's thermal budget. */
        COOLING_UNIT("cooling_unit", 1),
        /** Brings the fibre backbone of its era, and the earlier fibre, into the cabinet's back. */
        FIBRE_INTERFACE("fibre_interface", 1),
        /** More battery for the rack's UPS: they stack. */
        UPS_BATTERY("ups_battery_expansion", 1),
        /** Measures the energy of every bay and lets the network switch the bays' power. */
        PDU("pdu", 1),
        /** Ten drive bays and no server: it extends the bays of the server right above it. */
        DRIVE_SHELF("drive_shelf", 2),
        /** Passive ventilation, the cheap partner of the Cooling Unit: one for every era, in its rack's colour. */
        VENT_PANEL("vent_panel", 1);

        private final String id;
        private final int heightU;

        Kind(final String id, final int heightU) {
            this.id = id;
            this.heightU = heightU;
        }

        public String id() {
            return id;
        }

        public int heightU() {
            return heightU;
        }
    }

    private final Kind kind;
    /** The era of the unit: a rack seats the units of its own era and of the earlier ones. */
    private final Era era;

    private static final TextKey KVM_TOOLTIP = TextKey.of("item.jsc.kvm_switch.tooltip",
            "Lets one monitor address every machine in the rack");
    private static final TextKey UPS_TOOLTIP = TextKey.of("item.jsc.rack_ups.tooltip",
            "Carries the whole rack through a power outage");
    private static final TextKey COOLING_TOOLTIP = TextKey.of("item.jsc.cooling_unit.tooltip",
            "Active cooling: raises the rack's thermal budget");
    private static final TextKey COOLING_WATTS = TextKey.of("item.jsc.cooling_unit.watts",
            "+%s W to the rack's heat budget");
    private static final TextKey FIBRE_TOOLTIP = TextKey.of("item.jsc.fibre_interface.tooltip",
            "Lets the rack's back take the fibre backbone");
    private static final TextKey FIBRE_STANDARD = TextKey.of("item.jsc.fibre_interface.standard",
            "Takes the Fibre Optic Cable");
    private static final TextKey FIBRE_ADVANCED = TextKey.of("item.jsc.fibre_interface.advanced",
            "Takes the Fibre Optic Cable and the OM5 Fibre Cable");
    private static final TextKey EQUIPMENT = TextKey.of("item.jsc.rack_unit.equipment",
            "%sU - rack equipment, not a computer");
    private static final TextKey UPS_BATTERY_TOOLTIP = TextKey.of("item.jsc.ups_battery_expansion.tooltip",
            "More battery for the rack's UPS: they stack");
    private static final TextKey PDU_TOOLTIP = TextKey.of("item.jsc.pdu.tooltip",
            "Measures every bay's energy and lets the network switch their power");
    private static final TextKey SHELF_TOOLTIP = TextKey.of("item.jsc.drive_shelf.tooltip",
            "Ten drive bays for the server right above it");
    private static final TextKey VENT_TOOLTIP = TextKey.of("item.jsc.vent_panel.tooltip",
            "Passive ventilation: raises the rack's thermal budget");
    private static final TextKey VENT_EVERY_ERA = TextKey.of("item.jsc.vent_panel.every_era",
            "Fits a rack of every era, in the rack's colour");
    private static final TextKey ADDS = TextKey.of("item.jsc.rack_unit.adds", "+%s to the rack's UPS");
    /* The drive bays of a shelf: two rows of five. */
    private static final int SHELF_DRIVES = 10;

    public RackUnitItem(final Properties properties, final Kind kind, final Era era) {
        super(properties.stacksTo(1));
        this.kind = kind;
        this.era = era;
    }

    public Kind kind() {
        return kind;
    }

    public Era era() {
        return era;
    }

    /** Whether a rack of {@code rackEra} seats the unit: one of its own era or an earlier one. */
    public boolean fitsRackOf(final Era rackEra) {
        return era.isAtMost(rackEra);
    }

    /**
     * What the Cooling Unit or the Vent Panel in {@code stack} adds to its rack's heat budget: nothing for any other
     * unit.
     */
    public static int coolingWattsOf(final ItemStack stack) {
        if (!(stack.getItem() instanceof RackUnitItem unit)) {
            return 0;
        }
        return switch (unit.kind) {
            case COOLING_UNIT -> RackThermals.coolingUnitWatts(unit.era);
            case VENT_PANEL -> RackThermals.VENT_PANEL_W;
            default -> 0;
        };
    }

    /** How much of the fibre the unit in {@code stack} lets into its rack: none for anything but a fibre interface. */
    public static FibreReach fibreReachOf(final ItemStack stack) {
        return stack.getItem() instanceof RackUnitItem unit && unit.kind == Kind.FIBRE_INTERFACE
                ? FibreReach.upTo(unit.era) : FibreReach.NONE;
    }

    @Override
    public int heightU() {
        return kind.heightU();
    }

    /** A drive shelf's bays; the other units cable none. */
    @Override
    public int driveSlots() {
        return kind == Kind.DRIVE_SHELF ? SHELF_DRIVES : 0;
    }

    @Override
    public RackLayout.Role rackRole() {
        return kind == Kind.DRIVE_SHELF ? RackLayout.Role.SHELF : RackLayout.Role.EQUIPMENT;
    }

    /** The rack-unit kind of a stack, or null when the stack is not a rack unit. */
    @Nullable
    public static Kind kindOf(final ItemStack stack) {
        return stack.getItem() instanceof RackUnitItem unit ? unit.kind() : null;
    }

    /** Whether the stack is a rack unit of the given kind. */
    public static boolean is(final ItemStack stack, final Kind kind) {
        return kindOf(stack) == kind;
    }

    /**
     * The UPS the {@code count} units {@code unitAt} reads make up, or null without a Rack UPS among them: every Rack
     * UPS mounted carries 3 kW and holds 2.7 MJ, every UPS Battery Expansion another 2.7 MJ.
     */
    @Nullable
    public static UpsRating rackUps(final int count, final IntFunction<ItemStack> unitAt) {
        int upses = 0;
        int expansions = 0;
        for (int slot = 0; slot < count; slot++) {
            final ItemStack unit = unitAt.apply(slot);
            if (is(unit, Kind.RACK_UPS)) {
                upses++;
            } else if (is(unit, Kind.UPS_BATTERY)) {
                expansions++;
            }
        }
        return upses == 0 ? null : new UpsRating(UpsRating.RACK.batteryJoules() * upses
                + UpsRating.EXPANSION_JOULES * expansions, UpsRating.RACK.carryWatts() * upses);
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final TextKey what = switch (kind) {
            case KVM_SWITCH -> KVM_TOOLTIP;
            case RACK_UPS -> UPS_TOOLTIP;
            case COOLING_UNIT -> COOLING_TOOLTIP;
            case FIBRE_INTERFACE -> FIBRE_TOOLTIP;
            case UPS_BATTERY -> UPS_BATTERY_TOOLTIP;
            case PDU -> PDU_TOOLTIP;
            case DRIVE_SHELF -> SHELF_TOOLTIP;
            case VENT_PANEL -> VENT_TOOLTIP;
        };
        tooltip.add(GameText.component(what).withStyle(ChatFormatting.GRAY));
        if (kind == Kind.RACK_UPS) {
            tooltip.add(GameText.component(UpsBlock.holds(UpsRating.RACK)).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (kind == Kind.UPS_BATTERY) {
            tooltip.add(GameText.component(ADDS.with(JsEnergy.ENERGY.amount(UpsRating.EXPANSION_JOULES)))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        if (kind == Kind.FIBRE_INTERFACE) {
            final TextKey takes = fibreReachOf(stack) == FibreReach.ADVANCED ? FIBRE_ADVANCED : FIBRE_STANDARD;
            tooltip.add(GameText.component(takes).withStyle(ChatFormatting.DARK_GRAY));
        }
        if (kind == Kind.COOLING_UNIT || kind == Kind.VENT_PANEL) {
            tooltip.add(GameText.component(COOLING_WATTS.with(coolingWattsOf(stack)))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(GameText.component(EQUIPMENT.with(kind.heightU())).withStyle(ChatFormatting.DARK_GRAY));
        if (kind == Kind.VENT_PANEL) {
            tooltip.add(GameText.component(VENT_EVERY_ERA).withStyle(ChatFormatting.DARK_GRAY));
        } else {
            HardwareTooltip.appendEra(tooltip, era);
        }
    }
}
