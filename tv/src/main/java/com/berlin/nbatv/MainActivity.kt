package com.berlin.nbatv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.berlin.nbatv.navigation.AppNavigation

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalTvMaterial3Api::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            Surface(modifier = Modifier.fillMaxSize()) { // TV Surface
                // 例如，从 ViewModel 获取，或者暂时使用静态列表
                AppNavigation(navController = navController)
            }
        }
    }
}