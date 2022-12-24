# Индексы и оптимизация SQL запросов

## JOIN

![Join Types](images/join_types.png)

```sql
CREATE OR REPLACE FUNCTION random_string(length INT)
    RETURNS VARCHAR AS
$$
BEGIN
    RETURN (
        SELECT STRING_AGG(SUBSTR('ABCDEFGHIJKLMNOPQRSTUVWXYZ',
                                 CEIL(RANDOM() * 26)::INTEGER, 1), '')
        FROM GENERATE_SERIES(1, length)
    );
END;
$$ LANGUAGE plpgsql;

CREATE TABLE one
(
    id   INT PRIMARY KEY,
    name VARCHAR(80)
);

CREATE TABLE two
(
    id   INT PRIMARY KEY,
    name VARCHAR(80)
);

INSERT INTO one (id, name)
SELECT i, random_string(10)
FROM GENERATE_SERIES(1, 10) AS i;

INSERT INTO two (id, name)
SELECT i, random_string(10)
FROM GENERATE_SERIES(6, 15) AS i;
```

##### INNER JOIN

Оператор внутреннего соединения. Порядок таблиц для оператора неважен, поскольку оператор является симметричным.
Выбираются только совпадающие данные из объединяемых таблиц.

  ```sql
SELECT o.id   AS id1
     , t.id   AS id2
     , o.name AS name1
     , t.name AS name2
FROM one o
    INNER JOIN two t ON o.id = t.id;
```

|  #  | id1 | id2 |    name1   |    name2   |
|:---:|:---:|:---:|:----------:|:----------:|
|  1  |  6  |  6  | BGAEGCFCDG | EHFHGCDEBA |
|  2  |  7  |  7  | AECCBACDDB | GBAADAFCDH |
|  3  |  8  |  8  | BAAEGCFCEB | DAACDGGBAC |
|  4  |  9  |  9  | CDGFFDCGEH | GBEECHCEBC |
|  5  |  10 |  10 | EFGDBFBBGF | BGBDEGBGDH |

##### FULL OUTER JOIN

Такое объединение вернет данные из обеих таблиц (совпадающие по условию объединения) ПЛЮС дополнит выборку оставшимися
данными из внешней таблицы, которые по условию не подходят, заполнив недостающие данные значением `NULL`.

```sql
SELECT o.id   AS id1
     , t.id   AS id2
     , o.name AS name1
     , t.name AS name2
FROM one o
    FULL OUTER JOIN two t ON o.id = t.id;
```

|  #  |  id1 |  id2 |    name1   |    name2   |
|:---:|:----:|:----:|:----------:|:----------:|
|  1  |   1  | null | GGDFDCEFEC |    null    |
|  2  |   2  | null | FAGCEDAGHA |    null    |
|  3  |   3  | null | CCGEGGDCDG |    null    |
|  4  |   5  | null | AGCCCFHAAF |    null    |
|  5  |   5  | null | FEAGFEFGDC |    null    |
|  6  |   6  |   6  | BGAEGCFCDG | EHFHGCDEBA |
|  7  |   7  |   7  | AECCBACDDB | GBAADAFCDH |
|  8  |   8  |   8  | BAAEGCFCEB | DAACDGGBAC |
|  9  |   9  |   9  | CDGFFDCGEH | GBEECHCEBC |
|  10 |   10 |  10  | EFGDBFBBGF | BGBDEGBGDH |
|  11 | null |  11  |    null    | CAADHGHAGB |
|  12 | null |  12  |    null    | HGEHCDAFEB |
|  13 | null |  13  |    null    | HCGCCBEACH |
|  14 | null |  14  |    null    | HCGCCBEACH |
|  15 | null |  15  |    null    | GHAABBCDED |

##### LEFT / RIGHT JOIN

Работают они одинаково, разница заключается в том что `LEFT` - указывает что "внешней" таблицей будет находящаяся слева.

```sql
SELECT o.id   AS id1
     , t.id   AS id2
     , o.name AS name1
     , t.name AS name2
FROM one o
    LEFT JOIN two t ON o.id = t.id;
```

|  #  |  id1 |  id2 |    name1   |    name2   |
|:---:|:----:|:----:|:----------:|:----------:|
|  1  |   1  | null | GGDFDCEFEC |    null    |
|  2  |   2  | null | FAGCEDAGHA |    null    |
|  3  |   3  | null | CCGEGGDCDG |    null    |
|  4  |   5  | null | AGCCCFHAAF |    null    |
|  5  |   5  | null | FEAGFEFGDC |    null    |
|  6  |   6  |   6  | BGAEGCFCDG | EHFHGCDEBA |
|  7  |   7  |   7  | AECCBACDDB | GBAADAFCDH |
|  8  |   8  |   8  | BAAEGCFCEB | DAACDGGBAC |
|  9  |   9  |   9  | CDGFFDCGEH | GBEECHCEBC |
|  10 |   10 |  10  | EFGDBFBBGF | BGBDEGBGDH |

##### CROSS JOIN

Оператор перекрёстного соединения, или декартова произведения CROSS JOIN соединяет две таблицы. Порядок таблиц для
оператора неважен, поскольку оператор является симметричным. При использовании оператора SQL CROSS JOIN каждая строка
левой таблицы сцепляется с каждой строкой правой таблицы. В результате получается таблица со всеми возможными
сочетаниями строк обеих таблиц.

```sql
SELECT o.id   AS id1
     , t.id   AS id2
     , o.name AS name1
     , t.name AS name2
FROM one o
    CROSS JOIN two t;
```

