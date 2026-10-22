/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.developer.studio.StudioAnswers;
import dev.jstech.core.developer.studio.StudioKeys;
import dev.jstech.core.developer.studio.StudioServices;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.multiblock.ShapePreview;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Catching a structure: two corners taken from the blocks looked at, what lies between them counted by the server as a
 * structure file holds it, saved to the mod under a name, and shown on the page and, through
 * {@link StudioClient#caught()}, in the world by whatever shows multiblocks where they stand. A structure caught is
 * made a multiblock on the same page ({@link MultiblockMaker}).
 */
final class StructuresPage extends StudioPage {

    private final Button corner1;
    private final Button corner2;
    private final TextField name = this.panel.add(new TextField(LONGEST));
    private final Button save;
    private final Button project;
    private final Button make;
    private final MultiblockMaker maker;
    private final StudioScene scene = new StudioScene();
    private @Nullable CompoundTag counted;
    private String written = "";
    private int writtenY;
    private boolean turning;

    private static final int LONGEST = 128;
    private static final String STRUCTURE = "structure";

    /** The corners, kept while the game runs, as the player opens the studio on one corner and then the other. */
    private static @Nullable BlockPos first;
    private static @Nullable BlockPos second;
    private static @Nullable ShapePreview shown;
    private static boolean projecting;

    StructuresPage(final StudioScreen studio) {
        super(studio);
        this.corner1 = this.panel.add(new Button(() -> words(ToolTexts.CORNER_1), () -> this.corner(true)));
        this.corner2 = this.panel.add(new Button(() -> words(ToolTexts.CORNER_2), () -> this.corner(false)));
        this.save = this.panel.add(new Button(() -> words(ToolTexts.SAVE_TO_MOD), this::save).setPrimary(true));
        this.project = this.panel.add(new Button(() -> words(projecting ? ToolTexts.HIDE_PROJECTOR
                : ToolTexts.SHOW_PROJECTOR), this::project));
        this.make = this.panel.add(new Button(() -> words(ToolTexts.MAKE_MULTIBLOCK), this::startMaking));
        this.maker = new MultiblockMaker(studio, this.panel, () -> this.name.edit(),
                () -> this.layout(this.regions), this::project, () -> projecting);
    }

    /** The structure caught, while it is asked to be shown in the world, at its own place, as it stands now. */
    static Optional<StudioClient.Caught> projected() {
        if (!projecting || shown == null || first == null || second == null) {
            return Optional.empty();
        }
        final BlockPos min = new BlockPos(Math.min(first.getX(), second.getX()), Math.min(first.getY(),
                second.getY()), Math.min(first.getZ(), second.getZ()));
        return Optional.of(new StudioClient.Caught(shown, min, ToolTexts.CATCH.text(), StudioTexts.STUDIO.text()));
    }

    /** The first corner caught, if any. */
    static @Nullable BlockPos first() {
        return first;
    }

    /** The second corner caught, if any. */
    static @Nullable BlockPos second() {
        return second;
    }

    @Override
    TextKey tab() {
        return StudioTool.STRUCTURES.title();
    }

    @Override
    List<String> crumbs() {
        return List.of(words(this.tab()), this.name.edit().isBlank() ? words(ToolTexts.A_NEW_ONE) : this.name.edit());
    }

    @Override
    List<String> lines() {
        if (MultiblockMaker.making()) {
            return this.maker.lines();
        }
        return this.counted == null ? List.of() : List.of(String.valueOf(this.counted.getInt(StudioKeys.BLOCKS)));
    }

    @Override
    void layout(final StudioLayout.Regions laid) {
        super.layout(laid);
        final boolean making = MultiblockMaker.making();
        this.maker.show(making);
        for (final UiComponent catching : List.of(this.corner1, this.corner2, this.name, this.save, this.project,
                this.make)) {
            catching.setVisible(!making);
        }
        final StudioLayout.Rect left = this.columns().get(0);
        if (making) {
            this.maker.layout(left);
            return;
        }
        final int x = left.x() + StudioDraw.INSET;
        final int wide = left.width() - 2 * StudioDraw.INSET;
        place(this.corner1, new StudioLayout.Rect(x, left.y() + 19, 70, 16));
        place(this.corner2, new StudioLayout.Rect(x, left.y() + 41, 70, 16));
        place(this.name, new StudioLayout.Rect(x, left.y() + 134, wide, 16));
        final int half = wide / 2 - 2;
        final int makeY;
        if (fits(half, ToolTexts.SAVE_TO_MOD, ToolTexts.SHOW_PROJECTOR, ToolTexts.HIDE_PROJECTOR)) {
            place(this.save, new StudioLayout.Rect(x, left.y() + 156, half, 18));
            place(this.project, new StudioLayout.Rect(x + half + 4, left.y() + 156, wide - half - 4, 18));
            makeY = left.y() + 178;
        } else {
            place(this.save, new StudioLayout.Rect(x, left.y() + 156, wide, 18));
            place(this.project, new StudioLayout.Rect(x, left.y() + 178, wide, 18));
            makeY = left.y() + 200;
        }
        place(this.make, new StudioLayout.Rect(x, makeY, wide, 18));
        this.writtenY = makeY + 24;
    }

    @Override
    void shown() {
        if (this.name.edit().isBlank()) {
            this.studio.lookedAt().ifPresent(at -> {
                final Minecraft game = Minecraft.getInstance();
                if (game.level != null) {
                    final ResourceLocation block = BuiltInRegistries.BLOCK.getKey(game.level.getBlockState(at)
                            .getBlock());
                    this.name.set(block.getNamespace() + ":" + STRUCTURE);
                }
            });
        }
        this.read();
    }

    @Override
    void render(final GuiGraphics g, final int mouseX, final int mouseY, final List<Component> tip) {
        final StudioPalette.Colours colours = StudioPalette.get();
        final List<StudioLayout.Rect> columns = this.columns();
        final StudioLayout.Rect left = columns.get(0);
        if (MultiblockMaker.making()) {
            this.maker.render(g, left, columns.get(1), this.viewArea(), mouseX, mouseY);
            return;
        }
        StudioDraw.panel(g, font(), left, words(ToolTexts.CATCH));
        final int x = this.corner1.x() + this.corner1.width() + 8;
        StudioDraw.text(g, font(), placeOf(first), x, this.corner1.y() + 4, left.right() - x - 20, colours.label());
        StudioDraw.text(g, font(), placeOf(second), x, this.corner2.y() + 4, left.right() - x - 20,
                colours.label());
        if (first != null) {
            StudioMarks.draw(g, font(), left.right() - StudioDraw.INSET - StudioMarks.WIDTH, this.corner1.y() + 3,
                    StudioSheet.Mark.FINE);
        }
        if (second != null) {
            StudioMarks.draw(g, font(), left.right() - StudioDraw.INSET - StudioMarks.WIDTH, this.corner2.y() + 3,
                    StudioSheet.Mark.FINE);
        }
        StudioDraw.small(g, font(), words(ToolTexts.CORNER_HINT), left.x() + StudioDraw.INSET, left.y() + 64,
                left.width() - 2 * StudioDraw.INSET, colours.dim());
        if (this.counted != null) {
            final int[] size = this.counted.getIntArray(StudioKeys.SIZE);
            if (size.length == 3) {
                StudioDraw.row(g, font(), left, left.y() + 80, words(ToolTexts.SIZE), GameText.resolve(
                        ToolTexts.SIZE_VALUE.with(size[0], size[1], size[2])), colours.label());
            }
            StudioDraw.row(g, font(), left, left.y() + 91, words(ToolTexts.BLOCKS), GameText.resolve(
                    ToolTexts.BLOCKS_VALUE.with(this.counted.getInt(StudioKeys.BLOCKS))), colours.label());
            StudioDraw.row(g, font(), left, left.y() + 102, words(ToolTexts.BLOCK_ENTITIES), GameText.resolve(
                    ToolTexts.BLOCK_ENTITIES_VALUE.with(this.counted.getInt(StudioKeys.ENTITIES))), colours.label());
        }
        StudioDraw.small(g, font(), words(ToolTexts.NAME), left.x() + StudioDraw.INSET, left.y() + 124,
                left.width(), colours.text());
        StudioDraw.small(g, font(), this.written, left.x() + StudioDraw.INSET, this.writtenY,
                left.width() - 2 * StudioDraw.INSET, colours.dim());
        final StudioLayout.Rect right = columns.get(1);
        StudioDraw.panel(g, font(), right, words(ToolTexts.AS_PROJECTOR));
        final StudioLayout.Rect view = this.viewArea();
        g.fillGradient(view.x(), view.y(), view.right(), view.bottom(), colours.skyTop(), colours.skyLow());
        this.scene.draw(g, view);
        Draw.outline(g, view.x(), view.y(), view.width(), view.height(), colours.frame());
        final boolean both = first != null && second != null;
        final boolean named = ResourceLocation.tryParse(this.name.edit().trim()) != null;
        this.save.setEnabled(both && named);
        this.project.setEnabled(both);
        this.make.setEnabled(both && named);
    }

    @Override
    boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (MultiblockMaker.making()) {
            if (this.viewArea().contains(mouseX, mouseY)) {
                // The page of the viewers takes its own buttons' clicks; a drag from anywhere on it turns its view.
                this.turning = true;
                this.maker.mouseClicked(mouseX, mouseY, button);
                return true;
            }
            return false;
        }
        if (this.viewArea().contains(mouseX, mouseY)) {
            this.turning = true;
            return true;
        }
        return false;
    }

    @Override
    boolean mouseDragged(final double mouseX, final double mouseY, final double dragX, final double dragY) {
        if (this.turning && MultiblockMaker.making()) {
            this.maker.mouseDragged(mouseX, mouseY, dragX, dragY);
            return true;
        }
        if (this.turning) {
            this.scene.turn(dragX, dragY);
            return true;
        }
        return false;
    }

    @Override
    void mouseReleased() {
        this.turning = false;
    }

    @Override
    boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollY) {
        if (this.viewArea().contains(mouseX, mouseY)) {
            this.scene.zoom(scrollY);
            return true;
        }
        return false;
    }

    @Override
    void closed() {
        this.scene.close();
    }

    private List<StudioLayout.Rect> columns() {
        return StudioLayout.columns(this.regions.body(), 0.47);
    }

    private StudioLayout.Rect viewArea() {
        final StudioLayout.Rect right = this.columns().get(1);
        return new StudioLayout.Rect(right.x() + 2, right.y() + StudioDraw.HEAD, right.width() - 4,
                right.height() - StudioDraw.HEAD - 2);
    }

    private void corner(final boolean isFirst) {
        final Optional<BlockPos> at = this.studio.lookedAt();
        if (at.isEmpty()) {
            this.studio.say(words(ToolTexts.NO_BLOCK));
            return;
        }
        if (isFirst) {
            first = at.get();
        } else {
            second = at.get();
        }
        this.read();
    }

    /* The blocks between the corners: shown on the page, and counted by the server as a structure file holds them. */
    private void read() {
        this.counted = null;
        final Minecraft game = Minecraft.getInstance();
        if (first == null || second == null || game.level == null) {
            this.scene.show(List.of());
            return;
        }
        this.scene.show(StudioScene.blocksOf(game.level, first, second));
        shown = shapeOf(game, first, second);
        this.ask(false);
    }

    private void save() {
        this.ask(true);
    }

    private void project() {
        projecting = !projecting;
        if (MultiblockMaker.making()) {
            // The pattern recorded is shown where it was built, as the projector will show it to be built.
            this.maker.preview().ifPresent(preview -> shown = preview);
        } else if (first != null && second != null && Minecraft.getInstance().level != null) {
            shown = shapeOf(Minecraft.getInstance(), first, second);
        }
    }

    /* Starts making the structure caught a multiblock, its controller the block the studio opened on. */
    private void startMaking() {
        if (this.maker.start(first, second)) {
            this.layout(this.regions);
        }
    }

    private void ask(final boolean saving) {
        if (first == null || second == null || Minecraft.getInstance().getConnection() == null) {
            return;
        }
        final CompoundTag args = new CompoundTag();
        args.putLong(StudioKeys.FROM, first.asLong());
        args.putLong(StudioKeys.TO, second.asLong());
        args.putBoolean(StudioKeys.SAVE, saving);
        args.putString(StudioKeys.NAME, this.name.edit().trim());
        StudioAnswers.ask(StudioServices.STRUCTURE, args, answered -> {
            if (answered.contains(StudioServices.REFUSED)) {
                this.studio.say(answered.getString(StudioServices.REFUSED));
                return;
            }
            this.counted = answered;
            if (saving) {
                this.written = answered.contains(StudioKeys.PATH) ? answered.getString(StudioKeys.PATH)
                        : words(ToolTexts.NOT_WRITTEN);
            }
        });
    }

    private static String placeOf(@Nullable final BlockPos at) {
        return at == null ? GameText.resolve(ToolTexts.NOT_SET) : GameText.resolve(ToolTexts.PLACE.with(at.getX(),
                at.getY(), at.getZ()));
    }

    /* The blocks between the corners as a multiblock's shape: each block its own place, its lowest corner first. */
    private static ShapePreview shapeOf(final Minecraft game, final BlockPos from, final BlockPos to) {
        final BlockPos min = new BlockPos(Math.min(from.getX(), to.getX()), Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ()));
        final BlockPos max = new BlockPos(Math.max(from.getX(), to.getX()), Math.max(from.getY(), to.getY()),
                Math.max(from.getZ(), to.getZ()));
        final List<ShapePreview.Ghost> ghosts = new ArrayList<>();
        for (final BlockPos at : BlockPos.betweenClosed(min, max)) {
            final BlockState state = game.level.getBlockState(at);
            if (!state.isAir()) {
                ghosts.add(new ShapePreview.Ghost(at.getX() - min.getX(), at.getY() - min.getY(),
                        at.getZ() - min.getZ(), at.getY() - min.getY(), List.of(BuiltInRegistries.BLOCK.getKey(
                                state.getBlock()).toString()), List.of(), false));
            }
        }
        return ShapePreview.of(ghosts);
    }
}
