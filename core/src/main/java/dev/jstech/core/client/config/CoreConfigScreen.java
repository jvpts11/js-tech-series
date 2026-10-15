/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.config;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.client.gui.component.Button;
import dev.jstech.core.client.gui.component.Checkbox;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.ScrollPanel;
import dev.jstech.core.client.gui.component.SearchField;
import dev.jstech.core.client.gui.component.Texts;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.style.StyleSkin;
import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.client.guide.GuideClient;
import dev.jstech.core.config.ConfigDraft;
import dev.jstech.core.config.ConfigFile;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigPresets;
import dev.jstech.core.config.ConfigScreenTexts;
import dev.jstech.core.config.ConfigSync;
import dev.jstech.core.config.format.ConfigFormatException;
import dev.jstech.core.gui.layout.ConfigScreenLayout;
import dev.jstech.core.gui.style.Style;
import dev.jstech.core.gui.style.StylePiece;
import dev.jstech.core.gui.style.StyleRole;
import dev.jstech.core.gui.style.StyleSprite;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The settings screen of a mod that keeps its settings the Core's way: a menu over the whole game, drawn through the
 * style the player wears, the game's own by default. Across the top, a tab for each kind of file the mod keeps (the
 * world's, the player's, every game's), and under them a banner when there is something to say: an operator changing a
 * server's settings, a world's settings only read here, a rule between settings broken. Down the left, the open tab's
 * sections, each with how many settings it holds and a mark while one of them is changed. On the right, the search,
 * the filter of what changed and the presets, then the section's settings in groups that fold away, each with the
 * control its kind of value calls for and, while it is off its default, a button back to it. The pointer resting on a
 * setting's name shows its whole description, with its picture and its link to the manual; a click pins it. The footer
 * puts the section back to its defaults, undoes and redoes, says how many changes wait and how many of them wait for a
 * restart, writes the settings out as text and takes them back, and keeps every change (Done).
 *
 * <p>Everything is reached by the keyboard too: Tab and Shift+Tab, or the arrows up and down, go from control to
 * control; Enter or Space presses; Ctrl+Tab goes to the next tab, Ctrl+F to the search, Ctrl+Z and Ctrl+Y undo and
 * redo; Escape leaves without keeping anything.
 *
 * <p>A mod hands this to the game as its settings screen:
 * {@code container.registerExtensionPoint(IConfigScreenFactory.class, CoreConfigScreen::new)}, or with its mark,
 * {@code (mod, parent) -> new CoreConfigScreen(mod, parent, MARK)}.
 */
public final class CoreConfigScreen extends Screen {

    @Nullable
    private final Screen parent;
    private final RowsHost host = new RowsHost();
    private final String modId;
    private final String modName;
    private final IConfigBadge badge;
    private final ConfigScreenModel model;
    private final ConfigStatus status;
    private final ConfigKeyboard keyboard;
    private final Set<String> folded = new HashSet<>();
    private final Panel root = new Panel();
    private final Panel fixed = new Panel();
    private final ScrollPanel list = new ScrollPanel();
    private final List<ConfigRailEntry> rail = new ArrayList<>();
    private final List<ConfigRows.Row> rows = new ArrayList<>();
    private final SearchField searchField = new SearchField(64);
    private final Checkbox filter;
    private final Button presets;
    private final List<Button> footerLeft = new ArrayList<>();
    private final List<Button> footerRight = new ArrayList<>();
    @Nullable
    private Popup popup;
    private StyleSkin skin;
    private StyleSkin windowSkin;
    private ConfigScreenLayout.Footer footer;
    private int scope;
    private int section;
    private String query = "";
    private boolean onlyChanged;
    private boolean rebuildDue = true;
    private int mouseX;
    private int mouseY;

    /** The mod's mark in the header's left corner. */
    private static final int BADGE = 16;
    /** How far over the screen a dialog, and a description under the pointer, are lifted. */
    private static final float FRONT = 400f;

    public CoreConfigScreen(final ModContainer container, @Nullable final Screen parent) {
        this(container, parent, IConfigBadge.CHIP);
    }

