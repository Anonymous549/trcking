# Database Documentation — LIFE OS 365

LIFE OS 365 uses a single local **Room SQLite** database (`life_os_365.db`, schema version 1) with strict foreign keys and indexes.

## Core Tables

| Table | Purpose | Key Foreign Keys & Indexes |
| --- | --- | --- |
| `habits` | Habit definitions & schedules | Indexed: `category`, `isArchived` |
| `habit_logs` | Daily completion / rest-day / missed records | FK `habitId -> habits(id)` CASCADE; Unique index `(habitId, epochDay)` |
| `tasks` | To-do items, priorities, deadlines & recurrence | Indexed: `dueEpochDay`, `isCompleted`, `isTopThree` |
| `routine_blocks` | Customizable daily schedule blocks (8 AM starter) | Indexed: `startMinuteOfDay`, `sortOrder` |
| `study_subjects` | Courses, weekly hour targets & exam dates | Indexed: `name` |
| `study_topics` | Chapters & spaced-revision dates | FK `subjectId -> study_subjects(id)` CASCADE |
| `study_sessions` | Pomodoro & deep-work session logs | FK `subjectId -> study_subjects(id)` CASCADE |
| `career_items` | Skills, career goals, projects & assignments | Indexed: `itemType`, `isCompleted` |
| `exercise_templates` | Built-in & custom exercise library | Indexed: `muscleGroup` |
| `workout_logs` | Logged workout sessions | Indexed: `epochDay` |
| `workout_exercises` | Sets, reps & weights per workout | FK `workoutId -> workout_logs(id)` CASCADE |
| `health_metrics` | Daily steps, weight, measurements, water, nutrition, sleep | PK: `epochDay` |
| `finance_categories` | Income & expense categories + monthly budgets | Indexed: `type` |
| `finance_transactions` | Local income/expense ledger (encrypted notes) | FK `categoryId -> finance_categories(id)` CASCADE |
| `savings_goals` | Savings targets & current progress | PK: `id` |
| `life_goals` | Daily/Weekly/Monthly/Yearly/Short/Long-term goals | Indexed: `horizon`, `isCompleted` |
| `goal_milestones` | Checkable milestones synced to goal progress | FK `goalId -> life_goals(id)` CASCADE |
| `journal_entries` | AES-GCM encrypted mood, gratitude, reflections & lessons | Indexed: `epochDay` |
| `books` | Reading list, pages read & notes | Indexed: `status` |
| `learning_resources` | Articles, courses, videos & study notes | Indexed: `resourceType` |
| `screen_time_logs` | Daily manual or UsageStats screen-time logs | PK: `epochDay` |
