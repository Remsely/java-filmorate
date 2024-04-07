package ru.yandex.practicum.filmorate.storage.user;

import ru.yandex.practicum.filmorate.model.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface UserStorage {
    User add(User user);

    User update(User user);

    User get(long id);

    List<User> getAll();

    User addFriend(long id, long friendId);

    User removeFriend(long id, long friendId);

    List<User> getFriends(long id);

    List<User> getCommonFriends(long id, long otherId);

    List<Long> getLikes(long id);

    boolean notContainUser(long id);

    public Map<Long, Set<Long>> findUsersWithLikes();
}