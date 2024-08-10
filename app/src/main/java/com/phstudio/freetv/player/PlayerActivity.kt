package com.phstudio.freetv.player

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import com.phstudio.freetv.R
import com.phstudio.freetv.R.id
import com.phstudio.freetv.R.layout
import com.phstudio.freetv.databinding.ActivityPlayerBinding

class PlayerActivity : AppCompatActivity() {

    private lateinit var zoomButton: ImageButton
    private lateinit var pipButton: ImageButton
    private lateinit var screenRotateButton: ImageButton
    private lateinit var playNext: ImageButton
    private lateinit var playPrev: ImageButton
    private lateinit var backButton: ImageButton
    private lateinit var videoName: TextView
    private lateinit var unlockButton: ImageButton
    private lateinit var lockButton: ImageButton
    private lateinit var playerUnlockControls: FrameLayout
    private lateinit var playerLockControls: FrameLayout
    private lateinit var playbackSpeedButton: ImageButton
    private lateinit var audioTrackButton: ImageButton
    private lateinit var subtitleTrackButton: ImageButton

    private lateinit var gestureDetector: GestureDetector
    private lateinit var volumeProgressBar: ProgressBar
    private lateinit var volumeProgressText: TextView
    private lateinit var brightnessProgressBar: ProgressBar
    private lateinit var brightnessProgressText: TextView
    private val volumeHideHandler = Handler(Looper.getMainLooper())
    private val brightnessHideHandler = Handler(Looper.getMainLooper())

    private lateinit var binding: ActivityPlayerBinding
    private lateinit var player: ExoPlayer
    private var currentZoom: VideoZoom = VideoZoom.BEST_FIT

    @SuppressLint("UnsafeOptInUsageError")
    private lateinit var trackSelector: DefaultTrackSelector
    private var link: String? = ""

    private val isPipSupported: Boolean by lazy {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && packageManager.hasSystemFeature(
            PackageManager.FEATURE_PICTURE_IN_PICTURE
        )
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layout.activity_player)

        binding = ActivityPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        player = ExoPlayer.Builder(this).build()
        binding.playerView.player = player

        volumeProgressBar = findViewById(id.volume_progress_bar)
        volumeProgressText = findViewById(id.volume_progress_text)
        brightnessProgressBar = findViewById(id.brightness_progress_bar)
        brightnessProgressText = findViewById(id.brightness_progress_text)
        setupGestureDetector()

