
package com.bogar.tv;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.*;
import android.text.Editable;
import android.text.TextWatcher;

import java.net.*;
import java.io.*;
import java.util.*;

public class MainActivity extends Activity {

    LinearLayout root, content;
    SharedPreferences prefs;

    ArrayList<String> names = new ArrayList<>();
    ArrayList<String> links = new ArrayList<>();
    HashSet<String> favorites = new HashSet<>();

    final int BG = Color.rgb(12, 18, 32);
    final int PANEL = Color.rgb(25, 35, 55);
    final int BLUE = Color.rgb(30, 110, 220);

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);

        prefs = getSharedPreferences("bogar_tv", MODE_PRIVATE);

        favorites.addAll(
            prefs.getStringSet("favorites", new HashSet<String>())
        );

        showPage("Kanallar");

        String lastUrl = prefs.getString("playlist_url", "");

        if (!lastUrl.isEmpty()) {
            loadPlaylist(lastUrl);
        }
    }

    TextView text(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.WHITE);
        t.setPadding(16, 16, 16, 16);
        return t;
    }

    Button button(String value) {
        Button b = new Button(this);
        b.setText(value);
        b.setTextColor(Color.WHITE);
        b.setBackgroundTintList(ColorStateList.valueOf(BLUE));
        return b;
    }

    void showPage(String page) {
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        setContentView(root);

        TextView logo = text("BOGAR TV", 27);
        logo.setGravity(Gravity.CENTER);
        root.addView(logo);

        ScrollView scroll = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(10, 8, 10, 8);
        scroll.addView(content);

        root.addView(scroll,
            new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout menu = new LinearLayout(this);
        menu.setBackgroundColor(PANEL);
        root.addView(menu);

        String[] tabs = {
            "Kanallar", "Listeler", "Favoriler", "Ayarlar"
        };

        for (String tab : tabs) {
            Button b = button(tab);
            b.setTextSize(10);
            menu.addView(b,
                new LinearLayout.LayoutParams(0, 55, 1));
            b.setOnClickListener(v -> showPage(tab));
        }

        content.addView(text(page, 22));

        if (page.equals("Kanallar")) showChannels(false);
        if (page.equals("Listeler")) showLists();
        if (page.equals("Favoriler")) showChannels(true);
        if (page.equals("Ayarlar")) showSettings();
    }

    void showLists() {
        content.addView(text(
            "GitHub RAW veya M3U bağlantısı ekle", 16));

        EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.LTGRAY);
        input.setHint("https://raw.githubusercontent.com/...");
        input.setText(prefs.getString("playlist_url", ""));
        content.addView(input);

        Button load = button("Listeyi Yükle");
        content.addView(load);

        load.setOnClickListener(v -> {
            String address = input.getText().toString().trim();

            if (!address.startsWith("http://") &&
                !address.startsWith("https://")) {
                Toast.makeText(this, "Geçerli bağlantı girin",
                    Toast.LENGTH_SHORT).show();
                return;
            }

            loadPlaylist(address);
        });
    }

    String normalizeUrl(String address) {
        if (address.contains("github.com/") &&
            address.contains("/blob/")) {
            return address.replace(
                "https://github.com/",
                "https://raw.githubusercontent.com/"
            ).replace("/blob/", "/");
        }
        return address;
    }

    void loadPlaylist(String address) {
        final String urlAddress = normalizeUrl(address);

        Toast.makeText(this, "Liste yükleniyor...",
            Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            ArrayList<String> newNames = new ArrayList<>();
            ArrayList<String> newLinks = new ArrayList<>();
            HttpURLConnection conn = null;

            try {
                conn = (HttpURLConnection)
                    new URL(urlAddress).openConnection();

                conn.setConnectTimeout(15000);
                conn.setReadTimeout(15000);
                conn.setRequestProperty("User-Agent", "BOGAR-TV/1.0");

                if (conn.getResponseCode() != 200) {
                    throw new IOException("Sunucu bağlantı hatası");
                }

                try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                        conn.getInputStream(), "UTF-8"))) {

                    String line;
                    String currentName = "Kanal";

                    while ((line = reader.readLine()) != null) {
                        line = line.trim();

                        if (line.startsWith("#EXTINF")) {
                            int comma = line.lastIndexOf(",");
                            if (comma >= 0) {
                                currentName =
                                    line.substring(comma + 1).trim();
                            }
                        } else if (
                            line.startsWith("http://") ||
                            line.startsWith("https://")) {

                            newNames.add(currentName);
                            newLinks.add(line);
                            currentName = "Kanal";

                            if (newNames.size() >= 1000) break;
                        }
                    }
                }

                if (newNames.isEmpty()) {
                    throw new IOException("Kanal bulunamadı");
                }

                runOnUiThread(() -> {
                    names.clear();
                    links.clear();
                    names.addAll(newNames);
                    links.addAll(newLinks);

                    prefs.edit()
                        .putString("playlist_url", urlAddress)
                        .apply();

                    Toast.makeText(this,
                        names.size() + " kanal yüklendi",
                        Toast.LENGTH_LONG).show();

                    showPage("Kanallar");
                });

            } catch (Exception e) {
                String error = e.getMessage();
                runOnUiThread(() ->
                    Toast.makeText(this,
                        "Hata: " + error,
                        Toast.LENGTH_LONG).show());
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }

    void showChannels(boolean onlyFavorites) {
        if (names.isEmpty()) {
            content.addView(text(
                "Henüz kanal yok.\nListeler bölümünden M3U ekleyin.",
                16));
            return;
        }

        if (!onlyFavorites) {
            EditText search = new EditText(this);
            search.setSingleLine(true);
            search.setHint("Kanal ara...");
            search.setTextColor(Color.WHITE);
            search.setHintTextColor(Color.LTGRAY);
            content.addView(search);

            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            content.addView(list);

            Runnable refresh = () -> {
                list.removeAllViews();
                String query = search.getText().toString()
                    .toLowerCase(Locale.ROOT);

                for (int i = 0; i < names.size(); i++) {
                    if (names.get(i).toLowerCase(Locale.ROOT)
                        .contains(query)) {
                        addChannelRow(list, i);
                    }
                }
            };

            search.addTextChangedListener(new TextWatcher() {
                public void beforeTextChanged(
                    CharSequence s, int start, int count, int after) {}

                public void onTextChanged(
                    CharSequence s, int start, int before, int count) {
                    refresh.run();
                }

                public void afterTextChanged(Editable s) {}
            });

            refresh.run();

        } else {
            LinearLayout list = new LinearLayout(this);
            list.setOrientation(LinearLayout.VERTICAL);
            content.addView(list);

            for (int i = 0; i < names.size(); i++) {
                if (favorites.contains(names.get(i))) {
                    addChannelRow(list, i);
                }
            }

            if (list.getChildCount() == 0) {
                content.addView(text("Henüz favori kanal yok.", 16));
            }
        }
    }

    void addChannelRow(LinearLayout list, int index) {
        String name = names.get(index);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundColor(PANEL);
        list.addView(row);

        Button play = button("▶ " + name);
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

        Button fav = button(
            favorites.contains(name) ? "★" : "☆");

        row.addView(fav);

        fav.setOnClickListener(v -> {
            if (favorites.contains(name)) {
                favorites.remove(name);
            } else {
                favorites.add(name);
            }

            prefs.edit()
                .putStringSet("favorites", new HashSet<>(favorites))
                .apply();

            showPage("Favoriler");
        });
    }

    void showSettings() {
        content.addView(text(
            "BOGAR TV\n\n" +
            "Sürüm: 1.0\n\n" +
            "Dört sekmeli televizyon uygulaması.\n\n" +
            "GitHub RAW ve M3U liste desteği.\n\n" +
            "Favori kanal kaydetme.\n" +
            "Kanal arama özelliği.",
            17));
    }
}
