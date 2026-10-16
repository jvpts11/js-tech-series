/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.effect;

import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.effect.EffectDefinition;
import dev.jstech.core.effect.Emitter;
import dev.jstech.core.effect.NewEffectLayout;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

/**
 * A new effect from nothing, at the block looked at: from one of its faces or a locator of its model, from a starting
 * point (smoke, steam, sparks, a beam, a trail, nothing) that already plays there as it starts. Create writes the
 * effect's file, under the block's mod, and opens it in the effects editor.
 */
public final class NewEffectScreen extends Screen {

    private final BlockPos block;
    private final ResourceLocation blockId;
    private final List<From> froms = new ArrayList<>();
    private final Object preview = new Object();
    private int from;
    private int start = STEAM;
    private EditBox file;
    private Text status = Text.EMPTY;

    private static final int STEAM = 1;
    private static final List<Start> STARTS = List.of(new Start(EffectTexts.SMOKE, "smoke"),
            new Start(EffectTexts.STEAM, "steam"), new Start(EffectTexts.SPARKS, "sparks"),
            new Start(EffectTexts.A_BEAM, "beam"), new Start(EffectTexts.A_TRAIL, "trail"),
            new Start(EffectTexts.NOTHING, ""));
    private static final String ENDING = ".json";
    private static final int MOST_FILE = 64;
    private static final int TEXT_DROP = 4;
    private static final int LIST_INSET = 2;
    private static final float SMALL = 0.75F;
    private static final double FACE = 0.501;
    private static final int LABEL_GAP = 6;

    public NewEffectScreen(final BlockPos block, final ResourceLocation blockId, final List<String> locators) {
        super(GameText.component(EffectTexts.NEW_EFFECT));
        this.block = block.immutable();
        this.blockId = blockId;
        for (final Direction face : Direction.values()) {
            this.froms.add(new From(face, ""));
        }
        locators.forEach(locator -> this.froms.add(new From(null, locator)));
        this.from = Direction.UP.get3DDataValue();
    }

    /** Chooses the starting point at {@code index}, which plays at once in place of the one before. */
    public void startFrom(final int index) {
        if (index >= 0 && index < STARTS.size()) {
            this.start = index;
            if (this.file != null) {
                this.file.setValue(defaultFile());
            }
            play();
        }
    }

