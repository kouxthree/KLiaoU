package com.kliaou.ui

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.kliaou.R
import com.kliaou.parts.RecyclerAdapter
import kotlinx.android.synthetic.main.activity_bind.*

class BindActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bind)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val uuid = intent.getStringExtra(RecyclerAdapter.ITEM_TO_BE_BOUND)
        txt_uuid.text = uuid.toString()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.getItemId() == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        return true
    }
}