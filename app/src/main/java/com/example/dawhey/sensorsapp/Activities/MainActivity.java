package com.example.dawhey.sensorsapp.Activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.example.dawhey.sensorsapp.Api.SensorsApiClient;
import com.example.dawhey.sensorsapp.Api.ServiceGenerator;
import com.example.dawhey.sensorsapp.Application;
import com.example.dawhey.sensorsapp.Models.Entries;
import com.example.dawhey.sensorsapp.R;
import com.example.dawhey.sensorsapp.Utilities.EntriesEvent;
import com.example.dawhey.sensorsapp.fragments.DataChartFragment;
import com.example.dawhey.sensorsapp.fragments.BrowseEntriesFragment;
import com.google.firebase.messaging.FirebaseMessaging;

import org.greenrobot.eventbus.EventBus;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TOPIC_NAME = "warnings";

    public SwipeRefreshLayout swipeRefreshLayout;
    private FragmentManager fragmentManager;
    private Entries entries;
    private int visibleFragmentId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseMessaging.getInstance().subscribeToTopic(TOPIC_NAME);
        Log.d("FCM", "Subscribed to warnings topic");

        initActivity();
        fragmentManager = getSupportFragmentManager();
        Fragment fragment;

        Application application = (Application) getApplication();

        visibleFragmentId = application.getFragmentId();
        switch (application.getFragmentId()) {
            case R.id.last_entries:
                fragment = new BrowseEntriesFragment();
                swipeRefreshLayout.setEnabled(true);

                break;
            case R.id.data_chart:
                fragment = new DataChartFragment();
                swipeRefreshLayout.setEnabled(false);

                break;
            default:
                fragment = new BrowseEntriesFragment();
                swipeRefreshLayout.setEnabled(true);
        }

        fragmentManager.beginTransaction().replace(R.id.container,fragment).commit();
        downloadEntries();
    }

    private void initActivity() {
        setContentView(R.layout.activity_main);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setColorSchemeColors(getResources().getColor(R.color.colorAccent));
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                downloadEntries();
            }
        });
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

    }

    public void downloadEntries() {
        if (visibleFragmentId != R.id.data_chart) {
            swipeRefreshLayout.setRefreshing(true);
        }
        SensorsApiClient service = ServiceGenerator.createService(SensorsApiClient.class);
        Call<Entries> entriesCall = service.getEntries();
        entriesCall.enqueue(new Callback<Entries>() {
            @Override
            public void onResponse(Call<Entries> call, Response<Entries> response) {
                if (response.isSuccessful()) {
                    entries = response.body();
                    EventBus.getDefault().post(new EntriesEvent(entries));
                } else {
                    Log.e(String.valueOf(response.code()), response.message());
                }
                swipeRefreshLayout.setRefreshing(false);
            }

            @Override
            public void onFailure(Call<Entries> call, Throwable t) {
                Log.e("Error", t.toString());
                Toast.makeText(getApplicationContext(), t.toString(), Toast.LENGTH_LONG).show();
                swipeRefreshLayout.setRefreshing(false);
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.side_nav_bar
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_refresh) {
            downloadEntries();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {

        Fragment fragment = null;
        Class fragmentClass;
        Application application = (Application) getApplication();

        // Handle navigation view item clicks here.
        int id = item.getItemId();
        application.setFragmentId(id);

        if (id == R.id.last_entries) {
            fragmentClass = BrowseEntriesFragment.class;
            swipeRefreshLayout.setEnabled(true);

        } else if (id == R.id.data_chart) {
            fragmentClass = DataChartFragment.class;
            swipeRefreshLayout.setEnabled(false);
        }  else {
            fragmentClass = BrowseEntriesFragment.class;
        }

        try {
            fragment = (Fragment) fragmentClass.newInstance();
            Bundle bundle = new Bundle();
            bundle.putSerializable("entries", entries);
            fragment.setArguments(bundle);
        } catch (Exception e) {
            e.printStackTrace();
        }

        fragmentManager.beginTransaction().replace(R.id.container, fragment).commit();
        setTitle(item.getTitle());
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