|  #  |  id1 |  id2 |    name1   |    name2   |
|:---:|:----:|:----:|:----------:|:----------:|
|   1 |   1  |   6  | GGDFDCEFEC | EHFHGCDEBA |
|   2 |   1  |   7  | GGDFDCEFEC | GBAADAFCDH |
|   3 |   1  |   8  | GGDFDCEFEC | DAACDGGBAC |
|   4 |   1  |   9  | GGDFDCEFEC | GBEECHCEBC |
|   5 |   1  |  10  | GGDFDCEFEC | BGBDEGBGDH |
| ... |  ... |  ... |     ...    |     ...    |
|  96 |  10  |  11  | EFGDBFBBGF | CAADHGHAGB |
|  97 |  10  |  12  | EFGDBFBBGF | HGEHCDAFEB |
|  98 |  10  |  13  | EFGDBFBBGF | HACAHBCFHC |
|  99 |  10  |  14  | EFGDBFBBGF | GHAABBCDED |
| 100 |  10  |  15  | EFGDBFBBGF | HCGCCBEACH |

## Индексы

Индексы в PostgreSQL — специальные объекты базы данных, предназначенные в основном для ускорения доступа к данным. Это
вспомогательные структуры: любой индекс можно удалить и восстановить заново по информации в таблице. Так же индексы
служат для поддержки ограничений целостности.

Индекс устанавливает соответствие между ключом и строками таблицы, в которых этот ключ встречается. Строки
идентифицируются с помощью `TID` (Tuple ID), который состоит из номера блока файла и позиции строки внутри блока. Тогда,
зная ключ или некоторую информацию о нем, можно быстро прочитать те строки, в которых может находиться интересующая нас
информация, не просматривая всю таблицу полностью.

При любой операции над проиндексированными данными: вставка, удаление или обновление строк таблицы, — индексы, созданные
для этой таблицы, должны быть перестроены, причем в рамках той же транзакции.

Механизм индексирования участвует в выполнении запросов. Он вызывается в соответствии с планом, построенным на этапе
оптимизации. Оптимизатор, перебирая и оценивая различные пути выполнения запроса, должен понимать возможности всех
методов доступа, которые потенциально можно применить:

* Сможет ли метод доступа отдавать данные сразу в нужном порядке или надо отдельно применить сортировку?
* Можно ли применить метод доступа для поиска `null`?

```sql
CREATE TABLE three
(
    a INT,
    b VARCHAR(10),
    c BOOLEAN
);


INSERT INTO three(a, b, c)
SELECT s.id, random_string(1), RANDOM() < 0.01
FROM GENERATE_SERIES(1, 100000) AS s(id)
ORDER BY RANDOM();

CREATE INDEX idx_three_a ON three (a);
ANALYSE three;

EXPLAIN SELECT * FROM three WHERE a = 1;
```

```
Index Scan using idx_three_a on three  (cost=0.29..8.31 rows=1 width=7)
  Index Cond: (a = 1)
```

### Сканирование по битовой карте

`Bitmap Index Scan` – метод битовых индексов заключается в создании отдельных битовых карт (последовательность 0 и 1)
для каждого возможного значения столбца, где каждому биту соответствует строка с индексируемым значением, а его значение
равное 1 означает, что запись, соответствующая позиции бита содержит индексируемое значение для данного столбца или
свойства.

Сначала метод доступа возвращает все `TID`, соответствующие условию (узел `Bitmap Index Scan`), и по ним строится
битовая карта версий строк. Затем версии строк читаются из таблицы (`Bitmap Heap Scan`) — при этом каждая страница будет
прочитана только один раз. Сканирование по битовой карте позволяет избежать повторных обращений к одной и той же
странице данных.

```sql
EXPLAIN SELECT * FROM three WHERE a <= 100;
```

```
Bitmap Heap Scan on three  (cost=4.99..228.28 rows=90 width=7)
  Recheck Cond: (a <= 100)
  ->  Bitmap Index Scan on idx_three_a  (cost=0.00..4.97 rows=90 width=0)
        Index Cond: (a <= 100)
```

Сначала метод доступа возвращает все `TID`, соответствующие условию (узел Bitmap Index Scan), и по ним строится битовая
карта версий строк. Затем версии строк читаются из таблицы (Bitmap Heap Scan) — при этом каждая страница будет прочитана
только один раз.

Если условия наложены на несколько полей таблицы, и эти поля проиндексированы, сканирование битовой карты позволяет
использовать несколько индексов одновременно. Для каждого индекса строятся битовые карты версий строк, которые затем
побитово логически умножаются (`AND`), либо логически складываются (`OR`).

```sql
CREATE INDEX idx_three_b ON three (b);
ANALYSE three;

EXPLAIN SELECT * FROM three WHERE a <= 100 AND b = 'a';
```

```
Bitmap Heap Scan on three  (cost=17.18..21.19 rows=1 width=7)
  Recheck Cond: ((a <= 100) AND ((b)::text = 'a'::text))
  ->  BitmapAnd  (cost=17.18..17.18 rows=1 width=0)
        ->  Bitmap Index Scan on idx_three_a  (cost=0.00..5.06 rows=102 width=0)
              Index Cond: (a <= 100)
        ->  Bitmap Index Scan on idx_three_b  (cost=0.00..11.87 rows=1010 width=0)
              Index Cond: ((b)::text = 'a'::text)
```

### Index Only Scan

Основная задача метода доступа — вернуть идентификаторы подходящих строк таблицы, чтобы механизм индексирования мог
прочитать из них необходимые данные. Но если индекс содержит все данные, требующиеся в запросе, то оптимизатор может
применить `Index Only Scan`.

