package dev.phi.prismlib.settings;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.OptionsList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModContainer;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** The screen for {@link Settings}: every category of a mod, in one scrolling list; options are changed and saved as they are touched. Client only. */
public final class SettingsClient {
    private SettingsClient() {}

    /** Put the mod's settings behind its Config button in the mod list. Call from the mod's constructor, on the client only. */
    public static void install(ModContainer container, String modId, String title) { install(container, modId, title, null); }

    /** The same with the mod's own window: a {@link ThemedSettingsScreen} in the given colours (null: the plain list). */
    public static void install(ModContainer container, String modId, String title, ThemedSettingsScreen.Theme theme) {
        String tag = net.neoforged.fml.ModList.get().getModContainerById(modId).map(m -> m.getModInfo().getDescription()).map(d -> d.strip().split("\\R")[0]).orElse("");
        var logoTex = net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(modId, "textures/gui/hub_logo.png");
        dev.phi.prismlib.hub.PrismHub.Logo logo = (g, x, y, size, alpha) -> { var rm = Minecraft.getInstance().getResourceManager(); (rm.getResource(logoTex).isPresent() ? dev.phi.prismlib.hub.PrismHub.texture(logoTex) : dev.phi.prismlib.hub.PrismHub.letter(title)).draw(g, x, y, size, alpha); };
        java.util.function.Function<Screen, Screen> opener = parent -> theme == null ? new Page(parent, modId, title) : new ThemedSettingsScreen(parent, modId, title, tag, theme, logo);
        container.registerExtensionPoint(IConfigScreenFactory.class, (c, parent) -> opener.apply(parent));
        // and the mod appears in the hub (the one screen for all the mods), with its logo textures/gui/hub_logo.png if it has one and the first line of its description
        dev.phi.prismlib.hub.PrismHub.register(new dev.phi.prismlib.hub.PrismHub.Entry(modId, title, tag, logo, opener));
    }

    private static final class Page extends net.minecraft.client.gui.screens.options.OptionsSubScreen {
        private final String modId;
        Page(Screen parent, String modId, String title) { super(parent, Minecraft.getInstance().options, Component.literal(title)); this.modId = modId; }

        @Override protected void addOptions() {
            for (Settings.Category cat : Settings.of(modId)) {
                var head = Button.builder(Component.literal(cat.title()), b -> {}).width(310).tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(cat.description()))).build();
                head.active = false;
                list.addSmall(List.<AbstractWidget>of(head));
                List<OptionInstance<?>> row = new ArrayList<>();
                for (Settings.Option o : cat.options()) {
                    row.add(widget(o));
                    if (row.size() == 2) { list.addSmall(row.get(0), row.get(1)); row.clear(); }
                }
                if (!row.isEmpty()) list.addSmall(row.get(0), null);
            }
        }

        @SuppressWarnings("unchecked")
        private static OptionInstance<?> widget(Settings.Option o) {
            var tip = OptionInstance.<Object>cachedConstantTooltip(Component.literal(o.description()));
            switch (o.kind()) {
                case BOOL -> {
                    var v = (net.neoforged.neoforge.common.ModConfigSpec.BooleanValue) o.value();
                    return OptionInstance.createBoolean(o.name(), OptionInstance.<Boolean>cachedConstantTooltip(Component.literal(o.description())), v.get(), on -> { v.set(on); v.save(); });
                }
                case INT -> {
                    var v = (net.neoforged.neoforge.common.ModConfigSpec.IntValue) o.value();
                    return new OptionInstance<>(o.name(), OptionInstance.<Integer>cachedConstantTooltip(Component.literal(o.description())), (cap, n) -> Component.literal(o.name() + ": " + o.unit() + n),
                            new OptionInstance.IntRange((int) o.min(), (int) o.max()), v.get(), n -> { v.set(n); v.save(); });
                }
                default -> {
                    var v = (net.neoforged.neoforge.common.ModConfigSpec.DoubleValue) o.value();
                    int steps = Math.max(1, (int) Math.round((o.max() - o.min()) / o.step()));
                    int now = (int) Math.round((v.get() - o.min()) / o.step());
                    return new OptionInstance<>(o.name(), OptionInstance.<Integer>cachedConstantTooltip(Component.literal(o.description())),
                            (cap, n) -> Component.literal(o.name() + ": " + String.format(Locale.ROOT, "%.2f", o.min() + n * o.step()).replaceAll("\\.?0+$", "") + o.unit()),
                            new OptionInstance.IntRange(0, steps), Math.max(0, Math.min(steps, now)), n -> { v.set(o.min() + n * o.step()); v.save(); });
                }
            }
        }
    }
}
