package com.github.hymanme.bezieranimation.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

import com.github.hymanme.adheisionview.AdhesionView;
import com.github.hymanme.bezieranimation.R;

/**
 * /**
 * Author   :hyman
 * Email    :hymanme@163.com
 * Create at 2016/10/15
 * Description:
 */

public class AdhesionActivity extends AppCompatActivity implements View.OnClickListener {
    private AdhesionView adhesionView;
    private Button btn_add;
    private Button btn_minus;
    private SeekBar seekBar;
    private SeekBar seekBar2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_adhesion);
        adhesionView = (AdhesionView) findViewById(R.id.adhesionView);
        btn_add = (Button) findViewById(R.id.btn_add);
        btn_minus = (Button) findViewById(R.id.btn_minus);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        seekBar2 = (SeekBar) findViewById(R.id.seekBar2);

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                adhesionView.setMaxDragLen(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                adhesionView.setDefaultDotRadius(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        btn_add.setOnClickListener(this);
        btn_minus.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        if ((v.getId() == R.id.btn_add)) {
            adhesionView.increment(10);
        } else if (v.getId() == R.id.btn_minus) {
            adhesionView.decrement(10);
        }
    }
}
