/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.model.kit;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.jstech.core.api.client.GpuSurface;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.gui.layout.ModelPreviewLayout;
import dev.jstech.core.model.AnimationController;
import dev.jstech.core.model.BakedGeometry;
import dev.jstech.core.model.IAnimationListener;
import dev.jstech.core.model.ModelAnimation;
import dev.jstech.core.model.ModelBone;
import dev.jstech.core.model.ModelDefinition;
import dev.jstech.core.model.ModelFormatException;
import dev.jstech.core.model.ModelLocator;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The preview of a model, for whoever makes one: the model turned, zoomed and moved with the mouse over a floor of
 * quarter blocks; its animations, each played or paused by its button; its tree of bones, the one chosen drawn in
 * yellow with the bones it hangs from; its locators by name; its glowing parts shown in the dark; its faces as wire;
 * the time of the animation, dragged along its timeline; the speed it plays at. A model's files changed and read again
 * show at once, the animation going on.
 */
public final class ModelPreviewScreen extends Screen {

    private final ResourceLocation id;
    private final List<Row> rows = new ArrayList<>();
    private final List<Button> animationButtons = new ArrayList<>();
    @Nullable
    private KitModel model;
    @Nullable
    private KitInstance instance;
    @Nullable
    private String broken;
    @Nullable
    private GpuSurface surface;
    @Nullable
    private ModelAnimation chosen;
    @Nullable
    private Timeline timeline;
    @Nullable
    private Button speedButton;
    private long seenReload = -1;
    private float yaw = 45;
    private float pitch = 30;
    private float zoom = 1;
    private float panX;
    private float panY;
    private boolean showBones = true;
    private boolean showLocators = true;
    private boolean showGlow;
    private boolean showWire;
    private boolean paused;
    private int speedIndex = 2;
    private int selected = -1;
    private int buttonScroll;
    private int treeScroll;

    private static final String CONTROLLER = "preview";
    private static final double[] SPEEDS = {0.25, 0.5, 1, 2, 4};
    private static final float SMALL = 0.75F;
    private static final float GRID_REACH = 2.0F;
    private static final float GRID_STEP = 0.25F;
    private static final float PIXEL = 1.0F / 16.0F;
    private static final int DARK = LightTexture.pack(0, 0);

    public ModelPreviewScreen(final ResourceLocation id) {
        super(GameText.component(KitTexts.TITLE.with(id.toString())));
        this.id = id;
    }

