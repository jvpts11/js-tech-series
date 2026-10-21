/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.developer.Pictures;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.developer.DevSources;
import dev.jstech.core.developer.studio.RecordedTest;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.ModList;
import net.neoforged.neoforgespi.language.IModFileInfo;
import net.neoforged.neoforgespi.language.IModInfo;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The studio's tests: what "Record a test" kept, step by step, and keeping it as a client test of its mod; and every
 * mod's reference pictures, each the same as the last run drew it or not, the one chosen shown as expected, as drawn
 * and with its differences marked, and taken as the new reference when the change was meant.
 */
final class TestsPage extends StudioPage {

    private final Button stop;
    private final Button keep;
    private final Button take;
    private final List<Picture> pictures = new ArrayList<>();
    private final List<ResourceLocation> textures = new ArrayList<>();
    private int chosen;
    private String kept = "";

    private static final String REFERENCES = "clienttest/references";
    private static final String DRAWN = "reference-pictures";
    private static final String RUNS = "clienttests";
    private static final String PNG = ".png";
    private static final String DIFFERENCES = "-differences";
    private static final int STEP_ROW = 16;
    private static final int PICTURE_ROW = 22;
    private static final int THUMB_WIDTH = 100;
    private static final int THUMB_HEIGHT = 80;

    TestsPage(final StudioScreen studio) {
        super(studio);
        this.stop = this.panel.add(new Button(() -> words(ToolTexts.STOP), TestRecorder::stop));
        this.keep = this.panel.add(new Button(() -> words(ToolTexts.KEEP), this::keep).setPrimary(true));
        this.take = this.panel.add(new Button(() -> words(ToolTexts.TAKE_NEW), this::take));
    }

    @Override
    TextKey tab() {
        return StudioTool.TESTS.title();
    }

    @Override
    List<String> lines() {
        return this.pictures.stream().map(Picture::name).toList();
    }

    @Override
    void layout(final StudioLayout.Regions laid) {
        super.layout(laid);
        final List<StudioLayout.Rect> columns = this.columns();
        final StudioLayout.Rect left = columns.get(0);
        final int x = left.x() + StudioDraw.INSET;
        final int wide = left.width() - 2 * StudioDraw.INSET;
        if (fits(wide - 94, ToolTexts.KEEP)) {
            place(this.stop, new StudioLayout.Rect(x, left.bottom() - 44, 90, 18));
            place(this.keep, new StudioLayout.Rect(x + 94, left.bottom() - 44, wide - 94, 18));
        } else {
            place(this.stop, new StudioLayout.Rect(x, left.bottom() - 66, wide, 18));
            place(this.keep, new StudioLayout.Rect(x, left.bottom() - 44, wide, 18));
        }
        final StudioLayout.Rect right = columns.get(1);
        place(this.take, new StudioLayout.Rect(right.x() + StudioDraw.INSET, right.y() + 168, 140, 16));
    }

    @Override
    void shown() {
        this.read();
    }

    @Override
    void render(final GuiGraphics g, final int mouseX, final int mouseY, final List<Component> tip) {
        final StudioPalette.Colours colours = StudioPalette.get();
        final List<StudioLayout.Rect> columns = this.columns();
        final StudioLayout.Rect left = columns.get(0);
        StudioDraw.panel(g, font(), left, words(ToolTexts.RECORDING_A_TEST));
        final List<RecordedTest.Step> steps = TestRecorder.steps();
        if (steps.isEmpty()) {
            StudioDraw.small(g, font(), words(ToolTexts.NOTHING_RECORDED), left.x() + StudioDraw.INSET,
                    left.y() + StudioDraw.HEAD + 4, left.width() - 2 * StudioDraw.INSET, colours.dim());
        }
        int y = left.y() + StudioDraw.HEAD + 4;
        for (int i = 0; i < steps.size() && y + STEP_ROW < this.stop.y() - 12; i++) {
            StudioDraw.text(g, font(), (i + 1) + ".", left.x() + StudioDraw.INSET, y, 18, colours.dim());
            StudioDraw.text(g, font(), said(steps.get(i)), left.x() + StudioDraw.INSET + 20, y,
                    left.width() - 2 * StudioDraw.INSET - 20, colours.label());
            y += STEP_ROW;
        }
        if (TestRecorder.recording()) {
            g.fill(left.x() + StudioDraw.INSET, y + 1, left.x() + StudioDraw.INSET + 6, y + 7, colours.recording());
            StudioDraw.small(g, font(), words(ToolTexts.RECORDING_NOW), left.x() + StudioDraw.INSET + 12, y,
                    left.width(), colours.recording());
        }
        StudioDraw.small(g, font(), this.kept, left.x() + StudioDraw.INSET, left.bottom() - 20,
                left.width() - 2 * StudioDraw.INSET, colours.dim());
        this.stop.setEnabled(TestRecorder.recording());
        this.keep.setEnabled(!steps.isEmpty() && !TestRecorder.recording() && TestRecorder.mod().isPresent());
        this.references(g, columns.get(1), colours);
    }

