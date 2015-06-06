package fm.a2d.sf.domain;

public class Preset {
    public String name;
    public String freq;
    public boolean isValid;

    public Preset() {
        name = "";
        freq = "";
        isValid = false;
    }

    public Preset(String name, String freq) {
        this.name = name;
        this.freq = freq;
        isValid = true;
    }
}
