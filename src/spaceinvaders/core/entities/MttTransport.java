package spaceinvaders.core.entities;

/** Independent state for one 100-HP Multi-Troop Transport. */
public final class MttTransport extends Invader {
    public enum State { DESCENDING, DEPLOYMENT_PAUSE, DEPLOYING, DESCENDING_AFTER_DEPLOYMENT }
    public State state = State.DESCENDING;
    public int stateMs;
    public int deployed;
    public int verticalMs;
    public long nextVolleyAtMs;

    public MttTransport(int x, int y) {
        super(x, y, 150, 95, InvaderKind.MTT, null);
        hp = 100;
        touchDamage = 4;
        scoreValue = 500;
        nextVolleyAtMs = 5000 + Math.floorMod(x, 10001);
    }

    public boolean deploymentInProgress() { return state == State.DEPLOYMENT_PAUSE || state == State.DEPLOYING; }
}