    @Override
    protected void init() {
        resolve();
        this.animationButtons.clear();
        final List<ModelAnimation> animations = this.model == null ? List.of() : this.model.animations();
        final int shown = ModelPreviewLayout.buttonsShown(this.height, animations.size());
        this.buttonScroll = Math.max(0, Math.min(this.buttonScroll, animations.size() - shown));
        for (int i = 0; i < shown; i++) {
            final int index = i;
            final Button button = Button.builder(Component.empty(), pressed -> pressAnimation(index))
                    .bounds(ModelPreviewLayout.columnX(this.width), ModelPreviewLayout.buttonY(i),
                            ModelPreviewLayout.COLUMN, ModelPreviewLayout.CONTROL).build();
            this.animationButtons.add(addRenderableWidget(button));
        }
        final TextKey[] switches = {KitTexts.BONES, KitTexts.LOCATORS, KitTexts.GLOW, KitTexts.WIRE};
        final boolean[] on = {this.showBones, this.showLocators, this.showGlow, this.showWire};
        for (int i = 0; i < switches.length; i++) {
            final int index = i;
            addRenderableWidget(Checkbox.builder(GameText.component(switches[i]), this.font)
                    .pos(ModelPreviewLayout.switchX(this.width, i), ModelPreviewLayout.switchY(this.height, i))
                    .selected(on[i]).onValueChange((box, value) -> toggle(index, value)).build());
        }
        this.timeline = addRenderableWidget(new Timeline(ModelPreviewLayout.timelineX(),
                ModelPreviewLayout.controlY(this.height), ModelPreviewLayout.timelineWidth(this.width)));
        this.speedButton = addRenderableWidget(Button.builder(speedText(), pressed -> nextSpeed())
                .bounds(ModelPreviewLayout.speedX(this.width), ModelPreviewLayout.controlY(this.height),
                        ModelPreviewLayout.SPEED_WIDTH, ModelPreviewLayout.CONTROL).build());
        labelButtons();
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        if (KitModels.reloadedAt() != this.seenReload) {
            // The files were read again: the same model, its shape and animations as they are now.
            rebuildWidgets();
        }
        final double now = now();
        if (this.instance != null) {
            this.instance.animate(KitClock.frame(), now, false, IAnimationListener.NONE);
        }
        super.render(g, mouseX, mouseY, partialTick);
        final ModelPreviewPalette.Colours colours = ModelPreviewPalette.get();
        g.fill(0, 0, this.width, ModelPreviewLayout.TITLE_HEIGHT, colours.titleBar());
        Draw.text(g, this.font, this.title, ModelPreviewLayout.MARGIN, (ModelPreviewLayout.TITLE_HEIGHT - 8) / 2,
                colours.title(), colours.titleBar());
        small(g, GameText.component(KitTexts.RELOADED.with((Util.getMillis() - KitModels.reloadedAt()) / 1000)),
                this.width - ModelPreviewLayout.MARGIN - ModelPreviewLayout.RELOADED_WIDTH,
                (ModelPreviewLayout.TITLE_HEIGHT - 6) / 2, colours.faint());
        view(g, colours);
        column(g, colours);
        g.fill(0, ModelPreviewLayout.bottomY(this.height), this.width, this.height, colours.bar());
        final Component playing = this.chosen == null ? GameText.component(KitTexts.NOTHING_PLAYS)
                : Component.literal(shortName(this.chosen.name()));
        Draw.text(g, this.font, this.font.plainSubstrByWidth(playing.getString(), ModelPreviewLayout.NAME_WIDTH),
                ModelPreviewLayout.MARGIN, ModelPreviewLayout.controlY(this.height) + 6, colours.text(),
                colours.bar());
        if (this.timeline != null) {
            this.timeline.follow(now);
        }
        for (int i = 0; i < this.animationButtons.size(); i++) {
            final ModelAnimation shown = animationAt(i);
            if (shown != null && shown == this.chosen && !this.paused) {
                final Button button = this.animationButtons.get(i);
                Draw.outline(g, button.getX() - 1, button.getY() - 1, button.getWidth() + 2, button.getHeight() + 2,
                        colours.frame());
            }
        }
    }

