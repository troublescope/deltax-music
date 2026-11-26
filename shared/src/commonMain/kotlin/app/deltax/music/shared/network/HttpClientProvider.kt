package app.deltax.music.shared.network

import io.ktor.client.HttpClient

expect fun createHttpClient(): HttpClient
