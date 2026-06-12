package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class FloatingBubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: FrameLayout? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private lateinit var bubbleLayout: LinearLayout
    private lateinit var expandedLayout: LinearLayout
    
    private lateinit var originalTextTv: TextView
    private lateinit var translatedTextTv: TextView
    private lateinit var speakerBtn: Button
    private lateinit var toggleTranslationBtn: Button
    private lateinit var statusDot: View

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        createNotificationChannel()
        startForeground(152, createNotification())

        setupFloatingView()
        observeTranslationState()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupFloatingView() {
        val root = FrameLayout(this)
        floatingView = root

        val density = resources.displayMetrics.density

        // Create main container (expanded)
        expandedLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            visibility = View.GONE
            setPadding((12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt(), (12 * density).toInt())
            
            // Slate dark card drawable with rounded corner
            background = GradientDrawable().apply {
                setColor(0xEE0F172A.toInt()) // Sleek Slate 900 semi transparent
                cornerRadius = 24 * density
                setStroke((1.5f * density).toInt(), 0xFF14FFEC.toInt()) // Cyber Teal border
            }
        }

        // Expanded text and controls
        val titleText = TextView(this).apply {
            text = "VoxBridge - Call Translator"
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, android.graphics.Typeface.BOLD)
            gravity = Gravity.CENTER_HORIZONTAL
        }
        expandedLayout.addView(titleText)

        // Horizontal divider
        val divider = View(this).apply {
            background = GradientDrawable().apply { setColor(0x55FFFFFF) }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (1 * density).toInt()
            ).apply {
                setMargins(0, (8 * density).toInt(), 0, (8 * density).toInt())
            }
        }
        expandedLayout.addView(divider)

        // Subtitles Original/Translated
        originalTextTv = TextView(this).apply {
            text = "En attente d'écoute..."
            setTextColor(0xFF94A3B8.toInt()) // Cool grey
            textSize = 13f
            maxLines = 2
        }
        expandedLayout.addView(originalTextTv)

        translatedTextTv = TextView(this).apply {
            text = "La traduction apparaîtra ici."
            setTextColor(0xFF14FFEC.toInt()) // Neon Cyber Teal
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, (4 * density).toInt(), 0, (8 * density).toInt())
            }
        }
        expandedLayout.addView(translatedTextTv)

        // Actions Row
        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        speakerBtn = Button(this).apply {
            text = "Parleur: Moi"
            textSize = 11f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(0xFF1E293B.toInt())
                cornerRadius = 16 * density
            }
            setPadding((12 * density).toInt(), 0, (12 * density).toInt(), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (36 * density).toInt()
            ).apply {
                setMargins(0, 0, (8 * density).toInt(), 0)
            }
            setOnClickListener {
                LiveTranslatorManager.toggleSpeaker()
            }
        }
        actionsRow.addView(speakerBtn)

        toggleTranslationBtn = Button(this).apply {
            text = "Mic ON"
            textSize = 11f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(0xFFE11D48.toInt()) // Crimson Rose
                cornerRadius = 16 * density
            }
            setPadding((12 * density).toInt(), 0, (12 * density).toInt(), 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                (36 * density).toInt()
            )
            setOnClickListener {
                if (LiveTranslatorManager.isListening.value) {
                    LiveTranslatorManager.stopListening()
                } else {
                    LiveTranslatorManager.startListeningCurrentSpeaker()
                }
            }
        }
        actionsRow.addView(toggleTranslationBtn)

        expandedLayout.addView(actionsRow)

        // Create Bubble Layout
        bubbleLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding((8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt(), (8 * density).toInt())
            
            background = GradientDrawable().apply {
                setColor(0xFF0F172A.toInt())
                shape = GradientDrawable.OVAL
                setStroke((2 * density).toInt(), 0xFF14FFEC.toInt()) // Glowing border
            }
        }

        // Status indicator dot
        statusDot = View(this).apply {
            background = GradientDrawable().apply {
                setColor(0xFF10B981.toInt()) // Emerald active dots
                shape = GradientDrawable.OVAL
            }
            layoutParams = LinearLayout.LayoutParams((8 * density).toInt(), (8 * density).toInt()).apply {
                setMargins(0, 0, (4 * density).toInt(), 0)
            }
        }
        bubbleLayout.addView(statusDot)

        // Voice waves icon
        val icon = ImageView(this).apply {
            layoutParams = FrameLayout.LayoutParams((28 * density).toInt(), (28 * density).toInt())
            // Let's create an elegant local icon vector dynamically
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(0xFF1E293B.toInt())
            }
            setPadding(4, 4, 4, 4)
            setImageResource(android.R.drawable.ic_btn_speak_now)
        }
        bubbleLayout.addView(icon)

        // Add both bubble and card to the root FrameLayout
        root.addView(bubbleLayout, FrameLayout.LayoutParams((56 * density).toInt(), (56 * density).toInt()))
        root.addView(expandedLayout, FrameLayout.LayoutParams((250 * density).toInt(), FrameLayout.LayoutParams.WRAP_CONTENT))

        // Window Manager Params
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 80
            y = 150
        }

        // Gesture Drag & Double Tap to Expand
        bubbleLayout.setOnTouchListener(object : View.OnTouchListener {
            private var lastAction: Int = 0
            private var initialX: Int = 0
            private var initialY: Int = 0
            private var initialTouchX: Float = 0f
            private var initialTouchY: Float = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        lastAction = event.action
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val diffX = event.rawX - initialTouchX
                        val diffY = event.rawY - initialTouchY
                        if (Math.abs(diffX) < 10 && Math.abs(diffY) < 10) {
                            // Clicked! Toggle Expanded state
                            toggleExpandView()
                        }
                        lastAction = event.action
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = initialX + (event.rawX - initialTouchX).toInt()
                        params.y = initialY + (event.rawY - initialTouchY).toInt()
                        windowManager.updateViewLayout(root, params)
                        lastAction = event.action
                        return true
                    }
                }
                return false
            }
        })

        // Dismiss card if clicked outside
        expandedLayout.setOnClickListener {
            toggleExpandView() // Collapse on tap card
        }

        windowManager.addView(root, params)
    }

    private fun toggleExpandView() {
        if (expandedLayout.visibility == View.VISIBLE) {
            expandedLayout.visibility = View.GONE
            bubbleLayout.visibility = View.VISIBLE
        } else {
            expandedLayout.visibility = View.VISIBLE
            bubbleLayout.visibility = View.GONE
        }
    }

    private fun observeTranslationState() {
        serviceScope.launch {
            LiveTranslatorManager.currentOriginalText.collect { text ->
                originalTextTv.text = if (text.isBlank()) "Écoute..." else text
            }
        }

        serviceScope.launch {
            LiveTranslatorManager.currentTranslatedText.collect { text ->
                translatedTextTv.text = if (text.isBlank()) "La traduction apparaîtra ici." else text
            }
        }

        serviceScope.launch {
            LiveTranslatorManager.currentSpeaker.collect { speaker ->
                speakerBtn.text = if (speaker == "ME") "Parleur: Moi 🗣️" else "Parleur: Autre 👥"
                speakerBtn.background = GradientDrawable().apply {
                    setColor(if (speaker == "ME") 0xFF0284C7.toInt() else 0xFF7C3AED.toInt()) // Deep sky vs royal purple
                    cornerRadius = 16 * resources.displayMetrics.density
                }
            }
        }

        serviceScope.launch {
            LiveTranslatorManager.isListening.collect { active ->
                toggleTranslationBtn.text = if (active) "Micro ON 🎧" else "Micro OFF 🔇"
                toggleTranslationBtn.background = GradientDrawable().apply {
                    setColor(if (active) 0xFF059669.toInt() else 0xFFDC2626.toInt()) // Green if actively listening else Red
                    cornerRadius = 16 * resources.displayMetrics.density
                }
                
                // Color change bubble status dot
                statusDot.background = GradientDrawable().apply {
                    setColor(if (active) 0xFF10B981.toInt() else 0xFFEF4444.toInt())
                    shape = GradientDrawable.OVAL
                }
            }
        }
    }

    private fun createNotification(): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = android.app.PendingIntent.getActivity(
            this, 0, intent,
            android.app.PendingIntent.FLAG_IMMUTABLE or android.app.PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, "voxbridge_channel")
            .setContentTitle("Traducteur d'Appels Live")
            .setContentText("Superposition d'appel active. Traduction prête.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "voxbridge_channel", "Service Traduction d'Appels",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notification de superposition de traduction d'appels"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        floatingView?.let { windowManager.removeView(it) }
        super.onDestroy()
    }
}
