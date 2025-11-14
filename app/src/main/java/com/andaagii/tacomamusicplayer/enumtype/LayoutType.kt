package com.andaagii.tacomamusicplayer.enumtype

import timber.log.Timber

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

    companion object {
        fun determineLayoutFromString(layout: String): LayoutType {
            return when(layout) {
                LINEAR_LAYOUT.type() -> LINEAR_LAYOUT
                TWO_GRID_LAYOUT.type() -> TWO_GRID_LAYOUT
                else -> {
                    Timber.d("determineLayoutFromString: UNKNOWN LAYOUT TYPE, returning LINEAR_LAYOUT")
                    LINEAR_LAYOUT
                }
            }
        }
    }
}