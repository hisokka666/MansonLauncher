package io.manson.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MainActivity extends Activity {
    static final int REQ_PICK = 11;
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    static final int[] SOLID = {0xFF0B2A4A, 0xFF111827, 0xFF374151, 0xFF0F3D3E, 0xFF3B1F4A, 0xFF000000};
    static final String[] SOLID_NAMES = {"Azul marinho", "Grafite", "Cinza", "Verde petróleo", "Roxo", "Preto"};
    static final int[] COLS = {6, 8, 10, 12};
    static final String HELP_PATH = "/sdcard/Android/data/io.manson.launcher/files/wallpaper.jpg";

    static class Entry {
        String label;
        String pkg;
        Drawable icon;
        Intent intent;
        boolean settings;
    }

    SharedPreferences prefs;
    float dp;
    float labelPx;
    int cols;
    int iconSize;
    int selected = 0;
    boolean organizing = false;
    boolean picked = false;
    FrameLayout root;
    ImageView photoView;
    View scrim;
    GridView grid;
    TextView clock;
    TextView hint;
    BaseAdapter adapter;
    final List<Entry> all = new ArrayList<>();
    final List<Entry> entries = new ArrayList<>();
    Set<String> hidden = new HashSet<>();
    final Handler ui = new Handler(Looper.getMainLooper());

    final Runnable tick = new Runnable() {
        @Override
        public void run() {
            clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date()));
            ui.postDelayed(this, 15000);
        }
    };

    final BroadcastReceiver pkgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            loadApps();
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("manson", MODE_PRIVATE);
        dp = getResources().getDisplayMetrics().density;
        cols = prefs.getInt("cols", 10);
        hidden = new HashSet<>(prefs.getStringSet("hidden", new HashSet<String>()));

        root = new FrameLayout(this);
        root.setClipChildren(false);

        photoView = new ImageView(this);
        photoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(photoView, new FrameLayout.LayoutParams(MATCH, MATCH));

        scrim = new View(this);
        scrim.setBackgroundColor(0x40000000);
        root.addView(scrim, new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setClipChildren(false);
        root.addView(column, new FrameLayout.LayoutParams(MATCH, MATCH));

        clock = new TextView(this);
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(TypedValue.COMPLEX_UNIT_PX, 34 * dp);
        clock.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        clock.setShadowLayer(8, 0, 2, 0x88000000);
        clock.setPadding((int) (56 * dp), (int) (28 * dp), 0, 0);
        column.addView(clock, new LinearLayout.LayoutParams(WRAP, WRAP));

        hint = new TextView(this);
        hint.setTextColor(0xFFFFE08A);
        hint.setTextSize(TypedValue.COMPLEX_UNIT_PX, 16 * dp);
        hint.setShadowLayer(6, 0, 2, 0x88000000);
        hint.setPadding((int) (56 * dp), (int) (4 * dp), 0, 0);
        hint.setText("MODO ORGANIZAR: OK pega ou solta o app · setas movem · VOLTAR termina");
        hint.setVisibility(View.GONE);
        column.addView(hint, new LinearLayout.LayoutParams(WRAP, WRAP));

        grid = new GridView(this);
        grid.setSelector(new ColorDrawable(Color.TRANSPARENT));
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setVerticalScrollBarEnabled(false);
        grid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selected = position;
                applyFocus();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        grid.setOnItemClickListener((parent, view, position, id) -> activate(entries.get(position)));
        column.addView(grid, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        setContentView(root);

        entries.add(settingsEntry());
        configureGrid();
        adapter = new Adapter();
        grid.setAdapter(adapter);
        applyBackground();
        loadApps();
    }

    void configureGrid() {
        DisplayMetrics m = getResources().getDisplayMetrics();
        int pad = (int) (48 * dp);
        int gap = (int) (14 * dp);
        int cellW = (m.widthPixels - 2 * pad - (cols - 1) * gap) / cols;
        iconSize = (int) (cellW * 0.56f);
        labelPx = Math.max(11 * dp, cellW * 0.12f);
        grid.setNumColumns(cols);
        grid.setHorizontalSpacing(gap);
        grid.setVerticalSpacing((int) (20 * dp));
        grid.setPadding(pad, (int) (24 * dp), pad, (int) (24 * dp));
    }

    class Cell extends LinearLayout {
        final ImageView icon;
        final TextView label;
        final GradientDrawable bg;

        Cell(Context c) {
            super(c);
            setOrientation(VERTICAL);
            setGravity(Gravity.CENTER_HORIZONTAL);
            int p = (int) (10 * dp);
            setPadding(p, p, p, p);

            icon = new ImageView(c);
            icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
            addView(icon, new LinearLayout.LayoutParams(iconSize, iconSize));

            label = new TextView(c);
            label.setTextColor(Color.WHITE);
            label.setTextSize(TypedValue.COMPLEX_UNIT_PX, labelPx);
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setGravity(Gravity.CENTER);
            label.setShadowLayer(4, 0, 2, 0xAA000000);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(MATCH, WRAP);
            lp.topMargin = (int) (8 * dp);
            addView(label, lp);

            bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0x4DFFFFFF, 0x1AFFFFFF});
            bg.setCornerRadius(18 * dp);
            setBackground(bg);
            setLook(false, false);
        }

        void setLook(boolean f, boolean p) {
            if (p) {
                bg.setColors(new int[]{0xCCFFFFFF, 0x66FFFFFF});
                bg.setStroke((int) (4 * dp), 0xFFFFC857);
            } else if (f) {
                bg.setColors(new int[]{0x99FFFFFF, 0x40FFFFFF});
                bg.setStroke((int) (3 * dp), 0xFF8FEBFF);
            } else {
                bg.setColors(new int[]{0x4DFFFFFF, 0x1AFFFFFF});
                bg.setStroke((int) (1 * dp), 0x44FFFFFF);
            }
            float s = p ? 1.2f : (f ? 1.12f : 1f);
            setScaleX(s);
            setScaleY(s);
            setTranslationZ((f || p) ? 12 * dp : 0);
            label.setTypeface(f ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        }
    }

    class Adapter extends BaseAdapter {
        @Override
        public int getCount() {
            return entries.size();
        }

        @Override
        public Object getItem(int i) {
            return entries.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View convert, ViewGroup parent) {
            Cell c = (convert instanceof Cell) ? (Cell) convert : new Cell(MainActivity.this);
            Entry e = entries.get(i);
            c.icon.setImageDrawable(e.icon);
            c.label.setText(e.label);
            c.setLook(i == selected, organizing && picked && i == selected);
            return c;
        }
    }

    void applyFocus() {
        int first = grid.getFirstVisiblePosition();
        for (int i = 0; i < grid.getChildCount(); i++) {
            View v = grid.getChildAt(i);
            if (v instanceof Cell) {
                int pos = first + i;
                ((Cell) v).setLook(pos == selected, organizing && picked && pos == selected);
            }
        }
    }

    Entry settingsEntry() {
        Entry e = new Entry();
        e.label = "Ajustes";
        e.icon = getResources().getDrawable(android.R.drawable.ic_menu_manage, getTheme());
        e.settings = true;
        return e;
    }

    void loadApps() {
        final PackageManager pm = getPackageManager();
        final String self = getPackageName();
        new Thread(() -> {
            List<Entry> list = new ArrayList<>();
            Set<String> seen = new HashSet<>();
            String[] cats = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
            for (String cat : cats) {
                Intent query = new Intent(Intent.ACTION_MAIN).addCategory(cat);
                for (ResolveInfo ri : pm.queryIntentActivities(query, 0)) {
                    String pkg = ri.activityInfo.packageName;
                    if (pkg.equals(self) || !seen.add(pkg)) continue;
                    Entry e = new Entry();
                    e.pkg = pkg;
                    e.label = String.valueOf(ri.loadLabel(pm));
                    e.icon = ri.loadIcon(pm);
                    e.intent = new Intent(Intent.ACTION_MAIN)
                            .addCategory(cat)
                            .setComponent(new ComponentName(pkg, ri.activityInfo.name))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                    list.add(e);
                }
            }
            Collections.sort(list, (a, b) -> a.label.compareToIgnoreCase(b.label));
            ui.post(() -> {
                all.clear();
                all.addAll(list);
                rebuild();
                if (selected >= entries.size()) selected = 0;
                grid.requestFocus();
            });
        }).start();
    }

    List<String> loadOrder() {
        List<String> out = new ArrayList<>();
        String s = prefs.getString("order", "");
        if (!s.isEmpty()) {
            for (String p : s.split("\n")) {
                if (!p.isEmpty()) out.add(p);
            }
        }
        return out;
    }

    void saveOrder() {
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i < entries.size(); i++) {
            if (entries.get(i).pkg != null) sb.append(entries.get(i).pkg).append('\n');
        }
        prefs.edit().putString("order", sb.toString()).apply();
    }

    void rebuild() {
        List<Entry> vis = new ArrayList<>();
        for (Entry e : all) {
            if (!hidden.contains(e.pkg)) vis.add(e);
        }
        final Map<String, Integer> rank = new HashMap<>();
        List<String> order = loadOrder();
        for (int i = 0; i < order.size(); i++) rank.put(order.get(i), i);
        Collections.sort(vis, (a, b) -> {
