# 📋 Order Notification System

[![Tech Stack](https://img.shields.io/badge/Stack-Spring%20Boot%203%20%7C%20RabbitMQ%20%7C%20PostgreSQL%20%7C%20Maven-green)](#)
[![Status](https://img.shields.io/badge/Status-Completed-brightgreen)](#)
[![Security](https://img.shields.io/badge/Security-DLQ%20%26%20Isolation-orange)](#)

O **Order Notification System** é uma demonstração de arquitetura de microserviços orientada a eventos (Event-Driven Architecture) de nível corporativo projetada usando **Java 21**, **Spring Boot 3.4.3**, **RabbitMQ** e **PostgreSQL**. O sistema resolve o problema clássico de acoplamento de sistemas ao segmentar o fluxo de processamento de pedidos e notificações de entrega através de canais assíncronos de mensageria altamente confiáveis.

O projeto demonstra padrões essenciais de sistemas distribuídos modernos, incluindo a publicação transacional de eventos, descarte seguro de mensagens com fila de reprocessamento por falhas (Dead Letter Queue - DLQ), modelagem de eventos compartilhados via biblioteca centralizada e isolamento completo de persistência de dados.

---

## 📐 1. Arquitetura & Componentes Internos

### Mapeamento da Estrutura de Pastas
```text
order-notification-system/
├── shared-events/             # Módulo Maven compartilhado de contratos
│   ├── src/main/java/com/example/events/
│   │   └── OrderCreatedEvent.java # Payload padronizado do evento de pedido criado
│   └── pom.xml
├── order-service/             # Microserviço produtor de Pedidos (Porta 8080)
│   ├── src/main/
│   │   ├── java/com/ecommerce/orderservice/
│   │   │   ├── config/        # Configurações de conexões e RabbitMQ Exchange/Queue
│   │   │   ├── controller/    # REST Controller para criação de pedidos
│   │   │   ├── domain/        # Entidades persistentes de domínio (Order, OrderStatus)
│   │   │   ├── dto/           # Data Transfer Objects para requisições de API
│   │   │   ├── publisher/     # Publicadores de eventos no Broker
│   │   │   └── repository/    # Interface Spring Data JPA para PostgreSQL
│   │   └── resources/
│   │       └── application.yml
│   └── pom.xml
├── notification-service/      # Microserviço consumidor de Notificações (Porta 8081)
│   ├── src/main/
│   │   ├── java/com/ecommerce/notificationservice/
│   │   │   ├── config/        # Configuração de filas e bindings para processamento
│   │   │   ├── consumer/      # Consumidor de mensagens da fila principal e da DLQ
│   │   └── resources/
│   │       └── application.yml
│   └── pom.xml
├── docker-compose.yml         # Orquestração local do RabbitMQ e do PostgreSQL
└── pom.xml                    # Arquivo POM Pai coordenador do Multi-Module
```

### Diagrama de Fluxo de Dados e Componentes
```text
 +-------------------+             +-----------------------+
 |  REST Client      |             |  Order Service        |
 |  (cURL/Postman)   +---[HTTP]--->|  - Port 8080          |
 +-------------------+             |  - Spring Data JPA    |
                                   +-----------+-----------+
                                               |
                                        [Write Order]
                                               v
                                   +-----------+-----------+
                                   |  PostgreSQL Database  |
                                   |  - Schema: eventdb    |
                                   +-----------------------+
                                               |
                                     [Publish OrderEvent]
                                               v
                                 +-------------+-------------+
                                 |  RabbitMQ Message Broker  |
                                 |  - Exchange: order-exch   |
                                 +------+-------------+------+
                                        |             |
                                 [Main Routing]     [Dead-Letter Routing]
                                        v             v
                                 +------+------+   +--+----------+
                                 | order-queue |   |  order-dlq  |
                                 +------+------+   +--+----------+
                                        |             |
                                  [AmqpListener]   [DlqListener]
                                        v             v
                                 +------+-------------+------+
                                 |  Notification Service     |
                                 |  - Port 8081              |
                                 |  - Sends SMS/Email Alerts |
                                 +---------------------------+
```

### Tabela de Tecnologias & Dependências

| Componente | Tecnologia | Versão Standard | Descrição / Papel no Ecossistema |
|------------|------------|-----------------|----------------------------------|
| **Linguagem Base** | Java 21 | JDK 21 | Runtime estável de alta performance |
| **Framework Web** | Spring Boot | 3.4.3 | Infraestrutura de injeção e ciclo de vida |
| **Broker de Mensagens** | RabbitMQ | 3.12-management | Engine de AMQP para desacoplamento e eventos |
| **Banco de Dados** | PostgreSQL | 15-alpine | Camada relacional de persistência do Order Service |
| **ORM** | Hibernate (Spring Data JPA) | Incorporado | Mapeamento relacional de tabelas de pedidos |
| **Gerenciador de Build** | Maven | 3.9+ | Orquestração do projeto multi-module e dependências |
| **Serialização** | Jackson AMQP Support | Incorporado | Conversão automática de payloads para JSON no RabbitMQ |

---

## 🚀 2. Guia de Instalação & Pré-requisitos

### Pré-requisitos de Sistema
Para compilar, testar e executar os microserviços, certifique-se de possuir:
* **Java Development Kit (JDK) 21** instalado e configurado no PATH.
* **Apache Maven 3.9+** instalado.
* **Docker & Docker Compose** (para subir o RabbitMQ e o PostgreSQL).

### Passo a Passo para Instalação Local

1. **Clonar o Repositório:**
   ```bash
   git clone <URL_DO_REPOSITORIO>
   cd order-notification-system
   ```

2. **Subir a Infraestrutura com Docker Compose:**
   ```bash
   docker-compose up -d
   ```
   Isso criará as instâncias do PostgreSQL (porta `5432`) e do RabbitMQ com console administrativo (porta `15672`).

3. **Compilar todos os Módulos Maven:**
   ```bash
   mvn clean install
   ```
   Isso compilará todos os subprojetos e instalará o módulo `shared-events` no repositório Maven local.

---

## ⚙️ 3. Configuração & Variáveis de Ambiente

Os microserviços possuem propriedades parametrizadas prontas para customização via variáveis de ambiente do sistema operacional.

### Configuração Geral

| Nome da Variável | Descrição | Valor Padrão | Microserviço Aplicado | Sensível? |
|------------------|-----------|--------------|-----------------------|-----------|
| `SPRING_DATASOURCE_URL` | String de conexão JDBC com o Postgres | `jdbc:postgresql://localhost:5432/eventdb` | `order-service` | Sim |
| `SPRING_DATASOURCE_USERNAME` | Usuário mestre do banco de dados | `postgres` | `order-service` | Não |
| `SPRING_DATASOURCE_PASSWORD` | Senha mestre do banco de dados | `postgres` | `order-service` | Sim |
| `SPRING_RABBITMQ_HOST` | Host da instância de RabbitMQ | `localhost` | Ambos | Não |
| `SPRING_RABBITMQ_PORT` | Porta AMQP do RabbitMQ | `5672` | Ambos | Não |
| `SPRING_RABBITMQ_USERNAME` | Usuário de autenticação do broker | `guest` | Ambos | Não |
| `SPRING_RABBITMQ_PASSWORD` | Senha de autenticação do broker | `guest` | Ambos | Sim |

---

## 🛠️ 4. Instruções de Execução e Uso

### Executando em Modo de Desenvolvimento (Local)

1. **Garantir que os containers estão rodando:**
   ```bash
   docker ps
   ```

2. **Iniciar o Order Service:**
   ```bash
   cd order-service
   mvn spring-boot:run
   ```

3. **Iniciar o Notification Service (em outro terminal):**
   ```bash
   cd notification-service
   mvn spring-boot:run
   ```

### Exemplos Práticos de Uso

#### Criar um Pedido via API REST (Dispara Publicação de Evento)
```bash
curl -X POST http://localhost:8080/api/orders \
     -H "Content-Type: application/json" \
     -d '{
       "customerId": "cust-9988",
       "amount": 149.90,
       "items": ["Teclado Mecânico RGB", "Mousepad Gamer"]
     }'
```

**Resposta Esperada (JSON):**
```json
{
  "id": 1,
  "customerId": "cust-9988",
  "amount": 149.90,
  "status": "CREATED",
  "createdAt": "2026-07-08T12:00:00Z"
}
```

No console do `notification-service`, você verá o log de processamento assíncrono:
```text
[notification-service] INFO - Received event: OrderCreatedEvent(orderId=1, customerId=cust-9988, amount=149.9)
[notification-service] INFO - Notification successfully dispatched to user cust-9988
```

---

## 🧪 5. End-to-End Testing & Qualidade

Cada subprojeto possui uma suite automatizada baseada em **JUnit Jupiter (JUnit 5)** e ferramentas nativas de teste do Spring Boot (`@SpringBootTest`).

### Executando Testes Automatizados
A partir do diretório raiz do ecossistema:
```bash
mvn test
```
Este comando executa todos os testes de todos os módulos. Os testes garantem:
* Inicialização bem sucedida do contexto do Spring Boot.
* Validação de serialização/deserialização do payload `OrderCreatedEvent`.
* Verificação da publicação de mensagens AMQP.

---

## 🛡️ 6. Aspectos de Segurança & Governança (AppSec)

A arquitetura do Order Notification System foi estruturada focando na resiliência e na governança operacional de dados:

1. **Arquitetura Dead Letter Queue (DLQ):**
   Para evitar perda de mensagens devido a falhas intermitentes (erros de rede, indisponibilidade do gateway de SMS, dados corruptos), a fila principal de pedidos redireciona mensagens rejeitadas sem re-enfileiramento direto (`requeue = false`) para uma Dead Letter Exchange. As mensagens falhas são armazenadas em `order-dlq` e processadas separadamente sem degradar o fluxo principal do sistema.

2. **Isolamento de Persistência:**
   O `notification-service` não acessa o banco de dados do `order-service`. Toda e qualquer alteração de estado é repassada exclusivamente via mensagens, o que blinda a infraestrutura interna de transações concorrentes indesejadas e impede vazamentos de PII desnecessários.

3. **Validação Estrita de Schemas:**
   A comunicação baseada no módulo `shared-events` garante que os dois serviços respeitem exatamente a mesma assinatura e tipos de dados, reduzindo as chances de ataques de injeção de carga útil no deserializador JSON.

---

## 💡 7. Troubleshooting & Solução de Problemas Comuns

| Sintoma de Erro | Causa Provável | Ação Corretiva / Resolução |
|-----------------|----------------|----------------------------|
| `AmqpConnectException: Connection refused` | O container do RabbitMQ não está ativo ou não completou o bootstrap | Verifique se o container está rodando pelo comando `docker ps`. Se sim, aguarde 10 segundos ou revise se a porta `5672` está sendo ocupada por outro processo. |
| `SQLState: 08001 / Connection refused` | O microserviço `order-service` não consegue se conectar ao Postgres | Certifique-se de que o container do Postgres está ativo. Se rodando localmente sem Docker, ajuste a variável `SPRING_DATASOURCE_URL` com as credenciais corretas. |
| `Message rejected: Infinite Loop in DLQ` | Erro constante de deserialização de payload corrupto no consumidor | Certifique-se de que as propriedades das filas no painel de administração (`http://localhost:15672`) configuraram `x-dead-letter-exchange` para evitar loops infinitos entre a fila e a DLQ. |
| `Maven build failure on shared-events` | Módulos executados fora da ordem de compilação correta | Execute `mvn clean install` a partir da raiz do projeto ao invés de compilar subpastas isoladamente na primeira execução. |
