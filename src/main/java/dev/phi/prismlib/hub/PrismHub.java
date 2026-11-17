package dev.phi.prismlib.hub;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

/**
 * The one place that manages all of Phalynxi's mods: each mod registers itself here (client side) with a name, a line about it, a logo and a way to open its settings, and Prism Lib
 * shows them all behind one button on the title, pause and options screens. {@link dev.phi.prismlib.settings.SettingsClient#install} registers a mod by itself; other mods (the Alterra
 * core) call {@link #register} directly, from their client setup.
 */
public final class PrismHub {
    private PrismHub() {}

    /** A logo drawn at a position and size, with an opacity (the button cross-fades between the logos of the mods). */
    @FunctionalInterface public interface Logo { void draw(GuiGraphics g, int x, int y, int size, float alpha); }

    /** A mod in the hub. {@code opener} makes the mod's settings screen, given the screen to come back to (null: it has none, the card only shows the mod). */
    public record Entry(String modId, String title, String tagline, Logo logo, Function<Screen, Screen> opener) {}

    private static final List<Entry> ENTRIES = new CopyOnWriteArrayList<>();

    /** Add a mod (or replace its earlier entry). Client side only. */
    public static void register(Entry entry) {
        ENTRIES.removeIf(e -> e.modId().equals(entry.modId()));
        ENTRIES.add(entry);
        ENTRIES.sort(java.util.Comparator.comparing(e -> e.title().toLowerCase(java.util.Locale.ROOT)));   // alphabetical: the button cycles through them in this order
    }

    public static List<Entry> entries() { return ENTRIES; }

    /** The mods that have settings to open: only these are shown (the button cycles through their logos, the screen lists them). */
    public static List<Entry> visible() { return ENTRIES.stream().filter(e -> e.opener() != null).toList(); }

    /** For mods that cannot depend on Prism Lib at compile time (they find this method by reflection): a mod with an atlas sprite as its logo. */
    public static void registerWithTexture(String modId, String title, String tagline, ResourceLocation logo, Function<Screen, Screen> opener) {
        register(new Entry(modId, title, tagline, texture(logo), opener));
    }

    public static void registerWithSprite(String modId, String title, String tagline, java.util.function.Supplier<net.minecraft.client.renderer.texture.TextureAtlasSprite> sprite, Function<Screen, Screen> opener) {
        register(new Entry(modId, title, tagline, sprite(sprite), opener));
    }

    private static final java.util.Map<ResourceLocation, int[]> SIZES = new java.util.concurrent.ConcurrentHashMap<>();
    private static int[] sizeOf(ResourceLocation tex) {
        return SIZES.computeIfAbsent(tex, k -> {
            try (var in = net.minecraft.client.Minecraft.getInstance().getResourceManager().open(k); var img = com.mojang.blaze3d.platform.NativeImage.read(in)) { return new int[] {img.getWidth(), img.getHeight()}; }
            catch (java.io.IOException | RuntimeException e) { return new int[] {32, 32}; }
        });
    }

    /** A logo from a texture: square, or a tall strip of square frames that is played round (about nine frames a second). */
    public static Logo texture(ResourceLocation tex) {
        return (g, x, y, size, alpha) -> {
            int[] wh = sizeOf(tex); int w = Math.max(1, wh[0]), h = Math.max(1, wh[1]), frames = Math.max(1, h / w);
            int f = frames > 1 ? (int) ((net.minecraft.Util.getMillis() / 110L) % frames) : 0;
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            g.setColor(1f, 1f, 1f, alpha);
            g.blit(tex, x, y, size, size, 0f, (float) (f * w), w, w, w, h);
            g.setColor(1f, 1f, 1f, 1f);
        };
    }

    /** A logo from a sprite of a texture atlas (an item's icon, animated ones included). */
    public static Logo sprite(java.util.function.Supplier<net.minecraft.client.renderer.texture.TextureAtlasSprite> sprite) {
        return (g, x, y, size, alpha) -> {
            com.mojang.blaze3d.systems.RenderSystem.enableBlend();
            g.blit(x, y, 0, size, size, sprite.get(), 1f, 1f, 1f, alpha);
        };
    }

    /** A plain tile with the first letter of the name, for mods that bring no logo. */
    public static Logo letter(String name) {
        String s = name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
        return (g, x, y, size, alpha) -> {
            int a = Math.round(alpha * 255f) << 24;
            g.fill(x, y, x + size, y + size, a | 0x2E3A52);
            var font = net.minecraft.client.Minecraft.getInstance().font;
            g.drawString(font, s, x + (size - font.width(s)) / 2, y + (size - 8) / 2, a | 0xE6EEFF, false);
        };
    }
}
