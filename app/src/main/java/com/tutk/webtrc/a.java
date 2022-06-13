package com.tutk.webtrc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Environment;

import com.android.aec.util.WebRtcUtil;
import com.ingenic.api.AudioFrame;
import com.ingenic.api.Frequency;
import com.ingenic.api.OnTSUpdateListener;
import com.ingenic.api.RecodeAudioDataListener;
import com.ingenic.b.c;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.Arrays;

/**
 * @Author: wangyj
 * @CreateDate: 2022/3/1
 * @Description:
 */
public class a implements AudioManager.OnAudioFocusChangeListener {
    private Context c;
    private static final boolean d = com.ingenic.a.b.a();
    private static final String e = "AudioPlayer";
    private int f = 60;
    private int g = 8000;
    private Thread h;
    private Thread i;
    private volatile boolean j;
    private boolean k;
    private boolean l;
    private boolean m;
    private boolean n = true;
    private com.ingenic.b.c o;
    private com.ingenic.b.b p;
    private com.ingenic.b.b q;
    private int r;
    private boolean s;
    private boolean t = true;
    private AudioTrack u = null;
    private AudioRecord v = null;
    private boolean w;
    private volatile int x;
    private volatile int y;
    private volatile boolean z;
    private RecodeAudioDataListener A;
    private float B = 1.0F;
    private float C = 1.0F;
    private WeakReference<OnTSUpdateListener> D;
    private boolean E = false;
    private MyReceiver F;
    long a;
    long b;

    public a(Context var1, RecodeAudioDataListener var2, Frequency var3, int var4) {
        this.c = var1;
        this.g = var3.value();
        this.f = 160;
        WebRtcUtil.init((long)this.g);
        this.A = var2;
        this.r = var4;
        this.o = new c(var3.value() * 4, this.r);
        this.p = new com.ingenic.b.b(var3.value() * 4, this.r);
        this.q = new com.ingenic.b.b(var3.value() * 2, this.r);
        com.ingenic.c.a.a().a(var1, this);
        com.ingenic.c.a.a().c();
        this.k();
        this.l();
    }

    private void k() {
        this.F = new MyReceiver();
        IntentFilter var1 = new IntentFilter();
        var1.addAction("android.media.VOLUME_CHANGED_ACTION");
        this.c.registerReceiver(this.F, var1);
    }

    private void l() {
        try {
            int var1 = com.ingenic.c.a.a().b().getStreamMaxVolume(3);
            int var2 = com.ingenic.c.a.a().b().getStreamVolume(3);
            int var3 = var1 / 2 - var2;
            var3 /= var1 / 5;
            if (var3 == 1 || var3 == -1) {
                var3 = 0;
            }

            int var4 = 1;

            for(int var5 = 0; var5 < (int)Math.log10((double)var3); ++var5) {
                var4 *= 10;
            }

            var3 /= var4;
            if (var3 > 0) {
                this.C = (float)(1.0D / Math.pow(2.0D, (double)var3));
                this.B = 1.0F;
            } else if (var3 < 0) {
                this.C = (float)(-var3);
                this.B = 1.0F;
            } else {
                this.C = 1.0F;
                this.B = 1.0F;
            }

            com.android.aec.util.a.a("AudioPlayer", "maxVolume = " + var1 + "  currVolume = " + var2);
            com.android.aec.util.a.a("AudioPlayer", "volume = " + var3 + "  farv = " + this.C + "  near_v = " + this.B);
        } catch (Throwable var6) {
            com.android.aec.util.a.c("AudioPlayer", "error :" + var6.getMessage());
        }

    }

    public void a(OnTSUpdateListener var1) {
        this.D = new WeakReference(var1);
    }

    private void m() {
        this.j = false;
        this.m = false;
        this.n = true;
        this.s = false;
        this.t = true;
        this.w = false;
        this.x = 0;
        this.y = 0;
    }

