<div align=center>
  
#  TaskFlow - Full Stack  

O **TaskFlow** é uma aplicação **full stack** para gerenciamento de tarefas pessoais.  
Permite ao usuário **criar, editar, visualizar e excluir** suas próprias tarefas de forma organizada, com **autenticação segura (JWT)** e uma **interface responsiva em Angular**.  

---

## 📌 Tecnologias Utilizadas

![Java](https://img.shields.io/badge/Java-0073b7?style=for-the-badge&logo=java&logoColor=white&color=orange)
![Spring](https://img.shields.io/badge/Spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.5-green.svg?style=for-the-badge&logo=spring-boot)
![Angular](https://img.shields.io/badge/Angular-DD0031?style=for-the-badge&logo=angular&logoColor=white)
![Database](https://img.shields.io/badge/Database-H2%20%26%20SQL%20Server-blue?style=for-the-badge&logo=database)

</div>

---

## ✨ Funcionalidades

- 📝 **CRUD de Tarefas** – criar, editar e excluir tarefas pessoais  
- 📊 **Organização** – visualização por status e prioridade  
- 🔐 **Autenticação JWT** – login seguro  
- 📱 **Interface Responsiva** – Angular 20.3 Stand alone

---

## 🖥️ Backend - Spring Boot

O backend foi desenvolvido em **Java 17 + Spring Boot 3.x**, com:  

- API RESTful  
- Autenticação e autorização com **JWT**  
- Banco **H2** para desenvolvimento e **SQL Server** em produção  
- Arquitetura em camadas (**Model, Repository, Service, DTO, Controller**)  

<div align="center">
  <img width="1855" height="959" alt="image" src="https://github.com/user-attachments/assets/60701ddd-b36e-4319-9b9c-2652b12327a5" />


  <p><em>🔎 Exemplo de endpoints RESTful no backend</em></p>
</div>

## 📖 Documentação da API

A documentação da API está disponível via **Swagger UI**:

👉 Acesse em: `http://localhost:8080/swagger.html`


---

## 🌐 Frontend - Angular 20.3

O frontend foi desenvolvido em **Angular + TypeScript**.  
A interface consome a API do backend, mantendo segurança via **JWT**.  

### 🔎 Telas da Aplicação

<div align="center">

<img width="1364" height="917" alt="image" src="https://github.com/user-attachments/assets/5a0cca47-d2e8-4fbf-b9f2-eaad2229ac7e" />

<p><em>🔑 Tela de Login</em></p>

<img width="1152" height="951" alt="image" src="https://github.com/user-attachments/assets/1c7dc491-3f4b-46d8-9dfe-9c1d01292c15" />

<p><em> Tela de Cadastro</em></p>

<img width="1319" height="826" alt="image" src="https://github.com/user-attachments/assets/d36bc5ee-c6d9-46d7-8c26-4a9250ec03fc" />

<p><em>✏️ Formulário de Edição de Tarefa</em></p>

<img width="1661" height="828" alt="image" src="https://github.com/user-attachments/assets/9b924d1e-acca-4380-9b95-546920be6193" />

<p><em>📋 Dashboard de Tarefas</em></p>



</div>

---

## 🔗 Integração Backend + Frontend

- ✅ Login gera **JWT** → armazenado no `localStorage`  
- ✅ Angular consome endpoints protegidos do Spring Boot  
- ✅ Serviços organizados (`AuthService`, `TaskService`)  
- ✅ Fluxo seguro e escalável para futuras features  

---

## ⚙️ Como Executar o Projeto

### 📥 Clonar o Repositório
```bash
git clone https://github.com/Rafael-Bessa/taskflow.git
cd taskflow
```

### 🖥️ Executar o Backend
```bash
cd TaskFlow-Backend
./mvnw spring-boot:run
```

A API sobe em: http://localhost:8080

### 🌐 Executar o Frontend
```bash
cd TaskFlow-Frontend
npm install
ng serve
```

A aplicação roda em: http://localhost:4200

