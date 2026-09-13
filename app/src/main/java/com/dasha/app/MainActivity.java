package com.dasha.app;

import android.app.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.*;
import android.graphics.Color;
import android.net.Uri;
import android.view.*;
import android.webkit.*;
import android.widget.*;

public class MainActivity extends Activity {
    private WebView web;
    private static final String APP_URL = "https://garysleonelmora-creator.github.io/Dasha/";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        showSplash();
    }

    private void showSplash() {
        getWindow().setStatusBarColor(Color.rgb(17,24,39));
        getWindow().setNavigationBarColor(Color.rgb(17,24,39));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(32,32,32,32);
        root.setBackgroundColor(Color.rgb(245,247,250));

        TextView logo = new TextView(this);
        logo.setText("IA");
        logo.setTextSize(44);
        logo.setTextColor(Color.WHITE);
        logo.setGravity(Gravity.CENTER);
        logo.setBackgroundColor(Color.rgb(17,24,39));
        LinearLayout.LayoutParams lpLogo = new LinearLayout.LayoutParams(150,150);
        lpLogo.bottomMargin = 26;
        root.addView(logo, lpLogo);

        TextView title = new TextView(this);
        title.setText("Ingeniería IA");
        title.setTextSize(30);
        title.setTextColor(Color.rgb(17,24,39));
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(-2,-2));

        TextView sub = new TextView(this);
        sub.setText("Asistente inteligente");
        sub.setTextSize(16);
        sub.setTextColor(Color.rgb(90,99,113));
        sub.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lpSub = new LinearLayout.LayoutParams(-2,-2);
        lpSub.topMargin = 10;
        root.addView(sub, lpSub);

        setContentView(root);
        new Handler(Looper.getMainLooper()).postDelayed(this::showApp, 1000);
    }

    private void showApp() {
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);

        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u=req.getUrl();
                String scheme=u.getScheme();
                if("http".equals(scheme)||"https".equals(scheme)) return false;
                try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){}
                return true;
            }
        });

        web.setDownloadListener((url,ua,cd,mime,len)->{
            try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
            catch(Exception e){Toast.makeText(this,"No se pudo abrir la descarga.",Toast.LENGTH_SHORT).show();}
        });

        setContentView(web);
        web.loadUrl(APP_URL);
    }

    @Override public void onBackPressed(){
        if(web!=null&&web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
