package spaceinvaders.app;

import spaceinvaders.core.GameState;
import spaceinvaders.ui.menu.GameConfigurator;

public final class DefaultAssets {
    private DefaultAssets(){}

    public static void apply(GameState s, GameConfigurator cfg) {
        // Images – swallow errors so game still boots if a file is missing
        try { cfg.setPlayerImage("image/playerImage.png"); } catch (Exception ignored) {}
        try { cfg.setInvaderImage("image/InvaderImage.png"); } catch (Exception ignored) {}
        try { cfg.setBulletImage("image/blaster_bolt.png"); } catch (Exception ignored) {}

        // Any other default state knobs:
        s.playerX = 200;
        s.playerWidth = 50;
        s.playerHeight = 60;
    }
}
