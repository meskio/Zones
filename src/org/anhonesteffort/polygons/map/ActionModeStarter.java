package org.anhonesteffort.polygons.map;

import com.actionbarsherlock.view.ActionMode;

// Workaround hackery.
public class ActionModeStarter implements Runnable {
  PolygonMapActivity mainActivity;
  ActionMode.Callback modeCallback;
  
  public ActionModeStarter(PolygonMapActivity mainActivity, ActionMode.Callback modeCallback) {
    this.mainActivity = mainActivity;
    this.modeCallback = modeCallback;
  }
  
  @Override
  public void run() {
    mainActivity.setMode(mainActivity.startActionMode(modeCallback));
  }
  
}
