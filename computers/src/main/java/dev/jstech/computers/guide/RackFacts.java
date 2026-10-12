/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.guide;

import dev.jstech.computers.hardware.ExpansionCardKind;
import dev.jstech.computers.hardware.StorageCardSpec;
import dev.jstech.computers.item.RackUnitItem;
import dev.jstech.computers.item.ServerItem;
import dev.jstech.computers.item.StorageCardItem;
import dev.jstech.computers.rack.RackChassis;
import dev.jstech.computers.rack.RackThermals;
import dev.jstech.core.guide.GuideBlock;
import dev.jstech.core.guide.GuideFacts;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.Item;

/**
 * The manual's tables of the servers, the storage cards and the Cooling Units, read from the items themselves as the
 * book is opened, so a chassis given another drive bay, or a card another era, is what the book prints and no number
 * is written twice.
 */
@TextHolder
public final class RackFacts {

    /** Each server's height, drive bays, processors, expansion slots and era. */
    public static final String SERVERS = "jsc:servers";
    /** Each storage card: what it is, the slot it takes, its era and how much of the read latency it takes away. */
    public static final String STORAGE_CARDS = "jsc:storage_cards";
    /** Each Cooling Unit: its era and what it adds to its rack's heat budget. */
    public static final String COOLING_UNITS = "jsc:cooling_units";

    private static final TextKey NO_LIMIT = TextKey.of("jsc.guide.facts.no_limit", "no limit");
    private static final TextKey ARRAY = TextKey.of("jsc.guide.facts.storage_card.array", "RAID controller");
    private static final TextKey CACHE = TextKey.of("jsc.guide.facts.storage_card.cache", "Read cache");

    private RackFacts() {
    }

    /** Says where the manual reads these tables from: once, as the manual is declared. */
    public static void declare() {
        GuideFacts.rows(SERVERS, RackFacts::serverRows);
        GuideFacts.rows(STORAGE_CARDS, RackFacts::storageCardRows);
        GuideFacts.rows(COOLING_UNITS, RackFacts::coolingUnitRows);
    }

    /* A row for each server, in the order the mod declares them. */
    private static List<GuideBlock.TableRow> serverRows() {
        final List<GuideBlock.TableRow> rows = new ArrayList<>();
        for (final Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof ServerItem server)) {
                continue;
            }
            final RackChassis chassis = server.chassis();
            rows.add(new GuideBlock.TableRow(List.of(
                    new GuideBlock.GuideValue.Words(item.getDescriptionId()),
                    new GuideBlock.GuideValue.Amount(chassis.heightU(), "U"),
                    new GuideBlock.GuideValue.Amount(chassis.driveSlots(), ""),
                    new GuideBlock.GuideValue.Amount(chassis.maxCpus(), ""),
                    chassis.maxPcie() == Integer.MAX_VALUE ? new GuideBlock.GuideValue.Words(NO_LIMIT.key())
                            : new GuideBlock.GuideValue.Amount(chassis.maxPcie(), ""),
                    new GuideBlock.GuideValue.Said(chassis.era().named()))));
        }
        return rows;
    }

    /* A row for each storage card, in the order the mod declares them. */
    private static List<GuideBlock.TableRow> storageCardRows() {
        final List<GuideBlock.TableRow> rows = new ArrayList<>();
        for (final Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof StorageCardItem card)) {
                continue;
            }
            final StorageCardSpec spec = card.spec();
            final boolean array = spec.kind() == ExpansionCardKind.RAID_CONTROLLER;
            rows.add(new GuideBlock.TableRow(List.of(
                    new GuideBlock.GuideValue.Words(item.getDescriptionId()),
                    new GuideBlock.GuideValue.Words((array ? ARRAY : CACHE).key()),
                    new GuideBlock.GuideValue.Said(spec.bus().label()),
                    new GuideBlock.GuideValue.Said(spec.era().named()),
                    array ? new GuideBlock.GuideValue.Words(CableFacts.NONE.key())
                            : new GuideBlock.GuideValue.Amount(spec.readCutPercent(), "%"))));
        }
        return rows;
    }

    /* A row for each Cooling Unit, in the order the mod declares them. */
    private static List<GuideBlock.TableRow> coolingUnitRows() {
        final List<GuideBlock.TableRow> rows = new ArrayList<>();
        for (final Item item : BuiltInRegistries.ITEM) {
            if (!(item instanceof RackUnitItem unit) || unit.kind() != RackUnitItem.Kind.COOLING_UNIT) {
                continue;
            }
            rows.add(new GuideBlock.TableRow(List.of(
                    new GuideBlock.GuideValue.Words(item.getDescriptionId()),
                    new GuideBlock.GuideValue.Said(unit.era().named()),
                    new GuideBlock.GuideValue.Amount(RackThermals.coolingUnitWatts(unit.era()), "W"))));
        }
        return rows;
    }
}
