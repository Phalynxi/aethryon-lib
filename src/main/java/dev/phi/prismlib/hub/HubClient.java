package dev.phi.prismlib.hub;

import dev.phi.prismlib.PrismLib;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/** The hub's button (on the title, pause and options screens, its icon turning smoothly through the logos of the registered mods) and the friendly screen it opens. Client only. */
@EventBusSubscriber(modid = PrismLib.ID, value = Dist.CLIENT)
public final class HubClient {
    private HubClient() {}

    private static final int PERIOD_MS = 3000, FADE_MS = 700;

    // ------------------------------------------------------------------ the button

    private static Button make(Screen from, int x, int y) {
        Button b = new Button(x, y, 20, 20, Component.literal("Mods"), btn -> Minecraft.getInstance().setScreen(new HubScreen(from)), supplier -> supplier.get()) {
            private PrismHub.Entry shown;
            @Override
            public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                super.renderWidget(g, mx, my, pt);
                List<PrismHub.Entry> all = PrismHub.visible();
                if (all.isEmpty()) return;
                long t = Util.getMillis(); int n = all.size(), i = (int) ((t / PERIOD_MS) % n);
                long into = t % PERIOD_MS; float f = into > PERIOD_MS - FADE_MS ? Mth.clamp((into - (PERIOD_MS - FADE_MS)) / (float) FADE_MS, 0f, 1f) : 0f;
                f = f * f * (3f - 2f * f);
                var cur = all.get(i); var next = all.get((i + 1) % n);
                var dominant = f < 0.5f ? cur : next;
                if (dominant != shown) { shown = dominant; setTooltip(Tooltip.create(Component.literal(dominant.title()).append(Component.literal("  (Prism: all your mods)").withStyle(net.minecraft.ChatFormatting.GRAY)))); }   // the tooltip names the mod whose logo is showing
                cur.logo().draw(g, getX() + 2, getY() + 2, 16, n > 1 ? 1f - f : 1f);
                if (n > 1 && f > 0f) next.logo().draw(g, getX() + 2, getY() + 2, 16, f);
            }
            @Override
            protected void renderScrollingString(GuiGraphics g, net.minecraft.client.gui.Font font, int inset, int color) { /* the logo is the label */ }
        };
        return b;
    }

    /** Widgets that do not count as being in the way (Essential's skin preview and badges are invisible hit areas). */
    private static boolean solid(Object l) { return l instanceof AbstractWidget w && w.visible && !w.getClass().getName().startsWith("gg.essential."); }

    private static boolean free(List<? extends net.minecraft.client.gui.components.events.GuiEventListener> all, Screen screen, Button self, int x, int y) {
        if (x < 2 || y < 2 || x + 20 > screen.width - 2 || y + 20 > screen.height - 2) return false;
        for (var l : all)
            if (l != self && solid(l) && l instanceof AbstractWidget w && x < w.getX() + w.getWidth() && x + 20 > w.getX() && y < w.getY() + w.getHeight() && y + 20 > w.getY()) return false;
        return true;
    }

    private static final Map<Screen, Button> PENDING = new WeakHashMap<>();

    /** Adds the button at a first guess; the real spot is worked out at the first frame, once every mod's buttons are there. */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void add(ScreenEvent.Init.Post e) {
        var screen = e.getScreen();
        if (!(screen instanceof TitleScreen) && !(screen instanceof PauseScreen) && !(screen instanceof OptionsScreen)) return;
        if (PrismHub.visible().isEmpty()) return;
        Button b = make(screen, screen.width / 2 + 160, 6);
        e.addListener(b);
        PENDING.put(screen, b);
    }

    @SubscribeEvent
    public static void place(ScreenEvent.Render.Pre e) {
        var screen = e.getScreen();
        Button b = PENDING.remove(screen);
        if (b == null) return;
        var all = screen.children();
        if (screen instanceof OptionsScreen) {
            Button online = null, video = null;
            for (var l : all) if (l instanceof Button x) {
                if (x.getMessage().equals(Component.translatable("options.online"))) online = x;
                else if (x.getMessage().equals(Component.translatable("options.video"))) video = x;
            }
            if (online != null && free(all, screen, b, online.getX() + online.getWidth() + 4, online.getY())) b.setPosition(online.getX() + online.getWidth() + 4, online.getY());
            else if (video != null && free(all, screen, b, video.getX() - 24, video.getY())) b.setPosition(video.getX() - 24, video.getY());
            return;
        }
        for (var l : all) {   // under Create's button if it is there (or above or beside it when something is in the way)
            if (l instanceof AbstractWidget w && w.getClass().getName().startsWith("com.simibubi.create.") && w.getWidth() <= 24) {
                int[][] spots = {{w.getX(), w.getY() + w.getHeight() + 4}, {w.getX(), w.getY() - 24}, {w.getX() + w.getWidth() + 4, w.getY()}, {w.getX() - 24, w.getY()}};
                for (int[] sp : spots) if (free(all, screen, b, sp[0], sp[1])) { b.setPosition(sp[0], sp[1]); return; }
            }
        }
        Component options = Component.translatable("menu.options");
        AbstractWidget optionsButton = null;
        for (var l : all) if (l instanceof Button x && x != b && x.getMessage().equals(options)) optionsButton = x;
        if (optionsButton == null) return;
        int right = optionsButton.getX() + optionsButton.getWidth(), y = optionsButton.getY();
        for (var l : all) if (l != b && solid(l) && l instanceof AbstractWidget w && w.getY() == y && w.getX() + w.getWidth() > right) right = w.getX() + w.getWidth();
        if (free(all, screen, b, right + 4, y)) b.setPosition(right + 4, y);
    }

    // ------------------------------------------------------------------ the screen

    /** The list of mods with settings, in the game's own look: a title and a column of ordinary buttons, each with the mod's logo inside it and a line about it as its tooltip. */
    public static final class HubScreen extends Screen {
        private final Screen parent;

        public HubScreen(Screen parent) { super(Component.literal("Mod Settings")); this.parent = parent; }

        @Override protected void init() {
            int w = 220, x = width / 2 - w / 2, y = 36;
            for (var e : PrismHub.visible()) {
                final var entry = e;
                Button b = new Button(x, y, w, 24, Component.literal(entry.title()), btn -> Minecraft.getInstance().setScreen(entry.opener().apply(this)), supplier -> supplier.get()) {
                    @Override public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
                        super.renderWidget(g, mx, my, pt);
                        entry.logo().draw(g, getX() + 4, getY() + 4, 16, 1f);
                    }
                    @Override protected void renderScrollingString(GuiGraphics g, net.minecraft.client.gui.Font font, int inset, int color) {
                        g.drawString(font, getMessage(), getX() + 28, getY() + (getHeight() - 8) / 2, color);
                    }
                };
                if (!entry.tagline().isBlank()) b.setTooltip(Tooltip.create(Component.literal(entry.tagline())));
                addRenderableWidget(b);
                y += 28;
            }
            addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose()).bounds(width / 2 - 100, height - 27, 200, 20).build());
        }

        @Override public void onClose() { Minecraft.getInstance().setScreen(parent); }

        @Override public void render(GuiGraphics g, int mx, int my, float pt) {
            super.render(g, mx, my, pt);
            g.drawCenteredString(font, title, width / 2, 15, 0xFFFFFF);
            if (PrismHub.visible().isEmpty()) g.drawCenteredString(font, "None of your mods have settings", width / 2, 50, 0xA0A0A0);
        }
    }
}
