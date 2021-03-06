package com.skoky.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import com.skoky.R


class HelpFragment : FragmentCommon() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_help, container, false) as WebView
        view.loadUrl("file:///android_asset/about/about.html")
        return view
    }

    companion object {

        const val TAG = "HelpFragment"

        @JvmStatic
        fun newInstance() = HelpFragment()
    }

}
