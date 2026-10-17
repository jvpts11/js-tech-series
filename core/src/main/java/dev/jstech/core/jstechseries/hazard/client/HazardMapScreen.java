/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.jstechseries.hazard.client;

import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.hazard.HazardMapsClient;
import dev.jstech.core.hazard.HazardKind;
import dev.jstech.core.hazard.HazardMap;
import dev.jstech.core.jstechseries.hazard.DeviceState;
import dev.jstech.core.jstechseries.hazard.DeviceStatePayload;
import dev.jstech.core.jstechseries.hazard.HazardDevice;
import dev.jstech.core.jstechseries.hazard.HazardDeviceItem;
import dev.jstech.core.jstechseries.hazard.HazardDevices;
import dev.jstech.core.jstechseries.hazard.HazardScreenTexts;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * The whole map of hazards, opened from the map device: the land round the player a block a pixel, each chunk the
 * map knows coloured by its reading, what was never read greyed; a button for each hazard the map shows, the legend of
 * its steps, and a chunk's reading and its age when it is clicked. Dragged, the map moves.
 */
public final class HazardMapScreen extends Screen {

    private final InteractionHand hand;
    private @Nullable TerrainPicture picture;
    private double centreX;
    private double centreZ;
    private int mapX;
    private int mapY;
    private int mapWidth;
    private int mapHeight;
    private boolean dragged;
    private @Nullable ChunkPos clicked;

    /** A chunk on the map, in pixels: a block a pixel. */
    public static final int CELL = 16;
    private static final int MARGIN = 8;
    private static final int TOP = 52;
    private static final int LEGEND_WIDTH = 116;
    private static final int TAB_WIDTH = 74;
    private static final int TAB_HEIGHT = 20;
    private static final int TABS_Y = 26;
    private static final int DONE_WIDTH = 90;
    private static final int SWATCH = 8;
    private static final int ROW = 12;
    /* How many chunks of land the map draws a frame, and how many chunks past the map's edge the land reaches. */
    private static final int CHUNKS_A_FRAME = 24;
    private static final int SPARE_CHUNKS = 4;
    private static final int TICKS_A_SECOND = 20;
    private static final int TITLE_COLOUR = 0xFF << 24 | 0xFFFFFF;
    private static final int TEXT_COLOUR = 0xFF << 24 | 0xE0E0E0;
    private static final int DIM_COLOUR = 0xFF << 24 | 0xA0A0A0;
    private static final int FRAME_COLOUR = 0xFF << 24;

    public HazardMapScreen(final InteractionHand hand) {
        super(HazardDevices.HAZARD_MAP.get().getDescription());
        this.hand = hand;
    }

    /** The hazard the map shows now, if any is mapped. */
    public Optional<HazardKind> shownKind() {
        return DeviceHud.shown(HazardDevices.state(held()));
    }

    /** The chunk last clicked, whose reading the legend shows. */
    public Optional<ChunkPos> clicked() {
        return Optional.ofNullable(this.clicked);
    }

    /** The map's rectangle on the screen: its left, top, width and height. */
    public int[] mapArea() {
        return new int[] {this.mapX, this.mapY, this.mapWidth, this.mapHeight};
    }

