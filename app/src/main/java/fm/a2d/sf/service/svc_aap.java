
    // Audio component API implemented by svc_aud for RadioService

package fm.a2d.sf.service;

    // Code move to 2 functions: audio_get() and audio_set(), as seen in svc_tap / svc_tnr. to minimize the coupling between main SpiritF Service RadioService, and it's instantiated Audio class svc_aud.
    // Two functions makes new additions easy; just define new key, value pairs to pass.

    // For 1 function, we could use Set/Get model seen in C tuner plugin code, eg: audio_sg()

    interface svc_aap {
      String audio_sessid_get();

      String audio_state_set(String new_state);

      String audio_mode_set(String new_mode);

      String audio_output_set(String new_output, boolean start_set);

      String audio_stereo_set(String new_stereo);

      String audio_record_state_set(String new_record_state);

      String audio_digital_amp_set();
}

