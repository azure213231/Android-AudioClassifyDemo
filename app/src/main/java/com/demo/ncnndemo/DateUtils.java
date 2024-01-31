package com.demo.ncnndemo;

import android.content.Context;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {

    private static final String TAG = "DataUtils";
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();

    public static long getStringToDateUTC(String dateString, String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // 设置时区为 UTC
        Date date = new Date();
        try {
            date = dateFormat.parse(dateString);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return date.getTime();
    }

    /**
     * 将具体的时间戳转为格式化的时间
     */
    public static String longTimeToString(String format, long time) {
        return new SimpleDateFormat(format, Locale.CHINA).format(new Date(time));
    }

    /**
     * 将具体的时间戳转为格式化的时间(过滤时区)
     */
    public static String longTimeToStringUTC(String format, long time) {
        Date date = new Date(time);
// 使用 SimpleDateFormat 格式化日期
        SimpleDateFormat sdf = new SimpleDateFormat(format);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // 设置时区为 UTC
        String formattedDate = sdf.format(date);
        return formattedDate;
    }


    /**
     * 判断当前日期是星期几
     */
    public static int getWeek() {
        int Week = 0;
        final Calendar c = Calendar.getInstance();
        c.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        String mWay = String.valueOf(c.get(Calendar.DAY_OF_WEEK));
        if ("1".equals(mWay)) {
            Week = 7;
        } else if ("2".equals(mWay)) {
            Week = 1;
        } else if ("3".equals(mWay)) {
            Week = 2;
        } else if ("4".equals(mWay)) {
            Week = 3;
        } else if ("5".equals(mWay)) {
            Week = 4;
        } else if ("6".equals(mWay)) {
            Week = 5;
        } else if ("7".equals(mWay)) {
            Week = 6;
        }
        return Week;
    }

    /**
     * 根据时间戳算出周几
     */
    public static int getWeekOfDate(long longTime) {
        int[] weekOfDays = {7, 1, 2, 3, 4, 5, 6};
        Calendar calendar = Calendar.getInstance();
        if (longTime != 0) {
            calendar.setTime(new Date(longTime));
        }
        int w = calendar.get(Calendar.DAY_OF_WEEK) - 1;
        if (w < 0) {
            w = 0;
        }
        return weekOfDays[w];
    }


    /**
     * 获取一天的毫秒值
     */
    public static long getOneDayTimes() {
        return 1000l * 60l * 60l * 24l;
    }


    /**
     * 得到指定月的天数
     */
    public static int getMonthAllDay(int year,int month) {
        Calendar a = Calendar.getInstance();
        a.clear();
        a.set(Calendar.YEAR, year);
        a.set(Calendar.MONTH, month-1);
        a.set(Calendar.DATE, 1);//把日期设置为当月第一天
        a.roll(Calendar.DATE, -1);//日期回滚一天，也就是最后一天
        return a.get(Calendar.DATE);
    }

    /**
     * 得到指定年的周数
     */
    public static int getWeekAllYear(int year) {
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.YEAR, year);

        int weeksInYear = calendar.getActualMaximum(Calendar.WEEK_OF_YEAR);
        return weeksInYear;
    }


    /**
     * 获取当月中，距今天的天数
     * 今天为1号，则为1
     */
    public static int getMonthDayFromFirstToToday() {

        return Integer.valueOf(longTimeToString("dd", System.currentTimeMillis()));
    }

    /**
     * 将日期对象格式化为指定格式的字符串
     *
     * @param date    日期对象
     * @param pattern 时间格式
     * @return 格式化后的时间字符串
     */
    public static String formatToStr(Date date, String pattern) {
        DateFormat dateFormat = getDateFormat(pattern);
        return dateFormat.format(date);
    }

    /**
     * 获取指定格式的日期格式化对象
     *
     * @param pattern 时间格式
     * @return 日期格式化对象
     */
    private static DateFormat getDateFormat(String pattern) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
        dateFormat.setTimeZone(DEFAULT_TIMEZONE);
        return dateFormat;
    }

    /**
     * 获取这个月是几月
     * */
    public static int getMonth(){
        // 获取当前日期和时间
        Date currentDate = new Date();

        // 创建一个Calendar实例，并将其设置为当前日期和时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        // 获取今天的月份（注意：Java中的月份是从0开始的，所以要加1）
        return calendar.get(Calendar.MONTH) + 1;
    }

    /**
     * 获取指定月份
     * */
    public static int getMonth(long timestamp){
        Date currentDate = new Date(timestamp);

        // 创建一个Calendar实例，并将其设置为当前日期和时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        // 获取今天的月份（注意：Java中的月份是从0开始的，所以要加1）
        return calendar.get(Calendar.MONTH) + 1;
    }

    /**
     * 获取指定日
     * */
    public static int getDay(long timestamp){
        Date currentDate = new Date(timestamp);

        // 创建一个Calendar实例，并将其设置为当前日期和时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        return calendar.get(Calendar.DAY_OF_MONTH) ;
    }

    public static int getYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    public static int getYear(long timestamp) {
        Date currentDate = new Date(timestamp);

        // 创建一个Calendar实例，并将其设置为当前日期和时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        return calendar.get(Calendar.YEAR);
    }

    /**
     * 获取时间戳
     * */
    public static long getTimeStamp(int year,int month,int day){
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month - 1, day); // 月份在Calendar中是从0开始的，所以需要减1

        Date date = calendar.getTime();
        long timestamp = date.getTime();
        return timestamp;
    }

    /**
     * 获取时间戳
     * */
    public static long getTimeStamp(String date,String pattern){
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            Date parse = sdf.parse(date);
            return parse.getTime();
        } catch (ParseException e) {
            return 0l;
        }
    }

    /**
     * 获取指定周
     * */
    public static int getWeek(long timestamp){
        Date currentDate = new Date(timestamp);

        // 创建一个Calendar实例，并将其设置为当前日期和时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        int i = calendar.get(Calendar.WEEK_OF_YEAR);
        return i;
    }

    /**
     * 根据一年的第几周获取周一
     * */
    public static Date getMonDayOfWeek(int year,int week){
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.WEEK_OF_YEAR, week);

        // 将日期设置为指定周的周一
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        Date monday = calendar.getTime();
        return monday;
    }

    /**
     * 获取日期所在周的周一
     * */
    public static Date getMonDay(long timestamp){
        Date currentDate = new Date(timestamp);

        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTime(currentDate);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);

        // 将日期设置为指定周的周一
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        Date monday = calendar.getTime();
        return monday;
    }

    /**
     * 获取日期所在周的周日
     * */
    public static Date getSunDay(long timestamp){
        Date currentDate = new Date(timestamp);

        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setTime(currentDate);
        calendar.setFirstDayOfWeek(Calendar.MONDAY);

        // 将日期设置为指定周的周一
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek()-1);
        Date monday = calendar.getTime();
        return monday;
    }

    /**
     * 根据一年的第几周获取周日
     * */
    public static Date getSunDayOfWeek(int year,int week){
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.setFirstDayOfWeek(Calendar.MONDAY);
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.WEEK_OF_YEAR, week);

        // 将日期设置为指定周的周日
        calendar.add(Calendar.DAY_OF_WEEK, 6);
        Date sunday = calendar.getTime();
        return sunday;
    }

    /**
     * 获取年份的第一天
     * */
    public static long getFirstDayOfYear(int year){
        Calendar calendar = Calendar.getInstance();
        calendar.clear();
        calendar.set(Calendar.YEAR, year);
        calendar.set(Calendar.DAY_OF_YEAR, 1);

        // 获取一年的第一天
        return calendar.getTime().getTime();
    }

    /**
     * 获取日期字符串
     * */
    public static String getStringFromTimeStamp(long timeStamp,String pattern){
        Date date = new Date(timeStamp);
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        String dateString = sdf.format(date);
        return dateString;
    }

    /**
     * 获取小时
     * */
    public static int geUTCtHours(long timestamp) {
        Date currentDate = new Date(timestamp);

        // 创建一个Calendar实例，并将其设置为当前日期和时间
        TimeZone timeZone = TimeZone.getTimeZone("UTC");
        Calendar calendar = Calendar.getInstance(timeZone );
        calendar.setTime(currentDate);

        return calendar.get(Calendar.HOUR);
    }

    /**
     * 获取小时
     * */
    public static int getHours(long timestamp) {
        Date currentDate = new Date(timestamp);

        // 创建一个Calendar实例，并将其设置为当前日期和时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        return calendar.get(Calendar.HOUR_OF_DAY);
    }

    /**
     * 获取分钟
     * */
    public static int getMinutes(long timestamp) {
        Date currentDate = new Date(timestamp);

        // 创建一个Calendar实例，并将其设置为当前日期和时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        return calendar.get(Calendar.MINUTE);
    }

    /**
     * 获取秒
     * */
    public static int getSeconds(long timestamp) {
        Date currentDate = new Date(timestamp);

        // 创建一个Calendar实例，并将其设置为当前日期和时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);

        return calendar.get(Calendar.SECOND);
    }

    /**
     * 获取一个月的第一天
     */
    public static long getFirstDayOfMonth(int year,int month) {

        Calendar cal_1 = Calendar.getInstance();//获取当前日期
        cal_1.clear();
        cal_1.set(Calendar.YEAR, year);
        cal_1.set(Calendar.MONTH, month - 1); // 减n个月
        cal_1.set(Calendar.DAY_OF_MONTH, 1);// 设置当前日为1号
        long firstTime = cal_1.getTime().getTime();

        return firstTime;
    }

    /**
     * 获取一个月的第一天的时间戳
     */
    public static long getFirstDayOfMonth(long timestamp) {
        Date currentDate = new Date(timestamp);

        // 创建一个Calendar实例，并将其设置为当前日期和时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        // 设置时间为当天的最后一毫秒
        calendar.set(Calendar.DAY_OF_MONTH, 1);// 设置当前日为1号

        return calendar.getTime().getTime();
    }

    /**
     * 获取一个月的最后一天的时间戳
     */
    public static long getLastDayOfMonth(long timestamp) {
        Date currentDate = new Date(timestamp);

        // 创建一个Calendar实例，并将其设置为当前日期和时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        // 设置时间为当天的最后一毫秒
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));// 设置当前日为1号

        return calendar.getTime().getTime();
    }

    /**
     * 获取某天的最后一个时间戳
     */
    public static long getLastTimestampOfDay(long timestamp) {
        Date currentDate = new Date(timestamp);

        // 创建一个Calendar实例，并将其设置为当前日期和时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        // 设置时间为当天的最后一毫秒
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);

        return calendar.getTimeInMillis();
    }

    /**
     * 获取某天的第一个时间戳
     */
    public static long getFirstTimestampOfDay(long timestamp) {
        Date currentDate = new Date(timestamp);

        // 创建一个Calendar实例，并将其设置为当前日期和时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        // 设置时间为当天的最后一毫秒
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
    }

    /**
     * 根据入睡时间获取应该是哪一天的记录
     */
    public static long getReportDayInWhenDay(long timestamp) {
        Date currentDate = new Date(timestamp);

        // 创建一个Calendar实例，并将其设置为当前日期和时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        if (calendar.get(Calendar.HOUR_OF_DAY) < 10){
            calendar.add(Calendar.DAY_OF_MONTH, -1); // 获取昨天的日期
        }

        // 设置时间为当天的最后一毫秒
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
    }

    /**
     * 根据入睡时间，获取所在日期的晚上8点
     */
    public static long getTwentySleepTimestampOfDay(long timestamp) {
        Date currentDate = new Date(timestamp);

        // 创建一个Calendar实例，并将其设置为当前日期和时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        if (calendar.get(Calendar.HOUR_OF_DAY) < 10){
            calendar.add(Calendar.DAY_OF_MONTH, -1); // 获取昨天的日期
        }

        // 设置时间为当天的20点
        calendar.set(Calendar.HOUR_OF_DAY, 20);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
    }

    /**
     * 获取是一周的第几天
     */
    public static int getIndexOfWeek(long timestamp) {
        Date currentDate = new Date(timestamp);

        // 创建一个Calendar实例，并将其设置为当前日期和时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        calendar.setFirstDayOfWeek(Calendar.MONDAY); // 设置一周的第一天为周一

        int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);

        // 将星期日（1）转换为 7
        if (dayOfWeek == Calendar.SUNDAY) {
            dayOfWeek = 7;
        } else {
            dayOfWeek--; // 其他星期减一
        }

        return  dayOfWeek;
    }
}
