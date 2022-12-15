# SQL

## Современные функции SQL

* [x] `SUM`, `AVG`, `HAVING`: у кого средний бал за семестр >= 4.5.
* [x] `LATERAL JOIN`: вывод первых 3 студентов в каждом курсе.
* [x] `SELECT DISTINC ...`, `SELECT COUNT(DISTINCT ...)`: подсчет различных записей.
* [x] `LIMIT ... OFFSET ...`, пагинация по всем студентам, `LIMIT, ORDER BY ID, ID > ...`, пагинация по ID.
* [x] `RECURSIVE`
    * вычисление факториала;
    * планета, континенты, страны.
* [x] `INLINE VIEW`: группа и курс, упорядоченная по имени и фамилии.
* [ ] `OVER ... PARTITION BY ...`: #TODO
* [x] `FILTER (WHERE ...)`: фильтрация результата.
* [x] `SUM(CASE a > 0 THEN 1 ELSE 0)`: сложные фильтры.
* [x] `INSERT INTO ... (SELECT ... FROM ...)`, `SELECT * INTO ... FROM ...`, `CREATE TABLE ... (LIKE ...)`: копирование
  таблицы в другую таблицу.
* [x] `INSERT ... ON CONFLICT DO UPDATE / NOTHING`: upsert.
* [x] `INSERT ... RETURNING id`: возвращение id только что созданной записи.
* [x] `REFERENCES ... ON DELETE ...`: cascade обновления / удаления.
* [ ] `MATERIALIZED VIEW`.
* [ ] `SELECT FOR UPDATE`.
* [ ] `JSON`.
* [ ] `ARRAY`.

### Пример

### Создание таблиц

Студенты (`students`), Курсы (`courses`), Оценки (`course_grades`).

```postgresql
DROP TABLE IF EXISTS course_grades;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS courses;
```

* `CHECK` – проверка значения поля при вставке значения.
* `SERIAL` == `INT` + sequence.
* `PRIMARY KEY` – на primary key всегда создается unique index.
* `ON DELETE / ON UPDATE` – действие выполняемое при удалении / обновлении записи на которую ссылается Foreign Key:
    * `SET NULL / DEFAULT` – в поле устанавливается `NULL` или значение по-умолчанию.
    * `CASCADE` – удаляет строки из зависимой таблицы при удалении или изменении связанных строк в главной таблице.
    * `RESTRICT` – предотвращает какие-либо действия в зависимой таблице при удалении или изменении связанных строк в
      главной таблице.
    * `NO ACTION` – (действие по умолчанию) предотвращает какие-либо действия в зависимой таблице при удалении или
      изменении связанных строк в главной таблице и генерирует ошибку. (Главным отличием этих двух вариантов является
      то, что `NO ACTION` позволяет отложить проверку в процессе транзакции, а `RESTRICT` — нет)

```postgresql
CREATE TABLE students
(
    id        SERIAL PRIMARY KEY,
    firstname VARCHAR(80) NOT NULL,
    lastname  VARCHAR(80) NOT NULL,
    github    VARCHAR(80),
    "group"   VARCHAR(8)  NOT NULL CHECK ("group" IN ('ИУ7-11М', 'ИУ7-12М', 'ИУ7-13М'))
);

CREATE TABLE courses
(
    id   SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL
);

CREATE INDEX idx_courses_name ON courses (name);

CREATE TABLE course_grades
(
    id         SERIAL PRIMARY KEY,
    grade      NUMERIC(8, 2)
        CHECK (grade BETWEEN 2 AND 5),
    course_id  INT
        CONSTRAINT fk_course_grades_course_id REFERENCES courses (id)
            ON DELETE CASCADE,
    student_id INT
        CONSTRAINT fk_course_grades_student_id REFERENCES students (id)
            ON DELETE SET NULL
);

CREATE INDEX idx_course_grades_grade ON course_grades (grade);
CREATE INDEX idx_course_grades_course_id ON course_grades (course_id);
CREATE INDEX idx_course_grades_student_id ON course_grades (student_id);
```

