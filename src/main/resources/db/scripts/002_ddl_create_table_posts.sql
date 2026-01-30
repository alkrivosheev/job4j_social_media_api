CREATE TABLE IF NOT EXISTS posts (
                       id SERIAL PRIMARY KEY,
                       user_id INTEGER NOT NULL,
                       title VARCHAR(255) NOT NULL,
                       content TEXT NOT NULL,
                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       is_deleted BOOLEAN DEFAULT FALSE,
                       FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                       CONSTRAINT valid_title_length CHECK (LENGTH(title) BETWEEN 1 AND 255)
);
