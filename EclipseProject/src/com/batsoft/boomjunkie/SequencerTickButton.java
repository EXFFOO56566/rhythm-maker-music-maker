package com.batsoft.boomjunkie;

import android.content.Context;
import android.widget.ToggleButton;

/**
 * Created by Enes Battal on 7/8/2015.
 */

public class SequencerTickButton extends ToggleButton {
	private int indexX, indexY;

	public SequencerTickButton(Context context) {
		super(context);
		this.setBackgroundResource(R.drawable.toggle_btn_blue_orange);
	}

	public SequencerTickButton(Context context, int iX, int iY) {
		super(context);
		this.indexX = iX;
		this.indexY = iY;
	}

	public void setBlueOrangeBackground() {
		this.setBackgroundResource(R.drawable.toggle_btn_blue_orange);
	}

	public void setGreenPurpleBackground() {
		this.setBackgroundResource(R.drawable.toggle_btn_green_purple);
	}
}
