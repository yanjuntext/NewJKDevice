package com.tutk.IOTC;

import com.tutk.liblocalrecorder.LocalRecorder;
import com.tutk.libmediaconvert.AudioConvert;
import com.tutk.libmediaconvert.AudioEncoder;
import com.tutk.libmediaconvert.VideoDecoder;

import java.io.File;

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/30
 * @Description:
 */
public class LocalRecording {

    private RecordBuf bd = new RecordBuf();
    private Object be = new Object();
    private Object bf;
    private int bg = -1;
    private int bh = -1;
    private int bi = -1;
    private int bj = 0;
    private int bk;
    private int bl;
    private long bm;
    private long bn = 0L;
    private boolean bo = false;
    private boolean bp = true;
    private boolean bq = false;
    private boolean br = false;
    private boolean bs = false;
    private String bt = null;
    private LocalRecorder bu = new LocalRecorder();
    private AudioEncoder bv;

    public LocalRecording() {
    }

    public void setSkipAudio() {
        this.bs = true;
    }

    public void setParserBuffer(byte[] var1) {
        Object var2 = this.bd.bw;
        synchronized(this.bd.bw) {
            this.bd.buffer = var1;
            Liotc.INSTANCE.i("code", "setsize");
            this.bd.bw.notify();
        }
    }

    public void setAudioEnvironment(int var1, int var2, int var3) {
        this.bg = var1;
        this.bh = var2;
        this.bi = var3;
        Object var4 = this.be;
        synchronized(this.be) {
            this.be.notify();
        }
    }

    private int h() {
        Long var1 = System.currentTimeMillis();
        if (this.bn <= 0L) {
            this.bn = var1;
            return 50;
        } else {
            int var2 = (int)(var1 - this.bn);
            if (var2 <= 0) {
                var2 = var2 < -200 ? 1 : (var2 < -100 ? 10 : 50);
                this.bn += (long)var2;
            } else {
                this.bn = var1;
            }

            return var2;
        }
    }

    private void i() {
        if (this.br) {
            long var1 = System.currentTimeMillis();
            long var3 = var1 - this.bm;
            int var5 = (int)(var3 - this.bu.GetVideoTimeStamp());
            int var6 = (int)(var3 - this.bu.GetAudioTimeStamp());
            Liotc.INSTANCE.e("LocalRecording", "time = " + var3);
            Liotc.INSTANCE.e("LocalRecording", "video duration = " + this.bu.GetVideoTimeStamp() + " , " + var5);
            Liotc.INSTANCE.e("LocalRecording", "audio duration = " + this.bu.GetAudioTimeStamp() + " , " + var6);
            if (var5 > 0) {
                byte[] var7 = new byte[1024];
                this.bu.WriteVideo(var7, var7.length, var5);
            }

            boolean var14 = true;
            int var8 = 0;

            while(var6 > 0) {
                boolean var9 = false;
                int var15;
                if (var6 - 100 >= 0) {
                    var15 = 100;
                    var6 -= var15;
                } else {
                    var15 = var6;
                    var6 = 0;
                }

                if (var15 != 0) {
                    int var10 = var15 * this.bh * this.bi;
                    byte[] var11 = new byte[var10];
                    byte[] var12 = new byte[20480];
                    int var13 = this.bv.encode(var11, var11.length, var12);
                    var8 += var15;
                    if (var13 > 0) {
                        this.bu.WriteAudio(var12, var13, var8);
                        var8 = 0;
                    }
                }
            }

            Liotc.INSTANCE.e("LocalRecording", "end videoTimeStamp = " + this.bu.GetVideoTimeStamp());
            Liotc.INSTANCE.e("LocalRecording", "end audioTimeStamp = " + this.bu.GetAudioTimeStamp());
        }
    }

