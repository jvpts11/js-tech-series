/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.config;

import dev.jstech.core.api.client.ISkin;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.client.gui.component.Popup;
import dev.jstech.core.client.gui.component.UiContext;
import dev.jstech.core.config.ConfigKey;
import dev.jstech.core.config.ConfigScreenTexts;
import dev.jstech.core.config.ConfigTraits;
import dev.jstech.core.gui.style.StyleRole;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.text.GameText;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.FormattedCharSequence;
import org.slf4j.Logger;

/**
 * A setting's whole description: its name, a picture beside the text when the setting has one (a strip of frames shown
 * in turn), every paragraph the mod's language gives it, its default and bounds, and a link to the manual's page that
 * tells more. Shown as the tooltip of a setting's name, and pinned where it was clicked, where its link can be
 * followed.
 */
final class ConfigDescription extends Popup {

    private final Card card;
    private final Consumer<ConfigTraits.ManualPage> onLink;
    private final int anchorX;
    private final int anchorY;

    /** The widest a description is, and the box its picture fits in. */
    private static final int WIDTH = 220;
    private static final int PICTURE = 64;
    private static final int PAD = 6;
    private static final int LINE = 10;
    /** How long a frame of a picture's strip shows. */
    private static final long FRAME_MILLIS = 500L;
    /** Where a PNG keeps its width and height, after its signature and the header's length and name. */
    private static final int PNG_WIDTH_AT = 16;
    /** Each picture's size, read once from its file. */
    private static final Map<ResourceLocation, int[]> SIZES = new ConcurrentHashMap<>();
    private static final Logger LOGGER = CoreLogs.INTERFACE.logger();

    ConfigDescription(final Card card, final Consumer<ConfigTraits.ManualPage> onLink, final int anchorX,
                      final int anchorY) {
        super("", card.width(), card.height());
        this.card = card;
        this.onLink = onLink;
        this.anchorX = anchorX;
        this.anchorY = anchorY;
    }

    /**
     * A description laid out, ready to draw.
     *
     * @param title   the setting's name
     * @param picture the picture beside the text, if it has one
     * @param lines   the description's lines, a blank one between paragraphs
     * @param meta    its default and bounds
     * @param link    the manual's page, if it has one
     * @param width   how wide the card is
     * @param height  how tall
     */
    record Card(String title, Optional<Picture> picture, List<FormattedCharSequence> lines, String meta,
                Optional<ConfigTraits.ManualPage> link, int width, int height) {

        Card {
            lines = List.copyOf(lines);
        }

        /** Where the text starts across the card, right of the picture when there is one. */
        int textX() {
            return PAD + picture.map(p -> p.width() + PAD).orElse(0);
        }

        /** Where the link sits down the card. */
        int linkY() {
            return height - PAD - LINE + 1;
        }
    }

    /**
     * A picture of a description.
     *
     * @param texture the texture
     * @param width   how wide it is drawn
     * @param height  how tall a frame is drawn
     * @param frames  how many frames its strip holds
     * @param fileWidth  the texture's width in its file
     * @param fileHeight the texture's height in its file
     */
    record Picture(ResourceLocation texture, int width, int height, int frames, int fileWidth, int fileHeight) {
    }

    /** {@code key}'s description laid out in {@code font}, with {@code meta} its default and bounds. */
    static Card cardOf(final ConfigScreenModel model, final ConfigKey<?> key, final Font font, final String meta) {
        final Optional<Picture> picture = key.traits().picture().flatMap(texture -> pictureOf(texture,
                key.traits().frames()));
        final int textWidth = WIDTH - 2 * PAD - picture.map(p -> p.width() + PAD).orElse(0);
        final List<FormattedCharSequence> lines = new ArrayList<>();
        final String description = model.description(key);
        if (!description.isEmpty()) {
            for (final String paragraph : description.split("\n")) {
                if (!lines.isEmpty()) {
                    lines.add(FormattedCharSequence.EMPTY);
                }
                lines.addAll(font.split(Component.literal(paragraph), textWidth));
            }
        }
        final int textHeight = lines.size() * LINE;
        final int body = Math.max(textHeight, picture.map(Picture::height).orElse(0));
        final boolean link = key.traits().manual().isPresent();
        final int height = PAD + LINE + 3 + body + (body > 0 ? 4 : 0) + LINE + (link ? LINE + 2 : 0) + PAD - 2;
        return new Card(model.name(key).getString(), picture, lines, meta, key.traits().manual(), WIDTH, height);
    }

