-- Tabela de usuários
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       cpf_cnpj VARCHAR(14),
                       phone VARCHAR(20),
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP,
                       last_login TIMESTAMP
);

-- Tabela de papéis (roles)
CREATE TABLE roles (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(50) NOT NULL UNIQUE,
                       description VARCHAR(255)
);

-- Tabela de associação entre usuários e papéis
CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role_id BIGINT NOT NULL,
                            PRIMARY KEY (user_id, role_id),
                            FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
                            FOREIGN KEY (role_id) REFERENCES roles (id) ON DELETE CASCADE
);

-- Tabela de endereços
CREATE TABLE addresses (
                           id BIGSERIAL PRIMARY KEY,
                           user_id BIGINT,
                           street VARCHAR(255) NOT NULL,
                           number VARCHAR(20) NOT NULL,
                           complement VARCHAR(100),
                           neighborhood VARCHAR(100) NOT NULL,
                           city VARCHAR(100) NOT NULL,
                           state VARCHAR(50) NOT NULL,
                           zip_code VARCHAR(10) NOT NULL,
                           country VARCHAR(50) NOT NULL DEFAULT 'Brasil',
                           default_address BOOLEAN NOT NULL DEFAULT FALSE,
                           address_type VARCHAR(20) NOT NULL DEFAULT 'BOTH',
                           FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Tabela de categorias
CREATE TABLE categories (
                            id BIGSERIAL PRIMARY KEY,
                            name VARCHAR(100) NOT NULL,
                            description VARCHAR(500),
                            parent_id BIGINT,
                            image_url VARCHAR(255),
                            is_active BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at TIMESTAMP NOT NULL,
                            updated_at TIMESTAMP,
                            FOREIGN KEY (parent_id) REFERENCES categories (id) ON DELETE SET NULL
);

-- Tabela de produtos
CREATE TABLE products (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(255) NOT NULL,
                          description TEXT,
                          price DECIMAL(10, 2) NOT NULL,
                          stock_quantity INTEGER NOT NULL DEFAULT 0,
                          image_url VARCHAR(255),
                          category_id BIGINT,
                          featured BOOLEAN NOT NULL DEFAULT FALSE,
                          weight DOUBLE PRECISION NOT NULL DEFAULT 0.5,
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP,
                          FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE SET NULL
);

-- Tabela para imagens adicionais do produto
CREATE TABLE product_images (
                                product_id BIGINT NOT NULL,
                                image_url VARCHAR(255) NOT NULL,
                                FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

-- Tabela para tamanhos disponíveis do produto
CREATE TABLE product_sizes (
                               product_id BIGINT NOT NULL,
                               size VARCHAR(10) NOT NULL,
                               FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

-- Tabela para cores disponíveis do produto
CREATE TABLE product_colors (
                                product_id BIGINT NOT NULL,
                                color VARCHAR(20) NOT NULL,
                                FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

-- Tabela de carrinhos
CREATE TABLE carts (
                       id BIGSERIAL PRIMARY KEY,
                       user_id BIGINT UNIQUE,
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP,
                       FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Tabela de itens do carrinho
CREATE TABLE cart_items (
                            id BIGSERIAL PRIMARY KEY,
                            cart_id BIGINT NOT NULL,
                            product_id BIGINT NOT NULL,
                            quantity INTEGER NOT NULL,
                            selected_size VARCHAR(10),
                            selected_color VARCHAR(20),
                            FOREIGN KEY (cart_id) REFERENCES carts (id) ON DELETE CASCADE,
                            FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE CASCADE
);

-- Tabela de pedidos
CREATE TABLE orders (
                        id BIGSERIAL PRIMARY KEY,
                        order_number VARCHAR(20) NOT NULL UNIQUE,
                        user_id BIGINT NOT NULL,
                        total_amount DECIMAL(10, 2) NOT NULL,
                        status VARCHAR(20) NOT NULL,
                        shipping_address_id BIGINT,
                        billing_address_id BIGINT,
                        created_at TIMESTAMP NOT NULL,
                        updated_at TIMESTAMP,
                        completed_at TIMESTAMP,
                        payment_intent_id VARCHAR(100),
                        tracking_number VARCHAR(100),
                        FOREIGN KEY (user_id) REFERENCES users (id),
                        FOREIGN KEY (shipping_address_id) REFERENCES addresses (id),
                        FOREIGN KEY (billing_address_id) REFERENCES addresses (id)
);

-- Tabela de itens do pedido
CREATE TABLE order_items (
                             id BIGSERIAL PRIMARY KEY,
                             order_id BIGINT NOT NULL,
                             product_id BIGINT NOT NULL,
                             quantity INTEGER NOT NULL,
                             price DECIMAL(10, 2) NOT NULL,
                             total_price DECIMAL(10, 2) NOT NULL,
                             selected_size VARCHAR(10),
                             selected_color VARCHAR(20),
                             FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
                             FOREIGN KEY (product_id) REFERENCES products (id)
);

-- Tabela de pagamentos
CREATE TABLE payments (
                          id BIGSERIAL PRIMARY KEY,
                          order_id BIGINT NOT NULL UNIQUE,
                          payment_method VARCHAR(50) NOT NULL,
                          payment_status VARCHAR(50) NOT NULL,
                          payment_intent_id VARCHAR(100),
                          amount DECIMAL(10, 2) NOT NULL,
                          created_at TIMESTAMP NOT NULL,
                          updated_at TIMESTAMP,
                          FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);
