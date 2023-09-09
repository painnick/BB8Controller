package com.painnick.bb8controller;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class CustomLogsLayout extends TableLayout {

    protected DateTimeFormatter TimeFormatter = DateTimeFormatter.ofPattern("mm:ss");

    // https://github.com/dracula/dracula-theme
    protected int DefaultFontColor = Color.parseColor("#f8f8f2");
    protected int SendFontColor = Color.parseColor("#f8f8f2");
    protected int RecvFontColor = Color.parseColor("#8be9fd");
    protected int ErrorFontColor = Color.parseColor("#ff5555");
    protected int WarnFontColor = Color.parseColor("#ffb86c");
    protected int InfoFontColor = Color.parseColor("#f8f8f2");
    protected int DebugFontColor = Color.parseColor("#6272a4");

    public CustomLogsLayout(Context context) {
        super(context);
        internalInit();
    }

    public CustomLogsLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        internalInit();
    }

    public void internalInit() {
        this.setLayoutParams(new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    public void debug(LocalTime tm, String msg) {
        String formattedNow = tm.format(TimeFormatter);

        Context context = getContext();

        TableRow tblRow = new TableRow(getContext());
        tblRow.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Time
        TextView tvTime = new TextView(context);
        tvTime.setText(formattedNow);
        tvTime.setTextColor(DebugFontColor);
        tvTime.setTextSize(tvTime.getTextSize() * 1 / 2);
        tblRow.addView(tvTime);

        // Direction
        TextView tvDir = new TextView(context);
        tvDir.append(" DD ");
        tvDir.setTextColor(DebugFontColor);
        tblRow.addView(tvDir);

        // Text
        TextView tvText = new TextView(context);
        tvText.setText(msg);
        tvText.setTextColor(DebugFontColor);
        tblRow.addView(tvText);

        addView(tblRow);
    }

    public void info(LocalTime tm, String msg) {
        String formattedNow = tm.format(TimeFormatter);

        Context context = getContext();

        TableRow tblRow = new TableRow(getContext());
        tblRow.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Time
        TextView tvTime = new TextView(context);
        tvTime.setText(formattedNow);
        tvTime.setTextColor(InfoFontColor);
        tvTime.setTextSize(tvTime.getTextSize() * 1 / 2);
        tblRow.addView(tvTime);

        // Direction
        TextView tvDir = new TextView(context);
        tvDir.append(" II ");
        tvDir.setTextColor(InfoFontColor);
        tblRow.addView(tvDir);

        // Text
        TextView tvText = new TextView(context);
        tvText.setText(msg);
        tvText.setTextColor(InfoFontColor);
        tblRow.addView(tvText);

        addView(tblRow);
    }

    public void warn(LocalTime tm, String msg) {
        String formattedNow = tm.format(TimeFormatter);

        Context context = getContext();

        TableRow tblRow = new TableRow(getContext());
        tblRow.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Time
        TextView tvTime = new TextView(context);
        tvTime.setText(formattedNow);
        tvTime.setTextColor(WarnFontColor);
        tvTime.setTextSize(tvTime.getTextSize() * 1 / 2);
        tblRow.addView(tvTime);

        // Direction
        TextView tvDir = new TextView(context);
        tvDir.append(" WW ");
        tvDir.setTextColor(WarnFontColor);
        tblRow.addView(tvDir);

        // Text
        TextView tvText = new TextView(context);
        tvText.setText(msg);
        tvText.setTextColor(WarnFontColor);
        tblRow.addView(tvText);

        addView(tblRow);
    }

    public void error(LocalTime tm, String msg) {
        String formattedNow = tm.format(TimeFormatter);

        Context context = getContext();

        TableRow tblRow = new TableRow(getContext());
        tblRow.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Time
        TextView tvTime = new TextView(context);
        tvTime.setText(formattedNow);
        tvTime.setTextColor(ErrorFontColor);
        tvTime.setTextSize(tvTime.getTextSize() * 1 / 2);
        tblRow.addView(tvTime);

        // Direction
        TextView tvDir = new TextView(context);
        tvDir.append(" EE ");
        tvDir.setTextColor(ErrorFontColor);
        tblRow.addView(tvDir);

        // Text
        TextView tvText = new TextView(context);
        tvText.setText(msg);
        tvText.setTextColor(ErrorFontColor);
        tblRow.addView(tvText);

        addView(tblRow);
    }

    public void send(LocalTime tm, String msg) {
        String formattedNow = tm.format(TimeFormatter);

        Context context = getContext();

        TableRow tblRow = new TableRow(getContext());
        tblRow.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Time
        TextView tvTime = new TextView(context);
        tvTime.setText(formattedNow);
        tvTime.setTextColor(DefaultFontColor);
        tvTime.setTextSize(tvTime.getTextSize() * 1 / 2);
        tblRow.addView(tvTime);

        // Direction
        TextView tvDir = new TextView(context);
        tvDir.append(" >> ");
        tvDir.setTextColor(SendFontColor);
        tblRow.addView(tvDir);

        // Text
        TextView tvText = new TextView(context);
        tvText.setText(msg);
        tvText.setTextColor(SendFontColor);
        tblRow.addView(tvText);

        addView(tblRow);
    }

    public void recv(LocalTime tm, String msg) {
        String formattedNow = tm.format(TimeFormatter);

        Context context = getContext();

        TableRow tblRow = new TableRow(getContext());
        tblRow.setLayoutParams(new TableLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Time
        TextView tvTime = new TextView(context);
        tvTime.setText(formattedNow);
        tvTime.setTextColor(DefaultFontColor);
        tvTime.setTextSize(tvTime.getTextSize() * 1 / 2);
        tblRow.addView(tvTime);

        // Direction
        TextView tvDir = new TextView(context);
        tvDir.append(" << ");
        tvDir.setTextColor(RecvFontColor);
        tblRow.addView(tvDir);

        // Text
        TextView tvText = new TextView(context);
        tvText.setText(msg);
        tvText.setTextColor(RecvFontColor);
        tblRow.addView(tvText);

        addView(tblRow);
    }
}
