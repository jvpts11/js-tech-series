/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Core.
 */
package dev.jstech.core.client.guide;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.jstech.core.guide.GuideManual;
import dev.jstech.core.guide.GuideSite;
import dev.jstech.core.guide.GuideStyle;
import dev.jstech.core.guide.GuideTexts;
import dev.jstech.core.guide.ManualReader;
import dev.jstech.core.log.CoreLogs;
import dev.jstech.core.text.GameText;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import org.slf4j.Logger;

/**
 * {@code /jstech manual site <manual>}, on the player's game: writes the manual as a website of plain files
 * ({@link GuideSite}) into the game's folder, under {@code jstech/manual_sites}, in the player's language and with the
 * recipes of the world they are in, and says where with a link that opens its front page. What it says is in
 * {@link GuideTexts}, which a server can read.
 */
public final class ManualSiteCommand {

    private static final Logger LOGGER = CoreLogs.GUIDE.logger();
    private static final String MANUAL = "manual";
    private static final String FOLDER = "jstech";
    private static final String SITES = "manual_sites";

    private ManualSiteCommand() {
    }

    public static void onRegisterCommands(final RegisterClientCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("jstech").then(Commands.literal("manual")
                .then(Commands.literal("site").then(Commands.argument(MANUAL, StringArgumentType.greedyString())
                        .suggests((context, builder) -> SharedSuggestionProvider.suggest(GuideLibrary.loaded()
                                .manuals().stream().map(GuideManual::id), builder))
                        .executes(context -> {
                            final String id = StringArgumentType.getString(context, MANUAL).strip();
                            final Path folder = folderOf(id);
                            final Optional<Path> written;
                            try {
                                written = write(id);
                            } catch (final IOException e) {
                                LOGGER.warn("The manual {} could not be written as a website to {}", id, folder, e);
                                context.getSource().sendFailure(GameText.component(GuideTexts.SITE_NOT_WRITTEN.with(
                                        folder.toString())));
                                return 0;
                            }
                            if (written.isEmpty()) {
                                context.getSource().sendFailure(GameText.component(GuideTexts.SITE_NO_MANUAL
                                        .with(id)));
                                return 0;
                            }
                            // The folder is named in the chat, and a click on it opens the site's front page.
                            final Component where = Component.literal(folder.toString()).withStyle(Style.EMPTY
                                    .withUnderlined(true).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE,
                                            folder.resolve(GuideSite.FRONT).toAbsolutePath().toString())));
                            context.getSource().sendSuccess(() -> GameText.component(GuideTexts.SITE_WRITTEN.with(
                                    GameText.of(where))), false);
                            return Command.SINGLE_SUCCESS;
                        })))));
    }

    /**
     * Writes the manual of that id as a website, every file of it, into its folder in the game's.
     *
     * @return the folder, or nothing when there is no such manual
     * @throws IOException when a file cannot be written
     */
    public static Optional<Path> write(final String id) throws IOException {
        final GuideLibrary library = GuideLibrary.loaded();
        final Optional<GuideManual> manual = library.manual(id);
        final Optional<GuideStyle> style = manual.flatMap(found -> library.style(found.style()));
        if (manual.isEmpty() || style.isEmpty()) {
            return Optional.empty();
        }
        final Minecraft minecraft = Minecraft.getInstance();
        final GuideRecipes recipes = new GuideRecipes();
        final ClientGuideText text = new ClientGuideText(minecraft.font, style.get(), recipes);
        final ManualReader reader = new ManualReader(manual.get(), library.contentsOf(manual.get()), text);
        final String language = minecraft.getLanguageManager().getSelected().replace('_', '-')
                .toLowerCase(Locale.ROOT);
        final Path folder = folderOf(id);
        for (final Map.Entry<String, String> file : GuideSite.files(reader, text, recipes.words(), language)
                .entrySet()) {
            final Path at = folder.resolve(file.getKey());
            Files.createDirectories(at.getParent());
            Files.writeString(at, file.getValue());
        }
        return Optional.of(folder);
    }

    /** The folder the manual of that id is written to as a website, in the game's own. */
    public static Path folderOf(final String id) {
        return Minecraft.getInstance().gameDirectory.toPath().resolve(FOLDER).resolve(SITES)
                .resolve(id.replace(':', '_').replace('/', '_'));
    }
}
