# 💳 Sistema de Pagamentos

Este é um sistema de pagamentos simplificado, com funcionalidades básicas de autenticação, transações e gerenciamento de usuários, idealizado para compor meu portfólio.

## 🚀 Tecnologias utilizadas

- **Java 17**
- **Spring Boot**
- **Spring Security + JWT**
- **PostgreSQL**
- **Flyway** (migrations)
- **Docker** (containerização futura)
- **AWS** (hospedagem futura)

---

## 👥 Tipos de Usuário

- **Usuário Comum** – Pode realizar transações
- **Comerciante** – Pode receber transações
- **Administrador** – Gerencia o sistema

---

## 🔐 Autenticação

A autenticação é feita via JWT. O fluxo básico é:

1. O usuário envia email/senha para `/auth/login`
2. O sistema valida as credenciais
3. Um token JWT é gerado e retornado
4. As requisições futuras utilizam o token no header

---

## 🗃️ Migrations

Gerenciadas com Flyway:

- `V1__create-user-table.sql` – Criação da tabela de usuários
- `V2__create_table_transactions.sql` – Criação da tabela de transações

---

## 📦 Próximos passos

- [ ] Containerização com Docker
- [ ] Deploy na AWS (EC2 + RDS ou ECS)