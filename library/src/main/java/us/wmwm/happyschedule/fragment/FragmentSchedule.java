package us.wmwm.happyschedule.fragment;

import java.util.Calendar;
import java.util.Date;

import org.json.JSONObject;

import us.wmwm.happyschedule.R;
import us.wmwm.happyschedule.ThreadHelper;
import us.wmwm.happyschedule.dao.WDb;
import us.wmwm.happyschedule.fragment.FragmentHappyAd.DiscardListener;
import us.wmwm.happyschedule.fragment.FragmentPickStations.OnGetSchedule;
import us.wmwm.happyschedule.model.Alarm;
import us.wmwm.happyschedule.model.AppAd;
import us.wmwm.happyschedule.model.AppConfig;
import us.wmwm.happyschedule.model.DepartureVision;
import us.wmwm.happyschedule.model.Schedule;
import us.wmwm.happyschedule.model.Station;
import us.wmwm.happyschedule.model.StationToStation;
import us.wmwm.happyschedule.util.PremiumUserHelper;
import us.wmwm.happyschedule.util.Streams;
import us.wmwm.happyschedule.views.BackListener;
import us.wmwm.happyschedule.views.ScheduleControlsView.ScheduleControlListener;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.flurry.android.FlurryAgent;
import com.squareup.timessquare.CalendarPickerView.OnDateSelectedListener;

public class FragmentSchedule extends Fragment implements BackListener {

    private static final String TAG = FragmentSchedule.class.getSimpleName();
    ViewPager pager;
    View loadingContainer;
    Station from;
    Station to;
    ScheduleControlListener controlListener = new ScheduleControlListener() {

        @Override
        public void onTrips(Schedule schedule, StationToStation stationToStation) {
            FragmentTrip t = FragmentTrip.newInstance(from, to,
                    stationToStation, schedule);
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_date_picker, t).addToBackStack(null)
                    .setBreadCrumbTitle(from.getName() + " to " + to.getName())
                    .commit();
        }

        @Override
        public void onTimerCancel(Alarm alarm) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onTimer(StationToStation sts) {

        }

        @Override
        public void onPin(StationToStation sts) {
        }

        @Override
        public void onReverse() {
            onGetSchedule.onGetSchedule(adapter.getCalendar(pager.getCurrentItem()), to, from);
        }

        @Override
        public void onDay() {
            final FragmentDatePicker picker = FragmentDatePicker
                    .newInstance(adapter.getCalendar(pager.getCurrentItem())
                            .getTime());
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            picker.setOnDateSelectedListener(new OnDateSelectedListener() {

                @Override
                public void onDateSelected(Date date) {
                    getFragmentManager().popBackStack();
                    // FragmentTransaction ft = getFragmentManager()
                    // .beginTransaction();
                    // ft.remove(picker);
                    // ft.commit();
                    pager.setCurrentItem(adapter.getPositionFor(date));
                }
            });
            ft.replace(R.id.fragment_date_picker, picker);
            ft.addToBackStack(null);
            ft.commit();
        }

        @Override
        public void onFavorite() {
            DepartureVision dv = new DepartureVision(from.getId(), to.getId());
            if(FavoriteHelper.hasFavorite(dv)) {
                FavoriteHelper.remove(dv);
            } else {
                FavoriteHelper.add(dv);
            }
        }

        public void onShare(Schedule schedule, StationToStation stationToStation) {
        }

        ;
    };
    Handler handler = new Handler();
    FragmentScheduleAdapter adapter;
    int adId;
    OnDepartureVision onDepartureVision = new OnDepartureVision() {

        @Override
        public void onDepartureVision(Station station, Station arrival) {
            try {
                getFragmentManager()
                        .beginTransaction()
                        .replace(
                                R.id.fragment_date_picker,
                                FragmentDepartureVision.newInstance(new DepartureVision(station.getId(),
                                        arrival == null ? null : arrival.getId()), 0, arrival, null, true))
                        .addToBackStack(null)
                        .setBreadCrumbTitle(
                                "DepartureVision @ " + station.getName())
                        .commit();
            } catch (Exception e) {

            }
        }
    };
    OnGetSchedule onGetSchedule;
    private long started;

    public static FragmentSchedule newInstance(Calendar day, Station from, Station to, boolean showAds) {
        Bundle b = new Bundle();
        b.putSerializable("from", from);
        b.putSerializable("to", to);
        b.putSerializable("day", day);
        b.putBoolean("showAds",showAds);
        FragmentSchedule s = new FragmentSchedule();
        s.setArguments(b);
        return s;
    }

    public static FragmentSchedule newInstance(Calendar day, Station from, Station to) {
        return newInstance(day,from,to,false);
    }

    public void setOnGetSchedule(OnGetSchedule onGetSchedule) {
        this.onGetSchedule = onGetSchedule;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule, container,
                false);
        pager = (ViewPager) view.findViewById(R.id.pager2);
        setHasOptionsMenu(true);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        adId = R.id.ad;
        Bundle b = getArguments();
        from = (Station) b.getSerializable("from");
        to = (Station) b.getSerializable("to");

        pager.setAdapter(adapter = new FragmentScheduleAdapter(from,
                to, getFragmentManager()).setControlListener(controlListener));
        adapter.setOnDepartureVision(onDepartureVision);
        adapter.setOnGetSchedule(onGetSchedule);
        pager.setCurrentItem(adapter.getTodaysPosition());

        ThreadHelper.getScheduler().submit(new Runnable() {
            @Override
            public void run() {
                try {
                    AppConfig config = AppConfig.get();
                    final AppAd ad = config.getBestAd(getActivity(),
                            FragmentSchedule.class);
                    if (ad != null) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                final FragmentHappyAd fad = FragmentHappyAd
                                        .newIntance(ad);
                                fad.setDiscardListener(new DiscardListener() {
                                    @Override
                                    public void onDiscard(AppAd ad) {
                                        getFragmentManager().beginTransaction()
                                                .remove(fad).commit();
                                        WDb.get()
                                                .savePreference(
                                                        "discard_"
                                                                + ad.getDiscardKey(),
                                                        System.currentTimeMillis()
                                                                + "");
                                    }
                                });
                                getFragmentManager().beginTransaction()
                                        .replace(R.id.top_ad, fad).commit();
                            }
                        });
                    }
                } catch (Exception e) {

                }
            }
        });

        if(b.getBoolean("showAds")) {
            getChildFragmentManager().beginTransaction().replace(R.id.ad, new FragmentHappytapAd()).commit();
        }

    }

    @Override
    public boolean onBack() {
        if (adapter.getTodaysPosition() != pager.getCurrentItem()) {
            pager.setCurrentItem(adapter.getTodaysPosition());
            return true;
        }
        return false;
    }
}
