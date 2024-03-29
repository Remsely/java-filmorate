CREATE TABLE IF NOT EXISTS mpa_ratings
(
    rating_id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name      VARCHAR(40) NOT NULL
);

CREATE TABLE IF NOT EXISTS genres
(
    genre_id INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name     VARCHAR(40) NOT NULL
);

CREATE TABLE IF NOT EXISTS films
(
    film_id     INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name        VARCHAR(40) NOT NULL,
    description VARCHAR(200),
    rating_id   INTEGER REFERENCES mpa_ratings (rating_id),
    release     TIMESTAMP CHECK (release >= '1895-12-28'),
    duration    INTEGER     NOT NULL CHECK (duration > 0)
);

CREATE TABLE IF NOT EXISTS films_genres
(
    film_id  INTEGER REFERENCES films (film_id) ON DELETE CASCADE,
    genre_id INTEGER REFERENCES genres (genre_id) ON DELETE CASCADE,
    PRIMARY KEY (film_id, genre_id)
);

CREATE TABLE IF NOT EXISTS users
(
    user_id  INTEGER GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name     VARCHAR(40) NOT NULL,
    login    VARCHAR(40) CHECK (regexp_like(login, '^\S+$')),
    email    VARCHAR     NOT NULL,
    birthday TIMESTAMP   NOT NULL
);

CREATE TABLE IF NOT EXISTS likes
(
    film_id INTEGER REFERENCES films (film_id) ON DELETE CASCADE,
    user_id INTEGER REFERENCES users (user_id) ON DELETE CASCADE,
    PRIMARY KEY (film_id, user_id)
);

CREATE TABLE IF NOT EXISTS follows
(
    target_id   INTEGER REFERENCES users (user_id) ON DELETE CASCADE,
    follower_id INTEGER REFERENCES users (user_id) ON DELETE CASCADE,
    approved    BOOLEAN DEFAULT FALSE,
    PRIMARY KEY (target_id, follower_id)
);