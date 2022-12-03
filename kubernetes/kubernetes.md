# Kubernetes

Kubernetes — это портативная расширяемая платформа с открытым исходным кодом для управления контейнеризованными рабочими
нагрузками и сервисами, которая облегчает как декларативную настройку, так и автоматизацию.

Kubernetes предоставляет:

* Мониторинг сервисов и распределение нагрузки: Kubernetes может обнаружить контейнер, используя имя DNS или собственный
  IP-адрес. Если трафик в контейнере высокий, Kubernetes может сбалансировать нагрузку и распределить сетевой трафик,
  чтобы развертывание было стабильным.
* Оркестрация хранилища Kubernetes позволяет автоматически смонтировать систему хранения, такую как локальное хранилище,
  провайдеры общедоступного облака и многое другое.
* Автоматическое развертывание и откаты: используя Kubernetes можно описать желаемое состояние развернутых контейнеров и
  изменить фактическое состояние на желаемое. Например, можно автоматизировать Kubernetes на создание новых контейнеров
  для развертывания, удаления существующих контейнеров и распределения всех их ресурсов в новый контейнер.
* Автоматическое распределение нагрузки: Kubernetes имеет набор нод с ресурсам, который он может использовать для
  запуска контейнерных задач, при деплое можно указать Kubernetes, сколько CPU и памяти требуется каждому контейнеру.
* Самоконтроль: Kubernetes перезапускает отказавшие контейнеры, заменяет и завершает работу контейнеров, которые не
  проходят определенную пользователем проверку работоспособности, и не показывает их клиентам, пока они не будут готовы
  к обслуживанию.
* Управление конфиденциальной информацией и конфигурацией: Kubernetes может хранить и управлять конфиденциальной
  информацией, такой как пароли, OAuth-токены и ключи SSH и т.д..

Обычно управление кластером Kubernetes выполняется из командной строки с помощью CLI `kubectl`.

На вход `kubectl` подается манифест с описанием желаемого состояния кластера. Получив эту информацию Kubernetes пытается
привести текущее состояние к желаемому с помощью генератора событий жизненного цикла подов (Pod Lifecycle Event
Generator). Для этого Kubernetes автоматически выполняет множество задач, таких как запуск или перезапуск контейнеров,
масштабирование количества реплик данного приложения и многое другое.

Кластер Kubernetes разворачивается на нодах, при этом каждая нода может быть в роли Master или Worker. Master ноды
занимаются управлением кластером и распределением задач на Worker. Worker используются для запуска контейнеров.
Kubernetes состоит из набора процессов и демонов на каждой ноде в кластере.

### Стурктура кластера Kubernetes

![Kubernetes](images/kubernetes/kubernetes.png)

На Master нодах в Kubernetes запускаются процессы:

* `kube-apiserver` (API-сервер) – клиентская часть панели управления кластером.
* `kube-controller-manager` – Компонент Control Plane запускает процессы контроллера. Каждый контроллер в свою очередь
  представляет собой отдельный процесс, и для упрощения все такие процессы скомпилированы в один двоичный файл и
  выполняются в одном процессе. Эти контроллеры включают:
    * Node Controller – уведомляет и реагирует на сбои узла.
    * Replication Controller – поддерживает правильное количество подов для каждого объекта контроллера репликации в
      системе.
    * Endpoints Controller – заполняет Endpoints, то есть связывает `Services` и `Pods`.
    * Account & Token Controllers – создают стандартные учетные записи и токены доступа API для новых пространств имен.
* `kube-scheduler` – компонент плоскости управления, который отслеживает созданные поды без привязанного узла и выбирает
  узел, на котором они должны работать. При планировании развёртывания подов на узлах учитываются множество факторов,
  включая требования к ресурсам, ограничения, связанные с аппаратными/программными политиками, принадлежности (affinity)
  и непринадлежности (anti-affinity) узлов/подов, местонахождения данных, предельных сроков.
* `cloud-controller-manager` – запускает контроллеры, которые взаимодействуют с основными облачными провайдерами. С
  помощью `cloud-controller-manager` код как облачных провайдеров, так и самого Kubernetes может разрабатываться
  независимо друг от друга.

