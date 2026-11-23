/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.upgrade;

import dev.jstech.core.JsCore;
import dev.jstech.core.content.IItemLook;
import dev.jstech.core.content.ItemBuilder;
import dev.jstech.core.content.ItemEntry;
import dev.jstech.core.jstechseries.JsTechTab;
import dev.jstech.core.machine.IUpgrade;
import dev.jstech.core.machine.UpgradeEffect;
import dev.jstech.core.machine.UpgradeItem;
import dev.jstech.core.registry.CoreItems;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The machine upgrades the series' mods share: real parts a player puts in a machine's upgrade slots, one identity for
 * any machine of any mod, each drawn after a real part. What each does and how many fit in one machine follow J's
 * Industrial's table of upgrades, where a machine adds what its upgrades gain and multiplies what they save. A mod
 * whose machines take upgrades declares that it uses them, and they show in the J's Tech tab.
 */
public final class SharedUpgrades {

    /* What a motor does, whatever its kind: half again as fast, for three quarters more energy. */
    private static final UpgradeEffect MOTOR = UpgradeEffect.NONE.withSpeed(1.5).withEnergy(1.75);
    /* The kind the three motors are, so four motors in all fit in one machine, of whichever kinds. */
    private static final ResourceLocation MOTORS = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "motor");
    /* How many of a motor, a drive, a line or a gas recovery unit fit in one machine; of precision tooling. */
    private static final int MOST = 4;
    private static final int MOST_TOOLING = 2;

    /** A small brushed DC motor. */
    public static final ItemEntry<UpgradeItem> BRUSHED_MOTOR = upgrade("brushed_motor", MOTOR, MOST, MOTORS)
            .named("Brushed Motor").register();
    /** An induction motor, its ribbed frame on its feet. */
    public static final ItemEntry<UpgradeItem> INDUCTION_MOTOR = upgrade("induction_motor", MOTOR, MOST, MOTORS)
            .named("Induction Motor").register();
    /** A brushless outrunner. */
    public static final ItemEntry<UpgradeItem> BRUSHLESS_MOTOR = upgrade("brushless_motor", MOTOR, MOST, MOTORS)
            .named("Brushless Motor").register();
    /** A drive that runs the machine's motor at the speed its work needs: a fifth less energy. */
    public static final ItemEntry<UpgradeItem> VARIABLE_FREQUENCY_DRIVE = upgrade("variable_frequency_drive",
            UpgradeEffect.NONE.withEnergy(0.8), MOST, null).named("Variable Frequency Drive").register();
    /** A second lane: one more recipe at once, its energy in proportion. */
    public static final ItemEntry<UpgradeItem> PARALLEL_LINE = upgrade("parallel_line",
            UpgradeEffect.NONE.withParallel(1), MOST, null).named("Parallel Line").register();
    /** Two more lanes: two more recipes at once. */
    public static final ItemEntry<UpgradeItem> ADVANCED_PARALLEL_LINE = upgrade("advanced_parallel_line",
            UpgradeEffect.NONE.withParallel(2), MOST, null).named("Advanced Parallel Line").register();
    /** A compressor that takes back the gas a recipe would lose: 15% less of its gases spent. */
    public static final ItemEntry<UpgradeItem> GAS_RECOVERY_UNIT = upgrade("gas_recovery_unit",
            UpgradeEffect.NONE.withGases(0.85), MOST, null).named("Gas Recovery Unit").register();
    /** A condenser that takes back the solvent a recipe would lose: 15% less of its liquids spent. */
    public static final ItemEntry<UpgradeItem> SOLVENT_RECOVERY_UNIT = upgrade("solvent_recovery_unit",
            UpgradeEffect.NONE.withLiquids(0.85), IUpgrade.ANY, null).named("Solvent Recovery Unit").register();
    /** A carbide end mill, its flutes coated: a tenth more of what the machine makes. */
    public static final ItemEntry<UpgradeItem> PRECISION_TOOLING = upgrade("precision_tooling",
            UpgradeEffect.NONE.withYield(1.1), MOST_TOOLING, null).named("Precision Tooling").register();
    /** A silencer: the machine makes no sound. */
    public static final ItemEntry<UpgradeItem> MUFFLER = upgrade("muffler", UpgradeEffect.NONE.withSound(0.0),
            IUpgrade.ANY, null).named("Muffler").register();
    /** A cyclone that parts the light from the heavy: byproducts half again as likely. */
    public static final ItemEntry<UpgradeItem> BYPRODUCT_SEPARATOR = upgrade("byproduct_separator",
            UpgradeEffect.NONE.withByproducts(1.5), IUpgrade.ANY, null).named("Byproduct Separator").register();

    private SharedUpgrades() {
    }

    /** Declares the upgrades, from the Core's constructor, while the Core's content is declared. */
    public static void declare() {
        // Loading the class declares them.
    }

    /** Every shared upgrade, in the order the tab shows them. */
    public static List<ItemEntry<UpgradeItem>> all() {
        return List.of(BRUSHED_MOTOR, INDUCTION_MOTOR, BRUSHLESS_MOTOR, VARIABLE_FREQUENCY_DRIVE, PARALLEL_LINE,
                ADVANCED_PARALLEL_LINE, GAS_RECOVERY_UNIT, SOLVENT_RECOVERY_UNIT, PRECISION_TOOLING, MUFFLER,
                BYPRODUCT_SEPARATOR);
    }

    /* An upgrade of {@code effect}, {@code most} of its {@code kind} in one machine, drawn flat in the J's Tech tab. */
    private static ItemBuilder<UpgradeItem> upgrade(final String id, final UpgradeEffect effect, final int most,
                                                    final @Nullable ResourceLocation kind) {
        return CoreItems.CONTENT.<UpgradeItem>item(id, properties -> new UpgradeItem(properties, effect, most, kind))
                .look(IItemLook.FLAT).tab(JsTechTab.FLUIDS_AND_COMPONENTS);
    }
}
