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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
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
    static final int[] COLS = {4, 5, 6, 8};
    static final String[] LIVE_TV_PKGS = {
            "com.mediatek.tv.oneworld.tvcenter", "com.tcl.tv", "com.google.android.tv", "com.android.tv"};
    static final String HELP_PATH = "/sdcard/Android/data/io.manson.launcher/files/wallpaper.jpg";
    static final String[] LANG_NAMES = {"Português", "English", "Español"};

    static final String[] THEME_NAMES = {"Azul (padrão)", "Grafite", "Roxo", "Verde", "Vermelho"};
    static final int[] THEME_ACCENT = {0xFF2EB8FF, 0xFFB8C4D6, 0xFFB26BFF, 0xFF3BE3A1, 0xFFFF4D5A};
    static final int[][] THEME_BG = {
            {0xFF0F1B36, 0xFF08112A, 0xFF040813},
            {0xFF1B1F27, 0xFF111419, 0xFF08090C},
            {0xFF1E1036, 0xFF120A26, 0xFF07040F},
            {0xFF0B2A24, 0xFF071A17, 0xFF030C0A},
            {0xFF2A0D12, 0xFF160709, 0xFF080304}};

    static final String[][] CITIES = {
            {"Aracaju", "-10.91", "-37.07"}, {"Belém", "-1.46", "-48.50"},
            {"Belo Horizonte", "-19.92", "-43.94"}, {"Boa Vista", "2.82", "-60.67"},
            {"Brasília", "-15.79", "-47.88"}, {"Campo Grande", "-20.46", "-54.62"},
            {"Cuiabá", "-15.60", "-56.10"}, {"Curitiba", "-25.43", "-49.27"},
            {"Florianópolis", "-27.60", "-48.55"}, {"Fortaleza", "-3.73", "-38.53"},
            {"Goiânia", "-16.68", "-49.25"}, {"João Pessoa", "-7.12", "-34.86"},
            {"Macapá", "0.03", "-51.07"}, {"Maceió", "-9.67", "-35.74"},
            {"Manaus", "-3.12", "-60.02"}, {"Natal", "-5.79", "-35.21"},
            {"Palmas", "-10.18", "-48.33"}, {"Porto Alegre", "-30.03", "-51.23"},
            {"Porto Velho", "-8.76", "-63.90"}, {"Recife", "-8.05", "-34.88"},
            {"Rio Branco", "-9.97", "-67.81"}, {"Rio de Janeiro", "-22.91", "-43.17"},
            {"Salvador", "-12.97", "-38.51"}, {"São Luís", "-2.53", "-44.30"},
            {"São Paulo", "-23.55", "-46.63"}, {"Teresina", "-5.09", "-42.80"},
            {"Vitória", "-20.32", "-40.34"}};

    // Traduções: {português, inglês, espanhol}
    static final String[][] TR = {
            {"SUA TV, DO SEU JEITO", "YOUR TV, YOUR WAY", "TU TV, A TU MANERA"},
            {"Configurações do", "Settings for", "Ajustes de"},
            {"MODO ORGANIZAR: OK pega ou solta o app · setas movem · VOLTAR termina",
                    "ORGANIZE MODE: OK picks up or drops the app · arrows move · BACK to finish",
                    "MODO ORGANIZAR: OK toma o suelta la app · flechas mueven · ATRÁS termina"},
            {"Não foi possível abrir", "Could not open", "No se pudo abrir"},
            {"Não encontrei o app de TV aberta nesta TV.", "Live TV app not found on this TV.",
                    "No encontré la app de TV en vivo en este TV."},
            {"Não encontrei entradas HDMI nesta TV.", "No HDMI inputs found on this TV.",
                    "No encontré entradas HDMI en este TV."},
            {"Entradas HDMI", "HDMI inputs", "Entradas HDMI"},
            {"Não foi possível abrir essa entrada.", "Could not open that input.",
                    "No se pudo abrir esa entrada."},
            {"Não foi possível abrir as configurações da TV.", "Could not open the TV settings.",
                    "No se pudieron abrir los ajustes del TV."},
            {"Máx.", "High", "Máx."},
            {"Mín.", "Low", "Mín."},
            {"Céu limpo", "Clear sky", "Cielo despejado"},
            {"Noite limpa", "Clear night", "Noche despejada"},
            {"Poucas nuvens", "Few clouds", "Pocas nubes"},
            {"Noite com poucas nuvens", "Night with few clouds", "Noche con pocas nubes"},
            {"Parcialmente nublado", "Partly cloudy", "Parcialmente nublado"},
            {"Nublado", "Overcast", "Nublado"},
            {"Neblina", "Fog", "Niebla"},
            {"Garoa", "Drizzle", "Llovizna"},
            {"Chuva", "Rain", "Lluvia"},
            {"Neve", "Snow", "Nieve"},
            {"Tempestade", "Thunderstorm", "Tormenta"},
            {"Tempo variável", "Variable weather", "Tiempo variable"},
            {"Cidade", "City", "Ciudad"},
            {"Buscar outra cidade...", "Search another city...", "Buscar otra ciudad..."},
            {"Buscar cidade", "Search city", "Buscar ciudad"},
            {"Nome da cidade", "City name", "Nombre de la ciudad"},
            {"Buscar", "Search", "Buscar"},
            {"Cancelar", "Cancel", "Cancelar"},
            {"Não foi possível buscar agora.", "Could not search right now.", "No se pudo buscar ahora."},
            {"Nenhuma cidade encontrada.", "No city found.", "No se encontró ninguna ciudad."},
            {"Escolha a cidade", "Choose the city", "Elige la ciudad"},
            {"Ajustes", "Settings", "Ajustes"},
            {"Ajustes do Início (idioma, clima, cidade, sons)...",
                    "Home settings (language, weather, city, sounds)...",
                    "Ajustes de inicio (idioma, clima, ciudad, sonidos)..."},
            {"Fundo e temas...", "Background and themes...", "Fondo y temas..."},
            {"Apps por linha", "Apps per row", "Apps por fila"},
            {"apps por linha", "apps per row", "apps por fila"},
            {"Escolher quais apps aparecem...", "Choose which apps are shown...", "Elegir qué apps se muestran..."},
            {"Organizar apps (mover)...", "Organize apps (move)...", "Organizar apps (mover)..."},
            {"Restaurar ordem alfabética", "Restore alphabetical order", "Restaurar orden alfabético"},
            {"Ajustes do Início", "Home settings", "Ajustes de inicio"},
            {"Idioma", "Language", "Idioma"},
            {"Widget de clima", "Weather widget", "Widget del clima"},
            {"Ligado", "On", "Activado"},
            {"Desligado", "Off", "Desactivado"},
            {"Temperatura", "Temperature", "Temperatura"},
            {"Sons de navegação", "Navigation sounds", "Sonidos de navegación"},
            {"Ligados", "On", "Activados"},
            {"Desligados", "Off", "Desactivados"},
            {"Fundo e temas", "Background and themes", "Fondo y temas"},
            {"Tema", "Theme", "Tema"},
            {"Azul (padrão)", "Blue (default)", "Azul (predeterminado)"},
            {"Grafite", "Graphite", "Grafito"},
            {"Roxo", "Purple", "Morado"},
            {"Verde", "Green", "Verde"},
            {"Vermelho", "Red", "Rojo"},
            {"Fundo padrão (do tema)", "Default background (from theme)", "Fondo predeterminado (del tema)"},
            {"Fundo de cor lisa...", "Solid color background...", "Fondo de color liso..."},
            {"Fundo com foto: escolher arquivo...", "Photo background: choose file...",
                    "Fondo con foto: elegir archivo..."},
            {"Fundo com foto: usar a enviada pelo Termux", "Photo background: use the one sent via Termux",
                    "Fondo con foto: usar la enviada por Termux"},
            {"Ajuda: enviar foto pelo Termux", "Help: send a photo via Termux", "Ayuda: enviar foto por Termux"},
            {"Cor lisa", "Solid color", "Color liso"},
            {"Azul marinho", "Navy blue", "Azul marino"},
            {"Cinza", "Gray", "Gris"},
            {"Verde petróleo", "Petrol green", "Verde petróleo"},
            {"Preto", "Black", "Negro"},
            {"Apps que aparecem", "Visible apps", "Apps visibles"},
            {"Salvar", "Save", "Guardar"},
            {"Foto de fundo", "Background photo", "Foto de fondo"},
            {"Como enviar uma foto:", "How to send a photo:", "Cómo enviar una foto:"},
            {"No Termux, com a TV conectada:", "In Termux, with the TV connected:",
                    "En Termux, con el TV conectado:"},
            {"Depois abra Ajustes > Fundo e temas e escolha: Fundo com foto: usar a enviada pelo Termux.",
                    "Then open Settings > Background and themes and choose: Photo background: use the one sent via Termux.",
                    "Luego abre Ajustes > Fondo y temas y elige: Fondo con foto: usar la enviada por Termux."},
            {"Nenhuma foto enviada ainda.", "No photo sent yet.", "Aún no se envió ninguna foto."},
            {"Esta TV não tem seletor de arquivos.", "This TV has no file picker.",
                    "Este TV no tiene selector de archivos."},
            {"Não foi possível usar essa foto.", "Could not use that photo.", "No se pudo usar esa foto."}
    };
    static final Map<String, String[]> TRMAP = new HashMap<>();

    static {
        for (String[] r : TR) {
            TRMAP.put(r[0], new String[]{r[1], r[2]});
        }
    }

    static class Entry {
        String label;
        String pkg;
        Drawable icon;
        Drawable banner;
        Intent intent;
    }

    SharedPreferences prefs;
    AudioManager audio;
    float k;
    int lang = 0;
    int accent = 0xFF2EB8FF;
    int cols;
    int cardW;
    int cardH;
    int selected = 0;
    boolean firstSelection = true;
    boolean organizing = false;
    boolean picked = false;
    FrameLayout root;
    ImageView photoView;
    View scrim;
    GridView grid;
    TextView clock;
    TextView date;
    TextView hint;
    LinearLayout weatherRow;
    WeatherIcon weatherIcon;
    TextView weatherText;
    BaseAdapter adapter;
    final List<Entry> all = new ArrayList<>();
    final List<Entry> entries = new ArrayList<>();
    Set<String> hidden = new HashSet<>();
    final Handler ui = new Handler(Looper.getMainLooper());

    final Runnable tick = new Runnable() {
        @Override
        public void run() {
            Date now = new Date();
            clock.setText(new SimpleDateFormat("HH:mm", Locale.getDefault()).format(now));
            date.setText(new SimpleDateFormat(datePattern(), loc()).format(now));
            ui.postDelayed(this, 15000);
        }
    };

    final BroadcastReceiver pkgReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            loadApps();
        }
    };

    String tr(String pt) {
        if (lang == 0) return pt;
        String[] v = TRMAP.get(pt);
        return v == null ? pt : v[lang - 1];
    }

    Locale loc() {
        if (lang == 1) return Locale.US;
        if (lang == 2) return new Locale("es", "ES");
        return new Locale("pt", "BR");
    }

    String datePattern() {
        return lang == 1 ? "EEEE, MMMM d" : "EEEE, d 'de' MMMM";
    }

    int defaultLang() {
        String l = Locale.getDefault().getLanguage();
        if ("en".equals(l)) return 1;
        if ("es".equals(l)) return 2;
        return 0;
    }

    int px(float v) {
        return Math.round(v * k);
    }

    TextView text(String s, float sizePx, int color, Typeface tf) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, sizePx);
        t.setTypeface(tf);
        t.setIncludeFontPadding(false);
        return t;
    }

    int theme() {
        int t = prefs.getInt("theme", 0);
        return (t < 0 || t >= THEME_NAMES.length) ? 0 : t;
    }

    void navSound() {
        if (audio == null || !prefs.getBoolean("sounds", true)) return;
        audio.playSoundEffect(AudioManager.FX_FOCUS_NAVIGATION_RIGHT);
    }

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("manson", MODE_PRIVATE);
        audio = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        lang = prefs.getInt("lang", defaultLang());
        if (lang < 0 || lang > 2) lang = 0;
        accent = THEME_ACCENT[theme()];
        k = getResources().getDisplayMetrics().widthPixels / 1920f;
        cols = prefs.getInt("cols", 5);
        if (cols < 3 || cols > 8) cols = 5;
        hidden = new HashSet<>(prefs.getStringSet("hidden", new HashSet<String>()));

        root = new FrameLayout(this);
        root.setClipChildren(false);

        photoView = new ImageView(this);
        photoView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(photoView, new FrameLayout.LayoutParams(MATCH, MATCH));

        scrim = new View(this);
        scrim.setBackgroundColor(0x55000000);
        root.addView(scrim, new FrameLayout.LayoutParams(MATCH, MATCH));

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setClipChildren(false);
        root.addView(column, new FrameLayout.LayoutParams(MATCH, MATCH));

        column.addView(buildHeader(), new LinearLayout.LayoutParams(MATCH, px(140)));
        column.addView(buildClockBlock(), new LinearLayout.LayoutParams(WRAP, WRAP));

        grid = new GridView(this);
        grid.setSelector(new ColorDrawable(Color.TRANSPARENT));
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setVerticalScrollBarEnabled(false);
        grid.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!firstSelection && position != selected) navSound();
                firstSelection = false;
                selected = position;
                applyFocus();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        grid.setOnFocusChangeListener((v, has) -> applyFocus());
        grid.setOnItemClickListener((parent, view, position, id) -> activate(entries.get(position)));
        column.addView(grid, new LinearLayout.LayoutParams(MATCH, 0, 1f));

        SettingsCard sc = new SettingsCard(this);
        sc.setOnClickListener(v -> {
            if (!organizing) showSettings();
        });
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(px(560), px(116));
        slp.leftMargin = px(84);
        slp.topMargin = px(6);
        slp.bottomMargin = px(40);
        column.addView(sc, slp);

        setContentView(root);

        configureGrid();
        adapter = new Adapter();
        grid.setAdapter(adapter);
        applyBackground();
        updateWeatherUi();
        loadApps();
    }

    View buildHeader() {
        FrameLayout header = new FrameLayout(this);
        header.setClipChildren(false);
        header.setPadding(px(84), px(8), px(84), 0);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.HORIZONTAL);
        left.setGravity(Gravity.CENTER_VERTICAL);

        ImageView mark = new ImageView(this);
        mark.setImageResource(R.drawable.ic_manson);
        left.addView(mark, new LinearLayout.LayoutParams(px(76), px(76)));

        View divider = new View(this);
        divider.setBackgroundColor(0x55FFFFFF);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(Math.max(1, px(2)), px(56));
        dlp.leftMargin = px(24);
        dlp.rightMargin = px(24);
        left.addView(divider, dlp);

        LinearLayout names = new LinearLayout(this);
        names.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("", px(28), Color.WHITE, Typeface.DEFAULT_BOLD);
        SpannableString ss = new SpannableString("MANSON LAUNCHER");
        ss.setSpan(new ForegroundColorSpan(0xFF1E8CFF), 7, 15, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setText(ss);
        title.setLetterSpacing(0.16f);
        TextView sub = text(tr("SUA TV, DO SEU JEITO"), px(16), 0xFF9AA7C0, Typeface.DEFAULT);
        sub.setLetterSpacing(0.2f);
        LinearLayout.LayoutParams sublp = new LinearLayout.LayoutParams(WRAP, WRAP);
        sublp.topMargin = px(6);
        names.addView(title, new LinearLayout.LayoutParams(WRAP, WRAP));
        names.addView(sub, sublp);
        left.addView(names, new LinearLayout.LayoutParams(WRAP, WRAP));
        header.addView(left, new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.START | Gravity.CENTER_VERTICAL));

        LinearLayout right = new LinearLayout(this);
        right.setOrientation(LinearLayout.HORIZONTAL);
        right.setGravity(Gravity.CENTER_VERTICAL);
        right.setClipChildren(false);

        RoundBtn live = new RoundBtn(this, 0);
        live.setOnClickListener(v -> openLiveTv());
        RoundBtn hdmi = new RoundBtn(this, 1);
        hdmi.setOnClickListener(v -> openHdmi());
        RoundBtn sys = new RoundBtn(this, 2);
        sys.setOnClickListener(v -> openSystemSettings());
        RoundBtn[] btns = {live, hdmi, sys};
        for (RoundBtn b : btns) {
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px(80), px(80));
            lp.leftMargin = px(20);
            right.addView(b, lp);
        }
        header.addView(right, new FrameLayout.LayoutParams(WRAP, WRAP, Gravity.END | Gravity.CENTER_VERTICAL));
        return header;
    }

    View buildClockBlock() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(px(84), px(4), 0, 0);

        clock = text("--:--", px(150), Color.WHITE, Typeface.create("sans-serif-light", Typeface.NORMAL));
        clock.setShadowLayer(12, 0, 2, 0x66000000);
        box.addView(clock, new LinearLayout.LayoutParams(WRAP, WRAP));

        date = text("", px(34), 0xFFE6ECF7, Typeface.DEFAULT);
        LinearLayout.LayoutParams dlp = new LinearLayout.LayoutParams(WRAP, WRAP);
        dlp.topMargin = px(8);
        box.addView(date, dlp);

        weatherRow = new LinearLayout(this);
        weatherRow.setOrientation(LinearLayout.HORIZONTAL);
        weatherRow.setGravity(Gravity.CENTER_VERTICAL);
        weatherIcon = new WeatherIcon(this);
        weatherRow.addView(weatherIcon, new LinearLayout.LayoutParams(px(48), px(48)));
        weatherText = text("", px(28), 0xFFDDE6F5, Typeface.DEFAULT);
        LinearLayout.LayoutParams wtlp = new LinearLayout.LayoutParams(WRAP, WRAP);
        wtlp.leftMargin = px(16);
        weatherRow.addView(weatherText, wtlp);
        weatherRow.setVisibility(View.GONE);
        LinearLayout.LayoutParams wlp = new LinearLayout.LayoutParams(WRAP, WRAP);
        wlp.topMargin = px(14);
        box.addView(weatherRow, wlp);

        hint = text(tr("MODO ORGANIZAR: OK pega ou solta o app · setas movem · VOLTAR termina"),
                px(22), 0xFFFFE08A, Typeface.DEFAULT);
        hint.setVisibility(View.GONE);
        LinearLayout.LayoutParams hlp = new LinearLayout.LayoutParams(WRAP, WRAP);
        hlp.topMargin = px(10);
        box.addView(hint, hlp);
        return box;
    }

    void configureGrid() {
        int pad = px(84);
        int gap = px(32);
        int w = getResources().getDisplayMetrics().widthPixels;
        cardW = (w - 2 * pad - (cols - 1) * gap) / cols;
        cardH = cardW * 9 / 16;
        grid.setNumColumns(cols);
        grid.setHorizontalSpacing(gap);
        grid.setVerticalSpacing(px(36));
        grid.setPadding(pad, px(24), pad, px(24));
    }

    class WeatherIcon extends View {
        int kind = 2;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        WeatherIcon(Context c) {
            super(c);
        }

        void setKind(int kd) {
            kind = kd;
            invalidate();
        }

        void cloud(Canvas cv, float cx, float cy, float s) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFFDDE6F5);
            cv.drawCircle(cx - s * 0.30f, cy + s * 0.05f, s * 0.30f, p);
            cv.drawCircle(cx + s * 0.05f, cy - s * 0.15f, s * 0.40f, p);
            cv.drawCircle(cx + s * 0.40f, cy + s * 0.08f, s * 0.28f, p);
            cv.drawRect(cx - s * 0.30f, cy + s * 0.05f, cx + s * 0.40f, cy + s * 0.36f, p);
        }

        @Override
        protected void onDraw(Canvas cv) {
            float cx = getWidth() / 2f;
            float cy = getHeight() / 2f;
            float s = Math.min(getWidth(), getHeight()) / 2f;
            if (kind == 0) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFFFFD54A);
                cv.drawCircle(cx, cy, s * 0.38f, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeCap(Paint.Cap.ROUND);
                p.setStrokeWidth(Math.max(2f, s * 0.10f));
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    cv.drawLine(cx + (float) (Math.cos(a) * s * 0.58f), cy + (float) (Math.sin(a) * s * 0.58f),
                            cx + (float) (Math.cos(a) * s * 0.84f), cy + (float) (Math.sin(a) * s * 0.84f), p);
                }
            } else if (kind == 1) {
                Path m = new Path();
                m.addCircle(cx, cy, s * 0.62f, Path.Direction.CW);
                Path cut = new Path();
                cut.addCircle(cx + s * 0.30f, cy - s * 0.18f, s * 0.56f, Path.Direction.CW);
                m.op(cut, Path.Op.DIFFERENCE);
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFFE9EEF9);
                cv.drawPath(m, p);
            } else if (kind == 2) {
                cloud(cv, cx, cy, s);
            } else {
                cloud(cv, cx, cy - s * 0.18f, s * 0.9f);
                if (kind == 3) {
                    p.setStyle(Paint.Style.STROKE);
                    p.setStrokeCap(Paint.Cap.ROUND);
                    p.setStrokeWidth(Math.max(2f, s * 0.09f));
                    p.setColor(0xFF4FC3F7);
                    for (int i = -1; i <= 1; i++) {
                        float x = cx + i * s * 0.34f;
                        cv.drawLine(x + s * 0.06f, cy + s * 0.36f, x - s * 0.06f, cy + s * 0.74f, p);
                    }
                } else if (kind == 4) {
                    Path b = new Path();
                    b.moveTo(cx + s * 0.05f, cy + s * 0.20f);
                    b.lineTo(cx - s * 0.15f, cy + s * 0.52f);
                    b.lineTo(cx, cy + s * 0.52f);
                    b.lineTo(cx - s * 0.08f, cy + s * 0.82f);
                    b.lineTo(cx + s * 0.20f, cy + s * 0.44f);
                    b.lineTo(cx + s * 0.05f, cy + s * 0.44f);
                    b.close();
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(0xFFFFD54A);
                    cv.drawPath(b, p);
                } else {
                    p.setStyle(Paint.Style.FILL);
                    p.setColor(Color.WHITE);
                    for (int i = -1; i <= 1; i++) {
                        cv.drawCircle(cx + i * s * 0.34f, cy + s * 0.58f, s * 0.08f, p);
                    }
                }
            }
        }
    }

    class RoundBtn extends View {
        final int kind;
        boolean f = false;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        RoundBtn(Context c, int kind) {
            super(c);
            this.kind = kind;
            setFocusable(true);
            setClickable(true);
        }

        @Override
        protected void onFocusChanged(boolean gain, int direction, Rect prev) {
            super.onFocusChanged(gain, direction, prev);
            f = gain;
            if (gain) navSound();
            setScaleX(gain ? 1.12f : 1f);
            setScaleY(gain ? 1.12f : 1f);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas cv) {
            float w = getWidth();
            float h = getHeight();
            float cx = w / 2f;
            float cy = h / 2f;
            float r = Math.min(w, h) / 2f - px(4);

            p.setStyle(Paint.Style.FILL);
            p.setColor(f ? ((accent & 0x00FFFFFF) | 0x55000000) : 0x22FFFFFF);
            cv.drawCircle(cx, cy, r, p);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(Math.max(2, px(f ? 4 : 2)));
            p.setColor(f ? accent : 0xB3FFFFFF);
            cv.drawCircle(cx, cy, r, p);

            float s = r;
            p.setColor(Color.WHITE);
            p.setStrokeWidth(Math.max(2, px(4)));
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);

            if (kind == 0) {
                RectF rf = new RectF(cx - s * 0.55f, cy - s * 0.38f, cx + s * 0.55f, cy + s * 0.30f);
                p.setStyle(Paint.Style.STROKE);
                cv.drawRoundRect(rf, px(6), px(6), p);
                Path tri = new Path();
                tri.moveTo(cx - s * 0.10f, cy - s * 0.18f);
                tri.lineTo(cx + s * 0.20f, cy - s * 0.04f);
                tri.lineTo(cx - s * 0.10f, cy + s * 0.10f);
                tri.close();
                p.setStyle(Paint.Style.FILL);
                cv.drawPath(tri, p);
                p.setStyle(Paint.Style.STROKE);
                cv.drawLine(cx - s * 0.25f, cy + s * 0.44f, cx + s * 0.25f, cy + s * 0.44f, p);
            } else if (kind == 1) {
                Path hp = new Path();
                hp.moveTo(cx - s * 0.45f, cy - s * 0.30f);
                hp.lineTo(cx + s * 0.45f, cy - s * 0.30f);
                hp.lineTo(cx + s * 0.45f, cy + s * 0.05f);
                hp.lineTo(cx + s * 0.22f, cy + s * 0.32f);
                hp.lineTo(cx - s * 0.22f, cy + s * 0.32f);
                hp.lineTo(cx - s * 0.45f, cy + s * 0.05f);
                hp.close();
                p.setStyle(Paint.Style.STROKE);
                cv.drawPath(hp, p);
                cv.drawLine(cx - s * 0.18f, cy - s * 0.12f, cx - s * 0.18f, cy + s * 0.10f, p);
                cv.drawLine(cx, cy - s * 0.12f, cx, cy + s * 0.10f, p);
                cv.drawLine(cx + s * 0.18f, cy - s * 0.12f, cx + s * 0.18f, cy + s * 0.10f, p);
            } else {
                final double[] offs = {-0.28, -0.20, 0.20, 0.28};
                final float r2 = s * 0.52f;
                final float r1 = s * 0.40f;
                final float[] rr = {r1, r2, r2, r1};
                Path g = new Path();
                g.setFillType(Path.FillType.EVEN_ODD);
                for (int t = 0; t < 8; t++) {
                    double a = t * Math.PI / 4;
                    for (int j = 0; j < 4; j++) {
                        float x = cx + (float) (rr[j] * Math.cos(a + offs[j]));
                        float y = cy + (float) (rr[j] * Math.sin(a + offs[j]));
                        if (t == 0 && j == 0) g.moveTo(x, y);
                        else g.lineTo(x, y);
                    }
                }
                g.close();
                g.addCircle(cx, cy, s * 0.18f, Path.Direction.CW);
                p.setStyle(Paint.Style.FILL);
                cv.drawPath(g, p);
            }
        }
    }

    class SettingsCard extends LinearLayout {
        final GradientDrawable bg;

        SettingsCard(Context c) {
            super(c);
            setOrientation(HORIZONTAL);
            setGravity(Gravity.CENTER_VERTICAL);
            setFocusable(true);
            setClickable(true);
            setPadding(px(30), px(14), px(30), px(14));

            ImageView ic = new ImageView(c);
            ic.setImageResource(R.drawable.ic_manson);
            addView(ic, new LinearLayout.LayoutParams(px(84), px(84)));

            LinearLayout t = new LinearLayout(c);
            t.setOrientation(VERTICAL);
            t.setPadding(px(22), 0, 0, 0);
            TextView a = text(tr("Configurações do"), px(24), 0xFFDDE6F5, Typeface.DEFAULT);
            TextView b = text("MansonLauncher", px(30), accent, Typeface.DEFAULT_BOLD);
            t.addView(a, new LinearLayout.LayoutParams(WRAP, WRAP));
            t.addView(b, new LinearLayout.LayoutParams(WRAP, WRAP));
            addView(t, new LinearLayout.LayoutParams(WRAP, WRAP));

            bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0xFF101B33, 0xFF0A1226});
            bg.setCornerRadius(px(22));
            setBackground(bg);
            look(false);
        }

        void look(boolean f) {
            bg.setStroke(Math.max(1, px(f ? 4 : 1)), f ? accent : 0x33FFFFFF);
            setScaleX(f ? 1.04f : 1f);
            setScaleY(f ? 1.04f : 1f);
            setTranslationZ(f ? px(12) : 0);
        }

        @Override
        protected void onFocusChanged(boolean gain, int direction, Rect prev) {
            super.onFocusChanged(gain, direction, prev);
            if (gain) navSound();
            look(gain);
        }
    }

    class Cell extends FrameLayout {
        final ImageView img;
        final TextView label;
        final GradientDrawable bg;
        final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        final RectF rr = new RectF();
        boolean focus = false;
        boolean pick = false;

        Cell(Context c) {
            super(c);
            setLayoutParams(new AbsListView.LayoutParams(cardW, cardH));
            bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                    new int[]{0xFF15233F, 0xFF0A1226});
            bg.setCornerRadius(px(20));
            setBackground(bg);
            setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View v, Outline o) {
                    o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), px(20));
                }
            });
            setClipToOutline(true);

            img = new ImageView(c);
            addView(img, new FrameLayout.LayoutParams(MATCH, MATCH));

            label = text("", px(20), Color.WHITE, Typeface.DEFAULT);
            label.setSingleLine(true);
            label.setEllipsize(TextUtils.TruncateAt.END);
            label.setGravity(Gravity.CENTER);
            label.setPadding(px(10), 0, px(10), px(12));
            addView(label, new FrameLayout.LayoutParams(MATCH, WRAP, Gravity.BOTTOM));

            ring.setStyle(Paint.Style.STROKE);
            setLook(false, false);
        }

        void bind(Entry e) {
            if (e.banner != null) {
                img.setScaleType(ImageView.ScaleType.CENTER_CROP);
                img.setPadding(0, 0, 0, 0);
                img.setImageDrawable(e.banner);
                label.setVisibility(View.GONE);
            } else {
                img.setScaleType(ImageView.ScaleType.FIT_CENTER);
                int p = px(18);
                img.setPadding(p, p, p, px(50));
                img.setImageDrawable(e.icon);
                label.setText(e.label);
                label.setVisibility(View.VISIBLE);
            }
        }

        void setLook(boolean f, boolean p) {
            focus = f;
            pick = p;
            float s = p ? 1.12f : (f ? 1.07f : 1f);
            setScaleX(s);
            setScaleY(s);
            setTranslationZ((f || p) ? px(14) : 0);
            invalidate();
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            super.dispatchDraw(canvas);
            float sw = Math.max(1f, px((focus || pick) ? 4 : 1));
            ring.setStrokeWidth(sw);
            ring.setColor(pick ? 0xFFFFC857 : (focus ? accent : 0x33FFFFFF));
            rr.set(sw / 2f, sw / 2f, getWidth() - sw / 2f, getHeight() - sw / 2f);
            canvas.drawRoundRect(rr, px(20), px(20), ring);
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
            c.bind(entries.get(i));
            c.setLook(grid.hasFocus() && i == selected, organizing && picked && i == selected);
            return c;
        }
    }

    void applyFocus() {
        int first = grid.getFirstVisiblePosition();
        boolean hasFocus = grid.hasFocus();
        for (int i = 0; i < grid.getChildCount(); i++) {
            View v = grid.getChildAt(i);
            if (v instanceof Cell) {
                int pos = first + i;
                ((Cell) v).setLook(hasFocus && pos == selected, organizing && picked && pos == selected);
            }
        }
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
                    e.banner = ri.activityInfo.loadBanner(pm);
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
        for (Entry e : entries) {
            if (e.pkg != null) sb.append(e.pkg).append('\n');
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
            int ra = rank.containsKey(a.pkg) ? rank.get(a.pkg) : Integer.MAX_VALUE;
            int rb = rank.containsKey(b.pkg) ? rank.get(b.pkg) : Integer.MAX_VALUE;
            if (ra != rb) return Integer.compare(ra, rb);
            return a.label.compareToIgnoreCase(b.label);
        });
        entries.clear();
        entries.addAll(vis);
        adapter.notifyDataSetChanged();
    }

    void activate(Entry e) {
        if (organizing) return;
        try {
            startActivity(e.intent);
        } catch (Exception ex) {
            Toast.makeText(this, tr("Não foi possível abrir") + " " + e.label, Toast.LENGTH_SHORT).show();
        }
    }

    void openLiveTv() {
        PackageManager pm = getPackageManager();
        for (String p : LIVE_TV_PKGS) {
            Intent i = pm.getLeanbackLaunchIntentForPackage(p);
            if (i == null) i = pm.getLaunchIntentForPackage(p);
            if (i != null) {
                try {
                    startActivity(i);
                    return;
                } catch (Exception ignored) {
                }
            }
        }
        Toast.makeText(this, tr("Não encontrei o app de TV aberta nesta TV."), Toast.LENGTH_LONG).show();
    }

    void openHdmi() {
        TvInputManager tm = (TvInputManager) getSystemService(Context.TV_INPUT_SERVICE);
        final List<TvInputInfo> hdmi = new ArrayList<>();
        if (tm != null) {
            try {
                for (TvInputInfo info : tm.getTvInputList()) {
                    if (info.getType() == TvInputInfo.TYPE_HDMI) hdmi.add(info);
                }
            } catch (Exception ignored) {
            }
        }
        if (hdmi.isEmpty()) {
            Toast.makeText(this, tr("Não encontrei entradas HDMI nesta TV."), Toast.LENGTH_LONG).show();
            return;
        }
        if (hdmi.size() == 1) {
            launchInput(hdmi.get(0));
            return;
        }
        String[] names = new String[hdmi.size()];
        for (int i = 0; i < names.length; i++) {
            CharSequence l = hdmi.get(i).loadLabel(this);
            names[i] = (l == null || l.length() == 0) ? ("HDMI " + (i + 1)) : String.valueOf(l);
        }
        new AlertDialog.Builder(this).setTitle(tr("Entradas HDMI"))
                .setItems(names, (d, which) -> launchInput(hdmi.get(which))).show();
    }

    void launchInput(TvInputInfo info) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW,
                    TvContract.buildChannelUriForPassthroughInput(info.getId())));
        } catch (Exception e) {
            Toast.makeText(this, tr("Não foi possível abrir essa entrada."), Toast.LENGTH_LONG).show();
        }
    }

    void openSystemSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Toast.makeText(this, tr("Não foi possível abrir as configurações da TV."), Toast.LENGTH_LONG).show();
        }
    }

    // ---------------- Clima ----------------

    String httpGet(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) bo.write(buf, 0, n);
            return bo.toString("UTF-8");
        } finally {
            c.disconnect();
        }
    }

    void refreshWeather(boolean force) {
        if (!prefs.getBoolean("w_on", true)) return;
        long last = prefs.getLong("w_time", 0);
        if (!force && System.currentTimeMillis() - last < 30 * 60 * 1000L) return;
        final String lat = prefs.getString("city_lat", "-23.55");
        final String lon = prefs.getString("city_lon", "-46.63");
        new Thread(() -> {
            try {
                String json = httpGet("https://api.open-meteo.com/v1/forecast?latitude=" + lat
                        + "&longitude=" + lon
                        + "&current=temperature_2m,weather_code,is_day"
                        + "&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1");
                prefs.edit().putString("w_json", json).putLong("w_time", System.currentTimeMillis()).apply();
                ui.post(this::updateWeatherUi);
            } catch (Exception ignored) {
            }
        }).start();
    }

    String fmtTemp(double c, boolean f) {
        return Math.round(f ? c * 9.0 / 5.0 + 32.0 : c) + "°";
    }

    String describe(int c, boolean day) {
        if (c == 0) return tr(day ? "Céu limpo" : "Noite limpa");
        if (c == 1) return tr(day ? "Poucas nuvens" : "Noite com poucas nuvens");
        if (c == 2) return tr("Parcialmente nublado");
        if (c == 3) return tr("Nublado");
        if (c == 45 || c == 48) return tr("Neblina");
        if (c >= 51 && c <= 57) return tr("Garoa");
        if ((c >= 61 && c <= 67) || (c >= 80 && c <= 82)) return tr("Chuva");
        if ((c >= 71 && c <= 77) || c == 85 || c == 86) return tr("Neve");
        if (c >= 95) return tr("Tempestade");
        return tr("Tempo variável");
    }

    int iconKind(int c, boolean day) {
        if (c == 0 || c == 1) return day ? 0 : 1;
        if (c >= 95) return 4;
        if ((c >= 51 && c <= 57) || (c >= 61 && c <= 67) || (c >= 80 && c <= 82)) return 3;
        if ((c >= 71 && c <= 77) || c == 85 || c == 86) return 5;
        return 2;
    }

    void updateWeatherUi() {
        boolean on = prefs.getBoolean("w_on", true);
        String json = prefs.getString("w_json", "");
        if (!on || json.isEmpty()) {
            weatherRow.setVisibility(View.GONE);
            return;
        }
        try {
            JSONObject jo = new JSONObject(json);
            JSONObject cur = jo.getJSONObject("current");
            double t = cur.getDouble("temperature_2m");
            int code = cur.getInt("weather_code");
            boolean day = cur.getInt("is_day") == 1;
            JSONObject daily = jo.getJSONObject("daily");
            double mx = daily.getJSONArray("temperature_2m_max").getDouble(0);
            double mn = daily.getJSONArray("temperature_2m_min").getDouble(0);
            boolean f = prefs.getBoolean("unit_f", false);
            weatherText.setText(fmtTemp(t, f) + "   " + describe(code, day)
                    + "   " + tr("Máx.") + " " + fmtTemp(mx, f) + "   " + tr("Mín.") + " " + fmtTemp(mn, f));
            weatherIcon.setKind(iconKind(code, day));
            weatherRow.setVisibility(View.VISIBLE);
        } catch (Exception e) {
            weatherRow.setVisibility(View.GONE);
        }
    }

    void setCity(String name, String lat, String lon) {
        prefs.edit().putString("city_name", name).putString("city_lat", lat).putString("city_lon", lon)
                .remove("w_json").remove("w_time").apply();
        updateWeatherUi();
        refreshWeather(true);
        Toast.makeText(this, tr("Cidade") + ": " + name, Toast.LENGTH_SHORT).show();
    }

    void showCities() {
        String[] names = new String[CITIES.length + 1];
        for (int i = 0; i < CITIES.length; i++) names[i] = CITIES[i][0];
        names[CITIES.length] = tr("Buscar outra cidade...");
        new AlertDialog.Builder(this).setTitle(tr("Cidade")).setItems(names, (d, which) -> {
            if (which == CITIES.length) {
                searchCity();
            } else {
                setCity(CITIES[which][0], CITIES[which][1], CITIES[which][2]);
            }
        }).show();
    }

    void searchCity() {
        final EditText et = new EditText(this);
        et.setSingleLine(true);
        et.setHint(tr("Nome da cidade"));
        new AlertDialog.Builder(this).setTitle(tr("Buscar cidade")).setView(et)
                .setPositiveButton(tr("Buscar"), (d, w) -> doSearch(et.getText().toString().trim()))
                .setNegativeButton(tr("Cancelar"), null).show();
    }

    void doSearch(final String q) {
        if (q.isEmpty()) return;
        new Thread(() -> {
            try {
                String json = httpGet("https://geocoding-api.open-meteo.com/v1/search?name="
                        + URLEncoder.encode(q, "UTF-8") + "&count=8&language=pt&format=json");
                JSONObject jo = new JSONObject(json);
                JSONArray rs = jo.optJSONArray("results");
                final List<String[]> found = new ArrayList<>();
                if (rs != null) {
                    for (int i = 0; i < rs.length(); i++) {
                        JSONObject r = rs.getJSONObject(i);
                        String label = r.optString("name");
                        if (r.has("admin1")) label += " - " + r.optString("admin1");
                        if (r.has("country")) label += ", " + r.optString("country");
                        found.add(new String[]{r.optString("name"),
                                String.valueOf(r.getDouble("latitude")),
                                String.valueOf(r.getDouble("longitude")), label});
                    }
                }
                ui.post(() -> showResults(found));
            } catch (Exception e) {
                ui.post(() -> Toast.makeText(this, tr("Não foi possível buscar agora."), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    void showResults(final List<String[]> found) {
        if (found.isEmpty()) {
            Toast.makeText(this, tr("Nenhuma cidade encontrada."), Toast.LENGTH_LONG).show();
            return;
        }
        String[] names = new String[found.size()];
        for (int i = 0; i < names.length; i++) names[i] = found.get(i)[3];
        new AlertDialog.Builder(this).setTitle(tr("Escolha a cidade")).setItems(names, (d, which) ->
                setCity(found.get(which)[0], found.get(which)[1], found.get(which)[2])).show();
    }

    // ---------------- Ajustes ----------------

    void showSettings() {
        String[] items = {
                tr("Ajustes do Início (idioma, clima, cidade, sons)..."),
                tr("Fundo e temas..."),
                tr("Apps por linha") + ": " + cols + "...",
                tr("Escolher quais apps aparecem..."),
                tr("Organizar apps (mover)..."),
                tr("Restaurar ordem alfabética")
        };
        new AlertDialog.Builder(this).setTitle(tr("Ajustes")).setItems(items, (d, which) -> {
            switch (which) {
                case 0:
                    showHomeSettings();
                    break;
                case 1:
                    showBackgrounds();
                    break;
                case 2:
                    showCols();
                    break;
                case 3:
                    showVisible();
                    break;
                case 4:
                    startOrganize();
                    break;
                default:
                    prefs.edit().remove("order").apply();
                    rebuild();
            }
        }).show();
    }

    void showBackgrounds() {
        String[] items = {
                tr("Tema") + ": " + tr(THEME_NAMES[theme()]) + "...",
                tr("Fundo padrão (do tema)"),
                tr("Fundo de cor lisa..."),
                tr("Fundo com foto: escolher arquivo..."),
                tr("Fundo com foto: usar a enviada pelo Termux"),
                tr("Ajuda: enviar foto pelo Termux")
        };
        new AlertDialog.Builder(this).setTitle(tr("Fundo e temas")).setItems(items, (d, which) -> {
            switch (which) {
                case 0:
                    showThemes();
                    break;
                case 1:
                    prefs.edit().putInt("bg", 0).apply();
                    applyBackground();
                    break;
                case 2:
                    showColors();
                    break;
                case 3:
                    pickPhoto();
                    break;
                case 4:
                    usePushedPhoto();
                    break;
                default:
                    showHelp(tr("Como enviar uma foto:"));
            }
        }).show();
    }

    void showHomeSettings() {
        final boolean wOn = prefs.getBoolean("w_on", true);
        final boolean f = prefs.getBoolean("unit_f", false);
        final boolean snd = prefs.getBoolean("sounds", true);
        String[] items = {
                tr("Idioma") + ": " + LANG_NAMES[lang] + "...",
                tr("Widget de clima") + ": " + (wOn ? tr("Ligado") : tr("Desligado")),
                tr("Cidade") + ": " + prefs.getString("city_name", "São Paulo") + "...",
                tr("Temperatura") + ": " + (f ? "°F (Fahrenheit)" : "°C (Celsius)"),
                tr("Sons de navegação") + ": " + (snd ? tr("Ligados") : tr("Desligados"))
        };
        new AlertDialog.Builder(this).setTitle(tr("Ajustes do Início")).setItems(items, (d, which) -> {
            switch (which) {
                case 0:
                    showLanguages();
                    break;
                case 1:
                    prefs.edit().putBoolean("w_on", !wOn).apply();
                    updateWeatherUi();
                    if (!wOn) refreshWeather(false);
                    showHomeSettings();
                    break;
                case 2:
                    showCities();
                    break;
                case 3:
                    prefs.edit().putBoolean("unit_f", !f).apply();
                    updateWeatherUi();
                    showHomeSettings();
                    break;
                default:
                    prefs.edit().putBoolean("sounds", !snd).apply();
                    showHomeSettings();
            }
        }).show();
    }

    void showLanguages() {
        new AlertDialog.Builder(this).setTitle(tr("Idioma")).setItems(LANG_NAMES, (d, which) -> {
            prefs.edit().putInt("lang", which).apply();
            recreate();
        }).show();
    }

    void showThemes() {
        String[] names = new String[THEME_NAMES.length];
        for (int i = 0; i < names.length; i++) names[i] = tr(THEME_NAMES[i]);
        new AlertDialog.Builder(this).setTitle(tr("Tema")).setItems(names, (d, which) -> {
            prefs.edit().putInt("theme", which).apply();
            recreate();
        }).show();
    }

    void showVisible() {
        final int n = all.size();
        final String[] names = new String[n];
        final boolean[] checked = new boolean[n];
        for (int i = 0; i < n; i++) {
            names[i] = all.get(i).label;
            checked[i] = !hidden.contains(all.get(i).pkg);
        }
        new AlertDialog.Builder(this)
                .setTitle(tr("Apps que aparecem"))
                .setMultiChoiceItems(names, checked, (d, which, isChecked) -> checked[which] = isChecked)
                .setPositiveButton(tr("Salvar"), (d, w) -> {
                    Set<String> h = new HashSet<>();
                    for (int i = 0; i < n; i++) {
                        if (!checked[i]) h.add(all.get(i).pkg);
                    }
                    hidden = h;
                    prefs.edit().putStringSet("hidden", new HashSet<>(h)).apply();
                    selected = 0;
                    rebuild();
                })
                .setNegativeButton(tr("Cancelar"), null)
                .show();
    }

    void startOrganize() {
        if (entries.isEmpty()) return;
        organizing = true;
        picked = false;
        hint.setVisibility(View.VISIBLE);
        if (selected >= entries.size()) selected = 0;
        grid.requestFocus();
        grid.setSelection(selected);
        applyFocus();
    }

    void exitOrganize() {
        organizing = false;
        picked = false;
        hint.setVisibility(View.GONE);
        saveOrder();
        applyFocus();
    }

    void moveSelected(int delta) {
        int from = selected;
        int to = selected + delta;
        if (from < 0 || to < 0 || to >= entries.size()) return;
        Entry e = entries.remove(from);
        entries.add(to, e);
        selected = to;
        saveOrder();
        adapter.notifyDataSetChanged();
        grid.setSelection(to);
        applyFocus();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent ev) {
        if (organizing && ev.getAction() == KeyEvent.ACTION_DOWN) {
            int code = ev.getKeyCode();
            if (code == KeyEvent.KEYCODE_BACK) {
                exitOrganize();
                return true;
            }
            if (code == KeyEvent.KEYCODE_DPAD_CENTER || code == KeyEvent.KEYCODE_ENTER) {
                if (ev.getRepeatCount() == 0 && selected >= 0) {
                    picked = !picked;
                    applyFocus();
                }
                return true;
            }
            if (picked) {
                if (code == KeyEvent.KEYCODE_DPAD_LEFT) {
                    moveSelected(-1);
                    return true;
                }
                if (code == KeyEvent.KEYCODE_DPAD_RIGHT) {
                    moveSelected(1);
                    return true;
                }
                if (code == KeyEvent.KEYCODE_DPAD_UP) {
                    moveSelected(-cols);
                    return true;
                }
                if (code == KeyEvent.KEYCODE_DPAD_DOWN) {
                    moveSelected(cols);
                    return true;
                }
            }
        }
        return super.dispatchKeyEvent(ev);
    }

    void showColors() {
        String[] names = new String[SOLID_NAMES.length];
        for (int i = 0; i < names.length; i++) names[i] = tr(SOLID_NAMES[i]);
        new AlertDialog.Builder(this).setTitle(tr("Cor lisa")).setItems(names, (d, which) -> {
            prefs.edit().putInt("bg", 1).putInt("color", which).apply();
            applyBackground();
        }).show();
    }

    void showCols() {
        String[] names = new String[COLS.length];
        for (int i = 0; i < COLS.length; i++) names[i] = COLS[i] + " " + tr("apps por linha");
        new AlertDialog.Builder(this).setTitle(tr("Apps por linha")).setItems(names, (d, which) -> {
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
                .setTitle(tr("Foto de fundo"))
                .setMessage(intro + "\n\n" + tr("No Termux, com a TV conectada:")
                        + "\n\nadb push foto.jpg " + HELP_PATH + "\n\n"
                        + tr("Depois abra Ajustes > Fundo e temas e escolha: Fundo com foto: usar a enviada pelo Termux."))
                .setPositiveButton("OK", null)
                .show();
    }

    void usePushedPhoto() {
        if (loadPhoto() != null) {
            prefs.edit().putInt("bg", 2).apply();
            applyBackground();
        } else {
            showHelp(tr("Nenhuma foto enviada ainda."));
        }
    }

    void pickPhoto() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(i, REQ_PICK);
        } catch (ActivityNotFoundException e) {
            showHelp(tr("Esta TV não tem seletor de arquivos."));
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
            Toast.makeText(this, tr("Não foi possível usar essa foto."), Toast.LENGTH_LONG).show();
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
        root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, THEME_BG[theme()]));
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
        updateWeatherUi();
        refreshWeather(false);
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
        grid.requestFocus();
    }

    @Override
    public void onBackPressed() {
    }
}
