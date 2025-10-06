package com.chimubot.maker.app.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.chimubot.maker.app.R
import com.chimubot.maker.core.notif.NotificationLogRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NotificationLogActivity : ComponentActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var adapter: NotificationLogAdapter
    private var collectJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_log)

        findViewById<TextView>(R.id.notification_log_toolbar_title).text =
            getString(R.string.notification_log_title)
        recyclerView = findViewById(R.id.notification_log_list)
        emptyView = findViewById(R.id.notification_log_empty)
        adapter = NotificationLogAdapter()

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        collectJob = lifecycleScope.launch {
            NotificationLogRepository.items.collectLatest { items ->
                adapter.submitList(items)
                emptyView.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onStop() {
        collectJob?.cancel()
        collectJob = null
        super.onStop()
    }
}
