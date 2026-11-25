package com.artifex.mupdf.mini;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.AttrRes;
import androidx.annotation.ColorInt;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

/**
 * Keeps MuPDF reader UI updates in one place without touching core rendering/gesture logic.
 */
public class ReaderUiController {
    private static final long BAR_ANIM_DURATION_MS = 140L;

    private final View backgroundLayout;
    private final View topBar;
    private final View bottomBar;
    private final View immersiveFooter;
    private final TextView titleLabel;
    private final TextView pageLabel;
    private final TextView immersiveFooterText;
    private final ProgressBar immersiveProgressBar;
    private final ImageView[] tintableIcons;

    public ReaderUiController(
            View backgroundLayout,
            View topBar,
            View bottomBar,
            View immersiveFooter,
            TextView titleLabel,
            TextView pageLabel,
            TextView immersiveFooterText,
            ProgressBar immersiveProgressBar,
            ImageView... tintableIcons
    ) {
        this.backgroundLayout = backgroundLayout;
        this.topBar = topBar;
        this.bottomBar = bottomBar;
        this.immersiveFooter = immersiveFooter;
        this.titleLabel = titleLabel;
        this.pageLabel = pageLabel;
        this.immersiveFooterText = immersiveFooterText;
        this.immersiveProgressBar = immersiveProgressBar;
        this.tintableIcons = tintableIcons;
    }

    public void applyTheme(int bgMode, int fontChoice) {
        Context context = resolveContext();
        FallbackPalette fallback = fallbackPaletteFor(bgMode);

        int bgColor = fallback.bgColor;
        int barColor = fallback.barColor;
        int textColor = fallback.textColor;
        int progressTrackColor = fallback.progressTrackColor;

        if (context != null) {
            bgColor = resolveColorAttr(context, R.attr.readerBackgroundColor, fallback.bgColor);
            barColor = resolveColorAttr(context, R.attr.readerChromeBackgroundColor, fallback.barColor);
            textColor = resolveColorAttr(context, R.attr.readerTextColor, fallback.textColor);
            progressTrackColor = resolveColorAttr(
                    context,
                    R.attr.readerProgressBackgroundColor,
                    fallback.progressTrackColor
            );
        }

        if (backgroundLayout != null) backgroundLayout.setBackgroundColor(bgColor);
        if (topBar != null) topBar.setBackgroundColor(barColor);
        if (bottomBar != null) bottomBar.setBackgroundColor(barColor);
        if (immersiveFooter != null) immersiveFooter.setBackgroundColor(bgColor);

        if (pageLabel != null) pageLabel.setTextColor(textColor);
        if (immersiveFooterText != null) immersiveFooterText.setTextColor(textColor);
        if (titleLabel != null) titleLabel.setTextColor(textColor);

        if (immersiveProgressBar != null) {
            immersiveProgressBar.setProgressTintList(ColorStateList.valueOf(textColor));
            immersiveProgressBar.setProgressBackgroundTintList(ColorStateList.valueOf(progressTrackColor));
        }

        if (tintableIcons != null) {
            for (ImageView icon : tintableIcons) {
                if (icon == null || icon.getDrawable() == null) continue;
                icon.setColorFilter(textColor);
            }
        }

        applyFontChoice(fontChoice);
    }

    private Context resolveContext() {
        if (backgroundLayout != null) return backgroundLayout.getContext();
        if (titleLabel != null) return titleLabel.getContext();
        if (pageLabel != null) return pageLabel.getContext();
        if (immersiveFooterText != null) return immersiveFooterText.getContext();
        return null;
    }

    private static final class FallbackPalette {
        @ColorInt final int bgColor;
        @ColorInt final int barColor;
        @ColorInt final int textColor;
        @ColorInt final int progressTrackColor;

        FallbackPalette(@ColorInt int bgColor, @ColorInt int barColor, @ColorInt int textColor, @ColorInt int progressTrackColor) {
            this.bgColor = bgColor;
            this.barColor = barColor;
            this.textColor = textColor;
            this.progressTrackColor = progressTrackColor;
        }
    }

