package xyz.zedler.patrick.tack.view;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ScrollView;

import androidx.annotation.Nullable;

public class RotaryScrollView extends ScrollView {

    public RotaryScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        requestFocus();
    }

    @Override
    public void onFocusChanged(
            boolean gainFocus,
            int direction,
            @Nullable Rect previouslyFocusedRect
    ) {
        super.onFocusChanged(gainFocus, direction, previouslyFocusedRect);
        if (!gainFocus) requestFocus(direction, previouslyFocusedRect);
    }
}