    /** Shows the readings of {@code kind}: kept on the device in hand. */
    public void show(final HazardKind kind) {
        final ItemStack held = held();
        if (held.isEmpty()) {
            return;
        }
        final DeviceState state = HazardDevices.state(held).showing(kind.id().toString());
        held.set(HazardDevices.STATE.get(), state);
        PacketDistributor.sendToServer(new DeviceStatePayload(this.hand == InteractionHand.MAIN_HAND, state));
        this.rebuildWidgets();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final Player player = this.minecraft == null ? null : this.minecraft.player;
        if (player == null || this.picture == null) {
            return;
        }
        final Optional<HazardKind> kind = shownKind();
        final Component title = kind.<Component>map(found -> GameText.component(HazardScreenTexts.MAP_TITLE.with(
                GameText.resolve(found.name())))).orElse(this.title);
        Draw.textCentered(g, this.font, title, this.width / 2, 10, TITLE_COLOUR);
        final int tabsEnd = MARGIN + DeviceHud.mapped().size() * (TAB_WIDTH + 4);
        Draw.text(g, this.font, GameText.resolve(HazardScreenTexts.MAP_SOURCES), tabsEnd + 4, TABS_Y + 6,
                DIM_COLOUR);
        this.picture.update(new ChunkPos((int) Math.floor(this.centreX), (int) Math.floor(this.centreZ)),
                CHUNKS_A_FRAME);
        g.fill(this.mapX - 1, this.mapY - 1, this.mapX + this.mapWidth + 1, this.mapY + this.mapHeight + 1,
                FRAME_COLOUR);
        HazardMapDrawing.draw(g, this.picture, this.mapX, this.mapY, this.mapWidth, this.mapHeight, CELL,
                this.centreX, this.centreZ, kind, player.getX() / 16, player.getZ() / 16, player.getYRot(), -1);
        if (kind.isEmpty()) {
            final List<FormattedCharSequence> lines = this.font.split(GameText.component(
                    HazardScreenTexts.MAP_NOTHING), this.mapWidth - 2 * MARGIN);
            for (int i = 0; i < lines.size(); i++) {
                Draw.text(g, this.font, lines.get(i), this.mapX + MARGIN,
                        this.mapY + this.mapHeight / 2 + i * ROW, TITLE_COLOUR);
            }
            return;
        }
        drawLegend(g, kind.get(), player);
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        this.dragged = false;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(final double mouseX, final double mouseY, final int button, final double dragX,
                                final double dragY) {
        if (button == 0 && onMap(mouseX, mouseY)) {
            this.centreX -= dragX / CELL;
            this.centreZ -= dragY / CELL;
            this.dragged = true;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(final double mouseX, final double mouseY, final int button) {
        if (button == 0 && !this.dragged && onMap(mouseX, mouseY)) {
            this.clicked = chunkAt(mouseX, mouseY);
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** The chunk under the pointer at {@code (mouseX, mouseY)} on the map. */
    public ChunkPos chunkAt(final double mouseX, final double mouseY) {
        final double x = this.centreX + (mouseX - this.mapX - this.mapWidth / 2.0) / CELL;
        final double z = this.centreZ + (mouseY - this.mapY - this.mapHeight / 2.0) / CELL;
        return new ChunkPos((int) Math.floor(x), (int) Math.floor(z));
    }

    @Override
    public void removed() {
        if (this.picture != null) {
            this.picture.close();
            this.picture = null;
        }
    }

    @Override
    protected void init() {
        final Player player = this.minecraft == null ? null : this.minecraft.player;
        if (player != null && this.picture == null) {
            this.centreX = player.getX() / 16;
            this.centreZ = player.getZ() / 16;
        }
        this.mapX = MARGIN;
        this.mapY = TOP;
        this.mapWidth = this.width - LEGEND_WIDTH - 3 * MARGIN;
        this.mapHeight = this.height - TOP - MARGIN;
        if (this.picture == null) {
            this.picture = new TerrainPicture(Math.max(this.mapWidth, this.mapHeight) / CELL + SPARE_CHUNKS, CELL);
        }
        final List<HazardKind> kinds = DeviceHud.mapped();
        final Optional<HazardKind> shown = shownKind();
        for (int i = 0; i < kinds.size(); i++) {
            final HazardKind kind = kinds.get(i);
            final Button tab = Button.builder(GameText.component(kind.name()), pressed -> show(kind))
                    .bounds(MARGIN + i * (TAB_WIDTH + 4), TABS_Y - 4, TAB_WIDTH, TAB_HEIGHT).build();
            tab.active = shown.map(found -> found != kind).orElse(true);
            this.addRenderableWidget(tab);
        }
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, pressed -> this.onClose())
                .bounds(this.width - MARGIN - DONE_WIDTH, this.height - MARGIN - TAB_HEIGHT, DONE_WIDTH, TAB_HEIGHT)
                .build());
    }

    /* The legend of the steps of {@code kind}'s map, and the reading of the chunk last clicked. */
    private void drawLegend(final GuiGraphics g, final HazardKind kind, final Player player) {
        final int x = this.width - LEGEND_WIDTH - MARGIN;
        int y = TOP;
        Draw.text(g, this.font, DeviceReadings.rateUnit(kind), x, y, TITLE_COLOUR);
        y += ROW + 2;
        final List<Double> steps = kind.mapSteps();
        for (int step = steps.size() - 1; step >= 0; step--) {
            final String label = step == steps.size() - 1
                    ? GameText.resolve(HazardScreenTexts.MAP_OVER.with(DeviceReadings.figure(steps.get(step))))
                    : GameText.resolve(HazardScreenTexts.MAP_BETWEEN.with(DeviceReadings.figure(steps.get(step)),
                    DeviceReadings.figure(steps.get(step + 1))));
            swatch(g, x, y, HazardMapDrawing.stepColour(kind, step), label);
            y += ROW;
        }
        swatch(g, x, y, HazardMapDrawing.PALETTE.get().normal(), GameText.resolve(HazardScreenTexts.MAP_NORMAL));
        y += ROW + 4;
        swatch(g, x, y, HazardMapDrawing.PALETTE.get().never(), GameText.resolve(HazardScreenTexts.MAP_NEVER_READ));
        y += ROW + 8;
        y = wrapped(g, GameText.component(HazardScreenTexts.MAP_CLICK), x, y, DIM_COLOUR);
        if (this.clicked != null) {
            final Optional<HazardMap.Reading> reading = HazardMapsClient.reading(kind.id(), this.clicked.toLong());
            final Component said = reading.map(found -> GameText.component(HazardScreenTexts.MAP_READING.with(
                    DeviceReadings.figure(found.value()) + " " + DeviceReadings.rateUnit(kind),
                    GameText.resolve(age(player.level().getGameTime() - found.time())))))
                    .orElseGet(() -> GameText.component(HazardScreenTexts.MAP_NOT_READ));
            wrapped(g, said, x, y + 4, TEXT_COLOUR);
        }
    }

    private void swatch(final GuiGraphics g, final int x, final int y, final int colour, final String label) {
        g.fill(x, y, x + SWATCH, y + SWATCH, colour);
        Draw.text(g, this.font, label, x + SWATCH + 4, y, TEXT_COLOUR);
    }

    private int wrapped(final GuiGraphics g, final Component text, final int x, final int y, final int colour) {
        int at = y;
        for (final FormattedCharSequence line : this.font.split(text, LEGEND_WIDTH)) {
            Draw.text(g, this.font, line, x, at, colour);
            at += this.font.lineHeight;
        }
        return at;
    }

    private boolean onMap(final double mouseX, final double mouseY) {
        return mouseX >= this.mapX && mouseX < this.mapX + this.mapWidth && mouseY >= this.mapY
                && mouseY < this.mapY + this.mapHeight;
    }

    private ItemStack held() {
        if (this.minecraft == null || this.minecraft.player == null) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = this.minecraft.player.getItemInHand(this.hand);
        return stack.getItem() instanceof HazardDeviceItem item && item.device() == HazardDevice.HAZARD_MAP ? stack
                : ItemStack.EMPTY;
    }

    /* How long ago {@code ticks} is, in the largest unit that fits. */
    private static Text age(final long ticks) {
        final long seconds = Math.max(0, ticks / TICKS_A_SECOND);
        final TextKey unit;
        final long count;
        if (seconds < 60) {
            unit = HazardScreenTexts.AGE_SECONDS;
            count = seconds;
        } else if (seconds < 3600) {
            unit = HazardScreenTexts.AGE_MINUTES;
            count = seconds / 60;
        } else if (seconds < 86_400) {
            unit = HazardScreenTexts.AGE_HOURS;
            count = seconds / 3600;
        } else {
            unit = HazardScreenTexts.AGE_DAYS;
            count = seconds / 86_400;
        }
        return unit.with(count);
    }
}
