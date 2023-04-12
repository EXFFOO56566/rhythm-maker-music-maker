package com.batsoft.boomjunkie;

import java.io.Serializable;

/**
 * Created by Enes Battal on 7/8/2015.
 */

public class SaveData implements Serializable {
    public boolean[][] sequencerTicks;
    public int[] btnDrumsIndex;

    public int[] btnDrumsDatabaseField;
    public boolean[] btnDrumsIsActive;
    public int bpm;
    public int sequencerTickCount;
    public int drumCount;
    public SaveData(boolean[][] _sequencerTicks, int[] _btnDrumsIndex,int[] _btnDrumsDatabaseField, boolean[] _btnDrumsIsActive, int _bpm, int _sequencerTickCount,int _drumCount){
        this.bpm = _bpm;
        this.sequencerTickCount=_sequencerTickCount;
        this.sequencerTicks = _sequencerTicks;
        this.btnDrumsIndex = _btnDrumsIndex;
        this.btnDrumsDatabaseField =_btnDrumsDatabaseField;
        this.btnDrumsIsActive = _btnDrumsIsActive;
        this.drumCount=_drumCount;
    }
}
