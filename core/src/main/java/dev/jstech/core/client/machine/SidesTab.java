/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.machine;

import static dev.jstech.core.machine.MachineScreenLayout.BUTTON_HEIGHT;
import static dev.jstech.core.machine.MachineScreenLayout.BUTTON_WIDTH;
import static dev.jstech.core.machine.MachineScreenLayout.CUBE_FRONT_X;
import static dev.jstech.core.machine.MachineScreenLayout.CUBE_REAR_X;
import static dev.jstech.core.machine.MachineScreenLayout.CUBE_SIZE;
import static dev.jstech.core.machine.MachineScreenLayout.CUBE_Y;
import static dev.jstech.core.machine.MachineScreenLayout.FACE_KNOB_R;
import static dev.jstech.core.machine.MachineScreenLayout.FACE_KNOB_X;
import static dev.jstech.core.machine.MachineScreenLayout.FACE_KNOB_Y;
import static dev.jstech.core.machine.MachineScreenLayout.FILTER_X;
import static dev.jstech.core.machine.MachineScreenLayout.FILTER_Y;
import static dev.jstech.core.machine.MachineScreenLayout.PULL_TOGGLE_X;
import static dev.jstech.core.machine.MachineScreenLayout.PULL_WHEEL_X;
import static dev.jstech.core.machine.MachineScreenLayout.PUSH_TOGGLE_X;
import static dev.jstech.core.machine.MachineScreenLayout.PUSH_WHEEL_X;
import static dev.jstech.core.machine.MachineScreenLayout.RESOURCE_X;
import static dev.jstech.core.machine.MachineScreenLayout.TOGGLE_HEIGHT;
import static dev.jstech.core.machine.MachineScreenLayout.TOGGLE_WIDTH;
import static dev.jstech.core.machine.MachineScreenLayout.TOGGLE_Y;
import static dev.jstech.core.machine.MachineScreenLayout.WHEEL_HEIGHT;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.connect.RelativeFace;
import dev.jstech.core.inventory.FaceMode;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.machine.FaceModes;
import dev.jstech.core.machine.MachineMenu;
import dev.jstech.core.machine.MachineResource;
import dev.jstech.core.machine.MachineScreenLayout;
import dev.jstech.core.machine.MachineScreenTexts;
import dev.jstech.core.machine.MachineSides;
import dev.jstech.core.machine.MachineWheels;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * The Sides tab, as the approved design draws it: a lit pushbutton for each resource the machine has; the machine as
 * two isometric cubes, seen from the front and from the back, each face tinted and named by what it does with the
 * resource picked (in, out, both or neither); the knob of the face picked; and below, the switches of what it pushes
 * out and pulls in by itself, the thumbwheels of how much a second, and the filter of what passes.
 *
 * <p>A machine with more resources than five buttons hold shows its states of matter (gas, supercritical fluid, slurry,
 * plasma and exotic matter) behind one button, which steps through them as it is pressed again.
 */
public final class SidesTab implements IMachineFace {

    private int picked;
    private int matter;
    private RelativeFace face = RelativeFace.FRONT;

    /* The faces each cube shows: its top, its left and its right. */
    private static final List<RelativeFace> FRONT_CUBE = List.of(RelativeFace.TOP, RelativeFace.FRONT,
            RelativeFace.RIGHT);
    private static final List<RelativeFace> REAR_CUBE = List.of(RelativeFace.BOTTOM, RelativeFace.BACK,
            RelativeFace.LEFT);
    private static final int LABEL_ROOM = 38;
    private static final int TOGGLE_WORD_ROOM = 19;
    private static final int VIEW_WORD_ROOM = 52;
    private static final int COLUMN_ROOM = 54;
    private static final int FLOW_ITEM = 0xFF << 24 | 0xBE9678;
    private static final int RING_SHOWN = 20;
    private static final int RING_PERIOD = 24;
    private static final int VIEW_WORD_Y = 88;
    private static final int TITLE_Y = 38;
    private static final int FLOWS = 3;

