package org.anhonesteffort.polygons;

import org.anhonesteffort.polygons.map.ZoneMapActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TableRow;

import com.actionbarsherlock.app.SherlockFragmentActivity;

public class MainMenuActivity extends SherlockFragmentActivity {
  private static final String TAG = "org.anhonesteffort.zoneDb.MainMenuActivity";
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.d(TAG, "onCreate()");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main_menu_layout);
    getSupportActionBar().setTitle(R.string.app_name);
    
    // Start the background service.
    Intent locationWatchIntent = new Intent(this, ZoneService.class);
    startService(locationWatchIntent);
  }

  @Override
  public void onResume() {
    Log.d(TAG, "onResume()");
    super.onResume();

    TableRow polygonMapButton = (TableRow) findViewById(R.id.polygon_map_row);
    polygonMapButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), ZoneMapActivity.class);
        startActivity(intent);
      }
    });

    TableRow polygonListButton = (TableRow) findViewById(R.id.polygon_list_row);
    polygonListButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), ZoneListActivity.class);
        intent.putExtra(ZoneListActivity.RESTORE_SELECTIONS, false);
        startActivity(intent);
      }
    });

    TableRow smsSettingsButton = (TableRow) findViewById(R.id.settings_row);
    smsSettingsButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), PreferencesActivity.class);
        startActivity(intent);
      }
    });
    
    TableRow helpButton = (TableRow) findViewById(R.id.help_row);
    helpButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), HelpActivity.class);
        startActivity(intent);
      }
    });
  }
}
