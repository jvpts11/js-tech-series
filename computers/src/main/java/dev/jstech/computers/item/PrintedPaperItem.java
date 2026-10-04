/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 jvpts11
 *
 * This file is part of J's Computers.
 */
package dev.jstech.computers.item;

import dev.jstech.computers.printer.IPrintedPaperReader;
import dev.jstech.computers.printer.PrintedDocument;
import dev.jstech.computers.printer.PrinterModel;
import dev.jstech.computers.registry.ComputingComponents;
import dev.jstech.core.text.GameText;
import dev.jstech.core.text.TextHolder;
import dev.jstech.core.text.TextKey;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * The sheet a printer turns out: the document it printed, its pages of text or its picture. Its tooltip gives the
 * title, the pages and the first lines, and the printer; a right click reads it page by page, as a written book is
 * read; in an item frame a picture shows on it, as a map does.
 */
@TextHolder
public class PrintedPaperItem extends Item {

    /** How many lines of the print the tooltip shows. */
    public static final int TOOLTIP_LINES = 3;
    /** Where a document came from: its program and its machine; the reading screen says it under a picture too. */
    public static final TextKey FROM = TextKey.of("jsc.printed_paper.from", "Printed from %s on %s");
    /** Which page is being read, under the sheet in the reading screen. */
    public static final TextKey PAGE_OF = TextKey.of("jsc.printed_paper.page_of", "Page %s of %s");

    private static final TextKey PAGES = TextKey.of("jsc.printed_paper.pages", "\"%s\", %s pages");
    private static final TextKey ONE_PAGE = TextKey.of("jsc.printed_paper.one_page", "\"%s\", 1 page");
    private static final TextKey PICTURE = TextKey.of("jsc.printed_paper.picture", "Picture: %s");
    private static final TextKey PRINTED_BY = TextKey.of("jsc.printed_paper.printed_by", "Printed by %s");
    private static final TextKey MORE = TextKey.of("jsc.printed_paper.more", "...");
    private static final TextKey READ = TextKey.of("jsc.printed_paper.read", "Right click to read");
    private static final TextKey BLANK = TextKey.of("jsc.printed_paper.blank", "Nothing is printed on it");

    public PrintedPaperItem(final Properties properties) {
        super(properties.stacksTo(1));
    }

    /** The document a sheet carries, the blank one when nothing was printed on it. */
    public static PrintedDocument document(final ItemStack stack) {
        return stack.getOrDefault(ComputingComponents.PRINTED_DOCUMENT.get(), PrintedDocument.EMPTY);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player, final InteractionHand hand) {
        final ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            IPrintedPaperReader.Holder.read(document(stack));
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final List<Component> tooltip,
                                final TooltipFlag flag) {
        final PrintedDocument document = document(stack);
        final PrinterModel printer = document.printerModel();
        if (printer == null) {
            tooltip.add(GameText.component(BLANK).withStyle(ChatFormatting.GRAY));
            return;
        }
        if (document.isPicture()) {
            tooltip.add(GameText.component(PICTURE.with(document.pictureName().isEmpty() ? document.title()
                    : document.pictureName())).withStyle(ChatFormatting.GRAY));
        } else {
            final int pages = document.pages().size();
            tooltip.add(GameText.component(pages == 1 ? ONE_PAGE.with(document.title())
                    : PAGES.with(document.title(), pages)).withStyle(ChatFormatting.GRAY));
            final List<String> lines = document.firstLines(TOOLTIP_LINES + 1);
            for (int i = 0; i < Math.min(TOOLTIP_LINES, lines.size()); i++) {
                tooltip.add(Component.literal(lines.get(i)).withStyle(ChatFormatting.DARK_GRAY));
            }
            if (lines.size() > TOOLTIP_LINES) {
                tooltip.add(GameText.component(MORE).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        if (!document.program().isEmpty() && !document.from().isEmpty()) {
            tooltip.add(GameText.component(FROM.with(document.program(), document.from()))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        tooltip.add(GameText.component(PRINTED_BY.with(printer.displayName())).withStyle(ChatFormatting.DARK_GRAY));
        tooltip.add(GameText.component(READ).withStyle(ChatFormatting.DARK_GRAY));
    }
}
