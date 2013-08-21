package org.anhonesteffort.polygons.map;

import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.geometry.TaggedPoint;
import org.anhonesteffort.polygons.geometry.TaggedPolygon;
import org.anhonesteffort.polygons.storage.DatabaseHelper;

import java.util.List;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class PolygonMapActivity extends SherlockFragmentActivity implements LocationListener {
  private static final String TAG                   = "org.anhonesteffort.polygons.map.PolygonMapActivity";
  public final static String SAVED_STATE            = "org.anhonesteffort.polygons.SAVED_STATE";
  public final static String SELECTED_POLYGON       = "org.anhonesteffort.polygons.SELECTED_POLYGON";
  public final static String SELECTED_POLYGON_FOCUS = "org.anhonesteffort.polygons.SELECTED_POLYGON_FOCUS";
  public final static String SELECTED_POINT         = "org.anhonesteffort.polygons.SELECTED_POINT";
  private final static String MAP_TYPE              = "org.anhonesteffort.polygons.MAP_TYPE";
  
  private final static int MAP_UPDATE_INTERVAL_MS   = 10000;
  private final static int MAP_UPDATE_INTERVAL_M    = 1;

  private DatabaseHelper applicationStorage;
  private ActionMode mActionMode;
  private NewPointsCallback newPointsCallback;
  private PointEditCallback pointEditCallback;
  private PolygonEditCallback polygonEditCallback;
  private Vibrator vibrator;
  //private LocationManager locationManager;
  private AlertDialog newPolygonDialog;
  private Bundle instanceState;

  public enum DrawState {NAVIGATE, NEW_LABEL, NEW_POINTS, EDIT_POLYGON, EDIT_POINT};
  private DrawState myState = DrawState.NAVIGATE;
  private TaggedPoint lastViewPoint;
  private double last_zoom_level;

  private TaggedPolygon<TaggedPoint> selectedPolygon;
  private TaggedPoint selectedPoint;
  private GooglePolygonMap polygonMap;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "onCreate()");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.polygon_map_layout);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setSubtitle(R.string.menu_polygon_map);

    applicationStorage = DatabaseHelper.getInstance(getBaseContext());
    polygonMap = new GooglePolygonMap(this);
    
    newPointsCallback = new NewPointsCallback(this);
    pointEditCallback = new PointEditCallback(this);
    polygonEditCallback = new PolygonEditCallback(this, polygonMap);
    
    //locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
    vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

    instanceState = savedInstanceState;
    if(savedInstanceState == null)
      instanceState = getIntent().getExtras();
  }

  @Override
  public void onResume() {
    Log.d(TAG, "onResume()");
    super.onResume();
    
    /*
    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, MAP_UPDATE_INTERVAL_MS, MAP_UPDATE_INTERVAL_M, this);
    Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    if(lastLocation != null)
      this.onLocationChanged(lastLocation);
    */
    
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    if(settings.contains(getString(R.string.key_last_known_location))) {
      String location = settings.getString(getString(R.string.key_last_known_location), "");
      TaggedPoint point = new TaggedPoint(-1,
                                Double.parseDouble(location.split(":")[1].split(",")[0]),
                                Double.parseDouble(location.split(":")[1].split(",")[1]));
      polygonMap.focusOnPoint(point, Double.parseDouble(location.split(":")[0]));
    }
  }

  @Override
  public void onPause() {
    Log.d(TAG, "onPause()");
    super.onPause();
    if(newPolygonDialog != null)
      newPolygonDialog.dismiss();
    
    //locationManager.removeUpdates(this);

    if(lastViewPoint != null) {
      String positionString = Double.toString(last_zoom_level)
          + ":"
          + Double.toString(lastViewPoint.getX())
          + ","
          + Double.toString(lastViewPoint.getY());
      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
      settings.edit().putString(getString(R.string.key_last_known_location), positionString).commit();
    }
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    Log.d(TAG, "onSavedInstanceState()");
    outState.putInt(SAVED_STATE, myState.ordinal());
    outState.putInt(MAP_TYPE, polygonMap.getViewType());
    if(selectedPolygon != null)
      outState.putInt(SELECTED_POLYGON, selectedPolygon.getID());
    if(selectedPoint != null)
      outState.putInt(SELECTED_POINT, selectedPoint.getID());
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getSupportMenuInflater().inflate(R.menu.map_nav_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch(item.getItemId()) {
      case R.id.new_polygon_button:
        setState(DrawState.NEW_LABEL);
        break;
      
      case R.id.switch_map_button:
        polygonMap.changeViewType();
        break;
        
      case android.R.id.home:
        this.finish();
        break;
    }
    return true;
  }

  // Sets the draw state and updates the UI appropriately.
  protected void setState(DrawState newState) {
    Log.d(TAG, "setState(), state: " + newState.name());
    Handler mHandler = new Handler();
    
    switch(newState) {
      case NAVIGATE:
        myState = DrawState.NAVIGATE;
        polygonMap.clearPoints();
        break;

      case NEW_LABEL:
        myState = DrawState.NEW_LABEL;
        showNewPolygonDialog();
        break;

      case NEW_POINTS:
        myState = DrawState.NEW_POINTS;
        mActionMode = this.getSherlock().startActionMode(newPointsCallback);
        
        for(TaggedPoint point : selectedPolygon.getPoints())
          polygonMap.addPoint(point);
        break;

      case EDIT_POLYGON:
        if(myState != DrawState.EDIT_POLYGON) {
          if(mActionMode != null)
            mActionMode.finish();
          mHandler.postDelayed(new ActionModeStarter(this, polygonEditCallback), 20);
        }
        else
          mActionMode.invalidate();
        myState = DrawState.EDIT_POLYGON;
        
        polygonMap.clearPoints();
        selectedPoint = null;
        for(TaggedPoint point : selectedPolygon.getPoints())
          polygonMap.addPoint(point);
        break;
        
      case EDIT_POINT:
        if(myState != DrawState.EDIT_POINT) {
          if(mActionMode != null)
            mActionMode.finish();
          mHandler.postDelayed(new ActionModeStarter(this, pointEditCallback), 20);
        }
        else
          mActionMode.invalidate();
        myState = DrawState.EDIT_POINT;
        
        polygonMap.clearPoints();
        for(TaggedPoint point : selectedPolygon.getPoints())
          polygonMap.addPoint(point);
        polygonMap.selectPoint(selectedPoint);
        break;

        default:
          break;
    }
  }
  
  public DrawState getState() {
    return myState;
  }
  
  public void setMode(ActionMode mode) {
    mActionMode = mode;
  }

  public TaggedPolygon<TaggedPoint> getSelectedPolygon() {
    return selectedPolygon;
  }
  
  public TaggedPoint getSelectedPoint() {
    return selectedPoint;
  }

  public void onMapLoad() {
    Log.d(TAG, "onMapLoad()");
    if(instanceState == null) {
      setState(DrawState.NAVIGATE);
      return;
    }

    if(instanceState.containsKey(MAP_TYPE))
      polygonMap.setViewType(instanceState.getInt(MAP_TYPE));
    if(instanceState.containsKey(SELECTED_POLYGON))
      selectedPolygon = applicationStorage.polygons.getPolygon(instanceState.getInt(SELECTED_POLYGON));
    if(instanceState.containsKey(SELECTED_POLYGON_FOCUS))
      polygonMap.focusOnPolygon(selectedPolygon);
    if(instanceState.containsKey(SELECTED_POINT))
      selectedPoint = applicationStorage.polygons.getPoint(instanceState.getInt(SELECTED_POINT));
    if(instanceState.containsKey(SAVED_STATE))
      setState(DrawState.values()[instanceState.getInt(SAVED_STATE)]);
  }

  public void onMapViewChange(TaggedPoint viewCenter, double view_zoom) {
    lastViewPoint = viewCenter;
    last_zoom_level = view_zoom;
    
    if(myState == DrawState.NEW_POINTS || myState == DrawState.EDIT_POLYGON || myState == DrawState.EDIT_POINT) {
      for(TaggedPoint point : selectedPolygon.getPoints())
        polygonMap.addPoint(point);
      
      if(myState == DrawState.EDIT_POINT)
        polygonMap.selectPoint(selectedPoint);
    }
  }

  public void onMapClick(TaggedPoint clickPoint) {
    Log.d(TAG, "onMapClick()");
    if(myState == DrawState.NEW_POINTS || myState == DrawState.EDIT_POLYGON || myState == DrawState.EDIT_POINT) {
      clickPoint = applicationStorage.polygons.addPoint(clickPoint, selectedPolygon.getID());
      selectedPolygon.getPoints().add(clickPoint);
      polygonMap.addPoint(clickPoint);
    }

    if(myState == DrawState.EDIT_POLYGON || myState == DrawState.EDIT_POINT) {
      updateSelectedPolygon();
      setState(DrawState.EDIT_POLYGON);
    }
  }

  public void onMapLongClick(TaggedPoint clickPoint) { 
    Log.d(TAG, "onMapLongClick()");
    if(myState == DrawState.NAVIGATE || myState == DrawState.EDIT_POLYGON || myState == DrawState.EDIT_POINT) {
      List<TaggedPolygon<TaggedPoint>> polygonList = applicationStorage.polygons.getPolygonsContainingPoint(clickPoint);
      
      if(polygonList.isEmpty() == false) {
        selectedPolygon = polygonList.get(0);
        setState(DrawState.EDIT_POLYGON);
        vibrator.vibrate(50);
      }
      else {
        setState(DrawState.NAVIGATE);
        if(mActionMode != null)
          mActionMode.finish();
      }
    }
  }

  public void onPointClick(TaggedPoint clickPoint) {
    // Nothing to do.
  }
  
  public void onPointMoveStart(TaggedPoint clickPoint) {
    vibrator.vibrate(30);
  }
  
  public void onPointMoveStop(TaggedPoint clickPoint) {
    Log.d(TAG, "onPointMoveStop()");
    selectedPoint = applicationStorage.polygons.updatePoint(clickPoint, selectedPolygon.getID());
    selectedPolygon = applicationStorage.polygons.getPolygon(selectedPolygon.getID());
    
    if(myState == DrawState.EDIT_POLYGON || myState == DrawState.EDIT_POINT) {
      updateSelectedPolygon();
      setState(DrawState.EDIT_POINT);
    }
  }

  // Redraws the selected polygon on map, updates in storage.
  public void updateSelectedPolygon() {
    Log.d(TAG, "updateSelectedPolygon()");
    
    switch(selectedPolygon.makeValid()) {
      case TaggedPolygon.OK:
        int selected_point_index = 0;
        if(selectedPoint != null) {
          for(int i = 0; i < selectedPolygon.getPoints().size(); i++) {
            if(selectedPolygon.getPoints().get(i).getID() == selectedPoint.getID())
              selected_point_index = i;
          }
        }
        
        polygonMap.removePolygon(selectedPolygon.getID());
        selectedPolygon = applicationStorage.polygons.updatePolygon(selectedPolygon);
        selectedPoint = selectedPolygon.getPoints().get(selected_point_index);
        polygonMap.addPolygon(selectedPolygon);
        break;

      case TaggedPolygon.TOO_FEW_POINTS:
        if(myState == DrawState.NEW_POINTS)
          applicationStorage.polygons.removePolygon(selectedPolygon.getID());
        else {
          selectedPolygon = applicationStorage.polygons.getPolygon(selectedPolygon.getID());
          polygonMap.removePolygon(selectedPolygon.getID());
          polygonMap.addPolygon(selectedPolygon);
        }
        Toast.makeText(this, this.getString(R.string.error_too_few_points), Toast.LENGTH_SHORT).show();
        break;

      case TaggedPolygon.TOO_MANY_POINTS:
        if(myState == DrawState.NEW_POINTS) {
          applicationStorage.polygons.removePolygon(selectedPolygon.getID());
        }
        else {
          selectedPolygon.getPoints().remove(selectedPolygon.getPoints().size() - 1);
          selectedPolygon = applicationStorage.polygons.updatePolygon(selectedPolygon);
          polygonMap.removePolygon(selectedPolygon.getID());
          polygonMap.addPolygon(selectedPolygon);
          
          if(mActionMode != null)
            mActionMode.finish();
          setState(DrawState.NAVIGATE);
        }
        Toast.makeText(this, this.getString(R.string.error_too_many_points), Toast.LENGTH_SHORT).show();
        break;

      default:
        polygonMap.removePolygon(selectedPolygon.getID());
        applicationStorage.polygons.removePolygon(selectedPolygon.getID());
        
        setState(DrawState.NAVIGATE);
        if(mActionMode != null)
          mActionMode.finish();
        Toast.makeText(this, this.getString(R.string.error_polygon_not_normal), Toast.LENGTH_SHORT).show();
    }
  }

  private void showNewPolygonDialog() {
    if(newPolygonDialog != null)
      newPolygonDialog.dismiss();
    
    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    LayoutInflater inflater = this.getLayoutInflater();
    final View view = inflater.inflate(R.layout.new_polygon_label_layout, null);

    builder.setView(view).setTitle(R.string.title_polygon_label_dialog);
    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
      public void onClick(DialogInterface dialog, int id) {
        EditText polygonLabelEdit = (EditText) view.findViewById(R.id.polygon_label);
        if(polygonLabelEdit.getText().length() < 1 || 
            applicationStorage.polygons.isPolygonLabelAvailable(polygonLabelEdit.getText().toString()) == false) {
          Toast.makeText(PolygonMapActivity.this, PolygonMapActivity.this.getString(R.string.error_polygon_label), Toast.LENGTH_SHORT).show();
          setState(DrawState.NEW_LABEL);
        }
        else {
          selectedPolygon = applicationStorage.polygons.addPolygon(polygonLabelEdit.getText().toString());
          setState(DrawState.NEW_POINTS);
        }
      }
    });
    builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
      @Override
      public void onCancel(DialogInterface arg0) {
        setState(DrawState.NAVIGATE);
      }
    });
    newPolygonDialog = builder.show();
  }
  
  @Override
  public void onLocationChanged(Location location) {
    polygonMap.onMyLocationChange(location);
  }

  @Override
  public void onProviderDisabled(String provider) {
    // Nothing to do.
  }

  @Override
  public void onProviderEnabled(String provider) {
    // Nothing to do.
  }

  @Override
  public void onStatusChanged(String provider, int status, Bundle extras) {
    // Nothing to do.
  }
}
