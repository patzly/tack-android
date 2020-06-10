package xyz.zedler.patrick.tack;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputLayout;

import java.util.Objects;

import xyz.zedler.patrick.tack.behavior.ScrollBehavior;

public class FeedbackActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		AppCompatDelegate.setDefaultNightMode(
				sharedPrefs.getBoolean("force_dark_mode",false)
						? AppCompatDelegate.MODE_NIGHT_YES
						: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
		);
		setContentView(R.layout.activity_feedback);

		findViewById(R.id.frame_close_feedback).setOnClickListener(v -> finish());

		(new ScrollBehavior()).setUpScroll(
				this,
				R.id.app_bar_feedback,
				R.id.linear_app_bar_feedback,
				R.id.scroll_feedback,
				true
		);

		TextInputLayout textInputLayoutSubject = findViewById(R.id.text_input_subject);
		Objects.requireNonNull(textInputLayoutSubject.getEditText()).setOnFocusChangeListener((View view, boolean hasFocus) -> {
			if(hasFocus) {
				ImageView imageViewSubject = findViewById(R.id.image_subject);
				((Animatable) imageViewSubject.getDrawable()).start();
			}
		});

		TextInputLayout textInputLayoutFeedback = findViewById(R.id.text_input_feedback);
		Objects.requireNonNull(textInputLayoutFeedback.getEditText()).setOnFocusChangeListener((View view, boolean hasFocus) -> {
			if(hasFocus) {
				ImageView imageViewFeedback = findViewById(R.id.image_feedback_box);
				((Animatable) imageViewFeedback.getDrawable()).start();
			}
		});

		FrameLayout frameLayoutSend = findViewById(R.id.frame_send);
		frameLayoutSend.setOnClickListener(v -> {
			ImageView imageViewSend = findViewById(R.id.image_send);
			((Animatable) imageViewSend.getDrawable()).start();

			if(Objects.requireNonNull(textInputLayoutFeedback.getEditText()).getText().toString().equals("")) {
				textInputLayoutFeedback.setError(getString(R.string.hint_error_empty));
			} else {
				textInputLayoutFeedback.setErrorEnabled(false);
				sendEmail(
						textInputLayoutSubject.getEditText().getText().toString(),
						textInputLayoutFeedback.getEditText().getText().toString(),
						sharedPrefs.getBoolean(
								"include_info",
								true
						)
				);
			}
		});

		SwitchMaterial switchIncludeInfo = findViewById(R.id.switch_include_info);
		switchIncludeInfo.setChecked(
				sharedPrefs.getBoolean(
						"include_info",
						true
				)
		);
		switchIncludeInfo.setOnCheckedChangeListener((CompoundButton buttonView, boolean isChecked) -> {
			ImageView imageViewIncludeInfo = findViewById(R.id.image_include_info);
			((Animatable) imageViewIncludeInfo.getDrawable()).start();

			PreferenceManager.getDefaultSharedPreferences(this).edit()
					.putBoolean("include_info", isChecked)
					.apply();
		});

		LinearLayout linearLayoutIncludeInfo = findViewById(R.id.linear_include_info);
		linearLayoutIncludeInfo.setOnClickListener(
				v -> switchIncludeInfo.setChecked(!switchIncludeInfo.isChecked())
		);

		findViewById(R.id.linear_rate).setOnClickListener(v -> {
			ImageView imageViewRate = findViewById(R.id.image_rate);
			((Animatable) imageViewRate.getDrawable()).start();

			Uri uri = Uri.parse("market://details?id=" + getPackageName());
			Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
			goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
					Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
					Intent.FLAG_ACTIVITY_MULTIPLE_TASK |
					Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
			try {
				startActivity(goToMarket);
			} catch (ActivityNotFoundException e) {
				startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(
						"http://play.google.com/store/apps/details?id=" + getPackageName()
				)));
			}
		});
	}

	private void sendEmail(String subject, String body, Boolean includeInfo) {
		if(!subject.equals("")) subject = ": " + subject;
		String info = "";
		if(includeInfo) {
			info = "\n\n" + System.getProperty("os.version") +
					"$" + android.os.Build.VERSION.SDK_INT +
					"\n" + android.os.Build.MODEL +
					" (" + android.os.Build.DEVICE +
					"), " + android.os.Build.MANUFACTURER;
		}
		Intent intent = new Intent(Intent.ACTION_SENDTO);
		intent.setData(
				Uri.parse(
						"mailto:" + getString(R.string.app_mail) +
								"?subject=" + Uri.encode("Feedback@Tack" + subject) +
								"&body=" + Uri.encode(body + info)
				)
		);
		startActivity(Intent.createChooser(intent, "Send email"));
	}
}
