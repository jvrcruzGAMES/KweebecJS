package games.jvrcruz.kweebecjs.lang;

import games.jvrcruz.kweebecjs.asset.RuntimeItemAssetManager;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

public final class RuntimeLangRegistry {
    private static final String GENERATED_LANG_FILE_NAME = "KweebecJS.lang";
    private static final String TRANSLATION_FILE_PREFIX = "KweebecJS.";
    private final RuntimeItemAssetManager runtimeItemAssetManager;
    private final Supplier<String> modGroupSupplier;
    private final Supplier<String> modIdSupplier;
    private final Map<String, Map<String, String>> localeEntries = new LinkedHashMap<>();
    private final Map<String, String> generatedLangAssets = new LinkedHashMap<>();

    public RuntimeLangRegistry(
            RuntimeItemAssetManager runtimeItemAssetManager,
            Supplier<String> modGroupSupplier,
            Supplier<String> modIdSupplier
    ) {
        this.runtimeItemAssetManager = runtimeItemAssetManager;
        this.modGroupSupplier = modGroupSupplier;
        this.modIdSupplier = modIdSupplier;
    }

    public synchronized String addFromArgs(Object[] args) {
        if (args.length < 3) {
            throw new IllegalArgumentException("event.add(locale, itemIdOrKey, value) or event.add(locale, itemIdOrKey, translationKey, value).");
        }

        String locale = requireNonBlank(String.valueOf(args[0]), "locale");
        String itemIdOrKey = requireNonBlank(String.valueOf(args[1]), "itemIdOrKey");
        String translationKey = "name";
        String value;
        if (args.length >= 4) {
            translationKey = requireNonBlank(String.valueOf(args[2]), "translationKey");
            value = requireNonBlank(String.valueOf(args[3]), "value");
        } else {
            value = requireNonBlank(String.valueOf(args[2]), "value");
        }

        String normalizedLocale = normalizeLocale(locale);
        String normalizedKey = normalizeLanguageKey(resolveLanguageKeyFromInput(itemIdOrKey, translationKey));
        Map<String, String> entries = localeEntries.computeIfAbsent(normalizedLocale, ignored -> new LinkedHashMap<>());
        for (String alias : buildLanguageKeyAliases(normalizedKey)) {
            entries.put(alias, value);
        }
        return normalizedLocale + ":" + normalizedKey;
    }

    public synchronized void clearRegisteredEntries() {
        localeEntries.clear();
        for (String relativePath : generatedLangAssets.keySet()) {
            runtimeItemAssetManager.removeGeneratedAsset(relativePath);
        }
        generatedLangAssets.clear();
    }

    public synchronized int applyRegisteredEntries() {
        for (String relativePath : generatedLangAssets.keySet()) {
            runtimeItemAssetManager.removeGeneratedAsset(relativePath);
        }
        generatedLangAssets.clear();

        int totalEntries = 0;
        for (Map.Entry<String, Map<String, String>> localeEntry : localeEntries.entrySet()) {
            String locale = localeEntry.getKey();
            Map<String, String> entries = new TreeMap<>(localeEntry.getValue());
            totalEntries += entries.size();
            String relativePath = "Server/Languages/" + locale + "/" + GENERATED_LANG_FILE_NAME;
            String content = toLangFile(entries);
            generatedLangAssets.put(relativePath, content);
            runtimeItemAssetManager.putGeneratedAsset(relativePath, content);
        }

        return totalEntries;
    }

    private static String toLangFile(Map<String, String> entries) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            builder.append(entry.getKey())
                    .append("=")
                    .append(escapeLangValue(entry.getValue()))
                    .append('\n');
        }
        return builder.toString();
    }

    private static String requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank.");
        }
        return value;
    }

    private static String normalizeLocale(String locale) {
        return locale.replace('_', '-');
    }

    private static String normalizeLanguageKey(String key) {
        return key.trim();
    }

    private String resolveLanguageKeyFromInput(String itemIdOrKey, String translationKey) {
        String trimmed = itemIdOrKey == null ? "" : itemIdOrKey.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("itemIdOrKey must not be blank.");
        }
        if (trimmed.contains(".")) {
            return trimmed;
        }

        return TRANSLATION_FILE_PREFIX
                + normalizeTranslationToken(modGroupSupplier.get()).toLowerCase(java.util.Locale.ROOT)
                + "_"
                + normalizeTranslationToken(modIdSupplier.get()).toLowerCase(java.util.Locale.ROOT)
                + "_"
                + normalizeTranslationToken(trimmed)
                + "."
                + normalizeTranslationToken(translationKey).toLowerCase(java.util.Locale.ROOT);
    }

    private static java.util.List<String> buildLanguageKeyAliases(String normalizedKey) {
        java.util.LinkedHashSet<String> aliases = new java.util.LinkedHashSet<>();
        aliases.add(normalizedKey);

        if (normalizedKey.startsWith("server.") && normalizedKey.length() > "server.".length()) {
            aliases.add(normalizedKey.substring("server.".length()));
        }
        if (normalizedKey.startsWith(TRANSLATION_FILE_PREFIX) && normalizedKey.length() > TRANSLATION_FILE_PREFIX.length()) {
            aliases.add(normalizedKey.substring(TRANSLATION_FILE_PREFIX.length()));
            aliases.add("server." + normalizedKey);
        }
        String serverPrefixedTranslation = "server." + TRANSLATION_FILE_PREFIX;
        if (normalizedKey.startsWith(serverPrefixedTranslation) && normalizedKey.length() > serverPrefixedTranslation.length()) {
            String noServer = normalizedKey.substring("server.".length());
            String noPrefix = normalizedKey.substring(serverPrefixedTranslation.length());
            aliases.add(noServer);
            aliases.add(noPrefix);
            aliases.add(TRANSLATION_FILE_PREFIX + noPrefix);
        }
        if (!normalizedKey.contains(".")) {
            aliases.add(TRANSLATION_FILE_PREFIX + normalizedKey);
            aliases.add("server." + TRANSLATION_FILE_PREFIX + normalizedKey);
        }

        return java.util.List.copyOf(aliases);
    }

    private static String normalizeTranslationToken(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        String normalized = value.trim().replaceAll("[^A-Za-z0-9]+", "_");
        normalized = normalized.replaceAll("_+", "_");
        normalized = normalized.replaceAll("^_+", "");
        normalized = normalized.replaceAll("_+$", "");
        return normalized.isBlank() ? "unknown" : normalized;
    }

    private static String escapeLangValue(String value) {
        return value.replace("\r", "").replace("\n", "\\n");
    }
}
