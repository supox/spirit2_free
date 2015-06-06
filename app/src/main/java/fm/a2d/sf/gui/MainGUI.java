
// GUI

package fm.a2d.sf.gui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.audiofx.AudioEffect;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Timer;

import fm.a2d.sf.R;
import fm.a2d.sf.com.com_api;
import fm.a2d.sf.com.com_uti;
import fm.a2d.sf.service.svc_aud;
import fm.a2d.sf.service.svc_tnr;

public class MainGUI implements gui_gap {//, gui_dlg.gui_dlg_lstnr {

    private static final int DAEMON_START_DIALOG = 1;                    // Daemon start
    private static final int DAEMON_ERROR_DIALOG = 2;                    // Daemon error
    private static final int TUNER_API_ERROR_DIALOG = 3;                    // Tuner API error
    private static final int TUNER_ERROR_DIALOG = 4;                    // Tuner error
    private static final int FREQ_SET_DIALOG = 5;                    // Frequency set
    private static final int GUI_MENU_DIALOG = 6;                    // Menu
    private static final int GUI_ABOUT_DIALOG = 7;                    // About
    private static final int GUI_TEST_DIALOG = 8;                    // Test
    private static final int GUI_DEBUG_DIALOG = 9;                    // Debug
    private static final int GUI_SHIM_DIALOG = 10;                    // Shim
    private static final int GUI_ACDB_DIALOG = 11;                    // ACDB Fix
    private static final int PRESET_CHANGE_DIALOG = 12;                    // Preset functions
    private static final int PRESET_RENAME_DIALOG = 14;                    // Preset rename
    private static final int MENU_SET = 0;
    private static final int MENU_EQ = 1;
    private static final int MENU_ABOUT = 2;
    private static int m_obinits = 1;
    private final boolean free = true;
    private final int update_interval = 2;  // 1 is too often
    // Code:
    // Presets: 16 = com_api.chass_preset_max
    private final ImageButton[] m_preset_ib = {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};   // 16 Preset Image Buttons
    private final TextView[] m_preset_tv = {null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null};   // 16 Preset Text Views
    private final long last_rotate_time = 0;
    private final double freq_at_210 = 85200;
    private final double freq_percent_factor = 251.5;
    private final View.OnClickListener preset_select_lstnr = new
            View.OnClickListener() {
                public void onClick(View v) {
                    ani(v);

                    for (int idx = 0; idx < com_api.chass_preset_max; idx++) {
                        if (v == m_preset_ib[idx]) {
                            com_uti.logd("idx: " + idx);

                            preset_go(idx);
                            // TODO
                            /*
                            try {
                                if (m_preset_freq[idx].equals(""))
                                    preset_set(idx);
                                else
                                    preset_go(idx);
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                            */
                            return;
                        }
                    }

                }
            };
    private final boolean new_logs = true;
    private String logfile = "/sdcard/bugreport.txt";
    private Activity m_gui_act = null;
    private Context m_context = null;
    private com_api m_com_api = null;
    // User Interface:
    private Animation m_ani_button = null;
    // Text:
    private TextView m_tv_rssi = null;
    private TextView m_tv_svc_count = null;
    private TextView m_tv_svc_phase = null;
    private TextView m_tv_svc_cdown = null;
    private TextView m_tv_pilot = null;
    private TextView m_tv_band = null;
    private TextView m_tv_freq = null;
    // RDS data:
    private TextView m_tv_picl = null;
    private TextView m_tv_ps = null;
    private TextView m_tv_ptyn = null;
    private TextView m_tv_rt = null;
    // ImageView Buttons:
    private ImageView m_iv_seekup = null;
    private ImageView m_iv_seekdn = null;
    private ImageView m_iv_prev = null;
    private ImageView m_iv_next = null;
    private ImageView m_iv_paupla = null;
    private ImageView m_iv_stop = null;
    private ImageView m_iv_pause = null;
    private ImageView m_iv_output = null;                             // ImageView for Speaker/Headset toggle
    private ImageView m_iv_volume = null;
    private ImageView m_iv_record = null;
    private ImageView m_iv_menu = null;
    private final View.OnClickListener short_click_lstnr = new View.OnClickListener() {
        public void onClick(View v) {

            com_uti.logd("view: " + v);
            ani(v);                                                          // Animate button
            if (v == null) {
                com_uti.loge("view: " + v);
            } else if (v == m_iv_menu)
                m_gui_act.showDialog(GUI_MENU_DIALOG);

            else if (v == m_iv_paupla)
                m_com_api.key_set("audio_state", "Toggle");

            else if (v == m_iv_stop) {
                m_com_api.key_set("tuner_state", "Toggle");
                //if (m_com_api.tuner_state.equals ("Start"))
                //  m_com_api.key_set ("tuner_state", "Stop");                      // Full power down/up
                //else
                //  m_com_api.key_set ("tuner_state", "Start");                     // Tuner only start, no audio
            } else if (v == m_iv_record)
                m_com_api.key_set("audio_record_state", "Toggle");

            else if (v == m_tv_freq)                                          // Frequency direct entry
                m_gui_act.showDialog(FREQ_SET_DIALOG);

            else if (v == m_iv_prev)
                m_com_api.key_set("tuner_freq", "Down");

            else if (v == m_iv_next)
                m_com_api.key_set("tuner_freq", "Up");

            else if (v == m_iv_volume) {
                AudioManager m_am = (AudioManager) m_context.getSystemService(Context.AUDIO_SERVICE);
                if (m_am != null) {
                    int stream_vol = m_am.getStreamVolume(svc_aud.audio_stream);
                    com_uti.logd("stream_vol : " + stream_vol);                  // Set to current volume (no change) to display system volume change dialog for user input on screen
                    m_am.setStreamVolume(svc_aud.audio_stream, stream_vol, AudioManager.FLAG_SHOW_UI);
                }
            } else if (v == m_iv_output) {
                if (m_com_api.audio_output.equals("Speaker"))        // If speaker..., Pressing button goes to headset
                    m_com_api.key_set("audio_output", "Headset");
                else
                    m_com_api.key_set("audio_output", "Speaker");
            } else if (v == m_iv_seekdn) {                                      // Seek down
                if (com_uti.s2_tx_apk())                                       // Transmit navigates presets instead of seeking
                    m_com_api.key_set("service_seek_state", "Down");
                else
                    m_com_api.key_set("tuner_seek_state", "Down");
            } else if (v == m_iv_seekup) {                                      // Seek up
                if (com_uti.s2_tx_apk())                                       // Transmit navigates presets instead of seeking
                    m_com_api.key_set("service_seek_state", "Up");
                else
                    m_com_api.key_set("tuner_seek_state", "Up");
            }

        }
    };
    private ImageView m_iv_pwr = null;
    // Radio Group/Buttons:
    private RadioGroup m_rg_band = null;
    private RadioButton rb_band_eu = null;
    private RadioButton rb_band_us = null;
    private RadioButton rb_band_uu = null;
    // Checkboxes:
    private CheckBox cb_speaker = null;
    private int pixel_width = 480;
    private int pixel_height = 800;
    // Lifecycle API
    private float pixel_density = 1.5f;
    // Dial:
    private android.os.Handler delay_dial_handler = null;
    private Runnable delay_dial_runnable = null;
    private gui_dia m_dial = null;
    private int last_dial_freq = -1;
    // Color:
    private int lite_clr = Color.WHITE;
    private int dark_clr = Color.GRAY;
    // Dialog methods:
    private int blue_clr = Color.BLUE;
    private Dialog daemon_start_dialog = null;
    private Dialog daemon_dialog = null;
    private String last_rt = "";
    private int last_audio_sessid_int = 0;
    private boolean gui_init = false;
    private int svc_count = 0;
    private int svc_cdown = 0;
    private String svc_phase = "";
    private android.os.Handler cdown_timeout_handler = null;
    private Runnable cdown_timeout_runnable = null;
    private int cur_preset_idx = 0;
    private final View.OnLongClickListener preset_change_lstnr = new View.OnLongClickListener() {
        public boolean onLongClick(View v) {
            ani(v);
            com_uti.logd("view: " + v);
            for (int idx = 0; idx < com_api.chass_preset_max; idx++) {
                if (v == m_preset_ib[idx]) {
                    cur_preset_idx = idx;
                    com_uti.logd("idx: " + idx);
                    // TODO
                    /*
                    if (m_preset_freq[idx].equals(""))
                        preset_set(idx);
                    else
                        m_gui_act.showDialog(PRESET_CHANGE_DIALOG);
                        */
                    break;
                }
            }
            return (true);
        }
    };
    private Timer logs_email_tmr = null;
    private final View.OnLongClickListener long_click_lstnr = new
            View.OnLongClickListener() {
                public boolean onLongClick(View v) {
                    com_uti.logd("view: " + v);
                    ani(v);                                                          // Animate button
                    if (v == m_iv_menu) {
                        m_gui_act.startActivity(new Intent(m_context, SettingsActivity.class));// Start Settings activity
                    } else if (v == m_iv_record) {
                        logs_email();
                    } else {
                        com_uti.loge("view: " + v);
                        return (false);                                                 // Don't consume long click
                    }
                    return (true);                                                    // Consume long click
                }
            };