    @Override
    public void renderBackground(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        g.fill(ModelPreviewLayout.viewX(), ModelPreviewLayout.viewY(), ModelPreviewLayout.viewX()
                + ModelPreviewLayout.viewWidth(this.width), ModelPreviewLayout.viewY()
                + ModelPreviewLayout.viewHeight(this.height), ModelPreviewPalette.get().view());
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX,
                                final double dragY) {
        if (inView(mouseX, mouseY) && button == 0) {
            if (hasShiftDown()) {
                this.panX += (float) dragX;
                this.panY += (float) dragY;
            } else {
                this.yaw += (float) dragX;
                this.pitch = Math.max(-90, Math.min(90, this.pitch + (float) dragY));
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        if (inView(mouseX, mouseY)) {
            this.zoom = (float) Math.max(0.1, Math.min(20, this.zoom * Math.pow(1.1, scrollY)));
            return true;
        }
        final int column = ModelPreviewLayout.columnX(this.width);
        final int count = this.model == null ? 0 : this.model.animations().size();
        if (mouseX >= column && mouseY < ModelPreviewLayout.bonesLabelY(this.height, count)) {
            this.buttonScroll = Math.max(0, this.buttonScroll - (int) Math.signum(scrollY));
            rebuildWidgets();
            return true;
        }
        if (mouseX >= column && mouseY < ModelPreviewLayout.switchesTop(this.height)) {
            this.treeScroll = Math.max(0, Math.min(Math.max(0, this.rows.size() - ModelPreviewLayout.treeRows(
                    this.height, count)), this.treeScroll - (int) Math.signum(scrollY)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final int count = this.model == null ? 0 : this.model.animations().size();
        final int top = ModelPreviewLayout.treeTop(this.height, count);
        final int shownRows = ModelPreviewLayout.treeRows(this.height, count);
        if (mouseX >= ModelPreviewLayout.columnX(this.width) && mouseY >= top
                && mouseY < top + shownRows * ModelPreviewLayout.TREE_ROW) {
            final int row = (int) ((mouseY - top) / ModelPreviewLayout.TREE_ROW) + this.treeScroll;
            if (row < this.rows.size()) {
                this.selected = this.rows.get(row).bone == this.selected ? -1 : this.rows.get(row).bone;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void removed() {
        if (this.surface != null) {
            this.surface.close();
            this.surface = null;
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    /* The model, read again if the files were, and what moves it, its animation going on. */
    private void resolve() {
        this.seenReload = KitModels.reloadedAt();
        final String playing = this.chosen == null ? null : this.chosen.name();
        try {
            final Optional<KitModel> declared = KitModels.get(this.id);
            this.model = declared.isPresent() ? declared.get() : KitModels.file(this.id);
            this.broken = null;
        } catch (final ModelFormatException | IllegalStateException missing) {
            this.model = null;
            this.broken = missing.getMessage();
        }
        this.instance = this.model == null ? null : new KitInstance(this.model);
        this.chosen = null;
        if (this.instance != null && playing != null) {
            this.instance.animator().animation(playing).ifPresent(again -> play(again, false));
        }
        this.rows.clear();
        if (this.model != null) {
            tree(null, 0);
        }
    }

    /* The tree of bones, each under the one it hangs from. */
    private void tree(@Nullable final String parent, final int depth) {
        final List<ModelBone> bones = this.model.data().bones();
        for (int i = 0; i < bones.size(); i++) {
            final ModelBone bone = bones.get(i);
            if (parent == null ? bone.parent() == null : parent.equals(bone.parent())) {
                this.rows.add(new Row(i, depth));
                tree(bone.name(), depth + 1);
            }
        }
    }

    private void view(final GuiGraphics g, final ModelPreviewPalette.Colours colours) {
        final int x = ModelPreviewLayout.viewX();
        final int y = ModelPreviewLayout.viewY();
        final int w = ModelPreviewLayout.viewWidth(this.width);
        final int h = ModelPreviewLayout.viewHeight(this.height);
        if (this.instance == null) {
            int line = y + 8;
            for (final FormattedCharSequence wrapped : this.font.split(GameText.component(KitTexts.BROKEN.with(
                    String.valueOf(this.broken))), w - 16)) {
                Draw.text(g, this.font, wrapped, x + 8, line, colours.text());
                line += this.font.lineHeight + 1;
            }
            return;
        }
        final double guiScale = Minecraft.getInstance().getWindow().getGuiScale();
        final int pixelsWide = Math.max(1, (int) Math.round(w * guiScale));
        final int pixelsTall = Math.max(1, (int) Math.round(h * guiScale));
        if (this.surface == null || this.surface.width() != pixelsWide || this.surface.height() != pixelsTall) {
            if (this.surface != null) {
                this.surface.close();
            }
            this.surface = new GpuSurface(pixelsWide, pixelsTall);
        }
        final SceneRenderer.View seen = seen(pixelsWide, pixelsTall, guiScale);
        g.flush();
        SceneRenderer.draw(this.surface, seen, this::paint);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        this.surface.blit(g, x, y, w, h);
        RenderSystem.disableBlend();
        g.enableScissor(x, y, x + w, y + h);
        overlay(g, colours, seen, pixelsWide, pixelsTall, guiScale);
        g.disableScissor();
        small(g, GameText.component(KitTexts.HINT), x + 4, y + h - 9, colours.faint());
    }

    /* How the model is seen: its middle at the view's, a block as big as the view leaves room for, zoomed. */
    private SceneRenderer.View seen(final int pixelsWide, final int pixelsTall, final double guiScale) {
        final KitModel shown = this.instance.model();
        final double reach = Math.max(1, Math.max(shown.bounds().getXsize(), Math.max(shown.bounds().getYsize(),
                shown.bounds().getZsize())));
        final float scale = (float) (Math.min(pixelsWide, pixelsTall) * 0.55 / reach * this.zoom);
        return new SceneRenderer.View((float) shown.bounds().getCenter().x, (float) shown.bounds().getCenter().y,
                (float) shown.bounds().getCenter().z, this.yaw, this.pitch, scale, (float) (this.panX * guiScale),
                (float) (this.panY * guiScale));
    }

    /* The floor, the model, and its wire, in the scene's surface. */
    private void paint(final PoseStack pose, final MultiBufferSource.BufferSource buffers) {
        final ModelPreviewPalette.Colours colours = ModelPreviewPalette.get();
        final VertexConsumer lines = buffers.getBuffer(RenderType.lines());
        for (float at = -GRID_REACH; at <= GRID_REACH + 1.0e-3F; at += GRID_STEP) {
            line(lines, pose, at, 0, -GRID_REACH, at, 0, GRID_REACH, colours.grid());
            line(lines, pose, -GRID_REACH, 0, at, GRID_REACH, 0, at, colours.grid());
        }
        KitRenderer.draw(this.instance, ModelDefinition.Variant.NONE, this, pose, buffers,
                this.showGlow ? DARK : LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, null, null);
        if (this.showWire) {
            wire(pose, buffers.getBuffer(RenderType.lines()), colours.wire());
        }
    }

    private void wire(final PoseStack pose, final VertexConsumer lines, final int colour) {
        final Matrix4f[] world = this.instance.world();
        final Vector3f a = new Vector3f();
        final Vector3f b = new Vector3f();
        final List<List<BakedGeometry.Part>> bones = this.instance.model().geometry().bones();
        for (int i = 0; i < bones.size(); i++) {
            if (this.instance.pose().hidden(i)) {
                continue;
            }
            for (final BakedGeometry.Part part : bones.get(i)) {
                final float[] v = part.vertices();
                for (int face = 0; face < v.length; face += BakedGeometry.STRIDE * 4) {
                    for (int k = 0; k < 4; k++) {
                        final int from = face + k * BakedGeometry.STRIDE;
                        final int to = face + (k + 1) % 4 * BakedGeometry.STRIDE;
                        world[i].transformPosition(a.set(v[from], v[from + 1], v[from + 2])).mul(PIXEL);
                        world[i].transformPosition(b.set(v[to], v[to + 1], v[to + 2])).mul(PIXEL);
                        line(lines, pose, a.x, a.y, a.z, b.x, b.y, b.z, colour);
                    }
                }
            }
        }
    }

    /* The bones' pivots and the lines between them, and the locators by name, over the picture. */
    private void overlay(final GuiGraphics g, final ModelPreviewPalette.Colours colours,
                         final SceneRenderer.View seen, final int pixelsWide, final int pixelsTall,
                         final double guiScale) {
        final Matrix4f[] world = this.instance.world();
        final List<ModelBone> bones = this.instance.model().data().bones();
        final Vector3f[] pivots = new Vector3f[bones.size()];
        for (int i = 0; i < bones.size(); i++) {
            final Vector3f pivot = world[i].transformPosition(new Vector3f(bones.get(i).pivot())).mul(PIXEL);
            pivots[i] = toScreen(seen, pixelsWide, pixelsTall, guiScale, pivot);
        }
        if (this.showBones) {
            final BufferBuilder builder = Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES,
                    DefaultVertexFormat.POSITION_COLOR);
            final Matrix4f at = g.pose().last().pose();
            boolean any = false;
            for (int i = 0; i < bones.size(); i++) {
                final int parent = this.instance.pose().parent(i);
                if (parent >= 0) {
                    final int colour = chain(i) ? colours.chosen() : colours.boneLine();
                    builder.addVertex(at, pivots[parent].x, pivots[parent].y, 0).setColor(colour);
                    builder.addVertex(at, pivots[i].x, pivots[i].y, 0).setColor(colour);
                    any = true;
                }
            }
            if (any) {
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                BufferUploader.drawWithShader(builder.buildOrThrow());
            }
            for (int i = 0; i < bones.size(); i++) {
                final int colour = chain(i) ? colours.chosen() : colours.boneLine();
                g.fill((int) pivots[i].x - 2, (int) pivots[i].y - 2, (int) pivots[i].x + 2, (int) pivots[i].y + 2,
                        colour);
            }
        }
        if (this.showLocators) {
            for (int i = 0; i < bones.size(); i++) {
                for (final ModelLocator locator : bones.get(i).locators()) {
                    final Vector3f place = world[i].transformPosition(new Vector3f(locator.offset())).mul(PIXEL);
                    final Vector3f shown = toScreen(seen, pixelsWide, pixelsTall, guiScale, place);
                    final int sx = (int) shown.x;
                    final int sy = (int) shown.y;
                    g.fill(sx - 5, sy, sx + 6, sy + 1, colours.locator());
                    g.fill(sx, sy - 5, sx + 1, sy + 6, colours.locator());
                    small(g, Component.literal(locator.name()), sx + 7, sy - 3, colours.locator());
                }
            }
        }
    }

    /* Whether bone {@code bone} is the chosen one or one the chosen one hangs from. */
    private boolean chain(final int bone) {
        for (int at = this.selected; at >= 0; at = this.instance.pose().parent(at)) {
            if (at == bone) {
                return true;
            }
        }
        return false;
    }

    private Vector3f toScreen(final SceneRenderer.View seen, final int pixelsWide, final int pixelsTall,
                              final double guiScale, final Vector3f point) {
        final Vector3f out = SceneRenderer.project(seen, pixelsWide, pixelsTall, point.x, point.y, point.z,
                new Vector3f());
        return out.set((float) (ModelPreviewLayout.viewX() + out.x / guiScale),
                (float) (ModelPreviewLayout.viewY() + out.y / guiScale), out.z);
    }

    private void column(final GuiGraphics g, final ModelPreviewPalette.Colours colours) {
        final int x = ModelPreviewLayout.columnX(this.width);
        final int count = this.model == null ? 0 : this.model.animations().size();
        Draw.text(g, this.font, GameText.component(KitTexts.ANIMATIONS), x, ModelPreviewLayout.animationsLabelY(),
                colours.text());
        if (count == 0) {
            small(g, GameText.component(KitTexts.NO_ANIMATIONS), x + 4, ModelPreviewLayout.buttonsTop(),
                    colours.faint());
        }
        Draw.text(g, this.font, GameText.component(KitTexts.BONES), x, ModelPreviewLayout.bonesLabelY(this.height,
                count), colours.text());
        final int top = ModelPreviewLayout.treeTop(this.height, count);
        final int shown = ModelPreviewLayout.treeRows(this.height, count);
        for (int row = 0; row < shown && row + this.treeScroll < this.rows.size(); row++) {
            final Row each = this.rows.get(row + this.treeScroll);
            final String name = this.model.data().bones().get(each.bone).name();
            small(g, Component.literal(name), x + 4 + each.depth * ModelPreviewLayout.TREE_INDENT,
                    top + row * ModelPreviewLayout.TREE_ROW, each.bone == this.selected ? colours.chosen()
                            : colours.tree());
        }
    }

    private void pressAnimation(final int index) {
        final ModelAnimation pressed = animationAt(index);
        if (pressed == null || this.instance == null) {
            return;
        }
        final AnimationController controller = this.instance.animator().controller(CONTROLLER);
        if (pressed == this.chosen) {
            this.paused = !this.paused;
            controller.speed(this.paused ? 0 : SPEEDS[this.speedIndex], now());
        } else {
            play(pressed, true);
        }
        labelButtons();
    }

    private void play(final ModelAnimation animation, final boolean fromStart) {
        final AnimationController controller = this.instance.animator().controller(CONTROLLER);
        final double now = now();
        final double at = this.chosen == null ? 0 : controller.elapsed(now);
        controller.replay(animation, now, 0);
        if (!fromStart) {
            controller.seek(at, now);
        }
        controller.speed(this.paused ? 0 : SPEEDS[this.speedIndex], now);
        this.chosen = animation;
    }

    private void toggle(final int index, final boolean value) {
        switch (index) {
            case 0 -> this.showBones = value;
            case 1 -> this.showLocators = value;
            case 2 -> this.showGlow = value;
            default -> this.showWire = value;
        }
    }

    private void nextSpeed() {
        this.speedIndex = (this.speedIndex + 1) % SPEEDS.length;
        if (this.instance != null && !this.paused) {
            this.instance.animator().controller(CONTROLLER).speed(SPEEDS[this.speedIndex], now());
        }
        if (this.speedButton != null) {
            this.speedButton.setMessage(speedText());
        }
    }

    private Component speedText() {
        return GameText.component(KitTexts.SPEED.with(String.format(Locale.ROOT, "%.2f", SPEEDS[this.speedIndex])
                .replaceAll("0+$", "").replaceAll("\\.$", ".0")));
    }

    private void labelButtons() {
        for (int i = 0; i < this.animationButtons.size(); i++) {
            final ModelAnimation shown = animationAt(i);
            if (shown == null) {
                continue;
            }
            final TextKey loop = switch (shown.loop()) {
                case LOOP -> KitTexts.LOOP;
                case HOLD -> KitTexts.HOLD;
                case ONCE -> KitTexts.ONCE;
            };
            final TextKey label = shown == this.chosen && !this.paused ? KitTexts.PAUSE : KitTexts.PLAY;
            this.animationButtons.get(i).setMessage(GameText.component(label.with(shortName(shown.name()),
                    GameText.resolve(loop))));
        }
    }

    @Nullable
    private ModelAnimation animationAt(final int index) {
        if (this.model == null || index + this.buttonScroll >= this.model.animations().size()) {
            return null;
        }
        return this.model.animations().get(index + this.buttonScroll);
    }

    private boolean inView(final double mouseX, final double mouseY) {
        return mouseX >= ModelPreviewLayout.viewX() && mouseY >= ModelPreviewLayout.viewY()
                && mouseX < ModelPreviewLayout.viewX() + ModelPreviewLayout.viewWidth(this.width)
                && mouseY < ModelPreviewLayout.viewY() + ModelPreviewLayout.viewHeight(this.height);
    }

    private void small(final GuiGraphics g, final Component text, final int x, final int y, final int colour) {
        Draw.textScaled(g, this.font, text.getString(), x, y, colour, SMALL);
    }

    /* The clock of the preview: the wall's, so a model plays on while the world stands still behind it. */
    private static double now() {
        return Util.getMillis() / 1000.0;
    }

    /* An animation's name without what every animation of a Bedrock file shares: "open" for "animation.door.open". */
    private static String shortName(final String name) {
        final int dot = name.lastIndexOf('.');
        return name.startsWith("animation.") && dot >= 0 ? name.substring(dot + 1) : name;
    }

    private static void line(final VertexConsumer lines, final PoseStack pose, final float x1, final float y1,
                             final float z1, final float x2, final float y2, final float z2, final int colour) {
        final float dx = x2 - x1;
        final float dy = y2 - y1;
        final float dz = z2 - z1;
        final float length = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (length == 0) {
            return;
        }
        lines.addVertex(pose.last(), x1, y1, z1).setColor(colour).setNormal(pose.last(), dx / length, dy / length,
                dz / length);
        lines.addVertex(pose.last(), x2, y2, z2).setColor(colour).setNormal(pose.last(), dx / length, dy / length,
                dz / length);
    }

    /* A row of the tree of bones: which bone, and how deep it hangs. */
    private record Row(int bone, int depth) {
    }

    /* The timeline: how far the animation is, dragged to put it there. */
    private final class Timeline extends AbstractSliderButton {

        private boolean dragging;

        Timeline(final int x, final int y, final int width) {
            super(x, y, width, ModelPreviewLayout.CONTROL, Component.empty(), 0);
        }

        /* Moves with the animation, unless it is being dragged. */
        void follow(final double now) {
            if (this.dragging || ModelPreviewScreen.this.instance == null || ModelPreviewScreen.this.chosen == null) {
                updateMessage();
                return;
            }
            final ModelAnimation playing = ModelPreviewScreen.this.chosen;
            final double elapsed = ModelPreviewScreen.this.instance.animator().controller(CONTROLLER).elapsed(now);
            this.value = playing.length() <= 0 ? 0 : playing.timeAt(elapsed) / playing.length();
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            final ModelAnimation playing = ModelPreviewScreen.this.chosen;
            final double length = playing == null ? 0 : playing.length();
            setMessage(GameText.component(KitTexts.TIME.with(String.format(Locale.ROOT, "%.2f", this.value * length),
                    String.format(Locale.ROOT, "%.2f", length))));
        }

        @Override
        protected void applyValue() {
            final ModelAnimation playing = ModelPreviewScreen.this.chosen;
            if (playing != null && ModelPreviewScreen.this.instance != null) {
                ModelPreviewScreen.this.instance.animator().controller(CONTROLLER).seek(this.value
                        * playing.length(), now());
            }
        }

        @Override
        public void onClick(final double mouseX, final double mouseY) {
            this.dragging = true;
            super.onClick(mouseX, mouseY);
        }

        @Override
        public void onRelease(final double mouseX, final double mouseY) {
            this.dragging = false;
            super.onRelease(mouseX, mouseY);
        }
    }
}
