package com.example.memegenerator.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import androidx.appcompat.widget.AppCompatTextView;

public class DraggableTextView extends AppCompatTextView {
    private float dX, dY;
    private boolean dragging = false;

    public DraggableTextView(Context context) {
        super(context);
    }

    public DraggableTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DraggableTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                dragging = true;
                dX = getX() - event.getRawX();
                dY = getY() - event.getRawY();
                bringToFront();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    setX(event.getRawX() + dX);
                    setY(event.getRawY() + dY);
                }
                return true;
            case MotionEvent.ACTION_UP:
                dragging = false;
                return true;
        }
        return super.onTouchEvent(event);
    }
}
