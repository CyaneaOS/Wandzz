package org.first.wandzz.magic.gesture;

import java.util.List;

public class GestureTemplate {

    private final String name;
    private final List<Point> points;

    public GestureTemplate(String name, List<Point> points) {
        this.name = name;
        this.points = points;
    }

    public String getName() {
        return name;
    }

    public List<Point> getPoints() {
        return points;
    }
}
