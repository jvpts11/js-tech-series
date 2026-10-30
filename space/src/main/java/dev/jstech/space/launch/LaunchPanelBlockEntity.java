/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Space.
 */
package dev.jstech.space.launch;

import com.mojang.serialization.Codec;
import dev.jstech.core.blockentity.BoolField;
import dev.jstech.core.blockentity.IntField;
import dev.jstech.core.blockentity.SyncedBlockEntity;
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.uuid.StructureUuid;
import dev.jstech.space.api.Propellant;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.pad.LaunchPads;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.TankLoad;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A Launch Panel as its block entity: the LCC-1 console by the pad. It finds the launcher standing on the pad and its
 * rocket, and reads the go and no-go of each system: the vehicle there, its propellant loaded, the tower's arms, the
 * deluge, the weather, the crew, the range and the hold-down clamps. With its MASTER key and its IGNITION switch
 * armed and all go, COUNT RUNNING starts the count at T-60: the tower's arms swing back from T-42, the LAUNCH button's
 * cover lifts at T-10, the LAUNCH button pressed by T-6 lets the engines light at T-6 (the deluge poured, the pad's
 * rules kept) and the rocket go at T-0; otherwise the count holds at T-6. It holds by itself on a no-go or a
 * thunderstorm, and its buttons hold, scrub or recycle it. The count runs on the rocket too, on its flight computer's
 * clock.
 */
public final class LaunchPanelBlockEntity extends SyncedBlockEntity {

    private final ValueField<Phase> phase;
    private final IntField left;
    private final IntField met;
    private final IntField holds;
    private final BoolField deluge;
    private final BoolField clamps;
    private final BoolField ignition;
    private final BoolField guard;
    private final BoolField master;
    private final BoolField launched;
    private final BoolField vehicle;
    private final IntField oxidiser;
    private final IntField fuel;
    private final BoolField delugeReady;
    private final BoolField storm;
    private final ValueField<String> name;
    private final ValueField<UUID> pilot;
    private final ValueField<StructureUuid> flight;
    private final BoolField lifted;
    private @Nullable LaunchPads.Stand stand;
    private int wait;

    /** The count, in seconds before release: its start, the tower's arms moving, the cover lifting, ignition. */
    public static final int START = 60;
    public static final int ARMS = 42;
    public static final int COVER = 10;
    public static final int LIGHT = 6;
    /** How full, in hundredths, a rocket's tanks must be for its propellant to be go. */
    public static final int FULL = 99;
    /** The controls of its face, by their numbers. */
    public static final int DELUGE = 0;
    public static final int CLAMPS = 1;
    public static final int GUARD = 2;
    public static final int IGNITION = 3;
    public static final int MASTER = 4;
    public static final int LAUNCH = 5;
    public static final int HOLD = 6;
    public static final int SCRUB = 7;
    public static final int RECYCLE = 8;
    public static final int RUN = 9;
    /** How far round it, under it and over it, the launcher on its pad is looked for. */
    public static final int REACH = 24;
    public static final int BELOW = 8;
    public static final int ABOVE = 16;
    /* Ticks in a second; how many between looks at the pad. */
    private static final int SECOND = 20;
    private static final int HUNDRED = 100;

    public LaunchPanelBlockEntity(final BlockPos pos, final BlockState state) {
        super(LaunchPanel.ENTITY.get(), pos, state);
        this.phase = fields().value("Phase", StableCodecs.byName(Phase.class), Phase.SAFE).save().toClient();
        this.left = fields().integer("Left", START * SECOND).save().toClient();
        this.met = fields().integer("Met", 0).save().toClient();
        this.holds = fields().integer("Holds", 0).save().toClient();
        this.deluge = fields().flag("Deluge", true).save().toClient();
        this.clamps = fields().flag("Clamps", true).save().toClient();
        this.ignition = fields().flag("Ignition", false).save().toClient();
        this.guard = fields().flag("Guard", false).save().toClient();
        this.master = fields().flag("Master", false).save().toClient();
        this.launched = fields().flag("Launched", false).save().toClient();
        // What it reads of the pad, told to the players who see its face.
        this.vehicle = fields().flag("Vehicle", false).toClient();
        this.oxidiser = fields().integer("Oxidiser", 0).toClient();
        this.fuel = fields().integer("Fuel", 0).toClient();
        this.delugeReady = fields().flag("DelugeReady", false).toClient();
        this.storm = fields().flag("Storm", false).toClient();
        this.name = fields().value("Name", Codec.STRING, "").toClient();
        // Who pressed LAUNCH, whose rocket it is in flight; the rocket released, and whether it has begun to climb.
        this.pilot = fields().nullable("Pilot", UUIDUtil.CODEC).save();
        this.flight = fields().nullable("Flight", StructureUuid.CODEC).save();
        this.lifted = fields().flag("Lifted", false).save();
    }

    /** Where its count is. */
    public Phase phase() {
        return this.phase.get();
    }

    /** The ticks left before release; and, once released, the mission's ticks since. */
    public int left() {
        return this.left.get();
    }

