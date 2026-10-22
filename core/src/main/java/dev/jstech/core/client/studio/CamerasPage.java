/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.studio;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;
import dev.jstech.core.camera.CameraPath;
import dev.jstech.core.camera.CameraPathCodecs;
import dev.jstech.core.client.camera.CameraPathsClient;
import dev.jstech.core.client.camera.CameraViewsClient;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.developer.DevSources;
import dev.jstech.core.gui.layout.CameraPathsLayout;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.model.Easing;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * The camera paths: every path of a file or declared down the left, and a new one written in place; the keyframes of
 * the one chosen in a table, each changed where it stands, kept by flying where the camera goes and pressing a key, or
 * added where the player stands; played as a scene on the player's game before it is saved, and saved to its file in
 * the mod; and the path seen from above, its keyframes numbered along the way the camera takes, round the place it is
 * played from.
 */
final class CamerasPage extends StudioPage {

    private final Button fly;
    private final Button add;
    private final Button play;
    private final Button save;
    private final TextField name = this.panel.add(new TextField(LONGEST));
    private CameraPathsLayout.Laid laid = CameraPathsLayout.of(this.regions.body(), 0, List.of(0, 0, 0, 0));
    private List<ResourceLocation> paths = List.of();
    private int firstPath;
    private int firstRow;
    private boolean writingNew;

    private static final int LONGEST = 128;
    private static final int TIME = 0;
    private static final int WHERE = 1;
    private static final int LOOKING = 2;
    private static final int FOV = 3;
    private static final int ROLL = 4;
    private static final int EASING = 5;
    private static final double TICKS_A_SECOND = 20.0;
    /** How far a notch of the wheel moves a time, in ticks, a field of view and a roll, in degrees. */
    private static final int TIME_STEP = 10;
    private static final double FOV_STEP = 5.0;
    private static final float ROLL_STEP = 1.0F;
    /** The field of view a keyframe takes as it is first given one, the game's own. */
    private static final double FIRST_FOV = 70.0;
    private static final double TENTHS = 10.0;
    /** The most dots drawn along the way, and the fewest ticks between two. */
    private static final int MOST_DOTS = 400;
    private static final int DOT_TICKS = 4;
    private static final int POINT = 3;
    private static final int POINT_PICK = 5;
    private static final int LEAST_ORIGIN = 4;
    private static final String IN_OUT = "easeInOut";
    private static final String IN = "easeIn";
    private static final String OUT = "easeOut";
    private static final String GENTLEST = "Sine";
    private static final String FOLDER = "jstech/camera_paths/";
    private static final String JSON = ".json";
    private static final String CELLS = " | ";
    private static final String BETWEEN = ", ";
    private static final Gson PRETTY = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    CamerasPage(final StudioScreen studio) {
        super(studio);
        this.fly = this.panel.add(new Button(() -> words(CameraPathRecorder.recording()
                ? CameraPathTexts.STOP_RECORDING : CameraPathTexts.FLY_AND_RECORD), this::fly));
        this.add = this.panel.add(new Button(() -> words(CameraPathTexts.ADD_HERE), this::addHere));
        this.play = this.panel.add(new Button(() -> words(CameraPathTexts.PLAY), this::play));
        this.save = this.panel.add(new Button(() -> words(CameraPathTexts.SAVE_TO_FILE), this::save)
                .setPrimary(true));
        this.name.setPlaceholder(words(CameraPathTexts.NAME_HINT));
        this.name.setOnEdit(() -> CameraPathRecorder.write(this.name.edit()));
        this.name.setVisible(false);
    }

    @Override
    TextKey tab() {
        return StudioTool.CAMERAS.title();
    }

    @Override
    List<String> crumbs() {
        return List.of(words(this.tab()), this.named().map(ResourceLocation::toString).orElseGet(() ->
                this.name.edit().isBlank() ? words(CameraPathTexts.A_NEW_ONE) : this.name.edit().trim()));
    }

    @Override
    List<String> lines() {
        final List<String> lines = new ArrayList<>();
        for (final CameraPath.Keyframe keyframe : CameraPathRecorder.keyframes()) {
            lines.add(String.join(CELLS, cells(keyframe)));
        }
        return lines;
    }

    @Override
    void layout(final StudioLayout.Regions laidOut) {
        super.layout(laidOut);
        this.relayout();
    }

