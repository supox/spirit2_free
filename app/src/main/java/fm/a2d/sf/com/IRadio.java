package fm.a2d.sf.com;

import fm.a2d.sf.domain.AFState;
import fm.a2d.sf.domain.Band;
import fm.a2d.sf.domain.Frequency;
import fm.a2d.sf.domain.RDSState;
import fm.a2d.sf.domain.SeekState;
import fm.a2d.sf.domain.StereoState;
import fm.a2d.sf.domain.TunerState;

public interface IRadio {
    Frequency getFrequency();

    void setFrequency(Frequency freq);

    Band getBand();

    void setBand(Band band);

    SeekState getSeekState();

    void setSeekState(SeekState state);

    StereoState getStereoState();

    void setStereoState(StereoState state);

    void setListener(IRadioListener listener);

    AFState getAFState();

    void setAFState(AFState state);

    TunerState getTunerState();

    void setTunerState(TunerState state);

    RDSState getRDSState();

    void setRDSState(RDSState state);

}