    @Override
    public void draw(final MachineView v) {
        final GuiGraphics g = v.g();
        final Font font = v.font();
        final MachineArt.Colours c = MachineArt.PALETTE.get();
        final List<Button> buttons = buttons(v);
        if (buttons.isEmpty()) {
            return;
        }
        for (int k = 0; k < buttons.size(); k++) {
            MachineArt.pushbutton(g, font, v.x(RESOURCE_X), v.y(MachineScreenLayout.resourceY(k, buttons.size())),
                    k == this.picked, c.pushLit(), MachineArt.fit(font, buttons.get(k).word(), LABEL_ROOM));
        }
        final MachineResource resource = resource(v);
        final MachineSides sides = v.machine().sides();
        final boolean ring = (int) v.tick() % RING_PERIOD < RING_SHOWN;
        cube(v, CUBE_FRONT_X, FRONT_CUBE, resource, ring);
        cube(v, CUBE_REAR_X, REAR_CUBE, resource, ring);
        MachineArt.engrave(g, font, MachineArt.fit(font, MachineWords.upper(MachineScreenTexts.FRONT_VIEW),
                VIEW_WORD_ROOM), v.x(CUBE_FRONT_X), v.y(VIEW_WORD_Y), true);
        MachineArt.engrave(g, font, MachineArt.fit(font, MachineWords.upper(MachineScreenTexts.REAR_VIEW),
                VIEW_WORD_ROOM), v.x(CUBE_REAR_X), v.y(VIEW_WORD_Y), true);
        if (v.working()) {
            flow(v, sides.mode(resource, RelativeFace.FRONT), sides.mode(resource, RelativeFace.RIGHT));
        }
        MachineArt.engrave(g, font, MachineArt.fit(font, MachineWords.upper(MachineScreenTexts.FACE_TITLE.with(
                MachineWords.face(this.face).text())), COLUMN_ROOM), v.x(FACE_KNOB_X), v.y(TITLE_Y), true);
        final FaceMode mode = sides.mode(resource, this.face);
        final List<MachineArt.KnobLabel> labels = new ArrayList<>();
        for (int i = 0; i < FaceModes.round().size(); i++) {
            final FaceMode each = FaceModes.round().get(i);
            labels.add(new MachineArt.KnobLabel(MachineWords.upper(MachineWords.mode(each)),
                    MachineScreenLayout.knobAngle(i), each == mode));
        }
        MachineArt.knob(g, font, v.x(FACE_KNOB_X), v.y(FACE_KNOB_Y), FACE_KNOB_R,
                MachineScreenLayout.knobAngle(FaceModes.round().indexOf(mode)), labels, "");
        final MachineSides.Automation auto = sides.automation(resource);
        final int unit = MachineWheels.unit(resource);
        MachineArt.toggle(g, v.x(PUSH_TOGGLE_X), v.y(TOGGLE_Y), auto.push());
        MachineArt.engrave(g, font, MachineArt.fit(font, MachineWords.upper(MachineScreenTexts.PUSH),
                TOGGLE_WORD_ROOM), v.x(PUSH_TOGGLE_X + TOGGLE_WIDTH + 3), v.y(TOGGLE_Y + 2), false);
        MachineArt.thumbwheel(g, font, v.x(PUSH_WHEEL_X), v.y(TOGGLE_Y), MachineWheels.shown(auto.pushRate() / unit));
        MachineArt.toggle(g, v.x(PULL_TOGGLE_X), v.y(TOGGLE_Y), auto.pull());
        MachineArt.engrave(g, font, MachineArt.fit(font, MachineWords.upper(MachineScreenTexts.PULL),
                TOGGLE_WORD_ROOM), v.x(PULL_TOGGLE_X + TOGGLE_WIDTH + 3), v.y(TOGGLE_Y + 2), false);
        MachineArt.thumbwheel(g, font, v.x(PULL_WHEEL_X), v.y(TOGGLE_Y), MachineWheels.shown(auto.pullRate() / unit));
        if (filtered(resource)) {
            MachineArt.pushbutton(g, font, v.x(FILTER_X), v.y(FILTER_Y), !auto.filter().equals(ItemFilter.EVERYTHING),
                    c.pushLit(), MachineWords.upper(MachineScreenTexts.FILTER));
        }
    }

