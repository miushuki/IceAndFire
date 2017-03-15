package com.southernbox.inf.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.southernbox.inf.R;
import com.southernbox.inf.entity.ContentDTO;
import com.southernbox.inf.entity.TabDTO;
import com.southernbox.inf.pager.MainViewPager;
import com.southernbox.inf.util.DayNightHelper;
import com.southernbox.inf.util.ToastUtil;
import com.southernbox.inf.widget.MaterialSearchView.MaterialSearchView;

import java.util.List;

/**
 * Created SouthernBox on 2016/3/27.
 * 主页
 */

public class MainActivity extends BaseActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private final static String TYPE_PERSON = "person";
    private final static String TYPE_HOUSE = "house";
    private final static String TYPE_HISTORY = "history";
    private final static String TYPE_CASTLE = "castle";

    private Toolbar mToolbar;
    private DrawerLayout drawer;
    private NavigationView navigationView;
    private SwitchCompat switchCompat;
    private MainViewPager mViewPager;

    private MaterialSearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initToolbar();
        initDrawerLayout();
        initNavigationView();
        initViewPager("人物", TYPE_PERSON);
    }

    private void initToolbar() {
        mToolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(mToolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        mToolbar.post(new Runnable() {
            @Override
            public void run() {
                //设置Toolbar的图标
                refreshToolbarIcon();
            }
        });

        searchView = (MaterialSearchView) findViewById(R.id.search_view);
        searchView.setEllipsize(true);
        searchView.setHint("搜索");

        List<ContentDTO> contentList = mRealm.where(ContentDTO.class).findAll();
        String[] contentNames = new String[contentList.size()];
        for (int i = 0; i < contentList.size(); i++) {
            contentNames[i] = contentList.get(i).getName();
        }
//        searchView.setSuggestions(getResources().getStringArray(R.array.query_suggestions));
        searchView.setSuggestions(contentNames);

        searchView.setOnSuggestionClickListener(new MaterialSearchView.OnSuggestionClickListener() {
            @Override
            public void onSuggestionClick(String name) {
                searchView.closeSearch();
                ContentDTO content = mRealm.where(ContentDTO.class)
                        .equalTo("name", name)
                        .findFirst();
                if (content != null) {
                    DetailActivity.show(
                            mContext,
                            content.getName(),
                            content.getImg(),
                            content.getHtml()
                    );
                }
            }
        });
    }

    private void initDrawerLayout() {
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, mToolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void initNavigationView() {
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        View navigationHeader = navigationView.getHeaderView(0);
        if (mDayNightHelper.isDay()) {
            navigationHeader.setBackgroundResource(R.drawable.side_nav_bar_day);
        } else {
            navigationHeader.setBackgroundResource(R.drawable.side_nav_bar_night);
        }

        Menu menu = navigationView.getMenu();
        MenuItem nightItem = menu.findItem(R.id.nav_night);
        View nightView = MenuItemCompat.getActionView(nightItem);
        switchCompat = (SwitchCompat) nightView.findViewById(R.id.switch_compat);

        if (mDayNightHelper.isDay()) {
            switchCompat.setChecked(false);
        } else {
            switchCompat.setChecked(true);
        }

        switchCompat.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    mDayNightHelper.setMode(DayNightHelper.DayNight.NIGHT);
                    setTheme(R.style.NightTheme);
                    refreshUI(false);
                } else {
                    mDayNightHelper.setMode(DayNightHelper.DayNight.DAY);
                    setTheme(R.style.DayTheme);
                    refreshUI(true);
                }
                showAnimation();
            }
        });
    }

    /**
     * 展示一个切换动画
     */
    private void showAnimation() {
        final View decorView = getWindow().getDecorView();
        Bitmap cacheBitmap = getCacheBitmapFromView(decorView);
        if (decorView instanceof ViewGroup && cacheBitmap != null) {
            final View view = new View(this);
            view.setBackground(new BitmapDrawable(getResources(), cacheBitmap));
            ViewGroup.LayoutParams layoutParam = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT);
            ((ViewGroup) decorView).addView(view, layoutParam);
            ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f);
            objectAnimator.setDuration(300);
            objectAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    ((ViewGroup) decorView).removeView(view);
                }
            });
            objectAnimator.start();
        }
    }

    /**
     * 获取 View 的缓存视图
     *
     * @param view 对应的View
     * @return 对应View的缓存视图
     */
    private Bitmap getCacheBitmapFromView(View view) {
        final boolean drawingCacheEnabled = true;
        view.setDrawingCacheEnabled(drawingCacheEnabled);
        view.buildDrawingCache(drawingCacheEnabled);
        final Bitmap drawingCache = view.getDrawingCache();
        Bitmap bitmap;
        if (drawingCache != null) {
            bitmap = Bitmap.createBitmap(drawingCache);
            view.setDrawingCacheEnabled(false);
        } else {
            bitmap = null;
        }
        return bitmap;
    }

    /**
     * 刷新界面UI
     */
    private void refreshUI(boolean isDay) {
        Resources.Theme theme = getTheme();
        TypedValue colorPrimary = new TypedValue();
        theme.resolveAttribute(R.attr.colorPrimary, colorPrimary, true);
        TypedValue colorPrimaryDark = new TypedValue();
        theme.resolveAttribute(R.attr.colorPrimaryDark, colorPrimaryDark, true);
        TypedValue colorAccent = new TypedValue();
        theme.resolveAttribute(R.attr.colorAccent, colorAccent, true);
        TypedValue colorBackground = new TypedValue();
        theme.resolveAttribute(R.attr.colorBackground, colorBackground, true);
        TypedValue darkTextColor = new TypedValue();
        theme.resolveAttribute(R.attr.darkTextColor, darkTextColor, true);
        TypedValue lightTextColor = new TypedValue();
        theme.resolveAttribute(R.attr.lightTextColor, lightTextColor, true);

        //更新Toolbar的背景、标题、图标颜色
        mToolbar.setBackgroundResource(colorPrimary.resourceId);
        mToolbar.setTitleTextColor(ContextCompat.getColor(mContext, lightTextColor.resourceId));
        refreshToolbarIcon();

        //更新TabLayout的背景及标识线颜色
        TabLayout mTabLayout = (TabLayout) findViewById(R.id.tab_layout);
        mTabLayout.setBackgroundResource(colorPrimary.resourceId);
        mTabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(mContext, colorAccent.resourceId));
        //更新侧滑菜单标题栏背景及字体颜色
        View navigationHeader = navigationView.getHeaderView(0);
        if (isDay) {
            navigationHeader.setBackgroundResource(R.drawable.side_nav_bar_day);
        } else {
            navigationHeader.setBackgroundResource(R.drawable.side_nav_bar_night);
        }
        TextView tvHeader = (TextView) navigationHeader.findViewById(R.id.textView);
        tvHeader.setTextColor(ContextCompat.getColor(mContext, lightTextColor.resourceId));
        //更新侧滑菜单背景
        navigationView.setBackgroundResource(colorBackground.resourceId);
        //更新侧滑菜单字体颜色
        navigationView.setItemTextColor(ContextCompat.getColorStateList(mContext, darkTextColor.resourceId));
        navigationView.setItemIconTintList(ContextCompat.getColorStateList(mContext, darkTextColor.resourceId));
        //更新ViewPagerUI
        mViewPager.refreshUI();

        refreshStatusBar();
    }

    /**
     * 刷新StatusBar
     */
    private void refreshStatusBar() {
        if (Build.VERSION.SDK_INT >= 21) {
            TypedValue typedValue = new TypedValue();
            Resources.Theme theme = getTheme();
            theme.resolveAttribute(R.attr.colorPrimaryDark, typedValue, true);
            getWindow().setStatusBarColor(ContextCompat.getColor(mContext, typedValue.resourceId));
        }
    }

    /**
     * 刷新Toolbar图标
     */
    private void refreshToolbarIcon() {
        Drawable navigationIcon = mToolbar.getNavigationIcon();
        if (navigationIcon != null) {
            if (mDayNightHelper.isDay()) {
                navigationIcon.setAlpha(255);
            } else {
                navigationIcon.setAlpha(128);
            }
        }
        Menu toolbarMenu = mToolbar.getMenu();
        Drawable searchIcon = toolbarMenu.getItem(0).getIcon();
        if (searchIcon != null) {
            if (mDayNightHelper.isDay()) {
                searchIcon.setAlpha(255);
            } else {
                searchIcon.setAlpha(128);
            }
        }
    }

    private long mExitTime;

    @Override
    public void onBackPressed() {
        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (searchView.isSearchOpen()) {
            searchView.closeSearch();
        } else if (System.currentTimeMillis() - mExitTime > 2000) {
            ToastUtil.show(this, "再按一次退出");
            mExitTime = System.currentTimeMillis();
        } else {
            ToastUtil.cancel();
            finish();
        }
    }

    private void initViewPager(String title, String firstType) {
        List<TabDTO> tabList = mRealm.where(TabDTO.class)
                .equalTo("firstType", firstType)
                .findAll();
        if (tabList != null && tabList.size() > 0) {
            mViewPager = new MainViewPager(this, title, tabList);
            mViewPager.initData();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main_toolbar, menu);
        //设置搜索框
        MenuItem item = menu.findItem(R.id.action_search);
        searchView.setMenuItem(item);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        int id = item.getItemId();
//        if (id == R.id.action_search) {
//            return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_person:
                initViewPager("人物", TYPE_PERSON);
                drawer.closeDrawer(GravityCompat.START);
                break;
            case R.id.nav_house:
                initViewPager("家族", TYPE_HOUSE);
                drawer.closeDrawer(GravityCompat.START);
                break;
            case R.id.nav_history:
                initViewPager("历史", TYPE_HISTORY);
                drawer.closeDrawer(GravityCompat.START);
                break;
            case R.id.nav_castles:
                initViewPager("城堡", TYPE_CASTLE);
                drawer.closeDrawer(GravityCompat.START);
                break;
            case R.id.nav_night:
                boolean isChecked = switchCompat.isChecked();
                if (isChecked) {
                    switchCompat.setChecked(false);
                } else {
                    switchCompat.setChecked(true);
                }
                break;
        }
        return true;
    }
}
