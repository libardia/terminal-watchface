package com.example.terminal;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.complications.ComplicationData;
import android.support.wearable.complications.ComplicationText;
import android.support.wearable.complications.SystemProviders;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.TextPaint;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import androidx.core.content.res.ResourcesCompat;

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
        private final WeakReference<TerminalWatchFace.Engine> mWeakReference;

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
        private SimpleDateFormat mHourMinuteFormat = new SimpleDateFormat("hh:mm:", Locale.getDefault());
        private SimpleDateFormat mAmTimezoneFormat = new SimpleDateFormat(" a z", Locale.getDefault());
        private SimpleDateFormat mDateFormat = new SimpleDateFormat("yyyy-MM-dd EEE", Locale.getDefault());


        private static final float TEXT_SIZE_RATIO = 0.065f;
        private static final float TEXT_X_RATIO = 0.1f;

        private static final int BATTERY_COMP_ID = 0;
        private static final int STEP_COMP_ID = 1;
        private static final int NOTIF_COMP_ID = 2;
        private final int[] COMP_IDS = {BATTERY_COMP_ID, STEP_COMP_ID, NOTIF_COMP_ID};

        private static final int BASE_TEXT_COLOR = Color.WHITE;
        private static final int TIME_COLOR = Color.GREEN;
        private static final int DATE_COLOR = Color.CYAN;
        private static final int BATTERY_COLOR = Color.MAGENTA;
        private static final int STEP_COLOR = Color.RED;
        private static final int NOTIF_COLOR = Color.YELLOW;
        private int[] PAINT_COLORS = {BASE_TEXT_COLOR, TIME_COLOR, DATE_COLOR, BATTERY_COLOR, STEP_COLOR, NOTIF_COLOR};

        private static final int AMBIENT_COLOR = Color.GRAY;

        private boolean mAmbient;
        private TextPaint[] mTextPaints;
        private static final int NUM_PAINTS = 6;
        private static final int BASE_PAINT = 0;
        private static final int TIME_PAINT = 1;
        private static final int DATE_PAINT = 2;
        private static final int BATTERY_PAINT = 3;
        private static final int STEP_PAINT = 4;
        private static final int NOTIF_PAINT = 5;

        private List<String> messages;
        private float mTextX;
        private float mExtraTextX;
        private float mTextY;
        private float mCenterX;
        private float mCenterY;
        private String mBatteryVisual = "NO_INFO";
        private String mStepString = "NO_INFO";
        private String mNotifString = "NO_INFO";

        private int mMinute;
        private String mHourMinuteString;
        private String mAmTimezoneString;
        private StringBuilder mSb = new StringBuilder();

        private int mDay;
        private String mDateString;
        // ================================================

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            mCalendar = Calendar.getInstance();
            mTextPaints = new TextPaint[NUM_PAINTS];

            setActiveComplications(COMP_IDS);

            setWatchFaceStyle(new WatchFaceStyle.Builder(TerminalWatchFace.this)
                    .setHideNotificationIndicator(true)
                    .build());
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(MSG_UPDATE_TIME);
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
                    p.setColor(AMBIENT_COLOR);
                }
            } else {
                for (int i = 0; i < NUM_PAINTS; i++) {
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
                    float battery = (complicationData.getValue() - complicationData.getMinValue())
                            / (complicationData.getMaxValue() - complicationData.getMinValue());
                    int length = Math.round(battery * 10);
                    int percent = Math.round(battery * 100);
                    StringBuilder sb = new StringBuilder("[");
                    for (int i = 0; i < 10; i++) {
                        sb.append(i < length ? "#" : ".");
                    }
                    sb.append("] ");
                    sb.append(String.format(Locale.getDefault(), "%02d", percent));
                    sb.append("%");
                    mBatteryVisual = sb.toString();
                    break;
                case STEP_COMP_ID:
                    mStepString = getShortText(complicationData);
                    break;
                case NOTIF_COMP_ID:
                    mNotifString = getShortText(complicationData);
                    break;
            }
            invalidate();
        }

        private String getShortText(ComplicationData data) {
            // It is truly ridiculous the effort you have to go through just to get the goddamn string out of this thing
            ComplicationText text = data.getShortText();
            if (text == null) {
                return "NO_INFO";
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
                p.setTypeface(ResourcesCompat.getFont(getApplicationContext(), R.font.consola));
                p.setFakeBoldText(true);
                p.setTextSize(height * TEXT_SIZE_RATIO);
                p.setAntiAlias(true);
                p.setTextAlign(Paint.Align.LEFT);
                p.setColor(PAINT_COLORS[i]);

                mTextPaints[i] = p;
            }

            // Set up text and calculate position values
            messages = new ArrayList<>();
            messages.add("tonyl@watch:~ $ now");
            messages.add("[TIME] ");
            messages.add("[DATE] ");
            messages.add("[BATT] ");
            messages.add("[STEP] ");
            messages.add("[NTIF] ");
            messages.add("tonyl@watch:~ $");

            float totalTextHeight = (messages.size() - 1) * mTextPaints[BASE_PAINT].getFontSpacing();
            mTextX = mCenterX - mTextPaints[BASE_PAINT].measureText(messages.get(0)) / 2;
            mTextX = width * TEXT_X_RATIO;
            mExtraTextX = mTextX + mTextPaints[BASE_PAINT].measureText(messages.get(1));
            mTextY = mCenterY - totalTextHeight / 2;

            // Init complications
            setDefaultSystemComplicationProvider(BATTERY_COMP_ID, SystemProviders.WATCH_BATTERY, ComplicationData.TYPE_RANGED_VALUE);
            setDefaultSystemComplicationProvider(STEP_COMP_ID, SystemProviders.STEP_COUNT, ComplicationData.TYPE_SHORT_TEXT);
            setDefaultSystemComplicationProvider(NOTIF_COMP_ID, SystemProviders.UNREAD_NOTIFICATION_COUNT, ComplicationData.TYPE_SHORT_TEXT);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            long now = System.currentTimeMillis();
            mCalendar.setTimeInMillis(now);

            // Clear screen
            canvas.drawColor(Color.BLACK);

            // Draw text
            float y = mTextY;
            for (int i = 0; i < messages.size(); i++) {
                String message = messages.get(i);
                canvas.drawText(message, mTextX, y, mTextPaints[BASE_PAINT]);
                switch (i) {
                    case 1:
                        canvas.drawText(makeTime(mCalendar), mExtraTextX, y, mTextPaints[TIME_PAINT]);
                        break;
                    case 2:
                        canvas.drawText(makeDate(mCalendar), mExtraTextX, y, mTextPaints[DATE_PAINT]);
                        break;
                    case 3:
                        canvas.drawText(mBatteryVisual, mExtraTextX, y, mTextPaints[BATTERY_PAINT]);
                        break;
                    case 4:
                        canvas.drawText(mStepString, mExtraTextX, y, mTextPaints[STEP_PAINT]);
                        break;
                    case 5:
                        canvas.drawText(mNotifString, mExtraTextX, y, mTextPaints[NOTIF_PAINT]);
                        break;
                }
                y += mTextPaints[BASE_PAINT].getFontSpacing();
            }
        }

        private String makeTime(Calendar c) {
            mSb.setLength(0);
            Date d = c.getTime();
            int minute = c.get(Calendar.MINUTE);
            if (minute != mMinute) {
                mMinute = minute;
                mHourMinuteString = mHourMinuteFormat.format(d);
                mAmTimezoneString = mAmTimezoneFormat.format(d);
            }

            mSb.append(mHourMinuteString);

            String sec = Integer.toString(c.get(Calendar.SECOND));
            if (sec.length() < 2) {
                mSb.append("0");
            }
            mSb.append(sec);

            mSb.append(mAmTimezoneString);

            return mSb.toString();
        }

        private String makeDate(Calendar c) {
            int day = c.get(Calendar.DAY_OF_MONTH);
            if (day != mDay) {
                mDay = day;
                mDateString = mDateFormat.format(c.getTime());
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
