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

  private Context context;
  private List<ZoneRecord> zoneList;

  public ZoneArrayAdapter(Context context, int textViewResourceId, List<ZoneRecord> zoneList) {
    super(context, textViewResourceId, zoneList);

    this.context = context;
    this.zoneList = zoneList;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    int meters;

    ZoneRecord zone = zoneList.get(position);
    DatabaseHelper applicationStorage = DatabaseHelper.getInstance(context);

    LayoutInflater layoutInflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    convertView = layoutInflater.inflate(R.layout.zone_list_row_layout, null);

    int count = 0;
    List<ActionRecord> actions = applicationStorage.actionDb.getActions(zone.getId());
    for(ActionRecord action : actions) {
      if(action.runOnEnter())
        count++;
      if(action.runOnExit())
        count++;
    }

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

    TextView zoneLabelView = (TextView) convertView.findViewById(R.id.zone_list_row_label);
    TextView zoneDetailsView = (TextView) convertView.findViewById(R.id.zone_list_row_details);
    zoneLabelView.setText(zone.getLabel());

    if(count == 1)
      zoneDetailsView.setText(count + " action ready at " + meters + "m away.");
    else
      zoneDetailsView.setText(count + " actions ready at " + meters + "m away.");

    if(applicationStorage.zoneDb.isZoneSelected(zone.getId()))
      convertView.setBackgroundResource(R.color.abs__holo_blue_light);
    else
      convertView.setBackgroundResource(0);

    convertView.setTag(R.integer.zone_list_row_id_tag, Integer.valueOf(zone.getId()));
    convertView.setTag(R.integer.zone_list_row_select_tag, Boolean.valueOf(applicationStorage.zoneDb.isZoneSelected(zone.getId())));

    return convertView;
  }

}
