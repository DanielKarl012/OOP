CREATE TABLE IF NOT EXISTS users (
    user_id        INT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL UNIQUE,
    password_hash  VARCHAR(64)  NOT NULL,
    email          VARCHAR(100) NOT NULL UNIQUE,
    role           VARCHAR(20)  NOT NULL,
    is_active      TINYINT(1)   NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS school_years (
    school_year_id INT AUTO_INCREMENT PRIMARY KEY,
    year_label     VARCHAR(20) NOT NULL,
    is_active      TINYINT(1)  NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS strands (
    strand_id    INT AUTO_INCREMENT PRIMARY KEY,
    strand_code  VARCHAR(20)  NOT NULL,
    strand_name  VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS students (
    student_id      INT AUTO_INCREMENT PRIMARY KEY,
    user_id         INT NOT NULL,
    lrn             VARCHAR(12)  NOT NULL UNIQUE,
    first_name      VARCHAR(50)  NOT NULL,
    middle_name     VARCHAR(50),
    last_name       VARCHAR(50)  NOT NULL,
    previous_school VARCHAR(150),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

CREATE TABLE IF NOT EXISTS sections (
    section_id     INT AUTO_INCREMENT PRIMARY KEY,
    section_name   VARCHAR(50) NOT NULL,
    grade_level    INT NOT NULL,
    strand_id      INT NOT NULL,
    school_year_id INT NOT NULL,
    slot_limit     INT NOT NULL,
    is_active      TINYINT(1) NOT NULL DEFAULT 1,
    FOREIGN KEY (strand_id)      REFERENCES strands(strand_id),
    FOREIGN KEY (school_year_id) REFERENCES school_years(school_year_id)
);

CREATE TABLE IF NOT EXISTS enrollments (
    enrollment_id     INT AUTO_INCREMENT PRIMARY KEY,
    student_id        INT NOT NULL,
    school_year_id    INT NOT NULL,
    strand_id         INT NOT NULL,
    section_id        INT NULL,
    year_level        INT NOT NULL,
    enrollee_type     VARCHAR(30) NOT NULL,
    enrollment_status VARCHAR(20) NOT NULL DEFAULT 'Pending',
    FOREIGN KEY (student_id)     REFERENCES students(student_id),
    FOREIGN KEY (school_year_id) REFERENCES school_years(school_year_id),
    FOREIGN KEY (strand_id)      REFERENCES strands(strand_id),
    FOREIGN KEY (section_id)     REFERENCES sections(section_id)
);

CREATE TABLE IF NOT EXISTS student_requirements (
    requirement_id     INT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id      INT NOT NULL,
    requirement_name   VARCHAR(100) NOT NULL,
    requirement_status VARCHAR(20)  NOT NULL,
    original_file_name VARCHAR(255),
    file_path          VARCHAR(500),
    submitted_at       TIMESTAMP NULL,
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id)
);

CREATE TABLE IF NOT EXISTS requirements (
    requirement_id   INT AUTO_INCREMENT PRIMARY KEY,
    requirement_name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS submitted_requirements (
    enrollment_id  INT NOT NULL,
    requirement_id INT NOT NULL,
    status         VARCHAR(20) NOT NULL,
    UNIQUE (enrollment_id, requirement_id),
    FOREIGN KEY (enrollment_id)  REFERENCES enrollments(enrollment_id),
    FOREIGN KEY (requirement_id) REFERENCES requirements(requirement_id)
);

CREATE TABLE IF NOT EXISTS fees (
    fee_id         INT AUTO_INCREMENT PRIMARY KEY,
    fee_name       VARCHAR(100) NOT NULL,
    amount         DECIMAL(10,2) NOT NULL,
    school_year_id INT NOT NULL,
    strand_id      INT NOT NULL,
    FOREIGN KEY (school_year_id) REFERENCES school_years(school_year_id),
    FOREIGN KEY (strand_id)      REFERENCES strands(strand_id)
);

CREATE TABLE IF NOT EXISTS discounts (
    discount_id    INT AUTO_INCREMENT PRIMARY KEY,
    discount_name  VARCHAR(100) NOT NULL,
    discount_type  VARCHAR(20)  NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL
);

CREATE TABLE IF NOT EXISTS payment_requests (
    request_id     INT AUTO_INCREMENT PRIMARY KEY,
    enrollment_id  INT NOT NULL,
    amount_due     DECIMAL(10,2) NOT NULL,
    description    VARCHAR(255),
    request_status VARCHAR(20) NOT NULL DEFAULT 'Open',
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id)
);

CREATE TABLE IF NOT EXISTS payment_request_details (
    request_id INT NOT NULL,
    item_name  VARCHAR(100) NOT NULL,
    item_type  VARCHAR(20)  NOT NULL,
    amount     DECIMAL(10,2) NOT NULL,
    FOREIGN KEY (request_id) REFERENCES payment_requests(request_id)
);

CREATE TABLE IF NOT EXISTS payments (
    payment_id       INT AUTO_INCREMENT PRIMARY KEY,
    request_id       INT NULL,
    enrollment_id    INT NOT NULL,
    amount_paid      DECIMAL(10,2) NOT NULL,
    payment_method   VARCHAR(30)  NOT NULL,
    reference_number VARCHAR(100),
    payment_status   VARCHAR(20)  NOT NULL DEFAULT 'Pending',
    paid_at          TIMESTAMP NULL,
    FOREIGN KEY (request_id)    REFERENCES payment_requests(request_id),
    FOREIGN KEY (enrollment_id) REFERENCES enrollments(enrollment_id)
);
