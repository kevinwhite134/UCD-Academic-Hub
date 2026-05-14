package ie.ucd.gpa;

import java.util.List;

record GradeScale(String id, String displayName, String brightspaceName, List<GradeBand> bands) {
    String gradeFor(double percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new IllegalArgumentException("Percentage must be between 0 and 100.");
        }
        if (percentage == 0) {
            return "NM";
        }
        return bands.stream()
                .filter(band -> band.contains(percentage))
                .map(GradeBand::grade)
                .findFirst()
                .orElse("NM");
    }
}
