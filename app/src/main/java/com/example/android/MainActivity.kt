package com.example.android

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Connect activity_main.xml to this Activity
        setContentView(R.layout.active_welcome_page)

        // Get views from XML
        val getStartedButton = findViewById<Button>(R.id.btnGetStarted)
        val signInText = findViewById<TextView>(R.id.tvSignIn)

        // Get Started button
        getStartedButton.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }

        // Sign In
        signInText.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }
}