# UCD Academic Hub

JavaFX desktop dashboard for tracking UCD modules, assessments, deadlines, tasks and GPA.

## Run

```powershell
mvn javafx:run
```

You can also open the folder in IntelliJ and run `ie.ucd.gpa.UcdGpaCalculatorApp`.

## What it does

- Opens on a dashboard with current GPA, modules passed, assessment progress and the next deadline.
- Lets you create, edit, delete and reorder modules.
- Stores module code, description, colour, pass grade, credits, semester and academic year.
- Lets you add weighted assessments with due dates, completion status, grades, notes and links.
- Calculates completion percentage, secured grade, pass status and target-grade requirements.
- Warns when assessment weights do not add up cleanly to 100%.
- Provides academic task cards with module links, priority, due date, completion status and custom URL buttons.
- Includes a calendar view that shows assessments and tasks due on a selected date.
- Keeps the original UCD GPA calculator as a dedicated hub option.
- Autosaves local data between sessions.

## Local Data

The app stores Academic Hub data in:

```text
%USERPROFILE%\.ucd-academic-hub\academic-hub.properties
```

No account, server or cloud connection is required.

## Current Notes

The PID proposes Python, PySide6 and SQLite. This implementation keeps the existing JavaFX project and evolves it into the dashboard first, so the current calculator remains usable while the hub grows. SQLite can be added later if you want the persistence layer to match the PID exactly.

## Original GPA Sources

- UCD Registry Grades: https://www.ucd.ie/registry/staff/registryservices/assessment/gradingsupport/grades/
- Standard Conversion Grade Scale 40% Pass: https://www.ucd.ie/registry/t4media/Standard_Conversion_Grade_Scale_40_Pass.pdf
- Alternative Linear Conversion Grade Scale 40% Pass: https://www.ucd.ie/registry/t4media/Alternative%20Linear%20Conversion%20Grade%20Scale%2040%20per%20cent%20Pass.pdf
- Alternative Non-Linear Conversion Grade Scale 50% Pass: https://www.ucd.ie/registry/t4media/Alternative%20Non-Linear%20Conversion%20Grade%20Scale%2050%20percent.pdf
