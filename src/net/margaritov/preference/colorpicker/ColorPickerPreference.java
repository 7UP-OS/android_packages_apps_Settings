/*
 * Copyright (C) 2011 Sergey Margaritov
 * Copyright (C) 2013 Slimroms
 * Copyright (C) 2016 DarkKat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.margaritov.preference.colorpicker;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceViewHolder;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;

import com.android.settings.SettingsActivity;
import com.android.settings.R;

import net.margaritov.preference.colorpicker.drawable.ColorViewCircleDrawable;
import net.margaritov.preference.colorpicker.fragment.ColorPickerFragment;
import net.margaritov.preference.colorpicker.util.ColorPickerHelper;
import net.margaritov.preference.colorpicker.widget.ColorViewButton;

/**
 * A preference type that allows a user to choose a color
 * 
 * @author Sergey Margaritov
 */
public class ColorPickerPreference extends Preference implements
        Preference.OnPreferenceClickListener, ColorPickerFragment.OnColorChangedListener {
    public static final String TAG = "ColorPickerPreference";

    private static final String ANDROID_NS      = "http://schemas.android.com/apk/res/android";
    private static final String DEFAULT_VALUE   = "defaultValue";

    private PreferenceViewHolder mViewHolder;

    private ColorPickerFragment mPickerFragment;

    LinearLayout widgetFrameView;
    private float mDensity = 0;
    private ColorViewButton mPreview;
    private final Resources mResources;
    private int mDefaultValue = Color.BLACK;
    private int mResetColor1 = Color.TRANSPARENT;
    private int mResetColor2 = Color.TRANSPARENT;
    private String mResetColor1Title = null;
    private String mResetColor2Title = null;
    private int mValue;
    private boolean mAlphaSliderVisible = true;

    public ColorPickerPreference(Context context) {
        this(context, null);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs) {
        this(context, attrs, R.attr.colorPreferenceStyle);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ColorPickerPreference(Context context, AttributeSet attrs, int defStyleAttr,
            int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        mResources = context.getResources();
        setOnPreferenceClickListener(this);

        if (attrs != null) {
            TypedArray a = context.obtainStyledAttributes(
                    attrs, R.styleable.ColorPickerPreference, defStyleAttr, defStyleRes);
            mDefaultValue = a.getColor(R.styleable.ColorPickerPreference_defaultColor,
                    Color.TRANSPARENT);
            mResetColor1 = a.getColor(R.styleable.ColorPickerPreference_resetColor1,
                    Color.TRANSPARENT);
            mResetColor2 = a.getColor(R.styleable.ColorPickerPreference_resetColor2,
                    Color.TRANSPARENT);
            mResetColor1Title = a.getString(R.styleable.ColorPickerPreference_resetColor1Title);
            mResetColor2Title = a.getString(R.styleable.ColorPickerPreference_resetColor2Title);
            mAlphaSliderVisible = a.getBoolean(
                    R.styleable.ColorPickerPreference_alphaSliderVisible, true);
            a.recycle();

            if (mDefaultValue == Color.TRANSPARENT) {
                String defaultValue = attrs.getAttributeValue(ANDROID_NS, DEFAULT_VALUE);
                if (defaultValue != null) {
                    if (defaultValue.startsWith("#")) {
                        try {
                            mDefaultValue = convertToColorInt(defaultValue);
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Wrong color: " + defaultValue);
                        }
                    } else {
                        int resourceId = attrs.getAttributeResourceValue(ANDROID_NS, DEFAULT_VALUE, Color.TRANSPARENT);
                        if (resourceId != 0) {
                            mDefaultValue = mResources.getInteger(resourceId);
                        }
                    }
                }
            }
            if (mDefaultValue == Color.TRANSPARENT) {
                mDefaultValue = Color.BLACK;
            }

            mValue = mDefaultValue;
        }
        setLayoutResource(R.layout.preference_color_picker);
        setWidgetLayoutResource(R.layout.preference_widget_color_picker);
    }

    @Override
    protected void onSetInitialValue(boolean restoreValue, Object defaultValue) {
        onColorChanged(restoreValue ? getValue() : (Integer) defaultValue);
    }

    @Override
    public void onBindViewHolder(PreferenceViewHolder view) {
        super.onBindViewHolder(view);
        mViewHolder = view;

       if (view != null) {
            mPreview = (ColorViewButton) view.findViewById(R.id.color_picker_widget);
        }
        if (mPreview != null) {
            TypedValue tv = new TypedValue();
            int borderColor;
            getContext().getTheme().resolveAttribute(android.R.attr.colorControlHighlight, tv, true);
            if (tv.type >= TypedValue.TYPE_FIRST_COLOR_INT && tv.type <= TypedValue.TYPE_LAST_COLOR_INT) {
                borderColor = tv.data;
            } else {
                borderColor = getContext().getColor(tv.resourceId);
            }
            mPreview.setBorderColor(borderColor);
        }

        setPreview();
    }

    private void setPreview() {
        if (mViewHolder == null)
            return;

        widgetFrameView = ((LinearLayout) mViewHolder
                .findViewById(android.R.id.widget_frame));

        if (widgetFrameView == null)
            return;
        widgetFrameView.setVisibility(View.VISIBLE);
        widgetFrameView.setPadding(
                widgetFrameView.getPaddingLeft(),
                widgetFrameView.getPaddingTop(),
                (int) (mDensity * 8),
                widgetFrameView.getPaddingBottom()
        );
        if (mPreview != null) {
            mPreview.setColor(mValue);
        }

    }

    private int getValue() {
        try {
            if (isPersistent()) {
                mValue = getPersistedInt(mDefaultValue);
            }
        } catch (ClassCastException e) {
            mValue = mDefaultValue;
        }

        return mValue;
    }

    @Override
    public void onColorChanged(int color) {
        if (isPersistent()) {
            persistInt(color);
        }
        mValue = color;
        try {
            getOnPreferenceChangeListener().onPreferenceChange(this, color);
        } catch (NullPointerException e) {
        }
        setPreview();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (mResetColor1 == Color.TRANSPARENT) {
            if (mResetColor2 != Color.TRANSPARENT) {
                mResetColor2 = Color.TRANSPARENT;
                Log.w(TAG + ".onPreferenceClick",
                        "Reset color 1 has not been set, ignore reset color 2 value");
            }
            if (mResetColor1Title != null) {
                mResetColor1Title = null;
                Log.w(TAG + ".onPreferenceClick",
                        "Reset color 1 has not been set, ignore reset color 1 title");
            }
            if (mResetColor2Title != null) {
                mResetColor2Title = null;
                Log.w(TAG + ".onPreferenceClick",
                        "Reset color 1 has not been set, ignore reset color 2 title");
            }
        } else if (mResetColor2 == Color.TRANSPARENT) {
            if (mResetColor2Title != null) {
                mResetColor2Title = null;
                Log.w(TAG + ".onPreferenceClick",
                        "Reset color 2 has not been set, ignore reset color 2 title");
            }
        }
        showFragment(null);
        return false;
    }

    private void showFragment(Bundle state) {
        SettingsActivity sa = null;
        if (getContext() instanceof ContextThemeWrapper) {
            if (((ContextThemeWrapper) getContext()).getBaseContext() instanceof SettingsActivity) {
                sa = (SettingsActivity) ((ContextThemeWrapper) getContext()).getBaseContext();
            }
        }
        if (sa == null) {
            return;
        }

        Bundle arguments;
        if (state != null) {
            arguments = new Bundle(state);
        } else {
            SharedPreferences prefs =
                    sa.getSharedPreferences("color_picker_fragment", Activity.MODE_PRIVATE);
            boolean showHelpScreen = prefs.getBoolean("show_help_screen", true);
            arguments = new Bundle();

            arguments.putInt("new_color", mValue);
            arguments.putInt("old_color", mValue);
            arguments.putBoolean("help_screen_visible", showHelpScreen);
        }
        arguments.putInt("initial_color", mValue);
        arguments.putInt("reset_color_1", mResetColor1);
        arguments.putInt("reset_color_2", mResetColor2);
        arguments.putCharSequence("reset_color_1_title", mResetColor1Title);
        arguments.putCharSequence("reset_color_2_title", mResetColor2Title);
        arguments.putBoolean("alpha_slider_visible", mAlphaSliderVisible);

        mPickerFragment = new ColorPickerFragment();
        mPickerFragment.setArguments(arguments);
        mPickerFragment.setOnColorChangedListener(this);

        FragmentTransaction transaction = sa.getFragmentManager().beginTransaction();
        transaction.replace(R.id.main_content, mPickerFragment);
        transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        transaction.addToBackStack(":settings:prefs");
        transaction.setBreadCrumbTitle(R.string.color_picker_fragment_title);
        transaction.commitAllowingStateLoss();
    }

    public void setResetColors(int resetColor1, int resetColor2) {
        mResetColor1 = resetColor1;
        mResetColor2 = resetColor2;
    }

    public void setResetColor(int color) {
        mResetColor1 = color;
    }

    public void setResetColorsTitle(int title1ResId, int title2ResId) {
        mResetColor1Title = mResources.getString(title1ResId);
        mResetColor2Title = mResources.getString(title2ResId);
    }

    public void setResetColorsTitle(String title1, String title2) {
        mResetColor1Title = title1;
        mResetColor2Title = title2;
    }

    public void setResetColorTitle(int titleResId) {
        mResetColor1Title = mResources.getString(titleResId);
    }

    public void setResetColorTitle(String title) {
        mResetColor1Title = title;
    }

    /**
     * Toggle Alpha Slider visibility (by default it's disabled)
     * 
     * @param enable
     */
    public void setAlphaSliderVisible(boolean visible) {
        mAlphaSliderVisible = visible;
    }

    /**
     * For custom purposes. Not used by ColorPickerPreferrence
     *
     * set color preview value from outside
     * @author kufikugel
     */
    public void setNewPreviewColor(int color) {
        onColorChanged(color);
    }

    /**
     * For custom purposes. Not used by ColorPickerPreferrence
     * 
     * @param color
     * @author Unknown
     */
    public static String convertToARGB(int color) {
        return ColorPickerHelper.convertToARGB(color);
    }

    /**
     * Converts a aarrggbb- or rrggbb color string to a color int
     * 
     * @param argb
     * @throws NumberFormatException
     * @author Unknown
     */
    public static int convertToColorInt(String argb) {
        return ColorPickerHelper.convertToColorInt(argb);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (mPickerFragment == null || !mPickerFragment.isVisible()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.isFragmentVisible = true;
        myState.fragmentState = mPickerFragment.getState();
        mPickerFragment.onSaveInstanceState(new Bundle());
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        if (myState.isFragmentVisible) {
            showFragment(myState.fragmentState);
        }
    }

    private static class SavedState extends BaseSavedState {
        boolean isFragmentVisible;
        Bundle fragmentState;

        public SavedState(Parcel source) {
            super(source);
            isFragmentVisible = source.readInt() == 1;
            fragmentState = source.readBundle();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(isFragmentVisible ? 1 : 0);
            dest.writeBundle(fragmentState);
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
