package id.sooon.dashlock;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Pengaturan DashLock: PIN, izin, toggle aktif/nonaktif.
 */
public class MainActivity extends Activity {

    private TextView statusOverlay, statusA11y, hint, help, holdLabel;
    private Switch toggle, lockConfirmSwitch, unlockPinSwitch;
    private android.widget.RadioGroup unlockModeGroup;
    private android.widget.RadioButton holdModeBtn, tapModeBtn;
    private LinearLayout holdSecRow;
    private final Button[] holdSecBtns = new Button[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        statusOverlay.setText(Perm.overlay(this) ? "Overlay: diizinkan" : "Overlay: belum");
        statusOverlay.setTextColor(Perm.overlay(this) ? 0xFF4CAF50 : 0xFFE57373);
        statusA11y.setText(Perm.accessibility(this) ? "Aksesibilitas: aktif" : "Aksesibilitas: belum");
        statusA11y.setTextColor(Perm.accessibility(this) ? 0xFF4CAF50 : 0xFFE57373);
        boolean locked = Prefs.isLocked(this);
        toggle.setEnabled(!locked);
        toggle.setChecked(Prefs.isEnabled(this));
        if (locked) {
            hint.setText(unlockHint());
        } else if (Prefs.unlockRequirePin(this) && !Prefs.hasPin(this)) {
            hint.setText("Aktifkan PIN buka kunci dan atur PIN, atau matikan PIN buka kunci.");
        } else if (!Perm.ready(this)) {
            hint.setText("Lengkapi izin di bawah, lalu aktifkan DashLock.");
        } else {
            hint.setText("DashLock siap. Tombol kuning transparan muncul di atas aplikasi lain.");
        }
        if (unlockModeGroup != null) {
            int mode = Prefs.unlockMode(this);
            if (mode == Prefs.UNLOCK_TRIPLE_TAP) tapModeBtn.setChecked(true);
            else holdModeBtn.setChecked(true);
            holdSecRow.setVisibility(mode == Prefs.UNLOCK_HOLD ? View.VISIBLE : View.GONE);
            holdLabel.setText("Durasi tahan buka kunci");
            int active = Prefs.unlockHoldSeconds(this);
            for (int i = 0; i < holdSecBtns.length; i++) {
                if (holdSecBtns[i] != null) styleChoiceBtn(holdSecBtns[i], i + 1 == active);
            }
        }
        if (help != null) help.setText(helpText());
        if (lockConfirmSwitch != null) {
            lockConfirmSwitch.setChecked(Prefs.lockConfirm(this));
        }
        if (unlockPinSwitch != null) {
            unlockPinSwitch.setChecked(Prefs.unlockRequirePin(this));
        }
    }

    private String unlockHint() {
        String pin = Prefs.unlockRequirePin(this) ? " lalu masukkan PIN" : "";
        if (Prefs.unlockMode(this) == Prefs.UNLOCK_TRIPLE_TAP) {
            return "Layar terkunci. Ketuk tombol 3× cepat" + pin + ".";
        }
        return "Layar terkunci. Tahan tombol " + Prefs.unlockHoldSeconds(this) + " detik" + pin + ".";
    }

    private String helpText() {
        String lock = Prefs.lockConfirm(this) ? "tahan tombol sebentar (konfirmasi)" : "tahan tombol sebentar";
        String unlock = Prefs.unlockMode(this) == Prefs.UNLOCK_TRIPLE_TAP
                ? "ketuk 3× cepat" : "tahan " + Prefs.unlockHoldSeconds(this) + " detik";
        if (Prefs.unlockRequirePin(this)) unlock += " lalu PIN";
        return "Kunci: " + lock + ". Buka: " + unlock
                + ". Geser tombol untuk pindah; lepas menempel kiri/kanan.";
    }

    private View buildUi() {
        int pad = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0B0B0A);
        root.setPadding(pad, pad, pad, pad);

