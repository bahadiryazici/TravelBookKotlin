package com.example.kotlinmaps.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.kotlinmaps.view.MapsActivity
import com.example.kotlinmaps.Singleton
import com.example.kotlinmaps.databinding.RecyclerRowBinding
import com.example.kotlinmaps.model.Place


class RecyclerAdapter(val list: List<Place>) : RecyclerView.Adapter<RecyclerAdapter.RecyclerHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerHolder {
        val recyclerRowBinding: RecyclerRowBinding = RecyclerRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RecyclerHolder(recyclerRowBinding)
    }

    override fun onBindViewHolder(holder: RecyclerHolder, position: Int) {
        holder.recyclerRowBinding.recyclerViewTextView.setText(list[position].name)
        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, MapsActivity::class.java)
            Singleton.selectedPlace = list.get(position)
            intent.putExtra("info", 1)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }

    class RecyclerHolder(val recyclerRowBinding: RecyclerRowBinding) : RecyclerView.ViewHolder(recyclerRowBinding.root) {

    }
}