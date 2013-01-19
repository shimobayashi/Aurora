package name.shimobayashi.aurora;

import java.util.List;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.widget.TextView;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

public class MainActivity extends Activity implements SensorEventListener {
	private SensorManager manager;
	private double[] currentOrientationValues = { 0.0, 0.0, 0.0 };
	private double[] currentAccelerationValues = { 0.0, 0.0, 0.0 };
	private double currentTremor = 0.0;
	private double maxTremor = 0.0;
	private long prevPostTime = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		manager = (SensorManager) getSystemService(SENSOR_SERVICE);
	}

	@Override
	protected void onStop() {
		super.onStop();
		manager.unregisterListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		List<Sensor> sensors = manager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if (sensors.size() > 0) {
			Sensor s = sensors.get(0);
			manager.registerListener(this, s, SensorManager.SENSOR_DELAY_NORMAL);
		}
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
			
			currentTremor = Math.sqrt(Math.pow(currentAccelerationValues[0], 2) + Math.pow(currentAccelerationValues[1], 2) + Math.pow(currentAccelerationValues[2], 2));
			if (currentTremor > maxTremor)
				maxTremor = currentTremor;
			
			// POST to Cosm
			long currentTime = System.currentTimeMillis();
			if (currentTime >= (prevPostTime + 1000 * 10)) { // 1 second past
				// POST
				//XXX
				// Update
				TextView t = (TextView) findViewById(R.id.textViewTremor);
				t.setText("Tremor:" + maxTremor);
				maxTremor = -1;
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
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}

}
