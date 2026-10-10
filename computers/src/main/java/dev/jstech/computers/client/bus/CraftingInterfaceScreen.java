/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.bus;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.computers.block.part.CraftingInterfacePart;
import dev.jstech.computers.client.AbstractComputerScreen;
import dev.jstech.computers.crafting.CraftingEras;
import dev.jstech.computers.crafting.CraftingLog;
import dev.jstech.computers.crafting.InterfaceScript;
import dev.jstech.computers.gui.layout.CraftingInterfaceLayout;
import dev.jstech.computers.gui.layout.CraftingInterfaceLayout.Kind;
import dev.jstech.computers.menu.CraftingInterfaceMenu;
import dev.jstech.computers.operation.payload.CraftingInterfaceEditPayload;
import dev.jstech.computers.operation.payload.InterfaceView;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Grounds;
import dev.jstech.core.client.gui.theme.JsTechTheme;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import dev.jstech.core.tier.HardwareEra;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The window of a Crafting Interface, in the skin of the Crafting Computer that commands it: a header with the lamp of
 * whether one drives it, and three tabs. Configure holds its name, the patterns it holds (a click picks one, and the
 * inputs of the one picked show with the router of its cable each goes through, which a click changes), its mode,
 * what it feeds and the Receiving Buses that credit it, paused or running, its most jobs, its warnings and what it is
 * doing now. Activity lists how its jobs ended; Software, its settings as software writes them.
 */
public class CraftingInterfaceScreen extends AbstractComputerScreen<CraftingInterfaceMenu> {

    private EditBox nameBox;
    @Nullable
    private String nameValue;
    private int tab = CraftingInterfaceLayout.TAB_CONFIGURE;
    private int picked;
    private int scroll;
    private int activityScroll;
    private int softwareScroll;
    private List<CraftingInterfaceLayout.Row> rows = List.of();
    private int contentHeight;
    private int softwareContent;

    private static final TextKey[] TAB_WORDS = {BusTexts.TAB_CONFIGURE, BusTexts.TAB_ACTIVITY, BusTexts.TAB_SOFTWARE};
    private static final int MOST_JOBS = 64;

    public CraftingInterfaceScreen(final CraftingInterfaceMenu menu, final Inventory inventory,
                                   final Component title) {
        super(menu, inventory, title);
        this.imageWidth = CraftingInterfaceLayout.WIDTH;
        this.imageHeight = CraftingInterfaceLayout.configureHeight(shape());
        this.titleLabelX = OFF_SCREEN;
        this.inventoryLabelY = OFF_SCREEN;
    }

    /** The tab shown: Configure, Activity or Software. */
    public int tab() {
        return tab;
    }

    /** Shows {@code shown}, as a click on its tab does. */
    public void showTab(final int shown) {
        tab = shown;
        if (nameBox != null) {
            nameBox.setVisible(shown == CraftingInterfaceLayout.TAB_CONFIGURE);
            nameBox.setFocused(false);
        }
        imageHeight = heightOf(shown);
    }

    /** The pattern picked, whose inputs show, as a click on its cell does. */
    public int picked() {
        return picked;
    }

    /** Picks pattern {@code index}, as a click on its cell does. */
    public void pick(final int index) {
        picked = Math.max(0, index);
    }

    @Override
    protected void init() {
        imageHeight = CraftingInterfaceLayout.configureHeight(shape());
        super.init();
        nameBox = new EditBox(font, leftPos + CraftingInterfaceLayout.NAME_X + 3,
                topPos + CraftingInterfaceLayout.NAME_Y + 2, CraftingInterfaceLayout.NAME_W - 6,
                CraftingInterfaceLayout.NAME_H - 3, GameText.component(InterfaceTexts.NAME_FIELD));
        nameBox.setBordered(false);
        nameBox.setTextShadow(false);
        nameBox.setMaxLength(CraftingInterfacePart.MAX_NAME_LENGTH);
        nameBox.setTextColor(JsTechTheme.text());
        // Set the value before the responder so restoring it (open, or a window resize) sends no packet.
        nameBox.setValue(nameValue != null ? nameValue : menu.interfaceName());
        nameBox.setResponder(this::onNameChanged);
        addRenderableWidget(nameBox);
        showTab(tab);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        imageHeight = heightOf(tab);
    }