```sql
EXPLAIN SELECT a FROM three WHERE a <= 100;
```

```
Index Only Scan using idx_three_a on three  (cost=0.29..6.08 rows=102 width=4)
  Index Cond: (a <= 100)
```

### Sequence scan

При запросе по индексу Postgres сначала читает индекс (упорядоченный), а потом по `TID` идет к страницам и читает данные
оттуда вразнобой. При этом последовательное чтение выполняется быстрее.

Индексы работают тем лучше, чем выше селективность условия, то есть чем меньше строк ему удовлетворяет. При увеличении
выборки возрастают и накладные расходы на чтение страниц индекса.

```sql
EXPLAIN SELECT * FROM three WHERE a <= 40000;
```

```
Seq Scan on three  (cost=0.00..1693.00 rows=39954 width=7)
  Filter: (a <= 40000)
```

При неселективном условии оптимизатор предпочтет использованию индекса последовательное сканирование таблицы целиком.

## Виды индексов

### [btree](https://habr.com/ru/company/postgrespro/blog/330544/)

Индекс btree (B-деревья) пригоден для данных, которые можно отсортировать, другими словами, для типа данных должны быть
определены операторы `>`, `>=`, `=`, `<`, `<=`.

B-деревья являются сбалансированными деревьями, поэтому время выполнения стандартных операций в них пропорционально
высоте. Другими словами, любую листовую страницу отделяет от корня одно и то же число внутренних страниц, следовательно,
поиск любого значения занимает одинаковое время.

![btree](images/btree.png)

Индексные записи B-дерева упакованы в страницы. В листовых страницах эти записи содержат индексируемые данные (ключи) и
ссылки на строки таблицы (`TID`). Во внутренних страницах каждая запись ссылается на дочернюю страницу индекса и
содержит минимальное значение ключа в этой странице.

B-деревья сильно ветвистые, то есть каждая страница содержит сразу много `TID`, за счет этого глубина B-деревьев
получается небольшой

Данные в индексе упорядочены по неубыванию (как между страницами, так и внутри каждой страницы), а страницы одного
уровня связаны между собой двунаправленным списком. Поэтому получить упорядоченный набор данных мы можем, просто проходя
по списку в одну или в другую сторону, не возвращаясь каждый раз к корню.

### [Hash](https://habr.com/ru/company/postgrespro/blog/328280/)

![Hash Index](images/hash_index.png)

Идея хеширования состоит в том, чтобы значению любого типа данных сопоставить некоторое небольшое число (0..N−1, всего N
значений). Полученное число можно использовать как индекс обычного массива, куда и складывать ссылки на строки
таблицы (`TID`). Элементы такого массива называют корзинами hash-таблицы — в одной корзине могут лежать несколько `TID`,
если одно и то же проиндексированное значение встречается в разных строках, либо если один hash-функция вернула корзину
для разных данных (коллизия). Для этого вместе с `TID` нужно хранить еще сам ключ, но экономии места сохраняется не сам
ключ, а его hash-код.

При вставке в индекс вычислим hash-функцию для ключа. Хеш-функции в Postgres всегда возвращают тип `INT`, что
соответствует диапазону 2^32, т.е. примерно 4 миллиарда значений. Число корзин изначально равно двум и увеличивается
динамически, подстраиваясь под объем данных.

Hash индексы не могут быть упорядоченными и не могут быть уникальными, т.к. могут быть коллизии.

### [GiST](https://habr.com/ru/company/postgrespro/blog/333878/)

GiST — сбалансированное по высоте дерево, состоящее из узлов-страниц. Узлы состоят из индексных записей. Каждая запись
листового узла содержит, если говорить в самом общем виде, некий предикат (логическое выражение) и ссылку на строку
таблицы (`TID`). Индексированные данные (ключ) должны удовлетворять этому предикату.

Каждая запись внутреннего узла также содержит предикат и ссылку на дочерний узел, причем все индексированные данные
дочернего поддерева должны удовлетворять этому предикату. Иными словами, предикат внутренней записи включает в себя
предикаты всех дочерних записей. Это важное свойство, заменяющее индексу GiST простую упорядоченность B-дерева.

Поиск в дереве GiST использует специальную функцию согласованности (`consistent`) — одну из функций, определяемых
интерфейсом, и реализуемую по-своему для каждого поддерживаемого семейства операторов.

Функция согласованности вызывается для индексной записи и определяет, согласуется ли предикат данной записи с поисковым
условием (вида `индексированное-поле` -> `оператор` -> `выражение`). Для внутренней записи она фактически определяет,
надо ли спускаться в соответствующее поддерево, а для листовой записи — удовлетворяют ли индексированные данные условию.

Поиск начинается с корневого узла. С помощью функции согласованности выясняется, в какие дочерние узлы имеет смысл
заходить (их может оказаться несколько), а в какие — нет. Затем алгоритм повторяется для каждого из найденных дочерних
узлов. Если же узел является листовым, то запись, отобранная функцией согласованности, возвращается в качестве одного из
результатов.

Поиск производится в глубину: алгоритм в первую очередь старается добраться до какого-нибудь листового узла. Это
позволяет по возможности быстро вернуть первые результаты (что может быть важно, если пользователя интересуют не все
результаты, а только несколько).

### [SP-GiST](https://habr.com/ru/company/postgrespro/blog/337502/)

Идея индексного метода SP-GiST состоит в разбиении области значений на неперекрывающиеся подобласти, каждая из которых,
в свою очередь, также может быть разбита. Такое разбиение порождает несбалансированные деревья (в отличие от B-деревьев
и обычного GiST).

