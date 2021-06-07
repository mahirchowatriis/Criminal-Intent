package com.riis.application

import android.app.DatePickerDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.DatePicker
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import java.sql.Date
import java.util.*


class DatePickerFragment : DialogFragment() {

    private val safeArgs: DatePickerFragmentArgs by navArgs()


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {


        val dateListener = DatePickerDialog.OnDateSetListener {
                _: DatePicker, year: Int, month: Int, day: Int ->

            val resultDate : java.util.Date = GregorianCalendar(year, month, day).time

            findNavController().previousBackStackEntry?.savedStateHandle?.set("resultDate", resultDate.toString())

        }

        val date = Date(safeArgs.date)

        val crimeDate: Date = Date.valueOf(date.toString())

        val calendar = Calendar.getInstance()
        calendar.time = crimeDate
        val initialYear = calendar.get(Calendar.YEAR)
        val initialMonth = calendar.get(Calendar.MONTH)
        val initialDay = calendar.get(Calendar.DAY_OF_MONTH)
        return DatePickerDialog(
            requireContext(),
            dateListener,
            initialYear,
            initialMonth,
            initialDay
        )
    }
}