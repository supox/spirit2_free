package fm.a2d.sf.domain;

public abstract class Region {

    private static Region s_currentRegion;

    public static void setCurrentRegion(Band band) {
        switch (band) {
            case EU:
                setCurrentRegion(new EuropeRegion());
                break;
            case US:
                setCurrentRegion(new AmericaRegion());
                break;
            case UU:
                setCurrentRegion(new ChinaRegion());
                break;
        }
    }

    public static Region getCurrentRegion() {
        return s_currentRegion;
    }

    public static void setCurrentRegion(Region region) {
        s_currentRegion = region;
    }

    public int getInc() {
        return 100;
    }

    public int getFreqLo() {
        return 87500;
    }

    public int getFreqHigh() {
        return 10800;
    }

    public boolean getFreqOdd() {
        return false;
    }

    public int roundFrequency(int freq) {
        final int inc = getInc();
        if (getFreqOdd()) {
            freq += inc / 2;
            freq /= inc;
            freq *= inc;
            freq -= inc / 2;
        } else {
            freq /= inc;
            freq *= inc;
        }
        return (freq);
    }

    public int normalizeFrequency(int freq) {
        if (freq < getFreqLo())
            freq = getFreqHigh();
        if (freq > getFreqHigh())
            freq = getFreqLo();
        return roundFrequency(freq);
    }
}

