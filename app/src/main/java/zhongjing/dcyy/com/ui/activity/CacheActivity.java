package zhongjing.dcyy.com.ui.activity;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.baoyz.swipemenulistview.SwipeMenu;
import com.baoyz.swipemenulistview.SwipeMenuCreator;
import com.baoyz.swipemenulistview.SwipeMenuItem;
import com.baoyz.swipemenulistview.SwipeMenuListView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import zhongjing.dcyy.com.R;
import zhongjing.dcyy.com.bean.CacheVideoInfo;
import zhongjing.dcyy.com.utils.DateUtils;
import zhongjing.dcyy.com.utils.FileUtils;

public class CacheActivity extends AppCompatActivity {
    public static final String CACHE_VIDEO_URL = "cache_video_url";
    private List<CacheVideoInfo> videoInfos = new ArrayList<>();

    private SwipeMenuListView listView;
    private ListViewAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cache);
        initData();
        initView();
        initEvent();
        initListener();
    }

    private static final String SAVE_PIC_PATH= Environment.getExternalStorageState().equalsIgnoreCase(Environment.MEDIA_MOUNTED) ? Environment.getExternalStorageDirectory().getAbsolutePath() : "/mnt/sdcard";//保存到SD卡
    public static final String SAVE_REAL_PATH = SAVE_PIC_PATH+ "/zhongjingVideo";//保存的确切位置
    private void initData() {
        File dir = new File(SAVE_REAL_PATH);
        File[] files = dir.listFiles();

        if(files==null){
            return;
        }
        for (File file : files) {
            CacheVideoInfo videoInfo = new CacheVideoInfo();

            /*MediaMetadataRetriever mmr=new MediaMetadataRetriever();
            mmr.setDataSource(file.getPath());
            //获取第一帧图像的bitmap对象
            Bitmap bitmap=mmr.getFrameAtTime();*/

            long fileSize = FileUtils.getFileSize(file);
            String filename = file.getName();
            String name = filename.substring(0,13);

            //videoInfo.videoBitmap = bitmap;
            videoInfo.videoSize = fileSize;
            videoInfo.videoName = name;

            videoInfos.add(videoInfo);
        }
    }

    private void initView() {
        listView = (SwipeMenuListView) findViewById(R.id.activity_cache_listView);
    }

    private void initEvent() {
        SwipeMenuCreator creator = new SwipeMenuCreator() {

            @Override
            public void create(SwipeMenu menu) {
                SwipeMenuItem deleteItem = new SwipeMenuItem(
                        getApplicationContext());
                deleteItem.setBackground(new ColorDrawable(Color.rgb(0xF9,
                        0x3F, 0x25)));
                deleteItem.setWidth(300);
                deleteItem.setTitle(R.string.delete);
                deleteItem.setTitleSize(18);
                deleteItem.setTitleColor(Color.WHITE);
                menu.addMenuItem(deleteItem);

            }
        };
        listView.setMenuCreator(creator);
        adapter = new ListViewAdapter();

        listView.setAdapter(adapter);

    }

    private void initListener() {
        listView.setOnMenuItemClickListener(new SwipeMenuListView.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(int position, SwipeMenu menu, int index) {
                switch (index){
                    case 0:        //删除键
                        CacheVideoInfo cacheVideoInfo = videoInfos.get(position);
                        File dir = new File(SAVE_REAL_PATH);
                        File deleteFile = new File(dir,cacheVideoInfo.videoName+".mp4");
                        deleteFile.delete();           //删除文件
                        videoInfos.remove(position);   //删除集合中的数据

                        adapter.notifyDataSetChanged();
                        break;
                }
                return false;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CacheVideoInfo cacheVideoInfo = videoInfos.get(position);
                Intent cacheVideoIntent = new Intent(CacheActivity.this,CacheVideoActivity.class);
                cacheVideoIntent.putExtra(CACHE_VIDEO_URL,cacheVideoInfo.videoName);
                startActivity(cacheVideoIntent);
            }
        });
    }

    public void click(View view){
        switch (view.getId()){
            case R.id.activity_cache_back:  //返回键
                finish();
                break;
        }
    }

    class ListViewAdapter extends BaseAdapter{

        @Override
        public int getCount() {
            if(videoInfos==null){
                return 0;
            }
            return videoInfos.size();
        }

        @Override
        public CacheVideoInfo getItem(int position) {
            return videoInfos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = View.inflate(CacheActivity.this, R.layout.item_cache_activity, null);
                holder = new ViewHolder();
                holder.videoPic = (ImageView) convertView
                        .findViewById(R.id.item_cache_activity_iv);
                holder.videoName = (TextView) convertView
                        .findViewById(R.id.item_cache_activity_tv_videoname);
                holder.videoSize = (TextView) convertView
                        .findViewById(R.id.item_cache_activity_tv_videosize);
                convertView.setTag(holder);

            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            CacheVideoInfo bean = videoInfos.get(position);

            //holder.videoPic.setImageBitmap(bean.videoBitmap);
            holder.videoName.setText(DateUtils.FormatDate(bean.videoName));
            holder.videoSize.setText(FileUtils.FormatFileSize(bean.videoSize));

            return convertView;
        }
        class ViewHolder {
            ImageView videoPic;
            TextView videoName;
            TextView videoSize;
        }
    }

}
