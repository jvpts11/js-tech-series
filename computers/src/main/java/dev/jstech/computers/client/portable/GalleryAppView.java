/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.portable;

import dev.jstech.computers.block.DataLinkNames;
import dev.jstech.computers.client.portable.IAppKit.Tone;
import dev.jstech.computers.client.portable.MobileCanvas.Align;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload;
import dev.jstech.computers.operation.payload.portable.PortableActionPayload.Action;
import dev.jstech.computers.operation.payload.portable.PortableGalleryPayload;
import dev.jstech.computers.portable.MobileApp;
import dev.jstech.core.jstechseries.network.DataLink;
import java.util.List;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * The gallery (computers/docs/design/wireless.md), as the K0.11 apps page drew it on the Hightide Wave: the pictures
 * taken, three to a row, the one chosen framed and ticked, its file's name, its size and the day it was taken, and
 * the personal computers of the network it is sent to as a PNG file, each with how it is on the network.
 */
final class GalleryAppView implements IMobileAppView {

    private static final int ASK_EVERY = 100;
    private static final int COLUMNS = 3;
    private static final String[] TICK = {"....X", "...X.", "X.X..", ".X..."};

    @Override
    public int askEvery() {
        return ASK_EVERY;
    }

    @Override
    @Nullable
    public PortableActionPayload ask(final UUID device, final MobileAppState state) {
        return PortableActionPayload.of(device, Action.GALLERY);
    }

    @Override
    public void paint(final AppPage page) {
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        final PortableGalleryPayload view = PortableApps.gallery(page.device());
        final List<PortableGalleryPayload.Picture> pictures = view == null ? List.of() : view.pictures();
        final float top = kit.bar(c, MobileApp.GALLERY, page.top(), MobileParts.name(MobileApp.GALLERY),
                count(pictures.size()), false);
        if (pictures.isEmpty()) {
            kit.note(c, top + 20, MobileTexts.say(AppTexts.NO_PICTURES), Tone.DIM);
            note(page, view, top + 32);
            return;
        }
        final PortableGalleryPayload.Picture chosen = chosen(page.state(), pictures);
        final float w = c.width();
        final float cell = (w - 4) / COLUMNS;
        final float sendTop = page.bottom() - 18 - Math.min(3, Math.max(1, view.targets().size())) * 15.0F;
        final int rows = Math.max(1, (int) ((sendTop - top - 26) / (cell + 1)));
        final int firstRow = Math.min(page.frame().scroll(), Math.max(0, (pictures.size() - 1) / COLUMNS + 1 - rows));
        for (int n = firstRow * COLUMNS; n < pictures.size() && n < (firstRow + rows) * COLUMNS; n++) {
            final PortableGalleryPayload.Picture picture = pictures.get(n);
            final float x = 1 + (n % COLUMNS) * (cell + 1);
            final float y = top + 1 + (n / COLUMNS - firstRow) * (cell + 1);
            final ResourceLocation texture = GalleryTextures.of(page.device(), picture.number());
            if (texture != null) {
                c.picture(texture, GalleryTextures.WIDTH, GalleryTextures.HEIGHT, GalleryTextures.SQUARE_U, 0,
                        GalleryTextures.HEIGHT, GalleryTextures.HEIGHT, x, y, cell, cell);
            } else {
                c.rect(x, y, cell, cell, kit.ink(Tone.SURFACE));
            }
            if (picture.number() == chosen.number()) {
                final int good = kit.ink(Tone.GOOD);
                c.rect(x, y, cell, 2, good);
                c.rect(x, y + cell - 2, cell, 2, good);
                c.rect(x, y, 2, cell, good);
                c.rect(x + cell - 2, y, 2, cell, good);
                c.disc(x + cell - 7, y + 7, 4.5F, good);
                c.glyph(TICK, x + cell - 9.5F, y + 5, kit.ink(Tone.SURFACE), 1);
            }
            c.press(x, y, cell, cell, MobileTap.run(() -> page.state().picture(picture.number())));
        }
        final float infoY = top + 2 + rows * (cell + 1);
        c.text(chosen.fileName(), 5, infoY, kit.ink(Tone.TEXT), MobileCanvas.SMALL);
        c.text(c.fit(MobileTexts.say(AppTexts.TAKEN_ON.with(chosen.width(), chosen.height(), chosen.day())),
                w - 40, MobileCanvas.SMALL), 5, infoY + 9, kit.ink(Tone.DIM), MobileCanvas.SMALL);
        final String delete = MobileTexts.say(AppTexts.DELETE);
        final float deleteW = c.width(delete, MobileCanvas.SMALL) + 4;
        c.text(delete, w - 5, infoY + 4, kit.ink(Tone.BAD), MobileCanvas.SMALL, Align.RIGHT, false);
        c.press(w - deleteW - 5, infoY, deleteW + 5, 14, MobileTap.run(() -> {
            page.send(PortableActionPayload.numbered(page.device(), Action.DELETE_PICTURE, "", chosen.number()));
            page.state().picture(-1);
        }));
        send(page, view, chosen, sendTop);
    }

    /* The computers the chosen picture goes to, each pressed to send it. */
    private static void send(final AppPage page, final PortableGalleryPayload view,
                             final PortableGalleryPayload.Picture chosen, final float top) {
        final MobileCanvas c = page.c();
        final IAppKit kit = page.kit();
        c.rect(0, top - 2, c.width(), page.bottom() - top + 2, kit.ink(Tone.SURFACE));
        float y = kit.section(c, top, MobileTexts.say(AppTexts.SEND_TO_A_COMPUTER));
        if (view.targets().isEmpty()) {
            kit.note(c, y + 2, MobileTexts.say(AppTexts.NO_COMPUTERS), Tone.DIM);
        }
        for (final PortableGalleryPayload.Target target : view.targets()) {
            if (y + kit.rowPitch() > page.bottom() - 8) {
                break;
            }
            y = kit.row(c, y, null, target.name(), linkName(target.link()), Tone.DIM, MobileTap.run(() ->
                    page.send(PortableActionPayload.numbered(page.device(), Action.SEND_PICTURE, target.node(),
                            chosen.number()))), false);
        }
        note(page, view, page.bottom() - 8);
    }

    /* What the last picture kept or sent came to, at the page's foot. */
    private static void note(final AppPage page, @Nullable final PortableGalleryPayload view, final float y) {
        if (view != null && !view.note().isEmpty()) {
            page.kit().note(page.c(), y, MobileTexts.say(view.note()), Tone.ACCENT);
        }
    }

    /* The picture chosen, or the newest when none is. */
    private static PortableGalleryPayload.Picture chosen(final MobileAppState state,
                                                         final List<PortableGalleryPayload.Picture> pictures) {
        for (final PortableGalleryPayload.Picture picture : pictures) {
            if (picture.number() == state.picture()) {
                return picture;
            }
        }
        return pictures.getLast();
    }

    /* How a computer is on the network: by Wi-Fi, or by its cable's name. */
    private static String linkName(final String link) {
        if (PortableGalleryPayload.Target.WIFI.equals(link)) {
            return MobileTexts.say(MobileTexts.WIFI);
        }
        final DataLink cable = link.isEmpty() ? null : DataLink.byName(link);
        return cable == null ? "" : MobileTexts.say(DataLinkNames.of(cable).text());
    }

    private static String count(final int pictures) {
        return pictures == 1 ? MobileTexts.say(AppTexts.ONE_PICTURE)
                : MobileTexts.say(AppTexts.PICTURES.with(pictures));
    }
}
