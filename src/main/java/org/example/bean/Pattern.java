package org.example.bean;

import java.util.List;

/**
 * 行实例类
 * @author chuan
 * @date 2024/8/27
 */
public class Pattern {

    private String name;
    private List<List<String>> rowInstances;
    private List<List<String>> dominantInstances;
    private List<List<String>> dominatedInstances;
    private double PI;
    private double confidence;
    private double lift;
    private int startSlot;
    private int endSlot;
    private String PRInfo;

    public Pattern(String name, List<List<String>> rowInstances) {
        this.name = name;
        this.rowInstances = rowInstances;
    }

    public Pattern(String name, double PI) {
        this.name = name;
        this.PI = PI;
    }

    public Pattern(String name, double PI, double confidence) {
        this.name = name;
        this.PI = PI;
        this.confidence = confidence;
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

    public String toString() {
        return name + " " + rowInstances + " " + PI;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<List<String>> getRowInstances() {
        return rowInstances;
    }

    public void setRowInstances(List<List<String>> rowInstances) {
        this.rowInstances = rowInstances;
    }

    public double getPI() {
        return PI;
    }

    public void setPI(double PI) {
        this.PI = PI;
    }

    public List<List<String>> getDominantInstances() {
        return dominantInstances;
    }

    public void setDominantInstances(List<List<String>> dominantInstances) {
        this.dominantInstances = dominantInstances;
    }

    public List<List<String>> getDominatedInstances() {
        return dominatedInstances;
    }

    public void setDominatedInstances(List<List<String>> dominatedInstances) {
        this.dominatedInstances = dominatedInstances;
    }

    public String getPRInfo() {
        return PRInfo;
    }

    public void setPRInfo(String PRInfo) {
        this.PRInfo = PRInfo;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public double getLift() {
        return lift;
    }

    public void setLift(double lift) {
        this.lift = lift;
    }
}
