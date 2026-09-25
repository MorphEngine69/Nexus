package com.morphengine.nexus.api.network;

/**
 * Identification color of a network, shown in the Nexus interface and on the
 * devices of the network. It does not affect connectivity or cable color.
 *
 * @param rgb packed {@code 0xRRGGBB} value, must be in {@code [0, MAX_RGB]}
 */
public record NetworkColor(int rgb) {

    /** The largest packed value, white; also the mask that keeps only the RGB bits of an ARGB color. */
    public static final int MAX_RGB = 0xFFFFFF;

    public static final NetworkColor DEFAULT = new NetworkColor(0x385892);

    public NetworkColor {
        if (rgb < 0 || rgb > MAX_RGB) {
            throw new IllegalArgumentException("rgb out of range [0, 0xFFFFFF]: " + rgb);
        }
    }
}
