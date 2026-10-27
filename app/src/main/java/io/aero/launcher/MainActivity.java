package io.aero.launcher;

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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity {
    static final int REQ_PICK = 11;
    static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT;
    static final int WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;
    static final int[] SOLID = {0xFF0B2A4A, 0xFF111827, 0xFF374151, 0xFF0F3D3E, 0xFF3B1F4A, 0xFF000000};
    static final String[] SOLID_NAMES = {"Azul marinho", "Grafite", "Cinza", "Verde petróleo", "Roxo", "Preto"};
    static final int[] COLS = {6, 8, 10, 12};
    static final String HELP_PATH = "/sdcard/Android/data/io.aero.launcher/files/wallpaper.jpg";

    static class Entry {
        String label;
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
    FrameLayout root;
    ImageView photoView;
    View scrim;
    GridView grid;
    TextView clock;
    BaseAdapter adapter;
    final List<Entry> entries = new ArrayList<>();
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
        prefs = getSharedPreferences("aero", MODE_PRIVATE);
        dp = getResources().getDisplayMetrics().density;
        cols = prefs.getInt("cols", 10);

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
            setFocusLook(false);
        }

        void setFocusLook(boolean f) {
            bg.setColors(f ? new int[]{0x99FFFFFF, 0x40FFFFFF} : new int[]{0x4DFFFFFF, 0x1AFFFFFF});
            bg.setStroke((int) ((f ? 3 : 1) * dp), f ? 0xFF8FEBFF : 0x44FFFFFF);
            float s = f ? 1.12f : 1f;
            setScaleX(s);
            setScaleY(s);
            setTranslationZ(f ? 12 * dp : 0);
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
            c.setFocusLook(i == selected);
            return c;
        }
    }

    void applyFocus() {
        int first = grid.getFirstVisiblePosition();
        for (int i = 0; i < grid.getChildCount(); i++) {
            View v = grid.getChildAt(i);
            if (v instanceof Cell) {
                ((Cell) v).setFocusLook(first + i == selected);
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
                entries.clear();
                entries.add(settingsEntry());
                entries.addAll(list);
                adapter.notifyDataSetChanged();
                if (selected >= entries.size()) selected = 0;
                grid.requestFocus();
            });
        }).start();
    }

    void activate(Entry e) {
        if (e.settings) {
            showSettings();
            return;
        }
        try {
            startActivity(e.intent);
        } catch (Exception ex) {
            Toast.makeText(this, "Não foi possível abrir " + e.label, Toast.LENGTH_SHORT).show();
        }
    }

    void showSettings() {
        String[] items = {
                "Fundo padrão (degradê azul)",
                "Fundo de cor lisa...",
                "Fundo com foto: escolher arquivo...",
                "Fundo com foto: usar a enviada pelo Termux",
                "Apps por linha: " + cols + "...",
                "Ajuda: enviar foto pelo Termux"
        };
        new AlertDialog.Builder(this).setTitle("Ajustes").setItems(items, (d, which) -> {
            switch (which) {
                case 0:
                    prefs.edit().putInt("bg", 0).apply();
                    applyBackground();
                    break;
                case 1:
                    showColors();
                    break;
                case 2:
                    pickPhoto();
                    break;
                case 3:
                    usePushedPhoto();
                    break;
                case 4:
                    showCols();
                    break;
                default:
                    showHelp("Como enviar uma foto:");
            }
        }).show();
    }

    void showColors() {
        new AlertDialog.Builder(this).setTitle("Cor lisa").setItems(SOLID_NAMES, (d, which) -> {
            prefs.edit().putInt("bg", 1).putInt("color", which).apply();
            applyBackground();
        }).show();
    }

    void showCols() {
        String[] names = new String[COLS.length];
        for (int i = 0; i < COLS.length; i++) names[i] = COLS[i] + " apps por linha";
        new AlertDialog.Builder(this).setTitle("Apps por linha").setItems(names, (d, which) -> {
            cols = COLS[which];
            prefs.edit().putInt("cols", cols).apply();
            selected = 0;
            configureGrid();
            adapter = new Adapter();
            grid.setAdapter(adapter);
        }).show();
    }

    void showHelp(String intro) {
        new AlertDialog.Builder(this)
                .setTitle("Foto de fundo")
                .setMessage(intro + "\n\nNo Termux, com a TV conectada:\n\nadb push foto.jpg "
                        + HELP_PATH + "\n\nDepois abra Ajustes e escolha: Fundo com foto: usar a enviada pelo Termux.")
                .setPositiveButton("OK", null)
                .show();
    }

    void usePushedPhoto() {
        if (loadPhoto() != null) {
            prefs.edit().putInt("bg", 2).apply();
            applyBackground();
        } else {
            showHelp("Nenhuma foto enviada ainda.");
        }
    }

    void pickPhoto() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(i, REQ_PICK);
        } catch (ActivityNotFoundException e) {
            showHelp("Esta TV não tem seletor de arquivos.");
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_PICK || res != RESULT_OK || data == null || data.getData() == null) return;
        try (InputStream in = getContentResolver().openInputStream(data.getData());
             FileOutputStream out = new FileOutputStream(photoFile())) {
            byte[] buf = new byte[65536];
            int n;
            while (in != null && (n = in.read(buf)) > 0) out.write(buf, 0, n);
            prefs.edit().putInt("bg", 2).apply();
            applyBackground();
        } catch (Exception e) {
            Toast.makeText(this, "Não foi possível usar essa foto.", Toast.LENGTH_LONG).show();
        }
    }

    File photoFile() {
        File d = getExternalFilesDir(null);
        if (d == null) d = getFilesDir();
        return new File(d, "wallpaper.jpg");
    }

    Bitmap loadPhoto() {
        File f = photoFile();
        if (!f.exists()) return null;
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(f.getPath(), o);
        int s = 1;
        while (o.outWidth / (s * 2) >= 1920) s *= 2;
        o = new BitmapFactory.Options();
        o.inSampleSize = s;
        return BitmapFactory.decodeFile(f.getPath(), o);
    }

    void applyBackground() {
        int mode = prefs.getInt("bg", 0);
        photoView.setVisibility(View.GONE);
        scrim.setVisibility(View.GONE);
        if (mode == 1) {
            int i = Math.max(0, Math.min(SOLID.length - 1, prefs.getInt("color", 0)));
            root.setBackgroundColor(SOLID[i]);
            return;
        }
        if (mode == 2) {
            Bitmap bmp = loadPhoto();
            if (bmp != null) {
                photoView.setImageBitmap(bmp);
                photoView.setVisibility(View.VISIBLE);
                scrim.setVisibility(View.VISIBLE);
                root.setBackgroundColor(Color.BLACK);
                return;
            }
        }
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{0xFF1E78B4, 0xFF0B3A6B, 0xFF051A3A}));
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_PACKAGE_ADDED);
        f.addAction(Intent.ACTION_PACKAGE_REMOVED);
        f.addDataScheme("package");
        registerReceiver(pkgReceiver, f);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            unregisterReceiver(pkgReceiver);
        } catch (Exception ignored) {
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ui.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        ui.removeCallbacks(tick);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        grid.setSelection(0);
    }

    @Override
    public void onBackPressed() {
    }
}
