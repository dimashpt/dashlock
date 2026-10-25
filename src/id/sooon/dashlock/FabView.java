package id.sooon.dashlock;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
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

    static final int SIZE_DP = 56;
    private static final float ALPHA_IDLE = 0.20f;
    private static final float ALPHA_ACTIVE = 0.80f;
    private static final long FADE_IDLE_DELAY_MS = 2000;
    private static final int HOLD_ARM_MS = 200;
    private static final int TAP_TIMEOUT_MS = 450;

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Drawable iconLocked;
    private final Drawable iconUnlocked;
    private final RectF arc = new RectF();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final int touchSlop;
    private int sizePx;

    Listener listener;
    boolean locked;

    private float visualAlpha = ALPHA_IDLE;
    private float downRawX, downRawY;
    private boolean dragging;
    private boolean holdArmed;
    private boolean unlockHolding;
    private long unlockHoldStart;
    private float holdProgress;
    private int unlockHoldMs;

    private int tapCount;
    private long lastTapAt;

    private final Runnable fadeIdleRunnable = () -> {
        visualAlpha = ALPHA_IDLE;
        invalidate();
    };

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
        fill.setColor(0xFF000000);
        border.setColor(0xFF9A9A9A);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(sizePx * 0.045f);
        ring.setColor(0xFFFFFFFF);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(sizePx * 0.07f);
        iconLocked = c.getResources().getDrawable(R.drawable.ic_fab_lock, c.getTheme());
        iconUnlocked = c.getResources().getDrawable(R.drawable.ic_fab_lock_open, c.getTheme());
        if (iconLocked != null) iconLocked.mutate();
        if (iconUnlocked != null) iconUnlocked.mutate();
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
            border.setStrokeWidth(sizePx * 0.045f);
            ring.setStrokeWidth(sizePx * 0.07f);
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

    private void setActiveVisual() {
        handler.removeCallbacks(fadeIdleRunnable);
        visualAlpha = ALPHA_ACTIVE;
        invalidate();
    }

    private void scheduleFadeIdle() {
        handler.removeCallbacks(fadeIdleRunnable);
        handler.postDelayed(fadeIdleRunnable, FADE_IDLE_DELAY_MS);
    }

    private int alphaByte() {
        return Math.max(0, Math.min(255, (int) (visualAlpha * 255f + 0.5f)));
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
        int a = alphaByte();
        fill.setAlpha(a);
        border.setAlpha(a);
        ring.setAlpha(a);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float borderW = border.getStrokeWidth();
        float fillR = sizePx / 2f - borderW;
        float borderR = sizePx / 2f - borderW / 2f;
        canvas.drawCircle(cx, cy, fillR, fill);
        canvas.drawCircle(cx, cy, borderR, border);
        if (unlockHolding && holdProgress > 0f) {
            arc.set(cx - fillR, cy - fillR, cx + fillR, cy + fillR);
            canvas.drawArc(arc, -90, 360f * holdProgress, false, ring);
        }
        drawFabIcon(canvas, cx, cy, fillR, a);
    }

    private void drawFabIcon(Canvas canvas, float cx, float cy, float r, int alpha) {
        Drawable d = locked ? iconLocked : iconUnlocked;
        if (d == null) return;
        float half = r * 0.34f;
        int left = (int) (cx - half);
        int top = (int) (cy - half);
        int right = (int) (cx + half);
        int bottom = (int) (cy + half);
        d.setBounds(left, top, right, bottom);
        d.setAlpha(alpha);
        d.draw(canvas);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                setActiveVisual();
                unlockHoldMs = unlockHoldMs(getContext());
                downRawX = event.getRawX();
                downRawY = event.getRawY();
                dragging = false;
                holdArmed = false;
                handler.removeCallbacks(armRunnable);
                handler.postDelayed(armRunnable, HOLD_ARM_MS);
                return true;
            case MotionEvent.ACTION_MOVE:
                setActiveVisual();
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
                scheduleFadeIdle();
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
