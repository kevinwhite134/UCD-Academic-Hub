package ie.ucd.gpa;

import java.util.List;
import java.util.Map;

final class UcdGradeData {
    static final List<String> LETTER_BASES = List.of("A", "B", "C", "D", "E", "F", "G", "NM", "ABS");
    static final List<String> LETTER_SUFFIXES = List.of("+", "", "-");

    static final Map<String, Double> GRADE_POINTS = Map.ofEntries(
            Map.entry("A+", 4.2),
            Map.entry("A", 4.0),
            Map.entry("A-", 3.8),
            Map.entry("B+", 3.6),
            Map.entry("B", 3.4),
            Map.entry("B-", 3.2),
            Map.entry("C+", 3.0),
            Map.entry("C", 2.8),
            Map.entry("C-", 2.6),
            Map.entry("D+", 2.4),
            Map.entry("D", 2.2),
            Map.entry("D-", 2.0),
            Map.entry("E+", 0.0),
            Map.entry("E", 0.0),
            Map.entry("E-", 0.0),
            Map.entry("F+", 0.0),
            Map.entry("F", 0.0),
            Map.entry("F-", 0.0),
            Map.entry("G+", 0.0),
            Map.entry("G", 0.0),
            Map.entry("G-", 0.0),
            Map.entry("NM", 0.0),
            Map.entry("ABS", 0.0)
    );

    static final List<GradeScale> SCALES = List.of(
            new GradeScale(
                    "standard40",
                    "Standard 40% Pass",
                    "(Default) 40% Pass Standard Letter/Numeric",
                    List.of(
                            band("A+", 90, 100),
                            band("A", 80, 90),
                            band("A-", 70, 80),
                            band("B+", 66.67, 70),
                            band("B", 63.33, 66.67),
                            band("B-", 60, 63.33),
                            band("C+", 56.67, 60),
                            band("C", 53.33, 56.67),
                            band("C-", 50, 53.33),
                            band("D+", 46.67, 50),
                            band("D", 43.33, 46.67),
                            band("D-", 40, 43.33),
                            band("E+", 36.67, 40),
                            band("E", 33.33, 36.67),
                            band("E-", 30, 33.33),
                            band("F+", 26.67, 30),
                            band("F", 23.33, 26.67),
                            band("F-", 20, 23.33),
                            band("G+", 16.67, 20),
                            band("G", 13.33, 16.67),
                            band("G-", 0.01, 13.33)
                    )
            ),
            new GradeScale(
                    "linear40",
                    "Alternative Linear 40% Pass",
                    "40% Pass Linear Letter/Numeric",
                    List.of(
                            band("A+", 95, 100),
                            band("A", 90, 95),
                            band("A-", 85, 90),
                            band("B+", 80, 85),
                            band("B", 75, 80),
                            band("B-", 70, 75),
                            band("C+", 65, 70),
                            band("C", 60, 65),
                            band("C-", 55, 60),
                            band("D+", 50, 55),
                            band("D", 45, 50),
                            band("D-", 40, 45),
                            band("E+", 35, 40),
                            band("E", 30, 35),
                            band("E-", 25, 30),
                            band("F+", 20, 25),
                            band("F", 15, 20),
                            band("F-", 10, 15),
                            band("G+", 5, 10),
                            band("G", 0.02, 5),
                            band("G-", 0.01, 0.02)
                    )
            ),
            new GradeScale(
                    "nonLinear50",
                    "Alternative Non-Linear 50% Pass",
                    "50% Pass Standard Letter/Numeric",
                    List.of(
                            band("A+", 90, 100),
                            band("A", 80, 90),
                            band("A-", 70, 80),
                            band("B+", 67.78, 70),
                            band("B", 65.56, 67.78),
                            band("B-", 63.33, 65.56),
                            band("C+", 61.12, 63.33),
                            band("C", 58.89, 61.12),
                            band("C-", 56.67, 58.89),
                            band("D+", 54.43, 56.67),
                            band("D", 52.22, 54.43),
                            band("D-", 50, 52.22),
                            band("E+", 45, 50),
                            band("E", 40, 45),
                            band("E-", 35, 40),
                            band("F+", 30, 35),
                            band("F", 25, 30),
                            band("F-", 20, 25),
                            band("G+", 15, 20),
                            band("G", 10, 15),
                            band("G-", 0.01, 10)
                    )
            )
    );

    private UcdGradeData() {
    }

    static GradeScale scaleById(String id) {
        return SCALES.stream()
                .filter(scale -> scale.id().equals(id))
                .findFirst()
                .orElse(SCALES.getFirst());
    }

    static double gradePoint(String grade) {
        return GRADE_POINTS.getOrDefault(grade, 0.0);
    }

    private static GradeBand band(String grade, double lowerInclusive, double upperExclusive) {
        return new GradeBand(grade, lowerInclusive, upperExclusive);
    }
}
