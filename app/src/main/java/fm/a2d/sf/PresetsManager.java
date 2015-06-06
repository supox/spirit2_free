
package fm.a2d.sf;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import fm.a2d.sf.domain.Preset;

public final class PresetsManager {
  private static final String SHARED_PREFS_FILE = "spirit2_presets_file";
  private static final String PRESETS = "PRESETS";
  private final Context m_context;
  private List<Preset> m_presets = new ArrayList<>();
  private int m_currentIndex = 0;
  private int activePresetIndex;

  public PresetsManager(Context context) {
    m_context = context;
    SharedPreferences prefs = m_context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);

    try {
      m_presets = deserializePresets(prefs.getString(PRESETS, "[]"));
    } catch (Exception e) {
    }
  }

  public static String serializePresets(List<Preset> presets) {
    return new Gson().toJson(presets);
  }

  public static List<Preset> deserializePresets(String presets) {
    return Arrays.asList(new Gson().fromJson(presets, Preset[].class));
  }

  public int getActivePresetIndex() {
    return m_currentIndex;
  }

  public void savePreset(int index, Preset preset) {
    while(m_presets.size() <= index) {
      m_presets.add(new Preset());
    }
    m_presets.set(index, preset);

    //save the task list to preference
    SharedPreferences prefs = m_context.getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);
    SharedPreferences.Editor editor = prefs.edit();
    editor.putString(PRESETS, serializePresets());
    editor.commit();
  }

  public Preset getPreset(int index) {
    try {
      return m_presets.get(index);
    } catch (IndexOutOfBoundsException ex) {
      return new Preset();
    }
  }

  public List<Preset> getPresets() {
    return m_presets;
  }

  public String serializePresets() {
    return new Gson().toJson(getPresets());
  }

  public Preset setActivePreset(int index) {
    m_currentIndex = index;
    return getPreset(index);
  }

  public Preset nextPreset(boolean up) {
    int presetIndex = m_currentIndex;
    for(int index = 0; index < m_presets.size(); index++) {
      presetIndex = (presetIndex + (up ? 1 : -1) % m_presets.size());
      if (presetIndex < 0) presetIndex += m_presets.size();
      if (getPreset(presetIndex).isValid) {
        return setActivePreset(presetIndex);
      }
    }
    // fail, probably no presets at all
    return getPreset(m_currentIndex);
  }
}
