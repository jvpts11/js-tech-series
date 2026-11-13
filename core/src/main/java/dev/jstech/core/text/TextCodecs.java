/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.text;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.Utf8String;
import net.minecraft.network.VarInt;
import net.minecraft.network.codec.StreamCodec;

/**
 * Text on the wire, in the form it was made in, so the player at the other end reads it in their own language.
 *
 * <p>A sentence travels as its key and what goes into it; its English does not, since the other side has the same
 * mod and so the same English file. Everything is cut to what the wire takes rather than refused, because a string
 * past its cap does not trim itself on the wire: it throws, and takes the connection with it.
 */
public final class TextCodecs {

    public static final StreamCodec<ByteBuf, Text> STREAM_CODEC = StreamCodec.of(TextCodecs::write, TextCodecs::read);

    /**
     * Text in a save or a block's update, in the form it was made in: words that are data as themselves, a sentence as
     * its key, its English and what goes into it, so a player reads it in their own language.
     */
    public static final Codec<Text> CODEC = Codec.recursive("text", self -> Codec.either(Codec.STRING,
                    RecordCodecBuilder.<Written>create(instance -> instance.group(
                            Codec.STRING.fieldOf("key").forGetter(Written::key),
                            Codec.STRING.fieldOf("english").forGetter(Written::english),
                            self.listOf().optionalFieldOf("args", List.of()).forGetter(Written::args))
                            .apply(instance, Written::new)))
            .xmap(either -> either.map(Text::literal, Written::text),
                    text -> text instanceof Text.Translated translated ? Either.right(new Written(
                            translated.key().key(), translated.key().english(), translated.args()))
                            : Either.left(text.english())));

    /** The longest words of data that travel, and the longest key; a save holds text to the same lengths. */
    static final int MOST_LETTERS = 8_192;
    static final int MOST_KEY_LETTERS = 256;

    /** How many arguments a sentence carries, and how deeply sentences sit inside each other. */
    private static final int MOST_ARGS = 16;
    private static final int MOST_DEPTH = 20;

    private static final byte LITERAL = 0;
    private static final byte TRANSLATED = 1;

    private TextCodecs() {
    }

    private static void write(final ByteBuf buf, final Text text) {
        write(buf, text, 0);
    }

    private static void write(final ByteBuf buf, final Text text, final int depth) {
        if (text instanceof Text.Translated translated && depth < MOST_DEPTH) {
            buf.writeByte(TRANSLATED);
            Utf8String.write(buf, TextBounds.clip(translated.key().key(), MOST_KEY_LETTERS), MOST_KEY_LETTERS);
            final List<Text> args = translated.args();
            final int count = Math.min(args.size(), MOST_ARGS);
            VarInt.write(buf, count);
            for (int i = 0; i < count; i++) {
                write(buf, args.get(i), depth + 1);
            }
            return;
        }
        buf.writeByte(LITERAL);
        // A sentence nested past the depth travels as the English it reads as, which is still something to read.
        Utf8String.write(buf, TextBounds.clip(text.resolve(ITextLanguage.ENGLISH), MOST_LETTERS), MOST_LETTERS);
    }

    private static Text read(final ByteBuf buf) {
        return read(buf, 0);
    }

    private static Text read(final ByteBuf buf, final int depth) {
        final byte kind = buf.readByte();
        if (kind != TRANSLATED || depth >= MOST_DEPTH) {
            return Text.literal(Utf8String.read(buf, MOST_LETTERS));
        }
        final String key = Utf8String.read(buf, MOST_KEY_LETTERS);
        // A count no sender writes, below none, is read as none rather than failing the whole message.
        final int count = Math.clamp(VarInt.read(buf), 0, MOST_ARGS);
        final List<Text> args = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            args.add(read(buf, depth + 1));
        }
        try {
            // The English stays behind with the sender; the key stands in for it where this side has none.
            return new Text.Translated(new TextKey(key, key), args);
        } catch (final IllegalArgumentException notAKey) {
            return Text.literal(key);
        }
    }

    /*
     * A sentence as a save writes it, read as it stands: one whose key or English a save of another hand broke is
     * read as its words, as the wire reads one, rather than failing the whole save it is in.
     */
    private record Written(String key, String english, List<Text> args) {

        Text text() {
            try {
                return new Text.Translated(new TextKey(key, english), args);
            } catch (final IllegalArgumentException broken) {
                return Text.literal(english.isBlank() ? key : english);
            }
        }
    }
}
