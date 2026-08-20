package dev.luchoc.littlespaceship.core.testsupport;

import dev.luchoc.littlespaceship.core.port.BalanceValues;
import dev.luchoc.littlespaceship.core.port.ContentSource;

/**
 * A {@link ContentSource} wrapping a {@link TestBalance}, for tests that need a world but not a
 * real content pipeline.
 */
public final class TestContent implements ContentSource {

    public final TestBalance balance;

    public TestContent() {
        this(new TestBalance());
    }

    public TestContent(TestBalance balance) {
        this.balance = balance;
    }

    @Override
    public BalanceValues balance() {
        return balance;
    }
}
