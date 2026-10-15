/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.cable;

import com.mojang.blaze3d.platform.InputConstants;
import dev.jstech.core.JsCore;
import dev.jstech.core.cable.CableEnd;
import dev.jstech.core.cable.CableEndLayout;
import dev.jstech.core.cable.CableEndMenu;
import dev.jstech.core.cable.EndFilters;
import dev.jstech.core.cable.EndNumberPayload;
import dev.jstech.core.cable.EndSettings;
import dev.jstech.core.cable.EndTexts;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.screen.CoreContainerScreen;
import dev.jstech.core.format.UnitFormatter;
import dev.jstech.core.grid.EndMode;
import dev.jstech.core.grid.GridKind;
import dev.jstech.core.inventory.ItemFilter;
import dev.jstech.core.material.Material;
import dev.jstech.core.material.Materials;
import dev.jstech.core.palette.Palette;
import dev.jstech.core.palette.PaletteHolder;
import dev.jstech.core.palette.Palettes;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * The screen of one end of a line, as the approved design draws it, in the game's own look: a window with the end's
 * mode keys, its priority and rate, its sixteen channels in the dyes' colours and its filter, whose nine slots hold
 * what passes and, in yellow, how much of it to keep in the block; below it, in a window of its own, the player's
 * inventory, from which a stack is clicked onto a filter slot. A line under the filter says what the end does.
 */
@PaletteHolder
public final class CableEndScreen extends CoreContainerScreen<CableEndMenu> {

    private final List<Button> modeKeys = new ArrayList<>();
    private final Map<String, ItemStack> modIcons = new HashMap<>();
    private Button filterKey;
    private EditBox priorityField;
    private EditBox rateField;
    private boolean priorityWasFocused;
    private boolean rateWasFocused;

    /** The screen's colours, the game's own. */
    public static final Palette<Colours> PALETTE = Palettes.declare(JsCore.MODID, "cable_end", new Colours(
            0xFF << 24 | 0xC6C6C6, 0xFF << 24 | 0xFFFFFF, 0xFF << 24 | 0x555555, 0xFF << 24,
            0xFF << 24 | 0x404040, 0xFF << 24 | 0x646464, 0xFF << 24 | 0xFFFFA0, 0xFF << 24 | 0x8B8B8B,
            0xFF << 24 | 0x373737, 0xFF << 24 | 0xFFFFFF, 0x80 << 24 | 0x8B8B8B));
    private static final int FIELD_MAX_LENGTH = 10;
    private static final int ITEM_SIZE = 16;
    private static final int OVER_ITEMS = 200;
    private static final int OVER_VEIL = 300;
    private static final int SWATCH_INSET = 2;
    private static final int TEN = 10;

    public CableEndScreen(final CableEndMenu menu, final Inventory inventory, final Component title) {
        super(menu, inventory, title);
        this.imageWidth = CableEndLayout.WIDTH;
        this.imageHeight = CableEndLayout.HEIGHT;
    }

    /** Hands the mod's bus the screen, once, from the Core's client. */
    public static void listen(final IEventBus modBus) {
        modBus.addListener(RegisterMenuScreensEvent.class, event -> event.register(EndSettings.MENU.get(),
                CableEndScreen::new));
    }

    @Override
    public boolean mouseClicked(final double mouseX, final double mouseY, final int button) {
        final double x = mouseX - this.leftPos;
        final double y = mouseY - this.topPos;
        final int ghost = CableEndLayout.ghostAt(x, y);
        if (ghost >= 0 && (button == InputConstants.MOUSE_BUTTON_LEFT || button == InputConstants.MOUSE_BUTTON_RIGHT)) {
            sendButton((button == InputConstants.MOUSE_BUTTON_LEFT ? CableEndMenu.FIRST_GHOST
                    : CableEndMenu.FIRST_CYCLE) + ghost);
            return true;
        }
        final int channel = CableEndLayout.channelAt(x, y);
        if (channel >= 0 && button == InputConstants.MOUSE_BUTTON_LEFT) {
            sendButton(CableEndMenu.FIRST_CHANNEL + channel);
            return true;
        }
        final boolean priorityBefore = this.priorityField.isFocused();
        final boolean rateBefore = this.rateField.isFocused();
        final boolean handled = super.mouseClicked(mouseX, mouseY, button);
        // A field clicked into has its number selected, so what is typed takes its place.
        selectAllIfFocused(this.priorityField, priorityBefore);
        selectAllIfFocused(this.rateField, rateBefore);
        return handled;
    }

