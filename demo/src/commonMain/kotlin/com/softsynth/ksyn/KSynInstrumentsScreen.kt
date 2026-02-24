package com.softsynth.ksyn

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.mobileer.audiobridge.AudioResult
import com.softsynth.ksyn.instruments.KSynInstrumentLibrary
import com.softsynth.ksyn.unitgen.*

class KSynInstrumentsPlayer : KSynPlayable {
    val ksynAudioBridge: KSynAudioBridge
    val synth = KSyn.createSynthesizer()
    val lineOut = LineOut()

    private val library = KSynInstrumentLibrary()
    
    // We instantiate one copy of each voice from the descriptions
    val voices: List<Pair<String, UnitVoice>> = library.voiceDescriptions.map { desc ->
        Pair(desc.name, desc.createUnitVoice())
    }

    var activeVoiceIndex by mutableStateOf(0)

    val activeVoice: UnitVoice
        get() = voices[activeVoiceIndex].second

    init {
        ksynAudioBridge = KSynAudioBridge(synth)
        synth.add(lineOut)

        // Add all voices to the synthesizer to ensure they are available
        for (voice in voices) {
            val voiceGen = voice.second as? UnitGenerator
            if (voiceGen != null) {
                synth.add(voiceGen)
                val ampPort = voiceGen.getPortByName("Amplitude") as? com.softsynth.ksyn.ports.UnitInputPort
                ampPort?.set(0.1)
            }
        }

        updateRouting()
        lineOut.start()
    }

    fun updateRouting() {
        // Disconnect all previous voice mappings to clear the lineOut
        for (voice in voices) {
            val output = voice.second.getOutputPort()
            output.disconnectAll()
        }

        // Connect the actively selected voice to the stereo LineOut
        val voiceOutput = activeVoice.getOutputPort()
        val numParts = voiceOutput.numParts
        if (numParts == 1) {
            // Mono: Connect output 0 to both left and right
            voiceOutput.connect(0, lineOut.input, 0)
            voiceOutput.connect(0, lineOut.input, 1)
        } else if (numParts >= 2) {
            // Stereo (or more): Connect output 0 to left, output 1 to right
            voiceOutput.connect(0, lineOut.input, 0)
            voiceOutput.connect(1, lineOut.input, 1)
        }
    }

    // Playable Methods
    override fun start(): AudioResult {
        return ksynAudioBridge.start()
    }

    override fun stop() {
        ksynAudioBridge.stop()
    }
}

class KSynInstrumentsScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val player = remember { KSynInstrumentsPlayer() }
        var isPlaying by remember { mutableStateOf(false) }

        Scaffold { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
            ) {
                Button(onClick = { navigator.pop() }) {
                    Text("Go Back")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    Button(
                        onClick = {
                            if (player.start() == AudioResult.OK) isPlaying = true
                        },
                        enabled = !isPlaying
                    ) { Text("START AUDIO") }

                    Button(
                        onClick = {
                            player.stop()
                            isPlaying = false
                        },
                        enabled = isPlaying
                    ) { Text("STOP AUDIO") }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Voice Selection Menu
                Text("Select Instrument", style = MaterialTheme.typography.titleMedium)
                var voiceMenuExpanded by remember { mutableStateOf(false) }
                
                ExposedDropdownMenuBox(
                    expanded = voiceMenuExpanded,
                    onExpandedChange = { voiceMenuExpanded = !voiceMenuExpanded }
                ) {
                    TextField(
                        readOnly = true,
                        value = player.voices[player.activeVoiceIndex].first,
                        onValueChange = {},
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = voiceMenuExpanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier.menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = voiceMenuExpanded,
                        onDismissRequest = { voiceMenuExpanded = false }
                    ) {
                        player.voices.forEachIndexed { index, pair ->
                            DropdownMenuItem(
                                text = { Text(pair.first) },
                                onClick = {
                                    player.activeVoiceIndex = index
                                    player.updateRouting()
                                    voiceMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Keyboard Input
                BlackWhiteKeyboard(
                    onNoteOn = { frequency ->
                        player.activeVoice.noteOn(
                            frequency = frequency,
                            amplitude = 0.5,
                            timeStamp = com.softsynth.ksyn.shared.time.TimeStamp(player.synth.currentTime)
                        )
                    },
                    onNoteOff = {
                        player.activeVoice.noteOff(
                            timeStamp = com.softsynth.ksyn.shared.time.TimeStamp(player.synth.currentTime)
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                // Active Voice Faders
                // Wrap in a Column/Box that limits size or allows scrolling natively if preferred.
                // Using weight to let the faders take available remaining vertical space while keeping the keyboard visible.
                Box(modifier = Modifier.weight(1f)) {
                    val activeGenerator = player.activeVoice as? UnitGenerator
                    if (activeGenerator != null) {
                        UnitGeneratorFaders(
                            unitGenerator = activeGenerator,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }

        DisposableEffect(Unit) {
            onDispose {
                player.stop()
            }
        }
    }
}
