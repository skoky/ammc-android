package com.skoky.fragment

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.skoky.MyApp
import com.skoky.R
import com.skoky.Tools
import com.skoky.fragment.content.Racer
import com.skoky.fragment.content.RacingModeModel
import java.text.SimpleDateFormat


class RacingModeRecyclerViewAdapter(private var mValues: MutableList<Racer>,
                                    private val onLongTapListener: (View) -> Unit)
    : RecyclerView.Adapter<RacingModeRecyclerViewAdapter.ViewHolder>() {

    private val mOnClickListener: View.OnClickListener

    companion object {
        const val TAG = "RacingAdapter"
    }

    init {
        mValues.sortByDescending { it.pos }
        mOnClickListener = View.OnClickListener { v ->
            val item = v.tag as Racer
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_racingmode, parent, false)
        view.setOnLongClickListener {
            onLongTapListener(it); true
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
            if (position == 0) {  // header
                holder.mPosition.text = "#"
                holder.mTrasView.text = "Transponder"
                holder.mLapCount.text = "Laps"
                holder.mLastLapTime.text = "Time"

            } else {

                val item = mValues[position - 1]

                holder.mPosition.text = item.pos.toString()
                holder.mTrasView.text = if (item.driverName.isNullOrBlank()) item.transponder else item.driverName
                holder.mLastLapTime.text = Tools.timeToText(item.lastLapTimeMs)
                holder.mLapCount.text = "     ${item.laps}"

            }


    override fun getItemCount(): Int = (mValues.size + 1)

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mPosition: TextView = mView.findViewById(R.id.item_position)
        val mTrasView: TextView = mView.findViewById(R.id.item_transponder)
        val mLastLapTime: TextView = mView.findViewById(R.id.item_last_lap_time)
        val mLapCount: TextView = mView.findViewById(R.id.item_laps_count)

    }

    val tmm = RacingModeModel()
    fun addRecord(transponder: String, time: Time) {
        mValues = tmm.newPassing(mValues.toList(), transponder, time).toMutableList()
    }

    fun getDriverForTransponder(transOrDriver: CharSequence?): String {

        mValues.forEach {
            if (it.transponder == transOrDriver) {
                return it.driverName.orEmpty()
            }
        }
        mValues.forEach {
            if (it.driverName == transOrDriver) {
                return it.driverName.orEmpty()
            }
        }
        return ""
    }

    fun getTransponder(transOrDriver: String): String {
        mValues.forEach {
            if (it.transponder == transOrDriver) {
                return it.transponder
            }
        }
        mValues.forEach {
            if (it.driverName == transOrDriver) {
                return it.transponder
            }
        }
        return ""
    }


    fun saveDriverName(app: MyApp, transOrDriver: String, newDriverName: String) {

        mValues.forEach {
            if (it.transponder == transOrDriver) {
                it.driverName = newDriverName
                notifyDataSetChanged()
                app.drivers.saveTransponder(it.transponder, newDriverName) {}
                return
            }
        }

        mValues.forEach {
            if (it.driverName == transOrDriver) {
                it.driverName = newDriverName
                notifyDataSetChanged()
                app.drivers.saveTransponder(it.transponder, newDriverName) {}
                return
            }
        }
    }

    fun clearResults() {
        mValues = mutableListOf()
    }

    fun updateDriverName(app: MyApp, transponder: String) {

        mValues.forEach { d ->
            if (d.transponder == transponder) {
                if (d.driverName.isNullOrEmpty()) {
                    app.drivers.getDriverForTransponderLastByDate(transponder) { foundDName ->
                        d.driverName = foundDName
                        notifyDataSetChanged()
                    }
                }
            }
        }

    }
}
