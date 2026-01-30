CREATE TABLE IF NOT EXISTS subscriptions (
                               id SERIAL PRIMARY KEY,
                               follower_id INTEGER NOT NULL,
                               following_id INTEGER NOT NULL,
                               created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                               FOREIGN KEY (follower_id) REFERENCES users(id) ON DELETE CASCADE,
                               FOREIGN KEY (following_id) REFERENCES users(id) ON DELETE CASCADE,
                               UNIQUE(follower_id, following_id),
                               CONSTRAINT different_users_sub CHECK (follower_id != following_id)
);