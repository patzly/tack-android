package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import com.google.common.primitives.Bytes;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

public class AudioUtil {

  private static final String TAG = AudioUtil.class.getSimpleName();

  private static final int SAMPLE_RATE_IN_HZ = 48000;
  private static final int SILENCE_CHUNK_SIZE = 8000;
  private static final int DATA_CHUNK_SIZE = 8;
  private static final byte[] DATA_MARKER = "data".getBytes(StandardCharsets.US_ASCII);

  private AudioTrack getNewAudioTrack() {
    AudioAttributes audioAttributes = new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
        .build();
    AudioFormat audioFormat = new AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
        .setSampleRate(SAMPLE_RATE_IN_HZ)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build();
    return new AudioTrack(
        audioAttributes,
        audioFormat,
        AudioTrack.getMinBufferSize(
            audioFormat.getSampleRate(), audioFormat.getChannelMask(), audioFormat.getEncoding()
        ),
        AudioTrack.MODE_STREAM,
        AudioManager.AUDIO_SESSION_ID_GENERATE
    );
  }

  private float[] loadAudio(@NonNull Context context, @RawRes int resId) {
    try (InputStream stream = context.getResources().openRawResource(resId)) {
      return readDataFromWavPcmFloat(stream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void writeAudio(AudioTrack track, float[] data, int size) {
    int result = track.write(data, 0, size, AudioTrack.WRITE_BLOCKING);
    if (result < 0) {
      throw new IllegalStateException("Failed to play audio data. Error code: " + result);
    }
  }

  private static float[] readDataFromWavPcmFloat(InputStream input) throws IOException {
    byte[] content;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      content = input.readAllBytes();
    } else {
      content = readInputStreamToBytes(input);
    }
    int indexOfDataMarker = Bytes.indexOf(content, DATA_MARKER);
    if (indexOfDataMarker < 0) {
      throw new RuntimeException("Could not find data marker in the content");
    }
    int startOfSound = indexOfDataMarker + DATA_CHUNK_SIZE;
    if (startOfSound > content.length) {
      throw new RuntimeException("Too short data chunk");
    }
    ByteBuffer byteBuffer = ByteBuffer.wrap(
        content, startOfSound, content.length - startOfSound
    );
    byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
    FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
    float[] data = new float[floatBuffer.remaining()];
    floatBuffer.get(data);
    return data;
  }

  private static byte[] readInputStreamToBytes(InputStream input) throws IOException {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    int read;
    byte[] data = new byte[4096];
    while ((read = input.read(data, 0, data.length)) != -1) {
      buffer.write(data, 0, read);
    }
    return buffer.toByteArray();
  }

  public AudioUtil() {

  }
}
