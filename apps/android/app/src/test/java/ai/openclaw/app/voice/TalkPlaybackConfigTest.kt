package ai.openclaw.app.voice

import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class TalkPlaybackConfigTest {
  @Test
  fun playbackModeFallsBackToSystemForUnknownValues() {
    assertEquals(TalkPlaybackMode.System, TalkPlaybackMode.fromConfigValue(null))
    assertEquals(TalkPlaybackMode.System, TalkPlaybackMode.fromConfigValue("bogus"))
    assertEquals(TalkPlaybackMode.Auto, TalkPlaybackMode.fromConfigValue("auto"))
  }

  @Test
  fun requestPlannerBuildsTalkSpeakParamsAndPersistsVoiceAndModel() {
    val plan =
      TalkSpeakRequestPlanner.build(
        text = "Hello from talk mode.",
        directive = TalkDirective(
          voiceId = "voice-override",
          modelId = "eleven_v3",
          speed = 1.1,
          stability = 0.5,
          similarity = 0.4,
          style = 0.2,
          speakerBoost = true,
          seed = 42,
          normalize = "on",
          language = "EN",
          outputFormat = "mp3_44100_128",
        ),
        currentVoiceId = "voice-current",
        currentModelId = "model-current",
      )

    assertEquals("voice-override", plan.nextVoiceId)
    assertEquals("eleven_v3", plan.nextModelId)
    assertEquals("Hello from talk mode.", plan.params["text"]?.jsonPrimitive?.content)
    assertEquals("voice-override", plan.params["voiceId"]?.jsonPrimitive?.content)
    assertEquals("eleven_v3", plan.params["modelId"]?.jsonPrimitive?.content)
    assertEquals("mp3_44100_128", plan.params["outputFormat"]?.jsonPrimitive?.content)
    assertEquals("1.1", plan.params["speed"]?.jsonPrimitive?.content)
    assertEquals("0.5", plan.params["stability"]?.jsonPrimitive?.content)
    assertEquals("0.4", plan.params["similarity"]?.jsonPrimitive?.content)
    assertEquals("0.2", plan.params["style"]?.jsonPrimitive?.content)
    assertEquals("true", plan.params["speakerBoost"]?.jsonPrimitive?.content)
    assertEquals("42", plan.params["seed"]?.jsonPrimitive?.content)
    assertEquals("on", plan.params["normalize"]?.jsonPrimitive?.content)
    assertEquals("en", plan.params["language"]?.jsonPrimitive?.content)
  }

  @Test
  fun requestPlannerKeepsStoredDefaultsWhenOverrideIsOnce() {
    val plan =
      TalkSpeakRequestPlanner.build(
        text = "Hi.",
        directive = TalkDirective(
          voiceId = "voice-once",
          once = true,
        ),
        currentVoiceId = "voice-default",
        currentModelId = "model-default",
      )

    assertEquals("voice-default", plan.nextVoiceId)
    assertEquals("model-default", plan.nextModelId)
    assertEquals("voice-once", plan.params["voiceId"]?.jsonPrimitive?.content)
    assertEquals("model-default", plan.params["modelId"]?.jsonPrimitive?.content)
  }

  @Test
  fun requestPlannerDropsInvalidProviderValues() {
    val plan =
      TalkSpeakRequestPlanner.build(
        text = "Hi.",
        directive = TalkDirective(
          modelId = "eleven_v3",
          speed = 5.0,
          stability = 0.25,
          similarity = 2.0,
          style = -1.0,
          seed = -1,
          normalize = "sometimes",
          language = "english",
        ),
        currentVoiceId = null,
        currentModelId = null,
      )

    assertFalse(plan.params.containsKey("speed"))
    assertFalse(plan.params.containsKey("stability"))
    assertFalse(plan.params.containsKey("similarity"))
    assertFalse(plan.params.containsKey("style"))
    assertFalse(plan.params.containsKey("seed"))
    assertFalse(plan.params.containsKey("normalize"))
    assertFalse(plan.params.containsKey("language"))
  }
}