    public int met() {
        return this.met.get();
    }

    /** How many times its count has held. */
    public int holds() {
        return this.holds.get();
    }

    /** Its switches: the deluge and the clamps on automatic, the ignition armed under its guard, the key armed. */
    public boolean deluge() {
        return this.deluge.get();
    }

    public boolean clamps() {
        return this.clamps.get();
    }

    public boolean ignition() {
        return this.ignition.get();
    }

    public boolean guard() {
        return this.guard.get();
    }

    public boolean master() {
        return this.master.get();
    }

    /** Whether its LAUNCH button has been pressed in this count. */
    public boolean launched() {
        return this.launched.get();
    }

    /** Whether the LAUNCH button's cover is up: from T-10 on, while a count runs or holds. */
    public boolean coverUp() {
        final Phase now = this.phase.get();
        return (now == Phase.COUNTING || now == Phase.HOLDING) && this.left.get() <= COVER * SECOND;
    }

    /** Whether a rocket keeping its plan stands on the launcher on its pad. */
    public boolean vehicle() {
        return this.vehicle.get();
    }

    /** How full its rocket's oxidiser and fuel are, in hundredths. */
    public int oxidiser() {
        return this.oxidiser.get();
    }

    public int fuel() {
        return this.fuel.get();
    }

    /** Whether its pad's service port holds a whole deluge. */
    public boolean delugeReady() {
        return this.delugeReady.get();
    }

    /** Whether a thunderstorm holds the count. */
    public boolean storm() {
        return this.storm.get();
    }

    /** The name of the rocket on its pad, or empty. */
    public String rocketName() {
        final String held = this.name.get();
        return held == null ? "" : held;
    }

    /** Whether everything a count needs is go: the vehicle, its propellant, the weather and the clamps. */
    public boolean go() {
        return vehicle() && oxidiser() >= FULL && fuel() >= FULL && !storm() && clamps();
    }

    /** A control of its face pressed, by its number, as its rules allow. */
    public void press(final int control) {
        press(control, null);
    }

    /** A control of its face pressed by {@code by}, whose rocket it is once they press LAUNCH. */
    public void press(final int control, @Nullable final UUID by) {
        final Phase now = this.phase.get();
        if (now == Phase.RELEASED) {
            return;
        }
        switch (control) {
            case DELUGE -> this.deluge.set(!deluge());
            case CLAMPS -> this.clamps.set(!clamps());
            case GUARD -> this.guard.set(!guard());
            case IGNITION -> {
                if (guard()) {
                    this.ignition.set(!ignition());
                }
            }
            case MASTER -> {
                this.master.set(!master());
                if (!master() && now == Phase.COUNTING) {
                    hold();
                }
            }
            case LAUNCH -> {
                if (coverUp()) {
                    this.launched.set(true);
                    this.pilot.set(by);
                    if (now == Phase.HOLDING && this.left.get() == LIGHT * SECOND && go()) {
                        this.phase.set(Phase.COUNTING);
                    }
                }
            }
            case HOLD -> {
                if (now == Phase.COUNTING) {
                    hold();
                }
            }
            case SCRUB -> {
                this.phase.set(Phase.SCRUBBED);
                this.left.set(START * SECOND);
                this.launched.set(false);
                this.master.set(false);
            }
            case RECYCLE -> {
                if (now == Phase.HOLDING && this.left.get() < COVER * SECOND) {
                    this.left.set(COVER * SECOND);
                    this.launched.set(false);
                }
            }
            case RUN -> run(now);
            default -> {
                return;
            }
        }
        setChanged();
    }

    /** Counts on, a tick at a time; looks at its pad every second. */
    public void tick() {
        if (!(this.level instanceof ServerLevel server)) {
            return;
        }
        if (--this.wait <= 0) {
            this.wait = SECOND;
            look(server);
        }
        final Phase now = this.phase.get();
        if (now == Phase.RELEASED) {
            this.met.set(this.met.get() + 1);
            climb(server);
            return;
        }
        if (now != Phase.COUNTING) {
            return;
        }
        if (!go() || !master()) {
            hold();
            return;
        }
        if (this.left.get() == LIGHT * SECOND) {
            if (!launched()) {
                // The LAUNCH button not pressed by ignition: the count holds there till it is.
                hold();
                return;
            }
            if (this.stand != null) {
                LaunchPads.ignite(server, this.stand.axis(), this.stand.side(), deluge());
                RocketLaunch.ignite(server, this.stand.axis());
            }
        }
        final int next = this.left.get() - 1;
        this.left.set(next);
        if (next <= 0) {
            release(server);
        }
        setChanged();
    }

    /* T-0: the rocket on the pad becomes one body and is let go; where it cannot be, the launch is scrubbed. */
    private void release(final ServerLevel server) {
        final Optional<MovingStructure> body = this.stand == null ? Optional.empty() : this.stand.computer()
                .flatMap(computer -> RocketLaunch.release(server, computer, this.pilot.get()));
        if (body.isEmpty()) {
            this.phase.set(Phase.SCRUBBED);
            this.left.set(START * SECOND);
            this.launched.set(false);
            return;
        }
        this.flight.set(body.get().id());
        this.lifted.set(false);
        this.phase.set(Phase.RELEASED);
        this.met.set(0);
    }

