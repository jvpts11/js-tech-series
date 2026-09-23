/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.rack.IMountableRackUnit;
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

/**
 * Equipment that serves the rack itself rather than being a computer: the KVM switch that lets one
 * monitor address several machines, the UPS that carries the whole cabinet through an outage, the
 * cooling that buys a dense rack its thermal headroom. Rack units spend the same rack-unit budget
 * the servers do, which is what makes filling a cabinet a set of real trade-offs.
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
        COOLING_UNIT("cooling_unit", 1);

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

    private static final TextKey KVM_TOOLTIP = TextKey.of("item.jsc.kvm_switch.tooltip",
            "Lets one monitor address every machine in the rack");
    private static final TextKey UPS_TOOLTIP = TextKey.of("item.jsc.rack_ups.tooltip",
            "Carries the whole rack through a power outage");
    private static final TextKey COOLING_TOOLTIP = TextKey.of("item.jsc.cooling_unit.tooltip",
            "Active cooling: raises the rack's thermal budget");
    private static final TextKey EQUIPMENT = TextKey.of("item.jsc.rack_unit.equipment",
            "%sU - rack equipment, not a computer");

    public RackUnitItem(final Properties properties, final Kind kind) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public int heightU() {
        return kind.heightU();
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

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context,
                                final List<Component> tooltip, final TooltipFlag flag) {
        final TextKey what = switch (kind) {
            case KVM_SWITCH -> KVM_TOOLTIP;
            case RACK_UPS -> UPS_TOOLTIP;
            case COOLING_UNIT -> COOLING_TOOLTIP;
        };
        tooltip.add(GameText.component(what).withStyle(ChatFormatting.GRAY));
        tooltip.add(GameText.component(EQUIPMENT.with(kind.heightU())).withStyle(ChatFormatting.DARK_GRAY));
    }
}
