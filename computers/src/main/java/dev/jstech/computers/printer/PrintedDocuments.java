/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.printer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.jstech.core.text.LongText;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * How a {@link PrintedDocument} is saved, with the sheet that carries it and in a printer's queue, and how it travels
 * to the players: every string within the document's own bounds, so a packet can never carry more than a printer
 * would turn out.
 */
public final class PrintedDocuments {

    public static final Codec<PrintedDocument> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.optionalFieldOf("title", "").forGetter(PrintedDocument::title),
            Codec.STRING.optionalFieldOf("from", "").forGetter(PrintedDocument::from),
            Codec.STRING.optionalFieldOf("program", "").forGetter(PrintedDocument::program),
            Codec.STRING.optionalFieldOf("printer", "").forGetter(PrintedDocument::printer),
            Codec.STRING.listOf().optionalFieldOf("pages", List.of()).forGetter(PrintedDocument::pages),
            LongText.CODEC.optionalFieldOf("picture", "").forGetter(PrintedDocument::picture),
            Codec.STRING.optionalFieldOf("picture_name", "").forGetter(PrintedDocument::pictureName)
    ).apply(i, PrintedDocument::new));

    private static final StreamCodec<ByteBuf, String> NAME = ByteBufCodecs.stringUtf8(PrintedDocument.MAX_NAME);
    private static final StreamCodec<ByteBuf, String> PAGE = ByteBufCodecs.stringUtf8(PrintedDocument.MAX_PAGE_CHARS);
    private static final StreamCodec<ByteBuf, String> PICTURE = ByteBufCodecs.stringUtf8(PrintedDocument.MAX_PICTURE);

    public static final StreamCodec<ByteBuf, PrintedDocument> STREAM_CODEC = StreamCodec.of((buf, document) -> {
        NAME.encode(buf, document.title());
        NAME.encode(buf, document.from());
        NAME.encode(buf, document.program());
        NAME.encode(buf, document.printer());
        ByteBufCodecs.VAR_INT.encode(buf, document.pages().size());
        for (final String page : document.pages()) {
            PAGE.encode(buf, page);
        }
        PICTURE.encode(buf, document.picture());
        NAME.encode(buf, document.pictureName());
    }, buf -> {
        final String title = NAME.decode(buf);
        final String from = NAME.decode(buf);
        final String program = NAME.decode(buf);
        final String printer = NAME.decode(buf);
        final int count = ByteBufCodecs.VAR_INT.decode(buf);
        if (count < 0 || count > PrintedDocument.MAX_PAGES) {
            throw new DecoderException("A printed document holds at most " + PrintedDocument.MAX_PAGES + " pages");
        }
        final List<String> pages = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            pages.add(PAGE.decode(buf));
        }
        return new PrintedDocument(title, from, program, printer, pages, PICTURE.decode(buf), NAME.decode(buf));
    });

    private PrintedDocuments() {
    }
}
