package me.zhennan.android.easyui.library.list;

import android.content.Context;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import me.zhennan.android.easyui.library.R;

/**
 * Created by larry.zzn@gmail.com on 15/12/10.
 *
 */
public class EasyList extends RelativeLayout {

    private DataProvider dataProviderCache = null;
    private ViewDecorator viewDecoratorCache = null;

    private SwipeRefreshLayout refreshLayout = null;
    private ViewGroup emptyBox = null;

    private RecyclerView listView = null;
    private Adapter listAdapter = null;

    private Adapter getListAdapter(){
        if(null == listAdapter){
            listAdapter = new Adapter();
        }
        return listAdapter;
    }

    private RecyclerView.LayoutManager getLayoutManager(){
        RecyclerView.LayoutManager result = null;
        if(null != getViewDecorator()){
            result = getViewDecorator().getLayoutManager();
        }
        // default
        if(null == result){
            result = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        }

        return result;
    }

    public EasyList(Context context, AttributeSet attrs) {
        super(context, attrs);

        // setup layout
        LayoutInflater.from(context).inflate(R.layout.easyui_easylist_layout, this, true);
        refreshLayout = (SwipeRefreshLayout)findViewById(R.id.easyui_box_refresh);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                DataProvider provider = getDataProvider();
                if(null == provider){
                    throw  new IllegalArgumentException("ListFragment.getDataProvider must not be null");
                }

                provider.onLoadFirstPage();
            }
        });

        emptyBox = (ViewGroup)findViewById(R.id.easyui_box_empty);

        listView = (RecyclerView)findViewById(R.id.easyui_list_view);
        listView.setLayoutManager(getLayoutManager());
        listView.setAdapter(getListAdapter());
    }


    public DataProvider getDataProvider(){
        return dataProviderCache;
    }

    public void setDataProvider(DataProvider provider){
        dataProviderCache = provider;
    }

    public ViewDecorator getViewDecorator(){
        return viewDecoratorCache;
    }

    public void setViewDecorator(ViewDecorator decorator){
        viewDecoratorCache = decorator;
    }

    /**
     * notify the list data was changed. so refresh the list.
     */
    public void notifyDataSetChanged(){
        if(refreshLayout.isRefreshing()){
            refreshLayout.setRefreshing(false);
        }

        if( 0 == listAdapter.getItemCount()){
            showEmptyView();
        }else{
            hideEmptyView();
        }

        listAdapter.notifyDataSetChanged();
    }



    boolean isEmptyViewCreated = false;

    /**
     * invalidate the empty view's presentation.
     */
    public void invalidateEmptyView(){
        isEmptyViewCreated = false;
        emptyBox.removeAllViews();

        // if the empty view was displayed, it will refresh it.
        if(0 == listAdapter.getItemCount()){
            showEmptyView();
        }
    }

    /**
     * invalidate layout manager if you don't like the DEFAULT_LAYOUT_MANAGER(LinearLayoutManager)
     */
    public void invalidateLayoutManager(){
        listView.setLayoutManager(getLayoutManager());
    }

    private void showEmptyView(){
        ViewDecorator decorator = getViewDecorator();
        if(null == decorator){
            throw  new IllegalArgumentException("ListFragment.getViewDecorator must not be null");
        }

        if(!isEmptyViewCreated){
            View emptyView = decorator.onCreateEmptyView(emptyBox);
            if(null != emptyView){
                emptyBox.addView(emptyView);
                isEmptyViewCreated = true;
            }
        }

        emptyBox.setVisibility(View.VISIBLE);
        refreshLayout.setVisibility(View.INVISIBLE);
    }

    private void hideEmptyView(){
        emptyBox.setVisibility(View.INVISIBLE);
        refreshLayout.setVisibility(View.VISIBLE);
    }

    static final int LOAD_MORE = Integer.MIN_VALUE;
    class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        @Override
        public int getItemCount() {
            DataProvider provider = getDataProvider();
            if(null == provider){
                throw  new IllegalArgumentException("ListFragment.getDataProvider must not be null");
            }
            int count;
            if(provider.isLastPage()) {
                count = provider.getCount();
            }else{
                count = provider.getCount() + 1;
            }
            return count;
        }

        @Override
        public int getItemViewType(int position) {
            DataProvider provider = getDataProvider();
            if(null == provider){
                throw  new IllegalArgumentException("ListFragment.getDataProvider must not be null");
            }else{

                boolean isLastPage = provider.isLastPage();
                int count = getItemCount();

                if(!isLastPage && position == count - 1){
                    return LOAD_MORE;
                }else{
                    return provider.getItemViewType(position);
                }
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            DataProvider provider = getDataProvider();
            if(null == provider){
                throw new IllegalArgumentException("ListFragment.getDataProvider must not be null");
            }

            ViewDecorator decorator = getViewDecorator();
            if(null == decorator){
                throw new IllegalArgumentException("ListFragment.getViewDecorator must not be null");
            }

            switch (viewType){
                case LOAD_MORE:
                    return new LoadMoreViewHolder(decorator.onCreateLoadMoreView(parent));
                default:
                    return provider.onCreateViewHolder(parent, viewType);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            DataProvider provider = getDataProvider();
            if(null == provider){
                throw new IllegalArgumentException("ListFragment.getDataProvider must not be null");
            }

            if(holder instanceof LoadMoreViewHolder){
                provider.onLoadNextPage();
            }else {
                provider.onBindViewHolder(holder, position);
            }
        }
    }

    class LoadMoreViewHolder extends RecyclerView.ViewHolder{
        public LoadMoreViewHolder(View itemView) {
            super(itemView);
        }
    }

    public interface ViewDecorator{
        RecyclerView.LayoutManager getLayoutManager();

        // todo: implement this in next version
//        RecyclerView.ItemDecoration getListItemDecoration();
//        RecyclerView.ItemDecoration getListItemDecoration(int position);


        View onCreateEmptyView(ViewGroup parent);
        View onCreateLoadMoreView(ViewGroup parent);
    }

    public interface DataProvider{
        int getCount();

        int getItemViewType(int position);
        RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type);
        void onBindViewHolder(RecyclerView.ViewHolder holder, int position);

        boolean isLastPage();

        void onLoadFirstPage();
        void onLoadNextPage();
    }


}
