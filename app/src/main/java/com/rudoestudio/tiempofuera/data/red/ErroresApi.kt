package com.rudoestudio.tiempofuera.data.red

import kotlinx.serialization.json.Json
import retrofit2.HttpException

/** El `{ "error": "…" }` que manda el API en las respuestas 4xx/5xx, si viene. */
fun HttpException.mensajeApi(json: Json): String? = runCatching {
    response()?.errorBody()?.string()?.let { json.decodeFromString(ErrorDto.serializer(), it).error }
}.getOrNull()
