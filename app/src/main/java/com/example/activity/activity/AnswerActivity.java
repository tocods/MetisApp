package com.example.activity.activity;

import org.json.JSONException;
import org.json.JSONObject;
import android.view.View;
import android.widget.Toast;
import android.widget.Button;
import android.widget.Chronometer;
import android.app.ProgressDialog;
import android.content.Intent;


import com.example.activity.MyApplication;
import com.example.activity.R;
import com.example.activity.bean.QuestBean;
import com.example.activity.message.CompeteMessage;
import com.example.activity.message.MatchMessage;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.example.activity.service.FragmentCallBack;


import butterknife.OnClick;
import cz.msebera.android.httpclient.Header;


import butterknife.BindView;

import com.example.activity.constants.config;
import com.example.activity.utils.LogUtils;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.RequestParams;
import com.example.activity.fragment.AnswerFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager.widget.ViewPager;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;


public class AnswerActivity extends BaseActivity implements Chronometer.OnChronometerTickListener,FragmentCallBack {
    @BindView(R.id.vp_answer)
    ViewPager vp_answer;

    @BindView(R.id._chro_exam)
    Chronometer chronometer;

    private volatile ArrayList<Fragment> fragmentList;
    private volatile List<QuestBean> message;
    private String field;
    private String myanswer = "null";
    private String playernum;
    private int myscore = 0;
    private int second = 20;
    private int nowpager = 0;


    @Override
    int getLayoutId()
    {
        return R.layout.activity_answer;
    }

    @Override
    void getPreIntent()
    {
        playernum = ((MyApplication)getApplication()).getPlaytype();
        field = getIntent().getExtras().get("field").toString().trim();
    }


    @Override
    void initView() {
        setChronometer();
        initNet(field);
        vp_answer.setOnPageChangeListener(new MyOnPageChangeListener());
    }

    @Override
    public void sendAnswer(String s){
        myanswer = s;
    }


    private void setChronometer() {
        chronometer.setText(nowtime());
        chronometer.start();
        chronometer.setOnChronometerTickListener(this);
    }

    /**
     * 计时器规则
     *
     * @param chronometer
     */
    @Override
     public void onChronometerTick(Chronometer chronometer) {
        second--;
        if (second == 0) {

        }
    }

    private String nowtime()
    {
        if (second < 10) {
            return ("最后" + second + "秒");
        } else {
            return ("" + second);
        }
    }






    private void initNet(String field)
    {
        fragmentList = new ArrayList<>();
        message = new ArrayList<>();
        LogUtils.e("initNet: 开始联网…………");
        final ProgressDialog progressDialog = new ProgressDialog(AnswerActivity.this, R.style.btnStyle);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("获取题目中...");
        progressDialog.show();
        Button btn = (Button)findViewById(R.id._btn_message);
        String url = config.HOST;
        AsyncHttpClient client = new AsyncHttpClient();
        RequestParams params = new RequestParams();
        params.put("type","single");
        params.put("field",field);
        client.get(config.URL_GET_QUESTION,params, new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(int statusCode, Header[] headers, JSONArray response) {
                        super.onSuccess(statusCode, headers, response);
                        for (int i = 0; i < response.length(); i++) {
                            try {
                                JSONObject obj = response.getJSONObject(i);
                                message.add(new QuestBean(
                                        i,
                                        obj.getString("type"),
                                        obj.getString("title"),
                                        obj.getString("optionA"),
                                        obj.getString("optionB"),
                                        obj.getString("optionC"),
                                        obj.getString("optionD"),
                                        obj.getString("answer"),
                                        obj.getString("field")
                                ));
                                fragmentList.add(new AnswerFragment(message.get(i)));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        vp_answer.setAdapter(new MainAdapter(getSupportFragmentManager()));

                        LogUtils.e("initNet: 联网结束…………");

                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                        super.onFailure(statusCode, headers, responseString, throwable);
                        Toast.makeText(AnswerActivity.this, "sbl", Toast.LENGTH_SHORT).show();
                    }
                }
        );
        progressDialog.dismiss();

    }

    class MainAdapter extends FragmentPagerAdapter {

        public MainAdapter(FragmentManager fm) {
            super(fm);
        }


        //获取条目
        @Override
        public Fragment getItem(int position) {
            return fragmentList.get(position);
        }

        //数目
        @Override
        public int getCount() {
            return fragmentList.size();
        }
    }

    @OnClick({R.id._btn_submit,R.id._btn_tool,R.id._btn_message})
    public void onViewClicked(View v)
    {
        switch (v.getId())
        {
            case R.id._btn_submit:
                if(myanswer == "null")
                {
                    return;
                }
                if(myanswer.equals(message.get(nowpager).getAnswer()))
                {
                    myscore += 20;
                }
                myanswer = "null";
                if(nowpager == fragmentList.size() - 1)
                    getGrade();
                vp_answer.setCurrentItem(++nowpager);
            case R.id._btn_message:
            case R.id._btn_tool:
        }
    }

    private class MyOnPageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            nowpager = position;
        }

        @Override
        public void onPageScrollStateChanged(int state) {

        }
    }

    public void getGrade() {
        if (playernum.equals("match")) {
            String username = ((MyApplication)getApplication()).getUsername();
            Thread thread = new Thread() {
                @Override
                public void run() {
                    ((MyApplication) getApplication()).sendMessage(new CompeteMessage(username, myscore));
                }
            };
        thread.start();
        }
        Intent intent1 = new Intent(AnswerActivity.this,GradeActivity.class);
        intent1.putExtra("grade",myscore+"");
        intent1.putExtra("num",playernum);
        startActivity(intent1);
    }

}

