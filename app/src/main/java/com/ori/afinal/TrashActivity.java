package com.ori.afinal;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.ori.afinal.Services.DatabaseService;
import com.ori.afinal.adapter.NotificationAdapter;
import com.ori.afinal.model.Event;
import java.util.ArrayList;
import java.util.List;

public class TrashActivity extends AppCompatActivity {

    private RecyclerView rvTrash;
    private View llEmptyTrash;
    private SearchView svTrash;
    private ImageButton btnBack;

    private NotificationAdapter adapter;
    private DatabaseService databaseService;
    private String currentUserId;
    private List<Event> fullTrashList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_trash);

        databaseService = DatabaseService.getInstance();
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            finish();
            return;
        }

        rvTrash = findViewById(R.id.rv_trash);
        llEmptyTrash = findViewById(R.id.ll_empty_trash);
        svTrash = findViewById(R.id.sv_trash);
        btnBack = findViewById(R.id.btn_back_from_trash);

        // התיקון לפנדה! מונע את הקריסה של הפונט
        com.airbnb.lottie.LottieAnimationView lottiePandaTrash = findViewById(R.id.lottie_panda_trash);
        if (lottiePandaTrash != null) {
            lottiePandaTrash.setFontAssetDelegate(new com.airbnb.lottie.FontAssetDelegate() {
                @Override
                public android.graphics.Typeface fetchFont(String fontFamily) {
                    return android.graphics.Typeface.DEFAULT_BOLD;
                }
            });
        }

        rvTrash.setLayoutManager(new LinearLayoutManager(this));
        // חשוב: isTrashMode = true
        adapter = new NotificationAdapter(currentUserId, true);
        rvTrash.setAdapter(adapter);

        // מאזין כדי להציג את הפנדה באופן חי אם מרוקנים את הפח
        adapter.setOnNotificationChangedListener(new NotificationAdapter.OnNotificationChangedListener() {
            @Override
            public void onNotificationCountChanged(int newCount) {
                if (newCount == 0) {
                    rvTrash.setVisibility(View.GONE);
                    llEmptyTrash.setVisibility(View.VISIBLE);
                }
            }
        });

        btnBack.setOnClickListener(v -> finish());

        svTrash.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }
            @Override
            public boolean onQueryTextChange(String newText) {
                filterTrash(newText);
                return true;
            }
        });

        loadTrash();
    }

    private void loadTrash() {
        databaseService.getUserTrashedNotifications(currentUserId, new DatabaseService.DatabaseCallback<List<Event>>() {
            @Override
            public void onCompleted(List<Event> trashedEvents) {
                if (isFinishing() || isDestroyed()) return;

                fullTrashList.clear();
                if (trashedEvents != null) {
                    fullTrashList.addAll(trashedEvents);
                }
                filterTrash(svTrash.getQuery().toString());
            }

            @Override
            public void onFailed(Exception e) {
                llEmptyTrash.setVisibility(View.VISIBLE);
            }
        });
    }

    private void filterTrash(String text) {
        List<Event> filtered = new ArrayList<>();
        for (Event e : fullTrashList) {
            if (text.isEmpty() || (e.getTitle() != null && e.getTitle().toLowerCase().contains(text.toLowerCase()))) {
                filtered.add(e);
            }
        }

        if (filtered.isEmpty()) {
            rvTrash.setVisibility(View.GONE);
            llEmptyTrash.setVisibility(View.VISIBLE);
        } else {
            rvTrash.setVisibility(View.VISIBLE);
            llEmptyTrash.setVisibility(View.GONE);
        }

        adapter.setEvents(filtered);
    }
}