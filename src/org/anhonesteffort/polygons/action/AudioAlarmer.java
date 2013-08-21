package org.anhonesteffort.polygons.action;

import org.anhonesteffort.polygons.R;
import org.anhonesteffort.polygons.PreferencesActivity;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;
import android.preference.PreferenceManager;
import android.util.Log;

public class AudioAlarmer extends IntentService {
  private static final String TAG = "org.anhonesteffort.polygons.action.ActionListActivity";
  private SoundPool soundPool;
  private int soundID;
  boolean loaded = false;

  public AudioAlarmer() {
    super(AudioAlarmer.class.getName());
  }

  @Override
  public void onCreate() {
    Log.d(TAG, "onCreate()");
    super.onCreate();

    soundPool = new SoundPool(10, AudioManager.STREAM_ALARM, 0);
    soundPool.setOnLoadCompleteListener(new OnLoadCompleteListener() {
      @Override
      public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
        loaded = true;
        synchronized(AudioAlarmer.this) {
          AudioAlarmer.this.notify();
        }
      }
    });
    soundID = soundPool.load(getBaseContext(), R.raw.laser, 1);
  }

  @Override
  protected void onHandleIntent(Intent intent) {
    Log.d(TAG, "onHandleIntent()");

    synchronized(this) {
      try {
        this.wait(5000);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

    AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
    int volume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM);
    int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);

    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxVolume, 0);
    if(loaded) {
      SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
      soundPool.play(soundID,
                       1,
                       1,
                       1,
                       Integer.parseInt(settings.getString(PreferencesActivity.PREF_AUDIO_ALARM_LENGTH, "30")),
                       1);
    }
    else
      Log.d(TAG, "SoundPool not loaded in time to play audio alarm!");
    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0);
  }
}
