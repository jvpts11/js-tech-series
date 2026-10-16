/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.builder.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.JsCore;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.jstechseries.builder.BuildSources;
import dev.jstech.core.jstechseries.builder.BuildStatus;
import dev.jstech.core.jstechseries.builder.BuilderActionPayload;
import dev.jstech.core.jstechseries.builder.BuilderLayout;
import dev.jstech.core.jstechseries.builder.BuilderPlan;
import dev.jstech.core.jstechseries.builder.BuilderState;
import dev.jstech.core.jstechseries.builder.BuilderStatePayload;
import dev.jstech.core.jstechseries.builder.BuilderTexts;
import dev.jstech.core.jstechseries.builder.IBuildSource;
import dev.jstech.core.jstechseries.builder.MultiblockBuilder;
import dev.jstech.core.jstechseries.probe.NetworkProbe;
import dev.jstech.core.jstechseries.probe.ProbeLayout;
import dev.jstech.core.jstechseries.probe.client.NetworkProbeScreen;
import dev.jstech.core.multiblock.MultiblockBlocks;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The Multiblock Builder held up before the player over the darkened world, as the approved design draws it: the
 * probes' body, its display with three pages (what it will place and what the player has to place it with, the block
 * chosen where a place takes several, and where the blocks come from), the soft keys that turn the pages and put it
 * away, the pad that chooses and builds, and its HOLD, SHOW, LIGHT and POWER keys.
 */
@PaletteHolder
public final class MultiblockBuilderScreen extends Screen {

    private final InteractionHand hand;
    private BuilderState state;
    private int left;
    private int top;

    /** The colours the builder adds to its family's. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "multiblock_builder", new Colours(
            0xFF << 24 | 0xF0564A, 0xFF << 24 | 0x1E3C46, 0xFF << 24 | 0x5FE6F0));
    /* The soft key that puts the builder away, after those of the pages. */
    private static final int BACK_KEY = 3;

    /** The builder in {@code hand}, held up. */
    public MultiblockBuilderScreen(final InteractionHand hand) {
        super(GameText.component(BuilderTexts.TITLE));
        this.hand = hand;
        this.state = MultiblockBuilder.state(held());
    }

    @Override
    protected void init() {
        this.left = (this.width - ProbeLayout.WIDTH) / 2;
        this.top = (this.height - ProbeLayout.HEIGHT) / 2;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        if (!held().is(MultiblockBuilder.ITEM.get())) {
            onClose();
        }
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final NetworkProbeScreen.Colours c = NetworkProbeScreen.PALETTE.get();
        g.blit(NetworkProbeScreen.PICTURE, this.left, this.top, 0, 0, ProbeLayout.WIDTH, ProbeLayout.HEIGHT,
                ProbeLayout.WIDTH, ProbeLayout.HEIGHT);
        small(g, BuilderTexts.TITLE.text(), ProbeLayout.TITLE_X, ProbeLayout.TITLE_Y, c.silk());
        final Optional<BuilderPlan> plan = this.state.on() ? BuilderClient.plan(held()) : Optional.empty();
        g.fill(x(ProbeLayout.LAMP_X), y(ProbeLayout.LAMP_Y), x(ProbeLayout.LAMP_X + ProbeLayout.LAMP_SIZE),
                y(ProbeLayout.LAMP_Y + ProbeLayout.LAMP_SIZE), plan.isPresent() ? c.green() : c.lampOff());
        smallRight(g, BuilderTexts.AIMED.text(), ProbeLayout.LINK_RIGHT, ProbeLayout.TITLE_Y, c.silk());
        if (this.state.on()) {
            if (this.state.light()) {
                g.fill(x(ProbeLayout.DISPLAY_LEFT), y(ProbeLayout.DISPLAY_TOP), x(ProbeLayout.DISPLAY_RIGHT),
                        y(ProbeLayout.DISPLAY_BOTTOM), c.lit());
            }
            tabs(g, c);
            if (plan.isEmpty()) {
                header(g, c, BuilderTexts.TITLE.text(), Text.EMPTY);
                small(g, (aim().isPresent() ? BuilderTexts.UNKNOWN : BuilderTexts.NOT_AIMED).text(),
                        ProbeLayout.TEXT_LEFT, BuilderLayout.TO_PLACE_Y, c.dim());
            } else {
                switch (this.state.page()) {
                    case BUILD -> build(g, c, plan.get());
                    case CHOICES -> choices(g, c, plan.get());
                    case SOURCE -> sources(g, c);
                }
            }
        }
        keys(g, c);
    }

