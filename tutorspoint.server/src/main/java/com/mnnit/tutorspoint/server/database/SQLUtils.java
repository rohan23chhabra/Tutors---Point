package com.mnnit.tutorspoint.server.database;

import com.mnnit.tutorspoint.core.*;
import com.mnnit.tutorspoint.core.todo.Todo;
import com.mnnit.tutorspoint.core.video.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.*;
import java.util.*;

import static com.mnnit.tutorspoint.server.database.SQLConstants.*;

public class SQLUtils {

    private static final Connection connection = Database.getConnection();

    private SQLUtils() {
        //no instance
    }

    public static void insertUser(final User user) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_USER);
        preparedStatement.setString(1, user.getUsername());
        preparedStatement.setString(2, user.getPassword());
        preparedStatement.setString(3, user.getUserType().toString());
        preparedStatement.executeUpdate();
    }

    public static void insertVideo(final Video video) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_VIDEO);
        preparedStatement.setString(1, video.getName());
        preparedStatement.setString(2, video.getUsername());
        preparedStatement.setString(3, video.getCategory());
        preparedStatement.setString(4, video.getDateTime());
        preparedStatement.setInt(5, video.getComments().size());
        preparedStatement.setString(6, video.getFormat());
        preparedStatement.executeUpdate();
        video.setVideoId(getVideoIdByName(video.getName()));

        List<Subscription> subscriptionList = getSubscriptionsForTeacher(video.getUsername());
        subscriptionList.forEach(subscription -> {
            try {
                insertNotification(new Notification(subscription, "New Video from - " + video.getUsername(), false));
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static int getVideoIdByName(final String videoName) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement(GET_VIDEO_ID_BY_VIDEO_NAME);
        preparedStatement.setString(1, videoName);
        final ResultSet resultSet = preparedStatement.executeQuery();
        int videoid = -1;
        while (resultSet.next()) {
            videoid = resultSet.getInt("videoid");
        }
        return videoid;
    }

    public static List<Subscription> getSubscriptionsForTeacher(final String teacherName) throws SQLException {
        Vector<Subscription> result = new Vector<>();
        final PreparedStatement preparedStatement = connection
                .prepareStatement("SELECT * FROM Subscriptions WHERE subscribedTo = ?");
        preparedStatement.setString(1, teacherName);
        final ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            final Subscription subscription = new Subscription();
            subscription.setId(resultSet.getInt("subscriptionId"));
            subscription.setSubscriber(resultSet.getString("subscriber"));
            subscription.setSubscribedTo(teacherName);
            result.add(subscription);
        }
        return result;
    }

    public static void insertNotification(final Notification notification) throws SQLException {
        final PreparedStatement preparedStatement = connection
                .prepareStatement("INSERT INTO Notifications (subscriptionId, message, isSent) VALUES (?, ?, ?)");
        preparedStatement.setInt(1, notification.getSubscription().getId());
        preparedStatement.setString(2, notification.getMessage());
        preparedStatement.setBoolean(3, notification.isSent());
        preparedStatement.executeUpdate();
    }

    public static List<Video> getVideoList() throws SQLException {
        Vector<Video> videos = new Vector<>();
        final PreparedStatement preparedStatement = connection.prepareStatement(GET_VIDEOS_LIST);
        final ResultSet resultSet = preparedStatement.executeQuery();
        Video video;
        while (resultSet.next()) {
            video = getVideoById(resultSet.getInt("videoid"));
            videos.add(video);
        }
        return videos;
    }

    public static Video getVideoById(final int id) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement(GET_VIDEO_BY_ID);
        preparedStatement.setInt(1, id);
        final ResultSet resultSet = preparedStatement.executeQuery();
        Video result = new Video();
        result.setVideoId(id);
        while (resultSet.next()) {
            result.setName(resultSet.getString("name"));
            result.setUsername(resultSet.getString("uploader"));
            result.setCategory(resultSet.getString("category"));
            result.setDateTime(resultSet.getString("uploadTime"));
            result.setLikes(getLikesByVideoId(id));
            result.setComments(getCommentsByVideoId(id));
            result.setTags(getTagsByVideoId(id));
            result.setFormat(resultSet.getString("format"));
        }
        return result;
    }

    public static List<Like> getLikesByVideoId(final int videoId) throws SQLException {
        Vector<Like> likes = new Vector<>();
        final PreparedStatement preparedStatement = connection.prepareStatement(GET_LIKES_BY_VIDEO_ID);
        preparedStatement.setInt(1, videoId);
        final ResultSet resultSet = preparedStatement.executeQuery();
        Like like = new Like();
        while (resultSet.next()) {
            like.setVideoId(videoId);
            like.setDateTime(resultSet.getString("dateTime"));
            like.setUsername(resultSet.getString("username"));
            likes.add(like);
            like = new Like();
        }
        return likes;
    }

    public static List<Comment> getCommentsByVideoId(final int id) throws SQLException {
        Vector<Comment> comments = new Vector<>();
        final PreparedStatement preparedStatement = connection.prepareStatement(GET_COMMENTS_BY_VIDEO_ID);
        preparedStatement.setInt(1, id);
        final ResultSet resultSet = preparedStatement.executeQuery();
        Comment comment = new Comment(null, null);
        while (resultSet.next()) {
            comment.setVideoId(id);
            comment.setUsername(resultSet.getString("commenter"));
            comment.setDateTime(resultSet.getString("dateTime"));
            comment.setMessage(resultSet.getString("message"));
            comments.add(comment);
            comment = new Comment(null, null);
        }
        return comments;
    }

    public static List<Tag> getTagsByVideoId(final int videoId) throws SQLException {
        Vector<Tag> result = new Vector<>();
        final PreparedStatement preparedStatement = connection.prepareStatement(GET_TAGS_BY_VIDEO_ID);
        preparedStatement.setInt(1, videoId);
        final ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            Tag tag = new Tag();
            tag.setName(resultSet.getString("name"));
            tag.setVideoId(resultSet.getInt("videoId"));
            result.add(tag);
        }
        return result;
    }

    public static void insertComment(final Comment comment) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_COMMENT);
        preparedStatement.setInt(1, comment.getVideoId());
        preparedStatement.setString(2, comment.getMessage());
        preparedStatement.setString(3, comment.getUsername());
        preparedStatement.setString(4, comment.getDateTime());
        preparedStatement.executeUpdate();
    }

    public static void insertTag(final Tag tag) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_TAG);
        preparedStatement.setString(1, tag.getName());
        preparedStatement.setInt(2, tag.getVideoId());
        preparedStatement.executeUpdate();
    }

    public static void insertLike(final Like like) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_LIKE);
        preparedStatement.setInt(1, like.getVideoId());
        preparedStatement.setString(2, like.getDateTime());
        preparedStatement.setString(3, like.getUsername());
        preparedStatement.executeUpdate();
    }

    public static List<Video> getVideosByCategory(final String category) throws SQLException {
        if (category == null) {
            return Collections.emptyList();
        }
        Vector<Video> result = new Vector<>();
        final PreparedStatement preparedStatement = connection.prepareStatement(GET_VIDEOS_BY_CATEGORY);
        preparedStatement.setString(1, category);
        final ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            Video video = getVideoById(resultSet.getInt("videoId"));
            result.add(video);
        }
        getCategoriesByParent(category).forEach(e -> {
            try {
                result.addAll(getVideosByCategory(e.getName()));
            } catch (SQLException e1) {
                e1.printStackTrace();
            }
        });
        return result;
    }

    public static List<VideoCategory> getCategoriesByParent(final String parentName) throws SQLException {
        if (parentName == null) {
            return Collections.emptyList();
        }
        Vector<VideoCategory> result = new Vector<>();
        final PreparedStatement preparedStatement = connection.prepareStatement(GET_CATEGORIES_BY_PARENT);
        preparedStatement.setString(1, parentName);
        final ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            final VideoCategory videoCategory = new VideoCategory(resultSet.getString("name"),
                    resultSet.getInt("rating"));
            videoCategory.setParentName(resultSet.getString("PARENT"));
            result.add(videoCategory);
            List<VideoCategory> children = getCategoriesByParent(videoCategory.getName());
            if (!children.isEmpty()) {
                result.addAll(children);
            }
        }
        return result;
    }

    public static List<User> getUsersList() throws SQLException {
        final Vector<User> result = new Vector<>();
        final PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM Users");
        ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            UserBuilder userBuilder = new UserBuilder();
            userBuilder.setUsername(resultSet.getString("username"));
            userBuilder.setPassword(resultSet.getString("password"));
            userBuilder.setUserType(UserType.valueOf(resultSet.getString("userype")));
            result.add(userBuilder.createUser());
        }
        return result;
    }

    public static void insertSubscription(final Subscription subscription) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement(INSERT_SUBSCRIPTION);
        preparedStatement.setString(1, subscription.getSubscriber());
        preparedStatement.setString(2, subscription.getSubscribedTo());
        preparedStatement.executeUpdate();
    }

    public static List<Subscription> getSubscriptionList() throws SQLException {
        final Vector<Subscription> result = new Vector<>();
        final PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT *\n" +
                        "FROM Subscriptions;");
        final ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            final Subscription subscription = new Subscription();
            subscription.setSubscriber(resultSet.getString("subscriber"));
            subscription.setSubscribedTo(resultSet.getString("subscribedTo"));
            subscription.setId(resultSet.getInt("subscriptionId"));
            result.add(subscription);
        }
        return result;
    }

    public static Notification getNotificationBySubscriptionId(final int id) throws SQLException {
        final PreparedStatement preparedStatement = connection
                .prepareStatement("SELECT * FROM Notifications WHERE subscriptionId = ?");
        preparedStatement.setInt(1, id);
        final ResultSet resultSet = preparedStatement.executeQuery();
        final Notification notification = new Notification();
        while (resultSet.next()) {
            notification.setSubscription(getSubscriptionById(id));
            notification.setMessage(resultSet.getString("message"));
            notification.setSent(resultSet.getBoolean("isSent"));
        }
        return notification;
    }

    public static Subscription getSubscriptionById(final int id) throws SQLException {
        final PreparedStatement preparedStatement = connection
                .prepareStatement("SELECT * FROM Subscriptions WHERE subscriptionId = ?");
        preparedStatement.setInt(1, id);
        final ResultSet resultSet = preparedStatement.executeQuery();
        Subscription subscription = new Subscription();
        while (resultSet.next()) {
            subscription.setId(id);
            subscription.setSubscriber(resultSet.getString("subscriber"));
            subscription.setSubscribedTo(resultSet.getString("subscribedTo"));
        }
        return subscription;
    }

    public static void insertTodo(final Todo todo) throws SQLException {
        final PreparedStatement preparedStatement = connection
                .prepareStatement("INSERT INTO Todos (student, message) VALUES (?, ?)");
        preparedStatement.setString(1, todo.getStudent());
        preparedStatement.setString(2, todo.getMessage());
        preparedStatement.executeUpdate();
    }

    public static List<Todo> getTodosList() throws SQLException {
        final Vector<Todo> result = new Vector<>();
        final PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM Todos");
        final ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            Todo todo = new Todo();
            todo.setStudent(resultSet.getString("student"));
            todo.setMessage(resultSet.getString("message"));
            result.add(todo);
        }
        return result;
    }

    public static List<Todo> getTodosByUser(final String user) throws SQLException {
        final Vector<Todo> result = new Vector<>();
        final PreparedStatement preparedStatement = connection
                .prepareStatement("SELECT * FROM Todos WHERE student = ?");
        preparedStatement.setString(1, user);
        final ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            Todo todo = new Todo();
            todo.setMessage(resultSet.getString("message"));
            todo.setStudent(user);
            result.add(todo);
        }
        return result;
    }

    public static void insertInProgress(final InProgress inProgress) throws SQLException {
        final PreparedStatement preparedStatement = connection
                .prepareStatement("INSERT INTO InProgress (student, category) VALUES (?, ?)");
        preparedStatement.setString(1, inProgress.getStudent());
        preparedStatement.setString(2, inProgress.getCategory().getName());
        preparedStatement.executeUpdate();
    }

    public static List<InProgress> getInProgressByUser(final String user) throws SQLException {
        final PreparedStatement preparedStatement = connection
                .prepareStatement("SELECT * FROM InProgress WHERE student=?");
        preparedStatement.setString(1, user);
        ResultSet resultSet = preparedStatement.executeQuery();
        Vector<InProgress> result = new Vector<>();
        while (resultSet.next()) {
            result.add(
                    new InProgress(getCategoryByName(resultSet.getString("category")), resultSet.getString("student")));
        }
        return result;
    }

    public static VideoCategory getCategoryByName(final String name) throws SQLException {
        final PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT * FROM MAIN.CATEGORIES WHERE NAME = ?");
        preparedStatement.setString(1, name);
        final ResultSet resultSet = preparedStatement.executeQuery();
        final VideoCategory videoCategory = new VideoCategory();
        while (resultSet.next()) {
            videoCategory.setName(name);
            videoCategory.setParentName(resultSet.getString("parent"));
            videoCategory.setRating(resultSet.getInt("rating"));
        }
        return videoCategory;
    }

    public static List<Video> getVideosByTag(final Tag tag) throws Throwable {
        Vector<Video> result = new Vector<>();
        final PreparedStatement preparedStatement = connection
                .prepareStatement("SELECT videoId FROM Tags WHERE name=?");
        preparedStatement.setString(1, tag.getName());
        final ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            Video video = getVideoById(resultSet.getInt("videoId"));
            result.add(video);
        }

        return result;
    }

    public static void deleteVideo(final int videoId, final String videoPath) throws Throwable {
        final PreparedStatement[] preparedStatements = new PreparedStatement[]{
                connection.prepareStatement("DELETE FROM Videos WHERE videoId=?"),
                connection.prepareStatement("DELETE FROM Tags WHERE videoId=?"),
                connection.prepareStatement("DELETE FROM Likes WHERE videoId=?"),
                connection.prepareStatement("DELETE FROM Comments WHERE videoId=?")
        };

        for (int i = 0; i < preparedStatements.length; i++) {
            PreparedStatement preparedStatement = preparedStatements[i];
            preparedStatement.setInt(1, videoId);
            preparedStatement.executeUpdate();
        }

        Files.delete(Paths.get(videoPath + videoId + ".vid"));
    }

    public static List<Video> getVideosByUploader(final String uploader) throws Throwable {
        Vector<Video> result = new Vector<>();
        final PreparedStatement preparedStatement = connection
                .prepareStatement("SELECT videoId FROM Videos WHERE uploader = ?");
        preparedStatement.setString(1, uploader);
        final ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            Video video = getVideoById(resultSet.getInt("videoId"));
            result.add(video);
        }
        return result;
    }

    public static double getAverageRating() throws Throwable {
        final PreparedStatement preparedStatement = connection
                .prepareStatement(
                        "SELECT CATEGORIES.RATING FROM CATEGORIES WHERE MAIN.CATEGORIES.NAME != 'Hyper Category'");
        final ResultSet resultSet = preparedStatement.executeQuery();
        double result = 0.0;
        int cnt = 0;
        while (resultSet.next()) {
            result += resultSet.getInt("rating");
            cnt++;
        }
        result /= cnt;
        return result;
    }

    public static List<VideoCategory> getSortedCategories() throws Throwable {
        Vector<VideoCategory> result = new Vector<>();
        final PreparedStatement preparedStatement = connection.prepareStatement(
                "SELECT *\n" +
                        "FROM MAIN.CATEGORIES\n" +
                        "WHERE NAME != 'Hyper Category'\n" +
                        "ORDER BY RATING");
        final ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            result.add(
                    new VideoCategory(
                            resultSet.getString("name"),
                            resultSet.getInt("rating"),
                            resultSet.getString("parent")
                    )
            );
        }

        return result;
    }

    public static List<Notification> getNotificationsForUser(final String username) throws Throwable {
        Vector<Notification> result = new Vector<>();
        final PreparedStatement preparedStatement = connection.prepareStatement(GET_NOTIFICATIONS_FOR_USER);
        preparedStatement.setString(1, username);
        final ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            result.add(new Notification(
                    new Subscription(
                            resultSet.getString("subscriber"),
                            resultSet.getString("subscribedTo"),
                            resultSet.getInt("subscriptionId")
                    ),
                    resultSet.getString("message"),
                    resultSet.getBoolean("issent")));
        }

        return result;
    }

    public static void updateIsSentForUser(final String username) throws Throwable {

        Vector<Integer> result = getSubscriptionIdsBySubscriber(username);
        result.forEach(id -> {
            try {
                final PreparedStatement preparedStatement = connection.prepareStatement("" +
                        "UPDATE MAIN.NOTIFICATIONS SET ISSENT=? WHERE SUBSCRIPTIONID=?");
                preparedStatement.setBoolean(1, true);
                preparedStatement.setInt(2, id);
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    public static Vector<Integer> getSubscriptionIdsBySubscriber(final String subscriber) throws Throwable {
        Vector<Integer> result = new Vector<>();
        final PreparedStatement preparedStatement = connection.prepareStatement("" +
                "SELECT MAIN.SUBSCRIPTIONS.SUBSCRIPTIONID FROM MAIN.SUBSCRIPTIONS WHERE SUBSCRIBER=?");
        preparedStatement.setString(1, subscriber);
        final ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            result.add(resultSet.getInt("subscriptionid"));
        }

        return result;
    }

    public static void deleteLike(final int videoId, final String username) throws Throwable {
        final PreparedStatement preparedStatement = connection.prepareStatement("" +
                "DELETE FROM MAIN.LIKES WHERE VIDEOID=? AND USERNAME=?");
        preparedStatement.setInt(1, videoId);
        preparedStatement.setString(2, username);
        preparedStatement.executeUpdate();
    }

    public static void deleteComment(final int videoId, final String username) throws Throwable {
        final PreparedStatement preparedStatement = connection
                .prepareStatement("DELETE FROM MAIN.COMMENTS WHERE VIDEOID=? AND COMMENTER=?");
        preparedStatement.setInt(1, videoId);
        preparedStatement.setString(2, username);
        preparedStatement.executeUpdate();
    }

    public static List<User> getUsersByUsername(final String username) throws Throwable {
        final Vector<User> result = new Vector<>();
        final PreparedStatement preparedStatement = connection
                .prepareStatement("SELECT * FROM MAIN.USERS WHERE USERNAME=?");
        preparedStatement.setString(1, username);
        final ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            String type = resultSet.getString("userype");
            UserType userType;
            if (type.equals("STUDENT")) {
                userType = UserType.STUDENT;
            } else {
                userType = UserType.TEACHER;
            }
            result.add(new User(resultSet.getString("username"),
                    resultSet.getString("password"), userType));
        }
        return result;
    }

    public static void updateRating(final int rating, final String category) throws Throwable {
        final PreparedStatement preparedStatement = connection
                .prepareStatement("UPDATE MAIN.CATEGORIES SET RATING=?,NUMBER=? WHERE NAME=?");

        VideoCategory videoCategory = getCategoryByName(category);
        int number = getNumberByCategory(category);
        if (number == 0) {
            //not rated yet.
            preparedStatement.setInt(1, rating);
        } else {
            //already rated at least once.
            int newRating = ((videoCategory.getRating() * number) + rating) / (number + 1);
            preparedStatement.setInt(1, newRating);
        }
        preparedStatement.setInt(2, number + 1);
        preparedStatement.setString(3, category);
        preparedStatement.executeUpdate();
    }

    public static Integer getNumberByCategory(final String category) throws Throwable {
        final PreparedStatement preparedStatement = connection
                .prepareStatement("SELECT MAIN.CATEGORIES.NUMBER FROM MAIN.CATEGORIES WHERE NAME=?");
        preparedStatement.setString(1, category);
        final ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            return resultSet.getInt("number");
        }
        return 0;
    }

    public static List<VideoCategory> getCategoriesByName(final String name) throws Throwable {
        Vector<VideoCategory> vector = new Vector<>();
        final PreparedStatement preparedStatement = connection
                .prepareStatement("SELECT * FROM MAIN.CATEGORIES WHERE NAME=?");
        preparedStatement.setString(1, name);
        final ResultSet resultSet = preparedStatement.executeQuery();
        while (resultSet.next()) {
            vector.add(new VideoCategory(name,
                    resultSet.getInt("rating"),
                    resultSet.getString("parent")));
        }

        return vector;
    }

    public static void insertCategory(final String name, final String parent) throws Throwable {
        final PreparedStatement preparedStatement = connection
                .prepareStatement("INSERT INTO MAIN.CATEGORIES (NAME, PARENT) VALUES (?,?)");
        preparedStatement.setString(1, name);
        preparedStatement.setString(2, parent);
        preparedStatement.executeUpdate();
    }

}
