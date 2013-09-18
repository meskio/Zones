package org.anhonesteffort.polygons;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

/**
 * Programmer: rhodey
 * Date: 9/18/13
 */
public class ActionCursorAdapter extends CursorAdapter {

  private Cursor actionCursor;

  public ActionCursorAdapter(Context context, Cursor actionCursor) {
    super(context, actionCursor);

    this.actionCursor = actionCursor;
  }

  @Override
  public View newView(Context context, Cursor cursor, ViewGroup viewGroup) {
    return null;  //To change body of implemented methods use File | Settings | File Templates.
  }

  @Override
  public void bindView(View view, Context context, Cursor cursor) {
    //To change body of implemented methods use File | Settings | File Templates.
  }
}
