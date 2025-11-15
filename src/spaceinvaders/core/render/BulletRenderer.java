package spaceinvaders.core.render;

import spaceinvaders.core.GameState;
import spaceinvaders.core.entities.Bullet;

import java.awt.Graphics2D;

public interface BulletRenderer {
    void render(Graphics2D g, Bullet b, GameState state);
}
