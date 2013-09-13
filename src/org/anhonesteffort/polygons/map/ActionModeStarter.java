package org.anhonesteffort.polygons.map;

import com.actionbarsherlock.view.ActionMode;

// Workaround hackery.
public class ActionModeStarter implements Runnable {
  ZoneMapActivity mainActivity;
  ActionMode.Callback modeCallback;
  
  public ActionModeStarter(ZoneMapActivity mainActivity, ActionMode.Callback modeCallback) {
    this.mainActivity = mainActivity;
    this.modeCallback = modeCallback;
  }
  
  @Override
  public void run() {
    mainActivity.setMode(mainActivity.startActionMode(modeCallback));
  }
  
}
