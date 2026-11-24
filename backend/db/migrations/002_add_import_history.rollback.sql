-- Откат удаляет историю импорта и назначения администраторов.
-- Сначала остановить приложение и сохранить резервную копию.
DROP TABLE import_operations;
ALTER TABLE users DROP COLUMN admin;
