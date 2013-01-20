package name.shimobayashi.aurora;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.List;

import name.shimobayashi.aurora.SoundLevelMeter.SoundLevelMeterListener;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener,
		SoundLevelMeterListener {
	private static final int REQUEST_CODE = 200; // For call preference activity

	// Sense Tremor
	private SensorManager manager;
	private double[] currentOrientationValues = { 0.0, 0.0, 0.0 };
	private double[] currentAccelerationValues = { 0.0, 0.0, 0.0 };
	private double currentTremor = 0.0;
	private double maxTremor = -1;

	// Sense Sound
	private SoundLevelMeter soundLevelMeter;
	private double maxSoundLevel = -1;

	// Cosm
	private long prevPostTime = 0;
	private int responseCode = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		manager = (SensorManager) getSystemService(SENSOR_SERVICE);
		soundLevelMeter = new SoundLevelMeter();
		soundLevelMeter.setListener(this);
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext());
		double baseValue = Double.valueOf(sp.getString("decibel_base_value", "12"));
		soundLevelMeter.setBaseValue(baseValue);
		(new Thread(soundLevelMeter)).start();
	}

	@Override
	protected void onStop() {
		super.onStop();
		manager.unregisterListener(this);
		soundLevelMeter.pause();
	}

	@Override
	protected void onResume() {
		super.onResume();

		List<Sensor> sensors = manager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (sensors.size() > 0) {
			Sensor s = sensors.get(0);
			manager.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
		}

		soundLevelMeter.resume();
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		soundLevelMeter.stop();
	}

	@Override
	public void onMeasure(double db) {
		// Log.d("SoundLevelMeter", "dB:" + db);
		maxSoundLevel = Math.max(maxSoundLevel, db);

		// On the way, update base value
		Context context = getApplicationContext();
		SharedPreferences sp = PreferenceManager
				.getDefaultSharedPreferences(context);
		double baseValue = Double.valueOf(sp.getString("decibel_base_value",
				"12"));
		soundLevelMeter.setBaseValue(baseValue);
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
			// Update currentTremor
			currentOrientationValues[0] = event.values[0] * 0.1f
					+ currentOrientationValues[0] * (1.0f - 0.1f);
			currentOrientationValues[1] = event.values[1] * 0.1f
					+ currentOrientationValues[1] * (1.0f - 0.1f);
			currentOrientationValues[2] = event.values[2] * 0.1f
					+ currentOrientationValues[2] * (1.0f - 0.1f);

			currentAccelerationValues[0] = event.values[0]
					- currentOrientationValues[0];
			currentAccelerationValues[1] = event.values[1]
					- currentOrientationValues[1];
			currentAccelerationValues[2] = event.values[2]
					- currentOrientationValues[2];

			currentTremor = Math.sqrt(Math.pow(currentAccelerationValues[0], 2)
					+ Math.pow(currentAccelerationValues[1], 2)
					+ Math.pow(currentAccelerationValues[2], 2));
			if (currentTremor > maxTremor)
				maxTremor = currentTremor;

			// PUT to Cosm
			// by the way, PUT and update sound level
			long currentTime = System.currentTimeMillis();
			if (currentTime >= (prevPostTime + 1000 * 10)) { // 10 second past
				// PUT
				responseCode = -1;
				Context context = getApplicationContext();
				SharedPreferences sp = PreferenceManager
						.getDefaultSharedPreferences(context);
				int feedId = Integer.valueOf(sp.getString("cosm_feed_id",
						"89487"));
				URI url = URI.create("http://api.cosm.com/v2/feeds/" + feedId);
				HttpPut request = new HttpPut(url);
				String apiKey = sp.getString("cosm_api_key", "");
				request.addHeader("X-ApiKey", apiKey);
				StringEntity entity;
				try {
					entity = new StringEntity(
							"{\"datastreams\":[{\"id\":\"bed-tremor\",\"current_value\":\""
									+ maxTremor
									+ "\"},{\"id\":\"bed-sound-level\",\"current_value\":\""
									+ maxSoundLevel + "\"}]}", "UTF-8");
					entity.setContentType("application/x-www-form-urlencoded");
					request.setEntity(entity);

					// Do PUT
					DefaultHttpClient httpClient = new DefaultHttpClient();
					HttpResponse response = httpClient.execute(request);
					responseCode = response.getStatusLine().getStatusCode();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				} catch (ClientProtocolException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
				// Update
				TextView t;
				t = (TextView) findViewById(R.id.textViewTremor);
				t.setText("Tremor:" + maxTremor);
				t = (TextView) findViewById(R.id.textViewSoundLevel);
				t.setText("dB:" + maxSoundLevel);
				t = (TextView) findViewById(R.id.textViewResponseCode);
				t.setText("ResponseCode:" + responseCode);
				maxTremor = -1;
				maxSoundLevel = -1;
				prevPostTime = currentTime;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_settings:
			Intent intent = new Intent(this, AuroraPreferenceActivity.class);
			startActivityForResult(intent, REQUEST_CODE);
			return true;
		}
		return false;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

}
