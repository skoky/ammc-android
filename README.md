# AMM timing app for MyLaps / AMB decoders

A timing app for Android. Compatible with MyLaps decoders (P3 protocol based):

* AMBrc Decoder - compatible, tested
* MyLaps RC4 Decoder - compatible, tested including unlocked features
* DR5 - compatible, tested. Drones racing transponders support
* AMBmx3 Decoder - compatible, tested
* TranX3 Decoder - compatible, tested
* ChipX / ProChip Decoder - compatible, tested
* BIB Decoder - compatible, not tested very well
* Vostok ID010 decoder - compatible, tested. Text based protocol, parsed in the app

All decoders connect over a LAN TCP/IP network. Not supported:

* MyLaps X2 decoders and the X2 server (discontinued)
* ProChip Smart / Championship / BibTag in TCP-server mode (discontinued). ChipX /
  ProChip devices speaking the P3 protocol over LAN work as any other AMB device
* RS232 / serial and USB connections
* Older devices based on the P98 / Enhanced protocol (AMB20, AMB130, TranX-2,
  TranXPro, Activ, PegaSys)

Thank you MyLaps for supporting this project!

## The ammc-rs library

P3 parsing and encoding is done by the ammc-rs library, shipped as a prebuilt
`libammc.so` in `ammc/src/main/jniLibs`. `AmmcBridge` is the whole JNI surface used
by the app.

The library returns JSON for `PASSING`, `VERSION`, `STATUS` and `PING` only, and
encodes `VERSION`, `NETWORKSETTINGS` and `PING` requests only. Decoder settings, GPS,
timeline, signals and loop-trigger records are logged inside the library and are not
returned to the app, so console mode shows version, status and ping data.

## Keeping the decoder connection alive

While a P3 decoder is connected the app pings it every second. An idle socket gets
dropped - WiFi power saving puts the radio to sleep, and the decoder or an access
point closes a quiet TCP connection - and the app's own decoder cleanup discards a
decoder that has been silent for 10 seconds even when its socket is still open. The
ping keeps traffic on the link in both directions; the decoder's reply counts as
activity. Vostok has no ping and is not pinged.

## Rebuilding the library

Rebuild the library from an ammc-rs checkout with:

```bash
./build_ammc_libs.sh
```

It builds `arm64-v8a` and `x86_64` with `cargo-ndk` and installs both into
`ammc/src/main/jniLibs`. See the script header for the prerequisites.
