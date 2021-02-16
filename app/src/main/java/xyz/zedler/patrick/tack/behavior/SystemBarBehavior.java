package xyz.zedler.patrick.tack.behavior;

import android.app.Activity;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.Window;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.widget.NestedScrollView;

import com.google.android.material.appbar.AppBarLayout;

import xyz.zedler.patrick.tack.R;

public class SystemBarBehavior {

    private final static String TAG = SystemBarBehavior.class.getSimpleName();

    private final static int COLOR_SCRIM = 0x55000000;
    private final static int COLOR_SCRIM_DARK = 0xB31e1f22;
    private final static int COLOR_SCRIM_LIGHT = 0xB3FFFFFF;

    private final Activity activity;
    private final Window window;
    private AppBarLayout appBarLayout;
    private ViewGroup container;
    private NestedScrollView scrollView;
    private ViewGroup scrollContent;

    int containerPaddingTop;
    int containerPaddingBottom;
    int scrollContentPaddingBottom;

    private boolean applyAppBarInsetOnContainer;
    private boolean hasScrollView;
    private boolean isScrollable;

    public SystemBarBehavior(@NonNull Activity activity) {
        this.activity = activity;
        window = activity.getWindow();

        // GOING EDGE TO EDGE
        layoutFullscreen();

        applyAppBarInsetOnContainer = true;
        hasScrollView = false;
        isScrollable = false;
    }

    public void setAppBar(AppBarLayout appBarLayout) {
        this.appBarLayout = appBarLayout;
    }

    public void setContainer(@NonNull ViewGroup container) {
        this.container = container;
        containerPaddingTop = container.getPaddingTop();
        containerPaddingBottom = container.getPaddingBottom();
    }

    public void setScroll(@NonNull NestedScrollView scrollView,
                          @NonNull ViewGroup scrollContent) {
        this.scrollView = scrollView;
        this.scrollContent = scrollContent;
        scrollContentPaddingBottom = scrollContent.getPaddingBottom();
        hasScrollView = true;

        if (container == null) setContainer(scrollView);
    }