Внутренний узел дерева SP-GiST хранит ссылки на дочерние узлы, для каждой ссылки может быть задана метка. Кроме того,
внутренний узел может хранить значение, называемое префиксом. На самом деле это значение не обязано быть именно
префиксом, его можно рассматривать как произвольный предикат, выполняющийся для всех дочерних узлов.

Листовые узлы SP-GiST содержат значение индексированного типа и ссылку на строку таблицы (`TID`). В качестве значения
могут использоваться сами индексированные данные (ключ поиска), но не обязательно: может храниться и сокращенное
значение. Кроме того, листовые узлы могут собираться в списки. Таким образом, внутренний узел может ссылаться не на одно
единственное значение, а на целый список.

### [GIN](https://habr.com/ru/company/postgrespro/blog/340978/)

GIN расшифровывается как Generalized Inverted Index — обратный индекс. Он работает с типами данных, значения которых не
являются атомарными, а состоят из элементов. При этом индексируются не сами значения, а отдельные элементы, каждый
элемент ссылается на те значения, в которых он встречается.

К каждому элементу привязан упорядоченный набор ссылок на строки таблицы, содержащие значения с этим элементом.

Основная область применения метода GIN — ускорение полнотекстового поиска.

## Оптимизация

### Неиспользование индексов

* Если большая выборка, а `work_mem` маленький, то планировщик выбирает Sequence Scan по таблице.
* Если стоимость вычисления с использованием индекса будет больше, чем обычный обход таблицы. Например, если существует
  10 млн. записей, id является первичным ключом и по нему построен индекс. При
  запросе `SELECT * FROM logs WHERE id > 100` будет выполнен Sequence Scan, т.к. фильтрация по индексу даст малое
  количество отфильтрованных записей (условие не селективное, т.е. возвращается много данных), а после фильтрации нужно
  будет идти в саму таблицу за данными.
* Если в запросе фигурирует `IS NOT NULL` в условии, при этом таблица является разряженной (кол-во `NULL` полей больше
  50%), а при построении индекса не было указано, что строить только по не пустым полям, то планировщик будет
  использовать Sequence Scan. (База данных предполагает, что индексируемая колонка без `NOT NULL` охватывает слишком
  большой диапазон, чтобы быть полезным, поэтому база данных не будет вести поиск по индексу). Так же это даст очень
  большой прирост в размере индекса.
* Типы полей в запросе должны в точности совпадать с типами в индексе.
* Порядок полей в индексе важен: если индекс построен по A, B, то если в запросе фигурирует только B, то индекс не будет
  использован.

### Оптимизация

1. Если в результате запроса данные дублируются (обычно при join 1:N), то это нельзя лечить с помощью `DISTINCT`.
   `DISTINCT` делает либо сортировку (обычно), либо агрегирование через хэширование.
2. При использовании индекса можно задавать сортировку. Если индекс построен по ASC (по-умолчанию), а в запросе
   используется DESC, то данные запрашиваются с диска не последовательно, что снижает скорость работы.
3. При частой перестройке индекса индекс становится фрагментированным (т.е. информация хранится не последовательно на
   диске). Для избавления от фрагментации используется команда `REINDEX`.
4. Обычно построение индекса требует установки блокировки типа `SHARE` на таблицу. Такая блокировка позволяет читать
   данные из таблицы, но запрещает любые изменения, пока строится индекс. В Postgres есть параметр построения
   индекса `CONCURRENTLY` индекс строится в фоновом режиме не мешая операциям записи, но становится доступным только
   когда завершатся все начатые до момента окончания его построения транзакции. Когда он используется, Postgres должен
   выполнить два сканирования таблицы, а кроме того, должен дождаться завершения всех существующих транзакций, которые
   потенциально могут модифицировать и использовать этот индекс. Таким образом, эта процедура требует проделать в сумме
   больше действий и выполняется значительно дольше, чем обычное построение индекса. Однако благодаря тому, что этот
   метод позволяет продолжать обычную работу с базой во время построения индекса, он оказывается полезным в
   производственной среде.
5. Функциональный индекс (построение индекса по immutable-функции).
6. Для сокращения размера индекса и повышения его селективности в конкретных условиях можно задавать частичные индексы с
   помощью `WHERE`.
7. Так же в случае разряженного поля нужно использовать `IS NOT NULL`, т.к. наличие `NULL` полей не приносит результата
   и лишь увеличивает размер индекса.
8. Можно в явном виде задавать hint (указывать планировщику, что нужно использовать конкретный индекс).
9. Убрать пересекающиеся индексы. Индекс, построенный по A, B перекрывает индекс по A.
10. Если вы пишете хитрые запросы, скорее всего структура данных не соответствует тому, что вам нужно.
11. При обновлении данных всегда задавать условие и дополнительно проверять с помощью конструкции `IS DISTINCT FROM`,
    что поле требует обновления:
    ```sql
    UPDATE four SET a = 10 WHERE a IS DISTINCT FROM 10;
    ```
12. Если требуется сделать `JOIN` маленькой таблицы на большую таблицу, то это все может работать медленно. Можно либо
    денормализовать две таблицы в одну с осознанной избыточностью данных, либо нужные записи из маленькой таблицы
    поднять заранее и в `WHERE` передавать id в блоке `IN`.

### EXPLAIN

##### Создание таблиц

```sql
CREATE TABLE IF NOT EXISTS four
(
    a INT,
    b VARCHAR(80),
    c INT
);

TRUNCATE four;
INSERT INTO four (a, b, c)
SELECT s.id, random_string(8), (CASE ROUND(3 * RANDOM()) WHEN 3 THEN (RANDOM() * 500)::INT END)
FROM GENERATE_SERIES(1, 10000) AS s(id);

ANALYSE four;
EXPLAIN SELECT * FROM four;
```

