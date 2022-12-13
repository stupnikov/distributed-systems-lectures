# SQL

## Современные функции SQL

* [x] `SUM`, `AVG`, `HAVING`: у кого средний бал за семестр >= 4.5.
* [x] `LATERAL JOIN`: вывод первых 3 студентов в каждом курсе.
* [x] `SELECT DISTINC ...`, `SELECT COUNT(DISTINCT ...)`: подсчет различных записей.
* [x] `LIMIT ... OFFSET ...`, пагинация по всем студентам, `LIMIT, ORDER BY ID, ID > ...`, пагинация по ID.
* [ ] `RECURSIVE`
    * вычисление факториала;
    * планета, континенты, страны.
* [ ] `INLINE VIEW`: группа и курс, упорядоченная по имени и фамилии.
* [ ] `OVER ... PARTITION BY ...`: #TODO
* [ ] `FILTER (WHERE ...)`: фильтрация результата.
* [ ] `SUM(CASE a > 0 THEN 1 ELSE 0)`: сложные фильтры.
* [ ] `INSERT INTO ... (SELECT ... FROM ...)`, `SELECT * INTO ... FROM ...`, `CREATE TABLE ... (LIKE ...)`: копирование
  таблицы в другую таблицу.
* [ ] `INSERT ... ON CONFLICT DO UPDATE / NOTHING`: upsert.
* [ ] `INSERT ... RETURNING id`: возвращение id только что созданной записи.
* [ ] `REFERENCES ... ON DELETE ...`: cascade обновления / удаления.
* [ ] `CREATE OR REPLACE FUNCTION ...`, `CREATE TRIGGER ...`: сохранение старой записи.
* [ ] `JSON`

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
        CONSTRAINT fk_course_grades_course_id REFERENCES courses (id),
    student_id INT
        CONSTRAINT fk_course_grades_student_id REFERENCES students (id)
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

### LATERAL JOIN

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

### Inline View

### Recursive View

Рекурсивное вычисление факториала.

```postgresql
WITH RECURSIVE t(n) AS (
    SELECT 1

    UNION ALL

    SELECT o.a + 1
    FROM one o
    WHERE o.a < 100
)
SELECT SUM(n)
FROM t;
```

#### Window function

В каждой группе вывести топ 3 студентов по РСОИ.

```postgresql
SELECT cg.*
FROM (
    SELECT cg.grade                                                          AS Grade
         , s.firstname || ' ' || s.lastname                                  AS StudentName
         , s."group"                                                         AS "Group"
         , ROW_NUMBER() OVER (PARTITION BY s."group" ORDER BY cg.grade DESC) AS rn
    FROM course_grades cg
        INNER JOIN students s ON s.id = cg.student_id
    WHERE cg.course_id = (SELECT c.id FROM courses c WHERE c.name = 'РСОИ')
) cg
WHERE cg.rn <= 3;
```

### OLD

```postgresql
--------------------------------
------ WITH (INLINE VIEW) ------
--------------------------------
SELECT *
FROM one o
    JOIN (SELECT c, d FROM two t) k ON o.a = k.c;

WITH k(c, d)
         AS (SELECT c, d FROM two)
SELECT *
FROM one o
    JOIN k ON o.a = k.c;

CREATE TABLE one_hist
(
    LIKE one
);
WITH deleted_rows AS
         (
             DELETE FROM one RETURNING *
         )
INSERT
INTO one_hist
SELECT *
FROM deleted_rows;

INSERT INTO one
SELECT *
FROM one_hist;
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
-------- WITH RECURSIVE --------
--------------------------------
WITH RECURSIVE t(n) AS (
    SELECT 1

    UNION ALL

    SELECT o.a + 1
    FROM one o
    WHERE o.a < 100
)
SELECT SUM(n)
FROM t;

CREATE TABLE geo
(
    id        INT NOT NULL PRIMARY KEY,
    parent_id INT REFERENCES geo (id),
    name      VARCHAR(1000)
);

INSERT INTO geo
    (id, parent_id, name)
VALUES (1, NULL, 'Планета Земля')
     , (2, 1, 'Континент Евразия')
     , (3, 1, 'Континент Северная Америка')
     , (4, 2, 'Европа')
     , (5, 4, 'Россия')
     , (6, 4, 'Германия')
     , (7, 5, 'Москва')
     , (8, 5, 'Санкт-Петербург')
     , (9, 6, 'Берлин');

WITH RECURSIVE r AS (
    SELECT id
         , parent_id
         , name
         , 1 AS level
    FROM geo
    WHERE id = 1

    UNION ALL

    SELECT geo.id
         , geo.parent_id
         , geo.name
         , r.level + 1 AS level
    FROM geo
        JOIN r
             ON geo.parent_id = r.id
)
SELECT *
FROM r;

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
     , SUM(salary) / (SELECT COUNT(*) FROM employee ee WHERE e.dep = ee.dep)
     , COUNT(e.salary)
FROM employee e
GROUP BY dep;

SELECT dep
     , salary
     , SUM(salary) OVER (PARTITION BY dep) / (SELECT COUNT(*) FROM employee ee WHERE e.dep = ee.dep)
     , ROW_NUMBER() OVER (ORDER BY salary DESC) AS r
     -- / (SELECT count(*) FROM employee ee WHERE e.dep = ee.dep)
FROM employee e
ORDER BY r;

--------------------------------
------------ OTHER -------------
--------------------------------
SELECT SUM(o.a) FILTER (WHERE a > 50), SUM(o.a)
FROM one o;

INSERT INTO one (SELECT * FROM one_hist);
CREATE TABLE one_hist
(
    LIKE one
);
```

### Данные для примеров

1. [Modern SQL](https://modern-sql.com/)
2. [Modern SQL (CMU Intro to Database Systems / Fall 2022)](https://www.youtube.com/watch?v=II5qNuxfSoo)
3. [We need tool support for keyset pagination](https://use-the-index-luke.com/no-offset)
4. [Modern SQL formatting](https://gist.github.com/mattmc3/38a85e6a4ca1093816c08d4815fbebfb)
5. [SQL Slides by Markus Winand](https://winand.at/sql-slides-for-developers)