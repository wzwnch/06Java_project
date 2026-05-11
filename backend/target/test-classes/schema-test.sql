DROP TABLE IF EXISTS link_user;
DROP TABLE IF EXISTS link_group;
DROP TABLE IF EXISTS link_link;
DROP TABLE IF EXISTS link_link_stats;
DROP TABLE IF EXISTS link_link_stats_today;

CREATE TABLE link_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(256) NOT NULL,
    phone VARCHAR(256),
    mail VARCHAR(256),
    real_phone VARCHAR(32),
    real_mail VARCHAR(64),
    del_flag TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE link_group (
    id BIGINT PRIMARY KEY,
    gid VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(64) NOT NULL,
    username VARCHAR(64) NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    del_flag TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_group_username ON link_group(username);
CREATE INDEX idx_group_gid ON link_group(gid);

CREATE TABLE link_link (
    id BIGINT PRIMARY KEY,
    short_code VARCHAR(16) NOT NULL UNIQUE,
    gid VARCHAR(32) NOT NULL,
    origin_url VARCHAR(2048) NOT NULL,
    favicon_url VARCHAR(512),
    title VARCHAR(256),
    expire_time DATETIME,
    status TINYINT NOT NULL DEFAULT 0,
    del_flag TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_link_gid ON link_link(gid);
CREATE INDEX idx_link_status ON link_link(status);
CREATE INDEX idx_link_short_code ON link_link(short_code);

CREATE TABLE link_link_stats (
    id BIGINT PRIMARY KEY,
    short_code VARCHAR(16) NOT NULL,
    gid VARCHAR(32) NOT NULL,
    pv BIGINT NOT NULL DEFAULT 0,
    uv VARCHAR(64),
    uip VARCHAR(64),
    ip VARCHAR(64),
    region VARCHAR(64),
    os VARCHAR(32),
    browser VARCHAR(32),
    device VARCHAR(32),
    network VARCHAR(32),
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stats_short_code ON link_link_stats(short_code);
CREATE INDEX idx_stats_create_time ON link_link_stats(create_time);

CREATE TABLE link_link_stats_today (
    id BIGINT PRIMARY KEY,
    short_code VARCHAR(16) NOT NULL,
    gid VARCHAR(32) NOT NULL,
    date DATE NOT NULL,
    pv BIGINT NOT NULL DEFAULT 0,
    uv BIGINT NOT NULL DEFAULT 0,
    uip BIGINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_stats_short_code_date ON link_link_stats_today(short_code, date);
