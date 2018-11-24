package com.skoky.fragment

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.skoky.R
import com.skoky.fragment.content.ConsoleModel
import kotlinx.android.synthetic.main.fragment_trainingmode.view.*
import java.text.SimpleDateFormat

class ConsoleModeRecyclerViewAdapter(private var mValues: MutableList<ConsoleModel>, private val mListener: ConsoleModeFragment.OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<ConsoleModeRecyclerViewAdapter.ViewHolder>() {

//    private val mOnClickListener: View.OnClickListener


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_consolemode, parent, false)

        return ViewHolder(view)
    }

    val df = SimpleDateFormat("HH:mm:ss.SSS")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.mDiff

//        if (position == 0) {  // header
//            holder.mIdView.text = "#"
//            holder.mLapTime.text = "Lap Time"
//            holder.mDiff.text = "Diff"
//
//        } else {
//            val item = mValues[position - 1]
//
//            holder.mIdView.text = item.number.toString()
//
//            if (item.diffMs==null) {
//
//                holder.mLapTime.text = df.format(Date(item.time.us / 1000))
//                holder.mDiff.text = ""
//
//            } else {
//
//                holder.mLapTime.text = timeToText(item.lapTimeMs)
//
//                if (item.lapTimeMs != (item.diffMs)) {
//                    holder.mDiff.text = String.format("%+.3f", item.diffMs.toFloat() / 1000)
//                    if (item.diffMs < 0 )
//                        holder.mDiff.setBackgroundResource(R.color.amm_green)
//                    else if (item.diffMs > 0)
//                        holder.mDiff.setBackgroundResource(R.color.amm_red)
//                }
//                // TODO set background color to red or green
//
//
//                with(holder.mView) {
//                    tag = item
//                    //    setOnClickListener(mOnClickListener)
//                }
//            }
//        }
    }


    override fun getItemCount(): Int = (mValues.size + 1)

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mDiff: TextView = mView.item_diff

    }

}
