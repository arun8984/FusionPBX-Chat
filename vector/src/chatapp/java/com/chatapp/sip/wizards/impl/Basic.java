package com.chatapp.sip.wizards.impl;

import im.vector.R;
import com.chatapp.ChatMainActivity;
import com.chatapp.Settings;

import com.chatapp.sip.api.SipProfile;
import com.chatapp.sip.api.SipUri;
//import com.chatapp.sip.ui.account.AccountWizard;
import com.chatapp.sip.utils.Log;

import java.util.HashMap;

public class Basic extends BaseImplementation {
	protected static final String THIS_FILE = "Basic W";

	/*
	 * private void bindFields() { accountDisplayName = (EditTextPreference)
	 * findPreference("display_name"); accountUserName = (EditTextPreference)
	 * findPreference("username"); accountServer = (EditTextPreference)
	 * findPreference("server"); accountPassword = (EditTextPreference)
	 * findPreference("password"); }
	 */
	public void fillLayout(final SipProfile account) {
		/*
		 * bindFields();
		 *
		 * accountDisplayName.setText(account.display_name);
		 */

		/*
		 * String serverFull = account.reg_uri; if (serverFull == null) {
		 * serverFull = ""; }else { serverFull = serverFull.replaceFirst("sip:",
		 * ""); }
		 *
		 * ParsedSipContactInfos parsedInfo =
		 * SipUri.parseSipContact(account.acc_id);
		 * accountUserName.setText(parsedInfo.userName);
		 * accountServer.setText(serverFull);
		 * accountPassword.setText(account.data);
		 */}

	public void updateDescriptions() {
		setStringFieldSummary("display_name");
		setStringFieldSummary("username");
		setStringFieldSummary("server");
		setPasswordFieldSummary("password");

	}

	private static HashMap<String, Integer> SUMMARIES = new HashMap<String, Integer>() {
		/**
		 *
		 */
		private static final long serialVersionUID = -5743705263738203615L;

		{
			put("display_name", R.string.w_common_display_name_desc);
			put("username", R.string.w_basic_username_desc);
			put("server", R.string.w_common_server_desc);
			put("password", R.string.w_basic_password_desc);

		}
	};

	@Override
	public String getDefaultFieldSummary(String fieldName) {
		Integer res = SUMMARIES.get(fieldName);
		if (res != null) {
			//return parent.getString(res);
			return "";
		}
		return "";
	}

	public boolean canSave() {
		boolean isValid = true;
		/*
		 * isValid &= checkField(accountDisplayName,
		 * isEmpty(accountDisplayName)); isValid &= checkField(accountPassword,
		 * isEmpty(accountPassword)); isValid &= checkField(accountServer,
		 * isEmpty(accountServer)); isValid &= checkField(accountUserName,
		 * isEmpty(accountUserName));
		 */
		return isValid;
	}

	public SipProfile buildAccount(SipProfile account) {
		Log.d(THIS_FILE, "begin of save ....");
		// TODO account name
		String sip = Settings.SIPServer//"206.165.108.83"//"206.165.108.63"//AccountWizard.sip.getText().toString()
				,user = ChatMainActivity.SipUsername//account.getSipUserName()
				, pass = ChatMainActivity.SipPassword//account.getPassword()
				, proxy=""; //AccountWizard.proxy.getText().toString();
		account.display_name = "MTN9ja";

		String[] serverParts = sip.split(":");
		account.acc_id = "<sip:" + SipUri.encodeUser(user.trim()) + "@"
				+ serverParts[0].trim() + ">";
		String regUri = "sip:" + sip;//AccountWizard.sip.getText().toString();
		account.reg_uri = regUri;
		if (proxy.length()==0)
			account.proxies = new String[] { regUri };
		else
			account.proxies = new String[] { "sip:" + proxy.trim() };
		Log.e("account fetched", user + regUri);
		account.realm = "*";
		account.username = user.trim();
		account.data = pass;
		account.scheme = SipProfile.CRED_SCHEME_DIGEST;
		account.datatype = SipProfile.CRED_DATA_PLAIN_PASSWD;
		// By default auto transport
		// account.transport = SipProfile.TRANSPORT_UDP;
		account.transport = SipProfile.TRANSPORT_AUTO;
		return account;
	}

	@Override
	public int getBasePreferenceResource() {
		//return R.xml.w_basic_preferences;
		return 1;
	}

	@Override
	public boolean needRestart() {
		return false;
	}
}