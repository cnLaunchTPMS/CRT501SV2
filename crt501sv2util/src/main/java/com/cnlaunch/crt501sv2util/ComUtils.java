package com.cnlaunch.crt501sv2util;

import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

class ComUtils {
    private static final String TAG = "ComUtils";
    private static final String PER_UP = "per-up";
    private static final String END = "\n";
    private static final String EXIT = "exit" + END;

    private static final String COMMAND_KILL_PREFIX = "am force-stop ";

    private static final String COMMAND_READ_OBD_VOLTAGE = "cat /sys/bus/iio/devices/iio:device0/in_voltage1_raw";

    private static int SOUND_OBD_INTERVAL = 50;
    private static int SOUND_OBD_CYCLE = 370370;
    private static int SOUND_OBD_RATIO = 185185;

    public static final String set_cycle = "/sys/devices/bsk_misc/beep_duration";
    public static final String set_duty_ratio = "/sys/devices/bsk_misc/beep_duty";


    protected static void command(String command) {
        sendCommand(new String[]{command});
    }

    /**
     * 强制杀死进程
     *
     * @param processName 进程名
     */
    protected static void killProcess(String processName) {
        sendCommand(new String[]{COMMAND_KILL_PREFIX + processName});
    }


    /**
     * 获取Obd电量
     *
     * @return
     */
    protected static String getObdVoltage() {
        return sendCommand(new String[]{COMMAND_READ_OBD_VOLTAGE});
    }

    protected static void gotoFactory(){
        ComUtils.sendCommand(new String[]{"am start -n com.cnlaunch.crpguard/com.cnlaunch.crpguard.factory.activity.factory.FATNewFactoryActivity"});
    }

    protected static void powerUSB(boolean isOpen){
        ComUtils.sendCommand(new String[]{"echo" + " " + (isOpen ? 1 : 0) + " > " + "sys/devices/bsk_misc/bsk_tpms_power"});
    }

    //查看tpms 系统节点上电情况
    protected static Boolean getTpmsPointState() {
        String flag = sendCommand(new String[]{"cat sys/devices/bsk_misc/bsk_tpms_power"}) ;
        if (!TextUtils.isEmpty(flag)) {
            return flag.trim().equals("1");
        } else {
            return false;
        }
    }

    protected static void playSound(int repeatCount){
        try {
            command("echo" + " " + SOUND_OBD_CYCLE  + " > " + set_cycle);
            command("echo" + " " + SOUND_OBD_RATIO + " > " + set_duty_ratio);
            for (int i = 0; i < repeatCount; i++) {
                command("echo" + " 1 > " + "/sys/devices/bsk_misc/beep_on_off");
                Thread.sleep(SOUND_OBD_INTERVAL);
                command("echo" + " 0 > " + "/sys/devices/bsk_misc/beep_on_off");
            }
        } catch (InterruptedException ie) {

        }
    }


    private static String sendCommand(String[] commands) {
        String line;

        String ret = null;
        Process process = null;
        DataOutputStream dos = null;
        InputStreamReader isr = null;
        BufferedReader reader = null;
        StringWriter sw = null;
        InputStream is = null;

        try {
            process = Runtime.getRuntime().exec(PER_UP);
            dos = new DataOutputStream(process.getOutputStream());
            for (String command : commands) {
                dos.writeBytes(command + END);
            }
            dos.writeBytes(END);
            dos.writeBytes(EXIT);
            dos.flush();
            process.waitFor();
            is = process.getInputStream();
            isr = new InputStreamReader(is);
            reader = new BufferedReader(isr);
            sw = new StringWriter();
            while ((line = reader.readLine()) != null) {
                sw.append(line).append(END);
            }
        } catch (Exception e) {
            Log.e(TAG, "异常：" + e.getMessage());
        } finally {
            try {
                if (reader != null) {
                    reader.close();
                }
                if (isr != null) {
                    isr.close();

                }
                if (dos != null) {
                    dos.close();
                }
                if (is != null) {
                    is.close();
                }

                if (sw != null) {
                    ret = sw.toString();
                    sw.close();

                } else {
                    ret = "";
                }
            } catch (Exception e) {
                Log.e(TAG, "异常：" + e.getMessage());
            }

        }
        return ret;
    }
}
