CREATE TABLE IF NOT EXISTS friendships (
                             id SERIAL PRIMARY KEY,
                             requester_id INTEGER NOT NULL,
                             addressee_id INTEGER NOT NULL,
                             status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'accepted', 'rejected')),
                             created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                             FOREIGN KEY (requester_id) REFERENCES users(id) ON DELETE CASCADE,
                             FOREIGN KEY (addressee_id) REFERENCES users(id) ON DELETE CASCADE,
                             UNIQUE(requester_id, addressee_id),
                             CONSTRAINT different_users CHECK (requester_id != addressee_id)
);