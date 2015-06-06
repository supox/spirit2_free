
// Service

package fm.a2d.sf.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.net.wifi.WifiManager;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import fm.a2d.sf.PresetsManager;
import fm.a2d.sf.R;
import fm.a2d.sf.com.IRadio;
import fm.a2d.sf.com.IRadioListener;
import fm.a2d.sf.com.com_api;
import fm.a2d.sf.com.com_uti;
import fm.a2d.sf.domain.AFState;
import fm.a2d.sf.domain.Band;
import fm.a2d.sf.domain.Frequency;
import fm.a2d.sf.domain.Preset;
import fm.a2d.sf.domain.RDSState;
import fm.a2d.sf.domain.Region;
import fm.a2d.sf.domain.StereoState;
import fm.a2d.sf.domain.TunerState;
import fm.a2d.sf.gui.gui_act;


public class RadioService extends Service implements svc_acb, IRadioListener {  // Service class implements Tuner API callbacks & Service Audio API callback

    // Also see AndroidManifest.xml.

    // Action: Commands sent to this service
    //"fm.a2d.sf.action.set"  = com_uti.api_action_id

    // Result: Broadcast multiple status/state data items to all registered listeners; apps, widgets, etc.
    //"fm.a2d.sf.result.get"  = com_uti.api_result_id


    // No constructor

    // Static data:
    private static int stat_creates = 1;

    // Instance data:
    private Context m_context = this;
    private com_api m_com_api = null;
    private svc_aap m_svc_aap = null;

    private Timer tuner_state_start_tmr = null;

    private AudioManager m_AM = null;

    private boolean service_update_gui = true;
    private PresetsManager m_PresetManager;
    private IRadio m_Radio;
    private PowerManager pmgr = null;

    // Continuing methods in lifecycle order:  (Some don't apply to services, more for activities)

  /*@Override
    public void onStart (Intent intent, int param_int) {
    com_uti.logd ("");
    super.onStart (intent, param_int);
    com_uti.logd ("");
    }*/
    private boolean need_audio_start_after_tuner_start = false;

