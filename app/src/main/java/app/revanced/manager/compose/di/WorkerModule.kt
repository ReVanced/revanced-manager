package app.revanced.manager.compose.di

import app.revanced.manager.compose.patcher.worker.PatcherWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module

val workerModule = module {
    workerOf(::PatcherWorker)
}