package com.example.newapp

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
//import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricPrompt
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var passwordList: ListView
    private lateinit var passwordAdapter: ArrayAdapter<String>
    private lateinit var addPasswordButton: Button
    private val passwords = ArrayList<String>()

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize biometric authentication
        executor = ContextCompat.getMainExecutor(this)
        setupBiometricPrompt()

        // Check for biometric authentication at app launch
        checkBiometricAuthentication()
    }

    private fun setupBiometricPrompt() {
        biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                // Biometric authentication succeeded, display stored passwords
                displayStoredPasswords()
            }
        })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Biometric Authentication")
            .setSubtitle("Place your finger on the sensor")
            .setNegativeButtonText("Cancel")
            .build()
    }

    private fun checkBiometricAuthentication() {
        if (checkBiometricPermission()) {
            // Prompt for biometric authentication
            biometricPrompt.authenticate(promptInfo)
        } else {
            // Handle biometric permission not granted
            Toast.makeText(this, "Biometric authentication is not available.", Toast.LENGTH_SHORT).show()
            // You can choose to exit the app or display a message
            finish()
        }
    }

    private fun checkBiometricPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_BIOMETRIC) == PackageManager.PERMISSION_GRANTED
    }

    private fun displayStoredPasswords() {
        passwordList = findViewById(R.id.passwordList)
        addPasswordButton = findViewById(R.id.addPasswordButton)

        passwordAdapter = ArrayAdapter(this,R.layout.mycolor, passwords)
        passwordList.adapter = passwordAdapter

        // Retrieve stored passwords from SharedPreferences and add them to the list
        val sharedPreferences = getSharedPreferences("passwords", Context.MODE_PRIVATE)
        val allEntries = sharedPreferences.all
        for (entry in allEntries.entries) {
            passwords.add(entry.value as String)
        }

        passwordList.setOnItemLongClickListener { parent, view, position, id ->
            showEditDeletePasswordDialog(position)
            true
        }

        addPasswordButton.setOnClickListener {
            showAddPasswordDialog()
        }
    }

    private fun showAddPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Add Password")

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_password, null)
        val appNameInput = dialogView.findViewById<EditText>(R.id.appNameInput)
        val gmailInput = dialogView.findViewById<EditText>(R.id.gmailInput)
        val passwordInput = dialogView.findViewById<EditText>(R.id.passwordInput)

        builder.setView(dialogView)

        builder.setPositiveButton("Add") { _, _ ->
            val appName = appNameInput.text.toString()
            val gmail = gmailInput.text.toString()
            val password = passwordInput.text.toString()

            if (appName.isNotBlank() && gmail.isNotBlank() && password.isNotBlank()) {
                val newPasswordEntry = "\nApplication Name: $appName\nGmail: $gmail\nPassword: $password\n"
                passwords.add(newPasswordEntry)
                passwordAdapter.notifyDataSetChanged()
                // Store the password in SharedPreferences
                val sharedPreferences = getSharedPreferences("passwords", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                val key = System.currentTimeMillis().toString()
                editor.putString(key, newPasswordEntry)
                editor.apply()
                Toast.makeText(this, "Password added succesfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Please add all the details", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton("Cancel") { _, _ -> }

        builder.show()
    }

    private fun showEditDeletePasswordDialog(position: Int) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Edit/Delete Password")

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_edit_delete_password, null)
        val passwordDisplay = dialogView.findViewById<EditText>(R.id.passwordDisplay)
        val editButton = dialogView.findViewById<Button>(R.id.editButton)
        val deleteButton = dialogView.findViewById<Button>(R.id.deleteButton)

        // Display the selected password
        passwordDisplay.setText(passwords[position])
        passwordDisplay.isEnabled = false

        builder.setView(dialogView)

        val dialog = builder.create()
        dialog.show()

        editButton.setOnClickListener {
            // Enable editing of the password
            passwordDisplay.isEnabled = true
            editButton.text = "Done" // Change button text to "Done"

            // Update the password when "Done" is clicked
            editButton.setOnClickListener {
                val editedPassword = passwordDisplay.text.toString()
                passwords[position] = editedPassword
                passwordAdapter.notifyDataSetChanged()

                // Update the edited password in SharedPreferences
                val sharedPreferences = getSharedPreferences("passwords", Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()

                val allEntries = sharedPreferences.all
                for (entry in allEntries.entries) {
                    if (entry.value == passwordDisplay.text.toString()) {
                        editor.remove(entry.key)
                        val key = System.currentTimeMillis().toString()
                        editor.putString(key, editedPassword)
                        editor.apply()
                        break
                    }
                }

                Toast.makeText(this, "Successfully Edited", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }

        deleteButton.setOnClickListener {
            // Remove the password from the list and SharedPreferences
            val passwordToDelete = passwords[position]
            passwords.removeAt(position)
            passwordAdapter.notifyDataSetChanged()

            val sharedPreferences = getSharedPreferences("passwords", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            // Find the key associated with the password and remove it
            val allEntries = sharedPreferences.all
            for (entry in allEntries.entries) {
                if (entry.value == passwordToDelete) {
                    editor.remove(entry.key)
                    editor.apply()
                    break
                }
            }

            dialog.dismiss()
            Toast.makeText(this, "Password deleted", Toast.LENGTH_SHORT).show()
        }
    }

}
