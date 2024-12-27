package v4lpt.vpt.f023.MYC;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import java.util.HashSet;
import java.util.Set;

public class SwipeController extends ItemTouchHelper.Callback {
    private static final float BUTTON_WIDTH = 300f;
    private boolean swipeBack = false;
    private Set<Integer> revealedPositions = new HashSet<>();
    private ButtonsState buttonShowedState = ButtonsState.GONE;
    private final OnSwipeListener swipeListener;
    private RecyclerView.ViewHolder currentItemViewHolder = null;

    enum ButtonsState {
        GONE,
        REVEALED,
        FULLY_SWIPED
    }

    public interface OnSwipeListener {
        void onDelete(int position);
    }

    public SwipeController(OnSwipeListener listener) {
        this.swipeListener = listener;
    }

    @Override
    public int getMovementFlags(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
        return makeMovementFlags(0, ItemTouchHelper.LEFT);
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        int position = viewHolder.getAdapterPosition();
        if (buttonShowedState == ButtonsState.FULLY_SWIPED) {
            swipeListener.onDelete(position);
        }
    }

    @Override
    public int convertToAbsoluteDirection(int flags, int layoutDirection) {
        if (swipeBack) {
            swipeBack = false;
            return 0;
        }
        return super.convertToAbsoluteDirection(flags, layoutDirection);
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            setTouchListener(recyclerView, viewHolder, dX);
        }
        currentItemViewHolder = viewHolder;

        if (buttonShowedState == ButtonsState.GONE) {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        } else {
            // Stay at reveal position
            float newX;
            if (buttonShowedState == ButtonsState.REVEALED) {
                newX = Math.max(dX, -BUTTON_WIDTH);
            } else {
                newX = dX;
            }
            super.onChildDraw(c, recyclerView, viewHolder, newX, dY, actionState, isCurrentlyActive);
        }

        drawButtons(c, viewHolder.itemView, buttonShowedState, dX);
    }

    private void drawButtons(Canvas c, View itemView, ButtonsState state, float dX) {
        float buttonWidthWithoutPadding = BUTTON_WIDTH;
        float corners = 0;

        View buttonInstance = null;
        if (currentItemViewHolder == null) {
            return;
        }

        // Draw red delete background
        Paint p = new Paint();
        p.setColor(Color.RED);
        RectF background = new RectF(
                itemView.getRight() + dX,
                itemView.getTop(),
                itemView.getRight(),
                itemView.getBottom()
        );
        c.drawRect(background, p);

        // Draw delete icon
        Drawable deleteIcon = ContextCompat.getDrawable(itemView.getContext(), R.drawable.ic_delete);
        if (deleteIcon != null) {
            int iconMargin = (itemView.getHeight() - deleteIcon.getIntrinsicHeight()) / 2;
            int iconTop = itemView.getTop() + iconMargin;
            int iconBottom = iconTop + deleteIcon.getIntrinsicHeight();
            int iconLeft = itemView.getRight() - iconMargin - deleteIcon.getIntrinsicWidth();
            int iconRight = itemView.getRight() - iconMargin;

            deleteIcon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
            deleteIcon.draw(c);
        }
    }

    private void setTouchListener(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX) {
        recyclerView.setOnTouchListener((v, event) -> {
            swipeBack = event.getAction() == MotionEvent.ACTION_CANCEL || event.getAction() == MotionEvent.ACTION_UP;
            if (swipeBack) {
                if (Math.abs(dX) >= BUTTON_WIDTH) {
                    if (Math.abs(dX) > recyclerView.getWidth() * 0.5f) {
                        buttonShowedState = ButtonsState.FULLY_SWIPED;
                    } else {
                        buttonShowedState = ButtonsState.REVEALED;
                        revealedPositions.add(viewHolder.getAdapterPosition());
                    }
                }
            }
            return false;
        });
    }

    public void onClickDelete(float x, float y, RecyclerView recyclerView) {
        if (currentItemViewHolder != null && buttonShowedState == ButtonsState.REVEALED) {
            View itemView = currentItemViewHolder.itemView;
            float deleteButtonStart = itemView.getRight() - BUTTON_WIDTH;

            if (x >= deleteButtonStart && x <= itemView.getRight() &&
                    y >= itemView.getTop() && y <= itemView.getBottom()) {
                int position = currentItemViewHolder.getAdapterPosition();
                swipeListener.onDelete(position);
            }
        }
    }
}
