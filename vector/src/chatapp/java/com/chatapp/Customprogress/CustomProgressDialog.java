package com.chatapp.Customprogress;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import im.vector.R;


/**
 * Created by Jack Wang on 2016/5/6.
 */
public class CustomProgressDialog extends AlertDialog {

    private TextView mMessageView;

    public CustomProgressDialog(Context context) {
        super(context);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.progress_avld, null);
        getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
        setCancelable(false);
        mMessageView = (TextView) view.findViewById(R.id.message);
        setView(view);
    }


    @Override
    public void setMessage(CharSequence message) {
        mMessageView.setText(message);
    }

}
