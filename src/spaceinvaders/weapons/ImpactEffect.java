// src/spaceinvaders/weapons/ImpactEffect.java
package spaceinvaders.weapons;

import spaceinvaders.core.GameState;

public interface ImpactEffect {
    void apply(GameState state, int x, int y);
}