    @Override
    void shown() {
        this.paths = this.known();
        if (!CameraPathRecorder.placed()) {
            if (this.paths.isEmpty()) {
                this.chooseNew();
            } else {
                this.choosePath(this.paths.get(0));
            }
        }
        this.writingNew = CameraPathRecorder.editing().isEmpty();
        this.name.set(CameraPathRecorder.written());
        this.relayout();
    }

    @Override
    boolean choose(final String what) {
        if (what.equals(words(CameraPathTexts.A_NEW_PATH))) {
            this.chooseNew();
            return true;
        }
        final ResourceLocation id = ResourceLocation.tryParse(what);
        if (id != null && this.paths.contains(id)) {
            this.choosePath(id);
            return true;
        }
        return false;
    }

    @Override
    void render(final GuiGraphics g, final int mouseX, final int mouseY, final List<Component> tip) {
        this.relayout();
        final StudioPalette.Colours colours = StudioPalette.get();
        StudioDraw.panel(g, font(), this.laid.list(), words(CameraPathTexts.PATHS));
        StudioDraw.panel(g, font(), this.laid.keyframes(), words(CameraPathTexts.KEYFRAMES));
        StudioDraw.panel(g, font(), this.laid.map(), words(CameraPathTexts.SEEN_FROM_ABOVE));
        this.renderPaths(g, mouseX, mouseY, colours);
        this.renderKeyframes(g, mouseX, mouseY, colours, tip);
        this.renderMap(g, colours);
        final Optional<CameraPath> path = CameraPathRecorder.path();
        this.play.setEnabled(path.isPresent());
        this.save.setEnabled(path.isPresent() && this.savedAs().isPresent());
    }

