package spaceinvaders.services.loading;

import spaceinvaders.services.exceptions.InvalidAudioException;
import spaceinvaders.services.exceptions.InvalidImageException;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.Image;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.net.*;

/** Handles resource loading from file, URL, or classpath. */
public final class AssetLoader {
    private static final String CLASSPATH_ROOT = "/spaceinvaders/resources/";
    private AssetLoader() {}

    /* -------------------- IMAGES -------------------- */

    /** Load an image from classpath (e.g. "image/p1_clone.png"). */
    public static Image imageFromResource(String relativePath) throws InvalidImageException {
        String path = normalize(relativePath);
        try {
            URL url = AssetLoader.class.getResource(path);
            if (url == null) throw new FileNotFoundException("Resource not found: " + path);
            Image img = ImageIO.read(url);
            if (img == null) throw new IllegalArgumentException("Not an image: " + path);
            return img;
        } catch (IOException | IllegalArgumentException e) {
            throw new InvalidImageException("Failed to load image: " + relativePath, e);
        }
    }

    /** Load an image from a local file. */
    public static Image imageFromFile(File file) throws InvalidImageException {
        try {
            Image img = ImageIO.read(file);
            if (img == null) throw new IllegalArgumentException("Not an image file: " + file.getAbsolutePath());
            return img;
        } catch (IOException | IllegalArgumentException e) {
            throw new InvalidImageException("Failed to load image file: " + file, e);
        }
    }

    /** Load an image from a URL. */
    public static Image imageFromUrl(String urlString) throws InvalidImageException {
        try {
            URL url = new URI(urlString).toURL();
            try (InputStream in = url.openStream()) {
                Image img = ImageIO.read(in);
                if (img == null) throw new IllegalArgumentException("URL did not return an image: " + urlString);
                return img;
            }
        } catch (URISyntaxException | IOException | IllegalArgumentException e) {
            throw new InvalidImageException("Failed to load image URL: " + urlString, e);
        }
    }

    /* -------------------- AUDIO (WAV) -------------------- */

    /** Load audio (e.g., WAV) from classpath (e.g. "audio/the_heist.wav"). */
    public static AudioInputStream audioFromResource(String relativePath) throws InvalidAudioException {
        String path = normalize(relativePath);
        try {
            URL url = AssetLoader.class.getResource(path);
            if (url == null) throw new FileNotFoundException("Resource not found: " + path);
            return AudioSystem.getAudioInputStream(url);
        } catch (UnsupportedAudioFileException | IllegalArgumentException | IOException e) {
            throw new InvalidAudioException("Failed to load audio: " + relativePath, e);
        }
    }

    /** Load audio from local file. */
    public static AudioInputStream audioFromFile(File file) throws InvalidAudioException {
        try {
            return AudioSystem.getAudioInputStream(file);
        } catch (UnsupportedAudioFileException | IOException e) {
            throw new InvalidAudioException("Failed to load audio file: " + file, e);
        }
    }

    /** Load audio from URL. */
    public static AudioInputStream audioFromUrl(String urlString) throws InvalidAudioException {
        try {
            URL url = new URI(urlString).toURL();
            return AudioSystem.getAudioInputStream(url);
        } catch (UnsupportedAudioFileException | IOException | URISyntaxException e) {
            throw new InvalidAudioException("Failed to load audio URL: " + urlString, e);
        }
    }

    /* -------------------- helpers -------------------- */

    private static String normalize(String relativePath) {
        if (relativePath == null || relativePath.isBlank())
            throw new IllegalArgumentException("Empty resource path");
        String p = relativePath.startsWith("/") ? relativePath.substring(1) : relativePath;
        return CLASSPATH_ROOT + p;
    }
}
