package spaceinvaders.core;

/** Keeps track of the active Scene and handles enter/exit lifecycle. */
public final class SceneManager {
    private Scene current;

    /** Switch to a new scene (calls onExit on old, onEnter on new). */
    public void set(Scene next) {
        if (current != null) current.onExit();
        current = next;
        if (current != null) current.onEnter();
    }

    /** Get the currently active scene (may be null). */
    public Scene current() {
        return current;
    }

    /** True if a scene is active. */
    public boolean hasScene() {
        return current != null;
    }

    /** Remove any active scene (calls onExit if present). */
    public void clear() {
        if (current != null) current.onExit();
        current = null;
    }
}
