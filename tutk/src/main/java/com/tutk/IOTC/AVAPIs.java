package com.tutk.IOTC;

import java.util.ArrayList;

/**
 * @Author: wangyj
 * @CreateDate: 2021/9/8
 * @Description:
 */
public class AVAPIs {
    public static final int TIME_DELAY_DELTA				=1;		//ms
    public static final int TIME_DELAY_MIN					=4;		//ms
    public static final int TIME_DELAY_MAX					=500;	//ms
    public static final int TIME_DELAY_INITIAL				=0;	//ms

    public static final int TIME_SPAN_LOSED					=1000;	//ms

    //--{{inner iotype-----------------------------------------------------
    public static final int IOTYPE_INNER_SND_DATA_DELAY		=0xFF;	//C--->D: avClient(AP) change time interval of sending packets by avSendFrameData(avServer)

    //--}}inner iotype-----------------------------------------------------

    public static final int API_ER_ANDROID_NULL					= -10000;
    //AVApis error code	===================================================================================
    /** The function is performed successfully. */
    public static final int		AV_ER_NoERROR						 =0;

    /** The passed-in arguments for the function are incorrect */
    public static final int		AV_ER_INVALID_ARG					=-20000;

    /** The buffer to receive frame is too small to store one frame */
    public static final int		AV_ER_BUFPARA_MAXSIZE_INSUFF		=-20001;

    /** The number of AV channels has reached maximum.
     * The maximum number of AV channels is determined by the passed-in
     * argument of avInitialize() */
    public static final int		AV_ER_EXCEED_MAX_CHANNEL			=-20002;

    /** Insufficient memory for allocation */
    public static final int		AV_ER_MEM_INSUFF					=-20003;

    /** AV fails to create threads. Please check if OS has ability to create threads for AV. */
    public static final int		AV_ER_FAIL_CREATE_THREAD			=-20004;

    /** A warning error code to indicate that the sending queue of video frame of an AV server
     * is almost full, probably caused by slow handling of an AV client or network
     * issue. Please note that this is just a warning, the video frame is actually
     * put in the queue. */
    public static final int		AV_ER_EXCEED_MAX_ALARM				=-20005;

    /** The frame to be sent exceeds the currently remaining video frame buffer.
     * The maximum of video frame buffer is controlled by avServSetMaxBufSize() */
    public static final int		AV_ER_EXCEED_MAX_SIZE				=-20006;

    /** The specified AV server has no response */
    public static final int		AV_ER_SERV_NO_RESPONSE				=-20007;

    /** An AV client does not call avClientStart() yet */
    public static final int		AV_ER_CLIENT_NO_AVLOGIN				=-20008;

    /** The client fails in authentication due to incorrect view account or password */
    public static final int		AV_ER_WRONG_VIEWACCorPWD			=-20009;

    /** The IOTC session of specified AV channel is not valid */
    public static final int		AV_ER_INVALID_SID					=-20010;

    /** The specified timeout has expired during some operation */
    public static final int		AV_ER_TIMEOUT						=-20011;

    /** The data is not ready for receiving yet. */
    public static final int		AV_ER_DATA_NOREADY					=-20012;

    /** Some parts of a frame are lost during receiving */
    public static final int		AV_ER_INCOMPLETE_FRAME				=-20013;

    /** The whole frame is lost during receiving */
    public static final int		AV_ER_LOSED_THIS_FRAME				=-20014;

    /** The remote site already closes the IOTC session.
     * Please call IOTC_Session_Close() to release local IOTC session resource */
    public static final int		AV_ER_SESSION_CLOSE_BY_REMOTE		=-20015;

    /** This IOTC session is disconnected because remote site has no any response
     * after a specified timeout expires. */
    public static final int		AV_ER_REMOTE_TIMEOUT_DISCONNECT		=-20016;

    /** Users exit starting AV server process */
    public static final int		AV_ER_SERVER_EXIT		    		=-20017;

    /** Users exit starting AV client process */
    public static final int		AV_ER_CLIENT_EXIT		    		=-20018;

    /** AV module has not been initialized */
    public static final int		AV_ER_NOT_INITIALIZED	    		=-20019;

    /** By design, an AV client cannot send frame and audio data to an AV server */
    public static final int		AV_ER_CLIENT_NOT_SUPPORT	   		=-20020;

    /** The AV channel of specified AV channel ID is already in sending IO control process */
    public static final int		AV_ER_SENDIOCTRL_ALREADY_CALLED	   	=-20021;

    /** The sending IO control process is terminated by avSendIOCtrlExit() */
    public static final int		AV_ER_SENDIOCTRL_EXIT		    	=-20022;

    /** The UID is a lite UID */
    public static final int		AV_ER_NO_PERMISSION					=-20023;

