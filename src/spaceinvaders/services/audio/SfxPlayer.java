// src/spaceinvaders/services/audio/SfxPlayer.java
package spaceinvaders.services.audio;

import javax.sound.sampled.*;
import java.io.InputStream;

public class SfxPlayer {
    // Fire-and-forget one-shots
    public void play(String resourcePath) {
        play(resourcePath, 1.0f);
    }

    public void play(String resourcePath, float volumeLinear) {
        try (InputStream in = SfxPlayer.class.getResourceAsStream(resourcePath)) {
            if (in == null) return;
            AudioInputStream ais = AudioSystem.getAudioInputStream(new java.io.BufferedInputStream(in));
            Clip clip = AudioSystem.getClip();
            clip.open(ais);
            setVolumeLinear(clip, volumeLinear);
            clip.start();
            // Let the system clean up when clip ends
            clip.addLineListener(ev -> {
                if (ev.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
        } catch (Exception ignored) {
            // fail softly
        }
    }

    private static void setVolumeLinear(Clip clip, float v) {
        v = Math.max(0f, Math.min(1f, v));
        try {
            FloatControl ctrl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            float dB = (v <= 0f) ? ctrl.getMinimum()
                                 : (float)(20.0 * Math.log10(v));
            dB = Math.max(ctrl.getMinimum(), Math.min(ctrl.getMaximum(), dB));
            ctrl.setValue(dB);
        } catch (IllegalArgumentException ignored) {
            // device may not expose MASTER_GAIN; best-effort
        }
    }
}
