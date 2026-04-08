package com.ori.afinal.Services;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.ori.afinal.model.Event;
import com.ori.afinal.model.User;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DatabaseService {

    private static final String TAG = "DatabaseService";
    private static DatabaseService instance;
    private final DatabaseReference databaseReference;

    private static final String USERS_PATH = "users";
    private static final String EVENTS_PATH = "events";
    private static final String NOTIFICATIONS_PATH = "notifications";

    private DatabaseService() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();
    }

    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    public interface DatabaseCallback<T> {
        void onCompleted(T object);
        void onFailed(Exception e);
    }

    private void writeData(@NotNull final String path, @NotNull final Object data, @Nullable final DatabaseCallback<Void> callback) {
        databaseReference.child(path).setValue(data).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (callback != null) callback.onCompleted(null);
            } else {
                if (callback != null) callback.onFailed(task.getException());
            }
        });
    }

    // --- User Section ---
    public void saveUser(@NotNull final User user, @Nullable final DatabaseCallback<Void> callback) {
        writeData(USERS_PATH + "/" + user.getId(), user, callback);
    }

    public void getUser(@NotNull final String uid, @NotNull final DatabaseCallback<User> callback) {
        databaseReference.child(USERS_PATH).child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                User user = snapshot.getValue(User.class);
                callback.onCompleted(user);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailed(error.toException());
            }
        });
    }

    public void updateUser(@NotNull final User user, @Nullable final DatabaseCallback<Void> callback) {
        writeData(USERS_PATH + "/" + user.getId(), user, callback);
    }

    // --- Event Section ---
    public void saveEvent(@NotNull final Event event, @Nullable final DatabaseCallback<Void> callback) {
        if (event.getId() == null || event.getId().isEmpty()) {
            String key = databaseReference.child(EVENTS_PATH).push().getKey();
            event.setId(key);
        }
        writeData(EVENTS_PATH + "/" + event.getId(), event, callback);
    }

    public void getEvent(@NotNull final String eventId, @NotNull final DatabaseCallback<Event> callback) {
        databaseReference.child(EVENTS_PATH).child(eventId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Event event = snapshot.getValue(Event.class);
                callback.onCompleted(event);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailed(error.toException());
            }
        });
    }

    public void updateEvent(@NotNull final Event event, @Nullable final DatabaseCallback<Void> callback) {
        writeData(EVENTS_PATH + "/" + event.getId(), event, callback);
    }

    public void getUserEvents(@NotNull final String userId, @NotNull final DatabaseCallback<List<Event>> callback) {
        databaseReference.child(EVENTS_PATH).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Event> events = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null && event.getParticipantIds() != null && event.getParticipantIds().contains(userId)) {
                        events.add(event);
                    }
                }
                callback.onCompleted(events);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailed(error.toException());
            }
        });
    }

    // --- Notifications & Trash Section ---
    public void getUserNotifications(@NotNull final String userId, @NotNull final DatabaseCallback<List<Event>> callback) {
        databaseReference.child(EVENTS_PATH).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Event> events = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null && event.getInvitedParticipantIds() != null && event.getInvitedParticipantIds().contains(userId)) {
                        if (event.getTrashedParticipantIds() == null || !event.getTrashedParticipantIds().contains(userId)) {
                            events.add(event);
                        }
                    }
                }
                callback.onCompleted(events);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailed(error.toException());
            }
        });
    }

    public void getUserTrashedNotifications(@NotNull final String userId, @NotNull final DatabaseCallback<List<Event>> callback) {
        databaseReference.child(EVENTS_PATH).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Event> events = new ArrayList<>();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Event event = dataSnapshot.getValue(Event.class);
                    if (event != null && event.getTrashedParticipantIds() != null && event.getTrashedParticipantIds().contains(userId)) {
                        events.add(event);
                    }
                }
                callback.onCompleted(events);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onFailed(error.toException());
            }
        });
    }

    public void respondToInvitation(@NotNull final String eventId, @NotNull final String userId, boolean isAccepted, @Nullable final DatabaseCallback<Void> callback) {
        getEvent(eventId, new DatabaseCallback<Event>() {
            @Override
            public void onCompleted(Event event) {
                if (event != null) {
                    if (event.getInvitedParticipantIds() != null) {
                        event.getInvitedParticipantIds().remove(userId);
                    }
                    if (isAccepted) {
                        if (event.getParticipantIds() == null) {
                            event.setParticipantIds(new ArrayList<>());
                        }
                        if (!event.getParticipantIds().contains(userId)) {
                            event.getParticipantIds().add(userId);
                        }
                    }
                    updateEvent(event, callback);
                } else if (callback != null) {
                    callback.onFailed(new Exception("Event not found"));
                }
            }

            @Override
            public void onFailed(Exception e) {
                if (callback != null) callback.onFailed(e);
            }
        });
    }

    public void moveNotificationToTrash(@NotNull final String eventId, @NotNull final String userId, @Nullable final DatabaseCallback<Void> callback) {
        getEvent(eventId, new DatabaseCallback<Event>() {
            @Override
            public void onCompleted(Event event) {
                if (event != null) {
                    if (event.getTrashedParticipantIds() == null) {
                        event.setTrashedParticipantIds(new ArrayList<>());
                    }
                    if (!event.getTrashedParticipantIds().contains(userId)) {
                        event.getTrashedParticipantIds().add(userId);
                    }
                    updateEvent(event, callback);
                } else if (callback != null) {
                    callback.onFailed(new Exception("Event not found"));
                }
            }

            @Override
            public void onFailed(Exception e) {
                if (callback != null) callback.onFailed(e);
            }
        });
    }

    public void restoreNotificationFromTrash(@NotNull final String eventId, @NotNull final String userId, @Nullable final DatabaseCallback<Void> callback) {
        getEvent(eventId, new DatabaseCallback<Event>() {
            @Override
            public void onCompleted(Event event) {
                if (event != null) {
                    if (event.getTrashedParticipantIds() != null) {
                        event.getTrashedParticipantIds().remove(userId);
                    }
                    updateEvent(event, callback);
                } else if (callback != null) {
                    callback.onFailed(new Exception("Event not found"));
                }
            }

            @Override
            public void onFailed(Exception e) {
                if (callback != null) callback.onFailed(e);
            }
        });
    }

    public void deleteNotificationPermanently(@NotNull final String eventId, @NotNull final String userId, @Nullable final DatabaseCallback<Void> callback) {
        getEvent(eventId, new DatabaseCallback<Event>() {
            @Override
            public void onCompleted(Event event) {
                if (event != null) {
                    if (event.getInvitedParticipantIds() != null) {
                        event.getInvitedParticipantIds().remove(userId);
                    }
                    if (event.getTrashedParticipantIds() != null) {
                        event.getTrashedParticipantIds().remove(userId);
                    }
                    updateEvent(event, callback);
                } else if (callback != null) {
                    callback.onFailed(new Exception("Event not found"));
                }
            }

            @Override
            public void onFailed(Exception e) {
                if (callback != null) callback.onFailed(e);
            }
        });
    }

    // --- Admin Actions ---
    public void getAllUsers(final DatabaseCallback<List<User>> callback) {
        databaseReference.child(USERS_PATH).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<User> users = new ArrayList<>();
                for (DataSnapshot userSnap : snapshot.getChildren()) {
                    User u = userSnap.getValue(User.class);
                    if (u != null) users.add(u);
                }
                if (callback != null) callback.onCompleted(users);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) callback.onFailed(error.toException());
            }
        });
    }

    public void getAllEventsGlobally(final DatabaseCallback<List<Event>> callback) {
        databaseReference.child(EVENTS_PATH).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Event> events = new ArrayList<>();
                for (DataSnapshot eventSnap : snapshot.getChildren()) {
                    Event e = eventSnap.getValue(Event.class);
                    if (e != null) events.add(e);
                }
                if (callback != null) callback.onCompleted(events);
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (callback != null) callback.onFailed(error.toException());
            }
        });
    }

    public void deleteUserFromDB(String userId, final DatabaseCallback<Void> callback) {
        databaseReference.child(USERS_PATH).child(userId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (callback != null) callback.onCompleted(null);
            } else {
                if (callback != null) callback.onFailed(task.getException());
            }
        });
    }

    public void deleteEventGlobally(String eventId, final DatabaseCallback<Void> callback) {
        databaseReference.child(EVENTS_PATH).child(eventId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (callback != null) callback.onCompleted(null);
            } else {
                if (callback != null) callback.onFailed(task.getException());
            }
        });
    }
}