package com.example.amazonbasicsirremote

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import java.io.File

class MideaIrEncoderTest {
    private val encoder = MideaIrEncoder()

    @Test
    fun autoAuto62FMatchesFlipperCapture() {
        assertEncodedCommandMatchesFlipperCapture(
            flipperName = "Auto_Auto_62F",
            command = AmazonBasicsCommands.temperatureCommand(TemperatureModeFan.AUTO_AUTO, 62),
        )
    }

    @Test
    fun coolAuto72FMatchesFlipperCapture() {
        assertEncodedCommandMatchesFlipperCapture(
            flipperName = "Cool_Auto_72F",
            command = AmazonBasicsCommands.temperatureCommand(TemperatureModeFan.COOL_AUTO, 72),
        )
    }

    @Test
    fun coolLow72FMatchesFlipperCapture() {
        assertEncodedCommandMatchesFlipperCapture(
            flipperName = "Cool_Low_72F",
            command = AmazonBasicsCommands.temperatureCommand(TemperatureModeFan.COOL_LOW, 72),
        )
    }

    @Test
    fun fanAutoMatchesFlipperCapture() {
        assertEncodedCommandMatchesFlipperCapture(
            flipperName = "Fan_Auto",
            command = AmazonBasicsCommands.fanOnlyCommand(FanSpeed.AUTO),
        )
    }

    @Test
    fun ledToggleMatchesFlipperCapture() {
        assertEncodedCommandMatchesFlipperCapture(
            flipperName = "LED_Toggle",
            command = AmazonBasicsCommands.toggleCommand(ToggleCommand.LED_TOGGLE),
        )
    }

    @Test
    fun energySaverToggleMatchesFlipperCapture() {
        assertEncodedCommandMatchesFlipperCapture(
            flipperName = "Energy_Saver_Toggle",
            command = AmazonBasicsCommands.toggleCommand(ToggleCommand.ENERGY_SAVER_TOGGLE),
        )
    }

    private fun assertEncodedCommandMatchesFlipperCapture(flipperName: String, command: IntArray) {
        val expected = flipperRawDataByName(flipperName)
        val actual = encoder.encodeCommand(command)

        assertArrayEquals("Encoded pulses should match the MSB Flipper capture for $flipperName", expected, actual)
    }

    private fun flipperRawDataByName(name: String): IntArray {
        val lines = msbFlipperFile.readLines()
        val nameLineIndex = lines.indexOf("name: $name")
        require(nameLineIndex >= 0) { "No Flipper capture named $name in ${msbFlipperFile.path}" }

        val dataLine = lines.drop(nameLineIndex + 1).first { it.startsWith("data: ") }
        return dataLine.removePrefix("data: ")
            .split(' ')
            .filter(String::isNotBlank)
            .map(String::toInt)
            .toIntArray()
    }

    private companion object {
        private val msbFlipperFile: File by lazy {
            generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
                .map { File(it, "Midea_R09B_BGCE_AmazonBasics_MSB_Flipper.ir") }
                .firstOrNull(File::isFile)
                ?: error("Could not find Midea_R09B_BGCE_AmazonBasics_MSB_Flipper.ir from ${System.getProperty("user.dir")}")
        }
    }
}
