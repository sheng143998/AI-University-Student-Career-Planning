package com.itsheng.common.utils;

import com.aliyun.oss.ClientException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.OSSException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.Date;

@Data
@AllArgsConstructor
@Slf4j
public class AliOssUtil {

    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    private String bucketName;

    /**
     * 文件上传
     *
     * @param bytes
     * @param objectName
     * @return
     */
    public String upload(byte[] bytes, String objectName) {

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 创建PutObject请求。
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(bytes));
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }

        //文件访问路径规则 https://BucketName.Endpoint/ObjectName
        StringBuilder stringBuilder = new StringBuilder("https://");
        stringBuilder
                .append(bucketName)
                .append(".")
                .append(endpoint)
                .append("/")
                .append(objectName);

        log.info("文件上传到:{}", stringBuilder.toString());

        return stringBuilder.toString();
    }

    /**
     * 从 OSS 下载文件
     *
     * @param fileUrl OSS 文件 URL
     * @return 文件字节数组
     */
    public byte[] download(String fileUrl) {
        // 创建 OSSClient 实例
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 从 URL 中提取 objectName
            String objectName = extractObjectNameFromUrl(fileUrl);
            log.info("从 OSS 下载文件：{}", objectName);

            // 获取 OSS 对象
            var ossObject = ossClient.getObject(bucketName, objectName);
            try (var inputStream = ossObject.getObjectContent();
                 var outputStream = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                return outputStream.toByteArray();
            }
        } catch (Exception e) {
            log.error("从 OSS 下载文件失败：{}", e.getMessage(), e);
            throw new RuntimeException("从 OSS 下载文件失败：" + e.getMessage(), e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 生成带签名的预览 URL（短期有效，inline 展示）
     *
     * @param fileUrl OSS 文件 URL
     * @param expirationMinutes 过期时间（分钟）
     * @return 签名后的 URL
     */
    public String generatePresignedUrl(String fileUrl, int expirationMinutes) {
        // 创建 OSSClient 实例
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            // 从 URL 中提取 objectName
            String objectName = extractObjectNameFromUrl(fileUrl);

            // 设置 URL 过期时间
            Date expiration = new Date(System.currentTimeMillis() + expirationMinutes * 60 * 1000L);

            // 生成签名 URL
            URL signedUrl = ossClient.generatePresignedUrl(bucketName, objectName, expiration);
            log.info("生成签名 URL: {}", signedUrl.toString());

            return signedUrl.toString();
        } catch (Exception e) {
            log.error("生成签名 URL 失败：{}", e.getMessage(), e);
            throw new RuntimeException("生成签名 URL 失败：" + e.getMessage(), e);
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    /**
     * 从完整的 OSS URL 中提取 objectName
     */
    private String extractObjectNameFromUrl(String fileUrl) {
        // URL 格式：https://bucketName.endpoint/objectName
        String prefix = "https://" + bucketName + "." + endpoint + "/";
        if (fileUrl.startsWith(prefix)) {
            return fileUrl.substring(prefix.length());
        }
        // 尝试另一种格式：https://endpoint/bucketName/objectName
        int firstSlash = fileUrl.indexOf("/", 8);
        if (firstSlash > 0) {
            int secondSlash = fileUrl.indexOf("/", firstSlash + 1);
            if (secondSlash > 0) {
                return fileUrl.substring(secondSlash + 1);
            }
        }
        // 如果无法解析，返回原路径（相对路径）
        return fileUrl.contains("/") ? fileUrl.substring(fileUrl.lastIndexOf("/") + 1) : fileUrl;
    }
}
