package fm.a2d.sf.domain;

public class Frequency {
    private static final int MHZ_FREQ_INC = 50;
    private final double m_frequency;

    public Frequency(String frequency) {
        this(Integer.valueOf(frequency));
    }

    public Frequency(int frequency) {
        m_frequency = Region.getCurrentRegion().normalizeFrequency(frequency);
    }

    public double toDouble() {
        return m_frequency;
    }

    public int toInt() {
        return (int) m_frequency;
    }

    public String toMHzString() {
        int freq = toInt();
        freq += MHZ_FREQ_INC / 2;
        freq = freq / MHZ_FREQ_INC;
        freq *= MHZ_FREQ_INC;
        double dfreq = (double) freq;
        dfreq = java.lang.Math.rint(dfreq);
        dfreq /= 1000;
        return ("" + dfreq);
    }
}

