package info.tonyl.terminal.config;

import android.content.Context;
import android.content.SharedPreferences;

import info.tonyl.terminal.R;

public class ConfigItem {

    public static final int TEXT_ONLY_TYPE = 0;

    private String mDescription;
    private int mType;
    private int mIconId;
    private int mWhich;
    private String mValue;

    public ConfigItem(String description, int iconId, String value, int type, int which) {
        mDescription = description;
        mType = type;
        mIconId = iconId;
        mWhich = which;
        mValue = value;
    }

    public String getDescription() {
        return mDescription;
    }

    public int getType() {
        return mType;
    }

    public int getIconId() {
        return mIconId;
    }

    public int getWhich() {
        return mWhich;
    }

    public void setValue(String value) {
        mValue = value;
    }

    public String getValue() {
        return mValue;
    }
}
