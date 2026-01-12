package spaceinvaders.core.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import spaceinvaders.core.GameState;
import spaceinvaders.core.entities.Invader;

public final class InvaderRenderer {

    private InvaderRenderer() {}

    public static void render(Graphics2D g, Invader inv, GameState state) {
        // base invader
        Image invaderImg = state.invaderImage;
        if (invaderImg != null) {
            g.drawImage(invaderImg, inv.x, inv.y, inv.width, inv.height, null);
        } else {
            g.setColor(Color.GREEN);
            g.fillRect(inv.x, inv.y, inv.width, inv.height);
        }

        // shield overlay
        if (inv.shieldHits > 0) {
            g.setColor(new Color(120, 200, 255, 90));
            g.fillOval(inv.x - 6, inv.y - 6, inv.width + 12, inv.height + 12);

            g.setColor(new Color(120, 200, 255, 140));
            g.drawOval(inv.x - 4, inv.y - 4, inv.width + 8, inv.height + 8);

            g.setColor(new Color(200, 240, 255, 120));
            g.drawArc(inv.x - 2, inv.y - 2, inv.width + 4, inv.height + 4, 40, 80);
        }

        // (optional) If you later add a shield break flash timer on Invader, draw it here too.
        // e.g. inv.shieldBreakFlashMs > 0 -> draw rings
    }
}
