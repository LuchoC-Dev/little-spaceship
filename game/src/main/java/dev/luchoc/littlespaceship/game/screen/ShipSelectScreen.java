package dev.luchoc.littlespaceship.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import dev.luchoc.littlespaceship.core.port.BalanceValues;
import dev.luchoc.littlespaceship.game.LittleSpaceshipGame;
import dev.luchoc.littlespaceship.game.adapter.content.JsonContentSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Ship selection/building, per {@code docs/planning/02-mvp-functional-spec.md}: one selectable
 * ship, its characteristics, and a screen conceptually ready for more without offering unlocking,
 * purchase or deep customisation — none of which the MVP has.
 *
 * <p>The empty second slot from {@code docs/design/mockups/screens.html} is not drawn here: the
 * spec calls it optional, and the design note for it — "it says the system grows without promising
 * anything" — is exactly the kind of promise this screen has no data to back yet. It can be added
 * with no layout change once a second ship exists to slot in.
 */
public final class ShipSelectScreen extends BaseUiScreen {

    public ShipSelectScreen(LittleSpaceshipGame game) {
        super(game, "SELECT SHIP");
        content.top().left();

        Label name = new Label("PROTOTYPE X-1", skin, "hud-label");
        Label description = new Label("SUSTAINED SHOT, BOMB, SLOW MODE", skin, "body");
        content.add(name).left().padBottom(6f).row();
        content.add(description).left().padBottom(16f).row();

        // The header labels used to sit alone with no numbers beside them — not a legibility issue,
        // the values simply were not read from anywhere. BalanceValues is what content.balance()
        // already exposes about the one MVP ship; a second content source instance costs nothing at
        // load time and keeps this screen free of a Simulation it has no other reason to start.
        BalanceValues balance = new JsonContentSource(Gdx.files.internal("data")).balance();
        Table stats = new Table();
        addStat(stats, "SPEED", String.format(Locale.ROOT, "%.0f", balance.playerSpeed()));
        addStat(stats, "FIRE", String.format(Locale.ROOT, "%.2fs", balance.weaponFireCooldown()));
        addStat(stats, "BOMBS", balance.initialBombs() + "/" + balance.maxBombs());
        addStat(stats, "LIVES", balance.initialLives() + "/" + balance.maxLives());
        content.add(stats).left().padBottom(24f).row();

        List<KeyboardFocusable> focusables = new ArrayList<>();
        MenuEntries.add(content, skin, "LAUNCH", () -> game.setScreen(new PlayScreen(game)), focusables);
        MenuEntries.add(content, skin, "BACK", () -> game.setScreen(new MenuScreen(game)), focusables);
        new MenuNavigator(stage, focusables);
    }

    /** One header/value column, header in {@code hud-label} (N4), value in {@code stat-value} (N7) —
     * the same label/value colour split {@code HudRenderer} uses, so a value reads as a value here
     * too rather than blending into its own header. */
    private void addStat(Table table, String header, String value) {
        Table column = new Table();
        column.add(new Label(header, skin, "hud-label")).left().row();
        column.add(new Label(value, skin, "stat-value")).left().row();
        table.add(column).left().padRight(24f);
    }
}
