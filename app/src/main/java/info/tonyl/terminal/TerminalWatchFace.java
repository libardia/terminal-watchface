package info.tonyl.terminal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationText;
import android.support.wearable.complications.SystemProviders;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextPaint;
import android.view.SurfaceHolder;

import androidx.core.content.res.ResourcesCompat;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import info.tonyl.terminal.constants.ComplicationDataConstants;
import info.tonyl.terminal.constants.Settings;
import info.tonyl.terminal.constants.TemperatureConstants;

/**
 * Analog watch face with a ticking second hand. In ambient mode, the second hand isn't
 * shown. On devices with low-bit ambient mode, the hands are drawn without anti-aliasing in ambient
 * mode. The watch face is drawn with less contrast in mute mode.
 * <p>
 * Important Note: Because watch face apps do not have a default Activity in
 * their project, you will need to set your Configurations to
 * "Do not launch Activity" for both the Wear and/or Application modules. If you
 * are unsure how to do this, please review the "Run Starter project" section
 * in the Google Watch Face Code Lab:
 * https://codelabs.developers.google.com/codelabs/watchface/index.html#0
 */
public class TerminalWatchFace extends CanvasWatchFaceService {

    /*
     * Updates rate in milliseconds for interactive mode. We update once a second to advance the
     * second hand.
     */
    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1);

    /**
     * Handler message id for updating the time periodically in interactive mode.
     */
    private static final int MSG_UPDATE_TIME = 0;

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private static class EngineHandler extends Handler {
        private final WeakReference<Engine> mWeakReference;

        public EngineHandler(TerminalWatchFace.Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            TerminalWatchFace.Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case MSG_UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }

    public static final int BATTERY_COMP_ID = 0;
    public static final int STEP_COMP_ID = 1;
    public static final int TEMP_COMP_ID = 2;
    public final int[] COMP_IDS = {BATTERY_COMP_ID, STEP_COMP_ID, TEMP_COMP_ID};

    private static String mStartMessage;
    private static String mEndMessage;
    private static SharedPreferences mPrefs;

    public static void updateUsernameMessages(Context context) {
        String username = getPrefs().getString(
                Settings.SETTING_USERNAME, context.getString(R.string.default_username));
        mStartMessage = username + context.getString(R.string.start_message_postfix);
        mEndMessage = username + context.getString(R.string.end_message_postfix);
    }

    public static SharedPreferences getPrefs() {
        return mPrefs;
    }

    private class Engine extends CanvasWatchFaceService.Engine {

        private final Handler mUpdateTimeHandler = new EngineHandler(this);
        private Calendar mCalendar;
        private final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            }
        };
        private boolean mRegisteredTimeZoneReceiver = false;

        // RELEVANT =======================================
        private static final float TEXT_SIZE_RATIO = 0.065f;
        private static final float TEXT_X_RATIO = 0.1f;

        private static final int BASE_TEXT_COLOR = Color.WHITE;
        private static final int TIME_COLOR = Color.GREEN;
        private static final int DATE_COLOR = Color.CYAN;
        private static final int BATTERY_COLOR = Color.MAGENTA;
        private static final int STEP_COLOR = Color.RED;
        private static final int TEMP_COLOR = Color.YELLOW;
        private int[] PAINT_COLORS = {BASE_TEXT_COLOR, TIME_COLOR, DATE_COLOR, BATTERY_COLOR, STEP_COLOR, TEMP_COLOR};

        private static final int AMBIENT_COLOR = Color.GRAY;

        private boolean mAmbient;
        private TextPaint[] mTextPaints;
        private static final int NUM_PAINTS = 6;
        private static final int BASE_PAINT = 0;
        private static final int TIME_PAINT = 1;
        private static final int DATE_PAINT = 2;
        private static final int BATTERY_PAINT = 3;
        private static final int STEP_PAINT = 4;
        private static final int TEMP_PAINT = 5;

        private List<String> mMessages;
        private float mTextX;
        private float mExtraTextX;
        private float mTextY;
        private float mCenterX;
        private float mCenterY;
        private String mBatteryVisual = ComplicationDataConstants.NO_INFO;
        private String mStepString = ComplicationDataConstants.NO_INFO;
        private String mTempString = ComplicationDataConstants.NO_INFO;

        private static final String HM_FORMAT_STRING = "hh:mm:";
        private static final String TZ_FORMAT_STRING = " z";
        private static final String D_FORMAT_STRING = "yyyy-MM-dd EEE";
        private static final String AMPM_FORMAT_STRING = " a";
        private SimpleDateFormat mHourMinuteFormat = new SimpleDateFormat(HM_FORMAT_STRING, Locale.getDefault());
        private SimpleDateFormat mTimezoneFormat = new SimpleDateFormat(TZ_FORMAT_STRING, Locale.getDefault());
        private SimpleDateFormat mDateFormat = new SimpleDateFormat(D_FORMAT_STRING, Locale.getDefault());
        private SimpleDateFormat mAmPmFormat = new SimpleDateFormat(AMPM_FORMAT_STRING, Locale.getDefault());

        private int mMinute;
        private int mHour;
        private TimeZone mTimezone;
        private String mHourMinuteString;
        private String mTimezoneString;
        private String mAmPmString;
        private StringBuilder mTimeSb = new StringBuilder();

        private int mDay;
        private String mDateString;
        // ================================================

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            mPrefs = getApplication().getSharedPreferences(Settings.PREF_NAME, MODE_PRIVATE);
            updateUsernameMessages(getApplicationContext());

            mCalendar = Calendar.getInstance();
            mTextPaints = new TextPaint[NUM_PAINTS];

            setActiveComplications(COMP_IDS);

            setWatchFaceStyle(new WatchFaceStyle.Builder(TerminalWatchFace.this)
                    .build());
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            mPrefs = null;
            super.onDestroy();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            mAmbient = inAmbientMode;

            if (mAmbient) {
                for (TextPaint p : mTextPaints) {
                    p.setAntiAlias(false);
                    p.setColor(AMBIENT_COLOR);
                }
            } else {
                for (int i = 0; i < NUM_PAINTS; i++) {
                    mTextPaints[i].setAntiAlias(true);
                    mTextPaints[i].setColor(PAINT_COLORS[i]);
                }
            }

            updateTimer();
        }

        @Override
        public void onComplicationDataUpdate(
                int complicationId, ComplicationData complicationData) {
            switch (complicationId) {
                case BATTERY_COMP_ID:
                    float battery = complicationData.getValue();
                    int length = Math.round(battery / 10);
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < 10; i++) {
                        sb.append(i < length ? "#" : ".");
                    }
                    sb.append("] ");
                    sb.append(String.format(Locale.getDefault(), "%02.0f", battery));
                    sb.append("%");
                    mBatteryVisual = sb.toString();
                    break;
                case STEP_COMP_ID:
                    mStepString = getShortText(complicationData);
                    break;
                case TEMP_COMP_ID:
                    mTempString = getShortText(complicationData);
                    // Only supports Fahrenheit at the moment
                    mTempString = mTempString.replaceFirst(TemperatureConstants.TEMPERATURE_PATTERN_F, TemperatureConstants.TEMPERATURE_REPLACEMENT);
                    break;
            }
            invalidate();
        }

        private String getShortText(ComplicationData data) {
            // It is truly ridiculous the effort you have to go through just to get the goddamn string out of this thing
            ComplicationText text = data.getShortText();
            if (text == null) {
                return ComplicationDataConstants.NO_INFO;
            }
            return text.getText(getApplicationContext(), System.currentTimeMillis()).toString();
        }

        @Override
        public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            super.onSurfaceChanged(holder, format, width, height);

            // Calculate screen center
            mCenterX = width / 2f;
            mCenterY = height / 2f;

            // Init paint objects
            for (int i = 0; i < NUM_PAINTS; i++) {
                TextPaint p = new TextPaint();
                p.setTypeface(ResourcesCompat.getFont(getApplicationContext(), R.font.consolab));
                p.setTextSize(height * TEXT_SIZE_RATIO);
                p.setAntiAlias(true);
                p.setTextAlign(Paint.Align.LEFT);
                p.setColor(PAINT_COLORS[i]);

                mTextPaints[i] = p;
            }

            // Set up text and calculate position values
            mMessages = new ArrayList<>();
            mMessages.add("[TIME] ");
            mMessages.add("[DATE] ");
            mMessages.add("[BATT] ");
            mMessages.add("[STEP] ");
            mMessages.add("[TEMP] ");

            // messages.size - 1 is how many are in this list, but we need two more, the start and end messages
            float totalTextHeight = ((mMessages.size() - 1) + 2) * mTextPaints[BASE_PAINT].getFontSpacing();

            mTextX = mCenterX - mTextPaints[BASE_PAINT].measureText(mMessages.get(0)) / 2;
            mTextX = width * TEXT_X_RATIO;
            mExtraTextX = mTextX + mTextPaints[BASE_PAINT].measureText(mMessages.get(1));
            mTextY = mCenterY - totalTextHeight / 2;

            // Init complications
            setDefaultSystemComplicationProvider(BATTERY_COMP_ID, SystemProviders.WATCH_BATTERY, ComplicationData.TYPE_RANGED_VALUE);
            setDefaultSystemComplicationProvider(STEP_COMP_ID, SystemProviders.STEP_COUNT, ComplicationData.TYPE_SHORT_TEXT);
            setDefaultComplicationProvider(TEMP_COMP_ID, null, ComplicationData.TYPE_EMPTY);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            // Clear screen
            canvas.drawColor(Color.BLACK);

            // Draw text
            float y = mTextY;

            // Draw the first line (with the username in it)
            canvas.drawText(mStartMessage, mTextX, y, mTextPaints[BASE_PAINT]);
            y += mTextPaints[BASE_PAINT].getFontSpacing();

            for (int i = 0; i < mMessages.size(); i++) {
                String message = mMessages.get(i);
                canvas.drawText(message, mTextX, y, mTextPaints[BASE_PAINT]);
                switch (i) {
                    case 0:
                        canvas.drawText(makeTime(), mExtraTextX, y, mTextPaints[TIME_PAINT]);
                        break;
                    case 1:
                        canvas.drawText(makeDate(), mExtraTextX, y, mTextPaints[DATE_PAINT]);
                        break;
                    case 2:
                        canvas.drawText(mBatteryVisual, mExtraTextX, y, mTextPaints[BATTERY_PAINT]);
                        break;
                    case 3:
                        canvas.drawText(mStepString, mExtraTextX, y, mTextPaints[STEP_PAINT]);
                        break;
                    case 4:
                        canvas.drawText(mTempString, mExtraTextX, y, mTextPaints[TEMP_PAINT]);
                        break;
                }
                y += mTextPaints[BASE_PAINT].getFontSpacing();
            }

            // Draw the last line (also has the username in it)
            canvas.drawText(mEndMessage, mTextX, y, mTextPaints[BASE_PAINT]);
        }

        private String makeTime() {
            // Reset the time string buffer (without making a new one)
            mTimeSb.setLength(0);

            // Get current time values
            Date d = mCalendar.getTime();
            int m = mCalendar.get(Calendar.MINUTE);
            int h = mCalendar.get(Calendar.HOUR);
            TimeZone tz = TimeZone.getDefault();

            // If the timezone has changed (or it switched in or out of DST)...
            if (!tz.equals(mTimezone) || mTimezone.inDaylightTime(d) != tz.inDaylightTime(d)) {
                // Update the format objects (because the locale probably changed)
                mHourMinuteFormat = new SimpleDateFormat(HM_FORMAT_STRING, Locale.getDefault());
                mTimezoneFormat = new SimpleDateFormat(TZ_FORMAT_STRING, Locale.getDefault());
                mDateFormat = new SimpleDateFormat(D_FORMAT_STRING, Locale.getDefault());
                mAmPmFormat = new SimpleDateFormat(AMPM_FORMAT_STRING, Locale.getDefault());

                // Regenerate the calendar, and cache the timezone
                mCalendar = Calendar.getInstance();
                mTimezone = mCalendar.getTimeZone();
                d = mCalendar.getTime();
                m = mCalendar.get(Calendar.MINUTE);
                h = mCalendar.get(Calendar.HOUR);
                mTimezoneString = mTimezoneFormat.format(d);
            }

            // If the minute or hour has changed...
            // (hour check only necessary if the timezone changed)
            if (m != mMinute || h != mHour) {
                // Cache it, and regenerate the hours, minutes, and AM/PM
                mMinute = m;
                mHour = h;
                mHourMinuteString = mHourMinuteFormat.format(d);
                mAmPmString = mAmPmFormat.format(d);
            }


            // Hour and minutes are first in the string
            mTimeSb.append(mHourMinuteString);

            // If we're in interactive mode...
            if (!mAmbient) {
                // Get the seconds, and make it a string
                String sec = Integer.toString(mCalendar.get(Calendar.SECOND));
                // but pad it with a 0 if it's one digit
                if (sec.length() < 2) {
                    mTimeSb.append("0");
                }
                // Then stick it on the final string
                mTimeSb.append(sec);
            } else {
                // If we're in ambient mode, skip all that and just append dashes
                mTimeSb.append("--");
            }

            // Next is the AM/PM string
            mTimeSb.append(mAmPmString);

            // And finally the timezone
            mTimeSb.append(mTimezoneString);

            return mTimeSb.toString();
        }

        private String makeDate() {
            int day = mCalendar.get(Calendar.DAY_OF_MONTH);
            if (day != mDay) {
                mDay = day;
                mDateString = mDateFormat.format(mCalendar.getTime());
            }

            return mDateString;
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);

            if (visible) {
                registerReceiver();
                /* Update time zone in case it changed while we weren't visible. */
                mCalendar.setTimeZone(TimeZone.getDefault());
                invalidate();
            } else {
                unregisterReceiver();
            }

            /* Check and trigger whether or not timer should be running (only in active mode). */
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            TerminalWatchFace.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            TerminalWatchFace.this.unregisterReceiver(mTimeZoneReceiver);
        }

        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
            if (shouldTimerBeRunning()) {
                mUpdateTimeHandler.sendEmptyMessage(MSG_UPDATE_TIME);
            }
        }

        private boolean shouldTimerBeRunning() {
            return isVisible() && !mAmbient;
        }

        private void handleUpdateTimeMessage() {
            invalidate();
            if (shouldTimerBeRunning()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS
                        - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(MSG_UPDATE_TIME, delayMs);
            }
        }
    }
}
