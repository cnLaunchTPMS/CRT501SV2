// IAidlForOuter.aidl
package com.cnlaunch.outer;

// Declare any non-default types here with import statements

import com.cnlaunch.outer.IAidlForOuterCallback;


interface IAidlForOuter {

    void registerCallback(in IAidlForOuterCallback callback);

    void unregisterCallback(in IAidlForOuterCallback callback);


    void sendOBDLearnData(int tireCount,int tireIndex);

    void sendOBDLearnResult(boolean isSuccess);

}

