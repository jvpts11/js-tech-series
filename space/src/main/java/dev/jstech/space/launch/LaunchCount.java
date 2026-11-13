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
import dev.jstech.core.blockentity.ValueField;
import dev.jstech.core.id.IStableName;
import dev.jstech.core.id.StableCodecs;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.MaterialLiquids;
import dev.jstech.core.moving.MovingStructure;
import dev.jstech.core.moving.MovingStructures;
import dev.jstech.core.uuid.StructureUuid;
import dev.jstech.space.advancement.SpaceFirsts;
import dev.jstech.space.api.Propellant;
import dev.jstech.space.api.Propellants;
import dev.jstech.space.api.RocketPart;
import dev.jstech.space.api.RocketParts;
import dev.jstech.space.cruise.CruiseFlight;
import dev.jstech.space.flight.FlightComputerBlockEntity;
import dev.jstech.space.hangar.RocketDesign;
import dev.jstech.space.launcher.LauncherDeckBlockEntity;
import dev.jstech.space.pad.LaunchPads;
import dev.jstech.space.pad.PadServicePortBlockEntity;
import dev.jstech.space.rocket.RocketStack;
import dev.jstech.space.rocket.SkeletonPropellants;
import dev.jstech.space.rocket.TankLoad;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * The count of a launch, kept by the launcher standing on the pad (its measuring hold-down arm), so a pad has one count
 * however it is worked: from a Launch Panel by the pad, from the LAUNCH page of the rocket's own flight computer, or
 * from both, each showing what the other did. It finds its rocket up the launcher's axis and reads the go and no-go of
 * each system: the vehicle there, its propellant loaded, the tower's arms, the deluge, the weather, the crew, the range
 * and the hold-down clamps. With its MASTER key and its IGNITION switch armed and all go, COUNT RUNNING starts the
 * count at T-60: the tower's arms swing back from T-42, the LAUNCH button's cover lifts at T-10, the LAUNCH button
 * pressed by T-6 lets the engines light at T-6 (the deluge poured, the pad's rules kept) and the rocket go at T-0;
 * otherwise the count holds at T-6. It holds by itself on a no-go or a thunderstorm, and its buttons hold, scrub or
 * recycle it. Whoever presses LAUNCH, inside or out, owns the rocket in flight. The count runs on the rocket too, on
 * its flight computer's clock.
 */
public final class LaunchCount {

    private final LauncherDeckBlockEntity owner;
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
    private final IntField padNumber;
    private final ValueField<String> padName;
    private final ValueField<BlockPos> rocket;
    private final ValueField<BlockPos> panel;
    private final ValueField<UUID> pilot;
    private final ValueField<StructureUuid> flight;
    private final BoolField lifted;
    private @Nullable LaunchPads.Stand stand;
    /* Where its pad's service port stood at the last look, which keeps the pad's number and name. */
    private @Nullable BlockPos port;
    private int wait;

    /** The count, in seconds before release: its start, the tower's arms moving, the cover lifting, ignition. */
    public static final int START = 60;
    public static final int ARMS = 42;
    public static final int COVER = 10;
    public static final int LIGHT = 6;
    /** How full, in hundredths, a rocket's tanks must be for its propellant to be go. */
    public static final int FULL = 99;
    /** Its controls, by their numbers, as the Launch Panel's face lays them out. */
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
    /**
     * The IGNITION switch and its guard as one key, the flight computer's: safe, it lifts the guard and arms the
     * switch; armed, it makes the switch safe and shuts the guard over it.
     */
    public static final int SWITCH = 10;
    /** Ticks in a second. */
    public static final int SECOND = 20;
    private static final int HUNDRED = 100;

    /** The count of the launcher {@code owner}, its state kept and told to the players who see it, as its own. */
    public LaunchCount(final LauncherDeckBlockEntity owner) {
        this.owner = owner;
        this.phase = owner.fields().value("Phase", StableCodecs.byName(Phase.class), Phase.SAFE).save().toClient();
        this.left = owner.fields().integer("Left", START * SECOND).save().toClient();
        this.met = owner.fields().integer("Met", 0).save().toClient();
        this.holds = owner.fields().integer("Holds", 0).save().toClient();
        this.deluge = owner.fields().flag("Deluge", true).save().toClient();
        this.clamps = owner.fields().flag("Clamps", true).save().toClient();
        this.ignition = owner.fields().flag("Ignition", false).save().toClient();
        this.guard = owner.fields().flag("Guard", false).save().toClient();
        this.master = owner.fields().flag("Master", false).save().toClient();
        this.launched = owner.fields().flag("Launched", false).save().toClient();
        // What it reads of the pad, told to the players who see it.
        this.vehicle = owner.fields().flag("Vehicle", false).toClient();
        this.oxidiser = owner.fields().integer("Oxidiser", 0).toClient();
        this.fuel = owner.fields().integer("Fuel", 0).toClient();
        this.delugeReady = owner.fields().flag("DelugeReady", false).toClient();
        this.storm = owner.fields().flag("Storm", false).toClient();
        this.name = owner.fields().value("Name", Codec.STRING, "").toClient();
        // Its pad's number and name, as its service port keeps them; 0 and empty where no port is by the pad.
        this.padNumber = owner.fields().integer("PadNumber", 0).toClient();
        this.padName = owner.fields().value("PadName", Codec.STRING, "").toClient();
        // The flight computer of the rocket it counts for, and the Launch Panel working it, if one is.
        this.rocket = owner.fields().nullable("Rocket", BlockPos.CODEC).toClient();
        this.panel = owner.fields().nullable("Panel", BlockPos.CODEC).toClient();
        // Who pressed LAUNCH, whose rocket it is in flight; the rocket released, and whether it has begun to climb.
        this.pilot = owner.fields().nullable("Pilot", UUIDUtil.CODEC).save();
        this.flight = owner.fields().nullable("Flight", StructureUuid.CODEC).save();
        this.lifted = owner.fields().flag("Lifted", false).save();
    }

    /** Whether a count may run: the vehicle there, its propellant loaded, no storm holding it, the clamps on. */
    public static boolean go(final boolean vehicle, final int oxidiser, final int fuel, final boolean storm,
                             final boolean clamps) {
        return vehicle && oxidiser >= FULL && fuel >= FULL && !storm && clamps;
    }

    /** Whether the LAUNCH button's cover is up: from T-10 on, while a count runs or holds. */
    public static boolean coverUp(final Phase phase, final int left) {
        return (phase == Phase.COUNTING || phase == Phase.HOLDING) && left <= COVER * SECOND;
    }

    /** Where its count is. */
    public Phase phase() {
        return this.phase.get();
    }

    /** The ticks left before release; and, once released, the mission's ticks since. */
    public int left() {
        return this.left.get();
    }

    /** All it shows, as the Launch Panel and the flight computer's LAUNCH page draw it. */
    public Shown shown() {
        return new Shown(phase(), left(), this.met.get(), this.holds.get(), this.deluge.get(), this.clamps.get(),
                this.ignition.get(), this.guard.get(), this.master.get(), this.launched.get(), this.vehicle.get(),
                this.oxidiser.get(), this.fuel.get(), this.delugeReady.get(), this.storm.get(),
                Objects.requireNonNullElse(this.name.get(), ""), this.padNumber.get(),
                Objects.requireNonNullElse(this.padName.get(), ""), this.panel.get() != null);
    }

    /** The rocket it released, while that rocket is still in this world. */
    public Optional<StructureUuid> flight() {
        return Optional.ofNullable(this.flight.get());
    }

    /** Whether it counts for the rocket whose flight computer stands at {@code computer}. */
    public boolean countsFor(final BlockPos computer) {
        return computer.equals(this.rocket.get());
    }

    /** A control pressed, by its number, as its rules allow. */
    public void press(final int control) {
        press(control, null);
    }

    /** A control pressed by {@code by}, inside the rocket or out, whose rocket it is once they press LAUNCH. */
    public void press(final int control, @Nullable final UUID by) {
        final Phase now = this.phase.get();
        if (now == Phase.RELEASED) {
            return;
        }
        switch (control) {
            case DELUGE -> this.deluge.set(!this.deluge.get());
            case CLAMPS -> this.clamps.set(!this.clamps.get());
            case GUARD -> {
                // A guard shut pushes its switch down with it, as a real cover does: no ignition stays armed under it.
                if (this.guard.get()) {
                    this.ignition.set(false);
                }
                this.guard.set(!this.guard.get());
            }
            case IGNITION -> {
                if (this.guard.get()) {
                    this.ignition.set(!this.ignition.get());
                }
            }
            case SWITCH -> {
                final boolean arm = !this.ignition.get();
                this.guard.set(arm);
                this.ignition.set(arm);
            }
            case MASTER -> {
                this.master.set(!this.master.get());
                if (!this.master.get() && now == Phase.COUNTING) {
                    hold();
                }
            }
            case LAUNCH -> {
                if (coverUp(now, left())) {
                    this.launched.set(true);
                    this.pilot.set(by);
                    if (now == Phase.HOLDING && left() == LIGHT * SECOND && go()) {
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
                scrub();
                this.master.set(false);
            }
            case RECYCLE -> {
                if (now == Phase.HOLDING && left() < COVER * SECOND) {
                    this.left.set(COVER * SECOND);
                    this.launched.set(false);
                }
            }
            case RUN -> run(now);
            default -> {
                return;
            }
        }
        this.owner.setChanged();
    }

    /**
     * Gives its pad {@code wanted} as its name, through its service port, as {@link PadServicePortBlockEntity#named}
     * keeps it; empty takes the name away. False where no port stands by its pad to keep it.
     */
    public boolean namePad(final String wanted) {
        final ServerLevel server = this.owner.getLevel() instanceof ServerLevel level ? level : null;
        if (this.port == null || server == null
                || !(server.getBlockEntity(this.port) instanceof PadServicePortBlockEntity pad)) {
            return false;
        }
        this.padName.set(pad.name(wanted));
        return true;
    }

    /** The Launch Panel at {@code at} works this count, and shows that it does on the rocket's LAUNCH page. */
    public void workedFrom(final BlockPos at) {
        this.panel.set(at.immutable());
    }

    /** Counts on, a tick at a time; looks at its pad and its rocket every second. */
    public void tick(final ServerLevel server) {
        if (--this.wait <= 0) {
            this.wait = SECOND;
            look(server);
        }
        final Phase now = this.phase.get();
        if (now == Phase.RELEASED) {
            // Once its rocket is off and no more in this world, landed or gone on, the count is free for the next.
            if (this.lifted.get() && (this.flight.get() == null
                    || MovingStructures.byId(server, this.flight.get()) == null)) {
                standDown();
                return;
            }
            this.met.set(this.met.get() + 1);
            climb(server);
            return;
        }
        if (now != Phase.COUNTING) {
            return;
        }
        if (!go() || !this.master.get()) {
            hold();
            return;
        }
        if (left() == LIGHT * SECOND) {
            if (!this.launched.get()) {
                // The LAUNCH button not pressed by ignition: the count holds there till it is.
                hold();
                return;
            }
            if (this.stand != null) {
                if (!this.stand.computer().map(computer -> RocketLaunch.lights(server, computer)).orElse(true)) {
                    // Too heavy for its engines, which the server keeps unlit: scrubbed, nothing spent.
                    scrub();
                    return;
                }
                LaunchPads.ignite(server, this.stand.axis(), this.stand.side(), this.deluge.get());
                RocketLaunch.ignite(server, this.stand.axis());
            }
        }
        final int next = left() - 1;
        this.left.set(next);
        if (next <= 0) {
            release(server);
        }
        this.owner.setChanged();
    }

    /* Whether everything a count needs is go. */
    private boolean go() {
        return go(this.vehicle.get(), this.oxidiser.get(), this.fuel.get(), this.storm.get(), this.clamps.get());
    }

    /* T-0: the rocket on the pad becomes one body and is let go; where it cannot be, the launch is scrubbed. */
    private void release(final ServerLevel server) {
        final Optional<BlockPos> computer = this.stand == null ? Optional.empty() : this.stand.computer();
        // Its crew, off the pad, find their flight computer on the page they will fly by.
        computer.map(server::getBlockEntity).filter(FlightComputerBlockEntity.class::isInstance)
                .map(FlightComputerBlockEntity.class::cast).ifPresent(FlightComputerBlockEntity::offThePad);
        final Optional<MovingStructure> body = computer.flatMap(at -> RocketLaunch.release(server, at,
                this.pilot.get()));
        if (body.isEmpty()) {
            scrub();
            return;
        }
        this.flight.set(body.get().id());
        this.lifted.set(false);
        this.phase.set(Phase.RELEASED);
        this.met.set(0);
        // Its crew aboard and whoever pressed LAUNCH have launched a rocket.
        final Set<ServerPlayer> launchedBy = new HashSet<>(CruiseFlight.aboard(server, body.get()));
        final UUID pressed = this.pilot.get();
        final ServerPlayer launcher = pressed == null ? null : server.getServer().getPlayerList().getPlayer(pressed);
        if (launcher != null) {
            launchedBy.add(launcher);
        }
        launchedBy.forEach(SpaceFirsts::launched);
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
            this.owner.setChanged();
        }
    }

    /* Starts the count, or runs it on from a hold, if its key and its ignition are armed and all is go. */
    private void run(final Phase now) {
        if (!this.master.get() || !this.ignition.get() || !go()) {
            return;
        }
        if (now == Phase.SAFE || now == Phase.SCRUBBED) {
            this.left.set(START * SECOND);
            this.launched.set(false);
            this.phase.set(Phase.COUNTING);
        } else if (now == Phase.HOLDING && (left() != LIGHT * SECOND || this.launched.get())) {
            this.phase.set(Phase.COUNTING);
        }
    }

    private void hold() {
        this.phase.set(Phase.HOLDING);
        this.holds.set(this.holds.get() + 1);
        this.owner.setChanged();
    }

    /* The launch called off: the count back to T-60, LAUNCH to press again. */
    private void scrub() {
        this.phase.set(Phase.SCRUBBED);
        this.left.set(START * SECOND);
        this.launched.set(false);
        this.owner.setChanged();
    }

    /* After a launch: safe again, its count back at the start, its key and its ignition to be armed anew. */
    private void standDown() {
        this.phase.set(Phase.SAFE);
        this.flight.set(null);
        this.lifted.set(false);
        this.launched.set(false);
        this.met.set(0);
        this.left.set(START * SECOND);
        this.master.set(false);
        this.ignition.set(false);
        this.owner.setChanged();
    }

    /*
     * Finds its rocket up the launcher's axis and reads it, and the pad's port; tells the rocket's flight computer it
     * stands on this launcher and sets its clock to the count; and lets go of a Launch Panel no longer working it.
     */
    private void look(final ServerLevel server) {
        this.stand = LaunchPads.stand(server, this.owner).orElse(null);
        final FlightComputerBlockEntity found = this.stand == null ? null : this.stand.computer()
                .map(server::getBlockEntity).filter(FlightComputerBlockEntity.class::isInstance)
                .map(FlightComputerBlockEntity.class::cast).orElse(null);
        this.rocket.set(found == null ? null : found.getBlockPos().immutable());
        this.vehicle.set(found != null);
        this.storm.set(LaunchPads.stormHolds(server));
        // The pad's port, read once: its deluge, and who the pad is.
        final PadServicePortBlockEntity pad = this.stand == null ? null
                : LaunchPads.port(server, this.stand.axis()).orElse(null);
        this.port = pad == null ? null : pad.getBlockPos();
        this.delugeReady.set(pad != null && pad.ready());
        this.padNumber.set(pad == null ? 0 : pad.number());
        this.padName.set(pad == null ? "" : pad.name());
        this.name.set(found == null ? "" : found.rocket().map(RocketDesign::name).orElse(""));
        final int[] full = found == null ? new int[2] : full(server, found);
        this.oxidiser.set(full[0]);
        this.fuel.set(full[1]);
        final BlockPos working = this.panel.get();
        if (working != null && !(server.getBlockEntity(working) instanceof LaunchPanelBlockEntity console
                && console.works(this.owner.getBlockPos()))) {
            this.panel.set(null);
        }
        if (found != null) {
            found.standsOn(this.owner.getBlockPos());
            final Phase now = this.phase.get();
            found.clock(now == Phase.RELEASED ? this.met.get() / SECOND : -(left() + SECOND - 1) / SECOND);
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

    /**
     * All a count shows, read at once: on the Launch Panel and on the rocket's LAUNCH page.
     *
     * @param phase       where the count is
     * @param left        the ticks left before release
     * @param met         the mission's ticks since release
     * @param holds       how many times the count has held
     * @param deluge      whether the deluge is on automatic
     * @param clamps      whether the clamps are on automatic
     * @param ignition    whether the ignition is armed
     * @param guard       whether the ignition's guard is lifted
     * @param master      whether the MASTER key is armed
     * @param launched    whether LAUNCH has been pressed in this count
     * @param vehicle     whether a rocket keeping its plan stands on the launcher
     * @param oxidiser    how full its rocket's oxidiser is, in hundredths
     * @param fuel        how full its rocket's fuel is, in hundredths
     * @param delugeReady whether the pad's service port holds a whole deluge
     * @param storm       whether a thunderstorm holds the count
     * @param rocketName  the name of the rocket on the pad, or empty
     * @param padNumber   the pad's number in its world, from 1; 0 where no service port stands by the pad
     * @param padName     the name the pad was given, or empty
     * @param panel       whether a Launch Panel by the pad works the count too
     */
    public record Shown(Phase phase, int left, int met, int holds, boolean deluge, boolean clamps, boolean ignition,
                        boolean guard, boolean master, boolean launched, boolean vehicle, int oxidiser, int fuel,
                        boolean delugeReady, boolean storm, String rocketName, int padNumber, String padName,
                        boolean panel) {

        /** No launcher in reach: safe, nothing on the pad. */
        public static final Shown NONE = new Shown(Phase.SAFE, START * SECOND, 0, 0, true, true, false, false, false,
                false, false, 0, 0, false, false, "", 0, "", false);

        /** Whether the LAUNCH button's cover is up. */
        public boolean coverUp() {
            return LaunchCount.coverUp(this.phase, this.left);
        }

        /** Whether everything a count needs is go. */
        public boolean go() {
            return LaunchCount.go(this.vehicle, this.oxidiser, this.fuel, this.storm, this.clamps);
        }

        /** Whether the rocket's oxidiser and fuel are both loaded. */
        public boolean loaded() {
            return this.oxidiser >= FULL && this.fuel >= FULL;
        }

        /** Whether a service port stands by the pad, which holds its deluge and gives it its number. */
        public boolean port() {
            return this.padNumber > 0;
        }
    }
}
