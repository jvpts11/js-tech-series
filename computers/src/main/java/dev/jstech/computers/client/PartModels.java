/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitClock;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.model.IAnimationListener;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelBone;
import dev.jstech.core.model.ModelData;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.model.ModelPose;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The parts installed in a computer, each drawn by a model of its own chosen by the item, as the small computers and
 * the Mainframe draw them: {@code geo/part/<item>.geo.json} and its atlas, the point the part plugs in by at its
 * origin, so a part made for one age sits on the seat of another age's board or case.
 *
 * <p>A part's model says what it is by its one root bone ({@code board}, {@code cpu} or {@code cartridge},
 * {@code ram}, {@code card}, {@code psu_<form>}, {@code disk_<form>}, {@code cooler}, {@code fan}); its fans are bones
 * {@code spin_<axis>_<name>}, turned while the machine runs; a supply's leads are bones {@code lead_<age>}, one kept;
 * and a part that sits more than one way carries each way as a bone {@code way_<name>}, only the one it sits in drawn.
 * A part with a model for a case's light, {@code <item>_<light>}, is drawn by it in a case of that light.
 */
final class PartModels {

    /** A part's fan is a bone named for the axis it turns about: spin_x_, spin_y_ or spin_z_ and its name. */
    private static final String SPIN = "spin_";
    private static final String LEAD = "lead_";
    /** One of the ways a part sits, a bone of the part's root: way_<name>. */
    private static final String WAY = "way_";
    private static final String CARTRIDGE = "cartridge";
    private static final String COOLER = "cooler";
    private static final String PSU = "psu_";
    private static final String PSU_TOP = "top";
    /** Under the Advanced cases' cover only that case's own supply is drawn: any other could not be seen. */
    private static final String PSU_COVER = "cover";
    private static final String[] PSU_FORMS = {PSU_TOP, "floor", PSU_COVER};
    private static final String[] PSU_SEATS = {"seat_psu_top", "seat_psu_floor", "seat_psu_cover"};
    /** The ticks a fan takes to turn once. */
    private static final int FAN_TICKS = 20;
    private static final float FULL_TURN = 360f;
    /** Half a supply's height, in the model's units: a supply turned over turns about its middle. */
    private static final float PSU_HALF_HEIGHT = 1.75f;
    private static final float UNITS = 16f;
    /** How many numbered names of one kind are kept; one past them is made when it is drawn. */
    private static final int NAMED = 16;
    /** Each hardware slot's part is drawn by a model of its own, kept by the slot it is in. */
    private static final String[] SLOT_PARTS = DeviceLamps.numbered("slot_", NAMED * 4);

    /* Where each part's model and texture are, by the item that names it, until the models are read again. */
    private static final Map<ResourceLocation, PartFiles> PART_FILES = new HashMap<>();
    /* The model of each part for each case light, by the item: <item>_<light>. */
    private static final Map<String, Map<ResourceLocation, ResourceLocation>> LIT_PARTS = new HashMap<>();
    private static final ModelFacts<PartShape> SHAPES = new ModelFacts<>(PartModels::shapeOf);
    /* What moves the part being drawn, one at a time on the thread that draws. */
    private static final PartFrame FRAME = new PartFrame();
    /* The part's moves: its fans turned, its supply's leads and its ways chosen. */
    private static final IKitDriver<PartFrame> PART_MOVES = new IKitDriver<>() {
        @Override
        public void drive(final PartFrame frame, final ModelAnimator animator, final double seconds) {
        }

        @Override
        public void move(final PartFrame frame, final ModelPose pose, final double seconds) {
            frame.move(pose);
        }

        @Override
        public boolean movesBones() {
            return true;
        }
    };

    private static long partFilesReadAt = Long.MIN_VALUE;

    private PartModels() {
    }

    /** The part an item is drawn as, or null for no item or an item with no model of its own. */
    static @Nullable Part part(@Nullable final ResourceLocation item) {
        if (item == null) {
            return null;
        }
        if (partFilesReadAt != KitModels.reloadedAt()) {
            PART_FILES.clear();
            partFilesReadAt = KitModels.reloadedAt();
        }
        final PartFiles files = PART_FILES.computeIfAbsent(item, PartFiles::of);
        if (!files.there()) {
            return null;
        }
        final Optional<KitModel> model = KitModels.look(files.geo(), files.texture(), files.animation());
        if (model.isEmpty()) {
            return null;
        }
        final PartShape shape = SHAPES.of(model.get().data());
        return shape.kind() == null ? null : new Part(model.get(), shape);
    }

    /** The part an item is drawn as in a case of {@code light}: its model for that light when it has one. */
    static @Nullable Part lit(@Nullable final ResourceLocation item, @Nullable final String light) {
        if (item == null || light == null) {
            return part(item);
        }
        final Part own = part(LIT_PARTS.computeIfAbsent(light, named -> new HashMap<>())
                .computeIfAbsent(item, id -> id.withSuffix("_" + light)));
        return own != null ? own : part(item);
    }

