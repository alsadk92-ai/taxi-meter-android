
package com.alsadk92ai.taximeter;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.view.View;
import java.util.Locale;

public class MainActivity extends Activity {
    LocationManager gps;
    Location previous;
    double km = 0;
    long started = 0, savedTime = 0;
    boolean running = false, dark = false;
    LinearLayout root;
    TextView distance, time, distanceFare, timeFare, total, status;

    final LocationListener listener = new LocationListener() {
        @Override public void onLocationChanged(Location loc) {
            if (!running) return;
            if (previous != null && loc.getAccuracy() < 60) {
                float meters = previous.distanceTo(loc);
                if (meters >= 0 && meters < 1000) km += meters / 1000.0;
            }
            previous = loc;
            updateValues();
        }
        @Override public void onProviderEnabled(String provider) {}
        @Override public void onProviderDisabled(String provider) {}
    };

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        gps = (LocationManager)getSystemService(LOCATION_SERVICE);
        buildUI();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            }, 10);
        }
        updateValues();
    }

    void buildUI() {
        root = new LinearLayout(this);
        root.setOrientation(1);
        root.setPadding(24, 20, 24, 20);
        root.setGravity(Gravity.CENTER_HORIZONTAL);

        ScrollView scroll = new ScrollView(this);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(1);
        page.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView heading = new TextView(this);
        heading.setText("عداد التاكسي");
        heading.setTextSize(30);
        heading.setGravity(Gravity.CENTER);
        heading.setPadding(0, 12, 0, 20);
        page.addView(heading);

        distance = metric(page);
        time = metric(page);
        distanceFare = metric(page);
        timeFare = metric(page);
        total = metric(page);
        total.setTextSize(28);

        status = new TextView(this);
        status.setText("جاهز لبدء الرحلة");
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        page.addView(status);

        Button start = new Button(this);
        start.setText("بدء الرحلة");
        start.setOnClickListener(v -> startTrip());
        page.addView(start);

        Button end = new Button(this);
        end.setText("إنهاء الرحلة");
        end.setOnClickListener(v -> endTrip());
        page.addView(end);

        Button reset = new Button(this);
        reset.setText("رحلة جديدة");
        reset.setOnClickListener(v -> resetTrip());
        page.addView(reset);

        Button theme = new Button(this);
        theme.setText("تبديل الوضع الليلي / النهاري");
        theme.setOnClickListener(v -> {
            dark = !dark;
            root.setBackgroundColor(dark ? Color.rgb(25,25,25) : Color.WHITE);
            int c = dark ? Color.WHITE : Color.BLACK;
            setColors(page, c);
        });
        page.addView(theme);

        scroll.addView(page);
        root.addView(scroll);
        setContentView(root);
        root.setBackgroundColor(Color.WHITE);
        setColors(page, Color.BLACK);
    }

    TextView metric(LinearLayout page) {
        TextView t = new TextView(this);
        t.setTextSize(21);
        t.setGravity(Gravity.CENTER);
        t.setPadding(8, 14, 8, 14);
        page.addView(t);
        return t;
    }

    void setColors(LinearLayout layout, int color) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            View v = layout.getChildAt(i);
            if (v instanceof TextView) ((TextView)v).setTextColor(color);
            if (v instanceof LinearLayout) setColors((LinearLayout)v, color);
            if (v instanceof ScrollView) {
                View child = ((ScrollView)v).getChildAt(0);
                if (child instanceof LinearLayout) setColors((LinearLayout)child, color);
            }
        }
    }

    void startTrip() {
        if (running) return;
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            }, 10);
            status.setText("اسمح باستخدام الموقع ثم ابدأ الرحلة");
            return;
        }
        try {
            if (!gps.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                status.setText("يرجى تشغيل GPS");
                return;
            }
            previous = gps.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            gps.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, listener);
            running = true;
            started = SystemClock.elapsedRealtime();
            savedTime = 0;
            status.setText("الرحلة جارية");
            updateValues();
        } catch (SecurityException e) {
            status.setText("تعذر الوصول إلى GPS");
        }
    }

    void endTrip() {
        if (!running) return;
        savedTime += SystemClock.elapsedRealtime() - started;
        running = false;
        try { gps.removeUpdates(listener); } catch (Exception ignored) {}
        status.setText("انتهت الرحلة");
        updateValues();
    }

    void resetTrip() {
        try { gps.removeUpdates(listener); } catch (Exception ignored) {}
        running = false;
        km = 0;
        previous = null;
        started = 0;
        savedTime = 0;
        status.setText("رحلة جديدة");
        updateValues();
    }

    long elapsed() {
        return savedTime + (running ? SystemClock.elapsedRealtime() - started : 0);
    }

    void updateValues() {
        if (distance == null) return;
        long sec = elapsed() / 1000;
        distance.setText(String.format(Locale.getDefault(), "المسافة: %.3f كم", km));
        time.setText(String.format(Locale.getDefault(),
            "الوقت: %02d:%02d:%02d", sec/3600, (sec%3600)/60, sec%60));
        distanceFare.setText(String.format(Locale.getDefault(),
            "أجرة المسافة: %.0f ل.س", km * 60));
        timeFare.setText(String.format(Locale.getDefault(),
            "أجرة الوقت: %.0f ل.س", sec / 60.0 * 5));
        total.setText(String.format(Locale.getDefault(),
            "الإجمالي: %.0f ل.س", km * 60 + sec / 60.0 * 5));
        if (running) root.postDelayed(this::updateValues, 1000);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        try { gps.removeUpdates(listener); } catch (Exception ignored) {}
    }
    }
