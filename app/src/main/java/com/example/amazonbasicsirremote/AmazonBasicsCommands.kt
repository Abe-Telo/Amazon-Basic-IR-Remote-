package com.example.amazonbasicsirremote

/**
 * Command-byte lookup table for the Amazon Basics/Midea air-conditioner IR captures.
 *
 * Temperature-bearing commands are available for 62F..86F. Fan-only and toggle
 * commands use the exact six-byte payloads supplied with the captures.
 */
object AmazonBasicsCommands {
    val TemperatureRange: IntRange = RemoteState.TemperatureRange

    val TemperatureCommands: Map<TemperatureCommandKey, IntArray> = buildMap {
        for (modeFan in TemperatureModeFan.entries) {
            for (temperatureF in TemperatureRange) {
                val key = TemperatureCommandKey(modeFan, temperatureF)
                put(key, RemoteState(modeFan.mode, modeFan.fanSpeed.toRemoteStateFanSpeed(), temperatureF).toCommandBytes())
            }
        }
    }

    val FanOnlyCommands: Map<FanOnlyCommandKey, IntArray> = mapOf(
        FanOnlyCommandKey(FanSpeed.AUTO) to intArrayOf(161, 164, 126, 255, 255, 91),
        FanOnlyCommandKey(FanSpeed.LOW) to intArrayOf(161, 140, 126, 255, 255, 115),
        FanOnlyCommandKey(FanSpeed.MEDIUM) to intArrayOf(161, 148, 126, 255, 255, 107),
        FanOnlyCommandKey(FanSpeed.HIGH) to intArrayOf(161, 156, 126, 255, 255, 99),
    )

    val ToggleCommands: Map<ToggleCommand, IntArray> = mapOf(
        ToggleCommand.LED_TOGGLE to intArrayOf(162, 8, 255, 255, 255, 117),
        ToggleCommand.ENERGY_SAVER_TOGGLE to intArrayOf(162, 2, 255, 255, 255, 126),
    )

    fun temperatureCommand(modeFan: TemperatureModeFan, temperatureF: Int): IntArray =
        TemperatureCommands.getValue(TemperatureCommandKey(modeFan, temperatureF)).copyOf()

    fun fanOnlyCommand(fanSpeed: FanSpeed): IntArray =
        FanOnlyCommands.getValue(FanOnlyCommandKey(fanSpeed)).copyOf()

    fun toggleCommand(toggle: ToggleCommand): IntArray =
        ToggleCommands.getValue(toggle).copyOf()
}

data class TemperatureCommandKey(
    val modeFan: TemperatureModeFan,
    val temperatureF: Int,
) {
    init {
        require(temperatureF in AmazonBasicsCommands.TemperatureRange) {
            "temperatureF must be in ${AmazonBasicsCommands.TemperatureRange}: $temperatureF"
        }
    }
}

data class FanOnlyCommandKey(val fanSpeed: FanSpeed)

enum class TemperatureModeFan(
    internal val mode: RemoteState.Mode,
    internal val fanSpeed: FanSpeed,
) {
    AUTO_AUTO(RemoteState.Mode.AUTO, FanSpeed.AUTO),
    COOL_AUTO(RemoteState.Mode.COOL, FanSpeed.AUTO),
    DRY_AUTO(RemoteState.Mode.DRY, FanSpeed.AUTO),
    COOL_LOW(RemoteState.Mode.COOL, FanSpeed.LOW),
    COOL_MEDIUM(RemoteState.Mode.COOL, FanSpeed.MEDIUM),
    COOL_HIGH(RemoteState.Mode.COOL, FanSpeed.HIGH),
}

enum class FanSpeed {
    AUTO,
    LOW,
    MEDIUM,
    HIGH,
}

enum class ToggleCommand {
    LED_TOGGLE,
    ENERGY_SAVER_TOGGLE,
}


private fun FanSpeed.toRemoteStateFanSpeed(): RemoteState.FanSpeed = when (this) {
    FanSpeed.AUTO -> RemoteState.FanSpeed.AUTO
    FanSpeed.LOW -> RemoteState.FanSpeed.LOW
    FanSpeed.MEDIUM -> RemoteState.FanSpeed.MEDIUM
    FanSpeed.HIGH -> RemoteState.FanSpeed.HIGH
}
