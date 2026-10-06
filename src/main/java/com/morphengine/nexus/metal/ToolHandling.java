package com.morphengine.nexus.metal;

/**
 * The attack baselines of the axe and the hoe of a metal. Vanilla sets them for each tier on its own, so that the sum
 * with the damage bonus of the tier comes out right.
 */
public record ToolHandling(float axeDamage, float axeSpeed, float hoeDamage, float hoeSpeed) {
}