        TextView title = text("DashLock", 28, true, 0xFFECE6D8);
        root.addView(title);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        // —— Status DashLock ——
        LinearLayout statusSec = section(content, "Status DashLock");
        hint = text("", 14, false, 0xFF8A8478);
        statusSec.addView(hint);
        statusSec.addView(spacer(dp(10)));
        LinearLayout toggleRow = row();
        toggleRow.addView(text("Aktifkan DashLock", 17, false, 0xFFECE6D8),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        toggle = new Switch(this);
        toggle.setOnCheckedChangeListener(this::onToggle);
        toggleRow.addView(toggle);
        statusSec.addView(toggleRow);
        help = text(helpText(), 12, false, 0xFF4A4740);
        help.setPadding(0, dp(12), 0, 0);
        statusSec.addView(help);

        // —— Kunci ——
        LinearLayout lockSec = section(content, "Kunci");
        LinearLayout lockConfirmRow = row();
        lockConfirmRow.addView(text("Konfirmasi sebelum mengunci", 16, false, 0xFFECE6D8),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        lockConfirmSwitch = new Switch(this);
        lockConfirmSwitch.setOnCheckedChangeListener((b, on) -> {
            Prefs.setLockConfirm(this, on);
            refresh();
        });
        lockConfirmRow.addView(lockConfirmSwitch);
        lockSec.addView(lockConfirmRow);
        TextView lockNote = text("Tahan tombol floating sebentar untuk mengunci layar.", 12, false, 0xFF4A4740);
        lockNote.setPadding(0, dp(10), 0, 0);
        lockSec.addView(lockNote);

        // —— Buka kunci ——
        LinearLayout unlockSec = section(content, "Buka kunci");
        LinearLayout unlockPinRow = row();
        unlockPinRow.addView(text("Wajib PIN saat membuka", 16, false, 0xFFECE6D8),
                new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        unlockPinSwitch = new Switch(this);
        unlockPinSwitch.setOnCheckedChangeListener((b, on) -> {
            if (on && !Prefs.hasPin(this)) {
                unlockPinSwitch.setChecked(false);
                showPinDialog(() -> {
                    Prefs.setUnlockRequirePin(this, true);
                    refresh();
                });
                return;
            }
            Prefs.setUnlockRequirePin(this, on);
            refresh();
        });
        unlockPinRow.addView(unlockPinSwitch);
        unlockSec.addView(unlockPinRow);
        unlockSec.addView(btn("Atur / ganti PIN", false, this::showPinDialog));
        unlockSec.addView(spacer(dp(14)));
        unlockSec.addView(text("Gestur buka kunci", 14, true, 0xFF8A8478));
        unlockModeGroup = new android.widget.RadioGroup(this);
        unlockModeGroup.setOrientation(LinearLayout.VERTICAL);
        holdModeBtn = radio("Tahan tombol (1–3 detik)", Prefs.UNLOCK_HOLD);
        tapModeBtn = radio("Ketuk tiga kali", Prefs.UNLOCK_TRIPLE_TAP);
        unlockModeGroup.addView(holdModeBtn);
        unlockModeGroup.addView(tapModeBtn);
        unlockModeGroup.setOnCheckedChangeListener((g, id) -> {
            Prefs.setUnlockMode(this, id == tapModeBtn.getId() ? Prefs.UNLOCK_TRIPLE_TAP : Prefs.UNLOCK_HOLD);
            refresh();
            syncService();
        });
        unlockSec.addView(unlockModeGroup);
        holdSecRow = new LinearLayout(this);
        holdSecRow.setOrientation(LinearLayout.VERTICAL);
        holdLabel = text("", 13, false, 0xFF8A8478);
        holdLabel.setPadding(0, dp(8), 0, dp(4));
        holdSecRow.addView(holdLabel);
        LinearLayout secBtns = new LinearLayout(this);
        secBtns.setOrientation(LinearLayout.HORIZONTAL);
        for (int s = 1; s <= 3; s++) {
            int sec = s;
            Button b = btn(sec + " dtk", false, () -> {
                Prefs.setUnlockHoldSeconds(this, sec);
                refresh();
                syncService();
            });
            holdSecBtns[s - 1] = b;
            secBtns.addView(b, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        }
        holdSecRow.addView(secBtns);
        unlockSec.addView(holdSecRow);

        // —— Izin ——
        LinearLayout permSec = section(content, "Izin");
        statusOverlay = text("", 14, false, 0xFF8A8478);
        permSec.addView(statusOverlay);
        permSec.addView(btn("Buka pengaturan overlay", false, () -> {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(i);
        }));
        permSec.addView(spacer(dp(8)));
        statusA11y = text("", 14, false, 0xFF8A8478);
        permSec.addView(statusA11y);
        permSec.addView(btn("Buka pengaturan aksesibilitas", false, () ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))));

        return root;
    }

    private void syncService() {
        if (Prefs.isEnabled(this)) {
            Intent sync = new Intent(this, LockService.class);
            sync.setAction(LockService.ACTION_SYNC);
            startService(sync);
        }
    }

    /** Judul section + panel gelap; mengembalikan isi panel. */
    private LinearLayout section(LinearLayout parent, String title) {
        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams blockLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        blockLp.topMargin = dp(14);
        block.setLayoutParams(blockLp);

        TextView heading = text(title, 13, true, 0xFFFFB000);
        heading.setLetterSpacing(0.04f);
        block.addView(heading);

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setPadding(dp(14), dp(12), dp(14), dp(14));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF151513);
        bg.setCornerRadius(dp(10));
        panel.setBackground(bg);
        LinearLayout.LayoutParams panelLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        panelLp.topMargin = dp(6);
        block.addView(panel, panelLp);

        parent.addView(block);
        return panel;
    }

