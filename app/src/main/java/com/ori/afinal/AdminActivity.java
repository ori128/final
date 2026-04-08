package com.ori.afinal;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.adapter.AdminEventAdapter;
import com.ori.afinal.adapter.AdminUserAdapter;
import com.ori.afinal.model.Event;
import com.ori.afinal.model.User;

import java.util.ArrayList;
import java.util.List;

public class AdminActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private MaterialButtonToggleGroup toggleTabs;
    private SearchView svAdmin;
    private RecyclerView rvAdminList;
    private View llEmptyState;
    private ProgressBar pbLoading;
    private TextView tvEmptyText;

    private DatabaseService databaseService;
    private FirebaseAuth mAuth;

    private AdminUserAdapter userAdapter;
    private AdminEventAdapter eventAdapter;

    private List<User> fullUserList = new ArrayList<>();
    private List<Event> fullEventList = new ArrayList<>();

    private boolean isShowingUsers = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rl_header), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        databaseService = DatabaseService.getInstance();
        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupAdapters();
        setupListeners();

        loadUsers();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btn_back_admin);
        toggleTabs = findViewById(R.id.toggle_admin_tabs);
        svAdmin = findViewById(R.id.sv_admin);
        rvAdminList = findViewById(R.id.rv_admin_list);
        llEmptyState = findViewById(R.id.ll_admin_empty);
        pbLoading = findViewById(R.id.pb_admin_loading);
        tvEmptyText = findViewById(R.id.tv_admin_empty_text);

        rvAdminList.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupAdapters() {
        userAdapter = new AdminUserAdapter(new AdminUserAdapter.OnUserActionListener() {
            @Override
            public void onDeleteClick(User user) {
                showDeleteUserDialog(user);
            }

            @Override
            public void onMakeAdminClick(User user) {
                showMakeAdminDialog(user);
            }

            @Override
            public void onRemoveAdminClick(User user) {
                showRemoveAdminDialog(user);
            }

            @Override
            public void onEditClick(User user) {
                Intent intent = new Intent(AdminActivity.this, UpdateUser.class);
                intent.putExtra("USER_ID", user.getId());
                startActivity(intent);
            }
        });

        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail() != null) {
            userAdapter.setCurrentUserEmail(mAuth.getCurrentUser().getEmail());
        }

        eventAdapter = new AdminEventAdapter(new AdminEventAdapter.OnEventActionListener() {
            @Override
            public void onDeleteClick(Event event) {
                showDeleteEventDialog(event);
            }

            @Override
            public void onEditClick(Event event) {
                // הקישור למסך עריכת פגישה!
                Intent intent = new Intent(AdminActivity.this, UpdateEvent.class);
                intent.putExtra("EVENT_ID", event.getId());
                startActivity(intent);
            }
        });
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        toggleTabs.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (isChecked) {
                if (checkedId == R.id.btn_tab_users) {
                    isShowingUsers = true;
                    svAdmin.setQueryHint("חפש משתמשים...");
                    loadUsers();
                } else if (checkedId == R.id.btn_tab_events) {
                    isShowingUsers = false;
                    svAdmin.setQueryHint("חפש פגישות...");
                    loadEvents();
                }
                svAdmin.setQuery("", false);
            }
        });

        svAdmin.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterData(newText);
                return true;
            }
        });
    }

    private void loadUsers() {
        showLoading("טוען משתמשים...");
        rvAdminList.setAdapter(userAdapter);

        databaseService.getAllUsers(new DatabaseService.DatabaseCallback<List<User>>() {
            @Override
            public void onCompleted(List<User> users) {
                fullUserList.clear();
                if (users != null) fullUserList.addAll(users);
                hideLoading();
                filterData(svAdmin.getQuery().toString());
            }

            @Override
            public void onFailed(Exception e) {
                hideLoading();
                Toast.makeText(AdminActivity.this, "שגיאה בטעינת משתמשים", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadEvents() {
        showLoading("טוען פגישות...");
        rvAdminList.setAdapter(eventAdapter);

        databaseService.getAllEventsGlobally(new DatabaseService.DatabaseCallback<List<Event>>() {
            @Override
            public void onCompleted(List<Event> events) {
                fullEventList.clear();
                if (events != null) fullEventList.addAll(events);
                hideLoading();
                filterData(svAdmin.getQuery().toString());
            }

            @Override
            public void onFailed(Exception e) {
                hideLoading();
                Toast.makeText(AdminActivity.this, "שגיאה בטעינת פגישות", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterData(String query) {
        String lowerQuery = query.toLowerCase();

        if (isShowingUsers) {
            List<User> filteredUsers = new ArrayList<>();
            for (User u : fullUserList) {
                String name = (u.getFname() + " " + u.getLname());
                String email = u.getEmail() != null ? u.getEmail() : "";
                if (name.toLowerCase().contains(lowerQuery) || email.toLowerCase().contains(lowerQuery)) {
                    filteredUsers.add(u);
                }
            }
            userAdapter.setUsers(filteredUsers);
            checkEmptyState(filteredUsers.isEmpty(), "לא נמצאו משתמשים");
        } else {
            List<Event> filteredEvents = new ArrayList<>();
            for (Event e : fullEventList) {
                String title = e.getTitle() != null ? e.getTitle() : "";
                if (title.toLowerCase().contains(lowerQuery)) {
                    filteredEvents.add(e);
                }
            }
            eventAdapter.setEvents(filteredEvents);
            checkEmptyState(filteredEvents.isEmpty(), "לא נמצאו פגישות");
        }
    }

    private void showDeleteUserDialog(User user) {
        new AlertDialog.Builder(this)
                .setTitle("מחיקת משתמש")
                .setMessage("האם אתה בטוח שברצונך למחוק את המשתמש: " + user.getEmail() + " ממסד הנתונים?")
                .setPositiveButton("כן, מחק", (dialog, which) -> {
                    databaseService.deleteUserFromDB(user.getId(), new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void object) {
                            Toast.makeText(AdminActivity.this, "המשתמש נמחק", Toast.LENGTH_SHORT).show();
                            loadUsers();
                        }
                        @Override
                        public void onFailed(Exception e) {
                            Toast.makeText(AdminActivity.this, "שגיאה", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void showMakeAdminDialog(User user) {
        new AlertDialog.Builder(this)
                .setTitle("הגדרת מנהל")
                .setMessage("האם להפוך את המשתמש: " + user.getFname() + " למנהל?")
                .setPositiveButton("כן", (dialog, which) -> {
                    user.setAdmin(true);
                    databaseService.updateUser(user, new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void object) {
                            Toast.makeText(AdminActivity.this, "מנהל עודכן", Toast.LENGTH_SHORT).show();
                            loadUsers();
                        }
                        @Override public void onFailed(Exception e) {}
                    });
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void showRemoveAdminDialog(User user) {
        new AlertDialog.Builder(this)
                .setTitle("הסרת מנהל")
                .setMessage("להחזיר את " + user.getFname() + " למשתמש רגיל?")
                .setPositiveButton("כן", (dialog, which) -> {
                    user.setAdmin(false);
                    databaseService.updateUser(user, new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void object) {
                            Toast.makeText(AdminActivity.this, "הוסר ניהול", Toast.LENGTH_SHORT).show();
                            loadUsers();
                        }
                        @Override public void onFailed(Exception e) {}
                    });
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void showDeleteEventDialog(Event event) {
        new AlertDialog.Builder(this)
                .setTitle("מחיקה גלובלית")
                .setMessage("למחוק את " + event.getTitle() + " מכל המערכת?")
                .setPositiveButton("מחק", (dialog, which) -> {
                    databaseService.deleteEventGlobally(event.getId(), new DatabaseService.DatabaseCallback<Void>() {
                        @Override
                        public void onCompleted(Void object) {
                            Toast.makeText(AdminActivity.this, "נמחק", Toast.LENGTH_SHORT).show();
                            loadEvents();
                        }
                        @Override public void onFailed(Exception e) {}
                    });
                })
                .setNegativeButton("ביטול", null)
                .show();
    }

    private void showLoading(String msg) {
        llEmptyState.setVisibility(View.VISIBLE);
        rvAdminList.setVisibility(View.GONE);
        pbLoading.setVisibility(View.VISIBLE);
        tvEmptyText.setText(msg);
    }

    private void hideLoading() {
        llEmptyState.setVisibility(View.GONE);
        rvAdminList.setVisibility(View.VISIBLE);
    }

    private void checkEmptyState(boolean isEmpty, String emptyMsg) {
        if (isEmpty) {
            rvAdminList.setVisibility(View.GONE);
            llEmptyState.setVisibility(View.VISIBLE);
            pbLoading.setVisibility(View.GONE);
            tvEmptyText.setText(emptyMsg);
        } else {
            rvAdminList.setVisibility(View.VISIBLE);
            llEmptyState.setVisibility(View.GONE);
        }
    }
}