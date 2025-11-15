package spaceinvaders.core.entities;

/** Strategy for invader movement. Keep it stateless or use fields on Invader. */
public interface MovementPattern {
    void update(Invader inv, long dtMs, int panelW, int panelH);
}
