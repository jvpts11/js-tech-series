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
import dev.jstech.core.client.gui.component.Panel;
import dev.jstech.core.client.gui.component.TextField;
import dev.jstech.core.client.gui.component.UiComponent;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.client.gui.style.StyleSkin;
import dev.jstech.core.client.gui.style.Styles;
import dev.jstech.core.client.mcp.McpGame;
import dev.jstech.core.gui.layout.StudioLayout;
import dev.jstech.core.text.GameText;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

/**
 * The studio, in full screen over the world: its pages under a strip of tabs, the things of every mod on the Core and
 * each of its tools, with the way to what is open in the bar, the search and the count of checks; along the foot what
 * is on, and Reload all, Run its tests, Record a test and Done.
 */
public final class StudioScreen extends Screen {

    private final Panel panel = new Panel();
    private final TextField search = panel.add(new TextField(SEARCH_LENGTH));
    private final Button checksButton;
    private final Button reloadButton;
    private final Button runButton;
    private final Button recordButton;
    private final Button doneButton;
    private final Map<StudioTool, StudioPage> pages = new EnumMap<>(StudioTool.class);
    private final ThingsPage things;
    private final @Nullable BlockPos lookedAt;
    private StudioTool tool = StudioTool.THINGS;
    private StudioLayout.Regions regions = StudioLayout.of(StudioLayout.MIN_WIDTH, StudioLayout.MIN_HEIGHT,
            List.of());
    private String status;

    private static final int SEARCH_LENGTH = 64;
    private static final String CRUMB = "  >  ";
    private static final String FOOT_GAP = "    ";
    /** The level of operator the game's own {@code /reload} asks. */
    private static final int RELOADS = 2;
    /** How dark behind the tab that is open. */
    private static final int TAB_GROUND = 0x78 << 24;

    /** The page the studio was last left on, which it opens on again. */
    private static StudioTool last = StudioTool.THINGS;

    /** The studio, every thing read, its home open. */
    public StudioScreen() {
        super(GameText.component(StudioTexts.STUDIO));
        final Minecraft game = Minecraft.getInstance();
        this.lookedAt = game.hitResult instanceof BlockHitResult hit && hit.getType() == HitResult.Type.BLOCK
                ? hit.getBlockPos().immutable() : null;
        this.things = new ThingsPage(this);
        this.search.setPlaceholder(GameText.resolve(StudioTexts.SEARCH));
        this.search.setOnEdit(() -> {
            this.open(StudioTool.THINGS);
            this.things.search(this.search.edit());
        });
        this.checksButton = panel.add(new Button(() -> GameText.resolve(StudioTexts.CHECKS.with(
                this.things.problems())), () -> {
                    this.open(StudioTool.THINGS);
                    this.things.nextWithProblems();
                }));
        this.reloadButton = panel.add(new Button(() -> GameText.resolve(StudioTexts.RELOAD_ALL), this::reloadAll));
        this.runButton = panel.add(new Button(() -> GameText.resolve(StudioTexts.RUN_TESTS), this::runTests));
        this.recordButton = panel.add(new Button(() -> GameText.resolve(StudioTexts.RECORD), TestRecorder::start));
        this.doneButton = panel.add(new Button(() -> GameText.resolve(StudioTexts.DONE), this::onClose));
        this.status = TestRecorder.recording() ? GameText.resolve(StudioTexts.RECORDING)
                : CameraPathRecorder.recording() ? CameraPathRecorder.line() : GameText.resolve(StudioTexts.MODE_ON);
        this.pages.put(StudioTool.THINGS, this.things);
        this.pages.put(StudioTool.BLOCK, new BlockPage(this));
        this.pages.put(StudioTool.COST, new CostPage(this));
        this.pages.put(StudioTool.PAYLOADS, new PayloadsPage(this));
        this.pages.put(StudioTool.RECIPES, new RecipesPage(this));
        this.pages.put(StudioTool.TAGS, new TagsPage(this));
        this.pages.put(StudioTool.LOOT, new LootPage(this));
        this.pages.put(StudioTool.TRANSLATIONS, new TranslationsPage(this));
        this.pages.put(StudioTool.SETTINGS, new SettingsPage(this));
        this.pages.put(StudioTool.STRUCTURES, new StructuresPage(this));
        this.pages.put(StudioTool.CAMERAS, new CamerasPage(this));
        this.pages.put(StudioTool.TESTS, new TestsPage(this));
        this.pages.put(StudioTool.SAVE, new SavePage(this));
        this.pages.put(StudioTool.LOGS, new LogsPage(this));
        this.pages.put(StudioTool.AGENTS, new AgentsPage(this));
        this.things.read("");
        this.tool = last;
        this.page().shown();
    }

    /** Opens the page of {@code next}; for a test as for a click on its tab. */
    public void open(final StudioTool next) {
        if (next != this.tool) {
            this.tool = next;
            last = next;
            this.page().shown();
        }
    }