### Заполнение таблиц данных

* `COPY ... FROM ... CSV` – вставка из csv файла (файл лежит внутри контейнера postgres).
* `INSERT INTO ... SELECT` – вставка в таблицу результата select.

```postgresql
COPY students (lastname, firstname, github, "group")
    FROM '/opt/data/students.csv'
    DELIMITER ';'
    CSV HEADER;

SELECT *
FROM students;

COPY courses (name)
    FROM '/opt/data/courses.csv'
    DELIMITER ';'
    CSV HEADER;

SELECT *
FROM courses;

INSERT INTO course_grades(grade, course_id, student_id)
    (SELECT FLOOR(RANDOM() * 7 + 4) * 0.5, c.id, s.id
     FROM students s
        , courses c);

SELECT c.name                           AS course
     , cg.grade                         AS grade
     , s.firstname || ' ' || s.lastname AS name
     , s."group"                        AS "group"
FROM course_grades cg
    INNER JOIN students s ON s.id = cg.student_id
    INNER JOIN courses c ON c.id = cg.course_id
WHERE c.id = 1;
```

### Агрегирующие функции

* `AVG`, `COUNT`, `SUM` – агрегирующие функции требуют группировку по другим полям в select.
* `HAVING` – фильтрация по результату агрегации.
* `DISTINCT` – удаляет из результата одинаковые записи.

Количество студентов, у которых средний бал больше 4.2.

```postgresql
SELECT s.firstname || ' ' || s.lastname AS StudentName
     , AVG(cg.grade)                    AS AverageGrade
FROM course_grades cg
    INNER JOIN students s ON s.id = cg.student_id
GROUP BY StudentName
HAVING AVG(cg.grade) > 4.2;
```

Студенты, у которых есть хотя бы одна тройка.

```postgresql
-- с использованием DISTINCT
SELECT DISTINCT s.firstname || ' ' || s.lastname AS StudentName
FROM course_grades cg
    INNER JOIN students s ON s.id = cg.student_id
WHERE cg.grade < 4;

-- с использованием GROUP BY
SELECT s.firstname || ' ' || s.lastname AS StudentName
FROM course_grades cg
    INNER JOIN students s ON s.id = cg.student_id
WHERE cg.grade < 4
GROUP BY s.id;
```

```postgresql
SELECT s.firstname || ' ' || s.lastname AS StudentName
FROM course_grades cg
    INNER JOIN students s ON s.id = cg.student_id
WHERE cg.grade < 4
GROUP BY StudentName;
```

### Subselect

Вывести всех студентов, у которых есть хотя бы одна пятерка.

```postgresql
SELECT s.firstname || ' ' || s.lastname AS StudentName
FROM students s
WHERE EXISTS(SELECT 1 FROM course_grades cg WHERE cg.student_id = s.id AND cg.grade = 5)
GROUP BY StudentName;
```

### Grouping by

The result of the `SELECT` and `WHERE` clauses are grouped separately by each specified group in the grouping set, and
aggregates functions executed for each group same as simple `GROUP BY` clauses, and then the final results are returned.

Вывести средний бал всех студентов и средний бал по группе:

```postgresql
SELECT s.firstname || ' ' || s.lastname AS "StudentName"
     , s.group                          AS "Group"
     , AVG(cg.grade)                    AS "AverageGrade"
FROM students s
    INNER JOIN course_grades cg ON s.id = cg.student_id
GROUP BY GROUPING SETS ( ("StudentName", s.group), (s.group));
```

Аналогично решению через `UNION ALL`:

```postgresql
SELECT s.firstname || ' ' || s.lastname AS "Name"
     , AVG(cg.grade)                    AS "AverageGrade"
FROM students s
    INNER JOIN course_grades cg ON s.id = cg.student_id
GROUP BY "Name"

UNION ALL

SELECT s.group       AS "Group"
     , AVG(cg.grade) AS "AverageGrade"
FROM students s
    INNER JOIN course_grades cg ON s.id = cg.student_id
GROUP BY "Group"
```

