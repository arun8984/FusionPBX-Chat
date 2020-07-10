package com.chatapp

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.preference.PreferenceManager
import im.vector.R
import im.vector.util.PreferencesManager
import kotlinx.android.synthetic.chatapp.fragment_sync.*

/**
 * A simple [Fragment] subclass.
 */
class SyncFragment :  DialogFragment(), View.OnClickListener {

    public lateinit var isyncListener: ISyncListener

    fun setSyncListener(syncListener: ISyncListener) {
        this.isyncListener = syncListener
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_sync, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var e: SharedPreferences.Editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        e.putBoolean(PreferencesManager.IS_SYNC_DIALOG_SHOWN, true).apply();
        cancel.setOnClickListener(this)
        sync.setOnClickListener(this)

    }

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
            R.id.cancel -> {
                dismiss()
            }
            R.id.sync -> {
                var e: SharedPreferences.Editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                e.putBoolean(PreferencesManager.SETTINGS_SYNC_CONTACTS_KEY, true).apply();
                isyncListener.onSyncEnableClick()
                dismiss()
            }
        }
    }

     fun show(manager: FragmentManager){
        val ft: FragmentTransaction = manager.beginTransaction()
        ft.add(this, tag)
        ft.commit()
    }
}