    @Override
    public void onCreate() {                                             // When service newly created...
        com_uti.logd("stat_creates: " + stat_creates++);
        m_PresetManager = new PresetsManager(m_context);

        try {
            if (m_com_api == null) {                                          // If not yet initialized...
                m_com_api = new com_api(this);                                 // Instantiate Common API   class
                com_uti.logd("m_com_api: " + m_com_api);
            }
            m_Radio = new RadioController(m_context, m_com_api);
            m_Radio.setListener(this);

            m_com_api.chass_plug_aud = com_uti.chass_plug_aud_get(m_context);// Setup Audio Plugin
            m_com_api.chass_plug_tnr = com_uti.chass_plug_tnr_get(m_context);// Setup Tuner Plugin

            com_uti.strict_mode_set(false);                                  // !!!! Disable strict mode so we can send network packets from Java

            m_AM = (AudioManager) m_context.getSystemService(Context.AUDIO_SERVICE);

            boolean remote_rcc_enable = false;
            if (com_uti.s2_tx_apk()) {                                       // If Transmit APK
                remote_rcc_enable = false;                                      // Remote/RCC not needed, for receive only
            }
            // Else if receive mode and Kitkat or earlier
            else if (com_uti.android_version < VERSION_CODES.LOLLIPOP) {
                remote_rcc_enable = true;                                       // Needed for lockscreen and AVRCP on BT devices
            }
            // Else if receive mode and Lollipop or later
            else if (com_uti.android_version >= VERSION_CODES.LOLLIPOP) {
                remote_rcc_enable = true;                                       // Needed for AVRCP on BT devices only ?
            }

            m_svc_aap = new svc_aud(this, this, m_com_api);                  // Instantiate audio        class

            String update_gui = com_uti.prefs_get(m_context, "service_update_gui", "");
            service_update_gui = !update_gui.equals("Disable");
            String update_notification = com_uti.prefs_get(m_context, "service_update_notification", "");
            String update_remote = com_uti.prefs_get(m_context, "service_update_remote", "");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        if (m_com_api == null) {
            return;
        }

        if (!m_com_api.audio_state.equals("Stop"))
            com_uti.loge("destroy with m_com_api.audio_state: " + m_com_api.audio_state);

        if (!m_com_api.tuner_state.equals("Stop"))
            com_uti.loge("destroy with m_com_api.tuner_state: " + m_com_api.tuner_state);

        if (!m_com_api.tuner_api_state.equals("Stop"))
            com_uti.loge("destroy with m_com_api.tuner_api_state: " + m_com_api.tuner_api_state);

        foreground_stop();

        m_Radio = null;
    }


    // Main entrance for commands sent from other components: GUI, Widget, Notification Shade, Remote Controls...
    // onStartCommand handles command intents sent via key_set() (->startService())

    private void foreground_start() {
        //if (service_update_remote || service_update_notification) // Stops media buttons ?

        com_uti.logd("Calling startForeground");

        Intent resultIntent = new Intent(this, gui_act.class);
        PendingIntent pIntent = PendingIntent.getActivity(
                this,
                0,
                resultIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        Notification notification = new Notification.Builder(this)
                .setContentTitle("Spirit 2")
                .setContentText("Tap to open")
                .setSmallIcon(R.drawable.img_icon)
                .setContentIntent(pIntent)
                .addAction(R.drawable.btn_rw, "", com_api.pend_intent_get(this, "service_seek_state", "Down"))
                .addAction(R.drawable.btn_play, "", com_api.pend_intent_get(this, "tuner_state", "Toggle"))
                .addAction(R.drawable.btn_ff, "", com_api.pend_intent_get(this, "service_seek_state", "Up"))
                .build();
        startForeground(com_uti.s2_notif_id, notification);
    }

    private void foreground_stop() {
        com_uti.logd("Calling stopForeground");
        stopForeground(true);                                            // Stop Foreground (for audio) and remove notification (true)       !! match startForeground()

    }


    // For state and status updates:

    @Override
    public IBinder onBind(Intent arg0) {
        com_uti.logd("");

        return (null);                                                       // Binding not allowed ; no direct call API, must use Intents
    }

    @Override                                                             //
    public int onStartCommand(Intent intent, int flags, int startId) {   //
        com_uti.logd("intent: " + intent + "  flags: " + flags + "  startId: " + startId);

        int start_type = START_STICKY;
        try {

            if (intent == null) {
                com_uti.loge("intent == null");
                return (start_type);
            }
            String action = intent.getAction();
            if (action == null) {
                com_uti.loge("action == null");
                return (start_type);
            }
            if (!action.equals(com_uti.api_action_id)) {                      // If this is NOT our "fm.a2d.sf.action.set" Intent...
                com_uti.loge("action: " + action);
                return (start_type);                                              // Done w/ error
            }

            Bundle extras = intent.getExtras();
            if (extras == null) {
                com_uti.loge("NULL extras");
                return (start_type);
            }
            com_uti.logd("extras: " + extras.describeContents());

            String val;


            // audio_android_smo    // ?? setMode == setPhoneState ???
            if (!(val = extras.getString("audio_android_smo", "")).equals("")) {
                m_AM.setMode(com_uti.int_get(val));
                com_uti.logd("com_uti.setMode: " + m_AM.getMode());
            }
            if (!(val = extras.getString("audio_android_gmo", "")).equals(""))
                com_uti.logd("com_uti.getMode: " + m_AM.getMode());

            // audio_android_sso
            if (!(val = extras.getString("audio_android_sso", "")).equals("")) {
                if (com_uti.int_get(val) == 0)
                    m_AM.setSpeakerphoneOn(false);
                else
                    m_AM.setSpeakerphoneOn(true);
            }

            // audio_android_spa
            if (!(val = extras.getString("audio_android_spa", "")).equals(""))
                m_AM.setParameters(val);

            // audio_android_gpa
            if (!(val = extras.getString("audio_android_gpa", "")).equals(""))
                com_uti.logd("m_AM.getParameters (): " + m_AM.getParameters(val));

            // audio_android_gfc
            if (!(val = extras.getString("audio_android_gfc", "")).equals(""))
                com_uti.logd("com_uti.getForceUse (FOR_COMMUNICATION): " + com_uti.getForceUse(com_uti.FOR_COMMUNICATION));
            // audio_android_gfm
            if (!(val = extras.getString("audio_android_gfm", "")).equals(""))
                com_uti.logd("com_uti.getForceUse (FOR_MEDIA): " + com_uti.getForceUse(com_uti.FOR_MEDIA));
            // audio_android_gfr
            if (!(val = extras.getString("audio_android_gfr", "")).equals(""))
                com_uti.logd("com_uti.getForceUse (FOR_RECORD): " + com_uti.getForceUse(com_uti.FOR_RECORD));
            // audio_android_gfd
            if (!(val = extras.getString("audio_android_gfd", "")).equals(""))
                com_uti.logd("com_uti.getForceUse (FOR_DOCK): " + com_uti.getForceUse(com_uti.FOR_DOCK));
            // audio_android_gfs
            if (!(val = extras.getString("audio_android_gfs", "")).equals(""))
                com_uti.logd("com_uti.getForceUse (FOR_SYSTEM): " + com_uti.getForceUse(com_uti.FOR_SYSTEM));
            // audio_android_gfh
            if (!(val = extras.getString("audio_android_gfh", "")).equals(""))
                com_uti.logd("com_uti.getForceUse (FOR_HDMI): " + com_uti.getForceUse(com_uti.FOR_HDMI_SYSTEM_AUDIO));

            // audio_android_sfc
            if (!(val = extras.getString("audio_android_sfc", "")).equals(""))
                com_uti.logd("com_uti.setForceUse (FOR_COMMUNICATION): " + com_uti.setForceUse(com_uti.FOR_COMMUNICATION, com_uti.int_get(val)));
            // audio_android_sfm
            if (!(val = extras.getString("audio_android_sfm", "")).equals(""))
                com_uti.logd("com_uti.setForceUse (FOR_MEDIA): " + com_uti.setForceUse(com_uti.FOR_MEDIA, com_uti.int_get(val)));
            // audio_android_sfr
            if (!(val = extras.getString("audio_android_sfr", "")).equals(""))
                com_uti.logd("com_uti.setForceUse (FOR_RECORD): " + com_uti.setForceUse(com_uti.FOR_RECORD, com_uti.int_get(val)));
            // audio_android_sfd
            if (!(val = extras.getString("audio_android_sfd", "")).equals(""))
                com_uti.logd("com_uti.setForceUse (FOR_DOCK): " + com_uti.setForceUse(com_uti.FOR_DOCK, com_uti.int_get(val)));
            // audio_android_sfs
            if (!(val = extras.getString("audio_android_sfs", "")).equals(""))
                com_uti.logd("com_uti.setForceUse (FOR_SYSTEM): " + com_uti.setForceUse(com_uti.FOR_SYSTEM, com_uti.int_get(val)));
            // audio_android_sfh
            if (!(val = extras.getString("audio_android_sfh", "")).equals(""))
                com_uti.logd("com_uti.setForceUse (FOR_HDMI): " + com_uti.setForceUse(com_uti.FOR_HDMI_SYSTEM_AUDIO, com_uti.int_get(val)));


            // audio_android_gdco
            if (!(val = extras.getString("audio_android_gdco", "")).equals(""))
                com_uti.logd("output com_uti.getDeviceConnectionState (): " + com_uti.getDeviceConnectionState(com_uti.int_get(val), ""));
            // audio_android_gdci
            if (!(val = extras.getString("audio_android_gdci", "")).equals(""))
                com_uti.logd("input com_uti.getDeviceConnectionState (): " + com_uti.getDeviceConnectionState(com_uti.int_get(val), ""));
            // audio_android_argo
            if (!(val = extras.getString("audio_android_argo", "")).equals(""))
                com_uti.logd("output com_uti.output_audio_routing_get (): " + com_uti.output_audio_routing_get());
            // audio_android_argi
            if (!(val = extras.getString("audio_android_argi", "")).equals(""))
                com_uti.logd("input com_uti.output_audio_routing_get (): " + com_uti.input_audio_routing_get());


            // audio_android_sduo
            if (!(val = extras.getString("audio_android_sduo", "")).equals(""))
                com_uti.logd("output com_uti.setDeviceConnectionState (UNAVAILABLE): " + com_uti.setDeviceConnectionState(com_uti.int_get(val), com_uti.DEVICE_STATE_UNAVAILABLE, ""));
            // audio_android_sdao
            if (!(val = extras.getString("audio_android_sdao", "")).equals(""))
                com_uti.logd("output com_uti.setDeviceConnectionState (AVAILABLE): " + com_uti.setDeviceConnectionState(com_uti.int_get(val), com_uti.DEVICE_STATE_AVAILABLE, ""));
            // audio_android_sdui
            if (!(val = extras.getString("audio_android_sdui", "")).equals(""))
                com_uti.logd("input com_uti.setDeviceConnectionState (UNAVAILABLE): " + com_uti.setDeviceConnectionState(com_uti.int_get(val) | 0x80000000, com_uti.DEVICE_STATE_UNAVAILABLE, ""));
            // audio_android_sdai
            if (!(val = extras.getString("audio_android_sdai", "")).equals(""))
                com_uti.logd("input com_uti.setDeviceConnectionState (AVAILABLE): " + com_uti.setDeviceConnectionState(com_uti.int_get(val) | 0x80000000, com_uti.DEVICE_STATE_AVAILABLE, ""));


            // tuner_seek_state
            val = extras.getString("tuner_seek_state", "");
            if (!val.equals("")) {
                m_Radio.setTunerState(TunerState.valueOf(val));
            }

            val = extras.getString("service_seek_state", "");

            if (val.equals("Down")) {
                preset_next(false);
            } else if (val.equals("Up")) {
                preset_next(true);
            }

            // Tuner:
            val = extras.getString("tuner_state", "");
            if (!val.equals(""))
                tuner_state_set(val);

            val = extras.getString("tuner_freq", "");
            if (!val.equals(""))
                tuner_freq_set(val);

            val = extras.getString("tuner_band", "");
            if (!val.equals("")) {
                setBand(val);
            }

            val = extras.getString("tuner_stereo", "");
            if (!val.equals("")) {
                m_Radio.setStereoState(StereoState.valueOf(val));
                com_uti.prefs_set(m_context, "tuner_stereo", val);
            }

            val = extras.getString("tuner_rds_state", "");
            if (!val.equals("")) {
                tuner_rds_state_set(val);
            }

            val = extras.getString("tuner_rds_af_state", "");
            if (!val.equals("")) {
                tuner_rds_af_state_set(val);
            }


            // These are just passed directly to the daemon:
            com_uti.extras_daemon_set("tuner_vol", extras);
            com_uti.extras_daemon_set("tuner_mute", extras);
            com_uti.extras_daemon_set("tuner_acdb", extras);
            com_uti.extras_daemon_set("tuner_softmute", extras);
            com_uti.extras_daemon_set("tuner_thresh", extras);
            com_uti.extras_daemon_set("tuner_antenna", extras);

            com_uti.extras_daemon_set("tuner_rds_pi", extras);
            com_uti.extras_daemon_set("tuner_rds_pt", extras);
            com_uti.extras_daemon_set("tuner_rds_ps", extras);
            com_uti.extras_daemon_set("tuner_rds_rt", extras);

            //Disable Plugin changing to ensure problems don't happen while FM running
            //    com_uti.extras_daemon_set ("chass_plug_aud", extras);
            //    com_uti.extras_daemon_set ("chass_plug_tnr", extras);


            // Audio:
            val = extras.getString("audio_mode", "");
            if (!val.equals("")) {
                m_svc_aap.audio_mode_set(val);
                com_uti.prefs_set(m_context, "audio_mode", m_com_api.audio_mode);
            }

            val = extras.getString("audio_state", "");
            if (!val.equals(""))
                audio_state_set(val);

            val = extras.getString("audio_digital_amp", "");
            if (!val.equals("")) {
                m_svc_aap.audio_digital_amp_set();//val);
                //com_uti.prefs_set (m_context, "audio_digital_amp", m_com_api.audio_digital_amp);
            }

            val = extras.getString("audio_output", "");
            if (!val.equals("")) {
                if (m_com_api.audio_state.equals("Start"))             // If audio is started...
                    m_svc_aap.audio_output_set(val, true);                         // Set new audio output with restart
                // Save audio output preference     !! Toggle may be converted
                com_uti.prefs_set(m_context, "audio_output", m_com_api.audio_output);
            }

            val = extras.getString("audio_stereo", "");
            if (!val.equals("")) {
                m_svc_aap.audio_stereo_set(val);
                com_uti.prefs_set(m_context, "audio_stereo", val);
            }

            val = extras.getString("audio_record_state", "");
            if (!val.equals(""))
                m_svc_aap.audio_record_state_set(val);

            val = extras.getString("service_update_gui", "");
            service_update_gui = !val.equals("Disable");

            service_update_send();                                             // Update GUI/Widget/Remote/Notification with latest data

        } catch (Throwable e) {
            e.printStackTrace();
        }

        return (start_type);
    }

    private void setBand(String val) {
        Band band = Band.valueOf(val);
        m_Radio.setBand(band);
        com_uti.prefs_set(m_context, "tuner_band", val);
        m_com_api.tuner_band = val;
        Region.setCurrentRegion(band);
    }

    private void displaysUpdate() {

        if (pmgr == null)
            pmgr = (PowerManager) m_context.getSystemService(Context.POWER_SERVICE);

        boolean screen_on = (com_uti.android_version >= 21) ? pmgr.isInteractive() : pmgr.isScreenOn();
        if (!screen_on)
            return;

        if (service_update_gui) {
            Intent service_update_intent = service_update_send();            // Send Intent to send and Update widgets, GUI(s), other components and
            m_com_api.api_service_update(service_update_intent);             // Update our copy of data in Radio API using Intent
        }

    }

    private void tuner_extras_put(Intent intent) {

        intent.putExtra("tuner_state", m_com_api.tuner_state);
        intent.putExtra("tuner_band", m_com_api.tuner_band);

        String freq_khz = m_com_api.tuner_freq;

        int ifreq = com_uti.int_get(freq_khz);

        //!! ifreq = com_uti.tnru_freq_fix (ifreq + 25);

        if (ifreq >= 50000 && ifreq < 500000) {
            m_com_api.tuner_freq = ("" + (double) ifreq / 1000);
            m_com_api.tuner_freq_int = ifreq;
        }
        com_uti.logv("m_com_api.tuner_freq: " + m_com_api.tuner_freq + "  m_com_api.tuner_freq_int: " + m_com_api.tuner_freq_int);
        intent.putExtra("tuner_freq", m_com_api.tuner_freq);


        intent.putExtra("tuner_rssi", m_com_api.tuner_rssi);
        intent.putExtra("tuner_pilot ", m_com_api.tuner_pilot);

        intent.putExtra("tuner_rds_pi", m_com_api.tuner_rds_pi);
        intent.putExtra("tuner_rds_picl", m_com_api.tuner_rds_picl);
        intent.putExtra("tuner_rds_ptyn", m_com_api.tuner_rds_ptyn);
        intent.putExtra("tuner_rds_ps", m_com_api.tuner_rds_ps);
        intent.putExtra("tuner_rds_rt", m_com_api.tuner_rds_rt);
    }

    private Intent service_update_send() {                               // Send all radio state & status info

        m_svc_aap.audio_sessid_get();

        com_uti.logv("audio_state: " + m_com_api.audio_state + "  audio_output: " + m_com_api.audio_output + "  audio_stereo: " + m_com_api.audio_stereo + "  audio_record_state: " + m_com_api.audio_record_state);

        Intent intent = new Intent(com_uti.api_result_id);                 // Create a new broadcast result Intent

        intent.putExtra("chass_presets", m_PresetManager.serializePresets());

        // Send audio data
        intent.putExtra("audio_state", m_com_api.audio_state);
        intent.putExtra("audio_output", m_com_api.audio_output);
        intent.putExtra("audio_stereo", m_com_api.audio_stereo);
        intent.putExtra("audio_record_state", m_com_api.audio_record_state);
        intent.putExtra("audio_sessid", m_com_api.audio_sessid);
        // Send tuner data
        if (m_Radio == null)
            intent.putExtra("tuner_state", "Stop");
        else
            tuner_extras_put(intent);

        m_com_api.service_update_send(intent, "", "");//m_com_api.chass_phase, m_com_api.chass_phtmo); // Send Phase Update + More

        return (intent);
    }

    private void preset_next(boolean up) {
        final List<Preset> presets = m_PresetManager.getPresets();
        int presetIndex = m_PresetManager.getActivePresetIndex();

        for (int index = 0; index < com_api.chass_preset_max; index++) {
            if (up) {
                presetIndex++;
            } else {
                presetIndex--;
            }
            presetIndex = presetIndex % presets.size();
            if (presetIndex < 0) presetIndex += presets.size();

            Preset preset = presets.get(presetIndex);
            if (preset.isValid) {
                tuner_freq_set(preset.freq);
                return;
            }
        }
    }


    // Audio State callback called only by svc_aud: audio_state_set()

    private void audio_state_set(String desired_state) {               // Called only by onStartCommand()
        com_uti.logd("desired_state: " + desired_state);
        if (desired_state.equals("Toggle")) {                    // TOGGLE:
            if (m_com_api.audio_state.equals("Start"))
                desired_state = "Pause";
            else
                desired_state = "Start";
        }
        // If Audio Stop or Pause...
        if (desired_state.equals("Stop") || desired_state.equals("Pause")) {
            m_svc_aap.audio_state_set(desired_state);                        // Set Audio State synchronously
            return;                                   // Return current audio state
        }

        if (!desired_state.equals("Start")) {
            com_uti.loge("Unexpected desired_state: " + desired_state);
            return;                                   // Return current audio state
        }

        // Else if Audio Start desired...
        if (m_com_api.audio_state.equals("Start"))               // If audio already started...
            return;                                   // Return current audio state

        // Else if audio stopped or paused and we want to start audio...
        String mode = com_uti.prefs_get(m_context, "audio_mode", "Digital");
        m_svc_aap.audio_mode_set(mode);                                    // Set audio mode from prefs, before audio is started

        String stereo = com_uti.prefs_get(m_context, "audio_stereo", "Stereo");
        m_svc_aap.audio_stereo_set(stereo);                                // Set audio stereo from prefs, before audio is started

        if (m_com_api.tuner_state.equals("Start")) {             // If tuner started...
            m_svc_aap.audio_state_set("Start");                              // Set Audio State immediately & synchronously
        } else {                                                              // Else if tuner not started...
            need_audio_start_after_tuner_start = true;                        // Signal tuner state callback that audio needs to be started after tuner finishes starting
            tuner_state_set("Start");                                        // Start tuner first, audio will start later via callback
        }

        return;                                     // Return current audio state
    }


    // TUNER:

    public void cb_audio_state(String new_audio_state) {                 // Audio state changed callback from svc_aud
        com_uti.logd("new_audio_state: " + new_audio_state);

        switch (new_audio_state) {
            case "Start":


                if (m_com_api.chass_plug_aud.equals("QCV"))
                    com_uti.ms_sleep(200);

                com_uti.daemon_set("tuner_mute", "Unmute");

                break;
            case "Stop":

                break;
            case "Pause":
                break;
            default:
                com_uti.loge("Unexpected new_audio_state: " + new_audio_state);
                return;
        }

        displaysUpdate();
    }

    private void tuner_state_set(String desired_state) {               // Called only by onStartCommand(), (maybe onDestroy in future)
        com_uti.logd("desired_state: " + desired_state);
        if (desired_state.equals("Toggle")) {                    // If Toggle...
            if (m_com_api.tuner_state.equals("Start"))
                desired_state = "Stop";
            else
                desired_state = "Start";
        }

        if (desired_state.equals("Start")) {                     // If Start...
            tuner_state_start_tmr = new Timer("tuner start", true);          // One shot Poll timer for file creates, SU commands, Bluedroid Init, then start tuner
            tuner_state_start_tmr.schedule(new tuner_state_start_tmr_hndlr(), 10);
            return;
        } else if (desired_state.equals("Stop")) {                 // If Stop...
            m_svc_aap.audio_state_set("Stop");                               // Set Audio State  synchronously to Stop
            m_Radio.setTunerState(TunerState.Stop);
            return;                                   // Return new tuner state
        }

        com_uti.loge("Unexpected desired_state: " + desired_state);
    }

    private void wifi_hack() {
        boolean disable_wifi_hack = false;
        if (disable_wifi_hack)
            return;

        if (com_uti.bt_get())
            return;

        if (com_uti.android_version < VERSION_CODES.LOLLIPOP)
            return;

        if (!m_com_api.chass_plug_aud.equals("LG2"))
            return;

        WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
        if (wifiManager.isWifiEnabled()) {
            com_uti.logd("Wifi IS on");
            wifiManager.setWifiEnabled(false);
            com_uti.logd("wifiManager.isWifiEnabled (): " + wifiManager.isWifiEnabled());
            //      com_uti.ms_sleep (1000);
            //      com_uti.loge ("wifiManager.isWifiEnabled (): " + wifiManager.isWifiEnabled ());
            wifiManager.setWifiEnabled(true);
            com_uti.logd("wifiManager.isWifiEnabled (): " + wifiManager.isWifiEnabled());
        } else {
            com_uti.logd("Wifi not on");
        }
    }

    private void tuner_rds_state_set(String val) {
        m_Radio.setRDSState(RDSState.valueOf(val));
        com_uti.prefs_set(m_context, "tuner_rds_state", val);
    }


    // Other non state machine tuner stuff:

    private void tuner_rds_af_state_set(String val) {
        m_Radio.setAFState(AFState.valueOf(val));
        com_uti.prefs_set(m_context, "tuner_rds_af_state", val);
    }

    private void tuner_prefs_init() {                                    // Load tuner prefs
        String band = com_uti.prefs_get(m_context, "tuner_band", "EU");
        setBand(band);

        String stereo = com_uti.prefs_get(m_context, "tuner_stereo", "Stereo");
        m_Radio.setStereoState(StereoState.valueOf(stereo));

        tuner_rds_state_set(com_uti.prefs_get(m_context, "tuner_rds_state", "Start")); // !! Always rewrites pref
        tuner_rds_af_state_set(com_uti.prefs_get(m_context, "tuner_rds_af_state", "Stop"));  // !! Always rewrites pref

        int freq = com_uti.prefs_get(m_context, "tuner_freq", 88500);
        m_Radio.setFrequency(new Frequency(freq));
    }

    private void tuner_freq_set(String freq) {
        m_Radio.setFrequency(new Frequency(freq));
    }

    /* IRadioListener */
    @Override
    public void onTunerBulk() {
        displaysUpdate();
    }

    @Override
    public void onStateChanged(TunerState state) {
        switch (state) {
            case Start:
                service_update_send();
                foreground_start();
                tuner_prefs_init();
                if (need_audio_start_after_tuner_start) {
                    need_audio_start_after_tuner_start = false;
                    m_svc_aap.audio_state_set("Start");
                }
                wifi_hack();                                                     // For LG G2 CM12 BT/WiFi problem
                break;
            case Stop:
                service_update_send();
                foreground_stop();

                // Optional, but helps clear out old data/problems:
                stopSelf();
                break;
            default:
                com_uti.loge("Unexpected new_tuner_state: " + state.toString());
                break;
        }
    }

    @Override
    public void onFreqChanged(Frequency freq) {
        m_com_api.tuner_stereo = "";

        m_com_api.tuner_rssi = "";//999";                            // ro ... ... Values:   RSSI: 0 - 1000
        m_com_api.tuner_qual = "";//SN 99";                          // ro ... ... Values:   SN 99, SN 30
        m_com_api.tuner_pilot = "";//Mono";                           // ro ... ... Values:   mono, stereo, 1, 2, blend, ... ?      1.5 ?

        m_com_api.tuner_rds_pi = "";//-1";                             // ro ... ... Values:   0 - 65535
        m_com_api.tuner_rds_picl = "";//WKBW";                           // ro ... ... Values:   North American Call Letters or Hex PI for tuner_rds_pi
        m_com_api.tuner_rds_pt = "";//-1";                             // ro ... ... Values:   0 - 31
        m_com_api.tuner_rds_ptyn = "";//";                               // ro ... ... Values:   Describes tuner_rds_pt (English !)
        m_com_api.tuner_rds_ps = "";//SpiritF";                        // ro ... ... Values:   RBDS 8 char info or RDS Station
        m_com_api.tuner_rds_rt = "";//Thanks for Your Support... :)";  // ro ... ... Values:   64 char

        m_com_api.tuner_rds_af = "";//";                               // ro ... ... Values:   Space separated array of AF frequencies
        m_com_api.tuner_rds_ms = "";//";                               // ro ... ... Values:   0 - 65535   M/S Music/Speech switch code
        m_com_api.tuner_rds_ct = "";//";                               // ro ... ... Values:   14 char CT Clock Time & Date

        m_com_api.tuner_rds_tmc = "";//";                               // ro ... ... Values:   Space separated array of shorts
        m_com_api.tuner_rds_tp = "";//";                               // ro ... ... Values:   0 - 65535   TP Traffic Program Identification code
        m_com_api.tuner_rds_ta = "";//";                               // ro ... ... Values:   0 - 65535   TA Traffic Announcement code
        m_com_api.tuner_rds_taf = "";//";                               // ro ... ... Values:   0 - 2^32-1  TAF TA Frequency

        m_com_api.tuner_freq = freq.toMHzString();
        m_com_api.tuner_freq_int = freq.toInt();

        displaysUpdate();

        com_uti.prefs_set(m_context, "tuner_freq", freq.toInt());
    }

    @Override
    public void cb_tuner_rssi(String rssi) {
    }

    @Override
    public void cb_tuner_pilot(String pilot) {
    }

    @Override
    public void cb_tuner_qual(String qual) {
    }

    // RDS:
    @Override
    public void cb_tuner_rds_pi(String pi) {
        displaysUpdate();
    }

    @Override
    public void cb_tuner_rds_pt(String pt) {
        displaysUpdate();
    }

    @Override
    public void cb_tuner_rds_ps(String ps) {
        displaysUpdate();
    }

    @Override
    public void cb_tuner_rds_rt(String rt) {
        displaysUpdate();
    }

    private int files_init() {
        com_uti.logd("starting...");

        //String wav_full_filename = com_uti.res_file_create (m_context, R.raw.s_wav,    "s.wav",         false);             // Not executable
        //String bb1_full_filename = com_uti.res_file_create (m_context, R.raw.b1_bin,     "b1.bin",        false);             // Not executable
        //String bb2_full_filename = com_uti.res_file_create (m_context, R.raw.b2_bin,     "b2.bin",        false);             // Not executable

        String add_full_filename = com_uti.res_file_create(m_context, R.raw.spirit_sh, "99-spirit.sh");
        // Check:
        int ret = 0;

        if (!com_uti.access_get(add_full_filename, false, false, true)) { // rwX
            com_uti.loge("error unexecutable addon.d script 99-spirit.sh");
            ret++;
        }
        // !!!!!! OM7 GPE does not have addon.d so shim is not updated
        if (ret == 0 && com_uti.file_get("/system/addon.d/99-spirit.sh") && com_uti.file_size_get("/system/addon.d/99-spirit.sh") != com_uti.file_size_get("/data/data/fm.a2d.sf/files/99-spirit.sh")) {

            com_uti.logw("Installing new shim files: Turn BT off");

            com_uti.bt_set(false, true);                                   // Bluetooth off, and wait for off
            com_uti.rfkill_bt_wait(false);                                 // Wait for BT off

            String cmd = "";
            cmd += ("mount -o remount,rw /system ; ");
            cmd += ("cp /data/data/fm.a2d.sf/files/99-spirit.sh /system/addon.d/99-spirit.sh ; ");
            cmd += ("chmod 755 /system/addon.d/99-spirit.sh ; ");
            if (com_uti.file_get("/system/lib/libbt-hci.so") && com_uti.file_get("/system/lib/libbt-hcio.so")) {                // Favor old style
                cmd += ("cp /data/data/fm.a2d.sf/lib/libbt-hci.so /system/lib/libbt-hci.so ; ");
                cmd += ("chmod 644 /system/lib/libbt-hci.so ; ");
            } else if (com_uti.file_get("/system/vendor/lib/libbt-vendor.so") && com_uti.file_get("/system/vendor/lib/libbt-vendoro.so")) {
                cmd += ("cp /data/data/fm.a2d.sf/lib/libbt-vendor.so /system/vendor/lib/libbt-vendor.so ; ");
                cmd += ("chmod 644 /system/vendor/lib/libbt-vendor.so ; ");
            }
            cmd += ("mount -o remount,ro /system ; ");
            com_uti.sys_run(cmd, true);
            com_uti.logd("Done Installing new shim files");
        } else if (ret == 0 && (com_uti.file_get(com_uti.platform_orig) || com_uti.platform_file_entirely_ours())) { // Use platform_file_entirely_ours () in case ROM adds later
            String cmd = "";
            cmd += ("mount -o remount,rw /system ; ");
            cmd += ("cp /data/data/fm.a2d.sf/files/99-spirit.sh /system/addon.d/99-spirit.sh ; ");
            cmd += ("chmod 755 /system/addon.d/99-spirit.sh ; ");
            cmd += ("mount -o remount,ro /system ; ");
            com_uti.sys_run(cmd, true);
            com_uti.logd("Done Installing addon.d script for /system/etc/audio_platform_info.xml");
        }

    /*
       if (! com_uti.access_get (bb1_full_filename, true, false, false)) { // Rwx
       com_uti.loge ("error inaccessible bb1 file");
       ret ++;
       }
       if (! com_uti.access_get (bb2_full_filename, true, false, false)) { // Rwx
       com_uti.loge ("error inaccessible bb2 file");
       ret ++;
       }
       */
        com_uti.logd("done ret: " + ret);
        return (ret);
    }


    // Hardware / API dependent part of RadioService:

    private int bcom_bluetooth_init() {                                  // Determine UART or SHIM Mode & Turn BT off if no shim installed
        com_uti.logd("Start");

        m_com_api.tuner_api_mode = "UART";                                  // Default = UART MODE
        if (com_uti.shim_files_possible_get()) {                           // If Bluedroid...
            com_uti.logd("Bluedroid support");

            if (com_uti.shim_files_operational_get() && com_uti.bt_get()) { // If shim files operational, and BT is on...
                com_uti.logd("Bluedroid shim installed & BT on");
                m_com_api.tuner_api_mode = "SHIM";                              // SHIM MODE
            }
        }

        if (m_com_api.tuner_api_mode.equals("UART")) {
            if (com_uti.bt_get()) {
                com_uti.logd("UART mode needed but BT is on; turn BT Off");
                com_uti.bt_set(false, true);                                   // Bluetooth off, and wait for off
                com_uti.rfkill_bt_wait(false);                                 // Wait for BT off
                //com_uti.logd ("Start 4 second delay after BT Off");
                //com_uti.ms_sleep (4000);                                      // Extra 4 second delay to ensure BT is off !!
                //com_uti.logd ("End 4 second delay after BT Off");
            }
        }

        com_uti.logd("done m_com_api.tuner_api_mode: " + m_com_api.tuner_api_mode);
        return (0);
    }

    // Tuner State callback called only by cb_tuner_key ("tuner_state") which is called for tuner_state only by:
    private class tuner_state_start_tmr_hndlr extends TimerTask {

        public void run() {
            int ret = files_init();
            if (ret != 0)
                com_uti.loge("files_init IGNORE Errors: " + ret);

            if (m_com_api.chass_plug_tnr.equals("BCH")) {
                m_com_api.service_update_send(null, "Broadcom Bluetooth Init", "10");// Send Phase Update
                ret = bcom_bluetooth_init();
                if (ret != 0) {
                    m_com_api.service_update_send(null, "ERROR Broadcom Bluetooth Init", "-1");// Send Error Update
                    return;
                }
            }
            com_uti.logd("Starting Tuner...");

            m_Radio.setTunerState(TunerState.Start);

            if (tuner_state_start_tmr != null)
                tuner_state_start_tmr.cancel();                                // Stop one shot poll timer (Not periodic so don't need ?)

            if (m_com_api.tuner_state.equals("Start"))             // If tuner started...
                com_uti.logd("Done Success with m_com_api.tuner_state: " + m_com_api.tuner_state);
            else
                com_uti.loge("Done Error with m_com_api.tuner_state: " + m_com_api.tuner_state);
        }
    }

}

