package com.chatapp.fragments;

import com.chatapp.C;

import java.util.ArrayList;

import im.vector.R;

public class HomeModel {

    String name;
    String colorCode;
    int icon;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getColorCode() {
        return colorCode;
    }

    public void setColorCode(String colorCode) {
        this.colorCode = colorCode;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public static ArrayList<HomeModel> getHomeList() {
        ArrayList<HomeModel> list = new ArrayList<>();
        Integer[] iconList = {R.drawable.wallet_scroll, R.drawable.profile_scroll, R.drawable.invite_friends_scroll,
                R.drawable.chat_video_conferencing_scroll
                , R.drawable.directcall_scroll, R.drawable.airtime_scroll, R.drawable.airtime_scroll, R.drawable.databundle_scroll, R.drawable.pbx_scroll
                , R.drawable.electricy_billpay_scroll, R.drawable.tv_bill_scroll, R.drawable.video_bill, R.drawable.settings_scroll,
                R.drawable.why_scroll, R.drawable.vouchare_recharge_scorll, R.drawable.logout_scroll};
        String[] colorList = {
                "#3398dc",
                "#39a8f0",
                "#268bcf",
                "#1c81c5",
                "#0b73ba",
                "#0064a8",
                "#0064a8",
                "#00579a",
                "#005095",
                "#40c9c4",
                "#10a19c",
                "#4c98cf",
                "#2481c5",
                "#4873a6",
                "#5A5485",
                "#524365",
        };

        String[] titles = {
                C.Companion.getWALLET_BALANCE(),
                C.Companion.getBUY_CREDIT(),
                C.Companion.getINVITE_FRIEND(),
                C.Companion.getCHAT_VIDEO_CONFERENCE(),
                C.Companion.getDIRECT_CALL(),
                C.Companion.getIMAT(),
                C.Companion.getTOPB(),
                C.Companion.getDBT(),
                C.Companion.getCPF(),
                C.Companion.getEBP(), C.Companion.getTBP(), C.Companion.getVT(), C.Companion.getTH(),
                C.Companion.getTC(), C.Companion.getVR(), C.Companion.getLOGOUT()
        };
        for (int i = 0; i < titles.length; i++) {
            HomeModel model = new HomeModel();
            model.colorCode = colorList[i];
            model.name = titles[i];
            model.icon = iconList[i];
            list.add(model);

        }
        return list;

    }
}