Так же там запущено key-value хранилище `etcd`, которое используется как основое хранилище всех данных в кластере
Kubernetes. Master координирует все процессы в кластере, такие как планирование выполнения приложений, сохранение
требуемого состояния приложений, а также их масштабирование и обновление.

Каждая Worker нода в кластере выполняет два процесса:

* `kubelet` – Агент, работающий на каждом узле в кластере. Он следит за тем, чтобы контейнеры были запущены в поде.
  Утилита `kubelet` принимает набор `PodSpecs`, и гарантирует работоспособность и исправность определённых в них
  контейнеров.
* `kube-proxy` – сетевой прокси, работающий на каждом узле в кластере. `kube-proxy`
  конфигурирует правила сети на узлах, при помощи них разрешаются сетевые подключения к `Pod` изнутри и снаружи
  кластера. `kube-proxy` использует уровень фильтрации пакетов в операционной системы, если он доступен. В противном
  случае, `kube-proxy` сам обрабатывает передачу сетевого трафика.

Kubernetes содержит ряд абстракций, которые представляют состояние системы: развернутые контейнеризованные приложения и
рабочие нагрузки, связанные с ними сетевые и дисковые ресурсы и другую информацию, что делает кластер.

Кластер Kubernetes состоит из набора машин, называемых нода (Node), которые запускают контейнеризированные приложения.
Каждая нода содержит поды (Pod) – минимальная сущность для развертывания в кластере. K8S управляет подами, а не
контейнерами напрямую.

### Основные объекты Kubernetes

##### Pod

`Pod` – минимальная сущность для развертывания в кластере. Каждый `Pod` предназначен для запуска одного (обычно)
экземпляра конкретного приложения. Если есть необходимость горизонтального масштабирования, то можно запустить несколько
экземпляров `Pod` - в терминологии Kubernetes это называется репликацией.

#### Service

Абстракция, которая определяет логический набор подов и политику доступа к ним, как сетевой сервис. `Pod`
создаются и удаляются, чтобы поддерживать описанное состояние кластера. Каждый pod имеет свой ip-адрес, но эти адреса не
постоянны и могут меняться со временем
(при переезде между нодами, например).

##### Volumes

Персистентное хранилище данных внутри кластера. По-умолчанию используется `emptyDir` – volume создается на диске и
существует до тех пор, пока `Pod` работает на этой ноде. `ConfigMaps` так же могут использоваться как volume для
конфигурирования приложения.

##### Namespace

`Namespace` – это виртуальные кластеры размещенные поверх физического.

##### Secrets

`Secrets` используются для хранения конфиденциальной информации.

Kubernetes также содержит абстракции более высокого уровня, которые опираются на Контроллеры (`Controller`) для создания
базовых объектов и предоставляют дополнительные функциональные и удобные функции. Они включают:

##### Deployment

`Deployment` обеспечивает декларативные обновления для `Pods` и `ReplicaSets`. Наиболее распространенный тип описания
ресурсов, состоит из секции описания `Pod` (`.spec.template`), `Labels` (`.spec.template.metadata.labels`), информация о
репликации (`.spec.replicas`).

##### DaemonSet

`DaemonSet` гарантирует, что определенный Pod будет запущен на всех нодах.

##### StatefulSet

`StatefulSet` используется для управления приложениями с сохранением состояния.

##### ReplicaSet

`ReplicaSet` гарантирует, что определенное количество экземпляров `Pod` будет запущено в кластере в любой момент
времени.

Есть еще несколько служебных сущностей, которые упрощают работу с k8s:

##### Labels

`Labels` используются для маркирования объектов кластера, а так же для выбора этих
объектов `kubectl get pods -l app=simple-backend`.

##### ConfigMaps

`ConfigMaps` – абстракция над файлами конфигурации, позволяет разделять настройки приложения и сами контейнеры, избавляя
от необходимости упаковывать конфиги в docker-образ.

##### Annotations

`Annotations` используются для добавления собственных метаданных к объектам. Такие клиенты, как инструменты и
библиотеки, могут получить эти метаданные. Эту информацию можно хранить в БД или файлах, но это усложняет процесс
создания общих клиентских библиотек. Некоторые примеры информации, которая может быть в аннотациях:

