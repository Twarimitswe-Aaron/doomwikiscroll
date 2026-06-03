-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Create base tables for the social history platform

-- Users table
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       username VARCHAR(50) UNIQUE NOT NULL,
                       email VARCHAR(255) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       first_name VARCHAR(100),
                       last_name VARCHAR(100),
                       bio TEXT,
                       profile_picture_url VARCHAR(500),
                       email_verified BOOLEAN DEFAULT FALSE,
                       two_factor_enabled BOOLEAN DEFAULT FALSE,
                       account_locked BOOLEAN DEFAULT FALSE,
                       login_attempts INTEGER DEFAULT 0,
                       last_login_at TIMESTAMP,
                       last_login_ip VARCHAR(45),
                       role VARCHAR(20) NOT NULL DEFAULT 'USER',
                       status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Categories table
CREATE TABLE categories (
                            id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                            name VARCHAR(100) UNIQUE NOT NULL,
                            description TEXT,
                            icon_url VARCHAR(500),
                            parent_category_id UUID REFERENCES categories(id),
                            display_order INTEGER DEFAULT 0,
                            is_active BOOLEAN DEFAULT TRUE,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Historical Events table
CREATE TABLE historical_events (
                                   id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                   title VARCHAR(500) NOT NULL,
                                   summary TEXT,
                                   detailed_content TEXT,
                                   event_date VARCHAR(50),
                                   event_year INTEGER,
                                   era VARCHAR(50),
                                   location VARCHAR(255),
                                   latitude DOUBLE PRECISION,
                                   longitude DOUBLE PRECISION,
                                   wikipedia_url VARCHAR(500),
                                   wikipedia_page_id BIGINT UNIQUE,
                                   image_url VARCHAR(500),
                                   thumbnail_url VARCHAR(500),
                                   source VARCHAR(100) DEFAULT 'WIKIPEDIA',
                                   view_count BIGINT DEFAULT 0,
                                   like_count BIGINT DEFAULT 0,
                                   comment_count BIGINT DEFAULT 0,
                                   share_count BIGINT DEFAULT 0,
                                   is_trending BOOLEAN DEFAULT FALSE,
                                   is_featured BOOLEAN DEFAULT FALSE,
                                   status VARCHAR(20) NOT NULL DEFAULT 'PUBLISHED',
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Event-Category junction table
CREATE TABLE event_categories (
                                  event_id UUID REFERENCES historical_events(id) ON DELETE CASCADE,
                                  category_id UUID REFERENCES categories(id) ON DELETE CASCADE,
                                  PRIMARY KEY (event_id, category_id)
);

-- Related Events table
CREATE TABLE related_events (
                                event_id UUID REFERENCES historical_events(id) ON DELETE CASCADE,
                                related_event_id UUID REFERENCES historical_events(id) ON DELETE CASCADE,
                                PRIMARY KEY (event_id, related_event_id)
);

-- Comments table
CREATE TABLE comments (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          content TEXT NOT NULL,
                          user_id UUID NOT NULL REFERENCES users(id),
                          event_id UUID NOT NULL REFERENCES historical_events(id),
                          parent_comment_id UUID REFERENCES comments(id),
                          like_count INTEGER DEFAULT 0,
                          is_edited BOOLEAN DEFAULT FALSE,
                          is_deleted BOOLEAN DEFAULT FALSE,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User Reactions table
CREATE TABLE user_reactions (
                                id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                user_id UUID NOT NULL REFERENCES users(id),
                                event_id UUID NOT NULL REFERENCES historical_events(id),
                                reaction_type VARCHAR(20) NOT NULL, -- LIKE, LOVE, WOW, SAD, ANGRY, INTERESTING
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                UNIQUE(user_id, event_id, reaction_type)
);

-- Bookmarks table
CREATE TABLE bookmarks (
                           id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                           user_id UUID NOT NULL REFERENCES users(id),
                           event_id UUID NOT NULL REFERENCES historical_events(id),
                           notes TEXT,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           UNIQUE(user_id, event_id)
);

-- Reading History table
CREATE TABLE reading_history (
                                 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                 user_id UUID NOT NULL REFERENCES users(id),
                                 event_id UUID NOT NULL REFERENCES historical_events(id),
                                 progress_percentage INTEGER DEFAULT 0,
                                 time_spent_seconds INTEGER DEFAULT 0,
                                 completed BOOLEAN DEFAULT FALSE,
                                 last_read_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User Follows (Topics/Categories)
CREATE TABLE user_follows (
                              id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                              user_id UUID NOT NULL REFERENCES users(id),
                              category_id UUID NOT NULL REFERENCES categories(id),
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              UNIQUE(user_id, category_id)
);

-- Email Verification Tokens
CREATE TABLE verification_tokens (
                                     id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                     user_id UUID NOT NULL REFERENCES users(id),
                                     token VARCHAR(500) NOT NULL UNIQUE,
                                     token_type VARCHAR(50) NOT NULL, -- EMAIL_VERIFICATION, PASSWORD_RESET, TWO_FACTOR
                                     expires_at TIMESTAMP NOT NULL,
                                     used BOOLEAN DEFAULT FALSE,
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_historical_events_title ON historical_events(title);
CREATE INDEX idx_historical_events_year ON historical_events(event_year);
CREATE INDEX idx_historical_events_status ON historical_events(status);
CREATE INDEX idx_historical_events_trending ON historical_events(is_trending) WHERE is_trending = TRUE;
CREATE INDEX idx_comments_event_id ON comments(event_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_comments_parent_id ON comments(parent_comment_id);
CREATE INDEX idx_bookmarks_user_id ON bookmarks(user_id);
CREATE INDEX idx_reading_history_user_id ON reading_history(user_id);
CREATE INDEX idx_verification_tokens_token ON verification_tokens(token);

-- Create text search indexes
CREATE INDEX idx_historical_events_title_trgm ON historical_events USING gin (title gin_trgm_ops);
CREATE INDEX idx_historical_events_summary_trgm ON historical_events USING gin (summary gin_trgm_ops);
