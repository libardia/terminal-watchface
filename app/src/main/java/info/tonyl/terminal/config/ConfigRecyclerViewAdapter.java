package info.tonyl.terminal.config;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import info.tonyl.terminal.R;
import info.tonyl.terminal.TerminalWatchFace;
import info.tonyl.terminal.constants.Settings;

public class ConfigRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private Activity mConfigActivity;
    private List<ConfigItem> mConfigItems;

    public static final int WEATHER_SETTING = 0;

    public ConfigRecyclerViewAdapter(Context context, Activity configActivity) {
        mContext = context;
        SharedPreferences prefs = mContext.getSharedPreferences(
                Settings.PREF_NAME, Context.MODE_PRIVATE);
        mConfigActivity = configActivity;
        mConfigItems = new ArrayList<>();
        mConfigItems.add(new ConfigItem(
                mContext.getString(R.string.weather_comp_setting),
                R.drawable.ic_landscape_white,
                getPrefString(prefs, Settings.SETTING_WEATHER, R.string.unset_config_value),
                ConfigItem.TEXT_ONLY_TYPE,
                WEATHER_SETTING));
    }

    private String getPrefString(SharedPreferences prefs, String key, int def) {
        return prefs.getString(key, mContext.getString(def));
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecyclerView.ViewHolder holder = null;

        switch (viewType) {
            case ConfigItem.TEXT_ONLY_TYPE:
                holder = new ConfigItemViewHolder(
                        LayoutInflater.from(mContext).inflate(
                                R.layout.config_item, parent, false));
        }

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ConfigItemViewHolder configHolder = (ConfigItemViewHolder) holder;
        configHolder.fill(mConfigItems.get(position));
    }

    @Override
    public int getItemCount() {
        return mConfigItems.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mConfigItems.get(position).getType();
    }

    public void setValueFor(int settingId, String value) {
        for (int i = 0; i < mConfigItems.size(); i++) {
            ConfigItem ci = mConfigItems.get(i);
            if (ci.getWhich() == settingId) {
                ci.setValue(value);
                notifyItemChanged(i);
            }
        }
    }

    public class ConfigItemViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView mDescriptionView;
        TextView mValueView;
        ImageView mIconView;
        ConfigItem mConfigItem;

        public ConfigItemViewHolder(@NonNull View itemView) {
            super(itemView);
            itemView.setOnClickListener(this);

            mDescriptionView = itemView.findViewById(R.id.description);
            mDescriptionView.setTextColor(Color.WHITE);

            mValueView = itemView.findViewById(R.id.value);
            mValueView.setTextColor(Color.rgb(0x80, 0x80, 0x80));

            mIconView = itemView.findViewById(R.id.icon);
        }

        public void fill(ConfigItem configItem) {
            mConfigItem = configItem;
            mDescriptionView.setText(mConfigItem.getDescription());
            mValueView.setText(mConfigItem.getValue());
            mIconView.setImageResource(mConfigItem.getIconId());
        }

        @Override
        public void onClick(View v) {
            switch (mConfigItem.getWhich()) {
                case WEATHER_SETTING:
                    ComponentName watchfaceComponentName = new ComponentName(mConfigActivity, TerminalWatchFace.class);
                    Intent chooser = ComplicationHelperActivity.createProviderChooserHelperIntent(
                            mConfigActivity,
                            watchfaceComponentName,
                            TerminalWatchFace.TEMP_COMP_ID,
                            ComplicationData.TYPE_SHORT_TEXT);

                    mConfigActivity.startActivityForResult(chooser, ConfigActivity.WEATHER_COMPLICATION_CONFIG_CODE);
                    break;
            }
        }
    }
}
