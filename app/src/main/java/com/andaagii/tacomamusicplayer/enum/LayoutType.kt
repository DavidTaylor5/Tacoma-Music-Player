package com.andaagii.tacomamusicplayer.enum

enum class LayoutType {
    LINEAR_LAYOUT {
        override fun type(): String {
            return "Linear"
        }
    },
    TWO_GRID_LAYOUT {
        override fun type(): String {
            return "2x2 Grid"
        }
    };

    abstract fun type(): String
}