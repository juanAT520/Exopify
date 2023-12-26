package com.juan.exopify.navigation

sealed class Rutas(val ruta: String) {
    object Principal : Rutas("principal")
}