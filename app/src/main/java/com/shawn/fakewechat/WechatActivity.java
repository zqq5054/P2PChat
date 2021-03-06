package com.shawn.fakewechat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.shawn.fakewechat.adapter.ContactAdapter;
import com.shawn.fakewechat.adapter.SimpleMenuAdapter;
import com.shawn.fakewechat.component.PopupMenuWindows;
import com.shawn.fakewechat.bean.ContactShowInfo;
import com.shawn.fakewechat.utils.HelpUtils;

/**
 * Created by fengshawn on 2017/8/1.
 */

public class WechatActivity extends AppCompatActivity {

    public static final int TYPE_USER = 0x11;
    public static final int TYPE_SERVICE = 0X12;
    public static final int TYPE_SUBSCRIBE = 0x13;
    private int toolbarHeight, statusBarHeight;
    private ListView lv;
    private Map<String,ContactShowInfo> contactShowInfoMap = new HashMap<>();
    private Handler mHandler = new Handler();
    private  ContactAdapter adapter;
    UserLineReceiver receiver = new UserLineReceiver();
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wechat_main);

        HelpUtils.transparentNav(this);

        Toolbar bar = findViewById(R.id.activity_wechat_toolbar);
        lv = findViewById(R.id.activity_wechat_lv);
        setSupportActionBar(bar);
        getSupportActionBar().setTitle("");
