package info.tonyl.terminal.config;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;

import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import info.tonyl.terminal.R;
import info.tonyl.terminal.constants.Settings;

public class ConfigActivity extends Activity {
    private static final String TAG = ConfigActivity.class.getSimpleName();

    static final int WEATHER_COMPLICATION_CONFIG_CODE = 1001;

    private WearableRecyclerView mWearableRecyclerView;
    private ConfigRecyclerViewAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.config_layout);

        mWearableRecyclerView = findViewById(R.id.wearable_recycler_view);
        mAdapter = new ConfigRecyclerViewAdapter(getApplicationContext(), this);

        // Aligns the first and last items on the list vertically centered on the screen.
        mWearableRecyclerView.setEdgeItemsCenteringEnabled(true);

        mWearableRecyclerView.setLayoutManager(new WearableLinearLayoutManager(this));

        // Improves performance because we know changes in content do not change the layout size of
        // the RecyclerView.
        mWearableRecyclerView.setHasFixedSize(true);

        mWearableRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == WEATHER_COMPLICATION_CONFIG_CODE
                && resultCode == RESULT_OK) {
            // Retrieves information for selected Complication provider.
            ComplicationProviderInfo complicationProviderInfo =
                    data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO);

            String newValue;
            if (complicationProviderInfo != null) {
                newValue = complicationProviderInfo.providerName;
            } else {
                newValue = getString(R.string.unset_config_value);
            }

            // Set back the current value in the config item
            SharedPreferences sp = getApplicationContext().getSharedPreferences(
                    Settings.PREF_NAME, Context.MODE_PRIVATE);

            sp.edit()
                    .putString(Settings.SETTING_WEATHER, newValue)
                    .apply();

            mAdapter.setValueFor(ConfigRecyclerViewAdapter.WEATHER_SETTING, newValue);
        }
    }
}