package com.atenas.fabric.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.atenas.fabric.cosmetics.CapeTextures;
import com.atenas.fabric.cosmetics.CosmeticSync;
import com.atenas.fabric.cosmetics.Cosmetics;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.AssetInfo;
import net.minecraft.util.Identifier;

/**
 * UNICO punto donde se engancha la capa: se cambia la textura de capa de las SkinTextures del jugador y el juego dibuja la capa de siempre
 * (misma fisica de tela y misma animacion que vanilla). Si la capa de Atenas aun carga, se deja la que ya tenia.
 * Si el compilador protesta por un nombre de Yarn (SkinTextures / AssetInfo.TextureAssetInfo), es aqui donde hay que ajustarlo.
 */
@Mixin(AbstractClientPlayerEntity.class)
public abstract class PlayerCapeMixin {
    @Inject(method = "getSkinTextures", at = @At("RETURN"), cancellable = true)
    private void atenas$cape(CallbackInfoReturnable<SkinTextures> cir) {
        String id = CosmeticSync.capeOf((AbstractClientPlayerEntity) (Object) this);
        if (id == null) return;
        Identifier tex = CapeTextures.get(Cosmetics.find(id));
        if (tex == null) return;
        SkinTextures s = cir.getReturnValue();
        cir.setReturnValue(new SkinTextures(s.body(), new AssetInfo.TextureAssetInfo(tex, tex), s.elytra(), s.model(), s.secure()));
    }
}
