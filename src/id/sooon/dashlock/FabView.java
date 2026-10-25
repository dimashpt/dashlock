package id.sooon.dashlock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

/**
 * Tombol bulat draggable: kunci (tahan) / buka (tahan atau triple tap); geser membatalkan.
 */
final class FabView extends View {
    interface Listener {
        void onDragEnd(int centerX, int centerY, int side);

        void onRequestLock();

        void onUnlockHoldComplete();

        void onUnlockHoldCancelled();
    }

    static final int SIZE_DP = 84;
    private static final int HOLD_ARM_MS = 200;
    private static final int TAP_TIMEOUT_MS = 450;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint icon = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arc = new RectF();
    private final RectF tmp = new RectF();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int touchSlop;
    private int sizePx;

    Listener listener;
    boolean locked;

    private float downRawX, downRawY;
    private boolean dragging;
    private boolean holdArmed;
    private boolean unlockHolding;
    private long unlockHoldStart;
    private float holdProgress;
    private int unlockHoldMs;

    private int tapCount;
    private long lastTapAt;

    private final Runnable armRunnable = () -> {
        if (dragging) return;
        holdArmed = true;
        if (locked) {
            if (Prefs.unlockMode(getContext()) == Prefs.UNLOCK_HOLD) {
                startUnlockHold();
            }
        } else if (listener != null) {
            listener.onRequestLock();
        }
    };

    FabView(Context c) {
        super(c);
        touchSlop = ViewConfiguration.get(c).getScaledTouchSlop();
        sizePx = sizePx(c);
        fill.setColor(0x80FFB000);
        ring.setColor(0xCC0B0B0A);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(sizePx * 0.06f);
        icon.setColor(0xFF0B0B0A);
        icon.setStyle(Paint.Style.STROKE);
        icon.setStrokeWidth(sizePx * 0.065f);
        icon.setStrokeCap(Paint.Cap.ROUND);
        icon.setStrokeJoin(Paint.Join.ROUND);
        unlockHoldMs = unlockHoldMs(c);
    }

    static int sizePx(Context c) {
        return (int) (SIZE_DP * c.getResources().getDisplayMetrics().density + 0.5f);
    }

    private static int unlockHoldMs(Context c) {
        return Prefs.unlockHoldSeconds(c) * 1000;
    }

    void reloadPrefs() {
        unlockHoldMs = unlockHoldMs(getContext());
        int newSize = sizePx(getContext());
        if (newSize != sizePx) {
            sizePx = newSize;
            ring.setStrokeWidth(sizePx * 0.06f);
            icon.setStrokeWidth(sizePx * 0.065f);
            requestLayout();
        }
        invalidate();
    }

    int diameterPx() { return sizePx; }

    void setLocked(boolean v) {
        locked = v;
        tapCount = 0;
        cancelHold();
        invalidate();
    }

    void cancelHold() {
        handler.removeCallbacks(armRunnable);
        holdArmed = false;
        if (unlockHolding) {
            unlockHolding = false;
            holdProgress = 0f;
            if (listener != null) listener.onUnlockHoldCancelled();
        }
        invalidate();
    }

    private void startUnlockHold() {
        unlockHolding = true;
        unlockHoldStart = SystemClock.uptimeMillis();
        handler.post(unlockTick);
    }

