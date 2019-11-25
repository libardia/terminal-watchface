package info.tonyl.terminal.config;

import android.content.Context;
import android.support.wearable.complications.ComplicationProviderInfo;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import info.tonyl.terminal.R;

public class ConfigRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private Context mContext;
    private List<ConfigItem> mConfigItems;

    public ConfigRecyclerViewAdapter(Context context) {
        mContext = context;
        mConfigItems = new ArrayList<>();
        mConfigItems.add(new ConfigItem(
                mContext.getString(R.string.weather_comp_setting),
                ConfigItem.TEXT_ONLY_TYPE));
        for (int i = 0; i < 20; i++) {
            mConfigItems.add(new ConfigItem(
                    mContext.getString(R.string.weather_comp_setting),
                    ConfigItem.TEXT_ONLY_TYPE));
        }
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

    public void updateSelectedComplication(ComplicationProviderInfo info) {

    }

    public class ConfigItemViewHolder extends RecyclerView.ViewHolder {
        public ConfigItemViewHolder(@NonNull View itemView) {
            super(itemView);
        }

        public void fill(ConfigItem configItem) {
            TextView descView = itemView.findViewById(R.id.description);
            descView.setText(configItem.getDescription());
        }
    }
}