    @Override
    boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        for (int i = 0; i < this.laid.pathRows(); i++) {
            if (CameraPathsLayout.pathRow(this.laid, i).contains(mouseX, mouseY)) {
                final int index = this.firstPath + i;
                if (index < this.paths.size()) {
                    this.choosePath(this.paths.get(index));
                } else if (index == this.paths.size()) {
                    this.chooseNew();
                }
                return true;
            }
        }
        final List<CameraPath.Keyframe> keyframes = CameraPathRecorder.keyframes();
        for (int i = 0; i < this.laid.rows(); i++) {
            final int index = this.firstRow + i;
            if (index < keyframes.size() && CameraPathsLayout.keyframeRow(this.laid, i).contains(mouseX, mouseY)) {
                CameraPathRecorder.choose(index);
                if (CameraPathsLayout.cell(this.laid, i, EASING).contains(mouseX, mouseY)) {
                    CameraPathRecorder.set(index, withEasing(keyframes.get(index), Screen.hasShiftDown() ? -1 : 1));
                }
                return true;
            }
        }
        if (this.laid.mapArea().contains(mouseX, mouseY)) {
            final CameraPathsLayout.Fit fit = this.fit(keyframes);
            for (int i = 0; i < keyframes.size(); i++) {
                final double[] at = fit.at(keyframes.get(i).x(), keyframes.get(i).z());
                if (Math.abs(at[0] - mouseX) <= POINT_PICK && Math.abs(at[1] - mouseY) <= POINT_PICK) {
                    CameraPathRecorder.choose(i);
                    this.showChosen();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollY) {
        final int notch = scrollY > 0 ? 1 : -1;
        if (this.laid.list().contains(mouseX, mouseY)) {
            this.firstPath = Math.clamp(this.firstPath - notch, 0, Math.max(0, this.paths.size() + 1
                    - this.laid.pathRows()));
            return true;
        }
        final List<CameraPath.Keyframe> keyframes = CameraPathRecorder.keyframes();
        for (int i = 0; i < this.laid.rows(); i++) {
            final int index = this.firstRow + i;
            if (index >= keyframes.size()) {
                break;
            }
            for (final int column : List.of(TIME, FOV, ROLL)) {
                if (CameraPathsLayout.cell(this.laid, i, column).contains(mouseX, mouseY)) {
                    CameraPathRecorder.choose(index);
                    CameraPathRecorder.set(index, changed(keyframes, index, column, notch));
                    return true;
                }
            }
        }
        if (this.laid.keyframes().contains(mouseX, mouseY)) {
            this.firstRow = Math.clamp(this.firstRow - notch, 0, Math.max(0, keyframes.size() - this.laid.rows()));
            return true;
        }
        return false;
    }

    @Override
    boolean keyPressed(final int key) {
        final int chosen = CameraPathRecorder.chosen();
        if (key == GLFW.GLFW_KEY_DELETE || key == GLFW.GLFW_KEY_BACKSPACE) {
            CameraPathRecorder.remove(chosen);
            this.showChosen();
            return chosen >= 0;
        }
        if (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN) {
            final int next = chosen + (key == GLFW.GLFW_KEY_UP ? -1 : 1);
            if (next >= 0 && next < CameraPathRecorder.keyframes().size()) {
                CameraPathRecorder.choose(next);
                this.showChosen();
            }
            return true;
        }
        return false;
    }

    /* Lays the page out for the keyframes it has now, and puts its buttons and the name where they stand. */
    private void relayout() {
        if (font() == null) {
            // The studio is not laid out yet; it lays its pages out as it opens.
            return;
        }
        final int longest = Math.max(font().width(words(CameraPathTexts.FLY_AND_RECORD)),
                font().width(words(CameraPathTexts.STOP_RECORDING)));
        this.laid = CameraPathsLayout.of(this.regions.body(), CameraPathRecorder.keyframes().size(), List.of(longest,
                font().width(words(CameraPathTexts.ADD_HERE)), font().width(words(CameraPathTexts.PLAY)),
                font().width(words(CameraPathTexts.SAVE_TO_FILE))));
        final List<Button> buttons = List.of(this.fly, this.add, this.play, this.save);
        for (int i = 0; i < buttons.size(); i++) {
            place(buttons.get(i), this.laid.buttons().get(i));
        }
        final int newRow = this.paths.size() - this.firstPath;
        final boolean shown = this.writingNew && newRow >= 0 && newRow < this.laid.pathRows();
        this.name.setVisible(shown);
        if (shown) {
            final StudioLayout.Rect row = CameraPathsLayout.pathRow(this.laid, newRow);
            place(this.name, new StudioLayout.Rect(row.x() + 2, row.y() + 1, row.width() - 4, row.height() - 2));
        }
    }

    private void renderPaths(final GuiGraphics g, final int mouseX, final int mouseY,
                             final StudioPalette.Colours colours) {
        final Optional<ResourceLocation> editing = CameraPathRecorder.editing();
        for (int i = 0; i < this.laid.pathRows(); i++) {
            final int index = this.firstPath + i;
            if (index > this.paths.size()) {
                break;
            }
            final StudioLayout.Rect row = CameraPathsLayout.pathRow(this.laid, i);
            final boolean isNew = index == this.paths.size();
            final boolean chosen = isNew ? this.writingNew : !this.writingNew && editing.isPresent()
                    && editing.get().equals(this.paths.get(index));
            if (chosen) {
                g.fill(row.x(), row.y(), row.right(), row.bottom(), colours.selected());
                Draw.outline(g, row.x(), row.y(), row.width(), row.height(), colours.outline());
            } else if (row.contains(mouseX, mouseY)) {
                g.fill(row.x(), row.y(), row.right(), row.bottom(), colours.pointed());
            }
            if (isNew && this.writingNew) {
                continue;
            }
            final String title = isNew ? words(CameraPathTexts.A_NEW_PATH) : this.paths.get(index).getPath();
            final String under = isNew ? words(CameraPathTexts.WRITTEN_HERE) : this.paths.get(index).getNamespace();
            StudioDraw.text(g, font(), title, row.x() + 4, row.y() + 1, row.width() - 8,
                    chosen ? colours.text() : colours.label());
            StudioDraw.small(g, font(), under, row.x() + 4, row.y() + 10, row.width() - 8, colours.dim());
        }
    }

    private void renderKeyframes(final GuiGraphics g, final int mouseX, final int mouseY,
                                 final StudioPalette.Colours colours, final List<Component> tip) {
        final List<TextKey> heads = List.of(CameraPathTexts.TIME, CameraPathTexts.WHERE, CameraPathTexts.LOOKING,
                CameraPathTexts.FOV, CameraPathTexts.ROLL, CameraPathTexts.EASING);
        for (int column = 0; column < heads.size(); column++) {
            final StudioLayout.Rect cell = CameraPathsLayout.cell(this.laid, 0, column);
            StudioDraw.small(g, font(), words(heads.get(column)), cell.x(), this.laid.headerY(), cell.width() - 2,
                    colours.label());
        }
        final List<CameraPath.Keyframe> keyframes = CameraPathRecorder.keyframes();
        final int inset = CameraPathsLayout.INSET;
        if (keyframes.isEmpty()) {
            StudioDraw.small(g, font(), words(CameraPathTexts.NONE_YET), this.laid.cells().get(0),
                    this.laid.firstRowY(), this.laid.keyframes().width() - 2 * inset, colours.dim());
        }
        for (int i = 0; i < this.laid.rows(); i++) {
            final int index = this.firstRow + i;
            if (index >= keyframes.size()) {
                break;
            }
            final StudioLayout.Rect row = CameraPathsLayout.keyframeRow(this.laid, i);
            final boolean chosen = index == CameraPathRecorder.chosen();
            if (chosen) {
                g.fill(row.x(), row.y(), row.right(), row.bottom(), colours.selected());
                Draw.outline(g, row.x(), row.y(), row.width(), row.height(), colours.outline());
            } else if (row.contains(mouseX, mouseY)) {
                g.fill(row.x(), row.y(), row.right(), row.bottom(), colours.pointed());
            }
            final List<String> cells = cells(keyframes.get(index));
            for (int column = 0; column < cells.size(); column++) {
                final StudioLayout.Rect cell = CameraPathsLayout.cell(this.laid, i, column);
                StudioDraw.small(g, font(), cells.get(column), cell.x(), cell.y() + 2, cell.width() - 2,
                        chosen ? colours.text() : colours.label());
                if (cell.contains(mouseX, mouseY) && column != WHERE && column != LOOKING) {
                    tip.add(GameText.component(CameraPathTexts.EDIT_TIP));
                    if (column == EASING) {
                        tip.add(Component.literal(keyframes.get(index).easing().serializedName()));
                    }
                }
            }
        }
        StudioDraw.smallWrapped(g, font(), words(CameraPathTexts.HINT), this.laid.keyframes().x() + inset,
                this.laid.hintY(), this.laid.keyframes().width() - 2 * inset, colours.dim());
        StudioDraw.small(g, font(), this.savedAs().map(CamerasPage::file).orElse(""), this.laid.keyframes().x()
                + inset, this.laid.fileY(), this.laid.keyframes().width() - 2 * inset, colours.dim());
    }

    /* The path seen from above: the place it is played from, the way the camera takes, and its keyframes numbered. */
    private void renderMap(final GuiGraphics g, final StudioPalette.Colours colours) {
        final StudioLayout.Rect area = this.laid.mapArea();
        g.fill(area.x(), area.y(), area.right(), area.bottom(), colours.mapGround());
        final List<CameraPath.Keyframe> keyframes = CameraPathRecorder.keyframes();
        final CameraPathsLayout.Fit fit = this.fit(keyframes);
        final double[] origin = fit.at(0, 0);
        final int side = Math.max(LEAST_ORIGIN, (int) Math.round(fit.scale()));
        final int ox = (int) Math.round(origin[0]) - side / 2;
        final int oy = (int) Math.round(origin[1]) - side / 2;
        g.fill(ox, oy, ox + side, oy + side, colours.origin());
        final String label = words(CameraPathTexts.ORIGIN);
        final int labelWidth = Math.round(font().width(label) * StudioDraw.SMALL);
        StudioDraw.small(g, font(), label, ox + side / 2 - labelWidth / 2, oy + side + 2, labelWidth + 1,
                colours.dim());
        CameraPathRecorder.path().ifPresent(path -> {
            final int step = Math.max(DOT_TICKS, path.length() / MOST_DOTS);
            for (int t = 0; t <= path.length(); t += step) {
                final CameraPath.Sample sample = path.at(t);
                final double[] at = fit.at(sample.x(), sample.z());
                final int x = (int) Math.round(at[0]);
                final int y = (int) Math.round(at[1]);
                g.fill(x, y, x + 1, y + 1, colours.pathPoint());
            }
        });
        for (int i = 0; i < keyframes.size(); i++) {
            final double[] at = fit.at(keyframes.get(i).x(), keyframes.get(i).z());
            final int x = (int) Math.round(at[0]);
            final int y = (int) Math.round(at[1]);
            final int colour = i == CameraPathRecorder.chosen() ? colours.outline() : colours.pathPoint();
            g.fill(x - POINT / 2, y - POINT / 2, x - POINT / 2 + POINT, y - POINT / 2 + POINT, colour);
            Draw.textScaled(g, font(), String.valueOf(i + 1), x + POINT, y - POINT, colour, StudioDraw.SMALL);
        }
    }

    private CameraPathsLayout.Fit fit(final List<CameraPath.Keyframe> keyframes) {
        final List<double[]> places = new ArrayList<>();
        for (final CameraPath.Keyframe keyframe : keyframes) {
            places.add(new double[] {keyframe.x(), keyframe.z()});
        }
        return CameraPathsLayout.fit(this.laid.mapArea(), places);
    }

    private List<ResourceLocation> known() {
        final List<ResourceLocation> known = new ArrayList<>(CameraPathsClient.known());
        CameraPathRecorder.editing().filter(id -> !known.contains(id)).ifPresent(known::add);
        return List.copyOf(known);
    }

    private void choosePath(final ResourceLocation id) {
        CameraPathRecorder.edit(id, CameraPathsClient.get(id).orElse(null), this.origin());
        this.writingNew = false;
        this.firstRow = 0;
        this.panel.focus(null);
        this.relayout();
    }

    private void chooseNew() {
        CameraPathRecorder.edit(null, null, this.origin());
        this.writingNew = true;
        this.firstRow = 0;
        this.firstPath = Math.max(0, this.paths.size() + 1 - this.laid.pathRows());
        this.relayout();
        this.panel.focus(this.name);
    }

    /* Where a path is played from on this page: where it was, or on the block looked at, or where the player stands. */
    private Vec3 origin() {
        if (CameraPathRecorder.placed()) {
            return CameraPathRecorder.origin();
        }
        final Minecraft game = Minecraft.getInstance();
        return this.studio.lookedAt().map(at -> Vec3.atBottomCenterOf(at.above())).orElseGet(() ->
                game.player == null ? Vec3.ZERO : Vec3.atBottomCenterOf(game.player.blockPosition()));
    }

    /* The id the path is saved as: its own, or the name written for a new one. */
    private Optional<ResourceLocation> savedAs() {
        final Optional<ResourceLocation> named = this.named();
        return named.isPresent() ? named : Optional.ofNullable(ResourceLocation.tryParse(this.name.edit().trim()))
                .filter(id -> !id.getPath().isEmpty() && this.name.edit().contains(":"));
    }

    private Optional<ResourceLocation> named() {
        return this.writingNew ? Optional.empty() : CameraPathRecorder.editing();
    }

    private void fly() {
        if (CameraPathRecorder.recording()) {
            CameraPathRecorder.stop();
        } else {
            CameraPathRecorder.start();
        }
    }

    private void addHere() {
        CameraPathRecorder.keepHere();
        this.showChosen();
    }

    private void play() {
        CameraPathRecorder.path().ifPresent(path -> {
            final Vec3 from = CameraPathRecorder.origin();
            Minecraft.getInstance().setScreen(null);
            CameraViewsClient.rehearse(path, from, 0.0F);
        });
    }

    /* Writes the path's file in its place in the mod's source, and has the game play it as written from now. */
    private void save() {
        final Optional<ResourceLocation> as = this.savedAs();
        final Optional<CameraPath> path = CameraPathRecorder.path();
        if (as.isEmpty()) {
            this.studio.say(words(CameraPathTexts.NAME_FIRST));
            return;
        }
        if (path.isEmpty()) {
            this.studio.say(words(CameraPathTexts.TWO_OR_MORE));
            return;
        }
        final ResourceLocation id = as.get();
        final Optional<Path> source = DevSources.resourcesOf(id.getNamespace());
        if (source.isEmpty()) {
            this.studio.say(words(ToolTexts.NO_SOURCE));
            return;
        }
        final StudioSheet.File file = StudioFiles.asset(ResourceLocation.fromNamespaceAndPath(id.getNamespace(),
                FOLDER + id.getPath() + JSON));
        final Path written = StudioFiles.sourceOf(file).orElse(source.get().resolve(file.path()));
        final JsonElement json = CameraPathCodecs.PATH.encodeStart(JsonOps.INSTANCE, path.get()).getOrThrow();
        try {
            Files.createDirectories(written.getParent());
            Files.writeString(written, PRETTY.toJson(json) + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (final IOException unwritten) {
            this.studio.say(String.valueOf(unwritten.getMessage()));
            return;
        }
        CameraPathRecorder.named(id);
        CameraPathsClient.hold(id, path.get());
        this.writingNew = false;
        this.panel.focus(null);
        this.paths = this.known();
        this.studio.say(GameText.resolve(ToolTexts.SAVED_TO.with(written.toString())));
    }

    /* Scrolls the table so the keyframe chosen is among its rows. */
    private void showChosen() {
        this.relayout();
        final int chosen = CameraPathRecorder.chosen();
        if (chosen < this.firstRow) {
            this.firstRow = Math.max(0, chosen);
        } else if (chosen >= this.firstRow + this.laid.rows()) {
            this.firstRow = chosen - this.laid.rows() + 1;
        }
    }

    /* Where the file of a path stands among its mod's resources. */
    private static String file(final ResourceLocation id) {
        return StudioFiles.asset(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), FOLDER + id.getPath()
                + JSON)).path();
    }

    /* What a keyframe's row says, a cell each column. */
    private static List<String> cells(final CameraPath.Keyframe keyframe) {
        return List.of(GameText.resolve(CameraPathTexts.SECONDS.with(number(keyframe.time() / TICKS_A_SECOND))),
                number(keyframe.x()) + BETWEEN + number(keyframe.y()) + BETWEEN + number(keyframe.z()),
                number(keyframe.yaw()) + BETWEEN + number(keyframe.pitch()),
                keyframe.fov() == 0 ? words(CameraPathTexts.OWN) : number(keyframe.fov()), number(keyframe.roll()),
                easing(keyframe.easing()));
    }

    /* An easing as the table names it: the gentlest curve by its way alone, the others with their curve's name. */
    static String easing(final Easing easing) {
        final String written = easing.serializedName();
        if (written.startsWith(IN_OUT)) {
            return named(CameraPathTexts.EASE_IN_OUT, CameraPathTexts.EASE_IN_OUT_OF, written.substring(
                    IN_OUT.length()));
        }
        if (written.startsWith(IN)) {
            return named(CameraPathTexts.EASE_IN, CameraPathTexts.EASE_IN_OF, written.substring(IN.length()));
        }
        if (written.startsWith(OUT)) {
            return named(CameraPathTexts.EASE_OUT, CameraPathTexts.EASE_OUT_OF, written.substring(OUT.length()));
        }
        return written;
    }

    private static String named(final TextKey gentlest, final TextKey other, final String curve) {
        return curve.equals(GENTLEST) ? words(gentlest) : GameText.resolve(other.with(curve.toLowerCase(Locale.ROOT)));
    }

    /* A number to a tenth, whole numbers with no point. */
    private static String number(final double value) {
        final double tenths = Math.round(value * TENTHS) / TENTHS;
        return tenths == Math.rint(tenths) ? Long.toString(Math.round(tenths)) : Double.toString(tenths);
    }

    /* The keyframe {@code index} with {@code column} turned a notch, its time kept between its neighbours'. */
    private static CameraPath.Keyframe changed(final List<CameraPath.Keyframe> keyframes, final int index,
                                               final int column, final int notch) {
        final CameraPath.Keyframe k = keyframes.get(index);
        return switch (column) {
            case TIME -> {
                final int least = index == 0 ? 0 : keyframes.get(index - 1).time() + 1;
                final int most = index + 1 < keyframes.size() ? keyframes.get(index + 1).time() - 1
                        : Integer.MAX_VALUE;
                yield new CameraPath.Keyframe(Math.clamp(k.time() + (long) notch * TIME_STEP, least, most), k.x(),
                        k.y(), k.z(), k.yaw(), k.pitch(), k.roll(), k.fov(), k.easing());
            }
            case FOV -> {
                final double fov = k.fov() == 0 && notch > 0 ? FIRST_FOV : Math.clamp(k.fov() + notch * FOV_STEP, 0.0,
                        CameraPath.Keyframe.MOST_FOV);
                yield new CameraPath.Keyframe(k.time(), k.x(), k.y(), k.z(), k.yaw(), k.pitch(), k.roll(), fov,
                        k.easing());
            }
            default -> new CameraPath.Keyframe(k.time(), k.x(), k.y(), k.z(), k.yaw(), k.pitch(),
                    Mth.wrapDegrees(k.roll() + notch * ROLL_STEP), k.fov(), k.easing());
        };
    }

    /* The keyframe with the easing after its own, or before it when {@code way} is below 0. */
    private static CameraPath.Keyframe withEasing(final CameraPath.Keyframe k, final int way) {
        final List<Easing> all = List.copyOf(EnumSet.allOf(Easing.class));
        final Easing next = all.get(Math.floorMod(all.indexOf(k.easing()) + way, all.size()));
        return new CameraPath.Keyframe(k.time(), k.x(), k.y(), k.z(), k.yaw(), k.pitch(), k.roll(), k.fov(), next);
    }
}