    private final Runnable unlockTick = new Runnable() {
        @Override
        public void run() {
            if (!unlockHolding) return;
            long elapsed = SystemClock.uptimeMillis() - unlockHoldStart;
            holdProgress = Math.min(1f, elapsed / (float) unlockHoldMs);
            invalidate();
            if (elapsed >= unlockHoldMs) {
                unlockHolding = false;
                holdProgress = 0f;
                if (listener != null) listener.onUnlockHoldComplete();
            } else {
                handler.postDelayed(this, 16);
            }
        }
    };

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(sizePx, sizePx);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float pad = ring.getStrokeWidth();
        float r = sizePx / 2f - pad;
        canvas.drawCircle(cx, cy, r, fill);
        if (unlockHolding && holdProgress > 0f) {
            arc.set(cx - r, cy - r, cx + r, cy + r);
            canvas.drawArc(arc, -90, 360f * holdProgress, false, ring);
        }
        drawPadlock(canvas, cx, cy, r * 0.46f, locked);
    }

    /** Gembok klasik: terkunci = shackle tertutup; terbuka = shackle terangkat. */
    private void drawPadlock(Canvas c, float cx, float cy, float scale, boolean isLocked) {
        float bodyW = scale * 1.05f;
        float bodyH = scale * 0.85f;
        float bodyLeft = cx - bodyW / 2f;
        float bodyTop = cy + scale * 0.05f;
        tmp.set(bodyLeft, bodyTop, bodyLeft + bodyW, bodyTop + bodyH);
        c.drawRoundRect(tmp, scale * 0.12f, scale * 0.12f, icon);

        float shW = bodyW * 0.78f;
        float shH = scale * 0.95f;
        float shLeft = cx - shW / 2f;
        float shTop = bodyTop - shH * 0.72f;
        arc.set(shLeft, shTop, shLeft + shW, shTop + shH);
        if (isLocked) {
            c.drawArc(arc, 200, 140, false, icon);
        } else {
            c.drawArc(arc, 200, 110, false, icon);
            c.drawLine(shLeft + shW * 0.92f, shTop + shH * 0.38f,
                    bodyLeft + bodyW * 0.15f, bodyTop + scale * 0.08f, icon);
        }
        if (isLocked) {
            c.drawLine(cx, bodyTop + bodyH * 0.42f, cx, bodyTop + bodyH * 0.72f, icon);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                unlockHoldMs = unlockHoldMs(getContext());
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                dragging = false;
                holdArmed = false;
                handler.removeCallbacks(armRunnable);
                handler.postDelayed(armRunnable, HOLD_ARM_MS);
                return true;
            case MotionEvent.ACTION_MOVE:
                float dx = event.getRawX() - downRawX;
                float dy = event.getRawY() - downRawY;
                if (!dragging && (Math.hypot(dx, dy) > touchSlop)) {
                    dragging = true;
                    tapCount = 0;
                    cancelHold();
                }
                if (dragging && getLayoutParams() instanceof WindowManager.LayoutParams) {
                    WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
                    lp.x += (int) (event.getRawX() - downRawX);
                    lp.y += (int) (event.getRawY() - downRawY);
                    downRawX = event.getRawX();
                    downRawY = event.getRawY();
                    WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
                    if (wm != null) wm.updateViewLayout(this, lp);
                }
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                handler.removeCallbacks(armRunnable);
                if (dragging && listener != null && getLayoutParams() instanceof WindowManager.LayoutParams) {
                    WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
                    int centerX = lp.x + getWidth() / 2;
                    int centerY = lp.y + getHeight() / 2;
                    android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
                    int side = centerX < dm.widthPixels / 2 ? Prefs.SIDE_LEFT : Prefs.SIDE_RIGHT;
                    listener.onDragEnd(centerX, centerY, side);
                } else if (!dragging) {
                    if (locked && Prefs.unlockMode(getContext()) == Prefs.UNLOCK_TRIPLE_TAP) {
                        long now = SystemClock.uptimeMillis();
                        if (now - lastTapAt > TAP_TIMEOUT_MS) tapCount = 0;
                        lastTapAt = now;
                        tapCount++;
                        if (tapCount >= 3) {
                            tapCount = 0;
                            cancelHold();
                            if (listener != null) listener.onUnlockHoldComplete();
                        }
                    } else if (!holdArmed) {
                        cancelHold();
                    }
                }
                dragging = false;
                return true;
            default:
                return super.onTouchEvent(event);
        }
    }
}
