package mx.semanai.emgchart;

public class Valores {

    private int sample;
    private float val;

    public Valores(int sample, float val) {
        this.sample = sample;
        this.val = val;
    }

    public int getSample() {
        return sample;
    }

    public void setSample(int sample) {
        this.sample = sample;
    }

    public float getVal() {
        return val;
    }

    public void setVal(float val) {
        this.val = val;
    }
}