    public synchronized void a() {
        this.j = false;
        if (this.h != null) {
            this.h.interrupt();

            try {
                this.h.join();
            } catch (InterruptedException var3) {
                var3.printStackTrace();
            }

            this.h = null;
        }

        if (this.i != null) {
            this.i.interrupt();

            try {
                this.i.join();
            } catch (InterruptedException var2) {
                var2.printStackTrace();
            }

            this.i = null;
        }

        this.m = false;
        if (this.o != null) {
            this.o.b();
        }

        if (this.p != null) {
            this.p.b();
        }

        if (this.q != null) {
            this.q.b();
        }

        this.m();
    }

    public synchronized void b() {
        if (this.o != null) {
            this.o.a();
            this.o = null;
        }

        if (this.p != null) {
            this.p.a();
            this.p = null;
        }

        if (this.q != null) {
            this.q.a();
            this.q = null;
        }

        if (this.D != null) {
            this.D.clear();
        }

        if (this.F != null) {
            this.c.unregisterReceiver(this.F);
            this.F = null;
        }

        WebRtcUtil.free();
        com.ingenic.c.a.a().d();
    }

    public void c() {
        this.k = true;
    }

    public void d() {
        this.k = false;
    }

    public boolean e() {
        return this.k;
    }

    public void f() {
        this.l = true;
    }

    public void g() {
        this.l = false;
    }

    public boolean h() {
        return this.l;
    }

    public synchronized boolean a(byte[] var1, int var2) {
        return this.o == null ? false : this.o.a(var1, var2);
    }

    public synchronized boolean a(AudioFrame var1) {
        return this.o == null ? false : this.o.a(var1);
    }

    public synchronized byte[] i() {
        return this.q == null ? null : this.q.c();
    }

    public synchronized void a(boolean var1, boolean var2) {
        if (!this.m) {
            this.m();
            this.k = var1;
            this.l = var2;
            this.m = true;
            this.j = true;
            if (this.i == null) {
                this.i = new Thread(new Runnable() {
                    public void run() {
                        try {
                            int[] var1 = new int[1];
                            a.this.u = MyAudioUtils.createTracker(var1, a.this.g, a.this.r, true);
                            if (a.this.u != null) {
                                int var2 = var1[0] / (a.this.g * 2 / 1000);
                                int var3 = a.this.u.setPositionNotificationPeriod(a.this.r / 2);
                                a.this.E = var3 == 0;
                                if (a.this.E) {
                                    a.this.u.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                                        long a;

                                        public void onPeriodicNotification(AudioTrack var1) {
                                            a.this.y = a.this.y + 1;
                                            if (a.this.y == 7) {
                                                a.this.b = (System.currentTimeMillis() - this.a) / 2L + this.a + 25L;
                                            }

                                            if (a.this.y == 6) {
                                                this.a = System.currentTimeMillis();
                                            }

                                            if (a.this.y > 10 && a.this.x == a.this.y) {
                                                a.this.z = true;
                                            }

                                            if (a.this.x > 1000) {
                                                a.this.x = a.this.x - 100;
                                                a.this.y = a.this.y - 100;
                                            }

                                        }

                                        public void onMarkerReached(AudioTrack var1) {
                                        }
                                    });
                                }

                                AudioFrame var4 = new AudioFrame();
                                a.this.u.play();
                                byte[] var5 = new byte[a.this.r];

                                do {
                                    if (a.this.k) {
                                        if (!a.this.o.b(var4)) {
                                            var4.setAudioData(var5, var5.length);
                                            var4.setTimeStamp(0L);
                                            var4.setPos(0);
                                        }
                                    } else {
                                        a.this.o.b();
                                        var4.setAudioData(var5, var5.length);
                                        var4.setTimeStamp(0L);
                                        var4.setPos(0);
                                    }

                                    if (a.this.u.getPlayState() != 3) {
                                        a.this.u.play();
                                    }

                                    Class var6;
                                    if (a.this.t && a.this.n) {
                                        var6 = com.ingenic.b.a.class;
                                        synchronized(com.ingenic.b.a.class) {
                                            while(!a.this.s && a.this.n) {
                                                com.ingenic.b.a.class.wait();
                                            }
                                        }

                                        a.this.t = false;
                                    }

                                    if (a.this.w || a.this.z) {
                                        var6 = com.ingenic.b.a.class;
                                        synchronized(com.ingenic.b.a.class) {
                                            while((a.this.w || a.this.z) && a.this.n) {
                                                com.ingenic.b.a.class.wait();
                                            }
                                        }

                                        a.this.u.play();
                                    }

                                    a.this.p.a(var4.getAudioData(), var4.dataLength());
                                    a.this.u.write(var4.getAudioData(), 0, var4.dataLength());
                                    OnTSUpdateListener var18 = null;
                                    if (a.this.D != null) {
                                        var18 = (OnTSUpdateListener) a.this.D.get();
                                    }

                                    if (var18 != null) {
                                        var18.onUpdate((var4.getTimeStamp() == 0L ? (long)var2 : var4.getTimeStamp()) - (long)var2);
                                    }

                                    a.this.x = a.this.x + 1;
                                } while(a.this.j);

                                return;
                            }
                        } catch (Throwable var16) {
                            var16.printStackTrace();
                            return;
                        } finally {
                            if (a.this.u != null && a.this.u.getPlayState() == 3) {
                                a.this.u.pause();
                                a.this.u.flush();
                                a.this.u.release();
                                a.this.u = null;
                            }

                        }

                    }
                }, "AudioPlay Thread");
                this.i.setPriority(6);
                this.i.start();
            }

