package com.example.messenger;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

public class ActivityFeedActivity extends AppCompatActivity {

    private ActivityFeedViewModel viewModel;

    private TextView totalSent;
    private TextView activeChats;
    private TextView unread;
    private TextView groupChats;
    private TextView lastAction;
    private TextView topContact;

    private TextView[] dayCounts;
    private LinearLayout[] dayBars;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

        viewModel = new ViewModelProvider(this).get(ActivityFeedViewModel.class);

        ImageButton buttonBack = findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(v -> onBackPressed());

        totalSent = findViewById(R.id.valueTotalSent);
        activeChats = findViewById(R.id.valueActiveChats);
        unread = findViewById(R.id.valueUnread);
        groupChats = findViewById(R.id.valueGroupChats);
        lastAction = findViewById(R.id.valueLastAction);
        topContact = findViewById(R.id.valueTopContact);

        dayCounts = new TextView[]{
                findViewById(R.id.dayCount1), findViewById(R.id.dayCount2), findViewById(R.id.dayCount3),
                findViewById(R.id.dayCount4), findViewById(R.id.dayCount5), findViewById(R.id.dayCount6), findViewById(R.id.dayCount7)
        };
        dayBars = new LinearLayout[]{
                findViewById(R.id.dayBar1), findViewById(R.id.dayBar2), findViewById(R.id.dayBar3),
                findViewById(R.id.dayBar4), findViewById(R.id.dayBar5), findViewById(R.id.dayBar6), findViewById(R.id.dayBar7)
        };

        viewModel.getStats().observe(this, this::render);
        viewModel.loadActivity();
    }

    private void render(ActivityFeedViewModel.ActivityStats stats) {
        totalSent.setText(String.valueOf(stats.totalSent));
        activeChats.setText(String.valueOf(stats.activeChats));
        unread.setText(String.valueOf(stats.unread));
        groupChats.setText(String.valueOf(stats.groupChats));
        lastAction.setText(stats.lastAction);
        topContact.setText(stats.topContact);

        int max = 1;
        for (int value : stats.week) {
            if (value > max) max = value;
        }

        for (int i = 0; i < 7; i++) {
            int value = stats.week[i];
            dayCounts[i].setText(String.valueOf(value));
            float weight = Math.max(0.2f, (float) value / (float) max);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) dayBars[i].getLayoutParams();
            params.weight = weight;
            dayBars[i].setLayoutParams(params);
            dayBars[i].setAlpha(value == 0 ? 0.35f : 1f);
        }
    }
}
