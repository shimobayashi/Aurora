package name.shimobayashi.aurora;

import java.nio.ByteBuffer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class SoundLevelMeter implements Runnable {
	private static final int SAMPLE_RATE = 8000;

	private int bufferSize;
	private AudioRecord audioRecord;
	private boolean isRecording;
	private boolean isPausing;

	public interface SoundLevelMeterListener {
		void onMeasure(double db);
	}

	private SoundLevelMeterListener listener;

	public SoundLevelMeter() {
		bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
				AudioFormat.CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_16BIT);
		audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
				SAMPLE_RATE, AudioFormat.CHANNEL_CONFIGURATION_MONO,
				AudioFormat.ENCODING_PCM_16BIT, bufferSize);
		listener = null;
		isRecording = true;
		pause();
	}

	public void setListener(SoundLevelMeterListener l) {
		listener = l;
	}

	@Override
	public void run() {
		android.os.Process
				.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
		resume();

		short[] buffer = new short[bufferSize];
		while (isRecording) {
			if (!isPausing) {
				int read = audioRecord.read(buffer, 0, bufferSize);
				if (read < 0) {
					throw new IllegalStateException();
				}

				int maxValue = 0;
				for (int i = 0; i < read; i++) {
					maxValue = Math.max(maxValue, buffer[i]);
				}

				double db = 20.0 * Math.log10(maxValue / 12.0);
				Log.d("SoundLevelMeter", "dB:" + maxValue);

				if (listener != null) {
					listener.onMeasure(db);
				}
			}

			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		audioRecord.stop();
		audioRecord.release();
	}

	public void stop() {
		isRecording = false;
	}

	public void pause() {
		if (!isPausing)
			audioRecord.stop();
		isPausing = true;
	}

	public void resume() {
		if (isPausing)
			audioRecord.startRecording();
		isPausing = false;
	}
}
