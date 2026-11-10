/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.computers.block.AbstractSmallComputerBlock;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.PrebuilderBlockEntity;
import dev.jstech.computers.client.chain.ChainMachineRenderer;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.IKitExtras;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelBone;
import dev.jstech.core.model.ModelPose;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

/**
 * Draws the formed Prebuilder from its terminal (computers/docs/design/survival.md), as its approved mock draws it,
 * moved by how far it is through the computer it builds ({@link PrebuilderJob}): the case of that computer on the lift
 * with its open side to the front, drawn by the small computers' own renderer with the parts it has so far; the parts
 * still to go in standing in the two kitting trays, and the one each arm carries in its gripper. Each arm is placed
 * pin by pin as a real one moves: its turret slewing under it, its upper arm and forearm solved for its wrist with the
 * elbow up, each hydraulic cylinder's barrel and rod between its two pins and the hose stretched between its own, the
 * wrist turned to the tool, the telescope's stages run out one after the other, and at its end the suction cup or the
 * two fingers, sized to the part and open or shut. Idle, the lift and the trays are down and the arms at rest.
 */
public final class PrebuilderRenderer implements IKitDriver<ChainMachineBlockEntity>,
        IKitExtras<ChainMachineBlockEntity> {

    private final ChainMachineRenderer base;

    /* Each machine's job, for the computer it builds now. */
    private static final Map<ChainMachineBlockEntity, Kept> JOBS = new WeakHashMap<>();
    private static final String[] ARMS = {"arm_a_", "arm_b_"};
    private static final String[] TRAYS = {"tray_left", "tray_right"};
    private static final float PIXELS = 16F;
    private static final float PIXEL = 1F / PIXELS;
    private static final Vector3fc UP = new Vector3f(0, 1, 0);
    private static final Vector3fc BACK = new Vector3f(0, 0, 1);
    /* How near the case may be to all the way down and still be drawn, in blocks. */
    private static final float SHOWN = 0.01F;
    /* The heads as the mock sizes them to a part, in blocks: the cup, its head, the fingers' reach and set. */
    private static final float CUP_MOST = PIXEL;
    private static final float CUP_SHARE = 0.45F;
    private static final float HEAD_MOST = 1.25F * PIXEL;
    private static final float HEAD_RIM = 0.45F * PIXEL;
    private static final float JAW_RIM = 0.35F * PIXEL;
    private static final float FINGER_CLEAR = 0.15F * PIXEL;
    private static final float FINGER_FOOT = 0.2F * PIXEL;
    private static final float FINGER_MOST = 1.2F * PIXEL;
    private static final float FINGER_LEAST = 0.6F * PIXEL;
    private static final float FINGER_SHORT = 0.4F * PIXEL;
    private static final float REACH_MOST = 1.4F * PIXEL;
    private static final float REACH_SHARE = 0.6F;

    private PrebuilderRenderer(final ChainMachine machine) {
        this.base = ChainMachineRenderer.of(machine);
    }

    /** The renderer of the Prebuilder's terminal. */
    public static BlockEntityRenderer<ChainMachineBlockEntity> create(final ChainMachine machine) {
        final PrebuilderRenderer drawn = new PrebuilderRenderer(machine);
        return KitBlockEntityRenderer.<ChainMachineBlockEntity>formedOnly(machine.modelId())
                .driver(drawn).extras(drawn).build();
    }

    @Override
    public void drive(final ChainMachineBlockEntity terminal, final ModelAnimator animator, final double seconds) {
        base.drive(terminal, animator, seconds);
    }

    @Override
    public void move(final ChainMachineBlockEntity terminal, final ModelPose pose, final double seconds) {
        base.move(terminal, pose, seconds);
        final PrebuilderRig rig = PrebuilderRig.of();
        if (rig == null) {
            return;
        }
        final Moment now = Moment.of(terminal, rig);
        final float down = now.job().trays(now.a());
        for (int k = 0; k < TRAYS.length; k++) {
            final int tray = pose.index(TRAYS[k]);
            if (tray >= 0) {
                pose.addMove(tray, 0, -down * rig.trays()[k].drop() * PIXELS, 0);
            }
        }
        final float lift = now.job().lift(now.a());
        for (int arm = 0; arm < ARMS.length; arm++) {
            pose(pose, rig, ARMS[arm], rig.arms()[arm], now.job().arm(arm, now.a(), lift));
        }
    }

    @Override
    public boolean movesBones() {
        return true;
    }

    /* The case and what it has so far, the parts in the trays and the ones the arms carry. */
    @Override
    public void draw(final ChainMachineBlockEntity terminal, final KitInstance instance, final PoseStack poses,
                     final MultiBufferSource buffers, final int light, final int overlay, final float partialTick) {
        final PrebuilderRig rig = PrebuilderRig.of();
        if (rig == null || !(terminal instanceof PrebuilderBlockEntity prebuilder)) {
            return;
        }
        final Moment now = Moment.of(terminal, rig);
        final PrebuilderJob job = now.job();
        final float lift = job.lift(now.a());
        final ItemStack building = prebuilder.building();
        if (now.working() && lift > SHOWN - rig.sink()) {
            poses.pushPose();
            poses.translate(rig.caseAt().x(), rig.caseAt().y() + lift, rig.caseAt().z());
            poses.mulPose(rig.caseTurn());
            if (building.getItem() instanceof BlockItem item
                    && item.getBlock() instanceof AbstractSmallComputerBlock<?>) {
                poses.translate(-0.5F, 0, -0.5F);
                ComputerDummies.draw(terminal, building, slot -> job.seated(slot, now.a()), true, poses, buffers,
                        light, overlay, partialTick);
            } else {
                poses.translate(0, 0.5F, 0);
                Minecraft.getInstance().getItemRenderer().renderStatic(building, ItemDisplayContext.FIXED, light,
                        overlay, poses, buffers, terminal.getLevel(), 0);
            }
            poses.popPose();
        }
        final float down = job.trays(now.a());
        for (final PrebuilderJob.Trip trip : job.trips()) {
            if (job.inTray(trip, now.a())) {
                part(terminal, trip, job.inTrayAt(trip, down), poses, buffers, light, overlay, partialTick);
            }
        }
        for (int arm = 0; arm < ARMS.length; arm++) {
            final PrebuilderJob.Arm state = job.arm(arm, now.a(), lift);
            if (state.held() != null && state.heldAt() != null) {
                part(terminal, state.held(), state.heldAt(), poses, buffers, light, overlay, partialTick);
            }
        }
    }

    /* A part where {@code at} stands it, as a machine holds it. */
    private static void part(final ChainMachineBlockEntity owner, final PrebuilderJob.Trip trip, final Matrix4f at,
                             final PoseStack poses, final MultiBufferSource buffers, final int light,
                             final int overlay, final float partialTick) {
        poses.pushPose();
        poses.mulPose(at);
        PartModels.loose(owner, PartModels.Drawer.key(trip.slot()), trip.item(), poses, buffers, light, overlay,
                partialTick);
        poses.popPose();
    }

    /*
     * Places one arm's bones for its state: the elbow solved up for the wrist, the turret turned to it, each link pin
     * to pin, the cylinders and the hose between their pins, the telescope run out and the head sized and opened.
     */
    private static void pose(final ModelPose pose, final PrebuilderRig rig, final String name,
                             final PrebuilderRig.Arm arm, final PrebuilderJob.Arm state) {
        final Vector3fc shoulder = arm.shoulder();
        final Vector3f wrist = state.wrist();
        final Vector3f reach = new Vector3f(wrist).sub(shoulder);
        final Vector3f heading = new Vector3f(reach.x(), 0, reach.z());
        final float out = heading.length();
        if (out > 1e-5F) {
            heading.div(out);
        } else {
            heading.set(BACK);
        }
        final float upper = arm.upper();
        final float fore = arm.fore();
        final float span = Math.clamp((float) Math.hypot(out, reach.y()), Math.abs(upper - fore) + 1e-4F,
                upper + fore - 1e-4F);
        final double raised = Math.atan2(reach.y(), out) + Math.acos(Math.clamp(
                (upper * upper + span * span - fore * fore) / (2 * upper * span), -1, 1));
        final Vector3f elbow = new Vector3f(heading).mul((float) (upper * Math.cos(raised)))
                .add(0, (float) (upper * Math.sin(raised)), 0).add(shoulder);
        final Vector3f side = new Vector3f(UP).cross(heading).normalize();
        final Matrix3f turret = new Matrix3f(side, UP, heading);
        final Matrix3f upperArm = along(new Vector3f(elbow).sub(shoulder), side);
        final Matrix3f foreArm = along(new Vector3f(wrist).sub(elbow), side);
        final Vector3f liftA = turret.transform(new Vector3f(arm.liftA())).add(arm.foot());
        final Vector3f liftB = upperArm.transform(new Vector3f(arm.liftB())).add(shoulder);
        final Vector3f foreA = upperArm.transform(new Vector3f(arm.foreA())).add(shoulder);
        final Vector3f foreB = foreArm.transform(new Vector3f(arm.foreB())).add(elbow);
        final Vector3f hoseA = turret.transform(new Vector3f(arm.hoseA())).add(arm.foot());
        final Vector3f hoseB = upperArm.transform(new Vector3f(arm.hoseB())).add(shoulder);
        place(pose, name + "turret", turret, arm.foot(), 1, 1, 1);
        place(pose, name + "upper", upperArm, shoulder, 1, 1, 1);
        place(pose, name + "fore", foreArm, elbow, 1, 1, 1);
        place(pose, name + "lift_barrel", along(new Vector3f(liftB).sub(liftA), side), liftA, 1, 1, 1);
        place(pose, name + "lift_rod", along(new Vector3f(liftA).sub(liftB), side), liftB, 1, 1, 1);
        place(pose, name + "fore_barrel", along(new Vector3f(foreB).sub(foreA), side), foreA, 1, 1, 1);
        place(pose, name + "fore_rod", along(new Vector3f(foreA).sub(foreB), side), foreB, 1, 1, 1);
        place(pose, name + "hose", along(new Vector3f(hoseB).sub(hoseA), side), hoseA, 1, 1,
                hoseA.distance(hoseB) / arm.hoseRest());
        // The wrist turned to the tool, and each stage of the telescope out of the one before by the same length.
        final Matrix3f tool = state.tool();
        final Vector3f axis = tool.getColumn(2, new Vector3f());
        place(pose, name + "wrist", tool, wrist, 1, 1, 1);
        final float step = (state.ext() - rig.drawnIn()) / Math.max(1, rig.stages() - 1);
        for (int stage = 0; stage < rig.stages(); stage++) {
            place(pose, name + "stage_" + stage, tool, new Vector3f(axis).mul(stage * step).add(wrist), 1, 1, 1);
        }
        head(pose, rig, name, state, new Vector3f(axis).mul(state.ext()).add(wrist));
    }

    /* The head at the telescope's end: the cup, or the two fingers set to the part they are tooled for. */
    private static void head(final ModelPose pose, final PrebuilderRig rig, final String name,
                             final PrebuilderJob.Arm state, final Vector3f face) {
        final PrebuilderJob.Trip tooled = state.tooled();
        final PrebuilderJob.Grip grip = tooled == null ? null : tooled.grip();
        final boolean cup = grip == null || grip.cup();
        final Matrix3f tool = state.tool();
        show(pose, name + "cup", cup);
        show(pose, name + "jaw", !cup);
        show(pose, name + "finger_0", !cup);
        show(pose, name + "finger_1", !cup);
        if (cup) {
            final float radius = grip == null ? rig.cup() : Math.min(CUP_MOST, CUP_SHARE * grip.face());
            final float half = Math.min(HEAD_MOST, radius + HEAD_RIM) / rig.cupHead();
            place(pose, name + "cup", tool, face, half, half, 1);
            return;
        }
        // The fingers close across the part, as long as its side lets them, and reach in as far as its depth does,
        // in the mock's sixteenths: at most 1.2 each way, at least 0.6, and 0.4 short of the side's far end.
        final float low = Math.min(Math.max(grip.besideLow(), Math.min(-FINGER_MOST, grip.besideHigh() - FINGER_SHORT)),
                -FINGER_LEAST);
        final float high = Math.max(Math.min(grip.besideHigh(), Math.max(FINGER_MOST, grip.besideLow() + FINGER_SHORT)),
                FINGER_LEAST);
        final float reach = rig.gap() + Math.min(REACH_MOST, REACH_SHARE * grip.depth());
        final float spread = grip.halfAcross() + FINGER_CLEAR + rig.open() * state.opened();
        place(pose, name + "jaw", tool, face, (spread + JAW_RIM) / (rig.jawSpan() + JAW_RIM), 1, 1);
        for (int finger = 0; finger < 2; finger++) {
            final float way = finger == 0 ? -1 : 1;
            final Vector3f at = tool.transform(new Vector3f(way * (spread - rig.jawSpan()), (low + high) / 2, 0))
                    .add(face);
            place(pose, name + "finger_" + finger, tool, at, 1, (high - low) / (2 * rig.finger()),
                    (reach + FINGER_FOOT) / (rig.reach() + FINGER_FOOT));
        }
    }

    /*
     * Places bone {@code name} with its frame turned {@code turn} and its own origin at {@code at}, in blocks, sized
     * {@code x}, {@code y}, {@code z} along its own axes: the bones are flat, each placed in the model's space.
     */
    private static void place(final ModelPose pose, final String name, final Matrix3fc turn, final Vector3fc at,
                              final float x, final float y, final float z) {
        final int bone = pose.index(name);
        if (bone < 0) {
            return;
        }
        final ModelBone rest = pose.model().bones().get(bone);
        final Quaternionf turned = new Quaternionf().setFromUnnormalized(turn);
        pose.setTurn(bone, turned);
        pose.multiplySize(bone, x, y, z);
        // Its origin lands at its rest place, its move and its pivot, less its pivot turned and sized.
        final Vector3f pivot = new Vector3f(rest.pivot()).mul(rest.scale()).mul(x, y, z);
        turned.transform(pivot);
        pose.addMove(bone, at.x() * PIXELS - rest.translation().x() - rest.pivot().x() + pivot.x(),
                at.y() * PIXELS - rest.translation().y() - rest.pivot().y() + pivot.y(),
                at.z() * PIXELS - rest.translation().z() - rest.pivot().z() + pivot.z());
    }

    private static void show(final ModelPose pose, final String name, final boolean shown) {
        final int bone = pose.index(name);
        if (bone >= 0) {
            pose.setHidden(bone, !shown);
        }
    }

    /* A frame whose third axis runs along {@code way} and whose first is {@code side} made square to it. */
    private static Matrix3f along(final Vector3f way, final Vector3fc side) {
        final Vector3f z = way.normalize();
        final Vector3f x = new Vector3f(side).sub(new Vector3f(z).mul(z.dot(side))).normalize();
        final Vector3f y = new Vector3f(z).cross(x);
        return new Matrix3f(x, y, z);
    }

    /* A machine's job, kept for the computer it was worked out for while the models stay as they were read. */
    private record Kept(ItemStack building, long models, PrebuilderRig rig, PrebuilderJob job) {
    }

    /*
     * Where a machine is now: its job (an idle one with nothing to bring while it builds nothing), how far into the
     * job's loop it is, and whether it is building.
     */
    private record Moment(PrebuilderJob job, float a, boolean working) {

        static Moment of(final ChainMachineBlockEntity terminal, final PrebuilderRig rig) {
            if (!(terminal instanceof PrebuilderBlockEntity prebuilder) || prebuilder.building().isEmpty()
                    || terminal.status() != MachineStatus.WORKING) {
                return new Moment(idle(terminal, rig), 0, false);
            }
            final PrebuilderJob job = job(prebuilder, rig);
            return new Moment(job, job.at(prebuilder.done()), true);
        }

        private static PrebuilderJob idle(final ChainMachineBlockEntity terminal, final PrebuilderRig rig) {
            final Kept kept = JOBS.get(terminal);
            if (kept != null && kept.building().isEmpty() && kept.rig() == rig) {
                return kept.job();
            }
            final PrebuilderJob job = PrebuilderJob.of(rig, ItemContainerContents.EMPTY, Map.of());
            JOBS.put(terminal, new Kept(ItemStack.EMPTY, KitModels.reloadedAt(), rig, job));
            return job;
        }

        /* The job for what it builds now: worked out again only when that changes, or the models are read again. */
        private static PrebuilderJob job(final PrebuilderBlockEntity prebuilder, final PrebuilderRig rig) {
            final ItemStack building = prebuilder.building();
            final Kept kept = JOBS.get(prebuilder);
            if (kept != null && kept.rig() == rig && kept.models() == KitModels.reloadedAt()
                    && ItemStack.matches(kept.building(), building)) {
                return kept.job();
            }
            final Map<String, Matrix4f> seats = ComputerDummies.seats(prebuilder, building);
            final PrebuilderJob job = PrebuilderJob.of(rig, building.getOrDefault(
                    ComputingComponents.ASSEMBLED_HARDWARE.get(), ItemContainerContents.EMPTY), seats);
            // Seats not read yet (the case's model still loading) are asked for again the next time.
            if (!seats.isEmpty()) {
                JOBS.put(prebuilder, new Kept(building.copy(), KitModels.reloadedAt(), rig, job));
            }
            return job;
        }
    }
}
