package com.example.datn;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class LoginTB extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login_tb);
        ImageView v1=findViewById(R.id.imageView);
        EditText eml,mk;
        TextView email2,pass2,forget,sign;
        Button log=findViewById(R.id.btnLogin);



        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

log.setOnClickListener(new View.OnClickListener() {
    @Override
    public void onClick(View v) {


        // Kiểm tra email và mật khẩu

        Intent m=new Intent(LoginTB.this,MainActivity.class);
        startActivity(m);
    }
});
    }
}