package com.github.vodobryshkin.islab1.backend.service;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Единое правило сравнения названий; исходная строка для отображения не меняется.
 */
public final class OrganizationNames {
    private static final Pattern WHITESPACE = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private OrganizationNames() {
    }

    public static String normalize(String name) {
        return WHITESPACE.matcher(name).replaceAll(" ").strip()
                .replace('«', '"').replace('»', '"').toLowerCase(Locale.ROOT);
    }
}
