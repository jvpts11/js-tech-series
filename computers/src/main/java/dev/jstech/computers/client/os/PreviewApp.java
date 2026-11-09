/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.client.os;

import dev.jstech.computers.operation.payload.RequestFileContentPayload;
import dev.jstech.computers.os.fs.PixImage;
import dev.jstech.core.client.gui.component.Draw;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Preview, Quince's viewer (computers/docs/design/quince.md): a picture on this computer, drawn at the largest whole
 * size the window has room for, its name on the window's bar. It shows; drawing is Paint's.
 */
@TextHolder
final class PreviewApp implements IDesktopApp, CodeFileReplies.IReader {

    private final BlockPos host;
    /** The picture shown, or null before one is opened. */
    @Nullable
    private PixImage image;
    /** The picture's path, or empty before one is opened. */
    private String path = "";
    /** What the window says when there is no picture to show. */
    private TextKey note = NO_PICTURE;

    private static final TextKey NO_PICTURE = TextKey.of("jsc.quince.preview.no_picture",
            "Open a picture from the Seeker to see it here.");
    private static final TextKey NOT_A_PICTURE = TextKey.of("jsc.quince.preview.not_a_picture",
            "This file is not a picture Preview can show.");

    PreviewApp(final BlockPos host) {
        this.host = host;
    }

    @Override
    public String title() {
        return this.path.isEmpty() ? QuinceAboutApp.programName("preview")
                : this.path.substring(this.path.lastIndexOf('/') + 1);
    }

    @Override
    public int defaultWidth() {
        return 200;
    }

    @Override
    public int defaultHeight() {
        return 150;
    }

    @Override
    public void openFile(final String target) {
        if (target == null || target.isEmpty()) {
            return;
        }
        CodeFileReplies.expectContent(this, target);
        PacketDistributor.sendToServer(new RequestFileContentPayload(this.host, target));
    }

    @Override
    public void onContent(final String target, final String content, final boolean exists) {
        final PixImage loaded = exists ? PixImage.decode(content) : null;
        this.image = loaded;
        this.path = loaded == null ? "" : target;
        this.note = loaded == null ? NOT_A_PICTURE : NO_PICTURE;
    }

    @Override
    public void renderContent(final GuiGraphics g, final Font font, final int x, final int y, final int width,
                              final int height, final int mouseX, final int mouseY, final float partialTick) {
        final QuinceLook.Colours c = QuinceLook.PULP.colours();
        g.fill(x, y, x + width, y + height, c.shade());
        if (this.image == null) {
            Draw.textCentered(g, font, GameText.resolve(this.note), x + width / 2, y + height / 2 - 4, c.light());
            return;
        }
        SigmaImages.paint(g, this.image, x + 4, y + 4, width - 8, height - 8);
    }
}
