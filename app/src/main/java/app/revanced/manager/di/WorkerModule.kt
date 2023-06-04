package app.revanced.manager.di

import app.revanced.manager.patcher.worker.PatcherWorker
import org.koin.androidx.workmanager.dsl.workerOf
import org.koin.dsl.module

val workerModule = module {
    workerOf(::PatcherWorker)
}