### LATERAL JOIN

`LATERAL JOIN` – subquery appearing in `FROM` can be preceded by the key word LATERAL. This allows them to reference
columns provided by preceding `FROM` items. (Without LATERAL, each subquery is evaluated independently and so cannot
cross-reference any other `FROM` item.)

A `LATERAL JOIN` is more like a correlated subquery, not a plain subquery, in that expressions to the right of
a `LATERAL JOIN` are evaluated once for each row left of it – just like a correlated subquery – while a plain subquery (
table expression) is evaluated once only.

Найти какой предмет студент сдал лучше всего.

```postgresql
SELECT s.firstname || ' ' || s.lastname                       AS StudentName
     , s."group"                                              AS "Group"
     , cg.grade                                               AS Grade
     , (SELECT name FROM courses c WHERE cg.course_id = c.id) AS Course
FROM students s
    JOIN LATERAL (SELECT *
                  FROM course_grades cg
                  WHERE s.id = cg.student_id
                  ORDER BY cg.grade DESC
                  LIMIT 1) cg
         ON TRUE
ORDER BY "Group", StudentName, Grade DESC;
```

### Pagination

Вывести список группы ИУ7-11М с пагинацией по 10 записей.

```postgresql
SELECT s.*
FROM students s
WHERE s."group" = 'ИУ7-11М'
ORDER BY s.lastname, s.firstname
LIMIT 10 OFFSET 10;
```

```postgresql
SELECT s.*
FROM students s
WHERE s."group" = 'ИУ7-11М'
  AND s.id > 10
ORDER BY s.lastname, s.firstname
LIMIT 10;
```

```postgresql
SELECT s.*
FROM students s
WHERE s."group" = 'ИУ7-11М'
ORDER BY s.lastname, s.firstname
    FETCH FIRST 10 ROWS ONLY
OFFSET 10 ROWS;

```

### Inline View

```postgresql
WITH student_avg_grade AS (SELECT s.firstname || ' ' || s.lastname AS student_name
                                , AVG(cg.grade)                    AS average_grade
                           FROM course_grades cg
                               INNER JOIN students s ON s.id = cg.student_id
                           GROUP BY student_name)
SELECT *
FROM student_avg_grade sag
WHERE sag.average_grade > 4.2;
```

Удаление данных из основной таблицы в history. Метод возвращает список реально удаленных записей.

`RETURNING *` – возвращает всю удаленную строку, `RETURNING id` – только id. Конструкция `RETURNING` применима
для `INSERT`, `UPDATE`, `DELETE`.

`CREATE TABLE ... (LIKE ...)` – создание таблицы на основе DDL другой таблицы.

```postgresql
CREATE TABLE students_history
(
    LIKE students
);

CREATE OR REPLACE FUNCTION delete_students(ids INT[])
    RETURNS TABLE
            (
                STUDENT_ID INT
            )
AS
$$
BEGIN
    RETURN QUERY
        WITH deleted_rows AS (DELETE FROM students WHERE id = ANY (ids) RETURNING *)
            INSERT INTO students_history (SELECT * FROM deleted_rows) RETURNING id;
END;
$$
    LANGUAGE plpgsql;

SELECT delete_students(ARRAY [1, 2, 3]);
```

### Recursive View

Рекурсивное вычисление факториала.

```postgresql
WITH RECURSIVE fact (n, factorial) AS (SELECT 1::NUMERIC
                                            , 1::NUMERIC

                                       UNION ALL

                                       SELECT n + 1         AS n
                                            , factorial * n AS factorial
                                       FROM fact
                                       WHERE n < 20)
SELECT *
FROM fact;
```

Вычисление чисел Фибоначчи:

