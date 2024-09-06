package com.cnlaunch.crt501sv2util;

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

    /**
     * 强制杀死进程
     *
     * @param processName 进程名
     */
    protected static void killProcess(String processName) {
        command(new String[]{COMMAND_KILL_PREFIX + processName});
    }


    /**
     * 获取Obd电量
     *
     * @return
     */
    protected static String getObdVoltage() {
        return command(new String[]{COMMAND_READ_OBD_VOLTAGE});
    }

    protected static void gotoFactory(){
        ComUtils.command(new String[]{"am start -n com.cnlaunch.crpguard/com.cnlaunch.crpguard.factory.activity.factory.FATNewFactoryActivity"});
    }

    private static String command(String[] commands) {
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
