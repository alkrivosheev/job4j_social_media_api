CREATE TABLE IF NOT EXISTS messages (
                          id SERIAL PRIMARY KEY,
                          sender_id INTEGER NOT NULL,
                          receiver_id INTEGER NOT NULL,
                          content TEXT NOT NULL,
                          is_read BOOLEAN DEFAULT FALSE,
                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                          FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
                          FOREIGN KEY (receiver_id) REFERENCES users(id) ON DELETE CASCADE,
                          CONSTRAINT different_users_msg CHECK (sender_id != receiver_id)
);