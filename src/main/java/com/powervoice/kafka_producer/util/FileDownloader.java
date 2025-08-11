package com.powervoice.kafka_producer.util;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
@Slf4j
@RequiredArgsConstructor
public class FileDownloader {

    @Value("${file.remote.host}") private String host;
    @Value("${file.remote.user}") private String user;
    @Value("${file.remote.pass}") private String pass;
    @Value("${file.remote.sftp:false}") private boolean useSftp;

    public byte[] download(String remotePath) {
        try {
            return useSftp ? downloadViaSFTP(remotePath) : downloadViaFTP(remotePath);
        } catch (Exception e) {
            log.error("Download failed: {}", remotePath, e);
            return null;
        }
    }

    private byte[] downloadViaFTP(String path) throws Exception {
        FTPClient ftp = new FTPClient();
        try {
            ftp.connect(host);
            if (!ftp.login(user, pass)) throw new RuntimeException("FTP login failed");
            ftp.enterLocalPassiveMode();
            ftp.setFileType(FTP.BINARY_FILE_TYPE);

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                boolean ok = ftp.retrieveFile(path, baos);
                if (!ok) throw new RuntimeException("FTP retrieveFile false: " + path);
                return baos.toByteArray();
            }
        } finally {
            try { ftp.logout(); } catch (Exception ignore) {}
            try { ftp.disconnect(); } catch (Exception ignore) {}
        }
    }

    private byte[] downloadViaSFTP(String path) throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(user, host, 22);
        session.setPassword(pass);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();

        ChannelSftp sftp = (ChannelSftp) session.openChannel("sftp");
        sftp.connect();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            sftp.get(path, baos);
            return baos.toByteArray();
        } finally {
            try { sftp.disconnect(); } catch (Exception ignore) {}
            try { session.disconnect(); } catch (Exception ignore) {}
        }
    }
}
