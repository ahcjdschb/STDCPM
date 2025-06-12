package org.example;


import org.example.bean.Pattern;
import org.example.bean.Point;
import org.example.bean.RowInstance;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author chuan
 * @date 2024/12/23
 */
public class STDCPMLS {
    int slotNum;
    int span;
    double minPI;
    double minDistance;
    Map<String, List<String>> starNeighbors;
    Map<String, Point> pointMap;
    Map<String, Integer> incrementedPoints;

    public STDCPMLS(int slotNum, int span, double minDistance, double minPI) {
        this.slotNum = slotNum;
        this.span = span;
        this.minDistance = minDistance;
        this.minPI = minPI;
        starNeighbors = new HashMap<>();
        incrementedPoints = new HashMap<>();
        pointMap = new HashMap<>();
    }

    public void run(String dataFilePath, String neighborFilePath) {
        initializeData(dataFilePath, neighborFilePath);
        miningPattern(dataFilePath);
    }

    private void miningPattern(String dataFilePath) {
        String basePath = getBasePath(dataFilePath);
        Map<String, Pattern> prePatterns = genSecondOrder();
        List<Set<String>> sortedPatterns = new ArrayList<>();
        sortedPatterns.add(new HashSet<>(prePatterns.keySet()));
        int k = 2;
        while (true) {
            writeToFile(basePath + k + "-size patterns.txt", prePatterns);
            if (isEmpty(sortedPatterns)) {
                System.out.println(k + "-size patterns is empty.");
                break;
            }
            List<Set<String>> candidatePatterns = genCandidatePattern(sortedPatterns, ++k);
            if (isEmpty(candidatePatterns)) {
                break;
            }
            Map<String, Pattern> patterns = selectPreventPattern(candidatePatterns, prePatterns, k);
            sortedPatterns = getSortedPatterns(patterns, k);
            prePatterns = patterns;
        }
    }

    private String getBasePath(String dataFilePath) {
        return dataFilePath.substring(0, dataFilePath.lastIndexOf("\\") + 1);
    }

