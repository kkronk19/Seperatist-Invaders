package spaceinvaders.core.render;

import java.awt.Graphics2D;
import java.util.LinkedHashMap;
import java.util.Map;
import spaceinvaders.core.GameState;
import spaceinvaders.core.entities.Blade;
import spaceinvaders.core.entities.Bullet;
import spaceinvaders.core.entities.Missile;

/** Registry + dispatcher for bullet rendering strategies. */
public final class BulletRenderers {
    private BulletRenderers() {}

    private static final Map<Class<?>, BulletRenderer> REGISTRY = new LinkedHashMap<>();

    static {
        // register most-specific FIRST
        register(Missile.class, new MissileRenderer());
        register(Blade.class,   new BladeRenderer());

        // default LAST so it doesn't steal subclasses in assignable lookup
        register(Bullet.class,  new DefaultBulletRenderer());
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

        // fallback: first renderer whose key is assignable from cls
        for (Map.Entry<Class<?>, BulletRenderer> e : REGISTRY.entrySet()) {
            if (e.getKey().isAssignableFrom(cls)) return e.getValue();
        }

        // ultimate fallback
        return REGISTRY.get(Bullet.class);
    }
}
