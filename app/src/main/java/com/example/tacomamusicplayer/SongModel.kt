package com.example.tacomamusicplayer

class SongModel {
    var aPath: String? = null
    var aName: String? = null
    var aAlbum: String? = null
    var aArtist: String? = null
    var aDuration: String? = null
    var aCdNumber: String? = null
    var aAlbumId: String? = null

    fun getaPath(): String? {
        return aPath
    }

    fun setPath(aPath: String?) {
        this.aPath = aPath
    }

    fun getaName(): String? {
        return aName
    }

    fun setName(aName: String?) {
        this.aName = aName
    }

    fun getaAlbum(): String? {
        return aAlbum
    }

    fun setAlbum(aAlbum: String?) {
        this.aAlbum = aAlbum
    }

    fun getaArtist(): String? {
        return aArtist
    }

    fun setArtist(aArtist: String?) {
        this.aArtist = aArtist
    }

    fun setDuration(aDuration: String?) {
        this.aDuration = aDuration
    }

    fun getDuration(): String? {
        return this.aDuration
    }

    fun setCdNumber(aCdNumber: String?) {
        this.aCdNumber = aCdNumber
    }

    fun setAlbumId(albumId: String?) {
        this.aAlbumId = albumId
    }

    fun getAlbumId(): String? {
        return this.aAlbumId
    }
}