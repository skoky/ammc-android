package com.skoky.fragment

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.skoky.R
import com.skoky.fragment.TrainingModeFragment.OnListFragmentInteractionListener
import com.skoky.fragment.content.Lap
import com.skoky.fragment.content.TrainingModeModel
import kotlinx.android.synthetic.main.fragment_trainingmode.view.*
import java.text.SimpleDateFormat
import java.util.*

class TrainingModeRecyclerViewAdapter(private var mValues: MutableList<Lap>, private val mListener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<TrainingModeRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mValues.sortByDescending { it.number }
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as Lap
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_trainingmode, parent, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == 0) {  // header
            holder.mIdView.text = "#"
            holder.mLapTime.text = "Lap Time"
            holder.mDiff.text = "Diff"

        } else {

            val item = mValues[position - 1]
            holder.mIdView.text = item.number.toString()

            if (item.lapTimeMs==0) {
                holder.mLapTime.text = Date(item.time).toString()
            } else {
                holder.mLapTime.text = timeToText(item.lapTimeMs)
            }
            holder.mDiff.text = item.diff.toString()

            with(holder.mView) {
                tag = item
                //    setOnClickListener(mOnClickListener)
            }
        }
    }

    private fun timeToText(lapTimeMs: Int): String {
        val millis = lapTimeMs % 1000
        val second = lapTimeMs / 1000 % 60
        val minute = lapTimeMs / (1000 * 60) // % 60
//        val hour = lapTimeMs / (1000 * 60 * 60) % 24
        return String.format("%d:%d.%d",minute,second,millis)
    }

    override fun getItemCount(): Int = (mValues.size + 1)

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.item_position
        val mLapTime: TextView = mView.item_time
        val mDiff: TextView = mView.item_diff

    }
    val tmm = TrainingModeModel()
    fun addRecord(transponder: Int, time: Long) {
        mValues = tmm.newPassing(mValues.toList(),transponder,time)
    }
}
