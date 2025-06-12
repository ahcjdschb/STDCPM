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
public class STDCPMDPR {
    int slotNum;
    int span;
    double minDistance;
    double minPI;
    double delta;
    Map<String, List<String>> instancesNeighbors;
    Map<String, Integer> incrementedPoints;
    Map<String, Point> pointMap;

    public STDCPMDPR(int slotNum, int span, double minDistance, double minPI) {
        this.slotNum = slotNum;
        this.span = span;
        this.minDistance = minDistance;
        this.minPI = minPI;
        instancesNeighbors = new HashMap<>();
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
        prePatterns = processSecondOrder(prePatterns);
        List<Set<String>> sortedPatterns = new ArrayList<>();
        sortedPatterns.add(new HashSet<>(prePatterns.keySet()));
        int k = 2;
        while (true) {
            writeToFile(basePath + k + "-size patterns.txt", prePatterns);
            if (isEmpty(sortedPatterns)) {
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

    private Map<String, Pattern> processSecondOrder(Map<String, Pattern> prePatterns) {
        Iterator<Map.Entry<String, Pattern>> iterator = prePatterns.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Pattern> entry = iterator.next();
            String patternName = entry.getKey();
            Pattern reversePattern = prePatterns.get(getReversePattern(patternName));
            if (reversePattern!= null) {
                if (entry.getValue().getPI() <= reversePattern.getPI()) {
                    iterator.remove();
                }
            }
        }
        return prePatterns;
    }

    private String getReversePattern(String patternName) {
        String[] split = patternName.split("-");
        StringBuilder result = new StringBuilder();
        for (int i = split.length - 1; i >= 0; i--) {
            result.append(split[i]).append("-");
        }
        result = new StringBuilder(result.substring(0, result.length() - 1));
        return result.toString();
    }

    private String getBasePath(String dataFilePath) {
        return dataFilePath.substring(0, dataFilePath.lastIndexOf("\\") + 1);
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
        List<String> candidates = new ArrayList<>();
        candidatePatterns.forEach(candidates::addAll);
        for (String patternName : candidates) {
            List<List<String>> instances = collectNeighbors(patternName, prePatterns, k);
            if (instances != null) {
                List<List<String>> rowInstances = searchRowInstance(patternName, instances);
                Pattern pattern = new Pattern(patternName, rowInstances);
                System.out.println(patternName + ": " + rowInstances.size());
                double PI = calPI(pattern);
                if (PI >= minPI) {
                    pattern.setPI(PI);
                    newPatterns.put(patternName, pattern);
                }
            }
        }
        return newPatterns;
    }

    private List<List<String>> searchRowInstance(String pattern, List<List<String>> instances) {
        String dominant = pattern.substring(0, pattern.indexOf("-"));
        List<List<String>> rowInstances = new ArrayList<>();
        RowInstance rowInstance;
        List<String> points = instances.get(0);
        Point point;
        for (String pointName : points) {
            point = pointMap.get(pointName);
            List<String> list = new ArrayList<>();
            list.add(pointName);
            rowInstance = new RowInstance(dominant, list, point.getStartSlot(), getTargetSlot(point));
            List<List<String>> lists = getIntersection(point, instances, dominant.split(",").length);
            backTracking(rowInstance, lists, rowInstances, 1);
        }
        return rowInstances;
    }

    private List<List<String>> getIntersection(Point point, List<List<String>> instances, int len) {
        Set<String> neighbors = new HashSet<>(instancesNeighbors.get(point.getName()));
        List<List<String>> lists = new ArrayList<>();
        List<List<String>> res = new ArrayList<>();
        res.add(null);
        lists.add(null);

        for (int i = 1; i < instances.size(); i++) {
            Set<String> intersection = new HashSet<>(instances.get(i));
            intersection.retainAll(neighbors);
            lists.add(intersection.stream().toList());
        }
        for (int i = 1; i < len; i++) {
            List<String> list = new ArrayList<>();
            for (String name : lists.get(i)) {
                if (pointMap.get(name).getStartSlot() == point.getStartSlot()) {
                    list.add(name);
                }
            }
            res.add(list);
        }
        for (int i = len; i < lists.size(); i++) {
            List<String> list = new ArrayList<>();
            for (String name : lists.get(i)) {
                int slot = pointMap.get(name).getStartSlot();
                if (point.getStartSlot() < slot && point.getStartSlot() + span >= slot) {
                    list.add(name);
                }
            }
            res.add(list);
        }
        return res;
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

    private void backTracking(RowInstance rowInstance, List<List<String>> instances, List<List<String>> rowInstances, int level) {
        if (level == instances.size()) {
            rowInstances.add(new ArrayList<>(rowInstance.getPoints()));
            return;
        }
        List<String> pointNames = instances.get(level);
        for (String pointName : pointNames) {
            Point point = pointMap.get(pointName);
            if (rowInstance.getDominant().contains(point.getType())) {
                if (isDominant(point, rowInstance)) {
                    rowInstance.getPoints().add(pointName);
                    if (point.getEndSlot() != -1 && point.getEndSlot() < rowInstance.getMaxSlot()) {
                        rowInstance.setMaxSlot(point.getEndSlot());
                    }
                } else {
                    continue;
                }
            } else {
                if (isDominated(point, rowInstance)) {
                    rowInstance.getPoints().add(pointName);
                } else {
                    continue;
                }
            }
            backTracking(rowInstance, instances, rowInstances, level + 1);
            // 回溯
            rowInstance.getPoints().remove(rowInstance.getPoints().size() - 1);
        }
    }

    private boolean isDominated(Point point, RowInstance rowInstance) {
        if (point.getStartSlot() > rowInstance.getMaxSlot() || point.getStartSlot() == rowInstance.getDominantSlot()) {
            return false;
        }
        List<String> points = rowInstance.getPoints();
        String dominantName;
        int len = rowInstance.getDominant().split(",").length;
        for (int i = 0; i < len; i++) {
            dominantName = points.get(i);
            List<String> neighbors = instancesNeighbors.get(dominantName);
            if (!neighbors.contains(point.getName())) {
                return false;
            }
        }
        List<String> pointNeighbors = instancesNeighbors.get(point.getName());
        if (points.size() > len) {
            String dominatedName;
            for (int i = len; i < points.size(); i++) {
                dominatedName = points.get(i);
                List<String> neighbors = instancesNeighbors.get(dominatedName);
                if (neighbors != null && neighbors.contains(point.getName())) {
                    continue;
                }
                if (pointNeighbors != null && pointNeighbors.contains(dominatedName)) {
                    continue;
                }
                return false;
            }
        }
        return true;
    }

    private boolean isDominant(Point point, RowInstance rowInstance) {
        if (point.getStartSlot() != rowInstance.getDominantSlot()) {
            return false;
        }
        List<String> points = rowInstance.getPoints();
        for (String prePointName : points) {
            List<String> neighbors = instancesNeighbors.get(prePointName);
            if (!neighbors.contains(point.getName())) {
                return false;
            }
        }
        return true;
    }

    private List<List<String>> collectNeighbors(String patternName, Map<String, Pattern> prePatterns, int k) {
        Map<String, List<String>> instancesMap = new HashMap<>();
        Set<String> intersect = intersect(patternName, prePatterns, k);
        Point point;
        for (String name : intersect) {
            point = pointMap.get(name);
            if (!instancesMap.containsKey(point.getType())) {
                instancesMap.put(point.getType(), new ArrayList<>());
                instancesMap.get(point.getType()).add(name);
            } else {
                List<String> points = instancesMap.get(point.getType());
                if (!points.contains(name)) {
                    points.add(name);
                }
            }
        }
        List<List<String>> instances = new ArrayList<>();
        String[] split = patternName.replace("-", ",").split(",");
        for (String type : split) {
            List<String> points = instancesMap.get(type);
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
            pattern.getRowInstances().forEach(pointNames::addAll);
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

    private boolean isEmpty(List<Set<String>> candidatePatterns) {
        for (Set<String> patterns : candidatePatterns) {
            if (!patterns.isEmpty()) {
                return false;
            }
        }
        return true;
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
                point = new Point(row[0], row[1], Double.parseDouble(row[2]), Double.parseDouble(row[3]), Integer.parseInt(row[4]), Integer.parseInt(row[5]));
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
                if (!instancesNeighbors.containsKey(row[0])) {
                    List<String> list = new ArrayList<>();
                    list.add(row[1]);
                    instancesNeighbors.put(row[0], list);
                } else {
                    instancesNeighbors.get(row[0]).add(row[1]);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Pattern> genSecondOrder() {
        Map<String, Pattern> patterns = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : instancesNeighbors.entrySet()) {
            String key = entry.getKey();
            List<String> neighbors = entry.getValue();
            Point dominant = pointMap.get(key);
            if (dominant == null) {
                continue;
            }
            StringBuilder sb = new StringBuilder();
            for (String name : neighbors) {
                Point neighbor = pointMap.get(name);
                if (neighbor == null || dominant.getStartSlot() == neighbor.getStartSlot()) {
                    continue;
                }
                String pattern = sb.append(dominant.getType()).append("-").append(neighbor.getType()).toString();
                List<String> rowInstance = Arrays.asList(dominant.getName(), neighbor.getName());
                patterns.computeIfAbsent(pattern, k -> new Pattern(k, new ArrayList<>())).getRowInstances().add(rowInstance);
                sb.setLength(0);
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
        Map<String, Set<String>> countMap = Arrays.stream(types)
                .collect(Collectors.toMap(type -> type, type -> new HashSet<>()));

        Point point;
        for (List<String> rowInstance : pattern.getRowInstances()) {
            for (String name : rowInstance) {
                point = pointMap.get(name);
                countMap.get(point.getType()).add(point.getName());
            }
        }
        StringBuilder sb = new StringBuilder();
        double PI = Double.MAX_VALUE;
        for (String type : types) {
            int participation = countMap.get(type).size();
            int all = incrementedPoints.get(type);
            double PR = (double) participation / all;
            PI = Math.min(PI, PR);
        }
        pattern.setPRInfo(sb.toString());
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
                        if (k == 3) {
                            candidatePatterns.get(index - 1).add(pattern);
                            continue;
                        }
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
}