##### Обновление статистики

Статистика перестраивается после определенного количества изменений данных, т.е. когда мы добавили 10 записей план
запроса не изменится (т.к. статистика не обновилась).

```sql
INSERT INTO four (a, b, c)
SELECT s.id, random_string(8), (RANDOM() * 500)::INT
FROM GENERATE_SERIES(1, 10) AS s(id);

-- Получаем старые данных из статистики
EXPLAIN ANALYSE SELECT * FROM four;

-- Перестраиваем статистику
ANALYSE four;
-- Получаем корректные данные из статистики
EXPLAIN ANALYSE SELECT * FROM four;
```

##### Селективность индексов

Если условие имеет маленькую селективность, то будет выполняться sequence scan, т.к. оптимизатор решит что это быстрее.

```sql
CREATE INDEX IF NOT EXISTS idx_four_a ON four (a);
EXPLAIN SELECT * FROM four WHERE a > 50;
```

При большой селективности Postgres используем индекс.

```sql
EXPLAIN SELECT * FROM four WHERE a > 9900;
```

##### Индексы по нескольким столбцам

```sql
EXPLAIN (ANALYSE, VERBOSE, BUFFERS)
SELECT * FROM four WHERE a = 10;

EXPLAIN SELECT a FROM four WHERE a = 10;

DROP INDEX IF EXISTS idx_four_a;
-- Удаляем старый индекс и создаем индекс по A и B
CREATE INDEX IF NOT EXISTS idx_four_a_b ON four (a, b);

-- Этот индекс может применяться отдельно по полю A
EXPLAIN SELECT * FROM four WHERE a > 9900;

-- Но не применяется по полю B, т.к. это поле стоит вторым в индексе
EXPLAIN SELECT * FROM four WHERE b = 'HAWJPWXY';
```

##### Функциональные индексы

Типы полей в запросе должны в точности совпадать с типами в индексе.

```sql
EXPLAIN SELECT * FROM four WHERE LOWER(b) = 'hawjpwxy';

CREATE INDEX IF NOT EXISTS idx_four_b_lower ON four (LOWER(b));

EXPLAIN SELECT * FROM four WHERE LOWER(b) = 'hawjpwxy';
```

##### Указание оператора доступа при создании индекса

Для применения индекса для поиска по шаблону (`%`) требуется указать оператор `VARCHAR_PATTERN_OPS`.

```sql
CREATE INDEX IF NOT EXISTS idx_four_b ON four (b);
EXPLAIN SELECT * FROM four WHERE b LIKE 'AAA%';

DROP INDEX idx_four_b;
CREATE INDEX IF NOT EXISTS idx_four_b ON four (b VARCHAR_PATTERN_OPS);

EXPLAIN SELECT * FROM four WHERE b LIKE 'AAA%';
```

##### Сортировка индексов

```sql
-- Используется обратная сортировка по индексу
EXPLAIN SELECT * FROM four ORDER BY a DESC;

DROP INDEX IF EXISTS idx_four_a_b;
EXPLAIN SELECT * FROM four ORDER BY a;
```

##### Индекс по разряженному полю

Создаем индекс на разряженное поле C (т.е. ~50% значений `NULL`).

```sql
SELECT COUNT(*) FROM four WHERE c IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_four_c ON four (c);

EXPLAIN SELECT * FROM four WHERE c > 400;

SELECT indexname                                             AS name
     , PG_SIZE_PRETTY(PG_RELATION_SIZE(indexname::REGCLASS)) AS size
FROM pg_indexes
WHERE tablename = 'four';
```

Размер индекса ~90Kb.

Создадим индекс, явно указав в условии, что индексируем только `NOT NULL` поля.

```sql
DROP INDEX idx_four_c;
CREATE INDEX IF NOT EXISTS idx_four_c ON four (c) WHERE c IS NOT NULL;

SELECT indexname                                             AS name
     , PG_SIZE_PRETTY(PG_RELATION_SIZE(indexname::REGCLASS)) AS size
FROM pg_indexes
WHERE tablename = 'four';
```

Размер индекса уменьшился до 40Kb. Частичный индекс дает существенную экономию для больших таблиц.

### Оптимизация JOIN

#### Hash Join

Строим hash-таблицу из меньшей таблицы.

```
FOR i IN second_table
    IF key_exists(hash(j))
```

|       Плюсы     |                                             Минусы                                    |    
|-----------------|---------------------------------------------------------------------------------------|
| Не нужен индекс | Нужно много памяти                                                                    |
|                 | Долгое время получение первой строки (т.к. сначала требуется построение hash-таблицы) |

```sql
CREATE TABLE IF NOT EXISTS five
(
    d INT,
    e BOOLEAN
);

TRUNCATE five;
INSERT INTO five (d, e)
SELECT s.id, s.id % 2 = 0
FROM GENERATE_SERIES(1, 10000) AS s(id);

ANALYSE five;

DROP INDEX IF EXISTS idx_four_a_b, idx_four_a, idx_five_d;

EXPLAIN VERBOSE
SELECT fr.*
FROM four fr
    INNER JOIN five fv ON fr.a = fv.d
WHERE fr.a > 100;
```

#### Nested loop

Объединение через вложенный цикл, оптимизатор обычно применяет для небольших выборок.

```
FOR i IN first_table
    FOR j IN second_table WHERE second_table.i = i
``` 

