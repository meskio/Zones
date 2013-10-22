package org.anhonesteffort.polygons;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TableRow;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import org.anhonesteffort.polygons.map.TestMapActivity;

public class MainMenuActivity extends SherlockFragmentActivity {

  private static final String TAG = "MainMenuActivity";
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate()");

    setContentView(R.layout.main_menu_layout);
    getSupportActionBar().setTitle(R.string.app_name);
    
    Intent zoneService = new Intent(this, ZoneService.class);
    startService(zoneService);
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume()");

    TableRow zoneMapButton = (TableRow) findViewById(R.id.zone_map_button);
    zoneMapButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), TestMapActivity.class);
        startActivity(intent);
      }
    });

    TableRow zoneListButton = (TableRow) findViewById(R.id.zone_list_button);
    zoneListButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), ZoneListActivity.class);
        startActivity(intent);
      }
    });

    TableRow settingsButton = (TableRow) findViewById(R.id.settings_button);
    settingsButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), PreferencesActivity.class);
        startActivity(intent);
      }
    });
    
    TableRow helpButton = (TableRow) findViewById(R.id.help_button);
    helpButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        Intent intent = new Intent();
        intent.setClass(getApplicationContext(), HelpActivity.class);
        startActivity(intent);
      }
    });
  }
}
