package info.tonyl.terminal.config;

public class ConfigItem {

    public static final int TEXT_ONLY_TYPE = 0;

    private String mDescription;
    private int mType;

    public ConfigItem(String description, int type) {
        mDescription = description;
        mType = type;
    }

    public String getDescription() {
        return mDescription;
    }

    public int getType() {
        return mType;
    }
}