    @Override
    boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final StudioLayout.Rect right = this.columns().get(1);
        if (!right.contains(mouseX, mouseY) || button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return false;
        }
        final int row = (int) ((mouseY - right.y() - StudioDraw.HEAD - 2) / PICTURE_ROW);
        if (mouseY < right.y() + StudioDraw.HEAD + 2 * PICTURE_ROW + 4 && row >= 0 && row < this.pictures.size()) {
            this.choose(row);
            return true;
        }
        return false;
    }

    @Override
    void closed() {
        this.forget();
    }

    private List<StudioLayout.Rect> columns() {
        return StudioLayout.columns(this.regions.body(), 0.47);
    }

    private void references(final GuiGraphics g, final StudioLayout.Rect area, final StudioPalette.Colours colours) {
        StudioDraw.panel(g, font(), area, words(ToolTexts.REFERENCES));
        if (this.pictures.isEmpty()) {
            StudioDraw.small(g, font(), words(ToolTexts.NO_PICTURES), area.x() + StudioDraw.INSET,
                    area.y() + StudioDraw.HEAD + 4, area.width() - 2 * StudioDraw.INSET, colours.dim());
            this.take.setVisible(false);
            return;
        }
        final int first = Math.max(0, Math.min(this.chosen, this.pictures.size() - 2));
        for (int i = first; i < Math.min(this.pictures.size(), first + 2); i++) {
            final Picture picture = this.pictures.get(i);
            final int y = area.y() + StudioDraw.HEAD + 4 + (i - first) * PICTURE_ROW;
            if (i == this.chosen) {
                g.fill(area.x() + 3, y - 2, area.right() - 3, y + PICTURE_ROW - 3, colours.selected());
            }
            StudioMarks.draw(g, font(), area.x() + StudioDraw.INSET, y + 1, picture.drawn() == null
                    ? StudioSheet.Mark.FINE : StudioSheet.Mark.ERROR);
            final int x = area.x() + StudioDraw.INSET + StudioMarks.WIDTH + 5;
            StudioDraw.text(g, font(), picture.name(), x, y, area.right() - x - StudioDraw.INSET, colours.text());
            StudioDraw.small(g, font(), picture.reference() == null ? words(ToolTexts.NO_REFERENCE)
                    : picture.drawn() == null ? words(ToolTexts.SAME)
                    : GameText.resolve(ToolTexts.DIFFERS.with(picture.differing())), x, y + 10,
                    area.right() - x - StudioDraw.INSET, colours.dim());
        }
        final List<TextKey> labels = List.of(ToolTexts.EXPECTED, ToolTexts.NOW, ToolTexts.DIFFERENCE);
        final int wide = Math.min(THUMB_WIDTH, (area.width() - 2 * StudioDraw.INSET - 12) / 3);
        for (int i = 0; i < labels.size(); i++) {
            final int x = area.x() + StudioDraw.INSET + i * (wide + 6);
            StudioDraw.small(g, font(), words(labels.get(i)), x, area.y() + 70, wide, colours.label());
            g.fill(x, area.y() + 80, x + wide, area.y() + 80 + THUMB_HEIGHT, colours.pointed());
            if (i < this.textures.size() && this.textures.get(i) != null) {
                g.blit(this.textures.get(i), x, area.y() + 80, 0, 0, wide, THUMB_HEIGHT, wide, THUMB_HEIGHT);
            }
            Draw.outline(g, x, area.y() + 80, wide, THUMB_HEIGHT, colours.frame());
        }
        this.take.setVisible(this.pictures.get(this.chosen).drawn() != null);
    }

    private void keep() {
        final Optional<Path> file = TestRecorder.keep();
        this.kept = file.map(path -> GameText.resolve(ToolTexts.KEPT.with(path.getFileName().toString(),
                TestRecorder.mod().orElse("")))).orElseGet(() -> words(ToolTexts.NO_SOURCE));
    }

    /* Every mod's reference pictures, and what the last run of the client tests drew of each that differed. */
    private void read() {
        this.pictures.clear();
        final List<Path> drawnFolders = drawnFolders();
        for (final IModInfo mod : ModList.get().getMods()) {
            final IModFileInfo file = ModList.get().getModFileById(mod.getModId());
            if (file == null) {
                continue;
            }
            final Path folder = file.getFile().findResource(REFERENCES);
            if (!Files.isDirectory(folder)) {
                continue;
            }
            // Listed whole and picked by name: a mod's jar is a file system that cannot match a pattern of names.
            try (Stream<Path> found = Files.list(folder)) {
                for (final Path reference : found.filter(path -> path.getFileName().toString().endsWith(PNG))
                        .toList()) {
                    final String name = reference.getFileName().toString().replace(PNG, "");
                    this.pictures.add(pictureOf(mod.getModId(), name, reference, drawnFolders));
                }
            } catch (final IOException unread) {
                // A mod whose pictures cannot be listed has none to show.
            }
        }
        // A picture drawn with no reference yet, as a new test's first run leaves it.
        for (final Path folder : drawnFolders) {
            try (Stream<Path> mods = Files.list(folder)) {
                mods.filter(Files::isDirectory).forEach(mod -> {
                    try (Stream<Path> drawn = Files.list(mod)) {
                        drawn.filter(path -> path.toString().endsWith(PNG) && !path.toString().contains(DIFFERENCES))
                                .forEach(path -> {
                                    final String name = path.getFileName().toString().replace(PNG, "");
                                    if (this.pictures.stream().noneMatch(picture -> picture.name().equals(name))) {
                                        this.pictures.add(new Picture(mod.getFileName().toString(), name, null, path,
                                                null, 0));
                                    }
                                });
                    } catch (final IOException unread) {
                        // Nothing drawn to read there.
                    }
                });
            } catch (final IOException unread) {
                // Nothing drawn to read there.
            }
        }
        this.pictures.sort((a, b) -> Boolean.compare(a.drawn() == null, b.drawn() == null));
        this.choose(0);
    }

    private void choose(final int row) {
        this.chosen = row;
        this.forget();
        if (row >= this.pictures.size()) {
            return;
        }
        final Picture picture = this.pictures.get(row);
        this.textures.add(load(picture.reference(), 0));
        this.textures.add(load(picture.drawn(), 1));
        this.textures.add(load(picture.differences(), 2));
    }

    /* Takes the picture drawn as the mod's reference, in the mod's source, and lets the drawn one go. */
    private void take() {
        if (this.chosen >= this.pictures.size()) {
            return;
        }
        final Picture picture = this.pictures.get(this.chosen);
        final Optional<Path> source = DevSources.resourcesOf(picture.mod());
        if (picture.drawn() == null || source.isEmpty()) {
            this.studio.say(words(ToolTexts.NO_SOURCE));
            return;
        }
        try {
            final Path reference = source.get().resolve(REFERENCES).resolve(picture.name() + PNG);
            Files.createDirectories(reference.getParent());
            Files.copy(picture.drawn(), reference, StandardCopyOption.REPLACE_EXISTING);
            Files.deleteIfExists(picture.drawn());
            if (picture.differences() != null) {
                Files.deleteIfExists(picture.differences());
            }
            this.studio.say(GameText.resolve(ToolTexts.TAKEN.with(picture.name())));
        } catch (final IOException unwritten) {
            this.studio.say(String.valueOf(unwritten.getMessage()));
        }
        this.read();
    }

    private @Nullable ResourceLocation load(@Nullable final Path picture, final int place) {
        if (picture == null || !Files.isRegularFile(picture)) {
            return null;
        }
        try (InputStream in = Files.newInputStream(picture)) {
            final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JsCore.MODID, "studio/reference_"
                    + place);
            Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(NativeImage.read(in)));
            return id;
        } catch (final IOException unread) {
            return null;
        }
    }

    private void forget() {
        this.textures.stream().filter(id -> id != null).forEach(id -> Minecraft.getInstance().getTextureManager()
                .release(id));
        this.textures.clear();
    }

    /* Where the client tests leave what they drew: this game's folder, and those of the test runs under it. */
    private static List<Path> drawnFolders() {
        final Path game = Minecraft.getInstance().gameDirectory.toPath();
        final List<Path> out = new ArrayList<>();
        if (Files.isDirectory(game.resolve(DRAWN))) {
            out.add(game.resolve(DRAWN));
        }
        final Path runs = game.resolve(RUNS);
        if (Files.isDirectory(runs)) {
            try (Stream<Path> shards = Files.list(runs)) {
                shards.map(shard -> shard.resolve(DRAWN)).filter(Files::isDirectory).forEach(out::add);
            } catch (final IOException unread) {
                // No runs to read.
            }
        }
        return out;
    }

    private static Picture pictureOf(final String mod, final String name, final Path reference,
                                     final List<Path> drawnFolders) {
        for (final Path folder : drawnFolders) {
            final Path drawn = folder.resolve(mod).resolve(name + PNG);
            if (Files.isRegularFile(drawn)) {
                final Path differences = folder.resolve(mod).resolve(name + DIFFERENCES + PNG);
                return new Picture(mod, name, reference, drawn, Files.isRegularFile(differences) ? differences : null,
                        differing(differences));
            }
        }
        return new Picture(mod, name, reference, null, null, 0);
    }

    /* How many pixels the test kit marked as differing. */
    private static int differing(final Path differences) {
        if (!Files.isRegularFile(differences)) {
            return 0;
        }
        try (InputStream in = Files.newInputStream(differences); NativeImage image = NativeImage.read(in)) {
            int off = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    off += image.getPixelRGBA(x, y) == Pictures.OFF ? 1 : 0;
                }
            }
            return off;
        } catch (final IOException unread) {
            return 0;
        }
    }

    /* A step as the player reads it. */
    private static String said(final RecordedTest.Step step) {
        return switch (step) {
            case RecordedTest.Use use -> GameText.resolve(ToolTexts.STEP_USE.with(use.block()));
            case RecordedTest.Click click -> GameText.resolve(ToolTexts.STEP_CLICK.with(click.component()));
            case RecordedTest.ClickAt at -> GameText.resolve(ToolTexts.STEP_CLICK_AT.with(at.dx(), at.dy()));
            case RecordedTest.Type type -> GameText.resolve(ToolTexts.STEP_TYPE.with(type.text()));
            case RecordedTest.Key key -> GameText.resolve(ToolTexts.STEP_KEY.with(key.key()));
            case RecordedTest.Wait wait -> GameText.resolve(ToolTexts.STEP_WAIT.with(wait.ticks()));
            case RecordedTest.Picture picture -> GameText.resolve(ToolTexts.STEP_PICTURE.with(picture.name()));
            case RecordedTest.Close close -> GameText.resolve(ToolTexts.STEP_CLOSE);
        };
    }

    /**
     * A reference picture and what the last run drew of it.
     *
     * @param mod         the mod whose it is
     * @param name        its name
     * @param reference   the mod's reference, or none yet
     * @param drawn       what the last run drew, when it differed or had no reference
     * @param differences the picture of where it differs
     * @param differing   how many pixels differ
     */
    private record Picture(String mod, String name, @Nullable Path reference, @Nullable Path drawn,
                           @Nullable Path differences, int differing) {
    }
}
