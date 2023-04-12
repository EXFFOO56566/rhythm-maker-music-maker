package com.batsoft.boomjunkie;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.widget.Button;

/**
 * Created by Enes Battal on 7/8/2015.
 */
public class DrumButton extends Button {
    private int index;

    public int getDatabaseField() {
        return databaseField;
    }

    public void setDatabaseField(int databaseField) {
        this.databaseField = databaseField;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setIsActive(boolean isActive) {
        this.isActive = isActive;
    }

    private int databaseField=-1;
    private boolean isActive=false;
    public DrumButton(Context context) {
        super(context);
    }

    public DrumButton(Context context, int i) {
        super(context);
        this.index = i;
        this.setBackgroundResource(R.drawable.btn_blue);
        this.setTextColor(Color.parseColor("#FFFFFF"));
        this.setTypeface(null, Typeface.BOLD);

    }
    public void setIndex(int i) {
        this.index = i;
    }
    public int getIndex(){
        return this.index;
    }
}
