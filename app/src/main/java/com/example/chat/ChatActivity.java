package com.example.chat;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    public RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private List<ChatData> chatList;
    private String nick;

    private EditText EditText_chat;
    private Button Button_send;
    private DatabaseReference myRef;
    public static final String NOTIFICATION_CHANNEL_ID = "10001";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        String android_id = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);
        nick = android_id;

        Button_send = findViewById(R.id.Button_send);
        EditText_chat = findViewById(R.id.EditText_chat);

        Button_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = EditText_chat.getText().toString(); //msg
                if (msg != null) {
                    ChatData chat = new ChatData();
                    chat.setNickname(nick);
                    chat.setMsg(msg);
                    chat.setFcmToken(FirebaseInstanceId.getInstance().getToken());
                    myRef.push().setValue(chat);
                }
                EditText_chat.setText("");
            }
        });


        mRecyclerView = findViewById(R.id.my_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(mLayoutManager);

        chatList = new ArrayList<>();
        mAdapter = new ChatAdapter(chatList, ChatActivity.this, nick);

        DBHelper helper = new DBHelper(this);
        SQLiteDatabase db = helper.getWritableDatabase();
        Cursor cursor = db.rawQuery("select * from tb_memo", null);
        if(cursor.getCount()>0){
            while (cursor.moveToNext()){
                ChatData data = new ChatData();
                data.setNickname(cursor.getString(cursor.getColumnIndex("nickname")));
                data.setMsg(cursor.getString(cursor.getColumnIndex("msg")));
                ((ChatAdapter) mAdapter).addChat(data);
            }
        }

        mRecyclerView.setAdapter(mAdapter);

        // Write a message to the database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        myRef = database.getReference();


        //caution!!!

        myRef.addChildEventListener(new ChildEventListener() {
            DBHelper helper = new DBHelper(getApplicationContext());
            SQLiteDatabase db = helper.getWritableDatabase();
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

                Log.d("LISTENER", "리스너 접속!!");
                removeNotification();
                ChatData chat = dataSnapshot.getValue(ChatData.class);
                Log.d("CHAT",dataSnapshot.getValue().toString());
                db.execSQL("insert into tb_memo(nickname, msg) values (?,?)",
                        new String[]{chat.getNickname(),chat.getMsg()});
                ((ChatAdapter) mAdapter).addChat(chat);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        //1. recyclerView - 반복
        //2. 디비 내용을 넣는다
        //3. 상대방폰에 채팅 내용이 보임 - get

        //1-1. recyclerview - chat data
        //1. message, nickname - Data Transfer Object

    }

    @Override
    public void onStop() {
        super.onStop();
//        Toast.makeText(getApplicationContext(), "Service 시작", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, ChatService.class);
        startService(intent);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onRestart() {
//        Toast.makeText(getApplicationContext(), "Service 시작", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(this, ChatService.class);
        stopService(intent);
        super.onRestart();
    }

    private void removeNotification() {
// Notification 제거
        NotificationManagerCompat.from(this).cancel(1234);
    }

}
