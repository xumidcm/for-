package com.example.rootwebviewdemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class FloatingOverlayService extends Service {

    private static final String CHANNEL_ID = "floating_overlay_channel";
    private static final int NOTIFICATION_ID = 1002;

    private WindowManager windowManager;
    private ImageView floatingBallView;
    private FrameLayout webPanelView;
    private WebView overlayWebView;
    private WindowManager.LayoutParams ballParams;
    private WindowManager.LayoutParams panelParams;
    private boolean webPanelVisible;

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        startForeground(NOTIFICATION_ID, buildNotification());
        createFloatingBall();
        createWebPanel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createFloatingBall() {
        floatingBallView = new ImageView(this);
        int iconPadding = dp(12);
        floatingBallView.setPadding(iconPadding, iconPadding, iconPadding, iconPadding);
        floatingBallView.setImageResource(getApplicationInfo().icon);
        floatingBallView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        floatingBallView.setBackground(buildBallBackground());
        floatingBallView.setElevation(dp(8));

        ballParams = new WindowManager.LayoutParams(
                dp(64),
                dp(64),
                getOverlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        ballParams.gravity = Gravity.TOP | Gravity.START;
        ballParams.x = dp(16);
        ballParams.y = dp(160);

        floatingBallView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX;
            private int initialY;
            private float initialTouchX;
            private float initialTouchY;
            private boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = ballParams.x;
                        initialY = ballParams.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        int deltaX = (int) (event.getRawX() - initialTouchX);
                        int deltaY = (int) (event.getRawY() - initialTouchY);
                        if (Math.abs(deltaX) > dp(4) || Math.abs(deltaY) > dp(4)) {
                            moved = true;
                        }
                        ballParams.x = initialX + deltaX;
                        ballParams.y = initialY + deltaY;
                        windowManager.updateViewLayout(floatingBallView, ballParams);
                        if (webPanelVisible) {
                            updatePanelPosition();
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!moved) {
                            toggleWebPanel();
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });

        windowManager.addView(floatingBallView, ballParams);
    }

    private void createWebPanel() {
        webPanelView = new FrameLayout(this);
        webPanelView.setVisibility(View.GONE);
        webPanelView.setElevation(dp(10));
        webPanelView.setBackground(buildPanelBackground());

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(12), dp(10), dp(12));
        header.setBackgroundColor(0xFFF5E4EA);

        TextView titleView = new TextView(this);
        titleView.setText(R.string.floating_panel_title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        titleView.setTextColor(0xFF5F3B47);
        titleView.setTypeface(titleView.getTypeface(), android.graphics.Typeface.BOLD);

        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1f
        );
        header.addView(titleView, titleParams);

        TextView closeButton = new TextView(this);
        closeButton.setText("X");
        closeButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        closeButton.setTextColor(0xFF724651);
        closeButton.setGravity(Gravity.CENTER);
        closeButton.setOnClickListener(v -> closeWebPanel());
        header.addView(closeButton, new LinearLayout.LayoutParams(dp(36), dp(36)));

        overlayWebView = new WebView(this);
        WebSettings settings = overlayWebView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setDomStorageEnabled(true);
        overlayWebView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        overlayWebView.setWebViewClient(new WebViewClient());
        overlayWebView.addJavascriptInterface(new JsBridge(), "NativeBridge");
        overlayWebView.loadUrl("file:///android_asset/index.html");

        content.addView(header, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        content.addView(overlayWebView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        webPanelView.addView(content, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        panelParams = new WindowManager.LayoutParams(
                getPanelWidth(),
                getPanelHeight(),
                getOverlayType(),
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
        );
        panelParams.gravity = Gravity.TOP | Gravity.START;
        updatePanelPosition();

        windowManager.addView(webPanelView, panelParams);
    }

    private void toggleWebPanel() {
        if (webPanelVisible) {
            closeWebPanel();
            return;
        }

        updatePanelPosition();
        webPanelView.setVisibility(View.VISIBLE);
        webPanelVisible = true;
    }

    private void closeWebPanel() {
        webPanelView.setVisibility(View.GONE);
        webPanelVisible = false;
    }

    private void updatePanelPosition() {
        if (ballParams == null || panelParams == null) {
            return;
        }

        DisplayMetrics metrics = getResources().getDisplayMetrics();
        int screenWidth = metrics.widthPixels;
        int screenHeight = metrics.heightPixels;
        int margin = dp(12);
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();

        int preferredX = ballParams.x + dp(4);
        int maxX = Math.max(margin, screenWidth - panelWidth - margin);
        panelParams.x = Math.min(Math.max(preferredX, margin), maxX);

        int preferredY = ballParams.y + dp(78);
        int maxY = Math.max(margin, screenHeight - panelHeight - margin);
        if (preferredY > maxY) {
            preferredY = ballParams.y - panelHeight - dp(12);
        }
        panelParams.y = Math.min(Math.max(preferredY, margin), maxY);

        if (webPanelView.getParent() != null) {
            windowManager.updateViewLayout(webPanelView, panelParams);
        }
    }

    private GradientDrawable buildBallBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.OVAL);
        drawable.setColor(0xF7FFFFFF);
        drawable.setStroke(dp(1), 0xFFE6D5DB);
        return drawable;
    }

    private GradientDrawable buildPanelBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(0xFFFDF8F9);
        drawable.setCornerRadius(dp(22));
        drawable.setStroke(dp(1), 0xFFEACDD5);
        return drawable;
    }

    private int getOverlayType() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private int getPanelWidth() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return Math.min(metrics.widthPixels - dp(24), dp(420));
    }

    private int getPanelHeight() {
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return Math.min(metrics.heightPixels - dp(72), dp(620));
    }

    private Notification buildNotification() {
        createNotificationChannel();

        Intent launchIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                launchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.overlay_notification_title))
                .setContentText(getString(R.string.overlay_notification_text))
                .setSmallIcon(getApplicationInfo().icon)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        if (notificationManager.getNotificationChannel(CHANNEL_ID) != null) {
            return;
        }

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.overlay_notification_channel),
                NotificationManager.IMPORTANCE_LOW
        );
        notificationManager.createNotificationChannel(channel);
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                value,
                getResources().getDisplayMetrics()
        ));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayWebView != null) {
            overlayWebView.destroy();
            overlayWebView = null;
        }
        if (webPanelView != null && webPanelView.getParent() != null) {
            windowManager.removeView(webPanelView);
        }
        if (floatingBallView != null && floatingBallView.getParent() != null) {
            windowManager.removeView(floatingBallView);
        }
    }
}
