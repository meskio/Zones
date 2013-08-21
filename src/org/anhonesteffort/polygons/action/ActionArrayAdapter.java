package org.anhonesteffort.polygons.action;

import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.PreferencesActivity;
import org.anhonesteffort.polygons.storage.ActionBroadcastRecord;
import org.anhonesteffort.polygons.storage.DatabaseHelper;

import java.util.List;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class ActionArrayAdapter extends ArrayAdapter<ActionBroadcastRecord> {
  private static final String TAG = "org.anhonesteffort.polygons.action.ActionArrayAdapter";
  private List<ActionBroadcastRecord> items;
  private Context context;

  public ActionArrayAdapter(Context context, int textViewResourceId, List<ActionBroadcastRecord> items) {
    super(context, textViewResourceId, items);
    this.context = context;
    this.items = items;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    ActionBroadcastRecord action = items.get(position);
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    convertView = vi.inflate(R.layout.action_row_layout, null);

    if(settings.getBoolean(PreferencesActivity.PREF_DEVICE_ADMIN, false) == false &&
        (action.getID() == R.integer.action_super_lock || action.getID() == R.integer.action_super_unlock || action.getID() == R.integer.action_factory_reset))
       return new View(context);

    else if(settings.getBoolean(PreferencesActivity.PREF_EMAIL, false) == false && action.getID() == R.integer.action_email_alert)
      return new View(context);

    TextView actionName = (TextView) convertView.findViewById(R.id.action_name);
    TextView actionDescription = (TextView) convertView.findViewById(R.id.action_description);
    CheckBox enterCheck = (CheckBox) convertView.findViewById(R.id.on_enter);
    CheckBox exitCheck = (CheckBox) convertView.findViewById(R.id.on_exit);

    if(actionName != null) {
      actionName.setText(action.getName());
      actionDescription.setText(action.getDescription());
      enterCheck.setChecked(action.runOnEnter());
      exitCheck.setChecked(action.runOnExit());
      enterCheck.setOnCheckedChangeListener(new onActionChangeListener(action, true, false));
      exitCheck.setOnCheckedChangeListener(new onActionChangeListener(action, false, true));
    }
    else
      Log.e(TAG, "actionName TextView is null, skipping.");
    
    return convertView;
  }

  private class onActionChangeListener implements OnCheckedChangeListener {
    private ActionBroadcastRecord action;
    private boolean enter_check_box;
    private boolean exit_check_box;

    public onActionChangeListener(ActionBroadcastRecord action, boolean enter_check_box, boolean exit_check_box) {
      this.action = action;
      this.enter_check_box = enter_check_box;
      this.exit_check_box = exit_check_box;
    }

    @Override
    public void onCheckedChanged(CompoundButton checkBoxView, boolean isChecked) {
      DatabaseHelper applicationStorage = DatabaseHelper.getInstance(context);

      // Sometimes the ListView is dumb.
      if(checkBoxView.isShown() == false)
        return;

      if(enter_check_box)
        action.setRunOnEnter(isChecked);
      else if(exit_check_box)
        action.setRunOnExit(isChecked);

      applicationStorage.actions.updatePolygonAction(action);
    }
  }
}
