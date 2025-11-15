package spaceinvaders.core.render;

import spaceinvaders.core.GameState;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.Missile;

import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;
import java.awt.Graphics2D;

/** Registry + dispatcher for bullet rendering strategies. */
public final class BulletRenderers {
    private BulletRenderers() {}

    private static final Map<Class<?>, BulletRenderer> REGISTRY = new LinkedHashMap<>();

    static {
        // register defaults
        register(Bullet.class,  new DefaultBulletRenderer());
        register(Missile.class, new MissileRenderer());
    }

    public static void register(Class<?> bulletClass, BulletRenderer renderer) {
        REGISTRY.put(bulletClass, renderer);
    }

    public static void render(Graphics2D g, Bullet b, GameState state) {
        BulletRenderer r = findRenderer(b.getClass());
        r.render(g, b, state);
    }

    private static BulletRenderer findRenderer(Class<?> cls) {
        // exact match first
        BulletRenderer r = REGISTRY.get(cls);
        if (r != null) return r;

        // fallback: find first renderer whose key is assignable from cls
        for (Map.Entry<Class<?>, BulletRenderer> e : REGISTRY.entrySet()) {
            if (e.getKey().isAssignableFrom(cls)) return e.getValue();
        }

        // ultimate fallback: default bullet renderer
        return REGISTRY.get(Bullet.class);
    }
}
