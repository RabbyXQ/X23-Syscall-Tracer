package com.rabbyx.x23p

import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

class MainActivity : AppCompatActivity() {

    private lateinit var listView: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listView = findViewById(R.id.processListView)

        // Execute the command to get the list of packages asynchronously
        GetProcessListTask().execute()

        // Set an item click listener for the ListView
        listView.setOnItemClickListener { _, _, position, _ ->
            val packageName = listView.getItemAtPosition(position).toString().removePrefix("package:")
            val intent = Intent(this, SyscallActivity::class.java)
            intent.putExtra("PACKAGE_NAME", packageName)
            startActivity(intent)
        }
    }

    private inner class GetProcessListTask : AsyncTask<Void, Void, Array<String>?>() {

        override fun doInBackground(vararg params: Void?): Array<String>? {
            // Command to list installed packages on the device
            val packageListCommand = arrayOf("sh", "-c", "pm list packages")

            try {
                // Execute the package list command
                val process = Runtime.getRuntime().exec(packageListCommand)
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = StringBuilder()
                var line: String?

                // Read the output from the command
                while (reader.readLine().also { line = it } != null) {
                    output.append(line).append("\n")
                }
                reader.close()
                process.waitFor()

                // Log the package list output
                println("Package list output: ${output.toString()}")

                // If the output is empty, return a default message
                if (output.isEmpty()) {
                    return arrayOf("No packages found or failed to execute command")
                }

                // Split the output into lines and return as array
                return output.toString().split("\n").toTypedArray()
            } catch (e: IOException) {
                e.printStackTrace()
                return arrayOf("Error: IOException occurred")
            } catch (e: InterruptedException) {
                e.printStackTrace()
                return arrayOf("Error: InterruptedException occurred")
            }
        }

        override fun onPostExecute(result: Array<String>?) {
            super.onPostExecute(result)

            if (result != null && result.isNotEmpty()) {
                // Use the default ArrayAdapter with simple_list_item_1
                val adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_list_item_1, result)
                listView.adapter = adapter
            } else {
                // Show error if failed to get the list
                Toast.makeText(this@MainActivity, "Failed to get package list or no packages found", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
