package org.anhonesteffort.polygons;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CursorAdapter;
import android.widget.TextView;

import org.anhonesteffort.polygons.database.ActionDatabase;
import org.anhonesteffort.polygons.database.DatabaseHelper;
import org.anhonesteffort.polygons.database.model.ActionRecord;

/**
 * Programmer: rhodey
 * Date: 9/18/13
 */
public class ActionCursorAdapter extends CursorAdapter {

  private Context context;

  public ActionCursorAdapter(Context context, Cursor actionCursor) {
    super(context, actionCursor);

    this.context = context;
  }

  @Override
  public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
    LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    ActionDatabase.Reader actionReader = new ActionDatabase.Reader(cursor);
    ActionRecord actionRecord = actionReader.getCurrent();

    if (isActionEnabled(actionRecord))
      return layoutInflater.inflate(R.layout.action_list_row_layout, null);
    else
      return new View(context);
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
  public void bindView(View view, Context context, Cursor cursor) {
    ActionDatabase.Reader actionReader = new ActionDatabase.Reader(cursor);
    ActionRecord actionRecord = actionReader.getCurrent();

    if(isActionEnabled(actionRecord)) {
      TextView actionName = (TextView) view.findViewById(R.id.action_name);
      TextView actionDescription = (TextView) view.findViewById(R.id.action_description);
      CheckBox actionEnterCheck = (CheckBox) view.findViewById(R.id.action_on_enter);
      CheckBox actionExitCheck = (CheckBox) view.findViewById(R.id.action_on_exit);

      actionName.setText(actionRecord.getName());
      actionDescription.setText(actionRecord.getDescription());
      actionEnterCheck.setChecked(actionRecord.runOnEnter());
      actionExitCheck.setChecked(actionRecord.runOnExit());
      actionEnterCheck.setOnCheckedChangeListener(new onActionChangeListener(actionRecord, true));
      actionExitCheck.setOnCheckedChangeListener(new onActionChangeListener(actionRecord, false));
    }
    else
      view.setVisibility(View.GONE);
  }

  private class onActionChangeListener implements CompoundButton.OnCheckedChangeListener {
    private ActionRecord actionRecord;
    private boolean is_enter_check_box;

    public onActionChangeListener(ActionRecord actionRecord, boolean is_enter_check_box) {
      this.actionRecord = actionRecord;
      this.is_enter_check_box = is_enter_check_box;
    }

    @Override
    public void onCheckedChanged(CompoundButton checkBoxView, boolean isChecked) {
      DatabaseHelper applicationStorage = DatabaseHelper.getInstance(context);

      if (checkBoxView.isShown() == false)
        return;

      if (is_enter_check_box)
        actionRecord.setRunOnEnter(isChecked);
      else
        actionRecord.setRunOnExit(isChecked);

      applicationStorage.getActionDatabase().updateZoneAction(actionRecord);
    }
  }

}
