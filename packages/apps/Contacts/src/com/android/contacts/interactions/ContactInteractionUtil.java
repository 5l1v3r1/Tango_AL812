/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.contacts.interactions;

import com.google.common.base.Preconditions;

import android.content.Context;
import android.text.format.DateUtils;

import com.android.contacts.common.testing.NeededForTesting;

import java.text.DateFormat;

import java.util.Calendar;
import java.util.Locale;

import com.android.contacts.R;
import java.text.SimpleDateFormat;



/**
 * Utility methods for interactions and their loaders
 */
public class ContactInteractionUtil {

    /**
     * @return a string like (?,?,?...) with {@param count} question marks.
     */
    @NeededForTesting
    public static String questionMarks(int count) {
        Preconditions.checkArgument(count > 0);
        StringBuilder sb = new StringBuilder("(?");
        for (int i = 1; i < count; i++) {
            sb.append(",?");
        }
        return sb.append(")").toString();
    }

    /**
     * Same as {@link formatDateStringFromTimestamp(long, Context, Calendar)} but uses the current
     * time.
     */
    @NeededForTesting
    public static String formatDateStringFromTimestamp(long timestamp, Context context) {
        return formatDateStringFromTimestamp(timestamp, context, Calendar.getInstance());
    }

    /**
     * Takes in a timestamp and outputs a human legible date. This checks the timestamp against
     * compareCalendar.
     * This formats the date based on a few conditions:
     * 1. If the timestamp is today, the time is shown
     * 2. If the timestamp occurs tomorrow or yesterday, that is displayed
     * 3. Otherwise {Month Date} format is used
     */
    @NeededForTesting
    public static String formatDateStringFromTimestamp(long timestamp, Context context,
            Calendar compareCalendar) {
        Calendar interactionCalendar = Calendar.getInstance();
        interactionCalendar.setTimeInMillis(timestamp);

        // compareCalendar is initialized to today
        //modify format data string by caohaolin begin
        //if (compareCalendarDayYear(interactionCalendar, compareCalendar)) {
			String year = context.getResources().getString(R.string.date_format_year);
			String month = context.getResources().getString(R.string.date_format_month);
			String templateDate;
			SimpleDateFormat dateFormat;
			if (getLocaleLanguage()) {
				templateDate = "yyyy"+year+"MM"+month+"dd E HH:mm";
				dateFormat = new SimpleDateFormat(templateDate);
			} else {
				templateDate = "MMM dd"+","+" yyyy"+" E HH:mm";
				dateFormat = new SimpleDateFormat(templateDate, Locale.ENGLISH);
			}
            //return DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT).format(
              //      interactionCalendar.getTime());
			return dateFormat.format(interactionCalendar.getTime());
        //}
        /*
        // Turn compareCalendar to yesterday
        compareCalendar.add(Calendar.DAY_OF_YEAR, -1);
        if (compareCalendarDayYear(interactionCalendar, compareCalendar)) {
            return context.getString(R.string.yesterday);
        }

        // Turn compareCalendar to tomorrow
        compareCalendar.add(Calendar.DAY_OF_YEAR, 2);
        if (compareCalendarDayYear(interactionCalendar, compareCalendar)) {
            return context.getString(R.string.tomorrow);
        }
        return DateUtils.formatDateTime(context, interactionCalendar.getTimeInMillis(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NO_YEAR);*/
        //modify format data string by caohaolin end
    }
    //HQ_wuruijun add for HQ01431674 start
    private static boolean getLocaleLanguage() {
        if (Locale.getDefault().getLanguage().equals("zh")) {
            return true;
        }
        return false;
    }
    //HQ_wuruijun add end
    /**
     * Compares the day and year of two calendars.
     */
    private static boolean compareCalendarDayYear(Calendar c1, Calendar c2) {
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
                c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }
}
