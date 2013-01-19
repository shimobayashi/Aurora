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
	private float[] currentOrientationValues = { 0.0f, 0.0f, 0.0f };
	private float[] currentAccelerationValues = { 0.0f, 0.0f, 0.0f };

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

			TextView t;
			t = (TextView) findViewById(R.id.textViewX);
			t.setText("X:" + currentAccelerationValues[0]);
			t = (TextView) findViewById(R.id.textViewY);
			t.setText("Y:" + currentAccelerationValues[1]);
			t = (TextView) findViewById(R.id.textViewZ);
			t.setText("Z:" + currentAccelerationValues[2]);
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
