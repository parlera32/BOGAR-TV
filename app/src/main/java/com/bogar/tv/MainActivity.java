
package com.bogar.tv;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.widget.*;

import java.net.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {

    final int BG = Color.rgb(12, 18, 32);
    final int PANEL = Color.rgb(25, 35, 54);
    final int GOLD = Color.rgb(210, 170, 75);
    final int WHITE = Color.WHITE;
    final int MUTED = Color.rgb(170, 184, 205);

    LinearLayout root, content, menu;
    EditText urlInput;

    ArrayList<String> names = new ArrayList<>();
    ArrayList<String> links = new ArrayList<>();
    HashSet<String> favorites = new HashSet<>();

    String currentPage = "Kanallar";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        getWindow().getDecorView().setSystemUiVisibility(0);

        SharedPreferences prefs =
            getSharedPreferences("bogar_tv", MODE_PRIVATE);

        favorites.addAll(
            prefs.getStringSet("favorites", new HashSet<>())
        );

        showPage("Kanallar");
    }

    GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    TextView label(String value, int size, int color) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(16, 12, 16, 12);
        return t;
    }

    void saveFavorites() {
        getSharedPreferences("bogar_tv", MODE_PRIVATE)
            .edit()
            .putStringSet("favorites", new HashSet<>(favorites))
            .apply();
    }

    void showPage(String page) {
        currentPage = page;

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);

        TextView header = label("BOGAR TV", 27, WHITE);
        header.setGravity(Gravity.CENTER);
        header.setTypeface(null, Typeface.BOLD);
        header.setPadding(12, 22, 12, 22);
        root.addView(header);

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(16, 8, 16, 16);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.addView(content);

        root.addView(scroll,
            new LinearLayout.LayoutParams(-1, 0, 1));

        TextView title = label(page, 23, GOLD);
        title.setTypeface(null, Typeface.BOLD);
        content.addView(title);

        if (page.equals("Kanallar")) showChannels(false);
        if (page.equals("Listeler")) showLists();
        if (page.equals("Favoriler")) showChannels(true);
        if (page.equals("Ayarlar")) showSettings();

        createMenu();
    }

    void createMenu() {
        menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.HORIZONTAL);
        menu.setBackgroundColor(PANEL);
        menu.setPadding(4, 8, 4, 8);

        String[] tabs = {
            "Kanallar", "Listeler", "Favoriler", "Ayarlar"
        };

        String[] icons = {"▣", "☷", "★", "⚙"};

        for (int i = 0; i < tabs.length; i++) {
            final String tab = tabs[i];

            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.VERTICAL);
            item.setGravity(Gravity.CENTER);
            item.setPadding(2, 8, 2, 8);

            boolean selected = currentPage.equals(tab);

            TextView icon = label(
                icons[i], 20, selected ? GOLD : WHITE);
            icon.setGravity(Gravity.CENTER);

            TextView name = label(
                tab, 11, selected ? GOLD : WHITE);
            name.setGravity(Gravity.CENTER);
            name.setPadding(0, 4, 0, 0);

            item.addView(icon);
            item.addView(name);

            if (selected) {
                item.setBackground(shape(
                    Color.rgb(47, 48, 49), 14));
            }

            menu.addView(item,
                new LinearLayout.LayoutParams(0, -2, 1));

            item.setOnClickListener(v -> showPage(tab));
        }

        root.addView(menu);
    }

    void showLists() {
        content.addView(label(
            "GitHub RAW veya M3U bağlantısı ekleyin",
            15, MUTED));

        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setHint("https://...");
        urlInput.setTextColor(WHITE);
        urlInput.setHintTextColor(MUTED);
        urlInput.setPadding(14, 12, 14, 12);
        urlInput.setBackground(shape(PANEL, 12));

        content.addView(urlInput);

        TextView load = actionButton("Listeyi Yükle");
        content.addView(load);

        load.setOnClickListener(v -> {
            String address =
                urlInput.getText().toString().trim();

            if (!address.startsWith("https://") &&
                !address.startsWith("http://")) {
                Toast.makeText(this,
                    "Geçerli bağlantı girin",
                    Toast.LENGTH_SHORT).show();
                return;
            }

            loadPlaylist(address);
        });
    }

    void loadPlaylist(String address) {
        Toast.makeText(this,
            "Kanallar yükleniyor...",
            Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            ArrayList<String> newNames = new ArrayList<>();
            ArrayList<String> newLinks = new ArrayList<>();
            HttpURLConnection conn = null;

            try {
                URL url = new URL(address);
                conn = (HttpURLConnection) url.openConnection();

                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "BOGAR-TV");

                BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
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

                    } else if (
                        line.startsWith("http://") ||
                        line.startsWith("https://")) {

                        newNames.add(currentName.isEmpty()
                            ? "Kanal" : currentName);

                        newLinks.add(line);
                        currentName = "";

                        if (newNames.size() >= 1000) break;
                    }
                }

                reader.close();

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
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    void showChannels(boolean onlyFavorites) {
        if (names.isEmpty()) {
            content.addView(label(
                "Henüz kanal yok.\nListeler bölümünden M3U ekleyin.",
                16, MUTED));
            return;
        }

        for (int i = 0; i < names.size(); i++) {
            final int index = i;
            String name = names.get(i);

            if (onlyFavorites && !favorites.contains(name))
                continue;

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackground(shape(PANEL, 12));
            row.setPadding(6, 4, 6, 4);

            TextView play = label("▶  " + name, 15, WHITE);

            row.addView(play,
                new LinearLayout.LayoutParams(0, -2, 1));

            play.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(links.get(index)));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this,
                        "Video oynatıcı bulunamadı",
                        Toast.LENGTH_SHORT).show();
                }
            });

            TextView fav = label(
                favorites.contains(name) ? "★" : "☆",
                23, GOLD);

            row.addView(fav);

            fav.setOnClickListener(v -> {
                if (favorites.contains(name)) {
                    favorites.remove(name);
                } else {
                    favorites.add(name);
                }

                saveFavorites();
                showPage(currentPage);
            });

            LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);

            params.setMargins(0, 5, 0, 5);
            content.addView(row, params);
        }
    }

    TextView actionButton(String title) {
        TextView b = label(title, 16, WHITE);
        b.setGravity(Gravity.CENTER);
        b.setTypeface(null, Typeface.BOLD);
        b.setBackground(shape(GOLD, 12));
        b.setPadding(16, 14, 16, 14);

        LinearLayout.LayoutParams p =
            new LinearLayout.LayoutParams(-1, -2);

        p.setMargins(0, 12, 0, 12);
        b.setLayoutParams(p);

        return b;
    }

    void showSettings() {
        content.addView(label("BOGAR TV", 25, GOLD));

        content.addView(label(
            "Sürüm: 1.1\n\n" +
            "GitHub RAW / M3U desteği\n" +
            "Dört sekmeli arayüz\n" +
            "Favori kanal sistemi\n" +
            "Harici video oynatıcı desteği",
            15, MUTED));
    }

    @Override
    public void onBackPressed() {
        if (!currentPage.equals("Kanallar")) {
            showPage("Kanallar");
        } else {
            super.onBackPressed();
        }
    }
}
