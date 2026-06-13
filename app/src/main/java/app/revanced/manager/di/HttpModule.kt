package app.revanced.manager.di

import android.content.Context
import app.revanced.manager.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.Dns
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import java.net.Inet4Address
import java.net.InetAddress

val httpModule = module {
    fun provideHttpClient(context: Context, json: Json) = HttpClient(OkHttp) {
        engine {
            config {
                dns(object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> {
                        val addresses = Dns.SYSTEM.lookup(hostname)
                        return if (hostname == "raw.githubusercontent.com") {
                            addresses.filterIsInstance<Inet4Address>()
                        } else {
                            addresses
                        }
                    }
                })
                cache(
                    Cache(
                        context.cacheDir.resolve("cache").also { it.mkdirs() },
                        1024 * 1024 * 100
                    )
                )
                followRedirects(true)
                followSslRedirects(true)
            }
        }
        install(ContentNegotiation) {
            json(json)
        }
        install(HttpTimeout) {
            socketTimeoutMillis = 10000
        }
        install(UserAgent) {
            agent = "ReVanced-Manager/${BuildConfig.VERSION_CODE}"
        }
    }

    fun provideJson() = Json {
        encodeDefaults = true
        isLenient = true
        ignoreUnknownKeys = true
    }

    single {
        provideHttpClient(androidContext(), get())
    }
    singleOf(::provideJson)
}