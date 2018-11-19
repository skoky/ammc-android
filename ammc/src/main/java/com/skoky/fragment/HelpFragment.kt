package com.skoky.fragment

import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import com.skoky.MyApp
import com.skoky.R


class HelpFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_help, container, false) as WebView
        view.loadUrl("file:///android_asset/about/about.html")
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

    }

    companion object {

        private const val ARG_COLUMN_COUNT = "column-count"
        const val TAG = "HelpFragment"

        @JvmStatic
        fun newInstance(columnCount: Int) =
                HelpFragment().apply {
                    arguments = Bundle().apply {
                        putInt(ARG_COLUMN_COUNT, columnCount)
                    }
                }
    }

}
