package com.cnlaunch.outer;

interface IAidlForOuterCallback {
    String getTag();

    void onOBDLearnData(int tireCount,int tireIndex);

    void onOBDLearnResult(boolean isSuccess);

}