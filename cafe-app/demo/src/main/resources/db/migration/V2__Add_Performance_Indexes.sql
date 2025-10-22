-- Add indexes for better query performance

-- Index for order date queries (used in daily revenue calculation)
CREATE INDEX IF NOT EXISTS idx_orders_order_date ON orders(order_date);

-- Index for order status queries (used in revenue calculations)
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);

-- Composite index for order date and status (most common query pattern)
CREATE INDEX IF NOT EXISTS idx_orders_date_status ON orders(order_date, status);

-- Index for order items quantity aggregation
CREATE INDEX IF NOT EXISTS idx_order_items_item_name ON order_items(item_name);

-- Index for order items quantity (used in top selling items)
CREATE INDEX IF NOT EXISTS idx_order_items_quantity ON order_items(quantity);

-- Index for user authentication
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