    /** The k-th of names kept, or one made for a k past them. */
    static String nth(final String[] names, final String prefix, final int k) {
        return k < names.length ? names[k] : prefix + k;
    }

    /* What a part's model is: its one root bone's name, its fans, its supply's leads and the ways it sits. */
    private static PartShape shapeOf(final ModelData model) {
        final List<ModelBone> bones = model.bones();
        String root = null;
        int roots = 0;
        for (final ModelBone bone : bones) {
            if (bone.parent() == null) {
                root = bone.name();
                roots++;
            }
        }
        final List<Integer> spins = new ArrayList<>();
        final List<Integer> leads = new ArrayList<>();
        final List<Integer> ways = new ArrayList<>();
        for (int i = 0; i < bones.size(); i++) {
            final String name = bones.get(i).name();
            final boolean onRoot = root != null && root.equals(bones.get(i).parent());
            if (name.startsWith(SPIN) && name.length() > SPIN.length()) {
                spins.add(i);
            } else if (name.startsWith(LEAD) && onRoot) {
                leads.add(i);
            } else if (name.startsWith(WAY) && onRoot) {
                ways.add(i);
            }
        }
        final int[] spinBones = indices(spins);
        final char[] axes = new char[spinBones.length];
        for (int k = 0; k < spinBones.length; k++) {
            axes[k] = bones.get(spinBones[k]).name().charAt(SPIN.length());
        }
        return new PartShape(roots == 1 ? root : null, spinBones, axes, indices(leads), names(bones, leads),
                indices(ways), names(bones, ways));
    }

    private static int[] indices(final List<Integer> picked) {
        return picked.stream().mapToInt(Integer::intValue).toArray();
    }

    private static String[] names(final List<ModelBone> bones, final List<Integer> picked) {
        final String[] out = new String[picked.size()];
        for (int k = 0; k < out.length; k++) {
            out[k] = bones.get(picked.get(k)).name();
        }
        return out;
    }

    /** A part's model, as it is drawn. */
    record Part(KitModel model, PartShape shape) {

        @Nullable String kind() {
            return shape.kind();
        }
    }

    /**
     * What a part's model is, worked out once from its bones: the name of its one root bone, which says the kind of
     * part, or none for a model of more roots than one; the bones of its fans and the axes they turn about; the leads
     * of a supply, each named for the age of the board it runs to; and the ways it sits.
     */
    record PartShape(@Nullable String kind, int[] spins, char[] axes, int[] leads, String[] leadNames,
                     int[] ways, String[] wayNames) {

        boolean moves() {
            return spins.length > 0 || leads.length > 0 || ways.length > 0;
        }
    }

    /** Where a part's model, texture and animations are. */
    private record PartFiles(ResourceLocation geo, ResourceLocation texture, ResourceLocation animation,
                             boolean there) {

        static PartFiles of(final ResourceLocation item) {
            final String namespace = item.getNamespace();
            final ResourceLocation geo = ResourceLocation.fromNamespaceAndPath(namespace,
                    "geo/part/" + item.getPath() + ".geo.json");
            return new PartFiles(geo,
                    ResourceLocation.fromNamespaceAndPath(namespace, "textures/block/part/" + item.getPath() + ".png"),
                    ResourceLocation.fromNamespaceAndPath(namespace,
                            "animations/part/" + item.getPath() + ".animation.json"),
                    Minecraft.getInstance().getResourceManager().getResource(geo).isPresent());
        }
    }

    /*
     * What moves the part being drawn: how far its fans have turned, the one lead its supply keeps and the one way it
     * sits.
     */
    private static final class PartFrame {

        private PartShape shape;
        private float turned;
        private @Nullable String lead;
        private @Nullable String way;

        /* Turns the fans about their axes, keeps only the supply's lead asked for and only the way it sits. */
        void move(final ModelPose pose) {
            for (int k = 0; k < shape.spins().length; k++) {
                final char axis = shape.axes()[k];
                pose.addEuler(shape.spins()[k], axis == 'x' ? turned : 0f, axis == 'y' ? turned : 0f,
                        axis == 'z' ? turned : 0f);
            }
            if (lead != null) {
                for (int k = 0; k < shape.leads().length; k++) {
                    pose.setHidden(shape.leads()[k], !shape.leadNames()[k].equals(lead));
                }
            }
            // A part that sits more than one way shows the one asked for, and none when it is asked for none.
            for (int k = 0; k < shape.ways().length; k++) {
                pose.setHidden(shape.ways()[k], !shape.wayNames()[k].equals(way));
            }
        }
    }

    /** One machine's parts drawn in one frame. */
    static final class Drawer {

