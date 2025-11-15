// src/spaceinvaders/services/audio/AudioManager.java
package spaceinvaders.services.audio;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public final class AudioManager {
    private static final AudioManager INSTANCE = new AudioManager();
    public static AudioManager get() { return INSTANCE; }

    private final Map<String, MusicPlayer> loops = new ConcurrentHashMap<>();
    private final SfxPlayer sfx = new SfxPlayer();
    private final Random rng = new Random();

    private AudioManager() {}

    // ---------- MUSIC (per-call volume only) ----------
    /** @deprecated Prefer playLoop(key, resourcePath, volume). */
    @Deprecated
    public void playLoop(String key, String resourcePath) {
        playLoop(key, resourcePath, 1.0f);
    }

    public void playLoop(String key, String resourcePath, float volume) {
        stopLoop(key);
        try {
            MusicPlayer p = new MusicPlayer();
            p.playLoopFromResource(resourcePath);
            loops.put(key, p);
            p.setVolumeLinear(clamp01(volume));
        } catch (Exception ignored) {}
    }

    /** Change an already playing loop’s volume. */
    public void setLoopVolume(String key, float volume) {
        MusicPlayer p = loops.get(key);
        if (p != null) p.setVolumeLinear(clamp01(volume));
    }

    public void stopLoop(String key) {
        MusicPlayer p = loops.remove(key);
        if (p != null) p.stop();
    }

    public void stopAllLoops() {
        loops.keySet().forEach(this::stopLoop);
    }

    // ---------- SFX (per-call volume only) ----------
    /** @deprecated Prefer playSfx(resourcePath, volume). */
    @Deprecated
    public void playSfx(String resourcePath) {
        playSfx(resourcePath, 1.0f);
    }

    public void playSfx(String resourcePath, float volume) {
        sfx.play(resourcePath, clamp01(volume));
    }

    // --- Array/varargs random sets (per-call volume only) ---
    /** @deprecated Prefer playSfxFromSet(paths, volume). */
    @Deprecated
    public void playSfxFromSet(String[] resourcePaths) {
        playSfxFromSet(resourcePaths, 1.0f);
    }

    public void playSfxFromSet(String[] resourcePaths, float volume) {
        if (resourcePaths == null || resourcePaths.length == 0) return;
        String pick = resourcePaths[rng.nextInt(resourcePaths.length)];
        playSfx(pick, volume);
    }

    public void playRandomSfx(float volume, String... paths) {
        if (paths == null || paths.length == 0) return;
        String pick = paths[rng.nextInt(paths.length)];
        playSfx(pick, volume);
    }

    private static float clamp01(float v) { return Math.max(0f, Math.min(1f, v)); }
}
