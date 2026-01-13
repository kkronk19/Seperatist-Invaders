package spaceinvaders.core.render;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import spaceinvaders.core.GameState;
import spaceinvaders.core.entities.Invader;

public final class InvaderRenderer {

    private InvaderRenderer() {}

    public static void render(Graphics2D g, Invader inv, GameState state) {

        // ----- choose image by kind -----
        Image img;
        switch (inv.kind) {
            case TANK:     img = state.invaderImageTank;     break;
            case SWARMER:  img = state.invaderImageSwarmer;  break;
            case SHIELDED: img = state.invaderImageShielded; break;
            case SHOOTER:  img = state.invaderImageShooter;  break;
            case BASIC:
            default:       img = state.invaderImageBasic;    break;
        }

        // ----- draw base -----
        if (img != null) {
            g.drawImage(img, inv.x, inv.y, inv.width, inv.height, null);
        } else {
            // fallback colors by kind (so you can tell what's missing)
            g.setColor(colorFor(inv.kind));
            g.fillRect(inv.x, inv.y, inv.width, inv.height);
        }

        // ----- shield overlay (ONLY for shielded that still has shield hits) -----
        if (inv.kind == Invader.InvaderKind.SHIELDED && inv.shieldHits > 0) {
            g.setColor(new Color(120, 200, 255, 90));
            g.fillOval(inv.x - 6, inv.y - 6, inv.width + 12, inv.height + 12);

            g.setColor(new Color(120, 200, 255, 140));
            g.drawOval(inv.x - 4, inv.y - 4, inv.width + 8, inv.height + 8);

            g.setColor(new Color(200, 240, 255, 120));
            g.drawArc(inv.x - 2, inv.y - 2, inv.width + 4, inv.height + 4, 40, 80);
        }
    }

    private static Color colorFor(Invader.InvaderKind k) {
        switch (k) {
            case TANK:     return new Color(200, 160, 60); // gold-ish
            case SWARMER:  return new Color(140, 255, 140);
            case SHIELDED: return new Color(100, 200, 255);
            case SHOOTER:  return new Color(255, 120, 120);
            case BASIC:
            default:       return Color.GREEN;
        }
    }
}
