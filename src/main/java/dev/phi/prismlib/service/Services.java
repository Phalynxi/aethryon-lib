package dev.phi.prismlib.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The meeting point of the mods. A mod that knows something (the wind, the clouds, how wet the world is) registers a provider for the matching interface;
 * a mod that wants to know asks for it. Several providers can exist: the one with the highest priority answers, and the others are there as fallbacks (see {@link #all}).
 * Nothing here knows any mod: a mod that is not installed is simply not registered.
 */
public final class Services {
    private Services() {}

    private record Entry<T>(T impl, int priority, String owner) {}
    private static final Map<Class<?>, List<Entry<?>>> REGISTRY = new ConcurrentHashMap<>();

    /** Register a provider. owner is the mod's id (for the log and for removing it again); priority: 0 is a default, 100 the normal one for a mod that really knows, more to override. */
    public static <T> void register(Class<T> type, T impl, int priority, String owner) {
        var list = REGISTRY.computeIfAbsent(type, k -> new CopyOnWriteArrayList<>());
        list.removeIf(e -> e.owner().equals(owner));
        list.add(new Entry<>(impl, priority, owner));
        list.sort(Comparator.comparingInt((Entry<?> e) -> -e.priority()));
    }

    /** Take away everything a mod registered. */
    public static void unregister(String owner) { REGISTRY.values().forEach(l -> l.removeIf(e -> e.owner().equals(owner))); }

    /** The provider that answers: the highest priority one, or the fallback when there is none. */
    @SuppressWarnings("unchecked")
    public static <T> T get(Class<T> type, T fallback) {
        var list = REGISTRY.get(type);
        return list == null || list.isEmpty() ? fallback : (T) list.get(0).impl();
    }

    /** Every provider, the best first (for services where answers are put together, such as lists of things to draw). */
    @SuppressWarnings("unchecked")
    public static <T> List<T> all(Class<T> type) {
        var list = REGISTRY.get(type); var out = new ArrayList<T>();
        if (list != null) for (var e : list) out.add((T) e.impl());
        return out;
    }

    public static boolean has(Class<?> type) { var l = REGISTRY.get(type); return l != null && !l.isEmpty(); }
}
