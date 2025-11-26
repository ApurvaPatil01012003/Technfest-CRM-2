package com.technfest.technfestcrm.view

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.text.method.PasswordTransformationMethod
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.technfest.technfestcrm.databinding.ActivityLoginBinding
import com.technfest.technfestcrm.repository.LoginRepository
import com.technfest.technfestcrm.viewmodel.LoginViewModel
import com.technfest.technfestcrm.viewmodel.LoginViewModelFactory
import androidx.core.content.edit
import com.technfest.technfestcrm.R

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private val viewModel: LoginViewModel by viewModels {
        LoginViewModelFactory(LoginRepository())
    }
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        if (prefs.getBoolean("is_logged_in", false)) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.edtUserName.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.loginUser(email, password)
        }

        viewModel.loginResponseLiveData.observe(this) { data ->
            if (data != null) {
                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()

                saveUserData(data)
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("FullName",data.fullName)
                intent.putExtra("workspaceId",data.workspaceId)
                intent.putExtra("Token",data.token)
                startActivity(intent)
                finish()
            }
        }

        viewModel.errorLiveData.observe(this) { msg ->
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }



        binding.edtPassword.setOnTouchListener { v, event ->
            if (event.action != MotionEvent.ACTION_UP) return@setOnTouchListener false

            val editText = binding.edtPassword
            val drawables = editText.compoundDrawablesRelative // safer for RTL
            val drawableEnd = drawables[2] // index 2 -> end drawable

            // if no drawable, nothing to do
            if (drawableEnd == null) return@setOnTouchListener false

            // calculate touch area (consider padding)
            val touchX = event.x.toInt()
            val width = editText.width
            val drawableWidth = drawableEnd.bounds.width()
            val paddingEnd = editText.paddingEnd
            val clickableStart = width - paddingEnd - drawableWidth

            if (touchX >= clickableStart) {
                // toggle visibility
                isPasswordVisible = !isPasswordVisible

                if (isPasswordVisible) {
                    // show password (remove transformation)
                    editText.transformationMethod = null
                    // change end icon to "visibility_off" (eye-off)
                    editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.baseline_lock_open_24, // start
                        0,
                        R.drawable.baseline_visibility_off_24, // end (eye-off)
                        0
                    )
                } else {
                    // hide password (apply password transformation)
                    editText.transformationMethod = PasswordTransformationMethod.getInstance()
                    // change end icon to "visibility" (eye)
                    editText.setCompoundDrawablesRelativeWithIntrinsicBounds(
                        R.drawable.baseline_lock_open_24,
                        0,
                        R.drawable.baseline_remove_red_eye_24,
                        0
                    )
                }

                // restore cursor position safely (post to ensure transformation applied)
                editText.post {
                    editText.setSelection(editText.text?.length ?: 0)
                }

                // consume the touch event
                return@setOnTouchListener true
            }

            false
        }


    }

    private fun saveUserData(data: com.technfest.technfestcrm.model.LoginResponse) {
        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        prefs.edit {
            putBoolean("is_logged_in", true)
            putString("token", data.token)
            putString("fullName", data.fullName)
            putString("userType", data.userType)
            putInt("workspaceId", data.workspaceId)
            putInt("employeeId", data.employeeId)
            Log.d("FullName",data.fullName)

            Log.d("workspaceId", data.workspaceId.toString())
        }
    }
}
