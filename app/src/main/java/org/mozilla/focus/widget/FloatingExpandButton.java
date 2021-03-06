/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.widget;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import android.util.AttributeSet;
import android.view.View;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AnimationUtils;


import java.util.ArrayList;
import java.util.List;
import org.mozilla.focus.R;

public class FloatingExpandButton extends FloatingActionButton {
    private boolean keepHidden;
    private List<SubActionButton> subButtons = new ArrayList<>();

    public List<SubActionButton> getSubButton() {
        return subButtons;
    }


    public void addSubButton(SubActionButton subButton) {
        this.subButtons.add(subButton);
    }

    public FloatingExpandButton(Context context) {
        super(context);
    }

    public FloatingExpandButton(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FloatingExpandButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    // range of movement
    private int rangeHeight;
    private int rangeWidth;
    // the start position of button
    private int startX;
    private int startY;
    // state of dragging
    private boolean isDrag;
    private int rawX;
    private int rawY;
    final static private int EDGEDIS = 50;
    final static private int DURATION = 500;
    private FloatingActionMenu actionMenu;
    final static private int ZERO_DEG = 0;
    final static private int NIGHT_DEG = 90;
    final static private int MINUS_NIGHT_DEG = -90;
    final static private int ONE_HUNDRED_EIGHTY_DEG = 180;
    final static private int MINUS_ONE_HUNDRED_EIGHTY_DEG = -180;

    public void addActionMenu(FloatingActionMenu menu) {
        this.actionMenu = menu;
    }

    public FloatingActionMenu getActionMenu(){return this.actionMenu;}

    public int getStartX(){
        return startX;
    }
    public int getStartY(){
        return startY;
    }

    public int getEdgedis(){
        return EDGEDIS;
    }
    public void updateSessionsCount(int tabCount) {
        final CoordinatorLayout.LayoutParams params = (CoordinatorLayout.LayoutParams) getLayoutParams();
        final FloatingActionButtonBehavior behavior = (FloatingActionButtonBehavior) params.getBehavior();
        AccessibilityManager accessibilityManager = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);

        keepHidden = tabCount != 1;

        if (behavior != null) {
            if (accessibilityManager != null && accessibilityManager.isTouchExplorationEnabled()) {
                // Always display erase button if Talk Back is enabled
                behavior.setEnabled(false);
            } else {
                behavior.setEnabled(!keepHidden);
            }
        }

        if (keepHidden) {
            setVisibility(View.GONE);
        }
    }

    @Override
    protected void onFinishInflate() {
        if (!keepHidden) {
            this.setVisibility(View.VISIBLE);
            this.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.fab_reveal));
        }

        super.onFinishInflate();
    }

    @Override
    public void setVisibility(int visibility) {
        if (keepHidden && visibility == View.VISIBLE) {
            // There are multiple callbacks updating the visibility of the button. Let's make sure
            // we do not show the button if we do not want to.
            return;
        }

        if (visibility == View.VISIBLE) {
            if (subButtons != null && subButtons.size() != 0)
                this.setSubVisibility(subButtons, visibility);
            show();
        } else {
            this.actionMenu.close(true);
            this.setSubVisibility(subButtons, visibility);
            hide();
        }
    }

    private void setSubVisibility(List<SubActionButton> subButtons, int visibility) {
        for (SubActionButton subButton : subButtons) {
            subButton.setVisibility(visibility);
        }
    }

    public void updateRanges(){
        // Gets the parent of this button which is the range of movement.
        ViewGroup parent;
        if (getParent() != null) {
            parent = (ViewGroup) getParent();
            // get the range of height and width
            rangeHeight = parent.getHeight();
            rangeWidth = parent.getWidth();
        }
    }

    public int getRangeHeight(){
        return rangeHeight;
    }

    public int getRangeWidth(){
        return rangeWidth;
    }
    // override onToucheVent so that button can listen the touch inputs (press/unpressed/drag)
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        // catch the touch position
        rawX = (int) event.getRawX();
        rawY = (int) event.getRawY();
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            // press the button
            case MotionEvent.ACTION_DOWN:
                setPressed(true);
                isDrag = false;
                getParent().requestDisallowInterceptTouchEvent(true);
                // save the start location of button
                startX = rawX;
                startY = rawY;
                updateRanges();
                break;
            // dragging the button
            case MotionEvent.ACTION_MOVE:
                if(!this.actionMenu.isOpen()){
                    if (rangeHeight <= 0 || rangeWidth == 0) {
                        isDrag = false;
                        break;
                    } else {
                        isDrag = true;
                    }

                    // calculate the distance of x and y from start location
                    int disX = rawX - startX;
                    int disY = rawY - startY;
                    int distance = (int) Math.sqrt(disX * disX + disY * disY);

                    // special case if the distance is 0 end dragging set the state to false
                    if (distance <= EDGEDIS) {
                        isDrag = false;
                        break;
                    }

                    // button size included
                    float x = getX() + disX;
                    float y = getY() + disY;
                    setDragLocation(x, y);


                    //break;
                    return false;
                }


                // unpressed button
            case MotionEvent.ACTION_UP:
                if (!isNotDrag()) {
                    // recovery from press
                    setPressed(false);
                    attractToClosestSide(rawX, rawY);
                }
                break;

            default:
                break;
        }
        // if drag then update session otherwise pass
        return !isNotDrag() || super.onTouchEvent(event);
    }
    public void setLocation(float x, float y){
        setX(x);
        setY(y);
    }

    public void setDragLocation(float x, float y){
        // test if reached the edge: left up right down
        if (x < 0) {
            x = 0;
        } else if (x > rangeWidth - getWidth()) {
            x = rangeWidth - getWidth();
        }
        if (y < 0) {
            y = 0;
        } else if (y > rangeHeight - EDGEDIS- getHeight()) {
            y = rangeHeight - getHeight() - EDGEDIS;
        }
        // Set the position of the button after dragging
        setLocation(x,y);
        // update the start position during dragging
        startX = rawX;
        startY = rawY;
    }

    public void attractToClosestSide(float x, float y){
        if (x >= rangeWidth / 2) {
            if (y <= rangeHeight / 2){
                this.actionMenu.setStartAngle(NIGHT_DEG);
                this.actionMenu.setEndAngle(ONE_HUNDRED_EIGHTY_DEG);
            } else {
                this.actionMenu.setStartAngle(MINUS_ONE_HUNDRED_EIGHTY_DEG);
                this.actionMenu.setEndAngle(MINUS_NIGHT_DEG);
            }
            // attract right
            animate().setInterpolator(new DecelerateInterpolator())
                    .setDuration(DURATION)
                    // keep 50 pixel away from the edge
                    .xBy(rangeWidth - getWidth() - getX() - EDGEDIS)
                    .start();
        } else {
            if (y <= rangeHeight / 2){
                this.actionMenu.setStartAngle(NIGHT_DEG);
                this.actionMenu.setEndAngle(ZERO_DEG);
            } else {
                this.actionMenu.setStartAngle(ZERO_DEG);
                this.actionMenu.setEndAngle(MINUS_NIGHT_DEG);
            }
            // attract left
            ObjectAnimator oa = ObjectAnimator.ofFloat(this, "x", getX(), EDGEDIS);
            oa.setInterpolator(new DecelerateInterpolator());
            oa.setDuration(DURATION);
            oa.start();
        }
    }

    @Override
    public boolean callOnClick() {
        return super.callOnClick();
    }

    // check is drag or not
    private boolean isNotDrag() {
        return !isDrag && (getX() == EDGEDIS || (getX() == rangeWidth - getWidth() - EDGEDIS));
    }
}