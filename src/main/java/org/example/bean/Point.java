package org.example.bean;

/**
 * @author chuan
 * @date 2024/8/14
 */

/*
 * 兴趣点（POI）类
 * */
public class Point implements Comparable<Point> {
    private String type;
    private String id;
    private String name;
    private double x;
    private double y;
    private int startSlot;
    private int endSlot;

    public Point(String type, String id, double x, double y, int startSlot, int endSlot) {
        this.type = type;
        this.id = id;
        this.x = x;
        this.y = y;
        this.startSlot = startSlot;
        this.endSlot = endSlot;
        this.name = type + "." + id;
    }
    public Point(String type, String id) {
        this.type = type;
        this.id = id;
    }
    public Point(String type, String id, int startSlot, int endSlot) {
        this.type = type;
        this.id = id;
        this.startSlot = startSlot;
        this.endSlot = endSlot;
        this.name = type + "." + id;
    }

    public Point(String type, String id, double x, double y) {
        this.type = type;
        this.id = id;
        this.x = x;
        this.y = y;
        this.name = type + "." + id;
    }

    public String toString() {
        return name;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public double getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getStartSlot() {
        return startSlot;
    }

    public void setStartSlot(int startSlot) {
        this.startSlot = startSlot;
    }

    public int getEndSlot() {
        return endSlot;
    }

    public void setEndSlot(int endSlot) {
        this.endSlot = endSlot;
    }

    @Override
    public int compareTo(Point o) {
        if (this.getType().compareTo(o.getType()) > 0) {
            return 1;
        } else if (this.getType().compareTo(o.getType()) < 0) {
            return -1;
        } else {
            if (this.getId().compareTo(o.getId()) > 0) {
                return 1;
            } else if (this.getId().compareTo(o.getId()) < 0) {
                return -1;
            }
        }
        return 0;
    }
}
