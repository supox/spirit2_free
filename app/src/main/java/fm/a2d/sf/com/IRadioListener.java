package fm.a2d.sf.com;

import fm.a2d.sf.domain.Frequency;
import fm.a2d.sf.domain.TunerState;

public interface IRadioListener {
    void onTunerBulk();

    void onStateChanged(TunerState state);

    void onFreqChanged(Frequency freq);

    // RDS - todos
    void cb_tuner_rssi(String val);

    void cb_tuner_pilot(String val);

    void cb_tuner_qual(String val);

    void cb_tuner_rds_pi(String val);

    void cb_tuner_rds_pt(String val);

    void cb_tuner_rds_ps(String val);

    void cb_tuner_rds_rt(String val);
}
