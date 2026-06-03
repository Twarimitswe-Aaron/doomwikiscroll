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
                                   random_key DOUBLE PRECISION NOT NULL DEFAULT random(),
                                   search_vector tsvector GENERATED ALWAYS AS (
                                       setweight(to_tsvector('english', coalesce(title, '')), 'A') ||
                                       setweight(to_tsvector('english', coalesce(summary, '')), 'B') ||
                                       setweight(to_tsvector('english', coalesce(detailed_content, '')), 'C') ||
                                       setweight(to_tsvector('english', coalesce(location, '')), 'D') ||
                                       setweight(to_tsvector('english', coalesce(era, '')), 'D')
                                   ) STORED,
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
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 UNIQUE(user_id, event_id)
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

-- Email delivery outbox
CREATE TABLE email_outbox (
                              id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                              user_id UUID REFERENCES users(id),
                              email_type VARCHAR(50) NOT NULL,
                              recipient_email VARCHAR(255) NOT NULL,
                              subject VARCHAR(255) NOT NULL,
                              template_name VARCHAR(100) NOT NULL,
                              template_variables TEXT NOT NULL,
                              status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                              attempts INTEGER NOT NULL DEFAULT 0,
                              max_attempts INTEGER NOT NULL DEFAULT 5,
                              next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              sent_at TIMESTAMP,
                              last_error TEXT,
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes for better performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_categories_active_display_order ON categories(is_active, display_order);
CREATE INDEX idx_categories_parent_id ON categories(parent_category_id);
CREATE INDEX idx_historical_events_year ON historical_events(event_year) WHERE status = 'PUBLISHED';
CREATE INDEX idx_historical_events_era ON historical_events(era) WHERE status = 'PUBLISHED';
CREATE INDEX idx_historical_events_status_created ON historical_events(status, created_at DESC);
CREATE INDEX idx_historical_events_status_random_key ON historical_events(status, random_key);
CREATE INDEX idx_historical_events_featured_created ON historical_events(is_featured, created_at DESC) WHERE is_featured = TRUE;
CREATE INDEX idx_historical_events_trending ON historical_events(is_trending) WHERE is_trending = TRUE;
CREATE INDEX idx_event_categories_category_event ON event_categories(category_id, event_id);
CREATE INDEX idx_event_categories_event_category ON event_categories(event_id, category_id);
CREATE INDEX idx_comments_event_id ON comments(event_id);
CREATE INDEX idx_comments_user_id ON comments(user_id);
CREATE INDEX idx_comments_parent_id ON comments(parent_comment_id);
CREATE INDEX idx_comments_event_top_level_created ON comments(event_id, created_at DESC) WHERE parent_comment_id IS NULL AND is_deleted = FALSE;
CREATE INDEX idx_comments_parent_created ON comments(parent_comment_id, created_at ASC) WHERE is_deleted = FALSE;
CREATE INDEX idx_user_reactions_user_event ON user_reactions(user_id, event_id);
CREATE INDEX idx_user_reactions_event_type ON user_reactions(event_id, reaction_type);
CREATE INDEX idx_bookmarks_user_created ON bookmarks(user_id, created_at DESC);
CREATE INDEX idx_bookmarks_event_id ON bookmarks(event_id);
CREATE INDEX idx_reading_history_user_id ON reading_history(user_id);
CREATE INDEX idx_reading_history_user_last_read ON reading_history(user_id, last_read_at DESC);
CREATE INDEX idx_user_follows_category_id ON user_follows(category_id);
CREATE INDEX idx_verification_tokens_token_type ON verification_tokens(token, token_type);
CREATE INDEX idx_verification_tokens_user_type_active ON verification_tokens(user_id, token_type) WHERE used = FALSE;
CREATE INDEX idx_verification_tokens_expires_at ON verification_tokens(expires_at);
CREATE INDEX idx_email_outbox_status_next_attempt ON email_outbox(status, next_attempt_at);
CREATE INDEX idx_email_outbox_user_type ON email_outbox(user_id, email_type);

-- Create text search indexes
CREATE INDEX idx_historical_events_title_trgm ON historical_events USING gin (lower(title) gin_trgm_ops);
CREATE INDEX idx_historical_events_summary_trgm ON historical_events USING gin (lower(summary) gin_trgm_ops);
CREATE INDEX idx_historical_events_search_vector ON historical_events USING gin (search_vector);
