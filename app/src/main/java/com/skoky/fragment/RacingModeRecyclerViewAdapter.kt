package com.skoky.fragment

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.skoky.R
import com.skoky.fragment.RacingModeFragment.OnListFragmentInteractionListener
import com.skoky.fragment.content.RacingLap
import com.skoky.fragment.content.RacingModeModel
import kotlinx.android.synthetic.main.fragment_racingmode.view.*
import java.text.SimpleDateFormat
import java.util.*

class RacingModeRecyclerViewAdapter(private var mValues: MutableList<RacingLap>, private val mListener: OnListFragmentInteractionListener?)
    : RecyclerView.Adapter<RacingModeRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    init {
        mValues.sortByDescending { it.number }
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as RacingLap
            // Notify the active callbacks interface (the activity, if the fragment is attached to
            // one) that an item has been selected.
            mListener?.onListFragmentInteraction(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_racingmode, parent, false)

        return ViewHolder(view)
    }

    val df = SimpleDateFormat("HH:mm:ss.SSS")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == 0) {  // header
            holder.mIdView.text = "#"
            holder.mLapTime.text = "Lap Time"
            holder.mDiff.text = "Diff"

        } else {

            val item = mValues[position - 1]
            if (item.diffMs==null) {

                holder.mIdView.text = item.number.toString()
                holder.mLapTime.text = df.format(Date(item.timeUs / 1000))
                holder.mDiff.text = ""

            } else {

                holder.mIdView.text = item.number.toString()

                holder.mLapTime.text = timeToText(item.lapTimeMs)

                if (item.lapTimeMs != (item.diffMs)) {
                    holder.mDiff.text = String.format("%+.3f", item.diffMs.toFloat() / 1000)
                    if (item.diffMs < 0 )
                        holder.mDiff.setBackgroundResource(R.color.amm_green)
                    else if (item.diffMs > 0)
                        holder.mDiff.setBackgroundResource(R.color.amm_red)
                }
                // TODO set background color to red or green


                with(holder.mView) {
                    tag = item
                    //    setOnClickListener(mOnClickListener)
                }
            }
        }
    }

    private fun timeToText(lapTimeMs: Int): String {
        val millis = lapTimeMs % 1000
        val second = lapTimeMs / 1000 % 60
        val minute = lapTimeMs / (1000 * 60)
        return String.format("%d:%d.%d", minute, second, millis)
    }

    override fun getItemCount(): Int = (mValues.size + 1)

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.item_position
        val mLapTime: TextView = mView.item_time
        val mDiff: TextView = mView.item_diff

    }

    val tmm = RacingModeModel()
    fun addRecord(transponder: Int, time: Long) {
        mValues = tmm.newPassing(mValues.toList(), transponder, time).toMutableList()
    }

    fun clearResults() {
        mValues = mutableListOf()
    }

}
