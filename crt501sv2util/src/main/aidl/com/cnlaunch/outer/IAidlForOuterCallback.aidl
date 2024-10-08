// IAidlForOuterCallback.aidl
package com.cnlaunch.outer;

// Declare any non-default types here with import statements

interface IAidlForOuterCallback {
    String getTag();

    void onOBDLearnData(int tireCount,int tireIndex);

    void onOBDLearnResult(boolean isSuccess);

}