|                                   Плюсы                                   |              Минусы            |    
|---------------------------------------------------------------------------|--------------------------------|
| Быстрый на небольших объемов данных                                       | Плох на больших объемах данных |
| Не требует много памяти                                                   |                                |
| Если по второй таблице идет сканирование по индексу, то получается быстро |                                |

```sql
CREATE INDEX IF NOT EXISTS idx_four_a ON four (a);
CREATE INDEX IF NOT EXISTS idx_five_d ON five (d);

EXPLAIN VERBOSE
SELECT fr.*
FROM four fr
    INNER JOIN five fv ON fr.a = fv.d
WHERE fr.a < 100;
```

#### Merge Join

Он используется, если объединяемые наборы данных отсортированы (или могут быть отсортированы с небольшими затратами) с
помощью ключа `JOIN`. После этого последовательно сканируются две таблицы и общие строки попадают в результат.

```sql
EXPLAIN VERBOSE
SELECT fr.*
FROM four fr
    INNER JOIN five fv ON fr.a = fv.d
WHERE fr.a < 1000
  AND fv.d < 100;
```

### Поддержание индексов

##### Сводная информация об индексах

```sql
SELECT pg_class.relname
     , PG_SIZE_PRETTY(pg_class.reltuples::BIGINT)          AS rows_in_bytes
     , pg_class.reltuples                                  AS num_rows
     , COUNT(*)                                            AS total_indexes
     , COUNT(*) FILTER (WHERE indisunique)                 AS unique_indexes
     , COUNT(*) FILTER (WHERE indnatts = 1 )               AS single_column_indexes
     , COUNT(*) FILTER (WHERE indnatts IS DISTINCT FROM 1) AS multi_column_indexes
FROM pg_namespace
    LEFT JOIN pg_class ON pg_namespace.oid = pg_class.relnamespace
    LEFT JOIN pg_index ON pg_class.oid = pg_index.indrelid
WHERE pg_namespace.nspname = 'public'
  AND pg_class.relkind = 'r'
GROUP BY pg_class.relname, pg_class.reltuples
ORDER BY pg_class.reltuples DESC;
```

##### Статистика использования индексов

```sql
SELECT idstat.relname                                             AS table_name
     , indexrelname                                               AS index_name
     , idstat.idx_scan                                            AS times_used
     , PG_SIZE_PRETTY(PG_RELATION_SIZE(idstat.relname::REGCLASS)) AS table_size
     , PG_SIZE_PRETTY(PG_RELATION_SIZE(indexrelname::REGCLASS))   AS index_size
     , n_tup_upd + n_tup_ins + n_tup_del                          AS num_writes
FROM pg_stat_user_indexes AS idstat
    JOIN pg_indexes ON indexrelname = indexname
    JOIN pg_stat_user_tables AS tabstat ON idstat.relname = tabstat.relname
WHERE idstat.idx_scan < 200
  AND indexdef !~* 'unique'
ORDER BY idstat.relname, indexrelname;
```

## Статистика выполнения

Статистическая информация, собираемая Postgres, имеет большое влияние на производительность системы. Зная статистику
распределения данных, оптимизатор может корректно оценить число строк, необходимый размер памяти и выбрать наиболее
быстрый план выполнения запроса.

Перед выполнением запроса планировщик создает план запроса. План запроса – набор операций для получения результата и
статистическая оценка времени выполнения. Для каждой такой элементарной операции (чтение данных с таблицы, сортировка,
объединение результатов) оценивается число строк и время выполнения в абстрактных единицах (время чтения страницы с
диска).

Оценочное количество страниц и записей:

```sql
SELECT reltuples, relpages FROM pg_class WHERE relname = 'one';
```

Статистическая информация:

```sql
SELECT * FROM pg_stats WHERE tablename = 'one' AND attname = 'a';
```

* `null_frac` – какой процент строк для этой таблицы является `null`&
* `avg_width` — средняя длина поля в таблице (для полей фиксированной длины это поле не актуально)&
* `n_distinct` – показывает, сколько в этом поле различных значений. `n_distinct` может быть как больше нуля, так и
  меньше нуля. Если больше нуля, то он показывает число различных значений, а если меньше нуля, то показывает долю от
  числа строк, которые будут принимать различные значения. Если `n_distinct = -0.5`, то это означало бы, что половина
  строк содержит уникальное значение, а вторая половина содержит их дубликаты. Если `n_distinct = -1`, то есть все
  значения разные. Число, большее нуля, представляет примерное количество различных значений в столбце. Если это число
  меньше нуля, его модуль представляет количество различных значений, делённое на количество строк.
* массивы `most_common_vals` и `most_common_freqs` содержат самые часто встречающиеся значения поля и соответствующие им
  частоты.
* `histogram_bounds` – массив интервалов, причем вероятность попасть в каждый интервал примерно одинаковая.

Для оценки значения, которого нет в `most_common_vals` используется факт отсутствия данного значения в списке в
сочетании с частотой для каждого значения из списка `most_common_vals`.

Т. е. нужно сложить частоты значений из списка `most_common_vals`, отнять полученное число от единицы, и полученное
значение разделить на количество остальных уникальных значений. Эти вычисления основаны на предположении, что значения,
которые не входят в список `most_common_vals`, имеют равномерное распределение.

```
(1 - sum(most_common_freqs)) / (num_distinct - num(most_common_vals))
```

```sql
DROP TABLE six;
CREATE TABLE IF NOT EXISTS six
(
    a INT,
    b VARCHAR(10),
    c INT
);

TRUNCATE six;
INSERT INTO six(a, b, c)
SELECT s.id, random_string(1), (RANDOM() * 500)::INT
FROM GENERATE_SERIES(1, 100000) AS s(id);

ANALYSE six;

SELECT * FROM pg_stats WHERE tablename = 'six';
```

