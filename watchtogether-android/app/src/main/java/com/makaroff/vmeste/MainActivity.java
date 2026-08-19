package com.makaroff.vmeste;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private static final String PREFS = "vmeste_prefs";
    private static final String KEY_SERVER = "server_url";
    private WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle("Вместе");
        String saved = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_SERVER, "");
        if (saved == null || saved.trim().isEmpty()) {
            showSetupScreen();
        } else {
            openWebApp(saved);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuItem item = menu.add("Сервер");
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if ("Сервер".contentEquals(item.getTitle())) {
            showServerDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void showSetupScreen() {
        if (webView != null) {
            webView.destroy();
            webView = null;
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(42), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(16, 17, 22));

        TextView title = new TextView(this);
        title.setText("Смотрим вместе");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView subtitle = new TextView(this);
        subtitle.setText("Первый запуск. Укажи адрес сервера синхронизации. Его можно поменять позже через пункт «Сервер» сверху.");
        subtitle.setTextColor(Color.rgb(190, 193, 202));
        subtitle.setTextSize(15);
        subtitle.setPadding(0, dp(14), 0, dp(20));
        root.addView(subtitle, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        EditText input = new EditText(this);
        input.setHint("https://example.com или http://IP:3000");
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.rgb(130, 133, 143));
        input.setBackgroundColor(Color.rgb(34, 36, 44));
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        root.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        Button button = new Button(this);
        button.setText("Открыть приложение");
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        buttonParams.topMargin = dp(14);
        root.addView(button, buttonParams);

        TextView note = new TextView(this);
        note.setText("APK хранит только адрес сервера. Само видео воспроизводится через встроенный YouTube-плеер.");
        note.setTextColor(Color.rgb(145, 148, 158));
        note.setTextSize(13);
        note.setPadding(0, dp(18), 0, 0);
        root.addView(note, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        button.setOnClickListener(v -> {
            String url = normalizeUrl(input.getText().toString());
            if (!isUsableUrl(url)) {
                Toast.makeText(this, "Введи корректный адрес сервера", Toast.LENGTH_SHORT).show();
                return;
            }
            getSharedPreferences(PREFS, MODE_PRIVATE)
                    .edit()
                    .putString(KEY_SERVER, url)
                    .apply();
            openWebApp(url);
        });

        setContentView(root);
    }

    private void showServerDialog() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setText(getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_SERVER, ""));
        input.setSelection(input.getText().length());

        int pad = dp(20);
        LinearLayout box = new LinearLayout(this);
        box.setPadding(pad, dp(8), pad, 0);
        box.addView(input, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        new AlertDialog.Builder(this)
                .setTitle("Адрес сервера")
                .setView(box)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String url = normalizeUrl(input.getText().toString());
                    if (!isUsableUrl(url)) {
                        Toast.makeText(this, "Некорректный адрес", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    getSharedPreferences(PREFS, MODE_PRIVATE)
                            .edit()
                            .putString(KEY_SERVER, url)
                            .apply();
                    openWebApp(url);
                })
                .setNegativeButton("Отмена", null)
                .setNeutralButton("Сбросить", (dialog, which) -> {
                    getSharedPreferences(PREFS, MODE_PRIVATE)
                            .edit()
                            .remove(KEY_SERVER)
                            .apply();
                    showSetupScreen();
                })
                .show();
    }

    private String normalizeUrl(String raw) {
        String value = raw == null ? "" : raw.trim();
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "http://" + value;
        }
        return value;
    }

    private boolean isUsableUrl(String value) {
        try {
            Uri uri = Uri.parse(value);
            String scheme = uri.getScheme();
            return ("http".equals(scheme) || "https".equals(scheme)) && uri.getHost() != null;
        } catch (Exception e) {
            return false;
        }
    }

    private void openWebApp(String url) {
        if (webView != null) {
            webView.destroy();
        }

        webView = new WebView(this);
        webView.setBackgroundColor(Color.rgb(16, 17, 22));

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebChromeClient(new WebChromeClient());
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                return !("http".equals(scheme) || "https".equals(scheme));
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) {
                    Toast.makeText(MainActivity.this,
                            "Не удалось подключиться к серверу",
                            Toast.LENGTH_LONG).show();
                }
            }
        });

        setContentView(webView);
        webView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        super.onDestroy();
    }
}
