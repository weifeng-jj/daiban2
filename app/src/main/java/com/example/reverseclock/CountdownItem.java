package com.example.reverseclock;

/**
 * 倒数日数据模型
 */
public class CountdownItem {
    public int id;           // 唯一标识
    public String name;      // 事件名称，如"爸爸的生日"
    public int year;         // 目标年份
    public int month;        // 目标月份（1-12）
    public int day;          // 目标日期
    public long createTime;  // 创建时间戳

    public CountdownItem() {}

    public CountdownItem(int id, String name, int year, int month, int day, long createTime) {
        this.id = id;
        this.name = name;
        this.year = year;
        this.month = month;
        this.day = day;
        this.createTime = createTime;
    }
}

