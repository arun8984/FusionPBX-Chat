package com.chatapp.fragments

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.chatapp.C
import com.chatapp.ChatMainActivity
import com.chatapp.SettingsWebActivity
import com.chatapp.WalletActivity
import com.chatapp.adapters.HomeAdapter
import im.vector.R
import im.vector.activity.VectorSettingsActivity
import kotlinx.android.synthetic.chatapp.fragment_home2.*


/**
 * A simple [Fragment] subclass.
 */
class HomeFragment : Fragment(), HomeAdapter.iHomClick {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        rv.setLayoutManager(GridLayoutManager(activity, 2))
        var adapter: HomeAdapter = HomeAdapter(context, HomeModel.getHomeList(), this)
        rv.adapter = adapter

    }

    override fun onHomeClick(title: String?) {
        when (title) {
            C.BUY_CREDIT -> {
                startActivity(Intent(activity, VectorSettingsActivity::class.java))
            }
            C.WALLET_BALANCE -> {
                startActivity(Intent(activity, WalletActivity::class.java))
            }
            C.INVITE_FRIEND -> {
                (activity as ChatMainActivity).invite()
            }
            C.CHAT_VIDEO_CONFERENCE -> {
                (activity as ChatMainActivity).chat()
            }
            C.DIRECT_CALL -> {
                (activity as ChatMainActivity).dialer()

            }
            C.IMAT -> {
                val myIntent = Intent(context, SettingsWebActivity::class.java)
                myIntent.putExtra("Bundle", "TopupA")
                startActivity(myIntent)
            }C.TOPB -> {
                val myIntent = Intent(context, SettingsWebActivity::class.java)
                myIntent.putExtra("Bundle", "TopupB")
                startActivity(myIntent)
            }
            C.DBT -> {
                val myIntent = Intent(context, SettingsWebActivity::class.java)
                myIntent.putExtra("Bundle", "data")
                startActivity(myIntent)

            }
            C.CPF -> {
                val myIntent = Intent(context, SettingsWebActivity::class.java)
                myIntent.putExtra("Bundle", "ippbx")
                startActivity(myIntent)
            }
            C.EBP -> {
                val myIntent = Intent(context, SettingsWebActivity::class.java)
                myIntent.putExtra("Bundle", "electric")
                startActivity(myIntent)
            }
            C.TBP -> {
                val myIntent = Intent(context, SettingsWebActivity::class.java)
                myIntent.putExtra("Bundle", "tv")
                startActivity(myIntent)
            }
            C.VT -> {
                val i = Intent(context, SettingsWebActivity::class.java)
                i.putExtra("Bundle", "videoplan")
                startActivity(i)

            }
            C.TH -> {
                startActivity(Intent(activity, VectorSettingsActivity::class.java))
            }
            C.TC -> {
                try {
                    val myIntent = Intent(activity, SettingsWebActivity::class.java)
                    myIntent.putExtra("Bundle", "Why")
                    startActivity(myIntent)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(activity, "No application can handle this request. Please install a webbrowser", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
            }
            C.VR -> {
                (activity as ChatMainActivity).voucherTransfer()

            }

            C.LOGOUT -> {
                (activity as ChatMainActivity).logout()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (activity as ChatMainActivity).hideItem();
    }

}