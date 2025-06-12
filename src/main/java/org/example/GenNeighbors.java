package org.example;

import org.example.bean.Point;
import org.gavaghan.geodesy.Ellipsoid;
import org.gavaghan.geodesy.GeodeticCalculator;
import org.gavaghan.geodesy.GeodeticCurve;
import org.gavaghan.geodesy.GlobalCoordinates;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class GenNeighbors {

    static int slotNum = 7;
    static int span = 3;
    static double minDistance = 1200;
    public static void main(String[] args) {
        String filename = "";
        genInstancesNeighbors(filename);
    }

    private static void writeToContent(String content) {
        try (BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("instancesNeighbors.txt", true))) {
            bufferedWriter.write(content);
        } catch (IOException e) {
            System.err.println("error：" + e.getMessage());
        }
    }


    public static void genInstancesNeighbors(String filename) {
        List<List<Point>> slotPoints = readFile(filename);
        calNeighbors(slotPoints);
    }

    public static void calNeighbors(List<List<Point>> slotPoints) {
        for (List<Point> points : slotPoints) {
            int slot = points.getFirst().getStartSlot();
            System.out.println("slot: " + slot);
            int targetSlot = slot + span;
            if (targetSlot > slotNum) {
                targetSlot = slotNum;
            }
            for (Point point : points) {
                int newSlot = targetSlot;
                if (point.getEndSlot() != -1 && targetSlot > point.getEndSlot()) {
                    newSlot = point.getEndSlot();
                }
                StringBuilder sb = new StringBuilder();
                for (int i = slot; i <= newSlot; i++) {
                    List<Point> targetPoints = slotPoints.get(i - 1);
                    for (Point targetPoint : targetPoints) {
                        if (point.getType().compareTo(targetPoint.getType()) != 0) {
                            double distance = getDistance(point, targetPoint);
                            if (distance <= minDistance) {
                                sb.append(point.getName()).append(" ").append(targetPoint.getName()).append(" ").append(distance).append("\n");
                            }
                        }
                    }
                }
                writeToContent(sb.toString());
            }
        }

    }

    public static List<List<Point>> readFile(String filename) {
        List<List<Point>> points = new ArrayList<>();
        for (int i = 1; i <= slotNum; i++) {
            points.add(new ArrayList<>());
        }
        File file = new File(filename);
        BufferedReader br;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            Point point;
            while ((line = br.readLine()) != null) {
                String[] row = line.split(",");
                point = new Point(row[0], row[1], Double.parseDouble(row[2]), Double.parseDouble(row[3]), Integer.parseInt(row[4]), Integer.parseInt(row[5]));
                points.get(point.getStartSlot() - 1).add(point);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return points;
    }

    public static double getDistance(Point point1, Point point2) {
        GlobalCoordinates firstPoint = new GlobalCoordinates(point1.getX(), point1.getY());
        GlobalCoordinates secondPoint = new GlobalCoordinates(point2.getX(), point2.getY());
        GeodeticCurve geoCurve = new GeodeticCalculator().calculateGeodeticCurve(Ellipsoid.WGS84, firstPoint, secondPoint);
        return geoCurve.getEllipsoidalDistance();
    }
}
