package info.tonyl.terminal.config;

import android.app.Activity;
import android.app.RemoteInput;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationHelperActivity;
import android.support.wearable.input.RemoteInputIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import info.tonyl.terminal.BuildConfig;
import info.tonyl.terminal.R;
import info.tonyl.terminal.TerminalWatchFace;
import info.tonyl.terminal.constants.RemoteInputConstants;
import info.tonyl.terminal.constants.Settings;

public class ConfigRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private Activity mConfigActivity;
    private List<ConfigItem> mConfigItems;

    public static final int WEATHER_SETTING = 0;
    public static final int USERNAME_SETTING = 1;
    public static final int ABOUT_VERSION = 2;

    public ConfigRecyclerViewAdapter(Context context, Activity configActivity) {
        mContext = context;
        mConfigActivity = configActivity;
        mConfigItems = new ArrayList<>();
        mConfigItems.add(new ConfigItem(
                mContext.getString(R.string.weather_comp_setting),
                R.drawable.ic_landscape_white,
                getPrefString(Settings.SETTING_WEATHER, R.string.unset_config_value),
                ConfigItem.TEXT_ONLY_TYPE,
                WEATHER_SETTING));
        mConfigItems.add(new ConfigItem(
                mContext.getString(R.string.username_setting),
                R.drawable.icn_styles,
                getPrefString(Settings.SETTING_USERNAME, R.string.default_username),
                ConfigItem.TEXT_ONLY_TYPE,
                USERNAME_SETTING));
        mConfigItems.add(new ConfigItem(
                mContext.getString(R.string.version_setting),
                R.drawable.icn_styles,
                BuildConfig.VERSION_NAME,
                ConfigItem.TEXT_ONLY_TYPE,
                ABOUT_VERSION));
    }

    private String getPrefString(String key, int def) {
        return TerminalWatchFace.getPrefs().getString(key, mContext.getString(def));
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
                case USERNAME_SETTING:
                    RemoteInput input = new RemoteInput.Builder(RemoteInputConstants.USERNAME_INPUT_KEY)
                            .setLabel(mContext.getString(R.string.username_setting))
                            .build();
                    Intent intent = new Intent(RemoteInputIntent.ACTION_REMOTE_INPUT);
                    intent.putExtra(RemoteInputIntent.EXTRA_REMOTE_INPUTS, new RemoteInput[]{input});
                    mConfigActivity.startActivityForResult(intent, ConfigActivity.USERNAME_CONFIG_CODE);
                    break;
            }
        }
    }
}
