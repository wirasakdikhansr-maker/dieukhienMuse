package com.videodl.app

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.json.JSONArray
import org.json.JSONObject

enum class Status { QUEUED, RUNNING, DONE, ERROR, CANCELED }
enum class EngineState { LOADING, READY, FAILED }

data class DlTask(
    val id: String,
    val url: String,
    val title: String,
    val thumb: String?,
    val quality: String,
    val status: Status = Status.QUEUED,
    val progress: Float = 0f,
    val eta: Long = 0,
    val message: String = "",
    val uri: String? = null,
    val time: Long = System.currentTimeMillis()
)

val QUALITIES = listOf(
    "best" to "Tốt nhất",
    "1080" to "1080p",
    "720" to "720p",
    "480" to "480p",
    "audio" to "🎵 MP3"
)

fun qualityLabel(q: String) = QUALITIES.firstOrNull { it.first == q }?.second ?: q

fun extractUrl(text: String): String? = Regex("https?://\\S+").find(text)?.value

fun shortError(e: Throwable): String {
    val raw = e.message ?: e.toString()
    val lines = raw.lines().map { it.trim() }.filter { it.isNotBlank() }
    val line = lines.lastOrNull { it.startsWith("ERROR") } ?: lines.lastOrNull() ?: raw
    return line.removePrefix("ERROR:").trim().take(220)
}

object Hub {
    val tasks = MutableStateFlow<List<DlTask>>(emptyList())
    val engine = MutableStateFlow(EngineState.LOADING)
    @Volatile var engineError: String? = null
    private lateinit var prefs: SharedPreferences

    fun init(ctx: Context) {
        prefs = ctx.getSharedPreferences("hub", Context.MODE_PRIVATE)
        val list = mutableListOf<DlTask>()
        try {
            val arr = JSONArray(prefs.getString("tasks", "[]") ?: "[]")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                var st = Status.valueOf(o.getString("status"))
                var msg = o.optString("message")
                if (st == Status.RUNNING || st == Status.QUEUED) {
                    st = Status.ERROR
                    msg = "Bị gián đoạn, bấm Thử lại"
                }
                list.add(
                    DlTask(
                        id = o.getString("id"),
                        url = o.getString("url"),
                        title = o.getString("title"),
                        thumb = if (o.isNull("thumb")) null else o.optString("thumb"),
                        quality = o.optString("quality", "best"),
                        status = st,
                        message = msg,
                        uri = if (o.isNull("uri")) null else o.optString("uri"),
                        time = o.optLong("time")
                    )
                )
            }
        } catch (_: Exception) {
        }
        tasks.value = list
    }

    private fun save() {
        val arr = JSONArray()
        tasks.value.forEach { t ->
            arr.put(
                JSONObject()
                    .put("id", t.id).put("url", t.url).put("title", t.title)
                    .put("thumb", t.thumb ?: JSONObject.NULL)
                    .put("quality", t.quality).put("status", t.status.name)
                    .put("message", t.message)
                    .put("uri", t.uri ?: JSONObject.NULL)
                    .put("time", t.time)
            )
        }
        prefs.edit().putString("tasks", arr.toString()).apply()
    }

    fun get(id: String): DlTask? = tasks.value.firstOrNull { it.id == id }

    fun add(t: DlTask) {
        tasks.update { listOf(t) + it }
        save()
    }

    fun update(id: String, f: (DlTask) -> DlTask) {
        val before = get(id)?.status
        tasks.update { l -> l.map { if (it.id == id) f(it) else it } }
        if (before != get(id)?.status) save()
    }

    fun remove(id: String) {
        tasks.update { l -> l.filter { it.id != id } }
        save()
    }

    fun delete(ctx: Context, t: DlTask) {
        t.uri?.let {
            try {
                ctx.contentResolver.delete(Uri.parse(it), null, null)
            } catch (_: Exception) {
            }
        }
        remove(t.id)
    }

    fun cancel(id: String) {
        update(id) {
            if (it.status == Status.QUEUED || it.status == Status.RUNNING)
                it.copy(status = Status.CANCELED, message = "") else it
        }
        Thread {
            try {
                YoutubeDL.getInstance().destroyProcessById(id)
            } catch (_: Throwable) {
            }
        }.start()
    }

    fun awaitEngine() {
        var waited = 0
        while (engine.value == EngineState.LOADING && waited < 120_000) {
            Thread.sleep(300)
            waited += 300
        }
        if (engine.value != EngineState.READY)
            throw IllegalStateException("Không khởi tạo được bộ máy tải: ${engineError ?: "hết thời gian chờ"}")
    }
}
