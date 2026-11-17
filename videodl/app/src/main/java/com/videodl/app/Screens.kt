package com.videodl.app

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun fmtDur(sec: Long): String {
    val h = sec / 3600
    val m = (sec % 3600) / 60
    val s = sec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

// ---------------------------------------------------------------- Home

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(vm: MainViewModel) {
    val engine by Hub.engine.collectAsStateWithLifecycle()
    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Header() }
        item { InputCard(vm, engine) }
        if (vm.loading) item {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(Modifier.size(22.dp), color = Acc2, strokeWidth = 2.5.dp)
                Spacer(Modifier.width(12.dp))
                Text("Đang đọc thông tin video…", color = TextDim)
            }
        }
        vm.error?.let { err ->
            item {
                GlassCard {
                    Text("⚠ Không đọc được video", color = Danger, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(err, color = TextDim, fontSize = 13.sp)
                    if (extractUrl(vm.url) != null) {
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(onClick = { vm.downloadDirect() }) { Text("Vẫn thử tải thẳng") }
                    }
                }
            }
        }
        vm.preview?.let { p -> item { PreviewCard(vm, p) } }
        if (vm.preview == null) {
            item {
                GlassCard {
                    Text("Hỗ trợ hàng nghìn trang web", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "YouTube", "TikTok", "Facebook", "Instagram", "X / Twitter",
                            "Douyin", "Bilibili", "Reddit", "Vimeo", "Twitch", "Pinterest",
                            "SoundCloud", "Dailymotion", "+ nhiều trang khác"
                        ).forEach {
                            Text(
                                it, fontSize = 12.sp, color = Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(Color(0x22FFFFFF))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
            item {
                GlassCard {
                    Text("💡 Mẹo nhanh", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Mở video trong TikTok, YouTube, Facebook… → bấm Chia sẻ → chọn VideoDL. " +
                            "App sẽ tự điền link và đọc thông tin video cho bạn.",
                        color = TextDim, fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun Header() {
    Row(Modifier.padding(top = 8.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(48.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Acc1, Acc2))),
            contentAlignment = Alignment.Center
        ) { Text("⬇", fontSize = 24.sp, color = Color.White) }
        Spacer(Modifier.width(12.dp))
        Column {
            Text("VideoDL", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            Text("Tải video mọi nền tảng, nhanh gọn", color = TextDim, fontSize = 13.sp)
        }
    }
}

@Composable
private fun InputCard(vm: MainViewModel, engine: EngineState) {
    val ctx = LocalContext.current
    GlassCard {
        OutlinedTextField(
            value = vm.url,
            onValueChange = { vm.url = it; vm.error = null },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Dán link video vào đây…", color = TextDim) },
            maxLines = 3,
            shape = RoundedCornerShape(14.dp),
            trailingIcon = {
                if (vm.url.isNotEmpty()) {
                    Text(
                        "✕", color = TextDim,
                        modifier = Modifier
                            .clickable { vm.url = ""; vm.preview = null; vm.error = null }
                            .padding(12.dp)
                    )
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { vm.analyze() }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Acc2,
                unfocusedBorderColor = Color(0x44FFFFFF),
                cursorColor = Acc2
            )
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = {
                    val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val t = cm.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString()
                    if (!t.isNullOrBlank()) {
                        vm.url = t.trim()
                        vm.analyze()
                    } else {
                        vm.events.tryEmit("Bộ nhớ tạm đang trống")
                    }
                },
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.height(50.dp)
            ) { Text("📋 Dán") }
            GradientButton(
                "🔍 Phân tích",
                Modifier.weight(1f),
                enabled = !vm.loading && vm.url.isNotBlank()
            ) { vm.analyze() }
        }
        when (engine) {
            EngineState.LOADING -> {
                Spacer(Modifier.height(10.dp))
                Text("⏳ Đang khởi tạo bộ máy tải (lần đầu hơi lâu)…", color = TextDim, fontSize = 12.sp)
            }
            EngineState.FAILED -> {
                Spacer(Modifier.height(10.dp))
                Text(
                    "⚠ Lỗi bộ máy tải: ${Hub.engineError ?: ""}",
                    color = Danger, fontSize = 12.sp
                )
            }
            else -> {}
        }
    }
}

@Composable
private fun PreviewCard(vm: MainViewModel, p: Preview) {
    GlassCard {
        Box(
            Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(14.dp)).background(Color(0x33FFFFFF))
        ) {
            AsyncImage(
                model = p.thumb, contentDescription = null,
                contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
            )
            if (p.duration > 0) {
                Text(
                    fmtDur(p.duration), color = Color.White, fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(8.dp)
                        .background(Color(0xAA000000), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(p.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        p.uploader?.let { Text(it, color = TextDim, fontSize = 13.sp) }
        Spacer(Modifier.height(14.dp))
        Text("Chọn chất lượng", color = TextDim, fontSize = 12.sp)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            QUALITIES.forEach { (k, label) ->
                FilterChip(
                    selected = vm.quality == k,
                    onClick = { vm.quality = k },
                    label = { Text(label) }
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        GradientButton("⬇  Tải xuống", Modifier.fillMaxWidth()) { vm.download() }
    }
}

// ---------------------------------------------------------------- Library

@Composable
fun LibraryScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val tasks by Hub.tasks.collectAsStateWithLifecycle()
    var del by remember { mutableStateOf<DlTask?>(null) }

    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Text(
            "Thư viện", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(start = 16.dp, top = 20.dp, end = 16.dp, bottom = 4.dp)
        )
        if (tasks.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📥", fontSize = 56.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("Chưa có video nào", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Video đã tải sẽ hiện ở đây", color = TextDim, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(tasks, key = { it.id }) { t -> TaskItem(t, vm, onDelete = { del = it }) }
            }
        }
    }

    del?.let { t ->
        AlertDialog(
            onDismissRequest = { del = null },
            title = { Text("Xóa video?") },
            text = { Text(if (t.uri != null) "Video sẽ bị xóa khỏi điện thoại." else "Xóa khỏi danh sách.") },
            confirmButton = { TextButton(onClick = { Hub.delete(ctx, t); del = null }) { Text("Xóa", color = Danger) } },
            dismissButton = { TextButton(onClick = { del = null }) { Text("Hủy") } }
        )
    }
}

@Composable
private fun TaskItem(t: DlTask, vm: MainViewModel, onDelete: (DlTask) -> Unit) {
    val ctx = LocalContext.current
    val audio = t.quality == "audio"
    GlassCard {
        Row {
            Box(
                Modifier.size(width = 96.dp, height = 64.dp).clip(RoundedCornerShape(12.dp))
                    .background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                if (t.thumb != null) {
                    AsyncImage(
                        model = t.thumb, contentDescription = null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(if (audio) "🎵" else "🎬", fontSize = 26.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(t.title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                val (txt, col) = when (t.status) {
                    Status.QUEUED -> "Đang chờ…" to TextDim
                    Status.RUNNING ->
                        (if (t.message.isNotEmpty()) t.message
                        else if (t.progress <= 0f) "Đang kết nối…"
                        else "${(t.progress * 100).toInt()}%" +
                            (if (t.eta > 0) " • còn ${fmtDur(t.eta)}" else "")) to Acc2
                    Status.DONE -> "✅ Đã lưu • ${qualityLabel(t.quality)}" to Ok
                    Status.ERROR -> "⚠ ${t.message}" to Danger
                    Status.CANCELED -> "Đã hủy" to TextDim
                }
                Text(txt, color = col, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
                if (t.status == Status.RUNNING) {
                    Spacer(Modifier.height(8.dp))
                    if (t.progress > 0f && t.message.isEmpty()) {
                        LinearProgressIndicator(
                            progress = { t.progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Acc2, trackColor = Color(0x33FFFFFF)
                        )
                    } else {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Acc2, trackColor = Color(0x33FFFFFF)
                        )
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            when (t.status) {
                Status.DONE -> {
                    TextButton(onClick = { openMedia(ctx, t.uri, audio) }) { Text("▶ Mở") }
                    TextButton(onClick = { shareMedia(ctx, t.uri, audio) }) { Text("↗ Chia sẻ") }
                    TextButton(onClick = { onDelete(t) }) { Text("🗑 Xóa", color = Danger) }
                }
                Status.QUEUED, Status.RUNNING ->
                    TextButton(onClick = { Hub.cancel(t.id) }) { Text("✕ Hủy", color = Danger) }
                Status.ERROR, Status.CANCELED -> {
                    TextButton(onClick = { vm.retry(t.id) }) { Text("↻ Thử lại") }
                    TextButton(onClick = { onDelete(t) }) { Text("🗑 Xóa", color = Danger) }
                }
            }
        }
    }
}

private fun openMedia(ctx: Context, uri: String?, audio: Boolean) {
    if (uri == null) return
    val i = Intent(Intent.ACTION_VIEW)
        .setDataAndType(Uri.parse(uri), if (audio) "audio/*" else "video/*")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        ctx.startActivity(i)
    } catch (_: Exception) {
        Toast.makeText(ctx, "Không tìm thấy app để mở file", Toast.LENGTH_SHORT).show()
    }
}

private fun shareMedia(ctx: Context, uri: String?, audio: Boolean) {
    if (uri == null) return
    val i = Intent(Intent.ACTION_SEND)
        .setType(if (audio) "audio/*" else "video/*")
        .putExtra(Intent.EXTRA_STREAM, Uri.parse(uri))
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    ctx.startActivity(Intent.createChooser(i, "Chia sẻ"))
}

// ---------------------------------------------------------------- Settings

@Composable
fun SettingsScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val engine by Hub.engine.collectAsStateWithLifecycle()
    var version by remember { mutableStateOf("…") }
    var busy by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(engine) {
        if (engine == EngineState.READY) {
            version = withContext(Dispatchers.IO) {
                try {
                    YoutubeDL.getInstance().version(ctx)
                } catch (_: Throwable) {
                    null
                }
            } ?: "?"
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                "Cài đặt", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        item {
            GlassCard {
                Text("Bộ máy tải (yt-dlp)", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Phiên bản: $version", color = TextDim, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Nếu một trang nào đó bỗng không tải được, hãy cập nhật bộ máy – các trang web hay đổi cách phát video.",
                    color = TextDim, fontSize = 12.sp
                )
                Spacer(Modifier.height(12.dp))
                GradientButton(
                    if (busy) "Đang cập nhật…" else "🔄 Cập nhật bộ máy",
                    Modifier.fillMaxWidth(),
                    enabled = !busy && engine == EngineState.READY
                ) {
                    scope.launch {
                        busy = true
                        msg = null
                        val r = withContext(Dispatchers.IO) {
                            try {
                                YoutubeDL.getInstance().updateYoutubeDL(ctx).toString()
                            } catch (e: Throwable) {
                                "Lỗi: " + shortError(e)
                            }
                        }
                        msg = when {
                            r.startsWith("Lỗi") -> r
                            r.contains("ALREADY", true) -> "Bạn đang dùng bản mới nhất ✅"
                            else -> "Đã cập nhật xong ✅"
                        }
                        version = withContext(Dispatchers.IO) {
                            try {
                                YoutubeDL.getInstance().version(ctx)
                            } catch (_: Throwable) {
                                null
                            }
                        } ?: version
                        busy = false
                    }
                }
                msg?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = if (it.startsWith("Lỗi")) Danger else Ok, fontSize = 13.sp)
                }
            }
        }
        item {
            GlassCard {
                Text("Nơi lưu file", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("🎬 Video → thư mục Movies/VideoDL (hiện trong Thư viện ảnh)", color = TextDim, fontSize = 13.sp)
                Text("🎵 Âm thanh → thư mục Music/VideoDL", color = TextDim, fontSize = 13.sp)
            }
        }
        item {
            GlassCard {
                Text("Lưu ý", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Chỉ tải nội dung bạn có quyền sử dụng. Video riêng tư hoặc yêu cầu đăng nhập có thể không tải được.",
                    color = TextDim, fontSize = 13.sp
                )
            }
        }
    }
}

// ---------------------------------------------------------------- Bottom bar

@Composable
fun BottomBar(vm: MainViewModel) {
    NavigationBar(containerColor = Color(0xE60E0B24)) {
        listOf("⬇" to "Tải về", "📁" to "Thư viện", "⚙" to "Cài đặt").forEachIndexed { i, (icon, label) ->
            NavigationBarItem(
                selected = vm.tab == i,
                onClick = { vm.tab = i },
                icon = { Text(icon, fontSize = 20.sp) },
                label = { Text(label, textAlign = TextAlign.Center) },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Acc1.copy(alpha = 0.35f),
                    selectedTextColor = Color.White,
                    unselectedTextColor = TextDim
                )
            )
        }
    }
}
