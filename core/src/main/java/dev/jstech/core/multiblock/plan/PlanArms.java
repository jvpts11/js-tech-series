/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.multiblock.plan;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.blockentity.IFieldPart;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * Robot arms that lay a planned build's blocks for its {@link PlanAssembler}, as a dock's welding robots do: each arm
 * runs along its rail, takes the block it is handed from its feeder, swings it over to its place, sets it down as the
 * swing ends and welds it, each step in its own time ({@link ArmMotion}). The assembler hands each block to the free
 * arm whose rail comes nearest its place, among those that reach it; an arm laying one block is not free again till it
 * has welded it.
 *
 * <p>Its owner declares it among its fields, saved and sent to the players who see it, whose games draw the arms from
 * it, and tells it where its arms stand ({@link #layout}); the arms' work goes on through a save.
 */
public final class PlanArms implements IFieldPart {

    private final Runnable onChange;
    private final List<Arm> arms = new ArrayList<>();
    /* Each arm's last block, laid or being laid; null for an arm that has laid nothing yet. */
    private final List<Job> jobs = new ArrayList<>();

    private static final Codec<List<Arm>> ARMS = Arm.CODEC.listOf();
    private static final Codec<List<Job>> JOBS = Job.CODEC.listOf();
    private static final String ARMS_KEY = "Arms";
    private static final String JOBS_KEY = "ArmJobs";

    /** Arms for a machine that runs {@code onChange} each time their work changes, to save it and send it. */
    public PlanArms(final Runnable onChange) {
        this.onChange = Objects.requireNonNull(onChange, "onChange");
    }

    /**
     * Stands the arms where {@code arms} says, in place of those it had; the blocks the old arms carried are handed
     * back to {@code back}.
     */
    public void layout(final List<Arm> next, final Consumer<Job> back) {
        if (next.equals(this.arms)) {
            return;
        }
        giveBack(back);
        this.arms.clear();
        this.arms.addAll(next);
        this.jobs.clear();
        for (int arm = 0; arm < next.size(); arm++) {
            this.jobs.add(null);
        }
        this.onChange.run();
    }

    /** Its arms, where they stand. */
    public List<Arm> arms() {
        return List.copyOf(this.arms);
    }

    /** The last block of the arm numbered {@code arm}, laid or being laid, if it has one. */
    public Optional<Job> job(final int arm) {
        return arm < 0 || arm >= this.jobs.size() ? Optional.empty() : Optional.ofNullable(this.jobs.get(arm));
    }

    /** Where the arm numbered {@code arm} is at {@code ticks}, the world's time. */
    public ArmMotion.State state(final int arm, final double ticks) {
        final ArmMotion.Rig rig = this.arms.get(arm).rig();
        final Job job = this.jobs.get(arm);
        return job == null ? ArmMotion.idle(rig) : ArmMotion.of(rig, job.rail(), point(job.tool()),
                point(Vec3.atCenterOf(job.at())), ticks - job.start());
    }

    /**
     * The free arm, at {@code now}, whose rail comes nearest the block at {@code at} and that reaches it; none when
     * every arm that reaches it is busy, or none reaches it.
     */
    public Optional<Integer> free(final BlockPos at, final long now) {
        final double[] place = point(Vec3.atCenterOf(at));
        int best = -1;
        double nearest = Double.MAX_VALUE;
        for (int arm = 0; arm < this.arms.size(); arm++) {
            final ArmMotion.Rig rig = this.arms.get(arm).rig();
            final Job job = this.jobs.get(arm);
            if (job != null && now < job.start() + ArmMotion.END || !rig.reaches(place)) {
                continue;
            }
            final double[] shoulder = rig.shoulder(rig.along(place));
            final double far = Math.pow(shoulder[0] - place[0], 2) + Math.pow(shoulder[2] - place[2], 2);
            if (far < nearest) {
                nearest = far;
                best = arm;
            }
        }
        return best < 0 ? Optional.empty() : Optional.of(best);
    }

    /** Whether any arm is free at {@code now}, done with its last block. */
    public boolean anyFree(final long now) {
        return this.jobs.stream().anyMatch(job -> job == null || now >= job.start() + ArmMotion.END);
    }

    /** Whether an arm carries a block to {@code at} that it has not set down yet. */
    public boolean carrying(final BlockPos at) {
        return this.jobs.stream().anyMatch(job -> job != null && !job.set() && job.at().equals(at));
    }

    /** How many of the block {@code block} the arms carry and have not set down yet. */
    public int carried(final ResourceLocation block) {
        return (int) this.jobs.stream().filter(job -> job != null && !job.set() && job.block().equals(block)).count();
    }

    /** The arm numbered {@code arm} takes the block {@code block} to lay at {@code at}, beginning at {@code now}. */
    public void start(final int arm, final BlockPos at, final ResourceLocation block, final long now) {
        final ArmMotion.State was = state(arm, now);
        final double[] tool = was.tool();
        this.jobs.set(arm, new Job(at.immutable(), block, now, was.rail(), new Vec3(tool[0], tool[1], tool[2]),
                false));
        this.onChange.run();
    }

    /** Each arm whose swing is over by {@code now} sets its block down, handing it to {@code setDown}. */
    public void tick(final long now, final Consumer<Job> setDown) {
        for (int arm = 0; arm < this.jobs.size(); arm++) {
            final Job job = this.jobs.get(arm);
            if (job != null && !job.set() && now >= job.start() + ArmMotion.SET) {
                final Job laid = job.laid();
                this.jobs.set(arm, laid);
                this.onChange.run();
                setDown.accept(laid);
            }
        }
    }

    /** Whether an arm is still at work: carrying a block, or welding the one it set down, at {@code now}. */
    public boolean working(final long now) {
        return this.jobs.stream().anyMatch(job -> job != null && now < job.start() + ArmMotion.END);
    }

    /** Hands each block the arms carry and have not set down to {@code back}, the arms letting it go. */
    public void giveBack(final Consumer<Job> back) {
        for (int arm = 0; arm < this.jobs.size(); arm++) {
            final Job job = this.jobs.get(arm);
            if (job != null && !job.set()) {
                this.jobs.set(arm, null);
                back.accept(job);
                this.onChange.run();
            }
        }
    }

    @Override
    public void save(final CompoundTag tag, final HolderLookup.Provider registries) {
        write(tag);
    }

    @Override
    public void load(final CompoundTag tag, final HolderLookup.Provider registries) {
        read(tag);
    }

    @Override
    public void writeClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        write(tag);
    }

    @Override
    public void readClient(final CompoundTag tag, final HolderLookup.Provider registries) {
        read(tag);
    }

    private void write(final CompoundTag tag) {
        ARMS.encodeStart(NbtOps.INSTANCE, List.copyOf(this.arms)).result().ifPresent(out -> tag.put(ARMS_KEY, out));
        final List<Job> kept = new ArrayList<>();
        for (final Job job : this.jobs) {
            kept.add(job == null ? Job.NONE : job);
        }
        JOBS.encodeStart(NbtOps.INSTANCE, kept).result().ifPresent(out -> tag.put(JOBS_KEY, out));
    }

    private void read(final CompoundTag tag) {
        this.arms.clear();
        this.jobs.clear();
        if (tag.contains(ARMS_KEY)) {
            ARMS.parse(NbtOps.INSTANCE, tag.get(ARMS_KEY)).result().ifPresent(this.arms::addAll);
        }
        final List<Job> kept = tag.contains(JOBS_KEY) ? JOBS.parse(NbtOps.INSTANCE, tag.get(JOBS_KEY)).result()
                .orElse(List.of()) : List.of();
        for (int arm = 0; arm < this.arms.size(); arm++) {
            final Job job = arm < kept.size() ? kept.get(arm) : Job.NONE;
            this.jobs.add(job.equals(Job.NONE) ? null : job);
        }
    }

    private static double[] point(final Vec3 at) {
        return new double[] {at.x, at.y, at.z};
    }

    /**
     * An arm on its rail.
     *
     * @param from   one end of its rail, where its base stands
     * @param to     the other end
     * @param feeder where the block waiting for it in its feeder stands, the block's middle
     * @param rest   where its tool's block stands at rest, from its base
     * @param upper  its upper link's length, in blocks
     * @param fore   its forearm's length
     */
    public record Arm(Vec3 from, Vec3 to, Vec3 feeder, Vec3 rest, double upper, double fore) {

        public static final Codec<Arm> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Vec3.CODEC.fieldOf("from").forGetter(Arm::from),
                Vec3.CODEC.fieldOf("to").forGetter(Arm::to),
                Vec3.CODEC.fieldOf("feeder").forGetter(Arm::feeder),
                Vec3.CODEC.fieldOf("rest").forGetter(Arm::rest),
                Codec.DOUBLE.fieldOf("upper").forGetter(Arm::upper),
                Codec.DOUBLE.fieldOf("fore").forGetter(Arm::fore)
        ).apply(instance, Arm::new));

        /** The arm as its motion reads it. */
        public ArmMotion.Rig rig() {
            return new ArmMotion.Rig(point(this.from), point(this.to), point(this.feeder), point(this.rest),
                    this.upper, this.fore);
        }
    }

    /**
     * An arm's block, laid or being laid.
     *
     * @param at    where it goes
     * @param block the block
     * @param start when the arm began, the world's time
     * @param rail  how far along its rail the arm stood as it began
     * @param tool  where its tool's block stood as it began
     * @param set   whether it is set down
     */
    public record Job(BlockPos at, ResourceLocation block, long start, double rail, Vec3 tool, boolean set) {

        public static final Codec<Job> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BlockPos.CODEC.fieldOf("at").forGetter(Job::at),
                ResourceLocation.CODEC.fieldOf("block").forGetter(Job::block),
                Codec.LONG.fieldOf("start").forGetter(Job::start),
                Codec.DOUBLE.fieldOf("rail").forGetter(Job::rail),
                Vec3.CODEC.fieldOf("tool").forGetter(Job::tool),
                Codec.BOOL.fieldOf("set").forGetter(Job::set)
        ).apply(instance, Job::new));
        /* An arm that has laid nothing, as it is saved. */
        private static final Job NONE = new Job(BlockPos.ZERO, ResourceLocation.withDefaultNamespace("air"), 0, 0,
                Vec3.ZERO, true);

        /** The same block, set down. */
        public Job laid() {
            return new Job(this.at, this.block, this.start, this.rail, this.tool, true);
        }
    }
}
