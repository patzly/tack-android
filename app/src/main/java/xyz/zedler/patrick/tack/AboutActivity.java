package xyz.zedler.patrick.tack;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Animatable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.IdRes;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import xyz.zedler.patrick.tack.behavior.ScrollBehavior;
import xyz.zedler.patrick.tack.fragment.TextBottomSheetDialogFragment;

public class AboutActivity extends AppCompatActivity implements View.OnClickListener {

	private final static boolean DEBUG = false;
	private final static String TAG = "AboutActivity";

	private long mLastClickTime = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

		AppCompatDelegate.setDefaultNightMode(
				sharedPrefs.getBoolean("force_dark_mode",false)
						? AppCompatDelegate.MODE_NIGHT_YES
						: AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
		);
		setContentView(R.layout.activity_about);

		findViewById(R.id.frame_back_about).setOnClickListener(v -> {
			if (SystemClock.elapsedRealtime() - mLastClickTime < 1000){
				return;
			}
			mLastClickTime = SystemClock.elapsedRealtime();
			finish();
		});

		(new ScrollBehavior()).setUpScroll(
				this,
				R.id.app_bar_about,
				R.id.linear_app_bar_about,
				R.id.scroll_about,
				true
		);

		setOnClickListeners(
				R.id.linear_changelog,
				R.id.linear_developer,
				R.id.linear_license_metronome,
				R.id.linear_license_material_components,
				R.id.linear_license_material_icons,
				R.id.linear_license_roboto
		);
	}

	private void setOnClickListeners(@IdRes int... viewIds) {
		for (int viewId : viewIds) {
			findViewById(viewId).setOnClickListener(this);
		}
	}

	@Override
	public void onClick(View v) {

		if (SystemClock.elapsedRealtime() - mLastClickTime < 600){
			return;
		}
		mLastClickTime = SystemClock.elapsedRealtime();

		int id = v.getId();
		if (id == R.id.linear_changelog) {
			startAnimatedIcon(R.id.image_changelog);
			showTextBottomSheet("changelog", R.string.info_changelog, 0);
		} else if (id == R.id.linear_developer) {
			startAnimatedIcon(R.id.image_developer);
			new Handler(Looper.getMainLooper()).postDelayed(
					() -> startActivity(
							new Intent(
									Intent.ACTION_VIEW,
									Uri.parse(
											"http://play.google.com/store/apps/dev?id=" +
													"8122479227040208191"
									)
							)
					), 300
			);
		} else if (id == R.id.linear_license_metronome) {
			startAnimatedIcon(R.id.image_license_metronome);
			showTextBottomSheet(
					"apache",
					R.string.license_metronome,
					R.string.license_metronome_link
			);
		} else if (id == R.id.linear_license_material_components) {
			startAnimatedIcon(R.id.image_license_material_components);
			showTextBottomSheet(
					"apache",
					R.string.license_material_components,
					R.string.license_material_components_link
			);
		} else if (id == R.id.linear_license_material_icons) {
			startAnimatedIcon(R.id.image_license_material_icons);
			showTextBottomSheet(
					"apache",
					R.string.license_material_icons,
					R.string.license_material_icons_link
			);
		} else if (id == R.id.linear_license_roboto) {
			startAnimatedIcon(R.id.image_license_roboto);
			showTextBottomSheet(
					"apache",
					R.string.license_roboto,
					R.string.license_roboto_link
			);
		}
	}

	private void showTextBottomSheet(String file, @StringRes int title, @StringRes int link) {
		Fragment textBottomSheetDialogFragment = new TextBottomSheetDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putString("title", getString(title));
		bundle.putString("file", file);
		if (link != 0) {
			bundle.putString("link", getString(link));
		}
		textBottomSheetDialogFragment.setArguments(bundle);
		getSupportFragmentManager().beginTransaction()
				.add(textBottomSheetDialogFragment, "text")
				.commit();
	}

	private void startAnimatedIcon(int viewId) {
		try {
			((Animatable) ((ImageView) findViewById(viewId)).getDrawable()).start();
		} catch (ClassCastException e) {
			if (DEBUG) Log.e(TAG, "startAnimatedIcon() requires AVD!");
		}
	}
}
