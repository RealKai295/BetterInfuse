package com.catadmirer.infuseSMP;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.Locale;
import java.util.Set;

@NullMarked
public class MessageTranslator {
    public static final Set<Locale> SUPPORTED_LOCALES = Set.of(
            Locale.US,
            Locale.of("es"),
            Locale.of("tr"),
            Locale.of("ru")
    );

    private final Infuse plugin = Infuse.getInstance();
    private boolean localeFallbackWarned;

    public static Locale resolveLocale(String lang) {
        String normalized = lang.toLowerCase(Locale.ROOT).replace('_', '-');

        return switch (normalized) {
            case "en", "en-us" -> Locale.US;
            case "es" -> Locale.of("es");
            case "tr" -> Locale.of("tr");
            case "ru" -> Locale.of("ru");
            default -> Locale.forLanguageTag(lang);
        };
    }

    public static Locale resolveLocale(Locale locale) {
        return resolveLocale(locale.toLanguageTag());
    }

    @Nullable
    public String translate(String key) {
        // Getting the locale from the config
        Locale locale = resolveLocale(plugin.getMainConfig().lang());

        // Defaulting to the en_US locale
        if (!SUPPORTED_LOCALES.contains(locale)) {
            if (!localeFallbackWarned) {
                Infuse.LOGGER.warn("Locale \"{}\" not recognized.  Falling back to en_US.", locale);
                localeFallbackWarned = true;
            }
            locale = Locale.US;
        }

        // Getting the translation
        FileConfiguration translations = getLocale(locale);
        String normalizedKey = key.toLowerCase();

        if (translations.isList(normalizedKey)) {
            return String.join("\n", translations.getStringList(normalizedKey));
        }

        return translations.getString(normalizedKey);
    }

    public void loadAll() {
        SUPPORTED_LOCALES.forEach(this::loadLocale);
    }

    public void loadLocale(Locale locale) {
        plugin.saveResource("lang/base/" + locale + ".yml", true);
    }

    public FileConfiguration getLocale(Locale locale) {
        File baseLocaleFile = new File(plugin.getDataFolder(), "lang/base/" + locale + ".yml");
        File customLocaleFile = new File(plugin.getDataFolder(), "lang/" + locale + ".yml");

        // Loading base translations
        FileConfiguration translations = YamlConfiguration.loadConfiguration(baseLocaleFile);

        // Loading custom translations
        if (!customLocaleFile.exists()) return translations;

        FileConfiguration custom = YamlConfiguration.loadConfiguration(customLocaleFile);

        for (String key : custom.getKeys(true)) {
            translations.set(key, custom.get(key));
        }

        return translations;
    }
}
