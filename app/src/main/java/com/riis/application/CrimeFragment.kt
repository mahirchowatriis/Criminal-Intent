package com.riis.application

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import java.text.SimpleDateFormat
import android.text.format.DateFormat
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import java.util.*



private const val TAG = "CrimeFragment"
private const val REQUEST_DATE = 0
private const val REQUEST_CONTACT = 1
private const val DATE_FORMAT = "EEE, MMM, dd"

class CrimeFragment : Fragment() {

    private lateinit var crime: Crime
    private lateinit var titleField: EditText
    private lateinit var dateButton: Button
    private lateinit var solvedCheckBox: CheckBox
    private lateinit var reportButton: Button
    private lateinit var suspectButton: Button
    private lateinit var pickContactContract: ActivityResultContract<Uri, Uri?>
    private lateinit var pickContactCallback: ActivityResultCallback<Uri?>
    private lateinit var pickContactLauncher: ActivityResultLauncher<Uri>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>


    private val crimeDetailViewModel: CrimeDetailViewModel by lazy {
        ViewModelProvider(this).get(CrimeDetailViewModel::class.java)
    }

    private val safeArgs: CrimeFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        crime = Crime()

        val crimeId: UUID = UUID.fromString(safeArgs.crimeId)
        crimeDetailViewModel.loadCrime(crimeId)

        pickContactContract = object : ActivityResultContract<Uri, Uri?>() {

            override fun createIntent(context: Context, input: Uri): Intent {
                return Intent(Intent.ACTION_PICK, input)
            }

            override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
                if(resultCode != Activity.RESULT_OK || intent == null)
                    return null
                return intent.data
            }
        }

        pickContactCallback = ActivityResultCallback<Uri?> { contactUri: Uri? ->

            val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)

            val cursor = contactUri?.let {
                requireActivity().contentResolver
                    .query(it, queryFields, null, null, null)
            }
            cursor?.use {

                if (it.count > 0) {

                    it.moveToFirst()
                    val suspect = it.getString(0)
                    crime.suspect = suspect
                    crimeDetailViewModel.saveCrime(crime)
                    suspectButton.text = suspect
                }
            }
        }

        pickContactLauncher = registerForActivityResult(pickContactContract, pickContactCallback)

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission())
        { isGranted ->
            if(isGranted){
                pickContactLauncher.launch(ContactsContract.Contacts.CONTENT_URI)
            }
        }


    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_crime, container, false)

        titleField = view.findViewById(R.id.crime_title) as EditText
        dateButton = view.findViewById(R.id.crime_date) as Button
        solvedCheckBox = view.findViewById(R.id.crime_solved) as CheckBox
        reportButton = view.findViewById(R.id.crime_report) as Button
        suspectButton = view.findViewById(R.id.crime_suspect) as Button

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        crimeDetailViewModel.crimeLiveData.observe(
            viewLifecycleOwner,
            Observer { crime ->
                crime?.let {
                    this.crime = crime
                    updateUI()
                }
            })

        val navBackStackEntry = findNavController().getBackStackEntry(R.id.crimeDetail_dest)


        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_RESUME
                && navBackStackEntry.savedStateHandle.contains("resultDate")) {
                val resultDate = navBackStackEntry.savedStateHandle.get<String>("resultDate")


                val dateFormatter = SimpleDateFormat("EE MMM dd HH:mm:ss z yyyy", Locale.getDefault())
                crime.date = dateFormatter.parse(resultDate as String) as Date
                updateUI()
            }
        }

        navBackStackEntry.lifecycle.addObserver(observer)

        viewLifecycleOwner.lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) {
                navBackStackEntry.lifecycle.removeObserver(observer)
            }
        })

    }


    override fun onStart() {
        super.onStart()


        val titleWatcher = object : TextWatcher {
            override fun beforeTextChanged(
                sequence: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {

            }
            override fun onTextChanged(
                sequence: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                crime.title = sequence.toString()
            }
            override fun afterTextChanged(sequence: Editable?) {

            }
        }
        titleField.addTextChangedListener(titleWatcher)


        solvedCheckBox.apply {
            setOnCheckedChangeListener { _, isChecked ->
                crime.isSolved = isChecked
            }
        }

        dateButton.setOnClickListener {

            val action = CrimeFragmentDirections.showDatePickerDialog(date=crime.date.time)
            this.findNavController().navigate(action)
            }

        reportButton.setOnClickListener {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, getCrimeReport())
                putExtra(
                    Intent.EXTRA_SUBJECT,
                    getString(R.string.crime_report_subject))
            }.also { intent ->

                val chooserIntent = Intent.createChooser(intent, getString(R.string.send_report))
                startActivity(chooserIntent)
            }
        }


        suspectButton.apply {
            setOnClickListener {
                requestPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
            }

            val pickContactIntent = pickContactContract.createIntent(requireContext(), ContactsContract.Contacts.CONTENT_URI)

            val packageManager: PackageManager = requireActivity().packageManager
            val resolvedActivity: ResolveInfo? =
                packageManager.resolveActivity(pickContactIntent,
                    PackageManager.MATCH_DEFAULT_ONLY)
            if (resolvedActivity == null) {
                isEnabled = false
            }
        }
    }

    private fun updateUI() {
        titleField.setText(crime.title)
        dateButton.text = crime.date.toString()
        solvedCheckBox.apply {
            isChecked = crime.isSolved
            jumpDrawablesToCurrentState()
        }
    }


    private fun getCrimeReport(): String {

        val dateString = DateFormat.format(DATE_FORMAT, crime.date).toString()

        val solvedString = if (crime.isSolved) {
            getString(R.string.crime_report_solved)
        } else {
            getString(R.string.crime_report_unsolved)
        }

        var suspect = if (crime.suspect.isBlank()) {
            getString(R.string.crime_report_no_suspect)
        } else {
            getString(R.string.crime_report_suspect, crime.suspect)
        }

        return getString(R.string.crime_report,
            crime.title, dateString, solvedString, suspect)
    }


    override fun onStop() {
        super.onStop()
        crimeDetailViewModel.saveCrime(crime)
    }

}


