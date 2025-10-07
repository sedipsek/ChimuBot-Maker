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
import com.chimubot.maker.core.state.ReplyHandleCache
import com.chimubot.maker.core.state.ReplySendTelemetry
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class NotificationLogActivity : ComponentActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: View
    private lateinit var adapter: NotificationLogAdapter
    private var collectJob: Job? = null
    private var telemetryJob: Job? = null
    private var handleMetricsJob: Job? = null
    private lateinit var successView: TextView
    private lateinit var failureView: TextView
    private lateinit var retryView: TextView
    private lateinit var lastErrorView: TextView
    private lateinit var handleSummaryView: TextView
    private val timestampFormatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_log)

        findViewById<TextView>(R.id.notification_log_toolbar_title).text =
            getString(R.string.notification_log_title)
        recyclerView = findViewById(R.id.notification_log_list)
        emptyView = findViewById(R.id.notification_log_empty)
        adapter = NotificationLogAdapter()
        successView = findViewById(R.id.notification_log_metric_success_value)
        failureView = findViewById(R.id.notification_log_metric_failure_value)
        retryView = findViewById(R.id.notification_log_metric_retry_value)
        lastErrorView = findViewById(R.id.notification_log_metric_last_error)
        handleSummaryView = findViewById(R.id.notification_log_metric_handle_summary)

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
        telemetryJob = lifecycleScope.launch {
            ReplySendTelemetry.metrics.collectLatest { metrics ->
                successView.text = metrics.successCount.toString()
                failureView.text = metrics.failureCount.toString()
                retryView.text = metrics.retryCount.toString()
                val lastError = metrics.lastErrorMessage
                lastErrorView.text = if (lastError.isNullOrBlank()) {
                    getString(R.string.notification_log_metric_last_error_none)
                } else {
                    getString(R.string.notification_log_metric_last_error, lastError)
                }
            }
        }
        handleMetricsJob = lifecycleScope.launch {
            ReplyHandleCache.metrics.collectLatest { metrics ->
                val formatted = timestampFormatter.format(Date(metrics.lastUpdatedAt))
                handleSummaryView.text = getString(
                    R.string.notification_log_metric_handles_template,
                    metrics.activeCount,
                    formatted
                )
            }
        }
    }

    override fun onStop() {
        collectJob?.cancel()
        collectJob = null
        telemetryJob?.cancel()
        telemetryJob = null
        handleMetricsJob?.cancel()
        handleMetricsJob = null
        super.onStop()
    }
}
