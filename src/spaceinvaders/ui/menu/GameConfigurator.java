package spaceinvaders.ui.menu;
import spaceinvaders.services.exceptions.GameExceptions;

import spaceinvaders.core.GameState;
import spaceinvaders.services.audio.MusicPlayer;
import spaceinvaders.services.loading.AssetLoader;

import java.awt.Image;

/** Handles changes to game assets and options from menus. */
public class GameConfigurator {
    private final GameState state;
    private final MusicPlayer music;

    public GameConfigurator(GameState state, MusicPlayer music) {
        this.state = state;
        this.music = music;
    }

    /* ---------- player ---------- */
    public void setPlayerImage(String resourcePath) {
        try {
            Image img = AssetLoader.imageFromResource(resourcePath);
            state.playerImage = img;
        } catch (Exception e) {
            GameExceptions.showErrorDialog("Could not load player: " + e.getMessage());
        }
    }

    /* ---------- Invader ---------- */
    public void setInvaderImage(String resourcePath) {
        try {
            Image img = AssetLoader.imageFromResource(resourcePath);
            state.invaderImage = img;
        } catch (Exception e) {
            GameExceptions.showErrorDialog("Could not load invader: " + e.getMessage());
        }
    }

    /* ---------- Bullets ---------- */
    public void setBulletType(GameState.BulletType type) {
        state.bulletType = type;
    }

    public void setBulletImage(String resourcePath) {
        try {
            Image img = AssetLoader.imageFromResource(resourcePath);
            state.bulletImage = img;
            state.bulletType = GameState.BulletType.IMAGE;
        } catch (Exception e) {
            GameExceptions.showErrorDialog("Could not load bullet: " + e.getMessage());
        }
    }

    /* ---------- Music ---------- */
    public void playMusic(String resourcePath) {
        try {
            music.playLoopFromResource(resourcePath);
        } catch (Exception e) {
            GameExceptions.showErrorDialog("Music error: " + e.getMessage());
        }
    }

    public void stopMusic() {
        music.stop();
    }

    /* ---------- Future Upgrades ---------- */
    public void applyUpgrade(String upgradeName) {
        // e.g. switch bullet type, adjust fire rate, etc.
        // left empty for now; you’ll populate this when implementing upgrades
    }
}
