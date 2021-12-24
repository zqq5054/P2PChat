package com.shawn.fakewechat.utils;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.Window;
import android.view.WindowManager;

import com.readystatesoftware.systembartint.SystemBarTintManager;
import com.shawn.fakewechat.R;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

/**
 * Created by fengshawn on 2017/8/8.
 */

public class HelpUtils {

    public static int getScreenHeight(Context context) {
        return context.getResources().getDisplayMetrics().heightPixels;
    }

    public static int getScreenWidth(Context context) {
        return context.getResources().getDisplayMetrics().widthPixels;
    }

    public static int getStatusBarHeight(Context context) {
        Resources res = context.getResources();
        int resId = res.getIdentifier("status_bar_height", "dimen", "android");
        return res.getDimensionPixelSize(resId);

        //也可以通过反射
        /**
         Class<?> c = null;
         Object obj = null;
         Field field = null;
         int x = 0, statusBarHeight = 0;
         try {
         c = Class.forName("com.android.internal.R$dimen");
         obj = c.newInstance();
         field = c.getField("status_bar_height");
         x = Integer.parseInt(field.get(obj).toString());
         statusBarHeight = context.getResources().getDimensionPixelSize(x);
         } catch (Exception e1) {
         e1.printStackTrace();
         }
         return statusBarHeight;
         */
    }

    /**
     * 用户友好时间显示
     *
     * @param nowTime 现在时间毫秒
     * @param preTime 之前时间毫秒
     * @return 符合用户习惯的时间显示
     */
    public static String calculateShowTime(long nowTime, long preTime) {
        if (nowTime <= 0 || preTime <= 0)
            return null;
        SimpleDateFormat format = new SimpleDateFormat("yy-MM-dd-HH-mm-E");
        String now = format.format(new Date(nowTime));
        String pre = format.format(new Date(preTime));
        String[] nowTimeArr = now.split("-");
        String[] preTimeArr = pre.split("-");
        //当天以内,年月日相同，超过一分钟显示
        if (nowTimeArr[0].equals(preTimeArr[0]) && nowTimeArr[1].equals(preTimeArr[1]) && nowTimeArr[2].equals(preTimeArr[2]) && nowTime - preTime > 60000) {
            return preTimeArr[3] + ":" + preTimeArr[4];
        }
        //一周以内
        else if (Integer.valueOf(nowTimeArr[2]) - Integer.valueOf(preTimeArr[2]) > 0 && nowTime - preTime < 7 * 24 * 60 * 60 * 1000) {

            if (Integer.valueOf(nowTimeArr[2]) - Integer.valueOf(preTimeArr[2]) == 1)
                return "昨天 " + preTimeArr[3] + ":" + preTimeArr[4];
            else
                return preTimeArr[5] + " " + preTimeArr[3] + ":" + preTimeArr[4];
        }
        //一周以上
        else if (nowTime - preTime > 7 * 24 * 60 * 60 * 1000) {
            return preTimeArr[0] + "年" + preTimeArr[1] + "月" + preTimeArr[2] + "日" + " " + preTimeArr[3] + ":" + preTimeArr[4];
        }
        return null;
    }


    public static long getCurrentMillisTime() {
        return System.currentTimeMillis();
    }


    public static void transparentNav(Activity activity) {
        setTransparentStatus(true, activity);
        SystemBarTintManager tintManager = new SystemBarTintManager(activity);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setStatusBarTintResource(R.color.colorDark);
    }

    @TargetApi(19)
    private static void setTransparentStatus(boolean on, Activity activity) {
        Window win = activity.getWindow();
        WindowManager.LayoutParams params = win.getAttributes();
        final int bits = WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS;
        if (on)
            params.flags |= bits;
        else
            params.flags &= ~bits;

        win.setAttributes(params);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public static Map<Integer, String> genKeyPair() throws NoSuchAlgorithmException {

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair pair = keyGen.generateKeyPair();

        byte[] publicBytes = pair.getPublic().getEncoded();
        byte[] privateBytes = pair.getPrivate().getEncoded();

        String pubKey =  Base64.getEncoder().encodeToString(publicBytes);
        String priKey = Base64.getEncoder().encodeToString(privateBytes);
        Map<Integer,String> keyMap = new HashMap<>();
        keyMap.put(0,pubKey);
        keyMap.put(1,priKey);
        return keyMap;
    }
    /**
     * RSA公钥加密
     *
     * @param str
     *            加密字符串
     * @param publicKey
     *            公钥
     * @return 密文
     * @throws Exception
     *             加密过程中的异常信息
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String encrypt(String str, String publicKey ) {
        //base64编码的公钥
        try {
            byte[] decoded = Base64.getDecoder().decode(publicKey);
            RSAPublicKey pubKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(decoded));
            //RSA加密
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            String outStr = Base64.getEncoder().encodeToString(cipher.doFinal(str.getBytes("UTF-8")));
            return outStr;
        }catch (Exception e){
            e.printStackTrace();
        }
        return str;
    }

    /**
     * RSA私钥解密
     *
     * @param str
     *            加密字符串
     * @param privateKey
     *            私钥
     * @return 铭文
     * @throws Exception
     *             解密过程中的异常信息
     */
    @RequiresApi(api = Build.VERSION_CODES.O)
    public static String decrypt(String str, String privateKey) {
        try {
            //64位解码加密后的字符串
            byte[] inputByte = android.util.Base64.decode(str.getBytes("UTF-8"),0);
            //base64编码的私钥
            byte[] decoded = Base64.getDecoder().decode(privateKey);
            RSAPrivateKey priKey = (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(decoded));
            //RSA解密
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            cipher.init(Cipher.DECRYPT_MODE, priKey);
            String outStr = new String(cipher.doFinal(inputByte));
            return outStr;
        }catch (Exception e){
            e.printStackTrace();
        }
        return str;
    }
}
