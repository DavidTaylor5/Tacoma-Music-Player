package com.andaagii.tacomamusicplayer.enumtype

/**
 * Strategy controlling how a new set of tracks is incorporated into the playback queue.
 *
 * Passed to `MainViewModel.addTracksSaveTrackOrder` to specify the desired queue
 * behaviour when the user taps a song, album, or playlist.
 */
enum class QueueAddType {

    /** Begin playback of the new tracks without modifying the existing queue contents. */
    QUEUE_DONT_ADD,

    /** Clear the current queue entirely, then populate it with the new tracks. */
    QUEUE_CLEAR_ADD,

    /** Append the new tracks after the last item already in the queue. */
    QUEUE_END_ADD
}
