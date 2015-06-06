package fm.a2d.sf.service;

import android.content.Context;

import fm.a2d.sf.com.IRadio;
import fm.a2d.sf.com.IRadioListener;
import fm.a2d.sf.com.com_api;
import fm.a2d.sf.com.com_uti;
import fm.a2d.sf.domain.AFState;
import fm.a2d.sf.domain.Band;
import fm.a2d.sf.domain.Frequency;
import fm.a2d.sf.domain.RDSState;
import fm.a2d.sf.domain.SeekState;
import fm.a2d.sf.domain.StereoState;
import fm.a2d.sf.domain.TunerState;

public class RadioController implements IRadio {

    private final svc_tap tnr;
    private IRadioListener m_listener = null;
    private final svc_tcb m_svcListener = new svc_tcb() {
        @Override
        public void cb_tuner_key(String key, String val) {
            if (m_listener == null)
                return;

            switch (key) {
                case "tuner_bulk":
                    m_listener.onTunerBulk();
                    break;
                case "tuner_state":
                    m_listener.onStateChanged(TunerState.valueOf(val));
                    break;
                case "tuner_freq":
                    m_listener.onFreqChanged(new Frequency(val));
                    break;

                // TODO - RDS
                case "tuner_rssi":
                    m_listener.cb_tuner_rssi(val);
                    break;
                case "tuner_pilot ":
                    m_listener.cb_tuner_pilot(val);
                    break;
                case "tuner_qual":
                    m_listener.cb_tuner_qual(val);
                    break;
                case "tuner_rds_pi":
                    m_listener.cb_tuner_rds_pi(val);
                    break;
                case "tuner_rds_pt":
                    m_listener.cb_tuner_rds_pt(val);
                    break;
                case "tuner_rds_ps":
                    m_listener.cb_tuner_rds_ps(val);
                    break;
                case "tuner_rds_rt":
                    m_listener.cb_tuner_rds_rt(val);
                    break;
                default:
                    com_uti.loge("key: " + key);
                    break;
            }
        }
    };

    RadioController(Context context, com_api api) {
        tnr = new svc_tnr(context, m_svcListener, api);
    }

    @Override
    public void setListener(IRadioListener listener) {
        m_listener = listener;
    }

    @Override
    public Frequency getFrequency() {
        String freq = tnr.tuner_get("tuner_freq");
        return new Frequency(freq);
    }

    @Override
    public void setFrequency(Frequency freq) {
        tnr.tuner_set("tuner_freq", freq.toString());
    }

    @Override
    public Band getBand() {
        return Band.valueOf(tnr.tuner_get("tuner_band"));
    }

    @Override
    public void setBand(Band band) {
        tnr.tuner_set("tuner_band", band.toString());
    }

    @Override
    public SeekState getSeekState() {
        return SeekState.valueOf(tnr.tuner_get("tuner_seek_state"));
    }

    @Override
    public void setSeekState(SeekState state) {
        tnr.tuner_set("tuner_seek_state", state.toString());
    }

    @Override
    public StereoState getStereoState() {
        return StereoState.valueOf(tnr.tuner_get("tuner_stereo"));
    }

    @Override
    public void setStereoState(StereoState state) {
        tnr.tuner_set("tuner_stereo", state.toString());
    }

    @Override
    public AFState getAFState() {
        return AFState.valueOf(tnr.tuner_get("tuner_rds_af_state"));
    }

    @Override
    public void setAFState(AFState state) {
        tnr.tuner_set("tuner_rds_af_state", state.toString());
    }

    @Override
    public TunerState getTunerState() {
        return TunerState.valueOf(tnr.tuner_get("tuner_state"));
    }

    @Override
    public void setTunerState(TunerState state) {
        tnr.tuner_set("tuner_state", state.toString());
    }

    @Override
    public RDSState getRDSState() {
        return RDSState.valueOf(tnr.tuner_get("tuner_rds_state"));
    }

    @Override
    public void setRDSState(RDSState state) {
        tnr.tuner_set("tuner_rds_state", state.toString());
    }
}
