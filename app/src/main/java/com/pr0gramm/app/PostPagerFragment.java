package com.pr0gramm.app;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.inject.Inject;
import com.pr0gramm.app.feed.FeedItem;
import com.pr0gramm.app.feed.FeedProxy;
import com.pr0gramm.app.feed.FeedService;

import roboguice.fragment.RoboFragment;
import roboguice.inject.InjectView;

/**
 */
public class PostPagerFragment extends RoboFragment {
    private static final String ARG_FEED_PROXY = "PostPagerFragment.feedProxy";
    private static final String ARG_START_ITEM = "PostPagerFragment.startItem";

    @Inject
    private FeedService feedService;

    @InjectView(R.id.pager)
    private ViewPager viewPager;

    private FeedProxy proxy;
    private PostAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_post_pager, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // use instance state if available.
        Bundle state = savedInstanceState != null
                ? savedInstanceState
                : getArguments();

        // re-create the proxy for the stream
        proxy = FeedProxy.fromBundle(state.getBundle(ARG_FEED_PROXY));

        // bind the loader to this fragment.
        proxy.setLoader(new FeedProxy.FragmentFeedLoader(this, feedService));

        // set the adapter
        adapter = new PostAdapter();
        viewPager.setAdapter(adapter);

        // calculate index of the first item to show and show that one.
        FeedItem start = state.getParcelable(ARG_START_ITEM);
        int index = proxy.getPosition(start).or(0);
        viewPager.setCurrentItem(index);
        Log.i("PostPager", "Starting at index: " + index);

        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            viewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
                @Override
                public void onPageSelected(int position) {
                    mainActivity.onScrollHideToolbarListener.reset();
                }
            });

            // reset the scrollbar here too
            mainActivity.onScrollHideToolbarListener.reset();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        Log.i("PostPager", "Saving state before stopping");
        int idx = viewPager.getCurrentItem();
        outState.putBundle(ARG_FEED_PROXY, proxy.toBundle(idx));
        outState.putParcelable(ARG_START_ITEM, proxy.getItemAt(idx));
    }

    private class PostAdapter extends MyFragmentStatePagerAdapter implements FeedProxy.OnChangeListener {
        public PostAdapter() {
            super(getChildFragmentManager());
            proxy.setOnChangeListener(this);
        }

        @Override
        public Fragment getItem(int position) {
            if (!proxy.isLoading()) {
                if (position > proxy.getItemCount() - 8) {
                    Log.i("PostPager", "requested pos=" + position + ", load next page");
                    proxy.loadNextPage();
                }

                if (position < 8) {
                    Log.i("PostPager", "requested pos=" + position + ", load prev page");
                    proxy.loadPreviousPage();
                }
            }

            return PostFragment.newInstance(proxy.getItemAt(position));
        }

        @Override
        public int getCount() {
            return proxy.getItemCount();
        }

        @Override
        public void onItemRangeInserted(int start, int count) {
            Log.i("PostPager", "Insert new posts at " + start);
            notifyDataSetChanged();
        }

        @Override
        public int getItemPosition(Object object) {
            FeedItem item = ((PostFragment) object).getFeedItem();
            return proxy.getPosition(item).or(PagerAdapter.POSITION_NONE);
        }

        @Override
        protected long getItemId(int position) {
            return proxy.getItemAt(position).getId();
        }

        @Override
        public void onItemRangeRemoved(int start, int count) {
            // should not happen
            throw new UnsupportedOperationException();
        }
    }

    public static PostPagerFragment newInstance(FeedProxy proxy, int idx) {
        Bundle arguments = new Bundle();
        arguments.putBundle(ARG_FEED_PROXY, proxy.toBundle(idx));
        arguments.putParcelable(ARG_START_ITEM, proxy.getItemAt(idx));

        PostPagerFragment fragment = new PostPagerFragment();
        fragment.setArguments(arguments);
        return fragment;
    }
}