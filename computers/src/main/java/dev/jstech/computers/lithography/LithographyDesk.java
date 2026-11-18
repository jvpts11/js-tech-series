/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.lithography;

import dev.jstech.computers.blockentity.AbstractComputerBlockEntity;
import dev.jstech.computers.chain.ChainChips;
import dev.jstech.computers.chain.machine.LithoLink;
import dev.jstech.computers.chain.machine.LithoStations;
import dev.jstech.computers.chain.machine.MaskWriting;
import dev.jstech.computers.chain.machine.WaferMachineBlockEntity;
import dev.jstech.computers.chain.machine.WaferWork;
import dev.jstech.core.jstechseries.Element;
import dev.jstech.core.jstechseries.Elements;
import dev.jstech.core.jstechseries.SeriesChemicals;
import dev.jstech.core.jstechseries.tier.Era;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.material.MatterState;
import dev.jstech.core.matter.MatterTank;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The lithography program's side on the computer (computers/docs/design/survival.md, "Masks"): what it shows of the
 * Lithography Stations linked to its computer, read off each station, and the mask it has one write. Both of the
 * program's forms, its text screen on MC-DOS and its window on the desktop systems, show what this reads.
 */
@TextHolder
public final class LithographyDesk {

    private static final TextKey STATION = TextKey.of("jsc.lithography.station", "%s, %s");
    private static final TextKey KRF = TextKey.of("jsc.lithography.light.krf", "KrF");
    private static final TextKey ARF = TextKey.of("jsc.lithography.light.arf", "ArF");
    private static final TextKey EUV = TextKey.of("jsc.lithography.light.euv", "EUV");
    private static final TextKey IDLE = TextKey.of("jsc.lithography.status.idle", "Idle");
    private static final TextKey WRITING = TextKey.of("jsc.lithography.status.writing", "Writing a mask");
    private static final TextKey EXPOSING = TextKey.of("jsc.lithography.status.exposing", "Exposing layer %s of %s");
    private static final TextKey NONE = TextKey.of("jsc.lithography.none", "none");
    private static final TextKey EMPTY = TextKey.of("jsc.lithography.slot.empty", "empty");
    private static final TextKey MASK = TextKey.of("jsc.lithography.slot.mask", "%s mask");
    private static final TextKey PHOTORESIST = TextKey.of("jsc.lithography.supply.photoresist", "Photoresist");
    private static final TextKey KRYPTON = TextKey.of("jsc.lithography.supply.krypton", "Krypton");
    private static final TextKey ARGON = TextKey.of("jsc.lithography.supply.argon", "Argon");
    private static final TextKey WATER = TextKey.of("jsc.lithography.supply.water", "Water");
    private static final int PERMILLE = 1000;
    private static final int TICKS_A_SECOND = 20;

    private LithographyDesk() {
    }

    /**
     * What the program shows of the station {@code index} among those {@code computer} works for, the first when
     * there are fewer; nothing when it works for none.
     */
    public static LithographyLook look(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                       final int index) {
        final List<WaferMachineBlockEntity> stations = LithoStations.of(level, computer);
        if (stations.isEmpty()) {
            return LithographyLook.NONE;
        }
        final int shown = index >= 0 && index < stations.size() ? index : 0;
        final WaferMachineBlockEntity station = stations.get(shown);
        final List<MaskWriting.Offer> offers = MaskWriting.offers(station.machine(), computer.displayEra());
        final List<LithographyLook.Pick> picks = new ArrayList<>();
        for (final MaskWriting.Offer offer : offers) {
            picks.add(new LithographyLook.Pick(offer.kind().serializedName(),
                    BuiltInRegistries.ITEM.getKey(offer.mask()), offer.name()));
        }
        final ItemStack mask = maskIn(station);
        final boolean blank = mask.is(ChainChips.MASK_BLANK.get());
        final Item writes = station.writes();
        final boolean writing = writes != null && blank && !station.exposing();
        final Optional<WaferWork> exposure = station.exposure();
        final int permille = station.getMaxProgress() <= 0 ? 0
                : (int) ((long) station.getProgress() * PERMILLE / station.getMaxProgress());
        final int seconds = Math.max(0, station.getMaxProgress() - station.getProgress()) / TICKS_A_SECOND;
        final Text status = exposure.<Text>map(work -> EXPOSING.with(work.layer(), work.layers()))
                .orElse(writing ? WRITING.text() : IDLE.text());
        return new LithographyLook(stations.size(), shown, STATION.with(eraOf(station).name(), light(station)),
                status, exposure.isPresent() ? nameOf(offers, mask) : NONE.text(), exposure.isPresent(),
                exposure.isPresent() ? permille : 0, exposure.isPresent() ? seconds : 0,
                LithoLink.heldMb(computer, station.getBlockPos(), level.getGameTime()), supplies(station),
                slotOf(offers, mask), blank, !mask.isEmpty() && !blank, picks,
                writing ? nameOf(offers, new ItemStack(writes)) : Text.EMPTY,
                writing ? permille : 0, writing ? seconds : 0);
    }

