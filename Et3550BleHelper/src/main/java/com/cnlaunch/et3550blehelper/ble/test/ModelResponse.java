package com.cnlaunch.et3550blehelper.ble.test;

import java.util.List;
import java.util.Map;

public class ModelResponse {

    public int DRN;
    public int MMK;
    public List<Map<String, String>> LSMD;

    @Override
    public String toString() {
        return "ModelResponse{" +
                "DRN=" + DRN +
                ", MMK=" + MMK +
                ", LSMD=" + LSMD +
                '}';
    }
}
