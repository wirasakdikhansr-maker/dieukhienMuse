package com.videodl.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.IBinder
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

class DownloadService : Service() {

    companion object {
        const val CH_RUN = "dl_run"
        const val CH_DONE = "dl_done"
        const val NOTIF_RUN = 1

        fun start(ctx: Context, id: String) {
            ContextCompat.startForegroundService(
                ctx, Intent(ctx, DownloadService::class.java).putExtra("id", id)
            )
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val active = AtomicInteger(0)
    private val sem = Semaphore(2)
    private var lastNotif = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CH_RUN, "Đang tải", NotificationManager.IMPORTANCE_LOW)
        )
        nm.createNotificationChannel(
            NotificationChannel(CH_DONE, "Tải xong", NotificationManager.IMPORTANCE_DEFAULT)
        )
        startForeground(
            NOTIF_RUN, runNotif("Đang chuẩn bị…", -1),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
        )
        scope.launch {
            Hub.tasks.collect { list ->
                val now = System.currentTimeMillis()
                if (now - lastNotif < 700) return@collect
                lastNotif = now
                val run = list.filter { it.status == Status.RUNNING }
                val q = list.count { it.status == Status.QUEUED }
                val text = if (run.isEmpty()) "Đang chờ… ($q)"
                else "Đang tải ${run.size} video" + if (q > 0) " • chờ $q" else ""
                val pct = if (run.isEmpty()) -1
                else (run.map { it.progress }.average() * 100).toInt()
                nm.notify(NOTIF_RUN, runNotif(text, pct))
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val id = intent?.getStringExtra("id")
        if (id == null) {
            if (active.get() == 0) stopSelf(startId)
            return START_NOT_STICKY
        }
        active.incrementAndGet()
        scope.launch {
            try {
                sem.withPermit { runTask(id) }
            } finally {
                if (active.decrementAndGet() == 0) stopSelf(startId)
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
    )

    private fun runNotif(text: String, pct: Int): Notification {
        val b = NotificationCompat.Builder(this, CH_RUN)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle("VideoDL")
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openAppIntent())
        if (pct >= 0) b.setProgress(100, pct, false) else b.setProgress(0, 0, true)
        return b.build()
    }

    private fun notifyDone(title: String, ok: Boolean) {
        val n = NotificationCompat.Builder(this, CH_DONE)
            .setSmallIcon(
                if (ok) android.R.drawable.stat_sys_download_done
                else android.R.drawable.stat_notify_error
            )
            .setContentTitle(if (ok) "Tải xong" else "Tải thất bại")
            .setContentText(title)
            .setAutoCancel(true)
            .setContentIntent(openAppIntent())
            .build()
        getSystemService(NotificationManager::class.java)
            .notify((System.nanoTime() and 0xFFFFFF).toInt() + 100, n)
    }

    private fun applyFormat(r: YoutubeDLRequest, q: String) {
        when (q) {
            "audio" -> {
                r.addOption("-x")
                r.addOption("--audio-format", "mp3")
                r.addOption("--audio-quality", "0")
            }
            "best" -> {
                r.addOption("-f", "bv*[ext=mp4]+ba[ext=m4a]/b[ext=mp4]/bv*+ba/b")
                r.addOption("--merge-output-format", "mp4")
            }
            else -> {
                val h = q
                r.addOption(
                    "-f",
                    "bv*[height<=$h][ext=mp4]+ba[ext=m4a]/b[height<=$h][ext=mp4]/" +
                        "bv*[height<=$h]+ba/b[height<=$h]/b"
                )
                r.addOption("--merge-output-format", "mp4")
            }
        }
    }

    private fun runTask(id: String) {
        val t = Hub.get(id) ?: return
        if (t.status != Status.QUEUED) return
        Hub.update(id) { it.copy(status = Status.RUNNING, progress = 0f, message = "Đang chuẩn bị…") }
        val tmp = File(getExternalFilesDir(null) ?: cacheDir, "tmp/$id").apply { mkdirs() }
        try {
            Hub.awaitEngine()
            val req = YoutubeDLRequest(t.url)
            req.addOption("--no-playlist")
            req.addOption("--no-mtime")
            req.addOption("-N", "4")
            req.addOption("-o", tmp.absolutePath + "/%(title).80s.%(ext)s")
            applyFormat(req, t.quality)

            YoutubeDL.getInstance().execute(req, id) { p, eta, _ ->
                Hub.update(id) {
                    if (it.status == Status.RUNNING)
                        it.copy(
                            progress = (p / 100f).coerceIn(0f, 1f),
                            eta = eta,
                            message = if (p >= 99.9f) "Đang xử lý…" else ""
                        )
                    else it
                }
            }

            if (Hub.get(id)?.status == Status.CANCELED) return
            val file = tmp.listFiles()
                ?.filter {
                    it.isFile && !it.name.endsWith(".part") &&
                        !it.name.endsWith(".ytdl") && !it.name.endsWith(".temp")
                }
                ?.maxByOrNull { it.length() }
                ?: throw IllegalStateException("Không tìm thấy file sau khi tải")

            Hub.update(id) { it.copy(message = "Đang lưu vào thư viện…") }
            val uri = saveToMedia(file)
            val title = if (t.title.startsWith("http")) file.nameWithoutExtension else t.title
            Hub.update(id) {
                it.copy(status = Status.DONE, progress = 1f, uri = uri.toString(), message = "", title = title)
            }
            notifyDone(title, true)
        } catch (e: Throwable) {
            if (Hub.get(id)?.status == Status.CANCELED) return
            Hub.update(id) { it.copy(status = Status.ERROR, message = shortError(e)) }
            notifyDone(t.title, false)
        } finally {
            tmp.deleteRecursively()
        }
    }

    private fun saveToMedia(file: File): Uri {
        val ext = file.extension.lowercase()
        val audio = ext in listOf("mp3", "m4a", "opus", "ogg", "aac", "wav", "flac")
        val mime = when (ext) {
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "webm" -> "video/webm"
            "mp3" -> "audio/mpeg"
            "m4a" -> "audio/mp4"
            "opus", "ogg" -> "audio/ogg"
            else -> if (audio) "audio/*" else "video/*"
        }
        val collection = if (audio)
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        else
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, file.name)
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, if (audio) "Music/VideoDL" else "Movies/VideoDL")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val resolver = contentResolver
        val uri = resolver.insert(collection, values)
            ?: throw IllegalStateException("Không ghi được vào bộ nhớ")
        resolver.openOutputStream(uri)!!.use { out ->
            file.inputStream().use { it.copyTo(out) }
        }
        values.clear()
        values.put(MediaStore.MediaColumns.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
        return uri
    }
}
