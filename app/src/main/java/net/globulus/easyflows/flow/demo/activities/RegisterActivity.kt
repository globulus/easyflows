package net.globulus.easyflows.flow.demo.activities

import android.app.DatePickerDialog
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_register.*
import net.globulus.easyflows.FlowManager
import net.globulus.easyflows.flow.demo.R
import net.globulus.easyflows.flow.demo.flows.RegisterChecklist
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RegisterActivity : BaseActivity(), RegisterChecklist {

    private var dobDate: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dob.setOnClickListener {
            DatePickerDialog(this, DatePickerDialog.OnDateSetListener { _, y, m, d ->
                dobDate = LocalDate.of(y, m + 1, d)
                dob.text = dobDate!!.format(DateTimeFormatter.ISO_LOCAL_DATE)
                register.isEnabled = true
            }, dobDate?.year ?: DEFAULT_YEAR,
                (dobDate?.monthValue ?: 1) - 1, dobDate?.dayOfMonth ?: 0)
                .show()
        }

        register.setOnClickListener {
            FlowManager.proceed(this)
        }
    }

    override fun isMinor(): Boolean {
        return (dobDate?.year ?: DEFAULT_YEAR) >= (LocalDate.now().minusYears(MINOR_AGE).year)
    }

    companion object {
        private const val DEFAULT_YEAR = 1970
        private const val MINOR_AGE = 18L
    }
}
