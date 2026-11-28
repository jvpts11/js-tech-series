/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.chain;

import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.client.chain.ChainEmitters.Emitter;
import dev.jstech.core.client.effect.ClientEffects;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import dev.jstech.core.multiblock.Orientation;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Draws a formed chain machine from its terminal, as its approved mock draws it (computers/docs/design/survival.md):
 * its model over the whole machine, playing its working loop while it works and at rest otherwise; what goes through
 * it (the ore falling into a crusher) and the glow of its work only while it works, so an idle one stands empty and
 * cold, drawn by the bones only an idle machine has (a heater not glowing); a tool (a station's mask) while one is
 * in its slot, worked or not; only the bones of
 * the mode it works in (a tube or a flat screen); the bones of each channel painted in the colour of the material at
 * work; the things of the recipe at its locators (the moulder's body and its mould); and, while it works, what it
 * gives off, each emitter an effect held in the world where the machine stands,
 * turned as it was built. Nothing is drawn while it is not formed: its blocks show.
 */
public final class ChainMachineRenderer implements IKitDriver<ChainMachineBlockEntity> {

    private final ChainMachine machine;

    /* The machines giving off their particles now, each with the holder of each emitter it holds. Held strongly: a
     * machine let go of without its emitters released would leave them giving off where it stood; each tick lets go
     * of the removed ones. */
    private static final Map<ChainMachineBlockEntity, Held> HELD = new HashMap<>();
    private static final String MODE = "mode_";
    /* The model's bones of what goes through the machine, a mode's own after the mode's name. */
    private static final String WORK = "work_";
    /* The model's bones only an idle machine has, in their place while it works: a heater cold. */
    private static final String IDLE = "idle_";
    /* The model's bones of a tool in its slot (a station's mask), shown while one is there, worked or not. */
    private static final String TOOL = "tool_";
    private static final String TINT = "_tint_";
    private static final String GLOW = "_glow";
    private static final String WORKING = "working";
    private static final double BLEND = 0.3;
    private static final int OPAQUE = 0xFF000000;

    private ChainMachineRenderer(final ChainMachine machine) {
        this.machine = machine;
    }

    /** The renderer of {@code machine}'s terminal. */
    public static BlockEntityRenderer<ChainMachineBlockEntity> create(final ChainMachine machine) {
        return KitBlockEntityRenderer.<ChainMachineBlockEntity>formedOnly(machine.modelId())
                .driver(new ChainMachineRenderer(machine))
                .held(ChainReflect::held)
                .build();
    }

    /**
     * What every chain machine's model does, for a machine whose renderer does more: its loop while it works, its
     * modes and channels, and what it gives off.
     */
    public static ChainMachineRenderer of(final ChainMachine machine) {
        return new ChainMachineRenderer(machine);
    }

    /** Lets go of what removed machines held, each tick of the game, and of everything as the player leaves. */
    public static void listen() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> release(false));
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> release(true));
    }

    @Override
    public void drive(final ChainMachineBlockEntity terminal, final ModelAnimator animator, final double seconds) {
        final boolean working = working(terminal);
        if (working) {
            animator.play("main", WORKING, seconds);
        } else {
            animator.controller("main").stop(seconds, BLEND);
        }
        emit(terminal, working);
    }

    @Override
    public void move(final ChainMachineBlockEntity terminal, final ModelPose pose, final double seconds) {
        final String mode = MODE + ChainReflect.mode(terminal) + "_";
        final boolean working = working(terminal);
        for (int bone = 0; bone < pose.bones(); bone++) {
            final String name = pose.model().bones().get(bone).name();
            if (name.startsWith(MODE) && !name.startsWith(mode) || marked(name, WORK) && !working
                    || marked(name, IDLE) && working || marked(name, TOOL) && terminal.tools().isEmpty()) {
                pose.setHidden(bone, true);
            }
            final int at = name.indexOf(TINT);
            if (at >= 0) {
                final String channel = name.substring(at + TINT.length()).replace(GLOW, "");
                pose.setTint(bone, OPAQUE | ChainReflect.colour(terminal, channel));
            }
        }
    }

    @Override
    public boolean movesBones() {
        return true;
    }

    /*
     * Holds the machine's emitters while it works, in its mode and its material's colours, again whenever its work
     * changes; lets them go once it stops.
     */
    private void emit(final ChainMachineBlockEntity terminal, final boolean working) {
        final Held held = HELD.get(terminal);
        final String mode = ChainReflect.mode(terminal);
        if (!working) {
            if (held != null) {
                HELD.remove(terminal).release();
            }
            return;
        }
        final String work = mode + terminal.takes() + terminal.makes();
        if (held != null && held.work().equals(work)) {
            return;
        }
        if (held != null) {
            held.release();
        }
        final Orientation standing = terminal.structure().orientation().orElse(Orientation.NORTH);
        final Vec3 floor = Vec3.atBottomCenterOf(terminal.getBlockPos());
        final List<Object> holders = new ArrayList<>();
        for (final Emitter emitter : ChainEmitters.of(machine.id())) {
            if (emitter.mode() != null && !emitter.mode().equals(mode)) {
                continue;
            }
            final Object holder = new Object();
            final Vec3 at = floor.add(turned(standing, emitter.at()));
            final Vec3 way = turned(standing, emitter.way());
            if (emitter.tint() != null) {
                ClientEffects.hold(holder, emitter.effect(), () -> at, () -> way,
                        ChainReflect.colour(terminal, emitter.tint()));
            } else {
                ClientEffects.hold(holder, emitter.effect(), () -> at, () -> way);
            }
            holders.add(holder);
        }
        HELD.put(terminal, new Held(work, holders));
    }

    /* Whether bone {@code name} is marked {@code mark}: first, or after its mode's name. */
    private static boolean marked(final String name, final String mark) {
        return name.startsWith(mark) || name.startsWith(MODE) && name.contains("_" + mark);
    }

    /* Whether the machine stands formed and at work. */
    private static boolean working(final ChainMachineBlockEntity terminal) {
        return terminal.formed() && terminal.status() == MachineStatus.WORKING;
    }

    /* A place on the machine as it stands facing north, turned as it was built. */
    private static Vec3 turned(final Orientation standing, final double[] at) {
        final double[] from = {at[0], at[1], at[2]};
        return new Vec3(standing.xSign() * from[standing.xFrom()], standing.ySign() * from[standing.yFrom()],
                standing.zSign() * from[standing.zFrom()]);
    }

    /*
     * Lets go of what removed machines hold, and those no longer formed and working, or of everything: a machine whose
     * structure broke is no longer drawn, so its renderer never hears it stopped.
     */
    private static void release(final boolean everything) {
        HELD.entrySet().removeIf(entry -> {
            final ChainMachineBlockEntity terminal = entry.getKey();
            if (everything || terminal.isRemoved() || !terminal.formed()
                    || terminal.status() != MachineStatus.WORKING) {
                entry.getValue().release();
                return true;
            }
            return false;
        });
    }

    /* What a machine holds: the work it was held for, and the holder of each emitter. */
    private record Held(String work, List<Object> holders) {

        void release() {
            holders.forEach(ClientEffects::release);
        }
    }
}
