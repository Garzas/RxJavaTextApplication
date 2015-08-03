package com.example.admin.rxjavatestapplication;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;

import com.appunite.rx.android.MoreViewObservables;
import com.appunite.rx.functions.BothParams;
import com.example.admin.rxjavatestapplication.dao.SpotifyResponseDao;
import com.example.admin.rxjavatestapplication.helpers.LoadMoreHelper;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import butterknife.ButterKnife;
import butterknife.InjectView;
import dagger.Provides;
import rx.Observer;
import rx.functions.Action1;
import rx.functions.Func1;

public class MainActivity extends BaseActivity {

    @Inject
    MyListViewAdapter myListViewAdapter;

    @InjectView(R.id.listView)
    RecyclerView recyclerView;
//    @InjectView(R.id.progressBarMainActivity)
//    View progressView;
//    @InjectView(R.id.viewRefreshData)
//    View buttonView;
//    @InjectView(R.id.bRefreshData)
//    Button bRefreshData;

    private RetrofitPresenter presenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.inject(this);

        presenter = MainApplication
                .fromApplication(getApplication())
                .objectGraph()
                .plus(new Module())
                .get(RetrofitPresenter.class);

        MainApplication
                .fromApplication(getApplication())
                .objectGraph()
                .plus(new Module())
                .inject(this);

        final LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(myListViewAdapter);

        presenter.listObservable()
                .compose(lifecycleMainObservable.<ImmutableList<RetrofitPresenter.AdapterItem>>bindLifecycle())
                .subscribe(myListViewAdapter);

        presenter.openDetailsObservable()
                .compose(lifecycleMainObservable.<BothParams<RetrofitPresenter.AdapterItem, ImageView>>bindLifecycle())
                .subscribe(startDetailsActivityAction(this));

        MoreViewObservables.scroll(recyclerView)
                .filter(LoadMoreHelper.mapToNeedLoadMore(layoutManager, myListViewAdapter))
                .compose(lifecycleMainObservable.bindLifecycle())
                .subscribe(presenter.loadMoreObserver());

    }

    @Nonnull
    private static Action1<BothParams<RetrofitPresenter.AdapterItem, ImageView>> startDetailsActivityAction(final Activity activity) {
        return new Action1<BothParams<RetrofitPresenter.AdapterItem, ImageView>>() {
            @Override
            public void call(BothParams<RetrofitPresenter.AdapterItem, ImageView> bothParams) {
                //noinspection unchecked
                ActivityOptionsCompat options =  ActivityOptionsCompat.makeSceneTransitionAnimation(activity, bothParams.param2(), "profile");
                ActivityCompat.startActivity(activity,
                        DetailsActivity.getIntent(activity, bothParams.param1().getId(), bothParams.param1().getOffset()),
                        options.toBundle());

            }
        };
    }


    @dagger.Module(
            injects = {
                    RetrofitPresenter.class,
                    MainActivity.class

            },
            addsTo = MainApplication.Module.class
    )
    class Module {
    }
}
