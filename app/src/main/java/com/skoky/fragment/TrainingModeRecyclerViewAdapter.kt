package com.skoky.fragment

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.skoky.R
import com.skoky.fragment.TrainingModeFragment.OnListFragmentInteractionListener
import com.skoky.fragment.content.TrainingModeModel


import kotlinx.android.synthetic.main.fragment_trainingmode.view.*

class TrainingModeRecyclerViewAdapter(private val mValues: MutableList<TrainingModeModel.Lap>, private val mListener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<TrainingModeRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mValues.sortByDescending { it.number }
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as TrainingModeModel.Lap
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
            holder.mLapTime.text = item.lapTimeMs.toString()
            holder.mDiff.text = item.diff.toString()

            with(holder.mView) {
                tag = item
                //    setOnClickListener(mOnClickListener)
            }
        }
    }

    override fun getItemCount(): Int = (mValues.size + 1)

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.item_position
        val mLapTime: TextView = mView.item_time
        val mDiff: TextView = mView.item_diff

    }

    fun addRecord(lap: TrainingModeModel.Lap) {
        mValues.add(lap)
        mValues.sortByDescending { it.number }
    }
}
