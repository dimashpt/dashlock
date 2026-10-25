package id.sooon.dashlock;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Dua jendela overlay: perisai layar penuh + FAB; dialog konfirmasi/PIN di atas perisai.
 */
final class OverlayController implements FabView.Listener {
    private final Context app;
    private final WindowManager wm;
    private final int marginPx;
    private final LockService service;

    private View shield;
    private FabView fab;
    private FrameLayout modalRoot;
    private boolean attached;

    OverlayController(LockService service) {
        this.service = service;
        this.app = LocaleHelper.wrap(service);
        this.wm = (WindowManager) app.getSystemService(Context.WINDOW_SERVICE);
        marginPx = (int) (12 * app.getResources().getDisplayMetrics().density + 0.5f);
    }

    void attach() {
        if (attached) return;
        attached = true;
        shield = new View(app);
        shield.setBackgroundColor(0x01000000);
        shield.setOnTouchListener((v, e) -> true);
        wm.addView(shield, shieldLp(false));

        fab = new FabView(app);
        fab.listener = this;
        fab.setLocked(Prefs.isLocked(app));
        wm.addView(fab, fabLp());

        modalRoot = new FrameLayout(app);
        updateShield();
        bringFabToFront();
    }

    void detach() {
        if (!attached) return;
        attached = false;
        hideModalLayer();
        removeIfPresent(fab);
        removeIfPresent(shield);
        shield = null;
        fab = null;
        modalRoot = null;
    }

    void refreshState() {
        if (!attached) return;
        if (fab != null) {
            fab.reloadPrefs();
            fab.setLocked(Prefs.isLocked(app));
        }
        updateShield();
        service.updateNotification();
    }

    private void updateShield() {
        if (shield == null) return;
        boolean locked = Prefs.isLocked(app);
        shield.setVisibility(locked ? View.VISIBLE : View.GONE);
        try {
            wm.updateViewLayout(shield, shieldLp(locked));
        } catch (IllegalArgumentException ignored) { }
        bringFabToFront();
    }

    /** Jendela overlay terakhir yang ditambahkan paling atas; FAB harus di atas perisai/modal kosong. */
    private void bringFabToFront() {
        if (fab == null) return;
        try {
            WindowManager.LayoutParams lp = (WindowManager.LayoutParams) fab.getLayoutParams();
            wm.removeView(fab);
            wm.addView(fab, lp);
        } catch (IllegalArgumentException ignored) { }
    }

    private boolean modalShowing() {
        return modalRoot != null && modalRoot.getParent() != null;
    }

    private void showModalLayer() {
        if (modalRoot == null || modalShowing()) return;
        wm.addView(modalRoot, modalLp(true));
    }

    private void hideModalLayer() {
        if (modalRoot == null || !modalShowing()) return;
        modalRoot.removeAllViews();
        modalRoot.setBackgroundColor(Color.TRANSPARENT);
        try {
            wm.removeView(modalRoot);
        } catch (IllegalArgumentException ignored) { }
        bringFabToFront();
    }

    private void removeIfPresent(View v) {
        if (v != null) {
            try {
                wm.removeView(v);
            } catch (Exception ignored) { }
        }
    }

