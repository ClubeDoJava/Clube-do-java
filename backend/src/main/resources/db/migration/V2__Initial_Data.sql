-- Inserir roles padrão
INSERT INTO roles (name, description)
SELECT 'ROLE_ADMIN', 'Administrador do sistema'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_ADMIN');

INSERT INTO roles (name, description)
SELECT 'ROLE_USER', 'Usuário comum'
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_USER');

-- Inserir categorias iniciais
INSERT INTO categories (name, description, image_url, is_active, created_at) VALUES
                                                                                 ('Camisetas', 'Camisetas com estampas de Java', 'categories/tshirts.jpg', TRUE, NOW()),
                                                                                 ('Canecas', 'Canecas personalizadas', 'categories/mugs.jpg', TRUE, NOW()),
                                                                                 ('Adesivos', 'Adesivos para notebook', 'categories/stickers.jpg', TRUE, NOW()),
                                                                                 ('Jaquetas', 'Jaquetas e moletons', 'categories/jackets.jpg', TRUE, NOW()),
                                                                                 ('Acessórios', 'Diversos acessórios para devs', 'categories/accessories.jpg', TRUE, NOW());

-- Inserir produtos iniciais
INSERT INTO products (name, description, price, stock_quantity, image_url, category_id, featured, weight, created_at) VALUES
                                                                                                                          ('Camiseta Java Developer', 'Camiseta oversized com estampa Java Developer', 79.90, 100, 'products/java-dev-tshirt.jpg', 1, TRUE, 0.3, NOW()),
                                                                                                                          ('Camiseta Spring Framework', 'Camiseta com logo do Spring Framework', 79.90, 80, 'products/spring-tshirt.jpg', 1, FALSE, 0.3, NOW()),
                                                                                                                          ('Camiseta Sout', 'Camiseta Sysout', 79.90, 50, 'products/jvm-tshirt.jpg', 1, FALSE, 0.3, NOW()),
                                                                                                                          ('Caneca Java', 'Caneca preta com logo Java', 49.90, 200, 'products/java-mug.jpg', 2, TRUE, 0.5, NOW()),
                                                                                                                          ('Caneca Código Java', 'Caneca com código Java impresso', 49.90, 150, 'products/java-code-mug.jpg', 2, FALSE, 0.5, NOW()),
                                                                                                                          ('Kit Adesivos Java', 'Kit com 5 adesivos de Java para notebook', 19.90, 300, 'products/java-stickers.jpg', 3, TRUE, 0.1, NOW()),
                                                                                                                          ('Adesivo Spring Boot', 'Adesivo grande com logo do Spring Boot', 9.90, 250, 'products/spring-sticker.jpg', 3, FALSE, 0.05, NOW()),
                                                                                                                          ('Jaqueta Java Developer', 'Jaqueta com capuz e estampa Java Dev', 149.90, 40, 'products/java-jacket.jpg', 4, TRUE, 0.8, NOW()),
                                                                                                                          ('Moletom JVM', 'Moletom com estampa da JVM', 129.90, 60, 'products/jvm-hoodie.jpg', 4, FALSE, 0.7, NOW()),
                                                                                                                          ('Mousepad Java', 'Mousepad com logo Java', 29.90, 120, 'products/java-mousepad.jpg', 5, FALSE, 0.2, NOW());

-- Inserir tamanhos para produtos
INSERT INTO product_sizes (product_id, size) VALUES
                                                 (1, 'P'), (1, 'M'), (1, 'G'), (1, 'GG'),
                                                 (2, 'P'), (2, 'M'), (2, 'G'), (2, 'GG'),
                                                 (3, 'P'), (3, 'M'), (3, 'G'), (3, 'GG'),
                                                 (8, 'P'), (8, 'M'), (8, 'G'), (8, 'GG'),
                                                 (9, 'P'), (9, 'M'), (9, 'G'), (9, 'GG');

-- Inserir cores para produtos
INSERT INTO product_colors (product_id, color) VALUES
                                                   (1, 'Preto'), (1, 'Branco'), (1, 'Azul'),
                                                   (2, 'Preto'), (2, 'Branco'),
                                                   (3, 'Preto'), (3, 'Cinza'),
                                                   (4, 'Preto'), (4, 'Branco'),
                                                   (5, 'Preto'), (5, 'Branco'),
                                                   (8, 'Preto'), (8, 'Azul'),
                                                   (9, 'Preto'), (9, 'Cinza'),
                                                   (10, 'Preto');

-- Criar usuário admin padrão (senha: admin123)
INSERT INTO users (name, email, password, created_at) VALUES
    ('Administrador', 'admin@clubedojava.com.br', '$2a$10$yYQaJrHzjOgD5wWCyelp0e3XM5J3Li5hWH3jsNgcQ5FK4R/Ga.JZG', NOW())
    ON CONFLICT (email) DO NOTHING;

-- Atribuir role de admin ao usuário
INSERT INTO user_roles (user_id, role_id) VALUES
    (1, 1)
    ON CONFLICT (user_id, role_id) DO NOTHING;