    /** The length of password is wrong */
    public static final int		AV_ER_WRONG_ACCPWD_LENGTH           =-20024;

    /** IOTC session has been closed */
    public static final int		AV_ER_IOTC_SESSION_CLOSED			=-20025;

    /** IOTC is deinitialized */
    public static final int		AV_ER_IOTC_DEINITIALIZED			=-20026;

    /** IOTC channel is used by other av channel */
    public static final int		AV_ER_IOTC_CHANNEL_IN_USED			=-20027;

    /** AV channel is waiting key frame */
    public static final int		AV_ER_WAIT_KEY_FRAME				=-20028;

    /** The AV channel of specified AV channel ID is already in reset buffer process */
    public static final int		AV_ER_CLEANBUF_ALREADY_CALLED		=-20029;

    /** IOTC UDP/TCP socket send queue is full. */
    public static final int	AV_ER_SOCKET_QUEUE_FULL					=-20030;

    /** AV module is already initialized. It is not necessary to re-initialize. */
    public static final int	AV_ER_ALREADY_INITIALIZED               =-20031;

    /** Dynamic Adaptive Streaming over AVAPI notified program to clean buffer */
    public static final int	AV_ER_DASA_CLEAN_BUFFER                 =-20032;

    /** Function is not support, need to using correct AV Server and AV Client  */
    public static final int	AV_ER_NOT_SUPPORT                       =-20033;

    /** AV fails to initialize DTLS module. */
    public static final int	AV_ER_FAIL_INITIALIZE_DTLS              =-20034;

    /** AV fails to create channel for DTLS connection. */
    public static final int	AV_ER_FAIL_CREATE_DTLS                  =-20035;

    /** The AV channel of specified AV channel ID is already in request process */
    public static final int	AV_ER_REQUEST_ALREADY_CALLED	   	    =-20036;

    /** Function is not supported on remote side. */
    public static final int	AV_ER_REMOTE_NOT_SUPPORT                =-20037;

    /** The token to be sent exceeds MAX_TOKEN_LENGTH. */
    public static final int	AV_ER_TOKEN_EXCEED_MAX_SIZE             =-20038;

    /** The server fail because client not support DTLS */
    public static final int	AV_ER_REMOTE_NOT_SUPPORT_DTLS           =-20039;

    /** The server fail in authentication due to incorrect password/token with DTLS security mode enabled*/
    public static final int	AV_ER_DTLS_WRONG_PWD					=-20040;

    /** The server fail in authentication due to the passowrd/token auth function return value is smaller than 0*/
    public static final int	AV_ER_DTLS_AUTH_FAIL					=-20041;

    /** The SDK Version not support DTLS */
    public static final int	AV_ER_SDK_NOT_SUPPORT_DTLS              =-21334;



    @Deprecated public native static int avGetAVApiVer();	//save as Little endian
    public native static String avGetAVApiVersionString();
    public native static int  avInitialize(int nMaxNumAllowed);
    public native static int  avDeInitialize();
    public native static int  avSendIOCtrl(int avIndex, int ioType, byte[] ioCtrlBuf, int ioCtrlBufSize); //block thread,wait ack
    public native static int  avRecvIOCtrl(int avIndex, int[] pioType, byte[] ioCtrlBuf, int ioCtrlBufMaxSize, int timeout_ms);
    public native static int  avSendIOCtrlExit(int avIndex);

    //Device Side
    public native static void avServStop(int avIndex);
    public native static void avServExit(int nSID, int ChID);
    public native static int  avSendFrameData(int avIndex, byte[]data,int dataSize, byte[] pFrmInfo,int FrmInfoSize);
    public native static int  avSendAudioData(int avIndex, byte[]data,int dataSize, byte[] pFrmInfo,int FrmInfoSize);

    //Client Side
    public static int avClientStart(int nSID, byte[] viewAcc, byte[] viewPwd, int timeout_sec, int[]pservType, int ChID) {
        return avClientStart(nSID, new String(viewAcc), new String(viewPwd), timeout_sec, pservType, ChID);
    }
    @Deprecated public native static int avClientStart(int nSID, String viewAcc, String viewPwd, int timeout_sec, int[]pservType, int ChID);
    @Deprecated public native static int  avClientStart2(int nSID,String viewAcc,String viewPwd, int timeout_sec,int[]pservType,int ChID, int[] bResend);
    public native static int  avClientStartEx(St_AVClientStartInConfig avClientInConfig, St_AVClientStartOutConfig avlientOutConfig);