    /** Draws {@code card} with its top left at {@code (x, y)}, in the style's tooltip. */
    static void draw(final GuiGraphics g, final UiContext ctx, final Card card, final int x, final int y) {
        final ISkin skin = ctx.skin();
        skin.tooltip(g, x, y, card.width(), card.height());
        skin.drawText(g, ctx.font(), card.title(), x + PAD, y + PAD, skin.text());
        final int bodyY = y + PAD + LINE + 3;
        card.picture().ifPresent(picture -> {
            final int frame = (int) (Util.getMillis() / FRAME_MILLIS % picture.frames());
            final int frameHeight = picture.fileHeight() / picture.frames();
            g.blit(picture.texture(), x + PAD, bodyY, picture.width(), picture.height(), 0, frame * frameHeight,
                    picture.fileWidth(), frameHeight, picture.fileWidth(), picture.fileHeight());
        });
        for (int i = 0; i < card.lines().size(); i++) {
            Draw.text(g, ctx.font(), card.lines().get(i), x + card.textX(), bodyY + i * LINE, skin.dim());
        }
        final int metaY = y + card.height() - PAD - LINE + 1 - (card.link().isPresent() ? LINE + 2 : 0);
        skin.drawText(g, ctx.font(), card.meta(), x + PAD, metaY, ConfigWidgets.colour(skin, StyleRole.NOTICE));
        if (card.link().isPresent()) {
            final String link = GameText.resolve(ConfigScreenTexts.READ_MANUAL);
            final int ink = ConfigWidgets.colour(skin, StyleRole.INFO);
            skin.drawText(g, ctx.font(), link, x + PAD, y + card.linkY(), ink);
            g.fill(x + PAD, y + card.linkY() + 9, x + PAD + skin.textWidth(ctx.font(), link), y + card.linkY() + 10,
                    ink);
        }
    }

    @Override
    public void placeIn(final int cx, final int cy, final int cw, final int ch) {
        // Pinned where it was clicked, kept inside the screen.
        final int x = Math.max(cx + 2, Math.min(cx + cw - card.width() - 2, anchorX));
        final int y = Math.max(cy + 2, Math.min(cy + ch - card.height() - 2, anchorY));
        setBounds(x, y, card.width(), card.height());
    }

    @Override
    public void renderIn(final GuiGraphics g, final UiContext ctx, final int cx, final int cy, final int cw,
                         final int ch) {
        placeIn(cx, cy, cw, ch);
        render(g, ctx);
    }

    @Override
    public void render(final GuiGraphics g, final UiContext ctx) {
        draw(g, ctx, card, x(), y());
    }

    @Override
    public boolean mouseClicked(final double mx, final double my, final int button) {
        if (contains(mx, my) && card.link().isPresent() && my >= y() + card.linkY() - 1
                && my < y() + card.linkY() + LINE) {
            onLink.accept(card.link().get());
            close();
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    /* A picture's texture, measured from its file; none when it cannot be read. */
    private static Optional<Picture> pictureOf(final String texture, final int frames) {
        final ResourceLocation id = ResourceLocation.tryParse(texture);
        if (id == null) {
            return Optional.empty();
        }
        final int[] size = SIZES.computeIfAbsent(id, ConfigDescription::measure);
        if (size[0] <= 0 || size[1] <= 0) {
            return Optional.empty();
        }
        final int frameHeight = size[1] / Math.max(1, frames);
        final int width = Math.min(PICTURE, size[0]);
        final int height = Math.max(1, frameHeight * width / size[0]);
        return Optional.of(new Picture(id, width, height, Math.max(1, frames), size[0], size[1]));
    }

    /* A PNG's width and height, from its header; zero when it is not there or not a PNG. */
    private static int[] measure(final ResourceLocation id) {
        final Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(id);
        if (resource.isEmpty()) {
            return new int[] {0, 0};
        }
        try (InputStream in = resource.get().open()) {
            final byte[] header = in.readNBytes(PNG_WIDTH_AT + 8);
            if (header.length < PNG_WIDTH_AT + 8) {
                return new int[] {0, 0};
            }
            return new int[] {bigEndian(header, PNG_WIDTH_AT), bigEndian(header, PNG_WIDTH_AT + 4)};
        } catch (final IOException e) {
            LOGGER.warn("the picture {} of a setting could not be read", id, e);
            return new int[] {0, 0};
        }
    }

    private static int bigEndian(final byte[] bytes, final int at) {
        return (bytes[at] & 0xFF) << 24 | (bytes[at + 1] & 0xFF) << 16 | (bytes[at + 2] & 0xFF) << 8
                | bytes[at + 3] & 0xFF;
    }
}