* Поля, управляемые декларативным уровнем конфигурации. Добавление этих полей в виде аннотаций позволяет отличать их от
  значений по умолчанию, установленных клиентами или серверами, а также от автоматически сгенерированных полей и полей,
  заданных системами автоматического масштабирования.
* Информация репозитории, сборке, выпуске или образе.
* Информация об источнике пользователя или инструмента/системы, например, URL-адреса связанных объектов из других
  компонентов экосистемы.

## Внешняя маршрутизация

При публикации сервиса есть три типа указания внешнего адреса:

* NodePort – открывает указанный порт для всех nodes, и трафик с этого порта отправляется в сервис.
  ![NodePort](images/kubernetes/nodeport.png)
* ClusterIP – обеспечивает сервис внутри кластера, к которому могут обращаться другие приложения внутри кластера.
  Внешнего доступа нет.
* LoadBalance – балансировщик нагрузки, выставляется наружу за пределы кластера.
  ![LoadBalancer](images/kubernetes/loadbalancer.png)
* Ingress – reverse proxy, под капотом использует `LoadBalancer`, трафик от которого маршрутизируется внутри кластера в
  соответствии с правилами в `Ingress`.
  ![Ingress](images/kubernetes/ingress.png)

## Сетевая модель

В основе сетевого устройства Kubernetes — у каждого пода свой уникальный IP. IP пода делится между всеми его
контейнерами и является доступным (маршрутизируемым) для всех остальных подов. На каждой машине есть сетевой интерфейс
eth0, внутри пода тоже есть eth0, на host-машине они подключены к интерфейсу vethxxx. Эти интерфейсы общаются с eth0
через ethernet bridge интерфейс cni0 (docker использует аналогичный docker0).

Взаимодействие между узлами реализуется либо посредством ARP-запросов (L2), либо с помощью таблицы роутинга
(ip-маршрутизация, L3). Для более гибкой маршрутизации строятся overlay-сети. Overlay-сеть выглядит как единая сеть
между нодами.

![Routing Table](images/kubernetes/rounting_table.png)

Предположим, нужно выполнить запрос из `Pod` 1 на одной ноде к `Pod` 3 на другой ноде. Запрос из пода попадает в
интерфейс cni0, а потом в интерфейс `flannel.1`, который оборачивает запрос в UDP-пакет и отправляет его дальше через
интерфейс eth0. Если нужный `Pod` находится на той же машине, то маршрутизация решается на уровне cni0 интерфейса.

![Overlay network](images/kubernetes/overlay_network.png)

### RBAC (Role Based Account Control)

RBAC (Role-based access control) — это система распределения прав доступа к различным объектам в кластере Kubernetes.
Объекты в кластере Kubernetes — это YAML-манифесты, а права доступа определяют, какому пользователю можно только
просматривать манифесты, а кто может их создавать, изменять или даже удалять.

##### ServiceAccount

ServiceAccount используется для ограничения прав ПО, которое работает в кластере. Всё общение между компонентами
кластера идёт через запросы к API-серверу, и каждый такой запрос как раз авторизуется специальным JWT-токеном. Этот
токен генерируется при создании объекта типа ServiceAccount и кладётся в secret.

В отличие от обычного пользователя, которому мы можем задать произвольный пароль, JWT-токен содержит внутри себя
служебную информацию с названием ServiceAccount, Namespace и подписан корневым сертификатом кластера.

Свой сервис аккаунт default есть в каждом namespace, он создаётся автоматически. По умолчанию прав у этого аккаунта на
доступ к API нет никаких.

