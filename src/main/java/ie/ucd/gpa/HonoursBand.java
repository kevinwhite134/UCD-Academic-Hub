package ie.ucd.gpa;

record HonoursBand(String name, double lowerInclusive, double upperInclusive) {
    boolean contains(double gpa) {
        return gpa >= lowerInclusive && gpa <= upperInclusive;
    }

    String rangeText() {
        return UcdGpaCalculatorApp.GPA_FORMAT.format(lowerInclusive)
                + " - "
                + UcdGpaCalculatorApp.GPA_FORMAT.format(upperInclusive);
    }
}
