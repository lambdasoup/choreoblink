package com.lambdasoup.choreoblink

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.support.v7.widget.CardView
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

class ChoreoRepository {

    val choreos = MutableLiveData<List<Choreo>>()

    init {
        choreos.postValue(listOf(Choreo("blink 1"), Choreo("blink 2")))
    }

}

data class Choreo(val id: String)

class ChoreoView @JvmOverloads constructor(context: Context,
                                           attrs: AttributeSet? = null,
                                           defStyleAttr: Int = 0)
    : CardView(context, attrs, defStyleAttr), Observer<List<Choreo>> {

    override fun onChanged(choreos: List<Choreo>?) {
        if (choreos == null) {
            return
        }

        adapter.choreos.clear()
        adapter.choreos.addAll(choreos)
        adapter.notifyDataSetChanged()
    }

    private val adapter = object : RecyclerView.Adapter<ChoreoViewHolder>() {

        val choreos = mutableListOf<Choreo>()

        override fun getItemCount(): Int {
            return choreos.size
        }

        override fun onBindViewHolder(holder: ChoreoViewHolder, position: Int) {
            val choreo = choreos[position]
            holder.button.text = choreo.id
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChoreoViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_choreo, parent, false)
            return ChoreoViewHolder(view)
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.card_choreo, this)
        val list: RecyclerView = findViewById(R.id.list)
        list.adapter = adapter
    }

    private class ChoreoViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val button: TextView = view.findViewById(R.id.button)

    }

}
