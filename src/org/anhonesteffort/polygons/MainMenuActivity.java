package org.anhonesteffort.polygons;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TableRow;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import org.anhonesteffort.polygons.database.DatabaseHelper;
import org.anhonesteffort.polygons.map.ZoneMapActivity;

public class MainMenuActivity extends SherlockFragmentActivity {

  private static final String TAG                  = "MainMenuActivity";
  private static final String ZONES_HAS_RUN_BEFORE = "org.anhonesteffort.polygons.MainMenuActivity.ZONES_HAS_RUN_BEFORE";
  
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Log.d(TAG, "onCreate()");

    setContentView(R.layout.main_menu_layout);
    getSupportActionBar().setTitle(R.string.app_name);
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume()");

    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    if (!settings.getBoolean(ZONES_HAS_RUN_BEFORE, false)) {
      Toast.makeText(MainMenuActivity.this, R.string.toast_initializing_spatial_database, Toast.LENGTH_SHORT).show();
      new InitializeApplicationTask().execute();
    }
    else
      initializeMenu();
  }

  private void initializeMenu() {
    TableRow zoneMapButton = (TableRow) findViewById(R.id.zone_map_button);
    zoneMapButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        Intent intent = new Intent(getApplicationContext(), ZoneMapActivity.class);
        startActivity(intent);
      }
    });

    TableRow zoneListButton = (TableRow) findViewById(R.id.zone_list_button);
    zoneListButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        Intent intent = new Intent(getApplicationContext(), ZoneListActivity.class);
        startActivity(intent);
      }
    });

    TableRow settingsButton = (TableRow) findViewById(R.id.settings_button);
    settingsButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        Intent intent = new Intent(getApplicationContext(), PreferencesActivity.class);
        startActivity(intent);
      }
    });

    TableRow helpButton = (TableRow) findViewById(R.id.help_button);
    helpButton.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        Intent intent = new Intent(getApplicationContext(), HelpActivity.class);
        startActivity(intent);
      }
    });
  }

  private void handleApplicationInitialized() {
    Toast.makeText(MainMenuActivity.this, R.string.toast_spatial_database_initialized, Toast.LENGTH_SHORT).show();
    Intent zoneService = new Intent(this, ZoneService.class);
    startService(zoneService);
    initializeMenu();
  }

  private class InitializeApplicationTask extends AsyncTask<Void, Void, Integer> {

    @Override
    protected Integer doInBackground(Void... params) {
      DatabaseHelper dbHelper = DatabaseHelper.getInstance(MainMenuActivity.this);
      return 0;
    }

    @Override
    protected void onProgressUpdate(Void... progress) {
      // Nothing to do.
    }

    @Override
    protected void onPostExecute(Integer result) {
      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(MainMenuActivity.this);
      settings.edit().putBoolean(ZONES_HAS_RUN_BEFORE, true).commit();
      handleApplicationInitialized();
    }

  }

}