```shell
$ kubectl describe pod \
    -l app.kubernetes.io/name=ingress-nginx 
    -l app.kubernetes.io/component=controller \
    -n ingress-nginx

Name:         ingress-nginx-controller-6bccc5966-l6rc5
Namespace:    ingress-nginx
    
Containers:
  controller:
    ...
    Mounts:
      /usr/local/certificates/ from webhook-cert (ro)
      /var/run/secrets/kubernetes.io/serviceaccount from kube-api-access-bm92n (ro)

# берем токен и расшифровываем в jwt.io
$ kubectl exec -it ingress-nginx-controller-6bccc5966-l6rc5 -n ingress-nginx -- cat /var/run/secrets/kubernetes.io/serviceaccount/token 
{
  "aud": [
    "https://kubernetes.default.svc.cluster.local"
  ],
  "exp": 1700933304,
  "iat": 1669397304,
  "iss": "https://kubernetes.default.svc.cluster.local",
  "kubernetes.io": {
    "namespace": "ingress-nginx",
    "pod": {
      "name": "ingress-nginx-controller-6bccc5966-l6rc5",
      "uid": "9aa52828-04c4-489d-91ab-cd02f5685c75"
    },
    "serviceaccount": {
      "name": "ingress-nginx",
      "uid": "a07e7ab7-b249-443d-9309-2e3d7b3a47a5"
    },
    "warnafter": 1669400911
  },
  "nbf": 1669397304,
  "sub": "system:serviceaccount:ingress-nginx:ingress-nginx"
}
```

#### Role

Role — это YAML-манифест, который описывает некий набор прав на объекты кластера Kubernetes.

```shell
$ kubectl get role -n ingress-nginx ingress-nginx -o yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: ingress-nginx
  namespace: ingress-nginx
rules:
- apiGroups: [ "" ]
  resources: [ "namespaces" ]
  verbs:
  - get
- apiGroups: [ "networking.k8s.io" ]
  resources: [ "ingresses" ]
  verbs:
  - get
  - list
  - watch
- apiGroups: [ "networking.k8s.io" ]
  resources: [ "ingresses/status" ]
  verbs:
  - update

$ kubectl describe role -n ingress-nginx ingress-nginx
Name:         ingress-nginx
PolicyRule:
  Resources                           Non-Resource URLs  Resource Names          Verbs
  ---------                           -----------------  --------------          -----
  events                              []                 []                      [create patch]
  leases.coordination.k8s.io          []                 []                      [create]
  configmaps                          []                 []                      [get list watch create]
  endpoints                           []                 []                      [get list watch]
  pods                                []                 []                      [get list watch]
  secrets                             []                 []                      [get list watch]
  services                            []                 []                      [get list watch]
  ingressclasses.networking.k8s.io    []                 []                      [get list watch]
  ingresses.networking.k8s.io         []                 []                      [get list watch]
  configmaps                          []                 [ingress-nginx-leader]  [get update]
  leases.coordination.k8s.io          []                 [ingress-nginx-leader]  [get update]
  namespaces                          []                 []                      [get]
  endpointslices.discovery.k8s.io     []                 []                      [list watch get]
  ingresses.networking.k8s.io/status  []                 []                      [update]
```

* `apiGroups` — описывает API-группу манифеста. Это то, что написано в поле apiVersion: до `/`. Если в `apiVersion`
  указана только версия, без группы, например, как в манифесте Pod, то считается, что у этого манифеста так называемая
  корневая группа (core-group); в роли корневой группы указывается как пустая строка "".
* `resourсes` — список ресурсов, к которым описывается доступ, во множественном числе. Посмотреть список ресурсов в
  кластере можно командой `kubectl api-resources`. Также есть подресурсы, описывающие специфические действия, например,
  подресурс `pods/log` разрешает просматривать логи контейнеров в поде.
* `verbs` — список действий, которые можно сделать с ресурсами, описанными выше: получить, посмотреть список, следить за
  изменением, отредактировать, удалить и т.п. Verbs описывают HTTP REST (GET, PUT, POST, DELETE) или, если более сложное
  действие (`watch`, `escalate`), то кодируется в URL.

Role описывает права в namespace. ClusterRole — это кластерный объект, сущность описывает права на объекты во всём
кластере.

#### RoleBinding, ClusterRoleBinding

С помощью механизма RoleBinding мы связываем Role и ServiceAccount. `roleRef` ссылается на роль, а `subjects` указывает
к какому ServiceAccount она принадлежит.

