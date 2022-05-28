package li.garteroboter.pren.preferences.plantnumber;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.NumberPicker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceDialogFragmentCompat;

import li.garteroboter.pren.R;

/*
created this based on the implementation in the library here:
https://github.com/h6ah4i/android-numberpickerprefcompat
added default value to set in the numberPickerWheel, canceled the unitTextView
 */

public class NumberPickerPreferenceDialogFragment extends PreferenceDialogFragmentCompat {
    private static final String SAVE_STATE_VALUE = "NumberPickerPreferenceDialogFragment.value";
    private static final String SHARED_PREFERENCES = "testandroidxpreferences"; // name for sharedPreferences location
    private NumberPicker mNumberPicker;
    private int mValue;

    @NonNull
    public static NumberPickerPreferenceDialogFragment newInstance(@NonNull String key) {
        final NumberPickerPreferenceDialogFragment fragment = new NumberPickerPreferenceDialogFragment();
        final Bundle args = new Bundle(1);
        args.putString(ARG_KEY, key);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            // if it is first run after installation - get the default value
            mValue = getNumberPickerPreference().getDefaultValue();
        } else {
            // if not - there is a saved value
            mValue = savedInstanceState.getInt(SAVE_STATE_VALUE);
        }
    }

    // get the NumberPickerPreference instance
    private NumberPickerPreference getNumberPickerPreference() {
        return (NumberPickerPreference) getPreference();
    }

    // save the value
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVE_STATE_VALUE, mValue);
    }

    @Override
    protected void onBindDialogView(@NonNull View view) {
        super.onBindDialogView(view);
        // finding the numberPicker view
        mNumberPicker = view.findViewById(R.id.number_picker);
        // throw an IllegalStateException if there is no NumberPicker view
        if (mNumberPicker == null) {
            throw new IllegalStateException("Dialog view must contain an NumberPicker with id");
        }
        // set the values for the NumberPicker view - min, max, default/value to show and if scrollable
        mNumberPicker.setMinValue(getNumberPickerPreference().getMinValue());
        mNumberPicker.setMaxValue(getNumberPickerPreference().getMaxValue());
        mValue = Integer.parseInt(restorePreferences());
        mNumberPicker.setValue(mValue);
        mNumberPicker.setWrapSelectorWheel(getNumberPickerPreference().getSelectorWheelValue());
    }

    // This method to store the custom preferences changes
    private void savePreferences(String value) {
        Activity activity = getActivity();
        SharedPreferences myPreferences;
        if (activity != null) {
            myPreferences = activity.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
            SharedPreferences.Editor myEditor = myPreferences.edit();
            myEditor.putString("number_picker_preference", value);
            myEditor.apply();
        }
    }

    // This method to restore the custom preferences data
    private String restorePreferences() {
        Activity activity = getActivity();
        SharedPreferences myPreferences;
        if (activity != null) {
            myPreferences = activity.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE);
            if (myPreferences.contains("number_picker_preference"))
                return myPreferences.getString("number_picker_preference", "");
            else return "";
        } else return "";
    }

    // what to do when the dialog is closed
    @Override
    public void onDialogClosed(boolean positiveResult) {
        if (positiveResult) {
            mNumberPicker.clearFocus();
            final String value = String.valueOf(mNumberPicker.getValue());
            savePreferences(value);

            if (getNumberPickerPreference().callChangeListener(value)) {
                getNumberPickerPreference().setValue(Integer.parseInt(value));
            }
        }
    }
}
