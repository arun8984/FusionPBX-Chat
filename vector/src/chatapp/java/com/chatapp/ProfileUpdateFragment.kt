package com.chatapp

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import im.vector.Matrix
import im.vector.R
import im.vector.VectorApp
import im.vector.activity.VectorAppCompatActivity
import im.vector.activity.VectorMediaPickerActivity
import im.vector.util.*
import kotlinx.android.synthetic.chatapp.fragment_profile_update.*
import org.jetbrains.anko.toast
import org.matrix.androidsdk.MXSession
import org.matrix.androidsdk.core.callback.ApiCallback
import org.matrix.androidsdk.core.listeners.IMXNetworkEventListener
import org.matrix.androidsdk.core.model.MatrixError
import org.matrix.androidsdk.listeners.MXMediaUploadListener

/**
 * A simple [Fragment] subclass.
 */
class ProfileUpdateFragment : DialogFragment() {

    private var mLoadingView: View? = null

    // members
    private lateinit var mSession: MXSession

    // disable some updates if there is
    private val mNetworkListener = IMXNetworkEventListener { }
    // events listener


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile_update, container, false)
    }

    override fun onStart() {
        super.onStart()
    }

    companion object {
        private const val ARG_MATRIX_ID = "VectorSettingsPreferencesFragment.ARG_MATRIX_ID"

    }

    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mLoadingView = view.findViewById(R.id.vector_settings_spinner_views)
        val appContext = activity?.applicationContext

        // retrieve the arguments
        val sessionArg = Matrix.getInstance(appContext).getSession(arguments?.getString(ARG_MATRIX_ID))

        // sanity checks
        if (null == sessionArg || !sessionArg.isAlive) {
            activity?.finish()
            return
        }

        mSession = sessionArg
        update.setOnClickListener(View.OnClickListener {
            var value = displayName.text.toString().trim();
            if (TextUtils.isEmpty(value)) {
                Toast.makeText(activity, "Please enter display name", Toast.LENGTH_LONG).show();
            } else {
                mSession.myUser.updateDisplayName(displayName.text.toString().trim(), object : ApiCallback<Void> {
                    override fun onSuccess(info: Void?) {
                        // refresh the settings value
                        PreferenceManager.getDefaultSharedPreferences(activity).edit {
                            putString(PreferencesManager.SETTINGS_DISPLAY_NAME_PREFERENCE_KEY, value)
                        }

                        onCommonDone(null)
                        goToSplash()

                    }

                    override fun onNetworkError(e: Exception) {
                        onCommonDone(e.localizedMessage)
                    }

                    override fun onMatrixError(e: MatrixError) {
                        if (MatrixError.M_CONSENT_NOT_GIVEN == e.errcode) {
                            activity?.runOnUiThread {
                                hideLoadingView()
                                (activity as VectorAppCompatActivity).consentNotGivenHelper.displayDialog(e)
                            }
                        } else {
                            onCommonDone(e.localizedMessage)
                        }
                    }

                    override fun onUnexpectedError(e: Exception) {
                        onCommonDone(e.localizedMessage)
                    }
                })
            }

        })
        displayName.setText(mSession.myUser.displayname)
        VectorUtils.loadUserAvatar(activity, mSession, settings_avatar, mSession.myUser)

        imgContainer.setOnClickListener(View.OnClickListener {
            onUpdateAvatarClick()
        })

    }

    private fun onUpdateAvatarClick() {
        if (checkPermissions(PERMISSIONS_FOR_TAKING_PHOTO, this, PERMISSION_REQUEST_CODE_LAUNCH_CAMERA)) {
            changeAvatar()
        }
    }

    private fun changeAvatar() {
        val intent = Intent(activity, VectorMediaPickerActivity::class.java)
        intent.putExtra(VectorMediaPickerActivity.EXTRA_AVATAR_MODE, true)
        startActivityForResult(intent, VectorUtils.TAKE_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                VectorUtils.TAKE_IMAGE -> {
                    val thumbnailUri = VectorUtils.getThumbnailUriFromIntent(activity, data, mSession.mediaCache)

                    if (null != thumbnailUri) {
                        displayLoadingView()

                        val resource = org.matrix.androidsdk.core.ResourceUtils.openResource(activity, thumbnailUri, null)

                        if (null != resource) {
                            mSession.mediaCache.uploadContent(resource.mContentStream, null, resource.mMimeType, null, object : MXMediaUploadListener() {

                                override fun onUploadError(uploadId: String?, serverResponseCode: Int, serverErrorMessage: String?) {
                                    activity?.runOnUiThread { onCommonDone(serverResponseCode.toString() + " : " + serverErrorMessage) }
                                }

                                override fun onUploadComplete(uploadId: String?, contentUri: String?) {
                                    activity?.runOnUiThread {
                                        mSession.myUser.updateAvatarUrl(contentUri, object : ApiCallback<Void> {
                                            override fun onSuccess(info: Void?) {
                                                onCommonDone(null)
                                                refreshDisplay()
                                            }

                                            override fun onNetworkError(e: Exception) {
                                                hideLoadingView()
                                                onCommonDone(e.localizedMessage)
                                            }

                                            override fun onMatrixError(e: MatrixError) {
                                                if (MatrixError.M_CONSENT_NOT_GIVEN == e.errcode) {
                                                    activity?.runOnUiThread {
                                                        hideLoadingView()
                                                        (activity as ChatMainActivity).consentNotGivenHelper.displayDialog(e)
                                                    }
                                                } else {
                                                    onCommonDone(e.localizedMessage)
                                                }
                                            }

                                            override fun onUnexpectedError(e: Exception) {
                                                hideLoadingView()
                                                onCommonDone(e.localizedMessage)
                                            }
                                        })
                                    }
                                }
                            })
                        }
                    }
                }
            }

        }

    }

    private fun displayLoadingView() {
        mLoadingView?.visibility = View.VISIBLE


    }

    private fun refreshDisplay() {
        hideLoadingView()
        imgCapContainer.visibility = View.GONE
        imgContainer.visibility = View.VISIBLE
        VectorUtils.loadUserAvatar(activity, mSession, settings_avatar, mSession.myUser)

    }

    private fun onCommonDone(errorMessage: String?) {
        if (!isAdded) return
        activity?.runOnUiThread {
            if (!TextUtils.isEmpty(errorMessage) && errorMessage != null) {
                VectorApp.getInstance().toast(errorMessage)
            }
            hideLoadingView()
        }
    }

    private fun goToSplash() { 
        activity!!.setResult(RESULT_OK)
        activity!!.startActivity(Intent(activity, ChatMainActivity::class.java))
        activity!!.finish()
    }


    private fun hideLoadingView() {
        mLoadingView?.visibility = View.GONE
    }

}