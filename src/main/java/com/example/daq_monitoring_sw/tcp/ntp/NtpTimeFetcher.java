//package com.example.daq_monitoring_sw.tcp.ntp;
//
//import org.apache.commons.net.ntp.NTPUDPClient;
//import org.apache.commons.net.ntp.TimeInfo;
//
//import javax.swing.event.CaretListener;
//import java.net.InetAddress;
//import java.util.Date;
//
//public class NtpTimeFetcher {
//    private static final String NTP_SERVER = "pool.ntp.org";
//
//    public static Date fetchNtpTime(){
//        NTPUDPClient client = new NTPUDPClient();
//        client.setDefaultTimeout(1000); // 타임아웃 설정(밀리초)
//
//        try{
//            InetAddress inetAddress = InetAddress.getByName(NTP_SERVER);
//            TimeInfo timeInfo = client.getTime(inetAddress); //NTP 서버에서 시간 가져오기
//            timeInfo.computeDetails(); // 지연시간 계싼
//
//            // NTP 서버로부터의 정확한 시간
//            long returnTime = timeInfo.getMessage().getTransmitTimeStamp().getTime();
//            return new Date(returnTime);
//        } catch (Exception e) {
//            System.err.println("NTP 시간 가져오기 오류: " + e.getMessage());
//            return null;
//        } finally {
//            client.close();
//        }
//    }
//
//}