    public CoreConfigScreen(final ModContainer container, @Nullable final Screen parent, final IConfigBadge badge) {
        super(GameText.component(ConfigScreenTexts.TITLE.with(container.getModInfo().getDisplayName())));
        this.parent = parent;
        this.modId = container.getModId();
        this.modName = container.getModInfo().getDisplayName();
        this.badge = badge;
        this.model = new ConfigScreenModel(modId);
        this.status = new ConfigStatus(model);
        this.keyboard = new ConfigKeyboard(root, this::focusOrder, () -> rail, this::scrollTo);
        this.searchField.setPlaceholder(GameText.resolve(ConfigScreenTexts.SEARCH));
        this.searchField.setRevertOnEscape(false);
        this.searchField.setOnEdit(() -> setQuery(searchField.edit()));
        this.searchField.setOnCommit(this::setQuery);
        this.filter = new Checkbox(() -> GameText.resolve(ConfigScreenTexts.ONLY_CHANGED), () -> onlyChanged,
                () -> setOnlyChanged(!onlyChanged)).setBoxSize(ConfigScreenLayout.CHECKBOX).setFocusable(true);
        this.presets = new Button(GameText.resolve(ConfigScreenTexts.PRESETS), this::openPresets).setFocusable(true);
        footerLeft.add(new Button(GameText.resolve(ConfigScreenTexts.DEFAULTS), this::sectionDefaults));
        footerLeft.add(new Button(GameText.resolve(ConfigScreenTexts.UNDO), this::undo));
        footerLeft.add(new Button(GameText.resolve(ConfigScreenTexts.REDO), this::redo));
        footerRight.add(new Button(GameText.resolve(ConfigScreenTexts.EXPORT), this::openExport));
        footerRight.add(new Button(GameText.resolve(ConfigScreenTexts.IMPORT), this::openImport));
        footerRight.add(new Button(GameText.resolve(ConfigScreenTexts.DONE), this::done));
        root.add(fixed);
        root.add(list);
        fixed.add(searchField);
        fixed.add(filter);
        fixed.add(presets);
        for (final Button button : footerLeft) {
            fixed.add(button.setFocusable(true));
        }
        for (final Button button : footerRight) {
            fixed.add(button.setFocusable(true));
        }
        list.setStep(ConfigScreenLayout.ROW);
    }

    // What a test reads and does, as a player would.

    /** The tabs, one for each kind of file the mod keeps. */
    public List<String> scopeLabels() {
        return model.scopes().stream().map(one -> ConfigScreenModel.tabTitle(one.side()).getString()).toList();
    }

    /** The sections the rail lists under the open tab. */
    public List<String> railLabels() {
        return openScope().map(one -> one.sections().stream().map(entry -> entry.title().getString()).toList())
                .orElse(List.of());
    }

    /** The names of the settings the list shows, however far it scrolls. */
    public List<String> rowNames() {
        refresh();
        final List<String> out = new ArrayList<>();
        for (final ConfigRows.Row row : rows) {
            row.key().ifPresent(key -> out.add(model.name(key).getString()));
        }
        return out;
    }

    /** Opens the tab {@code index}, as a click on it does. */
    public void openTab(final int index) {
        if (index >= 0 && index < model.scopes().size()) {
            scope = index;
            section = 0;
            rebuildDue = true;
        }
    }

    /** Opens the section called {@code title}, under whichever tab holds it, as a click on it does. */
    public boolean openSection(final String title) {
        for (int s = 0; s < model.scopes().size(); s++) {
            final List<ConfigScreenModel.Section> sections = model.scopes().get(s).sections();
            for (int i = 0; i < sections.size(); i++) {
                if (sections.get(i).title().getString().equals(title)) {
                    scope = s;
                    select(i);
                    return true;
                }
            }
        }
        return false;
    }

    /** Finds settings by their names and what they do, as typing in the search does. */
    public void search(final String words) {
        searchField.set(words);
        setQuery(words);
    }

    /** Shows only the settings off their defaults, or every one, as the filter's box does. */
    public void setOnlyChanged(final boolean only) {
        onlyChanged = only;
        rebuildDue = true;
    }

    /** Folds the group called {@code title} of the open section, or opens it, as a click on its header does. */
    public boolean foldGroup(final String title) {
        final Optional<ConfigScreenModel.Section> open = openSection();
        if (open.isEmpty()) {
            return false;
        }
        for (final ConfigScreenModel.Group group : open.get().groups()) {
            if (group.title().getString().equals(title)) {
                toggleFold(open.get().file(), group.path());
                return true;
            }
        }
        return false;
    }

