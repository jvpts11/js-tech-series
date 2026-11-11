/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client;

import static dev.jstech.computers.client.Printer3DPanelTexts.FULL;
import static dev.jstech.computers.client.Printer3DPanelTexts.HEIGHT;
import static dev.jstech.computers.client.Printer3DPanelTexts.NO_WALLET;
import static dev.jstech.computers.client.Printer3DPanelTexts.PAUSED;
import static dev.jstech.computers.client.Printer3DPanelTexts.PRINTING;
import static dev.jstech.computers.client.Printer3DPanelTexts.READY;
import static dev.jstech.computers.client.Printer3DPanelTexts.TOUCH_DONE;
import static dev.jstech.computers.client.Printer3DPanelTexts.TOUCH_NAME;
import static dev.jstech.computers.client.Printer3DPanelTexts.TOUCH_PAUSED;
import static dev.jstech.computers.client.Printer3DPanelTexts.TOUCH_PRINTING;
import static dev.jstech.computers.client.Printer3DPanelTexts.TOUCH_READY;
import static dev.jstech.computers.client.Printer3DPanelTexts.TOUCH_WAITING;
import static dev.jstech.computers.client.Printer3DPanelTexts.TO_TRAY;
import static dev.jstech.computers.client.Printer3DPanelTexts.WAITING;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.jstech.computers.ComputingLooks;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.blockentity.Printer3DBlockEntity;
import dev.jstech.computers.teracoin.PrintMotion;
import dev.jstech.computers.teracoin.PrintMotion.Nozzle;
import dev.jstech.computers.teracoin.PrintMotion.Row;
import dev.jstech.core.client.model.kit.IKitDriver;
import dev.jstech.core.client.model.kit.IKitExtras;
import dev.jstech.core.client.model.kit.KitBlockEntityRenderer;
import dev.jstech.core.client.model.kit.KitInstance;
import dev.jstech.core.client.model.kit.KitModels;
import dev.jstech.core.model.ModelAnimator;
import dev.jstech.core.model.ModelPose;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * Draws a 3D printer (computers/docs/design/teracoin.md, "The 3D Printer") as its approved model moves: the Prosa's
 * gantry climbing its screws with the head running along the row, its bed sliding the finished item into the tray and
 * its spool turning as it feeds; the Rattan's bed sinking a row at a time, its head on its gantry under the roof, its
 * belt carrying the item off into the slot. The item itself is printed for real: its own texels from the bottom, a row
 * at a time, the row in progress laid up to the nozzle; an item that is a block is printed as its cube. The Prosa's
 * display and the Rattan's touchscreen say what it does, and the Rattan's smoked glass door is drawn see-through.
 */
@PaletteHolder
public final class Printer3DRenderer implements IKitDriver<Printer3DBlockEntity>, IKitExtras<Printer3DBlockEntity> {

    /* Each item's rows as it is printed, read once from its picture, until the pictures are read again. */
    private final Map<Item, Printed> printed = new HashMap<>();
    private long readAt = Long.MIN_VALUE;