    @Override
    public boolean mouseScrolled(final double mouseX, final double mouseY, final double scrollX,
                                 final double scrollY) {
        if (scrollY == 0) {
            return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        }
        final boolean up = scrollY > 0;
        final boolean many = hasShiftDown();
        final int ghost = CableEndLayout.ghostAt(mouseX - this.leftPos, mouseY - this.topPos);
        if (ghost >= 0) {
            final int first = up ? many ? CableEndMenu.FIRST_MUCH_MORE : CableEndMenu.FIRST_MORE
                    : many ? CableEndMenu.FIRST_MUCH_LESS : CableEndMenu.FIRST_LESS;
            sendButton(first + ghost);
            return true;
        }
        if (this.priorityField.isMouseOver(mouseX, mouseY)) {
            sendButton(up ? many ? CableEndMenu.PRIORITY_UP_TEN : CableEndMenu.PRIORITY_UP
                    : many ? CableEndMenu.PRIORITY_DOWN_TEN : CableEndMenu.PRIORITY_DOWN);
            return true;
        }
        if (this.rateField.isMouseOver(mouseX, mouseY)) {
            sendButton(up ? many ? CableEndMenu.RATE_UP_TEN : CableEndMenu.RATE_UP
                    : many ? CableEndMenu.RATE_DOWN_TEN : CableEndMenu.RATE_DOWN);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /* A field being typed in keeps every key but Escape, which closes the screen, and Enter, which sets the number. */
    @Override
    public boolean keyPressed(final int keyCode, final int scanCode, final int modifiers) {
        for (final EditBox field : new EditBox[] {this.priorityField, this.rateField}) {
            if (!field.isFocused() || keyCode == InputConstants.KEY_ESCAPE) {
                continue;
            }
            if (keyCode == InputConstants.KEY_RETURN || keyCode == InputConstants.KEY_NUMPADENTER) {
                field.setFocused(false);
                setFocused(null);
                return true;
            }
            return field.keyPressed(keyCode, scanCode, modifiers) || field.canConsumeInput();
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(final GuiGraphics g, final int mouseX, final int mouseY, final float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        final double x = mouseX - this.leftPos;
        final double y = mouseY - this.topPos;
        final int ghost = CableEndLayout.ghostAt(x, y);
        if (ghost >= 0) {
            g.renderComponentTooltip(this.font, ghostTooltip(ghost), mouseX, mouseY);
            return;
        }
        final int channel = CableEndLayout.channelAt(x, y);
        if (channel >= 0) {
            final DyeColor dye = DyeColor.byId(channel);
            g.renderTooltip(this.font, GameText.component(EndTexts.CHANNEL_TIP.with(Text.literal(String.valueOf(
                    channel + 1)), GameText.of(Component.translatable("color.minecraft." + dye.getName())))),
                    mouseX, mouseY);
        }
    }

    @Override
    protected void init() {
        super.init();
        this.modeKeys.clear();
        final List<EndMode> modes = EndMode.round();
        for (int k = 0; k < modes.size(); k++) {
            final int pressed = CableEndMenu.FIRST_MODE + k;
            this.modeKeys.add(addRenderableWidget(Button.builder(GameText.component(modes.get(k).text()),
                    button -> sendButton(pressed)).bounds(this.leftPos + CableEndLayout.modeX(k),
                    this.topPos + CableEndLayout.MODE_Y, CableEndLayout.modeWidth(k), CableEndLayout.KEY_HEIGHT)
                    .build()));
        }
        this.priorityField = numberRow(CableEndLayout.PRIORITY_Y, CableEndLayout.PRIORITY_FIELD_WIDTH,
                CableEndMenu.PRIORITY_DOWN, CableEndMenu.PRIORITY_DOWN_TEN, CableEndMenu.PRIORITY_UP,
                CableEndMenu.PRIORITY_UP_TEN, EndTexts.PRIORITY);
        this.rateField = numberRow(CableEndLayout.RATE_Y, CableEndLayout.RATE_FIELD_WIDTH, CableEndMenu.RATE_DOWN,
                CableEndMenu.RATE_DOWN_TEN, CableEndMenu.RATE_UP, CableEndMenu.RATE_UP_TEN, EndTexts.RATE);
        this.filterKey = addRenderableWidget(Button.builder(filterWord(this.menu.end()),
                button -> sendButton(CableEndMenu.FILTER_MODE)).bounds(this.leftPos + CableEndLayout.FILTER_X,
                this.topPos + CableEndLayout.FILTER_Y, CableEndLayout.FILTER_WIDTH, CableEndLayout.KEY_HEIGHT)
                .build());
        showEnd(this.menu.end());
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        // A field left, by a click elsewhere or by Enter, sets what was typed in it.
        if (this.priorityWasFocused && !this.priorityField.isFocused()) {
            typed(false, this.priorityField.getValue());
        }
        if (this.rateWasFocused && !this.rateField.isFocused()) {
            typed(true, this.rateField.getValue());
        }
        this.priorityWasFocused = this.priorityField.isFocused();
        this.rateWasFocused = this.rateField.isFocused();
        showEnd(this.menu.end());
    }

    @Override
    protected void renderBg(final GuiGraphics g, final float partialTick, final int mouseX, final int mouseY) {
        final Colours c = PALETTE.get();
        window(g, c, this.leftPos, this.topPos, this.leftPos + CableEndLayout.WIDTH,
                this.topPos + CableEndLayout.PANEL_HEIGHT);
        window(g, c, this.leftPos, this.topPos + CableEndLayout.INVENTORY_TOP, this.leftPos + CableEndLayout.WIDTH,
                this.topPos + CableEndLayout.HEIGHT);
        for (final Slot slot : this.menu.slots) {
            if (slot.isActive()) {
                slotFrame(g, c, this.leftPos + slot.x - 1, this.topPos + slot.y - 1);
            }
        }
        final CableEnd end = this.menu.end();
        channels(g, c, end.channel());
        ghosts(g, c, end);
    }

    /* The words, drawn over the keys: the title, each row's word, the rate's unit, the hint, the chosen keys framed. */
    @Override
    protected void renderLabels(final GuiGraphics g, final int mouseX, final int mouseY) {
        final Colours c = PALETTE.get();
        final CableEnd end = this.menu.end();
        fitted(g, this.title.getString(), CableEndLayout.TITLE_X, CableEndLayout.TITLE_Y, CableEndLayout.TITLE_WIDTH,
                1.0F, c.title());
        word(g, c, EndTexts.MODE, CableEndLayout.MODE_Y, true);
        word(g, c, EndTexts.PRIORITY, CableEndLayout.PRIORITY_Y, false);
        word(g, c, EndTexts.RATE, CableEndLayout.RATE_Y, false);
        word(g, c, EndTexts.CHANNEL, CableEndLayout.CHANNEL_Y, false);
        word(g, c, EndTexts.FILTER, CableEndLayout.FILTER_Y, false);
        Draw.text(g, this.font, GameText.resolve(items() ? EndTexts.UNIT_ITEMS : EndTexts.UNIT_FLUID),
                CableEndLayout.unitX(), CableEndLayout.wordY(CableEndLayout.RATE_Y), c.title());
        fitted(g, GameText.resolve(hint(end)), CableEndLayout.WORD_X, CableEndLayout.HINT_Y,
                CableEndLayout.WIDTH - 2 * CableEndLayout.WORD_X, CableEndLayout.SMALL, c.hint());
        Draw.text(g, this.font, this.playerInventoryTitle, CableEndLayout.INVENTORY_X,
                CableEndLayout.INVENTORY_WORD_Y, c.title());
        final int chosen = EndMode.round().indexOf(end.mode());
        if (chosen >= 0) {
            Draw.outline(g, CableEndLayout.modeX(chosen), CableEndLayout.MODE_Y, CableEndLayout.modeWidth(chosen),
                    CableEndLayout.KEY_HEIGHT, c.chosen());
        }
    }

    /* A number row: its minus and plus keys, ten at a time with Shift, and its field between them. */
    private EditBox numberRow(final int y, final int fieldWidth, final int down, final int downTen, final int up,
                              final int upTen, final TextKey name) {
        final Tooltip steps = Tooltip.create(GameText.component(EndTexts.STEPS));
        addRenderableWidget(Button.builder(Component.literal("-"), button -> sendButton(hasShiftDown() ? downTen
                : down)).bounds(this.leftPos + CableEndLayout.STEP_X, this.topPos + y, CableEndLayout.STEP_WIDTH,
                CableEndLayout.KEY_HEIGHT).tooltip(steps).build());
        final EditBox field = addRenderableWidget(new EditBox(this.font, this.leftPos + CableEndLayout.FIELD_X,
                this.topPos + y, fieldWidth, CableEndLayout.KEY_HEIGHT, GameText.component(name)));
        field.setMaxLength(FIELD_MAX_LENGTH);
        field.setFilter(typed -> typed.matches("-?\\d*"));
        addRenderableWidget(Button.builder(Component.literal("+"), button -> sendButton(hasShiftDown() ? upTen
                : up)).bounds(this.leftPos + CableEndLayout.plusX(fieldWidth), this.topPos + y,
                CableEndLayout.STEP_WIDTH, CableEndLayout.KEY_HEIGHT).tooltip(steps).build());
        return field;
    }

    /* The end as the server last said it, in the fields no one is typing in and on the filter's key. */
    private void showEnd(final CableEnd end) {
        if (!this.priorityField.isFocused()) {
            setIfChanged(this.priorityField, String.valueOf(end.priority()));
        }
        if (!this.rateField.isFocused()) {
            setIfChanged(this.rateField, String.valueOf(end.rate()));
        }
        final Component word = filterWord(end);
        if (!this.filterKey.getMessage().getString().equals(word.getString())) {
            this.filterKey.setMessage(word);
        }
    }

    private static void selectAllIfFocused(final EditBox field, final boolean before) {
        if (field.isFocused() && !before) {
            field.moveCursorToEnd(false);
            field.setHighlightPos(0);
        }
    }

    private static void setIfChanged(final EditBox field, final String value) {
        if (!field.getValue().equals(value)) {
            field.setValue(value);
        }
    }

    /* Sends the number typed in a field, read as the most a field holds; nothing for a field left empty. */
    private void typed(final boolean rate, final String value) {
        if (value.isEmpty() || "-".equals(value)) {
            return;
        }
        try {
            PacketDistributor.sendToServer(new EndNumberPayload(this.menu.pos(), rate, Long.parseLong(value)));
        } catch (final NumberFormatException tooLong) {
            // The field holds ten characters, so only a number past a long's reach is left unread.
        }
    }

    private static Component filterWord(final CableEnd end) {
        return GameText.component(end.filter().mode() == ItemFilter.Mode.ONLY ? EndTexts.ONLY : EndTexts.ALL_BUT);
    }

    private boolean items() {
        return this.menu.grid() == GridKind.ITEM;
    }

    /* The sixteen channels, each in its dye's colour on a black edge, the chosen one framed. */
    private void channels(final GuiGraphics g, final Colours c, final int chosen) {
        for (int k = 0; k < CableEndLayout.CHANNELS; k++) {
            final int x = this.leftPos + CableEndLayout.channelX(k);
            final int y = this.topPos + CableEndLayout.channelY(k);
            g.fill(x, y, x + CableEndLayout.CHANNEL_WIDTH, y + CableEndLayout.CHANNEL_HEIGHT, c.outline());
            g.fill(x + 1, y + 1, x + CableEndLayout.CHANNEL_WIDTH - 1, y + CableEndLayout.CHANNEL_HEIGHT - 1,
                    0xFF << 24 | DyeColor.byId(k).getTextureDiffuseColor());
            if (k == chosen) {
                Draw.outline(g, x - 1, y - 1, CableEndLayout.CHANNEL_WIDTH + 2, CableEndLayout.CHANNEL_HEIGHT + 2,
                        c.chosen());
                Draw.outline(g, x - 2, y - 2, CableEndLayout.CHANNEL_WIDTH + 4, CableEndLayout.CHANNEL_HEIGHT + 4,
                        c.outline());
            }
        }
    }

    /* The nine filter slots: each rule's thing faded, as a ghost is, its kind marked, the amount kept in yellow. */
    private void ghosts(final GuiGraphics g, final Colours c, final CableEnd end) {
        final List<ItemFilter.Rule> rules = end.filter().rules();
        for (int k = 0; k < CableEndLayout.GHOSTS; k++) {
            final int x = this.leftPos + CableEndLayout.ghostX(k);
            final int y = this.topPos + CableEndLayout.GHOST_Y;
            slotFrame(g, c, x - 1, y - 1);
            if (k >= rules.size()) {
                continue;
            }
            final ItemFilter.Rule rule = rules.get(k);
            icon(g, rule, x, y);
            g.pose().pushPose();
            g.pose().translate(0, 0, OVER_ITEMS);
            g.fill(x, y, x + ITEM_SIZE, y + ITEM_SIZE, c.veil());
            g.pose().translate(0, 0, OVER_VEIL - OVER_ITEMS);
            final String mark = rule instanceof ItemFilter.Tag ? "#" : rule instanceof ItemFilter.Mod ? "@" : "";
            if (!mark.isEmpty()) {
                Draw.textScaled(g, this.font, mark, x, y, c.chosen(), CableEndLayout.SMALL);
            }
            if (rule.amount() > 0) {
                final String kept = UnitFormatter.forCurrentLocale().compact(rule.amount());
                final int width = (int) Math.ceil(this.font.width(kept) * CableEndLayout.SMALL);
                Draw.textScaled(g, this.font, kept, x + ITEM_SIZE + 1 - width, y + ITEM_SIZE - 6, c.keep(),
                        CableEndLayout.SMALL);
            }
            g.pose().popPose();
        }
    }

    /* What a rule names, drawn as its item, its liquid's bucket or the liquid itself, or a material's colour. */
    private void icon(final GuiGraphics g, final ItemFilter.Rule rule, final int x, final int y) {
        final GridKind grid = this.menu.grid();
        final Optional<String> id = switch (rule) {
            case ItemFilter.Fuzzy fuzzy -> Optional.of(fuzzy.item());
            case ItemFilter.Exact exact -> Optional.of(exact.pattern().itemId());
            case ItemFilter.Tag tag -> EndFilters.firstOfTag(grid, tag.tag()).map(ResourceLocation::toString);
            case ItemFilter.Mod mod -> Optional.empty();
        };
        if (rule instanceof ItemFilter.Mod mod) {
            g.renderItem(modIcon(mod.mod()), x, y);
            return;
        }
        final ResourceLocation key = id.map(ResourceLocation::tryParse).orElse(null);
        if (key == null) {
            return;
        }
        if (grid == GridKind.ITEM) {
            g.renderItem(new ItemStack(BuiltInRegistries.ITEM.get(key)), x, y);
            return;
        }
        final Fluid fluid = BuiltInRegistries.FLUID.get(key);
        if (fluid != Fluids.EMPTY) {
            if (fluid.getBucket() != Items.AIR) {
                g.renderItem(new ItemStack(fluid.getBucket()), x, y);
            } else {
                fluidSprite(g, fluid, x, y);
            }
            return;
        }
        final Optional<Material> material = Materials.byId(key);
        if (material.isPresent()) {
            g.fill(x + SWATCH_INSET, y + SWATCH_INSET, x + ITEM_SIZE - SWATCH_INSET, y + ITEM_SIZE - SWATCH_INSET,
                    0xFF << 24 | material.get().colour());
        }
    }

    private static void fluidSprite(final GuiGraphics g, final Fluid fluid, final int x, final int y) {
        final IClientFluidTypeExtensions client = IClientFluidTypeExtensions.of(fluid);
        final FluidStack stack = new FluidStack(fluid, 1);
        final TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(client.getStillTexture(stack));
        final int tint = client.getTintColor(stack);
        g.blit(x, y, 0, ITEM_SIZE, ITEM_SIZE, sprite, (tint >> 16 & 0xFF) / 255F, (tint >> 8 & 0xFF) / 255F,
                (tint & 0xFF) / 255F, 1F);
    }

    /* The first thing a mod adds, standing for everything it does. */
    private ItemStack modIcon(final String mod) {
        return this.modIcons.computeIfAbsent(mod, namespace -> {
            for (final Item item : BuiltInRegistries.ITEM) {
                if (item != Items.AIR && BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(namespace)) {
                    return new ItemStack(item);
                }
            }
            return ItemStack.EMPTY;
        });
    }

    /* What a filter slot's tooltip says: what its rule names, how much is kept, and what clicking it does. */
    private List<Component> ghostTooltip(final int k) {
        final List<ItemFilter.Rule> rules = this.menu.end().filter().rules();
        final List<Component> lines = new ArrayList<>();
        if (k >= rules.size()) {
            lines.add(GameText.component(EndTexts.GHOST_EMPTY));
            return lines;
        }
        final ItemFilter.Rule rule = rules.get(k);
        lines.add(GameText.component(ruleName(rule)));
        final String kept = UnitFormatter.forCurrentLocale().compact(rule.amount());
        lines.add(GameText.component(rule.amount() <= 0 ? EndTexts.GHOST_NO_KEEP.text()
                : (items() ? EndTexts.GHOST_KEEP_ITEMS : EndTexts.GHOST_KEEP_FLUID).with(kept)));
        final int dim = PALETTE.get().hint();
        for (final TextKey use : new TextKey[] {EndTexts.GHOST_USE, EndTexts.GHOST_CYCLE, EndTexts.GHOST_SCROLL}) {
            lines.add(GameText.component(use).withColor(dim & 0xFFFFFF));
        }
        return lines;
    }

    /* What a rule names, as a player reads it. */
    private Text ruleName(final ItemFilter.Rule rule) {
        return switch (rule) {
            case ItemFilter.Fuzzy fuzzy -> thingName(fuzzy.item());
            case ItemFilter.Exact exact -> thingName(exact.pattern().itemId());
            case ItemFilter.Tag tag -> EndTexts.GHOST_TAG.with(Text.literal("#" + tag.tag()));
            case ItemFilter.Mod mod -> EndTexts.GHOST_MOD.with(Text.literal(ModList.get()
                    .getModContainerById(mod.mod()).map(container -> container.getModInfo().getDisplayName())
                    .orElse(mod.mod())));
        };
    }

    private Text thingName(final String id) {
        return FlowSightClient.nameOf(this.menu.grid(), id).orElseGet(() -> Text.literal(id));
    }

    /* The line under the filter: off, what is kept, what an all-but filter keeps out, or how fast the end goes. */
    private Text hint(final CableEnd end) {
        if (end.mode() == EndMode.OFF) {
            return EndTexts.HINT_OFF.text();
        }
        final ItemFilter filter = end.filter();
        if (filter.mode() == ItemFilter.Mode.ONLY) {
            for (final ItemFilter.Rule rule : filter.rules()) {
                if (rule.amount() > 0) {
                    final String kept = UnitFormatter.forCurrentLocale().compact(rule.amount());
                    return (items() ? EndTexts.HINT_KEEPS_ITEMS : EndTexts.HINT_KEEPS_FLUID).with(kept,
                            ruleName(rule));
                }
            }
        } else if (!filter.rules().isEmpty()) {
            return EndTexts.HINT_ALL_BUT.text();
        }
        if (end.rate() <= 0) {
            return EndTexts.HINT_FREE.text();
        }
        return (items() ? EndTexts.HINT_RATE_ITEMS : EndTexts.HINT_RATE_FLUID).with(
                UnitFormatter.forCurrentLocale().compact(end.rate()));
    }

    /* A row's word, shrunk to fit the room before the row's keys. */
    private void word(final GuiGraphics g, final Colours c, final TextKey word, final int rowY,
                      final boolean modeRow) {
        fitted(g, GameText.resolve(word), CableEndLayout.WORD_X, CableEndLayout.wordY(rowY),
                CableEndLayout.wordRoom(modeRow), 1.0F, c.title());
    }

    /* Text at most {@code scale}, shrunk further to fit {@code room}. */
    private void fitted(final GuiGraphics g, final String text, final int x, final int y, final int room,
                        final float scale, final int colour) {
        final float fit = Math.min(scale, room / (float) Math.max(1, this.font.width(text)));
        if (fit >= 1.0F) {
            Draw.text(g, this.font, text, x, y, colour);
        } else {
            Draw.textScaled(g, this.font, text, x, y, colour, fit);
        }
    }

    /*
     * A window as the game draws its own: the black outline stepping in at each corner, a white bevel two pixels deep on
     * the top and left, a dark one on the bottom and right, and the grey ground.
     */
    private static void window(final GuiGraphics g, final Colours c, final int x0, final int y0, final int x1,
                               final int y1) {
        g.fill(x0 + 2, y0 + 1, x1 - 2, y1 - 1, c.ground());
        g.fill(x0 + 1, y0 + 2, x1 - 1, y1 - 2, c.ground());
        g.fill(x0 + 2, y0, x1 - 2, y0 + 1, c.outline());
        g.fill(x0 + 2, y1 - 1, x1 - 2, y1, c.outline());
        g.fill(x0, y0 + 2, x0 + 1, y1 - 2, c.outline());
        g.fill(x1 - 1, y0 + 2, x1, y1 - 2, c.outline());
        for (final int[] corner : new int[][] {{x0 + 1, y0 + 1}, {x1 - 2, y0 + 1}, {x0 + 1, y1 - 2}, {x1 - 2, y1 - 2}}) {
            g.fill(corner[0], corner[1], corner[0] + 1, corner[1] + 1, c.outline());
        }
        g.fill(x0 + 2, y0 + 1, x1 - 3, y0 + 3, c.light());
        g.fill(x0 + 1, y0 + 2, x0 + 3, y1 - 3, c.light());
        g.fill(x0 + 3, y1 - 3, x1 - 2, y1 - 1, c.dark());
        g.fill(x1 - 3, y0 + 3, x1 - 1, y1 - 2, c.dark());
        g.fill(x1 - 3, y0 + 2, x1 - 2, y0 + 3, c.ground());
        g.fill(x0 + 2, y1 - 3, x0 + 3, y1 - 2, c.ground());
    }

    /* A slot as the game draws one: 18 square, dark on its top and left edges, white on its bottom and right. */
    private static void slotFrame(final GuiGraphics g, final Colours c, final int x, final int y) {
        g.fill(x, y, x + 18, y + 18, c.slot());
        g.fill(x, y, x + 17, y + 1, c.slotDark());
        g.fill(x, y, x + 1, y + 17, c.slotDark());
        g.fill(x + 1, y + 17, x + 18, y + 18, c.light());
        g.fill(x + 17, y + 1, x + 18, y + 18, c.light());
    }

    /**
     * The screen's colours, the game's own.
     *
     * @param ground   a window's grey
     * @param light    its white bevel, and a slot's lit edges
     * @param dark     its dark bevel
     * @param outline  its black outline, and a channel's edge
     * @param title    the words, as the game writes a window's title
     * @param hint     the line under the filter
     * @param keep     the amount kept, in a filter slot
     * @param slot     a slot's ground
     * @param slotDark a slot's shaded edges
     * @param chosen   the frame of the chosen mode and channel, and the mark of a tag's or a mod's rule
     * @param veil     what fades a filter slot's thing, as a ghost
     */
    public record Colours(int ground, int light, int dark, int outline, int title, int hint, int keep, int slot,
                          int slotDark, int chosen, int veil) {
    }
}