    @Override
    public boolean clicked(final MachineView v, final double x, final double y, final int button) {
        final List<Button> buttons = buttons(v);
        if (buttons.isEmpty()) {
            return false;
        }
        for (int k = 0; k < buttons.size(); k++) {
            if (MachineScreenLayout.on(x, y, RESOURCE_X, MachineScreenLayout.resourceY(k, buttons.size()),
                    BUTTON_WIDTH, BUTTON_HEIGHT)) {
                if (k == this.picked && buttons.get(k).group()) {
                    this.matter = (this.matter + 1) % matterStates(v).size();
                }
                this.picked = k;
                return true;
            }
        }
        final MachineResource resource = resource(v);
        final RelativeFace hit = cubeFaceAt(x, y);
        if (hit != null) {
            this.face = hit;
            if (button == InputConstants.MOUSE_BUTTON_RIGHT) {
                turn(v, resource, next(v.machine().sides().mode(resource, hit)));
            }
            return true;
        }
        if (MachineScreenLayout.onDisc(x, y, FACE_KNOB_X, FACE_KNOB_Y, FACE_KNOB_R)) {
            turn(v, resource, next(v.machine().sides().mode(resource, this.face)));
            return true;
        }
        for (int i = 0; i < FaceModes.round().size(); i++) {
            final int[] word = MachineScreenLayout.knobWord(FACE_KNOB_X, FACE_KNOB_Y, FACE_KNOB_R,
                    MachineScreenLayout.knobAngle(i));
            if (MachineScreenLayout.onWord(x, y, word, MachineArt.width(v.font(), MachineWords.upper(
                    MachineWords.mode(FaceModes.round().get(i)))))) {
                turn(v, resource, FaceModes.round().get(i));
                return true;
            }
        }
        if (MachineScreenLayout.on(x, y, PUSH_TOGGLE_X, TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT)) {
            v.press().accept(MachineMenu.automationButton(resource, MachineMenu.PUSH));
            return true;
        }
        if (MachineScreenLayout.on(x, y, PULL_TOGGLE_X, TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT)) {
            v.press().accept(MachineMenu.automationButton(resource, MachineMenu.PULL));
            return true;
        }
        if (wheel(v, resource, x, y, y < TOGGLE_Y + WHEEL_HEIGHT / 2.0)) {
            return true;
        }
        if (filtered(resource) && MachineScreenLayout.on(x, y, FILTER_X, FILTER_Y, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            v.press().accept(MachineMenu.FIRST_FILTER + MachineResource.round().indexOf(resource));
            return true;
        }
        return false;
    }

    @Override
    public boolean scrolled(final MachineView v, final double x, final double y, final double amount) {
        return !buttons(v).isEmpty() && amount != 0 && wheel(v, resource(v), x, y, amount > 0);
    }

    @Override
    public List<Component> tooltip(final MachineView v, final double x, final double y) {
        final List<Button> buttons = buttons(v);
        if (buttons.isEmpty()) {
            return List.of();
        }
        for (int k = 0; k < buttons.size(); k++) {
            if (MachineScreenLayout.on(x, y, RESOURCE_X, MachineScreenLayout.resourceY(k, buttons.size()),
                    BUTTON_WIDTH, BUTTON_HEIGHT)) {
                final Button each = buttons.get(k);
                return List.of(GameText.component((each.group() ? MachineScreenTexts.TIP_MATTER
                        : MachineScreenTexts.TIP_RESOURCE).with(each.resource().text())));
            }
        }
        final MachineResource resource = resource(v);
        final MachineSides sides = v.machine().sides();
        final RelativeFace hit = cubeFaceAt(x, y);
        if (hit != null) {
            return List.of(GameText.component(MachineScreenTexts.TIP_FACE.with(MachineWords.face(hit).text(),
                    resource.text(), MachineWords.mode(sides.mode(resource, hit)).text())),
                    GameText.component(MachineScreenTexts.TIP_FACE_USE));
        }
        if (MachineScreenLayout.onDisc(x, y, FACE_KNOB_X, FACE_KNOB_Y, FACE_KNOB_R)) {
            return List.of(GameText.component(MachineScreenTexts.TIP_FACE.with(MachineWords.face(this.face).text(),
                    resource.text(), MachineWords.mode(sides.mode(resource, this.face)).text())),
                    GameText.component(MachineScreenTexts.TIP_KNOB_USE));
        }
        final MachineSides.Automation auto = sides.automation(resource);
        if (MachineScreenLayout.on(x, y, PUSH_TOGGLE_X, TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT)) {
            return List.of(GameText.component(MachineScreenTexts.TIP_PUSH.with(resource.text(),
                    (auto.push() ? MachineScreenTexts.ON : MachineScreenTexts.OFF).text())));
        }
        if (MachineScreenLayout.on(x, y, PULL_TOGGLE_X, TOGGLE_Y, TOGGLE_WIDTH, TOGGLE_HEIGHT)) {
            return List.of(GameText.component(MachineScreenTexts.TIP_PULL.with(resource.text(),
                    (auto.pull() ? MachineScreenTexts.ON : MachineScreenTexts.OFF).text())));
        }
        if (MachineScreenLayout.wheelDrumAt(x, y, PUSH_WHEEL_X, TOGGLE_Y) >= 0
                || MachineScreenLayout.wheelDrumAt(x, y, PULL_WHEEL_X, TOGGLE_Y) >= 0) {
            return List.of(GameText.component(MachineWheels.unit(resource) == 1 ? MachineScreenTexts.WHEEL_ITEMS
                    : MachineScreenTexts.WHEEL_HUNDREDS), GameText.component(MachineScreenTexts.WHEEL_HINT));
        }
        if (filtered(resource) && MachineScreenLayout.on(x, y, FILTER_X, FILTER_Y, BUTTON_WIDTH, BUTTON_HEIGHT)) {
            return List.of(GameText.component(filterWords(auto.filter())),
                    GameText.component(MachineScreenTexts.TIP_FILTER_USE));
        }
        return List.of();
    }

    /* The resource the cubes and the controls show: the button picked, or the state of matter its group steps to. */
    private MachineResource resource(final MachineView v) {
        final List<Button> buttons = buttons(v);
        this.picked = Math.min(this.picked, buttons.size() - 1);
        final Button chosen = buttons.get(this.picked);
        if (!chosen.group()) {
            return chosen.resource();
        }
        final List<MachineResource> states = matterStates(v);
        this.matter = Math.min(this.matter, states.size() - 1);
        return states.get(this.matter);
    }

    /* The buttons: one for each resource, or the states of matter behind one when the buttons would be too many. */
    private static List<Button> buttons(final MachineView v) {
        final List<MachineResource> all = v.machine().sides().resources();
        final List<Button> buttons = new ArrayList<>();
        if (all.size() <= MachineScreenLayout.RESOURCES_SHOWN) {
            for (final MachineResource resource : all) {
                buttons.add(new Button(resource, false, MachineWords.upper(resource.text())));
            }
            return buttons;
        }
        boolean grouped = false;
        for (final MachineResource resource : all) {
            if (resource.matter().isEmpty()) {
                buttons.add(new Button(resource, false, MachineWords.upper(resource.text())));
            } else if (!grouped) {
                grouped = true;
                buttons.add(new Button(resource, true, MachineWords.upper(MachineScreenTexts.MATTER)));
            }
        }
        return buttons;
    }

    private static List<MachineResource> matterStates(final MachineView v) {
        return v.machine().sides().resources().stream().filter(resource -> resource.matter().isPresent()).toList();
    }

    /* One of the cubes, its faces tinted by what they do with {@code resource}, the face picked ringed. */
    private void cube(final MachineView v, final int ox, final List<RelativeFace> faces,
                      final MachineResource resource, final boolean ring) {
        final List<MachineArt.CubeFace> drawn = new ArrayList<>();
        for (final RelativeFace each : faces) {
            final FaceMode mode = v.machine().sides().mode(resource, each);
            drawn.add(new MachineArt.CubeFace(MachineWords.upper(MachineWords.face(each)),
                    MachineWords.upper(MachineWords.mode(mode)), MachineWords.colour(mode)));
        }
        MachineArt.isoCube(v.g(), v.font(), v.x(ox), v.y(CUBE_Y), CUBE_SIZE, drawn,
                ring ? faces.indexOf(this.face) : -1);
    }

    /* Things flowing in at the front face and out at the right one while the machine works, as each face lets them. */
    private static void flow(final MachineView v, final FaceMode front, final FaceMode right) {
        for (int k = 0; k < FLOWS; k++) {
            final double s = (v.tick() / 8.0 + k / (double) FLOWS) % 1.0;
            if (front == FaceMode.IN || front == FaceMode.BOTH) {
                final int ix = v.x(CUBE_FRONT_X - 32 + (int) (s * 10));
                final int iy = v.y(CUBE_Y + 46 - (int) (s * 5));
                MachineArt.fill(v.g(), ix, iy, ix + 2, iy + 2, FLOW_ITEM);
            }
            if (right == FaceMode.OUT || right == FaceMode.BOTH) {
                final int ox = v.x(CUBE_FRONT_X + 26 + (int) (s * 8));
                final int oy = v.y(CUBE_Y + 42 + (int) (s * 4));
                MachineArt.fill(v.g(), ox, oy, ox + 2, oy + 2, FLOW_ITEM);
            }
        }
    }

    /* The face of either cube under ({@code x}, {@code y}); none for neither. */
    private static @Nullable RelativeFace cubeFaceAt(final double x, final double y) {
        final int front = MachineScreenLayout.cubeFaceAt(x, y, CUBE_FRONT_X, CUBE_Y, CUBE_SIZE);
        if (front >= 0) {
            return FRONT_CUBE.get(front);
        }
        final int rear = MachineScreenLayout.cubeFaceAt(x, y, CUBE_REAR_X, CUBE_Y, CUBE_SIZE);
        return rear >= 0 ? REAR_CUBE.get(rear) : null;
    }

    /* Sets what the face picked does with {@code resource}. */
    private void turn(final MachineView v, final MachineResource resource, final FaceMode to) {
        v.press().accept(MachineMenu.sideButton(resource, this.face, to));
    }

    /* Turns the drum under ({@code x}, {@code y}) of either wheel, up or down; whether one was there. */
    private static boolean wheel(final MachineView v, final MachineResource resource, final double x, final double y,
                                 final boolean up) {
        final int push = MachineScreenLayout.wheelDrumAt(x, y, PUSH_WHEEL_X, TOGGLE_Y);
        if (push >= 0) {
            v.press().accept(MachineMenu.automationButton(resource, (up ? MachineMenu.PUSH_UP : MachineMenu.PUSH_DOWN)
                    + push));
            return true;
        }
        final int pull = MachineScreenLayout.wheelDrumAt(x, y, PULL_WHEEL_X, TOGGLE_Y);
        if (pull >= 0) {
            v.press().accept(MachineMenu.automationButton(resource, (up ? MachineMenu.PULL_UP : MachineMenu.PULL_DOWN)
                    + pull));
            return true;
        }
        return false;
    }

    private static FaceMode next(final FaceMode mode) {
        final List<FaceMode> round = FaceModes.round();
        return round.get((round.indexOf(mode) + 1) % round.size());
    }

    /* Whether a filter has a meaning for {@code resource}: energy and heat pass whole. */
    private static boolean filtered(final MachineResource resource) {
        return resource != MachineResource.ENERGY && resource != MachineResource.HEAT;
    }

    private static Text filterWords(final ItemFilter filter) {
        if (filter.rules().isEmpty() || filter.mode() != ItemFilter.Mode.ONLY) {
            return MachineScreenTexts.TIP_FILTER_ALL.text();
        }
        return MachineScreenTexts.TIP_FILTER_ONLY.with(filter.rules().getFirst() instanceof ItemFilter.Fuzzy fuzzy
                ? fuzzy.item() : "");
    }

    /**
     * One pushbutton of a resource.
     *
     * @param resource the resource, or the first state of matter for the group
     * @param group    whether it stands for the states of matter together
     * @param word     its legend
     */
    private record Button(MachineResource resource, boolean group, String word) {
    }
}