    @Override
    @Nullable
    protected HardwareEra screenEra() {
        final InterfaceView state = menu.state();
        return state == null ? HardwareEra.STANDARD : state.skin();
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final int x = leftPos;
        final int y = topPos;
        JsTechTheme.window(g, x, y, imageWidth, imageHeight);
        JsTechTheme.headerBar(g, x + CraftingInterfaceLayout.HEADER_X, y + CraftingInterfaceLayout.HEADER_Y,
                CraftingInterfaceLayout.HEADER_W);
        // The early-PC skin's windows have a title bar in the system blue, with the title in cream on it.
        final boolean titleBar = JsTechTheme.active().style().doubleBevel();
        if (titleBar) {
            Grounds.fill(g, x + CraftingInterfaceLayout.HEADER_X, y + CraftingInterfaceLayout.HEADER_Y,
                    x + CraftingInterfaceLayout.HEADER_X + CraftingInterfaceLayout.HEADER_W,
                    y + CraftingInterfaceLayout.HEADER_Y + 16, JsTechTheme.tabOn());
        }
        Draw.text(g, font, GameText.resolve(InterfaceTexts.TITLE), x + CraftingInterfaceLayout.TITLE_X,
                y + CraftingInterfaceLayout.TITLE_Y, titleBar ? JsTechTheme.tabLabelOn() : JsTechTheme.text());
        BusDraw.lamp(g, x + CraftingInterfaceLayout.LAMP_X, y + CraftingInterfaceLayout.LAMP_Y,
                CraftingInterfaceLayout.LAMP_SIZE, linked());
        drawTabs(g, x, y, mouseX, mouseY);
        switch (tab) {
            case CraftingInterfaceLayout.TAB_ACTIVITY -> drawActivity(g, x, y);
            case CraftingInterfaceLayout.TAB_SOFTWARE -> drawSoftware(g, x, y);
            default -> drawConfigure(g, x, y, mouseX, mouseY);
        }
    }

    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        // Everything is drawn with the window in renderBg; the container's own two labels are not used.
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        if (BusDraw.inside(mouseX, mouseY, leftPos + CraftingInterfaceLayout.LAMP_X,
                topPos + CraftingInterfaceLayout.LAMP_Y, CraftingInterfaceLayout.LAMP_SIZE,
                CraftingInterfaceLayout.LAMP_SIZE)) {
            g.renderTooltip(font, GameText.component(linked() ? InterfaceTexts.LINKED : InterfaceTexts.OFFLINE),
                    mouseX, mouseY);
        } else if (tab == CraftingInterfaceLayout.TAB_CONFIGURE) {
            final List<Component> lines = configureTooltip(mouseX - leftPos, mouseY - topPos);
            if (!lines.isEmpty()) {
                g.renderComponentTooltip(font, lines, mouseX, mouseY);
            }
        }
        renderTooltip(g, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (nameBox != null && nameBox.isVisible() && nameBox.isMouseOver(mouseX, mouseY)) {
            return super.mouseClicked(mouseX, mouseY, button);
        }
        for (int i = 0; i < CraftingInterfaceLayout.TABS; i++) {
            if (button == 0 && BusDraw.inside(mouseX, mouseY, leftPos + CraftingInterfaceLayout.tabX(i),
                    topPos + CraftingInterfaceLayout.TAB_Y, CraftingInterfaceLayout.tabW(i),
                    CraftingInterfaceLayout.TAB_H)) {
                showTab(i);
                return true;
            }
        }
        if (tab == CraftingInterfaceLayout.TAB_CONFIGURE && clickConfigure((int) mouseX - leftPos,
                (int) mouseY - topPos, button)) {
            setFocused(null);
            nameBox.setFocused(false);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        final int viewH = switch (tab) {
            case CraftingInterfaceLayout.TAB_ACTIVITY -> CraftingInterfaceLayout.ACTIVITY_VIEW_H;
            case CraftingInterfaceLayout.TAB_SOFTWARE -> CraftingInterfaceLayout.softwareView(softwareContent);
            default -> CraftingInterfaceLayout.configureView(shape());
        };
        final int viewY = tab == CraftingInterfaceLayout.TAB_CONFIGURE ? CraftingInterfaceLayout.CONFIGURE_VIEW_Y
                : CraftingInterfaceLayout.VIEW_Y;
        if (!BusDraw.inside(mouseX - leftPos, mouseY - topPos, CraftingInterfaceLayout.LABEL_X, viewY,
                CraftingInterfaceLayout.ROW_W, viewH)) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        final int step = (int) -Math.signum(scrollY) * CraftingInterfaceLayout.ROW;
        switch (tab) {
            case CraftingInterfaceLayout.TAB_ACTIVITY -> activityScroll = Math.max(0, activityScroll + step);
            case CraftingInterfaceLayout.TAB_SOFTWARE -> softwareScroll = Math.max(0, softwareScroll + step);
            default -> scroll = Math.max(0, scroll + step);
        }
        return true;
    }

    @Override
    public boolean keyPressed(final int key, final int scan, final int mods) {
        // While the name has focus, keys go to it and the inventory key types a letter; escape just leaves it.
        if (nameBox != null && nameBox.isFocused()) {
            if (key == InputConstants.KEY_ESCAPE) {
                nameBox.setFocused(false);
                setFocused(null);
                return true;
            }
            nameBox.keyPressed(key, scan, mods);
            return true;
        }
        return super.keyPressed(key, scan, mods);
    }

    private void drawConfigure(final GuiGraphics g, final int x, final int y, final int mouseX, final int mouseY) {
        BusDraw.small(g, font, GameText.resolve(InterfaceTexts.NAME), x + CraftingInterfaceLayout.LABEL_X,
                y + CraftingInterfaceLayout.NAME_Y + 2, JsTechTheme.dim());
        BusDraw.well(g, x + CraftingInterfaceLayout.NAME_X, y + CraftingInterfaceLayout.NAME_Y,
                CraftingInterfaceLayout.NAME_W, CraftingInterfaceLayout.NAME_H);
        final CraftingInterfaceLayout.Shape shape = shape();
        rows = CraftingInterfaceLayout.rows(shape);
        contentHeight = CraftingInterfaceLayout.contentHeight(shape);
        final int viewTop = y + CraftingInterfaceLayout.CONFIGURE_VIEW_Y;
        final int viewH = CraftingInterfaceLayout.configureView(shape);
        scroll = Math.max(0, Math.min(scroll, contentHeight - viewH));
        final InterfaceView state = menu.state();
        if (state == null) {
            return;
        }
        g.enableScissor(x + CraftingInterfaceLayout.LABEL_X, viewTop, x + CraftingInterfaceLayout.RIGHT,
                viewTop + viewH);
        for (final CraftingInterfaceLayout.Row row : rows) {
            final int ry = viewTop + row.y() - scroll;
            if (ry + row.height() > viewTop && ry < viewTop + viewH) {
                drawRow(g, state, row, x, ry, mouseX - x, mouseY - ry);
            }
        }
        g.disableScissor();
        BusDraw.scrollbar(g, x + CraftingInterfaceLayout.SCROLL_X, viewTop, viewH, scroll, contentHeight);
    }

    private void drawRow(final GuiGraphics g, final InterfaceView s, final CraftingInterfaceLayout.Row row,
                         final int x, final int y, final int px, final int py) {
        switch (row.kind()) {
            case PATTERNS -> {
                label(g, InterfaceTexts.PATTERNS, x, y + 2);
                BusDraw.small(g, font, GameText.resolve(InterfaceTexts.COUNT.with(s.patterns().size(), s.capacity())),
                        x + CraftingInterfaceLayout.LABEL_X, y + 11, JsTechTheme.dim());
                for (int i = 0; i < s.capacity(); i++) {
                    final int cx = x + CraftingInterfaceLayout.CONTROL_X + i % CraftingInterfaceLayout.CELLS_PER_ROW
                            * CraftingInterfaceLayout.CELL;
                    final int cy = y + 1 + i / CraftingInterfaceLayout.CELLS_PER_ROW * CraftingInterfaceLayout.CELL;
                    final ItemStack icon = i < s.patterns().size() ? s.patterns().get(i).icon() : ItemStack.EMPTY;
                    BusDraw.cell(g, cx, cy, icon, false);
                    if (i < s.patterns().size() && i == picked) {
                        Draw.outline(g, cx + 1, cy + 1, 16, 16, JsTechTheme.accent());
                    }
                }
            }
            case PATTERN_NOTE -> paragraph(g, GameText.resolve(InterfaceTexts.PATTERN_NOTE), x, y);
            case INPUTS_LABEL -> {
                final InterfaceView.PatternView pattern = pickedPattern(s);
                if (pattern != null) {
                    BusDraw.small(g, font, BusDraw.clip(font, GameText.resolve(InterfaceTexts.INPUTS_OF.with(
                            pattern.name())).toUpperCase(Locale.ROOT), CraftingInterfaceLayout.ROW_W),
                            x + CraftingInterfaceLayout.LABEL_X, y + 1, JsTechTheme.dim());
                }
            }
            case MAPPING -> drawMapping(g, s, row.index(), x, y, px, py);
            case MODE -> {
                label(g, InterfaceTexts.MODE, x, y + 2);
                toggle(g, x, y, px, py, s.exclusive() ? 0 : 1, InterfaceTexts.EXCLUSIVE, InterfaceTexts.NOT_EXCLUSIVE);
                mark(g, s, CraftingInterfacePart.MODE, x, y);
            }
            case MODE_NOTE -> paragraph(g, GameText.resolve(s.exclusive() ? InterfaceTexts.EXCLUSIVE_NOTE
                    : InterfaceTexts.SHARED_NOTE), x, y);
            case FEEDS -> valueLines(g, InterfaceTexts.FEEDS, GameText.resolve(s.feeds()), x, y, JsTechTheme.text());
            case RECEIVING -> {
                label(g, InterfaceTexts.RECEIVING, x, y + 2);
                BusDraw.small(g, font, BusDraw.clip(font, GameText.resolve(s.receiving()),
                        CraftingInterfaceLayout.RIGHT - CraftingInterfaceLayout.CONTROL_X - 4),
                        x + CraftingInterfaceLayout.CONTROL_X + 4, y + 2, JsTechTheme.text());
            }
            case STATE -> {
                label(g, InterfaceTexts.STATE, x, y + 2);
                toggle(g, x, y, px, py, s.paused() ? 1 : 0, InterfaceTexts.RUNNING, InterfaceTexts.PAUSED);
                mark(g, s, CraftingInterfacePart.STATE, x, y);
            }
            case JOBS -> {
                label(g, InterfaceTexts.MAX_JOBS, x, y + 2);
                final int h = CraftingInterfaceLayout.CONTROL_H;
                BusDraw.button(g, font, "-", x + CraftingInterfaceLayout.MINUS_X, y, CraftingInterfaceLayout.STEP_W,
                        h, over(px, py, CraftingInterfaceLayout.MINUS_X, 0, CraftingInterfaceLayout.STEP_W, h));
                BusDraw.well(g, x + CraftingInterfaceLayout.VALUE_X, y, CraftingInterfaceLayout.VALUE_W, h);
                final String value = s.maxJobs() == 0 ? GameText.resolve(InterfaceTexts.AUTO)
                        : String.valueOf(s.maxJobs());
                BusDraw.smallCentered(g, font, value, x + CraftingInterfaceLayout.VALUE_X
                        + CraftingInterfaceLayout.VALUE_W / 2, y + 2, JsTechTheme.text());
                BusDraw.button(g, font, "+", x + CraftingInterfaceLayout.PLUS_X, y, CraftingInterfaceLayout.STEP_W,
                        h, over(px, py, CraftingInterfaceLayout.PLUS_X, 0, CraftingInterfaceLayout.STEP_W, h));
                final String program = s.marks().getOrDefault(CraftingInterfacePart.JOBS, "");
                if (!program.isEmpty()) {
                    BusDraw.mark(g, font, program, x + CraftingInterfaceLayout.NOTE_X, y + 2,
                            CraftingInterfaceLayout.RIGHT - CraftingInterfaceLayout.NOTE_X);
                } else {
                    BusDraw.small(g, font, BusDraw.clip(font, GameText.resolve(InterfaceTexts.JOBS_NOTE),
                            CraftingInterfaceLayout.RIGHT - CraftingInterfaceLayout.NOTE_X),
                            x + CraftingInterfaceLayout.NOTE_X, y + 2, JsTechTheme.dim());
                }
            }
            case WARNING -> {
                if (row.index() < s.warnings().size()) {
                    final int h = row.height() - 2;
                    BusDraw.bar(g, x + CraftingInterfaceLayout.LABEL_X, y, CraftingInterfaceLayout.ROW_W, h, false);
                    Draw.outline(g, x + CraftingInterfaceLayout.LABEL_X, y, CraftingInterfaceLayout.ROW_W, h,
                            JsTechTheme.red());
                    final List<String> lines = warningLines(s.warnings().get(row.index()));
                    for (int i = 0; i < lines.size(); i++) {
                        BusDraw.small(g, font, lines.get(i), x + CraftingInterfaceLayout.LABEL_X + 3,
                                y + 4 + i * CraftingInterfaceLayout.LINE, JsTechTheme.red());
                    }
                }
            }
            case NOW -> valueLines(g, InterfaceTexts.NOW, GameText.resolve(s.now()), x, y, switch (s.nowTone()) {
                case InterfaceView.GOOD -> JsTechTheme.green();
                case InterfaceView.WARN -> JsTechTheme.amber();
                default -> JsTechTheme.dim();
            });
        }
    }

    /* An input of the pattern picked: its cell, its name and amount over why its router carries it, the router. */
    private void drawMapping(final GuiGraphics g, final InterfaceView s, final int index, final int x, final int y,
                             final int px, final int py) {
        final InterfaceView.PatternView pattern = pickedPattern(s);
        if (pattern == null || index >= pattern.inputs().size()) {
            return;
        }
        final InterfaceView.InputView input = pattern.inputs().get(index);
        final boolean missing = input.why() == InterfaceView.NO_ROUTER;
        BusDraw.bar(g, x + CraftingInterfaceLayout.LABEL_X, y, CraftingInterfaceLayout.ROW_W,
                CraftingInterfaceLayout.MAPPING_H, false);
        if (missing) {
            Draw.outline(g, x + CraftingInterfaceLayout.LABEL_X, y, CraftingInterfaceLayout.ROW_W,
                    CraftingInterfaceLayout.MAPPING_H, JsTechTheme.red());
        }
        final ItemStack icon = input.key().isItem() ? input.key().stack(1) : ItemStack.EMPTY;
        BusDraw.cell(g, x + CraftingInterfaceLayout.LABEL_X, y + 1, icon, false);
        final int textX = x + CraftingInterfaceLayout.LABEL_X + CraftingInterfaceLayout.CELL + 4;
        final int room = CraftingInterfaceLayout.PICK_X - CraftingInterfaceLayout.LABEL_X - CraftingInterfaceLayout.CELL
                - 8;
        BusDraw.small(g, font, BusDraw.clip(font, GameText.resolve(InterfaceTexts.INPUT.with(
                input.key().displayName().getString(), input.amount())), room), textX, y + 3, JsTechTheme.text());
        final TextKey why = switch (input.why()) {
            case InterfaceView.CHOSEN -> InterfaceTexts.WHY_CHOSEN;
            case InterfaceView.FILTER -> InterfaceTexts.WHY_FILTER;
            case InterfaceView.ANY -> InterfaceTexts.WHY_ANY;
            default -> InterfaceTexts.WHY_NONE;
        };
        BusDraw.small(g, font, BusDraw.clip(font, GameText.resolve(why), room), textX, y + 11,
                missing ? JsTechTheme.red() : JsTechTheme.dim());
        if (s.mode() != InterfaceView.CABLE) {
            return;
        }
        final String router = input.router() >= 0 && input.router() < s.routers().size()
                ? GameText.resolve(s.routers().get(input.router())) : GameText.resolve(InterfaceTexts.NO_ROUTER);
        BusDraw.button(g, font, BusDraw.clip(font, router + " v", CraftingInterfaceLayout.PICK_W - 6),
                x + CraftingInterfaceLayout.PICK_X, y + 4, CraftingInterfaceLayout.PICK_W,
                CraftingInterfaceLayout.CONTROL_H, over(px, py, CraftingInterfaceLayout.PICK_X, 4,
                        CraftingInterfaceLayout.PICK_W, CraftingInterfaceLayout.CONTROL_H));
    }

    private boolean clickConfigure(final int x, final int y, final int button) {
        final InterfaceView s = menu.state();
        final int viewTop = CraftingInterfaceLayout.CONFIGURE_VIEW_Y;
        final int viewH = CraftingInterfaceLayout.configureView(shape());
        if (s == null || !BusDraw.inside(x, y, CraftingInterfaceLayout.LABEL_X, viewTop,
                CraftingInterfaceLayout.ROW_W, viewH)) {
            return false;
        }
        final int at = y - viewTop + scroll;
        for (final CraftingInterfaceLayout.Row row : rows) {
            if (at < row.y() || at >= row.y() + row.height()) {
                continue;
            }
            final int ry = at - row.y();
            switch (row.kind()) {
                case PATTERNS -> {
                    for (int i = 0; i < s.patterns().size(); i++) {
                        if (over(x, ry, CraftingInterfaceLayout.CONTROL_X + i % CraftingInterfaceLayout.CELLS_PER_ROW
                                * CraftingInterfaceLayout.CELL, 1 + i / CraftingInterfaceLayout.CELLS_PER_ROW
                                * CraftingInterfaceLayout.CELL, CraftingInterfaceLayout.CELL,
                                CraftingInterfaceLayout.CELL)) {
                            picked = i;
                            return true;
                        }
                    }
                }
                case MAPPING -> {
                    if (over(x, ry, CraftingInterfaceLayout.PICK_X, 4, CraftingInterfaceLayout.PICK_W,
                            CraftingInterfaceLayout.CONTROL_H)) {
                        return clickRouter(s, row.index(), button);
                    }
                }
                case MODE -> {
                    final int option = optionAt(x, ry, InterfaceTexts.EXCLUSIVE, InterfaceTexts.NOT_EXCLUSIVE);
                    if (option >= 0) {
                        send(CraftingInterfaceEditPayload.of(CraftingInterfaceEditPayload.MODE, option == 0 ? 1 : 0));
                        return true;
                    }
                }
                case STATE -> {
                    final int option = optionAt(x, ry, InterfaceTexts.RUNNING, InterfaceTexts.PAUSED);
                    if (option >= 0) {
                        send(CraftingInterfaceEditPayload.of(CraftingInterfaceEditPayload.STATE, option));
                        return true;
                    }
                }
                case JOBS -> {
                    final int step = hasShiftDown() ? 8 : 1;
                    if (over(x, ry, CraftingInterfaceLayout.MINUS_X, 0, CraftingInterfaceLayout.STEP_W,
                            CraftingInterfaceLayout.CONTROL_H)) {
                        send(CraftingInterfaceEditPayload.of(CraftingInterfaceEditPayload.JOBS,
                                Math.max(0, s.maxJobs() - step)));
                        return true;
                    }
                    if (over(x, ry, CraftingInterfaceLayout.PLUS_X, 0, CraftingInterfaceLayout.STEP_W,
                            CraftingInterfaceLayout.CONTROL_H)) {
                        send(CraftingInterfaceEditPayload.of(CraftingInterfaceEditPayload.JOBS,
                                Math.min(MOST_JOBS, s.maxJobs() + step)));
                        return true;
                    }
                }
                default -> {
                    return false;
                }
            }
            return false;
        }
        return false;
    }

    /* A click on an input's router: the next router of the cable, or back to the one its filter picks. */
    private boolean clickRouter(final InterfaceView s, final int input, final int button) {
        final InterfaceView.PatternView pattern = pickedPattern(s);
        if (pattern == null || input >= pattern.inputs().size() || s.routers().isEmpty()) {
            return false;
        }
        final int current = pattern.inputs().get(input).router();
        final int next = button == 1 ? -1 : (current + 1) % s.routers().size();
        send(new CraftingInterfaceEditPayload(CraftingInterfaceEditPayload.ROUTE, picked, input, next, ""));
        return true;
    }

    private List<Component> configureTooltip(final int x, final int y) {
        final InterfaceView s = menu.state();
        if (s == null || !BusDraw.inside(x, y, CraftingInterfaceLayout.LABEL_X,
                CraftingInterfaceLayout.CONFIGURE_VIEW_Y, CraftingInterfaceLayout.ROW_W,
                CraftingInterfaceLayout.configureView(shape()))) {
            return List.of();
        }
        final int at = y - CraftingInterfaceLayout.CONFIGURE_VIEW_Y + scroll;
        for (final CraftingInterfaceLayout.Row row : rows) {
            if (at < row.y() || at >= row.y() + row.height()) {
                continue;
            }
            final int ry = at - row.y();
            if (row.kind() == Kind.MAPPING && s.mode() == InterfaceView.CABLE
                    && over(x, ry, CraftingInterfaceLayout.PICK_X, 4,
                    CraftingInterfaceLayout.PICK_W, CraftingInterfaceLayout.CONTROL_H)) {
                return List.of(GameText.component(InterfaceTexts.PICK_HINT));
            }
            if (row.kind() == Kind.PATTERNS) {
                for (int i = 0; i < s.patterns().size(); i++) {
                    if (over(x, ry, CraftingInterfaceLayout.CONTROL_X + i % CraftingInterfaceLayout.CELLS_PER_ROW
                            * CraftingInterfaceLayout.CELL, 1 + i / CraftingInterfaceLayout.CELLS_PER_ROW
                            * CraftingInterfaceLayout.CELL, CraftingInterfaceLayout.CELL,
                            CraftingInterfaceLayout.CELL)) {
                        return List.of(GameText.component(s.patterns().get(i).name()));
                    }
                }
            }
        }
        return List.of();
    }

    private void drawActivity(final GuiGraphics g, final int left, final int top) {
        final InterfaceView s = menu.state();
        final List<CraftingLog.Entry> entries = s == null ? List.of() : s.log();
        final int viewTop = top + CraftingInterfaceLayout.VIEW_Y;
        final int content = entries.size() * CraftingInterfaceLayout.ENTRY_H;
        activityScroll = Math.max(0, Math.min(activityScroll, content - CraftingInterfaceLayout.ACTIVITY_VIEW_H));
        if (entries.isEmpty()) {
            BusDraw.small(g, font, GameText.resolve(InterfaceTexts.NO_ACTIVITY), left + CraftingInterfaceLayout.LABEL_X,
                    viewTop + 2, JsTechTheme.dim());
        }
        g.enableScissor(left + CraftingInterfaceLayout.LABEL_X, viewTop, left + CraftingInterfaceLayout.RIGHT,
                viewTop + CraftingInterfaceLayout.ACTIVITY_VIEW_H);
        for (int i = 0; i < entries.size(); i++) {
            final int y = viewTop + i * CraftingInterfaceLayout.ENTRY_H - activityScroll;
            if (y + CraftingInterfaceLayout.ENTRY_H > viewTop
                    && y < viewTop + CraftingInterfaceLayout.ACTIVITY_VIEW_H) {
                drawEntry(g, entries.get(i), left, y);
            }
        }
        g.disableScissor();
        BusDraw.scrollbar(g, left + CraftingInterfaceLayout.SCROLL_X, viewTop, CraftingInterfaceLayout.ACTIVITY_VIEW_H,
                activityScroll, content);
        final List<String> note = BusDraw.lines(font, GameText.resolve(InterfaceTexts.ACTIVITY_NOTE),
                CraftingInterfaceLayout.ROW_W);
        for (int i = 0; i < Math.min(2, note.size()); i++) {
            BusDraw.small(g, font, note.get(i), left + CraftingInterfaceLayout.LABEL_X,
                    top + CraftingInterfaceLayout.ACTIVITY_NOTE_Y + i * CraftingInterfaceLayout.LINE,
                    JsTechTheme.dim());
        }
    }

    private void drawEntry(final GuiGraphics g, final CraftingLog.Entry entry, final int left, final int y) {
        BusDraw.bar(g, left + CraftingInterfaceLayout.LABEL_X, y, CraftingInterfaceLayout.ROW_W,
                CraftingInterfaceLayout.ENTRY_H - 1, false);
        final String time = BusDraw.clock(entry.time());
        final int x = left + CraftingInterfaceLayout.LABEL_X + 3;
        BusDraw.small(g, font, time, x, y + 2, JsTechTheme.dim());
        final int whatX = x + BusDraw.width(font, time) + 5;
        final String name = BusKeys.name(entry.what()).getString();
        final String what = entry.kind() == CraftingLog.UNEXPECTED ? name
                : GameText.resolve(InterfaceTexts.JOB.with(name, entry.total()));
        BusDraw.small(g, font, BusDraw.clip(font, what, left + CraftingInterfaceLayout.RIGHT - 3 - whatX), whatX,
                y + 2, JsTechTheme.text());
        final TextKey status = switch (entry.kind()) {
            case CraftingLog.COMPLETED, CraftingLog.UNEXPECTED -> InterfaceTexts.STATUS_COMPLETED;
            case CraftingLog.PARTIAL -> InterfaceTexts.STATUS_PARTIAL;
            case CraftingLog.FAILED -> InterfaceTexts.STATUS_FAILED;
            default -> InterfaceTexts.STATUS_WAITING;
        };
        final String word = GameText.resolve(status);
        final int colour = switch (entry.kind()) {
            case CraftingLog.COMPLETED, CraftingLog.UNEXPECTED -> JsTechTheme.green();
            case CraftingLog.FAILED -> JsTechTheme.red();
            default -> JsTechTheme.amber();
        };
        BusDraw.smallRight(g, font, word, left + CraftingInterfaceLayout.RIGHT - 3, y + 10, colour);
        final String why = GameText.resolve(switch (entry.kind()) {
            case CraftingLog.UNEXPECTED -> InterfaceTexts.UNEXPECTED.with(entry.amount(), name);
            case CraftingLog.FAILED -> InterfaceTexts.JOB_FAILED.text();
            case CraftingLog.DRAINING -> InterfaceTexts.DRAINING.text();
            default -> InterfaceTexts.JOB_DONE.with(entry.amount(), entry.total());
        });
        BusDraw.small(g, font, BusDraw.clip(font, why, CraftingInterfaceLayout.ROW_W - 11 - BusDraw.width(font, word)),
                x, y + 10, JsTechTheme.dim());
    }

    private void drawSoftware(final GuiGraphics g, final int left, final int top) {
        final InterfaceView s = menu.state();
        if (s == null) {
            return;
        }
        final List<Section> sections = sections(s);
        final int viewTop = top + CraftingInterfaceLayout.VIEW_Y;
        int content = 0;
        for (final Section section : sections) {
            content += CraftingInterfaceLayout.SECTION_LABEL_H + CraftingInterfaceLayout.codeBox(section.lines().size())
                    + CraftingInterfaceLayout.SECTION_GAP;
        }
        softwareContent = content;
        final int viewH = CraftingInterfaceLayout.softwareView(content);
        softwareScroll = Math.max(0, Math.min(softwareScroll, content - viewH));
        g.enableScissor(left + CraftingInterfaceLayout.LABEL_X, viewTop, left + CraftingInterfaceLayout.RIGHT,
                viewTop + viewH);
        int y = viewTop - softwareScroll;
        for (final Section section : sections) {
            if (!section.label().isEmpty()) {
                BusDraw.small(g, font, section.label(), left + CraftingInterfaceLayout.LABEL_X, y + 1,
                        JsTechTheme.dim());
            }
            y += CraftingInterfaceLayout.SECTION_LABEL_H;
            final int box = CraftingInterfaceLayout.codeBox(section.lines().size());
            if (section.code()) {
                BusDraw.code(g, left + CraftingInterfaceLayout.LABEL_X, y, CraftingInterfaceLayout.ROW_W, box);
            }
            for (int i = 0; i < section.lines().size(); i++) {
                BusDraw.small(g, font, BusDraw.clip(font, section.lines().get(i), CraftingInterfaceLayout.ROW_W - 6),
                        left + CraftingInterfaceLayout.LABEL_X + 3, y + CraftingInterfaceLayout.BOX_PAD
                                + i * CraftingInterfaceLayout.LINE, section.code() ? JsTechTheme.accent()
                                : JsTechTheme.dim());
            }
            y += box + CraftingInterfaceLayout.SECTION_GAP;
        }
        g.disableScissor();
        BusDraw.scrollbar(g, left + CraftingInterfaceLayout.SCROLL_X, viewTop, viewH, softwareScroll, content);
    }

    /* The Software tab: where software finds it, and the same settings in IQL and in Sigma. */
    private List<Section> sections(final InterfaceView s) {
        final List<Section> sections = new ArrayList<>();
        if (s.name().isEmpty()) {
            sections.add(new Section("", BusDraw.lines(font, GameText.resolve(InterfaceTexts.UNNAMED),
                    CraftingInterfaceLayout.ROW_W - 6), false));
            return sections;
        }
        final String name = s.name();
        sections.add(new Section(GameText.resolve(InterfaceTexts.ADDRESS), List.of("interface://" + name), true));
        sections.add(new Section(GameText.resolve(InterfaceTexts.IQL),
                InterfaceScript.iql(name, s.exclusive(), s.maxJobs(), s.paused()), true));
        sections.add(new Section(GameText.resolve(InterfaceTexts.SIGMA),
                InterfaceScript.sigma(name, s.exclusive(), s.maxJobs(), s.paused()), true));
        if (!s.marks().isEmpty()) {
            final List<String> marks = new ArrayList<>();
            s.marks().forEach((setting, program) -> marks.add(GameText.resolve(InterfaceTexts.SET_BY_LINE.with(
                    setting, program))));
            sections.add(new Section(GameText.resolve(InterfaceTexts.SET_BY_PROGRAMS), marks, false));
        }
        sections.add(new Section("", BusDraw.lines(font, GameText.resolve(InterfaceTexts.SOFTWARE_NOTE),
                CraftingInterfaceLayout.ROW_W - 6), false));
        return sections;
    }

    private void drawTabs(final GuiGraphics g, final int x, final int y, final int mouseX, final int mouseY) {
        for (int i = 0; i < CraftingInterfaceLayout.TABS; i++) {
            final int tx = x + CraftingInterfaceLayout.tabX(i);
            final int ty = y + CraftingInterfaceLayout.TAB_Y;
            final int tw = CraftingInterfaceLayout.tabW(i);
            final boolean chosen = i == tab;
            if (chosen) {
                JsTechTheme.selectedTab(g, tx, ty, tw, CraftingInterfaceLayout.TAB_H);
                Draw.outline(g, tx, ty, tw, CraftingInterfaceLayout.TAB_H, JsTechTheme.accent());
            } else {
                JsTechTheme.button(g, tx, ty, tw, CraftingInterfaceLayout.TAB_H, BusDraw.inside(mouseX, mouseY, tx, ty,
                        tw, CraftingInterfaceLayout.TAB_H));
                Draw.outline(g, tx, ty, tw, CraftingInterfaceLayout.TAB_H, JsTechTheme.line());
            }
            final String word = BusDraw.clip(font, GameText.resolve(TAB_WORDS[i]), tw - 4);
            BusDraw.smallCentered(g, font, word, tx + tw / 2, ty + 2,
                    chosen ? JsTechTheme.tabLabelOn() : JsTechTheme.dim());
        }
    }

    private void toggle(final GuiGraphics g, final int x, final int y, final int px, final int py, final int chosen,
                        final TextKey... words) {
        final int[] widths = optionWidths(words);
        final int[] at = CraftingInterfaceLayout.options(CraftingInterfaceLayout.CONTROL_X, widths);
        for (int i = 0; i < words.length; i++) {
            BusDraw.option(g, font, GameText.resolve(words[i]), x + at[i], y, widths[i], i == chosen,
                    over(px, py, at[i], 0, widths[i], CraftingInterfaceLayout.CONTROL_H));
        }
    }

    /* Which option of a toggle of {@code words} the point is over, or -1. */
    private int optionAt(final int x, final int y, final TextKey... words) {
        final int[] widths = optionWidths(words);
        final int[] at = CraftingInterfaceLayout.options(CraftingInterfaceLayout.CONTROL_X, widths);
        for (int i = 0; i < words.length; i++) {
            if (over(x, y, at[i], 0, widths[i], CraftingInterfaceLayout.CONTROL_H)) {
                return i;
            }
        }
        return -1;
    }

    private int[] optionWidths(final TextKey... words) {
        final int[] widths = new int[words.length];
        for (int i = 0; i < words.length; i++) {
            widths[i] = BusDraw.optionWidth(font, GameText.resolve(words[i]));
        }
        return widths;
    }

    /* The mark of a setting a program set, after the toggle, when there is room. */
    private void mark(final GuiGraphics g, final InterfaceView s, final String setting, final int x, final int y) {
        final String program = s.marks().getOrDefault(setting, "");
        if (program.isEmpty()) {
            return;
        }
        final int[] widths = optionWidths(InterfaceTexts.EXCLUSIVE, InterfaceTexts.NOT_EXCLUSIVE);
        final int end = CraftingInterfaceLayout.CONTROL_X + widths[0] + widths[1] + 8;
        if (end < CraftingInterfaceLayout.RIGHT - 10) {
            BusDraw.mark(g, font, program, x + end, y + 2, CraftingInterfaceLayout.RIGHT - end);
        }
    }

    private void label(final GuiGraphics g, final TextKey word, final int x, final int y) {
        BusDraw.fitted(g, font, GameText.resolve(word), x + CraftingInterfaceLayout.LABEL_X, y, JsTechTheme.dim(),
                CraftingInterfaceLayout.CONTROL_X - CraftingInterfaceLayout.LABEL_X - 4);
    }

    private void paragraph(final GuiGraphics g, final String text, final int x, final int y) {
        final List<String> lines = BusDraw.lines(font, text, CraftingInterfaceLayout.ROW_W);
        for (int i = 0; i < lines.size(); i++) {
            BusDraw.small(g, font, lines.get(i), x + CraftingInterfaceLayout.LABEL_X,
                    y + 1 + i * CraftingInterfaceLayout.LINE, JsTechTheme.dim());
        }
    }

    /* A row's word and its value wrapped under the value's column. */
    private void valueLines(final GuiGraphics g, final TextKey word, final String value, final int x, final int y,
                            final int colour) {
        label(g, word, x, y + 2);
        final List<String> lines = BusDraw.lines(font, value, CraftingInterfaceLayout.RIGHT
                - CraftingInterfaceLayout.CONTROL_X);
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            BusDraw.small(g, font, lines.get(i), x + CraftingInterfaceLayout.CONTROL_X,
                    y + 2 + i * CraftingInterfaceLayout.LINE, colour);
        }
    }

