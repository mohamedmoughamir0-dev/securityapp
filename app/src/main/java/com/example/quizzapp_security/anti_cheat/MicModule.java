package com.example.quizzapp_security.anti_cheat;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

public class MicModule {

    private static final int SAMPLE_RATE = 16000;
    private static final double SPEECH_THRESHOLD = 1200.0;

    public interface OnSpeechListener {
        void onSpeechDetected();
        void onSilence();
    }

    private final OnSpeechListener listener;
    private AudioRecord audioRecord;
    private Thread monitorThread;
    private volatile boolean isRunning = false;

    public MicModule(OnSpeechListener listener) {
        this.listener = listener;
    }

    public void start() {
        if (isRunning) return;
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT) * 2;

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e("MicModule", "AudioRecord non initialisé — état: " + audioRecord.getState());
            return;
        }

        isRunning = true;
        audioRecord.startRecording();
        Log.d("MicModule", "Enregistrement démarré, état: " + audioRecord.getRecordingState());

        monitorThread = new Thread(() -> {
            short[] buffer = new short[bufferSize / 2];
            boolean wasSpeaking = false;
            while (isRunning) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                Log.d("MicModule", "read=" + read);
                if (read > 0) {
                    double rms = calculateRMS(buffer, read);
                    Log.d("MicModule", "RMS = " + (int) rms);
                    if (rms > SPEECH_THRESHOLD) {
                        if (!wasSpeaking) {
                            wasSpeaking = true;
                            listener.onSpeechDetected();
                        }
                    } else {
                        if (wasSpeaking) {
                            wasSpeaking = false;
                            listener.onSilence();
                        }
                    }
                }
            }
        });
        monitorThread.start();
    }

    public void stop() {
        isRunning = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        if (monitorThread != null) {
            monitorThread.interrupt();
            monitorThread = null;
        }
    }

    private double calculateRMS(short[] buffer, int length) {
        long sum = 0;
        for (int i = 0; i < length; i++) {
            sum += (long) buffer[i] * buffer[i];
        }
        return Math.sqrt((double) sum / length);
    }
}
