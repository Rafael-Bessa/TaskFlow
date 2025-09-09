<div align="center" id="top">

# TaskFlow 

</div>

O **TaskFlow** é uma API RESTful para gerenciamento de tarefas colaborativas, inspirada em ferramentas como Trello. A proposta é permitir que usuários criem, editem, atribuam e acompanhem tarefas de forma organizada, com autenticação segura e regras de negócio, como validação de prazos e limites de tarefas por usuário. O objetivo é criar uma solução escalável para equipes, ideal para aprendizado e demonstração de microserviços.

Este projeto foi desenvolvido por **Rafael Bessa** como um exercício prático para aprendizado de Java com Spring Boot, focado em construir uma API robusta com integração a banco de dados e autenticação JWT. É um projeto de portfólio voltado para demonstrar habilidades em desenvolvimento back-end.

## Detalhes do Projeto

<div>
  <img src="https://img.shields.io/badge/Java-0073b7?style=for-the-badge&logo=java&logoColor=white&color=orange">
  <img src="https://img.shields.io/badge/Spring-6DB33F?style=for-the-badge&logo=spring&logoColor=white">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.5.5-green.svg?style=for-the-badge&logo=spring-boot">
</div>
<br>

🚀 **Iniciando o Projeto com Spring Initializr**

O primeiro passo para construir a API foi utilizar o [Spring Initializr](https://start.spring.io), uma ferramenta prática que simplifica a criação de projetos Spring Boot. Com ela, configurei o projeto com **Maven**. Escolhi um conjunto inicial de dependências para dar o pontapé no desenvolvimento, adicionando outras conforme a necessidade. A imagem abaixo ilustra a configuração inicial no Spring Initializr:

<img width="1710" height="797" alt="image" src="https://github.com/user-attachments/assets/53ca5c26-70b3-4aab-9604-c0415e99d3a2" />

<hr>

Configurando o arquivo application.properties para perfis de desenvolvimento e produção, utilizando H2 para o ambiente de desenvolvimento e SQL Server para o ambiente de produção.

<img width="1429" height="666" alt="image" src="https://github.com/user-attachments/assets/ee3cfca8-0ec4-4f44-93a0-85fa3f6c7b1e" />
<hr>
Desenvolvimento dos endpoints CRUD para as entidades User e Task, aplicando as boas práticas de uma arquitetura RESTful. A implementação inclui a segregação em camadas de model, repository, service, dto e controller, garantindo modularidade e manutenibilidade do código.
<br>

<img width="1604" height="790" alt="image" src="https://github.com/user-attachments/assets/5c0ed322-c589-48ca-a365-85d3fefad1ea" />

<img width="1482" height="740" alt="image" src="https://github.com/user-attachments/assets/95e1e7bc-73cb-41d1-a484-697fe404249e" />

<img width="373" height="805" alt="image" src="https://github.com/user-attachments/assets/b6e207cb-ca27-4ddf-9500-417b7fe322e9" />



