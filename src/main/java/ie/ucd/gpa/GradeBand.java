package ie.ucd.gpa;

record GradeBand(String grade, double lowerInclusive, double upperExclusive) {
    boolean contains(double percentage) {
        if (percentage == 100.0 && upperExclusive == 100.0) {
            return percentage >= lowerInclusive;
        }
        return percentage >= lowerInclusive && percentage < upperExclusive;
    }

    String rangeText() {
        if (upperExclusive == 100.0) {
            return ">=" + format(lowerInclusive) + " to 100";
        }
        return ">=" + format(lowerInclusive) + " to <" + format(upperExclusive);
    }

    private static String format(double value) {
        if (value == Math.rint(value)) {
            return String.valueOf((int) value);
        }
        return String.format("%.2f", value);
    }
}
