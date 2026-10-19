package com.atenas.fabric;

import java.util.List;
import java.util.Locale;

import com.atenas.fabric.hud.Hud;
import com.atenas.fabric.hud.Mods;
import com.atenas.fabric.shared.Config;
import com.atenas.fabric.shared.SharedDir;
import com.atenas.fabric.shared.Waypoint;
import com.atenas.fabric.shared.WaypointStore;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;

/**
 * Atenas Client para 1.21.11 (Fabric, mappings Yarn). Primer corte: base del proyecto + waypoints compartidos con el cliente 1.8.9
 * (mismo archivo <.minecraft>/atenas/waypoints.json, mismo formato y mismas claves de mundo). Los demas modulos se portan encima.
 */
public final class AtenasFabricClient implements ClientModInitializer {
    public static final String VERSION = "0.3.0";

    @Override
    public void onInitializeClient() {
        System.out.println("[AtenasClient] 1.21.11 listo. Waypoints en " + SharedDir.dir());

        Mods.init();
        ClientTickEvents.END_CLIENT_TICK.register(AtenasLoading::tryInstall);
        ClientTickEvents.END_CLIENT_TICK.register(Mods::tick);
        ClientTickEvents.END_CLIENT_TICK.register(com.atenas.fabric.hud.Extras::tick);
        com.atenas.fabric.hud.Extras.register();
        net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback.EVENT.register((type, renderer, helper, ctx) -> {
            if (type == net.minecraft.entity.EntityType.PLAYER) helper.register(new com.atenas.fabric.cosmetics.CosmeticLayer(renderer));
        });
        ClientTickEvents.END_CLIENT_TICK.register(com.atenas.fabric.hud.HudExtras::tick);
        ClientTickEvents.END_CLIENT_TICK.register(com.atenas.fabric.cosmetics.CosmeticSync::tick);
        HudElementRegistry.attachElementBefore(VanillaHudElements.CHAT, Identifier.of("atenasclientfabric", "hud"), Hud::render);

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            ClientCommandManager.literal("atenas")
                .then(ClientCommandManager.argument("modulo", StringArgumentType.word())
                    .suggests((c, b) -> net.minecraft.command.CommandSource.suggestMatching(Mods.toggleNames(), b))
                    .executes(ctx -> {
                        String m = StringArgumentType.getString(ctx, "modulo");
                        if (!Mods.toggleNames().contains(m)) { ctx.getSource().sendFeedback(Text.literal("Modulos: " + String.join(", ", Mods.toggleNames()))); return 0; }
                        boolean on = Config.toggle(m, !java.util.Set.of("fullbright", "speed", "memory", "clock").contains(m));
                        ctx.getSource().sendFeedback(Text.literal(m + (on ? ": activado" : ": desactivado")));
                        return 1;
                    }))));
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
            ClientCommandManager.literal("atenaswp")
                .then(ClientCommandManager.literal("list").executes(ctx -> {
                    List<Waypoint> l = WaypointStore.current();
                    if (l.isEmpty()) ctx.getSource().sendFeedback(Text.literal("No hay waypoints en este mundo."));
                    for (Waypoint w : l) {
                        ctx.getSource().sendFeedback(Text.literal(String.format(Locale.ROOT, "%s: %.0f %.0f %.0f (dim %d)", w.name, w.x, w.y, w.z, w.dim)));
                    }
                    return 1;
                }))
                .then(ClientCommandManager.literal("add")
                    .executes(ctx -> add(ctx.getSource().getPlayer().getX(), ctx.getSource().getPlayer().getY(), ctx.getSource().getPlayer().getZ(), null, ctx.getSource()))
                    .then(ClientCommandManager.argument("nombre", StringArgumentType.greedyString())
                        .executes(ctx -> add(ctx.getSource().getPlayer().getX(), ctx.getSource().getPlayer().getY(), ctx.getSource().getPlayer().getZ(),
                            StringArgumentType.getString(ctx, "nombre"), ctx.getSource())))))
        );
    }

    private static int add(double x, double y, double z, String name, net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource src) {
        String n = name == null || name.trim().isEmpty() ? WaypointStore.nextName() : name.trim();
        Waypoint w = WaypointStore.add(n, x, y, z, WaypointStore.currentDim(), 3);
        src.sendFeedback(Text.literal(w == null ? "Limite de waypoints alcanzado en este mundo." : "Waypoint guardado: " + n));
        return 1;
    }
}