    @Override
    public void renderBackground(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        // The world stays in view behind the instrument, darkened, as behind the game's book.
        renderTransparentBackground(g);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final double x = mouseX - this.left;
        final double y = mouseY - this.top;
        final int soft = ProbeLayout.softAt(x, y);
        if (soft >= 0) {
            if (soft == BACK_KEY) {
                onClose();
                return true;
            }
            return set(this.state.onPage(BuilderState.Page.order().get(soft)));
        }
        if (ProbeLayout.onKey(x, y, ProbeLayout.HOLD_X, ProbeLayout.HOLD_Y)) {
            return act(BuilderActionPayload.Action.HOLD);
        }
        if (ProbeLayout.onKey(x, y, ProbeLayout.SHOW_X, ProbeLayout.SHOW_Y)) {
            return set(this.state.toggleShow());
        }
        if (ProbeLayout.onKey(x, y, ProbeLayout.LIGHT_X, ProbeLayout.LIGHT_Y)) {
            return set(this.state.toggleLight());
        }
        if (ProbeLayout.onKey(x, y, ProbeLayout.POWER_X, ProbeLayout.POWER_Y)) {
            return set(this.state.togglePower());
        }
        final ProbeLayout.Pad pad = ProbeLayout.padAt(x, y);
        return pad != ProbeLayout.Pad.NONE ? press(pad) : super.mouseClicked(mouseX, mouseY, button);
    }