    public MainGUI(Context c, com_api the_com_api) {                     // Constructor
        com_uti.logd("m_obinits: " + m_obinits++);

        m_context = c;
        m_gui_act = (Activity) c;
        m_com_api = the_com_api;
    }

    public void on_pos() {
        com_uti.logd("");
    }

    public void on_neu() {
        com_uti.logd("");
    }

    public void on_neg() {
        com_uti.logd("");
    }


    // Old style hardware Menu button stuff:

    public boolean gap_state_set(String state) {
        boolean ret = false;
        if (state.equals("Start"))
            ret = gui_start();
        else if (state.equals("Stop"))
            ret = gui_stop();
        return (ret);
    }

    private boolean gui_stop() {
        gui_init = false;
        return (true);
    }

    private boolean gui_start() {

        gui_init = false;

        com_uti.strict_mode_set(false);                                      // !! Hack for s2d comms to allow network activity on UI thread

        DisplayMetrics dm = new DisplayMetrics();
        m_gui_act.getWindowManager().getDefaultDisplay().getMetrics(dm);
        pixel_width = dm.widthPixels;
        pixel_height = dm.heightPixels;
        pixel_density = m_context.getResources().getDisplayMetrics().density;
        com_uti.logd("pixel_width: " + pixel_width + "  pixel_height: " + pixel_height + "  pixel_density: " + pixel_density);


        m_gui_act.requestWindowFeature(Window.FEATURE_NO_TITLE);            // No title to save screen space
        m_gui_act.setContentView(R.layout.gui_gui_layout);                   // Main Layout

        LinearLayout.LayoutParams frame_layout_params = new android.widget.LinearLayout.LayoutParams(pixel_width, ViewGroup.LayoutParams.MATCH_PARENT);

        FrameLayout new_fl_view = (FrameLayout) m_gui_act.findViewById(R.id.new_fl);
        new_fl_view.setLayoutParams(frame_layout_params);

        FrameLayout old_fl_view = (FrameLayout) m_gui_act.findViewById(R.id.old_fl);
        old_fl_view.setLayoutParams(frame_layout_params);

        dial_init();                                                       // Initialize frequency disl

        // Set button animation
        m_ani_button = AnimationUtils.loadAnimation(m_context, R.anim.ani_button);

        m_tv_band = (TextView) m_gui_act.findViewById(R.id.tv_band);
        m_tv_pilot = (TextView) m_gui_act.findViewById(R.id.tv_pilot);
        m_tv_rssi = (TextView) m_gui_act.findViewById(R.id.tv_rssi);

        m_tv_svc_count = (TextView) m_gui_act.findViewById(R.id.tv_svc_count);
        m_tv_svc_phase = (TextView) m_gui_act.findViewById(R.id.tv_svc_phase);
        m_tv_svc_cdown = (TextView) m_gui_act.findViewById(R.id.tv_svc_cdown);

        m_tv_picl = (TextView) m_gui_act.findViewById(R.id.tv_picl);
        m_tv_ps = (TextView) m_gui_act.findViewById(R.id.tv_ps);
        m_tv_ptyn = (TextView) m_gui_act.findViewById(R.id.tv_ptyn);
        m_tv_rt = (TextView) m_gui_act.findViewById(R.id.tv_rt);

        m_iv_seekdn = (ImageView) m_gui_act.findViewById(R.id.iv_seekdn);
        m_iv_seekdn.setOnClickListener(short_click_lstnr);

        m_iv_seekup = (ImageView) m_gui_act.findViewById(R.id.iv_seekup);
        m_iv_seekup.setOnClickListener(short_click_lstnr);

        m_iv_prev = (ImageView) m_gui_act.findViewById(R.id.iv_prev);
        m_iv_prev.setOnClickListener(short_click_lstnr);
        m_iv_prev.setId(R.id.iv_prev);

        m_iv_next = (ImageView) m_gui_act.findViewById(R.id.iv_next);
        m_iv_next.setOnClickListener(short_click_lstnr);
        m_iv_next.setId(R.id.iv_next);

        m_tv_freq = (TextView) m_gui_act.findViewById(R.id.tv_freq);
        m_tv_freq.setOnClickListener(short_click_lstnr);

        m_iv_paupla = (ImageView) m_gui_act.findViewById(R.id.iv_paupla);
        m_iv_paupla.setOnClickListener(short_click_lstnr);
        m_iv_paupla.setId(R.id.iv_paupla);

        m_iv_stop = (ImageView) m_gui_act.findViewById(R.id.iv_stop);
        m_iv_stop.setOnClickListener(short_click_lstnr);
        m_iv_stop.setId(R.id.iv_stop);

        m_iv_pause = (ImageView) m_gui_act.findViewById(R.id.iv_pause);
        m_iv_pause.setOnClickListener(short_click_lstnr);
        m_iv_pause.setId(R.id.iv_pause);

        m_iv_output = (ImageView) m_gui_act.findViewById(R.id.iv_output);
        m_iv_output.setOnClickListener(short_click_lstnr);
        m_iv_output.setId(R.id.iv_output);

        m_iv_volume = (ImageView) m_gui_act.findViewById(R.id.iv_volume);
        m_iv_volume.setOnClickListener(short_click_lstnr);
        m_iv_volume.setId(R.id.iv_volume);

        m_iv_record = (ImageView) m_gui_act.findViewById(R.id.iv_record);
        m_iv_record.setOnClickListener(short_click_lstnr);
        //m_iv_record.setOnLongClickListener (long_click_lstnr);
        m_iv_record.setId(R.id.iv_record);

        m_iv_menu = (ImageView) m_gui_act.findViewById(R.id.iv_menu);
        m_iv_menu.setOnClickListener(short_click_lstnr);
        m_iv_menu.setOnLongClickListener(long_click_lstnr);
        m_iv_menu.setId(R.id.iv_menu);

        rb_band_eu = (RadioButton) m_gui_act.findViewById(R.id.rb_band_eu);
        rb_band_us = (RadioButton) m_gui_act.findViewById(R.id.rb_band_us);
        rb_band_uu = (RadioButton) m_gui_act.findViewById(R.id.rb_band_uu);

        cb_speaker = (CheckBox) m_gui_act.findViewById(R.id.cb_speaker);

        try {
            lite_clr = Color.parseColor("#ffffffff");                        // lite like PS
            dark_clr = Color.parseColor("#ffa3a3a3");                        // grey like RT
            blue_clr = Color.parseColor("#ff32b5e5");                        // ICS Blue
        } catch (Throwable e) {
            e.printStackTrace();
        }

        m_tv_rt.setTextColor(lite_clr);
        m_tv_ps.setTextColor(lite_clr);

        presets_setup();

        gui_pwr_update(false);

        long curr_time = com_uti.utc_ms_get();
        long gui_start_first = com_uti.long_get(com_uti.prefs_get(m_context, "gui_start_first", ""));
        if (gui_start_first <= 0L) {
            gui_start_first = curr_time;
            com_uti.prefs_set(m_context, "gui_start_first", "" + curr_time);
        }

        int gui_start_count = com_uti.prefs_get(m_context, "gui_start_count", 0);
        gui_start_count++;
        com_uti.prefs_set(m_context, "gui_start_count", gui_start_count);

        m_rg_band = (RadioGroup) m_gui_act.findViewById(R.id.rg_band);

        // !! tuner_band_set() is now the first thing that starts RadioService, if not already started

        m_com_api.chass_plug_aud = com_uti.chass_plug_aud_get(m_context);  // Setup Audio Plugin
        m_com_api.chass_plug_tnr = com_uti.chass_plug_tnr_get(m_context);  // Setup Tuner Plugin

        if (gui_start_count <= 1) {// If known device and first 1 runs...
            String cc = com_uti.country_get(m_context).toUpperCase();
            if (cc.equals("US") || cc.equals("CA") || cc.equals("MX")) {   // If USA, Canada or Mexico
                com_uti.logd("Setting band US");
                tuner_band_set("US");                                          // Band = US
            } else {
                com_uti.logd("Setting band EU");
                tuner_band_set("EU");                                          // Else Band = EU
            }
        }

        if (!com_uti.file_get("/dev/s2d_running"))                        // If daemon not running
            m_gui_act.showDialog(DAEMON_START_DIALOG);                       // Show the Start dialog

        String band = com_uti.prefs_get(m_context, "tuner_band", "EU");
        //if (! m_com_api.chass_plug_aud.equals ("UNK"))
        tuner_band_set(band);
        switch (m_com_api.tuner_band) {
            case "EU":
                rb_band_eu.setChecked(true);
                rb_band_us.setChecked(false);
                rb_band_uu.setChecked(false);
                break;
            case "US":
                rb_band_eu.setChecked(false);
                rb_band_us.setChecked(true);
                rb_band_uu.setChecked(false);
                break;
            case "UU":
                rb_band_eu.setChecked(false);
                rb_band_us.setChecked(false);
                rb_band_uu.setChecked(true);
                break;
        }

        load_prefs();

        if (!m_com_api.chass_plug_aud.equals("UNK") && !m_com_api.tuner_state.equals("Start"))             // If known device and tuner not started...
            m_com_api.key_set("audio_state", "Start");                       // Start audio service (which starts tuner (and daemon) first, if not already started)

        gui_init = true;

        return (true);
    }
    //private static final int MENU_SHIM    = 2;
    //private static final int MENU_ACDB    = 3;
    //private static final int MENU_DIG     = 5;
    //private static final int MENU_ANA     = 6;
    //private static final int MENU_SLEEP   = 7;
    //private static final int MENU_TEST    = 8;
    //private static final int MENU_DEBUG   = 9;