    // Mirrors the per-mode values declared in lib/res/values/styles.xml (Theme.MuPdfReader*),
    // used only if attribute resolution fails (e.g. context without the reader theme applied).
    private FallbackPalette fallbackPaletteFor(int bgMode) {
        switch (bgMode) {
            case 1: // cream
                return new FallbackPalette(
                        Color.parseColor("#FAF4E8"),
                        Color.parseColor("#EDE0C8"),
                        Color.parseColor("#333333"),
                        Color.parseColor("#EDE0C8")
                );
            case 2: // dark
                return new FallbackPalette(
                        Color.parseColor("#222222"),
                        Color.parseColor("#222222"),
                        Color.parseColor("#EEEEEE"),
                        Color.parseColor("#444444")
                );
            default: // light/default
                return new FallbackPalette(
                        Color.parseColor("#FFFFFF"),
                        Color.parseColor("#FFFFFF"),
                        Color.parseColor("#333333"),
                        Color.parseColor("#E0E0E0")
                );
        }
    }

    @ColorInt
    private int resolveColorAttr(Context context, @AttrRes int attr, @ColorInt int fallbackColor) {
        TypedValue tv = new TypedValue();
        if (!context.getTheme().resolveAttribute(attr, tv, true)) {
            return fallbackColor;
        }

        if (tv.resourceId != 0) {
            return resolveColorResource(context, tv.resourceId, fallbackColor);
        }

        if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
            return tv.data;
        }

        return fallbackColor;
    }

    @ColorInt
    private int resolveColorResource(Context context, int resId, @ColorInt int fallbackColor) {
        if (resId == 0) return fallbackColor;
        try {
            return ContextCompat.getColor(context, resId);
        } catch (Resources.NotFoundException ex) {
            return fallbackColor;
        }
    }

    private void applyFontChoice(int fontChoice) {
        Typeface typeface = Typeface.DEFAULT;
        if (fontChoice == 1) {
            Context context = null;
            if (backgroundLayout != null) {
                context = backgroundLayout.getContext();
            } else if (titleLabel != null) {
                context = titleLabel.getContext();
            }

            if (context != null) {
                int resId = context.getResources().getIdentifier("literata_regular", "font", context.getPackageName());
                if (resId != 0) {
                    Typeface resolved = ResourcesCompat.getFont(context, resId);
                    if (resolved != null) {
                        typeface = resolved;
                    }
                }
            }
        }

        if (titleLabel != null) titleLabel.setTypeface(typeface);
        if (pageLabel != null) pageLabel.setTypeface(typeface);
        if (immersiveFooterText != null) immersiveFooterText.setTypeface(typeface);
    }

    public void updateProgress(int pageNumber, int pageCount) {
        String label = pageNumber + " / " + pageCount;
        if (pageLabel != null) {
            pageLabel.setText(label);
        }
        if (immersiveFooterText != null) {
            immersiveFooterText.setText(label);
        }
        if (immersiveProgressBar != null) {
            immersiveProgressBar.setMax(Math.max(pageCount, 1));
            immersiveProgressBar.setProgress(Math.max(0, Math.min(pageNumber, pageCount)));
        }
    }

    public void setBarsVisible(boolean visible, View currentBar) {
        setBarsVisible(visible, currentBar, false);
    }

    public void setBarsVisible(boolean visible, View currentBar, boolean animated) {
        int barsVis = visible ? View.VISIBLE : View.GONE;
        int footerVis = visible ? View.GONE : View.VISIBLE;
        if (!animated) {
            if (topBar != null) topBar.setVisibility(barsVis);
            if (currentBar != null) currentBar.setVisibility(barsVis);
            if (bottomBar != null) bottomBar.setVisibility(barsVis);
            if (immersiveFooter != null) immersiveFooter.setVisibility(footerVis);
            return;
        }

        animateVisibility(topBar, visible);
        animateVisibility(currentBar, visible);
        animateVisibility(bottomBar, visible);
        animateVisibility(immersiveFooter, !visible);
    }

    private void animateVisibility(View view, boolean visible) {
        if (view == null) return;

        if (visible) {
            if (view.getVisibility() == View.VISIBLE && view.getAlpha() >= 1f) return;
            view.animate().cancel();
            view.setAlpha(0f);
            view.setVisibility(View.VISIBLE);
            view.animate().alpha(1f).setDuration(BAR_ANIM_DURATION_MS).start();
        } else {
            if (view.getVisibility() != View.VISIBLE) return;
            view.animate().cancel();
            view.animate().alpha(0f).setDuration(BAR_ANIM_DURATION_MS).withEndAction(new Runnable() {
                @Override
                public void run() {
                    view.setVisibility(View.GONE);
                    view.setAlpha(1f);
                }
            }).start();
        }
    }
}
