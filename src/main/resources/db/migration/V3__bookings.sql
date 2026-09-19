Create EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE bookings(
    id BIGSERIAL PRIMARY KEY,
    student_id BIGINT NOT NULL REFERENCES users(id),
    tutor_profile_id BIGINT NOT NULL REFERENCES tutor_profiles(id),
    subject_id BIGINT NOT NULL REFERENCES subjects(id),
    session_start TIMESTAMP NOT NULL,
    session_end TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT  NULL DEFAULT now()
);

ALTER TABLE bookings
ADD CONSTRAINT no_overlapping_bookings
EXCLUDE USING gist(
    tutor_profile_id WITH =,
    tsrange(session_start, session_end) WITH &&
) WHERE (status IN ('PENDING', 'CONFIRMED'));

CREATE INDEX idx_bookings_student_id ON bookings(student_id);
CREATE INDEX idx_bookings_tutor_profile_id ON bookings(tutor_profile_id);