    /* Once the released rocket's body stands ready, it climbs. */
    private void climb(final ServerLevel server) {
        final StructureUuid id = this.flight.get();
        if (id == null || this.lifted.get()) {
            return;
        }
        final MovingStructure body = MovingStructures.byId(server, id);
        if (body != null && body.ready()) {
            RocketLaunch.lift(server, body);
            this.lifted.set(true);
            setChanged();
        }
    }

    /* Starts the count, or runs it on from a hold, if its key and its ignition are armed and all is go. */
    private void run(final Phase now) {
        if (!master() || !ignition() || !go()) {
            return;
        }
        if (now == Phase.SAFE || now == Phase.SCRUBBED) {
            this.left.set(START * SECOND);
            this.launched.set(false);
            this.phase.set(Phase.COUNTING);
        } else if (now == Phase.HOLDING && (this.left.get() != LIGHT * SECOND || launched())) {
            this.phase.set(Phase.COUNTING);
        }
    }

    private void hold() {
        this.phase.set(Phase.HOLDING);
        this.holds.set(this.holds.get() + 1);
        setChanged();
    }

    /* Finds the launcher on its pad and its rocket, reads them, and sets the rocket's clock to its count. */
    private void look(final ServerLevel server) {
        this.stand = LaunchPads.standBy(server, getBlockPos(), REACH, BELOW, ABOVE).orElse(null);
        final FlightComputerBlockEntity rocket = this.stand == null ? null : this.stand.computer()
                .map(server::getBlockEntity).filter(FlightComputerBlockEntity.class::isInstance)
                .map(FlightComputerBlockEntity.class::cast).orElse(null);
        this.vehicle.set(rocket != null);
        this.storm.set(LaunchPads.stormHolds(server));
        this.delugeReady.set(this.stand != null && LaunchPads.protection(server, this.stand.axis(),
                this.stand.side()).deluge());
        this.name.set(rocket == null ? "" : rocket.rocket().map(RocketDesign::name).orElse(""));
        final int[] full = rocket == null ? new int[2] : full(server, rocket);
        this.oxidiser.set(full[0]);
        this.fuel.set(full[1]);
        if (rocket != null) {
            final Phase now = this.phase.get();
            rocket.clock(now == Phase.RELEASED ? this.met.get() / SECOND : -(this.left.get() + SECOND - 1) / SECOND);
        }
    }

    /* How full its rocket's oxygen and its other propellant are, in hundredths of what its tanks hold of each. */
    private static int[] full(final ServerLevel server, final FlightComputerBlockEntity rocket) {
        final Optional<RocketDesign> design = rocket.rocket();
        if (design.isEmpty()) {
            return new int[2];
        }
        final Map<ResourceLocation, RocketPart> parts = RocketParts.all(server);
        final List<RocketStack.Section> sections = design.get().stack().sections();
        final ResourceLocation oxygen = MaterialLiquids.materialOf(SkeletonPropellants.LIQUID_OXYGEN.get())
                .map(Material::id).orElse(null);
        double oxygenHeld = 0;
        double oxygenRoom = 0;
        double fuelHeld = 0;
        double fuelRoom = 0;
        final Optional<Propellant> methalox = Propellants.get(server, SkeletonPropellants.METHALOX);
        for (int k = 0; k < sections.size(); k++) {
            final RocketPart part = parts.get(sections.get(k).part().id());
            if (part == null || part.propellant() <= 0 || methalox.isEmpty()) {
                continue;
            }
            final double room = part.propellant() * (part.stretches() ? sections.get(k).part().layers() : 1);
            final TankLoad load = rocket.tanks().get(k);
            for (final Propellant.Component component : methalox.get().components()) {
                final double held = load == null ? 0 : load.kilograms().getOrDefault(component.material(), 0.0);
                if (component.material().equals(oxygen)) {
                    oxygenHeld += held;
                    oxygenRoom += room * component.share();
                } else {
                    fuelHeld += held;
                    fuelRoom += room * component.share();
                }
            }
        }
        return new int[] {share(oxygenHeld, oxygenRoom), share(fuelHeld, fuelRoom)};
    }

    private static int share(final double held, final double room) {
        return room <= 0 ? 0 : (int) Math.min(HUNDRED, Math.floor(held / room * HUNDRED + 1e-6));
    }

    /** Where a count is. */
    public enum Phase implements IStableName {
        /** Not counting, the pad made safe. */
        SAFE("safe"),
        /** Counting down. */
        COUNTING("counting"),
        /** Held. */
        HOLDING("holding"),
        /** Released: the rocket goes. */
        RELEASED("released"),
        /** Scrubbed, back to the start. */
        SCRUBBED("scrubbed");

        private final String serializedName;

        Phase(final String serializedName) {
            this.serializedName = serializedName;
        }

        @Override
        public String serializedName() {
            return this.serializedName;
        }
    }
}
