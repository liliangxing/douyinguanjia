package me.douyin.guanjia.adapter;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import me.douyin.guanjia.model.Music;
import me.douyin.guanjia.utils.Utils;
import me.douyin.guanjia.utils.binding.Bind;
import me.douyin.guanjia.utils.binding.ViewBinder;
import me.douyin.guanjia.R;

/**
 * 搜索结果适配器
 * Created by hzwangchenyan on 2016/1/13.
 */
public class SearchMusicAdapter extends BaseAdapter {
    private List<Music> mData;
    private OnMoreClickListener mListener;

    public SearchMusicAdapter(List<Music> data) {
        mData = data;
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public Object getItem(int position) {
        return mData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        Music music =  mData.get(position);
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_holder_music, parent, false);
            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        Utils.getBitmapUtils().display(holder.ivCover,music.getCoverPath());
        holder.tvTitle.setText(music.getTitle());
        holder.tvArtist.setText(music.getPath());
        if(!TextUtils.isEmpty(music.getAlbum())){
            holder.tvArtist.setText(music.getAlbum()+" "+
                    music.getPath());
        }
        holder.ivMore.setOnClickListener(v -> mListener.onMoreClick(position));
        holder.vDivider.setVisibility(isShowDivider(position) ? View.VISIBLE : View.GONE);
        return convertView;
    }

    private boolean isShowDivider(int position) {
        return position != mData.size() - 1;
    }

    public void setOnMoreClickListener(OnMoreClickListener listener) {
        mListener = listener;
    }

    private static class ViewHolder {
        @Bind(R.id.iv_cover)
        private ImageView ivCover;
        @Bind(R.id.tv_title)
        private TextView tvTitle;
        @Bind(R.id.tv_artist)
        private TextView tvArtist;
        @Bind(R.id.iv_more)
        private ImageView ivMore;
        @Bind(R.id.v_divider)
        private View vDivider;

        public ViewHolder(View view) {
            ViewBinder.bind(this, view);
            //ivCover.setVisibility(View.GONE);
        }
    }
}