    /* The words and bars of the two screens, and the filament. */
    private static final Palette<Screens> SCREENS = Palettes.declare(JsComputers.MODID, "device/printer_3d",
            new Screens(0xFFE8F0FF, 0xFF7FD8FF, 0xFFE8ECF2, 0xFF28303C, 0xFF50C8FF, 0xFFE8E4D6));
    private static final ResourceLocation GLASS = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
            "textures/block/printer_3d/rattan_glass.png");
    private static final ModelFacts<PartModels.Spins> FANS = new ModelFacts<>(model -> PartModels.Spins.of(
            model.bones()));
    private static final int GRID = 16;
    private static final float PIXEL = 16f;
    private static final float HALF_BLOCK = 0.5f;
    private static final double MIDDLE = 8.0;
    private static final double SCREW_TURN = 120.0;
    private static final int TICKS_A_SECOND = 20;
    private static final double MM_A_PIXEL = 5.0;
    /* Where each model was built: the Prosa's gantry over the first row, the Rattan's bed under it, its head. */
    private static final double PROSA_GANTRY = PrintMotion.PROSA_BED + PrintMotion.TEXEL + PrintMotion.PROSA_HEAD_LIFT;
    private static final double RATTAN_BED = PrintMotion.RATTAN_TIP - PrintMotion.TEXEL;
    /* The Prosa's display and the Rattan's touchscreen: their words' top left, scale and room, in sixteenths. */
    private static final float LCD_X = 3.1f - 7f / 32f;
    private static final float LCD_Y = 4.2f - 8f / 32f;
    private static final float LCD_Z = 0f;
    private static final float LCD_SCALE = 0.25f / 8f;
    private static final int LCD_ROOM = 83;
    private static final float TOUCH_X = 15.7f - 12f / 16f;
    private static final float TOUCH_Y = 2.5f - 10f / 16f;
    private static final float TOUCH_Z = 0.3f;
    private static final float TOUCH_SCALE = 0.5f / 8f;
    private static final int TOUCH_ROOM = 56;
    private static final float BAR_TOP = 1.0f;
    private static final float BAR_BOTTOM = 1.125f;
    private static final float BAR_W = 3.5f;
    private static final float OUT = 0.01f;
    /* The Rattan's glass door: its pane, in sixteenths. */
    private static final float[] PANE = {1.6f, 2.9f, 0.15f, 14.4f, 12.2f, 0.25f};

    private Printer3DRenderer() {
    }

    /** The renderer of the 3D printers. */
    public static BlockEntityRenderer<Printer3DBlockEntity> create(final BlockEntityRendererProvider.Context context) {
        final Printer3DRenderer drawn = new Printer3DRenderer();
        return KitBlockEntityRenderer.<Printer3DBlockEntity>look(ComputingLooks.PRINTER_3D).driver(drawn).extras(drawn)
                .build();
    }

    @Override
    public void drive(final Printer3DBlockEntity printer, final ModelAnimator animator, final double seconds) {
    }

    @Override
    public void move(final Printer3DBlockEntity printer, final ModelPose pose, final double seconds) {
        final Scene scene = scene(printer);
        final boolean working = scene.working();
        if (!printer.model().belt()) {
            final PrintMotion.Prosa p = scene.prosa();
            move(pose, "gantry", 0, p.gantry() - PROSA_GANTRY, 0);
            move(pose, "head", -(p.head() - MIDDLE), 0, 0);
            move(pose, "bed", 0, 0, p.bed());
            turn(pose, "screw_left", 0, p.gantry() * SCREW_TURN, 0);
            turn(pose, "screw_right", 0, -p.gantry() * SCREW_TURN, 0);
            turn(pose, "spin_x_spool", p.spool(), 0, 0);
        } else {
            final PrintMotion.Rattan r = scene.rattan();
            move(pose, "bed", 0, r.bed() - RATTAN_BED, 0);
            move(pose, "gantry", 0, 0, r.headW() - PrintMotion.RATTAN_HEAD_W);
            move(pose, "head", -(r.head() - MIDDLE), 0, 0);
            final double roll = Math.toDegrees(r.belt() / PrintMotion.BELT_R);
            turn(pose, "roll_front", -roll, 0, 0);
            turn(pose, "roll_back", -roll, 0, 0);
            move(pose, "belt_marks", 0, 0, -(r.belt() % PrintMotion.BELT_P));
            turn(pose, "screw_left", 0, r.bed() * SCREW_TURN, 0);
            turn(pose, "screw_right", 0, -r.bed() * SCREW_TURN, 0);
            DeviceLamps.show(pose, "light_on", working);
            DeviceLamps.show(pose, "light_off", !working);
            DeviceLamps.show(pose, "led_on", working);
            DeviceLamps.show(pose, "led_off", !working);
        }
        if (working) {
            FANS.of(pose.model()).turn(pose, PartModels.fanTurn(seconds));
        }
    }

    @Override
    public boolean movesBones() {
        return true;
    }

    @Override
    public void draw(final Printer3DBlockEntity printer, final KitInstance instance, final PoseStack poses,
                     final MultiBufferSource buffers, final int light, final int overlay, final float partialTick) {
        final Scene scene = scene(printer);
        poses.pushPose();
        // From the block's middle to its corner, in sixteenths: the model's own measures.
        poses.translate(-HALF_BLOCK, 0, -HALF_BLOCK);
        poses.scale(1 / PIXEL, 1 / PIXEL, 1 / PIXEL);
        if (scene.print() != null) {
            item(scene, printer.model().belt(), poses, buffers, light, overlay);
        }
        if (!printer.model().belt()) {
            filament(scene.prosa(), poses, buffers);
            display(printer, scene, poses, buffers);
        } else {
            touchscreen(printer, scene, poses, buffers);
            glass(poses, buffers, light, overlay);
        }
        poses.popPose();
    }

    /* What the printer is doing this frame, worked out once from its phase. */
    private Scene scene(final Printer3DBlockEntity printer) {
        final Item item = printer.currentItem();
        final Printed print = item == null ? null : printed(item);
        final List<Row> rows = print == null ? List.of() : print.rows();
        final float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        final double ticks = printer.phaseTicks() + (printer.paused() ? 0.0 : partial);
        double progress = 0.0;
        double ejected = -1.0;
        boolean shows = false;
        switch (printer.phase()) {
            case Printer3DBlockEntity.PRINTING -> {
                progress = Math.min(1.0, ticks / printer.model().itemTicks());
                shows = true;
            }
            case Printer3DBlockEntity.EJECTING -> {
                ejected = ticks / TICKS_A_SECOND;
                shows = true;
            }
            case Printer3DBlockEntity.OUTPUT_FULL -> {
                progress = 1.0;
                shows = true;
            }
            default -> {
            }
        }
        final List<Row> moving = shows ? rows : List.of();
        return new Scene(shows ? print : null, item,
                PrintMotion.prosa(moving, progress, ejected), PrintMotion.rattan(moving, progress, ejected),
                progress, ejected, printer.phase() == Printer3DBlockEntity.PRINTING
                || printer.phase() == Printer3DBlockEntity.EJECTING, printer.paused());
    }

    /* The item printed so far, or as it is put out: its texels, or its cube for a block. */
    private static void item(final Scene scene, final boolean belt, final PoseStack poses,
                             final MultiBufferSource buffers, final int light, final int overlay) {
        final Printed print = scene.print();
        final Nozzle nozzle = belt ? scene.rattan().nozzle() : scene.prosa().nozzle();
        final double foot = belt ? scene.rattan().itemFoot() : PrintMotion.PROSA_BED;
        final double w = belt ? scene.rattan().itemW() : scene.prosa().itemW();
        if (print == null || (belt ? scene.rattan().gone() : scene.prosa().gone())) {
            return;
        }
        final double tip = belt ? scene.rattan().tip() : scene.prosa().tip();
        final double dropV = belt ? scene.rattan().dropV() : scene.prosa().dropV();
        final double dropW = belt ? scene.rattan().dropW() : scene.prosa().dropW();
        poses.pushPose();
        if (tip != 0 || dropV != 0 || dropW != 0) {
            // Falling into the output: turned forward about its front foot, and moved down and in.
            poses.translate(0, dropV, dropW);
            poses.translate(0, foot, w - PrintMotion.TEXEL / 2);
            poses.mulPose(Axis.XP.rotationDegrees((float) -tip));
            poses.translate(0, -foot, -(w - PrintMotion.TEXEL / 2));
        }
        final VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(print.side().atlasLocation()));
        final PoseStack.Pose pose = poses.last();
        final List<Row> rows = print.rows();
        final int done = nozzle == null ? rows.size() : nozzle.row();
        final int r0 = rows.getFirst().r();
        final double u0 = PrintMotion.itemU0();
        final double s = PrintMotion.TEXEL;
        for (int k = 0; k < rows.size() && k <= done; k++) {
            final Row row = rows.get(k);
            int a = row.a();
            int b = row.b();
            if (k == done && nozzle != null) {
                if (nozzle.laid() <= 0) {
                    continue;
                }
                if (nozzle.sweep() > 0) {
                    b = a + nozzle.laid();
                } else {
                    a = b - nozzle.laid();
                }
            }
            final float y0 = (float) (foot + (row.r() - r0) * s);
            final float y1 = (float) (y0 + s);
            if (print.block()) {
                final float half = (float) (GRID * s / 2);
                box(vc, pose, (float) (PIXEL - u0 - b * s), y0, (float) (w - half), (float) (PIXEL - u0 - a * s),
                        y1, (float) (w + half), print, row.r(), a, b, light, overlay);
                continue;
            }
            for (int i = a; i < b; i++) {
                if (!print.opaque(i, row.r())) {
                    continue;
                }
                texel(vc, pose, (float) (PIXEL - u0 - (i + 1) * s), y0, (float) (w - s / 2),
                        (float) (PIXEL - u0 - i * s), y1, (float) (w + s / 2), print.side(), i, row.r(), light,
                        overlay);
            }
        }
        poses.popPose();
    }

    /* The filament from the spool over the guide on the top bar down into the head, as lines. */
    private static void filament(final PrintMotion.Prosa p, final PoseStack poses, final MultiBufferSource buffers) {
        final VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        final int colour = SCREENS.get().filament();
        final double[][] points = {{p.head(), p.gantry() + 3.0, 8.1}, {11.0, 14.9, 9.9}, {9.3, 14.55, 11.6}};
        for (int k = 0; k + 1 < points.length; k++) {
            line(lines, poses.last(), points[k], points[k + 1], colour);
        }
    }

    /* The Prosa's four-line display: its name, what it does, the item and the nozzle's height. */
    private static void display(final Printer3DBlockEntity printer, final Scene scene, final PoseStack poses,
                                final MultiBufferSource buffers) {
        final Font font = Minecraft.getInstance().font;
        final String[] lines = {printer.model().name(), GameText.resolve(state(printer, scene)),
                scene.item() == null ? "" : new ItemStack(scene.item()).getHoverName().getString(),
                scene.prosa().nozzle() == null ? "" : GameText.resolve(HEIGHT.with(String.format(Locale.ROOT, "%.1f",
                        scene.prosa().nozzle().top() * MM_A_PIXEL)))};
        final Matrix4f face = face(poses, LCD_X, LCD_Y, LCD_Z);
        for (int i = 0; i < lines.length; i++) {
            final Matrix4f at = new Matrix4f(face).scale(LCD_SCALE, LCD_SCALE, 1).translate(0, i * GRID / 2f, 0);
            font.drawInBatch(font.plainSubstrByWidth(lines[i], LCD_ROOM), 0, 0, SCREENS.get().lcd(), false, at,
                    buffers, Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
        }
    }

    /* The Rattan's touchscreen: what it does, the item and the copy, and the print's bar. */
    private static void touchscreen(final Printer3DBlockEntity printer, final Scene scene, final PoseStack poses,
                                    final MultiBufferSource buffers) {
        final Font font = Minecraft.getInstance().font;
        final Matrix4f face = face(poses, TOUCH_X, TOUCH_Y, TOUCH_Z);
        final Screens colours = SCREENS.get();
        final String state = GameText.resolve(touchState(printer, scene));
        final List<Printer3DBlockEntity.Job> jobs = printer.jobs();
        final String name = jobs.isEmpty() || scene.item() == null ? printer.model().name()
                : GameText.resolve(TOUCH_NAME.with(new ItemStack(scene.item()).getHoverName().getString(),
                jobs.getFirst().done() + 1, jobs.getFirst().copies()));
        final Matrix4f words = new Matrix4f(face).scale(TOUCH_SCALE, TOUCH_SCALE, 1);
        font.drawInBatch(font.plainSubstrByWidth(state, TOUCH_ROOM), 0, 0, colours.state(), false, words, buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
        font.drawInBatch(font.plainSubstrByWidth(name, TOUCH_ROOM), 0, GRID / 2f, colours.name(), false,
                new Matrix4f(words), buffers, Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
        final VertexConsumer fill = buffers.getBuffer(RenderType.textBackground());
        rect(fill, face, 0, BAR_TOP, BAR_W, BAR_BOTTOM, colours.track());
        if (scene.progress() > 0) {
            rect(fill, face, 0, BAR_TOP, (float) (BAR_W * scene.progress()), BAR_BOTTOM, colours.bar());
        }
    }

    /* The smoked glass of the Rattan's door, both its faces, see-through. */
    private static void glass(final PoseStack poses, final MultiBufferSource buffers, final int light,
                              final int overlay) {
        final VertexConsumer vc = buffers.getBuffer(RenderType.entityTranslucent(GLASS));
        final PoseStack.Pose pose = poses.last();
        final float x0 = PANE[0];
        final float y0 = PANE[1];
        final float x1 = PANE[3];
        final float y1 = PANE[4];
        for (final float z : new float[] {PANE[2], PANE[5]}) {
            final float nz = z == PANE[2] ? -1 : 1;
            vertex(vc, pose, x1, y1, z, 0, 0, 0, 0, nz, light, overlay);
            vertex(vc, pose, x1, y0, z, 0, 1, 0, 0, nz, light, overlay);
            vertex(vc, pose, x0, y0, z, 1, 1, 0, 0, nz, light, overlay);
            vertex(vc, pose, x0, y1, z, 1, 0, 0, 0, nz, light, overlay);
        }
    }

    /* A face's own measures from its top left as one stands before its north face: right, down, and out as +z. */
    private static Matrix4f face(final PoseStack poses, final float x, final float y, final float z) {
        poses.pushPose();
        poses.translate(x, y, z);
        poses.mulPose(Axis.YP.rotationDegrees(180));
        poses.scale(1, -1, 1);
        poses.translate(0, 0, OUT);
        final Matrix4f face = new Matrix4f(poses.last().pose());
        poses.popPose();
        return face;
    }

    private static TextKey state(final Printer3DBlockEntity printer, final Scene scene) {
        if (printer.paused()) {
            return PAUSED;
        }
        return switch (printer.phase()) {
            case Printer3DBlockEntity.PRINTING -> PRINTING;
            case Printer3DBlockEntity.EJECTING -> TO_TRAY;
            case Printer3DBlockEntity.WAITING_TRC -> WAITING;
            case Printer3DBlockEntity.WAITING_WALLET -> NO_WALLET;
            case Printer3DBlockEntity.OUTPUT_FULL -> FULL;
            default -> READY;
        };
    }

    private static TextKey touchState(final Printer3DBlockEntity printer, final Scene scene) {
        if (printer.paused()) {
            return TOUCH_PAUSED;
        }
        return switch (printer.phase()) {
            case Printer3DBlockEntity.PRINTING -> TOUCH_PRINTING;
            case Printer3DBlockEntity.EJECTING, Printer3DBlockEntity.OUTPUT_FULL -> TOUCH_DONE;
            case Printer3DBlockEntity.WAITING_TRC, Printer3DBlockEntity.WAITING_WALLET -> TOUCH_WAITING;
            default -> TOUCH_READY;
        };
    }

    /* How {@code item} prints: its rows read from its picture, or its cube's sixteen for a block. */
    private Printed printed(final Item item) {
        if (readAt != KitModels.reloadedAt()) {
            printed.clear();
            readAt = KitModels.reloadedAt();
        }
        return printed.computeIfAbsent(item, Printer3DRenderer::read);
    }

    private static Printed read(final Item item) {
        final Minecraft minecraft = Minecraft.getInstance();
        final BakedModel model = minecraft.getItemRenderer().getModel(new ItemStack(item), null, null, 0);
        if (item instanceof BlockItem blockItem && model.isGui3d()) {
            final BlockState state = blockItem.getBlock().defaultBlockState();
            final BakedModel block = minecraft.getBlockRenderer().getBlockModel(state);
            final TextureAtlasSprite side = sprite(block, state, Direction.NORTH);
            final TextureAtlasSprite top = sprite(block, state, Direction.UP);
            final List<Row> rows = new ArrayList<>(GRID);
            for (int r = 0; r < GRID; r++) {
                rows.add(new Row(r, 0, GRID));
            }
            return new Printed(rows, side, top, true);
        }
        final TextureAtlasSprite sprite = model.getParticleIcon();
        final List<Row> rows = new ArrayList<>();
        for (int r = 0; r < GRID; r++) {
            int a = -1;
            int b = -1;
            for (int i = 0; i < GRID; i++) {
                if (opaque(sprite, i, r)) {
                    a = a < 0 ? i : a;
                    b = i + 1;
                }
            }
            if (a >= 0) {
                rows.add(new Row(r, a, b));
            }
        }
        if (rows.isEmpty()) {
            rows.add(new Row(0, 0, GRID));
        }
        return new Printed(rows, sprite, sprite, false);
    }

    /* Whether the texel of the sixteen-by-sixteen grid at column {@code i}, row {@code r} from the foot, is there. */
    private static boolean opaque(final TextureAtlasSprite sprite, final int i, final int r) {
        final int w = sprite.contents().width();
        final int h = sprite.contents().height();
        return !sprite.contents().isTransparent(0, i * w / GRID, (GRID - 1 - r) * h / GRID);
    }

    private static TextureAtlasSprite sprite(final BakedModel model, final BlockState state, final Direction side) {
        final List<BakedQuad> quads = model.getQuads(state, side, RandomSource.create(0L));
        return quads.isEmpty() ? model.getParticleIcon() : quads.getFirst().getSprite();
    }

    /* One texel of a flat item: a little box of its colour, read where the texel is on its picture. */
    private static void texel(final VertexConsumer vc, final PoseStack.Pose pose, final float x0, final float y0,
                              final float z0, final float x1, final float y1, final float z1,
                              final TextureAtlasSprite sprite, final int i, final int r, final int light,
                              final int overlay) {
        final float u = sprite.getU((i + HALF_BLOCK) / GRID);
        final float v = sprite.getV((GRID - 1 - r + HALF_BLOCK) / GRID);
        cube(vc, pose, x0, y0, z0, x1, y1, z1, new float[] {u, v, u, v}, new float[] {u, v, u, v},
                new float[] {u, v, u, v}, light, overlay);
    }

    /* One layer of a block's cube: its sides the row of its side's picture, its top its top's picture. */
    private static void box(final VertexConsumer vc, final PoseStack.Pose pose, final float x0, final float y0,
                            final float z0, final float x1, final float y1, final float z1, final Printed print,
                            final int r, final int a, final int b, final int light, final int overlay) {
        final TextureAtlasSprite side = print.side();
        final float v0 = side.getV((GRID - 1 - r) / (float) GRID);
        final float v1 = side.getV((GRID - r) / (float) GRID);
        final float[] front = {side.getU(a / (float) GRID), v0, side.getU(b / (float) GRID), v1};
        final float[] around = {side.getU0(), v0, side.getU1(), v1};
        final TextureAtlasSprite top = print.top();
        final float[] lid = {top.getU(a / (float) GRID), top.getV0(), top.getU(b / (float) GRID), top.getV1()};
        cube(vc, pose, x0, y0, z0, x1, y1, z1, front, around, lid, light, overlay);
    }

    /*
     * A box's six faces: {@code front} on its north and south, {@code around} on its east and west and under it,
     * {@code lid} on its top, each {u0, v0, u1, v1} as the face is seen from outside.
     */
    private static void cube(final VertexConsumer vc, final PoseStack.Pose pose, final float x0, final float y0,
                             final float z0, final float x1, final float y1, final float z1, final float[] front,
                             final float[] around, final float[] lid, final int light, final int overlay) {
        quad(vc, pose, new float[][] {{x1, y1, z0}, {x1, y0, z0}, {x0, y0, z0}, {x0, y1, z0}}, front, 0, 0, -1,
                light, overlay);
        quad(vc, pose, new float[][] {{x0, y1, z1}, {x0, y0, z1}, {x1, y0, z1}, {x1, y1, z1}},
                new float[] {front[2], front[1], front[0], front[3]}, 0, 0, 1, light, overlay);
        quad(vc, pose, new float[][] {{x1, y1, z1}, {x1, y0, z1}, {x1, y0, z0}, {x1, y1, z0}}, around, 1, 0, 0,
                light, overlay);
        quad(vc, pose, new float[][] {{x0, y1, z0}, {x0, y0, z0}, {x0, y0, z1}, {x0, y1, z1}}, around, -1, 0, 0,
                light, overlay);
        quad(vc, pose, new float[][] {{x1, y1, z0}, {x0, y1, z0}, {x0, y1, z1}, {x1, y1, z1}}, lid, 0, 1, 0,
                light, overlay);
        quad(vc, pose, new float[][] {{x1, y0, z1}, {x0, y0, z1}, {x0, y0, z0}, {x1, y0, z0}}, around, 0, -1, 0,
                light, overlay);
    }

    /* A face: its four corners from its top left as seen, going down first, with {u0, v0, u1, v1} over them. */
    private static void quad(final VertexConsumer vc, final PoseStack.Pose pose, final float[][] corners,
                             final float[] uv, final float nx, final float ny, final float nz, final int light,
                             final int overlay) {
        vertex(vc, pose, corners[0][0], corners[0][1], corners[0][2], uv[0], uv[1], nx, ny, nz, light, overlay);
        vertex(vc, pose, corners[1][0], corners[1][1], corners[1][2], uv[0], uv[3], nx, ny, nz, light, overlay);
        vertex(vc, pose, corners[2][0], corners[2][1], corners[2][2], uv[2], uv[3], nx, ny, nz, light, overlay);
        vertex(vc, pose, corners[3][0], corners[3][1], corners[3][2], uv[2], uv[1], nx, ny, nz, light, overlay);
    }

    private static void vertex(final VertexConsumer vc, final PoseStack.Pose pose, final float x, final float y,
                               final float z, final float u, final float v, final float nx, final float ny,
                               final float nz, final int light, final int overlay) {
        vc.addVertex(pose, x, y, z).setColor(-1).setUv(u, v).setOverlay(overlay).setLight(light)
                .setNormal(pose, nx, ny, nz);
    }

    /* A line between two places in the model's measures (u from the east), of {@code colour}. */
    private static void line(final VertexConsumer lines, final PoseStack.Pose pose, final double[] a,
                             final double[] b, final int colour) {
        final float ax = (float) (PIXEL - a[0]);
        final float bx = (float) (PIXEL - b[0]);
        final float dx = bx - ax;
        final float dy = (float) (b[1] - a[1]);
        final float dz = (float) (b[2] - a[2]);
        final float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        lines.addVertex(pose, ax, (float) a[1], (float) a[2]).setColor(colour)
                .setNormal(pose, dx / length, dy / length, dz / length);
        lines.addVertex(pose, bx, (float) b[1], (float) b[2]).setColor(colour)
                .setNormal(pose, dx / length, dy / length, dz / length);
    }

    private static void rect(final VertexConsumer fill, final Matrix4f face, final float x0, final float y0,
                             final float x1, final float y1, final int argb) {
        fill.addVertex(face, x0, y0, 0).setColor(argb).setLight(LightTexture.FULL_BRIGHT);
        fill.addVertex(face, x0, y1, 0).setColor(argb).setLight(LightTexture.FULL_BRIGHT);
        fill.addVertex(face, x1, y1, 0).setColor(argb).setLight(LightTexture.FULL_BRIGHT);
        fill.addVertex(face, x1, y0, 0).setColor(argb).setLight(LightTexture.FULL_BRIGHT);
    }

    private static void move(final ModelPose pose, final String bone, final double x, final double y,
                             final double z) {
        final int at = pose.index(bone);
        if (at >= 0) {
            pose.addMove(at, (float) x, (float) y, (float) z);
        }
    }

    private static void turn(final ModelPose pose, final String bone, final double x, final double y,
                             final double z) {
        final int at = pose.index(bone);
        if (at >= 0) {
            pose.addEuler(at, (float) x, (float) y, (float) z);
        }
    }

    /**
     * How an item prints: its rows, the pictures its texels and its cube's sides and top are read from, and whether it
     * is a block printed as its cube.
     */
    private record Printed(List<Row> rows, TextureAtlasSprite side, TextureAtlasSprite top, boolean block) {

        boolean opaque(final int i, final int r) {
            return Printer3DRenderer.opaque(side, i, r);
        }
    }

    /** What the printer does this frame. */
    private record Scene(@Nullable Printed print, @Nullable Item item, PrintMotion.Prosa prosa,
                         PrintMotion.Rattan rattan, double progress, double ejected, boolean working,
                         boolean paused) {
    }

    /** The colours of the screens' words and bars, and of the filament. */
    private record Screens(int lcd, int state, int name, int track, int bar, int filament) {
    }
}
