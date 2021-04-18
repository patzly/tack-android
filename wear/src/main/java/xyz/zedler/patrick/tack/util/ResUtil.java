package xyz.zedler.patrick.tack.util;

import android.content.Context;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ResUtil {

  public static String readFromFile(Context context, String fileName) {
    StringBuilder text = new StringBuilder();
    try {
      InputStream inputStream = context.getAssets().open(fileName + ".txt");
      InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
      for (String line; (line = bufferedReader.readLine()) != null; ) {
        text.append(line).append('\n');
      }
      text.deleteCharAt(text.length() - 1);
      inputStream.close();
    } catch (Exception e) {
      return null;
    }
    return text.toString();
  }
}