##### Запрос без условия

```sql
EXPLAIN SELECT * FROM six;
```

```
Seq Scan on six  (cost=0.00..1541.00 rows=100000 width=11)
```

`rows` == `reltuples`

```sql
SELECT reltuples, relpages FROM pg_class WHERE relname = 'six';
```

| reltuples | relpages |
|:---------:|:--------:|
|   100000  |    541   |

##### Вычисление по `histogram_bounds`

```sql
ALTER TABLE six
    ALTER COLUMN a SET STATISTICS 10;

ANALYSE six;

SELECT null_frac
     , n_distinct
     , most_common_vals
     , most_common_freqs
     , histogram_bounds
FROM pg_stats
WHERE tablename = 'six'
  AND attname = 'a';

```

|    Attribute Name   | Value |
|---------------------|-------|
| `null_frac`         |    0  |
| `n_distinct`        |   -1  |
| `most_common_vals`  |  null |
| `most_common_freqs` |  null |
| `histogram_bounds`  | {1,10149,20413,30398,40358,50596,60352,70186,80247,89994,100000} |

```sql
EXPLAIN SELECT * FROM six WHERE a <= 25000;
```

Предполагается, что внутри гистограммы линейное распределение значений. Обрабатывается часть гистограммы, которая
соответствует условию (`1-10149`, `10149-20413`, количество полных интервалов = 2).

```
Seq Scan on six  (cost=0.00..1791.00 rows=24594 width=11)
  Filter: (a <= 25000)

selectivity = ((количество полных интервалов) + (current - bucket[3].min)/(bucket[3].max - bucket[3].min)) / num_buckets =
    (2 + (25000 - 20413) / (30398 - 20413)) / 10 = 0.24593890836

rows = reltuples * selectivity = 10000 * 0.24593890836 ~= 24594
```

##### Вычисление по `most_common_vals`

Если значение попадает в `most_common_vals`, то просто берется соответствующее значение из `most_common_freqs`.

```sql
SELECT null_frac
     , n_distinct
     , most_common_vals
     , most_common_freqs
     , histogram_bounds
FROM pg_stats
WHERE tablename = 'six'
  AND attname = 'b';
```

|    Attribute Name   | Value |
|---------------------|-------|
| `null_frac`         |    0  |
| `n_distinct`        |   26  |
| `most_common_vals`  | {G,I,M,Y,A,V,N,Q,O,L,B,C,T,P,R,W,H,X,D,U,Z,J,S,E,K,F} |
| `most_common_freqs` | {0.04100000113248825,0.03999999910593033,0.03983333334326744,0.03946666792035103,0.03933333232998848,0.039133332669734955,0.03906666487455368,0.03896666690707207,0.038866665214300156,0.03876666724681854,0.038733333349227905,0.038466665893793106,0.03843333199620247,0.03830000013113022,0.03830000013113022,0.03826666623353958,0.038233332335948944,0.03813333436846733,0.03776666522026062,0.037566665560007095,0.03739999979734421,0.03736666589975357,0.03736666589975357,0.037300001829862595,0.037166666239500046,0.036766666918992996} |
| `histogram_bounds`  |  null |

```sql
EXPLAIN SELECT * FROM six WHERE b = 'A';
```

```
Seq Scan on six  (cost=0.00..1791.00 rows=3933 width=11)
  Filter: ((b)::text = 'A'::text)
        
index of 'A' in most_common_vals = 5
selectivity = most_common_freq[5] = 0.03933333232998848
rows = reltuples * selectivity = 10000 * 0.03933333232998848 ~= 3933
```

Для оценки избирательности значения, которого нет в списке `most_common_vals` используется факт отсутствия данного
значения в списке в сочетании с частотой для каждого значения из списка `most_common_vals`. Т. е. нужно сложить частоты
значений из списка `most_common_vals`, отнять полученное число от единицы, и полученное значение разделить на количество
остальных уникальных значений.

Эти вычисления основаны на предположении, что значения, которые не входят в список `most_common_vals`, имеют равномерное
распределение.

```sql
ALTER TABLE six
    ALTER COLUMN b SET STATISTICS 10;

ANALYSE six;

SELECT null_frac
     , n_distinct
     , most_common_vals
     , most_common_freqs
     , histogram_bounds
FROM pg_stats
WHERE tablename = 'six'
  AND attname = 'b';
```

|    Attribute Name   | Value |
|---------------------|-------|
| `null_frac`         |    0  |
| `n_distinct`        |   26  |
| `most_common_vals`  | {I,V,A,M,J,B,H,C,G,Q} |
| `most_common_freqs` | {0.039500001817941666,0.039480000734329224,0.039329998195171356,0.03914999961853027,0.038839999586343765,0.03880999982357025,0.03880000114440918,0.03878000006079674,0.03863000124692917,0.03863000124692917} |
| `histogram_bounds`  | {D,E,K,L,O,P,S,U,W,Y,Z} |

`L` отсутствует в `most_common_vals`.

```sql
EXPLAIN SELECT * FROM six WHERE b = 'L';
```

```
Seq Scan on six  (cost=0.00..1791.00 rows=3813 width=11)
  Filter: ((b)::text = 'L'::text)

selectivity = (1 - sum(most_common_freqs)) / (n_distinct - length(most_common_values)) =
    (1 - 0.38995) / (26 - 10) = 0.038128125 
    
rows = reltuples * selectivity = 10000 * 0.038128125 ~= 3813

sum(m) = SELECT (SELECT sum(mcf) FROM unnest(stat.most_common_freqs) mcf) AS sum_mcf 
         FROM pg_stats stat
         WHERE stat.tablename = 'six'
            AND stat.attname = 'b';
```