    /** The page open now. */
    public StudioTool tool() {
        return this.tool;
    }

    /** What the page open now lists, a line a row, as a test reads it. */
    public List<String> lines() {
        return this.page().lines();
    }

    /** The thing chosen, if any; for a test. */
    public Optional<StudioThing> chosen() {
        return this.things.chosen();
    }

    /** Chooses the thing of {@code id}, as a click on its row does; whether there is one. For a test as for a click. */
    public boolean choose(final ResourceLocation id) {
        return this.things.choose(id);
    }

    /**
     * Chooses the row of the page open now that says {@code what}, as a click on it does; whether it has one. For a
     * test as for a click.
     */
    public boolean choose(final String what) {
        return this.page().choose(what);
    }

    /** Types {@code text} into the field of the page open now that has the keyboard, as the player types. */
    public void type(final String text) {
        for (final char c : text.toCharArray()) {
            this.page().panel().charTyped(c);
        }
    }

    /**
     * Presses the button, of the page open now or of the studio's own, that says {@code label}, when it may be
     * pressed; whether there was one. For a test as for a click.
     */
    public boolean press(final String label) {
        for (final Panel each : List.of(this.page().panel(), panel)) {
            for (final UiComponent child : each.children()) {
                if (child instanceof Button button && button.visible() && button.enabled()
                        && button.label().equals(label)) {
                    button.press();
                    return true;
                }
            }
        }
        return false;
    }

    /** What the thing chosen is made of and what is wrong across its parts; empty for none chosen. */
    public Optional<StudioSheet> sheet() {
        return this.things.sheet();
    }

    /** How many problems every thing has, all told. */
    public int problems() {
        return this.things.problems();
    }

    /** Has a machine on the bench be in {@code state}; for a test as for a click on its button. */
    public void showState(final BenchState state) {
        this.things.showState(state);
    }

