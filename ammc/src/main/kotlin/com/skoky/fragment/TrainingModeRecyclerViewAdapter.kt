package com.skoky.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.skoky.R
import com.skoky.fragment.content.TrainingLap
import com.skoky.fragment.content.TrainingModeModel
import java.text.SimpleDateFormat
import java.util.*

class TrainingModeRecyclerViewAdapter()
    : RecyclerView.Adapter<TrainingModeRecyclerViewAdapter.ViewHolder>() {

    private var mValues: MutableList<TrainingLap> = mutableListOf()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_trainingmode, parent, false)

        return ViewHolder(view)
    }

    private val df = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (position == 0) {  // header
            holder.mIdView.text = "#"
            holder.mLapTime.text = "Lap Time"
            holder.mDiff.text = "Diff"

        } else {
            val item = mValues[position - 1]

            holder.mIdView.text = item.number.toString()

            if (item.diffMs == null) {

                holder.mLapTime.text = df.format(Date(item.time.us / 1000))
                holder.mDiff.text = ""

            } else {

                holder.mLapTime.text = timeToText(item.lapTimeMs)

                if (item.lapTimeMs != (item.diffMs)) {
                    holder.mDiff.text = String.format("%+.3f", item.diffMs.toFloat() / 1000)
                    if (item.diffMs < 0)
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

    fun timeToText(lapTimeMs: Int): String {
        val millis = lapTimeMs % 1000
        val second = lapTimeMs / 1000 % 60
        val minute = lapTimeMs / (1000 * 60)
        return String.format("%d:%d.%d", minute, second, millis)
    }

    override fun getItemCount(): Int = (mValues.size + 1)

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.findViewById(R.id.item_position)
        val mLapTime: TextView = mView.findViewById(R.id.item_time)
        val mDiff: TextView = mView.findViewById(R.id.item_diff)

    }

    fun getLastLap(): TrainingLap? {
        return mValues.maxByOrNull { it.number }
    }

    val tmm = TrainingModeModel()
    fun addRecord(transponder: String, time: Time) {
        mValues = tmm.newPassing(mValues.toList(), transponder, time).toMutableList()
    }

    fun clearResults() {
        mValues = mutableListOf()
    }

}
