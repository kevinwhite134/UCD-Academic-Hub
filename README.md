# UCD GPA Calculator

JavaFX GPA calculator for UCD grades.

## Run

```powershell
mvn javafx:run
```

You can also open the folder in IntelliJ and run `ie.ucd.gpa.UcdGpaCalculatorApp`.

## Run From A Folder

The `dist` folder contains a copy you can run outside IntelliJ:

```text
dist\Run UCD GPA Calculator.bat
```

Keep the `.bat` file, the app `.jar`, and the `lib` folder together.

## What it does

- Starts with 10 large circles so you can choose 1 to 10 subjects.
- Shows one subject card per subject.
- Percentage mode lets each subject use one of three UCD conversion scales:
  - Standard Conversion Grade Scale 40% Pass
  - Alternative Linear Conversion Grade Scale 40% Pass
  - Alternative Non-Linear Conversion Grade Scale 50% Pass
- Letter-grade mode skips percentages and lets you choose a letter plus `+`, plain, or `-`.
- Displays the calculated grade, grade point, overall GPA, all conversion scales, and the UCD grade-point table.

## Sources

- UCD Registry Grades: https://www.ucd.ie/registry/staff/registryservices/assessment/gradingsupport/grades/
- Standard Conversion Grade Scale 40% Pass: https://www.ucd.ie/registry/t4media/Standard_Conversion_Grade_Scale_40_Pass.pdf
- Alternative Linear Conversion Grade Scale 40% Pass: https://www.ucd.ie/registry/t4media/Alternative%20Linear%20Conversion%20Grade%20Scale%2040%20per%20cent%20Pass.pdf
- Alternative Non-Linear Conversion Grade Scale 50% Pass: https://www.ucd.ie/registry/t4media/Alternative%20Non-Linear%20Conversion%20Grade%20Scale%2050%20percent.pdf