            if (this.h == null) {
                this.h = new Thread(new Runnable() {
                    public void run() {
                        byte[] var1 = new byte[a.this.r];
                        BufferedOutputStream var2 = null;
                        BufferedOutputStream var3 = null;
                        BufferedOutputStream var4 = null;

                        try {
                            if (d) {
                                String var5 = Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
                                var2 = com.android.aec.util.b.a(var5 + "debug_out.pcm");
                                var3 = com.android.aec.util.b.a(var5 + "debug_far.pcm");
                                var4 = com.android.aec.util.b.a(var5 + "debug_near.pcm");
                            }

                            byte[] var6 = new byte[a.this.r];
                            Class var7 = com.ingenic.b.a.class;
                            synchronized(com.ingenic.b.a.class) {
                                a.this.s = true;
                                com.ingenic.b.a.class.notify();
                            }

                            while((a.this.E ? a.this.y : a.this.x) <= 5) {
                                Thread.sleep(10L);
                            }

                            while((a.this.E ? a.this.y : a.this.x) <= 6) {
                                Thread.sleep(1L);
                            }

                            int var41 = 0;

                            while(true) {
                                ++var41;
                                if (var41 > a.this.y) {
                                    int[] var42 = new int[1];
                                    a.this.v = MyAudioUtils.creatAudioRecord(a.this.c,var42, a.this.g, a.this.r, true);
                                    if (a.this.v == null) {
                                        throw new NullPointerException("audio recode is null");
                                    }

                                    a.this.a = System.currentTimeMillis();
                                    a.this.v.startRecording();
                                    a.this.f = 60;
                                    long var9 = 0L;
                                    long var11 = 0L;
                                    byte[] var14 = new byte[var42[0]];

                                    while(true) {
                                        while(a.this.j) {
                                            byte[] var40 = a.this.p.c();
                                            if (var40 == null) {
                                                a.this.w = true;
                                                Class var15 = com.ingenic.b.a.class;
                                                synchronized(com.ingenic.b.a.class) {
                                                    var40 = new byte[a.this.r];

                                                    for(int var16 = 0; var16 < a.this.f / 50 + 3; ++var16) {
                                                        a.this.v.read(var1, 0, a.this.r);
                                                        WebRtcUtil.bufferFarendAndProcess(var40, var1, var6, a.this.r, a.this.f, 0, a.this.B, a.this.C);
                                                        if (a.this.l) {
                                                            a.this.q.a(var6, var6.length);
                                                        }

                                                        if (d) {
                                                            var4.write(var1);
                                                            Arrays.fill(var40, (byte)100);
                                                            var3.write(var40);
                                                            Arrays.fill(var40, (byte)0);
                                                            var2.write(var6);
                                                        }
                                                    }

                                                    var11 = 0L;
                                                    var9 = 0L;

                                                    while(var9 - var11 < 10L) {
                                                        var11 = System.currentTimeMillis();
                                                        int var13 = a.this.v.read(var14, 0, var14.length);
                                                        var9 = System.currentTimeMillis();
                                                        if (d) {
                                                            byte[] var43 = new byte[var13];
                                                            var4.write(var14);
                                                            var2.write(var14, 0, var13);
                                                            Arrays.fill(var43, (byte)100);
                                                            var3.write(var43);
                                                        }

                                                        if (a.this.l) {
                                                            a.this.q.a(var14, var13);
                                                        }

                                                        if (a.this.A != null) {
                                                            a.this.A.onRecodeAudioData(var14, var13, (byte[])null);
                                                        }
                                                    }

                                                    if (a.this.u != null && a.this.u.getState() == 1) {
                                                        a.this.v.stop();
                                                        a.this.v.startRecording();
                                                        if (a.this.y > 0) {
                                                            while(true) {
                                                                if (a.this.p.c() == null) {
                                                                    a.this.u.pause();
                                                                    a.this.u.flush();
                                                                    a.this.y = 0;
                                                                    a.this.x = 0;
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                    }

                                                    a.this.w = false;
                                                    if (a.this.z) {
                                                        a.this.z = false;
                                                    }

                                                    com.ingenic.b.a.class.notify();
                                                }

                                                Thread.sleep(100L);
                                            } else {
                                                a.this.v.read(var1, 0, a.this.r);
                                                if (d) {
                                                    var11 = System.currentTimeMillis();
                                                }

                                                if (a.this.A != null) {
                                                    a.this.A.onRecodeAudioData(var1, a.this.r, (byte[])null);
                                                }

                                                if (a.this.k) {
                                                    WebRtcUtil.bufferFarendAndProcess(var40, var1, var6, a.this.r, a.this.f, 0, a.this.B, a.this.C);
                                                } else {
                                                    System.arraycopy(var1, 0, var6, 0, var6.length);
                                                }

                                                if (d) {
                                                    var9 = System.currentTimeMillis();
                                                }

                                                if (a.this.l) {
                                                    a.this.q.a(var6, var6.length);
                                                }

                                                if (d) {
                                                    var4.write(var1);
                                                    var3.write(var40);
                                                    var2.write(var6);
                                                }
                                            }
                                        }

                                        return;
                                    }
                                }

                                byte[] var8 = a.this.p.c();
                            }
                        } catch (Throwable var38) {
                            var38.printStackTrace();
                        } finally {
                            a.this.n = false;
                            if (a.this.v != null) {
                                int var10000 = a.this.v.getState();
                                //a.this.v;
                                if (var10000 == 1) {
                                    a.this.v.release();
                                }
                            }

                            a.this.v = null;
                            if (var4 != null) {
                                try {
                                    var4.close();
                                } catch (IOException var35) {
                                    var35.printStackTrace();
                                }
                            }

                            if (var3 != null) {
                                try {
                                    var3.close();
                                } catch (Throwable var34) {
                                    var34.printStackTrace();
                                }
                            }

                            if (var2 != null) {
                                try {
                                    var2.close();
                                } catch (IOException var33) {
                                    var33.printStackTrace();
                                }
                            }

                            class NamelessClass_1 extends Thread {
                                NamelessClass_1() {
                                }

                                public void run() {
                                    try {
                                        Thread.sleep(100L);
                                    } catch (InterruptedException var4) {
                                        var4.printStackTrace();
                                    }

                                    Class var1 = com.ingenic.b.a.class;
                                    synchronized(com.ingenic.b.a.class) {
                                        com.ingenic.b.a.class.notifyAll();
                                    }
                                }
                            }

                            (new NamelessClass_1()).start();
                        }

                    }
                }, "RecodeThread");
                this.h.setPriority(6);
                this.h.start();
            }

        }
    }

    public void onAudioFocusChange(int var1) {
        switch(var1) {
            case -3:
            case -2:
            case -1:
                this.a();
            case 0:
            default:
                break;
            case 1:
            case 2:
            case 3:
                this.a(this.k, this.l);
        }

    }

    private class MyReceiver extends BroadcastReceiver {
        private MyReceiver() {
        }

        public void onReceive(Context var1, Intent var2) {
            if (var2.getAction() != null && var2.getAction().equals("android.media.VOLUME_CHANGED_ACTION")) {
                a.this.l();
            }

        }
    }
}
