package ru.yandex.practicum.filmorate.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.enumarate.ChoosingSearch;
import ru.yandex.practicum.filmorate.exception.EntityNotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.ErrorResponse;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FilmService {
    private final FilmStorage filmStorage;
    private final UserStorage userStorage;
    private final DirectorStorage directorStorage;

    @Autowired
    public FilmService(@Qualifier("filmDbStorage") FilmStorage filmStorage,
                       @Qualifier("userDbStorage") UserStorage userStorage,
                       @Qualifier("directorDbStorage") DirectorStorage directorStorage) {
        this.filmStorage = filmStorage;
        this.userStorage = userStorage;
        this.directorStorage = directorStorage;
    }

    public Film addFilm(Film film) {
        Film savedFilm = filmStorage.add(film);
        log.info("Фильм добавлен. Film: {}", savedFilm);
        return savedFilm;
    }

    public Film updateFilm(Film film) {
        Film savedUFilm = filmStorage.update(film);
        log.info("Данные фильма обновлены. Film: {}", savedUFilm);
        return savedUFilm;
    }

    public Film getFilm(long id) {
        Film film = filmStorage.get(id);
        log.info("Получен фильм с id {}. Film: {}", id, film);
        return film;
    }

    public List<Film> getAllFilms() {
        List<Film> films = filmStorage.getAll();
        log.info("Получен список всех фильмов. List<Film>: {}", films);
        return films;
    }

    public Film addLike(long id, long userId) {
        if (userStorage.notContainUser(userId)) {
            throw new EntityNotFoundException(
                    new ErrorResponse("User id", String.format("Не найден пользователь с ID: %d.", id))
            );
        }
        Film film = filmStorage.addLike(id, userId);
        log.info("Добавлен лайк фильму с id {} от пользователя с id {}. Film: {}", id, userId, film);
        return film;
    }

    public Film removeLike(long id, long userId) {
        if (userStorage.notContainUser(userId)) {
            throw new EntityNotFoundException(
                    new ErrorResponse("User id", String.format("Не найден пользователь с ID: %d.", id))
            );
        }
        Film film = filmStorage.removeLike(id, userId);
        log.info("Удален лайк фильму с id {} от пользователя с id {}. Film: {}", id, userId, film);
        return film;
    }

    public List<Film> getPopular(int count) {
        List<Film> films = filmStorage.getPopular(count);
        log.info("Получен список {} самых популярных фильмов. List<Film>: {}", count, films);
        return films;
    }

    public List<Film> getCommonFilm(long userId, long friendId) {
        List<Long> likeUser = userStorage.getLikes(userId);
        List<Long> friendUser = userStorage.getLikes(friendId);
        return likeUser.stream()
                .filter(friendUser::contains)
                .map(this::getFilm)
                .collect(Collectors.toList());

    }

    // DIRECTOR.Получить список фильмов режиссера отсортированных по количеству лайков или году выпуска.
    public List<Film> getDirectorFilmsList(long id, String sortBy) {
        List<Film> films = filmStorage.getDirectorSortedFilms(id, sortBy);
        log.info("Получен список фильмов режисера Director Id: {}, " +
                "сортировка sortBy: {} list: {}", id, sortBy, films);
        return films;
    }

    //Поиск
    public List<Film> search(String query, List<String> by) {
        int len = by.size();
        if (len==1 && by.get(0).equals(String.valueOf(ChoosingSearch.title))) {
            List<Film> films = filmStorage.getFilmWithName(query);
            logQueryInfo(query, by, films);
            return films;
        } else if (len==1 && by.get(0).equals(String.valueOf(ChoosingSearch.director))) {
            List<Director> director = directorStorage.getDirectorsWithName(query);
            List<Film> films = new ArrayList<>();
            for (Director dir : director) {
                films.addAll(filmStorage.getDirectorSortedFilms(dir.getId(), "likes"));
            }
            logQueryInfo(query, by, films);
            return films;
        } else {
            List<Film> films = new ArrayList<>();

            directorStorage.getDirectorsWithName(query).forEach(
                    director -> films.addAll(filmStorage.getDirectorSortedFilms(director.getId(), "likes"))
            );
            films.addAll(filmStorage.getFilmWithName(query));

            logQueryInfo(query, by, films);
            return films;
        }
    }

    private void logQueryInfo(String query, List<String> by, List<Film> films) {
        log.info("Получен список фильмов по запросу '{}'. Поиск по {}: list: {}", query, by, films);
    }

}