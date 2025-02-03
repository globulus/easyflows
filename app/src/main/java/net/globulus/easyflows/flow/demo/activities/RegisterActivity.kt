package net.globulus.easyflows.flow.demo.activities

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import net.globulus.easyflows.flow.demo.R
import net.globulus.easyflows.flow.demo.flows.RegisterChecklist
import net.globulus.easyflows.proceed
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RegisterActivity : BaseActivity(), RegisterChecklist {

    private var dobDate: LocalDate? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        findViewById<Button>(R.id.dob).apply {
            setOnClickListener {
                DatePickerDialog(this@RegisterActivity, { _, y, m, d ->
                    dobDate = LocalDate.of(y, m + 1, d)
                    text = dobDate!!.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    findViewById<Button>(R.id.register).isEnabled = true
                }, dobDate?.year ?: DEFAULT_YEAR,
                    (dobDate?.monthValue ?: 1) - 1, dobDate?.dayOfMonth ?: 0)
                    .show()
            }
        }

        findViewById<Button>(R.id.register).setOnClickListener {
            proceed()
        }
    }

    override val isMinor
        get() = (dobDate?.year ?: DEFAULT_YEAR) >= (LocalDate.now().minusYears(MINOR_AGE).year)

    companion object {
        private const val DEFAULT_YEAR = 1970
        private const val MINOR_AGE = 18L
    }
}