    public boolean startRecording(int var1, String var2, boolean var3) {
        if (var1 != VideoDecoder.VideoCodec.VIDEO_CODEC_H264.getValue()
                && var1 != VideoDecoder.VideoCodec.VIDEO_CODEC_HEVC.getValue()) {
            Liotc.INSTANCE.e("LocalRecording", "Can not support the video codec : " + var1);
            return false;
        } else {
            synchronized(this) {
                this.bo = false;
                this.bp = false;
                this.bp = false;
                Liotc.INSTANCE.e("LocalRecording", "111");
                if (this.bq) {
                    return false;
                }

                Object var5;
                if (!this.bs && (this.bg == -1 || this.bh == -1 || this.bi == -1)) {
                    var5 = this.be;
                    synchronized(this.be) {
                        Liotc.INSTANCE.e("LocalRecording", "222");
                        try {
                            this.be.wait(1000L);
                        } catch (InterruptedException var15) {
                            ;
                        }
                        Liotc.INSTANCE.e("LocalRecording", "333");
                        if (this.bg == -1 || this.bh == -1 || this.bi == -1) {
                            Liotc.INSTANCE.e("LocalRecording", "can not get the audio enviroment settings.");
                            this.bs = true;
                        }
                    }
                }
                Liotc.INSTANCE.e("LocalRecording", "4444");
                this.bt = var2;
                if (var1 == VideoDecoder.VideoCodec.VIDEO_CODEC_HEVC.getValue()) {
                    this.bu.Open(var2, 2);
                } else {
                    this.bu.Open(var2, 1);
                }
                Liotc.INSTANCE.e("LocalRecording", "555");
                if (!this.bs) {
                    this.bu.SetAudioTrack(this.bh, this.bg);
                }
                Liotc.INSTANCE.e("LocalRecording", "666");
                if (this.bd.buffer == null) {
//                    try {
//                        this.bd.bw.wait();
//                    } catch (InterruptedException var14) {
//                        var14.printStackTrace();
//                    }
                }

                if (var1 == VideoDecoder.VideoCodec.VIDEO_CODEC_HEVC.getValue()) {
                    var5 = this.bd.bw;
                    synchronized(this.bd.bw) {
                        if(bd.buffer != null) {
                            this.bu.SetVideoParser(this.bd.buffer, this.bd.buffer.length);
                        }
                    }
                }

                Liotc.INSTANCE.e("LocalRecording", "startRecording: Record_videoWidth = " + this.bk + " Record_videoHeight = " + this.bl);
                this.bu.SetVideoTrack(this.bk, this.bl);
                if (this.bv != null) {
                    this.bv.release();
                    this.bv = null;
                }

                this.bv = new AudioEncoder();
                int var18 = this.bi == 8 ? 0 : 1;
                this.bv.create(AudioConvert.AudioCodec.AUDIO_CODEC_AAC_RAW, this.bg, var18, this.bh == 1 ? 0 : 1);
                this.br = false;
                this.bq = true;
            }

            if (var3 && this.bf == null) {
                this.bf = new Object();
                Object var4 = this.bf;
                synchronized(this.bf) {
                    try {
                        this.bf.wait();
                    } catch (InterruptedException var11) {
                        var11.printStackTrace();
                    }
                }
            }

            return this.bq;
        }
    }

    public void setRecorderVideoTrack(int var1, int var2) {
        this.bk = var1;
        this.bl = var2;
    }

    public boolean stopRecording() {
        Object var1 = this.be;
        synchronized(this.be) {
            this.be.notify();
        }

        synchronized(this) {
            if (!this.bq) {
                return false;
            } else {
                this.i();
                this.bu.Close();
                if (this.bv != null) {
                    this.bv.release();
                    this.bv = null;
                }

                this.bq = false;
                this.bs = false;
                if (!this.bp && this.bt != null) {
                    File var2 = new File(this.bt);
                    if (var2.exists()) {
                        var2.delete();
                    }
                }

                this.bn = 0L;
                this.bm = 0L;
                if (this.bf != null) {
                    Object var8 = this.bf;
                    synchronized(this.bf) {
                        this.bf.notify();
                    }

                    this.bf = null;
                }

                return true;
            }
        }
    }

    public boolean recodeAudioFrame(byte[] var1, int var2, int var3) {
        synchronized(this) {
            Liotc.INSTANCE.e("recodeAudioFrame",this.bq +"---------" + this.bs +"-----"+this.bp);
            if (this.bq && !this.bs) {
                if (!this.bp) {
                    return false;
                } else {
                    this.bj += var3;
                    byte[] var5 = new byte[var2];
                    System.arraycopy(var1, 0, var5, 0, var2);
                    byte[] var6 = new byte[20480];
                    int var7 = this.bv.encode(var5, var2, var6);
                    Liotc.INSTANCE.e("LocalRecording", "len = " + var7 + ", duration : " + var3 + "(" + this.bj + ")");
                    if (var7 > 0) {
                        Liotc.INSTANCE.e("BB", "mAudioDuration: " + this.bj);
                        this.bu.WriteAudio(var6, var7, this.bj);
                        this.bj = 0;
                        return true;
                    } else {
                        return false;
                    }
                }
            } else {
                return false;
            }
        }
    }

    public boolean recordVideoFrame(byte[] var1, int var2, boolean var3) {
        if (var3) {
            this.bp = true;
            if (this.bf != null) {
                Object var4 = this.bf;
                synchronized(this.bf) {
                    this.bf.notify();
                }

                this.bf = null;
            }

            if (!this.br) {
                this.bm = System.currentTimeMillis();
                this.br = true;
            }
        }

        synchronized(this) {
            if (!this.bq) {
                return false;
            }

            if (!this.bp) {
                return false;
            }

            int var5 = this.h();
            Liotc.INSTANCE.e("BB", "VideoDuration: " + var5);
            this.bu.WriteVideo(var1, var2, var5);
        }

        this.bo = true;
        return true;
    }

    public boolean isRecording() {
        return this.bq;
    }

    public boolean hasRecordFreme() {
        return this.bo;
    }

    private class RecordBuf {
        Object bw;
        byte[] buffer;

        private RecordBuf() {
            this.bw = new Object();
        }
    }
}
