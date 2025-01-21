package com.rabbyx.x23p

import android.os.AsyncTask
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader

class SyscallActivity : AppCompatActivity() {

    private lateinit var syscallTextView: TextView
    private lateinit var packageName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_syscall)

        syscallTextView = findViewById(R.id.syscallTextView)

        // Get the package name from intent
        packageName = intent.getStringExtra("PACKAGE_NAME") ?: ""

        // Start the task to get PID and syscall trace
        if (packageName.isNotEmpty()) {
            GetPidAndTraceTask(packageName).execute()
        }
    }

    private inner class GetPidAndTraceTask(private val packageName: String) : AsyncTask<Void, Void, String?>() {

        override fun doInBackground(vararg params: Void?): String? {
            try {
                // Get PID of the package
                val pidCommand = arrayOf("sh", "-c", "pidof $packageName")
                val pidProcess = Runtime.getRuntime().exec(pidCommand)
                val pidReader = BufferedReader(InputStreamReader(pidProcess.inputStream))
                val pidOutput = StringBuilder()
                var pidLine: String?

                // Read the output (PID)
                while (pidReader.readLine().also { pidLine = it } != null) {
                    pidOutput.append(pidLine).append("\n")
                }
                pidReader.close()
                pidProcess.waitFor()

                val pid = pidOutput.toString().trim()
                if (pid.isEmpty()) {
                    return "Error: Could not find PID for $packageName"
                }

                // Log PID for debugging
                println("Found PID: $pid")

                // If PID is found, execute the strace command as root
                val straceCommand = arrayOf("su", "-c", "strace -p $pid -T -tt -o /data/local/tmp/strace_output.txt")
                val straceProcess = Runtime.getRuntime().exec(straceCommand)
                straceProcess.waitFor()

                // Wait for strace output to be generated
                Thread.sleep(1000) // Wait a bit longer to allow strace to start

                // Read the strace output for 90 seconds
                val straceFile = File("/data/local/tmp/strace_output.txt")
                val endTime = System.currentTimeMillis() + 90000 // 90 seconds from now
                val straceOutput = StringBuilder()

                // Ensure the file exists
                if (!straceFile.exists()) {
                    return "Error: strace output file not found."
                }

                // Monitor the file for 90 seconds
                while (System.currentTimeMillis() < endTime) {
                    val fileReader = BufferedReader(FileReader(straceFile))
                    var line: String?

                    // Read lines from the file
                    while (fileReader.readLine().also { line = it } != null) {
                        straceOutput.append(line).append("\n")
                    }

                    // Sleep for a short duration before checking again (to avoid overloading the system)
                    Thread.sleep(500)
                }

                // Return the collected syscalls
                return straceOutput.toString()

            } catch (e: IOException) {
                e.printStackTrace()
                return "Error: IOException occurred"
            } catch (e: InterruptedException) {
                e.printStackTrace()
                return "Error: InterruptedException occurred"
            }
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)

            if (result != null && result.isNotEmpty()) {
                // Display the syscalls in the TextView
                syscallTextView.text = result
            } else {
                // Show error if no syscalls found
                Toast.makeText(this@SyscallActivity, "Failed to get syscall trace", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
