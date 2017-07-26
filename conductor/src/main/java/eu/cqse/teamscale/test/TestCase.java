package eu.cqse.teamscale.test;

public class TestCase {
    public String setName;
    public String testName;
    public String uniformPath;
    public long duration;
    public int round;
    public int minTimesCovered;

    @Override
    public String toString() {
        return "\n" + setName + '.' + testName + '\'' + ", round=" + round + ", minTimesCovered=" + minTimesCovered;
    }
}
