package me.rjy.android.media.center.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * URL历史记录管理器，使用SharedPreferences存储播放过的URL
 * 支持添加、获取、清除历史记录，自动去重
 */
class URLHistoryManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "url_history_prefs"
        private const val KEY_HISTORY = "url_history"
        private const val MAX_HISTORY_SIZE = 20 // 最大历史记录数量
        private const val SEPARATOR = ";;;" // 历史记录分隔符，使用不常见的字符串
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 添加URL到历史记录（自动去重，最新的在最前面）
     */
    suspend fun addUrl(url: String) = withContext(Dispatchers.IO) {
        val history = getHistory()
        // 去重：移除已存在的相同URL
        val newHistory = mutableListOf<String>().apply {
            add(url) // 新URL添加到最前面
            addAll(history.filter { it != url })
        }
        // 限制最大数量
        val limitedHistory = newHistory.take(MAX_HISTORY_SIZE)
        saveHistory(limitedHistory)
    }

    /**
     * 获取所有历史记录（最新的在前面）
     */
    suspend fun getHistory(): List<String> = withContext(Dispatchers.IO) {
        val historyString = prefs.getString(KEY_HISTORY, "") ?: ""
        if (historyString.isEmpty()) {
            emptyList()
        } else {
            // 使用分隔符分割，过滤空字符串
            historyString.split(SEPARATOR).filter { it.isNotBlank() }
        }
    }

    /**
     * 清除所有历史记录
     */
    suspend fun clearHistory() = withContext(Dispatchers.IO) {
        prefs.edit {
            remove(KEY_HISTORY)
        }
    }

    /**
     * 从历史记录中移除指定URL
     */
    suspend fun removeUrl(url: String) = withContext(Dispatchers.IO) {
        val history = getHistory()
        val newHistory = history.filter { it != url }
        saveHistory(newHistory)
    }

    /**
     * 保存历史记录到SharedPreferences
     */
    private fun saveHistory(history: List<String>) {
        val historyString = history.joinToString(SEPARATOR)
        prefs.edit {
            putString(KEY_HISTORY, historyString)
        }
    }

    /**
     * 检查URL是否已在历史记录中
     */
    suspend fun contains(url: String): Boolean = withContext(Dispatchers.IO) {
        getHistory().contains(url)
    }

    /**
     * 获取历史记录数量
     */
    suspend fun getHistoryCount(): Int = withContext(Dispatchers.IO) {
        getHistory().size
    }
}
