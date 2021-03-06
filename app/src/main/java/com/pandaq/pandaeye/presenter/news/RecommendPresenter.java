package com.pandaq.pandaeye.presenter.news;

import com.pandaq.pandaeye.CustomApplication;
import com.pandaq.pandaeye.config.Constants;
import com.pandaq.pandaeye.disklrucache.DiskCacheManager;
import com.pandaq.pandaeye.model.api.ApiManager;
import com.pandaq.pandaeye.model.neteasynews.NewsBean;
import com.pandaq.pandaeye.model.neteasynews.RecommendNewsList;
import com.pandaq.pandaeye.model.neteasynews.TopNewsList;
import com.pandaq.pandaeye.presenter.BasePresenter;
import com.pandaq.pandaeye.ui.ImplView.INewsListFrag;
import com.pandaq.pandaqlib.magicrecyclerView.BaseItem;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;


/**
 * Created by PandaQ on 2016/9/22.
 * email : 767807368@qq.com
 */

public class RecommendPresenter extends BasePresenter {

    private INewsListFrag mNewsListFrag;
    private int currentIndex;

    public RecommendPresenter(INewsListFrag newsListFrag) {
        this.mNewsListFrag = newsListFrag;
    }

    public void refreshNews() {
        mNewsListFrag.showRefreshBar();
        currentIndex = 0;
        ApiManager.getInstence().getTopNewsServie()
                .getRecommendNews(currentIndex + "")
                .map(new Function<RecommendNewsList, ArrayList<NewsBean>>() {
                    @Override
                    public ArrayList<NewsBean> apply(RecommendNewsList recommendNewsList) {
                        return recommendNewsList.getRecNewsArrayList();
                    }
                })
                .flatMap(new Function<ArrayList<NewsBean>, Observable<NewsBean>>() {
                    @Override
                    public Observable<NewsBean> apply(ArrayList<NewsBean> topNewses) throws Exception {
                        return Observable.fromIterable(topNewses);
                    }
                })
                .filter(new Predicate<NewsBean>() {
                    @Override
                    public boolean test(NewsBean topNews) throws Exception {
                        return topNews.getUrl() != null;
                    }
                })
                .map(new Function<NewsBean, BaseItem>() {
                    @Override
                    public BaseItem apply(NewsBean topNews) {
                        BaseItem<NewsBean> baseItem = new BaseItem<>();
                        baseItem.setData(topNews);
                        return baseItem;
                    }
                })
                .toList()
                //将 List 转为ArrayList 缓存存储 ArrayList Serializable对象
                .map(new Function<List<BaseItem>, ArrayList<BaseItem>>() {
                    @Override
                    public ArrayList<BaseItem> apply(List<BaseItem> baseItems) {
                        ArrayList<BaseItem> items = new ArrayList<>();
                        items.addAll(baseItems);
                        return items;
                    }
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<ArrayList<BaseItem>>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onSuccess(ArrayList<BaseItem> value) {
                        DiskCacheManager manager = new DiskCacheManager(CustomApplication.getContext(), Constants.CACHE_NEWS_FILE);
                        manager.put(Constants.CACHE_RECOMMEND_NEWS, value);
                        currentIndex += 20;
                        mNewsListFrag.hideRefreshBar();
                        mNewsListFrag.refreshNewsSuccessed(value);
                    }

                    @Override
                    public void onError(Throwable e) {
                        mNewsListFrag.hideRefreshBar();
                        mNewsListFrag.refreshNewsFail(e.getMessage());
                    }

                });
    }

    //两个方法没区别,只是刷新会重新赋值
    public void loadMore() {
        ApiManager.getInstence().getTopNewsServie()
                .getRecommendNews(currentIndex + "")
                .map(new Function<RecommendNewsList, ArrayList<NewsBean>>() {
                    @Override
                    public ArrayList<NewsBean> apply(RecommendNewsList recommendNewsList) {
                        return recommendNewsList.getRecNewsArrayList();
                    }
                })
                .flatMap(new Function<ArrayList<NewsBean>, Observable<NewsBean>>() {
                    @Override
                    public Observable<NewsBean> apply(ArrayList<NewsBean> topNewses) {
                        return Observable.fromIterable(topNewses);
                    }
                })
                .filter(new Predicate<NewsBean>() {
                    @Override
                    public boolean test(NewsBean topNews) {
                        return topNews.getUrl() != null;
                    }
                })
                .map(new Function<NewsBean, BaseItem>() {
                    @Override
                    public BaseItem apply(NewsBean topNews) {
                        BaseItem<NewsBean> baseItem = new BaseItem<>();
                        baseItem.setData(topNews);
                        return baseItem;
                    }
                })
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<BaseItem>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        addDisposable(d);
                    }

                    @Override
                    public void onSuccess(List<BaseItem> value) {
                        if (value != null && value.size() > 0) {
                            //每刷新成功一次多加载20条
                            currentIndex += 20;
                            mNewsListFrag.loadMoreSuccessed((ArrayList<BaseItem>) value);
                        } else {
                            mNewsListFrag.loadAll();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        mNewsListFrag.loadMoreFail(e.getMessage());
                    }

                });

    }

    /**
     * 读取缓存
     */
    public void loadCache() {
        DiskCacheManager manager = new DiskCacheManager(CustomApplication.getContext(), Constants.CACHE_NEWS_FILE);
        ArrayList<BaseItem> topNews = manager.getSerializable(Constants.CACHE_RECOMMEND_NEWS);
        if (topNews != null) {
            mNewsListFrag.refreshNewsSuccessed(topNews);
        }
    }
}
