package xyz.zedler.patrick.tack.util;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ResUtil {

    private final static String TAG = ResUtil.class.getSimpleName();
    private final static boolean DEBUG = false;

    @NonNull
    public static String readFromFile(Context context, String file) {
        StringBuilder text = new StringBuilder();
        try {
            InputStream inputStream = context.getAssets().open(file + ".txt");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            for (String line; (line = bufferedReader.readLine()) != null;) {
                text.append(line).append('\n');
            }
            text.deleteCharAt(text.length() - 1);
            inputStream.close();
        } catch (FileNotFoundException e) {
            if (DEBUG) Log.e(TAG, "readFromFile: \"" + file + "\" not found!");
            return "";
        } catch (Exception e) {
            if (DEBUG) Log.e(TAG, "readFromFile: " + e.toString());
            return "";
        }
        return text.toString();
    }

    public static void share(Context context, @StringRes int resId) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, context.getString(resId));
        intent.setType("text/plain");
        context.startActivity(Intent.createChooser(intent, null));
    }
}
