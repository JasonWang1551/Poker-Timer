package com.firetv.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.firetv.R;

/** Shared state colors for both inflated and dynamically created buttons. */
public final class ButtonColors {
    private ButtonColors() {
    }

    // Shared appearance controls: edit these to tune every theme's button effects.
    private static final float ACCENT_BLEND = 0.95f;
    private static final float FOCUS_LIGHTNESS_SHIFT = 0.025f;
    private static final float PRESSED_LIGHTNESS_SHIFT = 0.10f;
    private static final float DISABLED_OPACITY = 0.3f;

    private static final int[][] STATES = {
            {-android.R.attr.state_enabled},
            {android.R.attr.state_pressed},
            {android.R.attr.state_focused},
            {android.R.attr.state_hovered},
            {}
    };


    public static int themeColor(Context context, int attribute) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attribute, value, true);
        return value.data;
    }

    public static float themeFloat(Context context, int attribute) {
        TypedValue value = new TypedValue();
        context.getTheme().resolveAttribute(attribute, value, true);
        return value.getFloat();
    }

    public static int disabledColor(int color) {
        return ColorUtils.setAlphaComponent(color, Math.round(Color.alpha(color) * DISABLED_OPACITY));
    }

    private static int shiftLightness(int color, float amount) {
        float[] hsl = new float[3];
        ColorUtils.colorToHSL(color, hsl);
        hsl[2] = Math.max(0f, Math.min(1f, hsl[2] + amount));
        return ColorUtils.HSLToColor(hsl);
    }

    public static void apply(View button) {
        Context context = button.getContext();
        int normal = themeColor(context, R.attr.buttonBackground);
        int accent = themeColor(context, R.attr.themeAccent);
        int text = themeColor(context, R.attr.themeText);
        int background = themeColor(context, R.attr.timerBackground);

        // Accent-heavy blending preserves the existing gold/green focus appearance.
        // The small lightness shift still distinguishes focus when accent == button.
        int focused = shiftLightness(ColorUtils.blendARGB(normal, accent, ACCENT_BLEND),
                ColorUtils.calculateLuminance(normal) < 0.5 ? FOCUS_LIGHTNESS_SHIFT : -FOCUS_LIGHTNESS_SHIFT);
        int pressed = shiftLightness(focused,
                ColorUtils.calculateLuminance(focused) > 0.35 ? -PRESSED_LIGHTNESS_SHIFT : PRESSED_LIGHTNESS_SHIFT);

        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(6 * context.getResources().getDisplayMetrics().density);
        drawable.setColor(new ColorStateList(STATES, new int[]{
                disabledColor(normal), pressed, focused, focused, normal
        }));
        button.setBackgroundTintList(null);
        button.setBackground(drawable);

        ColorStateList labels = new ColorStateList(STATES, new int[]{
                disabledColor(text), readableText(text, background, pressed),
                readableText(text, background, focused), readableText(text, background, focused),
                text
        });
        if (button instanceof ImageButton) {
            ((ImageButton) button).setImageTintList(labels);
        } else if (button instanceof TextView) {
            ((TextView) button).setTextColor(labels);
        }
    }

    /** Level selection is a shade of the menu, with the theme text left unchanged. */
    public static void applySelection(View row) {
        Context context = row.getContext();
        int menu = ColorUtils.compositeColors(ColorUtils.setAlphaComponent(
                themeColor(context, R.attr.menuBackground), Math.round(
                        themeFloat(context, R.attr.menuOpacity) * 255)),
                themeColor(context, R.attr.timerBackground));
        int text = themeColor(context, R.attr.themeText);
        int fill = selectionFill(menu, text);
        int accent = themeColor(context, R.attr.themeAccent);
        int[][] states = {
                {android.R.attr.state_pressed}, {android.R.attr.state_focused},
                {android.R.attr.state_hovered}, {android.R.attr.state_selected}, {}
        };
        float density = context.getResources().getDisplayMetrics().density;
        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(4 * density);
        drawable.setColor(new ColorStateList(states,
                new int[]{fill, fill, fill, Color.TRANSPARENT, Color.TRANSPARENT}));
        drawable.setStroke(Math.round(3 * density), new ColorStateList(states,
                new int[]{accent, accent, accent, accent, Color.TRANSPARENT}));
        row.setBackground(drawable);
    }

    private static int selectionFill(int menu, int text) {
        int shade = ColorUtils.calculateLuminance(menu) < 0.6 ? Color.WHITE : Color.BLACK;
        // Start with 40% if white and 15% if black, reducing it if it weakens text contrast.
        for (int percent = shade == Color.WHITE ? 40 : 15; percent >= 0; percent--) {
            int candidate = ColorUtils.blendARGB(menu, shade, percent / 100f);
            if (ColorUtils.calculateContrast(text, candidate) >= 4.5) {
                return candidate;
            }
        }
        // If the base menu itself has poor contrast, move toward the safer extreme.
        int safe = ColorUtils.calculateContrast(text, Color.BLACK)
                > ColorUtils.calculateContrast(text, Color.WHITE) ? Color.BLACK : Color.WHITE;
        for (int percent = 1; percent <= 100; percent++) {
            int candidate = ColorUtils.blendARGB(menu, safe, percent / 100f);
            if (ColorUtils.calculateContrast(text, candidate) >= 4.5) {
                return candidate;
            }
        }
        return safe;
    }

    private static int readableText(int preferred, int dark, int fill) {
        if (ColorUtils.calculateContrast(preferred, fill) >= 4.5) {
            return preferred;
        }
        // Keep the existing background-colored labels on bright focus fills.
        if (ColorUtils.calculateContrast(dark, fill) >= 4.5) {
            return dark;
        }
        return ColorUtils.calculateContrast(Color.BLACK, fill)
                > ColorUtils.calculateContrast(Color.WHITE, fill) ? Color.BLACK : Color.WHITE;
    }
}
