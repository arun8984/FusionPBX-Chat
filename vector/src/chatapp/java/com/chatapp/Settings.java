/*
 * Copyright (c) 2020 New Vector Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.chatapp;


import android.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class Settings {

    //public static boolean hasContactPermission;
    public static boolean useNativeCamera = true;
    public static boolean useJitsiConfCall = true;
    public static boolean useBatteryOptimisation = true;

    public static String SIPServer = "";
    public static String SIPDomain = "";
    public static final int SIPPort = 6580;
    public static final String CHECK_DEVICE_API = "http://192.95.33.62/vibgyor_okay/billing_auto_register/check_device.php";
    public static final String UPDATE_DEVICE_API = "http://192.95.33.62/vibgyor_okay/billing_auto_register/update_device.php";
    public static final String TRACK_GET_DEVICELIST_API = "https://billing.adoreinfotech.co.in/crm/videos_im/billing_auto_register/get_device_list.php";
    public static final String GET_DEVICEINFO_API = "https://billing.adoreinfotech.co.in/crm/videos_im/billing_auto_register/get_device_location.php";
    public static final String VIDEO_CHARGE = "https://billing.adoreinfotech.co.in/crm/WhizzApp_api/billing_balance/video_charges.php";
    public static final String VIDEO_CALL_TIME = "https://billing.adoreinfotech.co.in/crm/WhizzApp_api/billing_balance/video_minutes.php";
    public static final String VIDEO_POPUP = "https://billing.adoreinfotech.co.in/crm/WhizzApp_api/billing_auto_register/video_popup.php";
    public static final String OTP_REQUEST_API = "http://209.126.75.76/crm/jude_api/billing_auto_register/otp_send.php";
    public static final String OTP_VERIFY_API = "http://209.126.75.76/crm/jude_api/billing_auto_register/otp_secure.php";
    public static final String CONTACTS_SYNC_API = "http://209.126.75.76/crm/jude_api/billing_auto_register/sync_contacts.php";
    public static final String BALANCE_API = "http://209.126.75.76/crm/jude_api/billing_balance/get_balance.php";
    public static final String RATES_API = "http://209.126.75.76/crm/jude_api/billing_rates/get_rates.php";
    public static final String BALANCE_TRANSFER_API = "http://209.126.75.76/crm/jude_api/billing_balance_transfer_balance/balance_transfer_org.php";
    public static final String BALANCE_TRANSFER_HISTORY = "http://209.126.75.76/crm/jude_api/billing_balance_transfer_balance/balance_transfer_report.php";
    public static final String VOUCHER_RECHARGE = "http://209.126.75.76/crm/jude_api/billing_voucher_recharge/refill_dialer_voucher.php";
    public static final String ENC_KEY = "h5r@mg7$#ueqdstj";
    public static final String DEFAULT_PUSHER_APP_ID = "com.dial09";
    public static final String DEFAULT_PUSHER_URL = "  ";
    public static String PushMsgID = "";




    public static String encrypt(String input, String key) {
        byte[] crypted = null;

        try {
            SecretKeySpec skey = new SecretKeySpec(key.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, skey);
            crypted = cipher.doFinal(input.getBytes());
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return new String(Base64.encode(crypted,android.util.Base64.DEFAULT));
    }
    public static char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    public static String asHex(byte[] buf){
        char[] chars = new char[2 * buf.length];

        for (int i = 0; i < buf.length; ++i){
            chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
            chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
        }

        return new String(chars);
    }

}