    public native static void avClientStop(int avIndex);
    public native static void avClientExit(int nSID, int ChID);
    public native static int  avRecvFrameData2(int avIndex,  byte[] buf, int inBufSize, int[] outBufSize, int[] outFrmSize,byte[] pFrmInfoBuf, int inFrmInfoBufSize, int[] outFrmInfoBufSize, int[] pFrmNo);
    public native static int  avRecvAudioData(int avIndex,byte[] buf, int bufMaxSize,byte[] pFrmInfo, int FrmInfoMaxSize, int[] pFrmNo);
    public native static int  avCheckAudioBuf(int avIndex); //return buf count
    @Deprecated public native static void avClientSetMaxBufSize(int nMaxBufSize);
    public native static int  avClientSetRecvBufMaxSize(int avIndex, int nMaxBufSizeKB);
    public native static int  avClientCleanBuf(int avIndex);
    public native static int  avClientCleanVideoBuf(int avIndex);
    public native static int  avClientCleanVideoBufNB(int avIndex);
    public native static int  avClientCleanAudioBuf(int avIndex);
    public native static int  avClientCleanLocalBuf(int avIndex);
    public native static int  avClientCleanLocalVideoBuf(int avIndex);

    @Deprecated public native static int  avServStart2(int nSID, String viewAcc, String viewPwd, int timeout_sec, int servType, int ChID);
    @Deprecated public native static int  avServStart3(int nSID, String viewAcc, String viewPwd, int timeout_sec, int servType, int ChID, int[] bResend);
    public native static int  avServStartEx(St_AVServStartInConfig avServerInConfig, St_AVServStartOutConfig avServerOutConfig);

    public native static void avServSetResendSize(int avIndex, int nSize);
    public native static int  avServSetDelayInterval(int avIndex, int nPacketNum, int nDelayMs);
    public native static float avResendBufUsageRate(int nAVChannelID);
    public native static float avClientRecvBufUsageRate(int nAVChannelID);
    @Deprecated public native static void AV_Set_Log_Path(String path, int maxSize);
    public native static int AV_Set_Log_Attr(St_LogAttr logAttr);
    public native static int avServResetBuffer(int avIndex, int eTarget, int Timeout_ms);/* eTarget: RESET_VIDEO = 0, RESET_AUDIO = 1, RESET_ALL = 2*/
    public native static int avClientRequestTokenWithIdentity(int av_index,String identity,String identity_description,String[] token,int[] status_code, int timeout_sec);
    public native static int avClientDeleteIdentity(int av_index, String identity, int identity_length, int[] status_code, int timeout_sec);
    public native static int avClientRequestIdentityArray(int av_index, ArrayList<St_AvIdentity>[] identities, int[] identity_count, int[] status_code, int timeout_sec);
    public native static int avClientRequestServerAbility(int av_index, String[] ability, int timeout_sec);
    public native static int avClientRequestChangeServerPassword(int av_index, String account,String old_password,String new_password,String[] new_iotc_authkey,int[] new_iotc_authkey_actual_length,int timeout_sec);
    public native static int avStatusCheck(int avIndex, St_AvStatus avStatus);
    public native static int avEnableVSaaS(String udid, String VSaaSconfig, avVSaaSConfigChangedFn notifyConfigChanged, avVSaaSUpdateContractInfoFn updateContractInfo);
    public native static int avServNotifyVSaasPullStream(VSaaSPullStreamAttr inputAttr, int timeout_sec);
    public native static int avServGetVSaasContractInfo(VSaaSContractInfo outputContractInfo, int timeout_sec);
    public native static int avSendJSONCtrlRequest(int av_index, String json_request, String[] json_response, int timeout_sec);
    /*##################Device Side CallBack##########################*/
    public interface avPasswordAuthFn {
        int password_auth(String account,String[] password);
    }

    public interface avTokenAuthFn {
        int token_auth(String identity,String[] token);
    }

    public interface avTokenRequestFn {
        int token_request(int av_index,String identity,String identity_description,String[] token);
    }

    public interface avTokenDeleteFn {
        int token_delete(int av_index,String identity);
    }

    public interface avIdentityArrayRequestFn {
        void identity_array_request(int av_index, ArrayList<St_AvIdentity> identities, int[] status_code);//identities is output
    }

    public interface avAbilityRequestFn {
        void ability_request(int av_index,String[] ability);
    }

    public interface avChangePasswordRequestFn {
        int change_password_request(int av_index,String account,String old_password ,String new_password ,String new_iotc_authkey);
    }

    public interface avVSaaSConfigChangedFn {
        void on_config_changed(String config);
    }

    public interface avVSaaSUpdateContractInfoFn {
        void on_update_contract(VSaaSContractInfo contract_info);
    }

    public interface avJsonCtrlRequestFn {
        void json_request(int av_index, String request_func, String json_format_args, String[] response);
    }

    static { try {
        System.loadLibrary("AVAPIs");
//        System.loadLibrary("AVAPIsT");
    }
    catch(UnsatisfiedLinkError ule){
        System.out.println("loadLibrary(AVAPIs),"+ule.getMessage());
    }
    }
}
