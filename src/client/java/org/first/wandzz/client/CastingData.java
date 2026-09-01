package org.first.wandzz.client;

import org.first.wandzz.magic.gesture.Point;
import java.util.ArrayList;
import java.util.List;

public class CastingData {
    static boolean casting = false;
    public static void startCasting(){
        casting = true;

    }public static void stopCasting(){
        casting = false;
    }

    public static boolean isCasting(){
        return casting;
    }

    static List<Point> points = new ArrayList<>();
    public static int getPointCount(){
        return points.size();
    }

    public static void clearPoints() {
        points.clear();
    }

    public static List<Point> getPointsCopy() {
        List<Point> copy_points = new ArrayList<>(points);

        return copy_points;
    }

    public static int getCopyCount(){
        return getPointsCopy().size();
    }
    public static void addPoint(double x, double y){
       points.add(new Point(x, y));
    }

}
