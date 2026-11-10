/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.chain.machine.ChainMachine;
import dev.jstech.computers.chain.machine.ChainMachineBlockEntity;
import dev.jstech.computers.chain.machine.PreinstallerBlockEntity;
import dev.jstech.computers.chain.machine.PreinstallerTexts;
import dev.jstech.computers.chain.machine.Preinstalls;
import dev.jstech.computers.client.chain.ChainMachineRenderer;
import dev.jstech.computers.client.chain.ChainRigs;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.IKitExtras;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.client.model.kit.KitClock;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.machine.MachineStatus;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Draws the formed Preinstaller from its terminal (computers/docs/design/survival.md), as its approved mock draws it,
 * moved by how far its job has gone: for a computer the shutter rolls up, the cradle brings the computer out on its
 * rails and docks it from behind while the firmware and then the system are written, and takes it back in; for a
 * loose board the nest rises out of the fixture with the board on its back and the pin head comes down on it; for a
 * loose disk the disk rises into the dock on its edge. Its console's screen shows the job, the firmware's bar in amber
 * and the system's in green, and DONE; the disc in its window turns while a system is read off it. What it works on is
 * the mod's own model of it, the computer with its parts drawn as the small computers are.
 */
@PaletteHolder
public final class PreinstallerRenderer implements IKitDriver<ChainMachineBlockEntity>,
        IKitExtras<ChainMachineBlockEntity> {

    private final ChainMachine machine;
    private final ChainMachineRenderer base;

    /** The console's screen: the job's word, DONE, the bars' words in use and not, the bars' track and the bars. */
    public static final Palette<Screen> SCREEN = Palettes.declare(JsComputers.MODID, "machine/preinstaller_screen",
            new Screen(0xFF39D6C4, 0xFF5FE07A, 0xFF7D8A9C, 0xFF3C424C, 0xFF1C222C, 0xFFF0B23A, 0xFF5FE07A,
                    0xFF5FE07A));
    /* Where each station's part of the mock's loop starts and ends, as fractions of it. */
    private static final float BOARD_FROM = 0.56F;
    private static final float DISK_FROM = 0.78F;
    private static final float WRITE_FROM = 0.19F;
    private static final float WRITE_TO = 0.46F;
    /* A moment well inside the computer's writing, telling its opening half from its closing one. */
    private static final float HALF_WAY = 0.3F;
    private static final float SIXTEENTH = 1F / 16F;
    private static final float PIXELS = 16F;
    /* The disc's marks turn this many degrees a second while a system is read off it, and rest at this turn. */
    private static final float DISC_SPEED = 900F;
    private static final float DISC_REST = 30F;
    /* The lamps blink this many times a second. */
    private static final int BLINKS = 8;
    /*
     * A font pixel on the console's face, in sixteenths, as the mock sets its words (three quarters of a texel at
     * eight texels a sixteenth); how far out of the face the screen is drawn, and its words over its bars.
     */
    private static final float WORDS = 0.09375F;
    private static final float OUT = 0.03F;
    private static final float ABOVE = 0.01F;

    private PreinstallerRenderer(final ChainMachine machine) {
        this.machine = machine;
        this.base = ChainMachineRenderer.of(machine);
    }

    /** The renderer of the Preinstaller's terminal. */
    public static BlockEntityRenderer<ChainMachineBlockEntity> create(final ChainMachine machine) {
        final PreinstallerRenderer drawn = new PreinstallerRenderer(machine);
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
        final JsonObject rig = ChainRigs.of(machine.id());
        if (!(terminal instanceof PreinstallerBlockEntity preinstaller) || !rig.has("cradle")) {
            return;
        }
        final Scene scene = Scene.of(preinstaller);
        final boolean blink = (int) (seconds * BLINKS) % 2 == 1;
        // The shutter rolls up into the canopy; shut, it hangs whole.
        final int shutter = pose.index("shutter");
        if (shutter >= 0) {
            pose.setHidden(shutter, scene.shutter() < 0.01F);
            pose.multiplySize(shutter, 1, Math.max(0.01F, scene.shutter()), 1);
        }
        // The cradle's carriage, its lamps and its dock out on the rails as far as the scene has it.
        final JsonObject cradle = rig.getAsJsonObject("cradle");
        final float travel = (ChainRigs.point(cradle, "out").z() - ChainRigs.point(cradle, "in").z()) * PIXELS
                * scene.out();
        for (final String bone : new String[] {"carriage", "carriage_lamps_off", "carriage_lamps_amber",
            "carriage_lamps_green"}) {
            move(pose, bone, 0, 0, travel);
        }
        move(pose, "dock_plug", 0, 0, travel + (scene.docked() ? 0 : ChainRigs.number(cradle, "dock") * PIXELS));
        show(pose, "carriage_lamps_off", !scene.docked());
        // Amber while the board's firmware is still to be written, green once it has some.
        final boolean firmwareOn = scene.firmware() >= 1 || !scene.writesFirmware();
        show(pose, "carriage_lamps_amber", scene.docked() && !firmwareOn);
        show(pose, "carriage_lamps_green", scene.docked() && firmwareOn);
        // The board fixture: the nest up, the head down on it, the rod from the beam stretched to the head.
        final JsonObject nest = rig.getAsJsonObject("nest");
        final JsonObject head = rig.getAsJsonObject("head");
        move(pose, "nest", 0, (scene.nest() - ChainRigs.number(nest, "down")) * PIXELS, 0);
        final float down = (scene.head() - ChainRigs.number(head, "up")) * PIXELS;
        for (final String bone : new String[] {"pin_head", "pin_lamp_off", "pin_lamp_lit"}) {
            move(pose, bone, 0, down, 0);
        }
        final int rod = pose.index("pin_rod");
        final float rest = ChainRigs.number(head, "rod");
        if (rod >= 0 && rest > 0) {
            pose.multiplySize(rod, 1, (rest - down / PIXELS) / rest, 1);
        }
        final boolean pinning = scene.station() == Preinstalls.Station.BOARD_FIXTURE && scene.writingFirmware();
        show(pose, "pin_lamp_off", !pinning);
        show(pose, "pin_lamp_lit", pinning);
        final boolean docking = scene.station() == Preinstalls.Station.DISK_DOCK && scene.writingSystem() && blink;
        show(pose, "dock_lamp_off", !docking);
        show(pose, "dock_lamp_lit", docking);
        final int marks = pose.index("disc_marks");
        if (marks >= 0) {
            final float turn = scene.spinning() ? (float) (seconds * DISC_SPEED) : DISC_REST;
            pose.setTurn(marks, new Quaternionf().rotationY((float) Math.toRadians(turn)));
        }
    }

    @Override
    public boolean movesBones() {
        return true;
    }

    /* What it works on, at its station, and its console's screen. */
    @Override
    public void draw(final ChainMachineBlockEntity terminal, final KitInstance instance, final PoseStack poses,
                     final MultiBufferSource buffers, final int light, final int overlay, final float partialTick) {
        final JsonObject rig = ChainRigs.of(machine.id());
        if (!(terminal instanceof PreinstallerBlockEntity preinstaller) || !rig.has("cradle")) {
            return;
        }
        final Scene scene = Scene.of(preinstaller);
        final ItemStack work = preinstaller.working();
        if (!work.isEmpty() && scene.station() != null) {
            switch (scene.station()) {
                case CRADLE -> computer(terminal, rig, scene, work, poses, buffers, light, overlay, partialTick);
                case BOARD_FIXTURE -> board(terminal, rig, scene, work, poses, buffers, light, overlay, partialTick);
                case DISK_DOCK -> disk(terminal, rig, scene, work, poses, buffers, light, overlay, partialTick);
            }
        }
        screen(rig, scene, KitClock.seconds(partialTick), poses, buffers);
    }

    /* The computer on its carriage, its corner where the carriage carries it; a server drawn as its item. */
    private static void computer(final ChainMachineBlockEntity owner, final JsonObject rig, final Scene scene,
                                 final ItemStack work, final PoseStack poses, final MultiBufferSource buffers,
                                 final int light, final int overlay, final float partialTick) {
        final JsonObject cradle = rig.getAsJsonObject("cradle");
        final Vector3f at = ChainRigs.point(cradle, "computer");
        at.z += (ChainRigs.point(cradle, "out").z() - ChainRigs.point(cradle, "in").z()) * scene.out();
        poses.pushPose();
        poses.translate(at.x(), at.y(), at.z());
        if (work.has(ComputingComponents.ASSEMBLED_HARDWARE.get())) {
            ComputerDummies.draw(owner, work, slot -> true, false, poses, buffers, light, overlay, partialTick);
        } else {
            poses.translate(0.5F, 0.5F, 0.5F);
            Minecraft.getInstance().getItemRenderer().renderStatic(work, ItemDisplayContext.FIXED, light, overlay,
                    poses, buffers, owner.getLevel(), 0);
        }
        poses.popPose();
    }

    /* The loose board on its back on the nest, its middle over the fixture's and its underside on the nest. */
    private static void board(final ChainMachineBlockEntity owner, final JsonObject rig, final Scene scene,
                              final ItemStack work, final PoseStack poses, final MultiBufferSource buffers,
                              final int light, final int overlay, final float partialTick) {
        final JsonObject nest = rig.getAsJsonObject("nest");
        final Vector3f at = ChainRigs.point(nest, "board");
        at.y += scene.nest() - ChainRigs.number(nest, "down");
        standing(owner, work, new Quaternionf().rotationZ((float) Math.toRadians(90)), at, poses, buffers, light,
                overlay, partialTick);
    }

    /* The loose disk on its edge rising into the dock. */
    private static void disk(final ChainMachineBlockEntity owner, final JsonObject rig, final Scene scene,
                             final ItemStack work, final PoseStack poses, final MultiBufferSource buffers,
                             final int light, final int overlay, final float partialTick) {
        final JsonObject disk = rig.getAsJsonObject("disk");
        final Vector3f at = ChainRigs.point(disk, "at");
        at.y += scene.disk() - ChainRigs.number(disk, "down");
        standing(owner, work, new Quaternionf().rotationX((float) Math.toRadians(-90)), at, poses, buffers, light,
                overlay, partialTick);
    }

    /* A part's model turned by {@code turn}, its middle over {@code at} and its foot on it, as the mock stands it. */
    private static void standing(final ChainMachineBlockEntity owner, final ItemStack work, final Quaternionf turn,
                                 final Vector3f at, final PoseStack poses, final MultiBufferSource buffers,
                                 final int light, final int overlay, final float partialTick) {
        final ResourceLocation item = BuiltInRegistries.ITEM.getKey(work.getItem());
        final float[] reach = PartModels.reach(item);
        if (reach == null) {
            return;
        }
        final Vector3f lo = new Vector3f(Float.MAX_VALUE);
        final Vector3f hi = new Vector3f(-Float.MAX_VALUE);
        for (int corner = 0; corner < 8; corner++) {
            final Vector3f c = new Vector3f(reach[(corner & 1) == 0 ? 0 : 3], reach[(corner & 2) == 0 ? 1 : 4],
                    reach[(corner & 4) == 0 ? 2 : 5]).rotate(turn).div(PIXELS);
            lo.min(c);
            hi.max(c);
        }
        poses.pushPose();
        poses.translate(at.x() - (lo.x() + hi.x()) / 2, at.y() - lo.y(), at.z() - (lo.z() + hi.z()) / 2);
        poses.mulPose(turn);
        PartModels.loose(owner, "loose_part", item, poses, buffers, light, overlay, partialTick);
        poses.popPose();
    }

    /*
     * The console's screen, drawn on its face as its picture's sixteenths read from the face's top left: the job's
     * word, DONE, each bar's word and its track filled as far as it has gone, and the floppy drive's light.
     */
    private static void screen(final JsonObject rig, final Scene scene, final double seconds, final PoseStack poses,
                               final MultiBufferSource buffers) {
        final JsonObject screen = rig.getAsJsonObject("screen");
        if (screen == null) {
            return;
        }
        final Vector3f topLeft = ChainRigs.point(screen, "top_left");
        final Screen colours = SCREEN.get();
        poses.pushPose();
        // The face's own top left, from the screen's, which stands 0.9 of a sixteenth in from it each way.
        poses.translate(topLeft.x() + 0.9F * SIXTEENTH, topLeft.y() + 0.9F * SIXTEENTH, topLeft.z());
        poses.mulPose(Axis.YP.rotationDegrees(180));
        poses.scale(SIXTEENTH, -SIXTEENTH, SIXTEENTH);
        // Turned and flipped so, the face's sixteenths run right and down as one stands before it, and out is +z.
        poses.translate(0, 0, OUT);
        final Matrix4f face = poses.last().pose();
        final VertexConsumer fill = buffers.getBuffer(RenderType.textBackground());
        if (scene.station() != null) {
            final boolean[] used = {scene.writesFirmware(), scene.writesSystem()};
            final float[] filled = {scene.firmware(), scene.system()};
            final int[] bars = {colours.firmware(), colours.system()};
            for (int k = 0; k < 2; k++) {
                final float top = 2.25F + k * 1.3F;
                rect(fill, face, 5.6F, top, 10.2F, top + 0.6F, colours.track());
                if (used[k] && filled[k] > 0) {
                    rect(fill, face, 5.6F, top, 5.6F + 4.6F * Math.min(1, filled[k]), top + 0.6F, bars[k]);
                }
            }
            final boolean reading = (scene.spinning() || scene.writingFirmware())
                    && (int) (seconds * BLINKS) % 2 == 1;
            if (reading) {
                rect(fill, face, 12.2F, 2.3F, 12.8F, 2.6F, colours.floppy());
            }
            final Font font = Minecraft.getInstance().font;
            words(font, face, buffers, GameText.resolve(switch (scene.station()) {
                case CRADLE -> PreinstallerTexts.COMPUTER;
                case BOARD_FIXTURE -> PreinstallerTexts.MOTHERBOARD;
                case DISK_DOCK -> PreinstallerTexts.DISK;
            }), 1.3F, 1.15F, colours.job());
            if (scene.done()) {
                final String done = GameText.resolve(PreinstallerTexts.DONE);
                words(font, face, buffers, done, 10.2F - font.width(done) * WORDS, 1.15F, colours.done());
            }
            words(font, face, buffers, GameText.resolve(PreinstallerTexts.FIRMWARE), 1.3F, 2.15F,
                    used[0] ? colours.word() : colours.unused());
            words(font, face, buffers, GameText.resolve(PreinstallerTexts.SYSTEM), 1.3F, 3.45F,
                    used[1] ? colours.word() : colours.unused());
        }
        poses.popPose();
    }

    private static void rect(final VertexConsumer fill, final Matrix4f face, final float x0, final float y0,
                             final float x1, final float y1, final int argb) {
        fill.addVertex(face, x0, y0, 0).setColor(argb).setLight(LightTexture.FULL_BRIGHT);
        fill.addVertex(face, x0, y1, 0).setColor(argb).setLight(LightTexture.FULL_BRIGHT);
        fill.addVertex(face, x1, y1, 0).setColor(argb).setLight(LightTexture.FULL_BRIGHT);
        fill.addVertex(face, x1, y0, 0).setColor(argb).setLight(LightTexture.FULL_BRIGHT);
    }

    private static void words(final Font font, final Matrix4f face, final MultiBufferSource buffers, final String text,
                              final float x, final float y, final int argb) {
        final Matrix4f at = new Matrix4f(face).translate(x, y, ABOVE).scale(WORDS, WORDS, 1);
        font.drawInBatch(text, 0, 0, argb, false, at, buffers, Font.DisplayMode.POLYGON_OFFSET, 0,
                LightTexture.FULL_BRIGHT);
    }

    private static void move(final ModelPose pose, final String bone, final float x, final float y, final float z) {
        final int at = pose.index(bone);
        if (at >= 0) {
            pose.addMove(at, x, y, z);
        }
    }

    private static void show(final ModelPose pose, final String bone, final boolean shown) {
        final int at = pose.index(bone);
        if (at >= 0) {
            pose.setHidden(at, !shown);
        }
    }

    /**
     * The console's colours.
     *
     * @param job      the job's word
     * @param done     DONE
     * @param word     a bar's word in use
     * @param unused   a bar's word the job does not use
     * @param track    a bar's track
     * @param firmware the firmware's bar
     * @param system   the system's bar
     * @param floppy   the floppy drive's light while it reads
     */
    public record Screen(int job, int done, int word, int unused, int track, int firmware, int system, int floppy) {
    }

    /*
     * Where the machine stands in its job, as the mock's loop has each station's part of it: the shutter (1 shut),
     * the carriage out (1 out on the bay), whether it is docked, which of the two it writes and how far each bar has
     * gone, the nest, the head and the disk in blocks, whether the disc turns and whether DONE shows. At rest with no
     * job. The loop's moments are the mock's, as fractions of it: for a computer the shutter rolls up by 0.05, the
     * carriage is out by 0.15 and docked from 0.17 to 0.47, back in by 0.53 and the shutter down by 0.56; for a board
     * the nest is up by 0.60 and the head down by 0.62, both back from 0.72; for a disk it is up by 0.82, down from
     * 0.95.
     */
    private record Scene(Preinstalls.Station station, float shutter, float out, boolean docked,
                         boolean writesFirmware, boolean writesSystem, float firmware, float system, float nest,
                         float head, float disk, boolean spinning, boolean done) {

        static Scene of(final PreinstallerBlockEntity machine) {
            final JsonObject rig = ChainRigs.of("preinstaller");
            final float nestDown = ChainRigs.number(rig.getAsJsonObject("nest"), "down");
            final float nestUp = ChainRigs.number(rig.getAsJsonObject("nest"), "up");
            final float headUp = ChainRigs.number(rig.getAsJsonObject("head"), "up");
            final float headDown = ChainRigs.number(rig.getAsJsonObject("head"), "down");
            final float diskDown = ChainRigs.number(rig.getAsJsonObject("disk"), "down");
            final float diskUp = ChainRigs.number(rig.getAsJsonObject("disk"), "up");
            if (machine.status() != MachineStatus.WORKING || machine.working().isEmpty()) {
                return new Scene(null, 1, 0, false, false, false, 0, 0, nestDown, headUp, diskDown, false, false);
            }
            final float p = machine.done();
            final float share = machine.firmwareShare();
            return switch (machine.station()) {
                case CRADLE -> {
                    final float a = p * BOARD_FROM;
                    final boolean opening = a < HALF_WAY;
                    final float split = WRITE_FROM + (WRITE_TO - WRITE_FROM) * share;
                    final float system = share >= 1 ? 0 : span(a, split, WRITE_TO);
                    yield new Scene(Preinstalls.Station.CRADLE,
                            opening ? 1 - span(a, 0, 0.05F) : span(a, 0.53F, BOARD_FROM),
                            opening ? span(a, 0.05F, 0.15F) : 1 - span(a, 0.48F, 0.53F), a >= 0.17F && a < 0.47F,
                            share > 0, share < 1, share > 0 ? span(a, WRITE_FROM, split) : 0, system, nestDown,
                            headUp, diskDown, system > 0 && system < 1, a >= WRITE_TO && a < 0.48F);
                }
                case BOARD_FIXTURE -> {
                    final float a = BOARD_FROM + p * (DISK_FROM - BOARD_FROM);
                    final boolean rising = a < 0.7F;
                    final float nest = nestDown + (nestUp - nestDown) * (rising ? span(a, BOARD_FROM, 0.6F)
                            : 1 - span(a, 0.74F, DISK_FROM));
                    final float head = headUp + (headDown - headUp) * (rising ? span(a, 0.6F, 0.62F)
                            : 1 - span(a, 0.72F, 0.74F));
                    yield new Scene(Preinstalls.Station.BOARD_FIXTURE, 1, 0, false, true, false,
                            span(a, 0.62F, 0.72F), 0, nest, head, diskDown, false, a >= 0.72F);
                }
                case DISK_DOCK -> {
                    final float a = DISK_FROM + p * (1 - DISK_FROM);
                    final float disk = diskDown + (diskUp - diskDown) * (a < 0.9F ? span(a, DISK_FROM, 0.82F)
                            : 1 - span(a, 0.95F, 1));
                    final float system = span(a, 0.82F, 0.94F);
                    yield new Scene(Preinstalls.Station.DISK_DOCK, 1, 0, false, false, true, 0, system, nestDown,
                            headUp, disk, system > 0 && system < 1, a >= 0.94F);
                }
            };
        }

        boolean writingFirmware() {
            return firmware > 0 && firmware < 1;
        }

        boolean writingSystem() {
            return system > 0 && system < 1;
        }

        /* How far a move from {@code from} to {@code to} has gone at {@code a}, eased in and out. */
        private static float span(final float a, final float from, final float to) {
            final float x = Math.clamp((a - from) / (to - from), 0, 1);
            return (float) (0.5 - 0.5 * Math.cos(Math.PI * x));
        }
    }
}
