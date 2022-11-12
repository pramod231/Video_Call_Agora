package com.example.videocallagora

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import live.videosdk.rtc.android.Meeting
import live.videosdk.rtc.android.Participant
import live.videosdk.rtc.android.Stream
import live.videosdk.rtc.android.lib.PeerConnectionUtils
import live.videosdk.rtc.android.listeners.MeetingEventListener
import live.videosdk.rtc.android.listeners.ParticipantEventListener
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

class ParticipantAdapter(meeting: Meeting) : RecyclerView.Adapter<ParticipantAdapter.PeerViewHolder>() {

    private var containerHeight = 0

    // creating a empty list which will store all participants
    private val participants: MutableList<Participant> = ArrayList()

    init {
        // adding the local participant(You) to the list
        participants.add(meeting.localParticipant)

        // adding Meeting Event listener to get the participant join/leave event in the meeting.
        meeting.addEventListener(object : MeetingEventListener() {
            override fun onParticipantJoined(participant: Participant) {
                // add participant to the list
                participants.add(participant)
                notifyItemInserted(participants.size - 1)
            }

            override fun onParticipantLeft(participant: Participant) {
                var pos = -1
                for (i in participants.indices) {
                    if (participants[i].id == participant.id) {
                        pos = i
                        break
                    }
                }
                // remove participant from the list
                participants.remove(participant)
                if (pos >= 0) {
                    notifyItemRemoved(pos)
                }
            }
        })
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
        containerHeight = parent.height
        return PeerViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_remote_peer, parent, false)
        )
    }

    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
        val participant = participants[position]

        val layoutParams = holder.itemView.layoutParams
        layoutParams.height = containerHeight / 3
        holder.itemView.layoutParams = layoutParams

        holder.tvName.text = participant.displayName

        // adding the initial video stream for the participant into the 'SurfaceViewRenderer'
        for ((_, stream) in participant.streams) {
            if (stream.kind.equals("video", ignoreCase = true)) {
                holder.svrParticipant.visibility = View.VISIBLE
                val videoTrack = stream.track as VideoTrack
                videoTrack.addSink(holder.svrParticipant)
                break
            }
        }

        // add Listener to the participant which will update start or stop the video stream of that participant
        participant.addEventListener(object : ParticipantEventListener() {
            override fun onStreamEnabled(stream: Stream) {
                if (stream.kind.equals("video", ignoreCase = true)) {
                    holder.svrParticipant.visibility = View.VISIBLE
                    val videoTrack = stream.track as VideoTrack
                    videoTrack.addSink(holder.svrParticipant)
                }
            }

            override fun onStreamDisabled(stream: Stream) {
                if (stream.kind.equals("video", ignoreCase = true)) {
                    val track = stream.track as VideoTrack
                    track?.removeSink(holder.svrParticipant)
                    holder.svrParticipant.clearImage()
                    holder.svrParticipant.visibility = View.GONE
                }
            }
        })
    }

    override fun getItemCount(): Int {
        return participants.size
    }

    class PeerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var svrParticipant: SurfaceViewRenderer = view.findViewById(R.id.svrParticipant)
        var tvName: TextView = view.findViewById(R.id.tvName)

        init{
            svrParticipant.init(PeerConnectionUtils.getEglContext(), null)
        }
    }
}