        private final BlockEntity machine;
        private final PoseStack poses;
        private final MultiBufferSource buffers;
        private final int light;
        private final int overlay;
        private final double seconds;
        /** How far the parts' fans have turned, in degrees: they stand still while the machine is off. */
        private final float turned;

        /** A supply drawn with none of its leads: a machine with no cables inside. */
        static final String NO_LEADS = "";

        Drawer(final BlockEntity machine, final boolean running, final PoseStack poses,
               final MultiBufferSource buffers, final int light, final int overlay, final float partialTick) {
            this.machine = machine;
            this.poses = poses;
            this.buffers = buffers;
            this.light = light;
            this.overlay = overlay;
            this.seconds = KitClock.seconds(partialTick);
            final Level level = machine.getLevel();
            this.turned = running && level != null
                    ? (level.getGameTime() % FAN_TICKS + partialTick) / FAN_TICKS * FULL_TURN : 0f;
        }

        /** The part in hardware slot {@code slot}, {@code item}, on {@code seat} of {@code on}; drawn or not. */
        @Nullable KitInstance slot(final int slot, @Nullable final ResourceLocation item, final KitInstance on,
                                   final String seat) {
            final Part part = part(item);
            return part == null ? null : draw(key(slot), part, on, seat, false, null, null);
        }

        /**
         * A processor on {@code seat} of {@code board}, and on it the cooler fitted over it, {@code cooler}, but on a
         * cartridge, which brings its own; a cooler that sits more than one way the way {@code way}, lit by
         * {@code lit}.
         */
        void processor(final int cpuSlot, @Nullable final ResourceLocation cpu, final int coolerSlot,
                       @Nullable final ResourceLocation cooler, final KitInstance board, final String seat,
                       @Nullable final String way, @Nullable final String lit) {
            final Part chip = part(cpu);
            if (chip == null || draw(key(cpuSlot), chip, board, seat, false, null, null) == null
                    || CARTRIDGE.equals(chip.kind())) {
                return;
            }
            final Part on = PartModels.lit(cooler, lit);
            if (on != null && COOLER.equals(on.kind())) {
                draw(key(coolerSlot), on, board, seat, false, null, way);
            }
        }

        /**
         * The supply in hardware slot {@code slot}, {@code item}, on the seat {@code machine} has for one: one made for
         * a case that holds its supply the other way up is turned over; it keeps only the lead {@code lead}.
         */
        void supply(final int slot, @Nullable final ResourceLocation item, final KitInstance machine,
                    final String lead) {
            final Part psu = part(item);
            if (psu == null || psu.kind() == null || !psu.kind().startsWith(PSU)) {
                return;
            }
            final String form = psu.kind().substring(PSU.length());
            for (int k = 0; k < PSU_FORMS.length; k++) {
                if (machine.pose().index(PSU_SEATS[k]) < 0) {
                    continue;
                }
                final String held = PSU_FORMS[k];
                if (PSU_COVER.equals(held) && !PSU_COVER.equals(form)) {
                    return;
                }
                draw(key(slot), psu, machine, PSU_SEATS[k], PSU_TOP.equals(held) != PSU_TOP.equals(form), lead, null);
                return;
            }
        }

        /*
         * Draws a part, on {@code seat} of the model {@code on} has, or where the drawing stands for none, the way
         * {@code way} when it sits more than one; the part as it is drawn, or none when that seat is not there.
         */
        @Nullable KitInstance draw(final String key, final Part part, @Nullable final KitInstance on,
                                   @Nullable final String seat, final boolean turnedOver, @Nullable final String lead,
                                   @Nullable final String way) {
            poses.pushPose();
            if (on != null && seat != null && !KitRenderer.toBone(on, seat, poses)) {
                poses.popPose();
                return null;
            }
            if (turnedOver) {
                poses.translate(0, PSU_HALF_HEIGHT / UNITS, 0);
                poses.scale(1, -1, 1);
                poses.translate(0, -PSU_HALF_HEIGHT / UNITS, 0);
            }
            final KitInstance instance = KitAnimations.part(machine, key, part.model());
            if (part.shape().moves()) {
                // The part's model is its own, so its fans, leads and ways are set for this frame before it is drawn.
                FRAME.shape = part.shape();
                FRAME.turned = turned;
                FRAME.lead = lead;
                FRAME.way = way;
                instance.animate(KitClock.frame(), seconds, PART_MOVES, FRAME, IAnimationListener.NONE);
            }
            KitRenderer.draw(instance, ModelDefinition.Variant.NONE, machine, poses, buffers, light, overlay, null,
                    machine.getLevel(), true);
            poses.popPose();
            return instance;
        }

        /** The name a hardware slot's part is kept by. */
        static String key(final int slot) {
            return nth(SLOT_PARTS, "slot_", slot);
        }
    }
}
