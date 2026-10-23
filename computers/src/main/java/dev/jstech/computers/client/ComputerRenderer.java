/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.block.IComputerCase;
import dev.jstech.computers.blockentity.AbstractSmallComputerBlockEntity;
import dev.jstech.computers.blockentity.CaseFront;
import dev.jstech.computers.blockentity.ComputerHardwareLayout;
import dev.jstech.computers.os.media.DiscTray;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.IKitExtras;
import dev.jstech.core.client.model.kit.KitAnimations;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.client.model.kit.KitClock;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitModel;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.client.model.kit.KitRenderer;
import dev.jstech.core.jstechseries.tier.HardwareEra;
import dev.jstech.core.model.IAnimationListener;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelBone;
import dev.jstech.core.model.ModelData;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.model.ModelPose;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a small computer as its case: the tower of its age, or the one of three cases a later machine comes in, its
 * front toward whoever placed it and its left side on or off as the player left it. The power lamp is lit while the
 * machine runs and the disk lamp blinks while it works its disk; each bay shows the part in it, or its cover, and a
 * drive's lamp blinks while the machine reads its disc, the hot-swap bay's while it works its disk; the case's fans
 * turn with the machine, by its animation, and so do the fans of the parts in it, turned here.
 *
 * <p>Inside it, each part the player installed is drawn by a model of its own, chosen by the item: the board on the
 * tray, and on the board's seats the processor, the memory, the cards and an M.2 drive; on the case's seats the supply
 * and the disks; and on the processor the cooler the case brings, unless the processor is a cartridge that brings its
 * own. A part's model has the point it plugs in by at its origin, so a part made for one age sits on the seat of
 * another age's board or case. The parts are drawn only when they can be seen: with the side off, or through the
 * glass or the mesh of a case that has some.
 */
