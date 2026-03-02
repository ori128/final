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
            NOTIFICATIONS_PATH = "notifications";

    public interface DatabaseCallback<T> {
        void onCompleted(T object);
        void onFailed(Exception e);
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
                if (callback != null) callback.onFailed(error.toException());
            } else {
                if (callback != null) callback.onCompleted(null);
            }
        });
    }

    private void deleteData(@NotNull final String path, @Nullable final DatabaseCallback<Void> callback) {
        readData(path).removeValue((error, ref) -> {
            if (error != null) {
                if (callback != null) callback.onFailed(error.toException());
            } else {
                if (callback != null) callback.onCompleted(null);
            }
        });
    }

    private DatabaseReference readData(@NotNull final String path) {
        return databaseReference.child(path);
    }

    private <T> void getData(@NotNull final String path, @NotNull final Class<T> clazz, @NotNull final DatabaseCallback<T> callback) {
        readData(path).get().addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
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
                callback.onFailed(task.getException());
                return;
            }
            List<T> tList = new ArrayList<>();
            task.getResult().getChildren().forEach(dataSnapshot -> {
                T t = dataSnapshot.getValue(clazz);
                if (t != null) tList.add(t);
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
                currentValue = function.apply(currentValue);
                currentData.setValue(currentValue);
                return Transaction.success(currentData);
            }

            @Override
            public void onComplete(@Nullable DatabaseError error, boolean committed, @Nullable DataSnapshot currentData) {
                if (error != null) {
                    callback.onFailed(error.toException());
                    return;
                }
                T result = currentData != null ? currentData.getValue(clazz) : null;
                callback.onCompleted(result);
            }
        });
    }

    // region User Section
    public void createNewUser(@NotNull final User user, @Nullable final DatabaseCallback<String> callback) {
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
                    } else if (callback != null) callback.onFailed(task.getException());
                });
    }

    public void loginUser(@NotNull final String email, final String password, @Nullable final DatabaseCallback<String> callback) {
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && mAuth.getCurrentUser() != null) {
                        if (callback != null) callback.onCompleted(mAuth.getCurrentUser().getUid());
                    } else if (callback != null) callback.onFailed(task.getException());
                });
    }

    public void getUser(@NotNull final String uid, @NotNull final DatabaseCallback<User> callback) {
        getData(USERS_PATH + "/" + uid, User.class, callback);
    }

    public void getEventList(@NotNull final DatabaseCallback<List<Event>> callback) {
        getDataList(EVENTS_PATH, Event.class, callback);
    }
    // endregion

    // region Event Section
    public void getUserEvents(@NotNull final String userId, @NotNull final DatabaseCallback<List<Event>> callback) {
        getEventList(new DatabaseCallback<List<Event>>() {
            @Override
            public void onCompleted(List<Event> allEvents) {
                List<Event> userEvents = new ArrayList<>();
                for (Event event : allEvents) {
                    boolean isCreator = event.getEventAdmin() != null && userId.equals(event.getEventAdmin().getId());
                    boolean isAccepted = event.getParticipantIds() != null && event.getParticipantIds().contains(userId);
                    if (isCreator || isAccepted) userEvents.add(event);
                }
                callback.onCompleted(userEvents);
            }
            @Override
            public void onFailed(Exception e) { callback.onFailed(e); }
        });
    }

    // הפונקציה שהייתה חסרה וגרמה לשגיאה
    public void getUserNotifications(@NotNull final String userId, @NotNull final DatabaseCallback<List<Event>> callback) {
        getEventList(new DatabaseCallback<List<Event>>() {
            @Override
            public void onCompleted(List<Event> allEvents) {
                List<Event> pendingEvents = new ArrayList<>();
                for (Event event : allEvents) {
                    if (event.getInvitedParticipantIds() != null && event.getInvitedParticipantIds().contains(userId)) {
                        pendingEvents.add(event);
                    }
                }
                callback.onCompleted(pendingEvents);
            }
            @Override
            public void onFailed(Exception e) { callback.onFailed(e); }
        });
    }

    public void updateEvent(@NotNull final Event event, @Nullable final DatabaseCallback<Void> callback) {
        writeData(EVENTS_PATH + "/" + event.getId(), event, callback);
    }

    public void getEvent(@NotNull final String eventId, @NotNull final DatabaseCallback<Event> callback) {
        getData(EVENTS_PATH + "/" + eventId, Event.class, callback);
    }

    public String generateEventId() { return generateNewId(EVENTS_PATH); }

    public String generateNotificationId() { return generateNewId(NOTIFICATIONS_PATH); }

    public void sendNotification(@NotNull final Notification notification, @Nullable final DatabaseCallback<Void> callback) {
        writeData(NOTIFICATIONS_PATH + "/" + notification.getId(), notification, callback);
    }
    // endregion
}