    private List<String> warningLines(final Text warning) {
        return BusDraw.lines(font, GameText.resolve(warning), CraftingInterfaceLayout.ROW_W - 6);
    }

    /* The rows as the interface is now: its patterns, the inputs of the one picked, its paragraphs and warnings. */
    private CraftingInterfaceLayout.Shape shape() {
        final InterfaceView s = menu.state();
        if (s == null) {
            return new CraftingInterfaceLayout.Shape(capacityOf(menu.era()), 1, 0, 1, 1, List.of(), 1);
        }
        final InterfaceView.PatternView pattern = pickedPattern(s);
        final List<Integer> warnings = new ArrayList<>();
        s.warnings().forEach(warning -> warnings.add(Math.max(1, warningLines(warning).size())));
        final int valueRoom = CraftingInterfaceLayout.RIGHT - CraftingInterfaceLayout.CONTROL_X;
        return new CraftingInterfaceLayout.Shape(s.capacity(), lines(GameText.resolve(InterfaceTexts.PATTERN_NOTE),
                CraftingInterfaceLayout.ROW_W), pattern == null ? 0 : pattern.inputs().size(),
                lines(GameText.resolve(s.exclusive() ? InterfaceTexts.EXCLUSIVE_NOTE : InterfaceTexts.SHARED_NOTE),
                        CraftingInterfaceLayout.ROW_W),
                Math.min(2, lines(GameText.resolve(s.feeds()), valueRoom)), warnings,
                Math.min(2, lines(GameText.resolve(s.now()), valueRoom)));
    }

