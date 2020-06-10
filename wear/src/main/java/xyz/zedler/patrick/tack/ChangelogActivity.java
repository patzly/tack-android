package xyz.zedler.patrick.tack;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public class ChangelogActivity extends WearableActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_changelog);

        String file = "changelog";
        String fileLocalized = file + "-" + Locale.getDefault().getLanguage();
        if(readFromFile(fileLocalized) != null) file = fileLocalized;

        ((TextView) findViewById(R.id.text_changelog)).setText(readFromFile(file));
    }

    private String readFromFile(String fileName) {
        StringBuilder text = new StringBuilder();
        try {
            InputStream inputStream = getAssets().open(fileName + ".txt");
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            for(String line; (line = bufferedReader.readLine()) != null;) {
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