##### Вычисление по `most_common_vals` и `histogram_bounds`

Предыдущий пример с поиском по `a` <= 25000 был большим упрощением, т.к. это уникальный столбец (`n_distinct` = 1), у
него нет значений в списке `most_common_vals` (т.к. все встречаются одинаково). Для неуникального столбца обычно
создаётся как гистограмма, так и список `most_common_vals`, при этом гистограмма не включает значения, представленные в
списке `most_common_vals`.

```
selectivity = mcv_selectivity + histogram_selectivity * histogram_fraction
```

```sql
ALTER TABLE six
    ALTER COLUMN c SET STATISTICS 10;

ANALYSE six;

SELECT null_frac
     , n_distinct
     , most_common_vals
     , most_common_freqs
     , histogram_bounds
FROM pg_stats
WHERE tablename = 'six'
  AND attname = 'c';
```

|    Attribute Name   |   Value  |
|---------------------|----------|
| `null_frac`         |      0   |
| `n_distinct`        |    501   |
| `most_common_vals`  | {328,26,278,70,127,159,308,61,305,370} |
| `most_common_freqs` | {0.00286666676402092,0.0027666667010635138,0.0027000000700354576,0.0026000000070780516,0.0026000000070780516,0.0026000000070780516,0.0026000000070780516,0.0025666665751487017,0.0025666665751487017,0.0025666665751487017} |
| `histogram_bounds`  | {0,52,103,153,202,250,300,352,403,451,500} |

В случае, если значения в столбце не уникальны, то оптимизатор применяет условие к каждому `most_common_vals` и
суммирует частоты `most_common_freqs`, для которых условие является верным. Это даёт точную оценку избирательности для
той части таблицы, которая содержит значения из списка `most_common_vals`. Подобным же образом используется гистограмма
для оценки избирательности для той части таблицы, которая не содержит значения из списка MCV, а затем эти две цифры
складываются для оценки общей избирательности.

```sql
EXPLAIN SELECT * FROM six WHERE c > 200;
```

```
Seq Scan on six  (cost=0.00..1791.00 rows=60141 width=10)
  Filter: (c > 200)
  
// Сумма все most_common_freqs, для которых их значение в most_common_vals удовлетворяет условию поиска
mvc_selectivity = SELECT SUM(r.mcf)
                  FROM pg_stats stat
                     , ROWS FROM (UNNEST(stat.most_common_vals::TEXT::INT[]),
                      UNNEST(stat.most_common_freqs)) r(mcv, mcf)
                  WHERE tablename = 'six'
                    AND attname = 'c'
                    AND r.mcv > 200;

mvc_selectivity = 0.0133

// Вычисление по histogram_bounds
histogram_selectivity = ((количество полных интервалов) + (current - bucket[4].min)/(bucket[4].max - bucket[4].min)) / num_buckets =
    1 - (3 + (200 - 153) / (202 - 153)) / 10 = 0.60408163265

// Количество значений, представленных гистограммой
histogram_fraction = 1 - sum(most_common_freqs)
histogram_fraction = SELECT 1 - (SELECT sum(mcf) FROM unnest(stat.most_common_freqs) mcf) AS sum_mcf
                     FROM pg_stats stat
                     WHERE stat.tablename = 'six'
                       AND stat.attname = 'c';

histogram_fraction = 0.9735666643828154    

selectivity = mcv_selectivity + histogram_selectivity * histogram_fraction =
    0.0133 + 0.60408163265 * 0.9735666643828154 = 0.60141374011
    
rows = reltuples * selectivity = 10000 * 0.60141374011 ~= 60141  
```

## Литература

1. [Производительность запросов в PostgreSQL – шаг за шагом](https://highload.guide/blog/query_performance_postgreSQL.html)
2. Оптимизация запросов. Основы EXPLAIN в PostgreSQL.
    1. [Часть 1](https://habr.com/ru/post/203320/)
    2. [Часть 2](https://habr.com/ru/post/203386/)
    3. [Часть 3](https://habr.com/ru/post/203484/)
3. Индексы в PostgreSQL.
    1. [Часть 1: Индексы](https://habr.com/ru/company/postgrespro/blog/326096/)
    2. [Часть 2](https://habr.com/ru/company/postgrespro/blog/326106/)
    3. [Часть 3: Hash](https://habr.com/ru/company/postgrespro/blog/328280/)
    4. [Часть 4: BTree](https://habr.com/ru/company/postgrespro/blog/330544/)
    5. [Часть 5: GiST](https://habr.com/ru/company/postgrespro/blog/333878/)
    6. [Часть 6: SP-GiST](https://habr.com/ru/company/postgrespro/blog/337502/)
    7. [Часть 7: GIN](https://habr.com/ru/company/postgrespro/blog/340978/)
4. [Индексы в PostgreSQL](https://habr.com/ru/company/postgrespro/blog/326096/)
5. [Row Estimation Examples](https://www.postgresql.org/docs/15/row-estimation-examples.html)
6. ["Под капотом" индексов Postgres](https://habr.com/xru/company/vk/blog/261871/)
7. [Индексы в PostgreSQL. Как понять, что создавать](https://www.youtube.com/watch?v=ju9F8OvnL4E)
8. [Антипаттерн orisnull: коварство иллюзорной простоты](https://www.youtube.com/watch?v=h927yUAdTD0)
9. [Index Maintenance](https://wiki.postgresql.org/wiki/Index_Maintenance)