package org.pr.dfs.model;

import org.pr.dfs.model.User;

public class UserContext {
    private static final ThreadLocal<User> currentUser = new ThreadLocal<>();

    public static void setCurrentUser(User user) {
        currentUser.set(user);
    }

    public static User getCurrentUser() {
        return currentUser.get();
    }

    public static String getCurrentUserId() {
        User user = currentUser.get();
        return user != null ? user.getUserId() : null;
    }

    public static String getCurrentUserDirectory() {
        User user = currentUser.get();
        return user != null ? user.getUserDirectory() : null;
    }

    public static void clear() {
        currentUser.remove();
    }
}
