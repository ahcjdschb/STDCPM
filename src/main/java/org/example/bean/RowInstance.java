package org.example.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chuan
 * @date 2024/9/5
 */
public class RowInstance {
    private String dominant;
    private List<String> points;
    private int dominantSlot;
    private int maxSlot;

    public RowInstance(String dominant, List<String> points, int dominantSlot, int maxSlot) {
        this.dominant = dominant;
        this.points = points;
        this.dominantSlot = dominantSlot;
        this.maxSlot = maxSlot;
    }

    public RowInstance() {
        points = new ArrayList<>();
    }

    public int getDominantSlot() {
        return dominantSlot;
    }

    public void setDominantSlot(int dominantSlot) {
        this.dominantSlot = dominantSlot;
    }

    public String getDominant() {
        return dominant;
    }

    public void setDominant(String dominant) {
        this.dominant = dominant;
    }

    public List<String> getPoints() {
        return points;
    }

    public void setPoints(List<String> points) {
        this.points = points;
    }

    public int getMaxSlot() {
        return maxSlot;
    }

    public void setMaxSlot(int maxSlot) {
        this.maxSlot = maxSlot;
    }
}
