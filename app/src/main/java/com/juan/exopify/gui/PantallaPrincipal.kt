package com.juan.exopify.gui

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.juan.exopify.R
import com.juan.exopify.dataBase.Cancion
import com.juan.exopify.dataBase.leerArchivo
import com.juan.exopify.ui.theme.AlmostBlack
import com.juan.exopify.ui.theme.lime_Green
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

class MiViewModel : ViewModel() {
    private val _listaMusica = MutableStateFlow<List<Cancion>>(listOf())
    val listaMusica = _listaMusica.asStateFlow()
    private val _estaAbierto = MutableStateFlow(false)
    val estaAbierto = _estaAbierto.asStateFlow()
    private val _caratulaCancionActual = MutableStateFlow(R.drawable.music1)
    val caratulaCancionActual = _caratulaCancionActual.asStateFlow()
    private val _tituloCancionActual = MutableStateFlow("")
    val tituloCancionActual = _tituloCancionActual.asStateFlow()
    private val _grupoCancionActual = MutableStateFlow("")
    val grupoCancionActual = _grupoCancionActual.asStateFlow()
    private val _exoPlayer: MutableStateFlow<ExoPlayer?> = MutableStateFlow(null)
    val exoPlayer = _exoPlayer.asStateFlow()
    private val _numeroAleatorio = MutableStateFlow(0)
    val numeroAleatorio = _numeroAleatorio.asStateFlow()
    private val _indiceCancionActual = MutableStateFlow(0)
    val indiceCancionActual = _indiceCancionActual.asStateFlow()
    private val _playlistActual = MutableStateFlow(1)
    val playlistActual = _playlistActual.asStateFlow()
    private val _posicionActual = MutableStateFlow(0L)
    val posicionActual = _posicionActual.asStateFlow()
    private val _duracionTotal = MutableStateFlow(0L)
    val duracionTotal = _duracionTotal.asStateFlow()
    private var _mediaItem: MediaItem? = null

    private fun crearExoPlayer(context: Context) {
        _exoPlayer.value?.release()
        _exoPlayer.value = ExoPlayer.Builder(context).build()
        _exoPlayer.value!!.prepare()
    }

    fun cambiaIconoPlay(): Int {
        if (_exoPlayer.value == null) {
            return R.drawable.play
        } else {
            return if (_exoPlayer.value!!.isPlaying) R.drawable.pause
            else R.drawable.play
        }
    }

    fun setearPlaylist(context: Context, numero: Int) {
        _playlistActual.value = numero
        _listaMusica.value = leerArchivo(context, numero)
    }

    fun resetearTituloCancionActual() {
        _tituloCancionActual.value = ""
    }

    fun cambiaCancion(index: Int) {
        _indiceCancionActual.value = index
    }

    fun cambiaEstado(cancion: Cancion) {
        _caratulaCancionActual.value = cancion.portada
        _tituloCancionActual.value = cancion.titulo
        _grupoCancionActual.value = cancion.grupo
        _indiceCancionActual.value = cancion.indice
    }

    fun cambiaPosicion(nuevaPosicion: Long) {
        _exoPlayer.value?.seekTo(nuevaPosicion)
        _posicionActual.value = nuevaPosicion
    }

    fun cambiarPlaylist(context: Context) {
        _exoPlayer.value!!.pause()
        resetearTituloCancionActual()
        _indiceCancionActual.value = 0
        _exoPlayer.value!!.clearMediaItems()
        inicializaExoPlayer(context, _listaMusica.value[_indiceCancionActual.value].cancion)
    }

    fun abrirInfo() {
        _estaAbierto.value = !_estaAbierto.value
    }