//        initData();

        bar.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        toolbarHeight = bar.getMeasuredHeight();
        statusBarHeight = HelpUtils.getStatusBarHeight(WechatActivity.this);
        findViewById(R.id.addChat).setOnClickListener(v->{
            Intent intent = new Intent(this,AddChatActivity.class);
            startActivity(intent);
        });
        IntentFilter filter = new IntentFilter();
        filter.addAction(getPackageName()+".online");
        filter.addAction(getPackageName()+".offline");
        filter.addAction(getPackageName()+".chat");
        this.registerReceiver(receiver,filter);
        adapter = new ContactAdapter(this, R.layout.item_wechat_main, new ArrayList<>());
        lv.setAdapter(adapter);
        addListViewClick();
    }
    private void addListViewClick(){


        lv.setOnTouchListener(new View.OnTouchListener() {
            int preX, preY;
            boolean isSlip = false, isLongClick = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        preX = (int) event.getX();
                        preY = (int) event.getY();
                        mHandler.postDelayed(() -> {
                            isLongClick = true;
                            int x = (int) event.getX();
                            int y = (int) event.getY();
                            //??????500ms?????????Y??????????????????Toolbar???statusBar??????
                            int position = lv.pointToPosition(x, y - toolbarHeight - statusBarHeight);
                            initPopupMenu(v, x, y, adapter, position, adapter.getContactInfos());

                        }, 500);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        int nowX = (int) event.getX();
                        int nowY = (int) event.getY();

                        int movedX = Math.abs(nowX - preX);
                        int movedY = Math.abs(nowY - preY);
                        if (movedX > 50 || movedY > 50) {
                            isSlip = true;
                            mHandler.removeCallbacksAndMessages(null);
                            //??????????????????
                        }
                        break;


                    case MotionEvent.ACTION_UP:
                        mHandler.removeCallbacksAndMessages(null);
                        if (!isSlip && !isLongClick) {
                            //??????????????????
                            int position = lv.pointToPosition(preX, preY);
                            if(position>=adapter.getCount()||position<0){
                                break;
                            }
                            String uuid = ((ContactShowInfo)adapter.getItem(position)).getUuid();
                            ContactShowInfo info = ((ContactShowInfo)adapter.getItem(position));
                            Intent intent = new Intent(WechatActivity.this, ChatActivity.class);
                            intent.putExtra("name", info.getUsername());
                            intent.putExtra("profileId", info.getHeadImage());
                            intent.putExtra("uuid",uuid);
                            startActivity(intent);
                        } else {
                            isSlip = false;
                            isLongClick = false;
                        }
                        break;
                }
                return false;
            }
        });

    }

    private void initData() {

        int[] headImgRes = {R.drawable.hdimg_3, R.drawable.group1, R.drawable.hdimg_2, R.drawable.user_2,
                R.drawable.user_3, R.drawable.user_4, R.drawable.user_5, R.drawable.hdimg_4,
                R.drawable.hdimg_5, R.drawable.hdimg_6};

        String[] usernames = {"Fiona", "  ...   ", "??????", "????????????", "????????????", "?????????????????????",
                "?????????Fiona", "?????????", "??????", "?????????"};
        //????????????
        String[] lastMsgs = {"?????????", "???????????????????????????", "?????????????????????", "???????????????????????????????????????2...",
                "??????????????????", "#????????????#???????????????????????????...", "??????:???????????????", "[Video Call]", "???????????????", "[????????????]"};

        String[] lastMsgTimes = {"17:40", "10:56", "7???26???", "??????", "7???27???", "09:46",
                "7???18???", "?????????", "7???26???", "4???23???"};

        int[] types = {TYPE_USER, TYPE_USER, TYPE_USER, TYPE_SUBSCRIBE, TYPE_SERVICE, TYPE_SUBSCRIBE,
                TYPE_USER, TYPE_USER, TYPE_USER, TYPE_USER};
        //??????&??????
        boolean[] isMutes = {false, true, false, false, false, false, true, false, false, false};
        boolean[] isReads = {true, true, true, true, true, true, true, true, true, true};

        List<ContactShowInfo> infos = new LinkedList<>();

        for (int i = 0; i < headImgRes.length; i++) {
            infos.add(i, new ContactShowInfo(headImgRes[i], usernames[i], lastMsgs[i], lastMsgTimes[i], isMutes[i], isReads[i], types[i]));
        }
        ContactAdapter adapter = new ContactAdapter(this, R.layout.item_wechat_main, infos);
        lv.setAdapter(adapter);


        lv.setOnTouchListener(new View.OnTouchListener() {
            int preX, preY;
            boolean isSlip = false, isLongClick = false;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        preX = (int) event.getX();
                        preY = (int) event.getY();
                        mHandler.postDelayed(() -> {
                            isLongClick = true;
                            int x = (int) event.getX();
                            int y = (int) event.getY();
                            //??????500ms?????????Y??????????????????Toolbar???statusBar??????
                            int position = lv.pointToPosition(x, y - toolbarHeight - statusBarHeight);
                            initPopupMenu(v, x, y, adapter, position, infos);

                        }, 500);
                        break;

                    case MotionEvent.ACTION_MOVE:
                        int nowX = (int) event.getX();
                        int nowY = (int) event.getY();

                        int movedX = Math.abs(nowX - preX);
                        int movedY = Math.abs(nowY - preY);
                        if (movedX > 50 || movedY > 50) {
                            isSlip = true;
                            mHandler.removeCallbacksAndMessages(null);
                            //??????????????????
                        }
                        break;


                    case MotionEvent.ACTION_UP:
                        mHandler.removeCallbacksAndMessages(null);
                        if (!isSlip && !isLongClick) {
                            //??????????????????
                            int position = lv.pointToPosition(preX, preY);

                            Intent intent = new Intent(WechatActivity.this, ChatActivity.class);
                            intent.putExtra("name", usernames[position]);
                            intent.putExtra("profileId", headImgRes[position]);
                            startActivity(intent);
                        } else {
                            isSlip = false;
                            isLongClick = false;
                        }
                        break;
                }
                return false;
            }
        });
    }

    /**
     * ????????????????????????
     *
     * @param isRead   true?????????false??????
     * @param position item position
     * @param adapter  ?????????
     * @param datas
     */
    private void setIsRead(boolean isRead, int position, ContactAdapter adapter, List<ContactShowInfo> datas) {
        ContactShowInfo info = datas.get(position);
        info.setRead(isRead);
        adapter.notifyDataSetChanged();
    }

    /**
     * ??????????????????item
     *
     * @param position ????????????position
     * @param adapter  ?????????
     * @param datas
     */
    private void deleteMsg(int position, ContactAdapter adapter, List<ContactShowInfo> datas) {
        datas.remove(position);
        adapter.notifyDataSetChanged();
    }

    /**
     * ?????????popup??????
     */
    private void initPopupMenu(View anchorView, int posX, int posY, ContactAdapter adapter, int itemPos, List<ContactShowInfo> data) {
        List<String> list = new ArrayList<>();
        ContactShowInfo showInfo = data.get(itemPos);
        //????????????????????????
        switch (showInfo.getAccountType()) {
            case TYPE_SERVICE:
                list.clear();
                if (showInfo.isRead())
                    list.add("????????????");
                else
                    list.add("????????????");
                list.add("???????????????");
                break;

            case TYPE_SUBSCRIBE:
                list.clear();
                if (showInfo.isRead())
                    list.add("????????????");
                else
                    list.add("????????????");
                list.add("???????????????");
                list.add("????????????");
                list.add("???????????????");
                break;

            case TYPE_USER:
                list.clear();
                if (showInfo.isRead())
                    list.add("????????????");
                else
                    list.add("????????????");
                list.add("????????????");
                list.add("???????????????");
                break;
        }
        list.add("????????????");
        SimpleMenuAdapter<String> menuAdapter = new SimpleMenuAdapter<>(this, R.layout.item_menu, list);
        PopupMenuWindows ppm = new PopupMenuWindows(this, R.layout.popup_menu_general_layout, menuAdapter);
        int[] posArr = ppm.reckonPopWindowShowPos(posX, posY);
        ppm.setAutoFitStyle(true);
        ppm.setOnMenuItemClickListener((parent, view, position, id) -> {

            switch (list.get(position)) {
                case "????????????":
                    setIsRead(false, itemPos, adapter, data);
                    break;

                case "????????????":
                    setIsRead(true, itemPos, adapter, data);
                    break;

                case "????????????":
                case "???????????????":
                    stickyTop(adapter, data, itemPos);
                    break;

                case "????????????":
                case "???????????????":
                    deleteMsg(itemPos, adapter, data);
                    break;
                case "????????????":
                    Intent intent = new Intent(WechatActivity.this,AddChatActivity.class);
                    intent.putExtra("uuid",showInfo.getUuid());
                    startActivity(intent);
                    break;
            }
            ppm.dismiss();
        });
        ppm.showAtLocation(anchorView, Gravity.NO_GRAVITY, posArr[0], posArr[1]);
    }


    /**
     * ??????item
     *
     * @param adapter
     * @param datas
     */
    private void stickyTop(ContactAdapter adapter, List<ContactShowInfo> datas, int position) {
        if (position > 0) {
            ContactShowInfo stickyTopItem = datas.get(position);
            datas.remove(position);
            datas.add(0, stickyTopItem);
        } else {
            return;
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(receiver);
    }

    class UserLineReceiver extends BroadcastReceiver{
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            String uuid = intent.getStringExtra("uuid");
            if((getPackageName()+".online").equals(action)) {
                if (!contactShowInfoMap.containsKey(uuid)) {
                    ContactShowInfo info = createContact(uuid);
                    contactShowInfoMap.put(uuid, info);
                    adapter.addData(info);
                }
            }else if((getPackageName()+".chat").equals(action)) {
                ContactShowInfo info = contactShowInfoMap.get(uuid);
                String data = intent.getStringExtra("data");
                System.out.println("chat data ==== "+data);
                info.setLastMsg(data);
                adapter.notifyDataSetChanged();
            }
        }
    }
    private ContactShowInfo createContact(String uuid){


        int[] headImgRes = {R.drawable.hdimg_3, R.drawable.group1, R.drawable.hdimg_2, R.drawable.user_2,
                R.drawable.user_3, R.drawable.user_4, R.drawable.user_5, R.drawable.hdimg_4,
                R.drawable.hdimg_5, R.drawable.hdimg_6};

        String[] usernames = {"Fiona", "  ...   ", "??????", "????????????", "????????????", "?????????????????????",
                "?????????Fiona", "?????????", "??????", "?????????"};
        //????????????
        String[] lastMsgs = {"?????????", "???????????????????????????", "?????????????????????", "???????????????????????????????????????2...",
                "??????????????????", "#????????????#???????????????????????????...", "??????:???????????????", "[Video Call]", "???????????????", "[????????????]"};

        String[] lastMsgTimes = {"17:40", "10:56", "7???26???", "??????", "7???27???", "09:46",
                "7???18???", "?????????", "7???26???", "4???23???"};

        int[] types = {TYPE_USER, TYPE_USER, TYPE_USER, TYPE_SUBSCRIBE, TYPE_SERVICE, TYPE_SUBSCRIBE,
                TYPE_USER, TYPE_USER, TYPE_USER, TYPE_USER};
        //??????&??????
        boolean[] isMutes = {false, true, false, false, false, false, true, false, false, false};
        boolean[] isReads = {true, true, true, true, true, true, true, true, true, true};
        Calendar calendar = Calendar.getInstance();
        String time =  calendar.get(Calendar.HOUR_OF_DAY)+":"+calendar.get(Calendar.MINUTE);
        ContactShowInfo info = new ContactShowInfo(R.drawable.hdimg_3, "??????", "", time, false, true, TYPE_USER);
        info.setUuid(uuid);
        return info;
    }
}

