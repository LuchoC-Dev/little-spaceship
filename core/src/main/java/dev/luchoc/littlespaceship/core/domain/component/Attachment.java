package dev.luchoc.littlespaceship.core.domain.component;

/**
 * The player's single equipped attachment: the third defensive layer, after the shield.
 *
 * <p>Durability is data and not a rule fixed in code: the MVP's attachments absorb exactly one hit,
 * but a tougher protective attachment must be supportable later with no code change. The component
 * is removed once durability reaches zero, which is what destroys the attachment and avoids the life
 * that hit would otherwise have cost.
 */
public final class Attachment {

    /** Hits left before the attachment is destroyed. */
    public int durability;

    /**
     * Creates an equipped attachment.
     *
     * @param durability hits it can absorb before being destroyed, strictly positive
     */
    public Attachment(int durability) {
        this.durability = durability;
    }
}
