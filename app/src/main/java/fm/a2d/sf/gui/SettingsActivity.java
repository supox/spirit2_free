
package fm.a2d.sf.gui;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.ListPreference;
import android.text.method.DigitsKeyListener;
import android.widget.EditText;

import java.util.Locale;

import fm.a2d.sf.R;
import fm.a2d.sf.com.com_api;
import fm.a2d.sf.com.com_uti;
import fm.a2d.sf.gui.gui_act;

public class SettingsActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {

  private void dig_sum_set (String key, boolean negative, boolean decimal) {
    digits_key_lstnr_set (key, negative, decimal);         // 0-9 PLUS: no negative, no decimal (positive integer)
    summary_set (key);
  }

  private SharedPreferences m_sp = null;

  private Context m_context = null;

  private com_api com_api_get (Context context) {
    if (gui_act.m_com_api == null) {
      gui_act.m_com_api = new com_api (context);                      // !! Operates in same process as gui_act !!
      com_uti.logd("gui_act.m_com_api: " + gui_act.m_com_api);
    }
    if (gui_act.m_com_api == null)
      com_uti.loge ("gui_act.m_com_api == null");
    return (gui_act.m_com_api);
  }

    // Lifecycle:
  @Override
  protected void onCreate (Bundle savedInstanceState) {
    com_uti.logd ("savedInstanceState: " + savedInstanceState);     // Results in huge output with many prefs
    super.onCreate (savedInstanceState);

    android.preference.PreferenceManager manager = getPreferenceManager ();
    manager.setSharedPreferencesName (com_uti.prefs_file);

    addPreferencesFromResource (R.xml.prefs);

    m_context = this;
    m_sp = com_uti.sp_get (m_context);
    com_uti.logd ("m_sp: " + m_sp);
  }

  void digits_key_lstnr_set (String key, boolean sign, boolean decimal) {
    EditTextPreference etp = (EditTextPreference) findPreference (key);                             // Get Preference object for key
    EditText et = (EditText) etp.getEditText ();
    et.setKeyListener (DigitsKeyListener.getInstance (sign, decimal));  // 0-9 PLUS sign, decimal
    return;
  }

  void summary_set (String key) {

    Preference pref = findPreference (key);                             // Get Preference object for key

    if (pref instanceof ListPreference) {                               // If a ListPreference...
      ListPreference lp = (ListPreference) pref;
      CharSequence summ = "";
      try {
        summ = lp.getEntry ();                                          // ArrayIndexOutOfBoundsException
      }
      catch (Throwable e) {
        e.printStackTrace ();
      }
      com_uti.logd ("key: " + key + "  summ: " + summ);

      if (summ == null || summ.equals ("")) {                           // If no selection yet...
          lp.setValueIndex (0);                                         // Use first value as default for all other list preferences (Audio output device, audio output mode etc.)

        try {
          summ = lp.getEntry ();                                        // Get name of entry
        }
        catch (Throwable e) {
          e.printStackTrace ();
        }
        com_uti.logd ("summ: " + summ);
      }
      pref.setSummary (summ);
    }
    else if (pref instanceof EditTextPreference) {
      //EditTextPreference etp = (EditTextPreference) pref;
      String summ;
        summ = m_sp.getString (key, "");                                  // Default default is empty string

      pref.setSummary (summ);                                           // !! ?? Why below and not to right ??
    }

    else if (pref instanceof CheckBoxPreference) {
      CheckBoxPreference cbp = (CheckBoxPreference) pref;
      Boolean summ;
    }

  }


  @Override
  protected void onResume() {
    com_uti.logd ("m_sp: " + m_sp);
    super.onResume ();
    if (m_sp != null)
      m_sp.registerOnSharedPreferenceChangeListener (this);          // Register our change listener
  }
  @Override
  protected void onPause() {
    com_uti.logd ("m_sp: " + m_sp);
    super.onPause ();
    if (m_sp != null)
      m_sp.unregisterOnSharedPreferenceChangeListener (this);        // UnRegister our change listener
  }

  public void onSharedPreferenceChanged (SharedPreferences sp, String key) {
    com_uti.logd ("m_sp: " + m_sp + "  sp: " + sp + "  key: " + key);

    if (com_api_get (m_context) == null)
      return;

    String val = sp.getString (key, "");
    if (val == null || val.equals (""))
      return;

    String lo_key = key.toLowerCase (Locale.getDefault ());

    if (lo_key.startsWith ("gui"))
      com_uti.logd ("ignore gui key");
    else
      gui_act.m_com_api.key_set (key, val);

    return;
  }

}

