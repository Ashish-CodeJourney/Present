package com.mindguard.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun OnboardingScreen(onContinue: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Present",
            fontSize = 32.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Text(
            "Reclaim your attention from addictive social media.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            "Tell Present what you want in plain words. It removes Reels and Shorts and keeps your DMs and search.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            "Privacy-first. No accounts. No tracking.",
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp),
            fontSize = 12.sp
        )

        Button(onClick = onContinue, modifier = Modifier.fillMaxWidth()) {
            Text("Continue")
        }
    }
}
