/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.abandoned;

import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.OsRegistry;
import dev.jstech.computers.os.ProgramKind;
import dev.jstech.computers.os.ProgramSpec;
import dev.jstech.computers.os.SoftwareHouse;
import dev.jstech.computers.os.media.InstallerStacks;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * The boxed software an abandoned building holds (computers/docs/design/survival.md, "Abandoned buildings"): the
 * programs of the building's era, and in a house's office only its house's (a Midsoft office only Midsoft's; a software
 * house office the house its sign names, the one its mock shows in each era; a startup other houses', never Midsoft's).
 * Every building also holds a way to a graphical system: in the Vintage, where no system has its own desktop, UNIX
 * System V and CDE for it; later, the era's system with its desktop.
 */
public final class AbandonedSoftware {

    /* The house a software house office's sign names in each era, as its mock shows it. */
    private static final Map<Era, SoftwareHouse> SOFTWARE_HOUSE = Map.of(HardwareEra.VINTAGE, SoftwareHouse.NOUVELL,
            HardwareEra.LEGACY, SoftwareHouse.AUTODECK, HardwareEra.TRANSITION, SoftwareHouse.AUTODECK,
            HardwareEra.STANDARD, SoftwareHouse.DAYLIGHT_FOUNDATION, HardwareEra.ADVANCED, SoftwareHouse.AUTODECK);
    private static final ResourceLocation UNIX = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "unix");
    private static final ResourceLocation CDE = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID, "cde");

    private AbandonedSoftware() {
    }

    /**
     * The programs {@code building} of {@code era} holds boxed, in the order of their ids; the era's programs of any
     * house when its own house wrote none of that era.
     */
    public static List<ProgramSpec> programs(final AbandonedBuilding building, final Era era) {
        final Predicate<ProgramSpec> ofEra = program -> program.installable() && program.era() == era
                && program.kind() != ProgramKind.DESKTOP_ENVIRONMENT && !InstallerStacks.forProgram(program).isEmpty();
        final List<ProgramSpec> own = sorted(ofEra.and(house(building, era)));
        return own.isEmpty() ? sorted(ofEra) : own;
    }

    /** The house whose software a software house office of {@code era} holds, the one its sign names. */
    public static SoftwareHouse softwareHouse(final Era era) {
        return SOFTWARE_HOUSE.getOrDefault(HardwareEra.withParts(era), SoftwareHouse.AUTODECK);
    }

    /**
     * The installers that make a graphical system of {@code era}: UNIX System V and CDE for it in the Vintage, the
     * era's own system with its desktop after.
     */
    public static List<ItemStack> graphicalSystem(final Era era) {
        if (HardwareEra.withParts(era) == HardwareEra.VINTAGE) {
            return List.of(InstallerStacks.found(InstallerStacks.forSystem(UNIX)), InstallerStacks.forProgram(CDE));
        }
        return List.of(InstallerStacks.found(InstallerStacks.forSystem(AbandonedLoadouts.system(era))));
    }

    /** The installer of {@code program}, boxed as a building holds it. */
    public static ItemStack boxed(final ProgramSpec program) {
        return InstallerStacks.forProgram(program);
    }

    /** The program of {@code building} of {@code era} picked by {@code pick} (any whole number), if it holds any. */
    public static Optional<ProgramSpec> pick(final AbandonedBuilding building, final Era era, final int pick) {
        final List<ProgramSpec> all = programs(building, era);
        return all.isEmpty() ? Optional.empty() : Optional.of(all.get(Math.floorMod(pick, all.size())));
    }

    private static Predicate<ProgramSpec> house(final AbandonedBuilding building, final Era era) {
        return switch (building) {
            case MIDSOFT_OFFICE -> program -> program.house().equals(SoftwareHouse.MIDSOFT);
            case SOFTWARE_HOUSE -> program -> program.house().equals(softwareHouse(era));
            case STARTUP -> program -> !program.house().equals(SoftwareHouse.MIDSOFT);
            default -> program -> true;
        };
    }

    private static List<ProgramSpec> sorted(final Predicate<ProgramSpec> which) {
        return OsRegistry.programs().stream().filter(which)
                .sorted(Comparator.comparing(program -> program.id().toString())).toList();
    }
}
