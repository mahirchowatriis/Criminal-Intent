package com.riis.application

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView


private const val TAG = "CrimeListFragment"


class CrimeListFragment : Fragment() {

    private lateinit var emptyScreenPromptLayout: LinearLayout
    private lateinit var emptyScreenNewCrimeButton: Button

    private lateinit var crimeRecyclerView: RecyclerView
    private var adapter: CrimeAdapter? = CrimeAdapter(emptyList())

    private val crimeListViewModel: CrimeListViewModel by lazy { //associating the fragment with the ViewModel CrimeListViewModel.kt
        ViewModelProvider(this).get(CrimeListViewModel::class.java)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_crime_list, container, false)
        crimeRecyclerView = view.findViewById(R.id.crime_recycler_view)
        emptyScreenNewCrimeButton = view.findViewById(R.id.empty_screen_new_crime_button)
        emptyScreenPromptLayout = view.findViewById(R.id.empty_screen_prompt_layout)


        emptyScreenNewCrimeButton.setOnClickListener {
            val crime = Crime()
            crimeListViewModel.addCrime(crime)
            val action = CrimeListFragmentDirections.moveToDetailView(crimeId=crime.id.toString())
            this.findNavController().navigate(action)

        }

        crimeRecyclerView.layoutManager = LinearLayoutManager(context)

        crimeRecyclerView.adapter = adapter

        return view

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Log.i(TAG,"view created")

        crimeListViewModel.crimeListLiveData.observe(
            viewLifecycleOwner,
            { crimes ->
                crimes?.let {
                    Log.i(TAG, "Got crimes ${crimes.size}")
                    updateUI(crimes)
                }
            })
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_crime_list, menu)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {

            R.id.new_crime -> {
                val crime = Crime()
                crimeListViewModel.addCrime(crime)
                val action = CrimeListFragmentDirections.moveToDetailView(crimeId=crime.id.toString())
                this.findNavController().navigate(action)
                true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }



    private fun updateUI(crimes: List<Crime>) {

        adapter = CrimeAdapter(crimes)

        crimeRecyclerView.adapter = adapter

        emptyScreenPromptLayout.visibility = if (crimes.isNotEmpty()) View.GONE else View.VISIBLE
    }


    private inner class CrimeHolder(view: View)
        : RecyclerView.ViewHolder(view),View.OnClickListener {

        private lateinit var crime: Crime


        private val titleTextView: TextView = itemView.findViewById(R.id.crime_title)
        private val dateTextView: TextView = itemView.findViewById(R.id.crime_date)
        private val solvedImageView: ImageView = itemView.findViewById(R.id.crime_solved)

        init {
            itemView.setOnClickListener(this)
        }

        fun bind(crime: Crime) {
            this.crime = crime

            titleTextView.text = this.crime.title
            dateTextView.text = this.crime.date.toString()

            solvedImageView.visibility = if (crime.isSolved) {
                View.VISIBLE
            } else {
                View.GONE
            }

        }

        override fun onClick(v: View) {

            val action = CrimeListFragmentDirections.moveToDetailView(crimeId=crime.id.toString())
            v.findNavController().navigate(action)

        }

    }

    private inner class CrimeAdapter(var crimes: List<Crime>)
        : RecyclerView.Adapter<CrimeHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
                : CrimeHolder {

            val view = layoutInflater.inflate(R.layout.list_item_crime, parent, false)
            return CrimeHolder(view)
        }

        override fun getItemCount() = crimes.size

        override fun onBindViewHolder(holder: CrimeHolder, position: Int) {
            val crime = crimes[position]
            holder.bind(crime)
        }
    }


}