    private void dial_init() {
        // Dial Relative Layout:
        RelativeLayout freq_dial_relative_layout = (RelativeLayout) m_gui_act.findViewById(R.id.freq_dial);
        android.widget.RelativeLayout.LayoutParams lp_dial = new android.widget.RelativeLayout.LayoutParams(android.widget.RelativeLayout.LayoutParams.MATCH_PARENT, android.widget.RelativeLayout.LayoutParams.MATCH_PARENT);   // WRAP_CONTENT
        //int dial_size = (pixel_width * 3) / 4;
        int dial_size = (pixel_width * 7) / 8;
        m_dial = new gui_dia(m_context, R.drawable.freq_dial_needle, -1, dial_size, dial_size); // Get dial instance/RelativeLayout view
        lp_dial.addRule(RelativeLayout.CENTER_IN_PARENT);
        freq_dial_relative_layout.addView(m_dial, lp_dial);

        // Dial internal Power Relative Layout:
        android.widget.RelativeLayout.LayoutParams lp_power;
        lp_power = new android.widget.RelativeLayout.LayoutParams(android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT, android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp_power.addRule(RelativeLayout.CENTER_IN_PARENT);

        m_iv_pwr = new ImageView(m_context);
        m_iv_pwr.setImageResource(R.drawable.dial_power_off);
        freq_dial_relative_layout.addView(m_iv_pwr, lp_power);

        m_dial.listener_set(new gui_dia.gui_dia_listener() {                      // Setup listener for state_chngd() and dial_chngd()

            private int last_dial_freq = 0;

            public boolean prev_go() {
                if (!m_com_api.tuner_state.equals("Start")) {
                    com_uti.logd("via gui_dia abort tuner_state: " + m_com_api.tuner_state);
                    return (false);                                               // Not Consumed
                }
                ani(m_iv_prev);
                m_com_api.key_set("tuner_freq", "Down");
                return (true);                                                  // Consumed
            }

            public boolean next_go() {
                if (!m_com_api.tuner_state.equals("Start")) {
                    com_uti.logd("via gui_dia abort tuner_state: " + m_com_api.tuner_state);
                    return (false);                                               // Not Consumed
                }
                ani(m_iv_next);
                m_com_api.key_set("tuner_freq", "Up");
                return (true);                                                  // Consumed
            }

            public boolean state_chngd() {
                com_uti.logd("via gui_dia m_com_api.audio_state: " + m_com_api.audio_state);
                if (m_com_api.audio_state.equals("Start"))
                    m_com_api.key_set("tuner_state", "Stop");
                else
                    m_com_api.key_set("audio_state", "Start");
                return (true);                                                  // Consumed
            }

            public boolean freq_go() {
                com_uti.logd("via gui_dia");
                if (!m_com_api.tuner_state.equals("Start")) {
                    com_uti.logd("via gui_dia abort tuner_state: " + m_com_api.tuner_state);
                    return (false);                                               // Not Consumed
                }
                if (last_dial_freq < com_uti.band_freq_lo || last_dial_freq > com_uti.band_freq_hi)
                    return (false);                                               // Not Consumed
                m_com_api.key_set("tuner_freq", "" + last_dial_freq);
                return (true);                                                  // Consumed
            }

            public boolean dial_chngd(double angle) {
                if (!m_com_api.tuner_state.equals("Start")) {
                    com_uti.logd("via gui_dia abort tuner_state: " + m_com_api.tuner_state);
                    return (false);                                               // Not Consumed
                }
                long curr_time = com_uti.tmr_ms_get();
                int freq = dial_freq_get(angle);
                com_uti.logd("via gui_dia angle: " + angle + "  freq: " + freq);
                freq += 25;
                freq /= 50;
                freq *= 50;
                if (freq < com_uti.band_freq_lo || freq > com_uti.band_freq_hi)
                    return (false);                                               // Not Consumed
                freq = com_uti.tnru_freq_enforce(freq);
                com_uti.logd("via gui_dia freq: " + freq + "  curr_time: " + curr_time + "  last_rotate_time: " + last_rotate_time);
                dial_freq_set(freq);   // !! Better to set fast !!
                last_dial_freq = freq;

                if (delay_dial_handler != null) {
                    if (delay_dial_runnable != null)
                        delay_dial_handler.removeCallbacks(delay_dial_runnable);
                } else
                    delay_dial_handler = new android.os.Handler();

                delay_dial_runnable = new Runnable() {
                    public void run() {
                        m_com_api.key_set("tuner_freq", "" + last_dial_freq);
                    }
                };
                delay_dial_handler.postDelayed(delay_dial_runnable, 50);

                return (true);                                                  // Consumed
            }
        });
    }

    private int dial_freq_get(double angle) {
        double percent = (angle + 150) / 3;
        return ((int) (freq_percent_factor * percent + freq_at_210));
    }

    // Root daemon stuff:

    private void dial_freq_set(int freq) {
        if (last_dial_freq == freq)                                         // Optimize
            return;
        if (m_dial == null)
            return;
        last_dial_freq = freq;
        double percent = (freq - freq_at_210) / freq_percent_factor;
        double angle = (percent * 3) - 150;
        m_dial.dial_angle_set(angle);
    }

    private void presets_setup() {
        m_preset_tv[0] = (TextView) m_gui_act.findViewById(R.id.tv_preset_0);
        m_preset_tv[1] = (TextView) m_gui_act.findViewById(R.id.tv_preset_1);
        m_preset_tv[2] = (TextView) m_gui_act.findViewById(R.id.tv_preset_2);
        m_preset_tv[3] = (TextView) m_gui_act.findViewById(R.id.tv_preset_3);
        m_preset_tv[4] = (TextView) m_gui_act.findViewById(R.id.tv_preset_4);
        m_preset_tv[5] = (TextView) m_gui_act.findViewById(R.id.tv_preset_5);
        m_preset_tv[6] = (TextView) m_gui_act.findViewById(R.id.tv_preset_6);
        m_preset_tv[7] = (TextView) m_gui_act.findViewById(R.id.tv_preset_7);
        m_preset_tv[8] = (TextView) m_gui_act.findViewById(R.id.tv_preset_8);
        m_preset_tv[9] = (TextView) m_gui_act.findViewById(R.id.tv_preset_9);
        m_preset_tv[10] = (TextView) m_gui_act.findViewById(R.id.tv_preset_10);
        m_preset_tv[11] = (TextView) m_gui_act.findViewById(R.id.tv_preset_11);
        m_preset_tv[12] = (TextView) m_gui_act.findViewById(R.id.tv_preset_12);
        m_preset_tv[13] = (TextView) m_gui_act.findViewById(R.id.tv_preset_13);
        m_preset_tv[14] = (TextView) m_gui_act.findViewById(R.id.tv_preset_14);
        m_preset_tv[15] = (TextView) m_gui_act.findViewById(R.id.tv_preset_15);

        m_preset_ib[0] = (ImageButton) m_gui_act.findViewById(R.id.ib_preset_0);
        m_preset_ib[1] = (ImageButton) m_gui_act.findViewById(R.id.ib_preset_1);
        m_preset_ib[2] = (ImageButton) m_gui_act.findViewById(R.id.ib_preset_2);
        m_preset_ib[3] = (ImageButton) m_gui_act.findViewById(R.id.ib_preset_3);
        m_preset_ib[4] = (ImageButton) m_gui_act.findViewById(R.id.ib_preset_4);
        m_preset_ib[5] = (ImageButton) m_gui_act.findViewById(R.id.ib_preset_5);
        m_preset_ib[6] = (ImageButton) m_gui_act.findViewById(R.id.ib_preset_6);
        m_preset_ib[7] = (ImageButton) m_gui_act.findViewById(R.id.ib_preset_7);
        m_preset_ib[8] = (ImageButton) m_gui_act.findViewById(R.id.ib_preset_8);
        m_preset_ib[9] = (ImageButton) m_gui_act.findViewById(R.id.ib_preset_9);
        m_preset_ib[10] = (ImageButton) m_gui_act.findViewById(R.id.ib_preset_10);
        m_preset_ib[11] = (ImageButton) m_gui_act.findViewById(R.id.ib_preset_11);
        m_preset_ib[12] = (ImageButton) m_gui_act.findViewById(R.id.ib_preset_12);
        m_preset_ib[13] = (ImageButton) m_gui_act.findViewById(R.id.ib_preset_13);
        m_preset_ib[14] = (ImageButton) m_gui_act.findViewById(R.id.ib_preset_14);
        m_preset_ib[15] = (ImageButton) m_gui_act.findViewById(R.id.ib_preset_15);

        for (int idx = 0; idx < com_api.chass_preset_max; idx++) {
            m_preset_ib[idx].setOnClickListener(preset_select_lstnr);
            m_preset_ib[idx].setOnLongClickListener(preset_change_lstnr);
        }

        // TODO !
      /*
    for (int idx = 0; idx < com_api.chass_preset_max; idx ++) {               // For all presets...
      PresetsManager.Preset preset = m_com_api.Preset.getPreset(idx);
      String name = preset.name;
      String freq = preset.freq;
      if (preset.isValid) {
        m_preset_freq [idx] = freq;
        m_preset_name [idx] = name;

        if (name != null)
          m_preset_tv [idx].setText (name);
        else
          m_preset_tv [idx].setText ("" + ((double) com_uti.int_get (freq)) / 1000);
        m_preset_ib [idx].setImageResource (R.drawable.transparent);
      }
      m_preset_tv [idx].setTextColor (clr);
    }
  */
    }

// m_gui_act.showDialog (DAEMON_ERROR_DIALOG);    // daemon_dialog    daemon_error_dialog_create

    //private int daemon_timeout = 15;                                      // Allow up to 15 seconds for SU prompt to be answered. SuperSU should time itself out by then.

    private void text_default() {
        //m_tv_svc_count.setText ("");
        //m_tv_svc_phase.setText  ("");
        //m_tv_svc_cdown.setText  ("");

        m_tv_pilot.setText("");
        m_tv_band.setText("");

        m_tv_rssi.setText("");
        m_tv_ps.setText("");
        m_tv_picl.setText("");
        m_tv_ptyn.setText("");
        m_tv_rt.setText("");
        m_tv_rt.setSelected(true);                                     // Need setSelected() for marquis
    }

    private void eq_start() {
        int audio_sessid_int = com_uti.int_get(m_com_api.audio_sessid);
        com_uti.logd("audio_sessid: " + m_com_api.audio_sessid + "  audio_sessid_int: " + audio_sessid_int);
        try {                                                               // Not every phone/ROM has EQ installed, especially stock ROMs
            Intent i = new Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL);
            i.putExtra(AudioEffect.EXTRA_AUDIO_SESSION, audio_sessid_int);         //The EXTRA_CONTENT_TYPE extra will help the control panel application customize both the UI layout and the default audio effect settings if none are already stored for the calling application
            m_gui_act.startActivityForResult(i, 0);
        } catch (Throwable e) {
            com_uti.loge("exception");
        }
    }

