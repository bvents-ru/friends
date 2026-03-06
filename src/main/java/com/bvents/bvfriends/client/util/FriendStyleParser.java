package com.bvents.bvfriends.client.util;

import net.minecraft.text.Style;
import net.minecraft.util.Formatting;

public final class FriendStyleParser {
    private FriendStyleParser() {
    }

    public static Style parseStyle(String raw, Style fallback) {
        Style style = Style.EMPTY;
        if (raw == null || raw.isBlank()) {
            return mergewf(style, fallback);
        }

        String input = raw.trim();
        if (input.startsWith("#") && input.length() == 7) {
            try {
                int rgb = Integer.parseUnsignedInt(input.substring(1), 16);
                return mergewf(style.withColor(rgb), fallback);
            } catch (NumberFormatException ignored) {
                return mergewf(style, fallback);
            }
        }

        for (int i = 0; i < input.length() - 1; i++) {
            if (input.charAt(i) != '&') {
                continue;
            }
            Formatting formatting = Formatting.byCode(input.charAt(i + 1));
            if (formatting == null) {
                continue;
            }
            style = applyFormatting(style, formatting);
        }

        return mergewf(style, fallback);
    }

    public static int parseArgb(String raw, int fallbackRgb) {
        Style style = parseStyle(raw, Style.EMPTY);
        if (style.getColor() != null) {
            return 0xFF000000 | style.getColor().getRgb();
        }
        return 0xFF000000 | fallbackRgb;
    }

    private static Style mergewf(Style style, Style fallback) {
        Style result = style;
        if (result.getColor() == null && fallback != null && fallback.getColor() != null) {
            result = result.withColor(fallback.getColor());
        }
        return result;
    }

    private static Style applyFormatting(Style style, Formatting formatting) {
        if (formatting == Formatting.RESET) {
            return Style.EMPTY;
        }
        if (formatting.isColor()) {
            return style.withColor(formatting);
        }
        if (formatting == Formatting.BOLD) {
            return style.withBold(true);
        }
        if (formatting == Formatting.ITALIC) {
            return style.withItalic(true);
        }
        if (formatting == Formatting.UNDERLINE) {
            return style.withUnderline(true);
        }
        if (formatting == Formatting.STRIKETHROUGH) {
            return style.withStrikethrough(true);
        }
        if (formatting == Formatting.OBFUSCATED) {
            return style.withObfuscated(true);
        }
        return style;
    }

    public static Formatting formatclr(String raw, Formatting fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        Formatting color = null;
        String input = raw.trim();
        for (int i = 0; i < input.length() - 1; i++) {
            if (input.charAt(i) != '&') {
                continue;
            }
            Formatting formatting = Formatting.byCode(input.charAt(i + 1));
            if (formatting == Formatting.RESET) {
                color = fallback;
            } else if (formatting != null && formatting.isColor()) {
                color = formatting;
            }
        }
        return color == null ? fallback : color;
    }

    public static Formatting parseMainColorFormatting(String raw, Formatting fallback) {
        return formatclr(raw, fallback);
    }
}
