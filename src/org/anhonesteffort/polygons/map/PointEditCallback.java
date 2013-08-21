package org.anhonesteffort.polygons.map;

import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.geometry.TaggedPoint;
import org.anhonesteffort.polygons.map.PolygonMapActivity.DrawState;

import android.util.Log;

import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public class PointEditCallback implements ActionMode.Callback {
  private static final String TAG = "org.anhonesteffort.polygons.map.PointEditCallback";
  private PolygonMapActivity mapActivity;
  
  public PointEditCallback(PolygonMapActivity mapActivity) {
    this.mapActivity = mapActivity;
  }
  
  private void removeSelectedPoint() {
    Log.d(TAG, "removeSelectedPoint()");
    TaggedPoint removePoint = null;

    for(TaggedPoint point : mapActivity.getSelectedPolygon().getPoints()) {
      if(point.getID() == mapActivity.getSelectedPoint().getID())
        removePoint = point;
    }
    if(removePoint != null)
      mapActivity.getSelectedPolygon().getPoints().remove(removePoint);
    
    mapActivity.updateSelectedPolygon();
    mapActivity.setState(DrawState.EDIT_POLYGON);
  }
  
  @Override
  public boolean onCreateActionMode(ActionMode mode, Menu menu) {
    Log.d(TAG, "onCreateActionMode()");
    MenuInflater inflater = mode.getMenuInflater();
    inflater.inflate(R.menu.edit_point_menu, menu);
    mode.setTitle(mapActivity.getString(R.string.title_edit_point));
    mode.setSubtitle(mapActivity.getSelectedPolygon().getLabel());
    return true;
  }

  @Override
  public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
    mode.setSubtitle(mapActivity.getSelectedPolygon().getLabel());
    return true;
  }

  @Override
  public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
    switch (item.getItemId()) {
      case R.id.delete_point_button:
        removeSelectedPoint();
        return true;
    }
    return false;
  }

  @Override
  public void onDestroyActionMode(ActionMode mode) {
    Log.d(TAG, "onDestroyActionMode()");
    if(mapActivity.getState() != DrawState.NEW_POINTS)
      mapActivity.setState(DrawState.NAVIGATE);
  }
}
