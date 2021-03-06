/*
* Copyright (C) 2014 MediaTek Inc.
* Modification based on code covered by the mentioned copyright
* and/or permission notice(s).
*/
/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.print;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.database.ContentObserver;
import android.database.DataSetObserver;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.print.PrintManager;
import android.print.PrinterDiscoverySession;
import android.print.PrinterDiscoverySession.OnPrintersChangeListener;
import android.print.PrinterId;
import android.print.PrinterInfo;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.Switch;
import android.widget.TextView;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.SettingsPreferenceFragment;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.android.settings.widget.SwitchBar;
import com.android.settings.widget.ToggleSwitch;

/**
 * Fragment with print service settings.
 */
public class PrintServiceSettingsFragment extends SettingsPreferenceFragment
        implements DialogInterface.OnClickListener, SwitchBar.OnSwitchChangeListener {

    private static final int LOADER_ID_PRINTERS_LOADER = 1;

    private static final int DIALOG_ID_ENABLE_WARNING = 1;

    private final SettingsContentObserver mSettingsContentObserver =
            new SettingsContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            updateUiForServiceState();
        }
    };

    private final DataSetObserver mDataObserver = new DataSetObserver() {
        @Override
        public void onChanged() {
            invalidateOptionsMenuIfNeeded();
            updateEmptyView();
        }

        @Override
        public void onInvalidated() {
            invalidateOptionsMenuIfNeeded();
        }

        private void invalidateOptionsMenuIfNeeded() {
            final int unfilteredItemCount = mPrintersAdapter.getUnfilteredCount();
            if ((mLastUnfilteredItemCount <= 0 && unfilteredItemCount > 0)
                    || mLastUnfilteredItemCount > 0 && unfilteredItemCount <= 0) {
                getActivity().invalidateOptionsMenu();
            }
            mLastUnfilteredItemCount = unfilteredItemCount;
        }
    };

    private SwitchBar mSwitchBar;
    private ToggleSwitch mToggleSwitch;

    private String mPreferenceKey;

    private CharSequence mSettingsTitle;
    private Intent mSettingsIntent;

    private CharSequence mAddPrintersTitle;
    private Intent mAddPrintersIntent;

    private CharSequence mEnableWarningTitle;
    private CharSequence mEnableWarningMessage;

    private ComponentName mComponentName;

    private PrintersAdapter mPrintersAdapter;

    // TODO: Showing sub-sub fragment does not handle the activity title
    // so we do it but this is wrong. Do a real fix when there is time.
    private CharSequence mOldActivityTitle;

    private int mLastUnfilteredItemCount;

    private boolean mServiceEnabled;

    private SearchView mSearchView;

    @Override
    public void onResume() {
        super.onResume();
        mSettingsContentObserver.register(getContentResolver());
        updateEmptyView();
        updateUiForServiceState();
    }

    @Override
    public void onPause() {
        mSettingsContentObserver.unregister(getContentResolver());
        if (mSearchView != null) {
            mSearchView.setOnQueryTextListener(null);
        }
        super.onPause();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initComponents();
        updateUiForArguments();
    }

    @Override
    public void onDestroyView() {
        if (mOldActivityTitle != null) {
            getActivity().getActionBar().setTitle(mOldActivityTitle);
        }
        super.onDestroyView();
        mSwitchBar.removeOnSwitchChangeListener(this);
        mSwitchBar.hide();
    }

    private void onPreferenceToggled(String preferenceKey, boolean enabled) {
        ComponentName service = ComponentName.unflattenFromString(preferenceKey);
        List<ComponentName> services = PrintSettingsUtils.readEnabledPrintServices(getActivity());
        if (enabled) {
            services.add(service);
        } else {
            services.remove(service);
        }
        PrintSettingsUtils.writeEnabledPrintServices(getActivity(), services);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        CharSequence title = null;
        CharSequence message = null;
        switch (dialogId) {
            case DIALOG_ID_ENABLE_WARNING:
                title = mEnableWarningTitle;
                message = mEnableWarningMessage;
                break;
            default:
                throw new IllegalArgumentException();
        }
        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setCancelable(true)
                .setPositiveButton(android.R.string.ok, this)
                .setNegativeButton(android.R.string.cancel, this)
                .create();
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        final boolean checked;
        switch (which) {
            case DialogInterface.BUTTON_POSITIVE:
                checked = true;
                mSwitchBar.setCheckedInternal(checked);
                getArguments().putBoolean(PrintSettingsFragment.EXTRA_CHECKED, checked);
                onPreferenceToggled(mPreferenceKey, checked);
                break;
            case DialogInterface.BUTTON_NEGATIVE:
                checked = false;
                mSwitchBar.setCheckedInternal(checked);
                getArguments().putBoolean(PrintSettingsFragment.EXTRA_CHECKED, checked);
                onPreferenceToggled(mPreferenceKey, checked);
                break;
            default:
                throw new IllegalArgumentException();
        }
    }

    private void updateEmptyView() {
        ListView listView = getListView();
        ViewGroup contentRoot = (ViewGroup) listView.getParent();
        View emptyView = listView.getEmptyView();
        if (!mToggleSwitch.isChecked()) {
            if (emptyView != null && emptyView.getId() != R.id.empty_print_state) {
                contentRoot.removeView(emptyView);
                emptyView = null;
            }
            if (emptyView == null) {
                emptyView = getActivity().getLayoutInflater().inflate(
                        R.layout.empty_print_state, contentRoot, false);
                ImageView iconView = (ImageView) emptyView.findViewById(R.id.icon);
                iconView.setContentDescription(getString(R.string.print_service_disabled));
                TextView textView = (TextView) emptyView.findViewById(R.id.message);
                textView.setText(R.string.print_service_disabled);
                contentRoot.addView(emptyView);
                listView.setEmptyView(emptyView);
            }
        } else if (mPrintersAdapter.getUnfilteredCount() <= 0) {
            if (emptyView != null
                    && emptyView.getId() != R.id.empty_printers_list_service_enabled) {
                contentRoot.removeView(emptyView);
                emptyView = null;
            }
            if (emptyView == null) {
                emptyView = getActivity().getLayoutInflater().inflate(
                        R.layout.empty_printers_list_service_enabled, contentRoot, false);
                contentRoot.addView(emptyView);
                listView.setEmptyView(emptyView);
            }
        } else if (mPrintersAdapter.getCount() <= 0) {
            if (emptyView != null && emptyView.getId() != R.id.empty_print_state) {
                contentRoot.removeView(emptyView);
                emptyView = null;
            }
            if (emptyView == null) {
                emptyView = getActivity().getLayoutInflater().inflate(
                        R.layout.empty_print_state, contentRoot, false);
                ImageView iconView = (ImageView) emptyView.findViewById(R.id.icon);
                iconView.setContentDescription(getString(R.string.print_no_printers_found));
                TextView textView = (TextView) emptyView.findViewById(R.id.message);
                textView.setText(R.string.print_no_printers_found);
                contentRoot.addView(emptyView);
                listView.setEmptyView(emptyView);
            }
        }
    }

    private void updateUiForServiceState() {
        List<ComponentName> services = PrintSettingsUtils.readEnabledPrintServices(getActivity());
        mServiceEnabled = services.contains(mComponentName);
        if (mServiceEnabled) {
            mSwitchBar.setCheckedInternal(true);
            mPrintersAdapter.enable();
        } else {
            mSwitchBar.setCheckedInternal(false);
            mPrintersAdapter.disable();
        }
        getActivity().invalidateOptionsMenu();
    }

    private void initComponents() {
        mPrintersAdapter = new PrintersAdapter();
        mPrintersAdapter.registerDataSetObserver(mDataObserver);

        final SettingsActivity activity = (SettingsActivity) getActivity();

        mSwitchBar = activity.getSwitchBar();
        mSwitchBar.addOnSwitchChangeListener(this);
        mSwitchBar.show();

        mToggleSwitch = mSwitchBar.getSwitch();
        mToggleSwitch.setOnBeforeCheckedChangeListener(new ToggleSwitch.OnBeforeCheckedChangeListener() {
            @Override
            public boolean onBeforeCheckedChanged(ToggleSwitch toggleSwitch, boolean checked) {
                if (checked) {
                    if (!TextUtils.isEmpty(mEnableWarningMessage)) {
                        mSwitchBar.setCheckedInternal(false);
                        getArguments().putBoolean(PrintSettingsFragment.EXTRA_CHECKED, false);
                        showDialog(DIALOG_ID_ENABLE_WARNING);
                        return true;
                    }
                    onPreferenceToggled(mPreferenceKey, true);
                } else {
                    onPreferenceToggled(mPreferenceKey, false);
                }
                return false;
            }
        });

        getListView().setSelector(new ColorDrawable(Color.TRANSPARENT));
        getListView().setAdapter(mPrintersAdapter);
    }


    @Override
    public void onSwitchChanged(Switch switchView, boolean isChecked) {
        updateEmptyView();
    }

    private void updateUiForArguments() {
        Bundle arguments = getArguments();

        // Key.
        mPreferenceKey = arguments.getString(PrintSettingsFragment.EXTRA_PREFERENCE_KEY);

        // Enabled.
        final boolean enabled = arguments.getBoolean(PrintSettingsFragment.EXTRA_CHECKED);
        mSwitchBar.setCheckedInternal(enabled);

        // Settings title and intent.
        String settingsTitle = arguments.getString(PrintSettingsFragment.EXTRA_SETTINGS_TITLE);
        String settingsComponentName = arguments.getString(
                PrintSettingsFragment.EXTRA_SETTINGS_COMPONENT_NAME);
        if (!TextUtils.isEmpty(settingsTitle) && !TextUtils.isEmpty(settingsComponentName)) {
            Intent settingsIntent = new Intent(Intent.ACTION_MAIN).setComponent(
                    ComponentName.unflattenFromString(settingsComponentName.toString()));
            List<ResolveInfo> resolvedActivities = getPackageManager().queryIntentActivities(
                    settingsIntent, 0);
            if (!resolvedActivities.isEmpty()) {
                // The activity is a component name, therefore it is one or none.
                if (resolvedActivities.get(0).activityInfo.exported) {
                    mSettingsTitle = settingsTitle;
                    mSettingsIntent = settingsIntent;
                }
            }
        }

        // Add printers title and intent.
        String addPrintersTitle = arguments.getString(
                PrintSettingsFragment.EXTRA_ADD_PRINTERS_TITLE);
        String addPrintersComponentName =
                arguments.getString(PrintSettingsFragment.EXTRA_ADD_PRINTERS_COMPONENT_NAME);
        if (!TextUtils.isEmpty(addPrintersTitle)
                && !TextUtils.isEmpty(addPrintersComponentName)) {
            Intent addPritnersIntent = new Intent(Intent.ACTION_MAIN).setComponent(
                    ComponentName.unflattenFromString(addPrintersComponentName.toString()));
            List<ResolveInfo> resolvedActivities = getPackageManager().queryIntentActivities(
                    addPritnersIntent, 0);
            if (!resolvedActivities.isEmpty()) {
                // The activity is a component name, therefore it is one or none.
                if (resolvedActivities.get(0).activityInfo.exported) {
                    mAddPrintersTitle = addPrintersTitle;
                    mAddPrintersIntent = addPritnersIntent;
                }
            }
        }

        // Enable warning title.
        mEnableWarningTitle = arguments.getCharSequence(
                PrintSettingsFragment.EXTRA_ENABLE_WARNING_TITLE);

        // Enable warning message.
        mEnableWarningMessage = arguments.getCharSequence(
                PrintSettingsFragment.EXTRA_ENABLE_WARNING_MESSAGE);

        // Component name.
        mComponentName = ComponentName.unflattenFromString(arguments
                .getString(PrintSettingsFragment.EXTRA_SERVICE_COMPONENT_NAME));

        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.print_service_settings, menu);

        MenuItem addPrinters = menu.findItem(R.id.print_menu_item_add_printer);
        if (mServiceEnabled && !TextUtils.isEmpty(mAddPrintersTitle)
                && mAddPrintersIntent != null) {
            addPrinters.setIntent(mAddPrintersIntent);
        } else {
            menu.removeItem(R.id.print_menu_item_add_printer);
        }

        MenuItem settings = menu.findItem(R.id.print_menu_item_settings);
        if (mServiceEnabled && !TextUtils.isEmpty(mSettingsTitle)
                && mSettingsIntent != null) {
            settings.setIntent(mSettingsIntent);
        } else {
            menu.removeItem(R.id.print_menu_item_settings);
        }

        MenuItem searchItem = menu.findItem(R.id.print_menu_item_search);
        if (mServiceEnabled && mPrintersAdapter.getUnfilteredCount() > 0) {
            mSearchView = (SearchView) searchItem.getActionView();
            mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String searchString) {
                    /// M: fix google issue ALPS01285513 @{
                    Activity activity = getActivity();
                    if (activity != null && !activity.isFinishing()) {
                        ((Filterable) getListView().getAdapter()).getFilter().filter(searchString);
                    }
                    /// @}
                    return true;
                }
            });
            mSearchView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View view) {
                    if (AccessibilityManager.getInstance(getActivity()).isEnabled()) {
                        view.announceForAccessibility(getString(
                                R.string.print_search_box_shown_utterance));
                    }
                }
                @Override
                public void onViewDetachedFromWindow(View view) {
                    Activity activity = getActivity();
                    if (activity != null && !activity.isFinishing()
                            && AccessibilityManager.getInstance(activity).isEnabled()) {
                        view.announceForAccessibility(getString(
                                R.string.print_search_box_hidden_utterance));
                    }
                }
            });
        } else {
            menu.removeItem(R.id.print_menu_item_search);
        }
    }

    private static abstract class SettingsContentObserver extends ContentObserver {

        public SettingsContentObserver(Handler handler) {
            super(handler);
        }

        public void register(ContentResolver contentResolver) {
            contentResolver.registerContentObserver(android.provider.Settings.Secure.getUriFor(
                    android.provider.Settings.Secure.ENABLED_PRINT_SERVICES), false, this);
        }

        public void unregister(ContentResolver contentResolver) {
            contentResolver.unregisterContentObserver(this);
        }

        @Override
        public abstract void onChange(boolean selfChange, Uri uri);
    }

    private final class PrintersAdapter extends BaseAdapter
            implements LoaderManager.LoaderCallbacks<List<PrinterInfo>>, Filterable {
        private final Object mLock = new Object();

        private final List<PrinterInfo> mPrinters = new ArrayList<PrinterInfo>();

        private final List<PrinterInfo> mFilteredPrinters = new ArrayList<PrinterInfo>();

        private CharSequence mLastSearchString;

        public void enable() {
            getLoaderManager().initLoader(LOADER_ID_PRINTERS_LOADER, null, this);
        }

        public void disable() {
            getLoaderManager().destroyLoader(LOADER_ID_PRINTERS_LOADER);
            mPrinters.clear();
        }

        public int getUnfilteredCount() {
            return mPrinters.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence constraint) {
                    synchronized (mLock) {
                        if (TextUtils.isEmpty(constraint)) {
                            return null;
                        }
                        FilterResults results = new FilterResults();
                        List<PrinterInfo> filteredPrinters = new ArrayList<PrinterInfo>();
                        String constraintLowerCase = constraint.toString().toLowerCase();
                        final int printerCount = mPrinters.size();
                        for (int i = 0; i < printerCount; i++) {
                            PrinterInfo printer = mPrinters.get(i);
                            if (printer.getName().toLowerCase().contains(constraintLowerCase)) {
                                filteredPrinters.add(printer);
                            }
                        }
                        results.values = filteredPrinters;
                        results.count = filteredPrinters.size();
                        return results;
                    }
                }

                @Override
                @SuppressWarnings("unchecked")
                protected void publishResults(CharSequence constraint, FilterResults results) {
                    synchronized (mLock) {
                        mLastSearchString = constraint;
                        mFilteredPrinters.clear();
                        if (results == null) {
                            mFilteredPrinters.addAll(mPrinters);
                        } else {
                            List<PrinterInfo> printers = (List<PrinterInfo>) results.values;
                            mFilteredPrinters.addAll(printers);
                        }
                    }
                    notifyDataSetChanged();
                }
            };
        }

        @Override
        public int getCount() {
            synchronized (mLock) {
                return mFilteredPrinters.size();
            }
        }

        @Override
        public Object getItem(int position) {
            synchronized (mLock) {
                return mFilteredPrinters.get(position);
            }
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getActivity().getLayoutInflater().inflate(
                        R.layout.printer_dropdown_item, parent, false);
            }

            PrinterInfo printer = (PrinterInfo) getItem(position);
            CharSequence title = printer.getName();
            CharSequence subtitle = null;
            Drawable icon = null;
            try {
                PackageInfo packageInfo = getPackageManager().getPackageInfo(
                        printer.getId().getServiceName().getPackageName(), 0);
                        subtitle = packageInfo.applicationInfo.loadLabel(getPackageManager());
                        icon = packageInfo.applicationInfo.loadIcon(getPackageManager());
            } catch (NameNotFoundException nnfe) {
                /* ignore */
            }

            TextView titleView = (TextView) convertView.findViewById(R.id.title);
            titleView.setText(title);

            TextView subtitleView = (TextView) convertView.findViewById(R.id.subtitle);
            if (!TextUtils.isEmpty(subtitle)) {
                subtitleView.setText(subtitle);
                subtitleView.setVisibility(View.VISIBLE);
            } else {
                subtitleView.setText(null);
                subtitleView.setVisibility(View.GONE);
            }

            ImageView iconView = (ImageView) convertView.findViewById(R.id.icon);
            if (icon != null) {
                iconView.setImageDrawable(icon);
                iconView.setVisibility(View.VISIBLE);
            } else {
                iconView.setVisibility(View.GONE);
            }

            return convertView;
        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

        @Override
        public Loader<List<PrinterInfo>> onCreateLoader(int id, Bundle args) {
            if (id == LOADER_ID_PRINTERS_LOADER) {
                return new PrintersLoader(getActivity());
            }
            return null;
        }

        @Override
        public void onLoadFinished(Loader<List<PrinterInfo>> loader,
                List<PrinterInfo> printers) {
            synchronized (mLock) {
                mPrinters.clear();
                final int printerCount = printers.size();
                for (int i = 0; i < printerCount; i++) {
                    PrinterInfo printer = printers.get(i);
                    if (printer.getId().getServiceName().equals(mComponentName)) {
                        mPrinters.add(printer);
                    }
                }
                mFilteredPrinters.clear();
                mFilteredPrinters.addAll(mPrinters);
                if (!TextUtils.isEmpty(mLastSearchString)) {
                    getFilter().filter(mLastSearchString);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public void onLoaderReset(Loader<List<PrinterInfo>> loader) {
            synchronized (mLock) {
                mPrinters.clear();
                mFilteredPrinters.clear();
                mLastSearchString = null;
            }
            notifyDataSetInvalidated();
        }
    }

    private static class PrintersLoader extends Loader<List<PrinterInfo>> {

        private static final String LOG_TAG = "PrintersLoader";

        private static final boolean DEBUG = false;

        private final Map<PrinterId, PrinterInfo> mPrinters =
                new LinkedHashMap<PrinterId, PrinterInfo>();

        private PrinterDiscoverySession mDiscoverySession;

        public PrintersLoader(Context context) {
            super(context);
        }

        @Override
        public void deliverResult(List<PrinterInfo> printers) {
            if (isStarted()) {
                super.deliverResult(printers);
            }
        }

        @Override
        protected void onStartLoading() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onStartLoading()");
            }
            // The contract is that if we already have a valid,
            // result the we have to deliver it immediately.
            if (!mPrinters.isEmpty()) {
                deliverResult(new ArrayList<PrinterInfo>(mPrinters.values()));
            }
            // We want to start discovery at this point.
            onForceLoad();
        }

        @Override
        protected void onStopLoading() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onStopLoading()");
            }
            onCancelLoad();
        }

        @Override
        protected void onForceLoad() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onForceLoad()");
            }
            loadInternal();
        }

        @Override
        protected boolean onCancelLoad() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onCancelLoad()");
            }
            return cancelInternal();
        }

        @Override
        protected void onReset() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onReset()");
            }
            onStopLoading();
            mPrinters.clear();
            if (mDiscoverySession != null) {
                mDiscoverySession.destroy();
                mDiscoverySession = null;
            }
        }

        @Override
        protected void onAbandon() {
            if (DEBUG) {
                Log.i(LOG_TAG, "onAbandon()");
            }
            onStopLoading();
        }

        private boolean cancelInternal() {
            if (mDiscoverySession != null
                    && mDiscoverySession.isPrinterDiscoveryStarted()) {
                mDiscoverySession.stopPrinterDiscovery();
                return true;
            }
            return false;
        }

        private void loadInternal() {
            if (mDiscoverySession == null) {
                PrintManager printManager = (PrintManager) getContext()
                        .getSystemService(Context.PRINT_SERVICE);
                mDiscoverySession = printManager.createPrinterDiscoverySession();
                mDiscoverySession.setOnPrintersChangeListener(new OnPrintersChangeListener() {
                    @Override
                    public void onPrintersChanged() {
                        deliverResult(new ArrayList<PrinterInfo>(
                                mDiscoverySession.getPrinters()));
                    }
                });
            }
            mDiscoverySession.startPrinterDiscovery(null);
        }
    }
}

