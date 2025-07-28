package com.powervoice.kafka_producer.util;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Component
@Slf4j
public class FileDownloader {

    private static final String HOST = "192.168.0.85";
    private static final String USER = "pvoice";
    private static final String PASS = "skfodi$3312";

    public byte[] download(String remotePath) {
        boolean switchToSFTP = false;
        try {
            if (switchToSFTP) {
                return downloadViaSFTP(remotePath);
            } else {
                return downloadViaFTP(remotePath);
            }
        } catch (Exception e) {
            log.error("Download failed: {}", remotePath, e);
            return null;
        }
    }

    private byte[] downloadViaFTP(String path) throws IOException {
        FTPClient ftp = new FTPClient();

        //FTP 접속 실패 예외 처리 추가
        try{
            ftp.connect(HOST);
            ftp.login(USER, PASS);
            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);
        }catch (IOException e){
            log.error("[FTP ERROR] 연결 실패: {}", e.getMessage(), e);
            throw new RuntimeException("FTP 연결 실패");
        }

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            boolean ok = ftp.retrieveFile(path, baos);
            return ok ? baos.toByteArray() : null;
        } finally {
            ftp.logout();
            ftp.disconnect();
        }
    }

    private byte[] downloadViaSFTP(String path) throws JSchException, SftpException, IOException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(USER, HOST, 22);
        session.setPassword(PASS);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            sftp.get(path, baos);
            return baos.toByteArray();
        } finally {
            sftp.disconnect();
            session.disconnect();
        }
    }
}

