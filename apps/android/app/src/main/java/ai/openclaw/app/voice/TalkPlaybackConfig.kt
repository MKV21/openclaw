package ai.openclaw.app.voice

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

internal enum class TalkPlaybackMode(val configValue: String) {
  System("system"),
  Auto("auto"),
  ;

  companion object {
    fun fromConfigValue(value: String?): TalkPlaybackMode {
      val normalized = value?.trim()?.lowercase()
      return entries.firstOrNull { it.configValue == normalized } ?: System
    }
  }
}

internal data class TalkSpeakRequestPlan(
  val params: JsonObject,
  val nextVoiceId: String?,
  val nextModelId: String?,
)

internal object TalkSpeakRequestPlanner {
  fun build(
    text: String,
    directive: TalkDirective?,
    currentVoiceId: String?,
    currentModelId: String?,
  ): TalkSpeakRequestPlan {
    val trimmedVoiceId = currentVoiceId?.trim()?.takeIf { it.isNotEmpty() }
    val trimmedModelId = currentModelId?.trim()?.takeIf { it.isNotEmpty() }
    val requestedVoiceId = directive?.voiceId?.trim()?.takeIf { it.isNotEmpty() }
    val requestedModelId = directive?.modelId?.trim()?.takeIf { it.isNotEmpty() }

    val nextVoiceId =
      when {
        directive?.voiceId != null && directive.once != true -> requestedVoiceId
        else -> trimmedVoiceId
      }
    val nextModelId =
      when {
        directive?.modelId != null && directive.once != true -> requestedModelId
        else -> trimmedModelId
      }

    val effectiveVoiceId = requestedVoiceId ?: trimmedVoiceId
    val effectiveModelId = requestedModelId ?: trimmedModelId

    val params =
      buildJsonObject {
        put("text", JsonPrimitive(text))
        effectiveVoiceId?.let { put("voiceId", JsonPrimitive(it)) }
        effectiveModelId?.let { put("modelId", JsonPrimitive(it)) }
        directive?.outputFormat?.trim()?.takeIf { it.isNotEmpty() }?.let {
          put("outputFormat", JsonPrimitive(it))
        }
        TalkPlaybackValueValidator.resolveSpeed(directive?.speed, directive?.rateWpm)?.let {
          put("speed", JsonPrimitive(it))
        }
        TalkPlaybackValueValidator.validatedStability(directive?.stability, effectiveModelId)?.let {
          put("stability", JsonPrimitive(it))
        }
        TalkPlaybackValueValidator.validatedUnit(directive?.similarity)?.let {
          put("similarity", JsonPrimitive(it))
        }
        TalkPlaybackValueValidator.validatedUnit(directive?.style)?.let {
          put("style", JsonPrimitive(it))
        }
        directive?.speakerBoost?.let { put("speakerBoost", JsonPrimitive(it)) }
        TalkPlaybackValueValidator.validatedSeed(directive?.seed)?.let {
          put("seed", JsonPrimitive(it))
        }
        TalkPlaybackValueValidator.validatedNormalize(directive?.normalize)?.let {
          put("normalize", JsonPrimitive(it))
        }
        TalkPlaybackValueValidator.validatedLanguage(directive?.language)?.let {
          put("language", JsonPrimitive(it))
        }
      }

    return TalkSpeakRequestPlan(
      params = params,
      nextVoiceId = nextVoiceId,
      nextModelId = nextModelId,
    )
  }
}

internal object TalkPlaybackValueValidator {
  fun resolveSpeed(speed: Double?, rateWpm: Int?): Double? {
    if (rateWpm != null && rateWpm > 0) {
      val resolved = rateWpm.toDouble() / 175.0
      if (resolved <= 0.5 || resolved >= 2.0) return null
      return resolved
    }
    if (speed != null) {
      if (speed <= 0.5 || speed >= 2.0) return null
      return speed
    }
    return null
  }

  fun validatedUnit(value: Double?): Double? {
    if (value == null) return null
    if (value < 0 || value > 1) return null
    return value
  }

  fun validatedStability(value: Double?, modelId: String?): Double? {
    if (value == null) return null
    val normalized = modelId?.trim()?.lowercase()
    if (normalized == "eleven_v3") {
      return if (value == 0.0 || value == 0.5 || value == 1.0) value else null
    }
    return validatedUnit(value)
  }

  fun validatedSeed(value: Long?): Long? {
    if (value == null) return null
    if (value < 0 || value > 4294967295L) return null
    return value
  }

  fun validatedNormalize(value: String?): String? {
    val normalized = value?.trim()?.lowercase() ?: return null
    return if (normalized in listOf("auto", "on", "off")) normalized else null
  }

  fun validatedLanguage(value: String?): String? {
    val normalized = value?.trim()?.lowercase() ?: return null
    if (normalized.length != 2) return null
    if (!normalized.all { it in 'a'..'z' }) return null
    return normalized
  }
}