```postgresql
WITH RECURSIVE fib(a, b) AS (SELECT 0::NUMERIC
                                  , 1::NUMERIC

                             UNION ALL

                             SELECT GREATEST(a, b), a + b AS a
                             FROM fib
                             WHERE a <= 100000)
SELECT a
FROM fib;
```

`WITH RECURSIVE` можно применять для обхода дерева:

```postgresql
DROP TABLE IF EXISTS geo;
CREATE TABLE geo
(
    id        SERIAL PRIMARY KEY,
    parent_id INT
        CONSTRAINT fk_geo_parent_id REFERENCES geo (id),
    name      VARCHAR(80)
);
```

```postgresql
INSERT INTO geo (id, parent_id, name)
VALUES (1, NULL, 'Планета Земля')
     , (2, 1, 'Евразия')
     , (3, 1, 'Северная Америка')
     , (4, 2, 'Европа')
     , (5, 4, 'Россия')
     , (6, 4, 'Германия')
     , (7, 5, 'Москва')
     , (8, 5, 'Санкт-Петербург')
     , (9, 6, 'Берлин');
```

```postgresql
WITH RECURSIVE recursive AS (SELECT g.id        AS id
                                  , g.parent_id AS Parent
                                  , g.name      AS Name
                                  , 1           AS Level
                             FROM geo g
                             WHERE g.id = 1

                             UNION ALL

                             SELECT g.id                AS id
                                  , g.parent_id         AS Parent
                                  , g.name              AS NAME
                                  , recursive.Level + 1 AS LEVEL
                             FROM geo g
                                 JOIN recursive
                                      ON g.parent_id = recursive.id)
SELECT *
FROM recursive;
```

#### Window function

В каждой группе вывести топ 3 студентов по РСОИ.

```postgresql
SELECT cg.*
FROM (SELECT cg.grade                                                          AS Grade
           , s.firstname || ' ' || s.lastname                                  AS StudentName
           , s."group"                                                         AS "Group"
           , ROW_NUMBER() OVER (PARTITION BY s."group" ORDER BY cg.grade DESC) AS rn
      FROM course_grades cg
          INNER JOIN students s ON s.id = cg.student_id
      WHERE cg.course_id = (SELECT c.id FROM courses c WHERE c.name = 'РСОИ')) cg
WHERE cg.rn <= 3;
```

Вывести среднюю оценку по группе по курсу РСОИ:

```postgresql
SELECT s."group"                                   AS "Group"
     , AVG(cg.grade) OVER (PARTITION BY s."group") AS "AverageGrade"
FROM students s
    INNER JOIN course_grades cg ON s.id = cg.student_id
WHERE cg.course_id = (SELECT C.id FROM courses C WHERE C.name = 'РСОИ')
GROUP BY s."group", "AverageGrade"
ORDER BY "AverageGrade";

```

### Filter

* `CASE WHEN ... THEN ... ELSE ... END`
* `FILTER (WHEN ...)`

Вывести количество 2, 3, 4 и 5 в разрезе групп:

```postgresql
SELECT s."group"                                            AS "Group"
     , SUM(CASE WHEN FLOOR(cg.grade) = 2 THEN 1 ELSE 0 END) AS "2"
     , SUM(CASE WHEN FLOOR(cg.grade) = 3 THEN 1 ELSE 0 END) AS "3"
     , SUM(CASE WHEN FLOOR(cg.grade) = 4 THEN 1 ELSE 0 END) AS "4"
     , SUM(CASE WHEN FLOOR(cg.grade) = 5 THEN 1 ELSE 0 END) AS "5"
FROM students s
    INNER JOIN course_grades cg ON s.id = cg.student_id
WHERE cg.course_id = (SELECT c.id FROM courses c WHERE c.name = 'РСОИ')
GROUP BY "Group";
```

