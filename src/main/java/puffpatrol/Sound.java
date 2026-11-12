package puffpatrol;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.awt.GraphicsEnvironment;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/** Little sound effects synthesised at start-up: no audio files. Silently disabled where audio is unavailable. */
final class Sound {
    enum Sfx { POP, THROW, BOOM, FREEZE, COIN, ROUND, ABILITY, UPGRADE, PLACE, LEAK, ZAP, THUMP }

    private static final float RATE = 22050f;
    private static final Map<Sfx, Clip[]> CLIPS = new EnumMap<>(Sfx.class);
    private static final Map<Sfx, Integer> NEXT = new EnumMap<>(Sfx.class);
    private static final Map<Sfx, Long> LAST = new EnumMap<>(Sfx.class);
    private static volatile boolean ready;
    static volatile boolean enabled = true;

    private Sound() {
    }

    /** Builds the clips on a background thread so the window opens straight away. */
    static void init() {
        if (GraphicsEnvironment.isHeadless()) return;
        Thread t = new Thread(() -> {
            try {
                AudioFormat fmt = new AudioFormat(RATE, 16, 1, true, false);
                for (Sfx s : Sfx.values()) {
                    byte[] pcm = toPcm(synth(s));
                    int copies = s == Sfx.POP || s == Sfx.THROW ? 4 : 2;
                    Clip[] cs = new Clip[copies];
                    for (int i = 0; i < copies; i++) {
                        cs[i] = AudioSystem.getClip();
                        cs[i].open(fmt, pcm, 0, pcm.length);
                    }
                    CLIPS.put(s, cs);
                    NEXT.put(s, 0);
                    LAST.put(s, 0L);
                }
                ready = true;
            } catch (Throwable e) {
                ready = false;
            }
        }, "sound-init");
        t.setDaemon(true);
        t.start();
    }

    static void play(Sfx s) {
        if (!ready || !enabled) return;
        long now = System.nanoTime();
        long gap = switch (s) {
            case POP -> 45_000_000L;
            case THROW -> 70_000_000L;
            case BOOM, ZAP, THUMP, FREEZE -> 90_000_000L;
            default -> 30_000_000L;
        };
        if (now - LAST.get(s) < gap) return;
        LAST.put(s, now);
        Clip[] cs = CLIPS.get(s);
        int i = NEXT.get(s);
        NEXT.put(s, (i + 1) % cs.length);
        Clip c = cs[i];
        c.stop();
        c.setFramePosition(0);
        c.start();
    }

    // ---------------------------------------------------------------- synthesis

    private static double[] synth(Sfx s) {
        Random r = new Random(s.ordinal());
        return switch (s) {
            case POP -> sweep(0.07, 900, 280, 0.35, 0.3, r);
            case THROW -> noise(0.08, 0.12, 0.25, r);
            case BOOM -> boom(r);
            case FREEZE -> sparkle(0.25, r);
            case COIN -> notes(0.07, 0.3, 988, 1319);
            case ROUND -> notes(0.11, 0.25, 523, 659, 784, 1047);
            case ABILITY -> sweep(0.4, 300, 1200, 0.3, 0.05, r);
            case UPGRADE -> notes(0.08, 0.28, 660, 880, 1175);
            case PLACE -> sweep(0.1, 220, 140, 0.4, 0.15, r);
            case LEAK -> sweep(0.3, 400, 150, 0.3, 0.0, r);
            case ZAP -> sweep(0.06, 1600, 900, 0.18, 0.2, r);
            case THUMP -> sweep(0.12, 120, 60, 0.5, 0.35, r);
        };
    }

    private static double[] sweep(double secs, double f0, double f1, double vol, double noise, Random r) {
        int n = (int) (secs * RATE);
        double[] out = new double[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            phase += 2 * Math.PI * (f0 + (f1 - f0) * t) / RATE;
            double env = Math.min(1, i / (RATE * 0.004)) * Math.pow(1 - t, 2);
            out[i] = vol * env * (Math.sin(phase) * (1 - noise) + (r.nextDouble() * 2 - 1) * noise);
        }
        return out;
    }

    private static double[] noise(double secs, double vol, double smooth, Random r) {
        int n = (int) (secs * RATE);
        double[] out = new double[n];
        double v = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            v += (r.nextDouble() * 2 - 1 - v) * smooth;
            out[i] = vol * 4 * v * Math.sin(Math.PI * t);
        }
        return out;
    }

    private static double[] boom(Random r) {
        int n = (int) (0.35 * RATE);
        double[] out = new double[n];
        double v = 0;
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            v += (r.nextDouble() * 2 - 1 - v) * 0.08;
            out[i] = 0.5 * Math.pow(1 - t, 3) * (v * 3 + 0.5 * Math.sin(2 * Math.PI * 60 * i / RATE));
        }
        return out;
    }

    private static double[] sparkle(double secs, Random r) {
        int n = (int) (secs * RATE);
        double[] out = new double[n];
        for (int i = 0; i < n; i++) {
            double t = i / (double) n;
            double f = 2000 + 600 * Math.sin(t * 40);
            out[i] = 0.15 * (1 - t) * Math.sin(2 * Math.PI * f * i / RATE) * (0.6 + 0.4 * Math.sin(t * 90));
        }
        return out;
    }

    private static double[] notes(double each, double vol, double... freqs) {
        int per = (int) (each * RATE);
        double[] out = new double[per * freqs.length];
        for (int k = 0; k < freqs.length; k++) {
            for (int i = 0; i < per; i++) {
                double t = i / (double) per;
                double env = Math.min(1, i / (RATE * 0.003)) * (1 - t);
                double ph = 2 * Math.PI * freqs[k] * i / RATE;
                out[k * per + i] = vol * env * (Math.sin(ph) + 0.25 * Math.sin(ph * 2));
            }
        }
        return out;
    }

    private static byte[] toPcm(double[] s) {
        byte[] b = new byte[s.length * 2];
        for (int i = 0; i < s.length; i++) {
            int v = (int) Math.max(-32767, Math.min(32767, s[i] * 32767));
            b[i * 2] = (byte) v;
            b[i * 2 + 1] = (byte) (v >> 8);
        }
        return b;
    }
}
