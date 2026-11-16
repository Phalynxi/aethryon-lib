package dev.phi.prismlib.settings;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A settings screen for any mod, without writing one. A mod lists its options (they stay values of its own config spec, so the config file keeps working) as categories, and
 * {@link SettingsClient#install} puts the screen behind the Config button of the mod in the mod list. Changes are saved to the config file as they are made.
 */
public final class Settings {
    private Settings() {}
    public enum Kind { BOOL, DOUBLE, INT }
    public record Option(String name, String description, Kind kind, ModConfigSpec.ConfigValue<?> value, double min, double max, double step, String unit) {
        public static Option bool(String name, String description, ModConfigSpec.BooleanValue value) { return new Option(name, description, Kind.BOOL, value, 0, 1, 1, ""); }
        public static Option number(String name, String description, ModConfigSpec.DoubleValue value, double min, double max, double step, String unit) { return new Option(name, description, Kind.DOUBLE, value, min, max, step, unit); }
        public static Option integer(String name, String description, ModConfigSpec.IntValue value, int min, int max, String prefix) { return new Option(name, description, Kind.INT, value, min, max, 1, prefix); }
    }
    public record Category(String title, String description, List<Option> options) {}

    private static final Map<String, List<Category>> BY_MOD = new ConcurrentHashMap<>();

    /** Add a page of options for a mod. */
    public static void category(String modId, String title, String description, List<Option> options) {
        BY_MOD.computeIfAbsent(modId, k -> new CopyOnWriteArrayList<>()).add(new Category(title, description, List.copyOf(options)));
    }
    public static List<Category> of(String modId) { return BY_MOD.getOrDefault(modId, List.of()); }
}
