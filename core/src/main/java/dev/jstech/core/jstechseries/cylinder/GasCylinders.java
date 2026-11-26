/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.cylinder;

import dev.jstech.core.JsCore;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.item.ItemGasTank;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.jstechseries.SharedUses;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialProperties;
import dev.jstech.core.material.MaterialValues;
import dev.jstech.core.matter.IMatterHandler;
import dev.jstech.core.matter.MatterCapabilities;
import dev.jstech.core.matter.MatterStack;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.PaletteRoles;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.registry.CoreItems;
import java.util.Optional;
import java.util.OptionalInt;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;

/**
 * The Gas Cylinder the series' mods share: a slim steel cylinder that carries a gas by hand, as a bucket carries a
 * fluid, and names the gas it holds wherever a gas is chosen, as a bucket names its fluid. It holds 50 litres at 200
 * times the open air's pressure, 10,000 mB, one gas at a time, one cylinder to a stack. Its shoulder is painted in the
 * colour the European code (EN 1089-3) gives the gas inside, from a table kept as data a resource pack can recolour; a
 * gas the table does not name shows its material's own colour, and an empty cylinder its body's bare steel.
 */
@PaletteHolder
public final class GasCylinders {

    /** What it holds: 50 litres at 200 times the open air's pressure, as a real 50 litre cylinder does. */
    public static final long VOLUME = 50L;
    public static final long PRESSURE = 200L;

    /** The Gas Cylinder. */
    public static final ItemEntry<GasCylinderItem> CYLINDER = CoreItems.CONTENT.item("gas_cylinder",
                    GasCylinderItem::new).named("Gas Cylinder").holdsGas(VOLUME, PRESSURE).look(IItemLook.HANDMADE)
            .tab(JsTechTab.BLOCKS_AND_TOOLS).register();

    /** The shoulder's colour for each gas the European code names, and for an empty cylinder. */
    public static final Palette<Shoulders> SHOULDERS = Palettes.declare(JsCore.MODID, "gas_cylinder_shoulders",
            new Shoulders(0xFFFACA30, 0xFFFACA30, 0xFF287233, 0xFFAF281E, 0xFFF5F3EB, 0xFF2C2C30, 0xFF57A639,
                    0xFF57A639, 0xFF57A639, 0xFF885C2E, 0xFF7A7B7A, 0xFF969CA4));
    /* A material's colour is written #RRGGBB; drawn, it is made opaque. */
    private static final int OPAQUE = 0xFF << 24;
    private static final int HEX = 16;

    static {
        // A mod used alone carries its gases by hand in it, so the Core itself uses it.
        SharedUses.declare(JsCore.MODID, CYLINDER::get);
    }

    private GasCylinders() {
    }

    /** Declares the cylinder, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares it.
    }

    /**
     * The shoulder's colour of a cylinder of {@code gas} by the European code, as {@code 0xAARRGGBB}; empty for a gas
     * the table does not name.
     */
    public static OptionalInt shoulder(final Material gas) {
        final Integer colour = PaletteRoles.read(SHOULDERS.get()).get(role(gas.id().getPath()));
        return colour == null ? OptionalInt.empty() : OptionalInt.of(colour);
    }

    /**
     * The colour a cylinder's shoulder is painted, as {@code 0xAARRGGBB}: of the gas in {@code cylinder} by the
     * European code, or its material's own colour for a gas the code does not name, or the bare steel when empty.
     */
    public static int shoulderOf(final ItemStack cylinder) {
        final IMatterHandler tank = cylinder.getCapability(MatterCapabilities.GAS_ITEM);
        final Optional<MatterStack> held = tank == null || tank.tanks() == 0 ? Optional.empty() : tank.stack(0);
        // An empty cylinder marked for a gas is painted for it, as a full one of that gas is.
        final Optional<Material> painted = held.map(MatterStack::material).or(() -> ItemGasTank.markOf(cylinder));
        if (painted.isEmpty()) {
            return SHOULDERS.get().empty();
        }
        final Material gas = painted.get();
        return shoulder(gas).orElseGet(() -> MaterialValues.value(gas, MaterialProperties.COLOUR, true)
                .map(colour -> OPAQUE | Integer.parseInt(colour.substring(1), HEX)).orElse(SHOULDERS.get().empty()));
    }

    /** An empty cylinder marked for {@code gas}: painted for it, and taking it alone. */
    @ApiStatus.Experimental
    public static ItemStack marked(final Material gas) {
        final ItemStack cylinder = new ItemStack(CYLINDER.get());
        ItemGasTank.mark(cylinder, Optional.of(gas));
        return cylinder;
    }

    /* A gas's name as a role of the table names it: carbon_dioxide as carbonDioxide. */
    private static String role(final String path) {
        final StringBuilder out = new StringBuilder(path.length());
        boolean upper = false;
        for (final char c : path.toCharArray()) {
            if (c == '_') {
                upper = true;
            } else {
                out.append(upper ? Character.toUpperCase(c) : c);
                upper = false;
            }
        }
        return out.toString();
    }

    /**
     * The table of shoulders, as the European code paints them: chlorine and fluorine, toxic and corrosive, yellow;
     * argon dark green; hydrogen, flammable, red; oxygen white; nitrogen black; krypton, neon and xenon, inert, bright
     * green; helium brown; carbon dioxide grey; and an empty cylinder's bare steel.
     */
    public record Shoulders(int chlorine, int fluorine, int argon, int hydrogen, int oxygen, int nitrogen, int krypton,
                            int neon, int xenon, int helium, int carbonDioxide, int empty) {
    }
}