    /**
     * Has the station {@code index} among those {@code computer} works for write the mask {@code mask}; why it does
     * not, when it does not, or that no such station is there.
     */
    public static Optional<TextKey> write(final ServerLevel level, final AbstractComputerBlockEntity computer,
                                          final int index, final ResourceLocation mask) {
        final List<WaferMachineBlockEntity> stations = LithoStations.of(level, computer);
        final Item item = BuiltInRegistries.ITEM.get(mask);
        if (index < 0 || index >= stations.size() || item == Items.AIR) {
            return Optional.of(MaskWriting.NOT_OFFERED);
        }
        return MaskWriting.write(stations.get(index), computer.displayEra(), item);
    }

    /* The mask or the blank among the station's inputs, the first; nothing when it holds neither. */
    private static ItemStack maskIn(final WaferMachineBlockEntity station) {
        for (final ItemStack stack : station.inputs()) {
            if (stack.is(ChainChips.MASK_BLANK.get()) || BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()
                    .startsWith("mask_")) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /* What stands in the mask slot: the blank's name, the mask by what the program calls it, or nothing. */
    private static Text slotOf(final List<MaskWriting.Offer> offers, final ItemStack mask) {
        if (mask.isEmpty()) {
            return EMPTY.text();
        }
        if (mask.is(ChainChips.MASK_BLANK.get())) {
            return GameText.of(mask.getHoverName());
        }
        return offers.stream().filter(offer -> mask.is(offer.mask())).findFirst()
                .map(offer -> MASK.with(offer.name())).orElseGet(() -> GameText.of(mask.getHoverName()));
    }

    /* What the program calls the mask {@code mask}, its item's name for one the station does not write. */
    private static Text nameOf(final List<MaskWriting.Offer> offers, final ItemStack mask) {
        return offers.stream().filter(offer -> mask.is(offer.mask())).findFirst().map(MaskWriting.Offer::name)
                .orElseGet(() -> GameText.of(mask.getHoverName()));
    }

    /* The era the station is named by: its own, the first it serves. */
    private static Era eraOf(final WaferMachineBlockEntity station) {
        final Era era = station.machine().era();
        return era == null ? HardwareEra.LEGACY : era;
    }

    /* The light it exposes with, as the program names it. */
    private static Text light(final WaferMachineBlockEntity station) {
        final Era era = eraOf(station);
        if (era == HardwareEra.LEGACY) {
            return KRF.text();
        }
        return era == HardwareEra.ADVANCED ? EUV.text() : ARF.text();
    }

    /*
     * The liquids and gases the station takes for each layer, with what it holds of each: photoresist always; the
     * Legacy's krypton for its laser; the ArF station's argon and its ultrapure water for the immersion.
     */
    private static List<LithographyLook.Supply> supplies(final WaferMachineBlockEntity station) {
        final List<LithographyLook.Supply> out = new ArrayList<>();
        out.add(liquid(station, SeriesChemicals.PHOTORESIST, PHOTORESIST));
        final Era era = eraOf(station);
        if (era == HardwareEra.LEGACY) {
            out.add(gas(station, Elements.of(Element.KRYPTON), KRYPTON));
        } else if (era == HardwareEra.TRANSITION || era == HardwareEra.STANDARD) {
            out.add(gas(station, Elements.of(Element.ARGON), ARGON));
            out.add(liquid(station, SeriesChemicals.ULTRAPURE_WATER, WATER));
        }
        return out;
    }

    private static LithographyLook.Supply liquid(final WaferMachineBlockEntity station, final Material liquid,
                                                 final TextKey named) {
        long held = 0;
        for (int i = 0; i < station.layout().inputTanks(); i++) {
            if (station.inputTank(i).getFluid().getFluid() == MaterialLiquids.fluid(liquid).get()) {
                held += station.inputTank(i).getFluidAmount();
            }
        }
        return new LithographyLook.Supply(named.text(), held);
    }

    private static LithographyLook.Supply gas(final WaferMachineBlockEntity station, final Material gas,
                                              final TextKey named) {
        long held = 0;
        for (final MatterTank tank : station.matter().inputs(MatterState.GAS)) {
            held += tank.held().filter(stack -> stack.material() == gas).map(stack -> stack.amount()).orElse(0L);
        }
        return new LithographyLook.Supply(named.text(), held);
    }
}
