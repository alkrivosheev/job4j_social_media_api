CREATE TABLE IF NOT EXISTS images (
                        id SERIAL PRIMARY KEY,
                        post_id INTEGER NOT NULL,
                        url VARCHAR(500) NOT NULL,
                        file_name VARCHAR(255) NOT NULL,
                        file_size INTEGER,
                        upload_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (post_id) REFERENCES posts(id) ON DELETE CASCADE
);