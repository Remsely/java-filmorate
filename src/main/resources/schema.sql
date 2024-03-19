create table IF NOT EXISTS mpa_ratings
(
    rating_id integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name      varchar(40) not null
);

create table IF NOT EXISTS genres
(
    genre_id integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name     varchar(40) not null
);

create table IF NOT EXISTS films
(
    film_id     integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name        varchar(40) not null,
    description varchar(200),
    rating_id   integer references MPA_RATINGS (RATING_ID),
    release     timestamp check (release >= '1895-12-28'),
    duration    integer     NOT NULL CHECK (duration > 0)
);

create table IF NOT EXISTS films_genres
(
    film_id  integer references FILMS (FILM_ID) on delete cascade,
    genre_id integer references GENRES (GENRE_ID) on delete cascade
);

create table IF NOT EXISTS users
(
    user_id  integer GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    name     varchar(40) not null,
    login    varchar(40) check (regexp_like(login, '^\S+$')),
    email    varchar     not null,
    birthday timestamp   not null
);

create table IF NOT EXISTS likes
(
    film_id integer references FILMS (FILM_ID) on delete cascade,
    user_id integer references USERS (USER_ID) on delete cascade
);

create table IF NOT EXISTS follows
(
    target_id   integer references USERS (USER_ID) on delete cascade,
    follower_id integer references USERS (USER_ID) on delete cascade,
    approved    boolean default false
);