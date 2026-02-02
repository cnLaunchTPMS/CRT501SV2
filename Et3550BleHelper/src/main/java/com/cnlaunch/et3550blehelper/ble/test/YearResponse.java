package com.cnlaunch.et3550blehelper.ble.test;

import java.util.List;
import java.util.Map;

public class YearResponse {

    public int DRN;
    public int MMK;
    public int MMD;
    public List<Map<String, String>> LSYR;

    @Override
    public String toString() {
        return "YearResponse{" +
                "DRN=" + DRN +
                ", MMK=" + MMK +
                ", MMD=" + MMD +
                ", LSYR=" + LSYR +
                '}';
    }
}
