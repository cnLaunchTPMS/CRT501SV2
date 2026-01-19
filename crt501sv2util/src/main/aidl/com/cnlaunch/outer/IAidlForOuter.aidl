package com.cnlaunch.outer;

import com.cnlaunch.outer.IAidlForOuterCallback;


interface IAidlForOuter {

    void registerCallback(in IAidlForOuterCallback callback);

    void unregisterCallback(in IAidlForOuterCallback callback);


    void sendOBDLearnData(int tireCount,int tireIndex);

    void sendOBDLearnResult(boolean isSuccess);

}

