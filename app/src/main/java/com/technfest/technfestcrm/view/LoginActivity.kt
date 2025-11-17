package com.technfest.technfestcrm.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.databinding.ActivityLoginBinding
import com.technfest.technfestcrm.repository.LoginRepository
import com.technfest.technfestcrm.viewmodel.LoginViewModel
import com.technfest.technfestcrm.viewmodel.LoginViewModelFactory
import kotlin.toString

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private val viewModel : LoginViewModel by viewModels()
    {
        LoginViewModelFactory(LoginRepository())
    }
    private var email: String = ""
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        val sharedPrefs = getSharedPreferences("App_Prefs", MODE_PRIVATE)
        val isLoggedIn = sharedPrefs.getBoolean("is_logged_in", false)
        val isWorkspaceSelected = sharedPrefs.getBoolean("is_workspace_selected", false)

        if (isLoggedIn) {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("Email", sharedPrefs.getString("Email", ""))
            intent.putExtra("Name", sharedPrefs.getString("Name", ""))
            intent.putExtra("Token", sharedPrefs.getString("Token", ""))
            startActivity(intent)
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.btnLogin.setOnClickListener {
            email = binding.edtUserName.text.toString().trim()
            val password = binding.edtPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.loginUser(email, password)
        }
        viewModel.loginResult.observe(this) { result ->
            Toast.makeText(this, result, Toast.LENGTH_LONG).show()
        }

        viewModel.tokenLiveData.observe(this) {
            navigateIfReady()
        }
        viewModel.userNameLiveData.observe(this) {
            navigateIfReady()
        }


    }
    private fun navigateIfReady() {
        val token = viewModel.tokenLiveData.value
        val userName = viewModel.userNameLiveData.value

        if (!token.isNullOrEmpty() && !userName.isNullOrEmpty()) {
            val sharedPrefs = getSharedPreferences("App_Prefs", MODE_PRIVATE)
            with(sharedPrefs.edit()) {
                putBoolean("is_logged_in", true)
                putString("Token", token)
                putString("Name", userName)
                putString("Email", email)
                apply()
            }
            Log.d("TOKKKKK","$token")
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("Email", email)
            intent.putExtra("Name", userName)
            intent.putExtra("Token", token)
            startActivity(intent)
            finish()
        }
    }
}