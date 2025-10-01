
-- Schema inicial do TaskFlow

-- Tabela de usuários
CREATE TABLE users (
    id BIGINT IDENTITY(1,1) NOT NULL,
    full_name NVARCHAR(255) NOT NULL,
    age INT NULL,
    email NVARCHAR(255) NOT NULL,
    password NVARCHAR(255) NOT NULL,
    CONSTRAINT PK_users PRIMARY KEY (id),
    CONSTRAINT UK_users_email UNIQUE (email)
);

-- Tabela de tasks
CREATE TABLE tasks (
    id BIGINT IDENTITY(1,1) NOT NULL,
    title NVARCHAR(255) NOT NULL,
    description NVARCHAR(MAX) NULL,
    due_date DATETIME2 NULL,
    priority NVARCHAR(20) NOT NULL,
    status NVARCHAR(20) NULL,
    created_at DATETIME2 NULL,
    updated_at DATETIME2 NULL,
    user_id BIGINT NULL,
    CONSTRAINT PK_tasks PRIMARY KEY (id),
    CONSTRAINT FK_tasks_user FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Índice para melhorar performance de consultas por usuário
CREATE INDEX IDX_tasks_user_id ON tasks(user_id);

-- Índice para melhorar performance de consultas por status
CREATE INDEX IDX_tasks_status ON tasks(status);