    private int overlayType() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        return WindowManager.LayoutParams.TYPE_PHONE;
    }

    private WindowManager.LayoutParams baseLp(int w, int h) {
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
        return new WindowManager.LayoutParams(w, h, overlayType(), flags, android.graphics.PixelFormat.TRANSLUCENT);
    }

    private WindowManager.LayoutParams shieldLp(boolean locked) {
        WindowManager.LayoutParams lp = baseLp(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        if (!locked) {
            lp.flags |= WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }
        return lp;
    }

    private WindowManager.LayoutParams modalLp(boolean interactive) {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                overlayType(),
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                android.graphics.PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        if (interactive) {
            lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
                    | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE;
        } else {
            lp.flags |= WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
        }
        return lp;
    }

    private void updateModalLayout(boolean interactive) {
        if (modalRoot == null || !modalShowing()) return;
        try {
            wm.updateViewLayout(modalRoot, modalLp(interactive));
        } catch (IllegalArgumentException ignored) { }
    }

    private WindowManager.LayoutParams fabLp() {
        int size = FabView.sizePx(app);
        WindowManager.LayoutParams lp = baseLp(size, size);
        lp.gravity = Gravity.TOP | Gravity.START;
        placeFab(lp, size);
        return lp;
    }

    private void placeFab(WindowManager.LayoutParams lp, int size) {
        android.util.DisplayMetrics dm = app.getResources().getDisplayMetrics();
        int y = Prefs.fabCenterY(app);
        if (y < 0) y = dm.heightPixels / 2;
        int minY = marginPx + size / 2;
        int maxY = dm.heightPixels - marginPx - size / 2;
        y = Math.max(minY, Math.min(maxY, y));
        int side = Prefs.fabSide(app);
        int x = side == Prefs.SIDE_LEFT ? marginPx : dm.widthPixels - marginPx - size;
        lp.x = x;
        lp.y = y - size / 2;
    }

    private void snapFab(int centerX, int centerY, int side) {
        Prefs.setFabSide(app, side);
        Prefs.setFabCenterY(app, centerY);
        if (fab == null) return;
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) fab.getLayoutParams();
        placeFab(lp, fab.getWidth());
        wm.updateViewLayout(fab, lp);
    }

    @Override
    public void onDragEnd(int centerX, int centerY, int side) {
        snapFab(centerX, centerY, side);
    }

    @Override
    public void onRequestLock() {
        if (Prefs.isLocked(app)) return;
        if (!Prefs.lockConfirm(app)) {
            Prefs.setLocked(app, true);
            if (fab != null) fab.cancelHold();
            refreshState();
            toast(app.getString(R.string.toast_locked));
            return;
        }
        showConfirmLock();
    }

    @Override
    public void onUnlockHoldComplete() {
        if (!Prefs.isLocked(app)) return;
        if (!Prefs.unlockRequirePin(app)) {
            if (fab != null) fab.cancelHold();
            Prefs.setLocked(app, false);
            refreshState();
            toast(app.getString(R.string.toast_unlocked));
            return;
        }
        showPinUnlock();
    }

    @Override
    public void onUnlockHoldCancelled() { }

    private void showConfirmLock() {
        if (modalRoot == null) return;
        showModalLayer();
        modalRoot.removeAllViews();
        modalRoot.setBackgroundColor(0x99000000);
        LinearLayout card = card();
        TextView title = label(app.getString(R.string.confirm_lock_title), 20, true);
        card.addView(title);
        card.addView(label(app.getString(R.string.confirm_lock_message), 14, false));
        LinearLayout row = new LinearLayout(app);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END);
        row.setPadding(0, dp(16), 0, 0);
        Button cancel = btn(app.getString(R.string.cancel), false);
        cancel.setOnClickListener(v -> {
            if (fab != null) fab.cancelHold();
            hideModalLayer();
        });
        Button ok = btn(app.getString(R.string.lock), true);
        ok.setOnClickListener(v -> {
            Prefs.setLocked(app, true);
            hideModalLayer();
            refreshState();
            toast(app.getString(R.string.toast_locked));
        });
        row.addView(cancel);
        row.addView(ok);
        card.addView(row);
        modalRoot.addView(wrapCard(card));
    }

    private void showPinUnlock() {
        if (modalRoot == null) return;
        showModalLayer();
        updateModalLayout(true);
        modalRoot.removeAllViews();
        modalRoot.setBackgroundColor(0x99000000);
        LinearLayout card = card();
        card.addView(label(app.getString(R.string.enter_pin), 20, true));
        EditText pin = new EditText(app);
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pin.setImeOptions(EditorInfo.IME_ACTION_DONE);
        pin.setTextColor(0xFFECE6D8);
        pin.setHintTextColor(0xFF8A8478);
        pin.setHint(app.getString(R.string.pin_hint_digits));
        pin.setBackgroundColor(0xFF151513);
        pin.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.addView(pin, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView err = label("", 13, false);
        err.setTextColor(0xFFE57373);
        card.addView(err);
        Runnable submit = () -> {
            String p = pin.getText().toString().trim();
            if (PinStore.verify(app, p)) {
                hideIme(pin);
                Prefs.setLocked(app, false);
                hideModalLayer();
                refreshState();
                toast(app.getString(R.string.toast_unlocked));
            } else {
                err.setText(app.getString(R.string.pin_wrong));
                pin.selectAll();
            }
        };
        pin.setOnEditorActionListener((OnEditorActionListener) (v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_ACTION_SEND) {
                submit.run();
                return true;
            }
            return false;
        });
        LinearLayout row = new LinearLayout(app);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.END);
        row.setPadding(0, dp(12), 0, 0);
        Button cancel = btn(app.getString(R.string.cancel), false);
        cancel.setOnClickListener(v -> {
            hideIme(pin);
            if (fab != null) fab.cancelHold();
            hideModalLayer();
        });
        Button ok = btn(app.getString(R.string.unlock), true);
        ok.setOnClickListener(v -> submit.run());
        row.addView(cancel);
        row.addView(ok);
        card.addView(row);
        modalRoot.addView(wrapCard(card));
        pin.post(() -> {
            pin.requestFocus();
            InputMethodManager imm = (InputMethodManager) app.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showSoftInput(pin, InputMethodManager.SHOW_IMPLICIT);
        });
    }

    private void hideIme(View v) {
        InputMethodManager imm = (InputMethodManager) app.getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private FrameLayout wrapCard(LinearLayout card) {
        FrameLayout wrap = new FrameLayout(app);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER;
        lp.leftMargin = dp(24);
        lp.rightMargin = dp(24);
        wrap.addView(card, lp);
        return wrap;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(app);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(20), dp(20), dp(20));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0xFF151513);
        bg.setCornerRadius(dp(12));
        card.setBackground(bg);
        return card;
    }

    private TextView label(String t, float sp, boolean bold) {
        TextView tv = new TextView(app);
        tv.setText(t);
        tv.setTextColor(0xFFECE6D8);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) tv.setTypeface(Typeface.DEFAULT_BOLD);
        else tv.setTextColor(0xFF8A8478);
        return tv;
    }

    private Button btn(String t, boolean primary) {
        Button b = new Button(app);
        b.setText(t);
        b.setAllCaps(false);
        b.setTextColor(primary ? 0xFF0B0B0A : 0xFFECE6D8);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(8));
        bg.setColor(primary ? 0xFFFFB000 : 0xFF2B2A26);
        b.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.leftMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
    }

    private int dp(int v) {
        return (int) (v * app.getResources().getDisplayMetrics().density + 0.5f);
    }

    private void toast(String message) {
        Toast.makeText(app, message, Toast.LENGTH_SHORT).show();
    }
}
