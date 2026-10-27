/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Industrial.
 */
package dev.jstech.industrial.integration.computers;

import dev.jstech.core.air.AirMix;
import dev.jstech.core.blockentity.BlockEntityFields;
import dev.jstech.core.blockentity.LongField;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.dimension.Climate;
import dev.jstech.core.hazard.GasClouds;
import dev.jstech.core.hazard.GasLimits;
import dev.jstech.core.machine.IMachineParameters;
import dev.jstech.core.machine.MachineParameter;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.Materials;
import dev.jstech.core.room.SealedRooms;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * The alerts an Industrial Controller Computer keeps of the machines it reads, as a plant's alarm panel keeps its own:
 * looked at once a second, an alert is raised the moment a machine falls into what it is about, and marked over once
 * the machine is out of it, so a fault that holds is one alert and a fault that comes back is another. It keeps the
 * latest {@link #KEPT}, newest first; an operator acknowledging one only dims it.
 */
@TextHolder
final class MachineAlerts {

    private final ValueField<List<MachineAlert>> kept;
    private final LongField next;

    /** How many alerts it keeps. */
    static final int KEPT = 64;
    public static final TextKey HEAT = TextKey.of("jsindustrial.alert.reading.heat", "%s °C, limit %s °C");
    public static final TextKey FUSE = TextKey.of("jsindustrial.alert.reading.fuse", "the fuse of its %s port");
    public static final TextKey ITS_FUSE = TextKey.of("jsindustrial.alert.reading.its_fuse", "its fuse");
    public static final TextKey SHORT = TextKey.of("jsindustrial.alert.reading.short",
            "short of the voltage of its %s port");
    public static final TextKey SHORT_OF_VOLTAGE = TextKey.of("jsindustrial.alert.reading.short_of_voltage",
            "short of its voltage");
    public static final TextKey CONDITION = TextKey.of("jsindustrial.alert.reading.condition", "condition %s%%");
    public static final TextKey GAS = TextKey.of("jsindustrial.alert.reading.gas", "%s, %s");
    /* Degrees Celsius above kelvin, and parts in a million of a share. */
    private static final double CELSIUS = 273.15;
    private static final double PARTS = 1_000_000;
    private static final double PERCENT = 100;
    /* The air's own gases, which leak from nothing. */
    private static final Set<ResourceLocation> AIR = Set.of(AirMix.OXYGEN, AirMix.NITROGEN, AirMix.ARGON);

    MachineAlerts(final BlockEntityFields fields) {
        this.kept = fields.value("Alerts", MachineAlert.CODEC.listOf(), List.<MachineAlert>of()).save();
        this.next = fields.longInteger("NextAlert", 1L).save();
    }

    /** Every alert it keeps, newest first. */
    List<MachineAlert> all() {
        return this.kept.get();
    }

    /**
     * Looks at the machines it reads, {@code names} by where each stands: raises an alert for each that has just fallen
     * into what one is about, and marks over those whose machine is out of it, or no longer read.
     *
     * @return the alerts raised now
     */
    List<MachineAlert> look(final ServerLevel level, final Map<Long, String> names) {
        final List<MachineAlert> raised = new ArrayList<>();
        final List<MachineAlert> after = new ArrayList<>(this.kept.get().size() + 1);
        final Map<Long, Map<AlertKind, Text>> now = new HashMap<>();
        for (final Map.Entry<Long, String> machine : names.entrySet()) {
            now.put(machine.getKey(), faultsOf(level, BlockPos.of(machine.getKey())));
        }
        final Set<String> held = new LinkedHashSet<>();
        for (final MachineAlert alert : this.kept.get()) {
            final Map<AlertKind, Text> faults = now.get(alert.pos());
            if (alert.active() && (faults == null || !faults.containsKey(alert.kind()))) {
                after.add(alert.over());
            } else {
                after.add(alert);
                if (alert.active()) {
                    held.add(alert.pos() + "/" + alert.kind().serializedName());
                }
            }
        }
        for (final Map.Entry<Long, Map<AlertKind, Text>> machine : now.entrySet()) {
            for (final Map.Entry<AlertKind, Text> fault : machine.getValue().entrySet()) {
                if (held.contains(machine.getKey() + "/" + fault.getKey().serializedName())) {
                    continue;
                }
                final long id = this.next.get();
                this.next.set(id + 1);
                final MachineAlert alert = new MachineAlert(id, machine.getKey(), level.getDayTime(),
                        names.get(machine.getKey()), fault.getKey(), fault.getValue(), false, true);
                raised.add(alert);
                after.add(0, alert);
            }
        }
        if (!raised.isEmpty() || !after.equals(this.kept.get())) {
            this.kept.set(List.copyOf(after.subList(0, Math.min(KEPT, after.size()))));
        }
        return raised;
    }

    /** Acknowledges the alert numbered {@code id}; whether it keeps one. */
    boolean acknowledge(final long id) {
        boolean found = false;
        final List<MachineAlert> after = new ArrayList<>(this.kept.get().size());
        for (final MachineAlert alert : this.kept.get()) {
            found |= alert.id() == id;
            after.add(alert.id() == id ? alert.seen() : alert);
        }
        if (found) {
            this.kept.set(List.copyOf(after));
        }
        return found;
    }

    /** Acknowledges every alert it keeps. */
    void acknowledgeAll() {
        this.kept.set(this.kept.get().stream().map(MachineAlert::seen).toList());
    }

    /* What the machine at {@code at} is in now that an alert is about, with what it reads of each. */
    private static Map<AlertKind, Text> faultsOf(final ServerLevel level, final BlockPos at) {
        final Map<AlertKind, Text> faults = new EnumMap<>(AlertKind.class);
        final IMachineParameters values = level.getCapability(IMachineParameters.CAPABILITY, at, null);
        if (values != null) {
            final Optional<MachineStatus> status = values.statusNow();
            if (status.isPresent()) {
                switch (status.get()) {
                    case OVERHEATED -> faults.put(AlertKind.OVERHEATING, heat(values));
                    case FUSE_BLOWN -> faults.put(AlertKind.BLOWN_FUSE, port(values, FUSE, ITS_FUSE));
                    case UNDERVOLTAGE -> faults.put(AlertKind.UNDERVOLTAGE, port(values, SHORT, SHORT_OF_VOLTAGE));
                    case DAMAGED -> faults.put(AlertKind.DAMAGED, condition(values));
                    default -> {
                        // Every other state is one a machine works or rests in.
                    }
                }
            }
        }
        leak(level, at).ifPresent(reading -> faults.put(AlertKind.GAS_LEAK, reading));
        return faults;
    }

    private static Text heat(final IMachineParameters values) {
        final Optional<MachineParameter> temperature = values.parameter("temperature");
        return temperature.isEmpty() ? AlertKind.OVERHEATING.text().text()
                : HEAT.with(Math.round(temperature.get().value() - CELSIUS),
                        Math.round(temperature.get().most() - CELSIUS));
    }

    private static Text port(final IMachineParameters values, final TextKey ofPort, final TextKey plain) {
        return values.powerDrawn().flatMap(drawn -> drawn.portClass()).map(port -> ofPort.with(port.name().text()))
                .orElse(plain.text());
    }

    private static Text condition(final IMachineParameters values) {
        return values.parameter("health").map(health -> CONDITION.with(Math.round(PERCENT * health.value()
                / Math.max(1, health.most())))).orElse(AlertKind.DAMAGED.text().text());
    }

    /*
     * The gas leaking into the air around the machine past its limit, the worst first: a cloud over it, or a gas the
     * sealed room it stands in holds that is not the air's own. Past its limit is past the share it explodes from, or
     * past the parts in a million it is dangerous from.
     */
    private static Optional<Text> leak(final ServerLevel level, final BlockPos at) {
        final Set<ResourceLocation> gases = new LinkedHashSet<>();
        final Vec3 where = Vec3.atCenterOf(at);
        for (final GasClouds.Cloud cloud : GasClouds.of(level)) {
            if (cloud.centre().distanceTo(where) <= cloud.radius()) {
                gases.add(cloud.gas());
            }
        }
        final Climate climate = SealedRooms.climateAt(level, at);
        climate.air().ifPresent(air -> air.shares().keySet().stream().filter(gas -> !AIR.contains(gas))
                .forEach(gases::add));
        double worst = 0;
        Text reading = null;
        for (final ResourceLocation id : gases) {
            final Optional<Material> gas = Materials.byId(id);
            if (gas.isEmpty()) {
                continue;
            }
            final double share = GasClouds.shareAt(level, at, gas.get());
            final double past = Math.max(share * PARTS / GasLimits.idlh(gas.get()),
                    GasLimits.lowerExplosive(gas.get()).stream().map(lower -> share / lower).max().orElse(0));
            if (past >= 1 && past > worst) {
                worst = past;
                reading = GAS.with(gas.get().name(), GasLimits.SHARE.with(percent(share)));
            }
        }
        return Optional.ofNullable(reading);
    }

    /* A share of the air as a percentage of at most two places: "0.9". */
    private static String percent(final double share) {
        final String written = String.format(Locale.ROOT, "%.2f", share * PERCENT);
        return written.contains(".") ? written.replaceAll("0+$", "").replaceAll("\\.$", "") : written;
    }
}
