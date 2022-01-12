package com.example.ar_project01.driodAR

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import com.example.ar_project01.R

class ScoreboardView(context: Context, attrs: AttributeSet? = null, defStyle: Int = -1)
  : FrameLayout(context, attrs, defStyle) {

  init {
    inflate(context, R.layout.scoreboard_view, this)

    findViewById<Button>(R.id.start_btn).setOnClickListener {
      it.isEnabled = false
      onStartTapped?.invoke()
    }
  }

  var onStartTapped: (() -> Unit)? = null
  var score: Int = 0
    set(value) {
      field = value
      findViewById<TextView>(R.id.score_counter).text = value.toString()
    }

  var life: Int = 0
    set(value) {
      if (field == 0 && value > field) {
        // Game has been restarted, hide game over message
        findViewById<TextView>(R.id.gameover).visibility = GONE
      }
      field = value
      findViewById<TextView>(R.id.life_counter).text = value.toString()

      // If player has 0 lives, show a game over message,
      // re enable start btn and change it's mesasge
      if (value <= 0) {
        findViewById<TextView>(R.id.gameover).visibility = View.VISIBLE
        findViewById<Button>(R.id.start_btn).isEnabled = true
        findViewById<Button>(R.id.start_btn).setText(R.string.restart)
      }
    }
}
