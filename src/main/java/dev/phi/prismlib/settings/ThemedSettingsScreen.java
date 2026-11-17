package dev.phi.prismlib.settings;

import dev.phi.prismlib.hub.PrismHub;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import java.util.List;
import java.util.Locale;

/**
 * A mod's own settings window, drawn the way Alterra's is (parchment window, a strip of categories with a card on the open one, one row per setting with a switch or a slider, dotted
 * dividers, the same buttons). It shows what the mod listed with {@link Settings}; changes apply and are saved to the config file as they are made. The sprites are Prism Lib's own copies.
 */
public final class ThemedSettingsScreen extends Screen {
    /** One colour scheme: the id names the sprite set (window_<id>, button_<id>, ...) and the rest are the text and strip colours. */
    public record Theme(String id, int ink, int faded, int cream, int gutter, int gutterLine, int gutterEdge, int dot, int hover, int thumbDark, int thumbLight) {
        /** Light and airy: sky blue and white. */
        public static final Theme SKY = new Theme("sky", 0xFF1D3552, 0xFF6B829C, 0xFFFFFFFF, 0xFFCFE0F2, 0xFF9DB8D4, 0xFFE8F2FC, 0xFFB5CBE2, 0x14000000, 0xFF35618F, 0xFF8FB4DC);
        /** Dusk: indigo with a warm glow. */
        public static final Theme DUSK = new Theme("dusk", 0xFFEDE8FA, 0xFF9C94BC, 0xFFFFF4E6, 0xFF171430, 0xFF3A3460, 0xFF2A2548, 0xFF5A5190, 0x1CFFFFFF, 0xFF6A5FA0, 0xFFC9B8F0);
        /** The hub: dark slate with a blue accent. */
        public static final Theme SLATE = new Theme("slate", 0xFFE6EAF2, 0xFF8B96A8, 0xFFF2F6FF, 0xFF141820, 0xFF2C3444, 0xFF222938, 0xFF4A566E, 0x1CFFFFFF, 0xFF53607A, 0xFFA9B6D0);
    }

    private final Theme theme;
    private static ResourceLocation sprite(String n, Theme th) { return ResourceLocation.fromNamespaceAndPath("prismlib", n + "_" + th.id()); }
    private final ResourceLocation WINDOW, NOTE_CARD, BUTTON, BUTTON_HOVER, BUTTON_GREEN, BUTTON_GREEN_HOVER, SWITCH_ON, SWITCH_OFF, SLIDER_TRACK, SLIDER_FILL, SLIDER_KNOB;

    private final Screen parent;
    private final String title;
    private final PrismHub.Logo logo;
    private final List<Settings.Category> cats;
    private int cat, scroll, dragging = -1, sideScroll;
    private String openGroup = "";
    private record Row(boolean header, String group, int cat) {}
    private int px, py, pw, ph, ctrlX, ctrlW = 120, listTop, listBottom;

    public ThemedSettingsScreen(Screen parent, String modId, String title, String tagline, Theme theme, PrismHub.Logo logo) {
        super(Component.literal(title + " settings"));
        this.parent = parent; this.title = title; this.logo = logo; this.theme = theme;
        WINDOW = sprite("window", theme); NOTE_CARD = sprite("note_card", theme); BUTTON = sprite("button", theme); BUTTON_HOVER = sprite("button_hover", theme); BUTTON_GREEN = sprite("button_green", theme); BUTTON_GREEN_HOVER = sprite("button_green_hover", theme);
        SWITCH_ON = sprite("switch_on", theme); SWITCH_OFF = sprite("switch_off", theme); SLIDER_TRACK = sprite("slider_track", theme); SLIDER_FILL = sprite("slider_fill", theme); SLIDER_KNOB = sprite("slider_knob", theme);
        this.cats = Settings.of(modId);
    }

    @Override protected void init() {
        pw = Math.min(width - 24, 470); ph = Math.min(height - 24, 290);
        px = (width - pw) / 2; py = (height - ph) / 2;
        if (openGroup.isEmpty() && !cats.isEmpty()) openGroup = groupOf(cats.get(0));
    }