    private boolean isEmpty(List<Set<String>> candidatePatterns) {
        for (Set<String> patterns : candidatePatterns) {
            if (!patterns.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private List<Set<String>> getSortedPatterns(Map<String, Pattern> patterns, int k) {
        if (patterns.isEmpty()) {
            return new ArrayList<>();
        }
        List<Set<String>> sortedPatterns = new ArrayList<>();
        List<String> keys = patterns.keySet().stream().toList();
        for (int i = 0; i < k; i++) {
            sortedPatterns.add(new HashSet<>());
        }
        for (String pattern : keys) {
            String[] split = pattern.substring(0, pattern.indexOf("-")).split(",");
            sortedPatterns.get(split.length - 1).add(pattern);
        }
        return sortedPatterns;
    }

    private Map<String, Pattern> selectPreventPattern(List<Set<String>> candidatePatterns, Map<String, Pattern> prePatterns, int k) {
        Map<String, Pattern> newPatterns = new HashMap<>();
        for (Set<String> patternsNames : candidatePatterns) {
            for (String patternName : patternsNames) {
                String dominant = patternName.substring(0, patternName.indexOf("-"));
                List<Set<String>> instances = collectNeighbors(patternName, prePatterns, k);
                List<List<String>> rowInstances = new ArrayList<>();
                if (instances != null) {
                    Point point;
                    for (String name : instances.get(0)) {
                        point = pointMap.get(name);
                        List<List<String>> lists = getIntersection(point, instances);
                        List<List<String>> res = cartesianProduct(lists);
                        rowInstances.addAll(res);
                    }
                    Pattern pattern = new Pattern(patternName, rowInstances);
                    filterRowInstances(pattern, newPatterns);
                }
            }
        }
        return newPatterns;
    }

    public static List<List<String>> cartesianProduct(List<List<String>> lists) {
        int totalCombinations = 1;
        for (List<String> list : lists) {
            totalCombinations *= list.size();
        }
        List<List<String>> result = new ArrayList<>();
        for (int i = 0; i < totalCombinations; i++) {
            List<String> combination = new ArrayList<>();
            int index = i;

            for (List<String> list : lists) {
                int listIndex = index % list.size();
                combination.add(list.get(listIndex));
                index /= list.size();
            }
            result.add(combination);
        }

        return result;
    }

    private List<List<String>> getIntersection(Point point, List<Set<String>> instances) {
        Set<String> neighbors = new HashSet<>(starNeighbors.get(point.getName()));
        List<Set<String>> lists = new ArrayList<>();
        List<List<String>> res = new ArrayList<>();
        List<String> first = new ArrayList<>();
        first.add(point.getName());
        res.add(first);
        lists.add(null);

        Set<String> intersection;
        for (int i = 1; i < instances.size(); i++) {
            intersection = new HashSet<>(instances.get(i));
            intersection.retainAll(neighbors);
            lists.add(intersection);
        }
        return res;
    }

    private List<Set<String>> collectNeighbors(String patternName, Map<String, Pattern> prePatterns, int k) {
        Map<String, Set<String>> instancesMap = new HashMap<>();
        Set<String> intersect = intersect(patternName, prePatterns, k);
        Point point;
        for (String name : intersect) {
            point = pointMap.get(name);
            if (!instancesMap.containsKey(point.getType())) {
                instancesMap.put(point.getType(), new HashSet<>());
                instancesMap.get(point.getType()).add(name);
            } else {
                instancesMap.get(point.getType()).add(name);
            }
        }
        List<Set<String>> instances = new ArrayList<>();
        String[] split = patternName.replace("-", ",").split(",");
        for (String type : split) {
            Set<String> points = instancesMap.get(type);
            if (points == null || points.isEmpty()) {
                return null;
            }
            instances.add(points);
        }
        return instances;
    }

    public Set<String> intersect(String patternName, Map<String, Pattern> prePatterns, int k) {
        Set<String> set = new HashSet<>();
        List<String> subSet = getSubSet(patternName);
        Map<String, Set<String>> instances = new HashMap<>();
        for (String key : subSet) {
            Pattern pattern = prePatterns.get(key);
            Set<String> pointNames = new HashSet<>();
            if (pattern != null) {
                pattern.getRowInstances().forEach(pointNames::addAll);
            }
            instances.put(key, pointNames);
        }
        if (k == 3) {
            for (String key : instances.keySet()) {
                set.addAll(instances.get(key));
            }
            return set;
        }
        for (int i = 0; i < subSet.size(); i++) {
            Set<String> set1 = instances.get(subSet.get(i));
            Set<String> res;
            for (int j = i + 1; j < subSet.size(); j++) {
                Set<String> set2 = instances.get(subSet.get(j));
                res = new HashSet<>(set1);
                res.retainAll(set2);
                set.addAll(res);
            }
        }
        return set;
    }

    public List<String> getSubSet(String pattern) {
        List<String> subSet = new ArrayList<>();
        String[] split = pattern.split("-");
        String[] dominant = split[0].split(",");
        String[] dominated = split[1].split(",");
        StringBuilder builder;
        if (dominant.length > 1) {
            for (int i = 0; i < dominant.length; i++) {
                builder = new StringBuilder();
                for (int j = 0; j < dominant.length; j++) {
                    if (i != j) {
                        builder.append(dominant[j]).append(",");
                    }
                }
                builder.delete(builder.length() - 1, builder.length());
                builder.append("-").append(split[1]);
                subSet.add(builder.toString());
            }
        }
        if (dominated.length > 1) {
            for (int i = 0; i < dominated.length; i++) {
                builder = new StringBuilder(split[0]);
                builder.append("-");
                for (int j = 0; j < dominated.length; j++) {
                    if (i != j) {
                        builder.append(dominated[j]).append(",");
                    }
                }
                builder.delete(builder.length() - 1, builder.length());
                subSet.add(builder.toString());
            }
        }
        return subSet;
    }


    private void filterRowInstances(Pattern pattern, Map<String, Pattern> newPatterns) {
        String[] split = pattern.getName().split("-");
        int dominantLen = split[0].split(",").length;
        List<List<String>> rowInstances = new ArrayList<>();
        for (List<String> row : pattern.getRowInstances()) {
            Point firstPoint = pointMap.get(row.get(0));
            RowInstance rowInstance = new RowInstance(split[0], null, firstPoint.getStartSlot(), getTargetSlot(firstPoint));
            if (!isCorrectSlot(rowInstance, row)) {
                continue;
            }
            if (isContain(dominantLen, row)) {
                rowInstances.add(row);
            }
        }
        pattern.setRowInstances(rowInstances);
        double PI = calPI(pattern);
        if (PI >= minPI) {
            pattern.setPI(PI);
            newPatterns.put(pattern.getName(), pattern);
//                System.out.println(pattern.getName() + ":" + PI);
        }
    }

    private boolean isCorrectSlot(RowInstance rowInstance, List<String> row) {
        for (String name : row) {
            Point point = pointMap.get(name);
            if (rowInstance.getDominant().contains(point.getType())) {
                if (point.getStartSlot() != rowInstance.getDominantSlot()) {
                    return false;
                }
                if (point.getEndSlot() != -1 && point.getEndSlot() < rowInstance.getMaxSlot()) {
                    rowInstance.setMaxSlot(point.getEndSlot());
                }
            } else {
                if (point.getStartSlot() > rowInstance.getMaxSlot() || point.getStartSlot() == rowInstance.getDominantSlot()) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isContain(int dominantLen, List<String> rowInstance) {
        int index = 0;
        while (index < dominantLen) {
            String name = rowInstance.get(index++);
            List<String> neighbors = starNeighbors.get(name);
            for (int i = index; i < rowInstance.size(); i++) {
                if (!neighbors.contains(rowInstance.get(i))) {
                    return false;
                }
            }
        }
        while (index < rowInstance.size()) {
            String thisName = rowInstance.get(index++);
            List<String> thisNeighbors = starNeighbors.get(thisName);
            for (int i = index; i < rowInstance.size(); i++) {
                String nextName = rowInstance.get(i);
                List<String> nextNeighbors = starNeighbors.get(nextName);
                if ((thisNeighbors != null && !thisNeighbors.contains(nextName)) &&
                        (nextNeighbors != null && !nextNeighbors.contains(thisName))) {
                    return false;
                }
            }
        }
        return true;
    }

    private int getTargetSlot(Point point) {
        int targetSlot = point.getStartSlot() + span;
        if (targetSlot > slotNum) {
            targetSlot = slotNum;
        }
        if (point.getEndSlot() != -1 && point.getEndSlot() < targetSlot) {
            targetSlot = point.getEndSlot();
        }
        return targetSlot;
    }

    public List<Set<String>> genCandidatePattern(List<Set<String>> sortedPatterns, int k) {
        List<Set<String>> candidatePatterns = new ArrayList<>();
        for (int i = 0; i < (k - 1); i++) {
            candidatePatterns.add(new HashSet<>());
        }
        Set<String> prePatterns = new HashSet<>();
        sortedPatterns.forEach(prePatterns::addAll);
        for (Set<String> sortedPattern : sortedPatterns) {
            List<String> patterns = sortedPattern.stream().toList();
            for (int i = 0; i < patterns.size(); i++) {
                String pattern1 = patterns.get(i);
                String[] types1 = pattern1.split("-");
                for (int j = i + 1; j < patterns.size(); j++) {
                    String pattern2 = patterns.get(j);
                    String[] types2 = pattern2.split("-");
                    String pattern = "";
                    if (types1[0].equals(types2[0])) {
                        pattern = connectDominated(types1, types2);
                    }
                    if (types1[1].equals(types2[1])) {
                        pattern = connectDominant(types1, types2);
                    }
                    if (pattern != null && !pattern.isEmpty()) {
                        String substring = pattern.substring(0, pattern.indexOf("-"));
                        int index = substring.split(",").length;
                        if (isPruned(pattern, prePatterns)) {
                            candidatePatterns.get(index - 1).add(pattern);
                        }
                    }
                }
            }
        }
        return candidatePatterns;
    }

    private boolean isPruned(String pattern, Set<String> prePatterns) {
        boolean flag = true;
        String[] split = pattern.split("-");
        String[] dominant = split[0].split(",");
        String[] dominated = split[1].split(",");
        StringBuilder builder;
        if (dominant.length > 1) {
            for (int i = 0; i < dominant.length; i++) {
                builder = new StringBuilder();
                for (int j = 0; j < dominant.length; j++) {
                    if (i != j) {
                        builder.append(dominant[j]).append(",");
                    }
                }
                builder.delete(builder.length() - 1, builder.length());
                builder.append("-").append(split[1]);
                if (!prePatterns.contains(builder.toString())) {
                    flag = false;
                    break;
                }
            }
        }
        if (dominated.length > 1) {
            for (int i = 0; i < dominated.length; i++) {
                builder = new StringBuilder(split[0]);
                builder.append("-");
                for (int j = 0; j < dominated.length; j++) {
                    if (i != j) {
                        builder.append(dominated[j]).append(",");
                    }
                }
                builder.delete(builder.length() - 1, builder.length());
                if (!prePatterns.contains(builder.toString())) {
                    flag = false;
                    break;
                }
            }
        }
        return flag;
    }

    private String connectDominant(String[] types1, String[] types2) {
        String dominant;
        if (!types1[0].contains(",")) {
            if (types1[0].compareTo(types2[0]) < 0) {
                dominant = types1[0] + "," + types2[0];
            } else {
                dominant = types2[0] + "," + types1[0];
            }
            return dominant + "-" + types1[1];
        }
        int index = types1[0].lastIndexOf(",");
        String prefix1 = types1[0].substring(0, index);
        String prefix2 = types2[0].substring(0, index);
        String suffix1 = types1[0].substring(index + 1);
        String suffix2 = types2[0].substring(index + 1);
        if (prefix1.equals(prefix2)) {
            dominant = prefix1;
            if (suffix1.compareTo(suffix2) < 0) {
                dominant += "," + suffix1 + "," + suffix2;
            } else {
                dominant += "," + suffix2 + "," + suffix1;
            }
            return dominant + "-" + types1[1];
        }
        return null;
    }

    private String connectDominated(String[] types1, String[] types2) {
        String dominated;
        if (!types1[1].contains(",")) {
            if (types1[1].compareTo(types2[1]) < 0) {
                dominated = types1[1] + "," + types2[1];
            } else {
                dominated = types2[1] + "," + types1[1];
            }
            return types1[0] + "-" + dominated;
        }
        int index = types1[1].lastIndexOf(",");
        String prefix1 = types1[1].substring(0, index);
        String prefix2 = types2[1].substring(0, index);
        String suffix1 = types1[1].substring(index + 1);
        String suffix2 = types2[1].substring(index + 1);
        if (prefix1.equals(prefix2)) {
            dominated = prefix1;
            if (suffix1.compareTo(suffix2) < 0) {
                dominated += "," + suffix1 + "," + suffix2;
            } else {
                dominated += "," + suffix2 + "," + suffix1;
            }
            return types1[0] + "-" + dominated;
        }
        return null;
    }

    private void initializeData(String dataFilePath, String neighborsFilePath) {
        File file;
        BufferedReader br;
        try {
            file = new File(dataFilePath);
            br = new BufferedReader(new FileReader(file));
            String line;
            Point point;
            while ((line = br.readLine()) != null) {
                String[] row = line.split(",");
                if (Integer.parseInt(row[4]) > slotNum) {
                    continue;
                }
                point = new Point(row[0], row[1], Integer.parseInt(row[4]), Integer.parseInt(row[5]));
                incrementedPoints.put(point.getType(), incrementedPoints.getOrDefault(point.getType(), 0) + 1);
                pointMap.put(point.getName(), point);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        file = new File(neighborsFilePath);
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                String[] row = line.split(" ");
                if (Double.parseDouble(row[2]) > minDistance) {
                    continue;
                }
                if (!starNeighbors.containsKey(row[0])) {
                    List<String> list = new ArrayList<>();
                    list.add(row[1]);
                    starNeighbors.put(row[0], list);
                } else {
                    starNeighbors.get(row[0]).add(row[1]);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Pattern> genSecondOrder() {
        Map<String, Pattern> patterns = new HashMap<>();
        for (String key : starNeighbors.keySet()) {
            List<String> neighbors = starNeighbors.get(key);
            Point dominant = pointMap.get(key);
            for (String name : neighbors) {
                Point neighbor = pointMap.get(name);
                if (neighbor == null || dominant.getStartSlot() == neighbor.getStartSlot()) {
                    continue;
                }
                String pattern = dominant.getType() + "-" + neighbor.getType();
                List<String> rowInstance = new ArrayList<>();
                rowInstance.add(dominant.getName());
                rowInstance.add(neighbor.getName());
                if (!patterns.containsKey(pattern)) {
                    List<List<String>> rowInstances = new ArrayList<>();
                    rowInstances.add(rowInstance);
                    patterns.put(pattern, new Pattern(pattern, rowInstances));
                } else {
                    patterns.get(pattern).getRowInstances().add(rowInstance);
                }

            }
        }

        patterns = patterns.entrySet().stream()
                .filter(entry -> {
                    Pattern pattern = entry.getValue();
                    double PI = calPI(pattern);
                    if (PI >= minPI) {
                        pattern.setPI(PI);
                        return true;
                    }
                    return false;
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return patterns;
    }

    private double calPI(Pattern pattern) {
        String patternName = pattern.getName().replace("-", ",");
        String[] types = patternName.split(",");
        Map<String, Set<String>> countMap = new HashMap<>();
        for (String type : types) {
            countMap.put(type, new HashSet<>());
        }
        Point point;
        for (List<String> rowInstance : pattern.getRowInstances()) {
            for (String name : rowInstance) {
                point = pointMap.get(name);
                countMap.get(point.getType()).add(point.getName());
            }
        }
        double PI = Double.MAX_VALUE;
        for (String type : types) {
            double thePI = (double) countMap.get(type).size() / incrementedPoints.get(type);
            PI = Math.min(PI, thePI);
        }
        return PI;
    }

    private void writeToFile(String filename, Map<String, Pattern> prePatterns) {
        if (prePatterns.isEmpty()) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (String key : prePatterns.keySet().stream().sorted().toList()) {
            Pattern pattern = prePatterns.get(key);
            sb.append(key).append(" ").append(pattern.getPI()).append(" ").append(pattern.getRowInstances().size()).append("\n");
        }
        writeToContent(filename, sb.toString());
    }

    private void writeToContent(String filePath, String content) {
        try {
            FileWriter fileWriter = new FileWriter(filePath);
            fileWriter.write(content);
            fileWriter.close();
            System.out.println(filePath + "文件写入成功！");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}