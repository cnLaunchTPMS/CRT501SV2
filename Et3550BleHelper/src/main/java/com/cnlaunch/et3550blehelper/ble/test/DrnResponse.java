package com.cnlaunch.et3550blehelper.ble.test;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class DrnResponse implements Serializable {

    private int DRN;
    List<Map<String, String>> LSMK;

    public int getDRN() {
        return DRN;
    }

    public void setDRN(int DRN) {
        this.DRN = DRN;
    }

    public List<Map<String, String>> getLSMK() {
        return LSMK;
    }

    public void setLSMK(List<Map<String, String>> LSMK) {
        this.LSMK = LSMK;
    }

    @Override
    public String toString() {
        return "DrnResponse{" +
                "DRN=" + DRN +
                ", LSMK=" + LSMK +
                '}';
    }
}
