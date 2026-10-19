package com.atenas.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.ClientBrandRetriever;

/**
 * Nombre del cliente en F3 ("Minecraft 1.21.11 (1.21.11/atenasclient)"). El texto sale de ClientBrandRetriever.getClientModName():
 * vanilla devuelve "vanilla" y Fabric Loader parchea ese metodo para devolver "fabric". Aqui se sustituye el valor final.
 * Es la misma marca que se envia al servidor y se ve en los informes de fallo. require = 0: si el metodo no se encuentra, se salta
 * sin tumbar el juego (AtenasFabricClient avisa en el log si la marca no quedo aplicada).
 */
@Mixin(ClientBrandRetriever.class)
public abstract class ClientBrandMixin {
    @Inject(method = "getClientModName", at = @At("RETURN"), cancellable = true, require = 0)
    private static void atenas$brand(CallbackInfoReturnable<String> cir) { cir.setReturnValue("atenasclient"); }
}
