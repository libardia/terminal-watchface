package info.tonyl.terminal.config;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.support.wearable.complications.ProviderChooserIntent;
import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.wear.widget.WearableRecyclerView;

import info.tonyl.terminal.R;

public class ConfigActivity extends Activity {
    private static final String TAG = ConfigActivity.class.getSimpleName();

    static final int COMPLICATION_CONFIG_REQUEST_CODE = 1001;

    private WearableRecyclerView mWearableRecyclerView;
    private ConfigRecyclerViewAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.config_layout);

        mWearableRecyclerView = findViewById(R.id.wearable_recycler_view);
        mAdapter = new ConfigRecyclerViewAdapter(getApplicationContext());

        // Aligns the first and last items on the list vertically centered on the screen.
        mWearableRecyclerView.setEdgeItemsCenteringEnabled(true);

        mWearableRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Improves performance because we know changes in content do not change the layout size of
        // the RecyclerView.
        mWearableRecyclerView.setHasFixedSize(true);

        mWearableRecyclerView.setAdapter(mAdapter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == COMPLICATION_CONFIG_REQUEST_CODE
                && resultCode == RESULT_OK) {

            // Retrieves information for selected Complication provider.
            ComplicationProviderInfo complicationProviderInfo =
                    data.getParcelableExtra(ProviderChooserIntent.EXTRA_PROVIDER_INFO);
            Log.d(TAG, "Provider: " + complicationProviderInfo);

            // Updates preview with new complication information for selected complication id.
            // Note: complication id is saved and tracked in the adapter class.
            mAdapter.updateSelectedComplication(complicationProviderInfo);
        }
    }
}