    private static int capacityOf(final HardwareEra era) {
        return CraftingEras.patternsPerInterface(era);
    }

    private int lines(final String text, final int room) {
        return Math.max(1, BusDraw.lines(font, text, room).size());
    }

    @Nullable
    private InterfaceView.PatternView pickedPattern(final InterfaceView s) {
        if (s.patterns().isEmpty()) {
            return null;
        }
        picked = Math.min(picked, s.patterns().size() - 1);
        return s.patterns().get(picked);
    }

    private int heightOf(final int shown) {
        return switch (shown) {
            case CraftingInterfaceLayout.TAB_ACTIVITY -> CraftingInterfaceLayout.ACTIVITY_HEIGHT;
            case CraftingInterfaceLayout.TAB_SOFTWARE -> CraftingInterfaceLayout.softwareHeight(softwareContent);
            default -> CraftingInterfaceLayout.configureHeight(shape());
        };
    }

    private boolean linked() {
        return menu.state() != null && menu.state().linked();
    }

    private void onNameChanged(final String value) {
        nameValue = value;
        send(new CraftingInterfaceEditPayload(CraftingInterfaceEditPayload.NAME, 0, 0, 0, value));
    }

    private static void send(final CraftingInterfaceEditPayload edit) {
        PacketDistributor.sendToServer(edit);
    }

    private static boolean over(final int px, final int py, final int x, final int y, final int w, final int h) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    /* A section of the Software tab: its word, its lines, and whether they are code. */
    private record Section(String label, List<String> lines, boolean code) {
    }
}