    fun inicializaExoPlayer(context: Context, cancion: Int) {
        if (_exoPlayer.value == null) {
            crearExoPlayer(context)
        }
        if (_mediaItem == null || cancion != _indiceCancionActual.value) {
            _mediaItem = MediaItem.fromUri(obtenerURI(context, cancion))
            _exoPlayer.value!!.setMediaItem(_mediaItem!!)
            _exoPlayer.value!!.prepare()
        }
        _exoPlayer.value!!.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)
                if (state == ExoPlayer.STATE_READY) {
                    _duracionTotal.value = _exoPlayer.value!!.duration
                } else if (state == ExoPlayer.STATE_ENDED) {
                    if (_indiceCancionActual.value < _listaMusica.value.size - 1) {
                        cambiaCancion(_indiceCancionActual.value + 1)
                        cambiaEstado(_listaMusica.value[_indiceCancionActual.value])
                        pausaYContinua()
                    }
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)
                if (isPlaying) {
                    viewModelScope.launch {
                        while (_exoPlayer.value!!.isPlaying) {
                            _posicionActual.value = _exoPlayer.value!!.currentPosition
                            delay(1000)
                        }
                    }
                }
            }
        })
    }


    fun reproduceCancion() {
        _exoPlayer.value!!.playWhenReady = true
        _exoPlayer.value!!.play()
    }

    fun pausaYContinua() {
        if (_exoPlayer.value == null) {
            reproduceCancion()
        } else {
            if (_exoPlayer.value!!.isPlaying) {
                _exoPlayer.value!!.pause()
            } else {
                _exoPlayer.value!!.playWhenReady = !_exoPlayer.value!!.isPlaying
            }
        }
    }

    fun cancionAleatoria() {
        _numeroAleatorio.value = Random.nextInt(0, listaMusica.value.size)
    }

    private fun obtenerURI(context: Context, idCancion: Int): Uri {
        return Uri.parse(
            ContentResolver.SCHEME_ANDROID_RESOURCE + "://" +
                    context.resources.getResourcePackageName(idCancion) + '/' +
                    context.resources.getResourceTypeName(idCancion) + '/' +
                    context.resources.getResourceEntryName(idCancion)
        )
    }
}

@Composable
fun PantallaPrincipal(drawerState: DrawerState, scope: CoroutineScope) {
    val miViewModel: MiViewModel = viewModel()
    val context = LocalContext.current
    miViewModel.setearPlaylist(context, miViewModel.playlistActual.value)
    val listaMusica = miViewModel.listaMusica.collectAsState().value
    val caratulaActual = miViewModel.caratulaCancionActual.collectAsState().value
    val tituloActual = miViewModel.tituloCancionActual.collectAsState().value
    val grupoActual = miViewModel.grupoCancionActual.collectAsState().value
    miViewModel.inicializaExoPlayer(
        context,
        listaMusica[miViewModel.indiceCancionActual.value].cancion
    )
    Box {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                Column(
                    modifier = Modifier
                        .background(AlmostBlack)
                        .fillMaxHeight()
                ) {
                    TextButton(
                        onClick = {
                            miViewModel.setearPlaylist(context, 1)
                            miViewModel.cambiarPlaylist(context)
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        content = {
                            Text(
                                text = "Playlist meme",
                                modifier = Modifier
                                    .padding(70.dp, 15.dp)
                            )
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = lime_Green
                        )
                    )
                    TextButton(
                        onClick = {
                            miViewModel.setearPlaylist(context, 2)
                            miViewModel.cambiarPlaylist(context)
                            scope.launch {
                                drawerState.close()
                            }
                        },
                        content = {
                            Text(
                                text = "Playlist buena",
                                modifier = Modifier
                                    .padding(70.dp, 15.dp)
                            )
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = lime_Green
                        )
                    )
                }
            },
            content = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    LazyColumn {
                        items(listaMusica) { cancion ->
                            tarjetaMusica(
                                cancion,
                                miViewModel
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
                infoCancion(miViewModel, caratulaActual, tituloActual, grupoActual, scope)
            }
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(lime_Green)
            ) {
                reproductorPrincipal(miViewModel, scope)
            }
        }
    }
}

