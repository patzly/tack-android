package xyz.zedler.patrick.tack.behavior;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.AppBarLayout;

import xyz.zedler.patrick.tack.R;

public class ScrollBehavior {

	private final static String TAG = ScrollBehavior.class.getSimpleName();
	private final static boolean DEBUG = false;

	private static final int STATE_SCROLLED_DOWN = 1;
	private static final int STATE_SCROLLED_UP = 2;

	private int currentState = STATE_SCROLLED_UP;
	private int pufferSize = 0; // distance before top scroll when overScroll is turned off
	private final int pufferDivider = 2; // distance gets divided to prevent cutoff of edge effect

	private boolean isTopScroll = false;
	private boolean liftOnScroll = true;
	private boolean showNavBarDivider = true;
	private boolean killObserver = true;
	private boolean noOverScroll = false;

	private Activity activity;
	private AppBarLayout appBarLayout;
	private View viewAppBar;
	private NestedScrollView scrollView;

	/**
	 * Initialize scroll behavior
	 */
	public void setUpScroll(
			@NonNull Activity activity,
			AppBarLayout appBarLayout,
			View viewAppBar,
			NestedScrollView scrollView,
			boolean liftOnScroll,
			boolean noOverScroll,
			boolean killObserver
	) {
		this.liftOnScroll = liftOnScroll;
		this.activity = activity;
		this.appBarLayout = appBarLayout;
		this.viewAppBar = viewAppBar;
		this.scrollView = scrollView;
		this.noOverScroll = noOverScroll;
		this.killObserver = killObserver;

		currentState = STATE_SCROLLED_UP;

		measureScrollView();
		setLiftOnScroll(liftOnScroll);

		if (DEBUG) Log.i(TAG, "setUpScroll: liftOnScroll = " + liftOnScroll);

		if (this.scrollView == null) return;
		this.scrollView.setOnScrollChangeListener((NestedScrollView v,
												   int scrollX,
												   int scrollY,
												   int oldScrollX,
												   int oldScrollY
		) -> {
			if (!isTopScroll && scrollY == 0) { // TOP
				onTopScroll();
			} else {
				if (scrollY < oldScrollY) { // UP
					if (currentState != STATE_SCROLLED_UP) {
						onScrollUp();
					}
					if (liftOnScroll) {
						if (scrollY < pufferSize) {
							new Handler(Looper.getMainLooper()).postDelayed(() -> {
								if (scrollY > 0) {
									this.scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
								}
							}, 1);
						}
					}
				} else if (scrollY > oldScrollY) {
					if (currentState != STATE_SCROLLED_DOWN) { // DOWN
						onScrollDown();
					}
				}
			}
		});
	}

	/**
	 * Initialize scroll behavior
	 */
	public void setUpScroll(
			@NonNull Activity activity,
			AppBarLayout appBarLayout,
			View viewAppBar,
			NestedScrollView scrollView,
			boolean liftOnScroll
	) {
		setUpScroll(
				activity,
				appBarLayout,
				viewAppBar,
				scrollView,
				liftOnScroll,
				false,
				true
		);
	}

	/**
	 * Gets called once when scrollY is 0.
	 */
	private void onTopScroll() {
		isTopScroll = true;
		if (appBarLayout != null) {
			if (liftOnScroll) {
				tintTopBars(R.color.background);
				appBarLayout.setLifted(false);
			}
			if (DEBUG) Log.i(TAG, "onTopScroll: liftOnScroll = " + liftOnScroll);
		} else if (DEBUG) {
			Log.e(TAG, "onTopScroll: appBarLayout is null!");
		}
	}

	/**
	 * Gets called once when the user scrolls up.
	 */
	private void onScrollUp() {
		currentState = STATE_SCROLLED_UP;
		if (appBarLayout != null) {
			appBarLayout.setLifted(true);
			tintTopBars(R.color.primary);
		} else if (DEBUG) {
			Log.e(TAG, "onScrollUp: appBarLayout is null!");
		}
		if (DEBUG) Log.i(TAG, "onScrollUp: UP");
	}

	/**
	 * Gets called once when the user scrolls down.
	 */
	private void onScrollDown() {
		isTopScroll = false; // second top scroll is unrealistic before down scroll
		currentState = STATE_SCROLLED_DOWN;
		if (appBarLayout != null && scrollView != null) {
			appBarLayout.setLifted(true);
			tintTopBars(R.color.primary);
			scrollView.setOverScrollMode(
					noOverScroll ? View.OVER_SCROLL_NEVER : View.OVER_SCROLL_IF_CONTENT_SCROLLS
			);
		} else if (DEBUG) {
			Log.e(TAG, "onScrollDown: appBarLayout or scrollView is null!");
		}
		if (DEBUG) Log.i(TAG, "onScrollDown: DOWN");
	}

	/**
	 * Sets the global boolean and moves the appBar manually if necessary.
	 * If scrollY of the scrollView is 0, OverScroll is turned off.
	 * Otherwise it's on if the the view is scrollable.
	 */
	public void setLiftOnScroll(boolean lift) {
		liftOnScroll = lift;
		if (appBarLayout != null) {
			appBarLayout.setLiftOnScroll(false); // We'll make this manually
			appBarLayout.setLiftable(true);
			if (scrollView != null) {
				if (lift) {
					if (scrollView.getScrollY() == 0) {
						appBarLayout.setLifted(false);
						tintTopBars(R.color.background);
						scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
					} else {
						appBarLayout.setLifted(true);
						tintTopBars(R.color.primary);
					}
				} else {
					appBarLayout.setLifted(true);
					tintTopBars(R.color.primary);
					scrollView.setOverScrollMode(
							noOverScroll
									? View.OVER_SCROLL_NEVER
									: View.OVER_SCROLL_IF_CONTENT_SCROLLS
					);
				}
			} else {
				if (lift) {
					appBarLayout.setLiftable(true);
					appBarLayout.setLifted(false);
					tintTopBars(R.color.background);
				} else {
					appBarLayout.setLiftable(false);
					tintTopBars(R.color.primary);
				}
			}
		} else if (DEBUG) Log.e(TAG, "setLiftOnScroll: appBarLayout is null!");
		if (DEBUG) Log.i(TAG, "setLiftOnScroll(" + lift + ")");
	}

