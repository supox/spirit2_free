package fm.a2d.sf.com;

import fm.a2d.sf.domain.AudioMode;
import fm.a2d.sf.domain.AudioOutput;
import fm.a2d.sf.domain.AudioState;
import fm.a2d.sf.domain.DigitalAmpState;
import fm.a2d.sf.domain.RecordState;
import fm.a2d.sf.domain.StereoState;

public interface IAudio {

    String audio_sessid_get();

    AudioState getAudioState();

    String setAudioState(AudioState state);

    AudioMode getMode();

    void setMode(AudioMode mode);

    void setOutput(AudioOutput mode, boolean startSet);

    AudioOutput getOutput();

    StereoState getStereo();

    void setStereo(StereoState state);

    RecordState getRecordState();

    void setRecordState(RecordState state);

    DigitalAmpState getDigitalAmpState();

    void setDigitalAmpState(DigitalAmpState state);
}
