package com.ori.afinal.Services;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.ori.afinal.model.Event;
import com.ori.afinal.model.Notification;
import com.ori.afinal.model.User;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class DatabaseService {

    private static final String TAG = "DatabaseService";

    private static final String USERS_PATH = "users",
            EVENTS_PATH = "events",
            CARTS_PATH = "carts",
            NOTIFICATIONS_PATH = "notifications"; // הנתיב להתראות

    public interface DatabaseCallback<T> {
        public void onCompleted(T object);
        public void onFailed(Exception e);
    }

    private static DatabaseService instance;

    private final DatabaseReference databaseReference;

    private DatabaseService() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
    }

    public static DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    private void writeData(@NotNull final String path, @NotNull final Object data, final @Nullable DatabaseCallback<Void> callback) {
        readData(path).setValue(data, (error, ref) -> {
            if (error != null) {
                if (callback == null) return;
                callback.onFailed(error.toException());
            } else {
                if (callback == null) return;
                callback.onCompleted(null);
            }
        });
    }

    private void deleteData(@NotNull final String path, @Nullable final DatabaseCallback<Void> callback) {
        readData(path).removeValue((error, ref) -> {
            if (error != null) {
                if (callback == null) return;
                callback.onFailed(error.toException());
            } else {
                if (callback == null) return;
                callback.onCompleted(null);
            }
        });
    }

    private DatabaseReference readData(@NotNull final String path) {
        return databaseReference.child(path);
    }

    private <T> void getData(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull final DatabaseCallback<T> callback) {
        readData(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error getting data", task.getException());
                callback.onFailed(task.getException());
                return;
            }
            T data = task.getResult().getValue(clazz);
            callback.onCompleted(data);
        });
    }

    private <T> void getDataList(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull final DatabaseCallback<List<T>> callback) {
        readData(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error getting data", task.getException());
                callback.onFailed(task.getException());
                return;
            }
            List<T> tList = new ArrayList<>();
            task.getResult().getChildren().forEach(dataSnapshot -> {
                T t = dataSnapshot.getValue(clazz);
                tList.add(t);
            });

            callback.onCompleted(tList);
        });
    }

    private String generateNewId(@NotNull final String path) {
        return databaseReference.child(path).push().getKey();
    }

    private <T> void runTransaction(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull UnaryOperator<T> function, @NotNull final DatabaseCallback<T> callback) {
        readData(path).runTransaction(new Transaction.Handler() {
            @NonNull
            @Override
            public Transaction.Result doTransaction(@NonNull MutableData currentData) {
                T currentValue = currentData.getValue(clazz);
                if (currentValue == null) {
                    currentValue = function.apply(null);
                } else {
                    currentValue = function.apply(currentValue);
                }
                currentData.setValue(currentValue);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    Log.e(TAG, "Transaction failed", error.toException());
                    callback.onFailed(error.toException());
                    return;
                }
                T result = currentData != null ? currentData.getValue(clazz) : null;
                callback.onCompleted(result);
            }
        });
    }

    // region User Section

    public void createNewUser(@NotNull final User user,
                              @Nullable final DatabaseCallback<String> callback) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.createUserWithEmailAndPassword(user.getEmail(), user.getPassword())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = mAuth.getUid();
                        user.setId(uid);
                        writeData(USERS_PATH + "/" + uid, user, new DatabaseCallback<Void>() {
                            @Override
                            public void onCompleted(Void v) {
                                if (callback != null) callback.onCompleted(uid);
                            }

                            @Override
                            public void onFailed(Exception e) {
                                if (callback != null) callback.onFailed(e);
                            }
                        });
                    } else {
                        if (callback != null)
                            callback.onFailed(task.getException());
                    }
                });
    }

    public void loginUser(@NotNull final String email,final String password,
                          @Nullable final DatabaseCallback<String> callback) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                        if (callback != null)
                            callback.onCompleted(uid);
                    } else {
                        if (callback != null)
                            callback.onFailed(task.getException());
                    }
                });
    }

    public void getUser(@NotNull final String uid, @NotNull final DatabaseCallback<User> callback) {
        getData(USERS_PATH + "/" + uid, User.class, callback);
    }

    public void getUserList(@NotNull final DatabaseCallback<List<User>> callback) {
        getDataList(USERS_PATH, User.class, callback);
    }

    public void deleteUser(@NotNull final String uid, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(USERS_PATH + "/" + uid, callback);
    }

    public void updateUser(@NotNull final User user, @Nullable final DatabaseCallback<Void> callback) {
        runTransaction(USERS_PATH + "/" + user.getId(), User.class, currentUser -> user, new DatabaseCallback<User>() {
            @Override
            public void onCompleted(User object) {
                if (callback != null) callback.onCompleted(null);
            }
            @Override
            public void onFailed(Exception e) {
                if (callback != null) callback.onFailed(e);
            }
        });
    }

    // endregion User Section

    // region Event Section

    public void createNewEvent(@NotNull final Event event, @Nullable final DatabaseCallback<Void> callback) {
        writeData(EVENTS_PATH + "/" + event.getId(), event, callback);
    }

    public void updateEvent(@NotNull final Event event, @Nullable final DatabaseCallback<Void> callback) {
        writeData(EVENTS_PATH + "/" + event.getId(), event, callback);
    }

    public void getEvent(@NotNull final String eventId, @NotNull final DatabaseCallback<Event> callback) {
        getData(EVENTS_PATH + "/" + eventId, Event.class, callback);
    }

    public void getEventList(@NotNull final DatabaseCallback<List<Event>> callback) {
        getDataList(EVENTS_PATH, Event.class, callback);
    }

    public void getUserEvents(@NotNull final String userId, @NotNull final DatabaseCallback<List<Event>> callback) {
        getDataList(EVENTS_PATH, Event.class, new DatabaseCallback<List<Event>>() {
            @Override
            public void onCompleted(List<Event> allEvents) {
                List<Event> userEvents = new ArrayList<>();
                for (Event event : allEvents) {
                    boolean isCreator = event.getEventAdmin() != null && userId.equals(event.getEventAdmin().getId());
                    boolean isAccepted = event.getParticipantIds() != null && event.getParticipantIds().contains(userId);

                    if (isCreator || isAccepted) {
                        userEvents.add(event);
                    }
                }
                callback.onCompleted(userEvents);
            }

            @Override
            public void onFailed(Exception e) {
                callback.onFailed(e);
            }
        });
    }

    // פונקציית המענה על הזמנה (אישור או דחייה) - עכשיו שולחת גם התראה למנהל!
    public void respondToInvitation(String eventId, String userId, boolean isAccepted, DatabaseCallback<Void> callback) {
        getEvent(eventId, new DatabaseCallback<Event>() {
            @Override
            public void onCompleted(Event event) {
                if (event != null) {
                    // עדכון הרשימות בהתאם לתשובה
                    if (event.getInvitedParticipantIds() != null) {
                        event.getInvitedParticipantIds().remove(userId);
                    }

                    if (isAccepted) {
                        if (event.getParticipantIds() == null) event.setParticipantIds(new ArrayList<>());
                        if (!event.getParticipantIds().contains(userId)) event.getParticipantIds().add(userId);
                        if (event.getDeclinedParticipantIds() != null) event.getDeclinedParticipantIds().remove(userId);
                    } else {
                        if (event.getDeclinedParticipantIds() == null) event.setDeclinedParticipantIds(new ArrayList<>());
                        if (!event.getDeclinedParticipantIds().contains(userId)) event.getDeclinedParticipantIds().add(userId);
                        if (event.getParticipantIds() != null) event.getParticipantIds().remove(userId);
                    }

                    // שומרים את הפגישה المעודכנת
                    updateEvent(event, new DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void object) {
                            // קודם כל מחזירים תשובה ל-UI כדי שהמשתמש לא יחכה
                            if (callback != null) callback.onCompleted(null);

                            // תהליך צדדי: יצירת התראה למנהל הפגישה
                            if (event.getEventAdmin() != null) {
                                String adminId = event.getEventAdmin().getId();
                                // אין טעם לשלוח למנהל התראה על עצמו אם איכשהו הוא הגיב
                                if (!adminId.equals(userId)) {

                                    // נביא את שם המשתמש שהגיב (כדי שהמנהל ידע מי זה)
                                    getUser(userId, new DatabaseCallback<User>() {
                                        @Override
                                        public void onCompleted(User user) {
                                            String userName = (user != null && user.getFname() != null) ? user.getFname() : "משתמש";
                                            String title = isAccepted ? "אישור הגעה" : "דחיית הגעה";
                                            String message = userName + (isAccepted ? " אישר/ה הגעה לפגישה: " : " דחה/תה הגעה לפגישה: ") + event.getTitle();

                                            Notification notification = new Notification(
                                                    generateNotificationId(),
                                                    adminId, // ההתראה נשלחת אל המנהל!
                                                    title,
                                                    message,
                                                    "INFO",
                                                    event.getId(),
                                                    System.currentTimeMillis()
                                            );
                                            sendNotification(notification, null);
                                        }

                                        @Override
                                        public void onFailed(Exception e) { }
                                    });
                                }
                            }
                        }

                        @Override
                        public void onFailed(Exception e) {
                            if (callback != null) callback.onFailed(e);
                        }
                    });
                }
            }

            @Override
            public void onFailed(Exception e) {
                if (callback != null) callback.onFailed(e);
            }
        });
    }

    public String generateEventId() {
        return generateNewId(EVENTS_PATH);
    }

    public void deleteEvent(@NotNull final String eventId, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(EVENTS_PATH + "/" + eventId, callback);
    }

    // endregion Event Section

    // region Notifications Section

    public String generateNotificationId() {
        return generateNewId(NOTIFICATIONS_PATH);
    }

    public void sendNotification(@NotNull final Notification notification, @Nullable final DatabaseCallback<Void> callback) {
        writeData(NOTIFICATIONS_PATH + "/" + notification.getId(), notification, callback);
    }

    public void getSmartNotifications(@NotNull final String userId, @NotNull final DatabaseCallback<List<Notification>> callback) {
        readData(NOTIFICATIONS_PATH).orderByChild("userId").equalTo(userId).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                Log.e(TAG, "Error getting notifications", task.getException());
                callback.onFailed(task.getException());
                return;
            }
            List<Notification> notificationsList = new ArrayList<>();
            task.getResult().getChildren().forEach(dataSnapshot -> {
                Notification n = dataSnapshot.getValue(Notification.class);
                if (n != null) {
                    notificationsList.add(n);
                }
            });
            callback.onCompleted(notificationsList);
        });
    }

    public void deleteNotification(@NotNull final String notificationId, @Nullable final DatabaseCallback<Void> callback) {
        deleteData(NOTIFICATIONS_PATH + "/" + notificationId, callback);
    }

    // endregion Notifications Section
}