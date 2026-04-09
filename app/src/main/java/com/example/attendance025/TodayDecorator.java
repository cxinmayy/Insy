package com.example.attendance025;

import android.graphics.Color;

import com.prolificinteractive.materialcalendarview.CalendarDay;
import com.prolificinteractive.materialcalendarview.DayViewDecorator;
import com.prolificinteractive.materialcalendarview.DayViewFacade;

public class TodayDecorator implements DayViewDecorator {

    private final CalendarDay today = CalendarDay.today();

    @Override
    public boolean shouldDecorate(CalendarDay day) {
        return day.equals(today);
    }

    @Override

    public void decorate(DayViewFacade view) {

        // Yellow background


        // Blue circle border
        android.graphics.drawable.GradientDrawable drawable =
                new android.graphics.drawable.GradientDrawable();

        drawable.setColor(Color.TRANSPARENT);
        drawable.setStroke(2, Color.parseColor("#2196F3"));
        drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);

        view.setSelectionDrawable(drawable);
    }
}