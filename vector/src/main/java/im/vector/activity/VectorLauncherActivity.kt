/*
 * Copyright 2019 New Vector Ltd
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

package im.vector.activity

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.webkit.WebView
import androidx.appcompat.app.AppCompatActivity
import com.chatapp.ChatLoginActivity
import com.chatapp.ChatMainActivity
import com.chatapp.FusionPBXLoginActivity

/**
 * This Activity is here only to display a logo when waiting for Riot Application to start
 */
class VectorLauncherActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val processName: String? = this!!.getProcessName(this)
            val packageName = this.packageName
            if (packageName != processName) {
                WebView.setDataDirectorySuffix(processName)
            }
        }
        startActivity(Intent(this, FusionPBXLoginActivity::class.java))
        finish()
    }
    private fun getProcessName(context: Context?): String? {
        if (context == null) return null
        val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (processInfo in manager.runningAppProcesses) {
            if (processInfo.pid == Process.myPid()) {
                return processInfo.processName
            }
        }
        return null
    }
}