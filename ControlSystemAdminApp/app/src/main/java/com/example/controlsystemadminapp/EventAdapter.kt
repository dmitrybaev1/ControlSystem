package com.example.controlsystemadminapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.controlsystemadminapp.R

class EventAdapter(private val list: ArrayList<EventItem>) : RecyclerView.Adapter<EventAdapter.EventHolder>() {


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.event_item,parent,false)
        return EventHolder(view)
    }

    override fun onBindViewHolder(holder: EventHolder, position: Int) {
        holder.employeeId.text = list[position].employeeId
        holder.datetime.text = list[position].dateTime
        holder.approved.text = list[position].approved
    }

    override fun getItemCount(): Int {
        return list.size
    }
    inner class EventHolder(v: View) : RecyclerView.ViewHolder(v){
        val view: View = v
        val employeeId: TextView = v.findViewById(R.id.employeeIdText)
        val datetime: TextView = v.findViewById(R.id.datetimeText)
        val approved: TextView = v.findViewById(R.id.approvedText)
    }
}