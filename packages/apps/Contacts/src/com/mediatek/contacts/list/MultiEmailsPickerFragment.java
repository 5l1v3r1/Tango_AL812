
package com.mediatek.contacts.list;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.util.SparseArray;
import android.util.SparseBooleanArray;

import com.android.contacts.common.list.ContactEntryListAdapter;

import com.android.contacts.common.list.ContactListFilter;

import java.util.ArrayList;

public class MultiEmailsPickerFragment extends DataKindPickerBaseFragment {

    private static final String RESULTINTENTEXTRANAME = "com.mediatek.contacts.list.pickdataresult";

    @Override
    protected ContactEntryListAdapter createListAdapter() {
        MultiEmailsPickerAdapter adapter = new MultiEmailsPickerAdapter(getActivity(),
                getListView());
        adapter.setFilter(ContactListFilter
                .createFilterWithType(ContactListFilter.FILTER_TYPE_ALL_ACCOUNTS));
        return adapter;
    }
protected void custIntent(Intent retIntent, long[] idArray){
        ArrayList<Uri> arrayList = new ArrayList<Uri>(); 
        ContactEntryListAdapter adapter = getAdapter();
        SparseBooleanArray sparseBooleanArray = getListView().getCheckedItemPositions();
        int count = sparseBooleanArray.size();
        if(adapter instanceof MultiEmailsPickerAdapter){
            for(int i=0; i < count; i++){
                arrayList.add(((MultiEmailsPickerAdapter)adapter).getDataUri(sparseBooleanArray.keyAt(i)));
            }
        }
        retIntent.putExtra("SelItemData_KeyValue", arrayList);
    }

}