        zoomButton = binding.playerView.findViewById(id.btn_video_zoom)
        pipButton = binding.playerView.findViewById(id.btn_pip)
        screenRotateButton = binding.playerView.findViewById(id.screen_rotate)
        playNext = binding.playerView.findViewById(id.btn_play_next)
        playPrev = binding.playerView.findViewById(id.btn_play_prev)
        backButton = binding.playerView.findViewById(id.back_button)
        videoName = binding.playerView.findViewById(id.video_name)
        unlockButton = binding.playerView.findViewById(id.btn_unlock_controls)
        lockButton = binding.playerView.findViewById(id.btn_lock_controls)
        playerUnlockControls = binding.playerView.findViewById(id.player_unlock_controls)
        playerLockControls = binding.playerView.findViewById(id.player_lock_controls)
        playbackSpeedButton = binding.playerView.findViewById(id.btn_playback_speed)
        audioTrackButton = binding.playerView.findViewById(id.btn_audio_track)
        subtitleTrackButton = binding.playerView.findViewById(id.btn_subtitle_track)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val decorView = window.decorView
            val insetsController = decorView.windowInsetsController
            insetsController?.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            insetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        }

        if (!isPipSupported) {
            pipButton.visibility = View.GONE
        }

        val name = intent.getStringExtra("Name")
        videoName.text = name

        link = intent.getStringExtra("Url")

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                Toast.makeText(
                    this@PlayerActivity,
                    getString(R.string.NotStream),
                    Toast.LENGTH_LONG
                ).show()
                (this@PlayerActivity as? Activity)?.finish()
            }
        })
        val mediaItem = MediaItem.fromUri(link!!)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.play()

        lockButton.setOnClickListener {
            playerUnlockControls.visibility = View.INVISIBLE
            playerLockControls.visibility = View.VISIBLE
        }

        unlockButton.setOnClickListener {
            playerUnlockControls.visibility = View.VISIBLE
            playerLockControls.visibility = View.INVISIBLE
        }

        backButton.setOnClickListener {
            player.pause()
            onBackPressedDispatcher.onBackPressed()
        }

        playNext.setOnClickListener {
            player.seekTo(player.currentPosition + 5000)
        }

        playPrev.setOnClickListener {
            player.seekTo(player.currentPosition - 5000)
        }

        zoomButton.setOnClickListener {
            toggleVideoZoom()
        }

        pipButton.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isPipSupported) {
                this.enterPictureInPictureMode(updatePictureInPictureParams())
            }
        }

        screenRotateButton.setOnClickListener {
            requestedOrientation = when (resources.configuration.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                else -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            }
        }

        playbackSpeedButton.setOnClickListener {
            PlaybackSpeedControlsDialogFragment(
                currentSpeed = player.playbackParameters.speed,
                onChange = {
                    player.setPlaybackSpeed(it)
                },
            ).show(supportFragmentManager, "PlaybackSpeedSelectionDialog")
        }

        initializeTrackSelector()

        audioTrackButton.setOnClickListener {
            TrackSelectionDialogFragment(
                type = C.TRACK_TYPE_AUDIO,
                tracks = player.currentTracks,
                onTrackSelected = { player.switchTrack(C.TRACK_TYPE_AUDIO, it) },
            ).show(supportFragmentManager, "TrackSelectionDialog")
        }

        subtitleTrackButton.setOnClickListener {
            TrackSelectionDialogFragment(
                type = C.TRACK_TYPE_TEXT,
                tracks = player.currentTracks,
                onTrackSelected = { player.switchTrack(C.TRACK_TYPE_TEXT, it) },
            ).show(supportFragmentManager, "TrackSelectionDialog")
        }
    }

    private val subtitleFileLauncher = registerForActivityResult(OpenDocument()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    @OptIn(UnstableApi::class)
    private fun initializeTrackSelector() {
        trackSelector = DefaultTrackSelector(this).apply {
            setParameters(buildUponParameters().setPreferredAudioLanguage("en"))
        }
    }

    private fun Player.switchTrack(trackType: @C.TrackType Int, trackIndex: Int?) {
        if (trackIndex == null) return
        when (trackType) {
            C.TRACK_TYPE_AUDIO -> "audio"
            C.TRACK_TYPE_TEXT -> "subtitle"
            else -> throw IllegalArgumentException("Invalid track type: $trackType")
        }

        if (trackIndex < 0) {
            trackSelectionParameters = trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(trackType, true)
                .build()
        } else {
            val tracks = currentTracks.groups.filter { it.type == trackType }

            if (tracks.isEmpty() || trackIndex >= tracks.size) {
                return
            }

            val trackSelectionOverride =
                TrackSelectionOverride(tracks[trackIndex].mediaTrackGroup, 0)

            trackSelectionParameters = trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(trackType, false)
                .setOverrideForType(trackSelectionOverride)
                .build()
        }
    }

    private fun toggleVideoZoom() {
        currentZoom = when (currentZoom) {
            VideoZoom.BEST_FIT -> VideoZoom.STRETCH
            VideoZoom.STRETCH -> VideoZoom.CROP
            VideoZoom.CROP -> VideoZoom.HUNDRED_PERCENT
            VideoZoom.HUNDRED_PERCENT -> VideoZoom.BEST_FIT
        }
        applyVideoZoom(currentZoom)
    }

    @OptIn(UnstableApi::class)
    private fun applyVideoZoom(zoom: VideoZoom) {
        when (zoom) {
            VideoZoom.BEST_FIT -> {
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                zoomButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@PlayerActivity,
                        R.drawable.ic_fit_screen
                    )
                )
            }

            VideoZoom.STRETCH -> {
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                zoomButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@PlayerActivity,
                        R.drawable.ic_aspect_ratio
                    )
                )
            }

            VideoZoom.CROP -> {
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                zoomButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@PlayerActivity,
                        R.drawable.ic_crop_landscape
                    )
                )

            }

            VideoZoom.HUNDRED_PERCENT -> {
                binding.playerView.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH
                zoomButton.setImageDrawable(
                    ContextCompat.getDrawable(
                        this@PlayerActivity,
                        R.drawable.ic_width_wide
                    )
                )

            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupGestureDetector() {
        gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                e1?.let {
                    if (playerLockControls.visibility != View.VISIBLE) {
                        val screenWidth = resources.displayMetrics.widthPixels
                        if (e1.x < screenWidth / 2) {
                            adjustBrightness(-distanceY)
                        } else {
                            adjustVolume(-distanceY)
                        }
                    }
                }
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
                return true
            }

            @UnstableApi
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (binding.playerView.isControllerFullyVisible) {
                    binding.playerView.hideController()
                } else {
                    binding.playerView.showController()
                }
                return super.onSingleTapConfirmed(e)
            }
        })

        binding.playerView.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            true
        }
    }

    private fun adjustVolume(distanceY: Float) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        val volumeChange = (distanceY * 0.05f).toInt()
        val newVolume = (currentVolume - volumeChange).coerceIn(0, maxVolume)
        audioManager.setStreamVolume(
            AudioManager.STREAM_MUSIC,
            newVolume,
            AudioManager.FLAG_SHOW_UI
        )

        updateVolumeUI()
    }

    private fun adjustBrightness(distanceY: Float) {
        val layoutParams = window.attributes
        val currentBrightness = layoutParams.screenBrightness

        val brightnessChange = (distanceY * 0.002f)
        val newBrightness = (currentBrightness - brightnessChange).coerceIn(0.0f, 1.0f)
        layoutParams.screenBrightness = newBrightness
        window.attributes = layoutParams

        updateBrightnessUI()
    }

    @SuppressLint("SetTextI18n")
    private fun updateVolumeUI() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        val volumePercentage = (currentVolume.toFloat() / maxVolume * 100).toInt()

        binding.volumeGestureLayout.visibility = View.VISIBLE
        volumeProgressText.text = "$volumePercentage%"
        volumeProgressBar.progress = volumePercentage
        volumeHideHandler.removeCallbacksAndMessages(null)
        volumeHideHandler.postDelayed({
            binding.volumeGestureLayout.visibility = View.GONE
        }, 2000)
    }

    @SuppressLint("SetTextI18n")
    private fun updateBrightnessUI() {
        val brightnessLevel = (window.attributes.screenBrightness * 100).toInt()

        binding.brightnessGestureLayout.visibility = View.VISIBLE
        brightnessProgressText.text = "$brightnessLevel%"
        brightnessProgressBar.progress = brightnessLevel
        brightnessHideHandler.removeCallbacksAndMessages(null)
        brightnessHideHandler.postDelayed({
            binding.brightnessGestureLayout.visibility = View.GONE
        }, 2000)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updatePictureInPictureParams(): PictureInPictureParams {
        val params: PictureInPictureParams = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .build()

        setPictureInPictureParams(params)
        return params
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        binding.playerView.useController = !isInPictureInPictureMode
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}