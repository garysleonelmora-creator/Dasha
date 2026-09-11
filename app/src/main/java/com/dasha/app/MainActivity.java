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
    private static final String PREFS = "dasha";
    private static final String KEY_URL = "server_url";

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        showSplash();
    }

    private Bitmap loadCover() {
        try {
            ByteArrayOutputStream text = new ByteArrayOutputStream();
            for (int i = 1; i <= 6; i++) {
                String name = String.format("portada_%02d.b64", i);
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
        root.addView(cover, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        setContentView(root);
        new Handler(Looper.getMainLooper()).postDelayed(this::showDasha, 2200);
    }

    private void showDasha() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);

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
                Uri u = req.getUrl();
                String scheme = u.getScheme();
                if ("http".equals(scheme) || "https".equals(scheme)) return false;
                try { startActivity(new Intent(Intent.ACTION_VIEW, u)); } catch(Exception ignored){}
                return true;
            }
        });

        web.setDownloadListener((url,ua,cd,mime,len)-> {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
            catch(Exception e) { Toast.makeText(this,"No se pudo abrir la descarga.",Toast.LENGTH_SHORT).show(); }
        });

        root.addView(web, new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);

        String url = getSharedPreferences(PREFS,MODE_PRIVATE).getString(KEY_URL,"");
        if(url.isEmpty()) askServer(); else web.loadUrl(url);
    }

    private void askServer(){
        EditText e = new EditText(this);
        e.setHint("https://tu-servidor-dasha.com");
        String old = getSharedPreferences(PREFS,MODE_PRIVATE).getString(KEY_URL,"");
        e.setText(old);

        new AlertDialog.Builder(this)
            .setTitle("Conectar Dasha")
            .setMessage("Escribe la dirección de tu servidor Dasha. En la misma Wi-Fi puedes usar http://IP-DE-TU-PC:8765/. Para usarla desde cualquier lugar debes usar una URL HTTPS pública.")
            .setView(e)
            .setCancelable(false)
            .setPositiveButton("Conectar",(d,w)->{
                String u=e.getText().toString().trim();
                if(u.isEmpty()){ askServer(); return; }
                if(!u.startsWith("http://")&&!u.startsWith("https://")) u="https://"+u;
                if(!u.endsWith("/")) u += "/";
                getSharedPreferences(PREFS,MODE_PRIVATE).edit().putString(KEY_URL,u).apply();
                web.loadUrl(u);
            })
            .setNegativeButton("Salir",(d,w)->finish())
            .show();
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        menu.add("Cambiar servidor");
        return true;
    }

    @Override public boolean onOptionsItemSelected(android.view.MenuItem item) {
        if ("Cambiar servidor".contentEquals(item.getTitle())) { askServer(); return true; }
        return super.onOptionsItemSelected(item);
    }

    @Override public void onBackPressed(){
        if(web!=null && web.canGoBack()) web.goBack(); else super.onBackPressed();
    }
}
