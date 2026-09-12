package org.velur.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String PREFS = "velur";
    private static final String KEY_URL = "app_url";
    private static final int REQ_FILE = 42;

    private static final int C_BG = 0xFF0A0910;
    private static final int C_PANEL = 0xFF16141F;
    private static final int C_FG = 0xFFEAE8F4;
    private static final int C_DIM = 0xFF9C97B4;
    private static final int C_ACCENT = 0xFF7C5CFF;
    private static final int C_ACCENT_SOFT = 0xFFA78BFA;

    private WebView web;
    private ProgressBar bar;
    private LinearLayout setup;
    private EditText input;
    private TextView setupMsg;
    private SharedPreferences prefs;
    private ValueCallback<Uri[]> fileCallback;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        Window win = getWindow();
        win.setStatusBarColor(C_BG);
        win.setNavigationBarColor(C_BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(C_BG);

        root.addView(buildTopBar(), new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(38)));

        FrameLayout stage = new FrameLayout(this);
        root.addView(stage, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        web = new WebView(this);
        web.setBackgroundColor(C_BG);
        stage.addView(web, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgressTintList(ColorStateList.valueOf(C_ACCENT_SOFT));
        FrameLayout.LayoutParams barLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(2));
        barLp.gravity = Gravity.TOP;
        stage.addView(bar, barLp);

        setup = buildSetupPanel();
        setup.setVisibility(View.GONE);
        stage.addView(setup, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(root);
        configureWeb();

        if (state == null) {
            String saved = prefs.getString(KEY_URL, "");
            if (saved.length() > 0) load(saved);
            else showSetup("Вставь адрес приложения на Perchance — и оно откроется здесь.");
        } else {
            web.restoreState(state);
        }
    }

    /* ---------------------------------------------------------------- */
    /* вид                                                              */
    /* ---------------------------------------------------------------- */

    private View buildTopBar() {
        FrameLayout top = new FrameLayout(this);
        top.setBackgroundColor(C_BG);

        View line = new View(this);
        line.setBackgroundColor(0xFF1B1926);
        FrameLayout.LayoutParams lineLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lineLp.gravity = Gravity.BOTTOM;
        top.addView(line, lineLp);

        TextView title = new TextView(this);
        title.setText("ВЕЛЮР");
        title.setTextColor(C_ACCENT_SOFT);
        title.setTextSize(11.5f);
        title.setLetterSpacing(0.34f);
        title.setGravity(Gravity.CENTER);
        top.addView(title, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView gear = new TextView(this);
        gear.setText("⚙");
        gear.setTextColor(C_DIM);
        gear.setTextSize(16);
        gear.setGravity(Gravity.CENTER);
        gear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu();
            }
        });
        FrameLayout.LayoutParams gearLp = new FrameLayout.LayoutParams(
                dp(46), ViewGroup.LayoutParams.MATCH_PARENT);
        gearLp.gravity = Gravity.END;
        top.addView(gear, gearLp);

        return top;
    }

    private LinearLayout buildSetupPanel() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setBackgroundColor(C_BG);
        panel.setPadding(dp(24), dp(24), dp(24), dp(24));

        TextView logo = new TextView(this);
        logo.setText("V");
        logo.setTextColor(Color.WHITE);
        logo.setTextSize(28);
        logo.setGravity(Gravity.CENTER);
        logo.setBackground(round(C_ACCENT, 22));
        LinearLayout.LayoutParams logoLp = new LinearLayout.LayoutParams(dp(78), dp(78));
        logoLp.bottomMargin = dp(14);
        panel.addView(logo, logoLp);

        TextView name = new TextView(this);
        name.setText("ВЕЛЮР");
        name.setTextColor(C_FG);
        name.setTextSize(20);
        name.setLetterSpacing(0.24f);
        name.setGravity(Gravity.CENTER);
        panel.addView(name);

        TextView sub = new TextView(this);
        sub.setText("переписка · фото · 18+");
        sub.setTextColor(C_DIM);
        sub.setTextSize(10.5f);
        sub.setLetterSpacing(0.16f);
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        subLp.topMargin = dp(5);
        subLp.bottomMargin = dp(20);
        panel.addView(sub, subLp);

        setupMsg = new TextView(this);
        setupMsg.setTextColor(C_DIM);
        setupMsg.setTextSize(13.5f);
        setupMsg.setGravity(Gravity.CENTER);
        panel.addView(setupMsg);

        input = new EditText(this);
        input.setHint("например: velur или https://perchance.org/velur");
        input.setHintTextColor(0xFF7F7A97);
        input.setTextColor(C_FG);
        input.setTextSize(14);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setBackground(round(C_PANEL, 12));
        input.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams inputLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        inputLp.topMargin = dp(14);
        panel.addView(input, inputLp);

        TextView go = new TextView(this);
        go.setText("Открыть");
        go.setTextColor(Color.WHITE);
        go.setTextSize(15);
        go.setGravity(Gravity.CENTER);
        go.setBackground(round(C_ACCENT, 12));
        go.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submitSetup();
            }
        });
        LinearLayout.LayoutParams goLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(46));
        goLp.topMargin = dp(12);
        panel.addView(go, goLp);

        TextView tip = new TextView(this);
        tip.setText("Адрес можно скопировать внутри приложения: кнопка 📲 в шапке → «Скопировать ссылку».");
        tip.setTextColor(0xFF6F6A86);
        tip.setTextSize(11.5f);
        tip.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams tipLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        tipLp.topMargin = dp(16);
        panel.addView(tip, tipLp);

        return panel;
    }

    /* ---------------------------------------------------------------- */
    /* webview                                                          */
    /* ---------------------------------------------------------------- */

    private void configureWeb() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);

        CookieManager cm = CookieManager.getInstance();
        cm.setAcceptCookie(true);
        cm.setAcceptThirdPartyCookies(web, true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleUrl(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUrl(Uri.parse(url));
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int progress) {
                bar.setProgress(progress);
                bar.setVisibility(progress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            request.grant(request.getResources());
                        } catch (Exception e) {
                            request.deny();
                        }
                    }
                });
            }

            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), REQ_FILE);
                    return true;
                } catch (Exception e) {
                    fileCallback = null;
                    return false;
                }
            }
        });
    }

    private boolean handleUrl(Uri uri) {
        if (uri == null) return false;
        String scheme = uri.getScheme();
        if (scheme == null || scheme.equals("http") || scheme.equals("https")) return false;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show();
        }
        return true;
    }

    /* ---------------------------------------------------------------- */
    /* адрес приложения                                                 */
    /* ---------------------------------------------------------------- */

    private void load(String url) {
        setup.setVisibility(View.GONE);
        bar.setVisibility(View.VISIBLE);
        bar.setProgress(0);
        web.loadUrl(url);
    }

    private void showSetup(String message) {
        if (message != null) setupMsg.setText(message);
        String saved = prefs.getString(KEY_URL, "");
        if (saved.length() > 0 && !saved.startsWith("http")) input.setText(saved);
        else input.setText("");
        input.setSelection(input.getText().length());
        setup.setVisibility(View.VISIBLE);
    }

    private void submitSetup() {
        String url = normalize(input.getText().toString());
        if (url == null) {
            setupMsg.setText("Похоже, это не адрес. Пример: velur или https://perchance.org/velur");
            return;
        }
        prefs.edit().putString(KEY_URL, url).apply();
        load(url);
    }

    private String normalize(String raw) {
        String v = raw == null ? "" : raw.trim();
        if (v.length() == 0) return null;
        if (v.startsWith("http://") || v.startsWith("https://")) return v;
        if (v.contains(".") && !v.contains(" ")) return "https://" + v;
        if (v.matches("[A-Za-z0-9-]+")) return "https://null.perchance.org/" + v.toLowerCase();
        return null;
    }

    /* ---------------------------------------------------------------- */
    /* меню                                                             */
    /* ---------------------------------------------------------------- */

    private void showMenu() {
        final String[] items = new String[]{"Изменить адрес", "Обновить страницу", "Открыть в браузере"};
        new AlertDialog.Builder(this)
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which == 0) showSetup("Укажи адрес приложения:");
                        else if (which == 1) web.reload();
                        else openInBrowser();
                    }
                })
                .show();
    }

    private void openInBrowser() {
        String url = web.getUrl();
        if (url == null || url.startsWith("about:")) url = prefs.getString(KEY_URL, null);
        if (url == null || url.length() == 0) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(this, "Не удалось открыть браузер", Toast.LENGTH_SHORT).show();
        }
    }

    /* ---------------------------------------------------------------- */
    /* системные события                                                */
    /* ---------------------------------------------------------------- */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE && fileCallback != null) {
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null) {
                if (data.getDataString() != null) {
                    result = new Uri[]{Uri.parse(data.getDataString())};
                } else if (data.getClipData() != null) {
                    int count = data.getClipData().getItemCount();
                    result = new Uri[count];
                    for (int i = 0; i < count; i++) {
                        result[i] = data.getClipData().getItemAt(i).getUri();
                    }
                }
            }
            fileCallback.onReceiveValue(result);
            fileCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private boolean handleBack() {
        if (setup.getVisibility() == View.VISIBLE) return false;
        if (web.canGoBack()) {
            web.goBack();
            return true;
        }
        return false;
    }

    @Override
    public void onBackPressed() {
        if (!handleBack()) super.onBackPressed();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && handleBack()) return true;
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        web.saveState(out);
    }

    @Override
    protected void onDestroy() {
        if (web != null) {
            web.setWebChromeClient(null);
            web.destroy();
        }
        super.onDestroy();
    }

    /* ---------------------------------------------------------------- */
    /* утилиты                                                          */
    /* ---------------------------------------------------------------- */

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable d = new GradientDrawable();
        d.setShape(GradientDrawable.RECTANGLE);
        d.setColor(color);
        d.setCornerRadius(dp(radiusDp));
        return d;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
