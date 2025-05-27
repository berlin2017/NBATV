package com.berlin.nbatv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults.topAppBarColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.MaterialTheme.colorScheme
import androidx.tv.material3.Text
import com.berlin.nbatv.data.VideoItem
import com.berlin.nbatv.ui.VideoList
import com.berlin.nbatv.ui.theme.NBATVTheme

class MainActivity : ComponentActivity() {

    private val list = listOf(VideoItem(id = 0, url = "", imageUrl = "https://images.unsplash.com/photo-1575936123452-b67c3203c357?fm=jpg&q=60&w=3000&ixlib=rb-4.1.0&ixid=M3wxMjA3fDB8MHxzZWFyY2h8Mnx8aW1hZ2V8ZW58MHx8MHx8fDA%3D", name = "TV1"),
        VideoItem(id = 0, url = "", imageUrl = "", name = "TV1"),VideoItem(id = 0, url = "", imageUrl = "", name = "TV1"),
        VideoItem(id = 0, url = "", imageUrl = "", name = "TV1"),)

    @OptIn(ExperimentalTvMaterial3Api::class, ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
                NBATVTheme {
                    Scaffold(
                        topBar = {
                            TopAppBar(
                                colors = topAppBarColors(
                                    containerColor = colorScheme.primary,
                                    titleContentColor = colorScheme.primary,
                                ),
                                title = {
                                    Text("NBA")
                                }
                            )
                        },
                    ) { innerPadding ->
                        Column (modifier = Modifier.padding(innerPadding)){
                            VideoList(list)
                        }
                        }
                    }
                }
            }
    }

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    NBATVTheme {
        Greeting("Android")
    }
}