# LED BLE controller protocol notes

Capture source: nRF Connect inspection of the LED strip controller profile used by common `ELK-BLEDOM` / `LED BLE` devices. If a different controller advertises different UUIDs, update this note and `LedBleCommands` before enabling writes for that hardware.

## Advertisement and GATT profile

| Field | Captured value |
| --- | --- |
| Advertised name | `ELK-BLEDOM` / `LED BLE` family name prefix |
| Address / identifier | BLE random/static address shown by nRF Connect; varies per controller, so it is not hard-coded |
| Primary service UUID | `0000fff0-0000-1000-8000-00805f9b34fb` |
| Write characteristic UUID | `0000fff3-0000-1000-8000-00805f9b34fb` |
| Write characteristic properties | `WRITE` and `WRITE_NO_RESPONSE` |
| Notify characteristic UUID | `0000fff4-0000-1000-8000-00805f9b34fb` when present |
| Notify characteristic properties | `NOTIFY` when present; not required for one-way commands |

The app should prefer `WRITE_NO_RESPONSE` for low-latency button commands when the property is present. `WRITE` is also accepted by the observed controller.

## Packet format

Commands are nine bytes:

```text
7e <opcode> <payload byte 1> <payload byte 2> <payload byte 3> <payload byte 4> <checksum> 00 ef
```

* Prefix byte: `0x7e`.
* Terminator bytes: `0x00 0xef` are required.
* Checksum: low eight bits of the sum of bytes 1 through 5, excluding the `0x7e` prefix and excluding the trailing `0x00 0xef` bytes.
* Byte order: bytes are written exactly as displayed in nRF Connect hex mode.

## Sample command bytes

| Action | Hex bytes | Notes |
| --- | --- | --- |
| Power on | `7e 04 01 00 00 00 05 00 ef` | Opcode `0x04`, state `0x01` |
| Power off | `7e 04 00 00 00 00 04 00 ef` | Opcode `0x04`, state `0x00` |
| RGB red | `7e 07 ff 00 00 00 06 00 ef` | RGB payload is red, green, blue, reserved |
| RGB green | `7e 07 00 ff 00 00 06 00 ef` | Checksum wraps at 8 bits |
| RGB blue | `7e 07 00 00 ff 00 06 00 ef` |  |
| RGB white | `7e 07 ff ff ff 00 04 00 ef` |  |
| Brightness 50% | `7e 01 32 00 00 00 33 00 ef` | Brightness byte uses `0x00` to `0x64` percent |
| Effect jump 7 colors | `7e 05 87 03 00 00 8f 00 ef` | Effect id `0x87`, speed `0x03` |
| Effect fade 7 colors | `7e 05 8a 03 00 00 92 00 ef` | Effect id `0x8a`, speed `0x03` |

All values above include checksum and terminator bytes.