    /** Writes the new effect's file and opens it in the effects editor; whether it could. */
    public boolean create() {
        final String name = this.file.getValue().endsWith(ENDING)
                ? this.file.getValue().substring(0, this.file.getValue().length() - ENDING.length())
                : this.file.getValue();
        final ResourceLocation id = ResourceLocation.tryBuild(this.blockId.getNamespace(), name);
        if (id == null) {
            return false;
        }
        final Optional<Path> target = EffectFileWriter.fileOf(id);
        if (EffectFiles.get(id).isPresent() || target.isPresent() && Files.exists(target.get())) {
            this.status = EffectTexts.EXISTS.text();
            return false;
        }
        final EffectDefinition made = startDefinition().withAnchor(anchor());
        try {
            EffectFileWriter.write(id, made);
        } catch (final IOException failed) {
            this.status = EffectTexts.NOT_SAVED.with(EffectTexts.NO_FOLDER.with(Text.literal(id.getNamespace())));
            return false;
        }
        ClientEffects.release(this.preview);
        this.minecraft.setScreen(new EffectEditorScreen(id, made, placed()));
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void renderBackground(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        // The world shows behind the card, as the starting point plays.
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        final EffectCards.Colours c = EffectCards.PALETTE.get();
        EffectCards.debugLines(g, this.font, List.of(GameText.component(EffectTexts.PREVIEW.with(
                STARTS.get(this.start).words()))));
        marker(g);
        final int x0 = NewEffectLayout.x(this.width);
        final int y0 = NewEffectLayout.MARGIN;
        EffectCards.window(g, x0, y0, x0 + NewEffectLayout.WIDTH, y0 + NewEffectLayout.HEIGHT);
        final int x = x0 + NewEffectLayout.INSET;
        Draw.text(g, this.font, GameText.component(EffectTexts.NEW_EFFECT), x, y0 + NewEffectLayout.TITLE_Y, c.ink(),
                c.ground());
        small(g, GameText.resolve(EffectTexts.AT_THE_BLOCK), x, y0 + NewEffectLayout.SUBTITLE_Y, c.soft());
        small(g, GameText.resolve(EffectTexts.BLOCK), x, y0 + NewEffectLayout.BLOCK_WORD_Y, c.ink());
        small(g, GameText.resolve(EffectTexts.FILE), x, y0 + NewEffectLayout.FILE_WORD_Y, c.ink());
        Draw.text(g, this.font, GameText.component(EffectTexts.START_FROM), x, y0 + NewEffectLayout.START_Y, c.ink(),
                c.ground());
        for (int i = 0; i < STARTS.size(); i++) {
            final int y = y0 + NewEffectLayout.LIST_Y + i * NewEffectLayout.LIST_PITCH;
            final boolean chosen = i == this.start;
            if (chosen) {
                g.fill(x0 + 4, y - LIST_INSET, x0 + NewEffectLayout.WIDTH - 4, y + NewEffectLayout.LIST_PITCH
                        - LIST_INSET, c.chosen());
            }
            Draw.text(g, this.font, GameText.component(STARTS.get(i).words()), x + LIST_INSET, y,
                    chosen ? c.white() : c.ink(), chosen ? c.chosen() : c.ground());
        }
        super.render(g, mouseX, mouseY, partialTick);
        if (!this.status.isEmpty()) {
            small(g, GameText.resolve(this.status), x, y0 + NewEffectLayout.HEIGHT + TEXT_DROP, c.marker());
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final int x0 = NewEffectLayout.x(this.width);
        final int y0 = NewEffectLayout.MARGIN + NewEffectLayout.LIST_Y - LIST_INSET;
        if (mouseX >= x0 + 4 && mouseX < x0 + NewEffectLayout.WIDTH - 4 && mouseY >= y0
                && mouseY < y0 + STARTS.size() * NewEffectLayout.LIST_PITCH) {
            startFrom((int) ((mouseY - y0) / NewEffectLayout.LIST_PITCH));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void removed() {
        ClientEffects.release(this.preview);
        super.removed();
    }

    @Override
    protected void init() {
        final int x = NewEffectLayout.x(this.width) + NewEffectLayout.INSET;
        final int y0 = NewEffectLayout.MARGIN;
        final EditBox blockField = new EditBox(this.font, x, y0 + NewEffectLayout.BLOCK_Y, NewEffectLayout.INNER,
                NewEffectLayout.FIELD_HEIGHT, GameText.component(EffectTexts.BLOCK));
        blockField.setValue(this.blockId.toString());
        blockField.setEditable(false);
        addRenderableWidget(blockField);
        addRenderableWidget(Button.builder(fromWords(), button -> {
            this.from = (this.from + 1) % this.froms.size();
            button.setMessage(fromWords());
            play();
        }).bounds(x, y0 + NewEffectLayout.FROM_Y, NewEffectLayout.INNER, NewEffectLayout.FROM_HEIGHT).build());
        final String kept = this.file == null ? defaultFile() : this.file.getValue();
        this.file = new EditBox(this.font, x, y0 + NewEffectLayout.FILE_Y, NewEffectLayout.INNER,
                NewEffectLayout.FIELD_HEIGHT, GameText.component(EffectTexts.FILE));
        this.file.setMaxLength(MOST_FILE);
        this.file.setValue(kept);
        addRenderableWidget(this.file);
        addRenderableWidget(Button.builder(GameText.component(EffectTexts.CREATE), button -> create())
                .bounds(x, y0 + NewEffectLayout.CREATE_Y, NewEffectLayout.INNER, NewEffectLayout.CREATE_HEIGHT)
                .build());
        if (EffectEngine.held(this.preview).isEmpty()) {
            play();
        }
    }

    /* Plays the starting point chosen where it starts, as it starts. */
    private void play() {
        final EffectAnchors.Placed placed = placed();
        ClientEffects.preview(this.preview, startDefinition(), placed::at, placed::way);
    }

    private EffectAnchors.Placed placed() {
        return EffectAnchors.of(this.minecraft.level, this.block, Optional.of(anchor()));
    }

    private EffectDefinition.Anchor anchor() {
        final From chosen = this.froms.get(this.from);
        return chosen.face() != null ? EffectDefinition.Anchor.at(chosen.face())
                : EffectDefinition.Anchor.locator(chosen.locator());
    }

    /* The starting point's effect, the Core's own of that name; one plain emitter for nothing. */
    private EffectDefinition startDefinition() {
        final String name = STARTS.get(this.start).effect();
        if (name.isEmpty()) {
            return new EffectDefinition(List.of(new Emitter("particles", Emitter.Kind.PARTICLES, true,
                    Emitter.Emission.NONE, Emitter.Look.PLAIN, Emitter.Motion.FLOATING, Emitter.Extra.NONE)),
                    EffectDefinition.NOTHING.reach(), Optional.empty());
        }
        return EffectFiles.get(ResourceLocation.fromNamespaceAndPath(JsCore.MODID, name))
                .orElse(EffectDefinition.NOTHING);
    }

    private String defaultFile() {
        final String name = STARTS.get(this.start).effect();
        return this.blockId.getPath() + (name.isEmpty() ? "" : "_" + name) + ENDING;
    }

    private Component fromWords() {
        final From chosen = this.froms.get(this.from);
        final Text where = chosen.face() != null ? EffectTexts.FACES.get(chosen.face().get3DDataValue()).text()
                : EffectTexts.LOCATOR.with(Text.literal(chosen.locator()));
        return GameText.component(EffectTexts.FROM.with(where));
    }

    /* Where the effect starts, in pink on the world: the face's edges, or the locator's place, and its name. */
    private void marker(final GuiGraphics g) {
        final From chosen = this.froms.get(this.from);
        final EffectAnchors.Placed placed = placed();
        final Text where;
        float[] label = EffectCards.onScreen(placed.at(), this.width, this.height);
        if (chosen.face() != null) {
            final Direction face = chosen.face();
            final List<Vec3> corners = corners(face);
            for (int i = 0; i < corners.size(); i++) {
                final float[] a = EffectCards.onScreen(corners.get(i), this.width, this.height);
                final float[] b = EffectCards.onScreen(corners.get((i + 1) % corners.size()), this.width,
                        this.height);
                if (a != null && b != null) {
                    EffectCards.dotted(g, a[0], a[1], b[0], b[1]);
                }
            }
            label = EffectCards.onScreen(corners.get(1), this.width, this.height);
            where = EffectTexts.THE_FACES.get(face.get3DDataValue()).text();
        } else {
            where = EffectTexts.THE_LOCATOR.with(Text.literal(chosen.locator()));
        }
        if (label != null) {
            // Beside the place, on the side away from the card.
            final String words = GameText.resolve(EffectTexts.HERE.with(where));
            final int wide = (int) Math.ceil(this.font.width(words) * SMALL);
            final int right = (int) label[0] + LABEL_GAP;
            final int x = right + wide < NewEffectLayout.x(this.width) - LABEL_GAP ? right
                    : (int) label[0] - wide - LABEL_GAP * 4;
            small(g, words, x, (int) label[1], EffectCards.PALETTE.get().marker());
        }
    }

    /* The corners of the block's face {@code face}, round it. */
    private List<Vec3> corners(final Direction face) {
        final Vec3 middle = Vec3.atCenterOf(this.block).relative(face, FACE);
        final Direction a = face.getAxis() == Direction.Axis.Y ? Direction.EAST : face.getClockWise();
        final Direction b = face.getAxis() == Direction.Axis.Y ? Direction.SOUTH : Direction.UP;
        final Vec3 u = Vec3.atLowerCornerOf(a.getNormal()).scale(0.5);
        final Vec3 v = Vec3.atLowerCornerOf(b.getNormal()).scale(0.5);
        return List.of(middle.subtract(u).subtract(v), middle.add(u).subtract(v), middle.add(u).add(v),
                middle.subtract(u).add(v));
    }

    private void small(final GuiGraphics g, final String words, final int x, final int y, final int colour) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(SMALL, SMALL, 1);
        Draw.text(g, this.font, words, 0, 0, colour);
        g.pose().popPose();
    }

    /* Where on the block an effect starts: a face, or a locator of its model. */
    private record From(Direction face, String locator) {
    }

    /* A starting point: its words, and the Core's effect of its name; none for nothing. */
    private record Start(TextKey words, String effect) {
    }
}
