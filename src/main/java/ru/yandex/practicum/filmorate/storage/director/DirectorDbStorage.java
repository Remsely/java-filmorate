package ru.yandex.practicum.filmorate.storage.director;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.EntityNotFoundException;
import ru.yandex.practicum.filmorate.exception.FilmAttributeNotExistOnFilmCreationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.ErrorResponse;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DirectorDbStorage implements DirectorStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Director get(long id) {
        checkDirectorExist(id);
        String sqlQuery = "SELECT * FROM director WHERE director_id = ?";
        return jdbcTemplate.queryForObject(sqlQuery, this::mapRowDirector, id);
    }

    @Override
    public Director add(Director director) {
        String sqlQuery = "INSERT INTO director (name) VALUES (?)";

        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sqlQuery, new String[]{"director_id"});
            statement.setString(1, director.getName());
            return statement;
        }, keyHolder);

        long id = Objects.requireNonNull(keyHolder.getKey()).longValue();
        director.setId(id);
        return director;
    }

    @Override
    public Director update(Director director) {
        long id = director.getId();
        checkDirectorExist(id);
        String sqlQuery = "UPDATE director SET name = ? WHERE director_id = ?";
        jdbcTemplate.update(sqlQuery, director.getName(), id);
        return this.get(director.getId());
    }

    @Override
    public void delete(long id) {
        checkDirectorExist(id);
        String sqlQuery = "DELETE FROM director WHERE director_id = ?";
        jdbcTemplate.update(sqlQuery, id);
    }

    @Override
    public List<Director> getAll() {
        String sqlQuery = "SELECT * FROM director";
        return jdbcTemplate.query(sqlQuery, this::mapRowDirector);
    }

    // Добавить режиссеров к фильму
    @Override
    public List<Director> addDirectors(long id, List<Director> directors) {
        if (directors != null && !directors.isEmpty()) {
            Set<Director> uniqFilmDirectors = new HashSet<>(directors);

            for (Director director : uniqFilmDirectors) {
                long directorId = director.getId();

                checkFilmDirectorExist(directorId);

                String sqlQuery = "INSERT INTO film_director (film_id, director_id) VALUES (?, ?)";
                jdbcTemplate.update(sqlQuery, id, directorId);
            }
        }
        return directors;
    }

    // Получить режиссеров фильма
    @Override
    public List<Director> getFilmDirectors(long id) {
        String sqlQuery = "SELECT * FROM film_director WHERE film_id = ?";
        return jdbcTemplate.query(sqlQuery, (rs, rowNum) -> this.get(rs.getLong("director_id")), id);
    }

    // Удалить все связи режиссеров с фильмом
    @Override
    public void deleteFilmDirectors(long filmId) {
        String sqlQuery = "DELETE FROM film_director WHERE film_id = ?";
        jdbcTemplate.update(sqlQuery, filmId);
    }

    @Override
    public boolean notContainDirector(long id) {
        String sqlQuery = "SELECT COUNT(*) FROM director WHERE director_id = ?";
        Integer count = jdbcTemplate.queryForObject(sqlQuery, Integer.class, id);
        return count != null && count == 0;
    }

    @Override
    public boolean filmNotContainDirector(long filmId, long directorId) {
        String sqlQuery = "SELECT COUNT(*) FROM film_director WHERE director_id = ? AND film_id = ?";
        Integer count = jdbcTemplate.queryForObject(sqlQuery, Integer.class, directorId, filmId);
        return count != null && count == 0;
    }

    private Director mapRowDirector(ResultSet rs, int rowNum) throws SQLException {
        return Director.builder()
                .id(rs.getLong("director_id"))
                .name(rs.getString("name"))
                .build();
    }

    private void checkFilmDirectorExist(long id) {
        if (notContainDirector(id)) {
            throw new FilmAttributeNotExistOnFilmCreationException(
                    new ErrorResponse("Director id", String.format("Не найден режиссер с Id: %d.", id))
            );
        }
    }

    private void checkDirectorExist(long id) {
        if (this.notContainDirector(id)) {
            throw new EntityNotFoundException(
                    new ErrorResponse("Director id", String.format("Не найден режиссер с ID: %d.", id))
            );
        }
    }
}