```shell
$ kubectl get rolebinding ingress-nginx -n ingress-nginx -o yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: ingress-nginx
  namespace: ingress-nginx
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: ingress-nginx
subjects:
- kind: ServiceAccount
  name: ingress-nginx
  namespace: ingress-nginx
```

RoleBinding даёт доступ только к тем сущностям, которые находятся в том же namespace, что и манифест RoleBinding.
ClusterRoleBinding позволяет выдать доступ к сущностям во всех namespace кластера сразу.

### Kubernetes Operator

#### CRD (Custom Resource Definition)

Ресурс — это endpoint в Kubernetes API, в котором хранится набор объектов API определенного Kind; например, встроенный
ресурс `pods` содержит коллекцию объектов Pod.

Custom Resource — это расширение API Kubernetes, которое не обязательно доступно по умолчанию и представляет собой
надстройку над Kubernetes. Custom Resources могут создаваться и удаляться в работающем кластере посредством динамической
регистрации, а администраторы могут обновлять пользовательские ресурсы независимо от самого кластера. После установки
пользовательского ресурса пользователи могут создавать и получить доступ к своим объектам с помощью `kubectl`, как и к
встроенным ресурсам, таким как Pods.

#### Operators

_Оператор_ – это человек, который управляет какой-то сложной системой.

В Kubernetes Operator — это программное расширение, которое использует декларативные CustomResource для управления
приложениями и их составными частями. Другими словами, операторы – это потребители Kubernetes API, которые действуют как
контроллеры для CustomResource.

Операторы подключаются к Kubernetes API и следят за соответствующими событиями. Они действуют как настраиваемые
контроллеры Kubernetes, вводя в кластер собственные типы объектов, которые получают информацию об обновлении
отслеживаемых объектов и выполняют изменения в подчиненных ресурсах.

Требуемое состояние описывается пользователем в YAML, создающим объекты Kubernetes в качестве пользовательского ресурса.
Оператор выполняет свой цикл всякий раз, когда такие объекты появляются, обновляются или удаляются. Операторы работают
как Pods в кластере.

Другими словами, операторы декларативно получают настройки и выполняют сложную настройку ресурсов на основе внутренней
логики. Например, оператор, ответственный за деплой Postgres в кластер Kubernetes, может на основе параметров развернуть
БД как один Pod или создать несколько Pod и настроить Master-Slave репликацию. За счет того, что движок оператора
является контроллером, т.е. по сути приложением, он может делать более сложные вещи, чем просто манифесты helm чартов.

## Пример

Есть возможность конвертировать существующие Docker Compose файлы в манифесты k8s.

```shell
$ kompose convert --controller deployment --out k8s/ --with-kompose-annotation=false
WARN Service "simple-backend" won't be created because 'ports' is not specified 
INFO Kubernetes file "k8s/simple-frontend-service.yaml" created 
INFO Kubernetes file "k8s/simple-backend-deployment.yaml" created 
INFO Kubernetes file "k8s/simple-frontend-deployment.yaml" created 
```

#### Развертывание Managed k8s cluster на DigitalOcean и запуск приложений в кластере.

