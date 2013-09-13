package org.anhonesteffort.polygons;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;
import org.anhonesteffort.polygons.database.DatabaseHelper;
import org.anhonesteffort.polygons.database.model.ActionRecord;
import org.anhonesteffort.polygons.database.model.ZoneRecord;
import org.anhonesteffort.polygons.map.GoogleGeometryFactory;

import java.util.List;

public class ZoneArrayAdapter extends ArrayAdapter<ZoneRecord> {
  private static final String TAG = "org.anhonesteffort.polygons.ZoneArrayAdapter";
  private List<ZoneRecord> items;
  private Context context;

  public ZoneArrayAdapter(Context context, int textViewResourceId, List<ZoneRecord> items) {
    super(context, textViewResourceId, items);
    this.context = context;
    this.items = items;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    ZoneRecord zone = items.get(position);
    DatabaseHelper applicationStorage = DatabaseHelper.getInstance(context);
    int meters;

    LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    convertView = vi.inflate(R.layout.polygon_row_layout, null);

    // Count the number of actionDb set for this polygon.
    int count = 0;
    List<ActionRecord> actions = applicationStorage.actionDb.getActions(zone.getId());
    for(ActionRecord action : actions) {
      if(action.runOnEnter())
        count++;
      if(action.runOnExit())
        count++;
    }

    // Try to determine the distance to this polygon.
    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    if(lastLocation != null) {
      meters = (int) applicationStorage.zoneDb.distanceBetween(
          GoogleGeometryFactory.buildPointRecord(
              new LatLng(
                  lastLocation.getLatitude(),
                  lastLocation.getLongitude()
              )), zone.getId());
    }
    else
      meters = -1;

    // Populate the view.
    TextView polygonLabel = (TextView) convertView.findViewById(R.id.polygon_label);
    TextView polygonDetails = (TextView) convertView.findViewById(R.id.polygon_details);
    polygonLabel.setText(zone.getLabel());
    if(count == 1)
      polygonDetails.setText(count + " action ready at " + meters + "m away.");
    else
      polygonDetails.setText(count + " actionDb ready at " + meters + "m away.");
    if(applicationStorage.zoneDb.isZoneSelected(zone.getId()))
      convertView.setBackgroundResource(R.color.abs__holo_blue_light);
    else
      convertView.setBackgroundResource(0);

    convertView.setTag(R.integer.polygon_id_tag, Integer.valueOf(zone.getId()));
    convertView.setTag(R.integer.polygon_select_tag, Boolean.valueOf(applicationStorage.zoneDb.isZoneSelected(zone.getId())));
    return convertView;
  }

}
