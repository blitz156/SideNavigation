package com.devspark.sidenavigation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.widget.*;
import org.xmlpull.v1.XmlPullParser;

import android.content.Context;
import android.content.res.XmlResourceParser;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView.OnItemClickListener;

/**
 * View of displaying side navigation.
 * 
 * @author e.shishkin
 * 
 */
public class SideNavigationView extends LinearLayout {
    private static final String LOG_TAG = SideNavigationView.class.getSimpleName();

    private LinearLayout navigationMenu;
    private ListView listView;
    private View outsideView;
    private HashMap<Integer, View> viewCache = new HashMap<Integer, View>();

    private ISideNavigationCallback callback;
    private ArrayList<SideNavigationItem> menuItems;
    private Mode mMode = Mode.LEFT;
    private List<ToggleButton>[] buttonMenuList;
    private int currentOpenExtraDialog = -1;

    public void setCurrentOpenExtraDialog(int currentOpenExtraDialog)  {
        this.currentOpenExtraDialog = currentOpenExtraDialog;
        ((BaseAdapter)listView.getAdapter()).notifyDataSetChanged();
    }

    public static enum Mode {
        LEFT, RIGHT
    };

    /**
     * Constructor of {@link SideNavigationView}.
     * 
     * @param context
     */
    public SideNavigationView(Context context) {
        super(context);
        load();
    }

