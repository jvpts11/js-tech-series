/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Tech Series.
 */
package dev.jstech.tests.gametest;

import dev.jstech.computers.JsComputers;
import dev.jstech.core.JsCore;
import dev.jstech.industrial.JsIndustrial;
import dev.jstech.core.datagen.DeclaredTexts;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.ITextLanguage;
import dev.jstech.core.text.Text;
import dev.jstech.core.text.TextCodecs;
import dev.jstech.core.text.TextKey;
import dev.jstech.tests.JsTests;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.Collection;
import java.util.List;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Text as the game carries it: declared sentences found where they are declared, resolved in English on the server,
 * turned into the game's components, and sent over the wire in the form they were made in.
 */
@GameTestHolder(JsTests.MODID)
@PrefixGameTestTemplate(false)
public final class TextGameTests {

    private static final String ARENA = "empty";

    /** The toolkit's Confirm, which the core declares beside its dialog. */
    private static final String CONFIRM = "gui.jscore.confirm";
    private static final String CRASHED = "jscore.operation.failure.crashed";

    private TextGameTests() {
    }

    /**
     * What a mod declares beside its code is found without being listed anywhere, and every class that declares
     * sentences loads on a server: one that exists only on a client (a screen) keeps its sentences beside it.
     */
    @GameTest(template = ARENA)
    public static void declared_findsEverySentenceAModDeclares(final GameTestHelper helper) {
        for (final String mod : List.of(JsCore.MODID, JsComputers.MODID, JsIndustrial.MODID)) {
            DeclaredTexts.of(mod);
        }
        final Collection<TextKey> core = DeclaredTexts.of(JsCore.MODID);
        helper.assertTrue(core.stream().anyMatch(key -> key.key().equals(CONFIRM) && key.english().equals("Confirm")),
                "the dialog's Confirm is found with its English; found " + core);
        helper.assertTrue(core.stream().anyMatch(key -> key.key().equals(CRASHED)), "and the Operations' failures");
        helper.succeed();
    }

    /** The server holds English, which is what a machine writes down in. */
    @GameTest(template = ARENA)
    public static void loaded_isEnglishOnTheServer(final GameTestHelper helper) {
        final Text crashed = new Text.Translated(TextKey.of(CRASHED, "declared elsewhere"),
                List.of(Text.literal("IOException"), Text.literal("disk gone")));
        final String read = GameText.resolve(crashed);
        helper.assertTrue(read.equals("the Operation ran into a problem: IOException (disk gone)"),
                "the server reads it from the English file; got " + read);
        helper.succeed();
    }

    /** A sentence becomes the game's translatable component, with its English as the fallback. */
    @GameTest(template = ARENA)
    public static void component_carriesTheKeyAndItsEnglish(final GameTestHelper helper) {
        final TextKey key = TextKey.of("jscore.test.component", "an English fallback %s");
        final Component component = GameText.component(key.with("here"));
        helper.assertTrue(component.getContents() instanceof TranslatableContents contents
                        && contents.getKey().equals(key.key())
                        && "an English fallback %s".equals(contents.getFallback()),
                "the key and the fallback travel with the component; got " + component);
        helper.assertTrue(component.getString().equals("an English fallback here"),
                "a key no language has reads as its English; got " + component.getString());
        helper.succeed();
    }

    /** Over the wire a sentence stays a sentence, so the player at the other end reads it in their language. */
    @GameTest(template = ARENA)
    public static void codec_carriesATextInTheFormItWasMadeIn(final GameTestHelper helper) {
        final TextKey outer = TextKey.of(CRASHED, "the Operation ran into a problem: %s (%s)");
        final Text sent = outer.with(TextKey.of(CONFIRM, "Confirm"), "x".repeat(20_000));
        final ByteBuf buf = Unpooled.buffer();
        try {
            TextCodecs.STREAM_CODEC.encode(buf, sent);
            final Text got = TextCodecs.STREAM_CODEC.decode(buf);
            helper.assertTrue(buf.readableBytes() == 0, "nothing is left unread");
            helper.assertTrue(got instanceof Text.Translated translated && translated.key().key().equals(CRASHED)
                            && translated.args().getFirst() instanceof Text.Translated,
                    "a sentence with a sentence in it arrives as both; got " + got);
            final String read = GameText.resolve(got);
            helper.assertTrue(read.startsWith("the Operation ran into a problem: Confirm (xxx"),
                    "and reads as the English file says; got " + read.substring(0, Math.min(80, read.length())));
            final Text data = ((Text.Translated) got).args().get(1);
            final int kept = data.resolve(ITextLanguage.ENGLISH).length();
            helper.assertTrue(kept > 0 && kept < 20_000, "data too long for the wire is cut, never refused; kept "
                    + kept);
        } finally {
            buf.release();
        }
        helper.succeed();
    }
}
