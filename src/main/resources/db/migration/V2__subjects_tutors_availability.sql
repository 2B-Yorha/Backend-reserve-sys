CREATE TABLE subjects(
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

CREATE TABLE tutor_profiles(
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    bio TEXT,
    hourly_rate NUMERIC(6,2)
);

CREATE TABLE tutor_subjects (
    tutor_profile_id BIGINT NOT NULL REFERENCES tutor_profiles(id) ON DELETE CASCADE,
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    PRIMARY KEY (tutor_profile_id, subject_id)
);

CREATE TABLE availability_slots(
    id BIGSERIAL PRIMARY KEY,
    tutor_profile_id BIGINT NOT NULL REFERENCES tutor_profiles(id) ON DELETE CASCADE,
    is_recurring BOOLEAN NOT NULL,
    day_of_week INTEGER,
    specific_date DATE,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT chk_recurring_fields CHECK(
        (is_recurring = TRUE AND day_of_week IS NOT NULL AND specific_date IS NULL)
        OR
        (is_recurring =FALSE AND specific_date IS NOT NULL AND day_of_week IS NULL)
    ),
    CONSTRAINT chk_time_order CHECK (start_time < end_time)
);

CREATE INDEX idx_availabitly_tutor ON availability_slots(tutor_profile_id);