Создание кластера k8s в DigitalOcean с помощью [terraform](https://www.terraform.io/intro/index.html) и
[terragrunt](https://terragrunt.gruntwork.io/.

```shell
$ git clone https://github.com/Romanow/terraform-do-k8s.git

# получаем DigitalOcean Token (https://docs.digitalocean.com/reference/api/create-personal-access-token/)
# создается k8s кластер из 3х нод 4CPU, 8Gb памяти в региона AMS, настраивается ingress (создается LoadBalancer)
# и создаются DNS записи в домене romanow-alex.ru
$ cd dev/kubernetes
$ terragrunt apply -auto-approve

$ doctl kubernetes cluster kubeconfig save terragrunt-cluster
```

Деплой приложения в кластер.

```shell
$ kubectl apply -f postgres
service/postgres created
configmap/postgres-config created
deployment.apps/postgres created

$ kubectl apply -f simple-backend.yml 
service/simple-frontend created
deployment.apps/simple-backend created

# проброс локального порта в контейнер
$ kubectl port-forward <pod-name> 8080:8080
Forwarding from 127.0.0.1:8080 -> 8080
Forwarding from [::1]:8080 -> 8080

$ curl 'http://localhost:8080/backend/?person=docker'
Hello from docker container                   

$ kubectl apply -f simple-frontend.yml 
service/simple-frontend configured
deployment.apps/simple-frontend created

$ kubectl get pods
NAME                               READY   STATUS    RESTARTS   AGE
postgres-54df449488-wj7g2          1/1     Running   0          3m55s
simple-backend-7f4cb85ff9-c69sq    1/1     Running   0          3m28s
simple-frontend-5db47b7c64-znq5s   1/1     Running   0          96s

$ kubectl get svc -n nginx-ingress
NAME                         TYPE           CLUSTER-IP     EXTERNAL-IP     PORT(S)                      AGE
nginx-stable-nginx-ingress   LoadBalancer   10.245.98.52   167.99.16.213   80:31075/TCP,443:32052/TCP   15m

$ kubectl get ingress
NAME              CLASS    HOSTS                             ADDRESS         PORTS   AGE
ingress-service   <none>   simple-frontend.romanow-alex.ru   167.99.16.213   80      2m27s

# для использования type: LoadBalancer нужно выключить ingress
$ helm uninstall nginx-stable -n nginx-ingress
release "nginx-stable" uninstalled

$ kubectl apply -f loadbalancer.yml 
service/simple-frontend configured

# для simple-frontend будет создан новый физический LoadBalancer, поэтому в DNS нужно будет поменять ip адрес
$ kubectl get services 
NAME              TYPE           CLUSTER-IP       EXTERNAL-IP                       PORT(S)         AGE
kubernetes        ClusterIP      10.245.0.1       <none>                            443/TCP         27m
postgres          ClusterIP      10.245.172.128   <none>                            5432/TCP        12m
simple-backend    ClusterIP      10.245.241.29    <none>                            8080/TCP        9m12s
simple-frontend   LoadBalancer   10.245.138.82    simple-frontend.romanow-alex.ru   443:31849/TCP   12m
```

Открыть в браузере `https://simple-frontend.romanow-alex.ru`

### Установка с помощью Helm

```shell
# create local cluster
$ kind create cluster --config kind.yml

# configure ingress
$ kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

# скачиваем Simple Backend
$ git clone git@github.com:Romanow/simple-backend.git

# устанавливаем postgres
$ cd simple-backend/k8s
$ helm install postgres postgres-chart/

# обновление postgres
$ helm upgrade \
  --set image.version=14 \
  --description 'Update to Postgres 14' \
  postgres postgres-chart/

# получение истории изменений postgres
$ helm history postgres
REVISION	UPDATED                 	STATUS  	CHART         	APP VERSION	DESCRIPTION     
1       	Wed Dec 15 12:33:33 2021	superseded	postgres-1.0.0	           	Install complete
2       	Wed Dec 15 12:57:32 2021	deployed  	postgres-1.0.0	           	Update to Postgres 14

# тестовый запуск Simple Backend без применения изменений на кластере
$ helm install simple-backend service-chart/ --debug --dry-run

# устанавливаем Simple Backend
$ helm install simple-backend service-chart/

# скачиваем Simple Frontend
$ git clone git@github.com:Romanow/simple-frontend.git
$ echo "127.0.0.1    simple-frontend.local" | sudo tee -a /etc/hosts > /dev/null

# устанавливаем Simple Frontend
$ helm install simple-frontend frontend-chart/ --set domain=simple-frontend.local

# открыть в браузере http://simple-frontend.local

# удаление services
$ helm uninstall simple-backend simple-frontend postgres
```

## Литература

1. [Собственный Kubernetes оператор за час](https://www.youtube.com/watch?v=tFzM-2pwL8A)
2. [Продвинутые абстракции Kubernetes: Job, CronJob, RBAC](https://www.youtube.com/watch?v=fUBpMbHsfL4)
3. [Сеть Kubernetes, отказоустойчивый setup кластера](https://www.youtube.com/watch?v=JNUD9j9QAnA)
4. [Helm. Темплейтирование приложений Kubernetes](https://www.youtube.com/watch?v=me6-_gmfFPo)
