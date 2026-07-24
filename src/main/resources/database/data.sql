-- Insert sample data for testing (only for dev/test profiles)
-- This file loads after schema.sql

-- Sample admin user (password: admin123, BCrypt-encoded)
INSERT INTO users (email, password, first_name, last_name, role, created_at, updated_at)
VALUES ('admin@example.com', '$2a$10$JjyhkRkoJszx5POcfcZn7esdLgnPCOdMz64d2gaM2hf2U9u8Aknni', 'Admin', 'User', 'ADMIN', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- Sample regular user (password: user123, BCrypt-encoded)
INSERT INTO users (email, password, first_name, last_name, role, created_at, updated_at)
VALUES ('user@example.com', '$2a$10$VTIyDu8baIjsZMi4LZZ47uzeI7Wu0SoFeoYwMLiot5nPZfKDUC/sG', 'John', 'Doe', 'USER', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (email) DO NOTHING;

-- Sample products
INSERT INTO products (name, description, price, stock_quantity, category, created_at, updated_at)
VALUES
  ('Laptop', 'High-performance laptop for developers', 999.99, 10, 'Electronics', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('Keyboard', 'Mechanical keyboard with RGB lighting', 129.99, 50, 'Electronics', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('Mouse', 'Wireless mouse with ergonomic design', 49.99, 100, 'Electronics', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('Monitor', '27-inch 4K display', 399.99, 15, 'Electronics', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
  ('Headphones', 'Noise-cancelling wireless headphones', 199.99, 30, 'Electronics', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (name) DO NOTHING;

-- Create cart for sample user
INSERT INTO cart (user_id, created_at, updated_at)
SELECT id, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP FROM users WHERE email = 'user@example.com'
ON CONFLICT (user_id) DO NOTHING;
