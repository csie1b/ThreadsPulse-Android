package io.github.csie1b.threadspulse.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.csie1b.threadspulse.data.UrlCleaner

class ShareReceiverActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            val cleanedUrl = UrlCleaner.cleanString(sharedText)

            val targetIntent = Intent(this, MainActivity::class.java).apply {
                putExtra("EXTRA_URL", cleanedUrl)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(targetIntent)
        }
        finish()
    }
}
