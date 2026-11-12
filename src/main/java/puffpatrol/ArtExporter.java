package puffpatrol;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/** Writes every generated sprite to PNG files so the art can be browsed outside the game. */
public final class ArtExporter {
    private ArtExporter() {
    }

    public static void main(String[] args) {
        File out = new File(args.length > 0 ? args[0] : "art");
        try {
            export(out);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Art written to " + out.getAbsolutePath());
    }

    static void export(File dir) throws IOException {
        File towers = new File(dir, "towers"), puffs = new File(dir, "puffs"), proj = new File(dir, "projectiles"), maps = new File(dir, "maps");
        for (File f : new File[]{towers, puffs, proj, maps}) f.mkdirs();
        for (TowerType t : TowerType.values()) write(Art.towerImage(t, 2), new File(towers, name(t) + ".png"));
        for (PuffType p : PuffType.values()) {
            write(Art.puffImage(p, 0, 2), new File(puffs, name(p) + ".png"));
            if (p.showsDamage()) write(Art.puffImage(p, 3, 2), new File(puffs, name(p) + "_cracked.png"));
        }
        for (Projectile.Kind k : Projectile.Kind.values()) write(Art.projectileImage(k, false, 3), new File(proj, name(k) + ".png"));
        write(Art.projectileImage(Projectile.Kind.STINGER, true, 3), new File(proj, "fire_stinger.png"));
        for (GameMap m : Maps.all()) write(m.background(), new File(maps, m.name.toLowerCase().replace(' ', '_') + ".png"));
        write(sheet(), new File(dir, "contact_sheet.png"));
    }

    static BufferedImage sheet() {
        int w = 1100, h = 680;
        BufferedImage img = Art.newImage(w, h);
        Graphics2D g = Art.gfx(img);
        g.setPaint(Art.vgrad(0, new Color(0xF7EFD9), h, new Color(0xE9DCBC)));
        g.fillRect(0, 0, w, h);
        g.setFont(new Font(Font.SERIF, Font.BOLD | Font.ITALIC, 34));
        g.setColor(Art.INK);
        g.drawString("Puffball Patrol: the cast", 30, 50);
        Font label = new Font(Font.SANS_SERIF, Font.PLAIN, 13);
        g.setFont(new Font(Font.SERIF, Font.ITALIC, 20));
        g.drawString("The meadow folk", 30, 92);
        TowerType[] ts = TowerType.values();
        for (int i = 0; i < ts.length; i++) {
            int x = 30 + i * 150, y = 104;
            card(g, x, y, 136, 170);
            g.drawImage(Art.towerImage(ts[i], 2), x + 4, y + 4, null);
            g.setFont(label);
            g.setColor(Art.INK);
            int sw = g.getFontMetrics().stringWidth(ts[i].label);
            g.drawString(ts[i].label, x + 68 - sw / 2, y + 156);
        }
        g.setFont(new Font(Font.SERIF, Font.ITALIC, 20));
        g.drawString("The visiting puffs", 30, 312);
        PuffType[] ps = PuffType.values();
        int x = 30;
        for (int i = 0; i < ps.length; i++) {
            BufferedImage p = Art.puffImage(ps[i], 0, i < 11 ? 1.6 : 1.0);
            int y = i < 11 ? 330 : 470;
            if (i == 11) x = 30;
            int cw = Math.max(86, p.getWidth() + 10);
            card(g, x, y, cw, i < 11 ? 120 : 190);
            g.drawImage(p, x + cw / 2 - p.getWidth() / 2, y + (i < 11 ? 50 : 86) - p.getHeight() / 2, null);
            g.setFont(label);
            g.setColor(Art.INK);
            String n = ps[i].label.replace(" puff", "");
            g.drawString(n, x + cw / 2 - g.getFontMetrics().stringWidth(n) / 2, y + (i < 11 ? 110 : 180));
            x += cw + 8;
        }
        int px = 600;
        g.setFont(new Font(Font.SERIF, Font.ITALIC, 20));
        g.drawString("Things that get thrown", px, 490);
        Projectile.Kind[] ks = Projectile.Kind.values();
        for (int i = 0; i < ks.length + 1; i++) {
            BufferedImage p = i < ks.length ? Art.projectileImage(ks[i], false, 3) : Art.projectileImage(Projectile.Kind.STINGER, true, 3);
            card(g, px + i * 92, 504, 80, 80);
            g.drawImage(p, px + i * 92 + 10, 514, null);
        }
        g.dispose();
        return img;
    }

    private static void card(Graphics2D g, int x, int y, int w, int h) {
        Shape s = new RoundRectangle2D.Double(x, y, w, h, 14, 14);
        g.setColor(new Color(0xFBF6E8));
        g.fill(s);
        g.setColor(new Color(0x8B6B4A));
        g.setStroke(new BasicStroke(1.5f));
        g.draw(s);
    }

    private static String name(Enum<?> e) {
        return e.name().toLowerCase();
    }

    private static void write(BufferedImage img, File f) throws IOException {
        ImageIO.write(img, "png", f);
    }
}
