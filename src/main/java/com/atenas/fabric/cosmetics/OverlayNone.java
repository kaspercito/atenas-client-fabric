package com.atenas.fabric.cosmetics;

/**
 * Overlay "sin tinte" (equivale a OverlayTexture.DEFAULT_UV = packUv(0, 10)).
 * Se define aqui para no depender del paquete de OverlayTexture, que cambia entre mappings.
 */
public final class OverlayNone {
    public static final int UV = (0 & 0xFFFF) | ((10 & 0xFFFF) << 16);

    private OverlayNone() {}
}
