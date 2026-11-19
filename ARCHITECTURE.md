# ARCHITECTURE.md — Архитектура приложения Yomizora

## Обзор

**Yomizora (読み空)** — полностью автономное Android-приложение для изучения японского языка через чтение текстов с фуриганой и проверкой понимания.

Проект построен по принципам **Clean Architecture** с чётким разделением слоёв:

- **`domain/`** — бизнес-модели (`JlptLevel`, `ReadingMaterial`, `Paragraph`, `TextSegment`, `ComprehensionQuestion`, `MaterialStatus`, `ReaderSettings`, `DictionaryEntry`) и интерфейсы репозиториев (`ContentRepository`, `ProgressRepository`, `SettingsRepository`).
- **`data/local/content/`** — база данных контента `ContentDatabase` (`yomizora_content.db`): таблицы `materials`, `dictionary`, `content_packs`.
- **`data/local/progress/`** — база данных прогресса пользователя `UserProgressDatabase` (`yomizora_progress.db`): таблицы `material_progress`, `saved_words`, `study_activity`, `user_state`.
- **`data/content/`** — подсистема управления контентом: `ContentPackageDto`, `ContentValidator`, `ContentImporter`, `ContentBootstrapManager`.
- **`data/repository/`** — реализации репозиториев, включающие логику последовательной разблокировки материалов и независимого расчёта прогресса по уровням.
- **`di/`** — модули Hilt (`DatabaseModule`, `RepositoryModule`).
- **`ui/`** — интерфейс на Jetpack Compose + Material 3:
  - `theme/` — светлая тема «Васи» (`Washi`) и тёмная тема «Суми» (`Sumi`).
  - `components/FuriganaText.kt` — рендеринг фуриганы строго над соответствующими иероглифами с атомарным переносом слов (`FlowRow`) и поддержкой режимов отображения.
  - `home/`, `library/`, `reading/`, `quiz/`, `favorites/`, `settings/`, `navigation/`.

## Ключевые архитектурные решения

1. **Две независимые базы Room (`ContentDatabase` и `UserProgressDatabase`)**:
   - Обновление контентных пакетов или добавление новых текстов никогда не затрагивает прогресс пользователя, избранное, сохранённые слова и статистику.
   - Сброс прогресса одного уровня (`resetLevelProgress`) затрагивает только записи этого уровня и сохраняет избранное и прогресс других уровней.
2. **Строгий контроль последовательного открытия**:
   - Статус каждого материала (`LOCKED`, `AVAILABLE`, `IN_PROGRESS`, `COMPLETED`) вычисляется динамически в `ProgressRepositoryImpl` на основе `orderIndex` и состояния предыдущего материала.
   - И `ReadingViewModel`, и `QuizViewModel` проверяют статус перед отображением, исключая обход блокировки через прямую навигацию или перезапуск.
3. **Масштабируемость уровней JLPT**:
   - `JlptLevel` уже включает `N5`, `N4`, `N3`, `N2`, `N1`. Новые уровни и категории добавляются через JSON-пакеты контента без переработки базы данных и навигации.
