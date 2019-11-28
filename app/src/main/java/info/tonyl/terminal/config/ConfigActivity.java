package info.tonyl.terminal.config;

import android.app.Activity;
import android.app.RemoteInput;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;

import androidx.wear.widget.WearableLinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import info.tonyl.terminal.R;
import info.tonyl.terminal.TerminalWatchFace;
import info.tonyl.terminal.constants.RemoteInputConstants;
import info.tonyl.terminal.constants.Settings;

public class ConfigActivity extends Activity {
    private static final String TAG = ConfigActivity.class.getSimpleName();

    public static final int WEATHER_COMPLICATION_CONFIG_CODE = 1001;
    public static final int USERNAME_CONFIG_CODE = 1002;

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
        if (requestCode == WEATHER_COMPLICATION_CONFIG_CODE && resultCode == RESULT_OK) {
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
            TerminalWatchFace.getPrefs().edit()
                    .putString(Settings.SETTING_WEATHER, newValue)
                    .apply();

            mAdapter.setValueFor(ConfigRecyclerViewAdapter.WEATHER_SETTING, newValue);
        } else if (requestCode == USERNAME_CONFIG_CODE && resultCode == RESULT_OK) {
            Bundle results = RemoteInput.getResultsFromIntent(data);
            String username = results.getCharSequence(RemoteInputConstants.USERNAME_INPUT_KEY).toString();

            // Set the username into the config
            TerminalWatchFace.getPrefs().edit()
                    .putString(Settings.SETTING_USERNAME, username)
                    .apply();

            mAdapter.setValueFor(ConfigRecyclerViewAdapter.USERNAME_SETTING, username);
            TerminalWatchFace.updateUsernameMessages(getApplicationContext());
        }
    }
}