    private android.widget.RadioButton radio(String label, int mode) {
        android.widget.RadioButton r = new android.widget.RadioButton(this);
        r.setText(label);
        r.setTextColor(0xFFECE6D8);
        r.setId(mode == Prefs.UNLOCK_TRIPLE_TAP ? View.generateViewId() : View.generateViewId());
        if (mode == Prefs.UNLOCK_HOLD) holdModeBtn = r;
        if (mode == Prefs.UNLOCK_TRIPLE_TAP) tapModeBtn = r;
        return r;
    }

    private void onToggle(CompoundButton button, boolean checked) {
        if (checked) {
            if (Prefs.unlockRequirePin(this) && !Prefs.hasPin(this)) {
                toggle.setChecked(false);
                showPinDialog(null);
                return;
            }
            if (!Perm.overlay(this) || !Perm.accessibility(this)) {
                toggle.setChecked(false);
                new AlertDialog.Builder(this)
                        .setTitle("Izin belum lengkap")
                        .setMessage("Izinkan tampil di atas aplikasi lain dan aktifkan layanan DashLock di Aksesibilitas.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }
            Prefs.setEnabled(this, true);
            LockService.start(this);
        } else {
            if (Prefs.isLocked(this)) {
                toggle.setChecked(true);
                return;
            }
            Prefs.setEnabled(this, false);
            LockService.stop(this);
        }
        refresh();
    }

    private void showPinDialog() {
        showPinDialog(null);
    }

    private void showPinDialog(Runnable afterSave) {
        EditText pin = new EditText(this);
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pin.setHint("PIN baru (4–6 digit)");
        new AlertDialog.Builder(this)
                .setTitle("PIN DashLock")
                .setView(pin)
                .setPositiveButton("Simpan", (d, w) -> {
                    String p = pin.getText().toString().trim();
                    if (!PinStore.setPin(this, p)) {
                        new AlertDialog.Builder(this)
                                .setMessage("PIN harus 4–6 digit angka.")
                                .setPositiveButton("OK", null)
                                .show();
                    } else {
                        if (afterSave != null) afterSave.run();
                        refresh();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    private View spacer(int h) {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, h));
        return v;
    }

    private TextView text(String s, float sp, boolean bold, int color) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextColor(color);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) tv.setTypeface(Typeface.DEFAULT_BOLD);
        return tv;
    }

    private void styleChoiceBtn(Button b, boolean selected) {
        b.setTextColor(selected ? 0xFF0B0B0A : 0xFFECE6D8);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(selected ? 0xFFFFB000 : 0xFF151513);
        b.setBackground(bg);
    }

    private Button btn(String label, boolean primary, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        styleChoiceBtn(b, primary);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(6);
        b.setLayoutParams(lp);
        b.setOnClickListener(v -> action.run());
        return b;
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }
}
