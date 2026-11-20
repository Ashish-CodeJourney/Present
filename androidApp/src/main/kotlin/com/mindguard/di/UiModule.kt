package com.mindguard.di

import com.mindguard.BuildConfig
import com.mindguard.MainViewModel
import com.mindguard.network.CompilerClient
import com.mindguard.network.HttpCompilerClient
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val uiModule = module {
    single<CompilerClient> {
        HttpCompilerClient(baseUrl = BuildConfig.PRESENT_API_URL, apiKey = BuildConfig.PRESENT_API_KEY)
    }
    viewModel { MainViewModel(get(), get(), get()) }
}
