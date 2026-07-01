package com.architectureworkbench.core.model.capture;

/** Canvas coordinates from the native Event Storm board. */
public class CanvasPosition {
    private double x;
    private double y;

    public CanvasPosition() {}
    public CanvasPosition(double x, double y) { this.x = x; this.y = y; }

    public double getX() { return x; }
    public void setX(double x) { this.x = x; }
    public double getY() { return y; }
    public void setY(double y) { this.y = y; }
}
