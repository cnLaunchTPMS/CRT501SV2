package com.cnlaunch.crt501sv2util;

import android.text.TextUtils;

import java.io.Serializable;

public class DiagTpmsBeanForOuter implements Serializable {

    public enum WheelNum {
        FOUR,FIVE,SIX
    }


    //诊断协议
    String diagEntry;

    //轮胎数
    WheelNum wheelNum;

    //id 比特位长度
    int idLen;


    //左前 id
    String fl;

    //右前 id
    String fr;

    //右后 id
    String rr;

    //左后 id
    String rl;

    //五轮车 备胎id
    String sp;


    //六轮车 右后外胎 id
    String rrOuter;

    //六轮车 右后内胎 id
    String rrInner;

    //六轮车 左后外胎 id
    String rlOuter;

    //六轮车 左后内胎 id
    String rlInner;



    public String getDiagEntry() throws Exception {
        checkIsNullOrEmpty(diagEntry);
        return diagEntry;
    }

    public void setDiagEntry(String diagEntry) {
        this.diagEntry = diagEntry;
    }


    public WheelNum getWheelNum() {
        return wheelNum;
    }

    public void setWheelNum(WheelNum wheelNum) {
        this.wheelNum = wheelNum;
    }

    public int getIdLen() throws Exception {
        checkIsNullOrEmpty(String.valueOf(idLen));
        return idLen;
    }

    public void setIdLen(int idLen) {
        this.idLen = idLen;
    }

    public String getFl() throws Exception {
        checkIsNullOrEmpty(fl);
        return fl;
    }

    public void setFl(String fl) {
        this.fl = fl;
    }

    public String getFr() throws Exception {
        checkIsNullOrEmpty(fr);
        return fr;
    }

    public void setFr(String fr) {
        this.fr = fr;
    }

    public String getRr() throws Exception {
        checkIsNullOrEmpty(rr);
        return rr;
    }

    public void setRr(String rr) {
        this.rr = rr;
    }

    public String getRl() throws Exception {
        checkIsNullOrEmpty(rl);
        return rl;
    }

    public void setRl(String rl) {
        this.rl = rl;
    }

    public String getSp() throws Exception {
        checkIsNullOrEmpty(sp);
        return sp;
    }

    public void setSp(String sp) {
        this.sp = sp;
    }

    public String getRrOuter() throws Exception {
        checkIsNullOrEmpty(rrOuter);
        return rrOuter;
    }

    public void setRrOuter(String rrOuter) {
        this.rrOuter = rrOuter;
    }

    public String getRrInner() throws Exception {
        checkIsNullOrEmpty(rrInner);
        return rrInner;
    }

    public void setRrInner(String rrInner) {
        this.rrInner = rrInner;
    }

    public String getRlOuter() throws Exception {
        checkIsNullOrEmpty(rlOuter);
        return rlOuter;
    }

    public void setRlOuter(String rlOuter) {
        this.rlOuter = rlOuter;
    }

    public String getRlInner() throws Exception {
        checkIsNullOrEmpty(rlInner);
        return rlInner;
    }

    public void setRlInner(String rlInner) {
        this.rlInner = rlInner;
    }

    private void checkIsNullOrEmpty(String data) throws Exception {
        if (TextUtils.isEmpty(data)){
            throw new Exception("数据不能为空");
        }
    }




    public String getTransmitString() throws Exception {

        StringBuilder transBuilder = new StringBuilder("DiagEntry=")
                .append(getDiagEntry())
                .append("\n");

        switch (wheelNum){

            case FOUR:
                transBuilder.append("WheelNum=").append(4).append("\n");
                transBuilder.append("IDlen=").append(getIdLen()).append("\n");
                transBuilder.append("FL=").append(getFl()).append("\n");
                transBuilder.append("FR=").append(getFr()).append("\n");
                transBuilder.append("RR=").append(getRr()).append("\n");
                transBuilder.append("RL=").append(getRl());
                break;

            case FIVE:
                transBuilder.append("WheelNum=").append(5).append("\n");
                transBuilder.append("IDlen=").append(getIdLen()).append("\n");
                transBuilder.append("FL=").append(getFl()).append("\n");
                transBuilder.append("FR=").append(getFr()).append("\n");
                transBuilder.append("RR=").append(getRr()).append("\n");
                transBuilder.append("RL=").append(getRl()).append("\n");
                transBuilder.append("SP=").append(getRl());
                break;



            case SIX:
                transBuilder.append("WheelNum=").append(6).append("\n");
                transBuilder.append("IDlen=").append(getIdLen()).append("\n");
                transBuilder.append("FL=").append(getFl()).append("\n");
                transBuilder.append("FR=").append(getFr()).append("\n");
                transBuilder.append("RR_OUTER").append(getRrOuter()).append("\n");
                transBuilder.append("RR_INNER").append(getRrInner()).append("\n");
                transBuilder.append("RL_OUTER").append(getRlOuter()).append("\n");
                transBuilder.append("RL_INNER").append(getRlInner());
                break;

            default:checkIsNullOrEmpty("");
        }




        return transBuilder.toString();
    }


    @Override
    public String toString() {
        return "DiagTpmsBeanForOuter{" +
                "diagEntry='" + diagEntry + '\'' +
                ", wheelNum=" + wheelNum +
                ", idLen=" + idLen +
                ", fl='" + fl + '\'' +
                ", fr='" + fr + '\'' +
                ", rr='" + rr + '\'' +
                ", rl='" + rl + '\'' +
                ", sp='" + sp + '\'' +
                ", rrOuter='" + rrOuter + '\'' +
                ", rrInner='" + rrInner + '\'' +
                ", rlOuter='" + rlOuter + '\'' +
                ", rlInner='" + rlInner + '\'' +
                '}';
    }
}