	/**
	 * Adds a globalLayoutListener to the scrollView to get its own and the content's height.
	 */
	private void measureScrollView() {
		if (scrollView == null) return;
		scrollView.getViewTreeObserver().addOnGlobalLayoutListener(
				new ViewTreeObserver.OnGlobalLayoutListener() {
					@Override
					public void onGlobalLayout() {
						int scrollViewHeight = scrollView.getMeasuredHeight();
						if (scrollView.getChildAt(0) != null) {
							int scrollContentHeight = scrollView.getChildAt(0).getHeight();
							showNavBarDivider = scrollViewHeight - scrollContentHeight < 0;
							setNavBarDividerVisibility();
							pufferSize = (scrollContentHeight - scrollViewHeight) / pufferDivider;
							if (DEBUG) {
								Log.i(TAG, "measureScrollView: viewHeight = "
										+ scrollViewHeight
										+ ", contentHeight = " + scrollContentHeight
								);
							}
						} else {
							if (DEBUG) Log.e(TAG, "measureScrollView: no child!");
						}
						// Kill ViewTreeObserver
						if (!killObserver) return;
						if (scrollView.getViewTreeObserver().isAlive()) {
							scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(
									this
							);
						}
					}
				});
	}

	/**
	 * Tints the navBarDivider with divider color if setUpBottomAppBar wasn't called before
	 * and the scrollView is scrollable, else transparent.
	 */
	private void setNavBarDividerVisibility() {
		if (activity != null) {
			int orientation = activity.getResources().getConfiguration().orientation;
			if (orientation == Configuration.ORIENTATION_PORTRAIT) {
				if (showNavBarDivider) {
					setNavBarDividerColor(R.color.stroke_secondary);
				} else {
					setNavBarDividerColor(R.color.transparent);
				}
				if (DEBUG) Log.i(TAG, "setNavBarDividerVisibility(" + showNavBarDivider + ")");
			} else {
				setNavBarDividerColor(R.color.stroke_secondary);
			}
		} else if (DEBUG) Log.wtf(TAG, "setNavBarDividerVisibility: activity is null!?");
	}

	/**
	 * If SDK version is 28 or higher this tints the navBarDivider.
	 */
	private void setNavBarDividerColor(@ColorRes int color) {
		if (activity != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
			activity.getWindow().setNavigationBarDividerColor(
					ContextCompat.getColor(activity, color)
			);
		} else if (DEBUG) Log.i(TAG, "setNavBarDividerColor: activity is null or SDK < 28");
	}

	@SuppressLint("PrivateResource")
	private void tintTopBars(@ColorRes int target) {
		if (activity == null || viewAppBar == null) {
			if (DEBUG) Log.e(TAG, "tintTopBars: activity or viewAppBar is null!");
			return;
		}
		int appBarColor = getAppBarColor();

		int targetColor = ContextCompat.getColor(activity, target);
		if (appBarColor != targetColor) {
			ValueAnimator valueAnimator = ValueAnimator.ofArgb(appBarColor, targetColor);
			valueAnimator.addUpdateListener(
					animation -> viewAppBar.setBackgroundColor(
							(int) valueAnimator.getAnimatedValue()
					)
			);
			valueAnimator.setDuration(activity.getResources().getInteger(
					R.integer.app_bar_elevation_anim_duration
			)).start();
			if (DEBUG) Log.i(TAG, "tintTopBars: appBarLinearLayout tinted");
		} else if (DEBUG) Log.i(TAG, "tintTopBars: current and target identical");

		int statusBarColor = activity.getWindow().getStatusBarColor();
		if (statusBarColor != targetColor) {
			ValueAnimator valueAnimator = ValueAnimator.ofArgb(statusBarColor, targetColor);
			valueAnimator.addUpdateListener(animation -> {
				if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M && appBarColor == Color.WHITE) {
					setStatusBarColor(
							ContextCompat.getColor(activity, R.color.status_bar_lollipop)
					);
				} else {
					setStatusBarColor((int) valueAnimator.getAnimatedValue());
				}
			});
			valueAnimator.setDuration(activity.getResources().getInteger(
					R.integer.app_bar_elevation_anim_duration
			)).start();
			if (DEBUG) Log.i(TAG, "tintTopBars: status bar tinted");
		} else {
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M
					&& appBarColor == ContextCompat.getColor(activity, R.color.white)
			) {
				setStatusBarColor(ContextCompat.getColor(activity, R.color.status_bar_lollipop));
			}
			if (DEBUG) Log.i(TAG, "tintTopBars: current and target identical");
		}
	}

	private int getAppBarColor() {
		Drawable background = viewAppBar.getBackground();
		if (background == null || background.getClass() != ColorDrawable.class) {
			viewAppBar.setBackgroundColor(
					ContextCompat.getColor(activity, R.color.background)
			);
		}
		return ((ColorDrawable) viewAppBar.getBackground()).getColor();
	}

	private void setStatusBarColor(int color) {
		activity.getWindow().setStatusBarColor(color);
	}
}
