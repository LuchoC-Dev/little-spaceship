package dev.luchoc.littlespaceship.core.port;

/**
 * Where the simulation gets the content it does not invent: balance values, enemy definitions,
 * level timelines.
 *
 * <p>The core parses nothing. It declares what it needs and the adapter hands it over already
 * built, which is why a test can assemble content by hand without reading a single file and why
 * changing the content format touches no game rule.
 *
 * <p>Enemy definitions and level timelines join this port with the content pipeline. Declaring them
 * now would mean guessing their shape before anything reads them.
 */
public interface ContentSource {

    /**
     * @return the balance values for this run, never null
     */
    BalanceValues balance();
}
