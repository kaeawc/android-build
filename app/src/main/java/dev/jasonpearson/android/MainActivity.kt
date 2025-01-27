/*
 * MIT License
 *
 * Copyright (c) 2023 Jason Pearson
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package dev.jasonpearson.android

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import dev.jasonpearson.android.ui.theme.AndroidTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AndroidTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background) {
                        Column {
                            Greeting("Android")
                            val androidVersion = Build.VERSION.SDK_INT
                            val targetSdkVersion = getTargetSdkVersion()

                            when (androidVersion.compareTo(targetSdkVersion)) {
                                0 ->
                                    DescribeVersion("The codebase is correctly targeting this device!")
                                -1 ->
                                    DescribeVersion("Device could be upgraded to target $targetSdkVersion SDK version.")
                                else ->
                                    DescribeVersion("Codebase should be upgraded to a more recent target version $targetSdkVersion.")
                            }
                        }
                    }
            }
        }
    }

    /**
     * Retrieves the target SDK version of the current application.
     *
     * @return The target SDK version as an integer. Returns -1 if unable to retrieve.
     */
    private fun getTargetSdkVersion(): Int {
        return try {
            packageManager.getPackageInfo(packageName, 0)
                .applicationInfo
                ?.targetSdkVersion ?: -1

        } catch (e: PackageManager.NameNotFoundException) {
            -1
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Composable
fun DescribeVersion(version: String, modifier: Modifier = Modifier) {
    Text(
        text = version,
        style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.5.sp),
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidTheme {
        Greeting("Android")
    }
}