@Composable
private fun tarjetaMusica(
    cancion: Cancion,
    miViewModel: MiViewModel
) {
    ElevatedCard(
        modifier = Modifier
            .padding(10.dp)
            .clickable {
                miViewModel.cambiaEstado(cancion)
                miViewModel.abrirInfo()
            }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = cancion.portada),
                contentDescription = "imagen ${cancion.titulo}",
                modifier = Modifier
                    .weight(1f)
                    .height(76.dp)
                    .clip(RoundedCornerShape(24.dp))
            )
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .weight(2f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = cancion.titulo,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp
                )
                Text(
                    text = cancion.grupo,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp
                )
                Text(
                    text = "Duración: ${cancion.duracion}",
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun infoCancion(
    miViewModel: MiViewModel,
    caratulaActual: Int,
    tituloActual: String,
    grupoActual: String,
    scope: CoroutineScope
) {
    val estadoMenu = rememberModalBottomSheetState(false)
    val estaAbierto = miViewModel.estaAbierto.collectAsState().value
    val posicionActual = miViewModel.posicionActual.collectAsState().value
    val duracionTotal = miViewModel.duracionTotal.collectAsState().value
    val posicionActualSegundos = (miViewModel.posicionActual.collectAsState().value / 1000).toInt()
    val duracionTotalSegundos = (miViewModel.duracionTotal.collectAsState().value / 1000).toInt()

    val posicionActualMinutos = posicionActualSegundos / 60
    val posicionActualSegundosRestantes = posicionActualSegundos % 60

    val duracionTotalMinutos = duracionTotalSegundos / 60
    val duracionTotalSegundosRestantes = duracionTotalSegundos % 60

    if (estaAbierto) {
        ModalBottomSheet(
            onDismissRequest = { miViewModel.abrirInfo() },
            sheetState = estadoMenu
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .height(750.dp)
                    .fillMaxSize()
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = caratulaActual),
                        contentDescription = "Imagen canción",
                        modifier = Modifier
                            .size(400.dp)
                    )
                    Text(
                        text = tituloActual,
                        fontSize = 30.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(15.dp)
                    )
                    Text(
                        text = grupoActual,
                        fontSize = 20.sp
                    )
                }
                Column {
                    Slider(
                        value = posicionActual.toFloat(),
                        onValueChange = { newValue ->
                            miViewModel.cambiaPosicion(newValue.toLong())
                        },
                        valueRange = 0f..duracionTotal.toFloat(),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = String.format(
                                "%02d:%02d",
                                posicionActualMinutos,
                                posicionActualSegundosRestantes
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Text(
                            text = String.format(
                                "%02d:%02d",
                                duracionTotalMinutos,
                                duracionTotalSegundosRestantes
                            ),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    controlesReproduccion(miViewModel, scope)
                }
            }
        }
    }
}

@Composable
private fun iconoReproductor(icono: Int, iconSize: Dp, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        Modifier.padding(10.dp)
    ) {
        Icon(
            painter = painterResource(id = icono),
            contentDescription = "Icono canción aleatoria",
            Modifier.size(iconSize)
        )
    }
}

@Composable
private fun reproductorPrincipal(
    miViewModel: MiViewModel,
    scope: CoroutineScope
) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable {
            miViewModel.abrirInfo()
        }) {
        Text(
            text = miViewModel.tituloCancionActual.value,
            fontSize = 25.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 15.dp)
        )
        controlesReproduccion(miViewModel, scope)
    }
}

@Composable
private fun controlesReproduccion(miViewModel: MiViewModel, scope: CoroutineScope) {
    val bucleActivo = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    Row {
        iconoReproductor(icono = R.drawable.shuffle, 24.dp) {
            miViewModel.cancionAleatoria()
            miViewModel.cambiaCancion(miViewModel.numeroAleatorio.value)
            miViewModel.cambiaEstado(miViewModel.listaMusica.value[miViewModel.indiceCancionActual.value])
            miViewModel.reproduceCancion()
        }
        iconoReproductor(icono = R.drawable.previous, 48.dp) {
            if (miViewModel.indiceCancionActual.value > 0) {
                miViewModel.cambiaCancion(miViewModel.indiceCancionActual.value - 1)
                miViewModel.cambiaEstado(miViewModel.listaMusica.value[miViewModel.indiceCancionActual.value])
                miViewModel.reproduceCancion()
            }
        }
        iconoReproductor(icono = miViewModel.cambiaIconoPlay(), 72.dp) {
            miViewModel.cambiaEstado(miViewModel.listaMusica.value[miViewModel.indiceCancionActual.value])
            miViewModel.pausaYContinua()

        }
        iconoReproductor(icono = R.drawable.next, 48.dp) {
            if (miViewModel.indiceCancionActual.value < miViewModel.listaMusica.value.size - 1) {
                miViewModel.cambiaCancion(miViewModel.indiceCancionActual.value + 1)
                miViewModel.cambiaEstado(miViewModel.listaMusica.value[miViewModel.indiceCancionActual.value])
                miViewModel.reproduceCancion()
            }
        }
        iconoReproductor(icono = R.drawable.repeat, 24.dp) {
            if (bucleActivo.value) {
                miViewModel.exoPlayer.value!!.repeatMode = Player.REPEAT_MODE_OFF
                bucleActivo.value = false
                scope.launch {
                    snackbarHostState.showSnackbar("La reproducción en bucle se ha desactivado.")
                }
            } else {
                miViewModel.exoPlayer.value!!.repeatMode = Player.REPEAT_MODE_ONE
                bucleActivo.value = true
                scope.launch {
                    snackbarHostState.showSnackbar("Las canciones se van a repetir en bucle hasta que pulses el botón de nuevo.")
                }
            }
        }
    }
    SnackbarHost(hostState = snackbarHostState)
}