    public Dialog gap_dialog_create(int id, Bundle args) {               // Create a dialog by calling specific *_dialog_create function    ; Triggered by showDialog (int id);
        //public DialogFragment gap_dialog_create (int id, Bundle args) {
        com_uti.logd("id: " + id + "  args: " + args);
        Dialog ret = null;                                                  // DialogFragment ret = null;
        switch (id) {
            case DAEMON_START_DIALOG:
                ret = daemon_start_dialog_create(id);
                break;
            case DAEMON_ERROR_DIALOG:
                ret = daemon_error_dialog_create(id);
                break;
            case TUNER_API_ERROR_DIALOG:
                ret = tuner_api_error_dialog_create(id);
                break;
            case TUNER_ERROR_DIALOG:
                ret = tuner_error_dialog_create(id);
                break;
            case GUI_MENU_DIALOG:
                ret = gui_menu_dialog_create(id);
                break;
            case GUI_ABOUT_DIALOG:
                ret = gui_about_dialog_create(id);
                break;
            case GUI_TEST_DIALOG:
                ret = gui_test_dialog_create(id);
                break;
            case GUI_DEBUG_DIALOG:
                ret = gui_debug_dialog_create(id);
                break;
            case GUI_SHIM_DIALOG:
                ret = gui_shim_dialog_create(id);
                break;
            case GUI_ACDB_DIALOG:
                ret = gui_acdb_dialog_create(id);
                break;
            case FREQ_SET_DIALOG:
                ret = freq_set_dialog_create(id);
                break;
            case PRESET_CHANGE_DIALOG:
                ret = preset_change_dialog_create(id);
                break;
            case PRESET_RENAME_DIALOG:
                ret = preset_rename_dialog_create(id);
                break;
        }
        //com_uti.logd ("dialog: " + ret);
        return (ret);
    }

    private Dialog gui_test_dialog_create(final int id) {
        com_uti.logd("id: " + id);
        AlertDialog.Builder dlg_bldr = new AlertDialog.Builder(m_context);
        return (dlg_bldr.create());
    }


    // Regional settings:

