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
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.kanade.imageselector.R;
import com.kanade.imageselector.impl.BoxingFrescoLoader;
import com.kanade.imageselector.impl.BoxingGlideLoader;
import com.kanade.imageselector.impl.BoxingPicassoLoader;
import com.kanade.imageselector_core.Boxing;
import com.kanade.imageselector_core.BoxingMediaLoader;
import com.kanade.imageselector_core.loader.IBoxingMediaLoader;
import com.kanade.imageselector_core.model.config.BoxingConfig;

/**
 * @author ChenSL
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createToolbar();
        findViewById(R.id.first_btn).setOnClickListener(this);
        findViewById(R.id.second_btn).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.first_btn:
                Intent intent1 = new Intent(MainActivity.this, FirstActivity.class);
                startActivity(intent1);
                break;
            case R.id.second_btn:
                BoxingConfig singleImgConfig = new BoxingConfig(BoxingConfig.Mode.SINGLE_IMG);
                Boxing.of(singleImgConfig).withIntent(this, SecondActivity.class).start(this);
                break;
            default:
                break;
        }
    }

    private void createToolbar() {
        Toolbar bar = (Toolbar) findViewById(R.id.nav_top_bar);
        setSupportActionBar(bar);
        getSupportActionBar().setTitle(R.string.app_name);
        bar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        IBoxingMediaLoader loader;
        switch (id) {
            case R.id.menu_fresco:
                loader = new BoxingFrescoLoader(this);
                break;
            case R.id.menu_glide:
                loader = new BoxingGlideLoader();
                break;
            case R.id.menu_picasso:
                loader = new BoxingPicassoLoader();
                break;
            default:
                loader = new BoxingFrescoLoader(this);
                break;
        }
        BoxingMediaLoader.getInstance().init(loader);
        return super.onOptionsItemSelected(item);
    }
}
