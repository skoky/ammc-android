package com.skoky.menu

import android.app.Activity
import android.os.Bundle
import android.webkit.WebView
import android.widget.FrameLayout
import com.skoky.R


class About : Activity() {

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about)
        val full = findViewById(R.id.aboutView) as WebView

        full.loadUrl("file:///android_asset/about/about.html")
    }
}
