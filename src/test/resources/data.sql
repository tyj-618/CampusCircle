INSERT INTO school (id, name, province, city, latitude, longitude, status)
VALUES
    (1, '南京大学', '江苏省', '南京市', 32.115055, 118.958743, 0),
    (2, '东南大学', '江苏省', '南京市', 31.887891, 118.818982, 0),
    (3, '南京航空航天大学', '江苏省', '南京市', 32.034454, 118.797028, 0),
    (4, '复旦大学', '上海市', '上海市', 31.298822, 121.503223, 0);

INSERT INTO category (name, code, sort_order, status)
VALUES
    ('课程交流', 'course', 10, 0),
    ('校园生活', 'life', 20, 0),
    ('二手闲置', 'market', 30, 0),
    ('失物招领', 'lost_found', 40, 0),
    ('活动组队', 'activity', 50, 0),
    ('求助问答', 'help', 60, 0);
