/*
 *  Copyright (C) 2017 Bilibili
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.kanade.imageselector.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.kanade.imageselector.R;
import com.kanade.imageselector_core.AbsBoxingActivity;
import com.kanade.imageselector_core.AbsBoxingViewFragment;
import com.kanade.imageselector_core.Boxing;
import com.kanade.imageselector_core.BoxingMediaLoader;
import com.kanade.imageselector_core.model.config.BoxingConfig;
import com.kanade.imageselector_core.model.entity.BaseMedia;
import com.kanade.imageselector_imply.ui.BoxingBottomSheetFragment;
import com.kanade.imageselector_imply.ui.BoxingViewActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * to show use {@link AbsBoxingViewFragment} without {@link AbsBoxingActivity}.
 * use {@link Boxing#setupFragment(AbsBoxingViewFragment, Boxing.OnBoxingFinishListener)} to set a fragment.
 *
 * @author ChenSL
 */
public class SecondActivity extends AppCompatActivity implements View.OnClickListener {
    private FrameLayout mInsideBottomSheet;
    private ImageView mResultImg;
    private BaseMedia mMedia;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
        createToolbar();
        findViewById(R.id.inside_bs_btn).setOnClickListener(this);
        mResultImg = (ImageView) findViewById(R.id.media_result);
        mResultImg.setOnClickListener(this);
        mInsideBottomSheet = (FrameLayout) findViewById(R.id.content_layout);
        BoxingBottomSheetFragment fragment = (BoxingBottomSheetFragment) getSupportFragmentManager().
                findFragmentByTag(BoxingBottomSheetFragment.TAG);
        if (fragment == null) {
            fragment = BoxingBottomSheetFragment.newInstance();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.content_layout, fragment, BoxingBottomSheetFragment.TAG).commit();

            BoxingConfig singleImgConfig = new BoxingConfig(BoxingConfig.Mode.SINGLE_IMG);
            Boxing.of(singleImgConfig).setupFragment(fragment, new Boxing.OnBoxingFinishListener() {

                @Override
                public void onBoxingFinish(Intent intent, List<BaseMedia> medias) {
                    BottomSheetBehavior behavior = BottomSheetBehavior.from(mInsideBottomSheet);
                    behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    if (medias != null && medias.size() > 0) {
                        BaseMedia media = mMedia = medias.get(0);
                        String path = media.getPath();
                        BoxingMediaLoader.getInstance().displayRaw(mResultImg, path, null);
                    }
                }
            });
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.inside_bs_btn:
                showFragment();
                break;
            case R.id.media_result:
                if (mMedia == null) {
                    return;
                }
                ArrayList<BaseMedia> medias = new ArrayList<>(1);
                medias.add(mMedia);
                Boxing.get().withIntent(this, BoxingViewActivity.class, medias).start(this, BoxingConfig.ViewMode.PREVIEW);
                break;
            default:
                break;
        }
    }

    private void showFragment() {
        if (mInsideBottomSheet != null) {
            BottomSheetBehavior behavior = BottomSheetBehavior.from(mInsideBottomSheet);
            if (behavior.getState() != BottomSheetBehavior.STATE_EXPANDED) {
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else {
                behavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        }
    }


    private void createToolbar() {
        Toolbar bar = (Toolbar) findViewById(R.id.nav_top_bar);
        setSupportActionBar(bar);
        getSupportActionBar().setTitle(R.string.second_demo_title);
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

}
