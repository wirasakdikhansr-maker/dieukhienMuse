package com.videodl.app

import android.app.Application
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Hub.init(this)
        Thread {
            try {
                YoutubeDL.getInstance().init(this)
                FFmpeg.getInstance().init(this)
                Hub.engine.value = EngineState.READY
            } catch (e: Throwable) {
                Hub.engineError = e.message ?: e.toString()
                Hub.engine.value = EngineState.FAILED
            }
        }.start()
    }
}
