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

    // אני משתמש פה בתבנית עיצוב שנקראת Singleton.
    // המטרה היא שיהיה רק מופע אחד (Instance) של השירות הזה בכל האפליקציה כדי לא להעמיס חיבורים לפיירבייס.
    private static DatabaseService instance;
    private final DatabaseReference databaseReference;

    // הגדרתי קבועים לשמות של "הטבלאות" (צמתים) במסד הנתונים כדי למנוע שגיאות כתיב בהמשך
    private static final String USERS_PATH = "users";
    private static final String EVENTS_PATH = "events";
    private static final String NOTIFICATIONS_PATH = "notifications";

    // הבנאי פה הוא private! אי אפשר ליצור אובייקט חדש מבחוץ באמצעות 'new'.
    // הדרך היחידה לקבל אותו היא דרך הפונקציה getInstance.
    private DatabaseService() {
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        databaseReference = database.getReference();
    }

    // זו הפונקציה שמחזירה את המופע היחיד של השירות. המילה synchronized מונעת התנגשויות
    // אם כמה תהליכים מנסים לגשת לזה בדיוק באותה המאית שנייה.
    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    // יצרתי ממשק (Interface) פנימי משלי בשם DatabaseCallback.
    // למה? כי בקשות לרשת/פיירבייס קורות "ברקע" (אסינכרוניות) ולוקחות זמן,
    // אי אפשר פשוט לעשות 'return', צריך להפעיל פונקציה כשהמידע מגיע (onCompleted) או כשנופלת שגיאה (onFailed).
    public interface DatabaseCallback<T> {
        void onCompleted(T object);
        void onFailed(Exception e);
    }

    // פונקציית עזר כללית שכתבתי כדי לחסוך קוד כפול. כל פעם שאני רוצה לכתוב מידע לדאטה-בייס,
    // אני פשוט שולח לה את הנתיב והמידע, והיא עושה את העבודה מול הפיירבייס.
    private void writeData(@NotNull final String path, @NotNull final Object data, @Nullable final DatabaseCallback<Void> callback) {
        databaseReference.child(path).setValue(data).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (callback != null) callback.onCompleted(null);
            } else {
                if (callback != null) callback.onFailed(task.getException());
            }
        });
    }

    // ------------------- אזור המשתמשים (User Section) ------------------- //

    // שומר משתמש חדש במסד הנתונים תחת הנתיב "users/USER_ID"
    public void saveUser(@NotNull final User user, @Nullable final DatabaseCallback<Void> callback) {
        writeData(USERS_PATH + "/" + user.getId(), user, callback);
    }

    // שולף משתמש קיים לפי ה-ID שלו
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

    // פונקציה לעדכון משתמש - בפועל היא פשוט דורסת את המידע הישן עם החדש (כמו Save)
    public void updateUser(@NotNull final User user, @Nullable final DatabaseCallback<Void> callback) {
        writeData(USERS_PATH + "/" + user.getId(), user, callback);
    }

    // ------------------- אזור הפגישות (Event Section) ------------------- //

    // שמירת פגישה חדשה. הוספתי פה לוגיקה חכמה:
    // אם לפגישה עדיין אין ID, אני מבקש מהפיירבייס לייצר לה ID ייחודי אוטומטית בעזרת ()push().getKey
    public void saveEvent(@NotNull final Event event, @Nullable final DatabaseCallback<Void> callback) {
        if (event.getId() == null || event.getId().isEmpty()) {
            String key = databaseReference.child(EVENTS_PATH).push().getKey();
            event.setId(key); // מעדכן את האובייקט עם ה-ID שנוצר
        }
        writeData(EVENTS_PATH + "/" + event.getId(), event, callback);
    }

    // שולף פגישה בודדת ספציפית לפי ה-ID שלה (למשל כשנכנסים לערוך אותה)
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

    // עדכון פגישה (כמו בעדכון משתמש, פשוט דורס את הישן בחדש)
    public void updateEvent(@NotNull final Event event, @Nullable final DatabaseCallback<Void> callback) {
        writeData(EVENTS_PATH + "/" + event.getId(), event, callback);
    }

    // הפונקציה ששואבת את רשימת הפגישות המאושרות של המשתמש.
    // אני מביא את כל הפגישות, רץ עליהן, ומסנן רק את אלו שה-ID של המשתמש שלי נמצא ברשימת המשתתפים (ParticipantIds).
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

    // ------------------- אזור התראות ופח אשפה (Notifications & Trash) ------------------- //

    // מביא התראות (פגישות שאני מוזמן אליהן אבל עוד לא אישרתי).
    // הסינון פה בודק: אני ברשימת המוזמנים (Invited) -> וגם אני *לא* ברשימת פח האשפה (Trashed).
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

    // שולף רק התראות שהמשתמש העביר לפח האשפה (מסנן לפי הרשימה TrashedParticipantIds).
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

    // הלוגיקה של מענה להזמנה - משתמש אישר או דחה.
    public void respondToInvitation(@NotNull final String eventId, @NotNull final String userId, boolean isAccepted, @Nullable final DatabaseCallback<Void> callback) {
        getEvent(eventId, new DatabaseCallback<Event>() {
            @Override
            public void onCompleted(Event event) {
                if (event != null) {
                    // קודם כל, אני מוריד את המשתמש מרשימת ה"ממתינים לתשובה" (Invited)
                    if (event.getInvitedParticipantIds() != null) {
                        event.getInvitedParticipantIds().remove(userId);
                    }
                    // אם הוא אישר (Accepted), אני מכניס אותו לרשימת המשתתפים בפועל (ParticipantIds)
                    if (isAccepted) {
                        if (event.getParticipantIds() == null) {
                            event.setParticipantIds(new ArrayList<>());
                        }
                        if (!event.getParticipantIds().contains(userId)) {
                            event.getParticipantIds().add(userId);
                        }
                    }
                    // שומר את העדכונים בדאטה-בייס
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

    // העברת התראה לפח - מכניס את ה-ID של המשתמש לרשימת ה-Trashed של הפגישה
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

    // שחזור מפח האשפה - מוציא את המשתמש מרשימת ה-Trashed חזרה להתראות הרגילות
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

    // מחיקה לתמיד - מסיר את המשתמש לגמרי מכל הקשר לפגישה (הן ממוזמנים והן מהפח)
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

    // ------------------- אזור מנהלים (Admin Actions) ------------------- //
    // כאן יצרתי פונקציות שמיועדות אך ורק למסך האדמין (מושכות/מוחקות נתונים באופן גלובלי)

    // שואבת את כל המשתמשים שיש באפליקציה בלי שום סינון
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

    // שואבת את כל הפגישות במערכת (של כל המשתמשים)
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

    // מחיקת משתמש מהמסד לפי ה-ID שלו
    public void deleteUserFromDB(String userId, final DatabaseCallback<Void> callback) {
        databaseReference.child(USERS_PATH).child(userId).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                if (callback != null) callback.onCompleted(null);
            } else {
                if (callback != null) callback.onFailed(task.getException());
            }
        });
    }

    // מחיקת פגישה לחלוטין ממסד הנתונים
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