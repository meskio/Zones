package org.anhonesteffort.polygons;

import java.util.List;

import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.geometry.TaggedPoint;
import org.anhonesteffort.polygons.geometry.TaggedPolygon;
import org.anhonesteffort.polygons.map.GoogleGeometryFactory;
import org.anhonesteffort.polygons.storage.ActionBroadcastRecord;
import org.anhonesteffort.polygons.storage.DatabaseHelper;

import com.google.android.gms.maps.model.LatLng;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class PolygonArrayAdapter extends ArrayAdapter<TaggedPolygon<TaggedPoint>> {
  private static final String TAG = "org.anhonesteffort.polygons.PolygonArrayAdapter";
  private List<TaggedPolygon<TaggedPoint>> items;
  private Context context;

  public PolygonArrayAdapter(Context context, int textViewResourceId, List<TaggedPolygon<TaggedPoint>> items) {
    super(context, textViewResourceId, items);
    this.context = context;
    this.items = items;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    TaggedPolygon<TaggedPoint> polygon = items.get(position);
    DatabaseHelper applicationStorage = DatabaseHelper.getInstance(context);
    int meters;

    LayoutInflater vi = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    convertView = vi.inflate(R.layout.polygon_row_layout, null);

    // Count the number of actions set for this polygon.
    int count = 0;
    List<ActionBroadcastRecord> actions = applicationStorage.actions.getPolygonActions(polygon.getID());
    for(ActionBroadcastRecord action : actions) {
      if(action.runOnEnter())
        count++;
      if(action.runOnExit())
        count++;
    }

    // Try to determine the distance to this polygon.
    LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
    if(lastLocation != null) {
      meters = (int) applicationStorage.polygons.getDistanceToPolygon(
          GoogleGeometryFactory.buildTaggedPoint(
              new LatLng(
                  lastLocation.getLatitude(),
                  lastLocation.getLongitude()
                  )), polygon.getID());
    }
    else
      meters = -1;

    // Populate the view.
    TextView polygonLabel = (TextView) convertView.findViewById(R.id.polygon_label);
    TextView polygonDetails = (TextView) convertView.findViewById(R.id.polygon_details);
    polygonLabel.setText(polygon.getLabel());
    if(count == 1)
      polygonDetails.setText(count + " action ready at " + meters + "m away.");
    else
      polygonDetails.setText(count + " actions ready at " + meters + "m away.");
    if(applicationStorage.polygons.isPolygonSelected(polygon.getID()))
      convertView.setBackgroundResource(R.color.abs__holo_blue_light);
    else
      convertView.setBackgroundResource(0);

    convertView.setTag(R.integer.polygon_id_tag, Integer.valueOf(polygon.getID()));
    convertView.setTag(R.integer.polygon_select_tag, Boolean.valueOf(applicationStorage.polygons.isPolygonSelected(polygon.getID())));
    return convertView;
  }

}
