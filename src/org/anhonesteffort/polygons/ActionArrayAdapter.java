package org.anhonesteffort.polygons;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import org.anhonesteffort.polygons.database.DatabaseHelper;
import org.anhonesteffort.polygons.database.model.ActionRecord;

import java.util.List;

public class ActionArrayAdapter extends ArrayAdapter<ActionRecord> {

  private Context context;
  private List<ActionRecord> actionList;

  public ActionArrayAdapter(Context context, int textViewResourceId, List<ActionRecord> actionList) {
    super(context, textViewResourceId, actionList);

    this.context = context;
    this.actionList = actionList;
  }

  private boolean isActionEnabled(ActionRecord action) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);

    if((action.getID() == R.integer.action_super_lock ||
        action.getID() == R.integer.action_super_unlock ||
        action.getID() == R.integer.action_factory_reset) &&
        (settings.getBoolean(PreferencesActivity.PREF_DEVICE_ADMIN, false) == false))
      return false;

    return true;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    ActionRecord action = actionList.get(position);

    LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    convertView = layoutInflater.inflate(R.layout.action_list_row_layout, null);

    if(isActionEnabled(action) == false)
      return new View(context);

    TextView actionName = (TextView) convertView.findViewById(R.id.action_name);
    TextView actionDescription = (TextView) convertView.findViewById(R.id.action_description);
    CheckBox actionEnterCheck = (CheckBox) convertView.findViewById(R.id.action_on_enter);
    CheckBox actionExitCheck = (CheckBox) convertView.findViewById(R.id.action_on_exit);

    if(actionName != null) {
      actionName.setText(action.getName());
      actionDescription.setText(action.getDescription());
      actionEnterCheck.setChecked(action.runOnEnter());
      actionExitCheck.setChecked(action.runOnExit());
      actionEnterCheck.setOnCheckedChangeListener(new onActionChangeListener(action, true));
      actionExitCheck.setOnCheckedChangeListener(new onActionChangeListener(action, false));
    }

    return convertView;
  }

  private class onActionChangeListener implements OnCheckedChangeListener {
    private ActionRecord action;
    private boolean is_enter_check_box;

    public onActionChangeListener(ActionRecord action, boolean is_enter_check_box) {
      this.action = action;
      this.is_enter_check_box = is_enter_check_box;
    }

    @Override
    public void onCheckedChanged(CompoundButton checkBoxView, boolean isChecked) {
      DatabaseHelper applicationStorage = DatabaseHelper.getInstance(context);

      if (checkBoxView.isShown() == false)
        return;

      if (is_enter_check_box)
        action.setRunOnEnter(isChecked);
      else
        action.setRunOnExit(isChecked);

      applicationStorage.actionDatabase.updateActionBroadcast(action);
    }
  }
}
