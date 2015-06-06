
    // GUI API < Activity

package fm.a2d.sf.gui;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

    interface gui_gap {                                              // GUI API definition:

      boolean gap_state_set(String state);

      void gap_service_update(Intent intent);

      void gap_gui_clicked(View v);

      Dialog gap_dialog_create(int id, Bundle args);

      boolean gap_menu_create(Menu menu);

      boolean gap_menu_select(int itemid);

}
