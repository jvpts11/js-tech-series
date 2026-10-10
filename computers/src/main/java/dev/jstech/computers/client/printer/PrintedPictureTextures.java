/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.printer;

import com.mojang.blaze3d.platform.NativeImage;
import dev.jstech.computers.JsComputers;
import dev.jstech.computers.os.fs.PixImage;
import dev.jstech.computers.printer.PrintedDocument;
import dev.jstech.computers.printer.PrintedPicture;
import dev.jstech.computers.printer.PrinterModel;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The textures of printed pictures: each picture as its printer put it on paper, made once and kept while it is seen,
 * for the reading screen (the ink alone, over the sheet it draws) and for an item frame (the ink on a square of the
 * printer's paper, as a map fills its frame). Bounded: the ones not looked at for longest are let go.
 */
public final class PrintedPictureTextures {

    /** How many pictures are kept at once. */
    private static final int MOST = 32;
    /** The square sheet a framed picture is drawn on, in its texture's pixels, and the paper round the picture. */
    private static final int FRAME_SIDE = 128;
    private static final int FRAME_MARGIN = 8;
    /** How long a picture is safe from being let go after it was last drawn, so a busy view never thrashes. */
    private static final long GRACE_MILLIS = 2000L;
    private static final Map<String, Slot> KEPT = new LinkedHashMap<>(16, 0.75F, true);
    /** Pictures that did not decode, so a malformed one is read once and not again at every frame. */
    private static final Set<String> FAILED = new LinkedHashSet<>();
    private static long nextTexture;

    private PrintedPictureTextures() {
    }

    /** The ink of a printed picture, or null when the sheet holds none or it cannot be read. */
    @Nullable
    public static Entry ink(final PrintedDocument document) {
        return entry(document, false, 0, 0);
    }

    /** The picture on a square of its paper, for an item frame, or null when the sheet holds none. */
    @Nullable
    public static Entry framed(final PrintedDocument document, final int paper, final int bar) {
        return entry(document, true, paper, bar);
    }

    /** Lets every texture go, as the player leaves a world. */
    public static void forgetAll() {
        for (final Slot slot : KEPT.values()) {
            Minecraft.getInstance().getTextureManager().release(slot.entry.texture());
        }
        KEPT.clear();
        FAILED.clear();
    }

    @Nullable
    private static Entry entry(final PrintedDocument document, final boolean framed, final int paper, final int bar) {
        final PrinterModel model = document.printerModel();
        if (!document.isPicture() || model == null) {
            return null;
        }
        // The picture's own text is part of the key, so two different pictures can never share a texture.
        final String key = (framed ? "f" : "i") + model.serializedName() + ":" + document.picture();
        final long now = Util.getMillis();
        final Slot kept = KEPT.get(key);
        if (kept != null) {
            kept.lastUsed = now;
            return kept.entry;
        }
        if (FAILED.contains(document.picture())) {
            return null;
        }
        final PixImage picture = PixImage.decode(document.picture());
        if (picture == null) {
            FAILED.add(document.picture());
            if (FAILED.size() > MOST) {
                FAILED.remove(FAILED.iterator().next());
            }
            return null;
        }
        final PrintedPicture.Raster raster = PrintedPicture.print(picture, model.ink());
        final NativeImage image = framed ? framedImage(raster, model, paper, bar) : inkImage(raster);
        final ResourceLocation id = ResourceLocation.fromNamespaceAndPath(JsComputers.MODID,
                "printed_picture/" + nextTexture++ + (framed ? "_f" : "_i"));
        Minecraft.getInstance().getTextureManager().register(id, new DynamicTexture(image));
        final Entry entry = new Entry(id, image.getWidth(), image.getHeight());
        KEPT.put(key, new Slot(entry, now));
        trim(now);
        return entry;
    }

    /*
     * Lets go of the pictures not drawn for a while, eldest first, while there are too many. One drawn a moment
     * ago stays even past the limit: with more pictures in view than fit, letting those go would decode and
     * upload every one of them again at every frame.
     */
    private static void trim(final long now) {
        final Iterator<Slot> eldest = KEPT.values().iterator();
        while (KEPT.size() > MOST && eldest.hasNext()) {
            final Slot slot = eldest.next();
            if (now - slot.lastUsed < GRACE_MILLIS) {
                return;
            }
            Minecraft.getInstance().getTextureManager().release(slot.entry.texture());
            eldest.remove();
        }
    }

    private static NativeImage inkImage(final PrintedPicture.Raster raster) {
        final NativeImage image = new NativeImage(raster.width(), raster.height(), true);
        for (int y = 0; y < raster.height(); y++) {
            for (int x = 0; x < raster.width(); x++) {
                image.setPixelRGBA(x, y, abgr(raster.get(x, y)));
            }
        }
        return image;
    }

    /* The picture fitted into a square of paper with a margin, the fanfold's bands behind it when it has them. */
    private static NativeImage framedImage(final PrintedPicture.Raster raster, final PrinterModel model,
                                           final int paper, final int bar) {
        final NativeImage image = new NativeImage(FRAME_SIDE, FRAME_SIDE, true);
        final boolean bands = model.sheet() == PrinterModel.Sheet.FANFOLD;
        for (int y = 0; y < FRAME_SIDE; y++) {
            final int ground = bands && (y / 16) % 2 == 1 ? bar : paper;
            for (int x = 0; x < FRAME_SIDE; x++) {
                image.setPixelRGBA(x, y, abgr(ground));
            }
        }
        final int room = FRAME_SIDE - FRAME_MARGIN * 2;
        final float scale = Math.min((float) room / raster.width(), (float) room / raster.height());
        final int w = Math.max(1, Math.round(raster.width() * scale));
        final int h = Math.max(1, Math.round(raster.height() * scale));
        final int x0 = (FRAME_SIDE - w) / 2;
        final int y0 = (FRAME_SIDE - h) / 2;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int ink = raster.get((int) (x / scale), (int) (y / scale));
                if ((ink >>> 24) != 0) {
                    image.setPixelRGBA(x0 + x, y0 + y, abgr(over(ink, image.getPixelRGBA(x0 + x, y0 + y))));
                }
            }
        }
        return image;
    }

    /* An ink pixel laid over the paper's, by the ink's opacity; the paper pixel is in the image's own order. */
    private static int over(final int ink, final int paperAbgr) {
        final int a = ink >>> 24;
        final int pr = paperAbgr & 0xFF;
        final int pg = (paperAbgr >> 8) & 0xFF;
        final int pb = (paperAbgr >> 16) & 0xFF;
        final int r = (((ink >> 16) & 0xFF) * a + pr * (255 - a)) / 255;
        final int g = (((ink >> 8) & 0xFF) * a + pg * (255 - a)) / 255;
        final int b = ((ink & 0xFF) * a + pb * (255 - a)) / 255;
        return 0xFF << 24 | r << 16 | g << 8 | b;
    }

    /* The game's images keep their pixels as alpha, blue, green, red. */
    private static int abgr(final int argb) {
        return (argb >>> 24) << 24 | (argb & 0xFF) << 16 | (argb >> 8 & 0xFF) << 8 | (argb >> 16 & 0xFF);
    }

    /**
     * A picture's texture and its size in pixels.
     *
     * @param texture the texture's id
     * @param width   how many pixels across
     * @param height  how many pixels down
     */
    public record Entry(ResourceLocation texture, int width, int height) {
    }

    /* A kept texture and when it was last drawn. */
    private static final class Slot {
        private final Entry entry;
        private long lastUsed;

        private Slot(final Entry entry, final long lastUsed) {
            this.entry = entry;
            this.lastUsed = lastUsed;
        }
    }
}
