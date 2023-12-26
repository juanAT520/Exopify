package com.juan.exopify.dataBase

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.runtime.Composable
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader

@Composable
fun LeerArchivoAssetsYCrearEnMemoria(context: Context) {
    val file = File(context.filesDir, "canciones.txt")

    if (!file.exists()) {
        val assetManager = context.assets
        val inputStream = assetManager.open("canciones.txt")
        val bufferedReader = BufferedReader(InputStreamReader(inputStream))
        bufferedReader.forEachLine { linea ->
            val datos = linea.split(",")
            if (datos.size == 6) {
                val contenido =
                    "${datos[0]},${datos[1]},${datos[2]},${datos[3]},${datos[4]},${datos[5]}"
                val fileOutputStream: FileOutputStream =
                    context.openFileOutput("canciones.txt", Context.MODE_APPEND)
                fileOutputStream.write("$contenido\n".toByteArray())
                fileOutputStream.close()
            }
        }
    }
}

@SuppressLint("DiscouragedApi")
fun leerArchivo(context: Context, playlistActual: Int): MutableList<Cancion> {
    val file = File(context.filesDir, "canciones.txt")
    val inputStream = FileInputStream(file)
    val bufferedReader = BufferedReader(InputStreamReader(inputStream))
    val listaCanciones = mutableListOf<Cancion>()
    bufferedReader.forEachLine { linea ->
        val datos = linea.split(",")
        if (datos.size == 6) {
            val caratula =
                context.resources.getIdentifier(datos[3], "drawable", context.packageName)
            val musica = context.resources.getIdentifier(datos[4], "raw", context.packageName)
            if (caratula != 0 && musica != 0) {
                val cancion = Cancion(
                    datos[0],
                    datos[1],
                    datos[2],
                    caratula,
                    musica,
                    datos[5].toInt()
                )
                listaCanciones.add(cancion)
            }
        }
    }
    return hacerPlaylists(listaCanciones, playlistActual)
}

private fun hacerPlaylists(listaCanciones: MutableList<Cancion>, playlistActual: Int): MutableList<Cancion> {
    val playlist1 = mutableListOf<Cancion>()
    val playlist2 = mutableListOf<Cancion>()
    listaCanciones.forEachIndexed() { i, cancion ->
        if (i < 9) playlist1.add(cancion)
        else playlist2.add(cancion)
    }
    return if (playlistActual == 1) playlist1
    else playlist2
}