    public void setUp() {
        // TOP INSET
        if (appBarLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(appBarLayout, (v, insets) -> {
                // STATUS BAR INSET
                appBarLayout.setPadding(
                        0, insets.getSystemWindowInsetTop(), 0,
                        appBarLayout.getPaddingBottom()
                );
                appBarLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
                // TODO: Marshmallow has weird issue when measured, changes width to wrap_content

                // APP BAR INSET
                if (container != null && applyAppBarInsetOnContainer) {
                    ViewGroup.MarginLayoutParams params
                            = (ViewGroup.MarginLayoutParams) container.getLayoutParams();
                    params.topMargin = appBarLayout.getMeasuredHeight();
                    container.setLayoutParams(params);
                }
                return insets;
            });
        } else {
            // if no app bar exists, status bar inset is applied to container
            ViewCompat.setOnApplyWindowInsetsListener(container, (v, insets) -> {
                // STATUS BAR INSET
                container.setPadding(
                        container.getPaddingLeft(),
                        containerPaddingTop + insets.getSystemWindowInsetTop(),
                        container.getPaddingRight(),
                        container.getPaddingBottom()
                );
                return insets;
            });
        }

        // NAV BAR INSET
        if (isPortrait()) {
            View container = hasScrollView ? scrollContent : this.container;
            ViewCompat.setOnApplyWindowInsetsListener(container, (v, insets) -> {
                int paddingBottom = hasScrollView
                        ? scrollContentPaddingBottom
                        : containerPaddingBottom;
                container.setPadding(
                        container.getPaddingLeft(),
                        container.getPaddingTop(),
                        container.getPaddingRight(),
                        paddingBottom + insets.getSystemWindowInsetBottom()
                );
                return insets;
            });
        } else {
            if (isGestureNav()) {
                View container = hasScrollView ? scrollContent : this.container;
                ViewCompat.setOnApplyWindowInsetsListener(container, (v, insets) -> {
                    int paddingBottom = hasScrollView
                            ? scrollContentPaddingBottom
                            : containerPaddingBottom;
                    container.setPadding(
                            container.getPaddingLeft(),
                            container.getPaddingTop(),
                            container.getPaddingRight(),
                            paddingBottom + insets.getSystemWindowInsetBottom()
                    );
                    return insets;
                });
            } else {
                View root = window.getDecorView().findViewById(android.R.id.content);
                ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
                    root.setPadding(
                            root.getPaddingLeft(),
                            root.getPaddingTop(),
                            insets.getSystemWindowInsetRight(),
                            root.getPaddingBottom()
                    );
                    return insets;
                });
            }
        }

        if (hasScrollView) {
            // call viewThreeObserver, this updates the system bar appearance
            measureScrollView();
        } else {
            // call directly because there won't be any changes caused by scroll content
            updateSystemBars();
        }
    }

    private void measureScrollView() {
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int scrollViewHeight = scrollView.getMeasuredHeight();
                        int scrollContentHeight = scrollContent.getHeight();
                        isScrollable = scrollViewHeight - scrollContentHeight < 0;
                        updateSystemBars();
                        // Kill ViewTreeObserver
                        if (scrollView.getViewTreeObserver().isAlive()) {
                            scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(
                                    this
                            );
                        }
                    }
                });
    }

    public void applyAppBarInsetOnContainer(boolean apply) {
        applyAppBarInsetOnContainer = apply;
    }

    public void applyBottomInset(@NonNull View view) {
        ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        int marginBottom = params.bottomMargin;
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            params.bottomMargin = marginBottom + insets.getSystemWindowInsetBottom();
            view.setLayoutParams(params);
            return insets;
        });
    }

    private void updateSystemBars() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) { // 29
            window.setStatusBarColor(Color.TRANSPARENT);
            if (!isDarkMode()) setLightStatusBar();
            if (isGestureNav()) {
                window.setNavigationBarColor(Color.TRANSPARENT);
                window.setNavigationBarContrastEnforced(true);
            } else {
                if (!isDarkMode()) setLightNavBar();
                if (isPortrait()) {
                    window.setNavigationBarColor(
                            isScrollable
                                    ? (isDarkMode() ? COLOR_SCRIM_DARK : COLOR_SCRIM_LIGHT)
                                    : Color.TRANSPARENT
                    );
                } else {
                    window.setNavigationBarDividerColor(getColor(R.color.stroke_secondary));
                    window.setNavigationBarColor(getColor(R.color.background));
                }
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) { // 28
            window.setStatusBarColor(Color.TRANSPARENT);
            if (!isDarkMode()) {
                setLightStatusBar();
                setLightNavBar();
            }
            if (isPortrait()) {
                window.setNavigationBarColor(
                        isScrollable
                                ? (isDarkMode() ? COLOR_SCRIM_DARK : COLOR_SCRIM_LIGHT)
                                : Color.TRANSPARENT
                );
            } else {
                window.setNavigationBarDividerColor(getColor(R.color.stroke_secondary));
                window.setNavigationBarColor(getColor(R.color.background));
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) { // 27
            window.setStatusBarColor(Color.TRANSPARENT);
            if (!isDarkMode()) setLightStatusBar();
            if (isPortrait()) {
                window.setNavigationBarColor(
                        isScrollable
                                ? (isDarkMode() ? COLOR_SCRIM_DARK : COLOR_SCRIM_LIGHT)
                                : Color.TRANSPARENT
                );
                if (!isDarkMode()) setLightNavBar();
            } else {
                window.setNavigationBarColor(Color.BLACK);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { // 23
            window.setStatusBarColor(Color.TRANSPARENT);
            if (!isDarkMode()) setLightStatusBar();
            if (isPortrait()) {
                window.setNavigationBarColor(
                        isDarkMode()
                                ? (isScrollable ? COLOR_SCRIM_DARK : Color.TRANSPARENT)
                                : COLOR_SCRIM
                );
            } else {
                window.setNavigationBarColor(Color.BLACK);
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // 21
            window.setStatusBarColor(isDarkMode() ? Color.TRANSPARENT : COLOR_SCRIM);
            if (isPortrait()) {
                window.setNavigationBarColor(
                        isDarkMode()
                                ? (isScrollable ? COLOR_SCRIM_DARK : Color.TRANSPARENT)
                                : COLOR_SCRIM
                );
            } else {
                window.setNavigationBarColor(Color.BLACK);
            }
        }
    }

    @SuppressWarnings("deprecation")
    private void layoutFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        } else {
            final int decorFitsFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            final View decorView = window.getDecorView();
            decorView.setSystemUiVisibility(decorView.getSystemUiVisibility() | decorFitsFlags);
        }
    }

    @SuppressWarnings("deprecation")
    private void setLightNavBar() {
        // TODO: SDK 30 method doesn't work
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return;
        final View decorView = window.getDecorView();
        decorView.setSystemUiVisibility(
                decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );
    }

    @SuppressWarnings("deprecation")
    private void setLightStatusBar() {
        // TODO: SDK 30 method doesn't work
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return;
        final View decorView = window.getDecorView();
        decorView.setSystemUiVisibility(
                decorView.getSystemUiVisibility() | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        );
    }

    private boolean isDarkMode() {
        int uiMode = activity.getResources().getConfiguration().uiMode;
        return (uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private boolean isGestureNav() {
        final int NAV_GESTURE = 2;
        Resources resources = activity.getResources();
        int resourceId = resources.getIdentifier(
                "config_navBarInteractionMode", "integer", "android"
        );
        int mode = resourceId > 0 ? resources.getInteger(resourceId) : 0;
        return mode == NAV_GESTURE;
    }

    private boolean isPortrait() {
        int orientation = activity.getResources().getConfiguration().orientation;
        return orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    private int getColor(@ColorRes int resId) {
        return ContextCompat.getColor(activity, resId);
    }
}
