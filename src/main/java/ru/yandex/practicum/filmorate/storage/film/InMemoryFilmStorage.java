package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.EntityNotFoundException;
import ru.yandex.practicum.filmorate.model.ErrorResponse;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> data;
    private long currentId;

    public InMemoryFilmStorage() {
        data = new HashMap<>();
        currentId = 1;
    }

    @Override
    public Film add(Film film) {
        film.setId(currentId++);
        data.put(film.getId(), film);
        return film;
    }

    @Override
    public Film update(Film film) {
        long id = film.getId();
        if (this.notContainFilm(id)) {
            throw new EntityNotFoundException(
                    new ErrorResponse("Film id", String.format("Не найден фильм с ID: %d.", id))
            );
        }
        data.put(id, film);
        return film;
    }

    @Override
    public Film get(long id) {
        if (this.notContainFilm(id)) {
            throw new EntityNotFoundException(
                    new ErrorResponse("Film id", String.format("Не найден фильм с ID: %d.", id))
            );
        }
        return data.get(id);
    }

    @Override
    public void delete(long id) {
        if (this.notContainFilm(id)) {
            throw new EntityNotFoundException(
                    new ErrorResponse("Film id", String.format("Не найден фильм с ID: %d.", id))
            );
        }
        data.remove(id);
    }

    @Override
    public List<Film> getAll() {
        return new ArrayList<>(data.values());
    }

    @Override
    public Film addLike(long id, long userId) {
        Film film = this.get(id);
        film.getLikes().add(userId);
        return film;
    }

    @Override
    public Film removeLike(long id, long userId) {
        Film film = this.get(id);
        film.getLikes().remove(userId);
        return film;
    }

    @Override
    public List<Film> getPopularFilm(int count, Long genreId, Integer year) {
        return null;
    }
    @Override
    public Set<Long> getLikes(long id) {
        if (this.notContainFilm(id)) {
            throw new EntityNotFoundException(
                    new ErrorResponse("Film id", String.format("Не найден фильм с ID: %d.", id))
            );
        }
        return data.get(id).getLikes();
    }

    @Override
    public boolean notContainFilm(long id) {
        return !data.containsKey(id);
    }

    @Override
    public List<Film> getFilmWithName(String name) {
        return null;
    }

    // Добавлен так как есть в интерфейсе "FilmStorage"
    @Override
    public List<Film> getDirectorSortedFilms(long id, String sortBy) {
        return new ArrayList<>();
    }

}