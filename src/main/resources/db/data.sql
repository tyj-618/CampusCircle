SET NAMES utf8mb4;

USE campuscircle;

INSERT INTO category (name, code, sort_order, status)
VALUES
    ('课程交流', 'course', 10, 0),
    ('校园生活', 'life', 20, 0),
    ('二手闲置', 'market', 30, 0),
    ('失物招领', 'lost_found', 40, 0),
    ('活动组队', 'activity', 50, 0),
    ('求助问答', 'help', 60, 0)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order),
    status = VALUES(status),
    updated_at = CURRENT_TIMESTAMP;
