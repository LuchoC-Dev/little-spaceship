package dev.luchoc.littlespaceship.core.port;

/**
 * One position inside a {@link FormationDefinition}, relative to the wave's spawn anchor.
 *
 * @param offsetX horizontal offset from the anchor, in logical units, positive to the right
 * @param offsetY vertical offset from the anchor, in logical units, positive upwards like
 *     {@code Transform}
 */
public record FormationSlot(float offsetX, float offsetY) {
}