    /**
     * Constructor of {@link SideNavigationView}.
     * 
     * @param context
     * @param attrs
     */
    public SideNavigationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        load();
    }

    /**
     * Loading of side navigation view.
     */
    private void load() {
        if (isInEditMode()) {
            return;
        }
        initView();
    }

    public View getCacheViewItemByPosition(int position) {
        return viewCache.get(position);
    }

    /**
     * Initialization layout of side menu.
     */
    private void initView() {
        removeAllViews();
        int sideNavigationRes;
        switch (mMode) {
            case LEFT:
                sideNavigationRes = R.layout.side_navigation_left;
                break;
            case RIGHT:
                sideNavigationRes = R.layout.side_navigation_right;
                break;

            default:
                sideNavigationRes = R.layout.side_navigation_left;
                break;
        }
        LayoutInflater.from(getContext()).inflate(sideNavigationRes, this, true);
        navigationMenu = (LinearLayout) findViewById(R.id.side_navigation_menu);
        listView = (ListView) findViewById(R.id.side_navigation_listview);
        outsideView = (View) findViewById(R.id.side_navigation_outside_view);
        outsideView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideMenu();
            }
        });
        listView.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (callback != null) {
                    SideNavigationItem item = (SideNavigationItem)menuItems.get(position);
                    callback.onSideNavigationItemClick(item.getId());
                }
            }
        });
    }

    /**
     * Setup of {@link ISideNavigationCallback} for callback of item click.
     * 
     * @param callback
     */
    public void setMenuClickCallback(ISideNavigationCallback callback) {
        this.callback = callback;
    }

    /**
     * Setup of side menu items.
     * 
     * @param menu - resource ID
     */
    public void setMenuItems(int menu) {
        parseXml(menu);
        if (menuItems != null && menuItems.size() > 0) {
            listView.setAdapter(new SideNavigationAdapter());
        }
    }

    public void setHideItemContent(List<ToggleButton> buttonMenuList[]) {
        this.buttonMenuList = buttonMenuList;
    }

    public ListAdapter getAdapter() {
        return listView.getAdapter();
    }

    /**
     * Setup sliding mode of side menu ({@code Mode.LEFT} or {@code Mode.RIGHT}). {@code Mode.LEFT} by default.
     * 
     * @param mode Sliding mode
     */
    public void setMode(Mode mode) {
        if (isShown()) {
            hideMenu();
        }
        mMode = mode;
        initView();
        // setup menu items
        if (menuItems != null && menuItems.size() > 0) {
            listView.setAdapter(new SideNavigationAdapter());
        }
    }

    /**
     * Getting current side menu mode ({@code Mode.LEFT} or {@code Mode.RIGHT}). {@code Mode.LEFT} by default.
     * 
     * @return side menu mode
     */
    public Mode getMode() {
        return mMode;
    }

    /**
	 * 
	 */
    public void setBackgroundResource(int resource) {
        listView.setBackgroundResource(resource);
    }

    /**
     * Show side navigation menu.
     */
    public void showMenu() {
        outsideView.setVisibility(View.VISIBLE);
        outsideView.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.side_navigation_fade_in));
        // show navigation menu with animation
        int animRes;
        switch (mMode) {
            case LEFT:
                animRes = R.anim.side_navigation_in_from_left;
                break;
            case RIGHT:
                animRes = R.anim.side_navigation_in_from_right;
                break;

            default:
                animRes = R.anim.side_navigation_in_from_left;
                break;
        }
        navigationMenu.setVisibility(View.VISIBLE);
        navigationMenu.startAnimation(AnimationUtils.loadAnimation(getContext(), animRes));
    }

    /**
     * Hide side navigation menu.
     */
    public void hideMenu() {
        outsideView.setVisibility(View.GONE);
        outsideView.startAnimation(AnimationUtils.loadAnimation(getContext(), R.anim.side_navigation_fade_out));
        // hide navigation menu with animation
        int animRes;
        switch (mMode) {
            case LEFT:
                animRes = R.anim.side_navigation_out_to_left;
                break;
            case RIGHT:
                animRes = R.anim.side_navigation_out_to_right;
                break;

            default:
                animRes = R.anim.side_navigation_out_to_left;
                break;
        }
        navigationMenu.setVisibility(View.GONE);
        navigationMenu.startAnimation(AnimationUtils.loadAnimation(getContext(), animRes));
    }

    /**
     * Show/Hide side navigation menu depending on visibility.
     */
    public void toggleMenu() {
        if (isShown()) {
            hideMenu();
        } else {
            showMenu();
        }
    }

    @Override
    public boolean isShown() {
        return navigationMenu.isShown();
    }

    /**
     * Parse XML describe menu.
     * 
     * @param menu - resource ID
     */
    private void parseXml(int menu) {
        menuItems = new ArrayList<SideNavigationItem>();

        try {
            XmlResourceParser xrp = getResources().getXml(menu);
            xrp.next();
            int eventType = xrp.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    String elemName = xrp.getName();
                    if (elemName.equals("item")) {
                        String textId = xrp.getAttributeValue(
                                "http://schemas.android.com/apk/res/android",
                                "title");
                        String resId = xrp.getAttributeValue(
                                "http://schemas.android.com/apk/res/android",
                                "id");
                        SideNavigationItem item = new SideNavigationItem();
                        item.setId(Integer.valueOf(resId.replace("@", "")));
                        item.setText(resourceIdToString(textId));
                        item.setType(0);
                        menuItems.add(item);
                    }
                    if (elemName.equals("hide_item")) {
                        String textId = xrp.getAttributeValue(
                                "http://schemas.android.com/apk/res/android",
                                "title");
                        String resId = xrp.getAttributeValue(
                                "http://schemas.android.com/apk/res/android",
                                "id");
                        SideNavigationItem item = new SideNavigationItem();
                        item.setId(Integer.valueOf(resId.replace("@", "")));
                        item.setText(resourceIdToString(textId));
                        item.setType(1);
                        menuItems.add(item);
                    }
                }
                eventType = xrp.next();
            }
        } catch (Exception e) {
            Log.w(LOG_TAG, e);
        }
    }

    /**
     * Convert resource ID to String.
     * 
     * @param
     * @return
     */
    private String resourceIdToString(String resId) {
        if (!resId.contains("@")) {
            return resId;
        } else {
            String id = resId.replace("@", "");
            return getResources().getString(Integer.valueOf(id));
        }
    }

    private class SideNavigationAdapter extends BaseAdapter {
        private LayoutInflater inflater;

        public SideNavigationAdapter() {
            inflater = LayoutInflater.from(getContext());
        }

        @Override
        public int getCount() {
            return menuItems.size();
        }

        @Override
        public Object getItem(int position) {
            return menuItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            SideNavigationItem item = menuItems.get(position);
            return item.getId();
        }

        @Override
        public int getViewTypeCount() {
            return 2;
        }

        @Override
        public int getItemViewType(int position) {
            return menuItems.get(position).getType();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ViewHolder holder;
            SideNavigationItem item = menuItems.get(position);

            if (convertView == null) {
                holder = new ViewHolder();

                if (item.getType() == 0) {
                    convertView = inflater.inflate(R.layout.side_navigation_item, null);
                    holder.text = (TextView) convertView.findViewById(R.id.side_navigation_item_text);
                }
                if (item.getType() == 1) {
                    convertView = inflater.inflate(R.layout.side_navigation_hide_item, null);
                    holder.verticalLL = (LinearLayout) convertView.findViewById(R.id.extra);
                }

                convertView.setTag(holder);
                viewCache.put(position, convertView);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            if (item.getType() == 0) {
                holder.text.setText(item.getText());
            }
            else {
                if (position != currentOpenExtraDialog) {
                    convertView.setVisibility(View.INVISIBLE);
                    ViewGroup.LayoutParams lp = holder.verticalLL.getLayoutParams();
                    lp.height = 0;
                    holder.verticalLL.setLayoutParams(lp);
                }
                else {
                    holder.verticalLL.removeAllViews();
                    int btnCount = buttonMenuList[currentOpenExtraDialog].size();
                    int nowCount = 0;
                    while (nowCount < btnCount) {
                        LinearLayout horizontalLL = new LinearLayout(getContext());
                        horizontalLL.setOrientation(LinearLayout.HORIZONTAL);

                        for (int columnIndex = 0; columnIndex < 5; columnIndex++) {
                            if (nowCount == btnCount) break;

                            Button btn = buttonMenuList[currentOpenExtraDialog].get(nowCount);
                            if (btn.getParent() != null)
                                ((ViewGroup)btn.getParent()).removeView(btn);
                            horizontalLL.addView(btn);
                            nowCount++;
                        }
                        holder.verticalLL.addView(horizontalLL);
                        ViewGroup.LayoutParams lp = holder.verticalLL.getLayoutParams();
                        lp.height = LayoutParams.WRAP_CONTENT;
                        holder.verticalLL.setLayoutParams(lp);
                    }
                    convertView.setVisibility(View.VISIBLE);
                }
            }

            return convertView;
        }

        class ViewHolder {
            TextView text;
            LinearLayout verticalLL;
        }
    }
}