public final class ComputerRenderer implements IKitDriver<AbstractSmallComputerBlockEntity>,
        IKitExtras<AbstractSmallComputerBlockEntity> {

    /* What the part being drawn is moved by: its fans' turn and the leads its supply keeps; one frame at a time. */
    private final PartFrame partFrame = new PartFrame();

    /** A case whose glass or mesh shows its inside carries this bone. */
    private static final String SEE_THROUGH = "see_through";
    /** The case's fans, which turn while the machine runs. */
    private static final String WORK = "work";
    private static final String WORK_CLIP = "animation.computer.work";
    private static final String CARTRIDGE = "cartridge";
    private static final String PSU = "psu_";
    private static final String PSU_TOP = "top";
    /** Under the Advanced cases' cover only that case's own supply is drawn: any other could not be seen. */
    private static final String PSU_COVER = "cover";
    private static final String[] PSU_FORMS = {PSU_TOP, "floor", PSU_COVER};
    private static final String[] PSU_SEATS = {"seat_psu_top", "seat_psu_floor", "seat_psu_cover"};
    private static final String SEAT_CPU = "seat_cpu";
    private static final String LEAD = "lead_";
    private static final String DISK = "disk_";
    private static final String M2 = "m2";
    /** A part's fan is a bone named for the axis it turns about: spin_x_, spin_y_ or spin_z_ and its name. */
    private static final String SPIN = "spin_";
    /** The ticks a fan takes to turn once, as the case's own fans turn in its animation. */
    private static final int FAN_TICKS = 20;
    private static final float FULL_TURN = 360f;
    /** Half a supply's height, in the model's units: a supply turned over turns about its middle. */
    private static final float PSU_HALF_HEIGHT = 1.75f;
    private static final float UNITS = 16f;
    /** How many numbered seats of one kind a name is kept for; a seat past them is named when it is drawn. */
    private static final int NAMED = 16;
    /** What a bay's model can show, as its bones name it: drive_<bay>_<part>. */
    private static final String[] BAY_PARTS = {"cd", "dvd", "bd", "hotswap", "hotswap_empty"};
    private static final String[][] DRIVE_BONES = bones("drive_", BAY_PARTS);
    /** The parts with an activity lamp, a bone of its own under the part's: lamp_<bay>_<part>. */
    private static final String HOT_SWAP = "hotswap";
    private static final String[] LAMP_PARTS = {"cd", "dvd", "bd", HOT_SWAP};
    private static final String[][] LAMP_BONES = bones("lamp_", LAMP_PARTS);
    /** The controllers each bay's tray and caddy play on. */
    private static final String[] TRAYS = DeviceLamps.numbered("tray_", CaseFront.BAYS);
    private static final String[] CADDIES = DeviceLamps.numbered("caddy_", CaseFront.BAYS);
    private static final String[] RAM_SEATS = DeviceLamps.numbered("seat_ram_", NAMED);
    private static final String[] CARD_SEATS = DeviceLamps.numbered("seat_card_", NAMED);
    /** Each hardware slot's part is drawn by a model of its own, kept by the slot it is in. */
    private static final String[] SLOT_PARTS = DeviceLamps.numbered("slot_", NAMED * 4);
    private static final String COOLER = "cooler";

    /* Where each part's model and texture are, by the item that names it, until the models are read again. */
    private static final Map<ResourceLocation, PartFiles> PART_FILES = new HashMap<>();
    /* The seats of each form of disk, on the board or in the case: seat_<form>_<n>. */
    private static final Map<String, String[]> DISK_SEATS = new HashMap<>();
    private static final Map<String, ResourceLocation> COOLERS = new HashMap<>();
    private static final Map<HardwareEra, String> LEADS = new EnumMap<>(HardwareEra.class);
    private static final ModelFacts<PartShape> SHAPES = new ModelFacts<>(ComputerRenderer::shapeOf);
    /* The part's moves: its fans turned, its supply's leads chosen. */
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

    private ComputerRenderer() {
    }

    /** The renderer of one of the small computers. */
    public static <T extends AbstractSmallComputerBlockEntity> BlockEntityRenderer<T> create(
            final BlockEntityRendererProvider.Context context) {
        final ComputerRenderer drawn = new ComputerRenderer();
        return KitBlockEntityRenderer.<T>look(ComputingLooks.COMPUTER).driver(drawn).extras(drawn).build();
    }

    /* The case's fans turn while the machine runs, and each bay's tray and caddy ride with what goes in and out. */
    @Override
    public void drive(final AbstractSmallComputerBlockEntity computer, final ModelAnimator animator,
                      final double seconds) {
        DeviceClips.play(animator, WORK, computer.visualRunning() ? WORK_CLIP : null, seconds);
        final CaseFront front = computer.front();
        final Level level = computer.getLevel();
        for (int bay = 0; bay < CaseFront.BAYS; bay++) {
            DeviceClips.play(animator, TRAYS[bay], front.tray(bay).playing(front.trayClips(bay), level), seconds);
            DeviceClips.play(animator, CADDIES[bay], caddy(front, bay, level), seconds);
        }
    }

    @Override
    public void move(final AbstractSmallComputerBlockEntity computer, final ModelPose pose, final double seconds) {
        final boolean running = computer.visualRunning();
        final boolean blink = DeviceLamps.blinkLit(computer.getLevel());
        DeviceLamps.show(pose, ComputingLooks.COMPUTER_POWER_LAMP, running);
        DeviceLamps.show(pose, ComputingLooks.COMPUTER_DISK_LAMP, running && computer.visualDiskBusy() && blink);
        DeviceLamps.show(pose, ComputingLooks.COMPUTER_SIDE_PANEL, !computer.sidePanelOff());
        // A bay's model carries every part it takes, each hidden but the one installed; an empty bay shows its cover.
        final CaseFront front = computer.front();
        for (int bay = 0; bay < CaseFront.BAYS; bay++) {
            final String shown = front.shown(bay);
            for (int part = 0; part < BAY_PARTS.length; part++) {
                DeviceLamps.show(pose, DRIVE_BONES[bay][part], BAY_PARTS[part].equals(shown));
            }
            // Its lamp: a drive's blinks while the machine reads its disc, the hot-swap bay's while it works the disk.
            final boolean working = HOT_SWAP.equals(shown) ? front.diskWorked(bay) : front.reading(bay);
            for (int part = 0; part < LAMP_PARTS.length; part++) {
                DeviceLamps.show(pose, LAMP_BONES[bay][part], LAMP_PARTS[part].equals(shown) && working && blink);
            }
        }
    }

    @Override
    public boolean movesBones() {
        return true;
    }

    @Override
    public void draw(final AbstractSmallComputerBlockEntity computer, final KitInstance instance,
                     final PoseStack poses, final MultiBufferSource buffers, final int light, final int overlay,
                     final float partialTick) {
        CaseFrontMedia.draw(computer, instance, poses, buffers, light, overlay);
        if (!(computer.getBlockState().getBlock() instanceof IComputerCase chassis)
                || !computer.sidePanelOff() && instance.pose().index(SEE_THROUGH) < 0) {
            return;
        }
        new Drawing(computer, chassis, instance, poses, buffers, light, overlay, partialTick).parts();
    }

    /* The bones of each part in each bay, named once rather than every frame: <prefix><bay>_<part>. */
    private static String[][] bones(final String prefix, final String[] parts) {
        final String[][] out = new String[CaseFront.BAYS][parts.length];
        for (int bay = 0; bay < CaseFront.BAYS; bay++) {
            for (int part = 0; part < parts.length; part++) {
                out[bay][part] = prefix + bay + "_" + parts[part];
            }
        }
        return out;
    }

    /* The k-th of names kept, or one made for a k past them. */
    private static String nth(final String[] names, final String prefix, final int k) {
        return k < names.length ? names[k] : prefix + k;
    }

    /* The hot-swap bay's caddy: riding in as its disk goes in, riding out as the disk comes out, else at rest. */
    private static @Nullable String caddy(final CaseFront front, final int bay, @Nullable final Level level) {
        final DiscTray.Clips clips = front.caddyClips(bay);
        if (clips == null || !front.caddyMoving(bay, level)) {
            return null;
        }
        return front.caddyWentIn(bay) ? clips.closing() : clips.opening();
    }

    /** The part an item is drawn as, or null for no item or an item with no model of its own. */
    private static @Nullable Part part(@Nullable final ResourceLocation item) {
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

    /* What a part's model is: its one root bone's name, its fans and its supply's leads. */
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
        for (int i = 0; i < bones.size(); i++) {
            final String name = bones.get(i).name();
            if (name.startsWith(SPIN) && name.length() > SPIN.length()) {
                spins.add(i);
            } else if (name.startsWith(LEAD) && root != null && root.equals(bones.get(i).parent())) {
                leads.add(i);
            }
        }
        final int[] spinBones = spins.stream().mapToInt(Integer::intValue).toArray();
        final char[] axes = new char[spinBones.length];
        for (int k = 0; k < spinBones.length; k++) {
            axes[k] = bones.get(spinBones[k]).name().charAt(SPIN.length());
        }
        final int[] leadBones = leads.stream().mapToInt(Integer::intValue).toArray();
        final String[] leadNames = new String[leadBones.length];
        for (int k = 0; k < leadBones.length; k++) {
            leadNames[k] = bones.get(leadBones[k]).name();
        }
        return new PartShape(roots == 1 ? root : null, spinBones, axes, leadBones, leadNames);
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

    /**
     * What a part's model is, worked out once from its bones: the name of its one root bone, which says the kind of
     * part ({@code board}, {@code cpu} or {@code cartridge}, {@code ram}, {@code card}, {@code psu_<form>},
     * {@code disk_<form>}, {@code cooler}), or none for a model of more roots than one; the bones of its fans and the
     * axes they turn about; and the leads of a supply, each named for the age of the board it runs to.
     */
    private record PartShape(@Nullable String kind, int[] spins, char[] axes, int[] leads, String[] leadNames) {

        boolean moves() {
            return spins.length > 0 || leads.length > 0;
        }
    }

    /** A part's model, as it is drawn. */
    private record Part(KitModel model, PartShape shape) {

        @Nullable String kind() {
            return shape.kind();
        }
    }

    /* What moves the part being drawn: how far its fans have turned, and the one lead its supply keeps. */
    private static final class PartFrame {

        private PartShape shape;
        private float turned;
        private @Nullable String lead;

        /* Turns the fans about their axes and keeps only the supply's lead the board takes. */
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
        }
    }

    /** One computer's parts drawn in one frame. */
    private final class Drawing {

        private final AbstractSmallComputerBlockEntity computer;
        private final IComputerCase chassis;
        private final KitInstance caseInstance;
        private final PoseStack poses;
        private final MultiBufferSource buffers;
        private final int light;
        private final int overlay;
        private final double seconds;
        /** How far the parts' fans have turned, in degrees: they stand still while the machine is off. */
        private final float turned;

        Drawing(final AbstractSmallComputerBlockEntity computer, final IComputerCase chassis,
                final KitInstance caseInstance, final PoseStack poses, final MultiBufferSource buffers,
                final int light, final int overlay, final float partialTick) {
            this.computer = computer;
            this.chassis = chassis;
            this.caseInstance = caseInstance;
            this.poses = poses;
            this.buffers = buffers;
            this.light = light;
            this.overlay = overlay;
            this.seconds = KitClock.seconds(partialTick);
            final Level level = computer.getLevel();
            this.turned = computer.visualRunning() && level != null
                    ? (level.getGameTime() % FAN_TICKS + partialTick) / FAN_TICKS * FULL_TURN : 0f;
        }

        void parts() {
            final ComputerHardwareLayout layout = computer.hardwareLayout();
            final int boardSlot = layout.motherboardSlot();
            final Part board = part(computer.installedPart(boardSlot));
            KitInstance boardInstance = null;
            if (board != null) {
                boardInstance = draw(slot(boardSlot), board, null, null, false, null);
                final Part cpu = part(computer.installedPart(layout.cpuStart()));
                if (cpu != null && draw(slot(layout.cpuStart()), cpu, boardInstance, SEAT_CPU, false, null) != null
                        && !CARTRIDGE.equals(cpu.kind())) {
                    final Part cooler = part(COOLERS.computeIfAbsent(chassis.caseStyle().caseName(
                            chassis.chassisEra()), name -> ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                            "cooler_" + name)));
                    if (cooler != null) {
                        draw(COOLER, cooler, null, null, false, null);
                    }
                }
                for (int k = 0; k < layout.ramCount(); k++) {
                    final int at = layout.ramStart() + k;
                    drawOn(at, boardInstance, nth(RAM_SEATS, "seat_ram_", k));
                }
                for (int k = 0; k < layout.pcieCount(); k++) {
                    final int at = layout.pcieStart() + k;
                    drawOn(at, boardInstance, nth(CARD_SEATS, "seat_card_", k));
                }
            }
            supply(layout.psuSlot());
            int onBoard = 0;
            int inBays = 0;
            for (int k = 0; k < layout.diskCount(); k++) {
                final int at = layout.diskStart() + k;
                final Part disk = part(computer.installedPart(at));
                if (disk == null || !disk.kind().startsWith(DISK)) {
                    continue;
                }
                final String form = disk.kind().substring(DISK.length());
                if (M2.equals(form)) {
                    if (boardInstance != null) {
                        draw(slot(at), disk, boardInstance, diskSeat(form, onBoard), false, null);
                    }
                    onBoard++;
                } else {
                    draw(slot(at), disk, caseInstance, diskSeat(form, inBays++), false, null);
                }
            }
        }

        /**
         * The supply, on the case's seat for it. One made for a case that holds its supply the other way up is
         * turned over; one from another age loses its leads, which run to a connector its board does not have there.
         */
        private void supply(final int slot) {
            final Part psu = part(computer.installedPart(slot));
            if (psu == null || !psu.kind().startsWith(PSU)) {
                return;
            }
            final String form = psu.kind().substring(PSU.length());
            for (int k = 0; k < PSU_FORMS.length; k++) {
                if (caseInstance.pose().index(PSU_SEATS[k]) < 0) {
                    continue;
                }
                final String held = PSU_FORMS[k];
                if (PSU_COVER.equals(held) && !PSU_COVER.equals(form)) {
                    return;
                }
                final String lead = LEADS.computeIfAbsent(chassis.chassisEra(), era -> LEAD + era.serializedName());
                draw(slot(slot), psu, caseInstance, PSU_SEATS[k], PSU_TOP.equals(held) != PSU_TOP.equals(form),
                        lead);
                return;
            }
        }

        private void drawOn(final int slot, final KitInstance on, final String seat) {
            final Part part = part(computer.installedPart(slot));
            if (part != null) {
                draw(slot(slot), part, on, seat, false, null);
            }
        }

        /*
         * Draws a part, on {@code seat} of the model {@code on} has, or where the case stands for none; the part as
         * it is drawn, or none when that seat is not there.
         */
        private @Nullable KitInstance draw(final String key, final Part part, @Nullable final KitInstance on,
                                           @Nullable final String seat, final boolean turnedOver,
                                           @Nullable final String lead) {
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
            final KitInstance instance = KitAnimations.part(computer, key, part.model());
            if (part.shape().moves()) {
                // The part's model is its own, so its fans and leads are set for this frame before it is drawn.
                partFrame.shape = part.shape();
                partFrame.turned = turned;
                partFrame.lead = lead;
                instance.animate(KitClock.frame(), seconds, PART_MOVES, partFrame, IAnimationListener.NONE);
            }
            KitRenderer.draw(instance, ModelDefinition.Variant.NONE, computer, poses, buffers, light, overlay, null,
                    computer.getLevel(), true);
            poses.popPose();
            return instance;
        }

        private String slot(final int slot) {
            return nth(SLOT_PARTS, "slot_", slot);
        }

        private String diskSeat(final String form, final int k) {
            return nth(DISK_SEATS.computeIfAbsent(form, named -> DeviceLamps.numbered("seat_" + named + "_", NAMED)),
                    "seat_" + form + "_", k);
        }
    }
}
