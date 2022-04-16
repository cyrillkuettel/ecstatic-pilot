package com.example.android.testingandroidxpreferences;

/*
created this based on the implementation in the library here:
https://github.com/h6ah4i/android-numberpickerprefcompat

added default value to set in the numberPickerWheel, canceled the unitTextView
 */

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import androidx.annotation.NonNull;
import androidx.preference.DialogPreference;

public class NumberPickerPreference extends DialogPreference {
    // the values to use for the NumberPicker
    private int mValue = 0;
    private int mDefaultValue = 5;
    private int mMaxValue = 7;
    private int mMinValue = 1;
    private boolean mWrapSelectorWheel = false; // enable or disable the 'circular behavior' - set on false

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs, defStyleAttr, defStyleRes);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr, 0);
    }

    public NumberPickerPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0, 0);
    }

    public NumberPickerPreference(Context context) {
        super(context);
        init(context, null, 0, 0);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        TypedArray ta = context.obtainStyledAttributes(
                attrs, R.styleable.Number_Picker_attrs, defStyleAttr, defStyleRes);
        mMinValue = ta.getInt(R.styleable.Number_Picker_attrs_np_minValue, mMinValue);
        mMaxValue = ta.getInt(R.styleable.Number_Picker_attrs_np_maxValue, mMaxValue);
        mDefaultValue = ta.getInt(R.styleable.Number_Picker_attrs_np_defaultValue, mDefaultValue);
        mWrapSelectorWheel = ta.getBoolean(R.styleable.Number_Picker_attrs_np_wrapSelector, mWrapSelectorWheel);
        ta.recycle();
        setDialogLayoutResource(R.layout.number_picker_layout);
    }

    public void setValue(int value) {
        final boolean wasBlocking = shouldDisableDependents();
        mValue = value;
        persistInt(value);
        final boolean isBlocking = shouldDisableDependents();
        if (isBlocking != wasBlocking) {
            notifyDependencyChange(isBlocking);
        }
    }

    public int getValue() {
        return mValue;
    }

    public int getDefaultValue() {
        return mDefaultValue;
    }

    public void setDefaultValue(int defaultValue) {
        mDefaultValue = defaultValue;
    }

    public int getMinValue() {
        return mMinValue;
    }

    public void setMinValue(int minValue) {
        mMinValue = minValue;
    }

    public int getMaxValue() {
        return mMaxValue;
    }

    public void setMaxValue(int maxValue) {
        mMaxValue = maxValue;
    }

    public boolean getSelectorWheelValue() {
        return mWrapSelectorWheel;
    }

    public void setSelectorWheelValue(boolean wrapSelectorWheel) {
        mWrapSelectorWheel = wrapSelectorWheel;
    }

    @NonNull
    @Override
    protected Object onGetDefaultValue(TypedArray a, int index) {
        return a.getInt(index, mDefaultValue);
    }

    @Override
    protected void onSetInitialValue(Object defaultValue) {
        setValue( mDefaultValue);
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (isPersistent()) {
            return superState;
        }

        final SavedState myState = new SavedState(superState);
        myState.value = getValue();
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            super.onRestoreInstanceState(state);
            return;
        }

        final SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        setValue(myState.value);
    }

    private static class SavedState extends BaseSavedState {
        int value;

        public SavedState(Parcel source) {
            super(source);
            value = source.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(value);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }


}