    @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }

    private static String groupOf(Settings.Category c) { return c.group().isEmpty() ? c.title() : c.group(); }

    /** The side strip: a row for every group, and under the open group a row for each of its pages. */
    private List<Row> sideRows() {
        List<Row> rows = new java.util.ArrayList<>();
        java.util.LinkedHashSet<String> groups = new java.util.LinkedHashSet<>();
        for (var c : cats) groups.add(groupOf(c));
        for (String g : groups) {
            rows.add(new Row(true, g, -1));
            if (g.equals(openGroup)) for (int i = 0; i < cats.size(); i++) if (groupOf(cats.get(i)).equals(g)) rows.add(new Row(false, g, i));
        }
        return rows;
    }
    private static int rowH(Row r) { return r.header() ? 24 : 18; }
    private net.minecraft.world.item.ItemStack groupIcon(String g) { for (var c : cats) if (groupOf(c).equals(g) && !c.icon().isEmpty()) return icon(c.icon()); return net.minecraft.world.item.ItemStack.EMPTY; }

    private static net.minecraft.world.item.ItemStack icon(String id) {
        if (id.isEmpty()) return net.minecraft.world.item.ItemStack.EMPTY;
        var rl = ResourceLocation.tryParse(id);
        return rl == null ? net.minecraft.world.item.ItemStack.EMPTY : net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(rl).map(net.minecraft.world.item.ItemStack::new).orElse(net.minecraft.world.item.ItemStack.EMPTY);
    }

    private List<Settings.Option> options() { return cats.isEmpty() ? List.of() : cats.get(Mth.clamp(cat, 0, cats.size() - 1)).options(); }
    private static boolean in(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    // ------------------------------------------------------------------ values

    private static double get(Settings.Option o) {
        Object v = o.value().get();
        return v instanceof Boolean b ? (b ? 1 : 0) : ((Number) v).doubleValue();
    }

    private static void set(Settings.Option o, double x) {
        switch (o.kind()) {
            case BOOL -> ((net.neoforged.neoforge.common.ModConfigSpec.BooleanValue) o.value()).set(x >= 0.5);
            case INT -> ((net.neoforged.neoforge.common.ModConfigSpec.IntValue) o.value()).set((int) Math.round(x));
            default -> ((net.neoforged.neoforge.common.ModConfigSpec.DoubleValue) o.value()).set(x);
        }
    }

    private static void save(Settings.Option o) { o.value().save(); }

    private void resetPage() {
        for (var o : options()) {
            Object d = o.value().getDefault();
            set(o, d instanceof Boolean b ? (b ? 1 : 0) : ((Number) d).doubleValue());
            save(o);
        }
    }

    private static String label(Settings.Option o) {
        double v = get(o);
        return o.kind() == Settings.Kind.INT ? o.unit() + Math.round(v) : String.format(Locale.ROOT, o.step() < 0.2 ? "%.1f" : "%.2f", v) + o.unit();
    }

    // ------------------------------------------------------------------ drawing

    private void dotted(GuiGraphics g, int x, int y, int w) { for (int i = 0; i + 2 <= w; i += 4) g.fill(x + i, y, x + i + 2, y + 1, theme.dot()); }

    private void button(GuiGraphics g, int x, int y, int w, String text, int mx, int my, boolean primary) {
        boolean hov = in(mx, my, x, y, w, 20);
        g.blitSprite(primary ? (hov ? BUTTON_GREEN_HOVER : BUTTON_GREEN) : (hov ? BUTTON_HOVER : BUTTON), x, y, w, 20);
        g.drawString(font, text, x + (w - font.width(text)) / 2, y + 6, theme.cream(), true);
    }

    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        super.render(g, mx, my, pt);   // the blurred background
        g.blitSprite(WINDOW, px, py, pw, ph);
        logo.draw(g, px + 12, py + 9, 18, 1f);
        g.drawString(font, title + " settings", px + 34, py + 16, theme.ink(), false);
        dotted(g, px + 12, py + 33, pw - 24);
        // categories: a darker strip of page, the open one on a card
        int sx = px + 8, sy = py + 36, sw = 112, sh = ph - 36 - 38;
        g.fill(sx, sy, sx + sw, sy + sh, theme.gutter());
        g.fill(sx + sw, sy, sx + sw + 1, sy + sh, theme.gutterLine()); g.fill(sx + sw + 1, sy, sx + sw + 2, sy + sh, theme.gutterEdge());
        var rows = sideRows();
        int total = 12; for (Row r : rows) total += rowH(r);
        sideScroll = Mth.clamp(sideScroll, 0, Math.max(0, total - sh));
        g.enableScissor(sx, sy, sx + sw, sy + sh);
        int sry = sy + 6 - sideScroll;
        for (Row r : rows) {
            int h = rowH(r); boolean hov = in(mx, my, sx, sry, sw, h) && my < sy + sh;
            if (r.header()) {
                if (hov) g.fill(sx + 2, sry, sx + sw - 2, sry + h - 2, theme.hover());
                var ic = groupIcon(r.group());
                if (!ic.isEmpty()) g.renderItem(ic, sx + 8, sry + 3);
                g.drawString(font, font.plainSubstrByWidth(r.group(), sw - 52), sx + 30, sry + 8, theme.ink(), false);
                int ax = sx + sw - 14, ay = sry + 11;   // a small arrow: down when open, right when closed
                if (r.group().equals(openGroup)) for (int k = 0; k < 4; k++) g.fill(ax + k, ay - 2 + k, ax + 7 - k, ay - 1 + k, theme.ink());
                else for (int k = 0; k < 4; k++) g.fill(ax + k, ay - 3 + k, ax + k + 1, ay + 4 - k, theme.ink());
            } else {
                if (r.cat() == cat) g.blitSprite(NOTE_CARD, sx + 12, sry, sw - 16, h - 1);
                else if (hov) g.fill(sx + 12, sry, sx + sw - 4, sry + h - 1, theme.hover());
                g.drawString(font, font.plainSubstrByWidth(cats.get(r.cat()).title(), sw - 24), sx + 17, sry + 5, theme.ink(), false);
            }
            sry += h;
        }
        g.disableScissor();
        // the settings of the open category
        int cx = sx + sw + 16, cw = px + pw - 8 - cx - 12, cy = py + 44;
        if (!cats.isEmpty()) g.drawString(font, cats.get(Mth.clamp(cat, 0, cats.size() - 1)).title(), cx, cy, theme.ink(), false);
        listTop = cy + 18; listBottom = py + ph - 40; int rowH = 24;
        var opts = options();
        int maxScroll = Math.max(0, opts.size() * rowH - (listBottom - listTop));
        scroll = Mth.clamp(scroll, 0, maxScroll);
        ctrlW = 120; ctrlX = cx + cw - ctrlW;
        g.enableScissor(cx - 4, listTop, cx + cw + 4, listBottom);
        for (int i = 0; i < opts.size(); i++) {
            var o = opts.get(i);
            int ry = listTop + i * rowH - scroll;
            if (ry + rowH < listTop || ry > listBottom) continue;
            if (my >= listTop && my <= listBottom && in(mx, my, cx - 4, ry, cw + 8, rowH - 2)) g.fill(cx - 4, ry, cx + cw + 4, ry + rowH - 2, theme.hover());
            g.drawString(font, font.plainSubstrByWidth(o.name(), ctrlX - cx - 10), cx, ry + 7, theme.ink(), false);
            control(g, o, ctrlX, ry + 5, i);
            dotted(g, cx, ry + rowH - 2, cw);
        }
        g.disableScissor();
        if (maxScroll > 0) {   // a slim scroll bar
            int trackH = listBottom - listTop, barH = Math.max(18, trackH * trackH / (opts.size() * rowH)), barY = listTop + (int) ((trackH - barH) * (scroll / (double) maxScroll));
            int x = px + pw - 12; g.fill(x, barY, x + 3, barY + barH, theme.thumbDark()); g.fill(x + 1, barY + 1, x + 2, barY + barH - 1, theme.thumbLight());
        }
        if (cats.isEmpty()) g.drawString(font, "Nothing to set yet", cx, cy + 18, theme.faded(), false);
        dotted(g, px + 12, py + ph - 36, pw - 24);
        button(g, px + 14, py + ph - 30, 110, "Reset to defaults", mx, my, false);
        button(g, px + pw - 14 - 80, py + ph - 30, 80, "Done", mx, my, true);
    }

    private void control(GuiGraphics g, Settings.Option o, int x, int y, int row) {
        if (o.kind() == Settings.Kind.BOOL) { g.blitSprite(get(o) >= 0.5 ? SWITCH_ON : SWITCH_OFF, x + ctrlW - 28, y, 28, 14); return; }
        int lw = 34, tw = ctrlW - lw - 4;
        double f = Mth.clamp((get(o) - o.min()) / Math.max(1e-9, o.max() - o.min()), 0, 1);
        g.blitSprite(SLIDER_TRACK, x, y + 4, tw, 6);
        g.blitSprite(SLIDER_FILL, x, y + 4, Math.max(4, (int) (tw * f)), 6);
        g.blitSprite(SLIDER_KNOB, x + (int) ((tw - 6) * f), y + 1, 6, 12);
        String t = label(o);
        g.drawString(font, t, x + tw + 4 + lw - font.width(t), y + 3, theme.ink(), false);
    }

    // ------------------------------------------------------------------ input

    private int rowAt(double mx, double my) {
        if (my < listTop || my >= listBottom || mx < px + 130) return -1;
        int i = (int) ((my - listTop + scroll) / 24);
        return i >= 0 && i < options().size() ? i : -1;
    }

    private void slideTo(Settings.Option o, double mx) {
        int tw = ctrlW - 34 - 4;
        double f = Mth.clamp((mx - ctrlX - 3) / (double) (tw - 6), 0, 1);
        double v = o.min() + f * (o.max() - o.min());
        if (o.step() > 0) v = o.min() + Math.round((v - o.min()) / o.step()) * o.step();
        set(o, Mth.clamp(v, o.min(), o.max()));
    }

    @Override public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        if (in(mx, my, px + pw - 14 - 80, py + ph - 30, 80, 20)) { onClose(); return true; }
        if (in(mx, my, px + 14, py + ph - 30, 110, 20)) { resetPage(); return true; }
        int sx = px + 8, sy = py + 36, sh = ph - 74;
        if (in(mx, my, sx, sy, 112, sh)) {
            int y = sy + 6 - sideScroll;
            for (Row r : sideRows()) {
                int h = rowH(r);
                if (my >= y && my < y + h) {
                    if (r.header()) { openGroup = r.group(); for (int i = 0; i < cats.size(); i++) if (groupOf(cats.get(i)).equals(r.group())) { cat = i; break; } }
                    else cat = r.cat();
                    scroll = 0; dragging = -1; return true;
                }
                y += h;
            }
        }
        int r = rowAt(mx, my);
        if (r >= 0) {
            var o = options().get(r);
            if (o.kind() == Settings.Kind.BOOL) { if (mx >= ctrlX + ctrlW - 34) { set(o, get(o) >= 0.5 ? 0 : 1); save(o); } return true; }
            if (mx >= ctrlX - 4 && mx <= ctrlX + ctrlW - 34) { dragging = r; slideTo(o, mx); return true; }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging >= 0 && dragging < options().size()) { slideTo(options().get(dragging), mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override public boolean mouseReleased(double mx, double my, int button) {
        if (dragging >= 0 && dragging < options().size()) { save(options().get(dragging)); dragging = -1; return true; }
        return super.mouseReleased(mx, my, button);
    }

    @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        if (in(mx, my, px + 8, py + 36, 112, ph - 74)) sideScroll = Math.max(0, sideScroll - (int) (sy * 20));
        else scroll = Math.max(0, scroll - (int) (sy * 20));
        return true;
    }
}
