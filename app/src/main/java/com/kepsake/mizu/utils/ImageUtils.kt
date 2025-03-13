package com.kepsake.mizu.utils

import android.annotation.SuppressLint

@SuppressLint("DefaultLocale")
fun getImages(): List<String> {
    return (1..100).map { "http://192.168.0.106:3000/image_${String.format("%03d", it)}.png" }
}