    private Dialog gui_debug_dialog_create(final int id) {
        com_uti.logd("id: " + id);

        AlertDialog.Builder dlg_bldr = new AlertDialog.Builder(m_context);
        dlg_bldr.setTitle("Debug");
        ArrayList<String> array_list = new ArrayList<>();
        array_list.add("SHIM");
        array_list.add("ACDB");
        array_list.add("Log");
        array_list.add("Digital");
        array_list.add("Analog");
        //array_list.add ("SELinux On");
        //array_list.add ("SELinux Off");

        dlg_bldr.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
            }
        });

        String[] items = new String[array_list.size()];
        array_list.toArray(items);

        dlg_bldr.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                com_uti.logd("item: " + item);
                gui_debug_menu_select(item);
            }

        });

        return (dlg_bldr.create());
    }

    private boolean gui_debug_menu_select(int itemid) {
        int ret = 0;
        com_uti.logd("itemid: " + itemid);                                 // When "Settings" is selected, after pressing Menu key
        switch (itemid) {
            case 0:
                m_gui_act.showDialog(GUI_SHIM_DIALOG);
                return (true);
            case 1:
                m_gui_act.showDialog(GUI_ACDB_DIALOG);
                return (true);
            case 2:
                logs_email();
                return (true);
            case 3:
                m_com_api.key_set("audio_mode", "Digital");
                Toast.makeText(m_context, "audio_mode = Digital", Toast.LENGTH_LONG).show();
                return (true);
            case 4:
                m_com_api.key_set("audio_mode", "Analog");
                Toast.makeText(m_context, "audio_mode = Analog", Toast.LENGTH_LONG).show();
                return (true);
/*
      case 5:
        ret = com_uti.sys_run ("setenforce 1", true);
        Toast.makeText (m_context, "setenforce 1 ret: " + ret, Toast.LENGTH_LONG).show ();
        return (true);
      case 6:
        Toast.makeText (m_context, "DISABLING SELINUX IS BAD FOR SECURITY !!!! ; USE FOR TESTING ONLY !!", Toast.LENGTH_LONG).show ();
        ret = com_uti.sys_run ("setenforce 0", true);
        Toast.makeText (m_context, "setenforce 0 ret: " + ret, Toast.LENGTH_LONG).show ();
        return (true);
*/
        }
        return (false);                                                     // Not consumed ?
    }

    private Dialog gui_about_dialog_create(final int id) {
        com_uti.logd("id: " + id);

        AlertDialog.Builder dlg_bldr = new AlertDialog.Builder(m_context);

        dlg_bldr.setTitle("SpiritF " + com_uti.app_version_get(m_context));

        String menu_msg = "Select Google Play or Cancel";
        if (free)
            menu_msg = "Select Google Play or Cancel";//Select Go Pro or Cancel.";

        dlg_bldr.setMessage(menu_msg);

        dlg_bldr.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        dlg_bldr.setNeutralButton("Debug", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                m_gui_act.showDialog(GUI_DEBUG_DIALOG);                        // Show the Debug dialog
            }
        });

        dlg_bldr.setPositiveButton("Google Play", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                purchase("fm.a2d." + "s2");                                    // Split string to avoid app name change by scripts
            }
        });

        return (dlg_bldr.create());
    }

    private Dialog gui_shim_dialog_create(final int id) {
/*  Code from RadioService:
      boolean unfriendly_auto_install_and_reboot = false;
      if (unfriendly_auto_install_and_reboot) {
        if (! com_uti.shim_files_operational_get ()) {                    // If shim files not operational...
          if (com_uti.bt_get ()) {                                        // July 31, 2014: only install shim if BT is on
            com_uti.bt_set (false, true);                                 // Bluetooth off, and wait for off

            com_uti.rfkill_bt_wait (false);     // Wait for BT off
            //com_uti.logd ("Start 4 second delay after BT Off");
            //com_uti.ms_sleep (4000);                                      // Extra 4 second delay to ensure BT is off !!
            //com_uti.logd ("End 4 second delay after BT Off");

            com_uti.shim_install ();                                      // Install shim

            com_uti.bt_set (true, true);                                  // Bluetooth on, and wait for on  (Need to set BT on so reboot has it on.)

            //Toast.makeText (m_context, "WARM RESTART PENDING FOR SHIM INSTALL !!", Toast.LENGTH_LONG).show ();  java.lang.RuntimeException: Can't create handler inside thread that has not called Looper.prepare()
            // Don't need a delay before reboot because BT is on enough to stay on after reboot
            //com_uti.sys_WAS_run ("kill `pidof system_server`", true);

            com_uti.sys_run ("reboot now", true);                         // M7 GPE requires reboot

            fresh_shim_install = true;
          }
        }
      }//if (unfriendly_auto_install_and_reboot) {
*/
        com_uti.logd("id: " + id);

        AlertDialog.Builder dlg_bldr = new AlertDialog.Builder(m_context);

        dlg_bldr.setTitle("Bluetooth SHIM");

        String menu_msg = "";
        menu_msg += "BT SHIM is only needed when BT is on for HTC One M7, LG G2, Xperia Z2/Z3.\n\n";

        menu_msg += "LG G2 can have audio problems unless SpiritF run once with BT off.\n\n";

        menu_msg += "Select: Install BT Shim to install. Reboot after.\n";
        menu_msg += "Select: Remove BT Shim to remove.";
        dlg_bldr.setMessage(menu_msg);

        dlg_bldr.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        dlg_bldr.setNeutralButton("Install", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (com_uti.shim_files_operational_get()) {
                    boolean reinstall_destroys_original = true;
                    if (reinstall_destroys_original) {
                        Toast.makeText(m_context, "Shim file already installed. Can't reinstall...", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(m_context, "Shim file already installed. Reinstalling...", Toast.LENGTH_LONG).show();
                        com_uti.shim_install();
                    }
                } else {
                    Toast.makeText(m_context, "Shim file installing...", Toast.LENGTH_LONG).show();
                    com_uti.shim_install();
                }
            }
        });

        dlg_bldr.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (com_uti.shim_files_operational_get()) {
                    Toast.makeText(m_context, "Shim file installed. Removing...", Toast.LENGTH_LONG).show();
                    com_uti.shim_remove();
                } else
                    Toast.makeText(m_context, "Shim file not installed !!!", Toast.LENGTH_LONG).show();
            }
        });

        return (dlg_bldr.create());
    }


    // :

    private Dialog gui_acdb_dialog_create(final int id) {
        com_uti.logd("id: " + id);

        AlertDialog.Builder dlg_bldr = new AlertDialog.Builder(m_context);

        dlg_bldr.setTitle("ACDB Fix");

        String menu_msg = "";
        menu_msg += "ACDB Fix is only needed when sound is bad on LG G3, Xperia Z1/2/3.\n\n";

        menu_msg += "Select: Install to install. Restart SpiritF after 10 seconds.\n";
        menu_msg += "Select: Remove to remove.";
        dlg_bldr.setMessage(menu_msg);

        dlg_bldr.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {     //
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        dlg_bldr.setNeutralButton("Install", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
        /*if (com_uti.acdbfix_files_operational_get ()) {
          boolean reinstall_destroys_original = true;
          if (reinstall_destroys_original) {
            Toast.makeText (m_context, "ACDB Fix already installed. Can't reinstall...", Toast.LENGTH_LONG).show ();
          }
          else {
            Toast.makeText (m_context, "ACDB Fix already installed. Reinstalling...", Toast.LENGTH_LONG).show ();
            com_uti.acdbfix_install (m_context);
          }
        }
        else {*/
                Toast.makeText(m_context, "ACDB Fix installing...", Toast.LENGTH_LONG).show();

                m_com_api.key_set("tuner_state", "Stop");
                com_uti.quiet_ms_sleep(2000);
                com_uti.acdbfix_install(m_context);
                //}
            }
        });

        dlg_bldr.setPositiveButton("Remove", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //if (com_uti.acdbfix_files_operational_get ()) {
                Toast.makeText(m_context, "ACDB Fix installed. Removing...", Toast.LENGTH_LONG).show();

                m_com_api.key_set("tuner_state", "Stop");
                com_uti.quiet_ms_sleep(2000);
                com_uti.acdbfix_remove();
        /*}
        else
          Toast.makeText (m_context, "ACDB Fix not installed !!!", Toast.LENGTH_LONG).show ();*/
            }
        });

        return (dlg_bldr.create());
    }


    // CDown timeout stuff:     See m_com_api.service_update_send()

    private Dialog gui_menu_dialog_create(final int id) {
        com_uti.logd("id: " + id);

        AlertDialog.Builder dlg_bldr = new AlertDialog.Builder(m_context);
        dlg_bldr.setTitle("SpiritF " + com_uti.app_version_get(m_context));
        ArrayList<String> array_list = new ArrayList<>();
        array_list.add("Set");
        if (free)
            array_list.add("Go Pro");
        else
            array_list.add("EQ");
        //array_list.add ("SHIM");
        //array_list.add ("ACDB");
        array_list.add("About");
        //array_list.add ("Digital");
        //array_list.add ("Analog");
        //array_list.add ("Sleep");
        //array_list.add ("Test");
        //array_list.add ("Debug");

        dlg_bldr.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
            }
        });

        String[] items = new String[array_list.size()];
        array_list.toArray(items);

        dlg_bldr.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                com_uti.logd("item: " + item);
                gap_menu_select(item);
            }

        });

        return (dlg_bldr.create());
    }

    public boolean gap_menu_create(Menu menu) {
        com_uti.logd("menu: " + menu);
        try {
            menu.add(Menu.NONE, MENU_SET, Menu.NONE, "Set").setIcon(R.drawable.ic_menu_preferences);
            if (free)
                menu.add(Menu.NONE, MENU_EQ, Menu.NONE, "Go Pro");
            else
                menu.add(Menu.NONE, MENU_EQ, Menu.NONE, "EQ");
            //menu.add (Menu.NONE, MENU_SHIM,   Menu.NONE,         "SHIM");//.setIcon (R.drawable.ic_menu_view);
            //menu.add (Menu.NONE, MENU_ACDB,   Menu.NONE,         "ACDB");//.setIcon (R.drawable.ic_menu_help);
            menu.add(Menu.NONE, MENU_ABOUT, Menu.NONE, "About");//.setIcon (R.drawable.ic_menu_info_details);
            //menu.add (Menu.NONE, MENU_DIG,    Menu.NONE,      "Digital");
            //menu.add (Menu.NONE, MENU_ANA,    Menu.NONE,       "Analog");
            //menu.add (Menu.NONE, MENU_SLEEP,  Menu.NONE,        "Sleep");//.setIcon (R.drawable.ic_lock_power_off);
            //menu.add (Menu.NONE, MENU_TEST,   Menu.NONE,         "Test");//.setIcon (R.drawable.ic_menu_view);
            //menu.add (Menu.NONE, MENU_DEBUG,  Menu.NONE,        "Debug");//.setIcon (R.drawable.ic_menu_help);
        } catch (Throwable e) {
            com_uti.loge("Exception: " + e);
            e.printStackTrace();
        }
        return (true);
    }

    public boolean gap_menu_select(int itemid) {
        com_uti.logd("itemid: " + itemid);                                 // When "Settings" is selected, after pressing Menu key
        switch (itemid) {
            case MENU_SET:
                m_gui_act.startActivity(new Intent(m_context, SettingsActivity.class));// Start Settings activity
                return (true);
            case MENU_EQ:
                if (free)
                    purchase("fm.a2d." + "s2");                                  // Split string to avoid app name change by scripts
                else
                    eq_start();
                return (true);
            case MENU_ABOUT:
                m_gui_act.showDialog(GUI_ABOUT_DIALOG);
                return (true);
      /*case MENU_SHIM:
        m_gui_act.showDialog (GUI_SHIM_DIALOG);
        return (true);
      case MENU_ACDB:
        m_gui_act.showDialog (GUI_ACDB_DIALOG);
        return (true);
      case MENU_DIG:
        m_com_api.key_set ("audio_mode", "Digital");
        return (true);
      case MENU_ANA:
        m_com_api.key_set ("audio_mode", "Analog");
        return (true);*/
            //case MENU_SLEEP:
            //  return (true);
            //case MENU_TEST:
            //  m_gui_act.showDialog (GUI_TEST_DIALOG);
            //  return (true);
      /*case MENU_DEBUG:
        m_gui_act.showDialog (GUI_DEBUG_DIALOG);
        return (true);*/
        }
        return (false);                                                     // Not consumed ?
    }

    private void daemon_start_dialog_dismiss() {
        com_uti.logd("");
//    daemon_timeout_stop ();
        if (daemon_start_dialog != null)
            daemon_start_dialog.dismiss();
        daemon_start_dialog = null;
    }

    private Dialog daemon_start_dialog_create(final int id) {
        //private DialogFragment daemon_start_dialog_create (final int id) {
        com_uti.logd("id: " + id);

        AlertDialog.Builder dlg_bldr = new AlertDialog.Builder(m_context);

        dlg_bldr.setTitle("SpiritF " + com_uti.app_version_get(m_context));

        boolean need_daemon = false;
        String start_msg = "";

        if (!com_uti.su_installed_get()) {
            start_msg += "ERROR: NO SuperUser/SuperSU/Root  SpiritF REQUIRES Root.\n\n";
        } else if (m_com_api.chass_plug_aud.equals("UNK")) {
            start_msg += "ERROR: Unknown Device. SpiritF REQUIRES International GS1/GS2/GS3/Note/Note2, HTC One, LG G2, Xperia Z+/Qualcomm.\n\n";
        } else if (m_com_api.chass_plug_tnr.equals("BCH")) {
            need_daemon = true;
            start_msg += "SpiritF Root Daemon Starting...\n\n" +
                    "HTC One, LG G2 & Sony Z2+ can take 7 seconds and may REBOOT on install.\n\n";
        } else {
            need_daemon = true;
            start_msg += "SpiritF Root Daemon Starting...\n\n";
        }

        dlg_bldr.setMessage(start_msg);

        daemon_start_dialog = dlg_bldr.create();                           // Save so we can dismiss dialog later

        return (daemon_start_dialog);
    }

    private Dialog daemon_error_dialog_create(final int id) {
        com_uti.logd("id: " + id);
        AlertDialog.Builder dlg_bldr = new AlertDialog.Builder(m_context);
        dlg_bldr.setTitle("SpiritF " + com_uti.app_version_get(m_context));
        String daemon_msg = "ERROR: Root Daemon did not start or stop after " + (svc_tnr.service_timeout_daemon_start) + "  seconds.\n\n" +
                "SpiritF REQUIRES Root and did not get it.\n\n" +
                "You need to tap System Settings-> About phone-> Build 7 times to enable System Settings-> Developer Options.\n\n" +
                "Then in Developer Options you need to set Root access to Apps or Apps and ADB.\n\n" +
                "Check SuperSU app or System Settings->Superuser and ensure SpiritF is enabled..\n\n";
        dlg_bldr.setMessage(daemon_msg);
        return (dlg_bldr.create());
    }

    private Dialog tuner_api_error_dialog_create(final int id) {
        com_uti.logd("id: " + id);
        AlertDialog.Builder dlg_bldr = new AlertDialog.Builder(m_context);
        dlg_bldr.setTitle("SpiritF " + com_uti.app_version_get(m_context));
        String daemon_msg = "ERROR: Tuner API did not start or stop.\n\n" +
                "This device may need a kernel with a working FM driver.\n\n";
        dlg_bldr.setMessage(daemon_msg);
        return (dlg_bldr.create());
    }

    private Dialog tuner_error_dialog_create(final int id) {
        com_uti.logd("id: " + id);
        AlertDialog.Builder dlg_bldr = new AlertDialog.Builder(m_context);
        dlg_bldr.setTitle("SpiritF " + com_uti.app_version_get(m_context));
        String daemon_msg = "ERROR: Tuner did not start or stop.\n\n" +
                "This device may need a kernel with a working FM driver.\n\n";
        dlg_bldr.setMessage(daemon_msg);
        return (dlg_bldr.create());
    }

    private void purchase(String app_name) {                                     // Purchase app_name: fm.a2d.sf

        try {
            m_gui_act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + app_name)));                  // Market only
            return;   // Done
        } catch (android.content.ActivityNotFoundException e) {
            com_uti.loge("There is no Google Play app installed");
        } catch (Throwable e) {
            e.printStackTrace();
        }

        try {
            //m_gui_act.startActivity (new Intent (Intent.ACTION_VIEW, Uri.parse ("http://market.android.com/details?" + app_name))); // Choose browser or market (Browser only if no market)
            m_gui_act.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + app_name))); // Choose browser or market (Browser only if no market)
        } catch (android.content.ActivityNotFoundException e) {
            com_uti.loge("There is no browser or market app installed");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }


    // API Callback:

    private Dialog freq_set_dialog_create(final int id) {                   // Set new frequency
        com_uti.logd("id: " + id);
        LayoutInflater factory = LayoutInflater.from(m_context);
        final View edit_text_view = factory.inflate(R.layout.edit_number, null);
        AlertDialog.Builder dlg_bldr = new AlertDialog.Builder(m_context);
        dlg_bldr.setTitle("Set Frequency");
        dlg_bldr.setView(edit_text_view);
        dlg_bldr.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {

                EditText edit_text = (EditText) edit_text_view.findViewById(R.id.edit_number);
                CharSequence newFreq = edit_text.getEditableText();
                String nFreq = String.valueOf(newFreq);                        // Get entered text as String
                freq_set(nFreq);
            }
        });
        dlg_bldr.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        return (dlg_bldr.create());
    }


    // UI buttons and other controls:

    // Presets:

    // _PRST
    private Dialog preset_rename_dialog_create(final int id) {                 // Rename preset
        com_uti.logd("id: " + id);
        LayoutInflater factory = LayoutInflater.from(m_context);
        final View edit_text_view = factory.inflate(R.layout.edit_text, null);
        AlertDialog.Builder dlg_bldr = new AlertDialog.Builder(m_context);
        dlg_bldr.setTitle("Preset Rename");
        dlg_bldr.setView(edit_text_view);
        dlg_bldr.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                EditText edit_text = (EditText) edit_text_view.findViewById(R.id.edit_text);
                CharSequence new_name = edit_text.getEditableText();
                String name = String.valueOf(new_name);
                preset_rename(cur_preset_idx, name);
            }
        });
        dlg_bldr.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });
        return (dlg_bldr.create());
    }

    private Dialog preset_change_dialog_create(final int id) {
        com_uti.logd("id: " + id);
        AlertDialog.Builder dlg_bldr = new AlertDialog.Builder(m_context);
        dlg_bldr.setTitle("Preset");
        ArrayList<String> array_list = new ArrayList<>();
        //array_list.add ("Tune");
        array_list.add("Replace");
        array_list.add("Rename");
        array_list.add("Delete");

        dlg_bldr.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
            }
        });
        String[] items = new String[array_list.size()];
        array_list.toArray(items);

        dlg_bldr.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {

                switch (item) {
                    case -1:                                                       // Tune to station
                        preset_go(cur_preset_idx);
                        break;
                    case 0:                                                       // Replace preset with currently tuned station
                        preset_set(cur_preset_idx);
                        break;
                    case 1:                                                       // Rename preset
                        m_gui_act.showDialog(PRESET_RENAME_DIALOG);
                        break;
                    case 2:                                                       // Delete preset   !! Deletes w/ no confirmation
                        preset_delete(cur_preset_idx);
                        break;
                    default:                                                      // Should not happen
                        break;
                }

            }
        });

        return (dlg_bldr.create());
    }

    private void freq_set(String nFreq) {
        if (TextUtils.isEmpty(nFreq)) {                                    // If an empty string...
            com_uti.loge("nFreq: " + nFreq);
            return;
        }
        Float ffreq = 0f;
        try {
            ffreq = Float.valueOf(nFreq);
        } catch (Throwable e) {
            com_uti.loge("ffreq = Float.valueOf (nFreq); failed");
            //e.printStackTrace ();
        }

        // 40.000-399.999
        // 400.00-3999.99
        // 4000.0-39999.9
        // 40000.-399999.

        int f_freq_lo = 40000;
        int f_freq_hi = 399999;

        int freq = (int) (ffreq * 1000);

        if (freq < f_freq_lo || freq > f_freq_hi * 1000) {
            com_uti.loge("1 Frequency invalid ffreq: " + ffreq + "  freq: " + freq);
            return;
        } else if (freq >= f_freq_lo * 1 && freq <= f_freq_hi * 1) {    // For 40 - 399
            freq /= 1;
        } else if (freq >= f_freq_lo * 10 && freq <= f_freq_hi * 10) {    // For 400 - 3999
            freq /= 10;
        } else if (freq >= f_freq_lo * 100 && freq <= f_freq_hi * 100) {    // For 4000 - 39999
            freq /= 100;
        } else if (freq >= f_freq_lo * 1000 && freq <= f_freq_hi * 1000) {    // For 40000 - 399999
            freq /= 1000;
        }

        if (freq < 0) {
            com_uti.loge("2 Frequency invalid ffreq: " + ffreq + "  freq: " + freq);
        } else if (freq <= 40001 && freq >= 39999) {                          // Code 40 = logs_email
            logs_email();
        } else if (freq >= com_uti.band_freq_lo && freq <= com_uti.band_freq_hi) {
            com_uti.logd("Frequency changing to : " + freq + " KHz");
            m_com_api.key_set("tuner_freq", "" + freq);
        } else {
            com_uti.loge("3 Frequency invalid ffreq: " + ffreq + "  freq: " + freq);
        }
    }

    private void gui_pwr_update(boolean pwr) {                            // Enables/disables buttons based on power
        if (pwr) {
            //m_iv_stop.setImageResource (R.drawable.btn_stop);
            m_iv_stop.setImageResource(R.drawable.dial_power_on_250sc);
            m_iv_pwr.setImageResource(R.drawable.dial_power_on);
        } else {
            //m_iv_stop.setImageResource (R.drawable.btn_play);
            m_iv_stop.setImageResource(R.drawable.dial_power_off_250sc);
            m_iv_pwr.setImageResource(R.drawable.dial_power_off);
            text_default();                                                  // Set all displayable text fields to initial OFF defaults
        }

        // Power button is always enabled
        m_iv_record.setEnabled(true);//pwr);      // Leave record button enabled for debug log
        m_iv_seekup.setEnabled(pwr);
        m_iv_seekdn.setEnabled(pwr);
        m_tv_rt.setEnabled(pwr);

        for (int idx = 0; idx < com_api.chass_preset_max; idx++)                // For all presets...
            m_preset_ib[idx].setEnabled(pwr);
    }

    private void cdown_timeout_stop() {
        com_uti.logd("svc_phase: " + svc_phase + "  svc_count: " + svc_count + "  svc_cdown: " + svc_cdown);
        if (cdown_timeout_handler != null) {
            if (cdown_timeout_runnable != null)
                cdown_timeout_handler.removeCallbacks(cdown_timeout_runnable);
        }
        cdown_timeout_handler = null;
        cdown_timeout_runnable = null;

        m_com_api.service_update_send(null, "", "");  // Prevent future detections++

        m_tv_svc_count.setText("");
    }

    private void cdown_timeout_start(String phase, int timeout) {
        svc_count = timeout;
        svc_cdown = timeout;
        svc_phase = phase;
        com_uti.logd("svc_phase: " + svc_phase + "  svc_count: " + svc_count + "  svc_cdown: " + svc_cdown);
        m_tv_svc_count.setText("" + svc_count);
        if (cdown_timeout_handler != null)                                  // If there is already a countdown in progress...
            cdown_timeout_stop();                                            // Stop that countdown

        cdown_timeout_handler = new android.os.Handler();                  // Create Handler to post delayed
        cdown_timeout_runnable = new Runnable() {                          // Create runnable to be called and re-scheduled every update_interval seconds until timeout or success
            public void run() {
                svc_cdown -= update_interval;                                   // Count down
                com_uti.logd("svc_phase: " + svc_phase + "  svc_count: " + svc_count + "  svc_cdown: " + svc_cdown);

                if (svc_cdown > 0) {                                            // If timeout not finished yet... Toast Update
                    Toast.makeText(m_context, "" + svc_cdown + " " + svc_phase, Toast.LENGTH_SHORT).show();
                    m_tv_svc_cdown.setText("" + svc_cdown);                     // Display new countdown
                    if (cdown_timeout_handler != null)                            // Schedule next callback/run in update_interval seconds
                        cdown_timeout_handler.postDelayed(cdown_timeout_runnable, update_interval * 1000);
                } else {                                                          // Else if timeout is finished... handle it w/ error code -777 = timeout
                    cdown_error_handle(-777);
                }
            }
        };
        if (cdown_timeout_handler != null)
            cdown_timeout_handler.postDelayed(cdown_timeout_runnable, update_interval * 1000);
    }

    private void cdown_error_handle(int err) {
        com_uti.logd("svc_phase: " + svc_phase + "  svc_count: " + svc_count + "  svc_cdown: " + svc_cdown);
        cdown_timeout_stop();
        svc_count = err;
        String err_str = svc_phase;
        if (err == -777)
            err_str = "TIMEOUT " + svc_phase;
        else if (!svc_phase.startsWith("ERROR") && !svc_phase.startsWith("TIMEOUT"))
            err_str = "ERROR " + err + " " + svc_phase;

        if (err_str.toLowerCase().contains("daemon")) {
            com_uti.loge("ERROR Daemon /dev/s2d_running: " + com_uti.quiet_file_get("/dev/s2d_running"));
            daemon_start_dialog_dismiss();
            //if (! com_uti.quiet_file_get ("/dev/s2d_running"))
            m_gui_act.showDialog(DAEMON_ERROR_DIALOG);
        } else if (err_str.toLowerCase().contains("tuner api")) {
            com_uti.loge("ERROR Tuner API /dev/s2d_running: " + com_uti.quiet_file_get("/dev/s2d_running"));
            daemon_start_dialog_dismiss();
            m_gui_act.showDialog(TUNER_API_ERROR_DIALOG);
        } else if (err_str.toLowerCase().contains("tuner")) {
            com_uti.loge("ERROR Tuner /dev/s2d_running: " + com_uti.quiet_file_get("/dev/s2d_running"));
            daemon_start_dialog_dismiss();
            m_gui_act.showDialog(TUNER_ERROR_DIALOG);
        } else if (err_str.toLowerCase().contains("bluetooth")) {
            com_uti.loge("ERROR Broadcom Bluetooth /dev/s2d_running: " + com_uti.quiet_file_get("/dev/s2d_running"));
            daemon_start_dialog_dismiss();
        }

        com_uti.loge("err_str: " + err_str);
        Toast.makeText(m_context, err_str, Toast.LENGTH_LONG).show();

        if (m_tv_svc_phase != null) {
            m_tv_svc_phase.setText(err_str);
        } else
            com_uti.loge("m_tv_svc_phase == null  !!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        if (m_tv_svc_count != null)
            m_tv_svc_count.setText("");
        if (m_tv_svc_cdown != null)
            m_tv_svc_cdown.setText("");
    }

    public void gap_service_update(Intent intent) {
        //com_uti.logd ("");

        // If daemon is running and daemon start dialog is showing, dismiss dialog
        if (daemon_start_dialog != null && com_uti.quiet_file_get("/dev/s2d_running")) {
            daemon_start_dialog_dismiss();
        }

        // Power:
        if (m_com_api.tuner_state.equals("Start"))
            gui_pwr_update(true);
        else
            gui_pwr_update(false);


        // Debug / Phase Info:

        m_tv_svc_phase.setText(m_com_api.chass_phase);
        m_tv_svc_cdown.setText(m_com_api.chass_phtmo);

        if (!m_com_api.chass_phtmo.equals("")) {
            com_uti.logd("Intent: " + intent + "  phase: " + m_com_api.chass_phase + "  cdown: " + m_com_api.chass_phtmo);

            if (m_com_api.chass_phtmo.equals("0")) {                       // If Success...
                com_uti.logd("Success m_com_api.chass_phtmo: " + m_com_api.chass_phtmo);
                cdown_timeout_stop();
                //m_com_api.chass_phtmo = "";   // Prevent future detections
            } else {
                int cdown = com_uti.int_get(m_com_api.chass_phtmo);
                if (cdown > 0) {
                    com_uti.logd("cdown: " + cdown);
                    cdown_timeout_start(m_com_api.chass_phase, cdown);
                } else if (cdown == 0) {
                    com_uti.loge("cdown: " + cdown);
                } else if (cdown < 0) {
                    com_uti.logd("cdown: " + cdown);
                    cdown_error_handle(cdown);
                }
            }
        }

        // Audio Session ID:

        int audio_sessid = com_uti.int_get(m_com_api.audio_sessid);
        if (audio_sessid != 0 && last_audio_sessid_int != audio_sessid) {                        // If audio session ID has changed...
            last_audio_sessid_int = audio_sessid;
            com_uti.logd("m_com_api.audio_sessid: " + m_com_api.audio_sessid + "  audio_sessid: " + audio_sessid);
        }

        // Mode Buttons at bottom:

        // Mute/Unmute:
        if (m_com_api.audio_state.equals("Start"))
            m_iv_paupla.setImageResource(R.drawable.sel_pause);
        else
            m_iv_paupla.setImageResource(R.drawable.btn_play);

        // Speaker/Headset:
        if (m_com_api.audio_output.equals("Speaker")) {                                  // Else if speaker..., Pressing button goes to headset
            if (m_iv_output != null)
                m_iv_output.setImageResource(android.R.drawable.stat_sys_headset);//ic_volume_bluetooth_ad2p);
            cb_speaker.setChecked(true);
        } else {                                                              // Pressing button goes to speaker
            if (m_iv_output != null)
                m_iv_output.setImageResource(android.R.drawable.ic_lock_silent_mode_off);
            cb_speaker.setChecked(false);
        }

        // Record Start/Stop:
        if (m_com_api.audio_record_state.equals("Start")) {
            m_iv_record.setImageResource(R.drawable.btn_record_press);
        } else {
            m_iv_record.setImageResource(R.drawable.btn_record);
        }

        // Frequency:
        int ifreq = (int) (com_uti.double_get(m_com_api.tuner_freq) * 1000);
        ifreq = com_uti.tnru_freq_fix(ifreq + 25);                       // Must fix due to floating point rounding need, else 106.1 = 106.099

        String freq = null;
        if (ifreq >= 50000 && ifreq < 500000) {
            dial_freq_set(ifreq);
            freq = ("" + (double) ifreq / 1000);
        }
        if (freq != null) {
            m_tv_freq.setText(freq);
        }

        m_tv_rssi.setText(m_com_api.tuner_rssi);

        switch (m_com_api.tuner_pilot) {
            case "Mono":
                m_tv_pilot.setText("M");
                break;
            case "Stereo":
                m_tv_pilot.setText("S");
                break;
            default:
                m_tv_pilot.setText("");
                break;
        }

        m_tv_band.setText(m_com_api.tuner_band);

        m_tv_picl.setText(m_com_api.tuner_rds_picl);

        m_tv_ps.setText(m_com_api.tuner_rds_ps);

        m_tv_ptyn.setText(m_com_api.tuner_rds_ptyn);

        if (!last_rt.equals(m_com_api.tuner_rds_rt)) {
            last_rt = m_com_api.tuner_rds_rt;
            m_tv_rt.setText(m_com_api.tuner_rds_rt);
            m_tv_rt.setSelected(true);
        }

    }


    // Disabled eye-candy animation:

    private void save_preset(int idx, String name, String freq) {
        // TODO!
        // m_com_api.Presets.savePreset(idx, new com_preset.Preset(name, freq));
    }

    private void preset_delete(int idx) {
        com_uti.logd("idx: " + idx);
        m_preset_tv[idx].setText("");
        m_preset_ib[idx].setImageResource(R.drawable.btn_preset);
        // TODO
        // m_com_api.Presets.savePreset(idx, new com_preset.Preset());
    }

    private void preset_rename(int idx, String name) {
        com_uti.logd("idx: " + idx);
        m_preset_tv[idx].setText(name);
        // Preset preset = m_com_api.Preset.setActivePreset(idx);
        // save_preset(idx, name, preset.freq);
    }

    // Handle GUI clicks for 2nd page settings:

    private void preset_go(int idx) {
        // TODO

        // Preset preset = m_com_api.Preset.setActivePreset(idx);
        // freq_set(preset.freq);
    }


    // Preferences:

    private void preset_set(int idx) {
        if (idx >= com_api.chass_preset_max) {
            com_uti.loge("idx: " + idx + "  com_api.chass_preset_max: " + com_api.chass_preset_max);
            return;
        }

        String freq_text = m_com_api.tuner_freq;
        if (m_com_api.tuner_band.equals("US")) {
            if (!m_com_api.tuner_rds_picl.equals(""))
                freq_text = m_com_api.tuner_rds_picl;
        } else {
            if (!m_com_api.tuner_rds_ps.trim().equals(""))
                freq_text = m_com_api.tuner_rds_ps;
        }

        m_preset_tv[idx].setText("" + freq_text);
        save_preset(idx, freq_text, m_com_api.tuner_freq);
        m_preset_ib[idx].setImageResource(R.drawable.transparent);  // R.drawable.btn_preset
    }

    private void ani(View v) {
        //if (v != null)
        //  v.startAnimation (m_ani_button);
    }

    public void gap_gui_clicked(View view) {                             // See res/layout/gui_pg2_layout.xml & gui_act
        int id = view.getId();
        com_uti.logd("id: " + id + "  view: " + view);
        switch (id) {

            case R.id.cb_test:
                break;

            case R.id.cb_visu:
                if (((CheckBox) view).isChecked())
                    visualizer_state_set("Start");
                else
                    visualizer_state_set("Stop");
                break;

            case R.id.cb_tuner_stereo:
                cb_tuner_stereo(((CheckBox) view).isChecked());
                break;

            case R.id.cb_audio_stereo:
                cb_audio_stereo(((CheckBox) view).isChecked());
                break;

            case R.id.cb_af:
                cb_af(((CheckBox) view).isChecked());
                break;

            case R.id.cb_speaker:
                if (((CheckBox) view).isChecked())
                    m_com_api.key_set("audio_output", "Speaker");
                else
                    m_com_api.key_set("audio_output", "Headset");
                break;

            case R.id.rb_band_eu:
                tuner_band_set("EU");
                rb_log(view, (RadioButton) view, ((RadioButton) view).isChecked());
                break;

            case R.id.rb_band_us:
                tuner_band_set("US");
                rb_log(view, (RadioButton) view, ((RadioButton) view).isChecked());
                break;

            case R.id.rb_band_uu:
                tuner_band_set("UU");
                rb_log(view, (RadioButton) view, ((RadioButton) view).isChecked());
                break;
        }
    }

    private void load_prefs() {
        String value = "";
        value = com_uti.prefs_get(m_context, "audio_output", "");
        if (value.equals("Speaker"))
            cb_speaker.setChecked(true);
        else
            cb_speaker.setChecked(false);

        value = com_uti.prefs_get(m_context, "tuner_stereo", "");
        if (value.equals("Mono"))
            ((CheckBox) m_gui_act.findViewById(R.id.cb_tuner_stereo)).setChecked(false);
        else
            ((CheckBox) m_gui_act.findViewById(R.id.cb_tuner_stereo)).setChecked(true);

        value = com_uti.prefs_get(m_context, "audio_stereo", "");
        if (value.equals("Mono"))
            ((CheckBox) m_gui_act.findViewById(R.id.cb_audio_stereo)).setChecked(false);
        else
            ((CheckBox) m_gui_act.findViewById(R.id.cb_audio_stereo)).setChecked(true);

        ((CheckBox) m_gui_act.findViewById(R.id.cb_visu)).setChecked(false);
    }

    private String visualizer_state_set(String state) {
        com_uti.logd("state: " + state);
        if (state.equals("Start")) {
            m_gui_act.findViewById(R.id.vis).setVisibility(View.VISIBLE);  //dial_init

            m_dial.setVisibility(View.INVISIBLE);
            m_iv_pwr.setVisibility(View.INVISIBLE);
            m_gui_act.findViewById(R.id.frequency_bar).setVisibility(View.INVISIBLE);

            int audio_sessid = com_uti.int_get(m_com_api.audio_sessid);
        } else {
            m_gui_act.findViewById(R.id.vis).setVisibility(View.INVISIBLE);//GONE);

            m_dial.setVisibility(View.VISIBLE);
            m_iv_pwr.setVisibility(View.VISIBLE);
            m_gui_act.findViewById(R.id.frequency_bar).setVisibility(View.VISIBLE);

        }
        return (state);                                                     // No error
    }

    private void tuner_band_set(String band) {
        m_com_api.tuner_band = band;
        com_uti.tnru_band_set(band);                                            // To setup band values; different process than service

        if (!m_com_api.chass_plug_aud.equals("UNK"))
            m_com_api.key_set("tuner_band", band);
    }


    // Radio button logging for test/debug:

    private void cb_tuner_stereo(boolean checked) {
        com_uti.logd("checked: " + checked);
        String val = "Stereo";
        if (!checked)
            val = "Mono";
        m_com_api.key_set("tuner_stereo", val);
    }


    // Debug logs Email:

    private void cb_audio_stereo(boolean checked) {
        com_uti.logd("checked: " + checked);
        String val = "Stereo";
        if (!checked)
            val = "Mono";
        m_com_api.key_set("audio_stereo", val);
    }

    private void cb_af(boolean checked) {
        com_uti.logd("checked: " + checked);
        if (checked)
            m_com_api.key_set("tuner_rds_af_state", "Start");
        else
            m_com_api.key_set("tuner_rds_af_state", "Stop");
    }

    private void rb_log(View view, RadioButton rbt, boolean checked) {
        int btn_id = m_rg_band.getCheckedRadioButtonId();            // get selected radio button from radioGroup
        RadioButton rad_band_btn = (RadioButton) m_gui_act.findViewById(btn_id);            // find the radiobutton by returned id
        com_uti.logd("view: " + view + "  rbt: " + rbt + "  checked: " + checked + "  rad btn text: " + "  btn_id: " + btn_id + "  rad_band_btn: " + rad_band_btn +
                "  text: " + rad_band_btn.getText());
    }

    private String cmd_build(String cmd) {
        String cmd_head = " ; ";
        String cmd_tail = " >> " + logfile;
        return (cmd_head + cmd + cmd_tail);
    }

    private String new_logs_cmd_get() {
        String cmd = "rm " + logfile;

        cmd += cmd_build("cat /data/data/fm.a2d.sf/shared_prefs/s2_prefs.xml");
        cmd += cmd_build("id");
        cmd += cmd_build("uname -a");
        cmd += cmd_build("getprop");
        cmd += cmd_build("ps");
        cmd += cmd_build("lsmod");
        // !! Wildcard * can lengthen command line unexpectedly !!
        cmd += cmd_build("modinfo /system/vendor/lib/modules/* /system/lib/modules/*");
        //cmd += cmd_build ("dumpsys");                                     // 11 seconds on MotoG
        cmd += cmd_build("dumpsys audio");
        cmd += cmd_build("dumpsys media.audio_policy");
        cmd += cmd_build("dumpsys media.audio_flinger");

        cmd += cmd_build("dmesg");

        cmd += cmd_build("logcat -d -v time");

        cmd += cmd_build("ls -lR /data/data/fm.a2d.sf/ /data/data/fm.a2d.sf/lib/ /init* /sbin/ /firmware/ /data/anr/ /data/tombstones/ /dev/ /system/ /sys/");

        return (cmd);
    }

    private boolean file_email(String subject, String filename) {        // See http://stackoverflow.com/questions/2264622/android-multiple-email-attachment-using-intent-question
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("message/rfc822");                                       // Doesn't work well: i.setType ("text/plain");
        i.putExtra(Intent.EXTRA_EMAIL, new String[]{"mikereidis@gmail.com"});
        i.putExtra(Intent.EXTRA_SUBJECT, subject);
        i.putExtra(Intent.EXTRA_TEXT, "Please write write problem, device/model and ROM/version. Please ensure " + filename + " file is actually attached or send manually. Thanks ! Mike.");
        i.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + filename)); // File -> attachment
        try {
            m_gui_act.startActivity(Intent.createChooser(i, "Send email..."));
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(m_context, "No email. Manually send " + filename, Toast.LENGTH_LONG).show();
        }
        //dlg_dismiss (DLG_WAIT);
        return (true);
    }

    private int long_logs_email() {
        String cmd = "bugreport > " + logfile;
        if (new_logs) {
            logfile = "/sdcard/spirit2log.txt";
            com_uti.daemon_set("audio_alsa_log", "1");                       // Log ALSA controls
            cmd = new_logs_cmd_get();
        }
        m_com_api.service_update_send(null, "Writing Debug Log", "20");    // Send Phase Update
        int ret = com_uti.sys_run(cmd, true);                              // Run "bugreport" and output to file
        m_com_api.service_update_send(null, "Sending Debug Log", "20");    // Send Phase Update

        String subject = "SpiritF " + com_uti.app_version_get(m_context);
        boolean bret = file_email(subject, logfile);                       // Email debug log file
        m_com_api.service_update_send(null, "Success Sending Debug Log", "0");// Send Success Update

        return (0);
    }

    private int logs_email() {
        int ret = 0;
        logs_email_tmr = new Timer("Logs Email", true);                    // One shot Poll timer for logs email
        if (logs_email_tmr != null) {
            logs_email_tmr.schedule(new logs_email_tmr_hndlr(), 10);        // Once after 0.01 seconds.
            Toast.makeText(m_context, "Please wait while debug log is collected. Will prompt when done...", Toast.LENGTH_LONG).show();
        }
        return (ret);
    }

    private class logs_email_tmr_hndlr extends java.util.TimerTask {
        public void run() {
            int ret = long_logs_email();
            com_uti.logd("done ret: " + ret);
        }
    }

}
