package net.typeblog.socks;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AppSelector {

    public interface OnAppSelectedListener {
        void onAppSelected(String appList);
    }

    public static void show(Activity activity, String currentApps, final OnAppSelectedListener listener) {
        final PackageManager pm = activity.getPackageManager();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        final List<AppInfo> appList = new ArrayList<>();
        
        Set<String> selectedPackages = new HashSet<>();
        if (currentApps != null && !currentApps.isEmpty()) {
            for (String p : currentApps.split("\n")) {
                selectedPackages.add(p.trim());
            }
        }

        for (ApplicationInfo packageInfo : packages) {
            // Filter out system apps that are not important or don't have a launcher? 
            // Better to show all but sort them.
            AppInfo info = new AppInfo();
            info.name = packageInfo.loadLabel(pm).toString();
            info.packageName = packageInfo.packageName;
            info.icon = packageInfo.loadIcon(pm);
            info.selected = selectedPackages.contains(info.packageName);
            appList.add(info);
        }

        Collections.sort(appList, new Comparator<AppInfo>() {
            @Override
            public int compare(AppInfo o1, AppInfo o2) {
                return o1.name.compareToIgnoreCase(o2.name);
            }
        });

        LinearLayout layout = new LinearLayout(activity);
        layout.setOrientation(LinearLayout.VERTICAL);

        EditText searchBox = new EditText(activity);
        searchBox.setHint(R.string.search_apps);
        searchBox.setSingleLine(true);
        layout.addView(searchBox);

        ListView listView = new ListView(activity);
        final AppAdapter adapter = new AppAdapter(activity, appList);
        listView.setAdapter(adapter);
        
        // Add ListView with weight 1 so it takes remaining space
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1.0f);
        layout.addView(listView, listParams);

        searchBox.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s);
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });

        new AlertDialog.Builder(activity)
                .setTitle(R.string.adv_app_selector)
                .setView(layout)
                .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    // Use simple loop for compatibility
                    StringBuilder result = new StringBuilder();
                    for (AppInfo info : appList) {
                        if (info.selected) {
                            result.append(info.packageName).append("\n");
                        }
                    }
                    listener.onAppSelected(result.toString().trim());
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private static class AppInfo {
        String name;
        String packageName;
        Drawable icon;
        boolean selected;
    }

    private static class AppAdapter extends BaseAdapter implements Filterable {
        private Context context;
        private List<AppInfo> appsOriginal;
        private List<AppInfo> appsFiltered;
        private AppFilter filter;

        AppAdapter(Context context, List<AppInfo> apps) {
            this.context = context;
            this.appsOriginal = apps;
            this.appsFiltered = apps;
        }

        @Override
        public int getCount() {
            return appsFiltered.size();
        }

        @Override
        public Object getItem(int position) {
            return appsFiltered.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(android.R.layout.simple_list_item_multiple_choice, parent, false);
                // We want custom layout with icon, but simple_list_item_multiple_choice is easiest for checkboxes.
                // Let's create a very simple one instead.
                convertView = LayoutInflater.from(context).inflate(R.layout.app_item, parent, false);
            }

            AppInfo info = appsFiltered.get(position);
            TextView text = convertView.findViewById(R.id.app_name);
            TextView pkg = convertView.findViewById(R.id.app_package);
            ImageView icon = convertView.findViewById(R.id.app_icon);
            CheckBox cb = convertView.findViewById(R.id.app_check);

            text.setText(info.name);
            pkg.setText(info.packageName);
            icon.setImageDrawable(info.icon);
            cb.setChecked(info.selected);

            convertView.setOnClickListener(v -> {
                info.selected = !info.selected;
                cb.setChecked(info.selected);
            });

            return convertView;
        }

        @Override
        public Filter getFilter() {
            if (filter == null) {
                filter = new AppFilter();
            }
            return filter;
        }

        private class AppFilter extends Filter {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                FilterResults results = new FilterResults();
                if (constraint == null || constraint.length() == 0) {
                    results.values = appsOriginal;
                    results.count = appsOriginal.size();
                } else {
                    String query = constraint.toString().toLowerCase();
                    List<AppInfo> filtered = new ArrayList<>();
                    for (AppInfo info : appsOriginal) {
                        if (info.name.toLowerCase().contains(query) || info.packageName.toLowerCase().contains(query)) {
                            filtered.add(info);
                        }
                    }
                    results.values = filtered;
                    results.count = filtered.size();
                }
                return results;
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void publishResults(CharSequence constraint, FilterResults results) {
                appsFiltered = (List<AppInfo>) results.values;
                notifyDataSetChanged();
            }
        }
    }
}
