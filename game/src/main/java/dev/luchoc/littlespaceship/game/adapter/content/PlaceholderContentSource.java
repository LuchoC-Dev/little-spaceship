package dev.luchoc.littlespaceship.game.adapter.content;

import dev.luchoc.littlespaceship.core.port.BalanceValues;
import dev.luchoc.littlespaceship.core.port.ContentSource;

/**
 * The only {@link ContentSource} this phase needs: fixed balance values and nothing else.
 *
 * <p>Enemy definitions and level timelines join this port with the content pipeline in phase 04;
 * this class does not implement them because nothing reads them yet.
 */
public final class PlaceholderContentSource implements ContentSource {

    private final BalanceValues balance = new PlaceholderBalanceValues();

    @Override
    public BalanceValues balance() {
        return balance;
    }
}
