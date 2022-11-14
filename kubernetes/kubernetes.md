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

# устанавливаем postgres
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

# тестовый запуск services (frontend + backend), без применения изменений на кластере
$ helm install services services-chart/ \
    --set simple-frontend.domain=local\
    --debug \
    --dry-run

$ echo "127.0.0.1    simple-frontend.local" | sudo tee -a /etc/hosts > /dev/null

# запускаем frontend и backend в кластер
$ helm install services services-chart/ --set simple-frontend.domain=local

# открыть в браузере http://simple-frontend.local

# удаление services
$ helm uninstall services postgres
```

```shell
$ git clone git@github.com:Romanow/micro-services-v2.git
$ cd k8s

$ helm install postgres postgres-chart/
$ helm install services services-chart/
$ helm install elasticsearch elasticsearch-chart/
$ helm install monitoring monitoring-chart/
$ helm install logging logging-chart/
$ helm install logging logging-chart/

$ helm repo add jaegertracing https://jaegertracing.github.io/helm-charts
$ helm search repo jaegertracing
NAME                            CHART VERSION   APP VERSION     DESCRIPTION
jaegertracing/jaeger            0.64.1          1.37.0          A Jaeger Helm chart for Kubernetes
jaegertracing/jaeger-operator   2.37.0          1.39.0          jaeger-operator Helm chart for Kubernetes

# устанавливаем репозиторий с cert manager
$ helm repo add jetstack https://charts.jetstack.io  
$ helm repo update

# устанавливаем cert manager CRD
$ kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.10.0/cert-manager.crds.yaml

# устанавливаем cert manager
$ helm install \                                                                                         master 
      cert-manager jetstack/cert-manager \
      --namespace cert-manager \
      --create-namespace \
      --version v1.10.0

# устанавливаем Jaeger Operator
$ helm install jaeger-operator jaegertracing/jaeger-operator
```

## Литература

2. [Helm](https://helm.sh/docs/)
1. [Kompose User Guide](https://kompose.io/user-guide/)