    /** What the foot says now. */
    public String status() {
        return this.status;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final StudioPalette.Colours colours = StudioPalette.get();
        this.bar(g, colours);
        this.strip(g, colours);
        final List<Component> tip = new ArrayList<>();
        this.page().render(g, mouseX, mouseY, tip);
        final StudioLayout.Rect foot = this.regions.foot();
        StudioDraw.footLine(g, foot.y() - 2, foot.width());
        // The MCP server, while it is on, beside what the foot says.
        final String said = McpGame.running().map(server -> this.status + FOOT_GAP + GameText.resolve(
                StudioTexts.MCP_LINE.with(server.port(), server.protocol().clients().all().size())))
                .orElse(this.status);
        StudioDraw.small(g, font, said, StudioDraw.INSET, foot.y() + (StudioLayout.FOOT - 6) / 2,
                this.regions.reload().x() - 2 * StudioDraw.INSET, colours.label());
        this.runButton.setEnabled(this.tool == StudioTool.THINGS && this.things.tested()
                && Minecraft.getInstance().getConnection() != null);
        final UiContext context = new UiContext(this.skin(), font, mouseX, mouseY, partialTick);
        panel.render(g, context);
        this.page().panel().render(g, context);
        if (this.tool == StudioTool.THINGS) {
            this.things.renderOver(g);
        }
        if (!tip.isEmpty()) {
            g.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        if (panel.mouseClicked(mouseX, mouseY, button)) {
            panel.mouseReleased(mouseX, mouseY, button);
            this.page().panel().focus(null);
            return true;
        }
        final Panel pagePanel = this.page().panel();
        if (pagePanel.mouseClicked(mouseX, mouseY, button)) {
            pagePanel.mouseReleased(mouseX, mouseY, button);
            panel.focus(null);
            return true;
        }
        final List<StudioLayout.Rect> tabs = this.regions.tabs();
        for (int i = 0; i < tabs.size(); i++) {
            if (tabs.get(i).contains(mouseX, mouseY)) {
                this.open(StudioTool.inOrder().get(i));
                return true;
            }
        }
        panel.focus(null);
        pagePanel.focus(null);
        return this.page().mouseClicked(mouseX, mouseY, button) || super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX,
                                final double dragY) {
        return this.page().mouseDragged(mouseX, mouseY, dragX, dragY)
                || this.page().panel().mouseDragged(mouseX, mouseY, button)
                || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        this.page().mouseReleased();
        return this.page().panel().mouseReleased(mouseX, mouseY, button)
                || super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        return this.page().panel().mouseScrolled(mouseX, mouseY, scrollY)
                || this.page().mouseScrolled(mouseX, mouseY, scrollY)
                || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(final int key, final int scanCode, final int modifiers) {
        if (key != GLFW.GLFW_KEY_ESCAPE) {
            if (panel.focusedChild() != null && panel.keyPressed(key, scanCode, modifiers)) {
                return true;
            }
            final Panel pagePanel = this.page().panel();
            if (pagePanel.focusedChild() != null && pagePanel.keyPressed(key, scanCode, modifiers)) {
                return true;
            }
            final boolean typing = panel.focusedChild() != null && panel.focusedChild().takesTyping()
                    || pagePanel.focusedChild() != null && pagePanel.focusedChild().takesTyping();
            if (!typing && this.page().keyPressed(key)) {
                return true;
            }
        }
        return super.keyPressed(key, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(final char c, final int modifiers) {
        if (panel.focusedChild() != null) {
            return panel.charTyped(c);
        }
        return this.page().panel().charTyped(c) || super.charTyped(c, modifiers);
    }

    @Override
    public void tick() {
        this.page().tick();
    }

    @Override
    public void removed() {
        this.pages.values().forEach(page -> {
            page.closed();
            page.panel().dispose();
        });
        panel.dispose();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    protected void init() {
        final List<Integer> labels = new ArrayList<>();
        for (final StudioTool each : StudioTool.inOrder()) {
            labels.add(Math.round(font.width(GameText.resolve(each.title())) * StudioDraw.SMALL));
        }
        this.regions = StudioLayout.of(width, height, labels);
        place(this.search, this.regions.search());
        place(this.checksButton, this.regions.checks());
        place(this.reloadButton, this.regions.reload());
        place(this.runButton, this.regions.runTests());
        place(this.recordButton, this.regions.record());
        place(this.doneButton, this.regions.done());
        this.pages.values().forEach(page -> page.layout(this.regions));
    }

    /** The font the studio writes in. */
    Font studioFont() {
        return font;
    }

    /** The block the player looked at as the studio opened, if any: what the block tools read. */
    Optional<BlockPos> lookedAt() {
        return Optional.ofNullable(this.lookedAt);
    }

    /** The thing chosen on the home page, which the tools that read a thing read. */
    Optional<StudioThing> thing() {
        return this.things.chosen();
    }

    /** Has the foot say {@code words}. */
    void say(final String words) {
        this.status = words;
    }

    /** Opens {@code file} in what the computer opens such files with, and says whether it could. */
    void open(final StudioSheet.File file) {
        this.say(StudioFiles.open(file) ? GameText.resolve(StudioTexts.OPENED.with(file.path()))
                : GameText.resolve(StudioTexts.NOT_OPENED.with(file.path(), file.modid())));
    }

    private StudioPage page() {
        return this.pages.get(this.tool);
    }

    /* The way to what is open, over the game's own header line, as the game's menus have it. */
    private void bar(final GuiGraphics g, final StudioPalette.Colours colours) {
        final String studio = GameText.resolve(StudioTexts.STUDIO);
        final int textY = (StudioLayout.BAR - 8) / 2;
        Draw.text(g, font, studio, StudioLayout.PAD, textY, colours.text());
        final int crumbX = StudioLayout.PAD + font.width(studio) + 2 * StudioDraw.INSET;
        final List<String> crumbs = this.page().crumbs();
        if (!crumbs.isEmpty()) {
            StudioDraw.text(g, font, String.join(CRUMB, crumbs), crumbX, textY,
                    this.regions.search().x() - StudioDraw.INSET - crumbX, colours.label());
        }
        StudioDraw.headLine(g, this.regions.bar().bottom(), this.regions.width());
    }

    /* The tools' tabs, the open one on a dark ground and underlined. */
    private void strip(final GuiGraphics g, final StudioPalette.Colours colours) {
        final List<StudioLayout.Rect> tabs = this.regions.tabs();
        final List<StudioTool> tools = StudioTool.inOrder();
        for (int i = 0; i < tabs.size(); i++) {
            final StudioLayout.Rect tab = tabs.get(i);
            final boolean open = tools.get(i) == this.tool;
            if (open) {
                g.fill(tab.x(), tab.y(), tab.right(), tab.bottom(), TAB_GROUND);
                g.fill(tab.x(), tab.bottom() - 1, tab.right(), tab.bottom(), colours.outline());
            }
            Draw.textScaled(g, font, GameText.resolve(tools.get(i).title()), tab.x() + StudioLayout.TAB_PAD,
                    tab.y() + 3, open ? colours.text() : colours.label(), StudioDraw.SMALL);
        }
        StudioDraw.headLine(g, this.regions.strip().bottom() + 1, this.regions.width());
    }

    /* The resources again, the world's data too when its owner may reload it, and then every thing read again. */
    private void reloadAll() {
        final Minecraft game = Minecraft.getInstance();
        this.say(GameText.resolve(StudioTexts.RELOADING));
        if (game.player != null && game.hasSingleplayerServer() && game.player.hasPermissions(RELOADS)) {
            game.player.connection.sendCommand("reload");
        }
        game.reloadResourcePacks().thenRun(() -> game.execute(() -> this.things.read(this.search.edit())));
    }

    private void runTests() {
        this.things.runTests();
    }

    private StyleSkin skin() {
        return Styles.skin(Styles.game());
    }

    private static void place(final UiComponent component, final StudioLayout.Rect rect) {
        component.setBounds(rect.x(), rect.y(), rect.width(), rect.height());
    }
}
