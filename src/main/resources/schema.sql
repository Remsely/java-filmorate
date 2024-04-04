CREATE TABLE IF NOT EXISTS mpa_rating
(
    rating_id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name      VARCHAR(40) NOT NULL
);

CREATE TABLE IF NOT EXISTS genre
(
    genre_id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name     VARCHAR(40) NOT NULL
);

CREATE TABLE IF NOT EXISTS film
(
    film_id     INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name        VARCHAR(40) NOT NULL,
    description VARCHAR(200),
    rating_id   INTEGER REFERENCES mpa_rating (rating_id),
    release     TIMESTAMP CHECK (release >= '1895-12-28'),
    duration    INTEGER     NOT NULL CHECK (duration > 0)
);

CREATE TABLE IF NOT EXISTS film_genre
(
    film_id  INTEGER REFERENCES film (film_id) ON DELETE CASCADE,
    genre_id INTEGER REFERENCES genre (genre_id) ON DELETE CASCADE,
    PRIMARY KEY (film_id, genre_id)
);

CREATE TABLE IF NOT EXISTS user_data
(
    user_id  INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name     VARCHAR(40) NOT NULL,
    login    VARCHAR(40) CHECK (regexp_like(login, '^\S+$')),
    email    VARCHAR     NOT NULL,
    birthday TIMESTAMP   NOT NULL
);

CREATE TABLE IF NOT EXISTS like_film
(
    film_id INTEGER REFERENCES film (film_id) ON DELETE CASCADE,
    user_id INTEGER REFERENCES user_data (user_id) ON DELETE CASCADE,
    PRIMARY KEY (film_id, user_id)
);

CREATE TABLE IF NOT EXISTS follow
(
    target_id   INTEGER REFERENCES user_data (user_id) ON DELETE CASCADE,
    follower_id INTEGER REFERENCES user_data (user_id) ON DELETE CASCADE,
    approved    BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (target_id, follower_id)
);

CREATE TABLE IF NOT EXISTS director
(
    director_id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name        VARCHAR(40) NOT NULL
);

CREATE TABLE IF NOT EXISTS film_director
(
    director_id INTEGER REFERENCES director (director_id) ON DELETE CASCADE,
    film_id     INTEGER REFERENCES film (film_id) ON DELETE CASCADE,
    PRIMARY KEY (director_id, film_id)
);

CREATE TABLE IF NOT EXISTS event_operation
(
    operation_id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name         VARCHAR(40) NOT NULL
);

CREATE TABLE IF NOT EXISTS event_type
(
    type_id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name    VARCHAR(40) NOT NULL
);

CREATE TABLE IF NOT EXISTS feed
(
    event_id     INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    user_id      INTEGER REFERENCES user_data (user_id) ON DELETE CASCADE,
    entity_id    INTEGER NOT NULL,
    type_id      INTEGER REFERENCES event_type (type_id) ON DELETE RESTRICT,
    operation_id INTEGER REFERENCES event_operation (operation_id) ON DELETE RESTRICT,
    time         TIMESTAMP
);