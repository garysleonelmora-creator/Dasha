package com.dasha.app;

import android.app.*;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.content.*;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.util.Base64;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.io.*;

public class MainActivity extends Activity {
    private WebView web;
    private static final String DASHA_URL = "https://garysleonelmora-creator.github.io/Dasha/";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        showSplash();
    }

    private Bitmap loadCover() {
        try {
            ByteArrayOutputStream text = new ByteArrayOutputStream();
            String[] names = {"portadaA.b64", "portadaB.b64"};
            for (String name : names) {
                InputStream in = getAssets().open(name);
                byte[] buf = new byte[4096];
                int n;
                while ((n = in.read(buf)) != -1) text.write(buf, 0, n);
                in.close();
            }
            byte[] img = Base64.decode(text.toByteArray(), Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(img, 0, img.length);
        } catch (Exception e) {
            return null;
        }
    }

    private void showSplash() {
        getWindow().setStatusBarColor(Color.rgb(217,62,153));
        getWindow().setNavigationBarColor(Color.rgb(217,62,153));
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(248,229,243));
        ImageView cover = new ImageView(this);
        Bitmap bitmap = loadCover();
        if (bitmap != null) cover.setImageBitmap(bitmap);
        cover.setScaleType(ImageView.ScaleType.FIT_CENTER);
        root.addView(cover, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);
        new Handler(Looper.getMainLooper()).postDelayed(this::showDasha, 1800);
    }

    private void showDasha() {
        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) {
                Uri u=req.getUrl(); String scheme=u.getScheme();
                if("http".equals(scheme)||"https".equals(scheme)) return false;
                try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(Exception ignored){}
                return true;
            }
        });
        web.setDownloadListener((url,ua,cd,mime,len)->{try{startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}catch(Exception e){Toast.makeText(this,"No se pudo abrir la descarga.",Toast.LENGTH_SHORT).show();}});
        setContentView(web);
        web.loadUrl(DASHA_URL);
    }

    @Override public void onBackPressed(){
        if(web!=null&&web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
