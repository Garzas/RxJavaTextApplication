package com.example.admin.rxjavatestapplication.dao;

import com.appunite.rx.ResponseOrError;
import com.appunite.rx.android.MyAndroidSchedulers;
import com.appunite.rx.operators.MoreOperators;
import com.appunite.rx.operators.OperatorMergeNextToken;
import com.example.admin.rxjavatestapplication.MyRetroFit;
import com.example.admin.rxjavatestapplication.model.Item;
import com.example.admin.rxjavatestapplication.model.SpotifyResponse;
import com.example.admin.rxjavatestapplication.model.Tracks;
import com.example.admin.rxjavatestapplication.schedulers.ObserveOnScheduler;
import com.example.admin.rxjavatestapplication.schedulers.SubscribeOnScheduler;
import com.google.common.collect.ImmutableList;

import java.util.List;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.PublishSubject;

public class SpotifyResponseDao {

    @Nonnull
    private final Observable<ResponseOrError<SpotifyResponse>> spotifyResponseObservable;
    @Nonnull
    private final PublishSubject<Object> loadMoreSubject = PublishSubject.create();
    @Nonnull
    private final MyRetroFit myRetroFit;
    @Nonnull
    private final Scheduler observeOnScheduler;
    @Nonnull
    private final Scheduler subscribeOnScheduler;

    Integer offset = 0;

    @Inject
    public SpotifyResponseDao(@Nonnull MyRetroFit retroFit,
                              @ObserveOnScheduler final Scheduler observeOnScheduler,
                              @SubscribeOnScheduler final Scheduler subscribeOnScheduler) {
        myRetroFit = retroFit;
        this.observeOnScheduler = observeOnScheduler;
        this.subscribeOnScheduler = subscribeOnScheduler;

        final OperatorMergeNextToken<SpotifyResponse, Object> mergeSpotifyResponseNextToken = OperatorMergeNextToken
                .create(new Func1<SpotifyResponse, Observable<SpotifyResponse>>() {
                            @Override
                            public Observable<SpotifyResponse> call(SpotifyResponse spotifyResponse) {
                                if (spotifyResponse == null) {

                                    return myRetroFit.listTracks(offset);
//                                            .subscribeOn(subscribeOnScheduler)
//                                            .observeOn(observeOnScheduler);
                                } else if (spotifyResponse.getTracks().getTotal() < offset) {
                                        return Observable.never();
                                    }
                                else {
                                    offset += 50;
                                    final Observable<SpotifyResponse> apiRequest = myRetroFit
                                            .listTracks(offset);
//                                            .subscribeOn(subscribeOnScheduler)
//                                            .observeOn(observeOnScheduler);
                                    return Observable.just(spotifyResponse).zipWith(apiRequest,
                                            new MergeTwoResponses());
                                }
                            }
                        }
                );

        spotifyResponseObservable = loadMoreSubject.startWith((Object) null)
                .lift(mergeSpotifyResponseNextToken)
                .compose(ResponseOrError.<SpotifyResponse>toResponseOrErrorObservable())
//                compose ponizej wywalone na potrzeby testow(błędy z MyAndroidSchedulels.NETWORK_SCHEDULER)
                .compose(MoreOperators.<SpotifyResponse>repeatOnError(MyAndroidSchedulers.NETWORK_SCHEDULER))
                .subscribeOn(subscribeOnScheduler)
                .observeOn(observeOnScheduler);
    }

    @Nonnull
    public Observable<ResponseOrError<SpotifyResponse>> spotifyItemsObservable() {
        return spotifyResponseObservable;
    }

    @Nonnull
    public Observable<ResponseOrError<SpotifyResponse>> clickedItemObservable(String offset) {
        this.offset = Integer.parseInt(offset);
        return spotifyResponseObservable;
    }

    public Observer<Object> loadMoreObserver() {
        return loadMoreSubject;
    }

    private static class MergeTwoResponses implements Func2<SpotifyResponse, SpotifyResponse, SpotifyResponse> {
        @Override
        public SpotifyResponse call(SpotifyResponse previous, SpotifyResponse moreData) {


                final List<Item> items = ImmutableList.<Item>builder()
                        .addAll(previous.getTracks().getItems())
                        .addAll(moreData.getTracks().getItems())
                        .build();
                return new SpotifyResponse(new Tracks(items, moreData.getTracks().getOffset(), moreData.getTracks().getTotal()));

        }
    }
}