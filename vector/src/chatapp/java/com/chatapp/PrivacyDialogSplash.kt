package com.chatapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import im.vector.R
import kotlinx.android.synthetic.chatapp.fragment_privacy_dialog_splash.*

/**
 * A simple [Fragment] subclass.
 */
class PrivacyDialogSplash : DialogFragment(), View.OnClickListener {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_privacy_dialog_splash, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        privacy_continue.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.privacy_continue -> {
                if (!cb.isChecked) {
                    Toast.makeText(activity, "Please accpet Term and Conditions", Toast.LENGTH_LONG).show()
                    return
                }
                dismiss()
            }
        }
    }
}