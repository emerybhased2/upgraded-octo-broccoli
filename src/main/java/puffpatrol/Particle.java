package puffpatrol;

import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;

final class Particle {
    enum Kind { POP, TEXT, RING, BOOM, STREAK, COIN }

    private static final Font TEXT_FONT = new Font(Font.SERIF, Font.BOLD | Font.ITALIC, 14);
    private static final Font COIN_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 13);

    final Kind kind;
    double x, y, x2, y2, vx, vy, size, spin;
    int life;
    final int max;
    Color color = Color.WHITE;
    String text;

    private Particle(Kind kind, double x, double y, int life) {
        this.kind = kind;
        this.x = x;
        this.y = y;
        this.life = life;
        this.max = life;
    }

    static Particle pop(double x, double y, Color c, double r, double spin) {
        Particle p = new Particle(Kind.POP, x, y, 14);
        p.color = c;
        p.size = r;
        p.spin = spin;
        return p;
    }

    static Particle text(double x, double y, String s, Color c) {
        Particle p = new Particle(Kind.TEXT, x, y, 40);
        p.text = s;
        p.color = c;
        p.vy = -0.8;
        return p;
    }

    static Particle ring(double x, double y, double r, Color c) {
        Particle p = new Particle(Kind.RING, x, y, 18);
        p.size = r;
        p.color = c;
        return p;
    }

    static Particle boom(double x, double y, double r) {
        Particle p = new Particle(Kind.BOOM, x, y, 16);
        p.size = r;
        return p;
    }

    static Particle streak(double x, double y, double x2, double y2) {
        Particle p = new Particle(Kind.STREAK, x, y, 10);
        p.x2 = x2;
        p.y2 = y2;
        return p;
    }

    static Particle coin(double x, double y, int amount) {
        Particle p = new Particle(Kind.COIN, x, y, 70);
        p.text = "+" + amount;
        p.vy = -0.7;
        return p;
    }

    boolean update() {
        x += vx;
        y += vy;
        vy *= 0.97;
        return --life > 0;
    }

    void draw(Graphics2D g) {
        float a = life / (float) max;
        float t = 1 - a;
        switch (kind) {
            case POP -> {
                g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(Art.alpha(color, (int) (255 * a)));
                for (int i = 0; i < 7; i++) {
                    double ang = spin + i * Math.PI * 2 / 7;
                    double r0 = size * (0.5 + t * 0.9), r1 = r0 + 3 + 3 * a;
                    g.draw(new Line2D.Double(x + Math.cos(ang) * r0, y + Math.sin(ang) * r0,
                            x + Math.cos(ang) * r1, y + Math.sin(ang) * r1));
                }
                g.setColor(Art.alpha(Color.WHITE, (int) (180 * a)));
                double cr = size * (0.3 + t * 0.6);
                g.draw(new Ellipse2D.Double(x - cr, y - cr, cr * 2, cr * 2));
            }
            case RING -> {
                double r = size * (0.35 + 0.65 * t);
                g.setStroke(new BasicStroke(3f * a + 1));
                g.setColor(Art.alpha(color, (int) (200 * a)));
                g.draw(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
                g.setColor(Art.alpha(color, (int) (50 * a)));
                g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
            }
            case BOOM -> {
                double r = size * (0.45 + 0.6 * t);
                g.setColor(new Color(255, 170, 60, (int) (170 * a)));
                g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
                double r2 = r * 0.6;
                g.setColor(new Color(255, 236, 150, (int) (220 * a)));
                g.fill(new Ellipse2D.Double(x - r2, y - r2, r2 * 2, r2 * 2));
                g.setStroke(new BasicStroke(1.5f));
                g.setColor(Art.alpha(Art.INK, (int) (120 * a)));
                g.draw(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
            }
            case STREAK -> {
                g.setStroke(new BasicStroke(4f * a + 0.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.setColor(new Color(255, 246, 200, (int) (230 * a)));
                g.draw(new Line2D.Double(x, y, x2, y2));
                g.setStroke(new BasicStroke(1.2f));
                g.setColor(new Color(140, 100, 60, (int) (200 * a)));
                g.draw(new Line2D.Double(x, y, x2, y2));
            }
            case TEXT -> {
                g.setFont(TEXT_FONT);
                int w = g.getFontMetrics().stringWidth(text);
                g.setColor(Art.alpha(Art.INK, (int) (200 * a)));
                g.drawString(text, (float) (x - w / 2.0 + 1), (float) y + 1);
                g.setColor(Art.alpha(color, (int) (255 * a)));
                g.drawString(text, (float) (x - w / 2.0), (float) y);
            }
            case COIN -> {
                Composite old = g.getComposite();
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(1f, a * 2)));
                Art.drawCoin(g, x - 10, y, 8);
                g.setFont(COIN_FONT);
                g.setColor(Art.INK);
                g.drawString(text, (float) x + 1, (float) y + 6);
                g.setColor(new Color(0xFFE27A));
                g.drawString(text, (float) x, (float) y + 5);
                g.setComposite(old);
            }
        }
    }
}
