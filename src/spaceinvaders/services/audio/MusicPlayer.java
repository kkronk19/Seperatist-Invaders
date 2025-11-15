// src/spaceinvaders/services/audio/MusicPlayer.java
package spaceinvaders.services.audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.InputStream;
import java.net.URL;

public class MusicPlayer {
    private Clip clip;

    public void playLoopFromUrl(String url) throws Exception {
    stop();
    AudioInputStream ais = AudioSystem.getAudioInputStream(new URL(url));
    clip = AudioSystem.getClip();
    clip.open(ais);
    clip.loop(Clip.LOOP_CONTINUOUSLY);
}

    public void playLoopFromResource(String resourcePath) throws Exception {
        stop();
        try (InputStream in = MusicPlayer.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new IllegalArgumentException("Resource not found: " + resourcePath);
            AudioInputStream ais = AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(in));
            clip = AudioSystem.getClip();
            clip.open(ais);
            clip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public void playLoopFromFile(File file) throws Exception {
        stop();
        AudioInputStream ais = AudioSystem.getAudioInputStream(file);
        clip = AudioSystem.getClip();
        clip.open(ais);
        clip.loop(Clip.LOOP_CONTINUOUSLY);
    }

    public void stop() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }

    /** Linear 0..1 volume; maps to MASTER_GAIN dB */
    public void setVolumeLinear(float v) {
        if (clip == null) return;
        v = Math.max(0f, Math.min(1f, v));
        FloatControl ctrl = getGainControl(clip);
        if (ctrl != null) {
            float dB = linearToDecibels(v, ctrl);
            ctrl.setValue(dB);
        }
    }

    private static FloatControl getGainControl(Clip c) {
        try { return (FloatControl) c.getControl(FloatControl.Type.MASTER_GAIN); }
        catch (IllegalArgumentException ignored) { return null; }
    }

    private static float linearToDecibels(float v, FloatControl ctrl) {
        // Avoid -Inf: when v==0, push near the control’s minimum
        if (v <= 0f) return ctrl.getMinimum();
        float dB = (float) (20.0 * Math.log10(v));
        return clamp(dB, ctrl.getMinimum(), ctrl.getMaximum());
        // Typical useful mapping: 1.0 -> 0 dB, 0.5 -> -6 dB, 0.25 -> -12 dB, etc.
    }

    private static float clamp(float x, float lo, float hi) {
        return Math.max(lo, Math.min(hi, x));
    }
}