```postgresql
SELECT s."group"                                   AS "Group"
     , COUNT(1) FILTER (WHERE FLOOR(cg.grade) = 2) AS "2"
     , COUNT(1) FILTER (WHERE FLOOR(cg.grade) = 3) AS "3"
     , COUNT(1) FILTER (WHERE FLOOR(cg.grade) = 4) AS "4"
     , COUNT(1) FILTER (WHERE FLOOR(cg.grade) = 5) AS "5"
FROM students s
    INNER JOIN course_grades cg ON s.id = cg.student_id
WHERE cg.course_id = (SELECT c.id FROM courses c WHERE c.name = 'РСОИ')
GROUP BY "Group";
```

### Upsert

* `ON CONFLICT (id)` – constraint violation на поле.
* `ON CONFLICT ON CONSTRAINT students_pkey` – constraint violation по имени.


* `DO UPDATE SET field = EXCLUDED.field ` – обновить значения поля данными из блока `VALUES`;
* `DO NOTHING` – ничего не делать;

```postgresql
INSERT INTO students (id, firstname, lastname, github, "group")
VALUES (1, 'Alexey', 'Romanov', 'romanow', 'ИУ7-13М')
ON CONFLICT(id) DO UPDATE SET firstname = excluded.firstname
                            , lastname  = excluded.lastname
                            , github    = excluded.github
                            , "group"   = excluded."group";

SELECT * FROM students WHERE id = 1;
```

### OLD

```postgresql
--------------------------------
------ MATERIALIZED VIEW -------
--------------------------------
CREATE OR REPLACE VIEW one_multi_two
AS
SELECT DISTINCT o.a * t.c AS m
FROM one o
   , two t;

SELECT *
FROM one_multi_two ot
ORDER BY ot.m;

UPDATE one
SET a = -1
WHERE a = 1;

DROP MATERIALIZED VIEW one_multi_two;
CREATE MATERIALIZED VIEW one_multi_two
AS
SELECT o.a * t.c AS m
FROM one o
   , two t;


SELECT *
FROM one_multi_two ot
ORDER BY ot.m;

UPDATE one
SET a = 1
WHERE a = -1;

REFRESH MATERIALIZED VIEW one_multi_two;

SELECT *
FROM one_multi_two ot
ORDER BY ot.m;

SELECT *
FROM one;
--------------------------------
---- OVER ... PARTITION BY -----
--------------------------------

-- 	Для простоты понимания можно считать, что postgres сначала выполняет весь запрос (кроме сортировки и LIMIT), а потом только просчитывает оконные выражения.
-- 	Окно — это некоторое выражение, описывающее набор строк, которые будет обрабатывать функция и порядок этой обработки.
-- 	Причем окно может быть просто задано пустыми скобками (), т.е. окном являются все строки результата запроса.

CREATE
    TABLE employee
(
    dep    INT,
    salary INT
);

INSERT INTO employee
VALUES (1, 1000)
     , (2, 500)
     , (2, 6000)
     , (3, 700)
     , (3, 800)
     , (3, 1100);

SELECT dep
     , AVG(salary) OVER (PARTITION BY dep) AS rrr
FROM employee e
ORDER BY rrr;

SELECT dep
     , salary
     , SUM(salary) OVER (PARTITION BY dep) / (SELECT COUNT(*) FROM employee ee WHERE e.dep = ee.dep)
     , ROW_NUMBER() OVER (ORDER BY salary DESC) AS r
     -- / (SELECT count(*) FROM employee ee WHERE e.dep = ee.dep)
FROM employee e
ORDER BY rrr;
```

### Данные для примеров

1. [Modern SQL](https://modern-sql.com/)
2. [Modern SQL (CMU Intro to Database Systems / Fall 2022)](https://www.youtube.com/watch?v=II5qNuxfSoo)
3. [We need tool support for keyset pagination](https://use-the-index-luke.com/no-offset)
4. [Modern SQL formatting](https://gist.github.com/mattmc3/38a85e6a4ca1093816c08d4815fbebfb)
5. [SQL Slides by Markus Winand](https://winand.at/sql-slides-for-developers)