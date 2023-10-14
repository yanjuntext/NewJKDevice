package com.tutk.IOTC.player

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.lifecycle.LifecycleObserver
import com.tutk.IOTC.*
import com.tutk.IOTC.Camera
import com.tutk.IOTC.listener.*
import com.tutk.IOTC.status.PlayMode
import com.tutk.IOTC.status.PlaybackStatus
import com.tutk.IOTC.status.RecordStatus
import com.tutk.IOTC.status.VoiceType
import com.tutk.bean.TEvent
import com.tutk.io.getAudioCodec
import com.tutk.io.parsePlayBack
import com.tutk.io.playback
import com.tutk.io.playbackSeekToPercent
import com.tutk.utils.PermissionUtil
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.*
import kotlin.math.sqrt

/**
 * @Author: wangyj
 * @CreateDate: 2021/12/8
 * @Description:回放播放器
 */
class PlaybackMonitor @JvmOverloads constructor(
    context: Context,
    attr: AttributeSet?,
    defStyle: Int = 0
) : SurfaceView(context, attr, defStyle), LifecycleObserver, SurfaceHolder.Callback, OnIOCallback,
    OnSessionChannelCallback, OnFrameCallback {

    //播放新的视频
    private val OPT_RESTART_NEW_EVENT = 2134

    private val TAG = PlaybackMonitor::class.java.simpleName

    private var mPlayMode: PlayMode = PlayMode.PLAY_BACK
    private var mVoiceType: VoiceType = VoiceType.ONE_WAY_VOICE

    private val DEFAULT_MAX_ZOOM_SCALE = 3.0F


    private val NONE = 0
    private val DRAG = 1
    private val ZOOM = 2

    private val mPaint = Paint()
    private var nScreenWidth = 0
    private var nScreenHeight = 0

    private var mPinchedMode = NONE
    private val mStartPoint = PointF()
    private val mCurrentPoint = PointF()


    private var mOrigDist = 0f
    private var mLastZoomTime = 0L
    private var mCurrentScale = 1.0f
    private var mCurrentMaxScale = DEFAULT_MAX_ZOOM_SCALE

    private var mGestureDetector: GestureDetector? = null
    private var mSurHolder: SurfaceHolder? = null

    private var vLeft = 0
    private var vTop = 0
    private var vRight = 0
    private var vBottom = 0

    private val mRectCanvas = Rect()
    private val mRectMonitor = Rect()
    private val mMoveRect = Rect()

    private var mLastFrame: Bitmap? = null
    private var mCamera: Camera? = null
    private var mAvChannel = -1

    private var mCurVideoWidth = 0
    private var mCurVideoHeight = 0

    private var mRenderJob: Job? = null

    private val mStartClickPoint = PointF()

    private var mBitmapWidth = 0
    private var mBitmapHeight = 0


    private var isRunning = false


    private var mOnClickListener: OnClickListener? = null
    private var mDownTime: Long = 0L

    private var isFullScreen = false

    private var mAVChannelRecordStatus: IAVChannelRecordStatus? = null


    var isRecording = false

    private var mRecordEvent: TEvent? = null
    private var mRecordDuration: Int = 0
    private var mRecordIndex: Int = 0

    private var mPlaybackStatus: PlaybackStatus? = null

    private var mOnPlaybackCallback: OnPlayBackCallback? = null

    //总时长
    private var mVideoTotalTime = 0

    //已播放时长
    private var mVideoPlayTime = 0

    //音频监听开关
    private var mAudioTrackStatus: Boolean = true

    private var mPlayTimeJob: Job? = null
    private var mPlayTimeRunning = false

    /**是否使用libyuv解析图片*/
    var withYuv = false

    private var mSeekTime = 0

    private var canDraw = false
    private var mMonitorThread: MonitorThread? = null

    /**是否使用设备端返回的播放进度*/
    var userDeviceStatusBarTime = false
    private var firstStatusBarTime = -1L
    private var mCurrentDeviceStatusBar = 0

    /**是否是新的录像方式*/
    var meoofRecord: Boolean = false

    private var isSeekTo = false
    private var seekToTime = -1

    init {
        mSurHolder = holder
        mSurHolder?.addCallback(this)
        isLongClickable = true
//        mGestureDetector = GestureDetector(this)
        mGestureDetector =
            GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent,
                    e2: MotionEvent,
                    velocityX: Float,
                    velocityY: Float
                ): Boolean {
                    Liotc.d("PlaybackMonitor", "onFling 1")
                    if (mRectCanvas.left != vLeft || mRectCanvas.top != vTop || mRectCanvas.right != vRight || mRectCanvas.bottom != vBottom)
                        return false
                    Liotc.d("PlaybackMonitor", "onFling 2")
                    return false
                }

                //双击
                override fun onDoubleTap(e: MotionEvent): Boolean {
                    Liotc.d("PlaybackMonitor", "onDoubleTap")
                    if (mRectCanvas.left > 0 || mRectCanvas.right < nScreenWidth || mRectCanvas.top > 0 || mRectCanvas.bottom < nScreenHeight) {
                        _setFullScreen()
                    } else {
                        scaleToOrigin()
                    }
                    return true
                }
            })
    }

    //重新播放或者播放新的
    private var isRestartPlayback = false
    private var newRecordEvent: TEvent? = null
    private var newRecordEventDuration: Int = 0
    private var newRecordEventIndex: Int = 0

    private val handler = object : Handler(Looper.myLooper()!!) {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            Liotc.d(
                TAG,
                "restartPlayback handleMessage=${msg.what} isRestartPlayback=$isRestartPlayback"
            )

            if (msg.what == OPT_RESTART_NEW_EVENT) {
                Liotc.d(TAG, "restartPlayback handleMessage isRestartPlayback=${isRestartPlayback}")
                if (isRestartPlayback) {
                    newRecordEvent?.let { event ->
                        mRecordEvent = event
                        mRecordDuration = newRecordEventDuration
                        mRecordIndex = newRecordEventIndex
                        newRecordEvent = null
                        newRecordEventDuration = 0
                        newRecordEventIndex = 0
                        Liotc.d(TAG, "restartPlayback handleMessage mAvChannel=$mAvChannel")
                        if (mAvChannel < 0) {
                            Liotc.d(
                                TAG,
                                "restartPlayback handleMessage mAvChannel=$mAvChannel  1111"
                            )
                            mRecordEvent?.let {
                                Liotc.d(
                                    TAG,
                                    "restartPlayback handleMessage mAvChannel=$mAvChannel  mPlaybackStatus=$mPlaybackStatus  2222"
                                )
                                if (mPlaybackStatus == null || mPlaybackStatus == PlaybackStatus.ERROR) {
                                    Liotc.d(
                                        TAG,
                                        "restartPlayback handleMessage mAvChannel=$mAvChannel  mPlaybackStatus=$mPlaybackStatus  33333"
                                    )
                                    mCamera.playback(
                                        type = PlaybackStatus.START,
                                        time = it,
                                        duration = mRecordDuration,
                                        index = mRecordIndex
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }


    /**设置充满全屏*/
    private fun _setFullScreen() {
        Liotc.d("PlaybackMonitor", "_setFullScreen 1")
        if (nScreenWidth != 0 && nScreenHeight != 0) {
            Liotc.d("PlaybackMonitor", "_setFullScreen 2")
            isFullScreen = false
            val cHeight = nScreenWidth * 9 / 16
            if (cHeight >= nScreenHeight) {
                val ratio = cHeight * 1.0f / nScreenHeight
                val space = (cHeight - nScreenHeight) / 2
                mCurrentScale = ratio
                mRectCanvas.set(0, -space, nScreenWidth, cHeight - space)
            } else {
                val cWidth = nScreenHeight * 16 / 9
                val ratio = cWidth * 1.0f / nScreenWidth
                val space = (cWidth - nScreenWidth) / 2
                mCurrentScale = ratio
                mRectCanvas.set(-space, 0, cWidth - space, nScreenHeight)
            }
        }
    }

    /**设置充满全屏*/
    fun setFullScreen() {
        isFullScreen = true
    }

    /**缩放至原始大小*/
    private fun scaleToOrigin() {
        if (nScreenWidth != 0 && nScreenHeight != 0) {
            val cHeight = nScreenWidth * 9 / 16
            if (cHeight >= nScreenHeight) {
                val cWidth = nScreenHeight * 16 / 9
                val space = (nScreenWidth - cWidth) / 2
                mRectCanvas.set(space, 0, cWidth + space, nScreenHeight)
            } else {
                val space = (nScreenHeight - cHeight) / 2
                mRectCanvas.set(0, space, nScreenWidth, cHeight + space)
            }
            mCurrentScale = 1f
        }
    }

    /**设置最大缩放倍数*/
    fun setMaxZoom(max: Float) {
        mCurrentMaxScale = max
    }

    /**
     * 必须调用此方法 回复才能正常运行
     */
    fun startPlayback(
        camera: Camera?,
        event: TEvent?,
        duration: Int = 0,
        index: Int = 0,
        playBackCallback: OnPlayBackCallback?
    ) {
        Liotc.d(TAG, "restartPlayback startPlayback 111")
        mRecordEvent = event
        mRecordDuration = duration
        mRecordIndex = index
        mOnPlaybackCallback = playBackCallback
        mCamera = camera

        mCamera?.registerSessionChannelCallback(this)
        mCamera?.registerIOCallback(this)
        mCamera?.registerFrameCallback(this)
        registerAVChannelRecordStatus(mAVChannelRecordStatus)
//        renderJob()
        if (mAvChannel < 0) {
            mRecordEvent?.let {
                if (mPlaybackStatus != PlaybackStatus.STOP && mPlaybackStatus != PlaybackStatus.START) {
                    mCamera.playback(
                        type = PlaybackStatus.START,
                        time = it,
                        duration = duration,
                        index = index
                    )
                }
            }
        }
    }

    //重新播放或者播放新的
    fun restartPlayback(
        camera: Camera? = null,
        event: TEvent?,
        duration: Int = 0,
        index: Int = 0,
        delay: Long = 500L,
        playBackCallback: OnPlayBackCallback?
    ) {
        Liotc.d(TAG, "restartPlayback 1111")
        isSeekTo = false
        mSeekTime = 0
        if (mRecordEvent == null) {
            startPlayback(camera, event, duration, index, playBackCallback)
            return
        }
        Liotc.d(TAG, "restartPlayback 22222")
        if (isRecording) {
            stopRecord()
        }
        destroyRendjob()
        stop()
        if (mCamera == null) {
            mCamera = camera
            mCamera?.registerSessionChannelCallback(this)
            mCamera?.registerIOCallback(this)
            mCamera?.registerFrameCallback(this)
            registerAVChannelRecordStatus(mAVChannelRecordStatus)
        }
        if (mOnPlaybackCallback == null) {
            mOnPlaybackCallback = playBackCallback
        }
        Liotc.d(TAG, "restartPlayback 3333 isRestartPlayback=$isRestartPlayback")
        handler.removeMessages(OPT_RESTART_NEW_EVENT)
        isRestartPlayback = true
        newRecordEvent = event
        newRecordEventDuration = duration
        newRecordEventIndex = index
        handler.sendEmptyMessageDelayed(OPT_RESTART_NEW_EVENT, delay)
        Liotc.d(TAG, "restartPlayback 4444 isRestartPlayback=$isRestartPlayback")
    }


    fun changePlayStatus() {
        Liotc.d(TAG, "restartPlayback changePlayStatus 111")
        if (mAvChannel >= 0 && (mPlaybackStatus == PlaybackStatus.PLAYING || mPlaybackStatus == PlaybackStatus.PAUSE)) {
            mRecordEvent?.let {
                Liotc.d(TAG, "restartPlayback changePlayStatus 222")
                mCamera.playback(type = PlaybackStatus.PAUSE, time = it)
            }
        } else if (mAvChannel < 0 && mPlaybackStatus == null) {
            mRecordEvent?.let {
                Liotc.d(TAG, "restartPlayback changePlayStatus 333")
                mCamera.playback(
                    type = PlaybackStatus.START,
                    time = it,
                    duration = mRecordDuration,
                    index = mRecordIndex
                )
            }
        }
    }

    /**
     * 暂停、开始播放
     */
    private fun pause() {
        if (mAvChannel >= 0 && mPlaybackStatus == PlaybackStatus.PLAYING) {
            mRecordEvent?.let {
                Liotc.d(TAG, "restartPlayback pause 111")
                mCamera.playback(type = PlaybackStatus.PAUSE, time = it)
            }
        }
    }

    /**
     * 暂停播放后回复播放
     */
    private fun resume() {
        if (mAvChannel >= 0 && mPlaybackStatus == PlaybackStatus.PAUSE) {
            mRecordEvent?.let {
                Liotc.d(TAG, "restartPlayback resume 111")
                mCamera.playback(type = PlaybackStatus.PAUSE, time = it)
            }
        }
    }

    fun seekTo(seekTime: Int) {
        if (mAvChannel >= 0 && mVideoTotalTime > 0) {
            mRecordEvent?.let {
                Liotc.d(TAG, "restartPlayback seekTo 111")
                AVAPIs.avClientCleanAudioBuf(mAvChannel)
                mSeekTime = seekTime
                val percent = ((mSeekTime * 1.0 / mVideoTotalTime) * 100).toInt()
                mCamera.playbackSeekToPercent(time = it, percent = percent)
            }
        } else if (mAvChannel == -1) {

            mRecordEvent?.let {
                isSeekTo = true

                Liotc.d(TAG, "restartPlayback seekTo 111")
                AVAPIs.avClientCleanAudioBuf(mAvChannel)
                mSeekTime = seekTime
                val percent = ((mSeekTime * 1.0 / mVideoTotalTime) * 100).toInt()
                mCamera.playbackSeekToPercent(time = it, percent = percent)
            }
        }
    }

    fun seekTo(
        camera: Camera? = null,
        event: TEvent?,
        duration: Int = 0,
        index: Int = 0,
        delay: Long = 500L,
        playBackCallback: OnPlayBackCallback?,
        seekTime: Int
    ) {
        if (mAvChannel >= 0 && mVideoTotalTime > 0) {
            mRecordEvent?.let {
                Liotc.d(TAG, "restartPlayback seekTo 111")
                AVAPIs.avClientCleanAudioBuf(mAvChannel)
                mSeekTime = seekTime
                val percent = ((mSeekTime * 1.0 / mVideoTotalTime) * 100).toInt()
                mCamera.playbackSeekToPercent(time = it, percent = percent)
            }
        } else if (mAvChannel == -1) {

            restartPlayback(camera, event, duration, index, delay, playBackCallback)
            isSeekTo = true
            mSeekTime = seekTime

        }
    }

    /**
     * 停止播放
     * 释放播放资源
     */
    private fun stop() {
//        if (mAvChannel >= 0 && mPlaybackStatus != PlaybackStatus.STOP) {
        Liotc.d(TAG, "restartPlayback stop mAvChannel=$mAvChannel")
        if (mAvChannel >= 0) {
            releaseAudio()
            Liotc.d(TAG, "stop1")
            mRecordEvent?.let {
                if (mPlaybackStatus != PlaybackStatus.STOP) {
                    Liotc.d(TAG, "restartPlayback stop 111")
                    Liotc.d(TAG, "stop 2")
                    Liotc.d("PlaybackMonitor", "receiveFrameDataTime stop playback")
                    mCamera.playback(type = PlaybackStatus.STOP, time = it)
                }
            }
            Liotc.d(TAG, "stop 3")
            if (isRecording) {
                stopRecord()
            }
            Liotc.d(TAG, "stop 4")
//            setAudioTrackStatus(false)
            Liotc.d(TAG, "stop 5")
            stopShow()
            Liotc.d(TAG, "stop 6")
            mCamera?.stop(mAvChannel)
            Liotc.d(TAG, "stop 7")
            //释放音频资源
            releaseAudio()

            stopPlayTime()
            mAvChannel = -1
            mPlaybackStatus = null
//            mOnPlaybackCallback = null
        } else {
            mPlaybackStatus = null
        }
    }

    private fun startPlayTime() {
        if (userDeviceStatusBarTime) return
        mPlayTimeJob?.cancel()
        mPlayTimeJob = null
        mPlayTimeRunning = true


        mPlayTimeJob = GlobalScope.launch(Dispatchers.Main) {
            flow {
                while (mPlayTimeRunning) {
                    emit(1)
                    delay(1000)
                }
            }.flowOn(Dispatchers.IO)
                .collect {
                    if (mPlayTimeJob?.isActive == true) {
                        mVideoPlayTime++
                        mOnPlaybackCallback?.onPlayBackStatus(
                            mPlaybackStatus,
                            mVideoTotalTime,
                            mVideoPlayTime
                        )
                    }
                }
        }
    }

    private fun stopPlayTime() {
        mPlayTimeRunning = false
        mPlayTimeJob?.cancel()
        mPlayTimeJob = null
    }

    /**解绑Camera*/
    fun unAttachCamera() {
        mAvChannel = -1

        mCamera?.unregisterSessionChannelCallback(this)
        mCamera?.unregisterFrameCallback(this)
        mCamera?.unregisterIOCallback(this)
        mCamera = null

        destroyRendjob()
        Liotc.d(TAG, "restartPlayback unAttachCamera")
    }

    private fun destroyRendjob() {
        isRunning = false
        mMonitorThread?.stopThread()
        kotlin.runCatching {
            mMonitorThread?.join()
        }
        mMonitorThread = null
    }

    /**
     * 开始播放回放
     */
    private fun startShow() {
        renderJob()
        mCamera?.setPlayMode(mAvChannel, mPlayMode)
        mCamera?.setVoiceType(mAvChannel, mVoiceType)
        mCamera?.start(mAvChannel, mCamera?.viewAccount ?: "admin", mCamera?.psw ?: "")
        mCamera.getAudioCodec(mAvChannel)
        mCamera?.startShow(context, mAvChannel, withYuv = withYuv)
        setAudioTrackStatus(mAudioTrackStatus)

    }


    private fun stopShow() {
        mCamera?.stopShow(mAvChannel)
    }

    fun setAudioTrackStatus(status: Boolean) {
        mAudioTrackStatus = status
        Liotc.d("PlaybackMonitor", "motion setAudioListener=$status")
        setAudioListener(if (mAudioTrackStatus) AudioListener.UNMUTE else AudioListener.MUTE)
    }

    /**监听*/
    private fun setAudioListener(audioListener: AudioListener) {
        if (mAvChannel >= 0) {
            Liotc.d("PlaybackMonitor", "motion setAudioListener=$audioListener")
            mCamera?.setAudioTrackStatus(context, mAvChannel, audioListener == AudioListener.UNMUTE)
        }
    }


    /**释放音频*/
    fun releaseAudio() {
        mCamera?.releaseAudio(mAvChannel)
    }

    fun registerAVChannelRecordStatus(listener: IAVChannelRecordStatus?) {
        mAVChannelRecordStatus = listener
        mCamera?.registerAVChannelRecordStatus(listener)
    }

    fun unRegisterAVChannelRecordStatus() {
        mCamera?.unRegisterAVChannelRecordStatus(mAVChannelRecordStatus)
        mAVChannelRecordStatus = null
    }

    fun startRecord(file: String?, callback: OnResultCallback<RecordStatus>?) {
        mCamera?.startRecord(context, mAvChannel, file, mBitmapWidth, mBitmapHeight) { result ->
            isRecording = result == RecordStatus.RECORDING
            callback?.onResult(result)
        }
    }


    fun stopRecord() {
        Liotc.d("stopRecord", "stopRecord")
        mCamera?.stopRecord()
        isRecording = false
    }


    private fun renderJob() {
        kotlin.runCatching {
            Liotc.d("PlaybackMonitor", "renderJob is Running[$isRunning]")
//            if (isRunning && mMonitorThread?.isThreadRunning() == true) {
//                Liotc.d("PlaybackMonitor", "renderJob is Running return [$isRunning],[${mRenderJob?.isActive}]")
//                return
//            }
//            Liotc.d("PlaybackMonitor", "renderJob surfaceDestroyed 1 renderJob=$canDraw")
//            if (!canDraw) return
            Liotc.d("PlaybackMonitor", "renderJob running")
            isRunning = true


            mMonitorThread = object : MonitorThread(surfaceHolder = holder) {
                override fun run() {

                    var videoCanvas: Canvas? = null

                    mPaint.isDither = true

                    while (isThreadRunning()) {
                        if (mCamera == null) break
                        Liotc.d(
                            "PlaybackMonitor",
                            "renderJob -----[${mLastZoomTime != null}],[${mLastFrame?.isRecycled == false}],canDraw=$canDraw  isThreadRunning=${isThreadRunning()}"
                        )
                        if (!isThreadRunning()) {
                            break
                        }
                        if (mLastFrame != null && mLastFrame?.isRecycled == false && canDraw) {
                            try {
                                videoCanvas = mSurHolder?.lockCanvas()
                                videoCanvas?.let { canvas ->
                                    canvas.drawColor(Color.BLACK)
                                    mLastFrame?.let { bitmap ->
                                        Liotc.d("PlaybackMonitor", "renderJob drawBitmap")
                                        canvas.drawBitmap(bitmap, null, mRectCanvas, mPaint)
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                Liotc.d(
                                    "PlaybackMonitor",
                                    "renderJob surfaceDestroyed renderJob error=${e.message}"
                                )
                            } finally {
                                videoCanvas?.let {
                                    if (this@PlaybackMonitor.canDraw) {
                                        mSurHolder?.unlockCanvasAndPost(it)
                                    }
                                }

                            }
                        }
                        Thread.sleep(33L)
                    }
                    isRunning = false

                    Liotc.d("PlaybackMonitor", "renderJob end")
                }
            }
            mMonitorThread?.start()

        }.onFailure {

        }
//        if (isRunning && mRenderJob?.isActive == true) {
//            Liotc.d("PlaybackMonitor", "renderJob is Running return [$isRunning],[${mRenderJob?.isActive}]")
//            return
//        }
//        Liotc.d("PlaybackMonitor", "renderJob running")
//        isRunning = true
//        mRenderJob = GlobalScope.launch(Dispatchers.IO) {
//
//            var videoCanvas: Canvas? = null
//            mPaint.isDither = true
//
//            while (isRunning && mRenderJob?.isActive == true) {
//                Liotc.d(
//                    "PlaybackMonitor",
//                    "renderJob -----[${mLastZoomTime != null}],[${mLastFrame?.isRecycled == false}]"
//                )
//                if (mLastFrame != null && mLastFrame?.isRecycled == false) {
//                    try {
//                        videoCanvas = mSurHolder?.lockCanvas()
//                        videoCanvas?.let { canvas ->
//                            canvas.drawColor(Color.BLACK)
//                            mLastFrame?.let { bitmap ->
//                                Liotc.d("PlaybackMonitor", "drawBitmap")
//                                canvas.drawBitmap(bitmap, null, mRectCanvas, mPaint)
//                            }
//                        }
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    } finally {
//                        videoCanvas?.let {
//                            if(canDraw){
//                                mSurHolder?.unlockCanvasAndPost(it)
//                            }
//                        }
//                        videoCanvas = null
//                    }
//                }
//                delay(33L)
//            }
//            Liotc.d("PlaybackMonitor", "renderJob end")
//            isRunning = false
//        }
    }

    override fun setOnClickListener(listener: OnClickListener?) {
        mOnClickListener = listener
    }


    fun onStart() {
        Liotc.d("PlaybackMonitor", "onStart")

    }

    fun onResume() {
        resume()
    }

    fun onPause() {
        if (mAvChannel >= 0 && mPlaybackStatus == PlaybackStatus.PLAYING) {
            pause()
        }
    }


    fun onStop() {
        if (isRecording) {
            stopRecord()
        }
        destroyRendjob()
    }

    fun onDestroy() {
        Liotc.d(TAG, "restartPlayback onDestroy 111")
        isRestartPlayback = false
        newRecordEvent = null
        newRecordEventDuration = 0
        newRecordEventIndex = 0
        handler.removeCallbacksAndMessages(null)
        Liotc.d("PlaybackMonitor", "onDestroy")
        if (isRecording) {
            stopRecord()
        }
        destroyRendjob()
        Liotc.d(TAG, "restartPlayback onDestroy 222 mAvChannel=$mAvChannel")
        stop()
        mOnPlaybackCallback = null
        unRegisterAVChannelRecordStatus()
        Liotc.d(TAG, "restartPlayback onDestroy")
        unAttachCamera()
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        Liotc.d("PlaybackMonitor", "motion event action [${event?.action}]")
        event?.let { _event ->
            when ((_event.action) and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    mDownTime = System.currentTimeMillis()
                    if (mRectCanvas.left != vLeft || mRectCanvas.top != vTop
                        || mRectCanvas.right != vRight || mRectCanvas.bottom != vBottom
                    ) {
                        mPinchedMode = DRAG
                    }

                    mStartPoint.set(_event.x, _event.y)
                    mStartClickPoint.set(_event.x, _event.y)

                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    val dist = spacing(_event)
                    if (dist > 10f) {
                        mPinchedMode = ZOOM
                        mOrigDist = dist
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mPinchedMode == ZOOM) {
                        val result = scaleVideo(_event)
                        if (result) {
                            return true
                        }
                    } else if (mPinchedMode == DRAG) {
                        val result = moveVideo(_event)
                        if (result) {
                            return true
                        }
                    }
                }
                MotionEvent.ACTION_UP,
                MotionEvent.ACTION_POINTER_UP -> {
                    if (mCurrentScale == 1f) {
                        mPinchedMode = NONE
                    }
                    //点击事件
                    if (_event.action == MotionEvent.ACTION_UP && System.currentTimeMillis() - mDownTime < 100) {
                        mOnClickListener?.onClick(this)
                    }

                    if (nScreenWidth != 0 && nScreenHeight != 0) {
                        val cHeight = nScreenWidth * 9 / 16
                        var left = mRectCanvas.left
                        var top = mRectCanvas.top
                        var right = mRectCanvas.right
                        var bottom = mRectCanvas.bottom

                        val sWidth = mRectCanvas.width()
                        val sHeight = mRectCanvas.height()
                        if (cHeight >= nScreenHeight) {
                            //以高为基准
                            if (top > 0) {
                                top = 0
                                bottom = sHeight
                            }

                            if (bottom < nScreenHeight) {
                                top = nScreenHeight - sHeight
                                bottom = nScreenHeight

                            }

                            if (left > vLeft) {
                                left = vLeft
                                right = vLeft + sWidth
                            }

                            if (right < vRight) {
                                right = vRight
                                left = vRight - sWidth
                            }
                            mRectCanvas.set(left, top, right, bottom)

                        } else {
                            //以宽为基准
                            if (left > 0) {
                                left = 0
                                right = left + sWidth
                            }
                            if (right < nScreenWidth) {
                                right = nScreenWidth
                                left = nScreenWidth - left
                            }

                            if (top > vTop) {
                                top = vTop
                                bottom = vTop + sHeight
                            }
                            if (bottom < vBottom) {
                                bottom = vBottom
                                top = vBottom - sHeight
                            }
                            mRectCanvas.set(left, top, right, bottom)
                        }
                    }

                }
            }
        }

        event?.let { evt ->
            mGestureDetector?.onTouchEvent(evt)
        }
        return true
    }

    /**计算双指巨鹿*/
    private fun spacing(event: MotionEvent): Float {
        Liotc.d("PlaybackMonitor", "spacing [${event.pointerCount}]")
        if (event.pointerCount > 1) {
            val x = event.getX(0) - event.getX(1)
            val y = event.getY(0) - event.getY(1)
            return sqrt(x * x + y * y)
        }
        return 0f
    }


    /**滑动视频*/
    private fun moveVideo(event: MotionEvent): Boolean {
        if (System.currentTimeMillis() - mLastZoomTime < 33) {
            return true
        }
        mCurrentPoint.set(event.x, event.y)

        val offsetX = mCurrentPoint.x - mStartPoint.x
        val offsetY = mCurrentPoint.y - mStartPoint.y

        mStartPoint.set(mCurrentPoint.x, mCurrentPoint.y)

        mMoveRect.set(mRectCanvas)
        mMoveRect.offset(offsetX.toInt(), offsetY.toInt())

        val width = mMoveRect.width()
        val height = mMoveRect.height()

        var left = mMoveRect.left
        var top = mMoveRect.top
        var right = mMoveRect.right
        var bottom = mMoveRect.bottom
        if (left > vLeft) {
            left = vLeft
            right = vLeft + width
        }

        if (top > vTop) {
            top = vTop
            bottom = vTop + height
        }

        if (right < vRight) {
            right = vRight
            left = right - width
        }

        if (bottom < vBottom) {
            bottom = vBottom
            top = bottom - height
        }
        mRectCanvas.set(left, top, right, bottom)

        return false
    }

    /**缩放视频*/
    private fun scaleVideo(event: MotionEvent): Boolean {
        Liotc.d("PlaybackMonitor", "scaleVideo 1 [${event.pointerCount}]")
        if (System.currentTimeMillis() - mLastZoomTime < 33 || event.pointerCount == 1) {
            return true
        }
        val newDist = spacing(event)
        Liotc.d("PlaybackMonitor", "scaleVideo 2 [$newDist]")
        if (mOrigDist == 0f) {
            return true
        }
        val scale = newDist / mOrigDist
        mCurrentScale *= scale
        mOrigDist = newDist
        Liotc.d("PlaybackMonitor", "scaleVideo 3 [$mCurrentScale],[$mCurrentMaxScale],[$scale]")
        if (mCurrentScale > mCurrentMaxScale) {
            mCurrentScale = mCurrentMaxScale
            return true
        }

        if (mCurrentScale < 1f) {
            mCurrentScale = 1f
        }

        val origWidth = vRight - vLeft
        val origHeight = vBottom - vTop

        val maxWidth = origWidth * mCurrentMaxScale.toInt()
        val maxHeight = origHeight * mCurrentMaxScale.toInt()

        val scaleWidth = (origWidth * mCurrentScale).toInt()
        val scaleHeight = (origHeight * mCurrentScale).toInt()

        var l =
            ((mRectMonitor.width() / 2) - ((mRectMonitor.width() / 2 - mRectCanvas.left) * scale)).toInt()
        var t =
            ((mRectMonitor.height() / 2) - ((mRectMonitor.height() / 2 - mRectCanvas.top) * scale)).toInt()

        var r = l + scaleWidth
        var b = t + scaleHeight
        Liotc.d("PlaybackMonitor", "scaleVideo 4 ")
        if (scaleWidth <= origWidth || scaleHeight <= origHeight) {
            l = vLeft
            t = vTop
            r = vRight
            b = vBottom
        } else if (scaleWidth >= maxWidth || scaleHeight >= maxHeight) {
            l = mRectCanvas.left
            t = mRectCanvas.top
            r = l + maxWidth
            b = t + maxHeight
        }
        mRectCanvas.set(l, t, r, b)
        Liotc.d("PlaybackMonitor", "scaleVideo 5 ")
        mLastZoomTime = System.currentTimeMillis()
        return false
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        canDraw = true
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Liotc.d(
            "PlaybackMonitor",
            "surfaceChanged [screen[$nScreenWidth,$nScreenWidth]],[surface[$width,$height]],measured[$measuredWidth,$measuredHeight]"
        )
        canDraw = true
        synchronized(this) {
            nScreenWidth = measuredWidth
            nScreenHeight = measuredHeight
            mRectMonitor.set(0, 0, width, height)
            val cHeight = nScreenWidth * 9 / 16
            if (cHeight > nScreenHeight) {
                //如果正常的高度 大于设置的高度,则以高度为基准,并且居中处理
                val cWidth = nScreenHeight * 16 / 9
                val space = (nScreenWidth - cWidth) / 2
                mRectCanvas.set(space, 0, cWidth + space, nScreenHeight)
            } else {
                val space = (nScreenHeight - cHeight) / 2
                mRectCanvas.set(0, space, nScreenWidth, cHeight + space)
            }

//            if (layoutOrientation == Camera.LANDS_ORIENTATION) {
//                mRectCanvas.set(0, 0, width, height)
//            } else {
//                mRectCanvas.set(0, 0, nScreenWidth, nScreenHeight)
//            }

//            if (mCurVideoWidth == 0 || mCurVideoHeight == 0) {
//                if (layoutOrientation == Camera.LANDS_ORIENTATION) {
//                    mRectCanvas.right = 4 * height / 3
//                    mRectCanvas.offset((width - mRectCanvas.right) / 2, 0)
//                } else {
//                    mRectCanvas.bottom = 3 * width / 4
//                    mRectCanvas.offset(0, (height - mRectCanvas.bottom) / 2)
//                }
//            } else {
//                if (layoutOrientation == Camera.LANDS_ORIENTATION) {
//
//                    mRectCanvas.right = mRectMonitor.right
//                    mRectCanvas.offset(0, 0)
//                } else {
//                    val ratio = mCurVideoWidth.toDouble() / mCurVideoHeight
//                    mRectCanvas.bottom = (mRectCanvas.right / ratio).toInt()
//                    mRectCanvas.offset(0, (mRectMonitor.bottom - mRectCanvas.bottom) / 2)
//
//                    //使图片居中
//                    val rmRight = mRectMonitor.right
//                    val rcRight = mRectCanvas.right
//
//                    val offsetX = (rmRight - rcRight) / 2
//                    mRectCanvas.set(offsetX, 0, mRectCanvas.width() + offsetX, mRectCanvas.bottom)
//                }
//            }

            vLeft = mRectCanvas.left
            vTop = mRectCanvas.top
            vRight = mRectCanvas.right
            vBottom = mRectCanvas.bottom

            mCurrentScale = 1f

            Liotc.d("PlaybackMonitor", "_setFullScreen surfaceChanged[$isFullScreen]")


//            parseMidPoint(
//                mMidPoint,
//                vLeft.toFloat(),
//                vTop.toFloat(),
//                vRight.toFloat(),
//                vBottom.toFloat()
//            )
//            parseMidPoint(
//                mMidPointForCanvas,
//                vLeft.toFloat(),
//                vTop.toFloat(),
//                vRight.toFloat(),
//                vBottom.toFloat()
//            )
        }
//        renderJob()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        canDraw = false
        isRunning = false
        mMonitorThread?.stopThread()

        mRenderJob?.cancel()
    }

    override fun receiveFrameData(camera: Camera?, avChannel: Int, bmp: Bitmap?) {


    }

    override fun receiveFrameData(camera: Camera?, avChannel: Int, bmp: Bitmap?, time: Long) {


        Liotc.d(
            "PlaybackMonitor",
            "receiveFrameData time=$time"
        )
        if (avChannel != mAvChannel) {
            Liotc.d(
                "PlaybackMonitor",
                "receiveFrameData error [$avChannel],[$mAvChannel],[${bmp == null}]"
            )
        }
        if (avChannel == mAvChannel) {
            Liotc.d(
                "PlaybackMonitor",
                "receiveFrameData success [$avChannel],[$mAvChannel],[${bmp == null}]"
            )
            mLastFrame = bmp

            if (mRenderJob == null || mRenderJob?.isActive != true || !isRunning) {
                Liotc.d("PlaybackMonitor", "restart render job")
//                renderJob()
            }

            nScreenWidth = measuredWidth
            nScreenHeight = measuredHeight
            mBitmapWidth = bmp?.width ?: 0
            mBitmapHeight = bmp?.height ?: 0
            if ((bmp?.width ?: 0) > 0 && (bmp?.height ?: 0) > 0 &&
                (nScreenHeight != mCurVideoHeight || nScreenWidth != mCurVideoWidth)
            ) {
                Liotc.d(
                    "PlaybackMonitor",
                    "screen[${nScreenWidth},${nScreenHeight}],video[${mCurVideoWidth},${mCurVideoHeight}]"
                )


                mCurVideoWidth = nScreenWidth
                mCurVideoHeight = nScreenHeight

                val cHeight = nScreenWidth * 9 / 16
                if (cHeight > nScreenHeight) {
                    //如果正常的高度 大于设置的高度,则以高度为基准,并且居中处理
                    val cWidth = nScreenHeight * 16 / 9
                    val space = (nScreenWidth - cWidth) / 2
                    mRectCanvas.set(space, 0, cWidth + space, nScreenHeight)
                } else {
                    val space = (nScreenHeight - cHeight) / 2
                    mRectCanvas.set(0, space, nScreenWidth, cHeight + space)
                }

                vLeft = mRectCanvas.left
                vTop = mRectCanvas.top
                vRight = mRectCanvas.right
                vBottom = mRectCanvas.bottom

                mCurrentScale = 1f
                Liotc.d("PlaybackMonitor", "_setFullScreen receiveFrameData[$isFullScreen]")
            }

            if (isFullScreen) {
                _setFullScreen()
            }
        }
    }

    override fun receiveFrameDataTime(time: Long) {
        if (userDeviceStatusBarTime) {

            if (mRecordEvent == null) {
                if (firstStatusBarTime == -1L) {
                    Liotc.d("PlaybackMonitor", "receiveFrameDataTime --------------")
                    firstStatusBarTime = time
                    mCurrentDeviceStatusBar = 0
                }
            } else {
                if (firstStatusBarTime == -1L) {
                    val startTime = (mRecordEvent?.time ?: 0L) * 1000
                    Liotc.d(
                        "PlaybackMonitor",
                        "receiveFrameDataTime --------------time=${time}   startTime=${startTime}"
                    )
                    firstStatusBarTime = startTime
                    mCurrentDeviceStatusBar = 0
                }
            }

            Liotc.d(
                "PlaybackMonitor",
                "receiveFrameDataTime time=$time   firstStatusBarTime=$firstStatusBarTime"
            )
            if (firstStatusBarTime == -1L) {
                firstStatusBarTime = time
                mCurrentDeviceStatusBar = 0
            } else {
                mCurrentDeviceStatusBar = (time - firstStatusBarTime).toInt()
            }
            mVideoPlayTime = mCurrentDeviceStatusBar
            if (meoofRecord) {
                if (mPlaybackStatus == PlaybackStatus.PLAYING) {
                    mOnPlaybackCallback?.onPlayBackStatus(
                        mPlaybackStatus,
                        mVideoTotalTime,
                        mCurrentDeviceStatusBar
                    )
                }
            }

        }
    }

    override fun receiveFrameInfo(
        camera: Camera?,
        avChannel: Int,
        bitRate: Long,
        frameRate: Int,
        onlineNm: Int,
        frameCount: Int,
        incompleteFrameCount: Int
    ) {
    }

    override fun receiveSessionInfo(camera: Camera?, resultCode: Int) {

    }

    override fun receiveChannelInfo(camera: Camera?, avChannel: Int, resultCode: Int) {

    }


    override fun receiveIOCtrlData(
        camera: Camera?,
        avChannel: Int,
        avIOCtrlMsgType: Int,
        data: ByteArray?
    ) {
        when (avIOCtrlMsgType) {

            AVIOCTRLDEFs.IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL_RESP -> {

                val playback = data.parsePlayBack()
                Liotc.d(
                    "PlaybackMonitor",
                    "--------IOTYPE_USER_IPCAM_RECORD_PLAYCONTROL_RESP type=${playback?.type},${mOnPlaybackCallback == null},channel=${playback?.channel}"
                )

                when (playback?.type) {
                    PlaybackStatus.START -> {
                        mVideoTotalTime = if (meoofRecord) {
                            mRecordDuration * 1000
                        } else {
                            playback.time / 1000
                        }
                        mVideoPlayTime = if (userDeviceStatusBarTime) mCurrentDeviceStatusBar else 0
                        mPlaybackStatus = PlaybackStatus.START
                        mOnPlaybackCallback?.onPlayBackStatus(
                            mPlaybackStatus,
                            mVideoTotalTime,
                            mVideoPlayTime
                        )
                        firstStatusBarTime = -1L
                        mCurrentDeviceStatusBar = 0
                        if (playback.channel in 0..31) {
                            mAvChannel = playback.channel
                            startShow()
                            mPlaybackStatus = PlaybackStatus.PLAYING
                            startPlayTime()
                            if(isSeekTo && mSeekTime > 0){
                                seekTo(mSeekTime)
                            }
                        } else {
                            mPlaybackStatus = PlaybackStatus.ERROR
                            mOnPlaybackCallback?.onPlayBackStatus(mPlaybackStatus, 0, 0)
                            stopPlayTime()
                        }
                        isSeekTo = false
                        mSeekTime = 0
                    }
                    PlaybackStatus.PAUSE -> {
                        if (mPlaybackStatus == PlaybackStatus.PAUSE) {
                            mPlaybackStatus = PlaybackStatus.PLAYING
                            startPlayTime()
                            renderJob()
                        } else {
                            isRunning = false
                            mPlaybackStatus = PlaybackStatus.PAUSE
                            stopPlayTime()
                            if (isRecording) {
                                stopRecord()
                            }
                        }
                        mOnPlaybackCallback?.onPlayBackStatus(
                            mPlaybackStatus,
                            mVideoTotalTime,
                            mVideoPlayTime
                        )
                    }
                    PlaybackStatus.STOP -> {
                        Liotc.d(TAG, "restartPlayback receiveIOCtrlData stop")
                        Liotc.d("PlaybackMonitor", "receiveFrameDataTime resp stop play")
                        isRunning = false
                        stopPlayTime()
                        stop()
                        mPlaybackStatus = PlaybackStatus.STOP
                        mOnPlaybackCallback?.onPlayBackStatus(
                            mPlaybackStatus,
                            mVideoTotalTime,
                            mVideoPlayTime
                        )
                        mPlaybackStatus = null
                    }
                    PlaybackStatus.END -> {
                        Liotc.d(TAG, "restartPlayback receiveIOCtrlData end stop")
                        mPlaybackStatus = PlaybackStatus.END
                        mOnPlaybackCallback?.onPlayBackStatus(
                            mPlaybackStatus,
                            mVideoTotalTime,
                            mVideoPlayTime
                        )
                        stopPlayTime()
                        stop()
                    }
                    PlaybackStatus.SEEKTIME -> {
                        mVideoPlayTime = mSeekTime
                    }
//                    PlaybackStatus.ERROR->{
//                        mPlaybackStatus = PlaybackStatus.ERROR
//                        mOnPlaybackCallback?.onPlayBackStatus(
//                            mPlaybackStatus,
//                            mVideoTotalTime,
//                            mVideoPlayTime
//                        )
//                        stopPlayTime()
//                        stop()
//                    }
                    else -> {

                    }
                }
            }
        }


    }

    //拍照
    fun takePhoto() = mLastFrame

    //拍照并保存到文件
    fun takePhoto(path: String, name: String): File? {
        val bitmap = mLastFrame ?: return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            //读写权限
            if (PermissionUtil.permissionIsGranted(
                    context, Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            ) {
                val file = File(path, name)
                FileOutputStream(file).use { fos ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
                    fos.flush()
                }
                val contentResolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DATA, file.absolutePath)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/*")
                }
                contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?.let { uri ->
                        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).apply {
                            data = uri
                        })
                    }
                return file
            } else {
                throw SecurityException("requires Manifest.permission.READ_EXTERNAL_STORAGE and Manifest.permission.WRITE_EXTERNAL_STORAGE")
            }
        } else {
            val file = File(path, name)
            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos)
                fos.flush()
            }
            val contentResolver = context.contentResolver
            val value = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/*")
                put(MediaStore.Images.Media.TITLE, name)
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_DCIM}/")
            }
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, value)
                ?.let { uri ->
                    FileInputStream(file).use { fis ->
                        BufferedInputStream(fis).use { bis ->
                            contentResolver.openOutputStream(uri)?.use { os ->
                                BufferedOutputStream(os).use { bos ->

//                                    val buffer = ByteArray(1024)
                                    var length: Int
                                    while (bis.read().also { length = it } != -1) {
                                        bos.write(length)
//                                        bos.write(buffer, 0, length)
                                    }
                                }
                            }
                        }
                    }
                }
            return file
        }
    }


    //使用ContentResolver保存
    fun takePhotoUri(path: String, name: String, title: String?): Uri? {
        val bitmap = mLastFrame ?: return null

        val contentResolver = context.contentResolver
        val value = ContentValues().apply {
            put(MediaStore.Images.Media.MIME_TYPE, "image/*")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.TITLE, title ?: name)
                put(MediaStore.Images.Media.RELATIVE_PATH, path)
            } else {
                put(MediaStore.Images.Media.DATA, "$path/$name")
            }
        }
        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, value)
        uri?.let {
            try {
                contentResolver.openOutputStream(it)?.use { os ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, os)
                }
                return it
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    fun interface OnPlayBackCallback {
        /**
         *  @param status
         *  @param totalTime 视频总时长 秒
         *  @param currentTime 当前播放的时长 秒
         */
        fun onPlayBackStatus(status: PlaybackStatus?, totalTime: Int, currentTime: Int)
    }
}