    /* The arrows and Enter work the pad, as on a keyboard the instrument would have. */
    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        final ProbeLayout.Pad pad = switch (keyCode) {
            case InputConstants.KEY_UP -> ProbeLayout.Pad.UP;
            case InputConstants.KEY_DOWN -> ProbeLayout.Pad.DOWN;
            case InputConstants.KEY_LEFT -> ProbeLayout.Pad.LEFT;
            case InputConstants.KEY_RIGHT -> ProbeLayout.Pad.RIGHT;
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER -> ProbeLayout.Pad.OK;
            default -> ProbeLayout.Pad.NONE;
        };
        return pad != ProbeLayout.Pad.NONE ? press(pad) : super.keyPressed(keyCode, scanCode, modifiers);
    }

    /** The builder as the player holds it now. */
    public BuilderState state() {
        return this.state;
    }

    /*
     * A key of the pad: up and down choose a row; left and right choose the block of a group of places on the page of
     * choices, and turn the pages elsewhere; OK builds from the page to build, and takes the chosen source from its.
     */
    private boolean press(final ProbeLayout.Pad pad) {
        if (!this.state.on()) {
            return true;
        }
        final Optional<BuilderPlan> plan = BuilderClient.plan(held());
        final int rows = rows(plan);
        return switch (pad) {
            case UP -> set(this.state.rowMoved(-1, rows));
            case DOWN -> set(this.state.rowMoved(1, rows));
            case LEFT -> this.state.page() == BuilderState.Page.CHOICES ? pick(plan, -1) : paged(-1);
            case RIGHT -> this.state.page() == BuilderState.Page.CHOICES ? pick(plan, 1) : paged(1);
            case OK -> switch (this.state.page()) {
                case BUILD -> plan.isPresent() && act(BuilderActionPayload.Action.BUILD);
                case SOURCE -> chooseSource();
                case CHOICES -> true;
            };
            case NONE -> false;
        };
    }

    private boolean paged(final int by) {
        final List<BuilderState.Page> pages = BuilderState.Page.order();
        return set(this.state.onPage(pages.get(Math.floorMod(pages.indexOf(this.state.page()) + by, pages.size()))));
    }

    /* Turns the block chosen for the chosen group of places {@code by} blocks along, round past the last. */
    private boolean pick(final Optional<BuilderPlan> plan, final int by) {
        if (plan.isEmpty() || plan.get().groups().isEmpty()) {
            return true;
        }
        final BuilderPlan.Group group = plan.get().groups().get(Math.min(this.state.row(),
                plan.get().groups().size() - 1));
        final int at = Math.max(0, group.fits().indexOf(chosen(group)));
        return set(this.state.choosing(group.key(), group.fits().get(Math.floorMod(at + by, group.fits().size()))));
    }

    private boolean chooseSource() {
        final List<ResourceLocation> ids = BuildSources.ids();
        final ResourceLocation id = ids.get(Math.min(this.state.row(), ids.size() - 1));
        final LocalPlayer player = Minecraft.getInstance().player;
        return player == null || !BuildSources.get(id).usable(player) || set(this.state.from(id));
    }

    /* How many rows the page shown has to choose among. */
    private int rows(final Optional<BuilderPlan> plan) {
        return switch (this.state.page()) {
            case BUILD -> plan.map(p -> p.order().uses().size()).orElse(0);
            case CHOICES -> plan.map(p -> p.groups().size()).orElse(0);
            case SOURCE -> BuildSources.ids().size();
        };
    }

    private boolean set(final BuilderState to) {
        if (!to.equals(this.state)) {
            this.state = to;
            held().set(MultiblockBuilder.STATE.get(), to);
            BuilderClient.forget();
            PacketDistributor.sendToServer(new BuilderStatePayload(this.hand == InteractionHand.MAIN_HAND, to));
        }
        return true;
    }

    private boolean act(final BuilderActionPayload.Action action) {
        if (this.state.on()) {
            PacketDistributor.sendToServer(new BuilderActionPayload(this.hand == InteractionHand.MAIN_HAND, action));
        }
        return true;
    }

    private ItemStack held() {
        final Minecraft minecraft = Minecraft.getInstance();
        return minecraft.player == null ? ItemStack.EMPTY : minecraft.player.getItemInHand(this.hand);
    }

    private Optional<BlockPos> aim() {
        final LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? Optional.empty() : MultiblockBuilder.aim(held(), player);
    }

    /* The block chosen for a group of places: the player's choice, else the first that fits. */
    private String chosen(final BuilderPlan.Group group) {
        final String choice = this.state.chosen().get(group.key());
        return choice != null && group.fits().contains(choice) ? choice : group.fits().getFirst();
    }

    /* The page to build, as the approved design lays it out. */
    private void build(final GuiGraphics g, final NetworkProbeScreen.Colours c, final BuilderPlan plan) {
        final Colours b = PALETTE.get();
        header(g, c, plan.name(), BuilderTexts.HEADER.with(plan.preview().sizeX(), plan.preview().sizeY(),
                plan.preview().sizeZ(), facing(plan.facing()).text()));
        final int x = ProbeLayout.TEXT_LEFT;
        small(g, BuilderTexts.TO_PLACE.text(), x, BuilderLayout.TO_PLACE_Y, c.cyan());
        smallRight(g, BuilderTexts.COUNTS.with(plan.wants().size(), plan.already()), ProbeLayout.TEXT_RIGHT,
                BuilderLayout.TO_PLACE_Y, c.dim());
        final List<Map.Entry<String, Integer>> uses = new ArrayList<>(plan.order().uses().entrySet());
        final int first = firstShown(uses.size(), BuilderLayout.MATERIAL_ROWS);
        for (int k = first; k < Math.min(uses.size(), first + BuilderLayout.MATERIAL_ROWS); k++) {
            final int y = BuilderLayout.MATERIALS_Y + (k - first) * BuilderLayout.MATERIAL_PITCH;
            final String block = uses.get(k).getKey();
            final int needs = uses.get(k).getValue();
            final int has = player() == null ? 0 : BuilderPlan.has(player(), this.state, block);
            icon(g, block, x, y);
            small(g, name(block), BuilderLayout.NAME_X, y + 3, k == this.state.row() ? c.white() : c.text());
            final Text have = BuilderTexts.HAVE.with(count(has));
            smallRight(g, have, ProbeLayout.TEXT_RIGHT, y + 3, has >= needs ? c.green() : b.red());
            smallRight(g, BuilderTexts.NEEDS.with(needs), ProbeLayout.TEXT_RIGHT - width(have) - 4, y + 3,
                    c.white());
        }
        if (!plan.groups().isEmpty()) {
            choiceBox(g, c, b, plan.groups().getFirst());
        }
        small(g, BuilderTexts.SOURCE.text(), x, BuilderLayout.SOURCE_Y, c.cyan());
        final LocalPlayer player = player();
        small(g, player != null && player.getAbilities().instabuild
                && BuildSources.INVENTORY.equals(this.state.source()) ? BuilderTexts.CREATIVE.text()
                : BuildSources.get(this.state.source()).name(), x, BuilderLayout.SOURCE_Y + BuilderLayout.LINE,
                c.text());
        ready(g, c, b, plan);
    }

    /* The first group of places that take several blocks, its blocks side by side, the chosen one framed. */
    private void choiceBox(final GuiGraphics g, final NetworkProbeScreen.Colours c, final Colours b,
                           final BuilderPlan.Group group) {
        g.fill(x(ProbeLayout.DISPLAY_LEFT + 1), y(BuilderLayout.CHOICE_TOP), x(ProbeLayout.DISPLAY_RIGHT - 1),
                y(BuilderLayout.CHOICE_TOP + BuilderLayout.CHOICE_HEIGHT), b.box());
        final Text head = group.places() == 1 ? BuilderTexts.ONE_PLACE_EITHER.text()
                : BuilderTexts.PLACES_EITHER.with(group.places());
        small(g, head, ProbeLayout.TEXT_LEFT, BuilderLayout.CHOICE_TOP + 2, c.amber());
        final String chosen = chosen(group);
        for (int k = 0; k < Math.min(group.fits().size(), BuilderLayout.CHOICE_OPTIONS); k++) {
            final String block = group.fits().get(k);
            final int bx = ProbeLayout.TEXT_LEFT + k * BuilderLayout.CHOICE_WIDTH;
            final int by = BuilderLayout.CHOICE_TOP + 10;
            if (block.equals(chosen)) {
                Draw.outline(g, x(bx - 1), y(by - 1), BuilderLayout.ICON + 2, BuilderLayout.ICON + 2, b.picked());
            }
            icon(g, block, bx, by);
            final int has = player() == null ? 0 : BuilderPlan.has(player(), this.state, block);
            final int room = (int) ((BuilderLayout.CHOICE_WIDTH - BuilderLayout.ICON - 4) / ProbeLayout.SMALL);
            small(g, Text.literal(this.font.plainSubstrByWidth(GameText.resolve(name(block)), room)),
                    bx + BuilderLayout.ICON + 2, by, block.equals(chosen) ? c.white() : c.dim());
            small(g, BuilderTexts.HAVE_SHORT.with(count(has)), bx + BuilderLayout.ICON + 2, by + 7, c.dim());
        }
    }

    /* The foot of the page to build: ready, lacking, or how the building goes. */
    private void ready(final GuiGraphics g, final NetworkProbeScreen.Colours c, final Colours b,
                       final BuilderPlan plan) {
        final BuildStatus status = BuilderClient.status();
        final boolean ours = aim().filter(status.controller()::equals).isPresent();
        final BuildStatus.Phase phase = ours ? status.phase() : BuildStatus.Phase.IDLE;
        final Text word;
        final Text line;
        final int colour;
        switch (phase) {
            case BUILDING -> {
                word = BuilderTexts.BUILDING.text();
                line = BuilderTexts.BUILDING_LINE.with(status.placed(), status.placed() + plan.wants().size());
                colour = c.cyan();
            }
            case HELD -> {
                word = BuilderTexts.HELD.text();
                line = BuilderTexts.HELD_LINE.text();
                colour = c.amber();
            }
            case BUILT -> {
                word = BuilderTexts.BUILT.text();
                line = BuilderTexts.BUILT_LINE.text();
                colour = c.green();
            }
            case STOPPED -> {
                word = BuilderTexts.STOPPED.text();
                line = status.reason().says().with(name(status.block()));
                colour = b.red();
            }
            default -> {
                if (plan.order().ready()) {
                    word = BuilderTexts.READY.text();
                    line = BuilderTexts.READY_LINE.text();
                    colour = c.green();
                } else {
                    final Map.Entry<String, Integer> lacks = plan.order().lacks().entrySet().iterator().next();
                    word = BuilderTexts.LACKING.text();
                    line = BuilderTexts.LACKING_LINE.with(lacks.getValue(), name(lacks.getKey()));
                    colour = b.red();
                }
            }
        }
        small(g, word, ProbeLayout.TEXT_LEFT, BuilderLayout.READY_Y, colour);
        final int after = ProbeLayout.TEXT_LEFT + width(word) + 4;
        final int room = (int) ((ProbeLayout.TEXT_RIGHT - after) / ProbeLayout.SMALL);
        small(g, Text.literal(this.font.plainSubstrByWidth(GameText.resolve(line), room)), after,
                BuilderLayout.READY_Y, c.text());
    }

    /* The page of choices: each group of places that take several blocks, its chosen block between arrows. */
    private void choices(final GuiGraphics g, final NetworkProbeScreen.Colours c, final BuilderPlan plan) {
        final Colours b = PALETTE.get();
        header(g, c, BuilderTexts.CHOOSE.text(), Text.EMPTY);
        final List<BuilderPlan.Group> groups = plan.groups();
        if (groups.isEmpty()) {
            small(g, BuilderTexts.NO_CHOICES.text(), ProbeLayout.TEXT_LEFT, BuilderLayout.TO_PLACE_Y, c.dim());
            return;
        }
        final int first = firstShown(groups.size(), BuilderLayout.GROUP_ROWS);
        for (int k = first; k < Math.min(groups.size(), first + BuilderLayout.GROUP_ROWS); k++) {
            final BuilderPlan.Group group = groups.get(k);
            final int y = BuilderLayout.TO_PLACE_Y + (k - first) * BuilderLayout.GROUP_PITCH;
            if (k == Math.min(this.state.row(), groups.size() - 1)) {
                g.fill(x(ProbeLayout.DISPLAY_LEFT + 1), y(y - 2), x(ProbeLayout.DISPLAY_RIGHT - 1),
                        y(y + BuilderLayout.GROUP_PITCH - 3), b.box());
            }
            small(g, group.places() == 1 ? BuilderTexts.CHOICE_ONE_PLACE.text()
                    : BuilderTexts.CHOICE_PLACES.with(group.places()), ProbeLayout.TEXT_LEFT, y, c.dim());
            final String chosen = chosen(group);
            icon(g, chosen, ProbeLayout.TEXT_LEFT, y + 7);
            small(g, BuilderTexts.PICKED.with(name(chosen)), BuilderLayout.NAME_X, y + 10, c.white());
        }
    }

    /* The page of sources: each place the blocks may come from, the one in use lit. */
    private void sources(final GuiGraphics g, final NetworkProbeScreen.Colours c) {
        final Colours b = PALETTE.get();
        header(g, c, BuilderTexts.SOURCES.text(), Text.EMPTY);
        final List<ResourceLocation> ids = BuildSources.ids();
        final LocalPlayer player = player();
        final int first = firstShown(ids.size(), BuilderLayout.SOURCE_ROWS);
        for (int k = first; k < Math.min(ids.size(), first + BuilderLayout.SOURCE_ROWS); k++) {
            final int y = BuilderLayout.TO_PLACE_Y + (k - first) * BuilderLayout.SOURCE_PITCH;
            final IBuildSource source = BuildSources.get(ids.get(k));
            if (k == Math.min(this.state.row(), ids.size() - 1)) {
                g.fill(x(ProbeLayout.DISPLAY_LEFT + 1), y(y - 2), x(ProbeLayout.DISPLAY_RIGHT - 1), y(y + 8),
                        b.box());
            }
            final boolean usable = player != null && source.usable(player);
            final boolean inUse = ids.get(k).equals(this.state.source());
            small(g, source.name(), ProbeLayout.TEXT_LEFT, y, inUse ? c.green() : usable ? c.text() : c.dim());
        }
    }

    /* The tabs of the three pages along the display's foot, the one shown lit, and the fourth that puts it away. */
    private void tabs(final GuiGraphics g, final NetworkProbeScreen.Colours c) {
        final List<BuilderState.Page> pages = BuilderState.Page.order();
        final int width = (ProbeLayout.DISPLAY_RIGHT - ProbeLayout.DISPLAY_LEFT) / ProbeLayout.PAGES;
        final int tabTop = ProbeLayout.DISPLAY_BOTTOM - ProbeLayout.TABS_HEIGHT;
        for (int k = 0; k < ProbeLayout.PAGES; k++) {
            final boolean on = k < pages.size() && pages.get(k) == this.state.page();
            final int tx = ProbeLayout.DISPLAY_LEFT + k * width;
            g.fill(x(tx), y(tabTop), x(tx + width), y(ProbeLayout.DISPLAY_BOTTOM), on ? c.cyan() : c.tabOff());
            if (k > 0) {
                g.fill(x(tx), y(tabTop), x(tx + 1), y(ProbeLayout.DISPLAY_BOTTOM), c.tft());
            }
            final Text word = tab(k, pages).text();
            small(g, word, tx + (width - width(word)) / 2, tabTop + 2, on ? c.tabInk() : c.dim());
        }
    }

    private static TextKey tab(final int k, final List<BuilderState.Page> pages) {
        if (k >= pages.size()) {
            return BuilderTexts.TAB_BACK;
        }
        return switch (pages.get(k)) {
            case BUILD -> BuilderTexts.TAB_BUILD;
            case CHOICES -> BuilderTexts.TAB_CHOICES;
            case SOURCE -> BuilderTexts.TAB_SOURCE;
        };
    }

    private static TextKey facing(final Direction facing) {
        return switch (facing) {
            case EAST -> BuilderTexts.EAST;
            case SOUTH -> BuilderTexts.SOUTH;
            case WEST -> BuilderTexts.WEST;
            default -> BuilderTexts.NORTH;
        };
    }

    /* The header: a word in the game's font on the left, a small figure on the right. */
    private void header(final GuiGraphics g, final NetworkProbeScreen.Colours c, final Text title, final Text right) {
        g.fill(x(ProbeLayout.DISPLAY_LEFT), y(ProbeLayout.DISPLAY_TOP), x(ProbeLayout.DISPLAY_RIGHT),
                y(ProbeLayout.DISPLAY_TOP + ProbeLayout.HEADER_HEIGHT), c.head());
        final int rightWidth = width(right);
        final int room = ProbeLayout.DISPLAY_RIGHT - ProbeLayout.DISPLAY_LEFT - 9 - rightWidth;
        Draw.text(g, this.font, this.font.plainSubstrByWidth(GameText.resolve(title), room),
                x(ProbeLayout.DISPLAY_LEFT + 3), y(ProbeLayout.DISPLAY_TOP + 2), c.white());
        smallRight(g, right, ProbeLayout.DISPLAY_RIGHT - 3, ProbeLayout.DISPLAY_TOP + 3, c.headText());
    }

    /* The four keys' words, each key sunk a pixel while it is on, and OK on the pad. */
    private void keys(final GuiGraphics g, final NetworkProbeScreen.Colours c) {
        final BuildStatus status = BuilderClient.status();
        final boolean holding = status.phase() == BuildStatus.Phase.HELD
                && aim().filter(status.controller()::equals).isPresent();
        key(g, c, BuilderTexts.HOLD, ProbeLayout.HOLD_X, ProbeLayout.HOLD_Y, holding, c.amberInk());
        key(g, c, BuilderTexts.SHOW, ProbeLayout.SHOW_X, ProbeLayout.SHOW_Y, this.state.show(), c.tealInk());
        key(g, c, BuilderTexts.LIGHT, ProbeLayout.LIGHT_X, ProbeLayout.LIGHT_Y, this.state.light(), c.silk());
        key(g, c, BuilderTexts.POWER, ProbeLayout.POWER_X, ProbeLayout.POWER_Y, this.state.on(), c.powerInk());
        final Text ok = NetworkProbe.OK.text();
        small(g, ok, ProbeLayout.PAD_X - width(ok) / 2, ProbeLayout.PAD_Y - 3, c.silk());
    }

    private void key(final GuiGraphics g, final NetworkProbeScreen.Colours c, final TextKey label, final int keyX,
                     final int keyY, final boolean on, final int ink) {
        if (on) {
            g.fill(x(keyX), y(keyY), x(keyX + ProbeLayout.KEY_WIDTH), y(keyY + ProbeLayout.KEY_HEIGHT), c.pressed());
        }
        final Text word = label.text();
        small(g, word, keyX + (ProbeLayout.KEY_WIDTH - width(word)) / 2, keyY + (on ? 4 : 3), ink);
    }

    /* A block's item, drawn small, at a place of the display. */
    private void icon(final GuiGraphics g, final String block, final int ix, final int iy) {
        final Block known = MultiblockBlocks.blockOf(block);
        if (known == null) {
            return;
        }
        g.pose().pushPose();
        g.pose().translate(x(ix), y(iy), 0);
        g.pose().scale(BuilderLayout.ICON_SCALE, BuilderLayout.ICON_SCALE, 1F);
        g.renderFakeItem(new ItemStack(known), 0, 0);
        g.pose().popPose();
    }

    /* The first row shown of {@code count} when {@code shown} fit, the chosen one in view. */
    private int firstShown(final int count, final int shown) {
        final int chosen = Math.min(this.state.row(), Math.max(0, count - 1));
        return Math.max(0, Math.min(count - shown, chosen - shown + 1));
    }

    private static Text name(final String block) {
        final Block known = MultiblockBlocks.blockOf(block);
        return known == null ? Text.literal(block) : GameText.of(known.getName());
    }

    /* A count as the display shows it: a player in creative has every block there is. */
    private static Text count(final int count) {
        return Text.literal(count >= Integer.MAX_VALUE / 4 ? "-" : String.valueOf(count));
    }

    private static LocalPlayer player() {
        return Minecraft.getInstance().player;
    }

    private void small(final GuiGraphics g, final Text text, final int sx, final int sy, final int colour) {
        Draw.textScaled(g, this.font, GameText.resolve(text), x(sx), y(sy), colour, ProbeLayout.SMALL);
    }

    private void smallRight(final GuiGraphics g, final Text text, final int right, final int sy, final int colour) {
        small(g, text, right - width(text), sy, colour);
    }

    private int width(final Text text) {
        return (int) Math.ceil(this.font.width(GameText.resolve(text)) * ProbeLayout.SMALL);
    }

    private int x(final int x) {
        return this.left + x;
    }

    private int y(final int y) {
        return this.top + y;
    }

    /**
     * The colours the builder adds to its family's.
     *
     * @param red    what is lacking, and a stop
     * @param box    the ground of a choice and of the row chosen
     * @param picked the frame of the block chosen
     */
    public record Colours(int red, int box, int picked) {
    }
}
