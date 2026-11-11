
package com.bogar.tv;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import java.net.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {

    LinearLayout root, content, menu;
    EditText urlInput;
    ArrayList<String> names = new ArrayList<>();
    ArrayList<String> links = new ArrayList<>();
    ArrayList<String> favorites = new ArrayList<>();

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        showPage("Kanallar");
    }

    TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.WHITE);
        t.setPadding(18, 18, 18, 18);
        return t;
    }

    Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        return b;
    }

    void showPage(String page) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.rgb(12, 18, 32));
        setContentView(root);

        root.addView(text("BOGAR TV", 27));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(content);
        root.addView(scroll,
            new LinearLayout.LayoutParams(-1, 0, 1));

        menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(menu);

        String[] tabs = {
            "Kanallar", "Listeler",
            "Favoriler", "Ayarlar"
        };

        for (String tab : tabs) {
            Button b = button(tab);
            menu.addView(b,
                new LinearLayout.LayoutParams(0, -2, 1));
            b.setOnClickListener(v -> showPage(tab));
        }

        content.addView(text(page, 22));

        if (page.equals("Kanallar")) showChannels(false);
        if (page.equals("Listeler")) showLists();
        if (page.equals("Favoriler")) showChannels(true);
        if (page.equals("Ayarlar")) {
            content.addView(text(
                "BOGAR TV v1.0\nGitHub M3U liste desteği",
                17));
        }
    }

    void showLists() {
        content.addView(text(
            "M3U veya GitHub RAW bağlantısı girin:",
            16));

        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setHint("https://...");
        urlInput.setTextColor(Color.WHITE);
        urlInput.setHintTextColor(Color.LTGRAY);
        content.addView(urlInput);

        Button load = button("Listeyi Yükle");
        content.addView(load);

        load.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();

            if (!url.startsWith("https://") &&
                !url.startsWith("http://")) {
                Toast.makeText(this,
                    "Geçerli bağlantı girin",
                    Toast.LENGTH_SHORT).show();
                return;
            }

            loadPlaylist(url);
        });
    }

    void loadPlaylist(String address) {
        Toast.makeText(this,
            "Liste yükleniyor...",
            Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            ArrayList<String> newNames = new ArrayList<>();
            ArrayList<String> newLinks = new ArrayList<>();

            try {
                URL url = new URL(address);
                HttpURLConnection conn =
                    (HttpURLConnection) url.openConnection();

                conn.setConnectTimeout(12000);
                conn.setReadTimeout(12000);

                BufferedReader reader =
                    new BufferedReader(new InputStreamReader(
                        conn.getInputStream(), "UTF-8"));

                String line;
                String currentName = "";

                while ((line = reader.readLine()) != null) {
                    line = line.trim();

                    if (line.startsWith("#EXTINF")) {
                        int comma = line.lastIndexOf(",");
                        currentName = comma >= 0
                            ? line.substring(comma + 1).trim()
                            : "Kanal";
                    } else if (line.startsWith("http://") ||
                               line.startsWith("https://")) {
                        newNames.add(currentName.isEmpty()
                            ? "Kanal" : currentName);
                        newLinks.add(line);
                        currentName = "";

                        if (newNames.size() >= 1000) break;
                    }
                }

                reader.close();
                conn.disconnect();

                runOnUiThread(() -> {
                    names.clear();
                    links.clear();
                    names.addAll(newNames);
                    links.addAll(newLinks);

                    Toast.makeText(this,
                        names.size() + " kanal yüklendi",
                        Toast.LENGTH_LONG).show();

                    showPage("Kanallar");
                });

            } catch (Exception e) {
                runOnUiThread(() ->
                    Toast.makeText(this,
                        "Liste yüklenemedi: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    void showChannels(boolean onlyFavorites) {
        if (names.isEmpty()) {
            content.addView(text(
                "Henüz kanal yok. Listeler bölümünden M3U ekleyin.",
                16));
            return;
        }

        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            String name = names.get(i);

            if (onlyFavorites && !favorites.contains(name))
                continue;

            Button play = button("▶ " + name);
            content.addView(play);

            play.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(links.get(index)));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this,
                        "Uygun video oynatıcı bulunamadı",
                        Toast.LENGTH_LONG).show();
                }
            });

            if (!onlyFavorites) {
                Button fav = button(
                    favorites.contains(name)
                    ? "★ Favorilerden çıkar"
                    : "☆ Favorilere ekle");

                content.addView(fav);

                fav.setOnClickListener(v -> {
                    if (favorites.contains(name))
                        favorites.remove(name);
                    else
                        favorites.add(name);

                    showPage("Kanallar");
                });
            }
        }
    }
}
