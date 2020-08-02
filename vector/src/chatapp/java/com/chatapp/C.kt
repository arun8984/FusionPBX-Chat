package com.chatapp

import android.content.Context
import im.vector.Matrix

class C {
    companion object {
        var WALLET_BALANCE = "Wallet Balance"
        var BUY_CREDIT = "Profile"
        var INVITE_FRIEND = "Invite Friend"
        var CHAT_VIDEO_CONFERENCE = "Chat/Video Confrencing"
        var DIRECT_CALL = "Direct Call"
        var IMAT = "International Mobile Airtime Topup A"
        var TOPB = "International Mobile Airtime Topup B"
        var DBT = "Data Bundle Topup"
        var CPF = "Cloud PBX Features"
        var EBP = "Electricity Bill's Payment"
        var TBP = "Television Bill's Payment"
        var VT = " Video Tariff"
        var TH = "Settings"
        var TC = "Why This App ?"
        var VR = " Voucher Recharge"
        var LOGOUT = "Logout"
        fun hasCorruptedStore(contxt: Context): Boolean {
            var hasCorruptedStore = false
            val sessions = Matrix.getMXSessions(contxt)
            for (session in sessions) {
                if (session.isAlive) {
                    hasCorruptedStore = hasCorruptedStore or session.dataHandler.store!!.isCorrupted
                }
            }
            return hasCorruptedStore
        }
    }


}