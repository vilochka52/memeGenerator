package com.example.memegenerator.ui;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;

import androidx.appcompat.widget.AppCompatTextView;

public class DraggableTextView extends AppCompatTextView {

    private float dX, dY;
    private final Rect bounds = new Rect();

    public DraggableTextView(Context c) { super(c); }
    public DraggableTextView(Context c, AttributeSet a) { super(c, a); }
    public DraggableTextView(Context c, AttributeSet a, int s) { super(c, a, s); }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent == null) return super.onTouchEvent(e);

        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dX = getX() - e.getRawX();
                dY = getY() - e.getRawY();
                return true;
            case MotionEvent.ACTION_MOVE:
                float newX = e.getRawX() + dX;
                float newY = e.getRawY() + dY;
                parent.getDrawingRect(bounds);
                newX = Math.max(bounds.left, Math.min(newX, bounds.right - getWidth()));
                newY = Math.max(bounds.top, Math.min(newY, bounds.bottom - getHeight()));
                setX(newX); setY(newY);
                return true;
        }
        return super.onTouchEvent(e);
    }
}