    /** The draft of the file the open section belongs to, for a test to change a setting as a click would. */
    @Nullable
    public ConfigDraft openDraft() {
        return openSection().map(open -> model.draft(open.file())).orElse(null);
    }

    /** What the banner under the tabs says, or nothing. */
    public String bannerText() {
        return banner().map(ConfigStatus.Line::text).orElse("");
    }

    /** What the footer says of the changes. */
    public String statusText() {
        return status.footer().text();
    }

    /** The dialog open over the screen, or none. */
    @Nullable
    public Popup openPopup() {
        return popup;
    }

    /** Presses the button labelled {@code label} over or under the list (Presets..., Done), as a click does. */
    public boolean press(final String label) {
        for (final UiComponent part : fixed.children()) {
            if (part instanceof Button button && button.label().equals(label) && button.enabled()) {
                button.press();
                return true;
            }
        }
        return false;
    }

    /**
     * Presses the button labelled {@code label} in the rows of the setting called {@code setting}, its own or one of
     * its entries' (+ Add), as a click does.
     */
    public boolean pressFor(final String setting, final String label) {
        refresh();
        boolean under = false;
        for (final ConfigRows.Row row : rows) {
            if (row.key().isPresent()) {
                under = model.name(row.key().get()).getString().equals(setting);
            }
            if (!under) {
                continue;
            }
            for (final UiComponent part : row.parts) {
                if (part instanceof Button button && button.label().equals(label)) {
                    button.press();
                    return true;
                }
            }
        }
        return false;
    }

    /** Takes the open section's last change back, as Undo does. */
    public void undo() {
        openDraftIfEditable().ifPresent(draft -> {
            draft.undo();
            rebuildDue = true;
        });
    }

    /** Takes it again, as Redo does. */
    public void redo() {
        openDraftIfEditable().ifPresent(draft -> {
            draft.redo();
            rebuildDue = true;
        });
    }

    /** Keeps every change and goes back, as Done does: in place, or sent to the server that keeps the file. */
    public void done() {
        if (!status.brokenRules().isEmpty()) {
            return;
        }
        for (final Map.Entry<ConfigFile, ConfigDraft> entry : model.drafts().entrySet()) {
            final ConfigDraft draft = entry.getValue();
            switch (ConfigScreenModel.access(entry.getKey())) {
                case LOCAL -> draft.apply();
                case REMOTE -> {
                    if (draft.changeCount() > 0) {
                        ConfigSync.sendEdits(entry.getKey(), draft.pendingPlain());
                    }
                    draft.discard();
                }
                case READ_ONLY -> draft.discard();
            }
        }
        back();
    }

    // The game's screen.

