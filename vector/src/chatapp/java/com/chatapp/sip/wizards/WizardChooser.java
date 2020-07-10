/**
 * Copyright (C) 2010-2012 Regis Montoya (aka r3gis - www.r3gis.fr)
 * This file is part of CSipSimple.
 *
 *  CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
 *
 *  CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.chatapp.sip.wizards;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WizardChooser extends AppCompatActivity implements ExpandableListView.OnChildClickListener {
	private ArrayList<ArrayList<Map<String, Object>>> childDatas;

	// private static final String THIS_FILE = "SIP ADD ACC W";
	ExpandableListView listView;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//setContentView(R.layout.add_account_wizard);

		//listView = (ExpandableListView) findViewById(R.id.list);

		Context context = getApplicationContext();
		
		// Now build the list adapter
		childDatas = WizardUtils.getWizardsGroupedList();
		
		WizardsListAdapter adapter = new WizardsListAdapter(
				this,
				// Groups
				WizardUtils.getWizardsGroups(context),
				android.R.layout.simple_expandable_list_item_1,
				new String[] { WizardUtils.LANG_DISPLAY },
                new int[] { android.R.id.text1 },
				// Child
                childDatas,
				android.R.layout.simple_list_item_2 ,
				new String[] { WizardUtils.LABEL }, new int[] { android.R.id.text1 } );


		listView.setAdapter(adapter);

		/*Button cancelBt = (Button) findViewById(R.id.cancel_bt);
		cancelBt.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				finish();
			}
		});*/
		
		if(childDatas.size() >= 1) {
			listView.expandGroup(0);
		}
		if(childDatas.size() >= 2) {
			listView.expandGroup(1);
		}
	}
	
	@Override
	public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
		Map<String, Object> data = childDatas.get(groupPosition).get(childPosition);
		String wizard_id = (String) data.get(WizardUtils.ID);
		
		Intent result = getIntent();
		result.putExtra(WizardUtils.ID, wizard_id);
		
		setResult(RESULT_OK, result);
		finish();

		return true;
	}

	
	private class WizardsListAdapter extends SimpleExpandableListAdapter {

		public WizardsListAdapter(Context context, List<? extends Map<String, ?>> groupData, int groupLayout, String[] groupFrom, int[] groupTo,
				List<? extends List<? extends Map<String, ?>>> childData, int childLayout, String[] childFrom, int[] childTo) {
			super(context, groupData, groupLayout, groupFrom, groupTo, childData, childLayout, childFrom, childTo);
		}
		
		public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
			View v = super.getChildView(groupPosition, childPosition, isLastChild, convertView, parent);
			bindView(v, childDatas.get(groupPosition).get(childPosition), groupPosition, childPosition);
			return v;
		}
		
		private void bindView(View view, Map<String, ?> data, int groupPosition, int childPosition) {
			// Apply TextViews
			((TextView) view).setCompoundDrawablesWithIntrinsicBounds((Integer) data.get( WizardUtils.ICON ), 0, 0, 0);
		}
	}

	
}
