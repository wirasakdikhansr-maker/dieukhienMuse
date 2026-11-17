package com.videodl.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class Preview(
    val url: String,
    val title: String,
    val thumb: String?,
    val uploader: String?,
    val duration: Long
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    var url by mutableStateOf("")
    var loading by mutableStateOf(false)
    var preview by mutableStateOf<Preview?>(null)
    var error by mutableStateOf<String?>(null)
    var quality by mutableStateOf("best")
    var tab by mutableIntStateOf(0)
    val events = MutableSharedFlow<String>(extraBufferCapacity = 4)

    fun onSharedLink(u: String) {
        url = u
        tab = 0
        analyze()
    }

    fun analyze() {
        val u = extractUrl(url)
        if (u == null) {
            error = "Hãy dán một đường link hợp lệ (bắt đầu bằng http…)"
            return
        }
        url = u
        loading = true
        error = null
        preview = null
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    Hub.awaitEngine()
                    val req = YoutubeDLRequest(u)
                    req.addOption("--no-playlist")
                    val info = YoutubeDL.getInstance().getInfo(req)
                    Result.success(
                        Preview(
                            url = u,
                            title = info.title ?: u,
                            thumb = info.thumbnail,
                            uploader = info.uploader,
                            duration = (info.duration ?: 0).toLong()
                        )
                    )
                } catch (e: Throwable) {
                    Result.failure(e)
                }
            }
            result.onSuccess { preview = it }
            result.onFailure { error = shortError(it) }
            loading = false
        }
    }

    fun download() {
        val p = preview ?: return
        enqueue(p.url, p.title, p.thumb)
    }

    fun downloadDirect() {
        val u = extractUrl(url) ?: return
        enqueue(u, u, null)
    }

    fun retry(id: String) {
        Hub.update(id) { it.copy(status = Status.QUEUED, progress = 0f, message = "") }
        DownloadService.start(getApplication(), id)
    }

    private fun enqueue(u: String, title: String, thumb: String?) {
        val t = DlTask(
            id = UUID.randomUUID().toString(),
            url = u, title = title, thumb = thumb, quality = quality
        )
        Hub.add(t)
        DownloadService.start(getApplication(), t.id)
        preview = null
        error = null
        url = ""
        tab = 1
        events.tryEmit("Đã thêm vào hàng chờ tải ✅")
    }
}