    @Override
    public void renderBackground(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        final Style style = skin.style();
        if (style.piece(StylePiece.MENU_GROUND) instanceof StyleSprite.GameMenu) {
            super.renderBackground(g, mouseX, mouseY, partialTick);
        } else {
            skin.piece(g, StylePiece.MENU_GROUND, 0, 0, width, height);
        }
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        refresh();
        place();
        super.render(g, mouseX, mouseY, partialTick);
        // Under a dialog nothing of the screen is lit by the pointer.
        final UiContext ctx = new UiContext(skin, font, popup == null ? mouseX : -1, popup == null ? mouseY : -1,
                partialTick);
        drawHeader(g);
        final boolean hasBanner = banner().isPresent();
        final int bottom = ConfigScreenLayout.contentBottom(footer);
        final int railTop = ConfigScreenLayout.railTop(hasBanner);
        final int listTop = ConfigScreenLayout.listTop(hasBanner);
        skin.piece(g, StylePiece.LIST_GROUND, ConfigScreenLayout.PAD, railTop - 2, ConfigScreenLayout.railWidth(width),
                bottom - railTop + 2);
        skin.piece(g, StylePiece.LIST_GROUND, ConfigScreenLayout.paneX(width), listTop,
                ConfigScreenLayout.paneRight(width) - ConfigScreenLayout.paneX(width), bottom - listTop);
        for (final ConfigRailEntry entry : rail) {
            entry.render(g, ctx);
        }
        root.render(g, ctx);
        drawFooter(g);
        final UiComponent focused = keyboard.focused();
        if (focused != null && focused.visible()) {
            Draw.outline(g, focused.x() - 1, focused.y() - 1, focused.width() + 2, focused.height() + 2,
                    skin.colour(StyleRole.MENU_TEXT));
        }
        g.pose().pushPose();
        g.pose().translate(0, 0, FRONT);
        if (popup != null) {
            // A pinned description wears the menu's look; every other dialog is a window of the game's.
            popup.renderIn(g, new UiContext(popup instanceof ConfigDescription ? skin : windowSkin, font, mouseX,
                    mouseY, partialTick), 0, 0, width, height);
            final List<Component> tip = popup.tooltip(mouseX, mouseY);
            if (!tip.isEmpty()) {
                g.renderComponentTooltip(font, tip, mouseX, mouseY);
            }
        } else {
            drawTooltip(g, ctx);
        }
        g.pose().popPose();
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        if (popup != null) {
            popup.mouseClicked(mx, my, button);
            closeIfDone();
            return true;
        }
        if (clickTabs(mx, my)) {
            return true;
        }
        for (final ConfigRailEntry entry : rail) {
            if (entry.visible() && entry.contains(mx, my)) {
                keyboard.release();
                entry.select();
                return true;
            }
        }
        return root.mouseClicked(mx, my, button) || super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseReleased(final double mx, final double my, final int button) {
        model.drafts().values().forEach(ConfigDraft::endSlide);
        if (popup != null) {
            popup.mouseReleased(mx, my, button);
            return true;
        }
        return root.mouseReleased(mx, my, button) || super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseDragged(final double mx, final double my, final int button, final double dragX,
                                final double dragY) {
        if (popup != null) {
            popup.mouseDragged(mx, my, button);
            return true;
        }
        return root.mouseDragged(mx, my, button) || super.mouseDragged(mx, my, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(final double mx, final double my, final double scrollX, final double scrollY) {
        if (popup != null) {
            popup.mouseScrolled(mx, my, scrollY);
            return true;
        }
        return root.mouseScrolled(mx, my, scrollY);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (popup != null) {
            popup.keyPressed(key, scanCode, modifiers);
            closeIfDone();
            return true;
        }
        final boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        final boolean shift = (modifiers & GLFW.GLFW_MOD_SHIFT) != 0;
        if (key == InputConstants.KEY_ESCAPE) {
            if (root.overlayOpen()) {
                root.closeOverlay();
            } else if (keyboard.typing()) {
                keyboard.release();
            } else {
                onClose();
            }
            return true;
        }
        if (control && shortcut(key, shift)) {
            return true;
        }
        final Optional<ConfigRailEntry> onRail = keyboard.railFocus();
        if (onRail.isPresent() && (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER
                || key == GLFW.GLFW_KEY_SPACE)) {
            onRail.get().select();
            return true;
        }
        if (root.keyPressed(key, scanCode, modifiers)) {
            return true;
        }
        if (key == GLFW.GLFW_KEY_TAB || key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_UP) {
            keyboard.move(key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_TAB && shift ? -1 : 1);
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(final char c, final int modifiers) {
        if (popup != null) {
            popup.charTyped(c);
            return true;
        }
        return root.charTyped(c) || super.charTyped(c, modifiers);
    }

    /* Leaving any other way than Done drops what changed. */
    @Override
    public void onClose() {
        back();
    }

    @Override
    public void removed() {
        root.dispose();
        super.removed();
    }

    @Override
    protected void init() {
        final Style style = Styles.forMod(modId);
        skin = Styles.menuSkin(style);
        windowSkin = Styles.skin(style);
        rebuildDue = true;
        refresh();
    }

    // The screen's own work.

    private void back() {
        if (minecraft != null) {
            minecraft.setScreen(parent);
        }
    }

    private Optional<ConfigScreenModel.Scope> openScope() {
        return scope < model.scopes().size() ? Optional.of(model.scopes().get(scope)) : Optional.empty();
    }

    private Optional<ConfigScreenModel.Section> openSection() {
        return openScope().filter(one -> section < one.sections().size()).map(one -> one.sections().get(section));
    }

    private Optional<ConfigDraft> openDraftIfEditable() {
        return openSection().filter(open -> ConfigScreenModel.editable(open.file()))
                .map(open -> model.draft(open.file()));
    }

    private Optional<ConfigStatus.Line> banner() {
        return status.banner(openSection(), minecraft != null && minecraft.player != null);
    }

    private void select(final int index) {
        section = index;
        list.setScroll(0);
        rebuildDue = true;
    }

    private void setQuery(final String words) {
        final String trimmed = words.trim();
        if (!trimmed.equals(query)) {
            query = trimmed;
            list.setScroll(0);
            rebuildDue = true;
        }
    }

    private void toggleFold(final ConfigFile file, final String group) {
        final String id = file.name() + ":" + group;
        if (!folded.remove(id)) {
            folded.add(id);
        }
        rebuildDue = true;
    }

    /* Builds the rail and the list again when what they show changed. */
    private void refresh() {
        if (!rebuildDue || skin == null) {
            return;
        }
        rebuildDue = false;
        rail.clear();
        openScope().ifPresent(one -> {
            for (int i = 0; i < one.sections().size(); i++) {
                final int index = i;
                final ConfigScreenModel.Section entry = one.sections().get(i);
                rail.add(new ConfigRailEntry(i, entry, () -> index == section && query.isEmpty(),
                        () -> changedIn(entry), () -> select(index)));
            }
        });
        list.clear();
        rows.clear();
        final ConfigRows.ShownFilter shown = (file, key) -> model.matches(key, query)
                && (!onlyChanged || model.draft(file).isOffDefault(key) || model.draft(file).isChanged(key));
        if (query.isEmpty()) {
            openSection().ifPresent(open -> rows.addAll(ConfigRows.build(host, open.file(), open.groups(), shown)));
        } else {
            // A search looks through every tab, each section's finds under its name.
            for (final ConfigScreenModel.Scope one : model.scopes()) {
                for (final ConfigScreenModel.Section entry : one.sections()) {
                    final List<ConfigRows.Row> found = ConfigRows.build(host, entry.file(), entry.groups(), shown);
                    if (!found.isEmpty()) {
                        rows.add(ConfigRows.title(host, ConfigScreenModel.tabTitle(one.side()).getString() + " / "
                                + entry.title().getString(), true));
                        rows.addAll(found);
                    }
                }
            }
        }
        if (rows.isEmpty()) {
            rows.add(ConfigRows.title(host, GameText.resolve(onlyChanged && query.isEmpty()
                    ? ConfigScreenTexts.NONE_CHANGED : ConfigScreenTexts.NO_RESULTS), false));
        }
        int top = 0;
        for (final ConfigRows.Row row : rows) {
            row.setTop(top);
            top += row.height();
            row.parts.forEach(list::add);
        }
        list.setContentHeight(top + ConfigScreenLayout.GROUP_GAP);
    }

    private boolean changedIn(final ConfigScreenModel.Section entry) {
        final ConfigDraft draft = model.draft(entry.file());
        for (final ConfigScreenModel.Group group : entry.groups()) {
            for (final ConfigKey<?> key : group.keys()) {
                if (draft.isChanged(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    /* Puts everything in place for this frame. */
    private void place() {
        final boolean hasBanner = banner().isPresent();
        final List<Integer> left = footerLeft.stream().map(button -> skin.textWidth(font, button.label())).toList();
        final List<Integer> right = footerRight.stream().map(button -> skin.textWidth(font, button.label()))
                .toList();
        footer = ConfigScreenLayout.footer(width, height, left, right, skin.textWidth(font, status.footer().text()));
        for (int i = 0; i < footerLeft.size(); i++) {
            footerLeft.get(i).setBounds(footer.left().get(i).x(), footer.buttonsY(), footer.left().get(i).width(),
                    ConfigScreenLayout.CONTROL_HEIGHT);
        }
        for (int i = 0; i < footerRight.size(); i++) {
            footerRight.get(i).setBounds(footer.right().get(i).x(), footer.buttonsY(), footer.right().get(i).width(),
                    ConfigScreenLayout.CONTROL_HEIGHT);
        }
        final Optional<ConfigScreenModel.Section> open = openSection();
        final boolean editable = open.map(entry -> ConfigScreenModel.editable(entry.file())).orElse(false);
        final Optional<ConfigDraft> draft = openDraftIfEditable();
        footerLeft.get(0).setEnabled(editable && query.isEmpty());
        footerLeft.get(1).setEnabled(draft.map(ConfigDraft::canUndo).orElse(false));
        footerLeft.get(2).setEnabled(draft.map(ConfigDraft::canRedo).orElse(false));
        footerRight.get(0).setEnabled(open.isPresent());
        footerRight.get(1).setEnabled(editable);
        footerRight.get(2).setEnabled(status.brokenRules().isEmpty());
        presets.setEnabled(editable);
        final ConfigScreenLayout.TopRow row = ConfigScreenLayout.topRow(width,
                skin.textWidth(font, GameText.resolve(ConfigScreenTexts.ONLY_CHANGED)),
                skin.textWidth(font, presets.label()));
        final int top = ConfigScreenLayout.contentTop(hasBanner);
        searchField.setBounds(row.search().x(), top, row.search().width(), ConfigScreenLayout.CONTROL_HEIGHT);
        filter.setBounds(row.filter().x(), top, row.filter().width(), ConfigScreenLayout.CONTROL_HEIGHT);
        presets.setBounds(row.presets().x(), top, row.presets().width(), ConfigScreenLayout.CONTROL_HEIGHT);
        final int bottom = ConfigScreenLayout.contentBottom(footer);
        final int railTop = ConfigScreenLayout.railTop(hasBanner);
        for (final ConfigRailEntry entry : rail) {
            final int y = railTop + entry.index() * ConfigScreenLayout.RAIL_ITEM;
            entry.setBounds(ConfigScreenLayout.PAD + 2, y, ConfigScreenLayout.railWidth(width) - 4,
                    ConfigScreenLayout.RAIL_ITEM);
            entry.setVisible(y + ConfigScreenLayout.RAIL_ITEM <= bottom);
        }
        final int listTop = ConfigScreenLayout.listTop(hasBanner);
        list.setBounds(ConfigScreenLayout.paneX(width), listTop,
                ConfigScreenLayout.paneRight(width) - ConfigScreenLayout.paneX(width), bottom - listTop);
        for (final ConfigRows.Row each : rows) {
            each.place(list.contentY(each.top()));
        }
    }

    private void drawHeader(final GuiGraphics g) {
        final int tabs = model.scopes().size();
        for (int i = 0; i < tabs; i++) {
            skin.tab(g, font, ConfigScreenLayout.tabX(width, i, tabs), 0, ConfigScreenLayout.tabWidth(width, tabs),
                    ConfigScreenLayout.HEADER, ConfigScreenModel.tabTitle(model.scopes().get(i).side()).getString(),
                    i == scope);
        }
        skin.separator(g, 0, ConfigScreenLayout.HEADER, width);
        // The mod's mark and name in the header's left corner, when the tabs leave room for them.
        final int room = (tabs == 0 ? width / 2 : ConfigScreenLayout.tabX(width, 0, tabs)) - 2 * ConfigScreenLayout.PAD;
        if (room >= BADGE) {
            badge.draw(g, ConfigScreenLayout.PAD, (ConfigScreenLayout.HEADER - BADGE) / 2, BADGE, skin.text());
            final int nameRoom = room - BADGE - ConfigScreenLayout.GAP;
            if (nameRoom >= font.width(modName.substring(0, Math.min(3, modName.length())))) {
                skin.drawText(g, font, Texts.clip(font, modName, nameRoom),
                        ConfigScreenLayout.PAD + BADGE + ConfigScreenLayout.GAP, (ConfigScreenLayout.HEADER - 8) / 2,
                        skin.text());
            }
        }
        banner().ifPresent(said -> {
            final int ink = ConfigWidgets.colour(skin, said.role());
            int x = ConfigScreenLayout.PAD + 2;
            if (said.lock()) {
                lock(g, x, ConfigScreenLayout.BANNER_Y, ink);
                x += 12;
            }
            skin.drawText(g, font, Texts.clip(font, said.text(), width - x - ConfigScreenLayout.PAD), x,
                    ConfigScreenLayout.BANNER_Y + 1, ink);
        });
    }

    private void drawFooter(final GuiGraphics g) {
        final StyleSprite line = skin.style().piece(StylePiece.SEPARATOR_FOOTER);
        skin.piece(g, StylePiece.SEPARATOR_FOOTER, 0, footer.top(), width,
                line instanceof StyleSprite.Tile tile ? tile.height() : 1);
        final ConfigStatus.Line said = status.footer();
        skin.drawText(g, font, said.text(), footer.statusX(), footer.statusY(), ConfigWidgets.colour(skin,
                said.role()));
    }

    /* The description of the setting whose name the pointer rests on, or the tooltip of the control under it. */
    private void drawTooltip(final GuiGraphics g, final UiContext ctx) {
        if (list.contains(mouseX, mouseY)) {
            for (final ConfigRows.Row row : rows) {
                final Optional<UiComponent> part = row.describedPart();
                if (part.isPresent() && part.get().contains(mouseX, mouseY) && row.key().isPresent()) {
                    final ConfigDescription.Card card = ConfigDescription.cardOf(model, row.key().get(), font,
                            model.meta(row.key().get()));
                    ConfigDescription.draw(g, ctx, card, Math.max(2, Math.min(width - card.width() - 2, mouseX + 10)),
                            Math.max(2, Math.min(height - card.height() - 2, mouseY + 8)));
                    return;
                }
            }
        }
        final List<Component> tip = root.tooltip(mouseX, mouseY);
        if (!tip.isEmpty()) {
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        } else if (filter.contains(mouseX, mouseY)) {
            g.renderComponentTooltip(font, List.of(GameText.component(ConfigScreenTexts.ONLY_CHANGED_TIP)), mouseX,
                    mouseY);
        } else if (footerLeft.get(0).contains(mouseX, mouseY)) {
            g.renderComponentTooltip(font, List.of(GameText.component(ConfigScreenTexts.DEFAULTS_TIP)), mouseX,
                    mouseY);
        }
    }

    private void show(final Popup opened) {
        keyboard.release();
        root.closeOverlay();
        popup = opened;
        opened.open();
        opened.placeIn(0, 0, width, height);
    }

    private void closeIfDone() {
        if (popup != null && !popup.isOpen()) {
            popup = null;
            rebuildDue = true;
        }
    }

    private void sectionDefaults() {
        openSection().filter(open -> ConfigScreenModel.editable(open.file()) && query.isEmpty()).ifPresent(open -> {
            final List<ConfigKey<?>> keys = new ArrayList<>();
            open.groups().forEach(group -> keys.addAll(group.keys()));
            model.draft(open.file()).defaults(keys);
            rebuildDue = true;
        });
    }

    private void openPresets() {
        openSection().filter(open -> ConfigScreenModel.editable(open.file())).ifPresent(open -> {
            final ConfigDraft draft = model.draft(open.file());
            show(new ConfigPresetsPopup(open.file(), draft::toPlain, (title, values) -> {
                final int taken = draft.takePlain(values);
                status.say(GameText.resolve(ConfigScreenTexts.PRESET_TAKEN.with(title, taken)), false);
                rebuildDue = true;
            }));
        });
    }

    private void openExport() {
        openSection().ifPresent(open -> show(ConfigTextPopup.export(ConfigPresets.export(open.file(),
                model.draft(open.file()).toPlain()))));
    }

    private void openImport() {
        openSection().filter(open -> ConfigScreenModel.editable(open.file())).ifPresent(open -> {
            final ConfigDraft draft = model.draft(open.file());
            show(ConfigTextPopup.importing(text -> {
                final Map<String, Object> values;
                try {
                    values = ConfigPresets.importText(open.file(), text);
                } catch (final ConfigFormatException unreadable) {
                    throw new IllegalArgumentException(unreadable.getMessage(), unreadable);
                }
                final String said = GameText.resolve(ConfigScreenTexts.IMPORTED.with(draft.takePlain(values)));
                status.say(said, false);
                rebuildDue = true;
                return said;
            }));
        });
    }

    private boolean clickTabs(final double mx, final double my) {
        if (my < 0 || my >= ConfigScreenLayout.HEADER) {
            return false;
        }
        final int tabs = model.scopes().size();
        for (int i = 0; i < tabs; i++) {
            final int x = ConfigScreenLayout.tabX(width, i, tabs);
            if (mx >= x && mx < x + ConfigScreenLayout.tabWidth(width, tabs)) {
                openTab(i);
                return true;
            }
        }
        return false;
    }

    /* The shortcuts held with Ctrl: the next tab, the search, undo and redo. */
    private boolean shortcut(final int key, final boolean shift) {
        switch (key) {
            case GLFW.GLFW_KEY_TAB -> {
                final int tabs = model.scopes().size();
                if (tabs > 0) {
                    openTab(Math.floorMod(scope + (shift ? -1 : 1), tabs));
                }
                return true;
            }
            case GLFW.GLFW_KEY_F -> {
                keyboard.focus(fixed, searchField);
                return true;
            }
            case GLFW.GLFW_KEY_Z -> {
                if (keyboard.typing()) {
                    return false;
                }
                if (shift) {
                    redo();
                } else {
                    undo();
                }
                return true;
            }
            case GLFW.GLFW_KEY_Y -> {
                if (keyboard.typing()) {
                    return false;
                }
                redo();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    /* The controls the keyboard goes through, in the order they read: the rail, the row over the list, the list, the
       footer. */
    private List<ConfigKeyboard.Stop> focusOrder() {
        final List<ConfigKeyboard.Stop> out = new ArrayList<>();
        for (final ConfigRailEntry entry : rail) {
            if (entry.visible()) {
                out.add(new ConfigKeyboard.Stop(null, entry));
            }
        }
        out.add(new ConfigKeyboard.Stop(fixed, searchField));
        out.add(new ConfigKeyboard.Stop(fixed, filter));
        if (presets.enabled()) {
            out.add(new ConfigKeyboard.Stop(fixed, presets));
        }
        for (final ConfigRows.Row row : rows) {
            for (final UiComponent part : row.parts) {
                if (part.focusable() && part.visible() && part.enabled()) {
                    out.add(new ConfigKeyboard.Stop(list, part));
                }
            }
        }
        for (final Button button : footerLeft) {
            if (button.enabled()) {
                out.add(new ConfigKeyboard.Stop(fixed, button));
            }
        }
        for (final Button button : footerRight) {
            if (button.enabled()) {
                out.add(new ConfigKeyboard.Stop(fixed, button));
            }
        }
        return out;
    }

    /* Scrolls the list so the row of a control the keyboard reached is in view. */
    private void scrollTo(final UiComponent component) {
        for (final ConfigRows.Row row : rows) {
            if (!row.parts.contains(component)) {
                continue;
            }
            if (row.top() < list.scroll()) {
                list.setScroll(row.top());
            } else if (row.top() + row.height() > list.scroll() + list.height()) {
                list.setScroll(row.top() + row.height() - list.height());
            }
        }
    }

    private static void lock(final GuiGraphics g, final int x, final int y, final int colour) {
        Draw.outline(g, x + 1, y, 5, 4, colour);
        g.fill(x, y + 3, x + 7, y + 9, colour);
    }

    /** What the rows ask of the screen. */
    private final class RowsHost implements ConfigRows.Host {

        @Override
        public ConfigScreenModel model() {
            return model;
        }

        @Override
        public Font font() {
            return font;
        }

        @Override
        public StyleSkin skin() {
            return skin;
        }

        @Override
        public int screenWidth() {
            return width;
        }

        @Override
        public boolean editable(final ConfigFile file) {
            return ConfigScreenModel.editable(file);
        }

        @Override
        public boolean folded(final ConfigFile file, final String group) {
            return folded.contains(file.name() + ":" + group);
        }

        @Override
        public void toggleFold(final ConfigFile file, final String group) {
            CoreConfigScreen.this.toggleFold(file, group);
        }

        @Override
        public void pick(final ConfigThings.Kind kind, final Consumer<String> chosen) {
            show(new ConfigPicker(kind, id -> {
                chosen.accept(id);
                rebuildDue = true;
            }));
        }

        @Override
        public void choose(final ConfigKey<?> key, final ConfigDraft draft) {
            if (minecraft != null) {
                minecraft.setScreen(ConfigChoosers.open(key, CoreConfigScreen.this, draft));
            }
        }

        @Override
        public void describe(final ConfigKey<?> key) {
            show(new ConfigDescription(ConfigDescription.cardOf(model, key, font, model.meta(key)),
                    page -> GuideClient.open(page.manual(), page.entry(), CoreConfigScreen.this), mouseX + 8,
                    mouseY + 6));
        }

        @Override
        public void rebuild() {
            rebuildDue = true;
        }
    }
}
